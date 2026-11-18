// Runs the whole app locally on macOS, Linux and Windows (Java 25 compact source file, no build step):
//   backend  (Spring Boot)                                 http://localhost:8080
//   frontend (dev server with live reload, proxies the API) http://localhost:5173
// Usage, from anywhere inside the repository:  java scripts/Dev.java
// Same behaviour as dev.sh. Ctrl-C stops both processes.
// (No #! line: the java launcher only accepts shebangs in files that do not end in .java.)
import module java.base;

static final int BACKEND_PORT = 8080;
static final int FRONTEND_PORT = 5173;
static final int DB_PORT = 5432;
static final boolean WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");

static final String HELP = """
        Usage: java scripts/Dev.java [--help]

        Starts the Spring Boot backend on http://localhost:%d and the frontend dev server
        (live reload, API proxied to the backend) on http://localhost:%d, with [BE]/[FE]
        prefixed logs. Ctrl-C stops both. Run it from anywhere inside the repository.
        PostgreSQL is expected on localhost:%d (see backend/.env).
        """.formatted(BACKEND_PORT, FRONTEND_PORT, DB_PORT);

/** One long-running child process and the label for its log lines. */
record Service(String tag, Path directory, List<String> command) {
}

void main(String[] args) throws Exception {
    if (List.of(args).contains("--help") || List.of(args).contains("-h")) {
        IO.println(HELP);
        return;
    }
    Path root = findRepositoryRoot();
    if (!isListening(DB_PORT)) {
        IO.println("WARN: PostgreSQL is not reachable on localhost:" + DB_PORT
                + " - the backend will fail to start until it is running.");
    }
    for (int port : List.of(BACKEND_PORT, FRONTEND_PORT)) {
        if (isListening(port)) {
            IO.println("Port " + port + " is already in use; stop whatever is running there first.");
            System.exit(1);
        }
    }
    Path frontend = root.resolve("frontend");
    if (!Files.isDirectory(frontend.resolve("node_modules"))) {
        IO.println("Installing frontend dependencies...");
        int exit = new ProcessBuilder(shell(List.of(npm(), "install"))).directory(frontend.toFile()).inheritIO().start().waitFor();
        if (exit != 0) {
            System.exit(exit);
        }
    }

    var services = List.of(
            new Service("[BE]", root.resolve("backend"), List.of(mavenWrapper(), "-q", "spring-boot:run")),
            new Service("[FE]", frontend, List.of(npm(), "run", "dev")));
    var processes = new CopyOnWriteArrayList<Process>();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> processes.forEach(this::stopTree)));
    for (Service service : services) {
        processes.add(start(service));
    }
    IO.println("Frontend: http://localhost:" + FRONTEND_PORT + "  Backend: http://localhost:" + BACKEND_PORT
            + "  (Ctrl-C stops both)");

    // when either side exits, stop the other one (via the shutdown hook)
    CompletableFuture.anyOf(processes.stream().map(Process::onExit).toArray(CompletableFuture[]::new)).join();
    IO.println("A process exited; shutting down the other one.");
    System.exit(1);
}

Process start(Service service) throws IOException {
    Process process = new ProcessBuilder(shell(service.command()))
            .directory(service.directory().toFile())
            .redirectErrorStream(true)
            .start();
    Thread.ofVirtual().name(service.tag()).start(() -> {
        try (var output = process.inputReader()) {
            output.lines().forEach(line -> IO.println(service.tag() + " " + line));
        } catch (IOException | UncheckedIOException ignored) {
            // stream closes when the process dies
        }
    });
    return process;
}

/** Stops a process and everything it started (Maven forks the app JVM, npm forks node and tsc). */
void stopTree(Process process) {
    List<ProcessHandle> tree = new ArrayList<>(process.descendants().toList());
    tree.add(process.toHandle());
    tree.forEach(ProcessHandle::destroy);
    for (ProcessHandle handle : tree) {
        try {
            handle.onExit().get(10, TimeUnit.SECONDS);
        } catch (Exception stillRunning) {
            handle.destroyForcibly();
        }
    }
}

/** Windows runs .cmd scripts through cmd.exe; elsewhere commands run directly. */
List<String> shell(List<String> command) {
    if (!WINDOWS) {
        return command;
    }
    var withShell = new ArrayList<>(List.of("cmd.exe", "/c"));
    withShell.addAll(command);
    return withShell;
}

String mavenWrapper() {
    return WINDOWS ? "mvnw.cmd" : "./mvnw";
}

String npm() {
    return WINDOWS ? "npm.cmd" : "npm";
}

boolean isListening(int port) {
    try (var socket = new Socket()) {
        socket.connect(new InetSocketAddress("localhost", port), 300);
        return true;
    } catch (IOException notListening) {
        return false;
    }
}

Path findRepositoryRoot() {
    for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
        if (Files.exists(dir.resolve("backend").resolve("pom.xml"))
                && Files.exists(dir.resolve("frontend").resolve("package.json"))) {
            return dir;
        }
    }
    throw new IllegalStateException("Run this from inside the E-Commerce-Website repository.");
}

// npm run dev: one build, then keep rebuilding on changes and serve on http://localhost:5173 with live reload.
import { build, watch } from './build.mjs';
import { startDevServer } from './dev-server.mjs';

try {
  await build();
} catch (error) {
  // keep going: tsc --watch below reports type errors as you fix them
  console.error(error.message);
}
const tsc = watch();
const server = startDevServer();

function shutdown() {
  tsc.kill();
  server.close();
  process.exit(0);
}
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);

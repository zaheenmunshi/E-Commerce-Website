// Local dev server (Node built-ins only).
// - Serves files that exist in dist/ (HTML, CSS, images, compiled JS).
// - Proxies everything else (pretty page routes, /api, /uploads, /oauth2, /login/oauth2, /actuator)
//   to the Spring Boot backend, keeping the Host header so OAuth redirects come back here.
// - Live reload: HTML gets an external <script> that listens on /__dev/events (CSP-safe).
import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const DIST_DIR = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', 'dist');
const PORT = Number(process.env.FRONTEND_PORT ?? 5173);
const BACKEND = new URL(process.env.BACKEND_URL ?? 'http://localhost:8080');
const LIVE_RELOAD_TAG = '<script type="module" src="/__dev/livereload.js"></script>';
const LIVE_RELOAD_JS = `const events = new EventSource('/__dev/events');
events.addEventListener('message', (event) => { if (event.data === 'reload') location.reload(); });
`;

const TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.map': 'application/json; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.webp': 'image/webp',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
};

const reloadClients = new Set();

/** Maps a URL path to a file inside dist/, or null (never escapes dist/). */
function resolveStatic(pathname) {
  let decoded;
  try {
    decoded = decodeURIComponent(pathname);
  } catch {
    return null;
  }
  if (decoded.includes('\0')) return null;
  const file = path.join(DIST_DIR, decoded === '/' ? 'index.html' : decoded);
  if (!file.startsWith(DIST_DIR + path.sep)) return null;
  try {
    return fs.statSync(file).isFile() ? file : null;
  } catch {
    return null;
  }
}

function injectLiveReload(html) {
  return html.includes('</body>') ? html.replace('</body>', `${LIVE_RELOAD_TAG}\n</body>`) : html + LIVE_RELOAD_TAG;
}

function serveFile(file, req, res) {
  const type = TYPES[path.extname(file).toLowerCase()] ?? 'application/octet-stream';
  const headers = { 'content-type': type, 'cache-control': 'no-store', 'x-content-type-options': 'nosniff' };
  if (type.startsWith('text/html')) {
    const body = injectLiveReload(fs.readFileSync(file, 'utf8'));
    res.writeHead(200, { ...headers, 'content-length': Buffer.byteLength(body) });
    res.end(req.method === 'HEAD' ? undefined : body);
    return;
  }
  res.writeHead(200, { ...headers, 'content-length': fs.statSync(file).size });
  if (req.method === 'HEAD') res.end();
  else fs.createReadStream(file).pipe(res);
}

function proxy(req, res) {
  const forwardedFor = [req.headers['x-forwarded-for'], req.socket.remoteAddress].filter(Boolean).join(', ');
  const upstream = http.request(
    {
      hostname: BACKEND.hostname,
      port: BACKEND.port || 80,
      method: req.method,
      path: req.url,
      headers: { ...req.headers, 'x-forwarded-for': forwardedFor, 'x-forwarded-proto': 'http' },
    },
    (upstreamRes) => {
      const type = upstreamRes.headers['content-type'] ?? '';
      if (!type.startsWith('text/html') || upstreamRes.headers['content-encoding']) {
        res.writeHead(upstreamRes.statusCode ?? 502, upstreamRes.headers);
        upstreamRes.pipe(res);
        return;
      }
      const chunks = [];
      upstreamRes.on('data', (chunk) => chunks.push(chunk));
      upstreamRes.on('end', () => {
        const body = injectLiveReload(Buffer.concat(chunks).toString('utf8'));
        const headers = { ...upstreamRes.headers, 'content-length': Buffer.byteLength(body) };
        delete headers['transfer-encoding'];
        res.writeHead(upstreamRes.statusCode ?? 502, headers);
        res.end(body);
      });
    },
  );
  upstream.on('error', () => {
    if (res.headersSent) return res.destroy();
    res.writeHead(502, { 'content-type': 'text/plain; charset=utf-8' });
    res.end(`Backend not reachable at ${BACKEND.origin}. Start it with ./dev.sh or "cd backend && ./mvnw spring-boot:run".`);
  });
  req.pipe(upstream);
}

function openReloadStream(req, res) {
  res.writeHead(200, { 'content-type': 'text/event-stream', 'cache-control': 'no-cache', connection: 'keep-alive' });
  res.write(': connected\n\n');
  reloadClients.add(res);
  req.on('close', () => reloadClients.delete(res));
}

function watchDist() {
  fs.mkdirSync(DIST_DIR, { recursive: true });
  let timer;
  fs.watch(DIST_DIR, { recursive: true }, () => {
    clearTimeout(timer);
    timer = setTimeout(() => reloadClients.forEach((client) => client.write('data: reload\n\n')), 200);
  });
}

export function startDevServer() {
  watchDist();
  const server = http.createServer((req, res) => {
    const { pathname } = new URL(req.url ?? '/', 'http://localhost');
    if (pathname === '/__dev/events') return openReloadStream(req, res);
    if (pathname === '/__dev/livereload.js') {
      res.writeHead(200, { 'content-type': 'text/javascript; charset=utf-8', 'cache-control': 'no-store' });
      return res.end(LIVE_RELOAD_JS);
    }
    const file = req.method === 'GET' || req.method === 'HEAD' ? resolveStatic(pathname) : null;
    if (file) return serveFile(file, req, res);
    return proxy(req, res);
  });
  server.listen(PORT, () => {
    console.log(`frontend dev server: http://localhost:${PORT}  (API + page routes proxied to ${BACKEND.origin})`);
  });
  return server;
}

const isCli = process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href;
if (isCli) startDevServer();

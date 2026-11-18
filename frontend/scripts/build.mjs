// Builds the frontend into dist/: copies public/ (HTML, CSS, images) and compiles src/ (TypeScript) to dist/js.
//   node scripts/build.mjs           one-off build
//   node scripts/build.mjs --watch   rebuild on every change (used by npm run dev)
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
export const PUBLIC_DIR = path.join(ROOT, 'public');
export const DIST_DIR = path.join(ROOT, 'dist');
const TSC = path.join(ROOT, 'node_modules', '.bin', process.platform === 'win32' ? 'tsc.cmd' : 'tsc');

function copyPublic() {
  fs.cpSync(PUBLIC_DIR, DIST_DIR, { recursive: true });
}

function runTsc(extraArgs) {
  return spawn(TSC, ['-p', path.join(ROOT, 'tsconfig.json'), ...extraArgs], { cwd: ROOT, stdio: 'inherit' });
}

export function build() {
  fs.rmSync(DIST_DIR, { recursive: true, force: true });
  copyPublic();
  return new Promise((resolve, reject) => {
    runTsc([]).on('exit', (code) => (code === 0 ? resolve() : reject(new Error(`tsc exited with ${code}`))));
  });
}

/** Keeps dist/ in sync: tsc --watch for src/, a recursive fs.watch for public/. */
export function watch() {
  const tsc = runTsc(['--watch', '--preserveWatchOutput']);
  fs.watch(PUBLIC_DIR, { recursive: true }, (_event, filename) => {
    if (!filename) return;
    const source = path.join(PUBLIC_DIR, filename);
    const target = path.join(DIST_DIR, filename);
    if (fs.existsSync(source)) {
      if (fs.statSync(source).isFile()) {
        fs.mkdirSync(path.dirname(target), { recursive: true });
        fs.copyFileSync(source, target);
      }
    } else {
      fs.rmSync(target, { recursive: true, force: true });
    }
  });
  return tsc;
}

const isCli = process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href;
if (isCli) {
  try {
    await build();
    console.log(`built ${path.relative(process.cwd(), DIST_DIR) || 'dist'}`);
    if (process.argv.includes('--watch')) {
      watch();
      console.log('watching public/ and src/ for changes...');
    }
  } catch (error) {
    console.error(error.message);
    process.exit(1);
  }
}

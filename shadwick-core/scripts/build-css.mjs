/*
  Builds every CSS bundle: one per entry in src/main/tailwind/entries (a skin x bundle variant), each as
  target/classes/dev/jbaby/shadwick/css/<entry>.css and, minified, <entry>.min.css, which Wicket serves in
  deployment mode. With --watch it rebuilds the unminified bundles on every change, which is what Wicket serves in
  development mode, so a running application picks the change up on reload.
*/
import {spawn} from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const entriesDir = path.join(root, 'src/main/tailwind/entries');
const outDir = path.join(root, 'target/classes/dev/jbaby/shadwick/css');
const watch = process.argv.includes('--watch');

// The CLI's own script, run with this Node: no shell, the same on every platform
const require = createRequire(import.meta.url);
const cliPackage = require.resolve('@tailwindcss/cli/package.json');
const bin = JSON.parse(fs.readFileSync(cliPackage, 'utf8')).bin;
const cli = path.join(path.dirname(cliPackage), typeof bin === 'string' ? bin : bin.tailwindcss);

const entries = fs.readdirSync(entriesDir).filter(file => file.endsWith('.css')).sort();
if (entries.length === 0) {
  throw new Error(`No entries in ${entriesDir}`);
}
fs.mkdirSync(outDir, {recursive: true});

function tailwind(input, output, ...flags) {
  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, [cli, '-i', input, '-o', output, ...flags], {cwd: root, stdio: ['ignore', 'inherit', 'pipe']});
    // The CLI reports progress on stderr, which Maven would log as errors; a failure still fails the build
    child.stderr.pipe(process.stdout);
    child.on('error', reject);
    child.on('exit', code => code === 0 ? resolve() : reject(new Error(`tailwindcss exited with ${code} for ${input}`)));
  });
}

const builds = entries.flatMap(entry => {
  const input = path.join(entriesDir, entry);
  const name = path.basename(entry, '.css');
  const output = path.join(outDir, `${name}.css`);
  return watch
      ? [tailwind(input, output, '--watch')]
      : [tailwind(input, output), tailwind(input, path.join(outDir, `${name}.min.css`), '--minify')];
});

try {
  await Promise.all(builds);
} catch (error) {
  console.error(error.message);
  process.exit(1);
}

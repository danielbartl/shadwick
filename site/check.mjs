import {access, readFile, readdir} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), 'dist');
const html = await readFile(path.join(root, 'index.html'), 'utf8');
const ids = [...html.matchAll(/\bid="([^"]+)"/g)].map(match => match[1]);
if (new Set(ids).size !== ids.length) throw new Error('Duplicate HTML IDs.');
if (/__[A-Z_]+__/.test(html)) throw new Error('Unresolved build placeholder.');
let references = 0;
for (const match of html.matchAll(/\b(?:src|href)="([^"]+)"/g)) {
  const reference = match[1];
  if (/^(?:https?:|mailto:)/.test(reference) || reference === '#') continue;
  if (reference.startsWith('#')) {
    if (!ids.includes(reference.slice(1))) throw new Error(`Missing anchor ${reference}`);
  } else {
    if (reference.startsWith('/')) throw new Error(`Root-relative asset breaks GitHub project Pages: ${reference}`);
    const [filename, fragment] = reference.split('#');
    const asset = path.resolve(root, filename);
    if (!asset.startsWith(root + path.sep)) throw new Error(`Asset outside website: ${reference}`);
    await access(asset);
    if (fragment && !(await readFile(asset, 'utf8')).includes(`id="${fragment}"`)) throw new Error(`Missing SVG symbol: ${reference}`);
  }
  references++;
}
const css = await readFile(path.join(root, 'assets/site.css'), 'utf8');
for (const [, filename] of css.matchAll(/url\(['"]?([^)'"\s]+)['"]?\)/g)) await access(path.resolve(root, 'assets', filename));
await access(path.join(root, 'assets/social.png'));
for (const file of ['LICENSE.txt', 'NOTICE.txt', 'assets/fonts/OFL.txt']) await access(path.join(root, file));
let bytes = 0;
async function size(directory) {
  for (const entry of await readdir(directory, {withFileTypes:true})) {
    const full = path.join(directory, entry.name);
    if (entry.isDirectory()) await size(full);
    else bytes += (await readFile(full)).length;
  }
}
await size(root);
console.log(`Site checks passed: ${references} internal references, ${ids.length} unique IDs, ${(bytes / 1024).toFixed(0)} KB total.`);

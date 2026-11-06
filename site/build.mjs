import {access, cp, mkdir, readFile, rm, writeFile} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
import {skins} from './skins.mjs';

const root = path.dirname(fileURLToPath(import.meta.url));
const output = path.join(root, 'dist');
const css = path.resolve(root, '../shadwick-core/target/classes/dev/jbaby/shadwick/css');
for (const skin of skins) {
  try { await access(path.join(css, `shadwick-${skin}-embedded.min.css`)); }
  catch { throw new Error('Build the library CSS first: ./mvnw -B -pl shadwick-core -am package -DskipTests'); }
}
const repository = process.env.GITHUB_REPOSITORY || 'danielbartl/shadwick';
const [owner, name] = repository.split('/');
const defaultUrl = `https://${owner}.github.io/${name.toLowerCase() === `${owner}.github.io`.toLowerCase() ? '' : `${name}/`}`;
const url = new URL(process.env.SITE_URL || defaultUrl);
if (!['https:', 'http:'].includes(url.protocol)) throw new Error('SITE_URL must be an HTTP(S) URL.');
url.hash = ''; url.search = '';
if (!url.pathname.endsWith('/')) url.pathname += '/';
const escapeHtml = value => value.replaceAll('&', '&amp;').replaceAll('"', '&quot;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
let html = (await readFile(path.join(root, 'index.html'), 'utf8')).replaceAll('__SITE_URL__', escapeHtml(url.href));
await rm(output, {recursive: true, force: true});
await mkdir(output, {recursive: true});
await cp(path.join(root, 'assets'), path.join(output, 'assets'), {recursive: true});
for (const skin of skins) {
  await cp(path.join(css, `shadwick-${skin}-embedded.min.css`), path.join(output, 'assets', `shadwick-${skin}.css`));
}
await cp(path.resolve(css, '../js/shadwick.js'), path.join(output, 'assets/shadwick.js'));
await cp(path.resolve(css, '../js/chart.js'), path.join(output, 'assets/chart.js'));
await cp(path.resolve(css, '../js/calendar.js'), path.join(output, 'assets/calendar.js'));
await cp(path.resolve(css, '../js/combobox.js'), path.join(output, 'assets/combobox.js'));
await cp(path.resolve(css, '../js/select.js'), path.join(output, 'assets/select.js'));
await cp(path.resolve(css, '../js/theme.js'), path.join(output, 'assets/theme.js'));
await cp(path.resolve(root, '../NOTICE'), path.join(output, 'NOTICE.txt'));
await cp(path.resolve(root, '../LICENSE'), path.join(output, 'LICENSE.txt'));
await cp(path.resolve(css, '../fonts/balsamiq-sans'), path.join(output, 'fonts/balsamiq-sans'), {recursive: true});

// Browsers may retain Pages assets for ten minutes. Content versions keep a new
// page from using old component CSS/scripts, including when switching skins.
async function versioned(reference) {
  const [filename, fragment] = reference.split('#');
  const content = await readFile(path.join(output, filename));
  const version = createHash('sha256').update(content).digest('hex').slice(0, 12);
  return `${filename}?v=${version}${fragment === undefined ? '' : `#${fragment}`}`;
}
const sprite = await versioned('./assets/icons.svg');
const tableScript = path.join(output, 'assets/table-demo.js');
await writeFile(tableScript, (await readFile(tableScript, 'utf8')).replaceAll('./assets/icons.svg', sprite));
for (const skin of skins) {
  const stylesheet = (await versioned(`./assets/shadwick-${skin}.css`)).replace('./assets/', '');
  html = html.replace(`<option value="${skin}">`, `<option value="${skin}" data-stylesheet="${stylesheet}">`);
}
const references = new Map();
for (const [, reference] of html.matchAll(/\b(?:src|href)="([^"]+)"/g)) {
  if (/^\.\/assets\/.*\.(?:css|js|svg|png)(?:#.*)?$/.test(reference) && !references.has(reference)) {
    references.set(reference, await versioned(reference));
  }
}
html = html.replace(/\b(src|href)="([^"]+)"/g, (_, attribute, reference) =>
  `${attribute}="${references.get(reference) || reference}"`);
await writeFile(path.join(output, 'index.html'), html);
await writeFile(path.join(output, '.nojekyll'), '');
await writeFile(path.join(output, 'robots.txt'), `User-agent: *\nAllow: /\nSitemap: ${url.href}sitemap.xml\n`);
await writeFile(path.join(output, 'sitemap.xml'), `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"><url><loc>${escapeHtml(url.href)}</loc></url></urlset>\n`);
console.log(`Built ${output}\nCanonical URL: ${url.href}`);

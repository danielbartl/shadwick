import {access, cp, mkdir, readFile, rm, writeFile} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
import {skins, skinDetails} from './skins.mjs';
import {buildDocumentation} from './docs/build.mjs';
import {buildAgentResources} from './agents.mjs';

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
const guideCount = JSON.parse(await readFile(path.resolve(root, '../docs/catalog.json'), 'utf8')).length;
html = html.replaceAll('__GUIDE_COUNT__', String(guideCount));
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
// Version every local asset reference in a page, resolving it from that page's directory.
async function versionPage(page, directory = '.') {
  const references = new Map();
  for (const [, reference] of page.matchAll(/\b(?:src|href)="([^"]+)"/g)) {
    if (/^\.{1,2}\/(?:\.\.\/)*assets\/.*\.(?:css|js|svg|png|webp)(?:#.*)?$/.test(reference) && !references.has(reference)) {
      const fromRoot = './' + path.posix.join(directory, reference);
      const version = (await versioned(fromRoot)).slice(fromRoot.split('#')[0].length);
      references.set(reference, reference.split('#')[0] + version);
    }
  }
  return page.replace(/\b(src|href)="([^"]+)"/g, (_, attribute, reference) =>
    `${attribute}="${references.get(reference) || reference}"`);
}

// One small page per skin, each loading only that skin's real embedded bundle.
const sampleTemplate = await readFile(path.join(root, 'skin-sample.html'), 'utf8');
await mkdir(path.join(output, 'skin-samples'), {recursive: true});
const gallery = [];
for (const skin of skins) {
  const [label, description] = skinDetails[skin];
  const page = sampleTemplate.replaceAll('__SKIN__', skin).replaceAll('__SKIN_NAME__', escapeHtml(label));
  await writeFile(path.join(output, 'skin-samples', `${skin}.html`), await versionPage(page, 'skin-samples'));
  gallery.push(`<li class="skin-tile" data-skin="${skin}"><iframe src="./skin-samples/${skin}.html" title="${escapeHtml(label)} skin sample" loading="lazy" tabindex="-1" aria-hidden="true"></iframe>`
    + `<div class="skin-tile-caption"><p><strong>${escapeHtml(label)}</strong><span>${escapeHtml(description)}</span></p>`
    + `<button type="button" class="skin-try" data-skin-choice="${skin}" hidden>Try ${escapeHtml(label)}<svg aria-hidden="true"><use href="./assets/icons.svg#arrow-up"/></svg></button></div></li>`);
}
html = html.replace('__SKIN_GALLERY__', gallery.join('\n        '));
html = await versionPage(html);
await writeFile(path.join(output, 'index.html'), html);
await writeFile(path.join(output, '.nojekyll'), '');
await writeFile(path.join(output, 'robots.txt'), `User-agent: *\nAllow: /\nSitemap: ${url.href}sitemap.xml\n`);
const documentationUrls = await buildDocumentation({repositoryRoot:path.resolve(root,'..'),output,siteUrl:url,repository});
await buildAgentResources({repositoryRoot:path.resolve(root,'..'),output,siteUrl:url,repository});
await writeFile(path.join(output, 'sitemap.xml'), `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"><url><loc>${escapeHtml(url.href)}</loc></url>${documentationUrls.map(page => `<url><loc>${escapeHtml(page)}</loc></url>`).join('')}</urlset>\n`);
console.log(`Built ${output}\nCanonical URL: ${url.href}`);

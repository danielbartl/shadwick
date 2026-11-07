import {readFile, readdir, mkdir, cp, access, writeFile} from 'node:fs/promises';
import path from 'node:path';
import {markdownLinks} from './docs/agent-markdown.mjs';

export const skillExamples = ['QuickStartApplication.java', 'QuickStartPage.java', 'QuickStartPage.html'];

async function skillFiles(root, relative = '') {
  const files = [];
  for (const entry of await readdir(path.join(root, relative), {withFileTypes: true})) {
    const file = relative ? relative + '/' + entry.name : entry.name;
    if (entry.isSymbolicLink()) throw new Error('Skill resources must be self-contained: ' + file);
    if (entry.isDirectory()) files.push(...await skillFiles(root, file));
    else if (entry.isFile()) files.push(file);
  }
  return files.sort();
}

/** Check portable skill references and the parity of templates with the compiled documentation examples. */
export async function validateSkill({repositoryRoot, catalog}) {
  const root = path.join(repositoryRoot, 'shadwick'), files = await skillFiles(root);
  for (const required of ['SKILL.md', 'agents/openai.yaml', 'LICENSE.txt']) {
    if (!files.includes(required)) throw new Error('Missing skill resource: ' + required);
  }
  for (const name of skillExamples) {
    const actual = await readFile(path.join(root, 'assets', name));
    const expected = await readFile(path.join(repositoryRoot, 'docs/examples', name));
    if (!actual.equals(expected)) throw new Error('Skill example differs from the compiled documentation example: ' + name);
  }
  if (!(await readFile(path.join(root, 'LICENSE.txt'))).equals(await readFile(path.join(repositoryRoot, 'LICENSE')))) {
    throw new Error('Skill license differs from the repository license');
  }
  const guides = new Set(catalog.map(entry => entry.slug));
  for (const file of files.filter(name => name.endsWith('.md'))) {
    const prose = await readFile(path.join(root, file), 'utf8');
    for (const reference of markdownLinks(prose)) {
      const prefix = 'https://danielbartl.github.io/shadwick/docs/';
      if (reference.startsWith(prefix) && reference.includes('/index.md')) {
        const slug = reference.slice(prefix.length).split('/index.md')[0];
        if (!guides.has(slug)) throw new Error('Unknown skill guide: ' + reference);
      }
      if (/^(?:https?:|mailto:|#)/.test(reference)) continue;
      const filename = decodeURIComponent(reference.split('#')[0]);
      const resolved = path.resolve(root, path.dirname(file), filename);
      if (!resolved.startsWith(root + path.sep) || filename.includes('\\') || /^\w+:/.test(filename)) {
        throw new Error('Skill reference leaves its package: ' + reference);
      }
      await access(resolved);
    }
  }
  return files;
}

export async function buildAgentResources({repositoryRoot, output, siteUrl, repository}) {
  const catalog = JSON.parse(await readFile(path.join(repositoryRoot, 'docs/catalog.json'), 'utf8'));
  const files = await validateSkill({repositoryRoot, catalog});
  const pom = await readFile(path.join(repositoryRoot, 'pom.xml'), 'utf8');
  const property = name => {
    const value = new RegExp('<' + name.replaceAll('.', '\\.') + '>([^<]+)</' + name.replaceAll('.', '\\.') + '>').exec(pom)?.[1];
    if (!value) throw new Error('Missing repository metadata: ' + name);
    return value;
  };
  const version = property('version'), java = property('maven.compiler.release'), wicket = property('wicket.version');
  const availability = (await readFile(path.join(repositoryRoot, 'README.md'), 'utf8')).includes('Not yet on Maven Central')
    ? 'The library is not yet on Maven Central; follow the installation guide to install it locally.'
    : 'Follow the installation guide for published coordinates and availability.';
  const url = relative => new URL(relative, siteUrl).href;
  let index = '# Shadwick\n\n> Server-rendered shadcn-style UI components for Apache Wicket, using typed Java models and matching Wicket markup.\n\n';
  index += `Artifact: dev.jbaby:shadwick. Repository version: ${version}. This build uses Java ${java} and Wicket ${wicket}. ${availability}\n\n`;
  index += 'Consuming applications do not need Node or Tailwind. Install Shadwick in WebApplication.init(), keep the default strict CSP, and pair Java IDs with matching wicket:id attributes. Application services own persistence and authorization.\n\n';
  index += `## Coding skill\n\n- [Shadwick skill](${url('skills/shadwick/SKILL.md')}): Component selection, models, forms, partial Ajax updates, data, themes and locale; includes working templates and supporting references.\n`;
  for (const group of new Set(catalog.map(entry => entry.group))) {
    index += '\n## ' + group + '\n\n';
    for (const entry of catalog.filter(entry => entry.group === group)) {
      const relative = 'docs/' + entry.slug + '/index.md';
      await access(path.join(output, relative));
      index += `- [${entry.title}](${url(relative)}): ${entry.description}\n`;
    }
  }
  index += `\n## Optional\n\n- [Source repository](https://github.com/${repository}): Java types, tests and runnable example pages.\n`;
  index += '- [Live component catalog](https://shadwick-production.up.railway.app/components): Interactive examples with Java and Wicket markup.\n';
  index += '- [Clover Studio](https://shadwick-production.up.railway.app/): A connected project-to-payment sample application.\n';
  const destination = path.join(output, 'skills/shadwick');
  await mkdir(destination, {recursive: true});
  await cp(path.join(repositoryRoot, 'shadwick'), destination, {recursive: true});
  await writeFile(path.join(output, 'llms.txt'), index);
  console.log(`Agent resources built: ${catalog.length} Markdown guides, ${files.length} skill files and llms.txt.`);
  return {guideCount: catalog.length, skillFiles: files};
}

/** Crawl published agent links within the configured project subpath without depending on external hosts. */
export async function checkAgentResources({repositoryRoot, output, siteUrl}) {
  const catalog = JSON.parse(await readFile(path.join(repositoryRoot, 'docs/catalog.json'), 'utf8'));
  const files = await validateSkill({repositoryRoot, catalog});
  const index = await readFile(path.join(output, 'llms.txt'), 'utf8');
  const expected = catalog.map(entry => new URL('docs/' + entry.slug + '/index.md', siteUrl).href).sort();
  const links = markdownLinks(index);
  const actual = links.filter(reference => reference.endsWith('/index.md')).sort();
  if (JSON.stringify(actual) !== JSON.stringify(expected)) throw new Error('llms.txt guide inventory differs from the documentation catalog');
  const localFile = reference => {
    const url = new URL(reference, siteUrl);
    if (url.origin !== siteUrl.origin) return null;
    if (!url.pathname.startsWith(siteUrl.pathname)) throw new Error('Agent link leaves the project subpath: ' + reference);
    const file = path.resolve(output, decodeURIComponent(url.pathname.slice(siteUrl.pathname.length)));
    if (!file.startsWith(output + path.sep)) throw new Error('Agent link leaves the website: ' + reference);
    return file;
  };
  for (const reference of links) { const file = localFile(reference); if (file) await access(file); }
  const markdown = ['docs/index.md', ...catalog.map(entry => 'docs/' + entry.slug + '/index.md'),
    ...files.filter(file => file.endsWith('.md')).map(file => 'skills/shadwick/' + file)];
  for (const relative of markdown) {
    const file = path.join(output, relative), raw = await readFile(file, 'utf8');
    for (const reference of markdownLinks(raw)) {
      const absolute = new URL(reference, new URL(relative, siteUrl));
      const target = localFile(absolute.href); if (target) await access(target);
    }
  }
  for (const relative of files) {
    if (!(await readFile(path.join(output, 'skills/shadwick', relative))).equals(await readFile(path.join(repositoryRoot, 'shadwick', relative)))) {
      throw new Error('Published skill differs from its source: ' + relative);
    }
  }
  return {guides: catalog.length, files: files.length};
}

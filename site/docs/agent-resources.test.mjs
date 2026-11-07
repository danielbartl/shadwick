import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtemp, mkdir, writeFile, readFile, rm, access} from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import {buildAgentResources, checkAgentResources, skillExamples} from '../agents.mjs';
import {mapMarkdownLinks, markdownLinks} from './agent-markdown.mjs';

async function fixture(t) {
  const repositoryRoot = await mkdtemp(path.join(os.tmpdir(), 'shadwick-agent-'));
  t.after(() => rm(repositoryRoot, {recursive:true, force:true}));
  const output = path.join(repositoryRoot, 'dist'), siteUrl = new URL('https://example.test/widgets/shadwick/');
  const write = async (file, content) => { const target = path.join(repositoryRoot, file); await mkdir(path.dirname(target), {recursive:true}); await writeFile(target, content); };
  await write('pom.xml', '<project><version>7.2-SNAPSHOT</version><properties><maven.compiler.release>17</maven.compiler.release><wicket.version>10.99.1</wicket.version></properties></project>');
  await write('README.md', 'Not yet on Maven Central.');
  await write('LICENSE', 'Fixture license'); await write('shadwick/LICENSE.txt', 'Fixture license');
  await write('docs/catalog.json', JSON.stringify([{slug:'components/sample', title:'Sample', group:'Components', description:'A sample component.'}]));
  await write('shadwick/SKILL.md', '---\nname: shadwick\ndescription: Build sample Wicket interfaces.\n---\n\n# Shadwick\n\n[Forms](references/forms.md)\n');
  await write('shadwick/agents/openai.yaml', 'interface:\n  display_name: "Shadwick"\n');
  await write('shadwick/references/forms.md', '[Template](../assets/QuickStartPage.java)\n\n[Guide](https://danielbartl.github.io/shadwick/docs/components/sample/index.md)\n');
  for (const name of skillExamples) {
    await write('docs/examples/' + name, 'Compiled fixture ' + name);
    await write('shadwick/assets/' + name, 'Compiled fixture ' + name);
  }
  await write('dist/docs/index.md', '[Sample](components/sample/index.md)\n');
  await write('dist/docs/components/sample/index.md', '# Sample\n\n[Self](index.md)\n');
  const args = {repositoryRoot, output, siteUrl, repository:'owner/project'};
  return {repositoryRoot, output, siteUrl, write, build:() => buildAgentResources(args), check:() => checkAgentResources(args)};
}

test('prose links can be rewritten without changing literal links inside code', () => {
  const source = '[Guide](./guide.md), [the `API`](./api.md) and `[literal](./not-a-guide.md)`\n\n```java\nString text = "[sample](./literal.md)";\n```\n';
  const seen = [];
  const changed = mapMarkdownLinks(source, url => { seen.push(url); return 'https://docs.test/guide/index.md'; });
  assert.deepEqual(seen, ['./guide.md', './api.md']);
  assert.equal(changed.slice(changed.indexOf('```')), source.slice(source.indexOf('```')));
  assert(changed.includes('`[literal](./not-a-guide.md)`'));
  assert.deepEqual(markdownLinks(source), ['./guide.md', './api.md']);
});

test('published skill and guide inventory preserve a configured project subpath and repository metadata', async t => {
  const f = await fixture(t); await f.build();
  const index = await readFile(path.join(f.output, 'llms.txt'), 'utf8');
  assert(index.includes('7.2-SNAPSHOT')); assert(index.includes('10.99.1'));
  const links = markdownLinks(index).filter(url => new URL(url).origin === f.siteUrl.origin);
  assert(links.length >= 2);
  for (const url of links) {
    assert(new URL(url).pathname.startsWith('/widgets/shadwick/'));
    await access(path.join(f.output, new URL(url).pathname.slice(f.siteUrl.pathname.length)));
  }
  assert.deepEqual(await f.check(), {guides:1, files:7});
  for (const name of skillExamples) assert.equal(await readFile(path.join(f.output, 'skills/shadwick/assets', name), 'utf8'), 'Compiled fixture ' + name);
});

test('skill templates cannot drift from the compiled documentation examples', async t => {
  const f = await fixture(t); await f.write('shadwick/assets/QuickStartPage.java', 'stale example');
  await assert.rejects(f.build(), /differs from the compiled documentation example/);
});

test('a skill cannot depend on a missing supporting file', async t => {
  const f = await fixture(t); await f.write('shadwick/SKILL.md', '# Skill\n\n[Missing](references/missing.md)\n');
  await assert.rejects(f.build(), {code:'ENOENT'});
});

test('relative skill references cannot escape the installed package', async t => {
  const f = await fixture(t); await f.write('shadwick/SKILL.md', '# Skill\n\n[Outside](../README.md)\n');
  await assert.rejects(f.build(), /leaves its package/);
});

test('a misspelled guide in the skill is rejected against the real guide inventory', async t => {
  const f = await fixture(t); await f.write('shadwick/references/forms.md', '[Guide](https://danielbartl.github.io/shadwick/docs/components/imaginary/index.md)\n');
  await assert.rejects(f.build(), /Unknown skill guide/);
});

test('published Markdown targets must exist and the index must cover every guide', async t => {
  const f = await fixture(t); await f.build();
  const file = path.join(f.output, 'llms.txt'), index = await readFile(file, 'utf8');
  await writeFile(file, index.split('\n').filter(line => !line.includes('/docs/components/sample/index.md')).join('\n'));
  await assert.rejects(f.check(), /inventory differs/);
  await writeFile(file, index);
  await rm(path.join(f.output, 'docs/components/sample/index.md'));
  await assert.rejects(f.check(), {code:'ENOENT'});
});

test('published copies must remain byte-identical to the portable skill', async t => {
  const f = await fixture(t); await f.build();
  await writeFile(path.join(f.output, 'skills/shadwick/assets/QuickStartPage.java'), 'unexpected modification');
  await assert.rejects(f.check(), /Published skill differs/);
});

import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtemp,mkdir,writeFile,readFile,rm} from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import {buildDocumentation} from './build.mjs';

async function fixture(t){
 const repositoryRoot=await mkdtemp(path.join(os.tmpdir(),'shadwick-doc-build-'));t.after(()=>rm(repositoryRoot,{recursive:true,force:true}));
 const output=path.join(repositoryRoot,'dist');
 const source='shadwick-core/src/main/java/dev/jbaby/shadwick/components/SwSample.java';
 const example='shadwick-examples/src/main/java/dev/jbaby/shadwick/examples/ExamplePage.java';
 const entry={slug:'components/sample',title:'Sample',group:'Components',description:'A useful component.',apis:[source],example:'ExamplePage'};
 const write=async(file,text)=>{await mkdir(path.dirname(path.join(repositoryRoot,file)),{recursive:true});await writeFile(path.join(repositoryRoot,file),text);};
 await write(source,'public class SwSample { /** Configure the label. */ public SwSample setLabel(String text) { return this; } }');
 await write(example,'public class ExamplePage {}');
 await write('docs/components/sample.md','# Sample\n\nA useful component.\n\n## Usage\n\n```java\nnew SwSample();\n```\n');
 for(const file of ['docs.css','docs.js','logo.svg','favicon.svg'])await write('dist/assets/'+file,'fixture');
 const save=async()=>write('docs/catalog.json',JSON.stringify([entry]));await save();
 const build=()=>buildDocumentation({repositoryRoot,output,siteUrl:new URL('https://example.test/widgets/shadwick/'),repository:'owner/project'});
 return {repositoryRoot,output,entry,source,write,save,build};
}
test('complete build preserves a project subpath, generates source APIs and writes the searchable inventory',async t=>{
 const f=await fixture(t),urls=await f.build();assert.deepEqual(urls,['https://example.test/widgets/shadwick/docs/','https://example.test/widgets/shadwick/docs/components/sample/']);
 const html=await readFile(path.join(f.output,'docs/components/sample/index.html'),'utf8');
 assert(html.includes('public SwSample setLabel(String text)'));assert(html.includes('Configure the label.'));
 assert(html.includes('../../../assets/docs.css?v='));assert(html.includes('aria-current="page"'));assert(html.includes('<details class="api-members">'));
 assert(!/\bhref="\//.test(html));assert(!/\bonclick=/.test(html));
 const search=JSON.parse(await readFile(path.join(f.output,'docs/search-index.json'),'utf8'));assert.equal(search.length,1);assert(search[0].keywords.includes('SwSample'));assert(search[0].body.includes('setLabel'));
});
test('new public Sw components fail coverage until a guide is registered',async t=>{
 const f=await fixture(t);await f.write(f.source.replace('SwSample','SwNew'),'public class SwNew {}');await assert.rejects(f.build(),/Missing component documentation.*SwNew/);
});
test('unknown guide links fail before publication',async t=>{
 const f=await fixture(t);await f.write('docs/components/sample.md','# Sample\n\n[Missing](./missing.md)\n');await assert.rejects(f.build(),/Unknown Markdown guide/);
});
test('printed examples cannot drift from compiled files',async t=>{
 const f=await fixture(t);await f.write('docs/examples/Sample.java','public class Sample {}\n');
 await f.write('docs/components/sample.md','# Sample\n\n<!-- snippet: docs/examples/Sample.java -->\n```java\nwrong\n```\n');await assert.rejects(f.build(),/Stale documentation snippet/);
});
test('API source paths reject traversal',async t=>{
 const f=await fixture(t);f.entry.apis.push('shadwick-core/src/main/java/../../secret.java');await f.save();await assert.rejects(f.build(),/Invalid API source/);
});
test('example class paths reject traversal',async t=>{
 const f=await fixture(t);f.entry.example='../../secret';await f.save();await assert.rejects(f.build(),/Invalid example class/);
});
test('duplicate routes fail instead of overwriting pages',async t=>{
 const f=await fixture(t);await f.write('docs/catalog.json',JSON.stringify([f.entry,f.entry]));await assert.rejects(f.build(),/Duplicate documentation route/);
});
test('raw script blocks cannot enter a generated guide',async t=>{
 const f=await fixture(t);await f.write('docs/components/sample.md','# Sample\n\n<script>alert(1)</script>\n');await assert.rejects(f.build(),/Unsupported Markdown block/);
});
test('Markdown companions contain the current API and discoverable links without HTML navigation',async t=>{
 const f=await fixture(t);await f.write('docs/components/sample.md','# Sample\n\n[Usage](sample.md#usage)\n\n## Usage\n\n```html\n<div wicket:id="sample"></div>\n```\n');
 await f.build();
 const machine=await readFile(path.join(f.output,'docs/components/sample/index.md'),'utf8');
 assert(machine.includes('https://example.test/widgets/shadwick/docs/components/sample/index.md#usage'));
 assert(machine.includes('public SwSample setLabel(String text)'));
 assert(machine.includes('```html\n<div wicket:id="sample"></div>\n```'));
 assert(!machine.includes('<nav'));
 const html=await readFile(path.join(f.output,'docs/components/sample/index.html'),'utf8');
 assert(html.includes('rel="alternate" type="text/markdown" href="./index.md"'));
 assert(html.includes('rel="describedby" href="../../../llms.txt"'));
 await f.write(f.source,'public class SwSample { public SwSample withLabel(String text) { return this; } }');
 await f.build();
 const updated=await readFile(path.join(f.output,'docs/components/sample/index.md'),'utf8');
 assert(updated.includes('withLabel(String text)'));assert(!updated.includes('setLabel(String text)'));
});

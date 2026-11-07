import {access,readFile,readdir,stat} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
import {skins} from './skins.mjs';
import {slug} from './docs/markdown.mjs';
import {checkAgentResources} from './agents.mjs';

const project=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const root=path.join(project,'site/dist');
const html=await readFile(path.join(root,'index.html'),'utf8');
const cache=new Map(),htmlFiles=[],sourceIds=new Map();
let references=0,bytes=0;
async function contents(file){if(!cache.has(file))cache.set(file,await readFile(file));return cache.get(file);}
async function walk(directory){for(const entry of await readdir(directory,{withFileTypes:true})){const full=path.join(directory,entry.name);if(entry.isDirectory())await walk(full);else{bytes+=(await stat(full)).size;if(entry.name.endsWith('.html'))htmlFiles.push(full);}}}
await walk(root);
const pageIds=new Map();
for(const file of htmlFiles){
  const content=(await contents(file)).toString('utf8');
  const ids=[...content.matchAll(/\bid="([^"]+)"/g)].map(m=>m[1]);
  if(new Set(ids).size!==ids.length)throw new Error('Duplicate HTML IDs: '+path.relative(root,file));
  if(/__[A-Z_]+__/.test(content))throw new Error('Unresolved build placeholder: '+file);
  pageIds.set(file,new Set(ids));
}
async function checkAsset(reference,current='index.html',requireVersion=true){
  if(reference.startsWith('/'))throw new Error('Root-relative link breaks project Pages: '+reference);
  const resolved=new URL(reference,'https://check.invalid/'+current);
  let filename=decodeURIComponent(resolved.pathname).slice(1);
  if(filename.endsWith('/')||!filename)filename+='index.html';
  const file=path.resolve(root,filename);
  if(!file.startsWith(root+path.sep))throw new Error('Asset outside website: '+reference);
  const content=await contents(file);
  const fragment=decodeURIComponent(resolved.hash.slice(1));
  if(fragment){
    const valid=pageIds.has(file)?pageIds.get(file).has(fragment):content.toString('utf8').includes(`id="${fragment}"`);
    if(!valid)throw new Error(`Missing fragment ${reference} from ${current}`);
  }
  if(requireVersion&&/\.(?:css|js|svg|png|json)$/.test(filename)){
    const expected=createHash('sha256').update(content).digest('hex').slice(0,12);
    if(resolved.searchParams.get('v')!==expected)throw new Error('Missing or stale content version: '+reference+' from '+current);
  }
}
for(const file of htmlFiles){
  const current=path.relative(root,file).split(path.sep).join('/');
  const content=(await contents(file)).toString('utf8');
  for(const [,reference]of content.matchAll(/\b(?:src|href|data-search-index)="([^"]+)"/g)){
    if(/^(?:https?:|mailto:)/.test(reference)||reference==='#')continue;
    await checkAsset(reference,current);references++;
  }
}
for(const cssFile of ['assets/site.css','assets/docs.css',...skins.map(s=>`assets/shadwick-${s}.css`)]){
  const stylesheet=(await contents(path.join(root,cssFile))).toString('utf8');
  for(const [,reference]of stylesheet.matchAll(/url\(['"]?([^)'"\s]+)['"]?\)/g)){
    const file=path.resolve(root,path.dirname(cssFile),reference);if(!file.startsWith(root+path.sep))throw new Error('CSS asset outside website');await access(file);
  }
}
const offered=new Set();
for(const [option,skin]of html.matchAll(/<option value="([a-z]+)"[^>]*>/g)){
  if(!skins.includes(skin))continue;const stylesheet=/data-stylesheet="([^"]+)"/.exec(option)?.[1];
  if(!stylesheet?.startsWith(`shadwick-${skin}.css?`))throw new Error('Missing versioned skin: '+skin);
  await checkAsset('./assets/'+stylesheet);offered.add(skin);references++;
}
if(offered.size!==skins.length)throw new Error('Expected every skin option');
for(const [,reference]of (await contents(path.join(root,'assets/table-demo.js'))).toString('utf8').matchAll(/href="([^"]+)"/g))await checkAsset(reference);
for(const file of ['LICENSE.txt','NOTICE.txt','assets/fonts/OFL.txt','fonts/balsamiq-sans/OFL.txt'])await access(path.join(root,file));
const catalog=JSON.parse(await readFile(path.join(project,'docs/catalog.json'),'utf8'));
const canonical=new URL(/<link rel="canonical" href="([^"]+)"/.exec(html)?.[1] || '');
await checkAgentResources({repositoryRoot:project,output:root,siteUrl:canonical});
const search=JSON.parse(await contents(path.join(root,'docs/search-index.json')));
if(search.length!==catalog.length||new Set(search.map(e=>e.href)).size!==search.length)throw new Error('Search index does not match guide inventory');
for(const entry of search)await checkAsset(entry.href,'docs/index.html',false);
// Markdown also remains usable directly on GitHub, including the README's compatibility anchors.
const markdownFiles=['README.md','docs/README.md','docs/CONTRIBUTING.md',...catalog.map(e=>'docs/'+e.slug+'.md')];
for(const source of markdownFiles){
  const raw=await readFile(path.join(project,source),'utf8');const prose=raw.replace(/```[\s\S]*?```/g,'');
  for(const [,reference]of prose.matchAll(/\[[^\]]+\]\(([^\s)]+)\)/g)){
    if(/^(?:https?:|mailto:)/.test(reference))continue;
    const [filename,fragment]=reference.split('#');const file=path.resolve(project,path.dirname(source),filename||path.basename(source));
    if(!file.startsWith(project+path.sep))throw new Error('Markdown link leaves repository');await access(file);
    if(fragment){
      if(!sourceIds.has(file)){
        const target=(await readFile(file,'utf8')).replace(/```[\s\S]*?```/g,'');
        sourceIds.set(file,new Set([...target.matchAll(/^#{1,6}\s+(.+)$/gm)].map(m=>slug(m[1]))));
      }
      if(!sourceIds.get(file).has(fragment))throw new Error(`Missing Markdown fragment ${reference} from ${source}`);
    }
  }
}
console.log(`Site checks passed: ${htmlFiles.length} HTML pages, ${references} internal references, ${[...pageIds.values()].reduce((n,ids)=>n+ids.size,0)} IDs, ${(bytes/1024).toFixed(0)} KB total; Markdown and search links verified.`);

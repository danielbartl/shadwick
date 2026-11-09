import {readFile,writeFile,mkdir,readdir} from 'node:fs/promises';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {escapeHtml as esc,renderMarkdown,plainText,slug} from './markdown.mjs';
import {javaApi} from './java-api.mjs';
import {guideMarkdown} from './agent-markdown.mjs';

export async function buildDocumentation({repositoryRoot,output,siteUrl,repository}) {
  const catalog=JSON.parse(await readFile(path.join(repositoryRoot,'docs/catalog.json'),'utf8'));
  const manifest=new Map(catalog.map(entry=>[path.resolve(repositoryRoot,'docs',entry.slug+'.md'),entry]));
  if(manifest.size!==catalog.length)throw new Error('Duplicate documentation route');
  const groups=[...new Set(catalog.map(p=>p.group))],apiCache=new Map(),pages=[];
  const sourceBase=`https://github.com/${repository}/blob/main/`;
  const version=async asset=>createHash('sha256').update(await readFile(path.join(output,asset))).digest('hex').slice(0,12);
  const assetHashes=new Map();
  for(const asset of ['assets/docs.css','assets/docs.js','assets/logo.svg','assets/favicon.svg'])assetHashes.set(asset,await version(asset));
  function urlFrom(route,target){let relative=path.posix.relative(route,target);if(!relative)relative='.';if(!relative.startsWith('.'))relative='./'+relative;return relative+(target.endsWith('/')&&!relative.endsWith('/')?'/':'');}
  function internal(route,target){return urlFrom(route,target);}
  function asset(route,target){return internal(route,target)+'?v='+assetHashes.get(target);}
  function resolveLink(entry,route,reference) {
    if(/^(?:https?:|mailto:|#)/.test(reference))return reference;
    if(reference.startsWith('/')||reference.includes('\\'))throw new Error(`Project Pages links must be relative: ${reference}`);
    const [filename,fragment]=reference.split('#');
    if(filename.endsWith('.md')){
      const resolved=path.resolve(repositoryRoot,'docs',path.dirname(entry.slug),filename);
      const target=manifest.get(resolved);
      if(!target)throw new Error(`Unknown Markdown guide ${reference} in ${entry.slug}`);
      return internal(route,'docs/'+target.slug+'/')+(fragment?'#'+fragment:'');
    }
    throw new Error(`Documentation links must name a guide, anchor or explicit external URL: ${reference}`);
  }
  async function apiFor(file){
    if(!apiCache.has(file)){
      if(!/^shadwick-core\/src\/main\/java\/[\w/]+\.java$/.test(file))throw new Error('Invalid API source path: '+file);
      apiCache.set(file,javaApi(await readFile(path.join(repositoryRoot,file),'utf8')));
    }return apiCache.get(file);
  }
  // Each public Sw component must have a guide, including table columns and form controls.
  async function componentFiles(dir){let files=[];for(const e of await readdir(dir,{withFileTypes:true})){const file=path.join(dir,e.name);if(e.isDirectory())files.push(...await componentFiles(file));else if(/^Sw.+\.java$/.test(e.name))files.push(path.relative(repositoryRoot,file).split(path.sep).join('/'));}return files;}
  const covered=new Set(catalog.flatMap(entry=>entry.apis));
  const uncovered=(await componentFiles(path.join(repositoryRoot,'shadwick-core/src/main/java/dev/jbaby/shadwick/components'))).filter(file=>!covered.has(file));
  if(uncovered.length)throw new Error('Missing component documentation: '+uncovered.join(', '));
  for(const entry of catalog) {
    if(!/^(?:guides|components)\/[a-z\d-]+$/.test(entry.slug))throw new Error('Invalid documentation slug: '+entry.slug);
    const markdown=await readFile(path.resolve(repositoryRoot,'docs',entry.slug+'.md'),'utf8');
    if(!markdown.startsWith('# '+entry.title+'\n'))throw new Error('Guide title does not match catalog: '+entry.slug);
    // Full examples in prose must stay byte-for-byte synchronized with their compiled source.
    for(const [,file,language,code]of markdown.matchAll(/<!-- snippet: ([\w./-]+) -->\n```(\w+)\n([\s\S]*?)\n```/g)){
      if(!/^docs\/examples\/[\w/-]+\.(?:java|html)$/.test(file)||file.split('/').includes('..'))throw new Error('Invalid snippet source: '+file);
      const source=await readFile(path.join(repositoryRoot,file),'utf8');if(source.trimEnd()!==code)throw new Error('Stale documentation snippet: '+file);
      if(!['java','html'].includes(language))throw new Error('Unsupported complete-example language');
    }
    const route='docs/'+entry.slug+'/';const rendered=renderMarkdown(markdown.replace(`# ${entry.title}\n\n${entry.description}\n\n`, `# ${entry.title}\n\n`),{omitTitle:true,resolveLink:ref=>resolveLink(entry,route,ref)});
    const apis=await Promise.all(entry.apis.map(apiFor));
    const headings=[...rendered.headings];let apiHtml='';
    if(apis.length){
      const id='api-reference';if(headings.some(h=>h.id===id))throw new Error('Reserved generated API heading in '+entry.slug);
      headings.push({level:2,id,text:'API reference'});apiHtml='<h2 id="api-reference" tabindex="-1">API reference<a class="heading-anchor" href="#api-reference" aria-label="Link to API reference">#</a></h2><p>Signatures and descriptions below come from the current Java source. Inherited Wicket methods remain available; related base classes and behaviors are included where useful.</p>';
      for(let index=0;index<apis.length;index++){
        const api=apis[index],id='api-'+api.name.toLowerCase();headings.push({level:3,id,text:api.name});
        apiHtml+=`<section class="api-class" aria-labelledby="${id}"><h3 id="${id}" tabindex="-1"><code>${esc(api.name)}</code></h3><p class="api-source"><a href="${sourceBase+entry.apis[index]}">Java source</a> <span>· ${api.members.length} declared members</span></p>`;
        apiHtml+=`<p class="api-declaration"><code>${esc(api.declaration)}</code></p>`;
        if(!api.members.length){apiHtml+='<p class="api-source">This type declares no additional configuration methods.</p></section>';continue;}
        apiHtml+=`<details class="api-members"><summary>Constructors, methods and hooks<span aria-hidden="true">+</span></summary><dl>`;
        for(const m of api.members)apiHtml+=`<div><dt><span class="api-kind">${m.kind}</span><code>${esc(m.signature)}</code></dt>${m.description?`<dd>${esc(m.description)}</dd>`:''}</div>`;
        apiHtml+='</dl></details></section>';
      }
    }
    if(!/^[A-Za-z]\w*$/.test(entry.example))throw new Error('Invalid example class');
    const example='shadwick-examples/src/main/java/dev/jbaby/shadwick/examples/'+entry.example+'.java';
    await readFile(path.join(repositoryRoot,example));
    const machineMarkdown=guideMarkdown({entry,markdown,apis,repository,resolveLink:reference=>{
      if(/^(?:https?:|mailto:|#)/.test(reference))return reference;
      const [filename,fragment]=reference.split('#');
      const target=manifest.get(path.resolve(repositoryRoot,'docs',path.dirname(entry.slug),filename));
      if(!target)throw new Error(`Unknown Markdown guide ${reference} in ${entry.slug}`);
      return new URL('docs/'+target.slug+'/index.md',siteUrl).href+(fragment?'#'+fragment:'');
    }});
    pages.push({...entry,route,markdown,machineMarkdown,headings,body:rendered.html+apiHtml+`<section class="example-source"><h2 id="example-source" tabindex="-1">Example source</h2><p>Read <a href="${sourceBase+example}">${esc(entry.example)}</a> for an application example. Run the examples locally to try the actual Wicket behavior; the <a href="${internal(route,'')}#components">landing studio</a> is a browser-only preview.</p></section>`,apiNames:apis.map(a=>a.name),apiSearch:apis.flatMap(a=>a.members.map(m=>m.signature)).join(' ')});
  }
  const indexRoute='docs/';
  const index={title:'Component documentation',description:'Build with Shadwick. Practical Wicket guides, complete examples and API references for every component.',group:'Documentation',route:indexRoute,slug:'',headings:[],apiNames:[]};
  index.machineMarkdown='# Shadwick documentation\n\n'+index.description+'\n';
  for(const group of groups)index.machineMarkdown+='\n## '+group+'\n\n'+pages.filter(p=>p.group===group).map(p=>`- [${p.title}](${new URL(p.route+'index.md',siteUrl).href}): ${p.description}`).join('\n')+'\n';
  index.body=`<section class="docs-start" aria-labelledby="start-title"><div><p class="eyebrow">Start with Wicket</p><h2 id="start-title">Your markup. Your models.<br>A familiar place to build.</h2><p>Install one library dependency, choose a skin, and bind components to Wicket models. Use these guides for the Java API, matching markup and integration details.</p><a class="docs-primary" href="${internal(indexRoute,'docs/guides/installation/')}">Install Shadwick <span aria-hidden="true">→</span></a></div><div class="docs-start-note"><span class="docs-chip">Java 21+</span><span class="docs-chip">Wicket 11</span><p>Nine skins. Nine message languages.<br>Standalone and embedded styles.</p><p class="docs-release">0.1.0 · install locally<br>Maven Central release is pending.</p></div></section>`;
  for(const group of groups){const id='group-'+slug(group);index.headings.push({level:2,id,text:group});index.body+=`<section class="docs-catalog" aria-labelledby="${id}"><h2 id="${id}">${esc(group)}</h2>${group==='Display'?`<p class="docs-related">Measurements and ratings: <a href="${internal(indexRoute,'docs/components/progress/')}">Progress</a>, <a href="${internal(indexRoute,'docs/components/meters/')}">Meters</a> and <a href="${internal(indexRoute,'docs/components/ratings/')}">Ratings</a>.</p>`:''}<div class="docs-card-grid">`+pages.filter(p=>p.group===group).map(p=>`<a class="docs-guide-card" href="${internal(indexRoute,p.route)}"><strong>${esc(p.title)} <span aria-hidden="true">↗</span></strong><span>${esc(p.description)}</span></a>`).join('')+'</div></section>';}
  const search=pages.map(p=>({title:p.title,description:p.description,group:p.group,href:p.slug+'/',keywords:p.apiNames.join(' '),body:plainText(p.markdown.replace(/```[\s\S]*?```/g,' ')).replace(/\s+/g,' ').slice(0,18000)+' '+p.apiSearch}));
  await mkdir(path.join(output,'docs'),{recursive:true});
  const searchJson=JSON.stringify(search);const searchHash=createHash('sha256').update(searchJson).digest('hex').slice(0,12);
  await writeFile(path.join(output,'docs/search-index.json'),searchJson);
  function layout(page) {
    const route=page.route,at=pages.indexOf(page),previous=at>0?pages[at-1]:null,next=at>=0?pages[at+1]:pages[0];
    const nav=groups.map(group=>`<section><h2>${esc(group)}</h2><ul>`+pages.filter(p=>p.group===group).map(p=>`<li><a href="${internal(route,p.route)}"${p.slug===page.slug?' aria-current="page"':''}>${esc(p.title)}</a></li>`).join('')+'</ul></section>').join('');
    const toc=page.headings.filter(h=>h.level<=3).map(h=>`<li${h.level===3?' class="toc-subsection"':''}><a href="#${h.id}">${esc(h.text)}</a></li>`).join('');
    const canonical=new URL(route,siteUrl).href;
    return `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="description" content="${esc(page.description)}"><meta name="color-scheme" content="light dark"><link rel="canonical" href="${canonical}"><link rel="alternate" type="text/markdown" href="./index.md"><link rel="describedby" href="${internal(route,'llms.txt')}"><link rel="icon" type="image/svg+xml" href="${asset(route,'assets/favicon.svg')}"><link rel="stylesheet" href="${asset(route,'assets/docs.css')}"><script src="${asset(route,'assets/docs.js')}" defer></script><title>${esc(page.title)} · Shadwick docs</title></head><body>
<a class="docs-skip" href="#content">Skip to content</a><header class="docs-header"><div class="docs-header-inner"><a class="docs-brand" href="${internal(route,'')}"><img src="${asset(route,'assets/logo.svg')}" width="30" height="30" alt=""><span>shadwick<span class="brand-period">.</span></span></a><span class="docs-header-rule" aria-hidden="true"></span><a class="docs-header-home" href="${internal(route,'docs/')}">Documentation</a><nav aria-label="Main navigation"><a href="${internal(route,'')}#components">Component studio</a><a href="https://github.com/${repository}">GitHub <span aria-hidden="true">↗</span></a></nav></div></header>
<div class="docs-layout"><aside class="docs-navigation"><details class="docs-nav"><summary>Browse documentation<span aria-hidden="true">⌄</span></summary><div class="docs-nav-content"><form role="search" aria-label="Documentation" class="docs-search" data-search-base="${internal(route,'docs/')}" data-search-index="${internal(route,'docs/search-index.json')}?v=${searchHash}" hidden><label for="docs-query">Search documentation</label><div class="docs-search-input"><input type="search" id="docs-query" name="q" aria-keyshortcuts="Control+K Meta+K" placeholder="Search guides and APIs…" autocomplete="off" spellcheck="false"><kbd aria-hidden="true">⌘ K</kbd></div></form><p class="docs-search-status" role="status" id="docs-search-status" hidden></p><nav class="docs-search-results" aria-label="Search results" hidden><ul></ul></nav><nav class="docs-sections" aria-label="Documentation"><a class="docs-overview" href="${internal(route,'docs/')}"${page===index?' aria-current="page"':''}>Overview <span>${pages.length} guides</span></a>${nav}</nav></div></details></aside>
<main id="content" tabindex="-1" class="docs-article"><header class="docs-article-header"><p class="eyebrow">${esc(page.group)}</p><h1>${esc(page.title)}</h1><p class="docs-lead">${esc(page.description)}</p>${page.apiNames.length?`<div class="docs-api-tags">${page.apiNames.slice(0,4).map(name=>`<code>${name}</code>`).join('')}</div>`:''}</header><div class="docs-prose">${page.body}</div><nav class="docs-page-navigation" aria-label="Previous and next guide">${previous?`<a href="${internal(route,previous.route)}"><span>Previous</span><strong>← ${esc(previous.title)}</strong></a>`:'<span></span>'}${next?`<a href="${internal(route,next.route)}"><span>Next</span><strong>${esc(next.title)} →</strong></a>`:''}</nav><footer class="docs-footer"><span>Shadwick · Apache 2.0</span>${page.slug?`<a href="https://github.com/${repository}/edit/main/docs/${page.slug}.md">Edit this guide ↗</a>`:`<a href="https://github.com/${repository}/tree/main/docs">Documentation source ↗</a>`}</footer></main>
<aside class="docs-on-this-page"><details class="docs-toc"><summary>On this page<span aria-hidden="true">⌄</span></summary><nav aria-label="On this page"><ol>${toc}</ol></nav></details></aside></div><span class="sr-only" id="docs-copy-status" role="status"></span></body></html>`;
  }
  for(const page of [index,...pages]){const directory=path.join(output,page.route);await mkdir(directory,{recursive:true});await writeFile(path.join(directory,'index.html'),layout(page));await writeFile(path.join(directory,'index.md'),page.machineMarkdown);}
  console.log(`Documentation built: ${pages.length} guides, ${apiCache.size} API types, every public Sw component covered.`);
  return [index,...pages].map(p=>new URL(p.route,siteUrl).href);
}

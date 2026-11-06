(() => {
  'use strict';
  const navigation=document.querySelector('.docs-nav');
  const toc=document.querySelector('.docs-toc');
  const desktop=matchMedia('(min-width: 761px)');
  const wide=matchMedia('(min-width: 1100px)');
  const resize=()=>{navigation.open=desktop.matches;toc.open=wide.matches;};
  resize();desktop.addEventListener('change',resize);wide.addEventListener('change',resize);
  const form=document.querySelector('[data-search-index]');form.hidden=false;
  const input=form.querySelector('input');
  form.querySelector('kbd').textContent=/Mac|iPhone|iPad/.test(navigator.userAgentData?.platform||navigator.platform)?'⌘ K':'Ctrl K';
  const status=document.querySelector('#docs-search-status');
  const sections=document.querySelector('.docs-sections');
  const results=document.querySelector('.docs-search-results');
  let indexPromise,request=0,timer;
  const normalize=value=>value.normalize('NFKD').replace(/\p{M}/gu,'').toLowerCase();
  const loadIndex=()=>indexPromise||(indexPromise=fetch(new URL(form.dataset.searchIndex,document.baseURI)).then(response=>{
    if(!response.ok)throw new Error('Documentation index unavailable');return response.json();
  }).catch(error=>{indexPromise=null;throw error;}));
  async function search(){
    const current=++request;const query=input.value.trim();
    if(!query){sections.hidden=false;results.hidden=true;status.hidden=true;return;}
    status.hidden=false;status.textContent='Searching…';
    try{
      const index=await loadIndex();if(request!==current)return;
      const terms=normalize(query).split(/\s+/).filter(Boolean);
      const matches=index.map(entry=>{
        const title=normalize(entry.title),keywords=normalize(entry.keywords),all=normalize(`${entry.title} ${entry.description} ${entry.keywords} ${entry.body}`);
        return {...entry,score:terms.every(term=>all.includes(term))?terms.reduce((score,term)=>score+(title.includes(term)?100:keywords.includes(term)?50:1),0):0};
      }).filter(entry=>entry.score>0).sort((a,b)=>b.score-a.score||a.title.localeCompare(b.title));
      const list=results.querySelector('ul');list.replaceChildren();
      const base=new URL(form.dataset.searchBase,document.baseURI);
      for(const match of matches){
        const item=document.createElement('li'),link=document.createElement('a'),title=document.createElement('strong'),group=document.createElement('small');
        link.href=new URL(match.href,base).href;title.textContent=match.title;group.textContent=match.group;
        if(new URL(link.href).pathname===location.pathname)link.setAttribute('aria-current','page');
        link.append(title,group);item.append(link);list.append(item);
      }
      sections.hidden=true;results.hidden=false;
      status.textContent=matches.length?`${matches.length} ${matches.length===1?'guide':'guides'} found`:'No matching guides. Try a component name, such as SwSelect.';
    }catch(error){if(request!==current)return;status.textContent='Search is unavailable. Browse the guides below.';sections.hidden=false;results.hidden=true;}
  }
  input.addEventListener('input',()=>{clearTimeout(timer);request++;if(!input.value.trim()){search();return;}results.hidden=true;status.hidden=false;status.textContent='Searching…';timer=setTimeout(search,140);});
  input.addEventListener('keydown',event=>{
    if(event.key==='Escape'){event.preventDefault();clearTimeout(timer);input.value='';search();}
    if(event.key==='ArrowDown'&&!results.hidden){const first=results.querySelector('a');if(first){event.preventDefault();first.focus();}}
  });
  form.addEventListener('submit',async event=>{event.preventDefault();clearTimeout(timer);await search();if(!results.hidden)results.querySelector('a')?.click();});
  document.addEventListener('keydown',event=>{
    if((event.metaKey||event.ctrlKey)&&event.key.toLowerCase()==='k'&&!event.altKey){event.preventDefault();navigation.open=true;input.focus();input.select();}
  });
  const initial=new URL(location.href).searchParams.get('q');
  if(initial){input.value=initial;navigation.open=true;search();}
  for(const button of document.querySelectorAll('[data-copy-code]')){
    button.hidden=false;
    const originalName=button.getAttribute('aria-label');
    button.addEventListener('click',async()=>{
      const code=document.getElementById(button.dataset.copyCode);const text=code.textContent;
      try{
        if(!navigator.clipboard?.writeText)throw new Error('Clipboard unavailable');
        await navigator.clipboard.writeText(text);button.textContent='Copied';button.setAttribute('aria-label',originalName.replace(/^Copy/,'Copied'));
        document.querySelector('#docs-copy-status').textContent='Example copied to clipboard.';
      }catch(error){
        const range=document.createRange();range.selectNodeContents(code);const selection=getSelection();selection.removeAllRanges();selection.addRange(range);
        document.querySelector('#docs-copy-status').textContent='Copy is unavailable. The example is selected; use your browser copy command.';button.textContent='Selected';button.setAttribute('aria-label',originalName.replace(/^Copy/,'Selected'));
      }
      clearTimeout(button._reset);button._reset=setTimeout(()=>{button.textContent='Copy';button.setAttribute('aria-label',originalName);},2000);
    });
  }
  if('IntersectionObserver'in window){
    const links=[...document.querySelectorAll('.docs-toc a')];
    const observer=new IntersectionObserver(entries=>{
      for(const entry of entries){if(!entry.isIntersecting)continue;for(const link of links){if(link.hash==='#'+entry.target.id)link.setAttribute('aria-current','location');else link.removeAttribute('aria-current');}}
    },{rootMargin:'-90px 0px -65% 0px'});
    for(const heading of document.querySelectorAll('.docs-prose h2[id],.docs-prose h3[id]'))observer.observe(heading);
  }
})();

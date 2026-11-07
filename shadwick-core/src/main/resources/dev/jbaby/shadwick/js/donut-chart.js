/* Native donut interaction; all values come from the server-rendered legend. See NOTICE. */
(function(){
'use strict';
const states=new Map(),Shadwick=window.Shadwick=window.Shadwick||{};
function scan(){
  states.forEach((state,root)=>{if(!root.isConnected||root.querySelector('.sw-donut-plot')!==state.plot){state.destroy();states.delete(root);}});
  document.querySelectorAll('.sw-donut-chart').forEach(root=>{
    if(states.has(root))return;
    const plot=root.querySelector('.sw-donut-plot'),tooltip=plot?.querySelector('.sw-chart-tooltip-content');
    if(!plot||!tooltip||plot.dataset.tooltip==='false')return;
    const segments=Array.from(plot.querySelectorAll('.sw-donut-segment')),parts=Array.from(root.querySelectorAll('.sw-donut-legend-item'));
    if(!parts.length||parts.length!==segments.length||parts.some(part=>!part.querySelector('.sw-donut-name')||!part.querySelector('.sw-donut-value')||!part.querySelector('.sw-donut-share')))return;
    const cleanups=[],listen=(node,type,handler)=>{node.addEventListener(type,handler);cleanups.push(()=>node.removeEventListener(type,handler));};
    let active=-1;
    plot.querySelectorAll('[data-donut-status]').forEach(node=>node.remove());
    const status=document.createElement('span');status.className='sw-chart-sr-only';status.dataset.donutStatus='';status.setAttribute('role','status');status.setAttribute('aria-atomic','true');plot.append(status);
    const titles=segments.map(segment=>({segment,title:segment.querySelector('title')}));titles.forEach(({title})=>title?.remove());
    const hide=()=>{tooltip.hidden=true;[...segments,...parts].forEach(node=>node.removeAttribute('data-active'));};
    const show=index=>{
      active=Math.max(0,Math.min(parts.length-1,index));
      const part=parts[active],name=part.querySelector('.sw-donut-name').textContent,value=part.querySelector('.sw-donut-value').textContent,share=part.querySelector('.sw-donut-share').textContent;
      const label=document.createElement('div');label.className='sw-chart-tooltip-label';label.textContent=name;
      const row=document.createElement('div');row.className='sw-chart-tooltip-item '+[...part.classList].find(name=>name.startsWith('sw-chart-series-'));
      const indicator=document.createElement('span');indicator.className='sw-chart-tooltip-indicator';indicator.setAttribute('aria-hidden','true');
      const number=document.createElement('span');number.className='sw-chart-tooltip-value';number.textContent=value+' ('+share+')';
      row.append(indicator,number);tooltip.replaceChildren(label,row);tooltip.hidden=false;
      segments.forEach((node,i)=>node.toggleAttribute('data-active',i===active));parts.forEach((node,i)=>node.toggleAttribute('data-active',i===active));
      if(document.activeElement===plot)status.textContent=name+': '+value+' ('+share+')';
    };
    plot.tabIndex=0;hide();
    listen(plot,'focus',()=>show(active<0?0:active));listen(plot,'blur',hide);
    const hit=event=>{const node=event.target.closest?.('.sw-donut-segment,.sw-donut-legend-item');return node&&root.contains(node)?node:null;};
    listen(root,'pointermove',event=>{const node=hit(event);if(node&&(Number(node.dataset.index)!==active||tooltip.hidden))show(Number(node.dataset.index));});
    listen(root,'pointerdown',event=>{const node=hit(event);if(node&&event.button===0){event.preventDefault();show(Number(node.dataset.index));plot.focus({preventScroll:true});}});
    listen(root,'pointerleave',()=>{if(document.activeElement!==plot)hide();});
    listen(plot,'keydown',event=>{
      if(event.altKey||event.ctrlKey||event.metaKey)return;
      const step=getComputedStyle(plot).direction==='rtl'?-1:1;
      switch(event.key){
        case 'ArrowDown':show(active+1);break;case 'ArrowUp':show(active-1);break;
        case 'ArrowRight':show(active+step);break;case 'ArrowLeft':show(active-step);break;
        case 'Home':show(0);break;case 'End':show(parts.length-1);break;
        case 'Escape':if(tooltip.hidden)return;hide();break;default:return;
      }
      event.preventDefault();event.stopPropagation();
    });
    states.set(root,{plot,destroy(){cleanups.forEach(off=>off());status.remove();hide();titles.forEach(({segment,title})=>{if(title&&segment.isConnected)segment.append(title);});plot.removeAttribute('tabindex');}});
  });
}
Shadwick.enhanceDonutCharts=scan;
new MutationObserver(records=>{if(records.some(record=>record.type==='childList'))scan();}).observe(document.documentElement,{childList:true,subtree:true});
window.addEventListener('pageshow',scan);scan();
})();

/* Shadwick's native SVG interaction. Structure/skin patterns adapted from Shadleaf; see NOTICE. */
(function(){
'use strict';
const states=new Map();
const Shadwick=window.Shadwick=window.Shadwick||{};
function scan(){
  states.forEach((state,root)=>{if(!root.isConnected||root.querySelector('.sw-chart-plot')!==state.plot){state.destroy();states.delete(root);}});
  document.querySelectorAll('.sw-chart').forEach(root=>{
    if(states.has(root))return;
    const plot=root.querySelector('.sw-chart-plot'),table=root.querySelector('.sw-chart-data table');
    if(!plot||!table||plot.dataset.tooltip==='false')return;
    const tooltip=plot.querySelector('.sw-chart-tooltip-content');
    const rows=Array.from(table.tBodies[0]?.rows||[]),headers=Array.from(table.tHead.rows[0].cells).slice(1);
    const bands=Array.from(plot.querySelectorAll('.sw-chart-hit'));
    if(!rows.length||!tooltip||rows.length!==bands.length)return;
    const cleanups=[];
    const listen=(node,type,handler)=>{node.addEventListener(type,handler);cleanups.push(()=>node.removeEventListener(type,handler));};
    let active=-1;
    plot.querySelectorAll(':scope > [data-chart-status]').forEach(node=>node.remove());
    plot.tabIndex=0;
    const hide=()=>{tooltip.hidden=true;bands.forEach(band=>band.removeAttribute('data-active'));};
    hide();
    const titles=bands.map(band=>({band,title:band.querySelector('title')}));
    titles.forEach(({title})=>title?.remove());
    const show=index=>{
      active=Math.max(0,Math.min(rows.length-1,index));
      tooltip.replaceChildren();
      const row=rows[active];
      const label=document.createElement('div');label.className='sw-chart-tooltip-label';label.textContent=row.cells[0].textContent;tooltip.append(label);
      const items=document.createElement('div');items.className='sw-chart-tooltip-items';tooltip.append(items);
      headers.forEach((header,s)=>{
        const value=row.cells[s+1].textContent;if(value==='')return;
        const item=document.createElement('div');item.className='sw-chart-tooltip-item '+header.dataset.color;
        const indicator=document.createElement('span');indicator.className='sw-chart-tooltip-indicator';indicator.setAttribute('aria-hidden','true');
        if(header.dataset.dashed)indicator.dataset.dashed='true';
        const name=document.createElement('span');name.className='sw-chart-tooltip-name';name.textContent=header.textContent;
        const number=document.createElement('span');number.className='sw-chart-tooltip-value';number.textContent=value;
        item.append(indicator,name,number);items.append(item);
      });
      tooltip.hidden=false;
      bands.forEach((band,i)=>band.toggleAttribute('data-active',i===active));
      if(document.activeElement===plot)status.textContent=[row.cells[0].textContent,...headers.map((header,s)=>header.textContent+': '+row.cells[s+1].textContent)].join('; ');
    };
    const status=document.createElement('span');status.className='sw-chart-sr-only';status.setAttribute('data-chart-status','');status.setAttribute('role','status');status.setAttribute('aria-atomic','true');plot.append(status);
    listen(plot,'focus',()=>show(active<0?0:active));
    listen(plot,'blur',hide);
    listen(plot,'pointermove',event=>{const band=event.target.closest?.('.sw-chart-hit');if(band&&band!==bands[active])show(Number(band.dataset.index));else if(band&&tooltip.hidden)show(Number(band.dataset.index));});
    listen(plot,'pointerdown',event=>{const band=event.target.closest?.('.sw-chart-hit');if(band){show(Number(band.dataset.index));plot.focus({preventScroll:true});}});
    listen(plot,'pointerleave',()=>{if(document.activeElement!==plot)hide();});
    listen(plot,'keydown',event=>{
      if(event.altKey||event.ctrlKey||event.metaKey)return;
      const rtl=getComputedStyle(plot).direction==='rtl';
      const step=rtl?-1:1;
      switch(event.key){
        case 'ArrowRight':show(active+step);break;
        case 'ArrowLeft':show(active-step);break;
        case 'Home':show(0);break;
        case 'End':show(rows.length-1);break;
        case 'Escape':if(tooltip.hidden)return;hide();break;
        default:return;
      }
      event.preventDefault();event.stopPropagation();
    });
    // The plot uses logical SVG coordinates. Mirror its geometry and categories for RTL;
    // HTML axes follow document direction naturally. No style attribute is generated.
    const sync=()=>{const svg=plot.querySelector('svg');svg.setAttribute('data-rtl',String(getComputedStyle(plot).direction==='rtl'));};
    const observer=new MutationObserver(sync);observer.observe(document.documentElement,{subtree:true,attributes:true,attributeFilter:['dir','class']});sync();
    states.set(root,{plot,destroy(){cleanups.forEach(off=>off());observer.disconnect();status.remove();hide();titles.forEach(({band,title})=>{if(title&&band.isConnected)band.append(title);});plot.removeAttribute('tabindex');}});
  });
}
Shadwick.enhanceCharts=scan;
new MutationObserver(records=>{if(records.some(r=>r.type==='childList'))scan();}).observe(document.documentElement,{childList:true,subtree:true});
window.addEventListener('pageshow',scan);
scan();
})();

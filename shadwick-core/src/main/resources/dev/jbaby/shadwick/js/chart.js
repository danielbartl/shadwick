/* Shadwick's native SVG interaction. Structure/skin patterns adapted from Shadleaf; see NOTICE. */
(function(){
'use strict';
const states=new Map();
const labelStates=new Map();
const axisStates=new Map();
const Shadwick=window.Shadwick=window.Shadwick||{};
function enhanceAxes(root){
  if(axisStates.has(root))return;
  const plot=root.querySelector('.sw-chart-plot'),axis=root.querySelector('.sw-chart-x-axis[data-max-ticks]'),table=root.querySelector('.sw-chart-data table');
  if(!plot||!axis||!table)return;
  const header=table.tHead?.rows[0],rows=Array.from(table.tBodies[0]?.rows||[]),ticks=Array.from(axis.children);
  if(!header||rows.length!==plot.querySelectorAll('.sw-chart-hit').length||rows.length!==ticks.length||rows.some(row=>row.cells.length!==header.cells.length))return;
  const layout=()=>{
    ticks.forEach(tick=>tick.removeAttribute('data-tick-hidden'));
    const bounds=axis.getBoundingClientRect(),limit=Number(axis.dataset.maxTicks)||8,gap=Number(axis.dataset.tickGap)||0;
    const boxes=ticks.map(tick=>tick.firstElementChild.getBoundingClientRect()),chosen=[];
    const order=[0,ticks.length-1];
    for(let i=1;i<limit-1;i++)order.push(Math.round(i*(ticks.length-1)/(limit-1)));
    ticks.forEach((_,i)=>order.push(i));
    const selected=new Set();
    [...new Set(order)].forEach(i=>{
      const box=boxes[i];
      if(!box||!box.width||selected.size>=limit||box.left<bounds.left-1||box.right>bounds.right+1||
        chosen.some(other=>box.left<other.right+gap&&box.right>other.left-gap))return;
      selected.add(i);chosen.push(box);
    });
    ticks.forEach((tick,i)=>tick.toggleAttribute('data-tick-hidden',!selected.has(i)));
  };
  axis.dataset.axisEnhanced='true';
  const resize=new ResizeObserver(layout);resize.observe(axis);
  const direction=new MutationObserver(layout);direction.observe(document.documentElement,{subtree:true,attributes:true,attributeFilter:['dir','class']});
  document.fonts?.addEventListener('loadingdone',layout);
  axisStates.set(root,{plot,axis,destroy(){resize.disconnect();direction.disconnect();document.fonts?.removeEventListener('loadingdone',layout);delete axis.dataset.axisEnhanced;ticks.forEach(tick=>tick.removeAttribute('data-tick-hidden'));}});
  layout();document.fonts?.ready.then(()=>{if(root.isConnected&&root.querySelector('.sw-chart-plot')===plot&&root.querySelector('.sw-chart-x-axis')===axis)layout();});
}
function enhanceLabels(root){
  if(labelStates.has(root))return;
  const plot=root.querySelector('.sw-chart-plot'),labels=Array.from(root.querySelectorAll('.sw-chart-value-label'));
  if(!plot||!labels.length)return;
  // The native data table follows the plot. Wait for it before retaining any label nodes from streamed markup.
  const table=root.querySelector('.sw-chart-data table'),header=table?.tHead?.rows[0],rows=Array.from(table?.tBodies[0]?.rows||[]);
  if(!header||header.cells.length<2||rows.length!==plot.querySelectorAll('.sw-chart-hit').length||rows.some(row=>row.cells.length!==header.cells.length))return;
  plot.dataset.labelsEnhanced='true';
  const layout=()=>{
    labels.forEach(label=>label.removeAttribute('data-collided'));
    const occupied=[];
    const candidates=labels.map(label=>({label,box:label.getBoundingClientRect()})).filter(({box})=>box.width>0&&box.height>0)
      .sort((a,b)=>Number(b.label.dataset.priority)-Number(a.label.dataset.priority));
    candidates.forEach(({label,box})=>{
      const bounds=label.ownerSVGElement.getBoundingClientRect();
      if(box.left<bounds.left||box.right>bounds.right||box.top<bounds.top||box.bottom>bounds.bottom||
        occupied.some(other=>box.left<other.right+3&&box.right>other.left-3&&box.top<other.bottom+3&&box.bottom>other.top-3)){
        label.dataset.collided='true';
      }else occupied.push(box);
    });
  };
  const resize=new ResizeObserver(layout);resize.observe(plot);
  const direction=new MutationObserver(layout);direction.observe(document.documentElement,{subtree:true,attributes:true,attributeFilter:['dir','class']});
  document.fonts?.addEventListener('loadingdone',layout);
  labelStates.set(root,{plot,destroy(){resize.disconnect();direction.disconnect();document.fonts?.removeEventListener('loadingdone',layout);delete plot.dataset.labelsEnhanced;labels.forEach(label=>label.removeAttribute('data-collided'));}});
  layout();document.fonts?.ready.then(()=>{if(root.isConnected&&root.querySelector('.sw-chart-plot')===plot)layout();});
}
function scan(){
  axisStates.forEach((state,root)=>{if(!root.isConnected||root.querySelector('.sw-chart-plot')!==state.plot||root.querySelector('.sw-chart-x-axis')!==state.axis){state.destroy();axisStates.delete(root);}});
  labelStates.forEach((state,root)=>{if(!root.isConnected||root.querySelector('.sw-chart-plot')!==state.plot){state.destroy();labelStates.delete(root);}});
  states.forEach((state,root)=>{if(!root.isConnected||root.querySelector('.sw-chart-plot')!==state.plot){state.destroy();states.delete(root);}});
  document.querySelectorAll('.sw-chart').forEach(root=>{
    if(root.classList.contains('sw-donut-chart'))return;
    enhanceLabels(root);
    enhanceAxes(root);
    if(states.has(root))return;
    const plot=root.querySelector('.sw-chart-plot'),table=root.querySelector('.sw-chart-data table');
    if(!plot||!table||!plot.querySelector('svg')||plot.dataset.tooltip==='false')return;
    const tooltip=plot.querySelector('.sw-chart-tooltip-content');
    // Parsing and Ajax can expose the table before its header/last row is complete. Retry on the next mutation.
    const header=table.tHead?.rows[0];
    if(!header||header.cells.length<2)return;
    const rows=Array.from(table.tBodies[0]?.rows||[]),headers=Array.from(header.cells).slice(1);
    const tipLabel=row=>row.cells[0].dataset.tooltipLabel??row.cells[0].textContent;
    const tipValue=(row,s)=>row.cells[s+1].dataset.tooltipValue??row.cells[s+1].textContent;
    const bands=Array.from(plot.querySelectorAll('.sw-chart-hit'));
    const horizontal=plot.dataset.orientation==='horizontal';
    if(!rows.length||!tooltip||rows.length!==bands.length||rows.some(row=>row.cells.length!==header.cells.length))return;
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
      if(horizontal)bands[active].closest('.sw-chart-row').append(tooltip);
      tooltip.replaceChildren();
      const row=rows[active];
      if(plot.dataset.tooltipLabelVisible!=='false'){
        const label=document.createElement('div');label.className='sw-chart-tooltip-label';label.textContent=tipLabel(row);tooltip.append(label);
      }
      const items=document.createElement('div');items.className='sw-chart-tooltip-items';tooltip.append(items);
      headers.forEach((header,s)=>{
        const value=tipValue(row,s);if(value==='')return;
        const item=document.createElement('div');item.className='sw-chart-tooltip-item '+header.dataset.color;
        const indicator=document.createElement('span');indicator.className='sw-chart-tooltip-indicator';indicator.setAttribute('aria-hidden','true');
        const kind=plot.dataset.tooltipIndicator||'auto';
        if(kind==='auto'&&header.dataset.dashed)indicator.dataset.dashed='true';
        if(kind!=='auto')indicator.dataset.indicator=kind;
        const name=document.createElement('span');name.className='sw-chart-tooltip-name';name.textContent=header.textContent;
        const number=document.createElement('span');number.className='sw-chart-tooltip-value';number.textContent=value;
        if(plot.dataset.tooltipIndicatorVisible!=='false')item.append(indicator);
        item.append(name,number);items.append(item);
      });
      tooltip.hidden=false;
      bands.forEach((band,i)=>band.toggleAttribute('data-active',i===active));
      if(document.activeElement===plot)status.textContent=[tipLabel(row),...headers.flatMap((header,s)=>tipValue(row,s)===''?[]:[header.textContent+': '+tipValue(row,s)])].join('; ');
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
        case 'ArrowRight':if(horizontal)return;show(active+step);break;
        case 'ArrowLeft':if(horizontal)return;show(active-step);break;
        case 'ArrowDown':if(!horizontal)return;show(active+1);break;
        case 'ArrowUp':if(!horizontal)return;show(active-1);break;
        case 'Home':show(0);break;
        case 'End':show(rows.length-1);break;
        case 'Escape':if(tooltip.hidden)return;hide();break;
        default:return;
      }
      if(horizontal&&event.key!=='Escape')bands[active].scrollIntoView({block:'nearest',inline:'nearest'});
      event.preventDefault();event.stopPropagation();
    });
    // The plot uses logical SVG coordinates. Mirror its geometry and categories for RTL;
    // HTML axes follow document direction naturally. No style attribute is generated.
    const sync=()=>{const rtl=String(getComputedStyle(plot).direction==='rtl');plot.querySelectorAll('svg').forEach(svg=>svg.setAttribute('data-rtl',rtl));};
    const observer=new MutationObserver(sync);observer.observe(document.documentElement,{subtree:true,attributes:true,attributeFilter:['dir','class']});sync();
    states.set(root,{plot,destroy(){cleanups.forEach(off=>off());observer.disconnect();status.remove();hide();titles.forEach(({band,title})=>{if(title&&band.isConnected)band.append(title);});plot.removeAttribute('tabindex');}});
  });
}
Shadwick.enhanceCharts=scan;
new MutationObserver(records=>{if(records.some(r=>r.type==='childList'))scan();}).observe(document.documentElement,{childList:true,subtree:true});
window.addEventListener('pageshow',scan);
scan();
})();

/* Native scatter exploration; formatted values are read from the server-rendered table. */
(function(){
'use strict';
const states=new Map(),Shadwick=window.Shadwick=window.Shadwick||{};
function scan(){
  states.forEach((state,root)=>{if(!root.isConnected||root.querySelector('.sw-scatter-plot')!==state.plot){state.destroy();states.delete(root);}});
  document.querySelectorAll('.sw-scatter-chart').forEach(root=>{
    if(states.has(root))return;
    const plot=root.querySelector('.sw-scatter-plot'),table=root.querySelector('.sw-chart-data table');
    if(!plot||!table)return;
    const header=table.tHead?.rows[0],rows=Array.from(table.tBodies[0]?.rows||[]);
    const hits=Array.from(plot.querySelectorAll('.sw-scatter-hit')),nodes=Array.from(plot.querySelectorAll('.sw-scatter-mark'));
    const byIndex=new Map(nodes.map(mark=>[mark.dataset.index,mark])),marks=hits.map(hit=>byIndex.get(hit.dataset.index));
    if(!header||header.cells.length!==4||rows.length!==Number(plot.dataset.rowCount)||hits.length!==Number(plot.dataset.pointCount)
        ||!hits.length||nodes.length!==hits.length||byIndex.size!==hits.length||rows.some(row=>row.cells.length!==4)
        ||hits.some((hit,i)=>!rows[Number(hit.dataset.index)]||!marks[i]))return;
    const cleanups=[],listen=(node,type,handler)=>{node.addEventListener(type,handler);cleanups.push(()=>node.removeEventListener(type,handler));};
    const axis=root.querySelector('.sw-scatter-x-axis');
    if(axis){
      const ticks=Array.from(axis.children);
      const layout=()=>{
        ticks.forEach(tick=>tick.removeAttribute('data-tick-hidden'));
        const bounds=axis.getBoundingClientRect(),chosen=[],selected=new Set();
        const boxes=ticks.map(tick=>tick.firstElementChild.getBoundingClientRect());
        const order=[0,ticks.length-1,...ticks.map((_,i)=>i)];
        [...new Set(order)].forEach(i=>{const box=boxes[i];
          if(!box||!box.width||box.left<bounds.left-1||box.right>bounds.right+1||chosen.some(other=>box.left<other.right+5&&box.right>other.left-5))return;
          selected.add(i);chosen.push(box);
        });
        ticks.forEach((tick,i)=>tick.toggleAttribute('data-tick-hidden',!selected.has(i)));
      };
      axis.dataset.axisEnhanced='true';
      const resize=new ResizeObserver(layout);resize.observe(axis);
      const direction=new MutationObserver(layout);direction.observe(document.documentElement,{subtree:true,attributes:true,attributeFilter:['dir','class']});
      document.fonts?.addEventListener('loadingdone',layout);
      cleanups.push(()=>{resize.disconnect();direction.disconnect();document.fonts?.removeEventListener('loadingdone',layout);
        delete axis.dataset.axisEnhanced;ticks.forEach(tick=>tick.removeAttribute('data-tick-hidden'));});
      layout();document.fonts?.ready.then(()=>{if(root.isConnected&&root.querySelector('.sw-scatter-plot')===plot)layout();});
    }
    const tooltip=plot.querySelector('.sw-chart-tooltip-content');
    if(plot.dataset.tooltip!=='false'&&tooltip){
      let active=-1;
      plot.querySelectorAll(':scope > [data-scatter-status]').forEach(node=>node.remove());
      const status=document.createElement('span');status.className='sw-chart-sr-only';status.dataset.scatterStatus='';
      status.setAttribute('role','status');status.setAttribute('aria-atomic','true');plot.append(status);
      const titles=hits.map(hit=>({hit,title:hit.querySelector('title')}));titles.forEach(({title})=>title?.remove());
      const hide=()=>{tooltip.hidden=true;marks.forEach(mark=>mark.removeAttribute('data-active'));};
      const show=index=>{
        active=Math.max(0,Math.min(hits.length-1,index));
        const row=rows[Number(hits[active].dataset.index)],color=row.dataset.color;
        const label=document.createElement('div');label.className='sw-chart-tooltip-label';label.textContent=row.cells[0].textContent;
        const series=document.createElement('div');series.className='sw-chart-tooltip-item '+color;
        const indicator=document.createElement('span');indicator.className='sw-chart-tooltip-indicator';indicator.setAttribute('aria-hidden','true');
        const name=document.createElement('span');name.className='sw-chart-tooltip-name';name.textContent=row.cells[1].textContent;
        series.append(indicator,name);tooltip.replaceChildren(label,series);
        for(const column of [2,3]){
          const item=document.createElement('div');item.className='sw-chart-tooltip-item';
          const heading=document.createElement('span');heading.className='sw-chart-tooltip-name';heading.textContent=header.cells[column].textContent;
          const value=document.createElement('span');value.className='sw-chart-tooltip-value';value.textContent=row.cells[column].textContent;
          item.append(heading,value);tooltip.append(item);
        }
        tooltip.hidden=false;marks.forEach((mark,i)=>mark.toggleAttribute('data-active',i===active));
        const mark=marks[active];if(mark.parentNode.lastElementChild!==mark)mark.parentNode.append(mark);
        if(document.activeElement===plot)status.textContent=[row.cells[0].textContent,row.cells[1].textContent,
          header.cells[2].textContent+': '+row.cells[2].textContent,header.cells[3].textContent+': '+row.cells[3].textContent].join('; ');
      };
      plot.tabIndex=0;hide();
      listen(plot,'focus',()=>show(active<0?0:active));listen(plot,'blur',hide);
      const hit=event=>{const node=event.target.closest?.('.sw-scatter-hit');return node&&plot.contains(node)?node:null;};
      listen(plot,'pointermove',event=>{const node=hit(event),index=hits.indexOf(node);if(index>=0&&(index!==active||tooltip.hidden))show(index);});
      listen(plot,'pointerdown',event=>{const node=hit(event);if(node&&event.button===0){show(hits.indexOf(node));plot.focus({preventScroll:true});}});
      listen(plot,'pointerleave',()=>{if(document.activeElement!==plot)hide();});
      listen(plot,'keydown',event=>{
        if(event.altKey||event.ctrlKey||event.metaKey)return;
        const step=getComputedStyle(plot).direction==='rtl'?-1:1;
        switch(event.key){
          case 'ArrowRight':show(active+step);break;case 'ArrowLeft':show(active-step);break;
          case 'ArrowDown':show(active+1);break;case 'ArrowUp':show(active-1);break;
          case 'Home':show(0);break;case 'End':show(hits.length-1);break;
          case 'Escape':if(tooltip.hidden)return;hide();break;default:return;
        }
        event.preventDefault();event.stopPropagation();
      });
      cleanups.push(()=>{status.remove();hide();titles.forEach(({hit,title})=>{if(title&&hit.isConnected)hit.append(title);});
        marks.forEach(mark=>{if(mark.isConnected&&hits[0].isConnected)hits[0].before(mark);});plot.removeAttribute('tabindex');});
    }
    states.set(root,{plot,destroy(){cleanups.forEach(off=>off());}});
  });
}
Shadwick.enhanceScatterCharts=scan;
new MutationObserver(records=>{if(records.some(record=>record.type==='childList'))scan();}).observe(document.documentElement,{childList:true,subtree:true});
window.addEventListener('pageshow',scan);scan();
})();

/*
  Native heatmap exploration; cell values and axis labels stay in the semantic table. A calendar heatmap's padding
  days (data-outside) hold no value: the keyboard and the pointer pass over them. A cell's data-tooltip-label (a
  calendar day's full date) names it in place of its row and column.
*/
(function(){
'use strict';
const states=new Map(),Shadwick=window.Shadwick=window.Shadwick||{};
function scan(){
  states.forEach((state,root)=>{if(!root.isConnected||root.querySelector('.sw-heatmap-plot')!==state.plot){state.destroy();states.delete(root);}});
  document.querySelectorAll('.sw-heatmap-chart').forEach(root=>{
    if(states.has(root))return;
    const plot=root.querySelector('.sw-heatmap-plot'),table=plot?.querySelector('table'),tooltip=root.querySelector('.sw-heatmap-tooltip');
    if(!plot||!table||!tooltip||plot.dataset.tooltip==='false')return;
    const rows=Array.from(table.tBodies[0]?.rows||[]),headers=Array.from(table.tHead?.rows[0]?.cells||[]).slice(1);
    const cells=Array.from(table.querySelectorAll('td.sw-heatmap-cell')),width=headers.length;
    if(!width||!rows.length||rows.length!==Number(plot.dataset.rows)||width!==Number(plot.dataset.columns)
       ||cells.length!==rows.length*width||rows.some(row=>row.cells.length!==width+1)
       ||cells.some((cell,i)=>Number(cell.dataset.index)!==i||(!('outside' in cell.dataset)&&(!cell.querySelector('.sw-heatmap-value')||!cell.querySelector('svg')))))return;
    const inside=index=>index>=0&&index<cells.length&&!('outside' in cells[index].dataset);
    // From index, the first cell inside the period in steps of step (within bounds), or -1
    const seek=(index,step,limit)=>{for(let i=index;limit(i);i+=step)if(inside(i))return i;return -1;};
    // The earliest and latest cells in reading order of a calendar: column by column (week by week), then by row
    const byColumn=Array.from({length:cells.length},(_,i)=>(i%width)*rows.length+Math.floor(i/width));
    const ordered=cells.map((_,i)=>i).filter(inside).sort((a,b)=>byColumn[a]-byColumn[b]);
    const first=ordered.length?ordered[0]:0,last=ordered.length?ordered[ordered.length-1]:cells.length-1;
    const cleanups=[],listen=(node,type,handler)=>{node.addEventListener(type,handler);cleanups.push(()=>node.removeEventListener(type,handler));};
    let active=first;
    plot.querySelectorAll(':scope > [data-heatmap-status]').forEach(node=>node.remove());
    const status=document.createElement('span');status.className='sw-chart-sr-only';status.dataset.heatmapStatus='';
    status.setAttribute('role','status');status.setAttribute('aria-atomic','true');plot.append(status);
    const titles=cells.map(cell=>({cell,title:cell.getAttribute('title')}));
    titles.forEach(({cell})=>cell.removeAttribute('title'));
    const hide=()=>{tooltip.hidden=true;cells.forEach(cell=>cell.removeAttribute('data-active'));};
    const show=(index,scroll=false)=>{
      active=Math.max(0,Math.min(cells.length-1,index));
      const cell=cells[active],r=Math.floor(active/width),c=active%width;
      const row=rows[r].cells[0].textContent,column=headers[c].textContent,name0=cell.dataset.tooltipLabel??row+' / '+column;
      const value=cell.dataset.tooltipValue??cell.querySelector('.sw-heatmap-value').textContent;
      const label=document.createElement('div');label.className='sw-chart-tooltip-label';label.textContent=name0;
      const item=document.createElement('div');item.className='sw-chart-tooltip-item '+plot.dataset.color;
      const indicator=document.createElement('span');indicator.className='sw-chart-tooltip-indicator';indicator.setAttribute('aria-hidden','true');
      const name=document.createElement('span');name.className='sw-chart-tooltip-name';name.textContent=plot.dataset.valueLabel;
      const number=document.createElement('span');number.className='sw-chart-tooltip-value';number.textContent=value;
      item.append(indicator,name,number);tooltip.replaceChildren(label,item);tooltip.hidden=false;
      cells.forEach((node,i)=>node.toggleAttribute('data-active',i===active));
      if(document.activeElement===plot)status.textContent=(cell.dataset.tooltipLabel??row+'; '+column)+'; '+plot.dataset.valueLabel+': '+value;
      if(scroll)cell.scrollIntoView({block:'nearest',inline:'nearest'});
    };
    hide();plot.dataset.heatmapEnhanced='true';
    listen(plot,'focus',()=>show(active));listen(plot,'blur',hide);
    const hit=event=>{const cell=event.target.closest?.('.sw-heatmap-cell');return cell&&table.contains(cell)&&!('outside' in cell.dataset)?cell:null;};
    listen(plot,'pointermove',event=>{const cell=hit(event);if(cell&&(Number(cell.dataset.index)!==active||tooltip.hidden))show(Number(cell.dataset.index));});
    listen(plot,'pointerdown',event=>{const cell=hit(event);if(cell&&event.button===0){event.preventDefault();show(Number(cell.dataset.index));plot.focus({preventScroll:true});}});
    listen(plot,'pointerleave',()=>{if(document.activeElement!==plot)hide();});
    listen(plot,'keydown',event=>{
      if(event.altKey||event.metaKey||(event.ctrlKey&&event.key!=='Home'&&event.key!=='End'))return;
      const r=Math.floor(active/width),c=active%width,step=getComputedStyle(plot).direction==='rtl'?-1:1;
      // Moves pass over padding days; with nothing inside in that direction, the active cell stays
      const go=index=>show(index<0?active:index,true);
      const inRow=i=>i>=r*width&&i<r*width+width,inColumn=i=>i>=0&&i<cells.length;
      switch(event.key){
        case 'ArrowRight':go(seek(active+step,step,inRow));break;
        case 'ArrowLeft':go(seek(active-step,-step,inRow));break;
        case 'ArrowDown':go(seek(active+width,width,inColumn));break;
        case 'ArrowUp':go(seek(active-width,-width,inColumn));break;
        case 'Home':go(event.ctrlKey?first:seek(r*width,1,inRow));break;
        case 'End':go(event.ctrlKey?last:seek(r*width+width-1,-1,inRow));break;
        case 'Escape':if(tooltip.hidden)return;hide();break;
        default:return;
      }
      event.preventDefault();event.stopPropagation();
    });
    states.set(root,{plot,destroy(){cleanups.forEach(off=>off());status.remove();hide();delete plot.dataset.heatmapEnhanced;
      titles.forEach(({cell,title})=>{if(title!==null&&cell.isConnected)cell.setAttribute('title',title);});}});
  });
}
Shadwick.enhanceHeatmaps=scan;
new MutationObserver(records=>{if(records.some(record=>record.type==='childList'))scan();}).observe(document.documentElement,{childList:true,subtree:true});
window.addEventListener('pageshow',scan);scan();
})();

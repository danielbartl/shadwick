// The source is the versioned API authority. This scanner intentionally reads direct class members,
// not anonymous callbacks or inherited Wicket implementation details.
export function javaApi(source) {
  const masked=source.replace(/"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\/\/[\s\S]*?(?=\n|$)|\/\*[\s\S]*?\*\//g, value=>value.replace(/[^\n]/g,' '));
  const type=/\bpublic\s+(?:(?:abstract|final|sealed|non-sealed)\s+)*(class|interface|enum|record)\s+(\w+)/.exec(masked);
  if(!type)throw new Error('Expected a public Java API type');
  const depth=new Uint16Array(masked.length);let nesting=0;
  for(let i=0;i<masked.length;i++){depth[i]=nesting;if(masked[i]==='{')nesting++;if(masked[i]==='}')nesting--;}
  const bodyStart=masked.indexOf('{',type.index+type[0].length);
  const declaration=source.slice(type.index,bodyStart).replace(/\s+/g,' ').trim();
  const members=[];
  if(type[1]==='enum') {
    let end=bodyStart+1;while(end<masked.length&&masked[end]!==';'&&!(masked[end]==='}'&&depth[end]===1))end++;
    const values=[...masked.slice(bodyStart+1,end).matchAll(/\b([A-Z][A-Z_\d]*)\s*(?=\(|,|$)/g)].map(m=>m[1]);
    members.push({kind:'Enum values',signature:values.join(', '),description:''});
  }
  if(type[1]==='record')members.push({kind:'Record',signature:declaration,description:'Record components define its canonical constructor and accessors.'});
  const declarations=[...masked.matchAll(/\b(public|protected)\s+/g)];
  if(type[1]==='interface')for(const match of masked.matchAll(/\b(?:default\s+)?[A-Za-z][\w<>, ?\[\].]*\s+\w+\s*(?=\()/gm)) {
    const start=match.index+match[0].search(/\S/);
    if(depth[start]===1&&!/^\s*(public|protected|private|static)\s/.test(match[0]))declarations.push({index:start,1:'public'});
  }
  declarations.sort((a,b)=>a.index-b.index);
  for(const match of declarations) {
    if(depth[match.index]!==1)continue;
    const tail=masked.slice(match.index), opening=tail.indexOf('('), stop=tail.search(/[;={}]/);
    const enumeration=/^(?:public|protected)\s+enum\s+(\w+)/.exec(tail);
    if(enumeration){
      const start=match.index+tail.indexOf('{')+1;let end=start;while(end<masked.length&&masked[end]!==';'&&!(masked[end]==='}'&&depth[end]===2))end++;
      const values=[...masked.slice(start,end).matchAll(/\b([A-Z][A-Z_\d]*)\s*(?=\(|,|$)/g)].map(m=>m[1]);
      members.push({kind:'Enum',signature:`${enumeration[0].trim()}`,description:values.join(', ')});continue;
    }
    if(opening<0||stop>=0&&stop<opening)continue;
    let close=match.index+opening,balance=0;
    for(;close<masked.length;close++){if(masked[close]==='(')balance++;if(masked[close]===')'&&--balance===0)break;}
    if(close===masked.length)throw new Error('Unbalanced Java signature');
    const signature=source.slice(match.index,close+1).replace(/\s+/g,' ').trim();
    const name=/(\w+)\s*\(/.exec(signature)?.[1];
    if(['renderHead','onComponentTag','onConfigure','onBeforeRender','onDetach','onEvent','onInitialize'].includes(name))continue;
    const before=source.slice(0,match.index).trimEnd();const end=before.lastIndexOf('*/'),begin=before.lastIndexOf('/**',end);
    let description='';
    if(begin>=0&&end>=begin&&/^(?:\s|@\w+(?:\([^)]*\))?)*$/.test(before.slice(end+2)))
      description=before.slice(begin+3,end).replace(/^\s*\* ?/gm,'').split(/\n\s*@(?:param|return|throws|see|since|deprecated)\b/)[0]
        .replace(/\{@(?:code|link|linkplain)\s+([^}]+)\}/g,'$1').replace(/<[^>]+>/g,' ').replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&amp;/g,'&').replace(/\s+/g,' ').trim();
    members.push({kind:name===type[2]?'Constructor':match[1]==='protected'?'Hook':'Method',signature,description});
  }
  return {name:type[2],kind:type[1],declaration,members};
}

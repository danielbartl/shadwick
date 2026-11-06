// Deliberately small authoring format: trusted repository Markdown, with escaped code and no raw HTML.
// Unsupported block syntax fails the build; additions to the format must come with renderer tests.
export function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}
export function plainText(value) {
  return value.replace(/`([^`]+)`/g, '$1').replace(/\[([^\]]+)\]\([^)]+\)/g, '$1').replace(/\*\*|__/g, '');
}
export function slug(value) {
  return plainText(value).normalize('NFKD').toLowerCase().replace(/[^\p{L}\p{N}\s-]/gu, '').trim().replace(/\s+/g, '-');
}
export function safeUrl(value) {
  if (/[\u0000-\u0020\\]/.test(value) || /^(?:javascript|data|vbscript):/i.test(value) || value.startsWith('//'))
    throw new Error(`Unsupported documentation URL: ${value}`);
  if (/^[a-z][a-z\d+.-]*:/i.test(value) && !/^(?:https?:|mailto:)/i.test(value)) throw new Error(`Unsupported documentation URL: ${value}`);
  return value;
}
export function inline(value, resolveLink = value => value) {
  // Tokenize before escaping; generated markup is never fed back through Markdown substitutions.
  const token = /`([^`\n]+)`|\[([^\]\n]+)\]\(([^\s)]+)(?:\s+"([^"]*)")?\)|\*\*([^*\n]+)\*\*|\*([^*\n]+)\*/g;
  let html = '', first = 0;
  for (const match of value.matchAll(token)) {
    html += escapeHtml(value.slice(first, match.index));
    if (match[1] !== undefined) html += `<code>${escapeHtml(match[1])}</code>`;
    else if (match[2] !== undefined) html += `<a href="${escapeHtml(safeUrl(resolveLink(match[3])))}"${match[4] ? ` title="${escapeHtml(match[4])}"` : ''}>${inline(match[2], resolveLink)}</a>`;
    else if (match[5] !== undefined) html += `<strong>${escapeHtml(match[5])}</strong>`;
    else html += `<em>${escapeHtml(match[6])}</em>`;
    first = match.index + match[0].length;
  }
  return html + escapeHtml(value.slice(first));
}
function highlighted(value, language) {
  if (!['java','html','xml','javascript','js','css'].includes(language)) return escapeHtml(value);
  const tokens = language === 'html' || language === 'xml'
    ? /<!--[\s\S]*?-->|<\/?[\w:][^>]*>/g
    : /\/\*[\s\S]*?\*\/|\/\/[^\n]*|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\b(?:public|private|protected|static|final|class|extends|implements|new|return|void|boolean|int|long|double|null|true|false|import|package|if|else|try|catch|throw|const|let|function|async|await)\b/g;
  let result = '', first = 0;
  for (const token of value.matchAll(tokens)) {
    result += escapeHtml(value.slice(first, token.index));
    const kind = token[0].startsWith('//') || token[0].startsWith('/*') || token[0].startsWith('<!--') ? 'comment'
      : /^['"]/.test(token[0]) ? 'string' : token[0].startsWith('<') ? 'tag' : 'keyword';
    result += `<span class="syntax-${kind}">${escapeHtml(token[0])}</span>`;
    first = token.index + token[0].length;
  }
  return result + escapeHtml(value.slice(first));
}
export function codeBlock(code, language = '', index = 0) {
  if (!/^[a-z\d+-]*$/i.test(language)) throw new Error('Invalid code language');
  const label = ({sh:'Shell',bash:'Shell',java:'Java',html:'HTML',xml:'XML',css:'CSS',js:'JavaScript',javascript:'JavaScript',properties:'Properties'})[language] || 'Code';
  return `<figure class="code-example"><figcaption><span>${label}</span><button type="button" class="copy-code" data-copy-code="code-${index}" aria-label="Copy ${label} example" hidden>Copy<span class="sr-only"> ${label} example</span></button></figcaption><pre tabindex="0" aria-label="${label} example"><code id="code-${index}"${language ? ` class="language-${language}"` : ''}>${highlighted(code, language)}</code></pre></figure>`;
}
export function renderMarkdown(source, {resolveLink = value => value, omitTitle = false, _counter = {value:0}, _used = new Map()} = {}) {
  const lines = source.replace(/\r\n/g, '\n').split('\n');
  const headings = [], used = _used;
  let html = '', index = 0, seenTitle = false;
  const startsBlock = line => /^(?:#{1,6}\s|```|>\s?|\s*[-*+]\s|\s*\d+\.\s|\|)/.test(line);
  while (index < lines.length) {
    const line = lines[index];
    if (!line.trim()) { index++; continue; }
    if (/^<!-- snippet: [\w./-]+ -->$/.test(line)) { index++; continue; }
    if (line.startsWith('```')) {
      const lang = line.slice(3).trim(); const body = []; index++;
      while (index < lines.length && lines[index] !== '```') body.push(lines[index++]);
      if (index === lines.length) throw new Error('Unclosed code fence');
      html += codeBlock(body.join('\n'), lang, _counter.value++); index++; continue;
    }
    if (/^#{1,6}\s*$/.test(line)) throw new Error('Empty heading anchor');
    const heading = /^(#{1,6})\s+(.+)$/.exec(line);
    if (heading) {
      index++; const level = heading[1].length;
      if (omitTitle && level === 1) { if (seenTitle || headings.length || html) throw new Error('Only the first heading may be H1'); seenTitle=true; continue; }
      if (level === 1 || level > 4) throw new Error('Documentation content headings must use H2–H4');
      const base = slug(heading[2]); if (!base) throw new Error('Empty heading anchor');
      const count = used.get(base) || 0; used.set(base, count + 1); const id = base + (count ? `-${count}` : '');
      headings.push({level,id,text:plainText(heading[2])});
      html += `<h${level} id="${id}" tabindex="-1">${inline(heading[2], resolveLink)}<a class="heading-anchor" href="#${id}" aria-label="Link to ${escapeHtml(plainText(heading[2]))}">#</a></h${level}>`; continue;
    }
    if (line.startsWith('|')) {
      const rows = []; while (index < lines.length && lines[index].startsWith('|')) rows.push(lines[index++]);
      const cells = value => value.trim().replace(/^\||\|$/g,'').split(/(?<!\\)\|/).map(c => c.trim().replace(/\\\|/g,'|'));
      if (rows.length < 2 || !cells(rows[1]).every(cell => /^:?-{3,}:?$/.test(cell))) throw new Error('A Markdown table needs a separator row');
      const header = cells(rows[0]);
      html += '<div class="docs-table-scroll" tabindex="0" role="region" aria-label="Reference table"><table><thead><tr>' + header.map(c=>`<th scope="col">${inline(c,resolveLink)}</th>`).join('')+'</tr></thead><tbody>';
      for (const row of rows.slice(2)) { const values=cells(row); if(values.length!==header.length) throw new Error('Mismatched table columns'); html += '<tr>'+values.map(c=>`<td>${inline(c,resolveLink)}</td>`).join('')+'</tr>'; }
      html += '</tbody></table></div>'; continue;
    }
    if (line.startsWith('>')) {
      const body=[];while(index<lines.length&&lines[index].startsWith('>'))body.push(lines[index++].replace(/^>\s?/,''));
      const quote=renderMarkdown(body.join('\n'),{resolveLink,_counter,_used:used});headings.push(...quote.headings);html += `<aside class="docs-callout">${quote.html}</aside>`;continue;
    }
    const list = /^(\s*)([-*+]|\d+\.)\s+(.+)$/.exec(line);
    if (list) {
      const ordered = /\d/.test(list[2]); const tag=ordered?'ol':'ul'; const items=[];
      const indent=list[1].length;
      while(index<lines.length){
        const item=/^(\s*)([-*+]|\d+\.)\s+(.+)$/.exec(lines[index]);
        if(!item||item[1].length!==indent||/\d/.test(item[2])!==ordered)break;
        const body=[item[3]];index++;
        while(index<lines.length&&lines[index].trim()&&!startsBlock(lines[index]))body.push(lines[index++].trim());
        items.push(`<li>${inline(body.join(' '),resolveLink)}</li>`);
        if(index<lines.length&&lines[index].trim()&&/^\s+[-*+\d]/.test(lines[index]))throw new Error('Nested Markdown lists are not supported; use a separate subsection');
      }
      html += `<${tag}>${items.join('')}</${tag}>`;continue;
    }
    if (/^\s{4}\S|^!\[|^\[.+\]:|^<[^ ]/.test(line)) throw new Error(`Unsupported Markdown block: ${line.slice(0,80)}`);
    const paragraph=[line];index++;
    while(index<lines.length&&lines[index].trim()&&!startsBlock(lines[index]))paragraph.push(lines[index++]);
    html += `<p>${inline(paragraph.join(' '), resolveLink)}</p>`;
  }
  return {html,headings};
}

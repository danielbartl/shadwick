/** Rewrite prose links without changing code examples. */
export function mapMarkdownLinks(markdown, resolveLink) {
  let fenced = false;
  return markdown.split('\n').map(line => {
    if (/^```/.test(line)) { fenced = !fenced; return line; }
    if (fenced) return line;
    const literals = [...line.matchAll(/`+[^`]*`+/g)].map(match => [match.index, match.index + match[0].length]);
    return line.replace(/\[([^\]]+)\]\(([^\s)]+)\)/g, (full, label, reference, offset) =>
      literals.some(([start, end]) => start <= offset && offset < end) ? full : `[${label}](${resolveLink(reference)})`);
  }).join('\n');
}

export function markdownLinks(markdown) {
  const result = [];
  mapMarkdownLinks(markdown, reference => { result.push(reference); return reference; });
  return result;
}

/** Plain Markdown companions for the same guides/API declarations as the HTML documentation. */
export function guideMarkdown({entry, markdown, apis, repository, resolveLink}) {
  const body = mapMarkdownLinks(markdown, resolveLink).trimEnd();
  const source = `https://github.com/${repository}/blob/main/`;
  let api = '';
  if (apis.length) {
    api = '\n\n## API reference\n\nDeclarations below come from the Java source in this build. Inherited Wicket methods remain available.\n';
    for (let i = 0; i < apis.length; i++) {
      const type = apis[i];
      api += `\n### ${type.name}\n\n[Java source](${source + entry.apis[i]})\n\n\`\`\`java\n${type.declaration}\n\`\`\`\n`;
      for (const member of type.members) {
        api += `\n${member.kind}:\n\n\`\`\`java\n${member.signature}\n\`\`\`\n`;
        if (member.description) api += `\n${member.description}\n`;
      }
    }
  }
  return body + api + `\n\n## Example source\n\n[${entry.example}](${source}shadwick-examples/src/main/java/dev/jbaby/shadwick/examples/${entry.example}.java)\n`;
}

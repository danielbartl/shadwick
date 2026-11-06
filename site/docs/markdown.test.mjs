import test from 'node:test';
import assert from 'node:assert/strict';
import {inline,renderMarkdown,safeUrl,slug} from './markdown.mjs';

test('inline code and labels escape HTML without reinterpreting generated attributes',()=>{
  const html=inline('Text `<script>alert("x")</script>` and **clear** [the `API`](./api.md).');
  assert(!html.includes('<script>'));assert(html.includes('&lt;script&gt;'));assert(html.includes('<strong>clear</strong>'));
  assert(html.includes('<a href="./api.md">the <code>API</code></a>'));
});
test('link resolver transforms destinations and its output is still checked',()=>{
  assert.equal(inline('[Guide](../a.md)',()=> '../../a/'),'<a href="../../a/">Guide</a>');
  assert.throws(()=>inline('[Guide](./a.md)',()=> 'javascript:alert(1)'),/Unsupported/);
});
for(const url of ['javascript:alert(1)','data:text/html,a','vbscript:a','//evil.example/a','https://safe.example/\nno','file:///etc/passwd','a\\b'])
  test('unsafe link is rejected: '+JSON.stringify(url),()=>assert.throws(()=>safeUrl(url),/Unsupported/));
test('fences preserve code whitespace and escape tags and attributes',()=>{
  const html=renderMarkdown('```html\n<dialog id="test">\n  <script>alert(1)</script>\n</dialog>\n```').html;
  assert(!html.includes('<dialog'));assert(!html.includes('<script>'));assert(html.includes('&lt;dialog id=&quot;test&quot;&gt;'));
  assert(html.includes('  '));assert(html.includes('aria-label="Copy HTML example"'));assert(html.includes('tabindex="0"'));
});
test('copy target IDs remain unique across callouts and the article',()=>{
  const html=renderMarkdown('```java\na();\n```\n\n> ```java\n> b();\n> ```\n\n```java\nc();\n```').html;
  const ids=[...html.matchAll(/id="(code-\d+)"/g)].map(m=>m[1]);assert.deepEqual(ids,['code-0','code-1','code-2']);
});
test('headings produce stable distinct links with punctuation and code removed',()=>{
  assert.equal(slug('Light, Dark and System preferences'),'light-dark-and-system-preferences');
  const doc=renderMarkdown('# Page\n\n## `SwButton`\n\nText\n\n## `SwButton`',{omitTitle:true});
  assert.deepEqual(doc.headings.map(h=>h.id),['swbutton','swbutton-1']);assert(!doc.html.includes('<h1'));
});
test('multiple H1 titles are rejected',()=>assert.throws(()=>renderMarkdown('# One\n\n# Two',{omitTitle:true}),/Only the first/));
test('tables render column headers and escaped cell markup',()=>{
  const html=renderMarkdown('| Option | Meaning |\n| --- | --- |\n| `x` | <img onerror=x> |').html;
  assert(html.includes('scope="col"'));assert(html.includes('&lt;img onerror=x&gt;'));assert(!html.includes('<img'));
});
for(const source of ['```java\nno closing fence','| A | B |\n| --- | --- |\n| one |','<script>alert(1)</script>','- Item\n  - Nested','## '])
  test('invalid or unsupported blocks fail: '+source.slice(0,24),()=>assert.throws(()=>renderMarkdown(source)));
test('simple lists and notes preserve their content',()=>{
  const html=renderMarkdown('- One\n- Two\n\n> A useful **note**.').html;
  assert(html.includes('<ul><li>One</li><li>Two</li></ul>'));assert(html.includes('class="docs-callout"'));assert(html.includes('<strong>note</strong>'));
});

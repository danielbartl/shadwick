/* Freeform tags: the native text field owns all submitted values. */
(function () {
'use strict';
const Shadwick = window.Shadwick = window.Shadwick || {};
const instances = new Map();
let serial = 0;
// Match Java String.trim(), including its deliberate treatment of nonbreaking spaces.
const trim = value => value.replace(/^[\u0000-\u0020]+|[\u0000-\u0020]+$/g, '');
const parse = value => [...new Set(value.split(',').map(trim).filter(Boolean))];
const sourceOf = root => root.querySelector(':scope > input.sw-tag-input-native');
function scan() {
  instances.forEach((state, root) => {
    if (!root.isConnected || sourceOf(root) !== state.source) {
      state.dispose();
      instances.delete(root);
    }
  });
  document.querySelectorAll('.sw-tag-input').forEach(root => {
    const source = sourceOf(root);
    if (!source || instances.has(root)) return;
    const state = {source};
    instances.set(root, state);
    enhance(root, source, state);
  });
}
Shadwick.enhanceTagInputs = scan;
new MutationObserver(records => {
  if (records.some(record => record.type === 'childList')) scan();
  if (records.some(record => record.type === 'attributes' && record.target.tagName === 'FIELDSET')) instances.forEach(state => state.sync());
}).observe(document.documentElement, {childList:true, subtree:true, attributes:true, attributeFilter:['disabled']});
window.addEventListener('pageshow', () => { scan(); instances.forEach(state => state.sync()); });
scan();

function enhance(root, source, state) {
  const cleanups = [];
  const listen = (node, type, handler, options) => {
    node.addEventListener(type, handler, options);
    cleanups.push(() => node.removeEventListener(type, handler, options));
  };
  root.querySelectorAll(':scope > .sw-combobox-control, :scope > .sw-tag-input-status, :scope > .sw-tag-input-instructions').forEach(node => node.remove());
  source.id ||= `sw-tags-${++serial}`;
  const editor = document.createElement('input');
  editor.type = 'text';
  editor.id = `${source.id}-editor`;
  editor.className = 'sw-combobox-input';
  editor.autocomplete = 'off';
  const control = document.createElement('div');
  control.className = 'sw-combobox-control';
  control.append(editor);
  const instructions = document.createElement('span');
  instructions.id = `${source.id}-instructions`;
  instructions.className = 'sw-tag-input-instructions';
  instructions.textContent = root.dataset.instructions;
  const status = document.createElement('div');
  status.className = 'sw-tag-input-status';
  status.setAttribute('role', 'status');
  status.setAttribute('aria-atomic', 'true');
  let tags = parse(source.value);
  let draft = '';
  let writing = false;
  let composing = false;
  let announcement = 0;
  let resetTimer = 0;
  const value = () => [...tags, ...(draft ? [draft] : [])].join(',');
  try {
    const saved = JSON.parse(source.dataset.swTagInputTags);
    const savedDraft = source.dataset.swTagInputDraft || '';
    if (Array.isArray(saved) && saved.every(tag => typeof tag === 'string') &&
        [...saved, ...(savedDraft ? [savedDraft] : [])].join(',') === source.value) {
      tags = saved; draft = savedDraft;
    }
  } catch (_) { /* First enhancement has no saved editor state. */ }
  const restoreAttribute = (name, saved) => {
    if (saved === null) source.removeAttribute(name); else source.setAttribute(name, saved);
  };
  const original = (name, key) => {
    if (root.hasAttribute('data-enhanced') && source.dataset[key] !== undefined) {
      try { return JSON.parse(source.dataset[key]); } catch (_) { /* Ignore malformed history metadata. */ }
    }
    return source.getAttribute(name);
  };
  const tabindex = original('tabindex', 'swTagInputTabindex');
  const ariaHidden = original('aria-hidden', 'swTagInputAriaHidden');
  source.dataset.swTagInputTabindex = JSON.stringify(tabindex);
  source.dataset.swTagInputAriaHidden = JSON.stringify(ariaHidden);
  const labels = Array.from(document.querySelectorAll('label')).filter(label => label.htmlFor === source.id || label.dataset.swTagInputFor === source.id);
  labels.forEach((label, index) => {
    label.id ||= `${source.id}-label-${index}`;
    label.dataset.swTagInputFor = source.id;
    label.htmlFor = editor.id;
  });
  source.tabIndex = -1;
  source.setAttribute('aria-hidden', 'true');
  root.append(control, instructions, status);
  root.dataset.enhanced = 'true';
  const mutable = () => !source.matches(':disabled') && !source.readOnly;
  const buttons = () => Array.from(control.querySelectorAll('.sw-combobox-chip-remove'));
  function announce(key, tag) {
    clearTimeout(announcement);
    status.textContent = '';
    announcement = setTimeout(() => { status.textContent = (root.dataset[key] || '{0}').replace('{0}', () => tag); }, 30);
  }
  function publish(change) {
    const before = source.value;
    source.value = value();
    source.dataset.swTagInputTags = JSON.stringify(tags);
    source.dataset.swTagInputDraft = draft;
    writing = true;
    try {
      if (before !== source.value) source.dispatchEvent(new Event('input', {bubbles:true}));
      if (change) source.dispatchEvent(new Event('change', {bubbles:true}));
    } finally { writing = false; }
  }
  function remove(index) {
    if (!mutable()) return;
    const [tag] = tags.splice(index, 1);
    render();
    (buttons()[index] || editor).focus();
    publish(true);
    announce('removed', tag);
  }
  function render() {
    control.querySelectorAll('.sw-combobox-chip').forEach(node => node.remove());
    tags.forEach((tag, index) => {
      const chip = document.createElement('span');
      chip.className = 'sw-combobox-chip';
      const label = document.createElement('span');
      label.className = 'sw-combobox-chip-label';
      label.textContent = tag;
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'sw-combobox-chip-remove';
      button.setAttribute('aria-label', (root.dataset.remove || 'Remove {0}').replace('{0}', () => tag));
      button.disabled = !mutable();
      // A fixed SVG, never constructed from a submitted tag.
      button.innerHTML = '<svg class="sw-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="m18 6-12 12M6 6l12 12"/></svg>';
      button.addEventListener('click', () => remove(index));
      button.addEventListener('keydown', event => {
        if (!mutable()) return;
        const rtl = getComputedStyle(root).direction === 'rtl';
        const back = rtl ? 'ArrowRight' : 'ArrowLeft';
        const next = rtl ? 'ArrowLeft' : 'ArrowRight';
        let target;
        if (event.key === 'Backspace' || event.key === 'Delete') { event.preventDefault(); remove(index); return; }
        if (event.key === back) target = buttons()[Math.max(0, index - 1)];
        if (event.key === next) target = buttons()[index + 1] || editor;
        if (event.key === 'Home') target = buttons()[0];
        if (event.key === 'End') target = editor;
        if (target) { event.preventDefault(); target.focus(); }
      });
      chip.append(label, button);
      control.insertBefore(chip, editor);
    });
    editor.value = draft;
  }
  function add(parts) {
    parts.map(trim).filter(Boolean).forEach(tag => {
      if (tags.includes(tag)) announce('duplicate', tag);
      else { tags.push(tag); announce('added', tag); }
    });
  }
  function commit() {
    if (!mutable() || composing) return;
    if (!draft) return;
    add(draft.split(','));
    draft = '';
    render();
    publish(true);
  }
  function edit(event) {
    if (!mutable()) return;
    draft = editor.value;
    const comma = composing || event?.isComposing ? -1 : draft.lastIndexOf(',');
    if (comma >= 0) {
      add(draft.slice(0, comma).split(','));
      draft = draft.slice(comma + 1);
      render();
    }
    publish(comma >= 0);
  }
  function sync() {
    if (source.value !== value()) { tags = parse(source.value); draft = ''; render(); }
    editor.disabled = source.matches(':disabled');
    editor.readOnly = source.readOnly;
    editor.placeholder = source.getAttribute('placeholder') || '';
    editor.tabIndex = tabindex === null ? 0 : Number(tabindex);
    ['aria-label', 'aria-labelledby', 'aria-invalid', 'aria-required', 'title'].forEach(name => {
      if (source.hasAttribute(name)) editor.setAttribute(name, source.getAttribute(name)); else editor.removeAttribute(name);
    });
    if (!editor.hasAttribute('aria-label') && !editor.hasAttribute('aria-labelledby') && labels.length) {
      editor.setAttribute('aria-labelledby', labels.map(label => label.id).join(' '));
    }
    editor.setAttribute('aria-describedby', [source.getAttribute('aria-describedby'), instructions.id].filter(Boolean).join(' '));
    buttons().forEach(button => { button.disabled = !mutable(); });
  }
  listen(editor, 'input', edit);
  listen(editor, 'compositionstart', () => { composing = true; });
  listen(editor, 'compositionend', () => { composing = false; edit(); });
  listen(editor, 'keydown', event => {
    if (event.isComposing || composing || event.keyCode === 229) return;
    if (event.key === 'Enter') { event.preventDefault(); commit(); }
    if (event.key === 'Backspace' && !editor.value && mutable() && tags.length) { event.preventDefault(); buttons().at(-1).focus(); }
  });
  listen(editor, 'blur', event => { if (!control.contains(event.relatedTarget)) commit(); });
  listen(control, 'click', event => { if (event.target === control) editor.focus(); });
  ['input', 'change'].forEach(type => listen(source, type, () => {
    if (writing) return;
    tags = parse(source.value); draft = ''; render(); sync();
  }));
  listen(source, 'focus', () => editor.focus());
  listen(source, 'invalid', event => { event.preventDefault(); editor.focus(); });
  const observer = new MutationObserver(sync);
  observer.observe(source, {attributes:true, attributeFilter:['aria-label', 'aria-labelledby', 'aria-describedby', 'aria-invalid', 'aria-required', 'placeholder', 'disabled', 'readonly', 'title', 'value']});
  cleanups.push(() => observer.disconnect());
  if (source.form) listen(source.form, 'reset', event => {
    clearTimeout(resetTimer);
    resetTimer = setTimeout(() => {
      if (event.defaultPrevented) return;
      tags = parse(source.value); draft = ''; render(); sync();
      source.dataset.swTagInputTags = JSON.stringify(tags);
      source.dataset.swTagInputDraft = '';
    }, 0);
  });
  state.sync = sync;
  state.dispose = () => {
    cleanups.forEach(off => off());
    clearTimeout(announcement); clearTimeout(resetTimer);
    restoreAttribute('tabindex', tabindex); restoreAttribute('aria-hidden', ariaHidden);
    labels.forEach(label => {
      if (label.htmlFor === editor.id) { label.htmlFor = source.id; delete label.dataset.swTagInputFor; }
    });
    control.remove(); instructions.remove(); status.remove();
    root.removeAttribute('data-enhanced');
  };
  render(); sync();
}
})();

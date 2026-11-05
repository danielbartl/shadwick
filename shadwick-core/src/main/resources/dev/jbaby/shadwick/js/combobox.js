/* Combobox adapted from Shadleaf (Apache-2.0, see NOTICE). Native selects own all submitted values. */
(function () {
'use strict';
const SEARCH_DELAY_MS = 300;
const ANNOUNCE_DELAY_MS = 500;
const PAGE_STEP = 10;
const MIRRORED = ['aria-describedby', 'aria-invalid', 'aria-label', 'aria-labelledby', 'aria-required', 'disabled', 'required', 'hidden', 'label', 'value', 'selected'];
let instances = 0;

const instancesByRoot = new Map();
const Shadwick = window.Shadwick = window.Shadwick || {};
function listener(cleanups) {
  return (node,type,handler,options) => { node.addEventListener(type,handler,options); cleanups.push(()=>node.removeEventListener(type,handler,options)); };
}
function hide(node) { if (node.matches(':popover-open')) node.hidePopover(); }
function show(node,source) {
  if (node.matches(':popover-open')) return;
  try { node.showPopover({source}); } catch (error) { if (error instanceof TypeError) node.showPopover(); else throw error; }
}
function scan() {
  instancesByRoot.forEach((state,root)=>{
    if (!root.isConnected || root.querySelector(':scope > select') !== state.select) { state.cleanups.forEach(off=>off()); instancesByRoot.delete(root); }
  });
  if (!HTMLElement.prototype.showPopover) return;
  document.querySelectorAll('.sw-combobox').forEach(root=>{
    const select=root.querySelector(':scope > select');
    if (!select || instancesByRoot.has(root)) return;
    const cleanups=[];
    const template=document.createElement('template');
    template.innerHTML='<div class="sw-combobox-control"><input class="sw-combobox-input" type="text" role="combobox" autocomplete="off" autocapitalize="off" spellcheck="false" aria-autocomplete="list" aria-expanded="false"><button type="button" class="sw-combobox-clear" tabindex="-1" hidden><svg class="sw-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="m18 6-12 12M6 6l12 12"/></svg></button><button type="button" class="sw-combobox-trigger" tabindex="-1" aria-expanded="false"><svg class="sw-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="m6 9 6 6 6-6"/></svg></button></div><div class="sw-combobox-content" popover="manual" data-side="bottom" data-align="start"><div class="sw-combobox-list" role="listbox"></div><div class="sw-combobox-empty"></div></div><div class="sw-combobox-status" role="status"></div><span class="sw-combobox-chip"><span class="sw-combobox-chip-label"></span><button type="button" class="sw-combobox-chip-remove" tabindex="-1"><svg class="sw-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="m18 6-12 12M6 6l12 12"/></svg></button></span><svg class="sw-icon sw-combobox-item-indicator" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M20 6 9 17l-5-5"/></svg>';
    const state={select,cleanups};
    instancesByRoot.set(root,state);
    setUp(root,select,template,listener(cleanups),cleanups,state);
  });
}
Shadwick.enhanceComboboxes=scan;
new MutationObserver(records=>{
  if (records.some(r=>r.type==='childList')) scan();
  if (records.some(r=>r.type==='attributes'&&r.target.tagName==='FIELDSET')) instancesByRoot.forEach(state=>state.sync?.());
}).observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['disabled']});
window.addEventListener('pagehide',()=>instancesByRoot.forEach(state=>state.close?.()));
window.addEventListener('pageshow',()=>{scan();instancesByRoot.forEach(state=>state.sync?.());});
scan();

function setUp(root, select, template, listen, cleanups, stateHandle) {
  const instance = ++instances;
  const id = select.id || `sw-combobox-${instance}`;
  select.id=id;
  // Markup from an earlier run, such as a page htmx restored from its history cache.
  root.querySelectorAll(':scope > .sw-combobox-control, :scope > .sw-combobox-content, :scope > .sw-combobox-status')
      .forEach(element => element.remove());

  const parts = template.content.cloneNode(true);
  const control = parts.querySelector('.sw-combobox-control');
  const input = control.querySelector('.sw-combobox-input');
  const clear = control.querySelector('.sw-combobox-clear');
  const trigger = control.querySelector('.sw-combobox-trigger');
  const content = parts.querySelector('.sw-combobox-content');
  const listbox = content.querySelector('.sw-combobox-list');
  const empty = content.querySelector('.sw-combobox-empty');
  const status = parts.querySelector('.sw-combobox-status');
  // Top-level elements of the template, which :scope cannot reach in a fragment.
  const chipPrototype = Array.from(parts.children).find(element => element.matches('.sw-combobox-chip'));
  const indicator = Array.from(parts.children).find(element => element.matches('.sw-combobox-item-indicator'));

  const multiple = select.multiple;
  const autoHighlight = root.dataset.autoHighlight === 'true';
  const searchUrl = root.dataset.searchUrl;
  const placeholder = root.dataset.placeholder || '';
  const emptyText = root.dataset.empty || 'No items found.';

  input.id = `${id}-input`;
  listbox.id = `${id}-listbox`;
  content.id = `${id}-content`;
  input.setAttribute('aria-controls', listbox.id);
  trigger?.setAttribute('aria-controls', listbox.id);
  if (multiple) {
    listbox.setAttribute('aria-multiselectable', 'true');
  }
  // Popup placement uses the control as an implicit CSS anchor.

  if (root.hasAttribute('data-enhanced')) {
    const original=select.dataset.swComboboxTabindex;
    if (original) select.setAttribute('tabindex',original); else select.removeAttribute('tabindex');
  }
  select.dataset.swComboboxTabindex=select.getAttribute('tabindex') || '';
  root.toggleAttribute('data-multiple',multiple);
  input.placeholder=root.dataset.placeholder || '';
  empty.textContent=root.dataset.empty || 'No items found.';
  clear.setAttribute('aria-label',root.dataset.clear || 'Clear selection');
  trigger.setAttribute('aria-label',root.dataset.toggle || 'Show options');
  if (root.dataset.showClear !== 'true') clear.remove();
  if (multiple) trigger.remove();
  root.append(control, content, status);
  select.tabIndex = -1;
  select.setAttribute('aria-hidden', 'true');
  root.dataset.enhanced = 'true';

  const optionOf = new Map();
  let rows = [];
  let active = null;
  // What was typed since the list opened; null while the list shows every option.
  let query = null;
  let state = 'idle';
  let searched = false;
  const initialOptions = searchUrl ? Array.from(select.children).map(child => child.cloneNode(true)) : [];
  const defaultValues = Array.from(select.options).filter(o=>o.defaultSelected).map(o=>o.value);
  let searchTimer = 0;
  let announceTimer = 0;
  let request = null;

  const isOpen = () => content.matches(':popover-open');
  const realOptions = () => Array.from(select.options).filter(option => option.dataset.placeholder === undefined);
  const chosenOptions = () => realOptions().filter(option => option.selected);
  const enabledRows = () => rows.filter(row => !row.hidden && row.getAttribute('aria-disabled') !== 'true');
  const visibleRows = () => rows.filter(row => !row.hidden);

  const render = () => {
    optionOf.clear();
    rows = [];
    listbox.replaceChildren();
    let index = 0;
    const addOption = (parent, option) => {
      if (option.hidden || option.closest('optgroup[hidden]')) {
        return;
      }
      const row = document.createElement('div');
      row.className = option.className || 'sw-combobox-item';
      row.id = `${listbox.id}-${index++}`;
      row.setAttribute('role', 'option');
      if (option.matches(':disabled') || option.parentElement?.disabled) {
        row.setAttribute('aria-disabled', 'true');
      }
      row.append(...contentOf(option));
      if (indicator) {
        row.append(indicator.cloneNode(true));
      }
      optionOf.set(row, option);
      rows.push(row);
      parent.append(row);
    };
    for (const child of select.children) {
      if (child instanceof HTMLOptionElement) {
        addOption(listbox, child);
      } else if (child instanceof HTMLOptGroupElement) {
        const group = document.createElement('div');
        group.className = child.className || 'sw-combobox-group';
        group.setAttribute('role', 'group');
        const label = document.createElement('div');
        label.className = 'sw-combobox-label';
        label.id = `${listbox.id}-group-${index}`;
        label.textContent = child.label;
        group.setAttribute('aria-labelledby', label.id);
        group.append(label);
        Array.from(child.children).filter(option => option instanceof HTMLOptionElement)
            .forEach(option => addOption(group, option));
        listbox.append(group);
      } else if (child instanceof HTMLHRElement) {
        const separator = document.createElement('div');
        separator.className = child.className || 'sw-combobox-separator';
        separator.setAttribute('aria-hidden', 'true');
        listbox.append(separator);
      }
    }
    active = null;
    filter();
  };

  /** Shows the rows that match the query (all of them when the server filtered), and the empty or busy message. */
  const filter = () => {
    const search = searchUrl || !query ? '' : normalize(query);
    for (const row of rows) {
      row.hidden = state !== 'idle' || search !== '' && !normalize(optionOf.get(row).label).includes(search);
    }
    listbox.querySelectorAll(':scope > [role="group"]').forEach(group => {
      group.hidden = !group.querySelector('[role="option"]:not([hidden])');
    });
    listbox.querySelectorAll(':scope > .sw-combobox-separator').forEach(separator => {
      separator.hidden = Boolean(query);
    });
    const count = visibleRows().length;
    const message = state === 'searching' ? root.dataset.searching
        : state === 'error' ? root.dataset.searchError
            : count === 0 ? emptyText : null;
    empty.textContent = message ?? emptyText;
    empty.hidden = message === null;
    listbox.hidden = count === 0;
    content.toggleAttribute('data-empty', count === 0);
    if (active && active.hidden) {
      active = null;
    }
    if (query && autoHighlight) {
      setActive(enabledRows()[0] ?? null);
    } else {
      setActive(active, false);
    }
    if (isOpen()) {
      content.dispatchEvent(new Event('sw-popup-place'));
    }
  };

  const announce = () => {
    clearTimeout(announceTimer);
    announceTimer = setTimeout(() => {
      const count = visibleRows().length;
      status.textContent = state === 'error' ? root.dataset.searchError
          : count === 0 ? emptyText
              : count === 1 ? root.dataset.result : root.dataset.results.replace('{0}', String(count));
    }, ANNOUNCE_DELAY_MS);
  };

  const setActive = (row, scroll = true) => {
    rows.forEach(each => each.toggleAttribute('data-active', each === row));
    if (row) {
      input.setAttribute('aria-activedescendant', row.id);
      if (scroll) {
        row.scrollIntoView({block: 'nearest'});
      }
    } else {
      input.removeAttribute('aria-activedescendant');
    }
    active = row;
  };

  const chosenRow = () => enabledRows().find(row => optionOf.get(row).selected);

  const sync = () => {
    const disabled = select.matches(':disabled');
    if (disabled && isOpen()) { hide(content); query=null; input.value=''; }
    input.disabled = disabled;
    // The chips fade with the control: text of a disabled control, which needs no contrast.
    if (disabled) {
      control.setAttribute('aria-disabled', 'true');
    } else {
      control.removeAttribute('aria-disabled');
    }
    for (const button of [clear, trigger]) {
      if (button) {
        button.disabled = disabled;
      }
    }
    for (const name of ['aria-describedby', 'aria-invalid']) {
      mirror(select, input, name);
    }
    if (select.required || select.getAttribute('aria-required') === 'true') input.setAttribute('aria-required','true');
    else input.removeAttribute('aria-required');
    const labelledBy = select.getAttribute('aria-labelledby') || (!select.hasAttribute('aria-label') ? labelIds(select,id) : '');
    for (const element of [input, listbox]) {
      if (labelledBy) {
        element.setAttribute('aria-labelledby', labelledBy);
        element.removeAttribute('aria-label');
      } else {
        element.removeAttribute('aria-labelledby');
        mirror(select, element, 'aria-label');
      }
    }
    const chosen = chosenOptions();
    if (multiple) {
      renderChips(chosen, disabled);
      input.placeholder = chosen.length ? '' : placeholder;
    } else if (query === null) {
      input.value = chosen[0] ? chosen[0].label : '';
    }
    if (clear) {
      clear.hidden = chosen.length === 0 && input.value === '';
    }
    rows.forEach(row => row.setAttribute('aria-selected', String(optionOf.get(row).selected)));
  };

  const renderChips = (chosen, disabled) => {
    // Removing a focused chip would blur it while it is removed, and focusout closes the list, rendering the chips.
    if (document.activeElement?.closest('.sw-combobox-chip') && control.contains(document.activeElement)) {
      input.focus({preventScroll: true});
    }
    control.querySelectorAll(':scope > .sw-combobox-chip').forEach(chip => chip.remove());
    if (!chipPrototype) {
      return;
    }
    for (const option of chosen) {
      const chip = chipPrototype.cloneNode(true);
      chip.querySelector('.sw-combobox-chip-label').textContent = option.label;
      const remove = chip.querySelector('.sw-combobox-chip-remove');
      remove.setAttribute('aria-label', root.dataset.removeLabel.replace('{0}', option.label));
      remove.disabled = disabled;
      chip.dataset.value = option.value;
      input.before(chip);
    }
  };

  const changed = () => {
    select.dispatchEvent(new Event('input', {bubbles: true}));
    select.dispatchEvent(new Event('change', {bubbles: true}));
  };

  const choose = row => {
    const option = row ? optionOf.get(row) : null;
    if (!option || option.matches(':disabled') || option.parentElement?.disabled || select.matches(':disabled')) {
      return;
    }
    if (multiple) {
      option.selected = !option.selected;
      changed();
      if (query !== null) {
        input.value = '';
        query = null;
        restoreOptions();
        filter();
      }
      setActive(rows.includes(row) ? row : null, false);
      return;
    }
    if (!option.selected) {
      option.selected = true;
      changed();
    }
    query = null;
    restoreOptions();
    close();
  };

  const clearChoice = () => {
    const chosen = chosenOptions();
    chosen.forEach(option => {
      option.selected = false;
    });
    const none = select.querySelector(':scope > option[data-placeholder]');
    if (none) {
      none.selected = true;
    }
    input.value = '';
    query = null;
    restoreOptions();
    if (chosen.length) {
      changed();
    }
    sync();
    filter();
  };

  const open = row => {
    if (input.disabled) {
      return;
    }
    if (!isOpen()) {
      show(content, control);
      input.setAttribute('aria-expanded','true'); trigger?.setAttribute('aria-expanded','true');
    }
    if (row !== undefined) {
      setActive(row);
    }
  };

  /** Closes the list. A single sw-combobox shows its choice again, or loses it when the text was emptied. */
  const close = () => {
    hide(content);
    input.setAttribute('aria-expanded','false'); trigger?.setAttribute('aria-expanded','false'); setActive(null,false);
    if (query !== null) {
      if (!multiple && input.value.trim() === '' && chosenOptions().length) {
        clearChoice();
        return;
      }
      if (multiple) {
        input.value = '';
      }
      query = null;
      restoreOptions();
    }
    sync();
    filter();
  };

  /** After a search, the options the page came with. */
  const restoreOptions = () => {
    if (searchUrl && searched) {
      searched = false;
      search('');
    }
  };

  const search = text => {
    clearTimeout(searchTimer);
    request?.abort();
    request = null;
    listbox.removeAttribute('aria-busy');
    if (text.trim() === '') {
      state = 'idle';
      replaceOptions(select, initialOptions.map(child => child.cloneNode(true)), false);
      return;
    }
    state = 'searching';
    searched = true;
    filter();
    searchTimer = setTimeout(() => {
      const url = new URL(searchUrl, document.baseURI);
      if (url.origin !== location.origin) { state='error'; filter(); announce(); return; }
      url.searchParams.set('q', text);
      const event = new CustomEvent('sw-combobox-search', {bubbles: true, cancelable: true, detail: {query: text, url}});
      if (!select.dispatchEvent(event)) {
        state = 'idle';
        filter();
        return;
      }
      if (event.detail.url.origin !== location.origin) { state='error'; filter(); announce(); return; }
      const controller = new AbortController();
      request = controller;
      listbox.setAttribute('aria-busy', 'true');
      fetch(event.detail.url, {headers: {Accept: 'application/json'}, credentials:'same-origin', cache:'no-store', signal: controller.signal})
          .then(response => {
            if (!response.ok || response.redirected) {
              throw new Error(`${response.status} from ${response.url}`);
            }
            if (!response.headers.get('content-type')?.includes('application/json')) throw new Error('Expected JSON');
            return response.json();
          })
          .then(results => {
            if (request !== controller) return;
            if (!Array.isArray(results) || results.length>200) throw new Error('Invalid sw-combobox results');
            const fragment=document.createDocumentFragment(); let previous=null, group=null; const seen=new Set();
            for (const item of results) {
              if (typeof item.id !== 'string' || !item.id || typeof item.text !== 'string' || seen.has(item.id)) throw new Error('Invalid sw-combobox option');
              seen.add(item.id);
              if (item.separator) { const separator=document.createElement('hr'); separator.className='sw-combobox-separator'; fragment.append(separator); group=null; previous=null; }
              if (item.group && (item.group!==previous || !group)) { group=document.createElement('optgroup'); group.label=String(item.group); group.className='sw-combobox-group'; fragment.append(group); }
              if (!item.group) group=null;
              previous=item.group;
              const option=new Option(item.text,item.id); option.disabled=!!item.disabled; option.className='sw-combobox-item';
              if (item.icon) {
                const drawing=new DOMParser().parseFromString(item.icon,'image/svg+xml').documentElement;
                // Keep drawing-only SVG. Endpoint data can never insert scripts, URLs or handlers.
                if (drawing.localName==='svg') { const safe=document.createElementNS('http://www.w3.org/2000/svg','svg'); for (const a of ['viewBox','fill','stroke','stroke-width','stroke-linecap','stroke-linejoin']) if (drawing.hasAttribute(a)) safe.setAttribute(a,drawing.getAttribute(a)); safe.setAttribute('class','sw-icon'); safe.setAttribute('aria-hidden','true'); safe.setAttribute('focusable','false'); for (const shape of drawing.children) if (['path','circle','rect','line','polyline','polygon','ellipse'].includes(shape.localName)) { const child=document.createElementNS('http://www.w3.org/2000/svg',shape.localName); for (const a of ['d','cx','cy','r','rx','ry','x','y','x1','x2','y1','y2','width','height','points']) if (shape.hasAttribute(a)) child.setAttribute(a,shape.getAttribute(a)); safe.append(child); } option.prepend(safe); }
              }
              (group || fragment).append(option);
            }
            state='idle'; replaceOptions(select,Array.from(fragment.children),true);
          })
          .catch(error => {
            if (error.name === 'AbortError' || request !== controller) {
              return;
            }
            state = 'error';
            filter();
            announce();
          })
          .finally(() => {
            if (request === controller) {
              request = null;
              listbox.removeAttribute('aria-busy');
            }
          });
    }, SEARCH_DELAY_MS);
  };

  render();
  sync();

  const observer = new MutationObserver(records => {
    const optionsChanged = records.some(record => record.type !== 'attributes' || record.target !== select);
    if (optionsChanged) {
      render();
      if (query) {
        announce();
      }
    }
    sync();
  });
  observer.observe(select, {attributes: true, attributeFilter: MIRRORED, childList: true, subtree: true,
    characterData: true});
  cleanups.push(() => {
    observer.disconnect();
    hide(content);
    clearTimeout(searchTimer);
    clearTimeout(announceTimer);
    request?.abort();
  });

  listen(document, 'pointerdown', event => { if (!root.contains(event.target)) close(); }, true);
  listen(document, 'beforetoggle', event => { if (event.newState === 'closed' && event.target !== content && event.target.contains(select)) close(); }, true);
  listen(document, 'close', event => { if (event.target.contains(select)) close(); }, true);
  stateHandle.sync=sync; stateHandle.close=close;

  listen(content, 'toggle', event => {
    const expanded = String(event.newState === 'open');
    input.setAttribute('aria-expanded', expanded);
    trigger?.setAttribute('aria-expanded', expanded);
    if (event.newState !== 'open') {
      setActive(null);
    }
  });

  listen(select, 'change', sync);
  listen(select, 'input', sync);
  listen(select, 'focus', () => input.focus());
  if (select.form) {
    // The reset event comes before the form resets its controls.
    listen(select.form, 'reset', event => setTimeout(() => {
      if (event.defaultPrevented || !select.isConnected) return;
      hide(content); input.setAttribute('aria-expanded','false');
      query = null;
      input.value = '';
      if (searchUrl) { clearTimeout(searchTimer); request?.abort(); request=null; state='idle'; searched=false; select.replaceChildren(...initialOptions.map(c=>c.cloneNode(true))); for (const o of select.options) o.selected=defaultValues.includes(o.value); if (!multiple && !defaultValues.length) select.selectedIndex=0; }
      sync();
      filter();
    }));
  }

  if (document.activeElement === select) {
    input.focus();
  }

  listen(input, 'input', event => {
    if (event.isComposing) return;
    query = input.value;
    if (clear) {
      clear.hidden = chosenOptions().length === 0 && input.value === '';
    }
    open();
    if (searchUrl) {
      search(query);
    } else {
      filter();
      announce();
    }
  });

  listen(input, 'compositionend', () => input.dispatchEvent(new Event('input',{bubbles:true})));
  listen(input, 'click', () => open());

  listen(input, 'keydown', event => {
    if (event.ctrlKey || event.metaKey || event.isComposing) {
      return;
    }
    const enabled = enabledRows();
    const index = enabled.indexOf(active);
    if (!isOpen()) {
      switch (event.key) {
        case 'ArrowDown':
          event.preventDefault();
          open(event.altKey ? null : chosenRow() ?? enabled[0] ?? null);
          return;
        case 'ArrowUp':
          event.preventDefault();
          open(chosenRow() ?? enabled.at(-1) ?? null);
          return;
        case 'Escape':
          if (multiple && input.value !== '') {
            event.preventDefault();
            input.value = '';
            query = '';
            close();
          }
          return;
        case 'Backspace':
          removeLastChip(event);
          return;
        case 'ArrowLeft':
          focusChipFromInput(event);
          return;
        default:
          return;
      }
    }

    let next;
    switch (event.key) {
      case 'ArrowDown':
        next = enabled[Math.min(index + 1, enabled.length - 1)];
        break;
      case 'ArrowUp':
        if (event.altKey) {
          event.preventDefault();
          if (active) {
            choose(active);
          }
          if (multiple || !active) {
            close();
          }
          return;
        }
        next = index < 0 ? enabled.at(-1) : enabled[Math.max(index - 1, 0)];
        break;
      case 'PageDown':
        next = enabled[Math.min(Math.max(index, 0) + PAGE_STEP, enabled.length - 1)];
        break;
      case 'PageUp':
        next = enabled[Math.max(index - PAGE_STEP, 0)];
        break;
      case 'Enter':
        event.preventDefault();
        if (active) choose(active);
        return;
      case 'Escape':
        // Not the dialog or sheet around the sw-combobox too.
        event.preventDefault();
        event.stopPropagation();
        close();
        return;
      case 'Tab':
        close();
        return;
      case 'Backspace':
        removeLastChip(event);
        return;
      case 'ArrowLeft':
        focusChipFromInput(event);
        return;
      default:
        return;
    }
    if (next) {
      event.preventDefault();
      setActive(next);
    }
  });

  listen(input, 'focusout', event => {
    if (!root.contains(event.relatedTarget)) {
      close();
    }
  });

  const chipButtons = () => Array.from(control.querySelectorAll(':scope > .sw-combobox-chip > .sw-combobox-chip-remove'));

  const removeChip = button => {
    const option = chosenOptions().find(each => each.value === button.parentElement.dataset.value);
    if (option) {
      option.selected = false;
      changed();
    }
  };

  function removeLastChip(event) {
    const last = chipButtons().at(-1);
    if (multiple && last && input.value === '' && !last.disabled) {
      event.preventDefault();
      removeChip(last);
    }
  }

  function focusChipFromInput(event) {
    const last = chipButtons().at(-1);
    if (multiple && last && input.selectionStart === 0 && input.selectionEnd === 0 && !last.disabled) {
      event.preventDefault();
      hide(content);
      last.focus();
    }
  }

  listen(control, 'keydown', event => {
    const button = event.target instanceof Element ? event.target.closest('.sw-combobox-chip-remove') : null;
    if (!button) {
      return;
    }
    const buttons = chipButtons();
    const index = buttons.indexOf(button);
    switch (event.key) {
      case 'ArrowLeft':
        event.preventDefault();
        buttons[Math.max(index - 1, 0)].focus();
        return;
      case 'ArrowRight':
        event.preventDefault();
        (buttons[index + 1] ?? input).focus();
        return;
      case 'Backspace':
      case 'Delete': {
        event.preventDefault();
        removeChip(button);
        const remaining = chipButtons();
        (remaining[Math.max(index - 1, 0)] ?? input).focus();
        return;
      }
      default:
    }
  });

  listen(control, 'focusout', event => {
    if (event.target !== input && !root.contains(event.relatedTarget)) {
      close();
    }
  });

  // The input keeps the focus while the pointer works the buttons and the list.
  listen(control, 'mousedown', event => {
    if (event.target !== input && !input.disabled) {
      event.preventDefault();
      if (document.activeElement !== input) {
        input.focus();
      }
    }
  });

  listen(control, 'click', event => {
    const target = event.target instanceof Element ? event.target : null;
    const remove = target?.closest('.sw-combobox-chip-remove');
    if (remove) {
      removeChip(remove);
      input.focus();
    } else if (clear && target?.closest('.sw-combobox-clear')) {
      clearChoice();
      input.focus();
    } else if (trigger && target?.closest('.sw-combobox-trigger')) {
      if (isOpen()) {
        close();
      } else {
        open(chosenRow() ?? null);
      }
    } else if (target !== input) {
      open();
    }
  });

  listen(content, 'mousedown', event => event.preventDefault());
  listen(content, 'pointermove', event => {
    const row = event.target instanceof Element ? event.target.closest('[role="option"]') : null;
    if (row && row !== active && row.getAttribute('aria-disabled') !== 'true') {
      setActive(row, false);
    }
  });
  listen(content, 'click', event => {
    const row = event.target instanceof Element ? event.target.closest('[role="option"]') : null;
    if (row && row.getAttribute('aria-disabled') !== 'true') {
      choose(row);
    }
  });
}

/**
 * Puts `children` (options, groups and separators) in the select in place of its options that are not chosen. A
 * chosen option keeps its place among them when they hold its value, and stays after them otherwise, so the select's
 * value does not change: hidden from the list for the results of a search (`hideKept`), shown with the page's options.
 */
function replaceOptions(select, children, hideKept) {
  const chosen = Array.from(select.options).filter(option => option.selected && option.dataset.placeholder === undefined);
  const chosenValues = new Set(chosen.map(option => option.value));
  const placeholder = select.querySelector(':scope > option[data-placeholder]');
  const fragment = document.createDocumentFragment();
  const found = new Set();
  for (const child of children) {
    if (!(child instanceof HTMLOptionElement || child instanceof HTMLOptGroupElement || child instanceof HTMLHRElement)) {
      continue;
    }
    const options = child instanceof HTMLOptionElement ? [child] : Array.from(child.querySelectorAll('option'));
    options.forEach(option => {
      if (chosenValues.has(option.value)) {
        found.add(option.value);
      }
      option.removeAttribute('selected');
    });
    fragment.append(child);
  }
  const kept = chosen.filter(option => !found.has(option.value));
  kept.forEach(option => {
    option.hidden = hideKept;
  });
  select.replaceChildren(...(placeholder ? [placeholder] : []), fragment, ...kept);
  for (const option of select.options) {
    if (chosenValues.has(option.value) && option.dataset.placeholder === undefined) {
      option.selected = true;
    }
  }
  if (!select.multiple && chosen.length === 0 && placeholder) {
    placeholder.selected = true;
  }
}

/** Read each time, so a test can switch it off. */

/** Lower case, without accents: what the filter compares. */
function normalize(text) {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim();
}

/** An option's content, or its text when the browser kept none of its markup. */
function contentOf(option) {
  const nodes = Array.from(option.childNodes).map(node => node.cloneNode(true));
  if (!nodes.some(node => node.textContent.trim() !== '' || node instanceof Element)) {
    return [document.createTextNode(option.label)];
  }
  return nodes;
}

function mirror(from, to, name) {
  const value = from.getAttribute(name);
  if (value === null) {
    to.removeAttribute(name);
  } else {
    to.setAttribute(name, value);
  }
}

/** The ids of the select's labels, given ids where they have none. */
function labelIds(select, id) {
  return Array.from(select.labels ?? []).map((label, index) => {
    if (!label.id) {
      label.id = `${id}-label${index === 0 ? '' : index + 1}`;
    }
    return label.id;
  }).join(' ');
}

})();

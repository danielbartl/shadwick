/* Custom select, adapted from Shadleaf's select runtime (Apache-2.0, see NOTICE).
   The real select owns the value, validation and events. No inline positioning styles. */
(function () {
    'use strict';
    var Shadwick = window.Shadwick = window.Shadwick || {};
    var states = new Map(), nextId = 0;
    var baseSelect = window.CSS && CSS.supports('appearance', 'base-select');
    function open(node) { return node.matches(':popover-open'); }
    function disabled(option) { return option.disabled || option.parentElement.tagName === 'OPTGROUP' && option.parentElement.disabled; }
    function visible(option) { return !option.hidden && !option.closest('optgroup[hidden]'); }
    function copyContent(option, target) {
        target.replaceChildren();
        if (!option) return;
        Array.from(option.childNodes).forEach(function (child) {
            if (!(child instanceof Element) || !child.classList.contains('sw-select-item-indicator')) target.append(child.cloneNode(true));
        });
    }
    function init(wrapper, select) {
        if (!select.id) select.id = 'sw-select-' + (++nextId);
        // Cloned enhanced markup has no listeners. Keep only the native source and static adornments.
        wrapper.querySelectorAll(':scope > [data-sw-select-generated]').forEach(function (node) { node.remove(); });
        var savedTab = select.getAttribute('data-sw-select-tabindex');
        if (wrapper.hasAttribute('data-enhanced')) {
            if (savedTab === '') select.removeAttribute('tabindex');
            else if (savedTab !== null) select.setAttribute('tabindex', savedTab);
            select.removeAttribute('aria-hidden'); wrapper.removeAttribute('data-enhanced');
        }
        var listeners = [], state = {select: select, cleanup: function () {
            listeners.forEach(function (off) { off(); });
            if (state.observer) state.observer.disconnect();
        }};
        function listen(node, type, handler) {
            node.addEventListener(type, handler);
            listeners.push(function () { node.removeEventListener(type, handler); });
        }
        if (baseSelect) {
            listen(select, 'keydown', function (event) {
                if (event.target === select && !event.altKey && !event.ctrlKey && !event.metaKey && !select.matches(':open') && !select.matches(':disabled') && ['Enter','Home','End'].includes(event.key) && select.showPicker) {
                    event.preventDefault();
                    try { select.showPicker(); } catch (error) { return; }
                    var options = Array.from(select.options).filter(function (o) { return visible(o) && !disabled(o); });
                    var option = event.key === 'Home' ? options[0] : event.key === 'End' ? options[options.length - 1] : null;
                    if (option) option.focus();
                }
            });
            listen(select, 'pointermove', function (event) {
                var option = event.target.closest('option');
                if (event.pointerType === 'mouse' && option && !disabled(option) && select.matches(':open') && document.activeElement !== option) option.focus({preventScroll: true});
            });
            if (select.form) listen(select.form, 'reset', function (event) {
                setTimeout(function () { if (!event.defaultPrevented && select.isConnected) select.selectedIndex = select.selectedIndex; }, 0);
            });
            return state;
        }
        if (!HTMLElement.prototype.showPopover) return state;
        var trigger = document.createElement('button');
        trigger.type = 'button'; trigger.className = 'sw-select'; trigger.id = select.id + '-trigger';
        trigger.dataset.swSelectGenerated = '';
        trigger.setAttribute('role', 'combobox'); trigger.setAttribute('aria-haspopup', 'listbox');
        trigger.setAttribute('aria-expanded', 'false');
        var value = document.createElement('span'); value.className = 'sw-select-value'; trigger.append(value);
        var list = document.createElement('div');
        list.className = 'sw-select-content'; list.id = select.id + '-listbox';
        list.dataset.swSelectGenerated = ''; list.dataset.side = 'bottom'; list.dataset.align = 'start';
        list.setAttribute('role', 'listbox'); list.setAttribute('popover', 'auto');
        trigger.setAttribute('aria-controls', list.id); trigger.setAttribute('popovertarget', list.id);
        select.dataset.swSelectTabindex = select.getAttribute('tabindex') || '';
        select.tabIndex = -1; select.setAttribute('aria-hidden', 'true');
        select.querySelectorAll(':scope > button').forEach(function (button) { button.tabIndex = -1; });
        wrapper.dataset.enhanced = ''; wrapper.append(trigger, list);
        var rows = [], active = null, prefix = '', typedAt = 0;
        function close() { if (open(list)) list.hidePopover(); trigger.setAttribute('aria-expanded', 'false'); trigger.removeAttribute('aria-activedescendant'); active = null; prefix = ''; typedAt = 0; }
        function enabledRows() { return rows.filter(function (row) { return !disabled(row.option); }); }
        function activate(row) {
            rows.forEach(function (entry) { entry.node.toggleAttribute('data-active', entry === row); });
            active = row;
            if (row) { trigger.setAttribute('aria-activedescendant', row.node.id); row.node.scrollIntoView({block:'nearest'}); }
            else trigger.removeAttribute('aria-activedescendant');
        }
        function show(edge) {
            if (select.matches(':disabled') || !rows.length) return;
            if (!open(list)) {
                try { list.showPopover({source:trigger}); } catch (error) { if (error instanceof TypeError) list.showPopover(); else throw error; }
            }
            trigger.setAttribute('aria-expanded', 'true');
            var enabled = enabledRows();
            activate(edge === 'first' ? enabled[0] : edge === 'last' ? enabled[enabled.length-1]
                : enabled.find(function (row) { return row.option.selected; }) || enabled[0]);
        }
        function choose() {
            if (!active || select.matches(':disabled') || disabled(active.option)) return;
            var index = active.option.index, changed = select.selectedIndex !== index;
            select.selectedIndex = index;
            close(); sync();
            if (changed) {
                select.dispatchEvent(new Event('input', {bubbles:true}));
                select.dispatchEvent(new Event('change', {bubbles:true}));
            }
        }
        function name() {
            var labelled = select.getAttribute('aria-labelledby'), label = select.getAttribute('aria-label');
            trigger.removeAttribute('aria-labelledby'); trigger.removeAttribute('aria-label');
            list.removeAttribute('aria-labelledby'); list.removeAttribute('aria-label');
            if (!labelled && !label) labelled = Array.from(select.labels || []).map(function (node, index) {
                if (!node.id) node.id = select.id + '-label-' + index;
                return node.id;
            }).join(' ');
            if (labelled) { trigger.setAttribute('aria-labelledby', labelled); list.setAttribute('aria-labelledby', labelled); }
            else if (label) { trigger.setAttribute('aria-label', label); list.setAttribute('aria-label', label); }
        }
        function sync() {
            trigger.disabled = select.matches(':disabled');
            ['aria-describedby','aria-invalid','aria-required'].forEach(function (attr) {
                if (select.hasAttribute(attr)) trigger.setAttribute(attr, select.getAttribute(attr));
                else trigger.removeAttribute(attr);
            });
            if (select.required) trigger.setAttribute('aria-required', 'true');
            name();
            var selected = select.options[select.selectedIndex];
            copyContent(selected, value);
            trigger.toggleAttribute('data-placeholder', !!selected && selected.hasAttribute('data-placeholder'));
            rows.forEach(function (row) {
                row.node.setAttribute('aria-selected', String(row.option.selected));
                row.node.setAttribute('aria-disabled', String(disabled(row.option)));
            });
            if (trigger.disabled) close();
        }
        state.sync = sync;
        function build() {
            close(); list.replaceChildren(); rows = [];
            function item(option, parent) {
                if (!visible(option)) return;
                var node = document.createElement('div'); node.className = 'sw-select-item';
                node.id = list.id + '-option-' + option.index; node.setAttribute('role', 'option');
                copyContent(option, node);
                var indicator = option.querySelector('.sw-select-item-indicator');
                if (indicator) node.append(indicator.cloneNode(true));
                else {
                    var mark = document.createElement('span'); mark.className = 'sw-select-item-indicator'; mark.textContent = '✓'; mark.setAttribute('aria-hidden','true'); node.append(mark);
                }
                rows.push({option:option, node:node}); parent.append(node);
            }
            Array.from(select.children).forEach(function (child) {
                if (child.tagName === 'OPTION') item(child, list);
                else if (child.tagName === 'OPTGROUP' && !child.hidden) {
                    var group = document.createElement('div'), heading = document.createElement('div');
                    group.setAttribute('role','group'); heading.className = 'sw-select-label';
                    heading.textContent = child.label; heading.id = list.id + '-group-' + list.children.length;
                    group.setAttribute('aria-labelledby',heading.id); group.append(heading);
                    Array.from(child.children).forEach(function (option) { if (option.tagName === 'OPTION') item(option,group); }); list.append(group);
                } else if (child.tagName === 'HR') {
                    var separator = document.createElement('div'); separator.className = 'sw-select-separator'; separator.setAttribute('role','presentation'); separator.setAttribute('aria-hidden','true'); list.append(separator);
                }
            });
            sync();
        }
        listen(trigger, 'click', function (event) { event.preventDefault(); if (open(list)) close(); else show(); });
        listen(trigger, 'keydown', function (event) {
            var key = event.key, isOpen = open(list);
            if (key === 'Escape' && isOpen) { event.preventDefault(); event.stopPropagation(); close(); return; }
            if (key === 'Tab') { if (isOpen) { choose(); close(); } return; }
            if (['ArrowDown','ArrowUp','Home','End','PageDown','PageUp','Enter',' '].includes(key)) {
                event.preventDefault(); event.stopPropagation();
                if (!isOpen) { show(key === 'Home' ? 'first' : key === 'End' ? 'last' : null); return; }
                if (key === 'Enter' || key === ' ' || key === 'ArrowUp' && event.altKey) { choose(); close(); return; }
                var enabled = enabledRows(), current = enabled.indexOf(active), index;
                if (key === 'Home') index = 0;
                else if (key === 'End') index = enabled.length - 1;
                else index = current + (key === 'ArrowUp' ? -1 : key === 'PageUp' ? -10 : key === 'PageDown' ? 10 : 1);
                activate(enabled[Math.max(0,Math.min(enabled.length-1,index))]); return;
            }
            if (key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
                event.preventDefault();
                var now = Date.now(); prefix = now - typedAt < 500 ? prefix + key.toLocaleLowerCase() : key.toLocaleLowerCase(); typedAt = now;
                var search = Array.from(prefix).every(function (c) { return c === prefix[0]; }) ? prefix[0] : prefix;
                var enabled = enabledRows(), start = enabled.indexOf(active);
                var candidates = search.length === 1 ? enabled.slice(start+1).concat(enabled.slice(0,start+1)) : enabled;
                var found = candidates.find(function (row) { return row.option.textContent.trim().toLocaleLowerCase().startsWith(search); });
                if (found) { if (!isOpen) show(); activate(found); }
            }
        });
        listen(list, 'mousedown', function (event) { event.preventDefault(); });
        listen(list, 'pointermove', function (event) {
            var row = rows.find(function (entry) { return entry.node.contains(event.target); });
            if (row && !disabled(row.option)) activate(row);
        });
        listen(list, 'click', function (event) {
            var row = rows.find(function (entry) { return entry.node.contains(event.target); });
            if (row && !disabled(row.option)) { activate(row); choose(); trigger.focus({preventScroll:true}); }
        });
        listen(list, 'beforetoggle', function (event) {
            if (event.newState === 'closed') { trigger.setAttribute('aria-expanded','false'); trigger.removeAttribute('aria-activedescendant'); active = null; prefix = ''; typedAt = 0; }
        });
        listen(select, 'focus', function () { trigger.focus({preventScroll:true}); });
        listen(select, 'input', sync); listen(select, 'change', sync);
        if (select.form) listen(select.form, 'reset', function (event) {
            setTimeout(function () { if (!event.defaultPrevented && select.isConnected) { close(); sync(); } },0);
        });
        state.observer = new MutationObserver(function (records) {
            if (records.some(function (record) { return record.type !== 'attributes' || record.target !== select; })) build();
            else sync();
        });
        state.observer.observe(select,{subtree:true,childList:true,characterData:true,attributes:true,
            attributeFilter:['disabled','required','aria-invalid','aria-describedby','aria-required','aria-labelledby','aria-label','label','selected','hidden','value']});
        build(); return state;
    }
    function scan() {
        states.forEach(function (state, wrapper) {
            if (!wrapper.isConnected || wrapper.querySelector(':scope > select.sw-select') !== state.select) {
                state.cleanup(); states.delete(wrapper);
            }
        });
        document.querySelectorAll('.sw-select-wrapper').forEach(function (wrapper) {
            var select = wrapper.querySelector(':scope > select.sw-select');
            if (select && !select.multiple && !states.has(wrapper)) states.set(wrapper,init(wrapper,select));
        });
    }
    Shadwick.enhanceSelects = scan;
    new MutationObserver(function (records) {
        if (records.some(function (record) { return record.type === 'childList'; })) scan();
        if (records.some(function (record) { return record.type === 'attributes' && record.target.tagName === 'FIELDSET'; }))
            states.forEach(function (state) { if (state.sync) state.sync(); });
    }).observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['disabled']});
    window.addEventListener('pagehide',function () {
        if (!HTMLElement.prototype.hidePopover) return;
        document.querySelectorAll('.sw-select-content:popover-open').forEach(function (list) { list.hidePopover(); });
    });
    window.addEventListener('pageshow',function () { scan(); states.forEach(function (state) { if (state.sync) state.sync(); }); });
    scan();
})();

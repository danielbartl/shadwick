/* Browser color preference, adapted from Shadleaf's theme bootstrap/toggle (Apache-2.0, see NOTICE).
 * External and parser-blocking on html; no inline script, style attributes, framework or server callback. */
(function () {
    'use strict';
    if (window.ShadwickTheme) return;
    var roots = new Map();
    var system = window.matchMedia('(prefers-color-scheme: dark)');
    var printMedia = window.matchMedia('print');
    var printing = printMedia.matches;
    function valid(value) { return value === 'light' || value === 'dark' || value === 'system'; }
    function rootOf(toggle) {
        var id = toggle.getAttribute('data-sw-theme-target');
        return id ? document.getElementById(id) : toggle.closest('[data-sw-theme-root]') || document.documentElement;
    }
    function stateFor(root) {
        var state = roots.get(root);
        if (state) return state;
        state = {root: root, fallback: valid(root.dataset.swThemeDefault) ? root.dataset.swThemeDefault : 'system',
            key: root.dataset.swThemeStorageKey || 'shadwick-theme', remember: root.dataset.swThemeRemember !== 'false', mode: null};
        roots.set(root, state);
        state.mode = load(state);
        apply(state, false);
        return state;
    }
    function load(state) {
        if (!state.remember) return state.mode || state.fallback;
        try {
            var stored = window.localStorage.getItem(state.key);
            return valid(stored) ? stored : state.fallback;
        } catch (error) { return state.mode || state.fallback; }
    }
    function dark(state) { return !printing && (state.mode === 'dark' || state.mode === 'system' && system.matches); }
    function sync(state) {
        document.querySelectorAll('.sw-theme-toggle').forEach(function (toggle) {
            if (rootOf(toggle) !== state.root) return;
            toggle.setAttribute('data-sw-theme-dark', String(dark(state)));
            toggle.setAttribute('data-sw-theme-ready', '');
            toggle.querySelectorAll('[data-sw-theme-choice]').forEach(function (item) {
                if (item.getAttribute('role') === 'menuitemradio') {
                    item.setAttribute('aria-checked', String(item.dataset.swThemeChoice === state.mode));
                }
            });
            toggle.querySelectorAll('.sw-theme-toggle-button').forEach(function (button) {
                button.setAttribute('aria-pressed', String(dark(state)));
            });
        });
    }
    function apply(state, announce) {
        var isDark = dark(state);
        state.root.classList.toggle('dark', isDark);
        state.root.dataset.swThemeMode = state.mode;
        state.root.dataset.swThemeResolved = isDark ? 'dark' : 'light';
        sync(state);
        if (announce) state.root.dispatchEvent(new CustomEvent('sw-theme-change', {bubbles: true,
            detail: {mode: state.mode, dark: isDark, rootId: state.root.id || null}}));
    }
    function choose(mode, root) {
        if (!valid(mode)) throw new TypeError('Theme mode must be light, dark or system');
        var state = stateFor(root);
        state.mode = mode;
        if (state.remember) {
            try { window.localStorage.setItem(state.key, mode); } catch (error) { /* This page still follows the choice. */ }
        }
        apply(state, true);
        // Two independent roots using one storage key represent the same preference in this document.
        if (state.remember) roots.forEach(function (other) {
            if (other !== state && other.remember && other.key === state.key) { other.mode = mode; apply(other, true); }
        });
    }
    function resolve(target) {
        var root = typeof target === 'string' ? document.getElementById(target) : target || document.documentElement;
        if (!(root instanceof Element)) throw new TypeError('A theme root must be an existing element');
        return root;
    }
    window.ShadwickTheme = {
        getMode: function (target) { return stateFor(resolve(target)).mode; },
        isDark: function (target) { return dark(stateFor(resolve(target))); },
        setMode: function (mode, target) { choose(mode, resolve(target)); },
        refresh: function (target) { var state = stateFor(resolve(target)); state.mode = load(state); apply(state, true); }
    };
    function scan() {
        roots.forEach(function (state, root) { if (!root.isConnected) roots.delete(root); });
        document.querySelectorAll('[data-sw-theme-root]').forEach(stateFor);
        document.querySelectorAll('.sw-theme-toggle').forEach(function (toggle) {
            var root = rootOf(toggle);
            if (root) sync(stateFor(root));
        });
    }
    document.addEventListener('click', function (event) {
        if (!(event.target instanceof Element)) return;
        var control = event.target.closest('[data-sw-theme-choice],.sw-theme-toggle-button');
        var toggle = control && control.closest('.sw-theme-toggle');
        if (!toggle || control.matches(':disabled,[aria-disabled="true"]') || control.closest('[inert]')) return;
        var root = rootOf(toggle);
        if (!root) return;
        var state = stateFor(root);
        var mode = control.hasAttribute('data-sw-theme-choice') ? control.dataset.swThemeChoice : dark(state) ? 'light' : 'dark';
        if (!valid(mode)) return;
        choose(mode, root);
        var menu = control.closest('.sw-dropdown-menu-content');
        if (menu && menu.matches(':popover-open')) {
            if (window.Shadwick && window.Shadwick.closePopover) window.Shadwick.closePopover(menu.id);
            else {
                menu.hidePopover();
                var trigger = toggle.querySelector('[popovertarget]');
                if (trigger) trigger.focus();
            }
        }
    });
    system.addEventListener('change', function () {
        roots.forEach(function (state) { if (state.mode === 'system') apply(state, true); });
    });
    window.addEventListener('storage', function (event) {
        try { if (event.storageArea && event.storageArea !== window.localStorage) return; } catch (error) { return; }
        roots.forEach(function (state) {
            if (state.remember && (event.key === state.key || event.key === null)) { state.mode = load(state); apply(state, true); }
        });
    });
    window.addEventListener('pageshow', function (event) {
        if (!event.persisted) return;
        roots.forEach(function (state) { state.mode = load(state); apply(state, true); });
        scan();
    });
    function print(active) {
        if (printing === active) return;
        printing = active;
        roots.forEach(function (state) { apply(state, true); });
    }
    window.addEventListener('beforeprint', function () { print(true); });
    window.addEventListener('afterprint', function () { print(false); });
    printMedia.addEventListener('change', function (event) { print(event.matches); });
    // html attributes already exist when this priority script runs, before styles and body parsing.
    if (document.documentElement.hasAttribute('data-sw-theme-root')) stateFor(document.documentElement);
    new MutationObserver(scan).observe(document.documentElement, {subtree: true, childList: true});
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', scan); else scan();
})();

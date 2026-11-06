/* Ajax busy controls; Wicket listener lifecycle follows Wicket Oat (MIT, see NOTICE). */
(function () {
    'use strict';
    var api = window.Shadwick = window.Shadwick || {};
    if (api.busy) return;
    var active = new Map();
    function restore(element, name, value) {
        if (value === null) element.removeAttribute(name); else element.setAttribute(name, value);
    }
    function finish(state) {
        if (!state || active.get(state.element) !== state) return;
        active.delete(state.element);
        restore(state.element, 'aria-busy', state.busy);
        restore(state.element, 'aria-disabled', state.disabled);
        restore(state.element, 'data-sw-busy', state.mark);
        state.indicator.remove();
    }
    api.busy = {
        start: function (attrs) {
            attrs.swBusyRejected = false; attrs.swBusyState = null;
            var element = document.getElementById(attrs.c);
            if (!element) return;
            if (active.has(element) || element.disabled || element.getAttribute('aria-disabled') === 'true'
                    || element.getAttribute('aria-busy') === 'true') {
                attrs.swBusyRejected = true;
                return;
            }
            var indicator = document.createElement('span');
            indicator.className = 'sw-busy-indicator';
            var spinner = document.createElement('span');
            spinner.className = 'sw-busy-spinner'; spinner.setAttribute('aria-hidden', 'true');
            var text = document.createElement('span');
            text.className = 'sw-busy-label'; text.textContent = element.dataset.swBusyLabel || 'Loading';
            indicator.append(spinner, text);
            var state = {element: element, indicator: indicator, busy: element.getAttribute('aria-busy'),
                disabled: element.getAttribute('aria-disabled'), mark: element.getAttribute('data-sw-busy')};
            active.set(element, state); attrs.swBusyState = state;
            element.setAttribute('aria-busy', 'true'); element.setAttribute('aria-disabled', 'true');
            element.setAttribute('data-sw-busy', 'true');
            // Keep native submit controls enabled so Wicket can serialize the submitter and its form.
            if (element.tagName === 'INPUT') {
                indicator.classList.add('sw-busy-input-indicator'); indicator.setAttribute('role', 'status'); element.after(indicator);
            } else element.prepend(indicator);
        },
        done: function (attrs) { finish(attrs.swBusyState); }
    };
    window.addEventListener('pageshow', function (event) { if (event.persisted) active.forEach(finish); });
})();

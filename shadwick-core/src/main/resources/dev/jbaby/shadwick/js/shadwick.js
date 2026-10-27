/*
 * Shadwick's script, for the few components that need one: form fields showing an error after an Ajax request
 * (BaseSwField) and dialogs (SwDialog). It only reads data- attributes and listens on the document, so it works for
 * markup Wicket adds or replaces over Ajax, and pages keep Wicket's strict, nonce-based CSP: no inline scripts or
 * event handlers.
 */
(function () {
    'use strict';
    if (window.Shadwick) {
        return;
    }
    window.Shadwick = {};
})();

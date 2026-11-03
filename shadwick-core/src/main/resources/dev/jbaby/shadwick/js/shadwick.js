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
    var Shadwick = window.Shadwick = {};

    // ---- Avatar images: delegated load/error handlers also cover Ajax replacements ----
    // Adapted from Shadleaf's avatar.js (Apache-2.0, see NOTICE).
    var avatarSelector = 'img.sw-avatar-image';
    function markFinished(image) {
        if (!image.complete || !image.getAttribute('src')) {
            return;
        }
        var src = image.currentSrc;
        image.decode().then(function () {
            if (image.currentSrc === src) { image.dataset.status = 'loaded'; }
        }, function () {
            if (image.currentSrc === src) { image.dataset.status = 'error'; }
        });
    }
    ['load', 'error'].forEach(function (type) {
        document.addEventListener(type, function (event) {
            if (event.target instanceof HTMLImageElement && event.target.matches(avatarSelector)) {
                event.target.dataset.status = type === 'load' ? 'loaded' : 'error';
            }
        }, true);
    });
    function scanAvatars(root) {
        if (root instanceof Element && root.matches(avatarSelector)) { markFinished(root); }
        if (root.querySelectorAll) { root.querySelectorAll(avatarSelector).forEach(markFinished); }
    }
    new MutationObserver(function (records) {
        records.forEach(function (record) {
            if (record.type === 'attributes' && record.target.matches(avatarSelector)) {
                delete record.target.dataset.status;
                markFinished(record.target);
            }
            record.addedNodes.forEach(scanAvatars);
        });
    }).observe(document.documentElement, {subtree: true, childList: true, attributes: true, attributeFilter: ['src', 'srcset']});
    scanAvatars(document);

    // ---- Feedback toasts: text-only, persistent and keyboard dismissible ----
    Shadwick.toast = function (message, level, dismissLabel) {
        var viewport = document.getElementById('sw-toaster');
        if (!viewport) {
            viewport = document.createElement('div');
            viewport.id = 'sw-toaster';
            viewport.className = 'sw-toaster';
            viewport.setAttribute('popover', 'manual');
            document.body.appendChild(viewport);
        }
        var toast = document.createElement('div');
        var returnFocus = document.activeElement;
        toast.className = 'sw-toast';
        toast.dataset.level = level;
        var severe = level === 'error' || level === 'fatal';
        if (severe) { toast.dataset.variant = 'destructive'; }
        var text = document.createElement('div');
        text.className = 'sw-toast-content';
        text.setAttribute('role', severe ? 'alert' : 'status');
        text.setAttribute('aria-atomic', 'true');
        toast.appendChild(text);
        var close = document.createElement('button');
        close.type = 'button';
        close.className = 'sw-button sw-toast-close';
        close.dataset.variant = 'ghost';
        close.dataset.size = 'icon';
        close.setAttribute('aria-label', dismissLabel);
        close.textContent = '\u00d7';
        close.addEventListener('click', function () {
            var focused = toast.contains(document.activeElement);
            var next = toast.nextElementSibling || toast.previousElementSibling;
            toast.remove();
            if (!viewport.children.length) {
                if (viewport.hidePopover) { viewport.hidePopover(); }
                viewport.remove();
                if (focused && returnFocus && returnFocus.isConnected) { returnFocus.focus(); }
            } else if (focused && next) {
                next.querySelector('button').focus();
            }
        });
        toast.appendChild(close);
        viewport.appendChild(toast);
        if (viewport.showPopover) { viewport.showPopover(); }
        else { viewport.dataset.fallback = ''; }
        // Insert the live region before its text so assistive technology can announce the update.
        requestAnimationFrame(function () { text.textContent = message; });
    };

    // ---- Form fields: an error that appeared, changed or went away (BaseSwField) ----

    // Sets the text of the error rather than replacing it, so screen readers announce the
    // change in its role="status" region, and leaves the inputs, their focus and what the
    // user typed alone
    Shadwick.fieldFeedback = function (fieldId, errorId, text, invalid) {
        var field = document.getElementById(fieldId);
        var error = document.getElementById(errorId);
        if (!field || !error) {
            return;
        }
        error.textContent = text;
        var container = field.querySelector('.sw-field');
        if (container) {
            if (invalid) {
                container.setAttribute('data-invalid', 'true');
            } else {
                container.removeAttribute('data-invalid');
            }
        }
        field.querySelectorAll('[aria-describedby~="' + errorId + '"]').forEach(function (input) {
            input.setAttribute('aria-invalid', invalid ? 'true' : 'false');
        });
    };

    // ---- Dialogs (SwDialog) ----

    // Shows a dialog rendered while open. One that was open already (data-sw-instant) shows
    // at once, without fading in again; after that, closing it animates.
    Shadwick.showDialog = function (id) {
        var dialog = document.getElementById(id);
        if (!dialog) {
            return;
        }
        if (!dialog.open) {
            dialog.showModal();
        }
        requestAnimationFrame(function () {
            requestAnimationFrame(function () {
                dialog.removeAttribute('data-sw-instant');
            });
        });
    };

    // Invoker commands (commandfor/command) where a browser lacks them: what a button
    // pointing at a dialog does in one that has them
    if (!('commandForElement' in HTMLButtonElement.prototype)) {
        document.addEventListener('click', function (event) {
            var button = event.target instanceof Element ? event.target.closest('button[commandfor]') : null;
            if (!button || button.disabled) {
                return;
            }
            var dialog = document.getElementById(button.getAttribute('commandfor'));
            if (!(dialog instanceof HTMLDialogElement)) {
                return;
            }
            var command = button.getAttribute('command');
            if (command === 'show-modal' && !dialog.open) {
                dialog.showModal();
            } else if ((command === 'close' || command === 'request-close') && dialog.open) {
                dialog.close(button.value);
            }
        });
    }

    // closedby="any" where a browser lacks it: a click that both starts and ends outside the
    // panel closes it. A click on the backdrop reaches the dialog itself, as does one on its
    // padding, so the position decides; where it started keeps a text selection dragged out
    // of the dialog from closing it.
    if (!('closedBy' in HTMLDialogElement.prototype)) {
        var outside = function (dialog, event) {
            var rect = dialog.getBoundingClientRect();
            return event.clientX < rect.left || event.clientX > rect.right
                || event.clientY < rect.top || event.clientY > rect.bottom;
        };
        var closesOnClickOutside = function (target) {
            return target instanceof HTMLDialogElement && target.open && target.getAttribute('closedby') === 'any';
        };
        var startedOutside = null;
        document.addEventListener('pointerdown', function (event) {
            startedOutside = closesOnClickOutside(event.target) && outside(event.target, event) ? event.target : null;
        });
        document.addEventListener('click', function (event) {
            var dialog = event.target;
            if (startedOutside === dialog && closesOnClickOutside(dialog) && outside(dialog, event)) {
                dialog.close();
            }
            startedOutside = null;
        });
    }

    // Without Cancel and Confirm (setFooterVisible(false)), Enter in a field submits nothing,
    // where the browser would submit the dialog's form, or a page's form around it
    document.addEventListener('keydown', function (event) {
        var input = event.target;
        if (event.key !== 'Enter' || !input.matches
            || !input.matches('input:not([type=submit], [type=button], [type=reset], [type=image])')) {
            return;
        }
        var form = input.closest('.sw-dialog-form');
        if (form && !form.querySelector(':scope > .sw-dialog-footer')) {
            event.preventDefault();
        }
    });
})();

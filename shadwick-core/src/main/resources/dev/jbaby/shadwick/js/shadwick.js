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

    // Input-group addon focus, adapted from Shadleaf (Apache-2.0, see NOTICE).
    // Delegation also covers groups inserted or replaced over Ajax.
    document.addEventListener('click', function (event) {
        if (!(event.target instanceof Element)) return;
        var addon = event.target.closest('.sw-input-group-addon');
        var interactive = event.target.closest('a[href], button, input, select, textarea, label, summary, [tabindex], [contenteditable]');
        if (!addon || (interactive && addon.contains(interactive))) return;
        var control = addon.parentElement.querySelector(':scope > :is(.sw-input-group-input, .sw-input-group-textarea)');
        if (control && !control.disabled) control.focus();
    });

    // Native file upload enhancement. Adapted from Shadleaf (Apache-2.0, see NOTICE).
    // The input's FileList is submitted by Wicket; files are never encoded into page state.
    var uploadStates = new WeakMap();
    function uploadMessage(root, key, values) {
        return (root.getAttribute('data-message-' + key) || '').replace(/\{(\d+)\}/g, function (_, index) {
            return values[Number(index)] == null ? '' : String(values[Number(index)]);
        });
    }
    function uploadState(root) {
        if (!uploadStates.has(root)) {
            var input = root.querySelector('.sw-file-upload-input');
            if (!input) return null;
            uploadStates.set(root, {files: Array.from(input.files || []), rejected: []});
            renderUpload(root);
        }
        return uploadStates.get(root);
    }
    function announceUpload(root, text) {
        var status = root.querySelector('.sw-file-upload-status');
        status.textContent = '';
        requestAnimationFrame(function () { if (root.isConnected) status.textContent = text; });
    }
    function renderUpload(root) {
        var state = uploadStates.get(root);
        var input = root.querySelector('.sw-file-upload-input');
        var list = root.querySelector('.sw-file-upload-list');
        var template = root.querySelector('.sw-file-upload-template');
        if (!state || !input || !list || !template) return;
        list.replaceChildren();
        function item(file, index, rejected, reason) {
            var row = template.content.firstElementChild.cloneNode(true);
            row.querySelector('.sw-file-upload-name').textContent = file.name;
            row.querySelector('.sw-file-upload-size').textContent = reason || new Intl.NumberFormat(
                root.closest('[lang]') ? root.closest('[lang]').lang : undefined
            ).format(file.size) + ' B';
            if (rejected) row.setAttribute('data-rejected', '');
            var remove = row.querySelector('[data-sw-remove-file]');
            remove.dataset.swRemoveFile = String(index);
            remove.dataset.swRejectedFile = String(rejected);
            remove.disabled = input.disabled || typeof DataTransfer === 'undefined';
            remove.setAttribute('aria-label', uploadMessage(root, rejected ? 'dismiss' : 'remove', [file.name]));
            list.appendChild(row);
        }
        state.files.forEach(function (file, index) { item(file, index, false); });
        state.rejected.forEach(function (entry, index) { item(entry.file, index, true, entry.reason); });
        list.hidden = list.children.length === 0;
    }
    function setUploadFiles(root, files) {
        var input = root.querySelector('.sw-file-upload-input');
        var transfer = new DataTransfer();
        files.forEach(function (file) { transfer.items.add(file); });
        input.files = transfer.files;
        uploadStates.get(root).files = Array.from(input.files);
    }
    function acceptsUpload(file, accept) {
        var tokens = (accept || '').toLowerCase().split(',').map(function (token) { return token.trim(); }).filter(Boolean);
        return tokens.length === 0 || tokens.some(function (token) {
            if (token.startsWith('.')) return file.name.toLowerCase().endsWith(token);
            if (token.endsWith('/*')) return (file.type || '').toLowerCase().startsWith(token.slice(0, -1));
            return (file.type || '').toLowerCase() === token;
        });
    }
    function addUploadFiles(root, incoming) {
        var input = root.querySelector('.sw-file-upload-input');
        var state = uploadState(root);
        if (!state || input.disabled || typeof DataTransfer === 'undefined') return;
        var files = input.multiple ? state.files.slice() : [];
        var maximum = input.multiple ? Number(root.dataset.maxFiles || 2147483647) : 1;
        var maxSize = Number(root.dataset.maxSize || Infinity);
        state.rejected = [];
        incoming.forEach(function (file) {
            var reason = !acceptsUpload(file, input.accept) ? uploadMessage(root, 'type', [])
                : file.size > maxSize ? uploadMessage(root, 'size', [maxSize]) : null;
            var duplicate = files.some(function (other) {
                return other.name === file.name && other.size === file.size && other.lastModified === file.lastModified;
            });
            if (!reason && duplicate) return;
            if (!reason && files.length >= maximum) reason = uploadMessage(root, 'count', [maximum]);
            if (reason) state.rejected.push({file: file, reason: reason});
            else files.push(file);
        });
        setUploadFiles(root, files);
        renderUpload(root);
        announceUpload(root, uploadMessage(root, 'selected', [files.length]) + ' '
            + state.rejected.map(function (entry) { return entry.file.name + ': ' + entry.reason; }).join(' '));
    }
    function scanUploads(node) {
        if (!(node instanceof Element || node instanceof Document)) return;
        if (node instanceof Element && node.matches('.sw-file-upload')) uploadState(node);
        node.querySelectorAll('.sw-file-upload').forEach(uploadState);
    }
    document.addEventListener('change', function (event) {
        if (!(event.target instanceof Element) || !event.target.matches('.sw-file-upload-input')) return;
        if (event.detail && event.detail.swUpload) return;
        var root = event.target.closest('.sw-file-upload');
        // The cached selection is read before the new picker result is merged into it.
        if (!uploadStates.has(root)) uploadStates.set(root, {files: [], rejected: []});
        var incoming = Array.from(event.target.files || []);
        if (incoming.length === 0 || typeof DataTransfer === 'undefined') {
            uploadStates.set(root, {files: incoming, rejected: []});
            renderUpload(root);
        } else addUploadFiles(root, incoming);
    }, true);
    document.addEventListener('click', function (event) {
        if (!(event.target instanceof Element)) return;
        var button = event.target.closest('[data-sw-remove-file]');
        if (!button) return;
        var root = button.closest('.sw-file-upload');
        var input = root.querySelector('.sw-file-upload-input');
        if (input.disabled || typeof DataTransfer === 'undefined') return;
        var state = uploadState(root);
        var index = Number(button.dataset.swRemoveFile);
        var rejected = button.dataset.swRejectedFile === 'true';
        var file = rejected ? state.rejected[index].file : state.files[index];
        if (rejected) state.rejected.splice(index, 1);
        else { var files = state.files.slice(); files.splice(index, 1); setUploadFiles(root, files); }
        renderUpload(root);
        var actions = root.querySelectorAll('[data-sw-remove-file]');
        (actions.length ? actions[Math.min(index, actions.length - 1)] : input).focus();
        announceUpload(root, uploadMessage(root, 'removed', [file.name]));
        // Emit native changes after the FileList is updated, without re-merging removed files.
        input.dispatchEvent(new Event('input', {bubbles: true}));
        input.dispatchEvent(new CustomEvent('change', {bubbles: true, detail: {swUpload: true}}));
    });
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(function (type) {
        document.addEventListener(type, function (event) {
            if (!(event.target instanceof Element)) return;
            var root = event.target.closest('.sw-file-upload');
            if (!root || !event.dataTransfer || !Array.from(event.dataTransfer.types).includes('Files')) return;
            var input = root.querySelector('.sw-file-upload-input');
            event.preventDefault();
            if (input.disabled || typeof DataTransfer === 'undefined') {
                event.dataTransfer.dropEffect = 'none';
                root.removeAttribute('data-dragging');
                return;
            }
            if (type === 'dragleave') {
                if (!root.contains(event.relatedTarget)) root.removeAttribute('data-dragging');
            } else if (type === 'drop') {
                root.removeAttribute('data-dragging');
                if (!event.dataTransfer.files.length) return;
                addUploadFiles(root, Array.from(event.dataTransfer.files));
                input.dispatchEvent(new Event('input', {bubbles: true}));
                input.dispatchEvent(new CustomEvent('change', {bubbles: true, detail: {swUpload: true}}));
            } else root.setAttribute('data-dragging', '');
        });
    });
    document.addEventListener('reset', function (event) {
        var form = event.target;
        queueMicrotask(function () {
            if (!event.defaultPrevented) form.querySelectorAll('.sw-file-upload').forEach(function (root) {
                uploadStates.set(root, {files: [], rejected: []}); renderUpload(root);
            });
        });
    });
    Shadwick.clearFileSelection = function (inputId) {
        var input = document.getElementById(inputId);
        if (!input) return;
        input.value = '';
        var root = input.closest('.sw-file-upload');
        if (root) {
            uploadStates.set(root, {files: [], rejected: []}); renderUpload(root);
            announceUpload(root, uploadMessage(root, 'cleared', []));
        }
    };
    window.addEventListener('pageshow', function () {
        document.querySelectorAll('.sw-file-upload').forEach(function (root) {
            var input = root.querySelector('.sw-file-upload-input');
            uploadStates.set(root, {files: Array.from(input.files || []), rejected: []}); renderUpload(root);
        });
    });
    scanUploads(document);

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
            record.addedNodes.forEach(scanUploads);
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

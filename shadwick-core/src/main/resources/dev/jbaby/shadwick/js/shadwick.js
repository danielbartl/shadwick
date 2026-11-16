/*
 * Shadwick's small progressive enhancements: form feedback, uploads, avatars, toasts and native dialogs/sheets.
 * It reads data- attributes and listens on the document, so it works for
 * markup Wicket adds or replaces over Ajax, and pages keep Wicket's strict, nonce-based CSP: no inline scripts or
 * event handlers.
 */
(function () {
    'use strict';
    var Shadwick = window.Shadwick = window.Shadwick || {};
    if (Shadwick._coreInstalled) return;
    Shadwick._coreInstalled = true;

    // Wicket restores focus after replacing an editor. Keep its input fully visible in a scrolled table.
    document.addEventListener('focusin', function (event) {
        if (event.target instanceof Element && event.target.matches('[data-sw-table-editor] input'))
            event.target.scrollIntoView({block: 'nearest', inline: 'nearest'});
    });

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
    // The input's FileList is submitted by Wicket; files are never encoded into page state. Each file is shown as an
    // attachment; while an Ajax submit sends the files, they show its progress (see trackUpload).
    var uploadStates = new WeakMap();
    function newUploadState(files) { return {files: files, rejected: [], done: [], upload: null}; }
    function uploadMessage(root, key, values) {
        return (root.getAttribute('data-message-' + key) || '').replace(/\{(\d+)\}/g, function (_, index) {
            return values[Number(index)] == null ? '' : String(values[Number(index)]);
        });
    }
    function uploadLanguage(root) { return root.closest('[lang]') ? root.closest('[lang]').lang : undefined; }
    function uploadState(root) {
        if (!uploadStates.has(root)) {
            var input = root.querySelector('.sw-file-upload-input');
            if (!input) return null;
            uploadStates.set(root, newUploadState(Array.from(input.files || [])));
            renderUpload(root);
        }
        return uploadStates.get(root);
    }
    // Sending or processing: the selection cannot change until the request ends.
    function uploadBusy(state) { return !!(state && state.upload && state.upload.request); }
    function announceUpload(root, text) {
        var status = root.querySelector('.sw-file-upload-status');
        status.textContent = '';
        requestAnimationFrame(function () { if (root.isConnected) status.textContent = text; });
    }

    // Image previews are drawn from the file into a canvas: no URL is made, so no img-src exception is needed, and a
    // thumbnail is decoded once per File, however often the list is drawn again.
    var uploadThumbnails = new WeakMap();
    function uploadThumbnail(file) {
        if (!uploadThumbnails.has(file)) {
            var image = /^image\//i.test(file.type || '') && !/svg/i.test(file.type) && file.size <= 20 * 1024 * 1024;
            uploadThumbnails.set(file, !image || typeof createImageBitmap !== 'function' ? Promise.resolve(null)
                : createImageBitmap(file).then(function (bitmap) {
                    var side = Math.min(bitmap.width, bitmap.height), canvas = document.createElement('canvas');
                    canvas.width = canvas.height = 256;
                    canvas.getContext('2d').drawImage(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2,
                        side, side, 0, 0, 256, 256);
                    if (bitmap.close) bitmap.close();
                    return canvas;
                }, function () { return null; }));
        }
        return uploadThumbnails.get(file);
    }
    function previewUpload(root, attachment, file) {
        var template = root.querySelector('.sw-file-upload-template[data-kind="image"]');
        if (!template) return;
        uploadThumbnail(file).then(function (source) {
            // A list drawn again since, or a component replaced, has no use for it.
            if (!source || !attachment.isConnected) return;
            var media = template.content.firstElementChild.cloneNode(true);
            var canvas = media.querySelector('canvas');
            canvas.width = source.width; canvas.height = source.height;
            canvas.getContext('2d').drawImage(source, 0, 0);
            var icon = attachment.querySelector(':scope > .sw-attachment-media');
            if (icon) icon.replaceWith(media);
        });
    }

    function uploadRowState(root, state, index) {
        var upload = state.upload, file = state.files[index];
        if (!upload) {
            return {state: 'idle', text: new Intl.NumberFormat(uploadLanguage(root)).format(file.size) + ' B'};
        }
        if (upload.state === 'uploading') {
            return {state: 'uploading', text: upload.shares
                ? uploadMessage(root, 'uploading', [new Intl.NumberFormat(uploadLanguage(root), {style: 'percent'})
                    .format(upload.shares[index] || 0)])
                : uploadMessage(root, 'uploading-indeterminate', [])};
        }
        return {state: upload.state, text: uploadMessage(root, upload.state === 'processing' ? 'processing' : 'failed', [])};
    }
    function setAttachmentState(attachment, value) {
        if (value) attachment.setAttribute('data-state', value); else attachment.removeAttribute('data-state');
        if (value === 'uploading' || value === 'processing') attachment.setAttribute('aria-busy', 'true');
        else attachment.removeAttribute('aria-busy');
    }
    function renderUpload(root) {
        var state = uploadStates.get(root);
        var input = root.querySelector('.sw-file-upload-input');
        var list = root.querySelector('.sw-file-upload-list');
        if (!state || !input || !list) return;
        var grid = root.getAttribute('data-layout') === 'grid';
        list.replaceChildren();
        function item(kind, file, index, rowState, text) {
            var template = root.querySelector('.sw-file-upload-template[data-kind="' + kind + '"]');
            if (!template) return;
            var row = template.content.firstElementChild.cloneNode(true);
            var attachment = row.querySelector('.sw-attachment');
            if (grid) attachment.setAttribute('data-orientation', 'vertical'); else attachment.setAttribute('data-size', 'sm');
            setAttachmentState(attachment, rowState);
            attachment.querySelector('.sw-attachment-title').textContent = file.name;
            attachment.querySelector('.sw-attachment-description').textContent = text;
            var remove = attachment.querySelector('[data-sw-remove-file]');
            if (index < 0) remove.closest('.sw-attachment-actions').remove();
            else {
                remove.dataset.swRemoveFile = String(index);
                remove.dataset.swRejectedFile = String(kind === 'rejected');
                remove.disabled = input.disabled || typeof DataTransfer === 'undefined';
                remove.setAttribute('aria-label', uploadMessage(root, kind === 'rejected' ? 'dismiss' : 'remove', [file.name]));
            }
            if (kind === 'file' && root.hasAttribute('data-previews')) previewUpload(root, attachment, file);
            list.appendChild(row);
        }
        state.files.forEach(function (file, index) {
            var row = uploadRowState(root, state, index);
            item('file', file, index, row.state, row.text);
        });
        state.done.forEach(function (file) { item('file', file, -1, null, uploadMessage(root, 'done', [])); });
        state.rejected.forEach(function (entry, index) { item('rejected', entry.file, index, 'error', entry.reason); });
        list.hidden = list.children.length === 0;
    }
    // Progress changes only each row's state and text: the rows, their focus and their thumbnails stay.
    function paintUpload(root) {
        var state = uploadStates.get(root);
        var rows = root.querySelectorAll('.sw-file-upload-list > .sw-file-upload-item > .sw-attachment');
        state.files.forEach(function (file, index) {
            var attachment = rows[index], row = uploadRowState(root, state, index);
            if (!attachment) return;
            setAttachmentState(attachment, row.state);
            var description = attachment.querySelector('.sw-attachment-description');
            if (description.textContent !== row.text) description.textContent = row.text;
        });
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
    // A new choice ends what the last upload left on show: its uploaded or failed files.
    function forgetUpload(state) {
        state.done = [];
        if (state.upload && !state.upload.request) state.upload = null;
    }
    function addUploadFiles(root, incoming) {
        var input = root.querySelector('.sw-file-upload-input');
        var state = uploadState(root);
        if (!state || input.disabled || uploadBusy(state) || typeof DataTransfer === 'undefined') return;
        forgetUpload(state);
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
        subscribeUploads();
    }

    // Upload progress for Wicket Ajax submits of a multipart form. Wicket publishes beforeSend before the request
    // goes out: its native engine passes the XMLHttpRequest itself, its jQuery engine a jqXHR and the settings whose
    // xhr factory jQuery calls next, which is wrapped for this one request. The browser reports the whole body's
    // progress; the files go into it one after the other, so each one's share is worked out from where it is.
    var uploadRequests = new Map();
    var uploadsSubscribed = false;
    function trackUpload(event, attrs, xhr, settings) {
        if (!attrs || !attrs.mp || !attrs.f) return;
        var form = document.getElementById(attrs.f);
        if (!form) return;
        var roots = Array.from(form.querySelectorAll('.sw-file-upload')).filter(function (root) {
            var state = uploadStates.get(root), input = root.querySelector('.sw-file-upload-input');
            return state && input && !input.disabled && !uploadBusy(state) && state.files.length > 0;
        });
        if (roots.length === 0) return;
        var request = {xhr: xhr, roots: roots, sizes: 0, offsets: new Map()};
        form.querySelectorAll('input[type=file]').forEach(function (input) {
            if (input.disabled) return;
            var offsets = [];
            Array.from(input.files || []).forEach(function (file) { offsets.push(request.sizes); request.sizes += file.size; });
            request.offsets.set(input, offsets);
        });
        roots.forEach(function (root) { startUpload(root, request); });
        var watch = function (real) {
            if (!real || !real.upload) return;
            real.upload.addEventListener('progress', function (progress) {
                progressUpload(request, progress.lengthComputable ? progress.loaded : 0,
                    progress.lengthComputable ? progress.total : 0);
                // Firefox fires the upload's load only with the response: every byte sent is the same news.
                if (progress.lengthComputable && progress.total > 0 && progress.loaded >= progress.total) sentUpload(request);
            });
            real.upload.addEventListener('load', function () { sentUpload(request); });
        };
        if (xhr && xhr.upload) watch(xhr);
        else if (settings && typeof settings.xhr === 'function') {
            var make = settings.xhr;
            settings.xhr = function () { var real = make.apply(this, arguments); watch(real); return real; };
        }
        uploadRequests.set(attrs, request);
    }
    function startUpload(root, request) {
        var state = uploadStates.get(root), input = root.querySelector('.sw-file-upload-input');
        state.rejected = []; state.done = [];
        state.upload = {state: 'uploading', shares: null, request: request, files: state.files.slice(),
            ariaDisabled: input.getAttribute('aria-disabled')};
        root.setAttribute('data-uploading', 'sending');
        // The body is already made: the input stays enabled, so it can take the focus back from Cancel, but it
        // refuses new files until the request ends.
        input.setAttribute('aria-disabled', 'true');
        renderUpload(root);
        announceUpload(root, uploadMessage(root, 'announce-uploading',
            [new Intl.NumberFormat(uploadLanguage(root)).format(state.files.length)]));
    }
    function progressUpload(request, loaded, total) {
        request.roots.forEach(function (root) {
            var state = uploadStates.get(root), input = root.querySelector('.sw-file-upload-input');
            if (!root.isConnected || !state || !state.upload || state.upload.request !== request
                    || state.upload.state !== 'uploading') return;
            var offsets = request.offsets.get(input);
            if (!total || !request.sizes || !offsets) state.upload.shares = null;
            else {
                var sent = loaded * request.sizes / total;
                state.upload.shares = state.files.map(function (file, index) {
                    var offset = offsets[index] || 0;
                    return file.size === 0 ? (sent >= offset ? 1 : 0) : Math.min(1, Math.max(0, (sent - offset) / file.size));
                });
            }
            paintUpload(root);
        });
    }
    // All bytes are sent: the server is working on them, and cancelling could no longer undo that.
    function sentUpload(request) {
        request.roots.forEach(function (root) {
            var state = uploadStates.get(root);
            if (!root.isConnected || !state || !state.upload || state.upload.request !== request
                    || state.upload.state === 'processing') return;
            state.upload.state = 'processing';
            var cancel = root.querySelector('.sw-file-upload-cancel');
            var refocus = document.activeElement === cancel;
            root.setAttribute('data-uploading', 'processing');
            if (refocus) root.querySelector('.sw-file-upload-input').focus();
            paintUpload(root);
        });
    }
    function finishUpload(root, state) {
        var input = root.querySelector('.sw-file-upload-input');
        var cancel = root.querySelector('.sw-file-upload-cancel');
        var refocus = document.activeElement === cancel;
        if (state.upload.ariaDisabled === null) input.removeAttribute('aria-disabled');
        else input.setAttribute('aria-disabled', state.upload.ariaDisabled);
        root.removeAttribute('data-uploading');
        if (refocus) input.focus();
    }
    function endUpload(root, how) {
        var state = uploadStates.get(root);
        finishUpload(root, state);
        // A failure leaves the files in the input, ready to send again; a 200 may be a validation error, which leaves
        // them too. Only clearSelection, called once the server has taken them, shows them as uploaded.
        state.upload = how === 'error' ? {state: 'error'} : null;
        renderUpload(root);
        if (how !== 'idle') announceUpload(root, uploadMessage(root, how === 'error' ? 'announce-failed' : 'announce-cancelled', []));
    }
    function completeUpload(event, attrs, xhr, textStatus) {
        var request = uploadRequests.get(attrs);
        if (!request) return;
        uploadRequests.delete(attrs);
        request.roots.forEach(function (root) {
            var state = uploadStates.get(root);
            if (!root.isConnected || !state || !state.upload || state.upload.request !== request) return;
            endUpload(root, textStatus === 'success' ? 'idle' : textStatus === 'abort' ? 'cancelled' : 'error');
        });
    }
    function subscribeUploads() {
        if (uploadsSubscribed || !window.Wicket || !Wicket.Event || !Wicket.Event.subscribe) return;
        uploadsSubscribed = true;
        Wicket.Event.subscribe('/ajax/call/beforeSend', trackUpload);
        Wicket.Event.subscribe('/ajax/call/complete', completeUpload);
    }
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', subscribeUploads);
    else subscribeUploads();
    document.addEventListener('click', function (event) {
        if (!(event.target instanceof Element)) return;
        var cancel = event.target.closest('.sw-file-upload-cancel');
        if (cancel) {
            var state = uploadStates.get(cancel.closest('.sw-file-upload'));
            if (uploadBusy(state) && state.upload.state === 'uploading' && state.upload.request.xhr
                    && typeof state.upload.request.xhr.abort === 'function') state.upload.request.xhr.abort();
            return;
        }
        // An input refusing new files while they are sent: its picker does not open (also from the keyboard).
        if (event.target.matches('.sw-file-upload-input') && uploadBusy(uploadStates.get(event.target.closest('.sw-file-upload')))) {
            event.preventDefault();
        }
    }, true);

    document.addEventListener('change', function (event) {
        if (!(event.target instanceof Element) || !event.target.matches('.sw-file-upload-input')) return;
        if (event.detail && event.detail.swUpload) return;
        var root = event.target.closest('.sw-file-upload');
        // The cached selection is read before the new picker result is merged into it.
        if (!uploadStates.has(root)) uploadStates.set(root, newUploadState([]));
        var incoming = Array.from(event.target.files || []);
        if (incoming.length === 0 || typeof DataTransfer === 'undefined') {
            uploadStates.set(root, newUploadState(incoming));
            renderUpload(root);
        } else addUploadFiles(root, incoming);
    }, true);
    document.addEventListener('click', function (event) {
        if (!(event.target instanceof Element)) return;
        var button = event.target.closest('[data-sw-remove-file]');
        if (!button) return;
        var root = button.closest('.sw-file-upload');
        var input = root.querySelector('.sw-file-upload-input');
        var state = uploadState(root);
        if (input.disabled || uploadBusy(state) || typeof DataTransfer === 'undefined') return;
        var index = Number(button.dataset.swRemoveFile);
        var rejected = button.dataset.swRejectedFile === 'true';
        var file = rejected ? state.rejected[index].file : state.files[index];
        forgetUpload(state);
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
            if (input.disabled || uploadBusy(uploadStates.get(root)) || typeof DataTransfer === 'undefined') {
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
                if (uploadBusy(uploadStates.get(root))) return;
                uploadStates.set(root, newUploadState([])); renderUpload(root);
            });
        });
    });
    Shadwick.clearFileSelection = function (inputId) {
        var input = document.getElementById(inputId);
        if (!input) return;
        input.value = '';
        var root = input.closest('.sw-file-upload');
        if (!root) return;
        var state = uploadStates.get(root);
        if (uploadBusy(state)) {
            // Called in the response to the request that sent them: the files are the server's now, so they show as
            // uploaded until the next choice, and leave the input so they are not sent twice.
            var sent = state.upload.files;
            finishUpload(root, state);
            uploadStates.set(root, {files: [], rejected: [], done: sent, upload: null});
            renderUpload(root);
            announceUpload(root, uploadMessage(root, 'announce-uploaded',
                [new Intl.NumberFormat(uploadLanguage(root)).format(sent.length)]));
        } else {
            uploadStates.set(root, newUploadState([])); renderUpload(root);
            announceUpload(root, uploadMessage(root, 'cleared', []));
        }
    };
    window.addEventListener('pageshow', function () {
        document.querySelectorAll('.sw-file-upload').forEach(function (root) {
            var input = root.querySelector('.sw-file-upload-input');
            uploadStates.set(root, newUploadState(Array.from(input.files || []))); renderUpload(root);
        });
    });
    scanUploads(document);


    // Popovers and tooltips, adapted from Shadleaf's popup/tooltip behavior (Apache-2.0, see NOTICE).
    // Native top-layer and CSS anchor positioning: no style attributes or client framework.
    var popupStates = new WeakMap();
    var popupSelector = '.sw-popover-content,.sw-dropdown-menu-content';
    var menuSelector = '.sw-dropdown-menu-content';
    var menuItemSelector = '[role="menuitem"],[role="menuitemcheckbox"],[role="menuitemradio"]';
    var tooltipStates = new WeakMap();
    var tooltipOwners = new WeakMap();
    var activeTooltip = null;
    var tooltipCounter = 0;
    var lastTooltipHidden = 0;
    var touchInteraction = false;
    var tabbable = 'a[href],button:enabled,input:enabled:not([type=hidden]),select:enabled,textarea:enabled,[tabindex]:not([tabindex="-1"]),[contenteditable="true"]';

    function popupTriggers(popup) {
        return Array.from(document.querySelectorAll('[popovertarget]')).filter(function (trigger) {
            return trigger.getAttribute('popovertarget') === popup.id;
        });
    }
    function popupState(popup) {
        if (!popupStates.has(popup)) popupStates.set(popup, {source: popupTriggers(popup)[0], focusTimer: 0});
        return popupStates.get(popup);
    }
    function hidePopup(popup) {
        if (popup && popup.matches(':popover-open')) popup.hidePopover();
    }
    function showPopup(popup, source) {
        if (popup.matches(':popover-open')) return;
        try { popup.showPopover({source: source}); }
        catch (error) { if (error instanceof TypeError) popup.showPopover(); else throw error; }
    }
    function menuItemEnabled(item) { return !item.matches(':disabled,[aria-disabled="true"]'); }
    function menuItems(menu) {
        return Array.from(menu.querySelectorAll(menuItemSelector)).filter(function (item) {
            return item.closest(menuSelector) === menu && menuItemEnabled(item) && visibleControl(item);
        });
    }
    function prepareMenu(menu) {
        menu.querySelectorAll(menuItemSelector).forEach(function (item) { item.setAttribute('tabindex', '-1'); });
    }
    function menuItemText(item) {
        if (item.hasAttribute('aria-label')) return item.getAttribute('aria-label').toLocaleLowerCase();
        var walker = document.createTreeWalker(item, NodeFilter.SHOW_TEXT), text = '', node;
        while ((node = walker.nextNode())) {
            if (!node.parentElement.closest('.sw-dropdown-menu-shortcut,[aria-hidden="true"]')) text += node.textContent;
        }
        return text.trim().toLocaleLowerCase();
    }
    function menuKeys(event) {
        if (!(event.target instanceof Element) || event.isComposing) return;
        var trigger = event.target.closest('[popovertarget]');
        var menu = trigger && document.getElementById(trigger.getAttribute('popovertarget'));
        if (menu && menu.matches(menuSelector) && !trigger.matches(':disabled,[aria-disabled="true"]')) {
            var openingState = popupState(menu);
            if (event.key === 'Enter' || event.key === ' ') openingState.keyboard = true;
            if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                event.preventDefault(); openingState.keyboard = true;
                openingState.last = event.key === 'ArrowUp'; openingState.source = trigger;
                if (menu.matches(':popover-open')) {
                    var entries = menuItems(menu);
                    (entries[openingState.last ? entries.length - 1 : 0] || menu).focus();
                } else showPopup(menu, trigger);
                return;
            }
        }
        menu = event.target.closest(menuSelector);
        if (!menu || !menu.matches(':popover-open')) return;
        if (event.key === 'Tab') { Shadwick.closePopover(menu.id); return; }
        var items = menuItems(menu), index = items.indexOf(document.activeElement), next;
        switch (event.key) {
            case 'ArrowDown': next = items[index < 0 ? 0 : Math.min(index + 1, items.length - 1)]; break;
            case 'ArrowUp': next = items[index < 0 ? items.length - 1 : Math.max(index - 1, 0)]; break;
            case 'Home': case 'PageUp': next = items[0]; break;
            case 'End': case 'PageDown': next = items[items.length - 1]; break;
            case ' ':
                if (event.target instanceof HTMLAnchorElement && menuItemEnabled(event.target)) {
                    event.preventDefault(); event.target.click();
                }
                return;
            default:
                if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
                    var state = popupState(menu), now = Date.now();
                    state.typed = now - (state.typedAt || 0) > 500 ? event.key : (state.typed || '') + event.key;
                    state.typedAt = now;
                    var text = state.typed.toLocaleLowerCase();
                    var repeated = text.length > 1 && Array.from(text).every(function (letter) { return letter === text[0]; });
                    var start = repeated || text.length === 1 ? index + 1 : Math.max(index, 0);
                    var search = repeated ? text[0] : text;
                    next = items.slice(start).concat(items.slice(0, start)).find(function (item) {
                        return menuItemText(item).startsWith(search);
                    });
                }
        }
        if (next) { event.preventDefault(); next.focus(); }
    }
    function visibleControl(node) {
        return node.getClientRects().length && getComputedStyle(node).visibility !== 'hidden' && !node.closest('[inert]');
    }
    Shadwick.closePopover = function (id) {
        var popup = document.getElementById(id);
        if (!popup || !popup.matches(':popover-open')) return;
        var source = popupState(popup).source;
        if (!source || !source.isConnected) source = popupTriggers(popup)[0];
        var returnFocus = popup.contains(document.activeElement);
        hidePopup(popup);
        if (returnFocus && source && source.isConnected) source.focus();
    };
    document.addEventListener('beforetoggle', function (event) {
        var popup = event.target;
        if (popup instanceof Element && event.newState === 'closed' && activeTooltip
            && popup.matches(popupSelector + ',dialog,.sw-sidebar') && popup.contains(activeTooltip.source)) {
            activeTooltip.focused = false; activeTooltip.dismissed = true; hideTooltip(activeTooltip);
        }
        if (!(popup instanceof Element) || !popup.matches(popupSelector)) return;
        var state = popupState(popup);
        var opening = event.newState === 'open';
        if (opening && popup.matches(menuSelector)) state.choosing = false;
        if (opening) {
            state.source = event.source || popupTriggers(popup)[0];
            var hint = state.source && tooltipStates.get(state.source);
            if (hint && state.source.hasAttribute('data-sw-sidebar-hint')) { hint.dismissed = true; hideTooltip(hint); }
        }
        popupTriggers(popup).forEach(function (trigger) { trigger.setAttribute('aria-expanded', String(opening)); });
        clearTimeout(state.focusTimer);
        if (opening) state.focusTimer = setTimeout(function () {
            if (popup.matches(menuSelector) && popup.isConnected && popup.matches(':popover-open')) {
                var entries = menuItems(popup);
                (state.keyboard ? entries[state.last ? entries.length - 1 : 0] || popup : popup).focus();
                state.keyboard = false; state.last = false; state.typed = ''; state.typedAt = 0;
                return;
            }
            if (popup.isConnected && popup.matches(':popover-open') && !popup.contains(document.activeElement)) {
                (Array.from(popup.querySelectorAll(tabbable)).find(visibleControl) || popup).focus();
            }
        }, 0);
    }, true);
    document.addEventListener('focusout', function (event) {
        if (!(event.target instanceof Element)) return;
        var popup = event.target.closest(popupSelector);
        if (!popup || !popup.matches(':popover-open')) return;
        var to = event.relatedTarget;
        if (to instanceof Node && !popup.contains(to) && !popupTriggers(popup).some(function (trigger) {
            return trigger === to || trigger.contains(to);
        })) hidePopup(popup);
    });

    function tooltipDescription(state, add) {
        if (state.mode === 'label') return;
        var source = state.source;
        var tokens = (source.getAttribute('aria-describedby') || '').split(/\s+/).filter(Boolean);
        var next = tokens.filter(function (id) { return id !== state.tip.id; });
        if (add) next.push(state.tip.id);
        var value = next.join(' ');
        if (value !== (source.getAttribute('aria-describedby') || '')) {
            if (value) source.setAttribute('aria-describedby', value); else source.removeAttribute('aria-describedby');
        }
    }
    function hideTooltip(state) {
        clearTimeout(state.showTimer); clearTimeout(state.hideTimer);
        if (state.tip.matches(':popover-open')) {
            state.tip.setAttribute('data-sw-instant', '');
            state.tip.hidePopover(); lastTooltipHidden = Date.now();
        }
        if (activeTooltip === state) activeTooltip = null;
    }
    function removeTooltip(source) {
        var state = tooltipStates.get(source);
        if (!state) return;
        hideTooltip(state);
        tooltipDescription(state, false);
        state.tip.remove();
        tooltipOwners.delete(state.tip);
        tooltipStates.delete(source);
    }
    function placedSide(popup, source) {
        var a = source.getBoundingClientRect(), p = popup.getBoundingClientRect();
        var x = p.left + p.width / 2, y = p.top + p.height / 2;
        return y < a.top ? 'top' : y > a.bottom ? 'bottom' : x < a.left ? 'left' : 'right';
    }
    function showTooltip(state) {
        clearTimeout(state.showTimer); clearTimeout(state.hideTimer);
        if (state.source.hasAttribute('data-sw-sidebar-hint')) {
            var sidebar = state.source.closest('.sw-sidebar');
            if (state.source.getAttribute('aria-expanded') === 'true' || !sidebar
                || !window.matchMedia('(width >= 48rem)').matches || sidebar.dataset.collapsible !== 'icon'
                || sidebar.closest('.sw-sidebar-provider').dataset.state !== 'collapsed') return;
            state.tip.dataset.side = sidebar.dataset.side === 'right' ? 'left' : 'right';
        }
        var parentPopup = state.source.closest(popupSelector);
        var parentDialog = state.source.closest('dialog');
        if (!state.source.isConnected || !visibleControl(state.source) || state.dismissed
            || state.source.matches(':disabled,[aria-disabled="true"]')
            || parentPopup && !parentPopup.matches(':popover-open') || parentDialog && !parentDialog.open) return;
        if (activeTooltip && activeTooltip !== state) hideTooltip(activeTooltip);
        activeTooltip = state;
        state.tip.removeAttribute('data-sw-instant');
        showPopup(state.tip, state.source);
        state.tip.dataset.placed = placedSide(state.tip, state.source);
    }
    function hideTooltipSoon(state) {
        clearTimeout(state.showTimer); clearTimeout(state.hideTimer);
        if (!state.focused) state.hideTimer = setTimeout(function () {
            if (!state.hovered && !state.tipHovered && !state.focused) hideTooltip(state);
        }, 100);
    }
    function setupTooltip(source) {
        if (!source.isConnected || !source.parentElement) return;
        var text = source.getAttribute('data-sw-tooltip');
        var previous = tooltipStates.get(source);
        var placement = source.getAttribute('data-tooltip-placement') || 'top';
        var mode = source.getAttribute('data-tooltip-mode') || 'description';
        var delay = Math.max(0, Number(source.getAttribute('data-tooltip-delay') || 300));
        if (previous && previous.text === text && previous.placement === placement && previous.mode === mode && previous.delay === delay) {
            source.removeAttribute('title');
            tooltipDescription(previous, true);
            return;
        }
        removeTooltip(source);
        if (!text || !text.trim() || typeof source.showPopover !== 'function') return;
        var tip = document.createElement('span');
        do { tip.id = 'sw-tooltip-' + (++tooltipCounter); } while (document.getElementById(tip.id));
        tip.className = 'sw-tooltip-content' + (source.hasAttribute('data-sw-sidebar-hint') ? ' sw-sidebar-menu-tooltip' : '');
        tip.setAttribute('popover', 'manual'); tip.setAttribute('role', 'tooltip');
        tip.setAttribute('data-side', placement); tip.setAttribute('data-align', 'center');
        if (mode === 'label') tip.setAttribute('aria-hidden', 'true');
        var label = document.createElement('span'); label.className = 'sw-tooltip-text'; label.textContent = text;
        var arrow = document.createElement('span'); arrow.className = 'sw-tooltip-arrow'; arrow.setAttribute('aria-hidden', 'true');
        tip.append(label, arrow);
        // Preserve the theme/modal context, outside any label so its text cannot rename the control.
        var wrappingLabel = source.closest('label');
        (wrappingLabel ? wrappingLabel.parentNode : source.parentNode).appendChild(tip);
        var state = {source: source, tip: tip, text: text, placement: placement, mode: mode, delay: delay,
            showTimer: 0, hideTimer: 0, hovered: false, tipHovered: false, focused: false, dismissed: false};
        tooltipStates.set(source, state); tooltipOwners.set(tip, state);
        source.removeAttribute('title'); tooltipDescription(state, true);
        if (!touchInteraction && document.activeElement === source && source.matches(':focus-visible')) {
            state.focused = true; showTooltip(state);
        }
    }
    function scanPopups(node) {
        if (!(node instanceof Element || node instanceof Document)) return;
        if (node instanceof Element && !node.isConnected) return;
        var nodes = node instanceof Element ? [node] : [];
        nodes = nodes.concat(Array.from(node.querySelectorAll('[data-sw-tooltip],' + popupSelector)));
        nodes.forEach(function (element) {
            if (element.hasAttribute('data-sw-tooltip')) setupTooltip(element);
            if (element.matches(popupSelector)) {
                if (element.matches(menuSelector)) prepareMenu(element);
                popupState(element);
                popupTriggers(element).forEach(function (trigger) {
                    trigger.setAttribute('aria-expanded', String(element.matches(':popover-open')));
                });
            }
        });
        if (node instanceof Element && node.closest(menuSelector)) prepareMenu(node.closest(menuSelector));
    }
    function cleanupPopups(node) {
        if (!(node instanceof Element) || node.isConnected) return;
        [node].concat(Array.from(node.querySelectorAll('[data-sw-tooltip],' + popupSelector))).forEach(function (element) {
            removeTooltip(element);
            if (popupStates.has(element)) {
                var state = popupStates.get(element); clearTimeout(state.focusTimer);
                popupTriggers(element).forEach(function (trigger) { trigger.setAttribute('aria-expanded', 'false'); });
                popupStates.delete(element);
            }
        });
    }
    ['pointerover', 'pointerout'].forEach(function (type) {
        document.addEventListener(type, function (event) {
            if (!(event.target instanceof Element) || event.pointerType === 'touch') return;
            var tip = event.target.closest('.sw-tooltip-content');
            var source = event.target.closest('[data-sw-tooltip]');
            var state = tip ? tooltipOwners.get(tip) : source && tooltipStates.get(source);
            if (!state) return;
            var current = tip || source;
            if (event.relatedTarget instanceof Node && current.contains(event.relatedTarget)) return;
            var entering = type === 'pointerover';
            if (tip) state.tipHovered = entering; else state.hovered = entering;
            if (entering) {
                clearTimeout(state.hideTimer);
                if (!tip && !state.dismissed && !state.tip.matches(':popover-open')) {
                    clearTimeout(state.showTimer);
                    state.showTimer = setTimeout(function () { showTooltip(state); },
                        activeTooltip || Date.now() - lastTooltipHidden < 300 ? 0 : state.delay);
                }
            } else {
                if (!tip && !state.focused) state.dismissed = false;
                hideTooltipSoon(state);
            }
        });
    });
    document.addEventListener('pointerdown', function (event) {
        touchInteraction = event.pointerType === 'touch';
        if (!(event.target instanceof Element)) return;
        var trigger = event.target.closest('[popovertarget]');
        var menu = trigger && document.getElementById(trigger.getAttribute('popovertarget'));
        if (menu && menu.matches(menuSelector)) { popupState(menu).keyboard = false; popupState(menu).source = trigger; }
        var state = tooltipStates.get(event.target.closest('[data-sw-tooltip]'));
        if (state) { state.dismissed = true; hideTooltip(state); }
    });
    document.addEventListener('focusin', function (event) {
        var state = tooltipStates.get(event.target);
        if (state && !touchInteraction && event.target.matches(':focus-visible')) {
            state.focused = true; state.dismissed = false;
            state.showTimer = setTimeout(function () {
                if (document.activeElement === state.source) showTooltip(state);
            }, 0);
        }
    });
    document.addEventListener('focusout', function (event) {
        var state = tooltipStates.get(event.target);
        if (state) { state.focused = false; if (!state.hovered) state.dismissed = false; hideTooltipSoon(state); }
    });
    document.addEventListener('keydown', function (event) {
        touchInteraction = false;
        if (event.key !== 'Escape') { menuKeys(event); return; }
        var cellEditor = event.target instanceof Element ? event.target.closest('[data-sw-table-editor]') : null;
        if (cellEditor) {
            var cancelEdit = cellEditor.querySelector('[data-sw-table-cancel]');
            if (cancelEdit) { event.preventDefault(); event.stopImmediatePropagation(); cancelEdit.click(); return; }
        }
        if (activeTooltip && activeTooltip.tip.matches(':popover-open')) {
            event.preventDefault(); event.stopImmediatePropagation();
            activeTooltip.dismissed = true; hideTooltip(activeTooltip);
            return;
        }
        // A nested select keeps focus on its trigger; dismiss only its own picker.
        var selectWrapper = event.target instanceof Element ? event.target.closest('.sw-select-wrapper,.sw-combobox') : null;
        if (selectWrapper && HTMLElement.prototype.showPopover && (selectWrapper.querySelector('.sw-select-content:popover-open,.sw-combobox-content:popover-open')
            || CSS.supports('appearance', 'base-select') && selectWrapper.querySelector('select.sw-select:open'))) return;
        var popup = event.target instanceof Element ? event.target.closest(popupSelector) : null;
        if (popup && popup.matches(':popover-open')) {
            event.preventDefault(); event.stopPropagation(); Shadwick.closePopover(popup.id);
        }
    }, true);
    document.addEventListener('pointermove', function (event) {
        if (!(event.target instanceof Element) || event.pointerType !== 'mouse') return;
        var menu = event.target.closest(menuSelector);
        if (!menu || !menu.matches(':popover-open')) return;
        var item = event.target.closest(menuItemSelector);
        var target = item && menuItemEnabled(item) ? item : menu;
        if (document.activeElement !== target) target.focus({preventScroll: true});
    });
    document.addEventListener('pointerout', function (event) {
        if (!(event.target instanceof Element) || event.pointerType !== 'mouse') return;
        var menu = event.target.closest(menuSelector);
        if (menu && menu.matches(':popover-open') && !menu.contains(event.relatedTarget) && menu.contains(document.activeElement)) {
            menu.focus({preventScroll: true});
        }
    });
    document.addEventListener('click', function (event) {
        if (!(event.target instanceof Element)) return;
        var item = event.target.closest(menuItemSelector), menu = item && item.closest(menuSelector);
        if (menu && (!menu.matches(':popover-open') || !menuItemEnabled(item) || popupState(menu).choosing)) {
            event.preventDefault(); event.stopImmediatePropagation();
            return;
        }
        if (!menu || !menu.matches(':popover-open') || !menuItemEnabled(item)) return;
        // Capture also covers Wicket callbacks that stop bubbling. Keep the item rendered through its default action.
        popupState(menu).choosing = true;
        var source = popupState(menu).source;
        if (!source || !source.isConnected) source = popupTriggers(menu)[0];
        if (source) source.focus({preventScroll: true});
        setTimeout(function () { hidePopup(menu); }, 0);
    }, true);
    window.addEventListener('resize', function () {
        if (activeTooltip) activeTooltip.tip.dataset.placed = placedSide(activeTooltip.tip, activeTooltip.source);
    });
    document.addEventListener('scroll', function () {
        if (activeTooltip) activeTooltip.tip.dataset.placed = placedSide(activeTooltip.tip, activeTooltip.source);
    }, true);
    scanPopups(document);

    // The mixed state of a checkbox (SwCheckBox, a table's select-all box) is a DOM property only, so the server
    // renders data-sw-indeterminate and this sets the property from it, again whenever the attribute changes.
    function syncIndeterminate(box) { box.indeterminate = box.getAttribute('data-sw-indeterminate') === 'true'; }
    function scanIndeterminateCheckboxes(root) {
        if (root instanceof Element && root.matches('input[data-sw-indeterminate]')) syncIndeterminate(root);
        if (root.querySelectorAll) root.querySelectorAll('input[data-sw-indeterminate]').forEach(syncIndeterminate);
    }
    scanIndeterminateCheckboxes(document);

    // ---- Avatar and attachment images: delegated handlers also cover Ajax replacements ----
    // Adapted from Shadleaf's avatar.js (Apache-2.0, see NOTICE).
    var avatarSelector = 'img.sw-avatar-image, img.sw-attachment-image';
    function markFinished(image) {
        if (!image.getAttribute('src')) { return; }
        if (!image.complete) {
            image.dataset.status = 'loading';
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
        dialogReturnIds.forEach(function (source, id) { if (!document.getElementById(id)) dialogReturnIds.delete(id); });
        records.forEach(function (record) {
            if (record.type === 'attributes' && record.target.matches(avatarSelector)) {
                delete record.target.dataset.status;
                markFinished(record.target);
            }
            record.addedNodes.forEach(scanAvatars);
            record.addedNodes.forEach(scanIndeterminateCheckboxes);
            // Also when the attribute is removed: the box then leaves the mixed state.
            if (record.type === 'attributes' && record.attributeName === 'data-sw-indeterminate'
                && record.target instanceof HTMLInputElement) syncIndeterminate(record.target);
            record.addedNodes.forEach(scanUploads);
            record.addedNodes.forEach(scanPopups);
            record.removedNodes.forEach(cleanupPopups);
            if (record.type === 'attributes' && record.target instanceof Element) {
                if (record.target.hasAttribute('data-sw-tooltip')) setupTooltip(record.target);
                else if (tooltipStates.has(record.target)) removeTooltip(record.target);
            }
        });
    }).observe(document.documentElement, {subtree: true, childList: true, attributes: true,
        attributeFilter: ['src', 'srcset', 'data-sw-indeterminate', 'data-sw-tooltip', 'data-tooltip-placement', 'data-tooltip-mode', 'data-tooltip-delay', 'aria-describedby']});
    scanAvatars(document);

    // Responsive sidebar: ordinary navigation on desktop, a modal-like native popover on mobile.
    // Own only the inert attributes we add, so caller-owned disabled regions survive close/Ajax.
    var sidebarDesktop = window.matchMedia('(width >= 48rem)');
    var sidebarStates = new Map();
    function sidebarNav(provider) {
        return Array.from(provider.querySelectorAll('.sw-sidebar')).find(function (nav) {
            return nav.closest('.sw-sidebar-provider') === provider;
        });
    }
    function sidebarTriggers(nav) {
        return Array.from(document.querySelectorAll('.sw-sidebar-trigger')).filter(function (trigger) {
            return trigger.getAttribute('aria-controls') === nav.id || trigger.getAttribute('popovertarget') === nav.id;
        });
    }
    function sidebarControls(nav) {
        return Array.from(nav.querySelectorAll(tabbable + ',summary')).filter(function (node) {
            return node.tabIndex >= 0 && !node.matches(':disabled') && visibleControl(node);
        });
    }
    function focusSidebar(nav) {
        // An action may already have moved focus into the panel or a nested menu/dialog before the timer runs.
        if (nav.contains(document.activeElement) || document.activeElement.closest('dialog:modal')) return;
        var current = nav.querySelector('a[aria-current="page"]');
        (current && visibleControl(current) ? current : sidebarControls(nav)[0] || nav).focus();
    }
    function sidebarInert(state) {
        var branch = state.nav;
        while (branch && branch !== document.body) {
            Array.from(branch.parentElement ? branch.parentElement.children : []).forEach(function (sibling) {
                if (sibling !== branch && !sibling.inert) { sibling.inert = true; state.inerted.add(sibling); }
            });
            branch = branch.parentElement;
        }
    }
    function sidebarRelease(state) {
        clearTimeout(state.focusTimer);
        state.inerted.forEach(function (node) { node.inert = false; });
        state.inerted.clear();
    }
    function sidebarSync(provider, state, open) {
        if (open === undefined) open = state.nav.matches(':popover-open');
        sidebarTriggers(state.nav).forEach(function (trigger) {
            trigger.setAttribute('aria-controls', state.nav.id);
            if (sidebarDesktop.matches && state.nav.dataset.placement === 'header') {
                trigger.removeAttribute('popovertarget'); trigger.removeAttribute('aria-expanded'); return;
            }
            trigger.setAttribute('aria-expanded', String(sidebarDesktop.matches
                ? state.nav.dataset.collapsible === 'none' || provider.dataset.state !== 'collapsed' : open));
            if (sidebarDesktop.matches) trigger.removeAttribute('popovertarget');
            else trigger.setAttribute('popovertarget', state.nav.id);
        });
    }
    function sidebarAnnounce(provider, state, open) {
        provider.dispatchEvent(new CustomEvent('sw-sidebar-toggle', {bubbles: true,
            detail: {state: provider.dataset.state, open: open, mobile: !sidebarDesktop.matches}}));
    }
    function sidebarClose(state, restore) {
        var focused = state.nav.contains(document.activeElement);
        hidePopup(state.nav); sidebarRelease(state);
        if (restore && focused && state.source && state.source.isConnected && visibleControl(state.source)) state.source.focus();
    }
    function sidebarToggle(provider, state, source) {
        state.source = source || sidebarTriggers(state.nav)[0];
        if (!sidebarDesktop.matches) {
            if (state.nav.matches(':popover-open')) sidebarClose(state, true);
            else showPopup(state.nav, state.source);
            return;
        }
        if (state.nav.dataset.collapsible === 'none' || state.nav.dataset.placement === 'header') return;
        provider.dataset.state = provider.dataset.state === 'collapsed' ? 'expanded' : 'collapsed';
        if (provider.dataset.sidebarRemember !== 'false') {
            var name = provider.dataset.sidebarCookie || 'sw-sidebar-state';
            if (/^[A-Za-z0-9_-]+$/.test(name)) document.cookie = name + '=' + provider.dataset.state
                + ';path=/;max-age=31536000;SameSite=Lax' + (location.protocol === 'https:' ? ';Secure' : '');
        }
        if (activeTooltip && state.nav.contains(activeTooltip.source)) hideTooltip(activeTooltip);
        if (provider.dataset.state === 'collapsed' && state.nav.contains(document.activeElement)
            && (state.nav.dataset.collapsible !== 'icon' || document.activeElement.matches('summary'))
            && state.source) state.source.focus();
        sidebarSync(provider, state); sidebarAnnounce(provider, state, false);
    }
    function scanSidebars() {
        sidebarStates.forEach(function (state, provider) {
            if (!provider.isConnected || sidebarNav(provider) !== state.nav) {
                sidebarClose(state, false); sidebarStates.delete(provider);
            }
        });
        document.querySelectorAll('.sw-sidebar-provider').forEach(function (provider) {
            var nav = sidebarNav(provider);
            if (!nav) return;
            var state = sidebarStates.get(provider);
            if (!state) {
                state = {nav: nav, source: null, inerted: new Set(), focusTimer: 0};
                sidebarStates.set(provider, state);
            }
            sidebarSync(provider, state);
            if (nav.matches(':popover-open') && !sidebarDesktop.matches) {
                sidebarInert(state);
                focusSidebar(nav);
            }
        });
    }
    document.addEventListener('beforetoggle', function (event) {
        var nav = event.target;
        if (!(nav instanceof Element) || !nav.matches('.sw-sidebar')) return;
        var provider = nav.closest('.sw-sidebar-provider'), state = sidebarStates.get(provider);
        if (!state) return;
        var opening = event.newState === 'open';
        if (opening && sidebarDesktop.matches) { event.preventDefault(); return; }
        if (opening) {
            state.source = event.source || state.source || sidebarTriggers(nav)[0];
            sidebarInert(state);
            state.focusTimer = setTimeout(function () {
                if (!nav.isConnected || !nav.matches(':popover-open')) return;
                focusSidebar(nav);
            }, 0);
        } else {
            var focused = nav.contains(document.activeElement);
            sidebarRelease(state);
            if (focused && state.source && state.source.isConnected && visibleControl(state.source)) state.source.focus();
        }
        sidebarSync(provider, state, opening); sidebarAnnounce(provider, state, opening);
    }, true);
    document.addEventListener('toggle', function (event) {
        if (event.target instanceof Element && event.target.matches('.sw-sidebar')) placeToaster();
    }, true);
    document.addEventListener('click', function (event) {
        if (!(event.target instanceof Element)) return;
        var trigger = event.target.closest('.sw-sidebar-trigger');
        if (trigger) {
            var nav = document.getElementById(trigger.getAttribute('aria-controls'));
            var provider = nav && nav.closest('.sw-sidebar-provider'), state = sidebarStates.get(provider);
            if (state) {
                state.source = trigger;
                if (sidebarDesktop.matches) { event.preventDefault(); sidebarToggle(provider, state, trigger); }
            }
        }
        var summary = event.target.closest('summary.sw-sidebar-menu-button');
        if (summary && sidebarDesktop.matches) {
            var summaryNav = summary.closest('.sw-sidebar');
            var summaryProvider = summaryNav && summaryNav.closest('.sw-sidebar-provider');
            var summaryState = sidebarStates.get(summaryProvider);
            if (summaryState && summaryNav.dataset.collapsible === 'icon' && summaryProvider.dataset.state === 'collapsed') {
                event.preventDefault(); summary.parentElement.open = true;
                sidebarToggle(summaryProvider, summaryState, sidebarTriggers(summaryNav)[0]);
            }
        }
        var link = event.target.closest('.sw-sidebar a[href]');
        if (link && !link.hasAttribute('data-sidebar-keep-open')) {
            var sidebar = link.closest('.sw-sidebar'), entry = sidebarStates.get(sidebar.closest('.sw-sidebar-provider'));
            if (entry && sidebar.matches(':popover-open')) setTimeout(function () { sidebarClose(entry, true); }, 0);
        }
    }, true);
    document.addEventListener('keydown', function (event) {
        if (event.defaultPrevented || !(event.target instanceof Element)) return;
        var target = event.target;
        if ((event.ctrlKey || event.metaKey) && !event.altKey && !event.shiftKey && event.key.toLowerCase() === 'b'
            && !target.closest('input,textarea,select,[contenteditable]:not([contenteditable="false"]),dialog:modal')) {
            var provider = target.closest('.sw-sidebar-provider');
            if (!provider && sidebarStates.size === 1) provider = sidebarStates.keys().next().value;
            var state = sidebarStates.get(provider);
            if (state && (!sidebarDesktop.matches || state.nav.dataset.collapsible !== 'none' && state.nav.dataset.placement !== 'header')) {
                event.preventDefault(); sidebarToggle(provider, state, sidebarTriggers(state.nav)[0]);
            }
            return;
        }
        var nav = target.closest('.sw-sidebar:popover-open');
        if (!nav || sidebarDesktop.matches || target.closest('dialog:modal')) return;
        var state = sidebarStates.get(nav.closest('.sw-sidebar-provider'));
        if (!state) return;
        if (event.key === 'Escape') {
            // Let a customizable native select dismiss its own picker first.
            var select = target.closest('select.sw-select');
            if (select && CSS.supports('appearance', 'base-select') && select.matches(':open')) return;
            // A native invoker can still own focus before the nested popup's focus timer runs.
            // Consume Escape here too, so the browser and this handler cannot close two layers.
            var nested = Array.from(nav.querySelectorAll(':is(' + popupSelector + ',.sw-select-content,.sw-combobox-content):popover-open')).pop();
            event.preventDefault();
            if (nested) Shadwick.closePopover(nested.id);
            else sidebarClose(state, true);
        }
        if (event.key === 'Tab') {
            // Match the menu's Tab behavior even when focus is still on its invoker.
            var nestedMenu = Array.from(nav.querySelectorAll(menuSelector + ':popover-open')).pop();
            if (nestedMenu) Shadwick.closePopover(nestedMenu.id);
            var controls = sidebarControls(nav), first = controls[0], last = controls[controls.length - 1];
            var focused = document.activeElement;
            if (!first) { event.preventDefault(); nav.focus(); }
            else if (event.shiftKey && (focused === first || focused === nav)) { event.preventDefault(); last.focus(); }
            else if (!event.shiftKey && (focused === last || focused === nav)) { event.preventDefault(); first.focus(); }
        }
    });
    sidebarDesktop.addEventListener('change', function () {
        sidebarStates.forEach(function (state, provider) {
            if (activeTooltip && state.nav.contains(activeTooltip.source)) hideTooltip(activeTooltip);
            sidebarClose(state, true); sidebarSync(provider, state);
            if (state.nav.contains(document.activeElement) && !visibleControl(document.activeElement)) {
                var trigger = sidebarTriggers(state.nav)[0]; if (trigger) trigger.focus();
            }
        });
    });
    window.addEventListener('pagehide', function () { sidebarStates.forEach(function (state) { sidebarClose(state, false); }); });
    window.addEventListener('pageshow', scanSidebars);
    new MutationObserver(function (records) {
        if (records.some(function (record) { return record.type === 'childList'; })) scanSidebars();
    }).observe(document.documentElement, {subtree: true, childList: true});
    scanSidebars();

    // ---- Tabs: Shadleaf's ARIA pattern, with delegated listeners for Wicket Ajax replacement ----
    var tabRoots = new WeakMap();
    function ownTabs(root) {
        return Array.from(root.querySelectorAll('[role="tab"]')).filter(function (tab) { return tab.closest('.sw-tabs') === root; });
    }
    function enabledTab(tab) { return !tab.matches(':disabled,[aria-disabled="true"],[hidden]'); }
    function tabPanel(root, tab) {
        var panel = document.getElementById(tab.getAttribute('aria-controls'));
        return panel && panel.matches('[role="tabpanel"]') && panel.closest('.sw-tabs') === root ? panel : null;
    }
    function tabFrom(target) { return target instanceof Element ? target.closest('.sw-tabs [role="tab"]') : null; }
    function activateTab(root, tab, announce, keepFocused) {
        var focused = keepFocused && root.dataset.activation === 'manual' ? ownTabs(root).find(function (candidate) {
            return candidate === document.activeElement && enabledTab(candidate);
        }) : null;
        var state = tabRoots.get(root) || {}; state.activeId = tab.id; tabRoots.set(root, state);
        ownTabs(root).forEach(function (other) {
            var active = other === tab, panel = tabPanel(root, other);
            other.setAttribute('aria-selected', String(active));
            other.setAttribute('tabindex', (focused ? other === focused : active) && enabledTab(other) ? '0' : '-1');
            if (!panel) return;
            var shown = active && panel.hasAttribute('hidden');
            if (active) { panel.removeAttribute('hidden'); panel.setAttribute('tabindex', '0'); }
            else { panel.setAttribute('hidden', enabledTab(other) ? 'until-found' : 'hidden'); panel.setAttribute('tabindex', '-1'); }
            if (shown && announce) panel.dispatchEvent(new CustomEvent('sw-tabs-show'));
        });
    }
    function tabAddress(tab) {
        if (!(tab instanceof HTMLAnchorElement) || !tab.hasAttribute('href') || tab.hasAttribute('download')
            || tab.target && tab.target !== '_self') return null;
        try {
            var url = new URL(tab.href);
            return url.origin === location.origin && /^(http|https):$/.test(url.protocol) ? url : null;
        } catch (error) { return null; }
    }
    function chooseTab(tab, keepHash) {
        if (!enabledTab(tab)) return;
        var root = tab.closest('.sw-tabs');
        if (tab.getAttribute('aria-selected') === 'true') return;
        activateTab(root, tab, true);
        var url = tabAddress(tab);
        if (url) {
            if (keepHash) url.hash = location.hash;
            try { history.replaceState(history.state, '', url.href); } catch (error) { /* The content still switches. */ }
        }
    }
    function scanTabs() {
        document.querySelectorAll('.sw-tabs').forEach(function (root) {
            var tabs = ownTabs(root), state = tabRoots.get(root);
            var current = state && tabs.find(function (tab) { return tab.id === state.activeId && enabledTab(tab); });
            if (!current) current = tabs.find(function (tab) { return enabledTab(tab) && tab.getAttribute('aria-selected') === 'true'; })
                || tabs.find(enabledTab) || tabs.find(function (tab) { return tab.getAttribute('aria-selected') === 'true'; });
            if (current) activateTab(root, current, false, true);
        });
    }
    document.addEventListener('click', function (event) {
        var tab = tabFrom(event.target);
        if (!tab || event.defaultPrevented) return;
        if (!enabledTab(tab)) { event.preventDefault(); return; }
        if (tab instanceof HTMLAnchorElement && (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || !tabAddress(tab))) return;
        event.preventDefault(); chooseTab(tab);
    });
    document.addEventListener('keydown', function (event) {
        var tab = tabFrom(event.target);
        if (!tab || event.defaultPrevented || event.ctrlKey || event.metaKey || event.altKey || !enabledTab(tab)) return;
        var root = tab.closest('.sw-tabs'), tabs = ownTabs(root).filter(enabledTab), index = tabs.indexOf(tab);
        if (index < 0 || !tabs.length) return;
        var vertical = root.dataset.orientation === 'vertical', manual = root.dataset.activation === 'manual';
        var forward = getComputedStyle(root).direction === 'rtl' ? -1 : 1, offset = 0, next;
        if (event.key === 'ArrowRight' && !vertical) offset = forward;
        else if (event.key === 'ArrowLeft' && !vertical) offset = -forward;
        else if (event.key === 'ArrowDown' && vertical) offset = 1;
        else if (event.key === 'ArrowUp' && vertical) offset = -1;
        else if (event.key === 'Home') next = tabs[0];
        else if (event.key === 'End') next = tabs[tabs.length - 1];
        else if (event.key === ' ' || event.key === 'Enter') { event.preventDefault(); chooseTab(tab); return; }
        else return;
        if (offset) next = tabs[(index + offset + tabs.length) % tabs.length];
        event.preventDefault();
        ownTabs(root).forEach(function (other) { other.setAttribute('tabindex', other === next ? '0' : '-1'); });
        next.focus(); if (!manual) chooseTab(next);
    });
    document.addEventListener('focusout', function (event) {
        var tab = tabFrom(event.target); if (!tab) return;
        var root = tab.closest('.sw-tabs'), next = tabFrom(event.relatedTarget);
        if (root.dataset.activation !== 'manual' || next && next.closest('.sw-tabs') === root) return;
        ownTabs(root).forEach(function (other) {
            other.setAttribute('tabindex', other.getAttribute('aria-selected') === 'true' && enabledTab(other) ? '0' : '-1');
        });
    });
    document.addEventListener('beforematch', function (event) {
        var panel = event.target instanceof Element ? event.target.closest('[role="tabpanel"]') : null;
        while (panel) {
            var root = panel.closest('.sw-tabs');
            var tab = root && ownTabs(root).find(function (candidate) { return tabPanel(root, candidate) === panel; });
            if (tab) chooseTab(tab, true);
            panel = panel.parentElement && panel.parentElement.closest('[role="tabpanel"]');
        }
    }, true);
    // Disabled native summaries are skipped without script too; this also prevents programmatic keyboard activation.
    ['click', 'keydown'].forEach(function (type) {
        document.addEventListener(type, function (event) {
            if (!(event.target instanceof Element)) return;
            var summary = event.target.closest('summary');
            if (summary && summary.parentElement.matches('.sw-accordion-item[data-disabled],.sw-collapsible[data-disabled]')
                && (type === 'click' || event.key === 'Enter' || event.key === ' ')) event.preventDefault();
        }, true);
    });
    new MutationObserver(scanTabs).observe(document.documentElement, {childList: true, subtree: true});
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', scanTabs); else scanTabs();

    // ---- Toasts: Shadleaf anatomy/interactions and Sonner-style stacking ----
    // The newest toast is the toaster's last child. Stack geometry (offsets, heights) goes into one constructable
    // stylesheet keyed by toast id, never into style attributes; without constructable stylesheets the toasts stay
    // a plain scrolling column.
    var toaster = null, toastStates = new Map(), toastReturnFocus = null, toastHover = false;
    var toastCounter = 0, toastSheet = null, toastResize = null, toastLayoutFrame = 0;
    var toastConfig = {position: 'bottom-end', visibleToasts: 3};
    var toastPositions = ['top-start', 'top-center', 'top-end', 'bottom-start', 'bottom-center', 'bottom-end'];
    function toastStackSupported() {
        try {
            return typeof CSSStyleSheet === 'function' && Array.isArray(document.adoptedStyleSheets)
                && typeof CSSStyleSheet.prototype.replaceSync === 'function';
        } catch (error) { return false; }
    }
    function toastStyles(rules) {
        if (!toastSheet) {
            if (!rules) return;
            toastSheet = new CSSStyleSheet();
            document.adoptedStyleSheets = document.adoptedStyleSheets.concat([toastSheet]);
        }
        toastSheet.replaceSync(rules);
    }
    function toastList() {
        return toaster ? Array.from(toaster.children).filter(function (node) { return node.classList.contains('sw-toast'); }) : [];
    }
    // Which toasts show and where: the newest in front, at most visibleToasts, each further back and smaller. A
    // collapsed toast behind the front one takes the front's height, so only the front one's height is measured
    // then; expanded, every toast's own height is.
    function layoutToasts() {
        if (!toaster || !toaster.hasAttribute('data-stacked')) return;
        toaster.toggleAttribute('data-expanded', toastHover || toaster.contains(document.activeElement));
        var expanded = toaster.hasAttribute('data-expanded'), visible = toastConfig.visibleToasts;
        var offset = 0, count = 0, rules = [], newestFirst = toastList().reverse();
        newestFirst.forEach(function (toast, index) {
            var state = toastStates.get(toast) || {}, shown = index < visible;
            toast.toggleAttribute('data-hidden', !shown);
            toast.toggleAttribute('data-behind', shown && index > 0);
            if (index === 0 || expanded) state.height = toast.offsetHeight;
            rules.push('#' + CSS.escape(toast.id) + '{--sw-toast-index:' + (shown ? index : visible - 1)
                + ';--sw-toast-offset:' + offset + 'px}');
            if (shown) { offset += state.height || 0; count++; }
        });
        var front = newestFirst[0] && toastStates.get(newestFirst[0]);
        rules.push('#sw-toaster{--sw-toast-front-height:' + (front && front.height || 0) + 'px;--sw-toast-stack-sum:'
            + offset + 'px;--sw-toast-count:' + Math.max(count, 1) + '}');
        toastStyles(rules.join('\n'));
    }
    function scheduleToastLayout() {
        if (toastLayoutFrame) return;
        toastLayoutFrame = requestAnimationFrame(function () { toastLayoutFrame = 0; layoutToasts(); updateToastTimers(); });
    }
    Shadwick.configureToasts = function (options) {
        options = options || {};
        if (toastPositions.indexOf(options.position) >= 0) toastConfig.position = options.position;
        var visible = Number(options.visibleToasts);
        if (Number.isInteger(visible) && visible >= 1 && visible <= 10) toastConfig.visibleToasts = visible;
        if (toaster) { toaster.dataset.position = toastConfig.position; updateToastTimers(); }
    };
    function topToastModal() {
        return document.activeElement.closest && document.activeElement.closest('dialog:modal,.sw-sidebar:popover-open')
            || Array.from(document.querySelectorAll('dialog:modal,.sw-sidebar:popover-open')).pop() || null;
    }
    function placeToaster() {
        if (!toaster || !toastStates.size) return;
        var parent = topToastModal() || document.body;
        var focused = toaster.contains(document.activeElement) ? document.activeElement : null;
        if (toaster.parentElement !== parent) parent.appendChild(toaster);
        if (toaster.showPopover) {
            if (!toaster.matches(':popover-open')) toaster.showPopover();
        } else toaster.dataset.fallback = '';
        if (focused && document.activeElement !== focused && visibleControl(focused)) focused.focus({preventScroll: true});
    }
    function toastPaused() {
        return toastHover || document.hidden || toaster && toaster.contains(document.activeElement);
    }
    function updateToastTimers() {
        layoutToasts();
        toastStates.forEach(function (state, toast) {
            if (!toast.isConnected) { clearTimeout(state.timer); toastStates.delete(toast); return; }
            // A toast waiting behind the visible ones keeps its time until it shows
            if (toastPaused() || toast.hasAttribute('data-hidden')) {
                if (state.timer) {
                    clearTimeout(state.timer); state.timer = 0;
                    state.remaining = Math.max(1000, state.remaining - (Date.now() - state.started));
                }
            } else if (!state.timer && state.remaining > 0) {
                state.started = Date.now();
                state.timer = setTimeout(function () { dismissToast(toast); }, state.remaining);
            }
        });
    }
    function dismissToast(toast) {
        var state = toastStates.get(toast);
        if (!state) return;
        clearTimeout(state.timer); toastStates.delete(toast);
        if (toastResize) toastResize.unobserve(toast);
        var focused = toast.contains(document.activeElement);
        var next = toast.nextElementSibling || toast.previousElementSibling;
        toast.remove();
        if (!toastStates.size) {
            if (toaster && toaster.matches(':popover-open')) toaster.hidePopover();
            if (toaster) toaster.remove();
            toaster = null; toastHover = false; forgetToastLayout();
        } else layoutToasts();
        if (focused) {
            var target = next && next.querySelector('.sw-toast-close') || toastReturnFocus;
            if (target && target.isConnected && visibleControl(target)) target.focus({preventScroll: true});
        }
        if (!toaster) toastReturnFocus = null;
        updateToastTimers();
    }
    function toastIcon(variant) {
        // Fixed Lucide shapes (ISC, see NOTICE). All notification text is assigned through textContent.
        var paths = {
            success: ['M9 12l2 2 4-4', 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0'],
            info: ['M12 16v-4', 'M12 8h.01', 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0'],
            warning: ['M10.3 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.7 3.86a2 2 0 0 0-3.4 0', 'M12 9v4', 'M12 17h.01'],
            error: ['M7.86 2h8.28L22 7.86v8.28L16.14 22H7.86L2 16.14V7.86Z', 'm15 9-6 6', 'm9 9 6 6']
        };
        if (!paths[variant]) return null;
        var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('viewBox', '0 0 24 24'); svg.setAttribute('fill', 'none');
        svg.setAttribute('stroke', 'currentColor'); svg.setAttribute('stroke-width', '2');
        svg.setAttribute('stroke-linecap', 'round'); svg.setAttribute('stroke-linejoin', 'round');
        svg.setAttribute('aria-hidden', 'true');
        paths[variant].forEach(function (d) { var path = document.createElementNS(svg.namespaceURI, 'path'); path.setAttribute('d', d); svg.appendChild(path); });
        return svg;
    }
    Shadwick.toast = function (message, options, dismissLabel, regionLabel) {
        var legacy = typeof options === 'string', level = legacy ? options : null;
        options = legacy ? {variant: level === 'fatal' ? 'error' : level === 'warn' ? 'warning' : level,
            dismissLabel: dismissLabel, regionLabel: regionLabel, duration: 0} : options || {};
        var variant = ['default', 'success', 'info', 'warning', 'error'].includes(options.variant) ? options.variant : 'default';
        var duration = options.duration == null ? 5000 : Number(options.duration);
        if (!Number.isFinite(duration) || duration < 0 || duration > 2147483647) duration = 5000;
        var source = document.activeElement;
        if (toaster && !toaster.isConnected) {
            toastStates.forEach(function (state) { clearTimeout(state.timer); });
            toastStates.clear(); toaster = null; toastHover = false; forgetToastLayout();
        }
        if (!toaster) {
            toaster = document.createElement('div'); toaster.id = 'sw-toaster'; toaster.className = 'sw-toaster';
            toaster.setAttribute('popover', 'manual'); toaster.setAttribute('role', 'region');
            toaster.dataset.position = toastConfig.position;
            if (toastStackSupported()) {
                toaster.setAttribute('data-stacked', '');
                if (typeof ResizeObserver === 'function') toastResize = new ResizeObserver(scheduleToastLayout);
            }
            toastReturnFocus = source;
            document.body.appendChild(toaster);
        }
        toaster.setAttribute('aria-label', options.regionLabel || 'Notifications (Alt+T)');
        var toast = document.createElement('div'); toast.className = 'sw-toast'; toast.id = 'sw-toast-' + (++toastCounter);
        toast.dataset.level = level || variant;
        if (variant !== 'default') toast.dataset.variant = legacy && variant === 'error' ? 'destructive' : variant;
        var icon = toastIcon(variant); if (icon) toast.appendChild(icon);
        var content = document.createElement('div'); content.className = 'sw-toast-content'; content.id = toast.id + '-content';
        content.setAttribute('role', variant === 'error' ? 'alert' : 'status'); content.setAttribute('aria-atomic', 'true');
        var title = options.title == null || String(options.title).trim() === '' ? null : document.createElement('div');
        if (title) { title.className = 'sw-toast-title'; content.appendChild(title); }
        var description = document.createElement('div');
        description.className = title ? 'sw-toast-description' : 'sw-toast-message'; content.appendChild(description);
        toast.appendChild(content);
        var close = document.createElement('button'); close.type = 'button'; close.className = 'sw-button sw-toast-close';
        close.dataset.variant = 'ghost'; close.dataset.size = 'icon';
        close.setAttribute('aria-label', options.dismissLabel || 'Dismiss notification'); close.textContent = '\u00d7';
        close.setAttribute('aria-describedby', content.id);
        close.addEventListener('click', function () { dismissToast(toast); }); toast.appendChild(close);
        toastStates.set(toast, {remaining: duration, timer: 0, started: 0, height: 0});
        toaster.appendChild(toast); placeToaster(); updateToastTimers();
        if (toastResize) toastResize.observe(toast);
        // Register the live region first, then set plain text, without moving keyboard focus.
        requestAnimationFrame(function () {
            if (!toast.isConnected) return;
            if (title) title.textContent = String(options.title);
            description.textContent = String(message == null ? '' : message);
            layoutToasts();
        });
        return toast.id;
    };
    document.addEventListener('pointerover', function (event) {
        if (toaster && toaster.contains(event.target) && !toaster.contains(event.relatedTarget) && event.pointerType !== 'touch') {
            toastHover = true; updateToastTimers();
        }
    });
    document.addEventListener('pointerout', function (event) {
        if (toaster && toaster.contains(event.target) && !toaster.contains(event.relatedTarget)) {
            toastHover = false; updateToastTimers();
        }
    });
    document.addEventListener('focusin', function (event) {
        if (!toaster) return;
        if (toaster.contains(event.target) && !toaster.contains(event.relatedTarget) && event.relatedTarget) toastReturnFocus = event.relatedTarget;
        updateToastTimers();
    });
    document.addEventListener('focusout', function () { queueMicrotask(updateToastTimers); });
    document.addEventListener('visibilitychange', updateToastTimers);
    document.addEventListener('keydown', function (event) {
        if (!toaster || !(event.target instanceof Element)) return;
        if (event.key === 'Escape' && toaster.contains(event.target)) {
            event.preventDefault(); event.stopImmediatePropagation(); dismissToast(event.target.closest('.sw-toast'));
        } else if (event.altKey && !event.ctrlKey && !event.metaKey && !event.shiftKey && event.code === 'KeyT') {
            event.preventDefault();
            if (!toaster.contains(document.activeElement)) toastReturnFocus = document.activeElement;
            var newest = toaster.lastElementChild;
            if (newest) newest.querySelector('.sw-toast-close').focus();
        }
    }, true);
    document.addEventListener('toggle', function (event) {
        if (event.target instanceof HTMLDialogElement) { placeToaster(); updateToastTimers(); }
    }, true);
    new MutationObserver(function () {
        if (!toaster) return;
        updateToastTimers();
        if (!toaster.isConnected || !toastStates.size) {
            if (toaster.isConnected) toaster.remove();
            toaster = null; toastHover = false; toastReturnFocus = null; forgetToastLayout();
        } else placeToaster();
    }).observe(document.documentElement, {childList: true, subtree: true});
    function forgetToastLayout() {
        if (toastResize) { toastResize.disconnect(); toastResize = null; }
        if (toastLayoutFrame) { cancelAnimationFrame(toastLayoutFrame); toastLayoutFrame = 0; }
        if (toastSheet) toastSheet.replaceSync('');
    }
    window.addEventListener('resize', function () { if (toaster) scheduleToastLayout(); });
    window.addEventListener('pagehide', function () {
        toastStates.forEach(function (state) { clearTimeout(state.timer); }); toastStates.clear();
        if (toaster) toaster.remove(); toaster = null; toastReturnFocus = null; toastHover = false; forgetToastLayout();
    });

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
    // Chromium can choose the implicitly focusable scrolling body when a sheet opens.
    // Prefer its first form field in that case; preserve explicit autofocus and focus on informational bodies.
    function focusSheetField(dialog) {
        if (!dialog.matches('.sw-sheet[open]') || !dialog.contains(document.activeElement)
            || !document.activeElement.matches('.sw-sheet-body-content')) return;
        var fields = dialog.querySelectorAll('input:enabled:not([type=hidden]), select:enabled, textarea:enabled');
        var field = Array.from(fields).find(function (candidate) {
            return candidate.getClientRects().length && getComputedStyle(candidate).visibility !== 'hidden';
        });
        if (field) field.focus();
    }
    document.addEventListener('toggle', function (event) {
        if (event.target instanceof HTMLDialogElement && event.target.open) focusSheetField(event.target);
    }, true);

    // A server action can replace the opener while its dialog is open. Remember its stable id,
    // and prefer a menu's trigger to the menu item hidden by showModal(). Native focus handles other closes.
    var dialogReturnIds = new Map();
    document.addEventListener('close', function (event) {
        var dialog = event.target;
        if (!(dialog instanceof HTMLDialogElement) || !dialog.isConnected || !dialogReturnIds.has(dialog.id)) return;
        var sourceId = dialogReturnIds.get(dialog.id); dialogReturnIds.delete(dialog.id);
        setTimeout(function () {
            if (document.querySelector('dialog:modal')) return;
            var active = document.activeElement, source = document.getElementById(sourceId);
            if (source && visibleControl(source) && (active === document.body || active === document.documentElement || dialog.contains(active))) source.focus();
        }, 0);
    }, true);

    Shadwick.showDialog = function (id) {
        var dialog = document.getElementById(id);
        if (!dialog) {
            return;
        }
        if (!dialog.open) {
            if (!dialogReturnIds.has(id)) {
                var source = document.activeElement;
                var popup = source instanceof Element ? source.closest(popupSelector) : null;
                if (popup) source = popupState(popup).source || popupTriggers(popup)[0] || source;
                if (source && source.id) dialogReturnIds.set(id, source.id);
            }
            dialog.showModal();
            focusSheetField(dialog);
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
                focusSheetField(dialog);
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

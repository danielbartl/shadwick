(() => {
  'use strict';
  const preview = document.querySelector('#component-preview');
  const skinSelect = document.querySelector('#skin');
  const stylesheet = document.querySelector('#component-styles');
  const dialog = document.querySelector('#project-dialog');
  const toast = document.querySelector('#site-toast');
  let toastTimer;
  let skinRequest = 0;

  function notify(message) {
    clearTimeout(toastTimer);
    toast.textContent = message;
    toast.classList.add('is-visible');
    toastTimer = setTimeout(() => toast.classList.remove('is-visible'), 6000);
  }
  function updateDescription() {
    document.querySelector('#preview-description').textContent = `${skinSelect.value === 'lyra' ? 'Lyra' : 'Vega'} · ${preview.classList.contains('dark') ? 'Dark' : 'Light'}`;
  }
  document.querySelector('.demo-submit').disabled = false;
  document.querySelector('#open-project').disabled = false;
  skinSelect.disabled = false;
  skinSelect.addEventListener('change', async () => {
    const requested = skinSelect.value;
    const sequence = ++skinRequest;
    const next = document.createElement('link');
    next.rel = 'stylesheet';
    next.href = new URL(`shadwick-${requested}.css`, stylesheet.href).href;
    next.media = 'not all';
    try {
      await new Promise((resolve, reject) => {
        next.addEventListener('load', resolve, {once: true});
        next.addEventListener('error', reject, {once: true});
        stylesheet.after(next);
      });
      if (sequence !== skinRequest) { next.remove(); return; }
      stylesheet.href = next.href;
      next.remove();
      preview.dataset.skin = requested;
      updateDescription();
    } catch {
      next.remove();
      skinSelect.value = preview.dataset.skin;
      notify('This skin could not load. Please try again.');
    }
  });
  document.querySelectorAll('[data-mode]').forEach(button => {
    button.disabled = false;
    button.addEventListener('click', () => {
      const dark = button.dataset.mode === 'dark';
      preview.classList.toggle('dark', dark);
      dialog.classList.toggle('dark', dark);
      document.querySelectorAll('[data-mode]').forEach(control => control.setAttribute('aria-pressed', String(control === button)));
      updateDescription();
    });
  });

  document.addEventListener('click', event => {
    if (!(event.target instanceof Element)) return;
    const addon = event.target.closest('.sw-input-group-addon');
    const interactive = event.target.closest('a[href], button, input, select, textarea, label, summary, [tabindex], [contenteditable]');
    if (!addon || (interactive && addon.contains(interactive))) return;
    const control = addon.parentElement.querySelector(':scope > :is(.sw-input-group-input, .sw-input-group-textarea)');
    if (control && !control.disabled) control.focus();
  });

  const preferences = document.querySelector('.choice-demo');
  preferences.addEventListener('change', () => {
    const updates = document.querySelector('#demo-updates').checked ? 'on' : 'off';
    const quiet = document.querySelector('#demo-quiet').checked ? 'on' : 'off';
    const digest = preferences.querySelector('input[name="digest"]:checked').value;
    document.querySelector('#demo-preferences-status').textContent =
      `Email updates ${updates} · Quiet hours ${quiet} · ${digest} digest.`;
  });

  const profileForm = document.querySelector('#profile-form');
  function validate(input, errorId, message) {
    const valid = input.validity.valid && input.value.trim().length > 0;
    input.setAttribute('aria-invalid', String(!valid));
    document.getElementById(errorId).textContent = valid ? '' : message;
    return valid;
  }
  profileForm.addEventListener('submit', event => {
    event.preventDefault();
    const name = document.querySelector('#demo-name');
    const email = document.querySelector('#demo-email');
    const nameValid = validate(name, 'demo-name-error', 'Please enter your name.');
    const emailValid = validate(email, 'demo-email-error', 'Please enter a valid email address.');
    const seats = document.querySelector('#demo-seats');
    const seatsValid = validate(seats, 'demo-seats-error', 'Choose between 1 and 10 seats.');
    if (!nameValid || !emailValid || !seatsValid) { (!nameValid ? name : !emailValid ? email : seats).focus(); return; }
    const plan = document.querySelector('#demo-plan');
    notify(`Looking good, ${name.value.trim().split(/\s+/)[0]}. ${plan.selectedOptions[0].text} plan selected. This demo stays in your browser.`);
  });
  document.querySelectorAll('[data-demo-variant]').forEach(button => {
    button.addEventListener('click', () => notify(`${button.dataset.demoVariant} button. Same Wicket API, a different expression.`));
  });

  const projectName = document.querySelector('#project-name');
  const projectDate = document.querySelector('#project-date');
  document.querySelector('#open-project').addEventListener('click', () => {
    document.querySelector('#project-form').reset();
    document.querySelector('#project-error').textContent = '';
    projectName.removeAttribute('aria-invalid');
    projectDate.removeAttribute('aria-invalid');
    document.querySelector('#project-date-error').textContent = '';
    dialog.showModal();
    projectName.focus();
  });
  document.querySelectorAll('[data-close-dialog]').forEach(button => button.addEventListener('click', () => dialog.close()));
  document.querySelector('#project-form').addEventListener('submit', event => {
    event.preventDefault();
    if (!validate(projectName, 'project-error', 'Give your idea a name.')) { projectName.focus(); return; }
    const dateValid = projectDate.validity.valid;
    projectDate.setAttribute('aria-invalid', String(!dateValid));
    document.querySelector('#project-date-error').textContent = dateValid ? '' : 'Choose a valid launch date.';
    if (!dateValid) { projectDate.focus(); return; }
    dialog.close();
    const launch = projectDate.value ? ` Launch date: ${projectDate.value}.` : '';
    const file = document.querySelector('#project-file').files[0];
    const attachment = file ? ` Reference: ${file.name}.` : '';
    notify(`“${projectName.value.trim()}” is a great start.${launch}${attachment} Demo only — nothing was saved.`);
  });
  dialog.addEventListener('close', () => document.querySelector('#open-project').focus());
  let backdropPointer = false;
  function outside(event) {
    const rect = dialog.getBoundingClientRect();
    return event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom;
  }
  dialog.addEventListener('pointerdown', event => { backdropPointer = event.target === dialog && outside(event); });
  dialog.addEventListener('click', event => { if (backdropPointer && event.target === dialog && outside(event)) dialog.close(); backdropPointer = false; });

  async function copy(text) {
    if (navigator.clipboard && window.isSecureContext) { await navigator.clipboard.writeText(text); return; }
    // The fallback also supports a local HTTP preview without the Clipboard API.
    const field = document.createElement('textarea');
    field.value = text;
    field.setAttribute('readonly', '');
    field.className = 'clipboard-fallback';
    document.body.appendChild(field);
    field.select();
    const succeeded = document.execCommand('copy');
    field.remove();
    if (!succeeded) throw new Error('Clipboard unavailable');
  }
  document.querySelectorAll('[data-copy], [data-copy-text]').forEach(button => {
    button.addEventListener('click', async () => {
      const text = button.dataset.copyText ?? document.getElementById(button.dataset.copy).textContent;
      try { await copy(text); notify('Copied. Go make something good.'); }
      catch { notify('Copy is unavailable here. You can select and copy the code directly.'); }
      button.focus();
    });
  });
})();

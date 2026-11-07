/* Catalog navigation and source tools. Demo interactions remain owned by Wicket/Shadwick. */
(() => {
  const init = () => {
    const menu = document.querySelector('.ex-catalog-menu');
    const field = document.getElementById('catalog-query');
    if (menu && field) {
      const media = matchMedia('(max-width: 60rem)');
      menu.open = !media.matches;
      media.addEventListener('change', event => { menu.open = !event.matches; });
      field.closest('.ex-catalog-search').hidden = false;
      const normalize = value => value.normalize('NFKD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
      const filter = () => {
        const query = normalize(field.value.trim());
        for (const item of document.querySelectorAll('[data-catalog-search]')) item.hidden = !normalize(item.dataset.catalogSearch).includes(query);
        for (const group of document.querySelectorAll('[data-catalog-group]')) group.hidden = !group.querySelector('[data-catalog-search]:not([hidden])');
        const count = document.querySelectorAll('.ex-catalog-nav [data-catalog-search]:not([hidden])').length;
        document.querySelector('.ex-catalog-search-status').textContent = query ? count + (count === 1 ? ' example found' : ' examples found') : '';
        document.querySelector('.ex-catalog-no-results').hidden = count !== 0;
      };
      field.addEventListener('input', filter);
      field.addEventListener('search', filter);
      field.addEventListener('keydown', event => { if (event.key === 'Escape') { field.value = ''; filter(); } });
      document.addEventListener('keydown', event => {
        if ((event.ctrlKey || event.metaKey) && !event.altKey && event.key.toLowerCase() === 'k') {
          event.preventDefault(); menu.open = true; field.focus(); field.select();
        }
      });
      const active = menu.querySelector('[aria-current=page]');
      const body = menu.querySelector('.ex-catalog-menu-body');
      if (active && menu.open) body.scrollTop = Math.max(0, active.getBoundingClientRect().top - body.getBoundingClientRect().top - 100);
    }
    const preferences = document.querySelector('.ex-preferences');
    if (preferences) {
      document.addEventListener('click', event => { if (!preferences.contains(event.target)) preferences.open = false; });
      preferences.addEventListener('keydown', event => { if (event.key === 'Escape') { preferences.open = false; preferences.querySelector('summary').focus(); } });
    }
    for (const button of document.querySelectorAll('[data-source-copy]')) {
      button.hidden = false;
      const copyName = button.getAttribute('aria-label');
      button.addEventListener('click', async () => {
        const file = button.closest('.ex-source-file');
        const code = file.querySelector('code');
        const status = button.closest('.ex-source').querySelector('.ex-copy-status');
        try {
          await navigator.clipboard.writeText(code.textContent);
          const filename = file.querySelector('summary > span').textContent;
          button.textContent = 'Copied'; button.setAttribute('aria-label', 'Copied ' + filename);
          status.textContent = filename + ' copied.';
          setTimeout(() => { button.textContent = 'Copy code'; button.setAttribute('aria-label', copyName); }, 1600);
        } catch {
          const range = document.createRange(); range.selectNodeContents(code);
          const selection = getSelection(); selection.removeAllRanges(); selection.addRange(range);
          file.querySelector('.ex-source-scroll').focus();
          status.textContent = 'Code selected. Press Control+C or Command+C to copy.';
        }
      });
    }
  };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init, { once: true }); else init();
})();

/* Application URL/history glue. Wicket owns requests and DOM patches. */
(() => {
  let searchCallback, navigationCallback;
  const url = (value, push) => {
    const next = new URL(value, location.href);
    if (push && next.href !== location.href) history.pushState({ clover: true }, '', next);
    else history.replaceState({ clover: true }, '', next);
  };
  const search = callback => { searchCallback = callback; history.replaceState({ clover: true }, '', location.href); };
  addEventListener('popstate', () => {
    const params = new URL(location.href).searchParams;
    if (searchCallback && params.get('view') === 'projects' && document.querySelector('.studio-filters')) {
      Wicket.Ajax.ajax({ u: searchCallback, ep: ['q', 'status', 'sort'].map((name, i) => ({ name: ['studioQ', 'studioStatus', 'studioSort'][i], value: params.get(name) || '' })) });
    } else if (navigationCallback) {
      Wicket.Ajax.ajax({ u: navigationCallback, ep: [
        { name: 'studioView', value: params.get('view') || 'overview' },
        { name: 'studioProject', value: params.get('project') || '0' },
        ...['q', 'status', 'sort'].map(name => ({ name: 'studio' + name, value: params.get(name) || '' }))
      ] });
    }
  });
  const locale = (language, skin, stylesheet) => {
    document.documentElement.lang = language;
    const links = () => [...document.querySelectorAll('link[href*="/css/shadwick-"]')];
    const activate = selected => { for (const link of links()) link.disabled = link !== selected; };
    let selected = links().find(link => link.href.includes('/css/shadwick-' + skin + '-'));
    if (selected) {
      selected.disabled = false;
      if (selected.sheet) activate(selected);
      else selected.addEventListener('load', () => activate(selected), { once: true });
    } else {
      // Shadwick pins a page's ordinary Ajax CSS to its original skin. This global preference
      // explicitly loads another public bundle; keep prior bundles for switching back.
      selected = document.createElement('link');
      selected.rel = 'stylesheet'; selected.href = stylesheet;
      selected.nonce = document.querySelector('link[rel=stylesheet]')?.nonce || '';
      selected.addEventListener('load', () => activate(selected), { once: true });
      document.head.appendChild(selected);
    }
  };
  const parameters = id => {
    const form = document.getElementById(id);
    return { studioQ: form.elements.q.value, studioStatus: form.elements.status.value, studioSort: form.elements.sort.value };
  };
  window.CloverStudio = { url, search, locale, parameters, navigation: callback => {
    navigationCallback = callback; history.replaceState({ clover: true }, '', location.href);
  } };
})();

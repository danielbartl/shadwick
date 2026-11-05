(() => {
  'use strict';
  const root = document.querySelector('.studio-invoices');
  if (!root) return;
  const body = root.querySelector('tbody');
  const rows = Array.from(body.children);
  const filter = root.querySelector('#studio-invoice-query');
  const all = root.querySelector('#studio-invoice-all');
  const controls = Array.from(root.querySelectorAll('[data-invoice-page]'));
  const sortButtons = Array.from(root.querySelectorAll('[data-invoice-sort]'));
  const selected = new Set();
  let page = 0, sort = 'invoice', ascending = true;
  const eligible = () => rows.filter(row => !row.hidden && row.isConnected && !row.querySelector('[data-invoice-select]').disabled);
  function selection() {
    const visible = eligible();
    const count = visible.filter(row => selected.has(row.dataset.invoice)).length;
    all.disabled = !visible.length;
    all.checked = !!visible.length && count === visible.length;
    all.indeterminate = count > 0 && count < visible.length;
    root.querySelector('#studio-invoice-selection').textContent = `${selected.size} selected`;
  }
  function render() {
    const query = filter.value.trim().toLowerCase();
    const found = rows.filter(row => `${row.dataset.invoice} ${row.dataset.customer} ${row.dataset.email}`.toLowerCase().includes(query));
    found.sort((a, b) => {
      const comparison = sort === 'amount' ? Number(a.dataset.amount) - Number(b.dataset.amount) : a.dataset.invoice.localeCompare(b.dataset.invoice);
      return (ascending ? comparison : -comparison) || a.dataset.invoice.localeCompare(b.dataset.invoice);
    });
    const pages = Math.max(1, Math.ceil(found.length / 3));
    page = Math.min(page, pages - 1);
    body.replaceChildren(...found);
    found.forEach((row, index) => { row.hidden = index < page * 3 || index >= (page + 1) * 3; });
    root.querySelector('#studio-invoice-empty').hidden = found.length > 0;
    root.querySelector('#studio-invoice-summary').textContent = found.length ? `${page * 3 + 1}–${Math.min(found.length, (page + 1) * 3)} of ${found.length}` : '0 of 0';
    controls.forEach(button => {
      const value = button.dataset.invoicePage;
      button.disabled = value === 'previous' ? page === 0 : value === 'next' ? page === pages - 1 : false;
      if (value !== 'previous' && value !== 'next') {
        button.hidden = Number(value) >= pages;
        button.dataset.variant = Number(value) === page ? 'outline' : 'ghost';
        if (Number(value) === page) button.setAttribute('aria-current', 'page');
        else button.removeAttribute('aria-current');
      }
    });
    sortButtons.forEach(button => {
      const active = button.dataset.invoiceSort === sort;
      if (active) button.closest('th').setAttribute('aria-sort', ascending ? 'ascending' : 'descending');
      else button.closest('th').removeAttribute('aria-sort');
      button.querySelector('use').setAttribute('href', `./assets/icons.svg#${active ? ascending ? 'chevron-up' : 'chevron-down' : 'chevrons-up-down'}`);
    });
    selection();
  }
  root.querySelector('#studio-invoice-filter').addEventListener('submit', event => { event.preventDefault(); page = 0; render(); });
  filter.disabled = false;
  filter.addEventListener('input', () => { page = 0; render(); });
  sortButtons.forEach(button => {
    button.disabled = false;
    button.addEventListener('click', () => {
      ascending = sort === button.dataset.invoiceSort ? !ascending : true;
      sort = button.dataset.invoiceSort; page = 0; render();
    });
  });
  controls.forEach(button => button.addEventListener('click', () => {
    const value = button.dataset.invoicePage;
    page = value === 'previous' ? page - 1 : value === 'next' ? page + 1 : Number(value);
    render();
  }));
  rows.forEach(row => {
    const checkbox = row.querySelector('[data-invoice-select]');
    checkbox.disabled = checkbox.dataset.locked === 'true';
    checkbox.addEventListener('change', () => {
      if (checkbox.checked) selected.add(row.dataset.invoice); else selected.delete(row.dataset.invoice);
      selection();
    });
    const preview = row.querySelector('[data-invoice-preview]');
    preview.disabled = false;
    preview.addEventListener('click', () => {
      document.querySelector('#studio-invoice-title').textContent = row.dataset.invoice;
      for (const field of ['customer', 'email', 'status']) document.querySelector(`#studio-invoice-detail-${field}`).textContent = row.dataset[field];
      document.querySelector('#studio-invoice-detail-amount').textContent = new Intl.NumberFormat('en', {style:'currency', currency:'EUR'}).format(Number(row.dataset.amount) / 100);
    });
  });
  all.addEventListener('change', () => {
    eligible().forEach(row => {
      row.querySelector('[data-invoice-select]').checked = all.checked;
      if (all.checked) selected.add(row.dataset.invoice); else selected.delete(row.dataset.invoice);
    });
    selection();
  });
  root.querySelector('#studio-invoice-pages').hidden = false;
  render();
})();

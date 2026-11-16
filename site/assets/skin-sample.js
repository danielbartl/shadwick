// The gallery switches every sample between light and dark by changing only the frame's URL fragment.
// Each sample also reports its card's height, so the gallery can size every frame to the tallest skin.
(() => {
  'use strict';
  const apply = () => document.documentElement.classList.toggle('dark', location.hash === '#dark');
  apply();
  window.addEventListener('hashchange', apply);
  if (window.parent === window) return;
  const report = () => {
    const card = document.querySelector('.sample-card');
    if (!card) return;
    const padding = parseFloat(getComputedStyle(document.body).paddingTop) * 2;
    window.parent.postMessage({type: 'shadwick-sample-height', height: Math.ceil(card.getBoundingClientRect().height + padding)}, location.origin);
  };
  window.addEventListener('DOMContentLoaded', () => {
    report();
    if ('ResizeObserver' in window) new ResizeObserver(report).observe(document.querySelector('.sample-card'));
    if (document.fonts) document.fonts.ready.then(report);
  });
})();

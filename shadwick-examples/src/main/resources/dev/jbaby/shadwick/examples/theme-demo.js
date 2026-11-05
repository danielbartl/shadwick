(function () {
    function update() {
        var output = document.querySelector('[data-theme-demo-status]');
        if (!output || !window.ShadwickTheme) return;
        var mode = window.ShadwickTheme.getMode();
        output.textContent = 'Preference: ' + mode.charAt(0).toUpperCase() + mode.slice(1)
            + ' · Currently ' + (window.ShadwickTheme.isDark() ? 'dark' : 'light') + '.';
    }
    document.addEventListener('sw-theme-change', update);
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', update); else update();
})();

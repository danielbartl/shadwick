# Theming

Shadwick uses shadcn/ui's CSS variables (`--background`, `--primary`, `--radius`, …), so any shadcn theme, from the
[theme generator](https://ui.shadcn.com/themes) or [tweakcn](https://tweakcn.com), drops in unchanged: redefine the
variables in your own stylesheet. Dark mode is `class="dark"` on `<html>`.

## Light, Dark and System preferences

Add `ThemeBehavior` to the page's `<html>` container in your base page, then place `SwThemeToggle` wherever a visitor
can choose an appearance. The default menu offers Light, Dark and System; the optional button switches light/dark.
The browser owns the preference, so switching needs no Ajax request or Wicket session:

```java
// Application startup: defaults are SYSTEM and "shadwick-theme".
Shadwick.install(this).setDefaultThemeMode(ThemeMode.SYSTEM).setThemeStorageKey("my-app-theme");

// Base page, with <html wicket:id="html"> in its markup.
TransparentWebMarkupContainer html = new TransparentWebMarkupContainer("html");
html.add(Sw.Behaviors.theme());
add(html);
add(new SwThemeToggle("appearance")); // <div wicket:id="appearance"></div>
add(new SwThemeToggle("darkMode").setPresentation(SwThemeToggle.Presentation.BUTTON));
```

The root behavior loads an external, parser-blocking priority script before the styles and body, applying the saved
or system preference before first paint. Keep it on every page that should follow the preference. A toggle alone
can initialize the document interactively, but does not provide the early root configuration. No inline scripts,
event handlers or style attributes are added, and Wicket's strict CSP stays unchanged.

`ThemeMode.LIGHT`, `DARK` and `SYSTEM` describe appearance independently of the selected skin or any named color
palette. The default menu uses the existing native popover menu, with arrow keys, Home/End, typeahead, Escape and
focus return. Its selected item has `aria-checked`; the simple button uses `aria-pressed` for resolved dark mode.
All controls for the root stay synchronized, including after Ajax replacement. Use `setLabel(String/IModel)`,
`setVariant`, `setSide` and `setAlign` to customize a control; `getTrigger()` and `getMenu()` expose its parts. The
default labels are resources `SwThemeToggle.label`, `.darkMode`, `.light`, `.dark` and `.system`.

The storage key contains `light`, `dark` or explicitly `system`; a missing or invalid value uses the configured
fallback. Storing System explicitly lets it override an application's Light/Dark fallback. System follows operating
system changes; explicit choices stay fixed. Changes synchronize across tabs with the same origin/key and are
restored on reload, full navigation and browser history restoration. If storage is blocked, the current page still
follows the visitor's choice. Printing temporarily resolves to light without changing the preference. With JavaScript
disabled, controls hide and the server fallback remains usable (System renders light).

`ThemeBehavior.setDefaultMode` and `.setStorageKey` override application defaults for one root. A preview on a light
host can use its own container with `setRememberChoice(false)` and a toggle's `setTarget(container)`; this keeps the
preview independent and stores nothing. The landing studio uses this setup and mirrors its state onto overlays and
notifications. The enclosing landing page keeps its own appearance.

For application scripts, `ShadwickTheme.getMode()`, `.isDark()`, `.setMode('system')` and `.refresh()` work on the
document root; an optional final argument selects a root element or its ID. A bubbling `sw-theme-change` event on the
root carries `{mode, dark, rootId}`. These browser preferences do not implement an authenticated, server-side theme
store. The **Theme** showcase (`/theme`) demonstrates persistence, synchronized controls, Ajax refresh and navigation;
other showcase pages keep their URL-based light/dark reference views.

## Embedded styles

If your application supplies its own reset and page styles (for example, its own Tailwind build), use the embedded
bundle. It includes the same tokens and components, with no preflight, page base styles or Tailwind theme variables:

```java
Shadwick.install(this).setSkin(Skin.LYRA).setBundle(Bundle.EMBEDDED);
```

Configure the bundle at application startup. The default is `Bundle.STANDALONE`.

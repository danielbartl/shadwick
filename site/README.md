# Shadwick landing page

A static, responsive website for GitHub Pages. Plain HTML, CSS and JavaScript; no site package installation, client framework or external runtime requests. Geist is self-hosted under its SIL Open Font License (`assets/fonts/OFL.txt`). The icon sprite uses Lucide shapes (ISC; see the root NOTICE).

The component studio uses **the library's compiled embedded CSS and component scripts**, with a custom green token palette. Skin selection loads the actual Vega, Lyra or Nova bundle. Light/Dark/System preference applies to the studio and its overlays. The studio theme toggle, user menus, sidebar, header, breadcrumb and first-in-body skip link use core styles too. Demo forms, buttons, dialogs, sheets, dropdown menus, popovers, tooltips and toasts run locally in the browser; no information is submitted or persisted. The site explicitly distinguishes this static preview from Wicket's server-side behavior.

## Build and preview

From the repository root:

```sh
./mvnw -B -pl shadwick-core -am verify
node site/build.mjs
node site/check.mjs
node site/serve.mjs
```

Open **http://127.0.0.1:4173/shadwick/**. The preview deliberately uses a project subpath to catch asset URL problems before deployment. Stop it with Ctrl+C. An optional positional argument changes the port (`node site/serve.mjs 4174`).

If Node isn't on your PATH, use the Node installed by Maven: `./shadwick-core/target/node/node` instead of `node` (Windows: `shadwick-core\target\node\node.exe`).

`build.mjs` writes the ignored `site/dist/`, copying the three minified skin bundles and core component scripts from `shadwick-core/target/classes`. Never edit these copied assets or commit generated site output. Edit the page in `index.html`, presentation in `assets/site.css` and site interactions in `assets/site.js`. All asset URLs are relative. CSS, scripts and image/icon references receive content hashes in their query strings, including dynamically selected skins and table icons, so a new deployment does not reuse stale assets from a previous version.

## GitHub Pages

1. In the repository's **Settings → Pages**, select **GitHub Actions** as the build and deployment source.
2. Commit and push the site and the library sources it uses to `main`.
3. The **Website** workflow builds and tests the library, builds and checks the static site, uploads its artifact, and deploys it to the `github-pages` environment. Pull requests build and check the artifact without deploying. The workflow can also be run manually.

The default URL for this repository is `https://danielbartl.github.io/shadwick/`. The workflow derives canonical and social URLs from `GITHUB_REPOSITORY`. To use a custom domain, set `SITE_URL` in the website build step and configure the domain in GitHub Pages settings. See [GitHub's custom workflow documentation](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages).

## Design and checks

The page uses an ivory canvas, forest-green accents, Geist typography and a live component composition. Shadwick’s original four-leaf clover represents a lucky find and a welcoming developer experience, with a botanical nod to Shadleaf. The vector mark lives in `assets/logo.svg`, with the same artwork in the favicon and a one-color version in the icon sprite; the social card carries the same identity. A reduced-motion preference disables decorative animation. Navigation, skin/mode controls, code copying, field validation, menus and overlays work with a keyboard. The page remains readable with JavaScript disabled; interactive controls are progressively enabled.

`check.mjs` verifies IDs, local anchors, SVG symbols, fonts, required assets, license files, subpath-safe references and matching content hashes. Before shipping visual changes, inspect desktop and narrow mobile layouts in a browser, try all skins and modes, test invalid form input, menu arrows/typeahead/Tab, toast Alt+T/dismissal/timer pausing, Escape and focus return on overlays (including the menu’s delete confirmation), and copy a code snippet. Browser testing should serve the built `dist/` at `/shadwick/`, as the preview does. Keep marketing typography scoped to its sections so it cannot override component dialog headings.

The studio includes the library sidebar: desktop icon rail, expandable navigation and a native mobile panel.
Its provider uses `data-sidebar-remember="false"`, so the preview stores no sidebar cookie.

The Layout selector moves that one navigation between its column and the site header; it never duplicates the
navigation or its IDs. Header placement shows the desktop link row and the same mobile panel, with branding/account
footer marked panel-only. Switching closes any open panel and nested menu first. Explore is a native submenu; in
the icon rail activating its summary expands the sidebar. Collection actions use a native menu beside the link.
Try both layouts at desktop/mobile widths, including keyboard focus, nested-menu Escape and switching back to a
previously expanded sidebar. Layout selection is transient and the no-script preview keeps its native sidebar.

Account menus in the sidebar and header share a transient demo state. My account/Settings focus the corresponding
preview fields; Sign out/Sign in switch the preview state locally without contacting an authentication service.

The appearance menu uses the core `SwThemeToggle` anatomy and `ShadwickTheme` runtime. It explicitly targets
`#component-preview`, whose theme root defaults to Light and uses `data-sw-theme-remember="false"`. It ignores and
does not write a visitor's stored application preference. System follows the operating system; reloading resets
the preview to Light. The host stays ivory. The external `theme.js` loads before styles; the preview's
`sw-theme-change` event mirrors the resolved appearance onto dialogs, sheets and the toast viewport. The toolbar's
icon reflects its targeted preview, even though the toolbar sits outside that root. With scripting disabled the
appearance control hides. Check all three choices in all skins, including OS changes while System is selected.

Actions/Details in the button card uses the core tabs runtime and styles. Actions stays the initial panel; Details
contains a native single accordion and standalone collapsible. Keyboard arrows switch tabs, and the native sections
use Enter/Space. The tabs wrapper is the direct card-content child so the selected skin supplies its inset;
narrow cards stack the action buttons. Inactive panels remain zero-sized even with host card padding. The accordion has a fixed unique
name because this static preview is never replaced over Ajax; Wicket's repeating component generates names per render.
With scripting disabled the initial Actions panel remains visible. Check open disclosure content, switching back to
Actions and opening its menu, in all skins/modes and both sidebar/header compositions. These previews store no state.

The studio's illustration attachment uses the core attachment anatomy, embedded CSS and image fallback runtime.
Its stretched native button opens a labelled image-preview dialog; the separate download link saves the actual SVG
without opening the preview. Both remain usable without script in browsers with invoker commands. The preview follows
the studio theme and supports Escape, its native close button and backdrop dismissal. Check the trigger/actions in
all skins and modes at desktop/mobile widths. The thumbnail has explicit dimensions, cover cropping and a fallback
icon; the preview preserves the illustration's aspect ratio. The original SVG is shared with the Wicket `/attachments` showcase.

The studio now includes a full-width invoice table using the actual table/pagination CSS. The six original rows
remain visible without script. With scripting, the preview filters names/invoices, sorts by invoice or amount,
shows three rows per page, keeps selected IDs across its pages and opens native invoice preview dialogs. Select-all
changes only eligible visible rows; INV-2005 is locked. All preview state is transient, with no storage or requests.
The preview demonstrates appearance/interactions; Wicket's providers and server callbacks own real table operations.
Check sorting, empty filters, checkboxes/indeterminate, pagination, preview/close/focus and dark-mode dialogs in all
skins, responsive widths and navigation placements. Source is assets/table-demo.js, without a runtime dependency.

The studio plan field now uses the actual custom-select anatomy and `select.js`. Browsers with customizable
select support use their styled native picker; other modern browsers progressively get the listbox. Both
retain the native select and its existing transient preview behavior. Check keyboard selection, reset/native
fallback, all skins/modes and narrow layouts; no inline positioning styles or persistent preferences are added.

The Project teams field uses the actual combobox CSS and `combobox.js`, backed by a native multiple select.
Type to filter, press Enter to add a chip, use Backspace/remove buttons to remove it, and Clear teams to reset
the choice locally. It has no server search or persistent storage. Without script it remains a labelled native
multiple select. Check chips, keyboard, narrow layouts, all skins/modes and native no-script selection.

The landing studio includes the actual inline calendar CSS/runtime for Next catch-up, and its project dialog
uses the date picker over the original native #project-date input. Selection stays local; the existing save toast
includes the ISO launch date. Check grid keys, nested popup focus/Escape, reset, all skins/modes and narrow
layouts. The catch-up field remains a native date input without script.

The Plans and progress section previews the actual chart CSS/runtime over native SVG and a data disclosure.
The adjacent month view uses server-style static HTML for October–December 2026; native fragment links
switch the visible month without script. Both previews follow the selected skin/theme and navigation layout.

Nova is available in the skin selector. Its compact geometry and muted footers come from the actual Nova bundle;
the studio's extra decorative card shadow applies only to Vega/Lyra so Nova retains its flat outlined cards.
Switching the selector also updates the accessible preview description to the selected skin's name.

The collection catalog links to all implemented families, including calendars/date pickers, month view, charts,
data tables/pagination, disclosures, attachments and reusable layouts. The status copy lists the three shipped
skins and keeps remaining skins, translations and documentation as future work; Maven Central remains pending.

# Changelog

## Unreleased

Development continues on `0.2.0-SNAPSHOT`.

### Changed

- Toasts stack, as in shadcn/ui's Sonner and Shadleaf: the newest in front, at most three showing, spreading out on
  hover or keyboard focus, with older ones waiting (their time stopped) until a newer one closes. In 0.1.0 every toast
  showed in a scrolling column. `ShadwickSettings.setVisibleToasts(...)` raises the count (1–10); browsers without
  constructable stylesheets keep the column. The toaster's distance from the edge is now 1.5rem (1rem on narrow screens).
- `SwFileDropzone` shows each chosen file as a small attachment (name and size, a rejected file as an error card), as
  Shadleaf does, in place of 0.1.0's own rows. Applications styling `.sw-file-upload-item/-name/-size` should style the
  attachment's parts instead.

### Added

- Docs: six copy-ready layout recipes (collapses to icons, grouped by section, collapsible sub-menus, inset with
  secondary navigation, a table of contents on the end, navigation in the header), each running in the examples app
  at `/recipes/…` and printed from its source; and a "Coming from shadcn/ui" guide.
- `SwSidebar.setGroupsOpen(true)` / `SwAppLayout.sidebarGroupsOpen()`: every sidebar group starts open, not only the one
  holding the current page.
- `SwChart`: `setShowCategoryGrid(true)` adds a grid line per category; `setCategoryTickMargin`,
  `setValueTickMargin` and `setPlotMargin` set the space between tick labels and the plot and at both ends of the plot,
  through a nonce-protected header stylesheet.
- `SwFileDropzone`: upload progress during Ajax submits (each file's share, Processing, Upload failed, and Uploaded after
  `clearSelection`), a Cancel upload button, `setLayout(Layout.GRID)` cards and `setPreviews(true)` image thumbnails,
  drawn into a canvas so no Content-Security-Policy change is needed. New messages in all nine languages.
- Field layout: `BaseSwField.setOrientation(HORIZONTAL)` puts a label beside its control, and `RESPONSIVE` does so while
  the enclosing field group is at least 28rem wide. `Sw.Behaviors.fieldGroup()` / `SwFieldGroup` stack fields with the
  skin's spacing and provide that container.
- `SwFormErrors`: a form's errors in one titled, destructive alert (global errors, or all with `Show.ALL`, each field
  error linking to its control), with an optional heading level and autofocus after normal and Ajax submits. Title
  translated into all nine languages.
- Toast positions: `ShadwickSettings.setToastPosition(ToastBehavior.Position...)` for a corner or the middle of the top
  or bottom edge (logical start/end), and `ToastBehavior.configure(...)`/`configureScript(...)` to change position and
  count for one page.
- Button sizes `EXTRA_SMALL`, `ICON_EXTRA_SMALL`, `ICON_SMALL` and `ICON_LARGE` in all nine skins. On a touch screen,
  the three smallest icon sizes get an invisible 44-pixel hit area, except inside input groups.
- `SwCheckBox.setIndeterminate(...)`: the mixed state ("some of a group selected"), from a model read on each render.
- `SwEmptyState.setBorder(false)` leaves out the dashed outline, which stays the default.
- `SwIcon.setSize(...)` and `setStrokeWidth(...)` for a fixed pixel size and a lighter or heavier stroke.
- `SwConfirmDialog.setSize(Size.SMALL)` for a narrow confirmation whose buttons share the width and stack when a label
  doesn't fit, and `setIcon(...)`/`setIconVariant(...)` for a decorative icon above the title.

### Fixed

- A custom select (`SwSelect`) whose native picker is replaced by Shadwick's fallback no longer leaves the native
  select's own button in the tab order when the browser parses that button after the select was enhanced. Screen
  readers could otherwise reach a control hidden from them; this also made a browser test fail intermittently.
- A link styled as a button (`sw-button`) inside a dialog, alert-dialog, sheet, empty-state or field description,
  or inside accordion content, is no longer underlined like a prose link. Fixed in all nine skins, following
  Shadleaf's equivalent fix.

## 0.1.0 — 2026-11-09

Published coordinates: `dev.jbaby:shadwick:0.1.0`.
The initial release includes the Wicket 11 / Java 21 migration.

- Server-rendered Wicket components and behaviors for forms, choices, calendars, overlays, menus, navigation,
  layouts, tables, charts, record displays and feedback.
- Field-owned labels, descriptions and validation messages; native/Ajax form submission and focused partial updates.
- Sortable/editable tables with stable row keys, selection, CSV export, column choices and expandable details.
- Native SVG Cartesian/donut/pie charts, 100% stacked bars/areas, reference lines/bands, forecasts, sparklines and value labels, with accessible data tables and keyboard exploration.
- Scatter plots with independently modeled numeric X/Y series, fixed-size circle/square/diamond markers, exact tables and keyboard/touch/Ajax support.
- Heatmap matrices with SVG intensity tiles, exact native table values, signed/fixed ranges, distinct missing cells and two-dimensional keyboard/touch/Ajax exploration.
- Concentric radial bars with common scales, full/partial sweeps, configurable tracks/gaps/ends and native/keyboard/touch/Ajax exploration.
- Radar profiles with common dimensions, fixed/automatic scales, missing-value gaps, numbered dimension keys and native/keyboard/touch/Ajax exploration.
- Fieldsets/custom fields, money/percent/range controls, button groups/split buttons, timelines, lazy panels,
  freeform tags, avatar groups and steps indicators.
- Nine visual skins, light/dark appearance, standalone/embedded bundles and nine built-in message languages.
- Packaged CSS, scripts, markup, fonts and licenses; strict Wicket CSP without a frontend build in consuming apps.
- Searchable documentation, a source-backed component catalog, Clover Studio and portable agent guidance/templates.
- Human-readable lifecycle composition, a reviewed public/protected API baseline and serialization round-trip checks.
- Wicket 11.0.0 / Java 21 baseline, Java 21/25 CI, Java 21 Docker/runtime builds and Servlet 6.1 hosts; preserved inline validation and explicit placeholders.
- Bound Cartesian tooltips to the full chart width in narrow embedded RTL layouts, keeping ordinary currency values on one line alongside wider axis glyphs and long values contained; browser coverage for native table controls and label collision handling.

Requires Java 21+, Wicket 11.0.0 and Jakarta Servlet 6.1+. The library remains independent of Spring Boot.
Stored Wicket pages/sessions are not a cross-build migration format. See the
[release notes](docs/releases/0.1.0.md) and [compatibility guide](docs/guides/compatibility.md).

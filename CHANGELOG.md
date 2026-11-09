# Changelog

## Unreleased

Development continues on `0.2.0-SNAPSHOT`.

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

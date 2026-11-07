# Data, appearance and locale

## Data tables

`SwDataTable<T,S>` uses standard Wicket columns and a sortable data provider. `iterator(first,count)` and `size()`
must describe the same filtered dataset. End sorts with a stable unique key so equal values do not jump between
pages. Return detachable row models when records come from a database; paging indexes are zero-based.

Use stable serializable IDs for `setRowKey(...)`, selection and row actions. A column's visibility is presentation,
not authorization. Validate inline edits and mutations against current records. CSV export includes filtered rows
in provider order; the application owns access policy and its intended export scope. Keep storage and transactions
in application services rather than retaining repository sessions or request objects in pages.

Read [data tables](https://danielbartl.github.io/shadwick/docs/components/data-table/index.md) and
[advanced tables](https://danielbartl.github.io/shadwick/docs/components/advanced-table/index.md) for providers,
selection loaders, edit predicates, column choice, expansion and export behavior.

## Skins and themes

Choose a skin through `ShadwickSettings`/`Shadwick.install(...)` and the documented `Skin` values. User-specific
skin choices require a `SkinStore`; changing the shared default is not a per-user preference. Ordinary component
Ajax updates retain the page's pinned skin resources. Follow the skin guide when implementing a whole-app skin
change rather than appending arbitrary stylesheet URLs to each control callback.

Standalone and embedded bundles have different reset expectations. Keep an existing application's host CSS and
select its bundle deliberately. Light/dark appearance is separate from the nine skin geometries; use
`ThemeBehavior`/`SwThemeToggle` where browser preferences are desired. Avoid inline style overrides for routine
component variants and retain the strict CSP.

Read [skins](https://danielbartl.github.io/shadwick/docs/guides/skins/index.md) and
[theming](https://danielbartl.github.io/shadwick/docs/guides/theming/index.md) before implementing preference changes.

## Locale and accessibility

Set the Wicket session locale and document language. The library ships English plus German, Spanish, French,
Italian, Japanese, Dutch, Portuguese and Croatian messages. Regional locales affect dates/numbers and fallbacks.
Application titles, field labels, choice text and notification content still need application resource bundles.
Clover Studio's English/German menu is a demo choice, not a limit on library locales.

Give icon-only actions names, distinguish row-action labels, preserve keyboard interaction and return focus after
overlays. Keep error descriptions linked to their fields. Use the component's native fallback where available;
server search, modal editing and other Ajax flows need script.

Read [internationalization](https://danielbartl.github.io/shadwick/docs/guides/internationalization/index.md) and
[accessibility](https://danielbartl.github.io/shadwick/docs/guides/accessibility/index.md) for overrides and checks.

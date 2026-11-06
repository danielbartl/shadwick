# Component documentation

Guides for installing Shadwick and composing its Wicket components, with matching Java/HTML,
accessibility notes and source-based API references. Start with [installation](guides/installation.md)
and [Wicket integration](guides/wicket-integration.md).

The website build renders these Markdown sources at `/docs/` in the existing GitHub Pages site.
The website adds API references generated from the [Java API source](../shadwick-core/src/main/java/dev/jbaby/shadwick/).
Those sources provide declared constructors, methods and hooks when reading the Markdown directly.

## Getting started

- [Installation](guides/installation.md) — Install Shadwick, configure Wicket and run your first form.
- [Wicket integration](guides/wicket-integration.md) — Models, markup ownership, Ajax, forms, resources and application responsibilities.
- [Skins](guides/skins.md) — Choose from nine geometries with shared light and dark color tokens.
- [Theming](guides/theming.md) — Color tokens, appearance preferences and embedded bundles.
- [Internationalization](guides/internationalization.md) — Use nine message languages, regional formats and Wicket resource overrides.
- [Accessibility](guides/accessibility.md) — Naming, keyboard use, feedback, native fallbacks and application content.

## Actions and feedback

- [Buttons](components/buttons.md) — Ajax actions, submit buttons, variants, sizes and icon-only names.
- [Busy controls](components/busy-controls.md) — Show pending work and guard repeated Ajax activation.
- [Toasts](components/toasts.md) — Notifications, dismissal, timing and accessible announcements.
- [Feedback](components/feedback.md) — Inline validation, fenced feedback panels and feedback toasts.

## Forms and choices

- [Form fields](components/forms.md) — Labels, models, validators, inline errors and normal or Ajax submission.
- [Text and number inputs](components/inputs.md) — Text, email, password, URL, telephone, search, textarea and numeric input.
- [Native selections](components/native-selects.md) — Browser select and multi-select controls with typed Wicket choices.
- [Custom select](components/select.md) — Styled single selection with native and progressive listbox paths.
- [Combobox](components/combobox.md) — Search single or multiple choices, remotely or locally.
- [Date and time fields](components/date-time.md) — Native local date, time, month, week and date-time inputs.
- [Toggles and input groups](components/toggles-input-groups.md) — Pressed buttons, single/multiple toggle choices and input addons.
- [Checkboxes switches and radios](components/choices.md) — Boolean and multiple choices with native form semantics.
- [File upload](components/file-upload.md) — Native upload and dropzones, validation, metadata and storage callbacks.
- [Ratings](components/ratings.md) — Half-star read-only ratings and native whole-star radio inputs.

## Data and planning

- [Calendar and date picker](components/calendar.md) — Single dates, ranges, disabled days and native-backed popup pickers.
- [Month view](components/month-view.md) — Server-rendered monthly plans with locale-aware weeks and custom day slots.
- [Charts](components/charts.md) — Native SVG bars, lines and areas with exact accessible data.
- [Native tables](components/table.md) — Compose native, compact, grouped and sticky-column tables.
- [Data tables](components/data-table.md) — Providers, sort state, selection, row actions and bookmarkable filters.
- [Advanced tables](components/advanced-table.md) — CSV downloads, column choices, lazy row details and staged inline edits.
- [Pagination](components/pagination.md) — Independent pagers, summaries, page sizes and native URL factories.
- [Progress](components/progress.md) — Native progress and SVG rings with known or indeterminate values.
- [Meters](components/meters.md) — Native measurements with bounds and quality thresholds.

## Overlays

- [Dialog](components/dialog.md) — Modal content, validation, confirmation and focus restoration.
- [Confirmation dialog](components/confirmation-dialog.md) — Confirm actions with server state and explicit destructive choices.
- [Sheet](components/sheet.md) — Side panels using native dialogs and logical edges.
- [Popover](components/popover.md) — Native popover content with controlled placement and focus behavior.
- [Tooltip](components/tooltip.md) — Descriptions for focus and hover without replacing control names.
- [Dropdown menus](components/dropdown-menu.md) — Action menus, checked items, native placement and keyboard navigation.

## Navigation and layout

- [App shell](components/app-shell.md) — Header, breadcrumb and skip-link primitives for your own page structure.
- [Sidebar](components/sidebar.md) — Responsive navigation, submenus, row actions and logical placement.
- [App layout](components/app-layout.md) — Application chrome, named slots, menus and page locale.
- [User menu](components/user-menu.md) — Identity display, menu placements and native sign-in/sign-out integration.
- [Tabs](components/tabs.md) — Keyboard tab selection with server-configured panels.
- [Accordion](components/accordion.md) — Single or multiple native details sections with named triggers.
- [Collapsible](components/collapsible.md) — One native details disclosure with optional content.

## Display

Related measurements and ratings: [Progress](components/progress.md), [Meters](components/meters.md) and [Ratings](components/ratings.md).

- [Cards](components/cards.md) — Compose content into cards with caller-owned markup.
- [Badges](components/badges.md) — Short escaped labels and status indicators.
- [Alerts](components/alerts.md) — Persistent messages and composed alert content.
- [Avatars](components/avatars.md) — Pictures, initials, loading and image-failure fallbacks.
- [Icons](components/icons.md) — Bundled Lucide SVGs with decorative or named semantics.
- [Skeletons and spinners](components/loading.md) — Decorative placeholders and named loading indicators.
- [Empty state](components/empty-state.md) — A heading, explanation and optional next action.
- [Separators](components/separators.md) — Decorative or semantic horizontal and vertical dividers.
- [Keyboard keys](components/keyboard.md) — Display keys and shortcut sequences in semantic markup.
- [Attachments](components/attachments.md) — Stored file cards, image previews, downloads and grouped displays.

## Contributing

See [documentation maintenance](CONTRIBUTING.md) for the build, supported Markdown, coverage checks and compiled examples.

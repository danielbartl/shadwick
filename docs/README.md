# Component documentation

Guides for installing Shadwick and composing its Wicket components, with matching Java/HTML,
accessibility notes and source-based API references. Start with [installation](guides/installation.md)
and [Wicket integration](guides/wicket-integration.md).

The website build renders these Markdown sources at `/docs/` in the existing GitHub Pages site.
The website adds API references generated from the [Java API source](../shadwick-core/src/main/java/dev/jbaby/shadwick/).
Those sources provide declared constructors, methods and hooks when reading the Markdown directly.

The initial component collection includes compound and numeric fields, connected actions,
record details, timelines, lazy panels, tags, avatar groups and process steps. Shadwick `0.1.0` is available from Maven Central with Wicket 11 and Java 21+. See
[Compatibility](guides/compatibility.md) for the released API and Java/Wicket requirements.

## Getting started

- [Release preparation](guides/releases.md) — Release artifacts, signing, Central staging and publication.
- [Installation](guides/installation.md) — Install Shadwick, configure Wicket and run your first form.
- [Wicket integration](guides/wicket-integration.md) — Models, markup ownership, Ajax, forms, resources and application responsibilities.
- [Compatibility](guides/compatibility.md) — Java/Wicket requirements, the released API, stored pages and browser coverage.
- [Agent guidance](guides/agent-guidance.md) — Use the portable coding skill, working templates and machine-readable guides.
- [Clover Studio](guides/clover-studio.md) — Run the connected agency workspace and try the project-to-payment journey.
- [Skins](guides/skins.md) — Choose from nine geometries with shared light and dark color tokens.
- [Theming](guides/theming.md) — Color tokens, appearance preferences and embedded bundles.
- [Internationalization](guides/internationalization.md) — Use nine message languages, regional formats and Wicket resource overrides.
- [Accessibility](guides/accessibility.md) — Naming, keyboard use, feedback, native fallbacks and application content.

## Actions and feedback

- [Buttons](components/buttons.md) — Ajax actions, submit buttons, variants, sizes and icon-only names.
- [Button groups and split buttons](components/button-groups.md) — Connected controls and primary actions with related commands.
- [Busy controls](components/busy-controls.md) — Show pending work and guard repeated Ajax activation.
- [Toasts](components/toasts.md) — Notifications, dismissal, timing and accessible announcements.
- [Feedback](components/feedback.md) — Inline validation, fenced feedback panels and feedback toasts.

## Forms and choices

- [Fieldsets and custom fields](components/fieldsets-custom-fields.md) — Native groups and compound inputs.
- [Money and percentages](components/money-percent.md) — Exact localized decimal values.
- [Range field](components/range.md) — Native slider with server-validated bounds and steps.

- [Form fields](components/forms.md) — Labels, models, validators, inline errors and normal or Ajax submission.
- [Text and number inputs](components/inputs.md) — Text, email, password, URL, telephone, search, textarea and numeric input.
- [Native selections](components/native-selects.md) — Browser select and multi-select controls with typed Wicket choices.
- [Custom select](components/select.md) — Styled single selection with native and progressive listbox paths.
- [Combobox](components/combobox.md) — Search single or multiple choices, remotely or locally.
- [Freeform tags](components/tag-input.md) — Editable chips, comma-separated native fallback and list validation.
- [Date and time fields](components/date-time.md) — Native local date, time, month, week and date-time inputs.
- [Toggles and input groups](components/toggles-input-groups.md) — Pressed buttons, single/multiple toggle choices and input addons.
- [Checkboxes switches and radios](components/choices.md) — Boolean and multiple choices with native form semantics.
- [File upload](components/file-upload.md) — Native upload and dropzones, validation, metadata and storage callbacks.
- [Ratings](components/ratings.md) — Half-star read-only ratings and native whole-star radio inputs.

## Data and planning

- [Steps indicator](components/steps.md) — Ordered process stages, current state and optional completed-step selection.
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
- [Description lists](components/description-list.md) — Read-only record details with native term/value relationships.
- [Timelines](components/timeline.md) — Ordered activity histories with localized times and decorative markers.
- [Badges](components/badges.md) — Short escaped labels and status indicators.
- [Alerts](components/alerts.md) — Persistent messages and composed alert content.
- [Avatars](components/avatars.md) — Pictures, initials, loading/error fallbacks and overlapping team groups.
- [Icons](components/icons.md) — Bundled Lucide SVGs with decorative or named semantics.
- [Skeletons and spinners](components/loading.md) — Decorative placeholders and named loading indicators.
- [Lazy-loaded panels](components/lazy-load.md) — Deferred content with shared readiness polling and a native fallback.
- [Empty state](components/empty-state.md) — A heading, explanation and optional next action.
- [Separators](components/separators.md) — Decorative or semantic horizontal and vertical dividers.
- [Keyboard keys](components/keyboard.md) — Display keys and shortcut sequences in semantic markup.
- [Attachments](components/attachments.md) — Stored file cards, image previews, downloads and grouped displays.

## Contributing

See [documentation maintenance](CONTRIBUTING.md) for the build, supported Markdown, coverage checks and compiled examples.

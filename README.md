<img src="site/assets/logo.svg" alt="Shadwick four-leaf clover" width="56" height="56">

# Shadwick

shadcn/ui-style UI components for [Apache Wicket](https://wicket.apache.org): shadcn's look, tokens and component
catalog, rendered on the server by Wicket. Add one Maven dependency and you're done: no Node, no Tailwind, no CSS
build in your application.

Shadwick is inspired by [Shadleaf](https://github.com/wimdeblauwe/shadleaf), shadcn/ui for Thymeleaf, and adapts its
markup and CSS. It is not affiliated with or endorsed by Shadleaf or shadcn/ui. Its Java API follows
[Wicket Oat](https://github.com/danielbartl/wicket-oat): if you know Wicket Oat, moving to Shadwick is mostly
renaming `Oat` to `Sw`.

> **Status: Forms, overlays, app shell, disclosures, attachment displays and data tables implemented.** Buttons, native input fields, custom selects, comboboxes, calendars/date pickers, month views, SVG charts, choices, toggles,
> input groups, dialogs, menus, toasts, layouts, tabs, accordions, collapsibles, attachment cards/galleries, sortable/editable data tables with CSV export, row details and column choices, pagination, display components and feedback,
> in nine skins (vega, lyra, nova, maia, mira, luma, rhea, sera, sketch), light and dark. Standalone and embedded CSS bundles. Busy buttons, progress/meter controls and star ratings. Built-in messages in nine languages.
> Not yet on Maven Central.

Read the [component documentation](docs/README.md) for focused guides, matching markup, accessibility notes and
API references generated from the Java source. The existing README anchors below link to those guides.

## Agent guidance

Use the [Shadwick skill](shadwick/SKILL.md) with its supporting references and working Java/HTML templates.
The [agent guide](docs/guides/agent-guidance.md) explains how to add the portable package to a compatible coding
agent. [llms.txt](https://danielbartl.github.io/shadwick/llms.txt) links to machine-readable guides with
source-derived API references. No agent configuration is changed by this repository.

## Getting started

Requires Wicket 10 and Java 17. Until the first release, install it locally with `./mvnw install`, then:

```xml
<dependency>
    <groupId>dev.jbaby</groupId>
    <artifactId>shadwick</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Install it in your application's `init()`:

```java
@Override
protected void init() {
    super.init();
    Shadwick.install(this);                    // vega, shadcn's classic look
    // Shadwick.install(this).setSkin(Skin.LYRA); // boxy, small type, square corners
    // Shadwick.install(this).setSkin(Skin.NOVA); // compact controls, rounder fields, muted footers
}
```

That adds Shadwick's stylesheet to every page. It works under Wicket's default strict, nonce-based Content Security
Policy and adds nothing to it.

Use `getMarkupSettings().setStripWicketTags(true)` for the component markup, as the showcase does. This also keeps
development-only `<wicket:panel>` wrappers out of native `details`/`summary` relationships.

### Skins

Choose from nine geometries with shared light and dark color tokens.

[Skins](docs/guides/skins.md)

### Internationalization

Use nine message languages, regional formats and Wicket resource overrides.

[Internationalization](docs/guides/internationalization.md)

### Buttons

Ajax actions, submit buttons, variants, sizes and icon-only names.

[Buttons](docs/components/buttons.md)

### Busy Ajax controls

Show pending work and guard repeated Ajax activation.

[Busy controls](docs/components/busy-controls.md)

### Progress, meters and ratings

Native progress and SVG rings with known or indeterminate values.

[Progress](docs/components/progress.md) · [Meters](docs/components/meters.md) · [Ratings](docs/components/ratings.md)

### Form fields

Labels, models, validators, inline errors and normal or Ajax submission.

[Form fields](docs/components/forms.md)

### Everyday inputs

Text, email, password, URL, telephone, search, textarea and numeric input.

[Text and number inputs](docs/components/inputs.md)

### Native selections

Browser select and multi-select controls with typed Wicket choices.

[Native selections](docs/components/native-selects.md)

### Custom select

Styled single selection with native and progressive listbox paths.

[Custom select](docs/components/select.md)

### Combobox

Search single or multiple choices, remotely or locally.

[Combobox](docs/components/combobox.md)

### Month view

Server-rendered monthly plans with locale-aware weeks and custom day slots.

[Month view](docs/components/month-view.md)

### Charts

Native SVG bars, lines, areas and donuts with grouped/stacked series, forecast joins, independent formats, light/dark colors, curves/corners, value labels and exact data; decorative sparklines for cards and tables.

[Charts](docs/components/charts.md)

### Calendar and date picker

Single dates, ranges, disabled days and native-backed popup pickers.

[Calendar and date picker](docs/components/calendar.md)

### Native dates and times

Native local date, time, month, week and date-time inputs.

[Date and time fields](docs/components/date-time.md)

### Toggles and input groups

Pressed buttons, single/multiple toggle choices and input addons.

[Toggles and input groups](docs/components/toggles-input-groups.md)

### File upload

Native upload and dropzones, validation, metadata and storage callbacks.

[File upload](docs/components/file-upload.md)

### Checkboxes, switches and radio groups

Boolean and multiple choices with native form semantics.

[Checkboxes switches and radios](docs/components/choices.md)

### Dialog

Modal content, validation, confirmation and focus restoration.

[Dialog](docs/components/dialog.md)

### Confirmation dialog

Confirm actions with server state and explicit destructive choices.

[Confirmation dialog](docs/components/confirmation-dialog.md)

### Sheet

Side panels using native dialogs and logical edges.

[Sheet](docs/components/sheet.md)

### Popover

Native popover content with controlled placement and focus behavior.

[Popover](docs/components/popover.md)

### Tooltip

Descriptions for focus and hover without replacing control names.

[Tooltip](docs/components/tooltip.md)

### Dropdown menus

Action menus, checked items, native placement and keyboard navigation.

[Dropdown menus](docs/components/dropdown-menu.md)

### Toasts

Notifications, actions, timing and accessible announcements.

[Toasts](docs/components/toasts.md)

### App shell

Header, breadcrumb and skip-link primitives for your own page structure.

[App shell](docs/components/app-shell.md)

### Sidebar

Responsive navigation, submenus, row actions and logical placement.

[Sidebar](docs/components/sidebar.md)

#### Submenus and actions

[Submenus and row actions](docs/components/sidebar.md#submenus-and-actions).

#### Navigation in the header

[Header navigation](docs/components/sidebar.md#navigation-in-the-header).

### Reusable app layout

Application chrome, named slots, menus and page locale.

[App layout](docs/components/app-layout.md)

### Tabs and disclosures

Keyboard tab selection with server-configured panels.

[Tabs](docs/components/tabs.md) · [Accordion](docs/components/accordion.md) · [Collapsible](docs/components/collapsible.md)

#### Accordion

[Accordion guide](docs/components/accordion.md).

#### Collapsible

[Collapsible guide](docs/components/collapsible.md).

### Tables

Compose native, compact, grouped and sticky-column tables.

[Native tables](docs/components/table.md)

### Data tables

Providers, sort state, selection, row actions and bookmarkable filters.

[Data tables](docs/components/data-table.md)

### Advanced tables

CSV downloads, column choices, lazy row details and staged inline edits.

[Advanced tables](docs/components/advanced-table.md)

### Bookmarkable table state

Providers, sort state, selection, row actions and bookmarkable filters.

[Data tables](docs/components/data-table.md)

### Pagination

Independent pagers, summaries, page sizes and native URL factories.

[Pagination](docs/components/pagination.md)

### Attachment displays

Stored file cards, image previews, downloads and grouped displays.

[Attachments](docs/components/attachments.md)

### User menu

Identity display, menu placements and native sign-in/sign-out integration.

[User menu](docs/components/user-menu.md)

### Theming

Color tokens, appearance preferences and embedded bundles.

[Theming](docs/guides/theming.md)

#### Light, Dark and System preferences

[Browser appearance preferences](docs/guides/theming.md#light-dark-and-system-preferences).

#### Embedded styles

[Embedded bundle integration](docs/guides/theming.md#embedded-styles).

### Display components

Compose content into cards with caller-owned markup.

[Cards](docs/components/cards.md) · [Badges](docs/components/badges.md) · [Alerts](docs/components/alerts.md) · [Avatars](docs/components/avatars.md) · [Icons](docs/components/icons.md) · [Skeletons and spinners](docs/components/loading.md) · [Empty state](docs/components/empty-state.md) · [Separators](docs/components/separators.md) · [Keyboard keys](docs/components/keyboard.md)

### Feedback

Inline validation, fenced feedback panels and feedback toasts.

[Feedback](docs/components/feedback.md)

## Building

```sh
./mvnw clean verify
```

No Node needed: the build installs a pinned Node LTS into `target/` and compiles the CSS with Tailwind CSS v4. The
jar ships only the finished CSS.

Run the showcase to explore the components with skin, light/dark and language controls:

```sh
./mvnw install -DskipTests
java -jar shadwick-examples/target/shadwick-examples-0.1.0-SNAPSHOT.jar
```

Open **http://localhost:8080/** for **Clover Studio**, a working agency workspace: create and edit
projects, complete tasks, prepare a downloadable client handoff and record invoice payments. The overview,
project screens and invoices share private, resettable session data. Commands and in-app navigation use
Ajax updates without full-page refreshes; native browsing/filtering remains available without script. Demo settings switch skins and the full interface between English and German; the header switches appearance.
See the [Clover Studio guide](docs/guides/clover-studio.md) for
the complete journey, design and data lifecycle. Try the [hosted Studio](https://shadwick-production.up.railway.app/) on Railway; existing `/studio` links continue to work.

The [live component catalog](https://shadwick-production.up.railway.app/components) is at **http://localhost:8080/components** locally. Its grouped navigation includes component search (Ctrl/⌘K), and each demo displays the actual Java and Wicket markup with copy controls and guide links.

While working on the CSS, `npm run watch:css` in `shadwick-core` (with a local
Node) rebuilds the bundles into `target/classes` on every change; reload the page to see it.

The browser checks (Playwright and axe-core, every page in all supported skins, light and dark) run with:

```sh
./mvnw verify -pl shadwick-examples -am -Pbrowser
# Optional: run every browser scenario for just one skin
./mvnw verify -pl shadwick-examples -am -Pbrowser -Dshadwick.browser.skins=nova
```

## Landing page

The static website lives in [`site/`](site/README.md), with an interactive component studio and a GitHub Pages
deployment workflow. See the site README for local preview and publishing instructions.

## License

Apache License 2.0. See [LICENSE](LICENSE), and [NOTICE](NOTICE) for the projects Shadwick adapts code from
(Shadleaf, shadcn/ui, Basecoat, Lucide).

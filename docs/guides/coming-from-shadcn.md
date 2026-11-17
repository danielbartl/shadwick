# Coming from shadcn/ui

What carries over from shadcn/ui, what each component is called here, and what changes when Wicket renders it.

> **A few names here are new in 0.2.0, not yet released:** the extra-small and small/large icon button sizes, `SwFieldGroup` and `SwFormErrors`. Everything else is in 0.1.0, the version on Maven Central.

Shadwick gives a Wicket application shadcn/ui's look, tokens and component catalog. If you know shadcn/ui from
React, most of what you know carries over: the same tokens, the same variants and sizes, the same anatomy. What
changes is who holds the state. In shadcn/ui a React component does; here a Wicket component on the server does,
bound to your models, and the browser's own elements do the rest.

## What carries over

- **The tokens.** Shadwick uses shadcn/ui's CSS variables unprefixed (`--background`, `--primary`, `--ring`,
  `--radius`, the chart and sidebar colors) on `:root` and `.dark`. A theme from shadcn/ui's theme page or tweakcn
  drops in unchanged; see [Theming](theming.md).
- **Dark mode.** `class="dark"` on `<html>`, set by [`ThemeBehavior`](theming.md) from the visitor's choice, or by you.
- **Variants and sizes.** A button has shadcn/ui's `default`, `outline`, `ghost`, `destructive`, `secondary` and
  `link` variants and its sizes; a badge and an alert have their own sets. They are enums per component:
  `ButtonBehavior.Variant.OUTLINE`, `ButtonBehavior.Size.ICON_SMALL`.
- **The anatomy.** A card has a header, title, description, content and footer; a dialog a title and description;
  a sidebar a header, content, groups and footer. The parts keep their names as CSS classes (`sw-card-title`,
  `sw-dialog-header`).
- **The look.** Shadwick's [skins](skins.md) are shadcn/ui-style geometries (radius, spacing, type size, shadows) over
  the same tokens; Vega is the classic look. The skin is chosen once for the application.

## At a glance

| shadcn/ui | Shadwick | Guide |
|---|---|---|
| Accordion | `SwAccordion` | [Accordion](../components/accordion.md) |
| Alert | `SwAlert`, `Sw.Behaviors.alert()` | [Alerts](../components/alerts.md) |
| Alert Dialog | `SwConfirmDialog` | [Confirmation dialog](../components/confirmation-dialog.md) |
| Avatar | `SwAvatar`, `SwAvatarGroup` | [Avatars](../components/avatars.md) |
| Badge | `SwBadge` | [Badges](../components/badges.md) |
| Breadcrumb | `SwBreadcrumb` | [App shell](../components/app-shell.md) |
| Button | `SwButton`, `SwSubmitButton`, `Sw.Behaviors.button()` | [Buttons](../components/buttons.md) |
| Button Group | `SwButtonGroup`, `SwSplitButton` | [Button groups](../components/button-groups.md) |
| Calendar, Date Picker | `SwCalendar`, `SwDatePicker` and their range versions | [Calendar](../components/calendar.md) |
| Card | `SwCard`, `Sw.Behaviors.card()` | [Cards](../components/cards.md) |
| Chart | `SwChart` and the other charts | [Charts](../components/charts.md) |
| Checkbox, Radio Group, Switch | `SwCheckBox`, `SwRadioChoice`, `SwSwitch` | [Checkboxes, switches and radios](../components/choices.md) |
| Collapsible | `SwCollapsible` | [Collapsible](../components/collapsible.md) |
| Combobox | `SwCombobox`, `SwMultiCombobox` | [Combobox](../components/combobox.md) |
| Data Table | `SwDataTable` | [Data tables](../components/data-table.md) |
| Dialog | `SwDialog` | [Dialog](../components/dialog.md) |
| Drawer, Sheet | `SwSheet` | [Sheet](../components/sheet.md) |
| Dropdown Menu | `SwDropdown`, `Sw.Behaviors.dropdownMenu(...)` | [Dropdown menus](../components/dropdown-menu.md) |
| Empty | `SwEmptyState` | [Empty state](../components/empty-state.md) |
| Field, Form, Label | `BaseSwField` (every field), `SwFieldGroup`, `SwFormErrors` | [Form fields](../components/forms.md) |
| Input, Textarea | `SwTextField`, `SwEmailField`, `SwTextArea`, … | [Text and number inputs](../components/inputs.md) |
| Input Group | `Sw.Behaviors.inputGroup()`, field prefixes and suffixes | [Toggles and input groups](../components/toggles-input-groups.md) |
| Kbd | `SwKbd` | [Keyboard keys](../components/keyboard.md) |
| Pagination | `SwPagination`, `SwPagingNavigator` | [Pagination](../components/pagination.md) |
| Popover | `SwPopover` | [Popover](../components/popover.md) |
| Progress | `SwProgress`, `SwRadialProgress` | [Progress](../components/progress.md) |
| Select | `SwSelect` (styled), `SwDropdownChoice` (native) | [Custom select](../components/select.md) |
| Separator | `SwSeparator` | [Separators](../components/separators.md) |
| Sidebar | `SwSidebar`, `SwAppLayout` | [Sidebar](../components/sidebar.md) |
| Skeleton, Spinner | `SwSkeleton`, `SwSpinner` | [Skeletons and spinners](../components/loading.md) |
| Slider | `SwRangeField` | [Range field](../components/range.md) |
| Sonner, Toast | `Sw.toast(...)`, `Sw.Behaviors.feedbackToasts()` | [Toasts](../components/toasts.md) |
| Table | `SwTable`, `Sw.Behaviors.table()` | [Native tables](../components/table.md) |
| Tabs | `SwTabs` | [Tabs](../components/tabs.md) |
| Toggle, Toggle Group | `SwToggle`, `SwToggleChoice` | [Toggles and input groups](../components/toggles-input-groups.md) |
| Tooltip | `Sw.Behaviors.tooltip(...)` | [Tooltip](../components/tooltip.md) |

There is no counterpart yet for Command, Context Menu, Menubar, Navigation Menu, Hover Card, Carousel, Resizable,
Scroll Area or Input OTP. Shadwick adds components shadcn/ui doesn't have: a month view, file uploads and
attachments, a user menu, a site header and skip link, steps, timelines, description lists, tags, ratings, meters,
money and percentage fields, and lazy-loaded panels.

## What changes everywhere

### The server holds the state

A shadcn/ui component keeps its state in React; a Shadwick component is a Wicket component, bound to a model. A
field reads and writes its model on submit, a dialog knows on the server whether it is open (`isOpen()`), and an
Ajax request changes what the page shows by re-rendering components (`target.add(...)`). There is no client state
to keep in sync, and nothing to fetch: the page arrives rendered.

```java
form.add(new SwTextField<String>("name").setRequired(true));      // bound through a CompoundPropertyModel
form.add(SwSubmitButton.onSubmit("save", "Save", target -> service.save(profile)));
```

### Behaviors instead of `asChild`

Where shadcn/ui passes `asChild` to style your own element as a button, Shadwick adds a behavior to your own
component: `link.add(Sw.Behaviors.button().setVariant(ButtonBehavior.Variant.OUTLINE))`. Most components come as both
a ready component (Shadwick owns its markup) and a behavior (you own the markup), through the facades
`Sw.Components` and `Sw.Behaviors`.

### Native elements instead of Radix

The browser's own elements do the work Radix does in shadcn/ui: `<dialog>` for dialogs and sheets, the Popover API
for popovers, menus and tooltips, `<details>` for accordions and collapsibles, real `<select>` and `<input>` elements
in fields. Focus handling, Escape and light dismissal are the browser's; a small script adds keyboard navigation
where the platform has none, such as in menus and tabs. Everything that can work without JavaScript does.

### Fields carry their label and error

A shadcn/ui form composes `FormField`, `FormLabel`, `FormControl` and `FormMessage`. A Shadwick field is one
component that renders its label (from your `.properties`), the input, an optional description and the inline error,
and wires `aria-describedby` and `aria-invalid`. Validation is Wicket's validators on the server; after an Ajax
submit, fields show their errors in place without `target.add(...)`. See [Form fields](../components/forms.md).

### A button doesn't submit

`SwButton` renders `type="button"` and runs an Ajax callback; to submit a form, use `SwSubmitButton`, which validates
the form first. A shadcn/ui `<Button>` inside a form submits it, as HTML buttons do.

### No accessible name, no render

An icon-only button without an accessible name fails to render in development mode, with a message saying how to fix
it, and logs a warning in deployment mode. See [Accessibility](accessibility.md).

### Start and end, not left and right

Sides follow the text direction: a sheet opens from `Side.START` or `Side.END`, a sidebar sits at the start, and a
right-to-left page mirrors them.

### A strict Content-Security-Policy

Shadwick renders no inline scripts, event handlers or style attributes, so it works under Wicket's default
nonce-based policy. Where a value has to reach CSS, such as a chart's custom color, Shadwick writes it into a small
stylesheet with the page's nonce.

## By area

- **Overlays.** `SwDialog` opens and closes from the server (`open(target)`, `close(target)`) or from a trigger button
  in the browser, and stays open when it is re-rendered; its content is a component with `CONTENT_ID`.
  `SwConfirmDialog.ask(target, title, description, callback)` replaces the Alert Dialog's controlled `open`. See
  [Dialog](../components/dialog.md) and [Confirmation dialog](../components/confirmation-dialog.md).
- **Forms.** A shadcn/ui Select with its trigger, content and items is a `SwSelect` bound to a model and a choice list,
  on a real `<select>`; a Combobox built from Popover and Command is a `SwCombobox`, which can search on the server.
  See [Custom select](../components/select.md) and [Combobox](../components/combobox.md).
- **Data.** TanStack Table's column definitions become Wicket's `IColumn`s and its data an `IDataProvider`: sorting,
  paging and filtering run on the server, and a page or a sort is a link that works without JavaScript. See
  [Data tables](../components/data-table.md).
- **Charts.** Recharts draws in the browser; `SwChart` renders native SVG on the server from a model and named series,
  with the exact values in a table beside it. See [Charts](../components/charts.md).
- **Feedback.** Sonner's `toast()` is `Sw.toast(target, message)` in an Ajax callback; Wicket's feedback messages can
  show as alerts (`SwFeedbackPanel`) or as toasts. See [Toasts](../components/toasts.md) and
  [Feedback](../components/feedback.md).
- **The application shell.** shadcn/ui's sidebar blocks are a `SwAppLayout` subclass or a `SwSidebar` with
  `MenuItem`s; the [layout recipes](../layouts/overview.md) are whole frames to start from.

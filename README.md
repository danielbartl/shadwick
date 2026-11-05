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
> input groups, dialogs, menus, toasts, layouts, tabs, accordions, collapsibles, attachment cards/galleries, sortable data tables, pagination, display components and feedback,
> in three skins (vega, lyra, nova), light and dark. Standalone and embedded CSS bundles.
> Not yet on Maven Central.

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

| Skin | Look |
| --- | --- |
| `Skin.VEGA` | Classic spacing, 36px default controls and subtle control shadows. The default. |
| `Skin.LYRA` | Square corners, small type and thin focus rings. |
| `Skin.NOVA` | Compact 32px controls, rounder fields, tighter spacing and muted card/dialog footers. |

Each skin has complete standalone and embedded bundles, including minified versions, and supports light/dark
appearance with the same markup and color tokens. Nova keeps mobile input text at 16px and the existing keyboard,
forced-color and reduced-motion behavior. Pick a skin with `Shadwick.install(this).setSkin(Skin.NOVA)`; the optional
`SkinStore` supports per-user selection. The showcase and landing studio include all three skin choices.

### Buttons

```java
// A button that runs over Ajax
add(SwButton.onClick("save", "Save", target -> save()));
add(SwButton.onClick("delete", "Delete", target -> delete())
        .setVariant(ButtonBehavior.Variant.DESTRUCTIVE));

// Any link or button styled as a shadcn button
add(new BookmarkablePageLink<>("home", HomePage.class)
        .add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.SMALL)));
```

```html
<button wicket:id="save"></button>
<a wicket:id="home">Home</a>
```

Variants: `DEFAULT`, `SECONDARY`, `OUTLINE`, `GHOST`, `DESTRUCTIVE`, `LINK`. Sizes: `DEFAULT`, `SMALL`, `LARGE`,
`ICON`. An icon-only button needs an `aria-label`: in development mode, a button without an accessible name fails
to render with a message saying how to fix it, and in deployment it is logged.

### Form fields

A field wraps a label, the input, an optional description and the inline error, and wires `aria-describedby` and
`aria-invalid` for you. It works with `CompoundPropertyModel` and takes its label from your `.properties` files:

```java
Form<Profile> form = new Form<>("form", new CompoundPropertyModel<>(profile));
form.add(new SwTextField<String>("name").setRequired(true));
form.add(new SwEmailField("email").setDescription("We never share it."));
form.add(new SwTextArea<String>("bio"));
form.add(SwSubmitButton.onSubmit("save", "Save", target -> service.save(profile)));
```

```html
<form wicket:id="form">
    <div wicket:id="name"></div>
    <div wicket:id="email"></div>
    <div wicket:id="bio"></div>
    <button wicket:id="save"></button>
</form>
```

After an Ajax submit, the fields show their errors themselves: no `target.add(...)` needed, and the input keeps its
focus and what the user typed.

### Everyday inputs

`SwEmailField`, `SwPasswordField`, `SwSearchField`, `SwTelField`, `SwUrlField` and
`SwNumberField<N>` share the same field wrapper, models and inline Ajax feedback:

```java
form.add(new SwEmailField("email").setRequired(true));
form.add(new SwPasswordField("password"));
form.add(new SwSearchField("query").setPlaceholder("Search projects"));
form.add(new SwTelField("telephone"));
form.add(new SwUrlField("website"));
form.add(new SwNumberField<Integer>("seats").setMin(1).setMax(10).setStep(1));
```

This example inherits models from a parent `CompoundPropertyModel`. Bind each field to a `<div wicket:id="...">`.
Email and URL use Wicket's server validators. Search and telephone accept arbitrary text; add application validators
when you need stricter rules. Password fields are required by default, clear their model on detach and never echo
values into HTML, following Wicket's `PasswordTextField`. Use `.setRequired(false)` for an optional password.

Number fields support integer and decimal types (including `BigDecimal`), min/max/step values or models, and native
HTML number formatting independent of the session locale. For an empty plain model, provide `.setType(Integer.class)`
(or the appropriate number class); compound models know their property type. Min/max are enforced by Wicket on the
server. **Step is a browser hint**, not a server validator: add a validator if alignment to the step is a domain rule.
A zero step renders Wicket's `step="any"` convention. Access the native component with `getInput()` for further options.

Native email, URL and number controls may perform browser validation before a submit. The showcase's **Inputs** page
uses `novalidate` on its forms so it can demonstrate Wicket's inline server errors, normal submission and Ajax focus
preservation. After an invalid Ajax submit the password stays in the existing browser input without being sent back
in the response; a full page render leaves it blank.

### Native selections

`SwDropdownChoice<T>` wraps Wicket's native `DropDownChoice<T>`. `SwListMultipleChoice<T>` is a native
`<select multiple>` list box, and `SwCheckBoxMultipleChoice<T>` presents a collection as labelled checkboxes
inside a fieldset. All three accept a choices model and an optional `IChoiceRenderer`, use the standard field
wrapper, and inherit selected models and `.properties` labels by id:

```java
var teams = Model.ofList(List.of("Design", "Engineering", "Support"));
form.add(new SwDropdownChoice<>("team", teams).setRequired(true));
form.add(new SwListMultipleChoice<>("teams", teams).setMaxRows(3).setRequired(true));
form.add(new SwCheckBoxMultipleChoice<>("channels", Model.ofList(List.of("Email", "SMS", "In-app")))
        .setInline(true).setRequired(true));
```

Bind each field to a `<div wicket:id="...">`. The dropdown property holds one choice; multi-selection properties
hold a collection, such as a mutable `List<T>`. Wicket updates existing collections in place. A required
multi-selection needs at least one valid choice; unrecognised submitted IDs cannot satisfy that requirement.
Optional multi-selections can be cleared. Use `dropdown.getInput().setNullValid(true)` to keep a selectable null
option in an optional dropdown. In page properties, `input.nullValid` supplies its text and `input.null`
labels the initial prompt, following Wicket's resource lookup for the wrapped control named `input`.

Use `AjaxFormComponentUpdatingBehavior("change")` for either native select and
`AjaxFormChoiceComponentUpdatingBehavior` for the checkbox group. Pass these to `.add(...)` to attach them to the
wrapped control. Inline feedback updates without replacing the select, checkbox nodes, current selection or focus.
The **Selections** showcase (`/selections`) demonstrates normal and Ajax submits, immediate updates, a custom
renderer with stable IDs, inline and stacked checkbox groups, and disabled selections. Multi-select keyboard
interaction follows the browser and operating system; checkbox groups provide an alternative without modifier keys.

### Custom select

`SwSelect<T>` is a single-choice field with the same constructors and model/renderer contracts as
`SwDropdownChoice<T>`, plus groups, icons, separators and disabled choices:

```java
var workspace = new SwSelect<>("workspace", "Workspace", selectedWorkspace, workspaceChoices,
        new ChoiceRenderer<Workspace>("name", "id"))
        .setPlaceholder("Choose a workspace")
        .setGroup(Workspace::group)
        .setIcon(Workspace::icon) // registered SwIcon name, or null
        .setDisabledWhen(Workspace::disabled)
        .setSeparatorBefore(w -> w.id().equals("support"))
        .setSize(SelectBehavior.Size.SMALL)
        .setRequired(true);
form.add(workspace);
```

Bind it to `<div wicket:id="workspace"></div>`. Choices retain their original order and renderer IDs;
adjacent equal group labels produce an optgroup. Dynamic labels, group names and placeholders are escaped.
Unknown, removed and disabled IDs fail server conversion, including for optional fields. Use stable renderer IDs
for changing collections. The choices model is read once per request and detached normally.

The real `<select>` always owns the value. Browsers supporting `appearance: base-select` draw the styled native
picker; other Popover API browsers receive an accessible combobox/listbox enhancement. Without JavaScript or
these APIs, the platform picker still submits and validates. No frontend dependency or CSP exception is needed.
Arrow keys, Home/End, typing, Enter/Space and Escape use the picker; the fallback also supports PageUp/PageDown.
Escape/outside dismiss without changing the value; Tab commits the highlighted fallback choice and moves focus.
Disabled choices are skipped. All skins, sizes, RTL, reduced motion and forced colors are supported.

`setPlaceholder(String/IModel<String>)` supplies the empty prompt. It is hidden from the list by default;
`getInput().setNullValid(true)` makes it a selectable clearing choice. Required validation still rejects empty values.
`SwSelect.placeholder` and `SwSelect.unavailable` are localizable resources. This is single selection;
searchable and multiple custom selection are provided by the combobox fields below. Native multi-selection remains
available through `SwListMultipleChoice` and `SwCheckBoxMultipleChoice`.

Attach `AjaxFormComponentUpdatingBehavior("change")` with `.add(...)` for immediate updates. Normal submit,
Ajax submit and inline feedback use `BaseSwField`; replacing the field with `target.add(select)` initializes the
new picker automatically. The fallback mirrors the source's name, description, error, required and disabled state,
including disabled fieldsets. When changing the native value from application JavaScript, dispatch `input`/`change`
to synchronize dependent UI. Native form reset restores the rendered default without firing change or updating
the server model. Refresh dependent server UI explicitly when an application needs a server-side reset.

For caller-owned markup, add `Sw.Behaviors.select()` to a wrapper containing a single `<select class="sw-select">`
with an associated label. Use `sw-select-item`, `sw-select-group`, `sw-select-separator` and a decorative
`sw-select-icon` for the same anatomy. `SwIcon.decorativeMarkup(name)` supplies trusted registry SVG for native
option bodies. The **Custom select** showcase (`/custom-select`) covers normal/Ajax validation, immediate updates,
reset, changing choices, groups/icons, a disabled field and a longer list. The landing studio previews plan selection
using the actual component CSS/runtime.

### Combobox

`SwCombobox<T>` searches a single choice; `SwMultiCombobox<T>` searches several choices displayed as removable
chips. Both wrap real Wicket selects and share the usual label, description and inline feedback:

```java
form.add(new SwCombobox<>("city", "City", selectedCity, cities,
        new ChoiceRenderer<City>("name", "id"))
        .setGroup(City::region)
        .setIcon(city -> "map-pin")
        .setDisabledWhen(City::unavailable)
        .setPlaceholder("Find a city")
        .setShowClear(true)
        .setRequired(true));

form.add(new SwMultiCombobox<>("teams", "Teams", selectedTeams, teams)
        .setAutoHighlight(true)
        .setShowClear(true)
        .setPlaceholder("Add teams"));
```

Use `<div wicket:id="city"></div>` / `<div wicket:id="teams"></div>`. Constructors follow the native choice
fields: selected model and choices model, optional `IChoiceRenderer`, String/model labels and description.
Without a selected model, `CompoundPropertyModel` supplies it by component ID. Multiple selection binds a
`Collection<T>`, including mutable lists/sets, through Wicket's collection update contract. Stable renderer IDs
are recommended for changing lists. The choices model snapshots once per request and detaches normally.

Typing filters locally without case or accents. A group hides when none of its choices match; separators hide
while searching. `setDisplay`, `setGroup`, `setIcon`, `setSeparatorBefore` and `setDisabledWhen` decorate the
options without reordering IDs. `setEmpty(String/IModel<String>)` customizes empty results. Only choices are
submitted; unmatched typed text is never accepted as a value. Disabled and unknown IDs fail conversion atomically.

By default typing highlights nothing. `setAutoHighlight(true)` highlights the first match for Enter. Arrows and
PageUp/PageDown move among enabled results while focus stays in the input; Home/End keep their caret behavior.
Enter chooses (or toggles a multiple choice), Escape restores the chosen text, and Tab closes without choosing.
Emptying a single input and leaving it clears its value. In a multiple field, Backspace in an empty input removes
the last chip. ArrowLeft from its start focuses the chip remove buttons; Left/Right move between them and
Delete/Backspace removes a chip. Clear/trigger/chip buttons have equivalent keyboard paths and stay out of Tab order.

For larger lists, supply an Oat-style server search callback with stable identity and a resolver:

```java
form.add(new SwCombobox<City>("city", "City", selectedCity)
        .setChoices(text -> cityService.searchVisibleCities(text, 20))
        .setChoiceId(City::id)
        .setChoiceResolver(id -> cityService.findVisibleCity(id))
        .setDisplay(City::name)
        .setMaxChoices(20)
        .setAutoHighlight(true)
        .setShowClear(true));
```

The resolver returns a currently authorized choice or null; it runs again on submission and its returned ID must
match. `setDisabledWhen` also checks newly resolved objects. Search results are never accumulated in the Wicket
page. The search callback receives at most 256 characters; `setMaxChoices` limits each answer (default 10,
allowed 1–200). Keep the service query itself bounded. The empty query supplies the initial choices unless an
explicit choices model was provided; current selected objects are included for display and native fallback.

Search waits 300 ms after typing, calls a same-origin Wicket JSON endpoint and cancels older requests. The
server filters; the browser preserves chosen options omitted from an answer, hides them from the current results,
and restores initial options plus current choices when search ends. Busy, empty and error states have localized
messages and a delayed result announcement. HTTP errors, redirects and invalid responses show an error and retain
the selection. `sw-combobox-search` bubbles from the select before each request; `event.detail.query` and
`event.detail.url` allow adding parameters, and `preventDefault()` cancels it. URLs remain same-origin.

Normal/Ajax submission and `AjaxFormComponentUpdatingBehavior("change")` use the native select. Inline Ajax
feedback updates in place and preserves the generated input's focus; `target.add(field)` initializes a replacement
and cleans up removed listeners, observers, timers and requests. Valid remote choices are also retained
when another field blocks model updates on a normal full-page validation response. Programmatic select value/option changes should
fire `input` or `change`. Native form reset restores rendered defaults without change events or server updates;
a canceled reset leaves the control alone. All skins/bundles, disabled fieldsets, RTL, IME composition, forced
colors, reduced motion and nested dialogs/popovers are supported without inline styles or CSP changes.

`Sw.Behaviors.combobox()` also enhances caller-owned wrappers containing a labelled
`<select class="sw-combobox-select">`. It supplies the input/list/chip anatomy without a client framework.
Behavior setters include placeholder/empty text, clear button and auto-highlight; typed fields supply their
own search URL. A caller-owned same-origin endpoint may return a JSON array of `{id, text, disabled?, group?,
separator?, icon?}`; icons are optional drawing-only SVG, sanitized before insertion. No-script/API fallback stays
a native single/multiple select, with only initially rendered choices available. The **Combobox** showcase
(`/combobox`) exercises local/server choices, validation, reset, live replacement and overlays; the landing
studio includes a searchable multiple-team picker using the actual CSS/runtime.

### Month view

`SwMonthView` renders an agenda as a native table, with custom Wicket content per day. It works without JavaScript:

```java
add(new SwMonthView("agenda")
        .setCalendarLocale(Locale.UK)
        .setShowWeekNumber(true)
        .setDayParameter("day")
        .setDayContent((id, day) -> new Label(id, bookings.countFor(day.date()))));
```

Bind it to `<div wicket:id="agenda"></div>`. `Day` exposes `date()`, `iso()`, `number()`, `label()`, `outside()` and
`today()`. For richer content, return a Panel or override `newDayContent(String, IModel<Day>)`. A component factory
runs for each visible day, including outside days; fetch a month's data once in a detachable model rather than
querying the database per cell.

The default month comes from the page's `month` parameter, falling back to today for an invalid/missing value.
Previous/next links preserve other page parameters. `setMonthParameter("rota")` gives an independent view its own
parameter. A constructor accepting `IModel<YearMonth>` lets the application own the month; when using this form,
read the navigation parameter into that model on page construction, or update it from your own Ajax controls.
Ajax updates re-render the whole component (`target.add(view)`).

`setDayParameter("day")` makes days bookmarkable links to the current page. `setDayPage(PageClass.class,
 day -> new PageParameters().add("date", day.iso()))` targets another page. Keep custom content noninteractive
when it is inside a day link. Without either setting, cells can contain their own controls.

`setToday(LocalDate/IModel)`, `setWeekStartsOn(DayOfWeek)`, `setShowOutsideDays`, `setShowWeekNumber` and
`setNavSize(DEFAULT/ICON_XS/ICON_SM/ICON/ICON_LG)` configure the view. The locale supplies week numbering and
localized names; navigation/day links stop at years 1–9999. Pass the user's current date explicitly when it differs
from the server's. The **Month view** showcase (`/month-view`) covers bookmarkable/independent months and Ajax.

### Charts

`SwChart<T>` follows Wicket Oat's model/category/series pattern. It renders responsive SVG on the server, so the plot
and exact-values table remain useful without script. A small optional vanilla runtime adds pointer and keyboard
tooltips. No charting framework or additional dependency is required.

```java
add(new SwChart<>("visitors", visitorsModel, Visit::month)
        .setTitle("Visitors by device")
        .setCategoryHeader("Month")
        .addSeries(ChartSeries.bar("Desktop", Visit::desktop))
        .addSeries(ChartSeries.line("Mobile", Visit::mobile).setDashed(true)));
```

Bind it to `<div wicket:id="visitors"></div>`. Import the types from `dev.jbaby.shadwick.components.chart`.
`ChartSeries.bar/line/area` accept String/model labels and serializable value extractors. Mix types in one chart;
`setStack("total")` shares a stack between bars or areas of the same type. Positive and negative values stack
separately. Null leaves a gap, including in the exact data. An empty model gets a real empty state.

Series support `setCurve(LINEAR/STEP/MONOTONE)`, `setDashed`, `setDots`, `setFillOpacity(0..1)` and
`setColor(1..5)` for chart tokens (`0` selects automatically). Curves apply to lines and areas; dots apply to both.
The default curve is a monotone cubic interpolation without overshoot. Colors follow light/dark theme tokens;
application CSS can override the chart tokens. Geometry uses finite numbers of magnitude at most `1e100`.
Labels, formatted values and categories are escaped; values are snapshotted once per request, and models detach.

`setValueFormat(NUMBER/INTEGER/COMPACT/PERCENT)` uses the session locale. `setCurrency(Currency)` selects currency
formatting. For custom output use `setValueFormat((number, locale) -> ...)` or `setCategoryFormat((value, locale)
 -> ...)`. Dates/YearMonth are formatted on the server as calendar values; LocalDateTime stays on its written clock.
Instant/OffsetDateTime/ZonedDateTime use `setTimeZone(ZoneId)` (UTC by default), independently of the browser's zone.

`setShowGrid`, `setShowXAxis`, `setShowYAxis`, `setShowLegend` and `setShowTooltip` control parts. Every plot has an
accessible name and a native **View data** disclosure. With script, focus the plot and use Left/Right, Home/End and
Escape. RTL follows visual order. Ajax replacement removes old listeners/observers; call `target.add(chart)` to
refresh. CSS controls chart colors; the graph uses a 240px plot height and fills its container. Tooltip and legend
geometry follow each skin, including embedded bundles, forced colors and reduced motion.

The **Charts** showcase (`/charts`) includes grouped/stacked bars, lines, stacked areas, a composed currency chart,
negative values/gaps, an empty state and live Ajax refresh. The landing uses the same CSS/runtime and native table.
This implementation does not include every Recharts option offered by Shadleaf: forecast `join`, arbitrary per-series
light/dark colors, natural splines and per-corner bar radii remain possible follow-up work.

### Calendar and date picker

`SwCalendar` draws an inline month grid; `SwDatePicker` opens it from a date button. Both bind a `LocalDate`.
`SwRangeCalendar` and `SwDateRangePicker` bind an inclusive `DateRange(from, to)` through two native inputs:

```java
form.add(new SwDatePicker("departure", "Departure", departure)
        .setRequired(true)
        .setMin(LocalDate.of(2026, 1, 1))
        .setMax(LocalDate.of(2026, 12, 31))
        .setDisabledDates("sat,sun,2026-12-24..2026-12-31")
        .setFormat(FormatStyle.MEDIUM)
        .setPlaceholder("Choose departure"));

form.add(new SwRangeCalendar("stay", "Stay", stay)
        .setNumberOfMonths(2)
        .setShowWeekNumber(true)
        .setWeekStartsOn(DayOfWeek.MONDAY));
```

Bind each to `<div wicket:id="..."></div>`. Constructors accept String/model labels, selected models and
optional descriptions, or inherit from `CompoundPropertyModel`. Single inputs keep the `field:input` path;
ranges expose `field:input:from` and `field:input:to`, also available as `getInput().getFrom()` / `getTo()`.
The form always submits ISO dates. A range allows open ends unless required; required means both ends. Empty
optional pairs bind null. Server validation rejects invalid/reversed dates, bounds, disabled dates and changes
to read-only fields without updating the model. Disabled rules apply to a complete range's interior too.

The first range pick starts it; the second completes it, swapping earlier endpoints. A completed range starts
a new one on the next pick. Disabled days remain reachable by arrows but cannot be selected; a range crossing
an unavailable day is rejected. An optional single date clears when its selected day is picked again. Required
dates remain selected. `setReadOnly(true)` permits browsing without changing the value; `setEnabled(false)`
disables navigation and selection. Min/max and disabled-date rules are also enforced without JavaScript.

Calendar configuration is shared by all four fields and `Sw.Behaviors.calendar()` for caller-owned markup:

- `setCaption(CalendarBehavior.Caption.LABEL/DROPDOWN/DROPDOWN_MONTHS/DROPDOWN_YEARS)` controls captions.
- `setNumberOfMonths(1..12)`, `setShowOutsideDays`, `setShowWeekNumber` configure the grid.
- `setCalendarLocale(Locale)` overrides the field locale (`setLocale` on the behavior);
  `setWeekStartsOn(DayOfWeek)` can override its first day.
  Week numbering uses the locale's `WeekFields` minimum-days rule. Month/day names come from `Intl`.
- `setDefaultMonth`, `setStartMonth`, `setEndMonth` accept `YearMonth` or models. Start/end constrain navigation,
  not server values: use min/max for validation. Defaults span 100 years back and 10 ahead, or input bounds.
- `setToday(LocalDate/IModel)` supplies a deterministic today marker; otherwise the browser's local day is used.
- `setMin` / `setMax` accept `LocalDate` or models. `setDisabledDates(String/IModel<String>)` accepts strict
  comma-separated ISO dates, inclusive `from..to` intervals and English weekday names/abbreviations.
- Picker `setFormat(FormatStyle)` defaults to LONG; `setPlaceholder(String/IModel<String>)` sets the empty text.

The grid uses Gregorian date-only arithmetic and UTC formatting, preserving dates through DST, skipped civil
days and years below 100. Values and configurable navigation dates use years 1–9999. Arrows move a day/week,
Home/End to week boundaries, PageUp/PageDown a month and Shift+PageUp/PageDown a year. Left/Right follow RTL.
Month/year selects retain focus and keep their events inside the calendar. One day is a grid tab stop; disabled
fields have none. Labels, descriptions and inline errors name/describe the grid and picker button.

Normal/Ajax submit uses `BaseSwField`. `AjaxFormComponentUpdatingBehavior("change")` supports immediate
single and range updates: a range validates both children and the runtime enables recursive Wicket serialization
only for its input panel. Both native endpoint values are written before either change event, so callbacks read
a coherent pair. Each changed endpoint emits its own native input/change event; applications should expect that
when both endpoints change. Model values remain unchanged on invalid submissions. Re-rendering the whole field
or its native input reinitializes the UI and removes old listeners/observers/timers. Native reset restores rendered
values without firing changes or updating the server model; canceled resets leave them alone.

Pickers use the existing native non-modal popup, CSS anchors and focus return; Escape dismisses only the current
picker before its parent dialog/sidebar. A range stays open until both endpoints are chosen. All skins/bundles,
RTL, forced colors, reduced motion and hosts without a reset are supported. No dependency or CSP exception is
needed. Without script, inline calendars show native date inputs and picker buttons open them in a native
popover. Without Popover API support, enhanced pickers expose their native inputs directly.

For caller-owned markup, place one or two labelled date inputs inside a wrapper with `Sw.Behaviors.calendar()`.
Set `min`, `max`, `readonly`, `required` and `disabled` on those native inputs. The typed fields wire these
attributes and validate the shared rules; behavior-only applications must enforce their own server validation.
`DateRange.contains(date)` handles open ends. The **Calendar & date picker** showcase (`/calendar`) demonstrates
all four fields, normal/live/Ajax validation, month/year selection, week numbers, reset and overlays. The landing
studio includes an inline catch-up calendar and a date picker in its project dialog.

### Native dates and times

All five fields reuse the standard label, description and inline Ajax feedback:

| Component | Model type | HTML value format |
|---|---|---|
| `SwDateField` | `LocalDate` | `2026-12-24` |
| `SwTimeField` | `LocalTime` | `14:05` or `14:05:30.125` |
| `SwDateTimeLocalField` | `LocalDateTime` | `2026-12-24T14:05` |
| `SwMonthField` | `String` | `2026-12` |
| `SwWeekField` | `String` | `2026-W52` (ISO week-year) |

```java
form.add(new SwDateField("date").setRequired(true));
form.add(new SwTimeField("time"));
form.add(new SwDateTimeLocalField("appointment"));
form.add(new SwMonthField("month"));
form.add(new SwWeekField("week"));
```

These inherit selected models from a parent `CompoundPropertyModel`; bind each to a `<div wicket:id="...">`.
The browser owns the picker and localized display. Values use [HTML's fixed date/time formats](https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#dates-and-times)
regardless of the Wicket session locale or application-wide converters. Server conversion rejects malformed values,
impossible dates and invalid month/week values, including week 53 in a 52-week ISO year. Optional blank fields set
the model to `null`. HTML dates require positive years within Java's supported date range.

`LocalDateTime` is a local wall-clock value: it carries no timezone and Shadwick performs no UTC conversion.
Time values omit zero seconds and truncate model nanoseconds to milliseconds when rendered. For seconds use
`.addToInput(AttributeModifier.replace("step", "1"))`; for milliseconds use `"0.001"`. Without this, the browser's
default step is one minute. The underlying model retains its original precision until the form updates it.

HTML `min`, `max` and `step` attributes are browser hints on these text fields. Add Wicket validators for server-side
bounds or scheduling rules, for example:

```java
LocalDate first = LocalDate.of(2026, 1, 1);
LocalDate last = LocalDate.of(2026, 12, 31);
form.add(new SwDateField("date").setRequired(true)
        .addValidator(RangeValidator.range(first, last))
        .addToInput(AttributeModifier.replace("min", first.toString()),
                AttributeModifier.replace("max", last.toString())));
```

Use `AjaxFormComponentUpdatingBehavior("change")` for immediate validation. The **Dates & time** showcase (`/dates`)
includes all five types, normal/Ajax submits, date bounds, an optional live date, disabled state and millisecond input.
Picker availability depends on the browser and operating system; server conversion still applies.
For a consistent custom month grid, use the calendar and date-picker components described above.

### Toggles and input groups

`SwToggle` is a Boolean checkbox drawn as a toggle button. `SwToggleChoice<T>` is a single-selection radio group;
`SwToggleMultipleChoice<T>` binds a collection of selected values. Their labels, descriptions and errors use the same
field API as the other controls:

```java
form.add(new SwToggle("highlight").setIcon("pencil").setIconOnly(true)
        .setVariant(ToggleBehavior.Variant.OUTLINE));
form.add(new SwToggleChoice<>("alignment", Model.ofList(List.of("Left", "Center", "Right")))
        .setRequired(true).setVariant(ToggleBehavior.Variant.OUTLINE).setSpacing(0));
form.add(new SwToggleMultipleChoice<>("formats", Model.ofList(List.of("Bold", "Italic", "Underline")))
        .setSize(ToggleBehavior.Size.SMALL));
```

Each binds to a `<div wicket:id="...">` and inherits its value and resource label by id. Toggles expose native checked
state: Space changes a checkbox, arrow keys move through a single-selection group, and clicking an already selected
radio keeps it selected. A Boolean toggle's `setRequired(true)` follows Wicket's checkbox semantics and allows `false`;
use a validator when `true` is required. Required groups need at least one valid choice. Group constructors support
explicit models and `IChoiceRenderer`, like `SwRadioChoice` and `SwCheckBoxMultipleChoice`; renderer labels stay escaped
text. An icon-only toggle keeps its text caption as the accessible name.

Variants are `DEFAULT` and `OUTLINE`; sizes are `DEFAULT`, `SMALL` and `LARGE`. Group `.setSpacing(0)` joins items;
1–4 select a gap (default 2). `.setOrientation(ToggleGroupBehavior.Orientation.VERTICAL)` stacks them. Use
`AjaxFormComponentUpdatingBehavior("change")` for `SwToggle`, and `AjaxFormChoiceComponentUpdatingBehavior` for either
group. Passing these to `.add(...)` attaches them to the underlying inputs. The browser owns selection and focus;
feedback updates in place.

Text-like fields now offer `.setPrefix(...)` and `.setSuffix(...)` for text or `IModel<String>` addons:

```java
form.add(new SwTextField<String>("workspace").setPrefix("https://").setSuffix(".example"));
form.add(new SwNumberField<Integer>("weight").setMin(1).setMax(100).setSuffix("kg"));
form.add(new SwTextArea<String>("notes").setPrefix("Draft").setSuffix("Markdown supported"));
form.add(new SwTextField<String>("project").setSuggestions(List.of("Shadwick", "Atlas", "Nimbus")));
```

A null addon model or model value hides it. The group wrapper appears only when an addon is present. Prefixes and
suffixes describe the input without changing its name or submitted value, remain escaped, and keep the same
`field:input` path. Textarea addons sit above/below the control. Datalist suggestions are native browser hints on
single-line inputs, with browser-dependent availability; they do not restrict submitted values. Add validators to
limit allowed values. Prefix/suffix and suggestion models are detached with the field.

For custom markup with icons or action buttons, compose the `Sw.Behaviors` helpers:

```java
group.add(Sw.Behaviors.inputGroup());
input.add(Sw.Behaviors.inputGroupInput());       // or inputGroupTextArea()
addon.add(Sw.Behaviors.inputGroupAddon()
        .setAlign(InputGroupAddonBehavior.Align.INLINE_END));
button.add(Sw.Behaviors.inputGroupButton());    // small ghost button by default
```

```html
<label for="search-input">Search library</label>
<div wicket:id="group" aria-label="Search tools">
    <input wicket:id="input" id="search-input" />
    <div wicket:id="addon"><button wicket:id="button" type="button">Clear</button></div>
</div>
```

Keep the control before addons in the DOM so it comes first in the tab order; CSS places `INLINE_START`, `INLINE_END`,
`BLOCK_START` and `BLOCK_END` addons. `.setBorder(true)` separates a block addon from its control.
`Sw.Behaviors.inputGroupText()` styles addon text. Group buttons use `ButtonBehavior.Variant` and `Size.SMALL` or
`Size.ICON`; name icon-only buttons. Use the behavior on a plain Wicket button or link; `SwButton` already has a
button behavior. Clicking noninteractive addon space focuses the control, including after Ajax replacement; buttons
and links keep their own actions. Rich groups should have a separate group name, as in the example above.

The **Groups** showcase (`/groups`) demonstrates normal and Ajax submissions, immediate keyboard-driven formatting,
joined and vertical groups, icon-only and disabled toggles, validation, datalists, textareas, and a custom group with
Clear and Ajax replacement actions.

### File upload

`SwFileUpload` is a native file input; `SwFileDropzone` adds Shadleaf's dropzone appearance, a selected-file list,
removal buttons and announcements. Both bind to `IModel<List<FileUpload>>` or an inherited `List<FileUpload>` property,
use the regular field label/description/error API, and default to multiple selection:

```java
Form<Documents> form = new Form<>("form", new CompoundPropertyModel<>(documents));
form.setMultiPart(true);
form.setMaxSize(Bytes.megabytes(10));       // whole multipart request, including overhead
form.setFileMaxSize(Bytes.megabytes(5));    // parser limit for each file
form.add(new SwFileDropzone("files").setRequired(true).setAccept(".pdf,.txt")
        .setMaxFiles(3).setMaxFileSize(Bytes.megabytes(2))
        .setDescription("Up to three documents, 2 MB each."));
```

```html
<form wicket:id="form" method="post">
    <div wicket:id="files"></div>
    <button wicket:id="save" type="submit">Upload</button>
</form>
```

Add a normal Wicket submit button or `SwSubmitButton` to process the form. **Read or persist every `FileUpload` inside
the submit callback.** Wicket closes upload streams and clears the upload model at request detach; do not retain
`FileUpload` objects in page/session state or asynchronous work. The library does not store files automatically.
Use `getInputStream()` for streaming application processing; use application storage keys rather than treating a
client filename as a destination path.

`setMultiple(false)` or `setMaxFiles(1)` renders a single picker and enforces that limit server-side.
`setMaxFiles(...)` and `setMaxFileSize(Bytes)` validate the submitted files, including requests that bypass the browser.
A named zero-byte file counts as a selection. Parser-level failures occur before field validation: keep form request
limits in place and show a `SwFeedbackPanel` for form-level errors. In an Ajax form, add that panel to the target in
the error callback; inline field errors update themselves. `setAccept(...)` is a picker/drop hint, not server-side
content validation—add application validators appropriate to the files you process.

With the dropzone, repeated picks and drops append files, matching name/size/modification-time duplicates are ignored,
and files can be removed before submission. Files rejected by its type/count/size hints are listed separately and
excluded from the native `FileList`. Native selection and keyboard access still work without JavaScript. If the browser
lacks the `DataTransfer` support needed for enhancement, selection uses the native picker and removal buttons stay disabled.
The component enhances selection; transport progress, image previews and automatic submission are outside this API.

After an invalid Ajax submit, leave the upload component out of `target.add(...)` to keep the selection and focus.
The browser sends those files again on the next submit. After success, clear the browser selection explicitly:

```java
SwFileDropzone files = new SwFileDropzone("files", "Documents", Model.ofList(new ArrayList<>()));
form.add(files);
form.add(SwSubmitButton.onSubmit("save", "Upload", target -> {
    service.process(files.getModelObject()); // consume files during this request
    files.clearSelection(target);           // no extra change event or submit
}));
```

A full page render clears the selection, as native file inputs cannot be populated from the server. The **Uploads**
showcase (`/uploads`) covers native and Ajax submissions, a single-file picker, server count/size/type validation,
request limits, drag/drop/removal and a disabled dropzone. Its receipts keep only escaped filenames and byte counts;
file content is discarded. The landing-page reference-file demo selects a local file without uploading it.

### Checkboxes, switches and radio groups

These use the same field wrapper, model binding, `.properties` labels and inline feedback as text fields:

```java
SwCheckBox accepted = new SwCheckBox("accepted", "Accept the terms", Model.of(false))
        .setDescription("Please read the terms before continuing.");
form.add(accepted);
form.add(new SwSwitch("notifications", "Email notifications", Model.of(true))
        .setSize(SwitchBehavior.Size.SMALL));
form.add(new SwRadioChoice<>("contact", "Contact preference", Model.of("Email"),
        Model.ofList(List.of("Email", "SMS", "Phone")))
        .setRequired(true).setInline(true));
```

Bind each to a `<div wicket:id="..."></div>`, as with text fields. `SwRadioChoice<T>` wraps Wicket's
`RadioChoice<T>` with a fieldset, legend, individually labelled native radios and an optional `IChoiceRenderer`.
Choices stack by default; `.setInline(true)` puts them in a wrapping row. Help text and validation errors describe
both the group and each radio. The checkbox and switch remain native Boolean controls, including keyboard and
disabled behavior; `Sw.Behaviors.swSwitch()` styles an existing Wicket `CheckBox` as a switch.

As with Wicket's `CheckBox`, unchecked means `false`, and `setRequired(true)` does **not** require it to be checked.
For consent or another rule that requires `true`, add a validator:

```java
accepted.addValidator(value -> {
    if (!Boolean.TRUE.equals(value.getValue())) {
        value.error(new ValidationError().setMessage("Accept the terms to continue."));
    }
});
```

Use `AjaxFormComponentUpdatingBehavior("change")` on a checkbox or switch, and
`AjaxFormChoiceComponentUpdatingBehavior` on a radio group. Passing them to the field's `.add(...)` routes them to
the underlying input. Plain and Ajax form submits work too; field errors update in place without resetting focus
or the current selection. The showcase's **Choices** page demonstrates all of these paths.

### Dialog

A modal on the native `<dialog>` element, with the same API as Wicket Oat's `OatDialog`:

```java
SwDialog dialog = new SwDialog("delete", "Delete the project?") {
    @Override
    protected void onConfirm(AjaxRequestTarget target) {
        projects.delete(project);
    }
};
dialog.setDescription("This cannot be undone.");
dialog.setConfirmLabel("Delete").setConfirmVariant(ButtonBehavior.Variant.DESTRUCTIVE);
add(dialog);
add(SwButton.onClick("openDelete", "Delete project", dialog::open));
```

Give it content with `setContent(...)` (id `SwDialog.CONTENT_ID`); its form fields are validated on Confirm, and it
stays open while one has an error. `setTrigger("About")` adds a button that opens it in the browser, without a
request.

### Confirmation dialog

`SwConfirmDialog` follows Wicket Oat's reusable `ask(...)` API. Add one to the page, then ask it from any Ajax callback:

```java
SwConfirmDialog confirm = new SwConfirmDialog("confirm").setConfirmLabel("Delete");
add(confirm);
add(SwButton.onClick("delete", "Delete invoice", target ->
        confirm.ask(target, "Delete invoice?", "This cannot be undone.", t -> {
            invoices.delete(invoice);
            t.add(table);
        })));
```

Render it on `<div wicket:id="confirm"></div>`. The native dialog has `role="alertdialog"`, takes its accessible name
and description from the question, and focuses Cancel initially. Escape and Cancel dismiss it; clicking the backdrop
leaves it open. There is no corner close button by default. The confirm button defaults to `DESTRUCTIVE`; use
`setConfirmVariant(...)` and `setConfirmLabel(...)` for other actions.

The callback stays on the server, is consumed before it runs, and is forgotten when the dialog closes. Asking another
question replaces the pending callback. The callback may call `ask(...)` again to show a follow-up question without
closing. An ACTIVE Ajax channel ignores repeated confirm clicks while a request is in flight. Titles and descriptions
also accept `IModel<String>`. Subclasses overriding `onClose` must call `super.onClose(target)` to forget the callback.

### Sheet

`SwSheet` is a native modal panel with `SwDialog`'s content, validation and open/close API, following OatDrawer's role:

```java
SwSheet settings = new SwSheet("settings", "Project settings") {
    @Override
    protected void onConfirm(AjaxRequestTarget target) {
        service.save(project); // content fields have validated and updated their models
    }
};
settings.setDescription("Edit the project, then save your changes.")
        .setContent(new SettingsPanel(SwSheet.CONTENT_ID, project))
        .setConfirmLabel("Save")
        .setTrigger("Edit project")
        .setSide(SwSheet.Side.END);
add(settings);
```

Render it on `<div wicket:id="settings"></div>`. `Side.END` is the default; `START` and `END` follow text direction,
while `TOP` and `BOTTOM` span the viewport width. The header and footer stay visible while the body scrolls. The
corner close button, Cancel, Escape and backdrop dismiss it. `setFooterVisible(false)` supports informational sheets.

Open it from any Ajax handler with `open(target)`, or use `open(content, target)` for temporary content removed on
close. Re-rendering an open sheet keeps it modal. Invalid fields update in place and keep focus; unconfirmed browser
input is cleared when it closes. Native modality handles the inert background and focus return. The **Overlays**
showcase (`/overlays`) covers confirmations, follow-up questions, nested overlays and all four sheet edges. The landing
page previews a confirmation and an end sheet as browser-only demos.

### Popover

`SwPopover` follows OatPopover's content-factory API and renders a native, non-modal popover:

```java
SwPopover filters = new SwPopover("filters", "Filters", id -> new FilterPanel(id, filter))
        .setTitle("Filter projects")
        .setDescription("Choose which projects to show.")
        .setSide(SwPopover.Side.BOTTOM)
        .setAlign(SwPopover.Align.START);
add(filters); // <div wicket:id="filters"></div>
```

The factory receives `SwPopover.CONTENT_ID`. `getTrigger()` exposes the native button for attributes or behaviors;
`setTriggerVariant(...)` changes its default `OUTLINE` appearance. `setContent(...)` can replace the content later.
The optional title names the panel; without it, the trigger label supplies its accessible name. Titles, descriptions
and trigger labels accept models.

The button opens it without a request. Focus moves to its first control (or the panel for informational content),
and Escape, outside clicks or tabbing out dismiss it. The page stays interactive. A nested popover's Escape closes
that panel and returns focus to its trigger. `Side.START`/`END` follow text direction; `TOP`/`BOTTOM` and
`Align.START`/`CENTER`/`END` control placement. CSS anchors position and flip the panel without inline styles. Older
Popover API browsers without CSS anchor positioning show a viewport panel instead.

Re-render `getContent()` to update an open popover; replacing the entire `SwPopover` resets it to closed. A content
panel may own a Wicket `Form` and `SwSubmitButton`, including normal inline field feedback. After applying changes,
call `filters.close(target)` to dismiss it and return focus when focus was inside it. Open state stays in the browser.

### Tooltip

Add `TooltipBehavior` to an existing control, or use the facade:

```java
button.add(Sw.Behaviors.tooltip("Download a CSV copy."));
field.addToInput(new TooltipBehavior(Model.of("Use the public project name."))
        .setPlacement(TooltipBehavior.Placement.BOTTOM)
        .setDelay(Duration.ofMillis(300)));
```

The API retains Oat's `Placement.TOP`/`BOTTOM`/`LEFT`/`RIGHT`; left/right follow the inline start/end in RTL.
Tooltips contain escaped plain text and supplement an existing accessible name. Attach them to the actual focusable
component, such as a field's input. `Mode.DESCRIPTION` preserves existing `aria-describedby` help/error references;
`Mode.LABEL` visually repeats an existing name without adding a duplicate description, for example on an icon button
with its own `aria-label`. Null or blank labels produce no tooltip.

Keyboard focus shows a tooltip immediately; pointer hover uses a 300 ms default delay. It stays while the pointer is
over the trigger or tooltip, has no interactive content, and Escape dismisses it without closing a surrounding
popover or dialog. Touch does not show hover hints. Model changes and Ajax replacement refresh the text and remove
stale tooltip elements. Without JavaScript, the native `title` remains available as a fallback. Keep essential help
in visible text or an interactive popover.

The **Popovers & tooltips** showcase (`/popups`) covers forms, nested panels, reactive hints, label mode, touch and
positioning. The landing page uses the same library script for its Java badge popover and preview-control hints.

### Dropdown menus

`SwDropdown<T>` follows OatDropdown's action-menu API. It uses native buttons in a non-modal popover:

```java
add(SwDropdown.of("actions", "Export as", Model.ofList(List.of("CSV", "JSON", "XML")),
        format -> format,
        (target, format) -> export(format)));
```

Bind it to `<div wicket:id="actions"></div>`. The action factory invokes the Ajax callback and closes the menu;
`getTrigger()` and `getMenu()` expose its parts. An empty or null list disables the trigger. Replacing the whole
component over Ajax resets it to closed. `setSide(DropdownMenuBehavior.Side.END)` and
`setAlign(DropdownMenuBehavior.Align.END)` change placement; start/end follow the text direction.

For custom rows, subclass and implement `populateItem(ListItem<T>)`, or use the four-argument `of` factory.
Supply a `Label("label", ...)` child for each repeated button. Use `item.setEnabled(false)` for unavailable actions,
and configure its existing `DropdownMenuItemBehavior` for destructive styling or inset text. Add the action behavior
you need; the client dismisses the menu after activation, and `close(target)` is available to server callbacks.

For navigation, groups or checked choices, compose behaviors on your own markup:

```java
WebMarkupContainer trigger = new WebMarkupContainer("trigger");
trigger.add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE));
WebMarkupContainer menu = new WebMarkupContainer("menu");
menu.add(Sw.Behaviors.dropdownMenu(trigger));
menu.add(new BookmarkablePageLink<>("settings", SettingsPage.class)
        .add(Sw.Behaviors.dropdownMenuItem()));
add(trigger, menu);
```

```html
<button wicket:id="trigger" type="button">Account</button>
<div wicket:id="menu"><a wicket:id="settings">Settings</a></div>
```

Use one menu behavior per menu, with a native `type="button"` trigger. Other helpers are `dropdownMenuLabel()`,
`dropdownMenuSeparator()`, `dropdownMenuGroup()` and `dropdownMenuShortcut()`. Give groups an accessible name.
Shortcut text is a visual hint; it does not register a key binding. Use buttons or Wicket links for actionable rows,
and native form controls for ordinary form fields (`SwDropdownChoice<T>` is the native select field).

`dropdownMenuCheckboxItem(IModel<Boolean>)` and `dropdownMenuRadioItem(IModel<Boolean>)` render server-owned
`aria-checked` states and detach their models. Your callback updates the value, enforces radio exclusivity and adds
the item or menu to the Ajax target. The browser does not change checked state optimistically. Disabled items have
ARIA/native disabled state; disabled links lose their destination, and Wicket enforces server-side enabled state.

Enter/Space or Arrow Down opens with the first enabled item focused; Arrow Up opens at the last. Arrow keys,
Home/End and Page Up/Down navigate, and typing searches labels (repeated letters cycle matches). Disabled items
are skipped. Pointer opening focuses the menu; pointer movement focuses rows. Actions and Escape return focus
to the trigger, Tab/Shift+Tab close and continue through the page, and outside interaction dismisses the menu.
Placement, viewport limits, reduced motion and forced colors use the shared popup CSS in all skins.

The **Menus** showcase (`/menus`) covers Ajax actions/replacement, empty lists, checked choices, links,
confirmations and menus inside a sheet. The landing page's **Project actions** menu uses the same library script.

### Toasts

Use `Sw.toast(...)` for a direct notification from an Ajax callback, without creating a Wicket feedback message.
Its overloads follow Oat's message, variant and optional title API. Add the resource behavior once to your page or
base page before using the helpers:

```java
add(Sw.Behaviors.toasts());
add(SwButton.onClick("save", "Save", target -> {
    service.save();
    Sw.toast(target, "Your workspace is ready.", ToastBehavior.Variant.SUCCESS, "Changes saved");
}));
```

Variants are `ToastBehavior.Variant.DEFAULT`, `SUCCESS`, `INFO`, `WARNING` and `ERROR`. The message is the body;
an optional title appears above it. Both are plain text, including any HTML-looking characters. Error notifications
use an alert live region; other variants use a polite status. Showing a toast leaves keyboard focus in place.

General toasts close after five seconds. Set a duration on a particular toast, or use zero to keep it until dismissed:

```java
Sw.toast(target, "Review your settings before continuing.", ToastBehavior.Variant.WARNING,
        "Take your time", Duration.ZERO);
Sw.toast(target, "The link is ready.", ToastBehavior.Variant.SUCCESS, "Copied", Duration.ofSeconds(8));
```

Durations are zero or 1–2147483647 milliseconds. Timers pause while the pointer is over the notifications, while a
toast has keyboard focus and while the page is hidden. Resuming leaves at least one second. Keep essential results
visible on the page; choose persistent messages for information that needs more reading time.

Alt+T focuses the newest toast's close button. The close button or Escape dismisses the focused toast; focus moves
to a neighboring toast, then back to the control it came from after the last one. Escape elsewhere leaves toasts
alone. A named Notifications region holds a scrollable column at the logical bottom/end of the viewport, with
mobile gutters. While a native modal is open, the notifications move into it so they stay usable; they return to the
page when it closes. Removing the viewport clears its timers; navigating away clears old messages, including in
browser history snapshots. Toasts have no interactive action content besides dismissal.

For a full render, keep the same resource behavior and use the script helper from `renderHead`:

```java
response.render(OnDomReadyHeaderItem.forScript(ToastBehavior.script(this,
        "You can continue where you left off.", ToastBehavior.Variant.INFO, "Welcome back", Duration.ZERO)));
```

The component overload and Ajax helpers resolve `ToastBehavior.close` and `ToastBehavior.label` from Wicket
resources (defaults: “Dismiss notification” and “Notifications (Alt+T)”). The pure
`ToastBehavior.script(message, variant, title[, duration])` overloads use English control labels. For local browser
interactions the same runtime is available as
`Shadwick.toast(message, {variant: 'success', title: 'Copied', duration: 5000})`; options also accept `dismissLabel`
and `regionLabel`. `duration: 0` keeps the message. No inline styles or client framework are required.

The **Toasts** showcase (`/toasts`) covers all variants, timing, plain text, Ajax replacement, full-page scripts and
modal context. The landing page uses the library's toast runtime for its demo notifications. Existing
`FeedbackToastsBehavior` continues to handle Wicket messages and fences, with persistent, manually dismissed toasts.

### App shell

App-shell components include reusable sidebar/header layouts, theme controls, a user menu, responsive navigation,
site header, breadcrumb trail and skip link. Named per-user color themes remain later work.

`SwSiteHeader` is a container on caller-owned `<header>` markup, following `SwCard`'s composition API. Add your
branding, ordinary navigation links and actions. `Sw.Behaviors.siteHeaderEnd()` pushes an action container to the
logical end; the row wraps and grows on narrow screens:

```java
SwSiteHeader header = new SwSiteHeader("header");
header.add(new BookmarkablePageLink<>("brand", HomePage.class));
WebMarkupContainer end = new WebMarkupContainer("end");
end.add(Sw.Behaviors.siteHeaderEnd());
header.add(end);
add(header);
```

```html
<header wicket:id="header">
    <a wicket:id="brand">My workspace</a>
    <div wicket:id="end"><button type="button">Save view</button></div>
</header>
```

Use `Sw.Behaviors.siteHeader()` on an existing container. `setInset(true)` removes the header's own background and
bottom border for a header within the page. Native `<header>` placement determines its landmark semantics; no
extra banner role is added. Headers scroll with the page; pair them with a sidebar trigger for responsive navigation.

`SwBreadcrumb<T>` follows OatBreadcrumb's list-model, `populateItem`, `of`, `setCurrent`, `setLabel` and `setMaxItems`
API. Bind it to a `<nav>`. Each item needs a `"link"` child wrapping a `"label"` child:

```java
add(SwBreadcrumb.of("path", Model.ofList(folders), (item, folder) ->
        item.add(new BookmarkablePageLink<>("link", FolderPage.class, folder.parameters())
                .add(new Label("label", folder.name()))))
        .setCurrent("Details")
        .setMaxItems(3));
```

Without current text, the final link gets `aria-current="page"`; `setCurrent(String/IModel<String>)` renders separate
escaped current-page text instead. A null current value returns to the last-link behavior. With more than
`maxItems` levels (minimum 2, counting current text), the first and final levels remain visible and an ellipsis opens
a menu of the middle ones. It reuses the dropdown menu's native popover, keyboard navigation, typeahead, dismissal
and focus return. The ellipsis does not count toward the limit. Labels come from `SwBreadcrumb.label` (Breadcrumb)
and `SwBreadcrumb.more` (Show more levels); `setLabel` or markup ARIA naming can distinguish multiple trails.

Models are read once per request and detached. Re-render the breadcrumb over Ajax to update its links, current
label or collapsed ranges; replacing an open trail closes its menu. Null or empty levels hide the trail, including
when current text is present. A markup placeholder lets a cleared trail reappear over Ajax. The list wraps long
labels, separators are hidden from assistive technology, and chevrons reverse in RTL.

For caller-owned markup, `Sw.Behaviors` also supplies `breadcrumb()`, `breadcrumbList()`, `breadcrumbItem()`,
`breadcrumbLink()`, `breadcrumbPage()` and `breadcrumbSeparator()`. Supply the actual `<nav>/<ol>/<li>/<a>/<span>`
structure and decorative separator content yourself. Explicit `aria-label` or `aria-labelledby` wins over the
standalone behavior's localized `BreadcrumbBehavior.label` fallback.

Put a skip link first in `<body>`, before repeated navigation. With a supplied component target it gets a markup
id and a missing `tabindex="-1"` is added to a non-interactive container; existing tabindex and native controls
retain their behavior:

```java
WebMarkupContainer main = new WebMarkupContainer("content");
main.setMarkupId("main");
add(main, new SwSkipLink("skip", main));
```

```html
<a wicket:id="skip"></a>
<!-- Header/navigation -->
<main wicket:id="content">...</main>
```

`new SwSkipLink("skip")` targets `id="main"`; `new SwSkipLink("skip", "results")` targets another id. For these
id-only forms, give a non-interactive target `tabindex="-1"` yourself. The localized label is `SwSkipLink.label`
(Skip to main content); `setLabel(String/IModel<String>)` replaces it. `Sw.Behaviors.skipLink([target])` enhances an
existing anchor. The link uses native fragment navigation and works without JavaScript: Tab reveals it, Enter
moves focus to the content, and the next Tab continues from there. Forced colors retain visible focus.

The **App shell** showcase (`/shell`) covers composition, real breadcrumb navigation, Ajax updates, empty trails,
long labels, RTL and keyboard access. Every showcase page now has the skip link; the landing studio uses the same
header/breadcrumb styles and a focusable native skip target.

### Sidebar

`SwSidebar` renders native navigation links and disclosure groups using Oat's `MenuItem` API. Bind its `<nav>`
directly inside a provider, beside the content inset:

```java
WebMarkupContainer provider = new WebMarkupContainer("layout");
provider.add(Sw.Behaviors.sidebarProvider());
SwSidebar sidebar = new SwSidebar("sidebar", Model.ofList(List.of(
        MenuItem.of("Overview", OverviewPage.class).withIcon("house"),
        MenuItem.group("Workspace",
                MenuItem.of("Inbox", InboxPage.class).withIcon("mail").withBadge(unreadCount),
                MenuItem.of("Projects", ProjectsPage.class).withIcon("folder")))))
        .setCollapsible(SidebarBehavior.Collapsible.ICON);
WebMarkupContainer content = new WebMarkupContainer("content");
content.add(Sw.Behaviors.sidebarInset());
content.add(new SwSidebarTrigger("toggle", sidebar));
provider.add(sidebar, content);
add(provider);
```

```html
<div wicket:id="layout">
    <nav wicket:id="sidebar"></nav>
    <main wicket:id="content">
        <header><button wicket:id="toggle"></button></header>
        <!-- Page content -->
    </main>
</div>
```

At 48rem and above, the sidebar is a sticky column. `setCollapsible` chooses `OFFCANVAS` (default), `ICON`, or
`NONE`; `setVariant` chooses `SIDEBAR` (default), `FLOATING`, or `INSET`. `setSide(END)` puts it on the logical end,
including RTL. On smaller screens the same navigation opens as a native popover with a close button. The enhanced
panel focuses the current link, makes the background inert, traps Tab, and returns focus on Escape or dismissal.
The trigger and Ctrl/Cmd+B toggle it; the shortcut leaves text fields and modal dialogs alone. Link navigation
closes the mobile panel. Reduced motion, forced colors and printing have dedicated styles.

Desktop collapse is a browser preference, remembered in the `sw-sidebar-state` cookie. Configure the provider
with `setDefaultState`, `setRememberState(false)` or a distinct `setCookieName` for independently remembered
sidebars. The cookie contains only `expanded`/`collapsed` and is not server-side open state. Native mobile opening
and navigation also work without JavaScript; desktop navigation stays expanded in that case. Focus trapping,
background inertness, shortcuts, icon hints and desktop collapse use the shared library script.

Links retain ordinary Tab navigation and have no menu roles. The current page uses `aria-current="page"`, matching
its page class and any supplied parameters exactly. Override `isCurrentPage` for application-specific route matching
or `newMenuLink` for custom links, including Ajax links. Unauthorized pages and groups without authorized children
are omitted. Groups are one level deep and open when one of their children is current. Labels and badges remain
escaped, and a missing icon uses a generic circle. Badges hide when null, blank or numeric zero; visible badges
describe their links, including in the icon rail. Hover/focus hints appear only in the desktop icon rail.

Use `setHeader`/`setFooter` with components named `SwSidebar.HEADER_ID`/`FOOTER_ID` for caller-owned content.
`setLabel(String/IModel<String>)` names the navigation; resource keys are `SwSidebar.label`, `SwSidebar.close` and
`SidebarTriggerBehavior.label`. `target.add(sidebar.getMenu())` refreshes badges/links without closing the mobile
panel. Replacing the whole sidebar or provider closes it and releases background inertness. Caller-owned inert
regions stay inert. The `sw-sidebar-toggle` browser event exposes `{state, open, mobile}`.

`Sw.Behaviors.sidebar()`, `sidebarProvider()`, `sidebarTrigger(component)` and `sidebarInset()` also support
caller-owned anatomy; header/content/footer/menu/menu-item/menu-button/menu-label helpers provide the corresponding
classes. The **Sidebar** showcase (`/sidebar`) demonstrates layouts, real routes and Ajax updates. The landing
studio includes the same responsive navigation with persistence disabled.

#### Submenus and actions

Labelled `MenuItem.group(...)` sections can now contain submenus as well as ordinary links. A submenu summary opens
its child links; a page with children keeps its own link and has a separate, named disclosure beside it:

```java
MenuItem.submenu("Projects",
        MenuItem.of("Recent", RecentProjectsPage.class),
        MenuItem.of("Favorites", FavoritesPage.class)).withIcon("folder");

MenuItem.of("Settings", SettingsPage.class).withIcon("settings").withChildren(
        MenuItem.of("Billing", BillingPage.class),
        MenuItem.of("Members", MembersPage.class));
```

Both use native `details`/`summary`, including Enter and Space without JavaScript. Current authorized descendants
open their submenu and labelled section. Activating a submenu's icon in the collapsed rail expands the sidebar and
opens that submenu. Children are leaf page links; labelled groups cannot nest. The existing five/six-argument
`MenuItem` constructors remain available. A denied parent page hides its whole entry; a disclosure without a parent
page hides when it has no authorized children. An authorized parent link stays available when all its children hide.

Override `newMenuAction(String id, MenuItem item)` or `newGroupAction(String id, MenuItem group)` in `SwSidebar`
to return a component bound to a native `<button>`. Defaults are hidden. The supplied body has a decorative
ellipsis for an item and a plus for a group; name the action with `aria-label`, including its context:

```java
@Override
protected Component newMenuAction(String id, MenuItem item) {
    return new AjaxLink<Void>(id) {
        @Override public void onClick(AjaxRequestTarget target) {
            // Update application state, then refresh badges without closing the mobile panel.
            target.add(getMenu());
        }
    }.add(AttributeModifier.replace("aria-label", "Refresh " + item.label().getObject()));
}
```

Action hooks are for ordinary items and group headings; submenu parents use their disclosure control instead.
Actions and badges have separate space at a link's end. Actions hide in the icon rail; group actions sit outside
the group's summary. `data-show-on-hover` keeps an item action visible on keyboard focus and while its menu is open,
and always on mobile. For caller-owned buttons/links, `Sw.Behaviors.sidebarMenuAction()` and `.sidebarGroupAction()`
reuse the same styling and accessible-name checks; `setLabel(String/IModel)` supplies the name and
`setShowOnHover(true)` configures an item action. Existing dropdown behaviors can turn a caller-owned action into a
native menu trigger. The disclosure label resource is `SwSidebar.pages` (“{0} pages”).

#### Navigation in the header

`sidebar.setPlacement(SidebarBehavior.Placement.HEADER)` puts the same navigation in a `SwSiteHeader` inside the
provider, with the inset below the header. On desktop it becomes a horizontally scrolling row and never collapses;
its trigger hides, Ctrl/Cmd+B leaves it alone, and it does not change the collapse cookie. On mobile it is the same
panel, with the existing focus and inertness behavior. The `SwAppLayout` base page below composes this automatically.

The desktop row shows labels, badges and item actions. Group headings are visually hidden but still name their
sections. Pageless submenu disclosures and submenu children hide in the row; a parent page link stays available.
This is a row of navigation links, with no hover dropdown navigation widget. Use `MenuItem.withPanelOnly(true)` for
entries or groups that belong only in the mobile panel. Caller-owned parts use `data-panel-only` on the element.
`INSET` makes the page a card below the header; the other variants leave it plain. The row keeps the page within its
viewport when links overflow, including keyboard focus moving to a scrolled link.

### Reusable app layout

Extend `SwAppLayout` to share a complete application shell across pages. It follows OatAppLayout's constructors and
protected model/factory hooks, using the existing Shadwick components:

```java
public abstract class WorkspaceLayout extends SwAppLayout {
    protected WorkspaceLayout(PageParameters parameters) { super(parameters); }

    @Override protected IModel<String> appNameModel() { return Model.of("My workspace"); }
    @Override protected IModel<List<MenuItem>> sidebarMenuItemsModel() {
        return Model.ofList(List.of(
                MenuItem.of("Overview", OverviewPage.class).withIcon("house"),
                MenuItem.of("Inbox", InboxPage.class).withIcon("mail")));
    }

    // Omit this override for a sidebar with an icon rail.
    @Override protected SidebarBehavior.Placement sidebarPlacement() {
        return SidebarBehavior.Placement.HEADER;
    }
}
```

Concrete pages pass their `PageParameters` to `super`, add their usual components with `add(...)`, and put their
markup in `<wicket:extend>…</wicket:extend>`. A single focusable `<main id="sw-main">` receives that content. The first
link skips to it, `lang` follows the page locale, and the priority theme script applies browser preferences before
paint. The defaults are an inset sidebar, icon collapse, remembered desktop state and a header theme menu. Layout
hooks run in `onInitialize`, after the subclass constructor has initialized its fields. Page spacing and content
typography belong to the application.

Override `appTitleModel()` for the document title (`SwAppLayout.title` by default; the name resource is
`SwAppLayout.name`). `newTopNavExtra(id)` supplies header actions such as a panel containing a theme toggle and
header `SwUserMenu`. `newSidebarHeader(id)` supplies branding, and `newSidebarFooter(id)` supplies a footer such as
`SwUserMenu`; use the supplied IDs. In header placement, sidebar header/footer slots are mobile-only.

`sidebarVariant()` and `sidebarCollapsible()` select the appearance; `newSidebarProviderBehavior()` configures the
cookie, initial state or disabled persistence. `newThemeBehavior()` configures appearance or returns null for
application-owned appearance; also replace the default theme control if the app supplies another. `newSidebar(id)`
can replace the composed sidebar. `newMenuLink`, `newMenuIcon`, `isCurrentPage`, `isAuthorized`, `newMenuAction` and
`newGroupAction` expose the same hooks on the base page. Default route matching uses the exact page class and supplied
parameters; override it when UI query parameters should not affect the current entry.

After initialization, `getSidebar()`, `getTopNav()`, `getMain()` and `getProvider()` expose the shell. Use
`target.add(getSidebar().getMenu())` to refresh navigation in place; a whole sidebar replacement closes its panel.
The **App layouts** showcases (`/layouts` and `/header-layout`) share content and routes across both compositions,
with submenu navigation, badge actions, Ajax refresh/replacement and theme preferences. The landing studio's Layout
selector moves its existing navigation between the two compositions without storing a preference.

### Tabs and disclosures

`SwTabs<T>` follows OatTabs' caller-owned container API: two lists from one model, with `populateTab` and
`populatePanel` hooks or the `of`/`Sw.Components.tabs` factory. Every panel renders up front; the browser switches
views without a server request, preserving entered values in the panels:

```java
add(SwTabs.of("tabs", Model.ofList(List.of("Account", "Preferences")),
        (item, title) -> item.add(new Label("tabLabel", title)),
        (item, title) -> item.add(new Label("panelContent", title + " content")))
        .setLabel("Workspace settings").setSelectedTab(0));
```

```html
<div wicket:id="tabs">
    <div wicket:id="tablist">
        <button wicket:id="tabButtons"><span wicket:id="tabLabel"></span></button>
    </div>
    <div wicket:id="tabPanels"><div wicket:id="panelContent"></div></div>
</div>
```

The component owns trigger/panel IDs and their ARIA relationships. It renders the initial selected panel and hides
the others before scripts run. `setVariant(TabsBehavior.Variant.LINE)` selects an underline instead of the raised
track. `setOrientation(TabsBehavior.Orientation.VERTICAL)` gives a column;
`setActivation(TabsBehavior.Activation.MANUAL)` moves focus with the arrows and waits for Enter or Space to switch.
The default is horizontal/automatic. Left/Right wrap and follow RTL, vertical tabs use Up/Down, and Home/End reach
the first/last enabled tab. Tab enters on the selected tab and then reaches its panel. Disabled entries are skipped.
Manual focus remains stable during DOM updates and returns the list's tab stop to the selected tab when focus leaves.

For links that also work without JavaScript, subclass and override `tabUrl(T item, int index)` to return a
bookmarkable Wicket URL, for example `urlFor(SettingsPage.class, new PageParameters().set("tab", index))`. The component
renders anchors instead of buttons. Read the parameter and call `setSelectedTab(...)` when constructing the page.
Normal clicks switch in place and use `history.replaceState`, keeping history state and adding no history entry;
modified clicks keep native navigation. Button tabs need JavaScript to switch; without it the initial panel remains
visible. Only same-origin HTTP(S) links targeting this window are enhanced.

Override `isTabVisible(T item, int index)` and `isTabEnabled(...)` to configure both lists together. Hidden entries
omit their panels too; invalid/hidden/disabled initial selections fall back to the first enabled visible entry.
If all visible entries are disabled, the first stays visible as read-only context. Empty groups hide with an
Ajax-safe placeholder. The shared list model is read once per request and detached.

`setSelectedTab` configures server rendering; browser choices do not update a server model. Replacing the whole
component with `target.add(tabs)` restores that configured selection. Updating content inside an existing root keeps
its active view. Panels may contain other tabs; IDs, focus and state stay scoped to their own root. Once enhanced,
inactive enabled panels use `hidden="until-found"`: find-in-page or a fragment link reveals the matching tab, through
nested panels too, in supporting browsers. A non-bubbling `sw-tabs-show` event fires on a newly shown panel, not at
initialization. Disabled panels stay plainly hidden. Hidden-panel geometry remains zero even with host panel styles.
`Sw.Behaviors.tabs()` adds the same runtime/style to caller-owned markup with its own IDs, ARIA and initial state.

#### Accordion

`SwAccordion<T>` follows OatAccordion's repeating `ListView` API and factories. It uses native `details` groups,
without JavaScript. A callback supplies `title` and `content`; transparent trigger, heading and body containers add
the disclosure structure while keeping those component paths on the list item:

```java
WebMarkupContainer faq = new WebMarkupContainer("faq");
faq.setOutputMarkupId(true);
faq.add(SwAccordion.of("questions", Model.ofList(List.of("Shipping", "Returns")), (item, question) -> {
    item.add(new Label("title", question), new Label("content", "About " + question));
}).setHeadingLevel(3).setOpenItems(0));
add(faq);
```

```html
<div wicket:id="faq" class="sw-accordion">
    <details wicket:id="questions">
        <summary wicket:id="trigger">
            <span wicket:id="heading"><span wicket:id="title"></span></span>
            <svg class="sw-accordion-trigger-icon" viewBox="0 0 24 24" fill="none"
                 stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg>
        </summary>
        <div wicket:id="body"><div wicket:id="content"></div></div>
    </details>
</div>
```

The summary is the first child of each details; its inner span is a heading (level 3 by default, configurable 1–6).
The default exclusive group permits one section open, and permits closing all. The constructor's `exclusive` argument
or `setExclusive(false)` makes sections independent. `setOpenItems(int...)` configures initial indexes; for an exclusive
group the earliest configured index in document order wins. Refresh the enclosing wrapper over Ajax, rather than the
repeating ListView. Each render generates a fresh group name, so new markup does not join the old exclusive group
while Wicket replaces it. Nested accordions have independent groups.

Calling `item.setEnabled(false)` fades/skips its summary and omits closed content; an initially open disabled item
keeps its content. Normal keyboard activation, expanded state and opening a section for a fragment/find match come
from the browser. Height and chevron motion respect reduced motion; browsers without height-to-auto interpolation
open immediately. The runtime adds no accordion keyboard shortcuts. Initial open indexes are server configuration,
not live browser state. `Sw.Behaviors.accordion([exclusive])` supports caller-owned items (attach the same behavior to
the ListView and its items); `.setName(...)` can supply an explicitly shared name. The generated name is preferable
for Ajax replacement. Trigger/content helpers style caller-owned markup; applications manage its headings and
disabled content visibility.

#### Collapsible

`SwCollapsible` provides the same native disclosure without an accordion's group or heading semantics:

```java
add(new SwCollapsible("more", "A little more about this workspace")
        .setVariant(ButtonBehavior.Variant.OUTLINE)
        .setContent(new WorkspaceDetailsPanel(SwCollapsible.CONTENT_ID)));
```

```html
<details wicket:id="more"></details>
```

The label is escaped and accepts String/model values, as do text-content overloads. `setContent(Component)` accepts
arbitrary markup with `CONTENT_ID`; `getTrigger()` and `getContent()` expose the parts. A plain summary is the default;
`setVariant` or `setSize` draws it as a button using existing skins. `setOpen(true)` renders it open. `isOpen()` reports
that configured initial state; native user toggles do not post to Wicket. Re-render the component to apply server
configuration. Disabling it locks/skips the summary and omits content when initially closed, while initially open
content stays readable. It works without JavaScript and does not animate its height. `Sw.Behaviors.collapsible()` and
`.collapsibleTrigger()` support caller-owned details/summary; manage content visibility when disabling that markup.

The **Tabs & disclosures** showcase (`/disclosure`) includes bookmarkable tabs, manual/vertical/nested views, native
single/multiple accordions, disabled sections, fragment links and Ajax refreshes. The landing studio's Actions/Details
tabs preview all three families. Lazy server-created tab panels through Wicket's `ITab`/`AjaxTabbedPanel` remain a
separate integration. The data-table module now supplies `wicket-extensions`, so the dependency is available for that future work.

### Tables

`SwTable` styles caller-owned native table markup. Wrap a wide table in `sw-table-container` (or add
`Sw.Behaviors.tableContainer()` to a Wicket container):

```java
add(new SwTable("report").setSize(TableBehavior.Size.SMALL).setStickyFirstColumn(true));
```

```html
<div class="sw-table-container">
    <table wicket:id="report">
        <caption>Quarterly report</caption>
        <thead><tr><th scope="col">Customer</th><th scope="col" data-align="end">Total</th></tr></thead>
        <tbody><tr><th scope="row">Ada Lovelace</th><td data-align="end">€125.00</td></tr></tbody>
    </table>
</div>
```

`DEFAULT` and `SMALL` control density. `data-align="center"`/`"end"` align native header/body cells. Direct-child CSS
keeps nested tables independent. Ordinary `colgroup`, `scope="colgroup"`, `rowspan`, `colspan`, `thead`, `tbody`,
`tfoot` and `caption` work as HTML does. Sticky first cells use logical positioning for RTL, follow the row tint,
and return to normal flow in print. For a table on a card, set `--sw-table-bg: var(--card)` in your stylesheet.

A wide table scrolls inside its wrapper. Focusable cell content scrolls into view with Tab. For a text-only report,
a named wrapper with `tabindex="0"` gives explicit cross-browser keyboard access; the `/tables` showcase does this
for its wide quarterly report. `Sw.Behaviors.table()` styles existing Wicket table components with the same options.

### Data tables

`SwDataTable<T,S>` follows Wicket Oat's constructor and wraps Wicket's `DataTable`, using standard `IColumn<T,S>` and
`ISortableDataProvider<T,S>`. The library now includes Wicket's official `wicket-extensions` module at the same managed
version as `wicket-core`; applications still add only Shadwick. No Spring Data or client table framework is needed.

```java
IModel<Set<Long>> selectedIds = new SetModel<>(new HashSet<>());
List<IColumn<Invoice, String>> columns = new ArrayList<>();
columns.add(new SwSelectionColumn<Invoice, String>(selectedIds, Invoice::id,
        ids -> invoices.findAllById(ids)).setRowLabel(Invoice::number));
columns.add(new LambdaColumn<>(Model.of("Invoice"), "number", Invoice::number));
columns.add(new SwNumberColumn<Invoice, String>(Model.of("Amount"), "amount", Invoice::amount)
        .setCurrency(Currency.getInstance("EUR")));
columns.add(new SwDateColumn<>(Model.of("Due"), "due", Invoice::due));

var actions = new SwActionsColumn<Invoice, String>()
        .setRowLabel(Invoice::number).setRowKey(Invoice::id);
actions.addAction("Edit invoice", (target, row) -> editDialog.open(target));
actions.addAction("Delete invoice", (target, row) -> confirmation.ask(target,
        "Delete " + row.number() + "?", "This cannot be undone.", confirmed -> {
            invoices.delete(row.id());
            confirmed.add(table);
        })).setVariant(DropdownMenuItemBehavior.Variant.DESTRUCTIVE);
columns.add(actions);

// Assign a field on your page: private SwDataTable<Invoice, String> table;
table = new SwDataTable<>("invoices", columns, provider, 10)
        .setLabel("Invoices").setPaginationLabel("Invoice pages")
        .setPageSizes(10, 20, 50);
add(table);
```

```html
<div wicket:id="invoices"></div>
```

The snippet's callbacks can refer to your table field. Providers perform sorting/filtering/paging on the server:
`iterator(first,count)` returns that slice, `size()` counts the same filtered rows, and `model(row)` should return a
detachable model that reloads by key. Sort by a unique key last, including when the primary sort is descending,
so equal primary values have a stable order across pages. Allow only supported properties in repository queries.

Headers report `aria-sort` on the sorted `th` and show the direction. Sorting returns to page zero. Sorting and
paging use Ajax with normal Wicket links as fallbacks; summaries and controls update with the rows. `getTable()`
exposes the underlying Wicket table, and `addTopToolbar`/`addBottomToolbar` accept standard Wicket toolbars.
`SwHeadersToolbar` can also be added to a caller-owned Wicket `DataTable`.

`SwDataTable` implements `IPageableItems`: `getCurrentPage()`/`setCurrentPage(...)` are zero-based, and the page/item
count and items-per-page methods delegate to Wicket. Page size must be positive. Empty results retain one displayed
pagination page with disabled Previous/Next and a `0 of 0` summary.

Use `setCaption(String/model)` for a visible native caption, `setLabel(String/model)` for the table's accessible
name, `setSize(TableBehavior.Size.SMALL)` for compact rows, or `setStickyFirstColumn(true)` to pin the first column.
`setToolbar(id -> component)` adds caller-owned content above the table (`TOOLBAR_ID`); `setEmptyState(id -> component)`
replaces the default message inside a body cell spanning the columns (`EMPTY_STATE_ID`). Repeated calls replace the
slot. The table owns its responsive scroll wrapper and separate summary/size/pagination footer.

The column helpers in `dev.jbaby.shadwick.components.table` compose with ordinary Wicket columns:

- `SwBadgeColumn` renders escaped text with a row-dependent `BadgeBehavior.Variant`.
- `SwNumberColumn` formats numbers/currencies for the Wicket locale, aligns them to the end, and accepts fixed or
  row-dependent currency plus `setFractionDigits(min,max)`.
- `SwDateColumn` formats `java.time` values for the locale, with `setStyle`, `setPattern` and `setZone` for instants.
  Null number/date values stay empty.
- `SwBooleanColumn` renders a named, read-only native checkbox, or an empty cell for null.
- `SwLinkColumn.toPage(...)` creates a normal bookmarkable row link; `onClick(...)` runs an Ajax callback.
- `SwActionsColumn` renders a named native dropdown per row. `setRowLabel` makes the name distinguish rows;
  `setRowKey` supplies a unique row key, giving the trigger/menu stable IDs across replacements so focus can return after updates and dialogs.
  Actions support visibility/enabled predicates and a destructive menu variant. Predicates are checked again when
  invoked; application callbacks still own authorization and persistence. Give the provider detachable row models
  so those checks see current records.

`SwSelectionColumn` can keep whole row values, or just keys with an optional key loader. Prefer keys/loaders for
entities: bulk actions get current selected records, including ones hidden by a filter, without scanning the whole
provider. Without a loader, lookup by key scans the provider's current filtered results. `setRowLabel` names each
checkbox; `setSelectableWhen` excludes locked rows from row updates and select-all. The header selects only eligible
rows on the current page and becomes indeterminate for a partial selection, using the existing small runtime.
It reads rendered row models instead of querying the provider again.

```java
table.addBulkAction("Mark selected as paid", (target, rows) -> {
    invoices.markPaid(rows);
    table.clearSelection(target);
});
```

Bulk callbacks receive a fresh list and retain selection until `clearSelection()`/`clearSelection(target)` is called,
so a confirmation may be cancelled. Selection follows the supplied model across stateful Ajax paging/sorting;
bookmarkable navigation creates a new page, so retaining selection there needs an application-owned persistent
selection model. Selection changes bubble a `SwSelectionColumn.SelectionChanged` event with the Ajax target. The
bulk bar's count is not a live region, avoiding duplicate checkbox announcements. Live selection, row-action
callbacks and modal CRUD need JavaScript; native table links, filters and pagination remain usable without it.

Wicket Oat's CSV export, column chooser, expandable-row and inline-edit APIs remain later enhancements. Modal CRUD
and ordinary custom columns are available in this group.

### Bookmarkable table state

Opt into native URLs by passing the page's parameters:

```java
table.setQuery(new TableQuery(parameters));
```

The current page class must be reconstructible from `PageParameters`. In this mode sorting and pagination navigate
to real bookmarkable URLs, so reload and browser Back/Forward retain the view. Ajax row actions remain available.
The default `S.toString()` names sort properties; an overload accepts a custom sort-key function. Only keys from the
sortable columns are accepted. Unknown/invalid sorts leave the provider's configured default order. URL page sizes
are capped by `setMaximumPageSize(...)` (default at least 200, or the constructor's larger explicit size).

`TableQuery` is an immutable copy of decoded Wicket `PageParameters`. `page(...)` preserves everything else;
`sort(property,ascending)` and `size(...)` reset the page. `forSearch("q","status")` drops the current page and the
form's own fields, keeping sort, size and unrelated/repeated parameters. Negative/malformed page numbers fall back
to zero; malformed sizes use the configured default. No query value is inserted into HTML as raw markup.

For two tables use `new TableQuery(parameters).qualified("members")`, giving the second table `members_page`,
`members_size` and `members_sort`; the constructor also accepts custom parameter names. Qualifiers change only their
own page, preserving the other table's state. `getParameters()` returns another defensive copy.

`SwQueryParameters` renders escaped hidden inputs for a native GET form, including repeated values:

```java
formContainer.add(new SwQueryParameters("params", query.forSearch("q", "status")));
```

```html
<form wicket:id="form" method="get">
    <label for="invoice-query">Search invoices</label>
    <input id="invoice-query" name="q" type="search">
    <wicket:container wicket:id="params"></wicket:container>
    <button type="submit">Search</button>
</form>
```

Bind that form to a `WebMarkupContainer` with an action pointing at the page's bookmarkable route. These are native
query field names; Wicket form-processing field names are a different API. The `/data` showcase includes the complete
form, URL and provider setup, plus an Ajax mode whose toolbar parameters follow its current sort/size.

### Pagination

`SwPagingNavigator` accepts any `IPageable`, with Previous/Next, first/last page numbers, the current page's siblings
and ellipses. A gap of one page displays that page. `setSiblings(0..10)` controls the bounded window, without allocating
one item per page. Current links have `aria-current="page"`; disabled links lose their href/tab stop and retain a
named link role. Labels/messages are component resources, and `setLabel(String/model)` distinguishes multiple navigators.

```java
var query = new TableQuery(parameters);
add(new SwPagingNavigator("pages", pageable).setLabel("Project pages")
        .setPageUrl(page -> urlFor(ProjectPage.class, query.page(page))));
add(new SwPaginationSummary("summary", pageableItems));
add(new SwPaginationSize("size", pageableItems).setSizes(10, 20, 50)
        .setSizeUrl(size -> urlFor(ProjectPage.class, query.size(size))));
```

Without URL factories, the navigator/size menu update the pageable over Ajax with Wicket fallback links. Give a
component pageable an output markup ID; override `onAjaxEvent(target)` when a surrounding region or other controls
also need updating. With URL factories they are native links and work without script. `SwPaginationSummary` accepts
`IPageableItems` and locale-formats the visible range, or `setFormat(Format.PAGE)` for “Page 2 of 28”. The size menu
always includes the current size, even when not listed. Both stay independent of footer placement.

`SwPagination<T>` retains OatPagination's caller-owned `nav`/`menu`/`items` structure and `populateItem`/`of` factory:

```html
<nav wicket:id="pagination"><ul wicket:id="menu"><li wicket:id="items">
    <a wicket:id="link"><span wicket:id="label"></span></a>
</li></ul></nav>
```

Use `Sw.Behaviors.pagination()` for a named nav and `paginationLink().setCurrent(...)` for ordinary page links;
attach link behaviors to raw Wicket links, which already handle native hrefs. `Sw.Components.pagination(...)` is the
factory facade. For a list without a total count, compose Previous/Next yourself with this generic API rather than
inventing a page count for `IPageable`.

The `/tables`, `/data`, `/invoice` and `/pagination` showcases cover native/nested/grouped tables, compact and pinned
columns, stateful/bookmarkable navigation, row CRUD, selection and independent pagers. The landing studio uses the
actual embedded table/pagination CSS with transient browser-only sample interactions; Wicket owns data operations
in a consuming application.

### Attachment displays

`SwAttachment` displays a stored file or image. It does not upload files or retain Wicket's request-scoped
`FileUpload` objects. Supply application-owned URLs and authorize access at their endpoints.

```java
var notes = new SwAttachment("notes", Model.of("Project notes.txt"))
        .setDescription("Text document · 4 KB")
        .setFullWidth(true)
        .setLink("/files/notes", "Open Project notes.txt");
add(notes);

var preview = new SwDialog("preview", "Landscape preview").setFooterVisible(false);
preview.setContent(new LandscapePanel(SwDialog.CONTENT_ID));
add(preview);
add(new SwAttachment("photo", "Wicklow hills.svg")
        .setDescription("Illustration · 800 × 600")
        .setImageUrl("/files/wicklow/thumbnail")
        .setOrientation(AttachmentBehavior.Orientation.VERTICAL)
        .setPreview(preview.get("dialog"), "Preview Wicklow hills.svg"));
```

```html
<div wicket:id="notes"></div>
<div wicket:id="photo"></div>
<div wicket:id="preview"></div>
```

Title, description, image URL and image alt accept String/model setters. Models load once per request and detach.
A thumbnail crops to a square; the default file icon remains as its fallback while loading or after failure.
The picture is decorative beside a filename. `setShowContent(false)` creates a compact media-only card whose media
and fallback use the title as their accessible name; `setImageAlt(...)` can describe the picture instead.
`setIcon(...)` changes the fallback; `setMedia(component)` accepts custom markup using `MEDIA_ID` (`mediaContent`).

`setActions(panel)` accepts arbitrary Wicket markup using `ACTIONS_ID` (`actions`). For example, a Panel can contain
a native `ResourceLink` with a `download` attribute, a preview link and an Ajax remove button. Add
`Sw.Behaviors.attachmentAction().setLabel("Download Project notes.txt")` to each icon control. It reuses existing
button styling/accessibility checks, or installs a compact ghost button when none is present. With `SwButton`,
choose `GHOST` and `ICON` yourself and use a null label model to render its icon markup.
The actions stay above the stretched trigger, occur before it in tab order and never activate it.

`setLink(url, label)` creates a native card link. `setPreview(dialogElement, label)` creates a native button with
`commandfor`/`show-modal`; pass the actual dialog element, such as `preview.get("dialog")`. This opens in the browser
without JavaScript in supporting browsers; the existing runtime supplies its older-browser fallback. Such an external
native invoker does not call `SwDialog.open(target)` or synchronize its server-side `isOpen()` state. If an application
needs that state or replaces an open dialog over Ajax, supply a named `SwButton` trigger that calls `preview.open(target)`
via `setTrigger(...)`, using `TRIGGER_ID` (`trigger`). A custom trigger binds to an anchor in the supplied template;
a raw `WebMarkupContainer` can change its tag to a button in `onComponentTag`.
Disabled native links lose their href and tab stop; disabled buttons use the native disabled attribute.

`AttachmentBehavior.State` is `DONE` (default), `IDLE`, `UPLOADING`, `PROCESSING` or `ERROR`. The working states set
`aria-busy` and shimmer the title; reduced motion and forced colors disable shimmer. State is presentation only:
also supply readable text such as “Uploading · 64%” or “Upload failed · Please try again”. Cards add no live regions;
a page can announce updates in one polite status region. Sizes are `DEFAULT`, `SMALL`, `EXTRA_SMALL`.

`SwAttachmentGroup` owns a caller-supplied container. Its default `ROW` layout scrolls and snaps; `GRID` wraps.
Use direct attachment children (set ListView items to render their body only), vertical/full-width cards in a grid,
and `setLabel("Project files")` for a named group. A named row takes keyboard focus for native arrow-key scrolling;
a grid adds no tab stop. RTL and print layouts are supported. The same APIs are available as
`Sw.Behaviors.attachment()`, `attachmentGroup()`, `attachmentAction()` and `attachmentTrigger()` on existing components;
caller-owned part markup uses `sw-attachment-media`, `-content`, `-title`, `-description` and `-actions` classes.

The `/attachments` showcase includes working downloads, a native image preview, row/grid groups, sizes, image fallbacks,
Ajax removal/restoration and a server-rendered status cycle. The landing studio includes a downloadable illustration
with a preview dialog using the actual embedded CSS and runtime.

### User menu

`SwUserMenu` follows OatUserMenu's model-backed API. It shows an account menu while the name is non-null, and an
optional Sign in link otherwise. Its default placement is the sidebar footer:

```java
SwUserMenu user = new SwUserMenu(SwSidebar.FOOTER_ID, () -> MySession.get().getUserName())
        .setDetail(() -> MySession.get().getEmail())
        .setPicture(() -> MySession.get().getPictureUrl())
        .setSignInPage(SignInPage.class);
user.addLink("My account", AccountPage.class).setIcon("user");
user.addLink("Settings", SettingsPage.class).setIcon("settings");
sidebar.setFooter(user);
```

For the site header, bind to a caller-owned `<div wicket:id="account"></div>` and use
`setPlacement(SwUserMenu.Placement.HEADER)`. The header button shows the avatar alone and is named “Account: …”; the
sidebar button shows the name and optional detail, and narrows to the avatar in an icon rail. Initials provide the
fallback while a picture loads or when none is available. Header initials are decorative CSS glyphs, so the button's
accessible name stays the full account name. All skins use a round header avatar button.

The sidebar menu opens beside the trigger and aligns to its end; the header menu opens below it. Existing mobile
sidebar styles place the menu above/below its trigger. `setSide` and `setAlign` override these defaults. Icon-rail
name hints reuse the tooltip behavior and stay hidden while the menu is open. Native popovers and the existing
menu runtime provide arrow keys, Home/End, typeahead, Tab, Escape and focus return. Opening and submitting Sign out
also work without JavaScript.

`addLink` accepts String or `IModel<String>` labels, a page class and optional `PageParameters`, returning a
`MenuLink` with `setIcon`. Links are escaped, unauthorized pages are omitted, and exact current-page matches get
`aria-current="page"`. `setDetail` and `setPicture` accept String or model values. Name/detail/picture models are read
once per request and detached. Refresh the whole component with `target.add(user)` after an account update; an open
menu closes on replacement. `getTrigger()` and `getMenu()` expose the composed controls.

`setSignInPage` supplies an ordinary bookmarkable link, hidden on that page and its subclasses. Without a sign-in
page, an anonymous menu has no visible content. Override `isSignedIn()` for an authenticated user whose name can be
null; the display falls back to “Your account”, with initials from the detail's email local part if available.

Sign out submits a separate hidden `StatelessForm` outside `role="menu"`, through a native submit button associated
by `form`. Place the user-menu component outside other forms. It keeps an otherwise stateless page stateless and
processes a previously rendered sign-out action after session expiry. The default `signOut()` invalidates the Wicket
session, then `onSignedOut()` redirects to the home page. Override these hooks for application authentication and
redirect handling; no Spring Security dependency is introduced. The form rejects non-POST requests and requests
Wicket's Fetch Metadata isolation policy identifies as cross-site. `getSignOutForm()` exposes the form for application
integration.

Labels are Wicket resources: `SwUserMenu.account`, `SwUserMenu.signIn`, `SwUserMenu.signOut` and `SwUserMenu.unnamed`.
The **User menu** showcase (`/user-menu`) demonstrates both placements, Ajax name/picture changes and a session-backed
demo sign-in/out flow. The landing studio previews the same anatomy with browser-only demo actions.

### Theming

Shadwick uses shadcn/ui's CSS variables (`--background`, `--primary`, `--radius`, …), so any shadcn theme, from the
[theme generator](https://ui.shadcn.com/themes) or [tweakcn](https://tweakcn.com), drops in unchanged: redefine the
variables in your own stylesheet. Dark mode is `class="dark"` on `<html>`.

#### Light, Dark and System preferences

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

#### Embedded styles

If your application supplies its own reset and page styles (for example, its own Tailwind build), use the embedded
bundle. It includes the same tokens and components, with no preflight, page base styles or Tailwind theme variables:

```java
Shadwick.install(this).setSkin(Skin.LYRA).setBundle(Bundle.EMBEDDED);
```

Configure the bundle at application startup. The default is `Bundle.STANDALONE`.

### Display components

```java
add(new SwCard("card").setSize(CardBehavior.Size.SMALL));
add(new SwBadge("badge", "New", BadgeBehavior.Variant.SECONDARY));
add(new SwAlert("alert", "The project could not be saved.", AlertBehavior.Variant.DESTRUCTIVE));
add(new SwAvatar("avatar", "AL").setLabel("Ada Lovelace"));
add(new SwSeparator("separator"));
add(new SwKbd("key", "K"));
add(new SwSkeleton("placeholder", SkeletonBehavior.Shape.CIRCLE));
add(new SwSpinner("loading").setLabel("Loading projects"));
add(new SwIcon("icon", "search").setLabel("Search"));
add(new SwEmptyState("empty", "No projects yet", "Create one to get started.").setIcon("folder"));
```

Cards keep your markup: use `sw-card-header`, `sw-card-title`, `sw-card-description`, `sw-card-content` and
`sw-card-footer` for their parts. `Sw.Behaviors.card()`, `.badge()`, `.alert()`, `.separator()`, `.kbd()`,
`.skeleton()` and `.spinner()` style existing Wicket components. A composed alert uses `sw-alert-title` and
`sw-alert-description`; `SwAlert` is a plain, escaped message.

Avatars accept a picture URL model, optional initials model and size, as in Wicket Oat. Initials show while the
picture loads or if it fails, including after Ajax replacement. Icons are bundled Lucide SVGs; `SwIcon.names()` lists
them, `setLabel(...)` names a standalone icon, and an unnamed icon is decorative. Bind icons and spinners to `<svg>`
or a wrapper tag, keys to `<kbd>`, and other display components to a container. Skeletons are decorative; put an
accessible loading status on their surrounding region. Separators are decorative unless `.setDecorative(false)`.

### Feedback

```java
// A panel for this form's messages; updates and clears itself during Ajax requests.
form.add(new SwFeedbackPanel("feedback", form));

// On a page: messages outside fenced panels, including session messages after navigation.
add(Sw.Behaviors.feedbackToasts());
```

Both skip the first field error already shown inline. Panels render alerts; errors use the destructive variant,
other levels use the default. Toasts preserve the feedback level, render message text safely and remain until
dismissed. Panel filters and fences work as with Wicket's `FencedFeedbackPanel`; a fenced panel reserves its
container's messages even while hidden. Set up the fences when constructing the page.

## Building

```sh
./mvnw clean verify
```

No Node needed: the build installs a pinned Node LTS into `target/` and compiles the CSS with Tailwind CSS v4. The
jar ships only the finished CSS.

Run the showcase to explore the components, including Menus and Toasts, with a skin and light/dark switch on every page:

```sh
./mvnw install -DskipTests
java -jar shadwick-examples/target/shadwick-examples-0.1.0-SNAPSHOT.jar
```

Then open http://localhost:8080. While working on the CSS, `npm run watch:css` in `shadwick-core` (with a local
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

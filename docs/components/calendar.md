# Calendar and date picker

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

## Selection and validation

The first range pick starts it; the second completes it, swapping earlier endpoints. A completed range starts
a new one on the next pick. Disabled days remain reachable by arrows but cannot be selected; a range crossing
an unavailable day is rejected. An optional single date clears when its selected day is picked again. Required
dates remain selected. `setReadOnly(true)` permits browsing without changing the value; `setEnabled(false)`
disables navigation and selection. Min/max and disabled-date rules are also enforced without JavaScript.

## Calendar options

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

## Caller-owned markup

For caller-owned markup, place one or two labelled date inputs inside a wrapper with `Sw.Behaviors.calendar()`.
Set `min`, `max`, `readonly`, `required` and `disabled` on those native inputs. The typed fields wire these
attributes and validate the shared rules; behavior-only applications must enforce their own server validation.
`DateRange.contains(date)` handles open ends. The **Calendar & date picker** showcase (`/calendar`) demonstrates
all four fields, normal/live/Ajax validation, month/year selection, week numbers, reset and overlays. The landing
studio includes an inline catch-up calendar and a date picker in its project dialog.

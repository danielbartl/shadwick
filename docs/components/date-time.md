# Date and time fields

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

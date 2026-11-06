# Month view

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

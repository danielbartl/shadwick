# Charts

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

Series support `setCurve`, `setDashed`, `setDots`, `setFillOpacity(0..1)` and
`setColor(1..5)` for chart tokens (`0` selects automatically). Curves apply to lines and areas; dots apply to both.
The default curve is a monotone cubic interpolation without overshoot. Colors follow light/dark theme tokens;
application CSS can override the chart tokens. Geometry uses finite numbers of magnitude at most `1e100`.
Labels, formatted values and categories are escaped; values are snapshotted once per request, and models detach.

## Appearance

Keep the palette, or give a series its own light/dark identity:

```java
var actual = ChartSeries.line("Actual", Report::actual)
        .setColors("#047857", "#34d399")
        .setCurve(ChartSeries.Curve.NATURAL)
        .setStrokeWidth(3);
```

`setColor(String)` uses one CSS color in both modes; `setColors(light, dark)` uses the light color when dark is null.
`setColor(0..5)` restores automatic/token colors without changing other series options. Plot marks, legend and
tooltip indicators share the same colors. Changes to the document or local wrapper's `.dark` class apply immediately.
Choose colors with enough contrast in both modes; the table remains available independently of color.

Colors accept hex, named colors, numeric `rgb/rgba/hsl/hsla/hwb/lab/lch/oklab/oklch/color` functions and
`var(--property)`. Define custom properties in application CSS. Declarations, markup, URLs and nested/fallback
expressions are rejected; put more elaborate expressions in an application custom property instead.
Browser-unsupported literals retain the ordinary palette. Scoped rules are contributed through Wicket's
nonce-protected header styles and deduplicated by their content. No style attributes, external resources or CSP
relaxation are needed. Ajax can change the palette without refreshing the page or affecting another chart.

### Curves and strokes

`ChartSeries.Curve` offers:

| Curve | Behavior |
| --- | --- |
| `MONOTONE` | Default smooth joins without overshoot between samples |
| `LINEAR` | Straight segments |
| `NATURAL` | Natural cubic spline with continuous first/second derivatives and natural endpoint boundaries |
| `STEP_BEFORE` | Switch value at the earlier category |
| `STEP_AFTER` | Switch at the later category |
| `STEP_MIDDLE` | Switch halfway between categories |
| `STEP` | Existing behavior, equivalent to `STEP_AFTER` |

Natural splines can overshoot between samples. Their line/fill is clipped to the plot; dots, labels, tooltips and
the table still show the original samples. Nulls split every curve into independent runs. All curves work with
line/area forecast joins and stacked areas. `setStrokeWidth(0..20)` selects native pixel width for lines/area outlines;
the default is 2. Bar fills are unaffected.

### Bar corners

```java
ChartSeries.bar("Delivered", Report::hours).setBarRadius(10, 10, 0, 0);
```

`setBarRadius(radius)` sets every corner; the four-argument overload sets physical top-left, top-right, bottom-right
and bottom-left corners. Values are native pixels, from 0 to 1000, clamped by SVG to half the actual bar width/height.
Zero makes a square corner. Physical corners stay the same in RTL and in either orientation, including negative
and stacked bars. For an outer rounded stack end, keep its inner series square and round the outer series explicitly.
Radii describe physical corners, so a negative value does not swap them automatically.

Explicit corners use native-pixel SVG viewports/masks with percentage positions. The bar resizes while its corners
retain their size; rendering also works without JavaScript. The table and hit regions keep their original data and
geometry. Existing charts retain their original bar rendering until a radius is supplied.

### Ticks, tooltips and legends

```java
chart.setValueTickCount(5)
        .setMaxCategoryTicks(8)
        .setMinCategoryTickGap(8)
        .setTooltipIndicator(SwChart.Indicator.LINE)
        .setLegendPosition(SwChart.LegendPosition.TOP)
        .setLegendAlignment(SwChart.LegendAlignment.START);
```

`setValueTickCount(2..20)` requests an approximate numeric tick count; nice domain rounding can add a tick. The
default is 5. `setMaxCategoryTicks(1..1000)` defaults to 8 and `setMinCategoryTickGap(0..100)` to 5 pixels. With script,
category labels are measured after font/size/direction changes; clipped or overlapping candidates are hidden while
respecting the maximum and minimum gap. Without script, a uniformly spaced subset remains, thinned further on
narrow screens. Horizontal bars retain every wrapped category row; numeric tick configuration still applies.
Tick thinning never changes the tooltip categories or complete data table. It also works with tooltips disabled.

Tooltip indicators offer `AUTO` (default, preserving each series' dashed identity), `DOT`, `LINE` or `DASHED`.
`setShowTooltipLabel(false)`, `setShowTooltipIndicator(false)` and `setShowTooltipCursor(false)` hide the visual
header, marks or category highlight independently. Keyboard announcements and native SVG titles retain the full
category/value context. `setShowTooltip(false)` still disables enhanced exploration entirely.

Legends can appear at `TOP` or `BOTTOM` (default), with logical `START`, `CENTER` (default) or `END` alignment.
Alignment follows RTL. Series order, names and values stay unchanged. Use `setShowLegend(false)` to omit the legend.

## Horizontal bars

Use horizontal bars for rankings and categories with long names. The data's order is the order from top to bottom;
sort it in the application when a ranking is required.

```java
add(new SwChart<>("clients", clientsModel, ClientHours::name)
        .setTitle("Hours by client")
        .setCategoryHeader("Client")
        .setOrientation(SwChart.Orientation.HORIZONTAL)
        .addSeries(ChartSeries.bar("Delivered", ClientHours::delivered))
        .addSeries(ChartSeries.bar("Planned", ClientHours::planned)));
```

Bind it to `<div wicket:id="clients"></div>`. Bar series are grouped by default. Give series the same
`setStack("hours")` to stack them in a shared row; different stacks and unstacked series get separate slots.
Positive values accumulate to one side of zero and negative values to the other. A null leaves an empty slot
and an empty data-table cell; zero remains a real value. Every row uses the same value scale.

Category names wrap in an HTML column, and each row grows to fit its text and bar groups. The chart height grows
with its rows. `setShowYAxis(false)` hides category names; `setShowXAxis(false)` hides the value labels underneath.
`setShowGrid(false)` hides grid lines while retaining the zero baseline. Horizontal orientation accepts bar series
only; combining it with a line or area fails clearly at render time. `VERTICAL` remains the default.

With script, focus the plot and use Up/Down, Home/End and Escape. In RTL, labels move to the right and value geometry
mirrors; row order and Up/Down navigation stay the same. Native SVG titles and the exact data table also work without
script. Formatting, legends, tooltips, skin colors, model detachment and `target.add(chart)` work in either orientation.

## Formatting and interaction

`setValueFormat(NUMBER/INTEGER/COMPACT/PERCENT)` uses the session locale. `setCurrency(Currency)` selects currency
formatting. For custom output use `setValueFormat((number, locale) -> ...)` or `setCategoryFormat((value, locale)
 -> ...)`. These shared formats supply the data table, value labels and, by default, axes and tooltips.

Use separate formats when the overview needs shorter text than the detail:

```java
add(new SwChart<>("revenue", reportsModel, Report::date)
        .setTitle("Monthly revenue")
        .setCategoryHeader("Report date")
        .setAxisCategoryFormat(SwChart.CategoryFormat.MONTH_SHORT)
        .setAxisValueFormat(SwChart.ValueFormat.COMPACT)
        .setTooltipLabelFormat(SwChart.CategoryFormat.DATE)
        .setTooltipCurrency(Currency.getInstance("EUR"))
        .addSeries(ChartSeries.bar("Revenue (EUR)", Report::revenue)));
```

The category axis can show `Apr` and numeric ticks `20K`, while the tooltip shows `Apr 1, 2026` and `€12,345.67`.
The table still uses the shared category and value formats, and value labels still use the shared value format.
Defaults remain unchanged when no overrides are supplied.

| Part | Named format | Custom formatter |
| --- | --- | --- |
| Numeric axis | `setAxisValueFormat(ValueFormat)` | `setAxisValueFormat((number, locale) -> ...)` |
| Category axis | `setAxisCategoryFormat(CategoryFormat)` | `setAxisCategoryFormat((category, locale) -> ...)` |
| Tooltip header | `setTooltipLabelFormat(CategoryFormat)` | `setTooltipLabelFormat((category, locale) -> ...)` |
| Tooltip values | `setTooltipValueFormat(ValueFormat)` | `setTooltipValueFormat((number, locale) -> ...)` |

Axis settings follow the data's meaning: a horizontal chart's numeric axis is X and category axis is Y.
Rotating the chart keeps its formatting. Numeric overrides accept all `ValueFormat` choices, including `CURRENCY`.
`setAxisCurrency(Currency)` and `setTooltipCurrency(Currency)` select a currency for just that part. A named `CURRENCY`
override without its own currency uses the shared `setCurrency` currency; if neither is set, rendering fails clearly.
Explicit overrides survive later shared-format changes. Within a part, the last named/custom/currency setter wins.
Passing null through a custom-formatter overload clears that part's override and restores inheritance.

### Dates and tooltip labels

`CategoryFormat` works in the shared `setCategoryFormat`, category-axis and tooltip-label settings:

| Format | Example in English | Required fields |
| --- | --- | --- |
| `DAY` | Apr 1 | Month and day |
| `MONTH_SHORT` | Apr | Month |
| `MONTH` | April | Month |
| `MONTH_YEAR` | Apr 2026 | Month and year |
| `DATE` | Apr 1, 2026 | Calendar date |
| `TIME` | 10:00 AM | Hour and minute |

Month names use the component locale, with the standalone grammatical form for `MONTH`; `DATE` and `TIME` use
localized styles. Dates/YearMonth are formatted on the
server as calendar values; LocalDateTime stays on its written clock. Instant/OffsetDateTime/ZonedDateTime use
`setTimeZone(ZoneId)` (UTC by default), independently of the browser's zone. `LocalTime` supports `TIME`.
A named format with incompatible input keeps the automatic category representation: a YearMonth has no time,
and a string that looks like a date is still a string. Custom category callbacks receive the original object and
locale, so use them for another pattern or type and handle any instant-to-zone conversion explicitly.

An alternate tooltip header can come from another property of the same row:

```java
chart.setTooltipLabel(invoice -> invoice.client() + " · " + invoice.reference());
```

The tooltip's label formatter applies to that extracted property. Without an override it uses the shared category
formatter; `setTooltipLabel(null)` restores the constructor's category extractor. This does not change the axis
categories or data-table row headers.

Formatting runs on the server. Data/category extractors and the optional alternate-label extractor run once per row
per request. Measurements reach formatters as their original `Number`, including BigDecimal precision; numeric-axis
ticks are computed doubles. Null measurements stay empty without calling the formatter. A callback returning null
or an empty string produces empty text, even
when the shared table cell has text. Attribute and text output is escaped; the runtime uses textContent.
Enhanced tooltips, keyboard announcements and native SVG titles use the same tooltip formats. The existing table
carries any differing tooltip text in data attributes; no client-side date parser, formatter code or extra request is needed.

`setShowGrid`, `setShowXAxis`, `setShowYAxis`, `setShowLegend` and `setShowTooltip` control parts. Every plot has an
accessible name and a native **View data** disclosure. With script, focus the plot and use Left/Right for vertical
charts or Up/Down for horizontal bars, with Home/End and Escape in either orientation. RTL follows visual order.
Ajax replacement removes old listeners/observers; call `target.add(chart)` to
refresh. CSS controls chart colors; the vertical graph uses a 240px plot height and fills its container. Tooltip and legend
geometry follow each skin, including embedded bundles, forced colors and reduced motion.

## Value labels

Labels are opt-in on each `ChartSeries`. The chart's value formatter also formats them:

```java
ChartSeries.bar("Visitors", Visit::desktop)
        .setValueLabels(ChartSeries.ValueLabels.HIGHEST_AND_LAST);
ChartSeries.line("Revenue", Month::revenue)
        .setValueLabels(ChartSeries.ValueLabels.LAST);
```

`NONE` is the default. `LAST` labels the last non-null value. `HIGHEST_AND_LAST` adds the highest numeric value,
choosing the first when tied; a point selected twice is labeled once. `ALL` requests a label for every non-null
value, including zero. For stacked bars/areas, labels show the original series values, rather than cumulative totals.
These modes work for bars, lines, areas and horizontal bars. Tooltips and the full data table retain all values.

Labels use a separate SVG with pixel-sized text and percentage anchors, so resizing never stretches the letters.
RTL uses mirrored anchors with readable numeric text. With JavaScript, overlapping or clipped labels are hidden;
the latest value has priority, followed by the highest. Resize, font and direction changes recompute placement, and
Ajax replacement disconnects old observers. This also works with `setShowTooltip(false)`.

Without script, vertical charts retain highest/latest labels to keep dense `ALL` requests readable; horizontal
bars keep their row labels. For small or dense native charts, prefer `LAST` or `HIGHEST_AND_LAST`. Custom value
formatters are escaped in labels just as in the tooltip and table.

## Forecast joins

A dashed line or area can continue another series without duplicating an actual value in the data:

```java
var actual = ChartSeries.line("Actual", Month::actual).setColor(1);
var forecast = ChartSeries.line("Forecast", Month::forecast)
        .setColor(1)
        .setDashed(true)
        .setJoin(actual);
add(new SwChart<>("sales", monthsModel, Month::name)
        .setTitle("Actual and forecast")
        .addSeries(actual)
        .addSeries(forecast));
```

The join takes a `ChartSeries<T>` reference, rather than a legend label or translated key. Register both series
in the same chart, in either order. Changing a label does not change the relationship. `setJoin(null)` clears it.

For a forecast beginning in April, the renderer finds the source's last non-null value before April and borrows
its plotted coordinates. If that is March, the dashed curve starts at the March point and continues to April.
The source model stays unchanged: March's forecast cell remains empty, its tooltip/native title/spoken values
list only the actual value, and the borrowed point gets no forecast dot or value label. The first real forecast
value still appears in April. Use the same chart token for a continuation, or a different one for a distinct scenario.

Joins work for all supported line/area curves. Area fills retain their opacity while the upper line is dashed.
Missing categories between the handoff and the first forecast, or gaps later in the forecast, remain gaps; joins
do not interpolate missing reports. If either side has no suitable value, or the forecast starts at the first row,
no anchor is added. Zero and negative source values are valid. Series that already overlap keep their supplied
values; the anchor is always strictly before the first real forecast value.

The joined series must be a dashed line/area and its source must be a line/area in the same chart. Self-joins,
cycles, duplicate registration in a joined chart and foreign sources fail clearly. Joined stacked areas must
share the same stack; they borrow the existing source boundary without adding the anchor to the stack again.
Joins can reference another joined series, but only its real input samples are eligible as a source.

The request snapshot supplies all coordinates and original text; joins do not reread extractors or mutate rows.
They render without JavaScript and recompute after `target.add(chart)`, so recording another actual period can
move the handoff forward. Keyboard/pointer tooltips and the accessible table retain their usual behavior.

## Sparklines

`SwSparkline` follows Wicket Oat's small-trend constructor and uses caller-owned markup:

```java
add(new SwSparkline("trend", weeklyHoursModel)
        .setFilled(true)
        .setColor(2));
```

```html
<p>Delivered this week: <strong>84 h</strong> <span wicket:id="trend"></span></p>
```

The model is `IModel<? extends List<? extends Number>>`, ordered oldest to newest. Put it in an existing `SwCard`
or a table cell, beside text that gives the value, period and meaning. It is decorative (`aria-hidden="true"`),
adds no focus stop, and has no tooltip or data disclosure. Use `SwChart` when users need to explore exact values.

The line is quiet and its latest known point uses a chart-token accent. `setColor(1..5)` changes that accent and
the optional fill. `setShowLastPoint(false)` hides the accent marker. Each sparkline scales its own finite values
(magnitude at most `1e100`), including negatives; heights across separate sparklines do not represent a shared scale.
Null entries leave gaps without shifting later positions. Isolated values remain points, a single value is centered,
flat values draw at mid-height, and empty/all-null models render no invented trend. Fill does not bridge gaps.

Rendering is native SVG, with no JavaScript dependency or inline styles. The quiet stroke and accent dot retain
their size as width changes, and RTL mirrors the oldest-to-newest direction. CSS defaults to 100 by 32 pixels;
adjust `.sw-sparkline` width in application CSS to fit a card or table cell. The component outputs a markup ID for
`target.add(sparkline)`, and its model detaches through Wicket's normal component lifecycle. Update any neighboring
value together with its trend when both change.

## Donut charts

`SwDonutChart<T>` compares positive parts of a whole using Wicket Oat's model/category/value constructor pattern:

```java
add(new SwDonutChart<>("regions", regionsModel, Region::name, Region::revenue)
        .setTitle("Revenue by region")
        .setCategoryHeader("Region")
        .setValueHeader("Revenue")
        .setCurrency(Currency.getInstance("EUR")));
```

Bind it to `<div wicket:id="regions"></div>` and import it from `dev.jbaby.shadwick.components.chart`.
The center shows the total; the legend lists each plotted part's value and percentage. The native **View data**
table preserves every original row and includes a total footer. Labels, custom formatting and native titles are escaped.

By default, more than five positive parts become the four largest plus **Other**. Kept parts retain their input
order; ties favor the earlier row, and identical rows each contribute once. `setMaxSegments(3)` keeps two plus Other;
the minimum is two. `setGroupSmallSegments(false)` draws all positive rows. Colors use the five chart tokens and
repeat after the fifth part. Grouping never removes rows from the exact table.

Values must be finite, nonnegative and at most `1e100` each. Negative values fail clearly rather than being clamped.
A null remains a missing value with empty value/share cells, while zero shows `0` and `0%`; neither creates a slice.
An empty model has an empty state. An all-zero/missing model keeps the zero total, ring track and exact table, with
a no-positive-values message. Decimal totals retain `BigDecimal` precision, and integral totals retain integer
precision. Geometry uses proportions of the decimal total; very small shares can be visually indistinguishable.

`setTitle`, `setCategoryHeader`, `setValueHeader` and `setTotalLabel` accept String or `IModel<String>` values.
Number formatting matches `SwChart`: use `setValueFormat(SwChart.ValueFormat.COMPACT)` or a serializable formatter,
or `setCurrency(Currency)`. Percentage labels use the component locale, rounded to one decimal place. Long center
totals use smaller text and may ellipsize; the complete formatted total remains available in the table footer.

`setShowLegend(false)` hides the legend; `setShowTooltip(false)` disables enhanced exploration. Without JavaScript,
the ring, total, visible legend, SVG titles and exact table still work. With it, focus the plot and use Up/Down or
Left/Right, Home/End and Escape. Left/Right follows RTL direction; Up/Down follows legend order. Pointer and touch
exploration also work on the legend, so tiny slices remain reachable. Built-in labels follow all nine library languages.
Use `target.add(donut)` for Ajax refresh; data is snapshotted once per request, models detach and replaced listeners
are removed. The chart uses the existing skins and strict CSP, with no charting dependency or inline styles.

The **Charts** showcase (`/charts`) includes vertical and horizontal grouped/stacked bars, client rankings, lines,
stacked areas, a composed currency chart, negative values/gaps, an empty state and live Ajax refresh in both
orientations. Donut examples include client hours, grouped/all regional shares, native empty/zero states and a live
regional breakdown. Sparkline examples compose cards and a team-capacity table, and labeled examples cover latest,
highest/latest, horizontal and dense layouts. Refresh report updates the delivered-hours card and its trend together.
Forecast examples show joined lines/areas, a missing future report and a handoff that moves when a new actual period
arrives. Formatting examples show compact/month axes with exact currency tooltips in both orientations, invoice
reference headers and formats preserved through Ajax report changes. The landing uses the same Cartesian chart
CSS/runtime and native table. Independent axis/tooltip formats apply to `SwChart`; donuts retain their shared value formatter.
Appearance examples include branded natural forecasts, native-pixel bar corners, a quieter daily activity report,
responsive ticks and an Ajax report that changes colors and restores a palette token. This implementation does not
include every Recharts option: gradients, reference lines, brushes, arbitrary tooltip/legend content and further
chart families remain separate work.

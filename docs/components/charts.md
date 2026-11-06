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

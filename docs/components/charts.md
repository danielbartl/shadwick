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

## 100% stacked bars and areas

Use `setStackMode(SwChart.StackMode.PERCENT)` to compare how each category is divided, independently of its total:

```java
add(new SwChart<>("mix", visitsModel, Visit::month)
        .setTitle("Device share by month")
        .setCategoryHeader("Month")
        .setStackMode(SwChart.StackMode.PERCENT)
        .addSeries(ChartSeries.bar("Desktop", Visit::desktop).setStack("devices"))
        .addSeries(ChartSeries.bar("Mobile", Visit::mobile).setStack("devices")));
```

Bind it to `<div wicket:id="mix"></div>`. The same mode works for `ChartSeries.area(...)` and horizontal bars
using `setOrientation(HORIZONTAL)`. Each named stack is normalized independently within each category. Bar and
area stacks are separate, even when their names match. A category with counts 30/70 and one with counts 300/700
have the same visual split. `StackMode.ABSOLUTE` is the default and restores ordinary value-based geometry.

Every series in percent mode must be a bar or area with a nonblank stack name. Lines and unstacked series fail
clearly rather than mixing original units with a proportion scale. Values must be nonnegative; ordinary absolute
stacks still support negative values. Decimal arithmetic computes the proportions before conversion to drawing
coordinates, including positive values too small for a `double` to represent directly.

Null values remain missing and are excluded from the denominator; a remaining positive value can fill the stack.
A zero stays an original zero. When a stack's total is zero, it has no plotted percentage: bars are absent and
areas break at that category. Its original zero and missing cells remain available in the table and tooltip.
This does not connect an area across missing/zero-total reports. Empty models retain the normal empty state.

The numeric domain is fixed at 0–1 and its default labels are localized percentages from 0% to 100%, independently
of the shared value format. `setValueTickCount(...)` still requests an approximate tick count. An explicit
`setAxisValueFormat(...)` takes precedence and receives fractions, such as `0.25` for 25%. Clearing that override
restores the mode's default axis format.

Tooltips, native SVG titles, value labels and the complete data table retain the original formatted amounts;
currency/custom shared formats continue to apply there. Highest-value labels still select by original amount,
rather than largest share. Existing custom tooltip formats, colors, corners, curves and legend options work in
percent mode. Forecast area joins borrow the source's normalized boundary without adding its value again.
Models detach normally, and `target.add(chart)` recomputes each category after Ajax updates. Both orientations and
area geometry render without JavaScript; no new client runtime or chart dependency is needed.

## Reference lines and bands

Add named targets and numeric ranges with `ChartReference`, imported from `dev.jbaby.shadwick.components.chart`:

```java
chart.addReference(ChartReference.line("Quarter target", 350).setColor(3));
chart.addReference(ChartReference.band("Planning range", 150, 250).setColor(2));
```

References follow the numeric axis: horizontal guides on vertical charts, and vertical guides repeated beside
each row of horizontal bars. Bands sit behind the data; lines use a separate overlay above ordinary and rounded
bars, without intercepting pointer/touch events. A visible list below the plot names each reference and its value
or range. It stays readable on narrow screens and in RTL; the plot's accessible description includes it. References
work with the series legend, axes, grid or enhanced tooltips hidden, and need no JavaScript.

By default a reference extends an absolute chart's numeric domain so its target/range is visible. The ordinary
tick rounding then applies. `setExtendDomain(false)` leaves the data scale unchanged and clips geometry to the
final numeric axis: an outside line is omitted and a partly visible band is trimmed. Its named caption remains
available. An empty data model keeps the normal empty state and reference captions, without inventing a plot.

For `StackMode.PERCENT`, references use fractions: `0.5` means 50%, and a band from `0.4` to `0.6` marks 40–60%.
Their values must stay within 0–1, even when domain extension is enabled; the percentage domain remains fixed.
Reference captions use the chart's shared value formatter in absolute mode and localized percentage formatting
in percent mode. Numeric-axis overrides can shorten tick labels without shortening the reference captions.
Reference formatting receives the original `Number`, preserving decimal precision for custom formatters.

Lines are dashed by default; `setDashed(false)` makes one solid. `setColor(0)` uses the neutral default and values
1–5 select chart palette tokens. Bands support `setFillOpacity(0..1)`, default `0.12`. Dashed styling applies to
lines and fill opacity to bands. Values must be finite with magnitude at most `1e100`; a band's lower value must
be strictly less than its upper value. Each defined, visible reference needs a nonblank label. Negative references
are valid on absolute charts, including comparisons with a zero baseline.

For changing goals, bind the values and optionally the label to Wicket models:

```java
IModel<Integer> goal = Model.of(300);
IModel<Integer> lower = Model.of(150);
IModel<Integer> upper = Model.of(250);
chart.addReference(ChartReference.line("Target", goal));
chart.addReference(ChartReference.band("Planning range", lower, upper));
```

Both factories also accept `IModel<String>` as the label. Update the models and call `target.add(chart)` to refresh
geometry, domain and captions together. A null line value, or either missing band bound, omits that reference for
the request. `setVisible(false)` also omits it without reading its label/value models. All reference models still
detach with the chart. Add each reference object once per chart.

References are guides, so they stay separate from the original data table and series tooltips. They do not add
categories or series, alter missing-value gaps, or contribute to percentage totals or forecast joins. Their named
captions provide the configured values using the same formatting rules without changing reported data.

## Scatter plots

`SwScatterChart` positions each point by two numeric coordinates. Each `ScatterSeries<T>` owns its dataset and
serializable name/X/Y extractors, so independent series do not need artificial categories or matching row counts:

```java
add(new SwScatterChart("relationships")
        .setTitle("Effort and revenue")
        .setXLabel("Effort (hours)")
        .setYLabel("Revenue")
        .setPointHeader("Project")
        .setYCurrency(Currency.getInstance("EUR"))
        .addSeries(ScatterSeries.of("Design", designModel,
                Project::name, Project::hours, Project::revenue))
        .addSeries(ScatterSeries.of("Engineering", engineeringModel,
                Project::name, Project::hours, Project::revenue)
                .setShape(ScatterSeries.Shape.SQUARE)));
```

Bind it to `<div wicket:id="relationships"></div>`. Import both classes from
`dev.jbaby.shadwick.components.chart`. Point names and series labels must be nonblank; series labels accept a
String or `IModel<String>`. Register each series object once. Datasets may have different row counts and input
types. Their coordinate units must match the chart's axis labels.

Both axes have numeric domains derived from complete coordinate pairs; row order does not control spacing.
Axes round to readable ticks and pad a single/flat coordinate so it stays drawable. They do not force zero into
the range by default. Use `setIncludeZero(true)` when the origin supplies useful context. Signed coordinates are
supported. Values must be finite with magnitude at most `1e100`; drawing coordinates use double precision.

A null X or Y leaves that measurement unplotted, and its original cells remain in the exact table. Orphan
coordinates do not stretch the domains. Empty datasets show an empty state; datasets with no complete pairs show
a separate message and retain their known cells. Duplicate coordinates and names are not deduplicated. Series
and input order remain intact, with no automatic sorting, interpolation, joining or statistical fit.

`ScatterSeries.Shape.CIRCLE` is the default; `SQUARE` and `DIAMOND` distinguish series without relying only on color.
`setColor(1..5)` selects a chart token; zero uses the next token automatically. `setPointRadius(2..12)` controls the
radius in native pixels, default four. Marker size stays fixed as the graph resizes. Pointer/touch hit regions are
larger than the visible markers. The selected point is raised above overlapping markers while enhanced exploration
is active; keyboard navigation and the table reach points that share a position.

X and Y formats are independent. Use `setXValueFormat(...)` / `setYValueFormat(...)` with `SwChart.ValueFormat`
or serializable `(number, locale) -> text` callbacks; `setXCurrency(...)` / `setYCurrency(...)` select currency
formatting. Formats apply to that axis, its table column, native titles and tooltips. Measurement callbacks receive
the original `Number`, including decimal precision; numeric ticks are computed doubles. Missing coordinates stay
empty without invoking a formatter. Returning null from a formatter produces empty text; passing a null custom
formatter restores the default number format for that coordinate. Strings are escaped, and browser tooltips read
the formatted table cells using `textContent`.

`setXTickCount(2..20)` / `setYTickCount(2..20)` request approximate counts, default five. With script, overlapping
numeric X labels are hidden after resizing, font loading or direction changes; the table retains all coordinates.
Without script, a smaller native subset remains on narrow charts. Long Y labels are visually truncated; exact
formatted values remain available in the table/tooltips. `setShowGrid`, `setShowXAxis`, `setShowYAxis`,
`setShowLegend` and `setShowTooltip` control their respective parts. Axis-label and point-header setters accept
String or `IModel<String>` values.

Enhanced plots have one focus stop. Arrow keys visit complete points in series/input order, Home/End jump to the
first/last point, and Escape dismisses the tooltip. Left/Right follows RTL direction; Up/Down follows input order.
This is sequence navigation, rather than a geometric nearest-point search. RTL mirrors the X geometry and numeric
axis while preserving readable text. Native SVG titles, shape legends and the full four-column data table work
without JavaScript. Built-in table/empty/instruction labels cover all nine library languages.

Update each series' data/label models or marker configuration and call `target.add(chart)` for Ajax refresh.
Snapshotting reads each configured series dataset once and each point extractor once per input row per request.
All chart and series models detach normally. Replacement
removes the old listeners, resize/direction observers and font callbacks. Initial component assembly uses
`onInitialize()`, and scatter interactions have their own small runtime. The first scatter increment does not
add bubbles, connecting/trend lines, brushes or the category chart's reference/stack options.

## Heatmaps

`SwHeatmapChart<T>` compares rows and named numeric columns on one shared, five-band intensity scale:

```java
add(new SwHeatmapChart<>("workload", teamsModel, TeamWeek::name)
        .setTitle("Weekly team workload")
        .setRowHeader("Team")
        .setValueHeader("Hours")
        .setRange(0, 50)
        .addColumn(HeatmapColumn.of("Monday", TeamWeek::monday))
        .addColumn(HeatmapColumn.of("Tuesday", TeamWeek::tuesday))
        .addColumn(HeatmapColumn.of("Wednesday", TeamWeek::wednesday)));
```

Bind it to `<div wicket:id="workload"></div>` and import both classes from
`dev.jbaby.shadwick.components.chart`. Constructors store configuration and models; initial component assembly
happens in `onInitialize()` after `super`. Each `HeatmapColumn` owns a String/model label and serializable value
extractor. Register at least one column; the same column instance cannot be registered twice. Input row order,
column registration order and duplicate labels stay intact. Titles, row headers, value headers and row/column
labels must be nonblank. Title/header setters accept String and `IModel<String>` values.

The matrix is a native table with scoped row and column headers, and SVG tiles behind its formatted values.
The color diagram and its exact data are the same table. Values are visible by default on readable backgrounds;
`setShowValues(false)` hides them visually while preserving their native accessible text, tooltips and original
measurements. Native cell titles provide hover detail without JavaScript. Null cells remain empty values and get
a dashed outline, a cross and localized “Not measured” text for tooltips/accessibility. Zero is an observed value
and never receives the missing mark. Known values with an empty/null custom formatter output remain known.

Measurements may be signed, must be finite and have magnitude at most `1e100`. Automatic bounds include the
observed minimum/maximum and zero. An all-zero or wholly unmeasured matrix uses 0–1. `setRange(lower, upper)` or
`setRange(lowerModel, upperModel)` fixes shared bounds; both must be defined, finite, within the same magnitude
limit and strictly increasing. An explicit range can exclude zero when all observations fit inside it. Any value
outside the configured range fails clearly, rather than being clamped. `setAutoScale()` releases the range models
and restores automatic bounds. Replaced models detach when no longer owned, including when bounds are swapped.

Five equal-width numeric intervals map to increasing opacity of one palette color. Band comparisons use exact
BigDecimal boundaries, including tiny decimals below double range. Intervals include their lower bound and exclude
their upper bound, except the last interval also includes the overall maximum. The legend shows all five numeric
ranges; their printed formatting can round close boundaries. Negative values use the same low-to-high sequential
scale, so a negative-only report's zero is at the high end. No automatic divergence, per-row normalization or
aggregation is applied. Use comparable units across the matrix, and transform domain-specific scores in the app.

`setColor(1..5)` chooses a skin palette token (default one). `setShowLegend(false)` hides the bands and missing
key; `setShowTooltip(false)` keeps native values/titles and scroll behavior while disabling enhanced exploration.
`setValueFormat(NUMBER/INTEGER/COMPACT/PERCENT)`, custom `(number, locale) -> ...` formatting and `setCurrency(...)`
follow the shared chart conventions. Custom callbacks receive original Number measurements and BigDecimal legend
boundaries; they skip null measurements. Choose formatting precise enough for close or tiny observations.

The matrix keeps readable cell sizes and scrolls inside its figure on narrow screens. Row headers stay visible
at the logical start as columns scroll. Long names and values wrap. The scroll region has one native focus stop;
individual cells do not add tab stops. With script, hover/tap any cell or focus the region to explore it with:

| Key | Action |
| --- | --- |
| Left / Right | Adjacent column within the same row, following RTL |
| Up / Down | Adjacent row, preserving the column |
| Home / End | First / last column of the current row |
| Control+Home / Control+End | First / last cell of the matrix |
| Escape | Close the tooltip while keeping focus |

Navigation clamps at the edges and scrolls the active cell into view. Table-backed tooltips and live announcements
identify the row, column, measurement name and formatted value. Without enhancement, native table navigation,
scrolling, exact values and missing markers still work. All nine built-in languages include the component labels
and instructions. Color is supplemented by visible values, range labels, missing symbols and accessible table data.

An empty data model has a native empty state. Entirely unmeasured matrices keep their labels/cells with a separate
message. One row or one column is valid. Update the data, column-label, header or range models and call
`target.add(chart)` to refresh the matrix and legend. Snapshots read data/extractors once per configured slot per
request; owned models detach, and replaced browser listeners are cleaned up. Heatmaps use a small separate runtime
for matrix navigation; the existing Cartesian/radial/radar interaction runtime remains independent.
Calendar/contribution layouts, custom thresholds, diverging palettes, per-row scales, aggregation and editable or
selectable cells remain later scope.

## Radial bar charts

`SwRadialChart<T>` draws concentric bars for independent named measurements on one common zero-based scale:

```java
add(new SwRadialChart<>("goals", goalsModel, Goal::name, Goal::score)
        .setTitle("Delivery goals")
        .setValueHeader("Score")
        .setMaximum(100)
        .setStartAngle(135)
        .setSweepAngle(270)
        .setRounded(true));
```

Bind it to `<div wicket:id="goals"></div>`. Import `SwRadialChart` from
`dev.jbaby.shadwick.components.chart`. The first row is the outermost ring; input order, duplicates, zero and
missing measurements retain their positions. Arc angle encodes value divided by the shared maximum. Each ring
has a different circumference, so compare angles rather than arc area or physical length. Use comparable units;
normalize measurements with different units or per-category goals in the application before plotting them.
There is no aggregate total or part-to-whole percentage calculation.

The default starts at the top and sweeps clockwise through a full circle. `setStartAngle(-360..360)` measures
clockwise degrees from three o'clock, with `-90` at the top. `setSweepAngle(...)` accepts positive angles up to
`360`; use start `180` and sweep `180` for an upper half-circle. Partial sweeps keep the same square plot and
caption. RTL mirrors the complete geometry; the HTML legend and values follow document direction.

Measurements must be finite, nonnegative and at most `1e100`. Null means not measured; zero is observed.
Both keep a hit ring and legend entry when other measured rows exist, even though neither draws a colored bar.
Tooltips and native titles explicitly identify missing measurements, and their table cells stay empty. An all-zero
report shows tracks on an automatic 0–1 scale. An entirely unmeasured report has its own native message and table;
an empty report has an empty state. A single row works as a gauge on the same shared-scale contract.

Automatic scaling rounds the largest measurement up to a readable bound. `setMaximum(Number)` fixes the bound;
`setMaximum(IModel<? extends Number>)` follows an application-owned changing bound. A fixed maximum must be
positive, finite, at most `1e100` and defined. Values above it fail clearly rather than being clamped.
`setAutoScale()` restores the automatic maximum and releases the previous bound model. Decimal ratios are
calculated before conversion to geometry, including values below double range. Very small arcs can be invisible;
the legend, keyboard and original table still reach their measurements. Choose a custom formatter when the
standard number format rounds a tiny measurement to zero.

The plot and caption name its shared zero and maximum. For fractional values such as `0.75`, use
`setMaximum(1).setValueFormat(SwChart.ValueFormat.PERCENT)` to display a percentage scale. Raw scores such as
`75` use `setMaximum(100)` and ordinary numeric formatting. Shared `setValueFormat(NUMBER/INTEGER/COMPACT/PERCENT)`
and custom `(number, locale) -> ...` formatting follow `SwChart`; `setCurrency(Currency)` selects currency.
Formatters receive original Numbers and skip null measurements. Empty custom output does not turn known data
into missing data. Titles and category/value table headers accept String or model values through `setTitle`,
`setCategoryHeader` and `setValueHeader`.

Palette colors cycle through tokens 1–5 in input order. `setColor(row -> row.color())` can keep colors tied to
application category keys through data updates; returned zero/null uses the row's automatic color. Invalid tokens
fail clearly. `setColor(null)` clears the callback. `setInnerRadius(0..0.85)` chooses the inner radius as a fraction
of the outer radius (default `0.35`), and `setGap(0..0.8)` chooses the empty fraction of each radial band (default
`0.2`). Tracks and bars scale with the diagram. `setRounded(true)` adds decorative end caps; they extend beyond
the encoded centerline endpoints and can exaggerate tiny values. Flat ends are the default. Full circles use two
SVG arcs, including when a value exactly reaches its maximum.

Use `setShowTracks(false)`, `setShowLegend(false)` or `setShowTooltip(false)` to hide those presentation parts.
The exact table remains available. Without enhancement, native SVG titles, tracks/bars, the legend and the table
still work. With script, hover/tap a ring or its legend entry, or focus the single plot stop and use arrow keys,
Home/End and Escape. Up/Down follows outer-to-inner order; Left/Right follows RTL. Full hit rings keep zero,
missing and tiny measurements reachable. Legend entries wrap long names and values without adding focus stops.
Built-in labels cover all nine library languages. The existing shared chart runtime supplies table-backed
tooltips, color indicators, live announcements and Ajax/cloning cleanup.

Update the data/header/maximum models and call `target.add(chart)` to replace the report, geometry and legend
together. Data and callbacks are read once per configured slot per request, and owned models detach. Constructors
store parameters/configuration; initial component assembly happens in `onInitialize()` after `super`. Use a small
set of categories so rings remain distinguishable. Stacked/grouped radial series, signed ranges, independent
per-ring bounds, center content and animated gauges remain later scope.

## Radar charts

`SwRadarChart<T>` compares named series across a common ordered list of dimensions:

```java
add(new SwRadarChart<>("profile", capabilitiesModel, Capability::name)
        .setTitle("Team capability profile")
        .setMaximum(100)
        .addSeries(RadarSeries.of("Current", Capability::current))
        .addSeries(RadarSeries.of("Target", Capability::target)
                .setDashed(true).setFillOpacity(0).setDots(false)));
```

Bind it to `<div wicket:id="profile"></div>` and import both classes from
`dev.jbaby.shadwick.components.chart`. Every spoke uses the same zero-based numeric scale. Use comparable units
or normalize unlike measures in the application before plotting them. The input order is the spoke order;
dimensions and series are not sorted or deduplicated. Titles, dimension names and series labels must be nonblank.
Series labels accept String or `IModel<String>` values, and extractors are serializable.

Spokes start at the top and proceed clockwise; numbered native-size labels match the full dimension names in the
HTML key below the plot. Long names wrap there rather than becoming clipped or tiny SVG text. RTL mirrors the
geometry and numbers together, while the text remains readable. The dimension key also supports enhanced hover
and touch exploration. Use a small set of comparable dimensions for a readable profile.

Measurements must be finite, nonnegative and at most `1e100`. A null is missing, while zero is an observed value
at the center. A complete series draws a closed polygon. An incomplete one has no fill and draws only edges
between adjacent measured dimensions, including the last/first pair; it never bridges a missing dimension or
substitutes an invented zero. Dots keep known isolated values visible. All original values and empty cells remain
in the exact table; tooltips/native titles explicitly identify missing measurements.

At least three dimensions are needed for a radar plot. Fewer dimensions retain their table with an explanatory
message. An empty model and a model with no measurements have distinct native states. An all-zero model draws
the measured center values on an automatic 0–1 scale, rather than becoming missing data.

The default automatic maximum rounds the largest measurement up to a readable bound. `setMaximum(Number)` fixes
the scale; `setMaximum(IModel<? extends Number>)` supports changing bounds. A configured maximum must be positive,
finite and at most `1e100`, with no missing model value. Values exceeding it fail rather than being silently
clamped. `setAutoScale()` restores automatic scaling. Replaced/cleared maximum models detach when released.
Decimal ratios are calculated before conversion to geometry, including positive values below double range.

`setLevels(2..10)` selects equally spaced polygon rings, default four. Radius is linear in value; polygon area is
not a separate measurement. The scale caption gives the formatted zero and maximum, while the table and tooltip
give exact formatted series values. `setShowGrid(false)` hides rings/spokes; dimension numbers and the key remain.
`setShowLegend(false)` hides the series legend, and `setShowTooltip(false)` disables enhanced exploration.

Each `RadarSeries` supports palette tokens through `setColor(1..5)` (zero selects the next automatically),
`setDashed`, `setDots`, `setFillOpacity(0..1)` and `setStrokeWidth(0..20)` in native pixels. Defaults are solid
two-pixel outlines, dots and `0.15` fill opacity. Dots remain three pixels in radius as the chart resizes. Filled
areas stay behind the outlines; transparent dimension sectors provide the hit regions.

Shared number/currency/custom formatting follows `setValueFormat(...)` and `setCurrency(...)`, with the same
locale-aware choices as `SwChart`. Formatters receive original `Number` measurements, preserving decimal
precision; missing measurements remain empty without invoking the formatter. A custom formatter's empty/null
output does not turn a known value into missing data. Chart titles and table dimension headers accept String or
`IModel<String>` values through `setTitle` and `setCategoryHeader`.

With script, focus the plot and use Left/Right, Home/End and Escape to explore dimensions in input order; Left/Right
follows RTL. Hover/tap the diagram or dimension key to inspect every series at that dimension. The shared chart
runtime supplies one plot focus stop, table-backed tooltips and live announcements. Without script, the diagram,
numbered key, native SVG titles, legend and exact table still work. Built-in labels cover all nine library languages.

Update the data, series-label or maximum models and call `target.add(chart)` to refresh geometry, dimension key,
scale and data together. Snapshots read data/extractors once per configured slot per request, all owned models
detach, and replaced listeners are removed. Component assembly happens in `onInitialize()` after `super`.
Per-dimension maxima, signed radial scales, stacked radar series and statistical scoring remain application-owned
or later scope; the initial component uses one shared comparable scale.

## Formatting and interaction

`setValueFormat(NUMBER/INTEGER/COMPACT/PERCENT)` uses the session locale. `setCurrency(Currency)` selects currency
formatting. For custom output use `setValueFormat((number, locale) -> ...)` or `setCategoryFormat((value, locale)
 -> ...)`. These shared formats supply the data table, value labels and, by default, axes and tooltips.
Percent mode uses its own default percentage format for the numeric axis.

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

## Pie charts

For a filled pie, use the same component with `setShape(SwDonutChart.Shape.PIE)`:

```java
add(new SwDonutChart<>("shares", regionsModel, Region::name, Region::revenue)
        .setShape(SwDonutChart.Shape.PIE)
        .setTitle("Revenue shares")
        .setCategoryHeader("Region")
        .setValueHeader("Revenue")
        .setCurrency(Currency.getInstance("EUR")));
```

Bind it to `<div wicket:id="shares"></div>`. `Shape.DONUT` remains the default. Pie slices are native SVG
sectors with the same grouping, formatting, localized legend, keyboard/pointer/touch exploration and exact table.
A single positive part fills the disk. Zero/missing values create no slices, and a zero-total pie shows a muted
disk and the no-positive-values message. Very small shares can round away visually; each part remains reachable
through its legend entry and present in the exact table. Pies have no center label; the table footer retains the
total and its configured label. Both shapes work without JavaScript and support `target.add(chart)` refreshes.

The **Charts** showcase (`/charts`) includes vertical and horizontal grouped/stacked bars, client rankings, lines,
stacked areas, a composed currency chart, negative values/gaps, an empty state and live Ajax refresh in both
orientations. Percent examples compare a device mix with vertical/horizontal bars and areas, retain zero-total
gaps and missing reports, and update all three views through Ajax. Donut examples include client hours, grouped/all regional shares, native empty/zero states and a live
regional breakdown. Pie examples show grouped/all revenue shares, a single-region disk, a zero-total state and live
Ajax updates. Sparkline examples compose cards and a team-capacity table, and labeled examples cover latest,
highest/latest, horizontal and dense layouts. Refresh report updates the delivered-hours card and its trend together.
Forecast examples show joined lines/areas, a missing future report and a handoff that moves when a new actual period
arrives. Formatting examples show compact/month axes with exact currency tooltips in both orientations, invoice
reference headers and formats preserved through Ajax report changes. The landing uses the same Cartesian chart
CSS/runtime and native table. Independent axis/tooltip formats apply to `SwChart`; donuts retain their shared value formatter.
Appearance examples include branded natural forecasts, native-pixel bar corners, a quieter daily activity report,
responsive ticks and an Ajax report that changes colors and restores a palette token.
Reference examples show targets/ranges on revenue, horizontal delivery and percentage charts; **Refresh targets**
updates a separate pair of revenue reports and their modeled bounds together.
Scatter examples compare independent design/engineering datasets, signed changes and overlapping points, with
quiet, single-point, missing-pair and empty states. **Refresh measurements** updates a separate dataset and its markers.
Radar examples compare fixed-scale team profiles and automatic workload scales, show missing assessments, long
dimension names, measured zeros and native boundary states. **Refresh assessment** changes the live profile, its
series label and modeled maximum together.
Radial examples show full/three-quarter/quiet half-circle comparisons, automatic support-hour scales, a single
launch gauge, zero/missing/tiny values and native empty states. **Refresh radial report** changes the categories,
colors, header, maximum and sweep together.
Heatmap examples show weekly workloads, hidden values, signed plan changes, long labels, tiny/zero/missing
measurements and a single report. **Refresh workload matrix** updates team rows, a day label, measurement name,
range and palette together.
This implementation does not include every Recharts option: gradients, brushes, arbitrary tooltip/legend content and further
chart families remain separate work.

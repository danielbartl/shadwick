package dev.jbaby.shadwick.components.chart;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;
import org.apache.wicket.*;
import org.apache.wicket.markup.head.*;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.string.Strings;

/** Responsive, server-rendered SVG bars/lines/areas with a native data disclosure and optional keyboard tooltips. */
public class SwChart<T> extends Panel {
    public static final JavaScriptResourceReference CHART_JS =
            new JavaScriptResourceReference(Shadwick.class, "js/chart.js");
    public enum ValueFormat { NUMBER, INTEGER, COMPACT, PERCENT, CURRENCY }
    public enum CategoryFormat { DAY, MONTH_SHORT, MONTH, MONTH_YEAR, DATE, TIME }
    public enum Orientation { VERTICAL, HORIZONTAL }
    public enum StackMode { ABSOLUTE, PERCENT }
    public enum Indicator { AUTO, DOT, LINE, DASHED }
    public enum LegendPosition { TOP, BOTTOM }
    public enum LegendAlignment { START, CENTER, END }
    private final IModel<? extends List<T>> data;
    private final SerializableFunction<T, ?> category;
    private final List<ChartSeries<T>> series = new ArrayList<>();
    private final List<ChartReference> references = new ArrayList<>();
    private IModel<String> title = Model.of("Chart"), categoryHeader = Model.of("Category");
    private SerializableBiFunction<Number, Locale, String> valueFormat;
    private SerializableBiFunction<Object, Locale, String> categoryFormat;
    private CategoryFormat namedCategoryFormat;
    private ValueFormatting axisValueFormat, tooltipValueFormat;
    private CategoryFormatting axisCategoryFormat, tooltipLabelFormat;
    private SerializableFunction<T, ?> tooltipLabel;
    private ValueFormat namedFormat = ValueFormat.NUMBER;
    private Currency currency;
    private ZoneId timeZone = ZoneOffset.UTC;
    private boolean grid = true, xAxis = true, yAxis = true, legend = true, tooltip = true;
    private boolean tooltipLabelVisible = true, tooltipIndicatorVisible = true, tooltipCursor = true;
    private Indicator tooltipIndicator = Indicator.AUTO;
    private LegendPosition legendPosition = LegendPosition.BOTTOM;
    private LegendAlignment legendAlignment = LegendAlignment.CENTER;
    private int valueTickCount = 5, maxCategoryTicks = 8, minCategoryTickGap = 5;
    private Orientation orientation = Orientation.VERTICAL;
    private StackMode stackMode = StackMode.ABSOLUTE;
    private transient Snapshot snapshot;
    private record Row(String category, String axisCategory, String tooltipLabel, List<Double> values, List<Double> plotValues,
                       List<String> text, List<String> tooltipText) implements Serializable {}
    private record ValueFormatting(ValueFormat named, Currency currency,
                                   SerializableBiFunction<Number, Locale, String> custom) implements Serializable {}
    private record CategoryFormatting(CategoryFormat named,
                                      SerializableBiFunction<Object, Locale, String> custom) implements Serializable {}
    private record Reference(String label, double lower, double upper, String text, boolean band, int color,
                             boolean dashed, double opacity, boolean extendDomain) implements Serializable {}
    private record Snapshot(List<Row> rows, List<String> labels, double[][] low, double[][] high, double[] ticks, int[] joins,
                            List<Reference> references)
            implements Serializable {}
    private record LabelSelection(Set<Integer> rows, int last, int highest) {}
    private record ValueLabel(int series, int row, String text, double x, double y, int side, int priority) {}
    private record NativeBar(int series, int row, double x, double y, double width, double height) {}
    public SwChart(String id, IModel<? extends List<T>> data, SerializableFunction<T, ?> category) {
        super(id);
        this.data = Objects.requireNonNull(data);
        this.category = Objects.requireNonNull(category);
        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(figure());
    }

    private WebMarkupContainer figure() {
        var figure = new WebMarkupContainer("figure");
        figure.add(new StyledBehavior("sw-chart"));
        figure.setOutputMarkupId(true);
        figure.add(new Label("content", (IModel<String>) () -> renderChart(figure.getMarkupId()))
                        .setEscapeModelStrings(false));
        return figure;
    }

    public SwChart<T> addSeries(ChartSeries<T> value) {
        series.add(Objects.requireNonNull(value));
        return this;
    }

    /** Add a numeric target or range. Its models detach with this chart. */
    public SwChart<T> addReference(ChartReference value) {
        Objects.requireNonNull(value);
        if (references.contains(value)) throw new IllegalArgumentException("A chart reference must be registered exactly once");
        references.add(value);
        return this;
    }

    /** Horizontal charts draw bar series, with categories down the page and values across it. */
    public SwChart<T> setOrientation(Orientation value) {
        orientation = Objects.requireNonNull(value);
        return this;
    }

    /** Normalize each named bar/area stack per category. Percent stacks require nonnegative values. */
    public SwChart<T> setStackMode(StackMode value) {
        stackMode = Objects.requireNonNull(value);
        return this;
    }

    public SwChart<T> setTitle(String value) {
        return setTitle(Model.of(value));
    }

    public SwChart<T> setTitle(IModel<String> value) {
        title = Objects.requireNonNull(value);
        return this;
    }

    public SwChart<T> setCategoryHeader(String value) {
        return setCategoryHeader(Model.of(value));
    }

    public SwChart<T> setCategoryHeader(IModel<String> value) {
        categoryHeader = Objects.requireNonNull(value);
        return this;
    }

    public SwChart<T> setValueFormat(SerializableBiFunction<Number, Locale, String> value) {
        valueFormat = value;
        return this;
    }

    public SwChart<T> setValueFormat(ValueFormat value) {
        namedFormat = Objects.requireNonNull(value);
        valueFormat = null;
        return this;
    }

    public SwChart<T> setCurrency(Currency value) {
        currency = Objects.requireNonNull(value);
        namedFormat = ValueFormat.CURRENCY;
        valueFormat = null;
        return this;
    }

    public SwChart<T> setCategoryFormat(SerializableBiFunction<Object, Locale, String> value) {
        categoryFormat = value;
        namedCategoryFormat = null;
        return this;
    }

    /** A named date/time format for categories in the table and, by default, axes and tooltips. */
    public SwChart<T> setCategoryFormat(CategoryFormat value) {
        namedCategoryFormat = Objects.requireNonNull(value);
        categoryFormat = null;
        return this;
    }

    /** Format numeric ticks independently of values in the tooltip, data table and value labels. */
    public SwChart<T> setAxisValueFormat(ValueFormat value) {
        axisValueFormat = new ValueFormatting(Objects.requireNonNull(value), null, null);
        return this;
    }

    /** Null restores the chart's shared value format. */
    public SwChart<T> setAxisValueFormat(SerializableBiFunction<Number, Locale, String> value) {
        axisValueFormat = value == null ? null : new ValueFormatting(null, null, value);
        return this;
    }

    /** Format numeric ticks as currency without changing the shared or tooltip format. */
    public SwChart<T> setAxisCurrency(Currency value) {
        axisValueFormat = new ValueFormatting(ValueFormat.CURRENCY, Objects.requireNonNull(value), null);
        return this;
    }

    /** Format category ticks independently of table categories and tooltip labels, in either orientation. */
    public SwChart<T> setAxisCategoryFormat(CategoryFormat value) {
        axisCategoryFormat = new CategoryFormatting(Objects.requireNonNull(value), null);
        return this;
    }

    /** Null restores the chart's shared category format. */
    public SwChart<T> setAxisCategoryFormat(SerializableBiFunction<Object, Locale, String> value) {
        axisCategoryFormat = value == null ? null : new CategoryFormatting(null, value);
        return this;
    }

    /** Format tooltip values independently of numeric ticks, the data table and value labels. */
    public SwChart<T> setTooltipValueFormat(ValueFormat value) {
        tooltipValueFormat = new ValueFormatting(Objects.requireNonNull(value), null, null);
        return this;
    }

    /** Null restores the chart's shared value format. */
    public SwChart<T> setTooltipValueFormat(SerializableBiFunction<Number, Locale, String> value) {
        tooltipValueFormat = value == null ? null : new ValueFormatting(null, null, value);
        return this;
    }

    /** Format tooltip values as currency without changing the shared or axis format. */
    public SwChart<T> setTooltipCurrency(Currency value) {
        tooltipValueFormat = new ValueFormatting(ValueFormat.CURRENCY, Objects.requireNonNull(value), null);
        return this;
    }

    /** Format tooltip labels independently of category ticks and the data table. */
    public SwChart<T> setTooltipLabelFormat(CategoryFormat value) {
        tooltipLabelFormat = new CategoryFormatting(Objects.requireNonNull(value), null);
        return this;
    }

    /** Null restores the chart's shared category format. */
    public SwChart<T> setTooltipLabelFormat(SerializableBiFunction<Object, Locale, String> value) {
        tooltipLabelFormat = value == null ? null : new CategoryFormatting(null, value);
        return this;
    }

    /** An alternate row property for tooltip labels. Null restores the constructor's category extractor. */
    public SwChart<T> setTooltipLabel(SerializableFunction<T, ?> value) {
        tooltipLabel = value;
        return this;
    }

    public SwChart<T> setTimeZone(ZoneId value) {
        timeZone = Objects.requireNonNull(value);
        return this;
    }

    public SwChart<T> setShowGrid(boolean value) {
        grid = value;
        return this;
    }

    public SwChart<T> setShowXAxis(boolean value) {
        xAxis = value;
        return this;
    }

    public SwChart<T> setShowYAxis(boolean value) {
        yAxis = value;
        return this;
    }

    public SwChart<T> setShowLegend(boolean value) {
        legend = value;
        return this;
    }

    public SwChart<T> setShowTooltip(boolean value) {
        tooltip = value;
        return this;
    }

    /** Approximate number of numeric-axis ticks, from 2 to 20. Nice domain rounding can add a tick. */
    public SwChart<T> setValueTickCount(int value) {
        if (value < 2 || value > 20) throw new IllegalArgumentException("Value tick count must be between 2 and 20");
        valueTickCount = value; return this;
    }
    /** Maximum visible category ticks, from 1 to 1000. Horizontal row labels remain complete. */
    public SwChart<T> setMaxCategoryTicks(int value) {
        if (value < 1 || value > 1000) throw new IllegalArgumentException("Maximum category ticks must be between 1 and 1000");
        maxCategoryTicks = value; return this;
    }
    /** Minimum pixel gap between enhanced category labels, from 0 to 100. */
    public SwChart<T> setMinCategoryTickGap(int value) {
        if (value < 0 || value > 100) throw new IllegalArgumentException("Category tick gap must be between 0 and 100");
        minCategoryTickGap = value; return this;
    }
    public SwChart<T> setTooltipIndicator(Indicator value) {
        tooltipIndicator = Objects.requireNonNull(value); return this;
    }
    public SwChart<T> setShowTooltipLabel(boolean value) { tooltipLabelVisible = value; return this; }
    public SwChart<T> setShowTooltipIndicator(boolean value) { tooltipIndicatorVisible = value; return this; }
    public SwChart<T> setShowTooltipCursor(boolean value) { tooltipCursor = value; return this; }
    public SwChart<T> setLegendPosition(LegendPosition value) {
        legendPosition = Objects.requireNonNull(value); return this;
    }
    public SwChart<T> setLegendAlignment(LegendAlignment value) {
        legendAlignment = Objects.requireNonNull(value); return this;
    }

    private String category(Object value) {
        return category(value, namedCategoryFormat, categoryFormat);
    }

    private String category(Object value, CategoryFormat named, SerializableBiFunction<Object, Locale, String> custom) {
        if (custom != null)
            return Objects.toString(custom.apply(value, getLocale()), "");
        if (named != null) {
            TemporalAccessor date = value instanceof Instant instant ? instant.atZone(timeZone)
                    : value instanceof OffsetDateTime offset ? offset.atZoneSameInstant(timeZone)
                    : value instanceof ZonedDateTime zoned ? zoned.withZoneSameInstant(timeZone)
                    : value instanceof TemporalAccessor temporal ? temporal : null;
            boolean compatible = date != null && switch (named) {
                case TIME -> date.isSupported(ChronoField.HOUR_OF_DAY) && date.isSupported(ChronoField.MINUTE_OF_HOUR);
                case MONTH_SHORT, MONTH -> date.isSupported(ChronoField.MONTH_OF_YEAR);
                case MONTH_YEAR -> date.isSupported(ChronoField.MONTH_OF_YEAR) && date.isSupported(ChronoField.YEAR);
                case DAY -> date.isSupported(ChronoField.MONTH_OF_YEAR) && date.isSupported(ChronoField.DAY_OF_MONTH);
                case DATE -> date.isSupported(ChronoField.EPOCH_DAY);
            };
            if (compatible) {
                DateTimeFormatter formatter = switch (named) {
                    case DAY -> DateTimeFormatter.ofPattern("MMM d", getLocale());
                    case MONTH_SHORT -> DateTimeFormatter.ofPattern("MMM", getLocale());
                    case MONTH -> DateTimeFormatter.ofPattern("LLLL", getLocale());
                    case MONTH_YEAR -> DateTimeFormatter.ofPattern("MMM uuuu", getLocale());
                    case DATE -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(getLocale());
                    case TIME -> DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(getLocale());
                };
                return formatter.format(date);
            }
        }
        if (value instanceof YearMonth month)
            return month.format(DateTimeFormatter.ofPattern("MMM uuuu", getLocale()));
        if (value instanceof LocalDate date)
            return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(getLocale()));
        if (value instanceof LocalDateTime date)
            return date.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(getLocale()));
        if (value instanceof Instant instant)
            return dateTime(instant);
        if (value instanceof OffsetDateTime date)
            return dateTime(date.toInstant());
        if (value instanceof ZonedDateTime date)
            return dateTime(date.toInstant());
        return Objects.toString(value, "");
    }

    private String dateTime(Instant instant) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(getLocale())
                .withZone(timeZone)
                .format(instant);
    }

    private String format(Number value) {
        return ChartValues.format(value, getLocale(), namedFormat, currency, valueFormat);
    }

    private String format(Number value, ValueFormatting override) {
        return ChartValues.format(value, getLocale(), override.named == null ? namedFormat : override.named,
                override.currency == null ? currency : override.currency, override.custom);
    }

    private String axisValue(double value) {
        if (axisValueFormat != null) return format(value, axisValueFormat);
        if (stackMode == StackMode.PERCENT)
            return ChartValues.format(value, getLocale(), ValueFormat.PERCENT, null, null);
        return format(value);
    }

    private Snapshot snapshot() {
        if (snapshot != null)
            return snapshot;
        if (title.getObject() == null || title.getObject().isBlank())
            throw new IllegalStateException("A chart needs a title for its accessible name");
        if (series.isEmpty())
            throw new IllegalStateException("A chart needs at least one series");
        if (orientation == Orientation.HORIZONTAL && series.stream().anyMatch(s -> s.type != ChartSeries.Type.BAR))
            throw new IllegalStateException("Horizontal charts support bar series only");
        validateStackMode();
        var sources = validateJoins();
        var labels = series.stream().map(s -> Objects.toString(s.label.getObject(), "")).toList();
        if (labels.stream().anyMatch(String::isBlank))
            throw new IllegalStateException("Every chart series needs a label");
        List<Reference> guides = referenceSnapshot();
        var rows = new ArrayList<Row>();
        List<T> items = data.getObject();
        if (items != null)
            for (T item : items) {
                var values = new ArrayList<Double>();
                var weights = new ArrayList<BigDecimal>();
                var text = new ArrayList<String>();
                var tooltipText = new ArrayList<String>();
                for (var s : series) {
                    Number value = s.value.apply(item);
                    double d = value == null ? 0 : value.doubleValue();
                    if (!Double.isFinite(d) || Math.abs(d) > 1e100)
                        throw new IllegalArgumentException("Chart values must be finite, with magnitude at most 1e100");
                    values.add(value == null ? null : d);
                    if (stackMode == StackMode.PERCENT) weights.add(normalizationWeight(value));
                    String formatted = format(value);
                    text.add(formatted);
                    tooltipText.add(tooltipValueFormat == null ? formatted : format(value, tooltipValueFormat));
                }
                Object rawCategory = category.apply(item);
                String formattedCategory = category(rawCategory);
                String axisCategory = axisCategoryFormat == null ? formattedCategory
                        : category(rawCategory, axisCategoryFormat.named, axisCategoryFormat.custom);
                Object rawLabel = tooltipLabel == null ? rawCategory : tooltipLabel.apply(item);
                String label = tooltipLabelFormat != null
                        ? category(rawLabel, tooltipLabelFormat.named, tooltipLabelFormat.custom)
                        : tooltipLabel == null ? formattedCategory : category(rawLabel);
                List<Double> original = Collections.unmodifiableList(values);
                List<Double> plot = stackMode == StackMode.PERCENT ? normalizedValues(weights) : original;
                rows.add(new Row(formattedCategory, axisCategory, label, original, plot,
                        List.copyOf(text), List.copyOf(tooltipText)));
            }
        double[][] low = new double[series.size()][rows.size()], high = new double[series.size()][rows.size()];
        double min = 0, max = 0;
        for (int r = 0; r < rows.size(); r++) {
            Map<String, double[]> stacks = new HashMap<>();
            for (int s = 0; s < series.size(); s++) {
                Double v = rows.get(r).plotValues.get(s);
                if (v == null)
                    continue;
                ChartSeries<T> part = series.get(s);
                double base = 0;
                if (part.stack != null && part.type != ChartSeries.Type.LINE) {
                    double[] sum = stacks.computeIfAbsent(stackKey(part), k -> new double[2]);
                    int sign = v < 0 ? 0 : 1;
                    base = sum[sign];
                    sum[sign] += v;
                }
                low[s][r] = base;
                high[s][r] = base + v;
                min = Math.min(min, Math.min(base, base + v));
                max = Math.max(max, Math.max(base, base + v));
            }
        }
        int[] joins = new int[series.size()];
        Arrays.fill(joins, -1);
        for (int s = 0; s < series.size(); s++) {
            ChartSeries<T> source = series.get(s).join;
            if (source == null) continue;
            int first = 0;
            while (first < rows.size() && rows.get(first).plotValues.get(s) == null) first++;
            if (first == rows.size()) continue;
            int from = sources.get(source);
            for (int r = first - 1; r >= 0; r--) {
                if (rows.get(r).plotValues.get(from) == null) continue;
                joins[s] = r;
                // Borrow the plotted coordinates, not another value in the stack or the original data.
                low[s][r] = low[from][r]; high[s][r] = high[from][r];
                break;
            }
        }
        if (stackMode == StackMode.ABSOLUTE) {
            for (Reference guide : guides) {
                if (!guide.extendDomain) continue;
                min = Math.min(min, guide.lower);
                max = Math.max(max, guide.upper);
            }
        }
        double[] axis = stackMode == StackMode.PERCENT ? ticks(0, 1, valueTickCount) : ticks(min, max, valueTickCount);
        return snapshot = new Snapshot(List.copyOf(rows), labels, low, high, axis, joins, guides);
    }

    private List<Reference> referenceSnapshot() {
        List<Reference> result = new ArrayList<>();
        for (ChartReference reference : references) {
            if (!reference.visible) continue;
            Number lower = reference.lower.getObject();
            Number upper = reference.upper == null ? lower : reference.upper.getObject();
            if (lower == null || upper == null) continue;
            BigDecimal from = referenceAmount(lower), to = referenceAmount(upper);
            if (reference.upper != null && from.compareTo(to) >= 0)
                throw new IllegalArgumentException("A reference band's lower value must be less than its upper value");
            String label = reference.label.getObject();
            if (label == null || label.isBlank()) throw new IllegalStateException("A chart reference needs a label");
            String text = referenceValue(lower);
            if (reference.upper != null) text += " – " + referenceValue(upper);
            result.add(new Reference(label, from.doubleValue(), to.doubleValue(), text, reference.upper != null,
                    reference.color, reference.dashed, reference.opacity, reference.extendDomain));
        }
        return List.copyOf(result);
    }

    private BigDecimal referenceAmount(Number value) {
        double number = value.doubleValue();
        if (!Double.isFinite(number) || Math.abs(number) > 1e100)
            throw new IllegalArgumentException("Reference values must be finite, with magnitude at most 1e100");
        BigDecimal amount = decimal(value);
        if (amount.abs().compareTo(BigDecimal.ONE.scaleByPowerOfTen(100)) > 0)
            throw new IllegalArgumentException("Reference values must be finite, with magnitude at most 1e100");
        if (stackMode == StackMode.PERCENT && (amount.signum() < 0 || amount.compareTo(BigDecimal.ONE) > 0))
            throw new IllegalArgumentException("Percent chart references must be between 0 and 1");
        return amount;
    }

    private String referenceValue(Number value) {
        return stackMode == StackMode.PERCENT
                ? ChartValues.format(value, getLocale(), ValueFormat.PERCENT, null, null) : format(value);
    }

    private void validateStackMode() {
        if (stackMode != StackMode.PERCENT) return;
        for (ChartSeries<T> part : series) {
            if (part.type == ChartSeries.Type.LINE || part.stack == null || part.stack.isBlank())
                throw new IllegalStateException("100% stacks require bar/area series with a nonblank stack name");
        }
    }

    private static BigDecimal normalizationWeight(Number value) {
        if (value == null) return null;
        BigDecimal weight = decimal(value);
        if (weight.signum() < 0)
            throw new IllegalArgumentException("100% stacks require nonnegative values");
        return weight;
    }

    private static BigDecimal decimal(Number value) {
        try { return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString()); }
        catch (NumberFormatException ignored) { return BigDecimal.valueOf(value.doubleValue()); }
    }

    private static String stackKey(ChartSeries<?> part) {
        return part.type + ":" + part.stack;
    }

    private List<Double> normalizedValues(List<BigDecimal> weights) {
        Map<String, BigDecimal> totals = new HashMap<>();
        for (int s = 0; s < series.size(); s++) {
            BigDecimal weight = weights.get(s);
            if (weight != null) totals.merge(stackKey(series.get(s)), weight, BigDecimal::add);
        }
        List<Double> result = new ArrayList<>();
        for (int s = 0; s < series.size(); s++) {
            BigDecimal weight = weights.get(s), total = totals.get(stackKey(series.get(s)));
            result.add(weight == null || total.signum() == 0 ? null
                    : weight.divide(total, MathContext.DECIMAL128).doubleValue());
        }
        return Collections.unmodifiableList(result);
    }
    private IdentityHashMap<ChartSeries<T>, Integer> validateJoins() {
        var indexes = new IdentityHashMap<ChartSeries<T>, Integer>();
        boolean joined = series.stream().anyMatch(s -> s.join != null);
        for (int s = 0; s < series.size(); s++) {
            if (indexes.put(series.get(s), s) != null && joined)
                throw new IllegalStateException("Joined chart series must be registered exactly once");
        }
        for (ChartSeries<T> part : series) {
            if (part.join == null) continue;
            if (part.type == ChartSeries.Type.BAR || !part.dashed)
                throw new IllegalStateException("A forecast join requires a dashed line or area");
            if (!indexes.containsKey(part.join)) throw new IllegalStateException("The join source must belong to the same chart");
            if (part.join.type == ChartSeries.Type.BAR) throw new IllegalStateException("The join source must be a line or area");
            boolean stacked = part.type == ChartSeries.Type.AREA && part.stack != null
                    || part.join.type == ChartSeries.Type.AREA && part.join.stack != null;
            if (stacked && (part.type != ChartSeries.Type.AREA || part.join.type != ChartSeries.Type.AREA
                    || !Objects.equals(part.stack, part.join.stack)))
                throw new IllegalStateException("Joined stacked areas must share the same stack");
            Set<ChartSeries<T>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            for (ChartSeries<T> current = part; current != null; current = current.join)
                if (!visited.add(current)) throw new IllegalStateException("Forecast joins must not contain a cycle");
        }
        return indexes;
    }
    private static boolean plotted(Snapshot snap, int series, int row) {
        return snap.rows.get(row).plotValues.get(series) != null || snap.joins[series] == row;
    }
    static double[] ticks(double min, double max) {
        return ticks(min, max, 5);
    }
    static double[] ticks(double min, double max, int target) {
        if (min == max)
            max = min + 1;
        double raw = Math.max(Double.MIN_VALUE, (max - min) / (target - 1));
        double magnitude = Math.max(Double.MIN_VALUE, Math.pow(10, Math.floor(Math.log10(raw))));
        double step = magnitude;
        for (double factor : new double[] {1, 2, 2.5, 5, 10}) {
            step = factor * magnitude;
            if (step >= raw)
                break;
        }
        double start = Math.floor(min / step) * step;
        int count = (int) Math.ceil((max - start) / step - 1e-9);
        double[] result = new double[count + 1];
        for (int i = 0; i <= count; i++) result[i] = start + i * step;
        return result;
    }

    private static String escape(String value) {
        return Strings.escapeMarkup(Objects.toString(value, "")).toString();
    }

    private static String num(double value) {
        if (!Double.isFinite(value))
            throw new IllegalArgumentException("Non-finite chart coordinate");
        return String.format(Locale.ROOT, "%.5f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static double y(double value, double[] ticks) {
        return 240 - (value - ticks[0]) / (ticks[ticks.length - 1] - ticks[0]) * 240;
    }

    private String color(int s) {
        String custom = ChartAppearance.className(series.get(s));
        return "sw-chart-series-" + (series.get(s).color == 0 ? s % 5 + 1 : series.get(s).color)
                + (custom.isEmpty() ? "" : " " + custom);
    }

    private String renderChart(String id) {
        Snapshot snap = snapshot();
        int count = snap.rows.size();
        String name = escape(title.getObject());
        StringBuilder b = new StringBuilder();
        b.append("<figcaption class=\"sw-chart-title\" id=\"")
                .append(id)
                .append("-title\">")
                .append(name)
                .append("</figcaption>");
        if (count == 0) {
            b.append("<p class=\"sw-chart-empty\" role=\"status\">")
                    .append(escape(getString("SwChart.empty", null, "No data to display.")))
                    .append("</p>");
            renderReferenceCaptions(b, id, snap);
            return b.toString();
        }
        b.append("<p class=\"sw-chart-sr-only\" id=\"")
                .append(id)
                .append("-help\">")
                .append(escape(tooltip ? getString(orientation == Orientation.HORIZONTAL
                                                         ? "SwChart.horizontalInstructions" : "SwChart.instructions", null,
                                                 "Use the arrow keys to explore values, Home and End to jump, "
                                                 + "Escape to dismiss. Exact values are available in the data table.")
                                       : getString("SwChart.dataInstructions", null,
                                                 "Exact values are available in the data table.")))
                .append("</p>");
        if (legend && legendPosition == LegendPosition.TOP) renderLegend(b, snap);
        if (orientation == Orientation.HORIZONTAL)
            renderHorizontalPlot(b, id, snap);
        else
            renderVerticalPlot(b, id, snap, name);
        renderReferenceCaptions(b, id, snap);
        if (legend && legendPosition == LegendPosition.BOTTOM) renderLegend(b, snap);
        b.append("<details class=\"sw-chart-data\"><summary>")
                .append(escape(getString("SwChart.data", null, "View data")))
                .append("</summary><div class=\"sw-chart-table-scroll\" tabindex=\"0\" role=\"region\" aria-labelledby=\"")
                .append(id).append("-title\"><table class=\"sw-table\"><caption "
                        + "class=\"sw-chart-sr-only\">")
                .append(name)
                .append("</caption><thead><tr><th scope=\"col\">")
                .append(escape(categoryHeader.getObject()))
                .append("</th>");
        for (int s = 0; s < series.size(); s++)
            b.append("<th scope=\"col\" data-color=\"")
                    .append(color(s))
                    .append("\"")
                    .append(series.get(s).dashed ? " data-dashed=\"true\"" : "")
                    .append('>')
                    .append(escape(snap.labels.get(s)))
                    .append("</th>");
        b.append("</tr></thead><tbody>");
        for (Row row : snap.rows) {
            b.append("<tr><th scope=\"row\"");
            if (!row.tooltipLabel.equals(row.category))
                b.append(" data-tooltip-label=\"").append(escape(row.tooltipLabel)).append('"');
            b.append('>').append(escape(row.category)).append("</th>");
            for (int s = 0; s < row.text.size(); s++) {
                b.append("<td");
                if (!row.tooltipText.get(s).equals(row.text.get(s)))
                    b.append(" data-tooltip-value=\"").append(escape(row.tooltipText.get(s))).append('"');
                b.append('>').append(escape(row.text.get(s))).append("</td>");
            }
            b.append("</tr>");
        }
        return b.append("</tbody></table></div></details>").toString();
    }

    private void renderLegend(StringBuilder b, Snapshot snap) {
        b.append("<ul class=\"sw-chart-legend-content\" data-position=\"")
                .append(legendPosition.name().toLowerCase(Locale.ROOT)).append("\" data-align=\"")
                .append(legendAlignment.name().toLowerCase(Locale.ROOT)).append("\" aria-label=\"")
                .append(escape(getString("SwChart.legend", null, "Series")))
                .append("\">");
        for (int s = 0; s < series.size(); s++)
            b.append("<li class=\"sw-chart-legend-item ")
                    .append(color(s))
                    .append("\"><span class=\"sw-chart-legend-indicator\"")
                    .append(series.get(s).dashed ? " data-dashed=\"true\"" : "")
                    .append(" aria-hidden=\"true\"></span>")
                    .append(escape(snap.labels.get(s)))
                    .append("</li>");
        b.append("</ul>");
    }

    private void interactionAttributes(StringBuilder b) {
        b.append(" data-stack-mode=\"").append(stackMode.name().toLowerCase(Locale.ROOT))
                .append("\" data-tooltip-label-visible=\"").append(tooltipLabelVisible).append("\" data-tooltip-indicator-visible=\"")
                .append(tooltipIndicatorVisible).append("\" data-tooltip-indicator=\"")
                .append(tooltipIndicator.name().toLowerCase(Locale.ROOT)).append("\" data-tooltip-cursor=\"")
                .append(tooltipCursor).append('"');
    }

    private static String plotDescription(String id, Snapshot snap) {
        return id + "-help" + (snap.references.isEmpty() ? "" : " " + id + "-references");
    }

    private static String referenceColor(Reference reference) {
        return reference.color == 0 ? "" : " sw-chart-series-" + reference.color;
    }

    private void renderReferenceCaptions(StringBuilder b, String id, Snapshot snap) {
        if (snap.references.isEmpty()) return;
        b.append("<ul class=\"sw-chart-references\" id=\"").append(id).append("-references\" aria-label=\"")
                .append(escape(getString("SwChart.references"))).append("\">");
        for (Reference reference : snap.references) {
            b.append("<li class=\"sw-chart-reference-item").append(referenceColor(reference))
                    .append("\"><span class=\"sw-chart-reference-indicator\" aria-hidden=\"true\" data-kind=\"")
                    .append(reference.band ? "band" : "line").append('"');
            if (!reference.band && reference.dashed) b.append(" data-dashed=\"true\"");
            b.append("></span><span class=\"sw-chart-reference-name\">").append(escape(reference.label))
                    .append("</span> <span class=\"sw-chart-reference-value\">").append(escape(reference.text)).append("</span></li>");
        }
        b.append("</ul>");
    }

    private void renderReferenceBands(StringBuilder b, Snapshot snap, int height) {
        double minimum = snap.ticks[0], maximum = snap.ticks[snap.ticks.length - 1];
        for (Reference reference : snap.references) {
            if (!reference.band) continue;
            double lower = Math.max(minimum, reference.lower), upper = Math.min(maximum, reference.upper);
            if (lower >= upper) continue;
            b.append("<rect class=\"sw-chart-reference-band").append(referenceColor(reference))
                    .append("\" aria-hidden=\"true\" fill-opacity=\"").append(num(reference.opacity)).append('"');
            if (orientation == Orientation.HORIZONTAL)
                b.append(" x=\"").append(num(x(lower, snap.ticks))).append("\" y=\"0\" width=\"")
                        .append(num(x(upper, snap.ticks) - x(lower, snap.ticks))).append("\" height=\"").append(height);
            else
                b.append(" x=\"0\" y=\"").append(num(y(upper, snap.ticks))).append("\" width=\"1000\" height=\"")
                        .append(num(y(lower, snap.ticks) - y(upper, snap.ticks)));
            b.append("\"/>");
        }
    }

    private void renderReferenceLines(StringBuilder b, Snapshot snap, int height) {
        List<Reference> lines = snap.references.stream().filter(reference -> !reference.band
                && reference.lower >= snap.ticks[0] && reference.lower <= snap.ticks[snap.ticks.length - 1]).toList();
        if (lines.isEmpty()) return;
        b.append("<svg class=\"sw-chart-reference-overlay\" viewBox=\"0 0 1000 ").append(height)
                .append("\" preserveAspectRatio=\"none\" aria-hidden=\"true\" focusable=\"false\">");
        for (Reference reference : lines) {
            b.append("<line class=\"sw-chart-reference-line").append(referenceColor(reference)).append('"');
            if (orientation == Orientation.HORIZONTAL)
                b.append(" x1=\"").append(num(x(reference.lower, snap.ticks))).append("\" x2=\"")
                        .append(num(x(reference.lower, snap.ticks))).append("\" y1=\"0\" y2=\"").append(height);
            else
                b.append(" x1=\"0\" x2=\"1000\" y1=\"").append(num(y(reference.lower, snap.ticks)))
                        .append("\" y2=\"").append(num(y(reference.lower, snap.ticks)));
            b.append('"');
            if (reference.dashed) b.append(" stroke-dasharray=\"5 5\"");
            b.append("/>");
        }
        b.append("</svg>");
    }

    private void renderVerticalPlot(StringBuilder b, String id, Snapshot snap, String name) {
        int count = snap.rows.size();
        List<LabelSelection> selections = labelSelections(snap);
        List<ValueLabel> labels = new ArrayList<>();
        List<NativeBar> nativeBars = new ArrayList<>();
        b.append("<div class=\"sw-chart-container\" data-y-axis=\"").append(yAxis).append("\">");
        if (yAxis) {
            b.append("<div class=\"sw-chart-y-axis\" aria-hidden=\"true\">");
            for (int i = snap.ticks.length - 1; i >= 0; i--)
                b.append("<span>").append(escape(axisValue(snap.ticks[i]))).append("</span>");
            b.append("</div>");
        }
        b.append("<div class=\"sw-chart-plot\" role=\"group\" aria-labelledby=\"")
                .append(id)
                .append("-title\" aria-describedby=\"")
                .append(escape(plotDescription(id, snap)))
                .append("\" data-tooltip=\"")
                .append(tooltip).append('"');
        interactionAttributes(b);
        b.append('>');
        b.append("<svg class=\"sw-chart-svg\" viewBox=\"0 0 1000 240\" preserveAspectRatio=\"none\" role=\"img\" "
                 + "aria-label=\"")
                .append(name)
                .append("\">");
        if (series.stream().anyMatch(part -> part.curve == ChartSeries.Curve.NATURAL))
            b.append("<defs><clipPath id=\"").append(escape(id)).append("-natural-clip\"><rect width=\"1000\" height=\"240\"/></clipPath></defs>");
        renderReferenceBands(b, snap, 240);
        if (grid)
            for (double tick : snap.ticks)
                b.append("<line class=\"sw-chart-grid\" x1=\"0\" x2=\"1000\" y1=\"")
                        .append(num(y(tick, snap.ticks)))
                        .append("\" y2=\"")
                        .append(num(y(tick, snap.ticks)))
                        .append("\"/>");
        List<String> barGroups = barGroups();
        for (int s = 0; s < series.size(); s++) {
            var part = series.get(s);
            String css = color(s);
            b.append("<g class=\"").append(css).append("\" data-series=\"").append(s).append('"');
            if (snap.joins[s] >= 0) b.append(" data-join-index=\"").append(snap.joins[s]).append('"');
            b.append('>');
            if (part.type == ChartSeries.Type.BAR) {
                int slot = barGroups.indexOf(barGroup(s));
                double width = 720.0 / count / barGroups.size();
                for (int r = 0; r < count; r++)
                    if (snap.rows.get(r).plotValues.get(s) != null) {
                        double top = y(Math.max(snap.low[s][r], snap.high[s][r]), snap.ticks),
                               bottom = y(Math.min(snap.low[s][r], snap.high[s][r]), snap.ticks);
                        double left = (r + .14) * 1000 / count + slot * width;
                        if (part.barRadius != null) {
                            nativeBars.add(new NativeBar(s, r, left / 10, top, width * .09, bottom - top));
                            continue;
                        }
                        b.append("<rect class=\"sw-chart-bar\" x=\"")
                                .append(num(left))
                                .append("\" y=\"")
                                .append(num(top))
                                .append("\" width=\"")
                                .append(num(width * .9))
                                .append("\" height=\"")
                                .append(num(bottom - top))
                                .append("\" rx=\"4\"><title>")
                                .append(escape(snap.rows.get(r).tooltipLabel + ": " + snap.labels.get(s) + " "
                                        + snap.rows.get(r).tooltipText.get(s)))
                                .append("</title></rect>");
                    }
            } else {
                for (int start = 0; start < count;) {
                    while (start < count && !plotted(snap, s, start)) start++;
                    if (start == count)
                        break;
                    int end = start;
                    while (end + 1 < count && plotted(snap, s, end + 1)) end++;
                    if (start == end && snap.joins[s] == start) { start++; continue; }
                    String path = path(snap, s, start, end, false, part.curve);
                    if (part.type == ChartSeries.Type.AREA) {
                        StringBuilder area =
                                new StringBuilder(path)
                                        .append(path(snap, s, end, start, true, part.curve).replaceFirst("^M", " L"))
                                        .append(" Z");
                        b.append("<path class=\"sw-chart-area\"");
                        naturalClip(b, id, part);
                        b.append(" fill-opacity=\"")
                                .append(num(part.opacity))
                                .append("\" d=\"")
                                .append(area)
                                .append("\"/>");
                    }
                    b.append("<path class=\"sw-chart-line\" stroke-width=\"").append(num(part.strokeWidth)).append('"');
                    naturalClip(b, id, part);
                    b.append(" d=\"")
                            .append(path)
                            .append("\"")
                            .append(part.dashed ? " stroke-dasharray=\"5 5\"" : "")
                            .append("/>");
                    if (part.dots || end == start)
                        for (int r = start; r <= end; r++)
                            if (snap.rows.get(r).plotValues.get(s) != null)
                                b.append("<circle class=\"sw-chart-dot\" cx=\"")
                                        .append(num((r + .5) * 1000 / count))
                                        .append("\" cy=\"")
                                        .append(num(y(snap.high[s][r], snap.ticks)))
                                        .append("\" r=\"4\"/>");
                    start = end + 1;
                }
            }
            b.append("</g>");
            for (int r : selections.get(s).rows) {
                double center = part.type == ChartSeries.Type.BAR
                        ? (r + .14) * 100 / count + barGroups.indexOf(barGroup(s)) * 72.0 / count / barGroups.size()
                                + 32.4 / count / barGroups.size()
                        : (r + .5) * 100 / count;
                double top = y(snap.high[s][r], snap.ticks);
                double baseline = Math.max(12, Math.min(228, top + (snap.rows.get(r).values.get(s) < 0 ? 14 : -8)));
                labels.add(new ValueLabel(s, r, snap.rows.get(r).text.get(s), center, baseline,
                        center < 15 ? 1 : center > 85 ? -1 : 0, labelPriority(selections.get(s), r)));
            }
        }
        for (int r = 0; r < count; r++)
            b.append("<rect class=\"sw-chart-hit\" data-index=\"")
                    .append(r)
                    .append("\" x=\"")
                    .append(num(r * 1000.0 / count))
                    .append("\" y=\"0\" width=\"")
                    .append(num(1000.0 / count))
                    .append("\" height=\"240\"><title>")
                    .append(escape(nativeTip(snap, r)))
                    .append("</title></rect>");
        b.append("</svg>");
        renderNativeBars(b, id, nativeBars, 240);
        renderReferenceLines(b, snap, 240);
        renderValueLabels(b, labels, 240);
        b.append("<div class=\"sw-chart-tooltip-content\" role=\"tooltip\" hidden></div></div>");
        if (xAxis) {
            if (yAxis)
                b.append("<span aria-hidden=\"true\"></span>");
            b.append("<div class=\"sw-chart-x-axis\" aria-hidden=\"true\" data-max-ticks=\"").append(maxCategoryTicks)
                    .append("\" data-tick-gap=\"").append(minCategoryTickGap).append("\">");
            Set<Integer> nativeTicks = new HashSet<>();
            int visible = Math.min(count, maxCategoryTicks);
            for (int i = 0; i < visible; i++) nativeTicks.add(visible == 1 ? 0 : (int) Math.round(i * (count - 1.0) / (visible - 1)));
            for (int r = 0; r < count; r++) {
                b.append("<span data-tick-index=\"").append(r).append('"');
                if (!nativeTicks.contains(r)) b.append(" data-native-hidden=\"true\"");
                b.append("><span>").append(escape(snap.rows.get(r).axisCategory)).append("</span></span>");
            }
            b.append("</div>");
        }
        b.append("</div>");
    }

    /** Bar stacks share one slot; unstacked series each receive their own. */
    private List<String> barGroups() {
        List<String> barGroups = new ArrayList<>();
        for (int s = 0; s < series.size(); s++)
            if (series.get(s).type == ChartSeries.Type.BAR) {
                String group = barGroup(s);
                if (!barGroups.contains(group))
                    barGroups.add(group);
            }
        return barGroups;
    }

    private String barGroup(int s) {
        return series.get(s).stack == null ? "series:" + s : "stack:" + series.get(s).stack;
    }

    private static double x(double value, double[] ticks) {
        return (value - ticks[0]) / (ticks[ticks.length - 1] - ticks[0]) * 1000;
    }

    private void renderHorizontalPlot(StringBuilder b, String id, Snapshot snap) {
        List<String> groups = barGroups();
        List<LabelSelection> selections = labelSelections(snap);
        int height = 16 + groups.size() * 20;
        b.append("<div class=\"sw-chart-container\" data-orientation=\"horizontal\" data-y-axis=\"")
                .append(yAxis)
                .append("\"><div class=\"sw-chart-plot\" data-orientation=\"horizontal\" role=\"group\" aria-labelledby=\"")
                .append(id).append("-title\" aria-describedby=\"").append(escape(plotDescription(id, snap))).append("\" data-tooltip=\"")
                .append(tooltip).append('"');
        interactionAttributes(b);
        b.append('>');
        for (int r = 0; r < snap.rows.size(); r++) {
            Row row = snap.rows.get(r);
            List<ValueLabel> labels = new ArrayList<>();
            List<NativeBar> nativeBars = new ArrayList<>();
            if (yAxis)
                b.append("<div class=\"sw-chart-category\" aria-hidden=\"true\">").append(escape(row.axisCategory)).append("</div>");
            b.append("<div class=\"sw-chart-row\"><svg class=\"sw-chart-row-svg\" viewBox=\"0 0 1000 ")
                    .append(height).append("\" width=\"100%\" height=\"").append(height)
                    .append("\" preserveAspectRatio=\"none\" aria-hidden=\"true\">");
            renderReferenceBands(b, snap, height);
            for (double tick : snap.ticks) {
                if (!grid && tick != 0) continue;
                b.append("<line class=\"").append(tick == 0 ? "sw-chart-baseline" : "sw-chart-grid")
                        .append("\" x1=\"").append(num(x(tick, snap.ticks))).append("\" x2=\"")
                        .append(num(x(tick, snap.ticks))).append("\" y1=\"0\" y2=\"").append(height).append("\"/>");
            }
            for (int s = 0; s < series.size(); s++) {
                if (row.plotValues.get(s) == null) continue;
                double start = x(Math.min(snap.low[s][r], snap.high[s][r]), snap.ticks),
                       end = x(Math.max(snap.low[s][r], snap.high[s][r]), snap.ticks);
                int top = 8 + groups.indexOf(barGroup(s)) * 20;
                if (series.get(s).barRadius != null) {
                    nativeBars.add(new NativeBar(s, r, start / 10, top, (end - start) / 10, 14));
                } else {
                    b.append("<g class=\"").append(color(s)).append("\" data-series=\"").append(s)
                            .append("\"><rect class=\"sw-chart-bar\" x=\"").append(num(start))
                            .append("\" y=\"").append(top)
                            .append("\" width=\"").append(num(end - start)).append("\" height=\"14\" rx=\"4\"><title>")
                            .append(escape(row.tooltipLabel + ": " + snap.labels.get(s) + " " + row.tooltipText.get(s)))
                            .append("</title></rect></g>");
                }
                if (selections.get(s).rows.contains(r)) {
                    double position = x(snap.high[s][r], snap.ticks) / 10;
                    int side = row.values.get(s) < 0 ? -1 : 1;
                    if (position > 85 && side > 0 || position < 15 && side < 0) side = -side;
                    labels.add(new ValueLabel(s, r, row.text.get(s), position, 19 + groups.indexOf(barGroup(s)) * 20,
                            side, labelPriority(selections.get(s), r)));
                }
            }
            b.append("<rect class=\"sw-chart-hit\" data-index=\"").append(r)
                    .append("\" x=\"0\" y=\"0\" width=\"1000\" height=\"").append(height).append("\"><title>")
                    .append(escape(nativeTip(snap, r))).append("</title></rect></svg>");
            renderNativeBars(b, id, nativeBars, height);
            renderReferenceLines(b, snap, height);
            renderValueLabels(b, labels, height);
            b.append("</div>");
        }
        if (xAxis) {
            if (yAxis) b.append("<span aria-hidden=\"true\"></span>");
            b.append("<div class=\"sw-chart-value-axis\" aria-hidden=\"true\">");
            for (double tick : snap.ticks)
                b.append("<span><span>").append(escape(axisValue(tick))).append("</span></span>");
            b.append("</div>");
        }
        b.append("<div class=\"sw-chart-tooltip-content\" role=\"tooltip\" hidden></div></div></div>");
    }

    private static void naturalClip(StringBuilder b, String id, ChartSeries<?> part) {
        if (part.curve == ChartSeries.Curve.NATURAL)
            b.append(" clip-path=\"url(#").append(escape(id)).append("-natural-clip)\"");
    }

    /** Native-pixel SVG overlay: percentage positions scale; individual rounded corners do not stretch. */
    private void renderNativeBars(StringBuilder b, String id, List<NativeBar> bars, int height) {
        if (bars.isEmpty()) return;
        b.append("<svg class=\"sw-chart-native-bars\" width=\"100%\" height=\"").append(height)
                .append("\" aria-hidden=\"true\" focusable=\"false\">");
        for (boolean rtl : new boolean[]{false, true}) {
            b.append("<g data-native-dir=\"").append(rtl ? "rtl" : "ltr").append("\">");
            for (NativeBar bar : bars) {
                if (bar.width <= 0 || bar.height <= 0) continue;
                String mask = escape(id) + "-bar-" + bar.series + "-" + bar.row + (rtl ? "-rtl" : "-ltr");
                b.append("<svg class=\"").append(color(bar.series)).append("\" data-series=\"").append(bar.series)
                        .append("\" data-index=\"").append(bar.row).append("\" x=\"").append(num(rtl ? 100 - bar.x - bar.width : bar.x))
                        .append("%\" y=\"").append(num(bar.y)).append("\" width=\"").append(num(bar.width)).append("%\" height=\"")
                        .append(num(bar.height)).append("\"><defs><mask id=\"").append(mask)
                        .append("\" maskUnits=\"objectBoundingBox\" maskContentUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\" width=\"1\" height=\"1\">");
                String[] corners = {"tl", "tr", "br", "bl"};
                for (int i = 0; i < corners.length; i++) {
                    boolean right = i == 1 || i == 2, bottom = i >= 2;
                    // Each clipped quadrant draws a full-size rounded rectangle. The SVG renderer clamps radii
                    // against the actual responsive bar dimensions, in native pixels, without script geometry.
                    b.append("<svg class=\"sw-chart-corner\" data-corner=\"").append(corners[i])
                            .append("\" x=\"").append(right ? "50%" : "0").append("\" y=\"").append(bottom ? "50%" : "0")
                            .append("\" width=\"50%\" height=\"50%\" overflow=\"hidden\"><rect fill=\"white\" x=\"")
                            .append(right ? "-100%" : "0").append("\" y=\"").append(bottom ? "-100%" : "0")
                            .append("\" width=\"200%\" height=\"200%\" rx=\"").append(num(series.get(bar.series).barRadius[i]))
                            .append("\" ry=\"").append(num(series.get(bar.series).barRadius[i])).append("\"/></svg>");
                }
                // The midlines always lie inside radii clamped to half the bar. Crisp one-pixel joins cover
                // fractional viewport seams without expanding or thresholding the antialiased outer corners.
                b.append("<line x1=\"50%\" x2=\"50%\" y1=\"0\" y2=\"100%\" stroke=\"white\" stroke-width=\"1\" shape-rendering=\"crispEdges\"/>")
                        .append("<line x1=\"0\" x2=\"100%\" y1=\"50%\" y2=\"50%\" stroke=\"white\" stroke-width=\"1\" shape-rendering=\"crispEdges\"/>")
                        .append("</mask></defs><rect class=\"sw-chart-bar\" width=\"100%\" height=\"100%\" mask=\"url(#")
                        .append(mask).append(")\"/></svg>");
            }
            b.append("</g>");
        }
        b.append("</svg>");
    }

    private List<LabelSelection> labelSelections(Snapshot snap) {
        List<LabelSelection> result = new ArrayList<>();
        for (int s = 0; s < series.size(); s++) {
            var selected = new LinkedHashSet<Integer>();
            int last = -1, highest = -1;
            var mode = series.get(s).valueLabels;
            if (mode != ChartSeries.ValueLabels.NONE) {
                for (int r = 0; r < snap.rows.size(); r++) {
                    Double value = snap.rows.get(r).values.get(s);
                    if (snap.rows.get(r).plotValues.get(s) == null) continue;
                    last = r;
                    if (highest < 0 || value > snap.rows.get(highest).values.get(s)) highest = r;
                    if (mode == ChartSeries.ValueLabels.ALL) selected.add(r);
                }
                if (last >= 0) selected.add(last);
                if (highest >= 0 && mode == ChartSeries.ValueLabels.HIGHEST_AND_LAST) selected.add(highest);
            }
            result.add(new LabelSelection(selected, last, highest));
        }
        return result;
    }
    private static int labelPriority(LabelSelection selected, int row) {
        return row == selected.last ? 3 : row == selected.highest ? 2 : 1;
    }
    /** A second SVG uses native pixel text and percentage anchors, so responsive geometry never stretches labels. */
    private static void renderValueLabels(StringBuilder b, List<ValueLabel> labels, int height) {
        if (labels.isEmpty()) return;
        b.append("<svg class=\"sw-chart-value-labels\" width=\"100%\" height=\"").append(height)
                .append("\" aria-hidden=\"true\" focusable=\"false\">");
        for (boolean rtl : new boolean[] {false, true}) {
            b.append("<g data-label-dir=\"").append(rtl ? "rtl" : "ltr").append("\">");
            for (ValueLabel label : labels) {
                if (label.text.isEmpty()) continue;
                int side = rtl ? -label.side : label.side;
                b.append("<text class=\"sw-chart-value-label\" data-series=\"").append(label.series)
                        .append("\" data-index=\"").append(label.row).append("\" data-priority=\"").append(label.priority)
                        .append("\" x=\"").append(num(rtl ? 100 - label.x : label.x)).append("%\" y=\"").append(num(label.y))
                        .append("\" dx=\"").append(side * 6).append("\" text-anchor=\"").append(side < 0 ? "end" : side > 0 ? "start" : "middle")
                        .append("\">").append(escape(label.text)).append("</text>");
            }
            b.append("</g>");
        }
        b.append("</svg>");
    }

    private static String nativeTip(Snapshot snap, int row) {
        StringJoiner text = new StringJoiner("; ");
        text.add(snap.rows.get(row).tooltipLabel);
        for (int s = 0; s < snap.labels.size(); s++)
            if (!snap.rows.get(row).tooltipText.get(s).isEmpty())
                text.add(snap.labels.get(s) + ": " + snap.rows.get(row).tooltipText.get(s));
        return text.toString();
    }

    private static String path(Snapshot snap, int s, int start, int end, boolean lower, ChartSeries.Curve curve) {
        int count = snap.rows.size();
        StringBuilder b = new StringBuilder("M");
        double[][] values = lower ? snap.low : snap.high;
        int direction = start <= end ? 1 : -1;
        if (curve == ChartSeries.Curve.NATURAL && start != end) {
            int length = Math.abs(end - start) + 1;
            double[] points = new double[length];
            for (int i = 0; i < length; i++) points[i] = y(values[s][start + i * direction], snap.ticks);
            double[][] controls = naturalControls(points);
            double xx = (start + .5) * 1000 / count, step = direction * 1000.0 / count;
            b.append(num(xx)).append(',').append(num(points[0]));
            for (int i = 0; i < length - 1; i++) {
                b.append(" C").append(num(xx + step / 3)).append(',').append(num(controls[0][i]))
                        .append(' ').append(num(xx + 2 * step / 3)).append(',').append(num(controls[1][i]))
                        .append(' ').append(num(xx + step)).append(',').append(num(points[i + 1]));
                xx += step;
            }
            return b.toString();
        }
        for (int r = start; direction > 0 ? r <= end : r >= end; r += direction) {
            double x = (r + .5) * 1000 / count, yy = y(values[s][r], snap.ticks);
            if (r == start)
                b.append(num(x)).append(',').append(num(yy));
            else if (curve == ChartSeries.Curve.STEP || curve == ChartSeries.Curve.STEP_AFTER
                    || curve == ChartSeries.Curve.STEP_BEFORE || curve == ChartSeries.Curve.STEP_MIDDLE) {
                if (curve == ChartSeries.Curve.STEP_MIDDLE) {
                    double previous = (r - direction + .5) * 1000 / count;
                    b.append(" H").append(num((previous + x) / 2)).append(" V").append(num(yy)).append(" H").append(num(x));
                } else if (direction > 0 ^ curve == ChartSeries.Curve.STEP_BEFORE)
                    b.append(" H").append(num(x)).append(" V").append(num(yy));
                else
                    b.append(" V").append(num(yy)).append(" H").append(num(x));
            } else if (curve == ChartSeries.Curve.MONOTONE) {
                double previous = (r - direction + .5) * 1000 / count,
                       previousY = y(values[s][r - direction], snap.ticks), mid = (previous + x) / 2;
                b.append(" C")
                        .append(num(mid))
                        .append(',')
                        .append(num(previousY))
                        .append(' ')
                        .append(num(mid))
                        .append(',')
                        .append(num(yy))
                        .append(' ')
                        .append(num(x))
                        .append(',')
                        .append(num(yy));
            } else
                b.append(" L").append(num(x)).append(',').append(num(yy));
        }
        return b.toString();
    }

    /** Natural cubic spline with zero second derivative at both endpoints, in equally spaced category coordinates. */
    static double[][] naturalControls(double[] points) {
        int count = points.length;
        double[] second = new double[count], diagonal = new double[count], rhs = new double[count];
        for (int i = 1; i < count - 1; i++) {
            double factor = i == 1 ? 0 : 1 / diagonal[i - 1];
            diagonal[i] = 4 - factor;
            rhs[i] = 6 * (points[i + 1] - 2 * points[i] + points[i - 1]) - factor * rhs[i - 1];
        }
        for (int i = count - 2; i > 0; i--) second[i] = (rhs[i] - second[i + 1]) / diagonal[i];
        double[][] result = new double[2][count - 1];
        for (int i = 0; i < count - 1; i++) {
            double delta = points[i + 1] - points[i];
            result[0][i] = points[i] + delta / 3 - (2 * second[i] + second[i + 1]) / 18;
            result[1][i] = points[i + 1] - delta / 3 - (second[i] + 2 * second[i + 1]) / 18;
        }
        return result;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptHeaderItem.forReference(CHART_JS));
        for (ChartSeries<T> part : series) {
            String appearance = ChartAppearance.className(part);
            if (!appearance.isEmpty()) response.render(CssHeaderItem.forCSS(ChartAppearance.css(part), appearance));
        }
    }

    @Override
    protected void onDetach() {
        snapshot = null;
        data.detach();
        title.detach();
        categoryHeader.detach();
        series.forEach(s -> s.label.detach());
        references.forEach(ChartReference::detach);
        super.onDetach();
    }
}

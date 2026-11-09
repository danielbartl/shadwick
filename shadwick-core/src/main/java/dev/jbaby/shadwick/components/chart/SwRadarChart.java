package dev.jbaby.shadwick.components.chart;

import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import org.apache.wicket.markup.head.*;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.util.string.Strings;

/** Native radial comparisons on common dimensions, with a shared zero-based scale and exact data table. */
public class SwRadarChart<T> extends Panel {
    private static final double CENTER = 160, RADIUS = 120, LABEL_RADIUS = 145;
    private final IModel<? extends List<T>> data;
    private final SerializableFunction<T, String> category;
    private final List<RadarSeries<T>> series = new ArrayList<>();
    private IModel<String> title = Model.of("Chart"), categoryHeader;
    private IModel<? extends Number> maximum;
    private SwChart.ValueFormat namedFormat = SwChart.ValueFormat.NUMBER;
    private SerializableBiFunction<Number, Locale, String> valueFormat;
    private Currency currency;
    private int levels = 4;
    private boolean grid = true, legend = true, tooltip = true;
    private transient Snapshot snapshot;
    private record Row(String category, List<BigDecimal> values, List<String> text) implements Serializable {}
    private record Style(String label, int color, boolean dashed, boolean dots, double opacity, double width) implements Serializable {}
    private record Snapshot(String title, String categoryHeader, List<Row> rows, List<Style> series,
                            BigDecimal maximum, String zeroText, String maximumText, String missing, boolean measured) implements Serializable {}

    public SwRadarChart(String id, IModel<? extends List<T>> data, SerializableFunction<T, String> category) {
        super(id);
        this.data = Objects.requireNonNull(data);
        this.category = Objects.requireNonNull(category);
        setOutputMarkupId(true);
    }

    @Override protected void onInitialize() {
        super.onInitialize();
        add(figure());
    }

    private WebMarkupContainer figure() {
        var figure = new WebMarkupContainer("figure");
        figure.add(new StyledBehavior("sw-chart sw-radar-chart"));
        figure.setOutputMarkupId(true);
        figure.add(new Label("content", (IModel<String>) () -> renderChart(figure.getMarkupId())).setEscapeModelStrings(false));
        return figure;
    }

    public SwRadarChart<T> addSeries(RadarSeries<T> value) {
        Objects.requireNonNull(value);
        if (series.contains(value)) throw new IllegalArgumentException("A radar series must be registered exactly once");
        series.add(value);
        return this;
    }

    public SwRadarChart<T> setTitle(String value) { return setTitle(Model.of(value)); }
    public SwRadarChart<T> setTitle(IModel<String> value) { title = Objects.requireNonNull(value); return this; }
    public SwRadarChart<T> setCategoryHeader(String value) { return setCategoryHeader(Model.of(value)); }
    public SwRadarChart<T> setCategoryHeader(IModel<String> value) { categoryHeader = Objects.requireNonNull(value); return this; }
    public SwRadarChart<T> setMaximum(Number value) { return setMaximum(Model.of(Objects.requireNonNull(value))); }
    public SwRadarChart<T> setMaximum(IModel<? extends Number> value) {
        Objects.requireNonNull(value);
        if (maximum != null && maximum != value) maximum.detach();
        maximum = value;
        return this;
    }
    public SwRadarChart<T> setAutoScale() {
        if (maximum != null) maximum.detach();
        maximum = null;
        return this;
    }
    /** Number of equally spaced polygon rings, from 2 to 10; default four. */
    public SwRadarChart<T> setLevels(int value) {
        if (value < 2 || value > 10) throw new IllegalArgumentException("Radar levels must be between 2 and 10");
        levels = value;
        return this;
    }
    public SwRadarChart<T> setValueFormat(SwChart.ValueFormat value) {
        namedFormat = Objects.requireNonNull(value); valueFormat = null; return this;
    }
    public SwRadarChart<T> setValueFormat(SerializableBiFunction<Number, Locale, String> value) { valueFormat = value; return this; }
    public SwRadarChart<T> setCurrency(Currency value) {
        currency = Objects.requireNonNull(value); namedFormat = SwChart.ValueFormat.CURRENCY; valueFormat = null; return this;
    }
    public SwRadarChart<T> setShowGrid(boolean value) { grid = value; return this; }
    public SwRadarChart<T> setShowLegend(boolean value) { legend = value; return this; }
    public SwRadarChart<T> setShowTooltip(boolean value) { tooltip = value; return this; }

    private String format(Number value) { return ChartValues.format(value, getLocale(), namedFormat, currency, valueFormat); }
    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalStateException(message);
        return value;
    }
    private static BigDecimal amount(Number value) {
        if (value == null) return null;
        double number = value.doubleValue();
        if (!Double.isFinite(number) || Math.abs(number) > 1e100)
            throw new IllegalArgumentException("Radar values must be finite, nonnegative and at most 1e100");
        BigDecimal result;
        try { result = value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString()); }
        catch (NumberFormatException ignored) { result = BigDecimal.valueOf(number); }
        if (result.signum() < 0 || result.compareTo(BigDecimal.ONE.scaleByPowerOfTen(100)) > 0)
            throw new IllegalArgumentException("Radar values must be finite, nonnegative and at most 1e100");
        return result;
    }

    private Snapshot snapshot() {
        if (snapshot != null) return snapshot;
        String name = required(title.getObject(), "A radar chart needs a title");
        String header = categoryHeader == null ? getString("SwRadarChart.dimension")
                : required(categoryHeader.getObject(), "A radar chart needs a dimension header");
        if (series.isEmpty()) throw new IllegalStateException("A radar chart needs at least one series");
        List<Style> styles = new ArrayList<>();
        for (int i = 0; i < series.size(); i++) {
            RadarSeries<T> part = series.get(i);
            styles.add(new Style(required(part.label.getObject(), "Every radar series needs a label"),
                    part.color == 0 ? i % 5 + 1 : part.color, part.dashed, part.dots, part.opacity, part.strokeWidth));
        }
        List<Row> rows = readRows();
        BigDecimal largest = BigDecimal.ZERO;
        boolean measured = false;
        for (Row row : rows) for (BigDecimal value : row.values) {
            if (value == null) continue;
            measured = true;
            largest = largest.max(value);
        }
        Number configured = maximum == null ? null : maximum.getObject();
        BigDecimal ceiling = maximum == null ? automaticMaximum(largest) : amount(configured);
        if (ceiling == null || ceiling.signum() <= 0) throw new IllegalArgumentException("Radar maximum must be positive");
        if (largest.compareTo(ceiling) > 0) throw new IllegalArgumentException("Radar values must not exceed the configured maximum");
        return snapshot = new Snapshot(name, header, List.copyOf(rows), List.copyOf(styles), ceiling,
                format(BigDecimal.ZERO), format(maximum == null ? ceiling : configured), getString("SwRadarChart.missing"), measured);
    }

    private List<Row> readRows() {
        List<Row> rows = new ArrayList<>();
        List<T> items = data.getObject();
        if (items == null) return rows;
        for (T item : items) {
            String name = required(category.apply(item), "Every radar dimension needs a label");
            List<BigDecimal> values = new ArrayList<>();
            List<String> text = new ArrayList<>();
            for (RadarSeries<T> part : series) {
                Number value = part.value.apply(item);
                values.add(amount(value));
                text.add(format(value));
            }
            rows.add(new Row(name, Collections.unmodifiableList(values), List.copyOf(text)));
        }
        return rows;
    }

    private BigDecimal automaticMaximum(BigDecimal largest) {
        if (largest.signum() == 0) return BigDecimal.ONE;
        if (largest.doubleValue() == 0) return largest;
        double[] ticks = SwChart.ticks(0, largest.doubleValue(), levels + 1);
        return BigDecimal.valueOf(ticks[ticks.length - 1]).max(largest);
    }

    private static String escape(String value) { return Strings.escapeMarkup(Objects.toString(value, "")).toString(); }
    private static String num(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite radar coordinate");
        return String.format(Locale.ROOT, "%.5f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
    private static double angle(int row, int count) { return row * Math.PI * 2 / count - Math.PI / 2; }
    private static double x(int row, int count, double radius) { return CENTER + Math.cos(angle(row, count)) * radius; }
    private static double y(int row, int count, double radius) { return CENTER + Math.sin(angle(row, count)) * radius; }
    private static double radius(BigDecimal value, BigDecimal maximum) { return value.divide(maximum, MathContext.DECIMAL128).doubleValue() * RADIUS; }
    private static String polygon(Snapshot s, double radius) {
        StringJoiner points = new StringJoiner(" ");
        for (int r = 0; r < s.rows.size(); r++) points.add(num(x(r, s.rows.size(), radius)) + "," + num(y(r, s.rows.size(), radius)));
        return points.toString();
    }

    private String renderChart(String id) {
        Snapshot s = snapshot();
        StringBuilder b = new StringBuilder("<figcaption class=\"sw-chart-title\" id=\"").append(id).append("-title\">")
                .append(escape(s.title)).append("</figcaption>");
        if (s.rows.isEmpty()) emptyState(b, "empty");
        else if (s.rows.size() < 3) emptyState(b, "tooFew");
        else if (!s.measured) emptyState(b, "noValues");
        else {
            renderPlot(b, s, id);
            renderScale(b, s, id);
            renderDimensionKey(b, s);
        }
        if (legend) renderLegend(b, s);
        if (!s.rows.isEmpty()) renderTable(b, s, id);
        return b.toString();
    }

    private void emptyState(StringBuilder b, String key) {
        b.append("<p class=\"sw-chart-empty\" role=\"status\">").append(escape(getString("SwRadarChart." + key))).append("</p>");
    }

    private void renderPlot(StringBuilder b, Snapshot s, String id) {
        b.append("<p class=\"sw-chart-sr-only\" id=\"").append(id).append("-help\">")
                .append(escape(getString(tooltip ? "SwRadarChart.instructions" : "SwRadarChart.dataInstructions"))).append("</p>")
                .append("<div class=\"sw-chart-plot sw-radar-plot\" role=\"group\" aria-labelledby=\"").append(id)
                .append("-title\" aria-describedby=\"").append(id).append("-help ").append(id)
                .append("-scale\" data-tooltip=\"").append(tooltip).append("\">")
                .append("<svg class=\"sw-radar-svg\" viewBox=\"0 0 320 320\" aria-hidden=\"true\">");
        if (grid) renderGrid(b, s);
        renderHits(b, s);
        for (int i = 0; i < s.series.size(); i++) renderSeries(b, s, i);
        b.append("</svg>");
        renderDots(b, s);
        renderNumbers(b, s);
        b.append("<div class=\"sw-chart-tooltip-content\" role=\"tooltip\" hidden></div></div>");
    }

    private void renderGrid(StringBuilder b, Snapshot s) {
        for (int ring = 1; ring <= levels; ring++)
            b.append("<polygon class=\"sw-radar-grid\" points=\"").append(polygon(s, RADIUS * ring / levels)).append("\"/>");
        for (int r = 0; r < s.rows.size(); r++)
            b.append("<line class=\"sw-radar-spoke\" x1=\"160\" y1=\"160\" x2=\"").append(num(x(r, s.rows.size(), RADIUS)))
                    .append("\" y2=\"").append(num(y(r, s.rows.size(), RADIUS))).append("\"/>");
    }

    private void renderHits(StringBuilder b, Snapshot s) {
        int count = s.rows.size();
        for (int r = 0; r < count; r++) {
            double start = angle(r, count) - Math.PI / count, end = angle(r, count) + Math.PI / count;
            b.append("<path class=\"sw-chart-hit\" data-index=\"").append(r).append("\" d=\"M160,160 L")
                    .append(num(CENTER + Math.cos(start) * 132)).append(',').append(num(CENTER + Math.sin(start) * 132))
                    .append(" A132,132 0 0 1 ").append(num(CENTER + Math.cos(end) * 132)).append(',')
                    .append(num(CENTER + Math.sin(end) * 132)).append(" Z\"><title>").append(escape(nativeTip(s, r))).append("</title></path>");
        }
    }

    private void renderSeries(StringBuilder b, Snapshot s, int index) {
        Style part = s.series.get(index);
        boolean complete = s.rows.stream().allMatch(row -> row.values.get(index) != null);
        b.append("<g class=\"sw-chart-series-").append(part.color).append("\" data-series=\"").append(index).append("\">");
        if (complete) {
            StringJoiner points = new StringJoiner(" ");
            for (int r = 0; r < s.rows.size(); r++) {
                double radius = radius(s.rows.get(r).values.get(index), s.maximum);
                points.add(num(x(r, s.rows.size(), radius)) + "," + num(y(r, s.rows.size(), radius)));
            }
            b.append("<polygon class=\"sw-radar-area\" fill-opacity=\"").append(num(part.opacity)).append("\" points=\"").append(points).append("\"/>")
                    .append("<polygon class=\"sw-radar-line\" points=\"").append(points).append('"');
            stroke(b, part);
            b.append("/>");
        } else {
            for (int r = 0; r < s.rows.size(); r++) {
                int next = (r + 1) % s.rows.size();
                BigDecimal from = s.rows.get(r).values.get(index), to = s.rows.get(next).values.get(index);
                if (from == null || to == null) continue;
                b.append("<line class=\"sw-radar-line\" x1=\"").append(num(x(r, s.rows.size(), radius(from, s.maximum))))
                        .append("\" y1=\"").append(num(y(r, s.rows.size(), radius(from, s.maximum))))
                        .append("\" x2=\"").append(num(x(next, s.rows.size(), radius(to, s.maximum))))
                        .append("\" y2=\"").append(num(y(next, s.rows.size(), radius(to, s.maximum)))).append('"');
                stroke(b, part);
                b.append("/>");
            }
        }
        b.append("</g>");
    }

    private static void stroke(StringBuilder b, Style part) {
        b.append(" stroke-width=\"").append(num(part.width)).append('"');
        if (part.dashed) b.append(" stroke-dasharray=\"5 5\"");
    }

    private void renderDots(StringBuilder b, Snapshot s) {
        b.append("<svg class=\"sw-radar-points\" width=\"100%\" height=\"100%\" aria-hidden=\"true\" focusable=\"false\">");
        for (int i = 0; i < s.series.size(); i++) {
            Style part = s.series.get(i);
            if (!part.dots) continue;
            for (int r = 0; r < s.rows.size(); r++) {
                BigDecimal value = s.rows.get(r).values.get(i);
                if (value == null) continue;
                double radius = radius(value, s.maximum);
                b.append("<circle class=\"sw-radar-dot sw-chart-series-").append(part.color).append("\" data-series=\"").append(i)
                        .append("\" data-index=\"").append(r).append("\" cx=\"").append(num(x(r, s.rows.size(), radius) / 3.2))
                        .append("%\" cy=\"").append(num(y(r, s.rows.size(), radius) / 3.2)).append("%\" r=\"3\"/>");
            }
        }
        b.append("</svg>");
    }

    private void renderNumbers(StringBuilder b, Snapshot s) {
        b.append("<svg class=\"sw-radar-labels\" width=\"100%\" height=\"100%\" aria-hidden=\"true\" focusable=\"false\">");
        for (boolean rtl : new boolean[]{false, true}) {
            b.append("<g data-radar-dir=\"").append(rtl ? "rtl" : "ltr").append("\">");
            for (int r = 0; r < s.rows.size(); r++) {
                double position = x(r, s.rows.size(), LABEL_RADIUS) / 3.2;
                b.append("<text class=\"sw-radar-number\" x=\"").append(num(rtl ? 100 - position : position)).append("%\" y=\"")
                        .append(num(y(r, s.rows.size(), LABEL_RADIUS) / 3.2)).append("%\" dy=\".35em\" text-anchor=\"middle\">")
                        .append(r + 1).append("</text>");
            }
            b.append("</g>");
        }
        b.append("</svg>");
    }

    private void renderScale(StringBuilder b, Snapshot s, String id) {
        b.append("<p class=\"sw-radar-scale\" id=\"").append(id).append("-scale\">").append(escape(getString("SwRadarChart.scale")))
                .append(": ").append(escape(s.zeroText)).append(" – ").append(escape(s.maximumText)).append("</p>");
    }

    private void renderDimensionKey(StringBuilder b, Snapshot s) {
        b.append("<ol class=\"sw-radar-dimensions\" data-chart-key aria-label=\"").append(escape(getString("SwRadarChart.dimensions"))).append("\">");
        for (int r = 0; r < s.rows.size(); r++)
            b.append("<li class=\"sw-radar-dimension\" data-chart-index=\"").append(r).append("\"><span class=\"sw-radar-index\" aria-hidden=\"true\">")
                    .append(r + 1).append("</span><span class=\"sw-radar-name\">").append(escape(s.rows.get(r).category)).append("</span></li>");
        b.append("</ol>");
    }

    private void renderLegend(StringBuilder b, Snapshot s) {
        b.append("<ul class=\"sw-chart-legend-content\" aria-label=\"").append(escape(getString("SwRadarChart.series"))).append("\">");
        for (Style part : s.series)
            b.append("<li class=\"sw-chart-legend-item sw-chart-series-").append(part.color)
                    .append("\"><span class=\"sw-chart-legend-indicator\" aria-hidden=\"true\"")
                    .append(part.dashed ? " data-dashed=\"true\"" : "").append("></span>").append(escape(part.label)).append("</li>");
        b.append("</ul>");
    }

    private void renderTable(StringBuilder b, Snapshot s, String id) {
        b.append("<details class=\"sw-chart-data\"><summary>").append(escape(getString("SwRadarChart.data")))
                .append("</summary><div class=\"sw-chart-table-scroll\" tabindex=\"0\" role=\"region\" aria-labelledby=\"").append(id)
                .append("-title\"><table class=\"sw-table\"><caption class=\"sw-chart-sr-only\">").append(escape(s.title))
                .append("</caption><thead><tr><th scope=\"col\">").append(escape(s.categoryHeader)).append("</th>");
        for (Style part : s.series)
            b.append("<th scope=\"col\" data-color=\"sw-chart-series-").append(part.color).append('"')
                    .append(part.dashed ? " data-dashed=\"true\"" : "").append('>').append(escape(part.label)).append("</th>");
        b.append("</tr></thead><tbody>");
        for (Row row : s.rows) {
            b.append("<tr><th scope=\"row\">").append(escape(row.category)).append("</th>");
            for (int i = 0; i < s.series.size(); i++) {
                b.append("<td");
                if (row.values.get(i) == null) b.append(" data-tooltip-value=\"").append(escape(s.missing)).append('"');
                b.append('>').append(escape(row.text.get(i))).append("</td>");
            }
            b.append("</tr>");
        }
        b.append("</tbody></table></div></details>");
    }

    private static String nativeTip(Snapshot s, int row) {
        StringJoiner tip = new StringJoiner("; ");
        tip.add(s.rows.get(row).category);
        for (int i = 0; i < s.series.size(); i++)
            tip.add(s.series.get(i).label + ": " + (s.rows.get(row).values.get(i) == null ? s.missing : s.rows.get(row).text.get(i)));
        return tip.toString();
    }

    @Override public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptHeaderItem.forReference(SwChart.CHART_JS));
    }

    @Override protected void onDetach() {
        snapshot = null;
        data.detach();
        title.detach();
        if (categoryHeader != null) categoryHeader.detach();
        if (maximum != null) maximum.detach();
        series.forEach(part -> part.label.detach());
        super.onDetach();
    }
}

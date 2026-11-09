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

/** Concentric radial bars on a shared zero-based scale, with an exact table and optional exploration. */
public class SwRadialChart<T> extends Panel {
    private static final double CENTER = 160, OUTER_RADIUS = 136;
    private final IModel<? extends List<T>> data;
    private final SerializableFunction<T, String> category;
    private final SerializableFunction<T, ? extends Number> value;
    private IModel<String> title = Model.of("Chart"), categoryHeader, valueHeader;
    private IModel<? extends Number> maximum;
    private SerializableFunction<T, Integer> color;
    private SwChart.ValueFormat namedFormat = SwChart.ValueFormat.NUMBER;
    private SerializableBiFunction<Number, Locale, String> valueFormat;
    private Currency currency;
    private double startAngle = -90, sweepAngle = 360, innerRadius = .35, gap = .2;
    private boolean rounded, tracks = true, legend = true, tooltip = true;
    private transient Snapshot snapshot;
    private record Row(String category, BigDecimal value, String text, int color) implements Serializable {}
    private record Snapshot(String title, String categoryHeader, String valueHeader, List<Row> rows,
                            BigDecimal maximum, String zeroText, String maximumText, String missing, boolean measured) implements Serializable {}

    public SwRadialChart(String id, IModel<? extends List<T>> data, SerializableFunction<T, String> category,
                         SerializableFunction<T, ? extends Number> value) {
        super(id);
        this.data = Objects.requireNonNull(data);
        this.category = Objects.requireNonNull(category);
        this.value = Objects.requireNonNull(value);
        setOutputMarkupId(true);
    }

    @Override protected void onInitialize() {
        super.onInitialize();
        add(figure());
    }

    private WebMarkupContainer figure() {
        var figure = new WebMarkupContainer("figure");
        figure.add(new StyledBehavior("sw-chart sw-radial-chart"));
        figure.setOutputMarkupId(true);
        figure.add(new Label("content", (IModel<String>) () -> renderChart(figure.getMarkupId())).setEscapeModelStrings(false));
        return figure;
    }

    public SwRadialChart<T> setTitle(String value) { return setTitle(Model.of(value)); }
    public SwRadialChart<T> setTitle(IModel<String> value) { title = Objects.requireNonNull(value); return this; }
    public SwRadialChart<T> setCategoryHeader(String value) { return setCategoryHeader(Model.of(value)); }
    public SwRadialChart<T> setCategoryHeader(IModel<String> value) { categoryHeader = Objects.requireNonNull(value); return this; }
    public SwRadialChart<T> setValueHeader(String value) { return setValueHeader(Model.of(value)); }
    public SwRadialChart<T> setValueHeader(IModel<String> value) { valueHeader = Objects.requireNonNull(value); return this; }
    public SwRadialChart<T> setMaximum(Number value) { return setMaximum(Model.of(Objects.requireNonNull(value))); }
    public SwRadialChart<T> setMaximum(IModel<? extends Number> value) {
        Objects.requireNonNull(value);
        if (maximum != null && maximum != value) maximum.detach();
        maximum = value;
        return this;
    }
    public SwRadialChart<T> setAutoScale() {
        if (maximum != null) maximum.detach();
        maximum = null;
        return this;
    }
    /** Clockwise degrees from three o'clock, from -360 to 360; default -90 (top). */
    public SwRadialChart<T> setStartAngle(double value) {
        if (!Double.isFinite(value) || value < -360 || value > 360)
            throw new IllegalArgumentException("Radial start angle must be between -360 and 360 degrees");
        startAngle = value;
        return this;
    }
    /** Clockwise sweep in degrees, greater than zero and at most 360; default a full circle. */
    public SwRadialChart<T> setSweepAngle(double value) {
        if (!Double.isFinite(value) || value <= 0 || value > 360)
            throw new IllegalArgumentException("Radial sweep angle must be positive and at most 360 degrees");
        sweepAngle = value;
        return this;
    }
    /** Inner radius as a fraction of the outer radius, from 0 to 0.85; default 0.35. */
    public SwRadialChart<T> setInnerRadius(double value) {
        if (!Double.isFinite(value) || value < 0 || value > .85)
            throw new IllegalArgumentException("Radial inner radius must be between 0 and 0.85");
        innerRadius = value;
        return this;
    }
    /** Empty fraction of each ring's radial band, from 0 to 0.8; default 0.2. */
    public SwRadialChart<T> setGap(double value) {
        if (!Double.isFinite(value) || value < 0 || value > .8)
            throw new IllegalArgumentException("Radial gap must be between 0 and 0.8");
        gap = value;
        return this;
    }
    /** Palette token per input row: 1–5; zero or null uses its position's automatic token. */
    public SwRadialChart<T> setColor(SerializableFunction<T, Integer> value) { color = value; return this; }
    public SwRadialChart<T> setRounded(boolean value) { rounded = value; return this; }
    public SwRadialChart<T> setShowTracks(boolean value) { tracks = value; return this; }
    public SwRadialChart<T> setShowLegend(boolean value) { legend = value; return this; }
    public SwRadialChart<T> setShowTooltip(boolean value) { tooltip = value; return this; }
    public SwRadialChart<T> setValueFormat(SwChart.ValueFormat value) {
        namedFormat = Objects.requireNonNull(value);
        valueFormat = null;
        return this;
    }
    public SwRadialChart<T> setValueFormat(SerializableBiFunction<Number, Locale, String> value) { valueFormat = value; return this; }
    public SwRadialChart<T> setCurrency(Currency value) {
        currency = Objects.requireNonNull(value);
        namedFormat = SwChart.ValueFormat.CURRENCY;
        valueFormat = null;
        return this;
    }

    private String format(Number value) { return ChartValues.format(value, getLocale(), namedFormat, currency, valueFormat); }
    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalStateException(message);
        return value;
    }
    private String header(IModel<String> model, String key) {
        return model == null ? getString("SwRadialChart." + key)
                : required(model.getObject(), "A radial chart needs a " + key + " header");
    }
    private static BigDecimal amount(Number value) {
        if (value == null) return null;
        double number = value.doubleValue();
        if (!Double.isFinite(number) || Math.abs(number) > 1e100)
            throw new IllegalArgumentException("Radial values must be finite, nonnegative and at most 1e100");
        BigDecimal result;
        try { result = value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString()); }
        catch (NumberFormatException ignored) { result = BigDecimal.valueOf(number); }
        if (result.signum() < 0 || result.compareTo(BigDecimal.ONE.scaleByPowerOfTen(100)) > 0)
            throw new IllegalArgumentException("Radial values must be finite, nonnegative and at most 1e100");
        return result;
    }

    private Snapshot snapshot() {
        if (snapshot != null) return snapshot;
        String name = required(title.getObject(), "A radial chart needs a title");
        String categoryName = header(categoryHeader, "category"), valueName = header(valueHeader, "value");
        List<Row> rows = readRows();
        BigDecimal largest = BigDecimal.ZERO;
        boolean measured = false;
        for (Row row : rows) if (row.value != null) {
            measured = true;
            largest = largest.max(row.value);
        }
        Number configured = maximum == null ? null : maximum.getObject();
        BigDecimal ceiling = maximum == null ? automaticMaximum(largest) : amount(configured);
        if (ceiling == null || ceiling.signum() <= 0) throw new IllegalArgumentException("Radial maximum must be positive");
        if (largest.compareTo(ceiling) > 0) throw new IllegalArgumentException("Radial values must not exceed the configured maximum");
        return snapshot = new Snapshot(name, categoryName, valueName, List.copyOf(rows), ceiling,
                format(BigDecimal.ZERO), format(maximum == null ? ceiling : configured), getString("SwRadialChart.missing"), measured);
    }

    private List<Row> readRows() {
        List<Row> rows = new ArrayList<>();
        List<T> items = data.getObject();
        if (items == null) return rows;
        for (T item : items) {
            String name = required(category.apply(item), "Every radial category needs a label");
            Number original = value.apply(item);
            BigDecimal number = amount(original);
            Integer token = color == null ? null : color.apply(item);
            if (token != null && (token < 0 || token > 5))
                throw new IllegalArgumentException("Radial color must be between 0 and 5");
            rows.add(new Row(name, number, format(original), token == null || token == 0 ? rows.size() % 5 + 1 : token));
        }
        return rows;
    }

    private static BigDecimal automaticMaximum(BigDecimal largest) {
        if (largest.signum() == 0) return BigDecimal.ONE;
        if (largest.doubleValue() == 0) return largest;
        double[] ticks = SwChart.ticks(0, largest.doubleValue());
        return BigDecimal.valueOf(ticks[ticks.length - 1]).max(largest);
    }
    private static String escape(String value) { return Strings.escapeMarkup(Objects.toString(value, "")).toString(); }
    private static String num(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite radial coordinate");
        return String.format(Locale.ROOT, "%.5f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
    private static String point(double radius, double degrees) {
        double angle = Math.toRadians(degrees);
        return num(CENTER + Math.cos(angle) * radius) + "," + num(CENTER + Math.sin(angle) * radius);
    }
    private String arc(double radius, double sweep) {
        StringBuilder path = new StringBuilder("M").append(point(radius, startAngle));
        // Two arcs also represent a full circle, for which SVG's coincident endpoints otherwise draw nothing.
        double half = sweep / 2;
        for (int step = 1; step <= 2; step++)
            path.append(" A").append(num(radius)).append(',').append(num(radius)).append(" 0 0 1 ")
                    .append(point(radius, startAngle + half * step));
        return path.toString();
    }

    private String renderChart(String id) {
        Snapshot s = snapshot();
        StringBuilder b = new StringBuilder("<figcaption class=\"sw-chart-title\" id=\"").append(id).append("-title\">")
                .append(escape(s.title)).append("</figcaption>");
        if (s.rows.isEmpty()) emptyState(b, "empty");
        else if (!s.measured) emptyState(b, "noValues");
        else {
            renderPlot(b, s, id);
            renderScale(b, s, id);
            if (legend) renderLegend(b, s);
        }
        if (!s.rows.isEmpty()) renderTable(b, s, id);
        return b.toString();
    }
    private void emptyState(StringBuilder b, String key) {
        b.append("<p class=\"sw-chart-empty\" role=\"status\">").append(escape(getString("SwRadialChart." + key))).append("</p>");
    }
    private void renderPlot(StringBuilder b, Snapshot s, String id) {
        b.append("<p class=\"sw-chart-sr-only\" id=\"").append(id).append("-help\">")
                .append(escape(getString(tooltip ? "SwRadialChart.instructions" : "SwRadialChart.dataInstructions"))).append("</p>")
                .append("<div class=\"sw-chart-plot sw-radial-plot\" role=\"group\" aria-labelledby=\"").append(id)
                .append("-title\" aria-describedby=\"").append(id).append("-help ").append(id)
                .append("-scale\" data-orientation=\"radial\" data-tooltip=\"").append(tooltip).append("\">")
                .append("<svg class=\"sw-radial-svg\" viewBox=\"0 0 320 320\" aria-hidden=\"true\">");
        double band = OUTER_RADIUS * (1 - innerRadius) / s.rows.size();
        for (int i = 0; i < s.rows.size(); i++) renderRing(b, s, i, OUTER_RADIUS - band * (i + .5), band);
        b.append("</svg><div class=\"sw-chart-tooltip-content\" role=\"tooltip\" hidden></div></div>");
    }
    private void renderRing(StringBuilder b, Snapshot s, int index, double radius, double band) {
        Row row = s.rows.get(index);
        String track = arc(radius, sweepAngle), width = num(band * (1 - gap));
        b.append("<g class=\"sw-chart-series-").append(row.color).append("\" data-ring=\"").append(index).append("\">");
        if (tracks) b.append("<path class=\"sw-radial-track\" d=\"").append(track).append("\" stroke-width=\"").append(width).append("\"/>");
        b.append("<path class=\"sw-chart-hit sw-radial-hit\" data-index=\"").append(index).append("\" d=\"").append(track)
                .append("\" stroke-width=\"").append(num(band)).append("\"><title>").append(escape(row.category + "; " + s.valueHeader + ": "
                        + (row.value == null ? s.missing : row.text))).append("</title></path>");
        if (row.value != null && row.value.signum() > 0) {
            double ratio = row.value.divide(s.maximum, MathContext.DECIMAL128).doubleValue();
            b.append("<path class=\"sw-radial-bar\" d=\"").append(arc(radius, sweepAngle * ratio))
                    .append("\" stroke-width=\"").append(width).append("\" stroke-linecap=\"").append(rounded ? "round" : "butt").append("\"/>");
        }
        b.append("</g>");
    }
    private void renderScale(StringBuilder b, Snapshot s, String id) {
        b.append("<p class=\"sw-radial-scale\" id=\"").append(id).append("-scale\">").append(escape(getString("SwRadialChart.scale")))
                .append(": ").append(escape(s.zeroText)).append(" – ").append(escape(s.maximumText)).append("</p>");
    }
    private void renderLegend(StringBuilder b, Snapshot s) {
        b.append("<ul class=\"sw-radial-legend\" data-chart-key aria-label=\"").append(escape(getString("SwRadialChart.legend"))).append("\">");
        for (int i = 0; i < s.rows.size(); i++) {
            Row row = s.rows.get(i);
            b.append("<li class=\"sw-radial-legend-item sw-chart-series-").append(row.color).append("\" data-chart-index=\"").append(i)
                    .append("\"><span class=\"sw-chart-legend-indicator\" aria-hidden=\"true\"></span><span class=\"sw-radial-name\">")
                    .append(escape(row.category)).append("</span><span class=\"sw-radial-value\">")
                    .append(escape(row.value == null ? s.missing : row.text)).append("</span></li>");
        }
        b.append("</ul>");
    }
    private void renderTable(StringBuilder b, Snapshot s, String id) {
        b.append("<details class=\"sw-chart-data\"><summary>").append(escape(getString("SwRadialChart.data")))
                .append("</summary><div class=\"sw-chart-table-scroll\" tabindex=\"0\" role=\"region\" aria-labelledby=\"").append(id)
                .append("-title\"><table class=\"sw-table\"><caption class=\"sw-chart-sr-only\">").append(escape(s.title))
                .append("</caption><thead><tr><th scope=\"col\">").append(escape(s.categoryHeader))
                .append("</th><th scope=\"col\" data-color=\"sw-chart-series-1\">").append(escape(s.valueHeader)).append("</th></tr></thead><tbody>");
        for (Row row : s.rows) {
            b.append("<tr><th scope=\"row\">").append(escape(row.category)).append("</th><td data-color=\"sw-chart-series-").append(row.color).append('"');
            if (row.value == null) b.append(" data-tooltip-value=\"").append(escape(s.missing)).append('"');
            b.append('>').append(escape(row.text)).append("</td></tr>");
        }
        b.append("</tbody></table></div></details>");
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
        if (valueHeader != null) valueHeader.detach();
        if (maximum != null) maximum.detach();
        super.onDetach();
    }
}

package dev.jbaby.shadwick.components.chart;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.util.SerializableBiFunction;
import java.io.Serializable;
import java.util.*;
import org.apache.wicket.markup.head.*;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.string.Strings;

/** Native numeric X/Y plots with an exact table and optional point exploration. */
public class SwScatterChart extends Panel {
    public static final JavaScriptResourceReference SCATTER_JS =
            new JavaScriptResourceReference(Shadwick.class, "js/scatter-chart.js");
    private final List<ScatterSeries<?>> series = new ArrayList<>();
    private IModel<String> title = Model.of("Chart"), xLabel = Model.of("X"), yLabel = Model.of("Y"), pointHeader;
    private Formatting xFormat = new Formatting(SwChart.ValueFormat.NUMBER, null, null);
    private Formatting yFormat = new Formatting(SwChart.ValueFormat.NUMBER, null, null);
    private boolean grid = true, xAxis = true, yAxis = true, legend = true, tooltip = true, includeZero;
    private int xTickCount = 5, yTickCount = 5;
    private transient Snapshot snapshot;
    private record Formatting(SwChart.ValueFormat named, Currency currency,
                              SerializableBiFunction<Number, Locale, String> custom) implements Serializable {}
    private record Point(int series, String name, Double x, Double y, String xText, String yText) implements Serializable {
        boolean complete() { return x != null && y != null; }
    }
    private record SeriesStyle(String label, int color, double radius, ScatterSeries.Shape shape) implements Serializable {}
    private record Snapshot(String title, String xLabel, String yLabel, String pointHeader, List<SeriesStyle> series,
                            List<Point> rows, List<Integer> plotted, double[] xTicks, double[] yTicks) implements Serializable {}

    public SwScatterChart(String id) {
        super(id);
        setOutputMarkupId(true);
    }

    @Override protected void onInitialize() {
        super.onInitialize();
        add(figure());
    }

    private WebMarkupContainer figure() {
        var figure = new WebMarkupContainer("figure");
        figure.add(new StyledBehavior("sw-chart sw-scatter-chart"));
        figure.setOutputMarkupId(true);
        figure.add(new Label("content", (IModel<String>) () -> renderChart(figure.getMarkupId())).setEscapeModelStrings(false));
        return figure;
    }

    public SwScatterChart addSeries(ScatterSeries<?> value) {
        Objects.requireNonNull(value);
        if (series.contains(value)) throw new IllegalArgumentException("A scatter series must be registered exactly once");
        series.add(value);
        return this;
    }

    public SwScatterChart setTitle(String value) { return setTitle(Model.of(value)); }
    public SwScatterChart setTitle(IModel<String> value) { title = Objects.requireNonNull(value); return this; }
    public SwScatterChart setXLabel(String value) { return setXLabel(Model.of(value)); }
    public SwScatterChart setXLabel(IModel<String> value) { xLabel = Objects.requireNonNull(value); return this; }
    public SwScatterChart setYLabel(String value) { return setYLabel(Model.of(value)); }
    public SwScatterChart setYLabel(IModel<String> value) { yLabel = Objects.requireNonNull(value); return this; }
    public SwScatterChart setPointHeader(String value) { return setPointHeader(Model.of(value)); }
    public SwScatterChart setPointHeader(IModel<String> value) { pointHeader = Objects.requireNonNull(value); return this; }
    public SwScatterChart setXValueFormat(SwChart.ValueFormat value) {
        xFormat = new Formatting(Objects.requireNonNull(value), xFormat.currency, null); return this;
    }
    public SwScatterChart setYValueFormat(SwChart.ValueFormat value) {
        yFormat = new Formatting(Objects.requireNonNull(value), yFormat.currency, null); return this;
    }
    /** Null restores the default number format on X. */
    public SwScatterChart setXValueFormat(SerializableBiFunction<Number, Locale, String> value) {
        xFormat = new Formatting(SwChart.ValueFormat.NUMBER, null, value); return this;
    }
    /** Null restores the default number format on Y. */
    public SwScatterChart setYValueFormat(SerializableBiFunction<Number, Locale, String> value) {
        yFormat = new Formatting(SwChart.ValueFormat.NUMBER, null, value); return this;
    }
    public SwScatterChart setXCurrency(Currency value) {
        xFormat = new Formatting(SwChart.ValueFormat.CURRENCY, Objects.requireNonNull(value), null); return this;
    }
    public SwScatterChart setYCurrency(Currency value) {
        yFormat = new Formatting(SwChart.ValueFormat.CURRENCY, Objects.requireNonNull(value), null); return this;
    }
    public SwScatterChart setXTickCount(int value) { xTickCount = tickCount(value); return this; }
    public SwScatterChart setYTickCount(int value) { yTickCount = tickCount(value); return this; }
    public SwScatterChart setIncludeZero(boolean value) { includeZero = value; return this; }
    public SwScatterChart setShowGrid(boolean value) { grid = value; return this; }
    public SwScatterChart setShowXAxis(boolean value) { xAxis = value; return this; }
    public SwScatterChart setShowYAxis(boolean value) { yAxis = value; return this; }
    public SwScatterChart setShowLegend(boolean value) { legend = value; return this; }
    public SwScatterChart setShowTooltip(boolean value) { tooltip = value; return this; }

    private static int tickCount(int value) {
        if (value < 2 || value > 20) throw new IllegalArgumentException("Scatter tick count must be between 2 and 20");
        return value;
    }

    private String format(Number value, Formatting formatting) {
        return ChartValues.format(value, getLocale(), formatting.named, formatting.currency, formatting.custom);
    }

    private Snapshot snapshot() {
        if (snapshot != null) return snapshot;
        String name = required(title.getObject(), "A scatter chart needs a title");
        String xName = required(xLabel.getObject(), "A scatter chart needs an X label");
        String yName = required(yLabel.getObject(), "A scatter chart needs a Y label");
        if (series.isEmpty()) throw new IllegalStateException("A scatter chart needs at least one series");
        List<SeriesStyle> styles = new ArrayList<>();
        List<Point> rows = new ArrayList<>();
        for (int i = 0; i < series.size(); i++) {
            ScatterSeries<?> part = series.get(i);
            String label = required(part.label.getObject(), "Every scatter series needs a label");
            styles.add(new SeriesStyle(label, part.color == 0 ? i % 5 + 1 : part.color, part.radius, part.shape));
            readSeries(part, i, rows);
        }
        List<Integer> plotted = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).complete()) plotted.add(i);
        String header = pointHeader == null ? getString("SwScatterChart.point")
                : required(pointHeader.getObject(), "A scatter chart needs a point header");
        return snapshot = new Snapshot(name, xName, yName, header, List.copyOf(styles), List.copyOf(rows), List.copyOf(plotted),
                domain(rows, plotted, true), domain(rows, plotted, false));
    }

    private <T> void readSeries(ScatterSeries<T> part, int index, List<Point> rows) {
        List<T> data = part.data.getObject();
        if (data == null) return;
        for (T item : data) {
            String name = required(part.name.apply(item), "Every scatter point needs a name");
            Number x = part.x.apply(item), y = part.y.apply(item);
            rows.add(new Point(index, name, coordinate(x), coordinate(y), format(x, xFormat), format(y, yFormat)));
        }
    }

    private static Double coordinate(Number value) {
        if (value == null) return null;
        double number = value.doubleValue();
        if (!Double.isFinite(number) || Math.abs(number) > 1e100)
            throw new IllegalArgumentException("Scatter coordinates must be finite, with magnitude at most 1e100");
        return number;
    }

    private double[] domain(List<Point> rows, List<Integer> plotted, boolean horizontal) {
        double minimum = Double.POSITIVE_INFINITY, maximum = Double.NEGATIVE_INFINITY;
        for (int i : plotted) {
            double value = horizontal ? rows.get(i).x : rows.get(i).y;
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
        }
        if (plotted.isEmpty()) return new double[]{0, 1};
        if (includeZero) { minimum = Math.min(0, minimum); maximum = Math.max(0, maximum); }
        if (minimum == maximum) {
            double padding = minimum == 0 ? 1 : Math.abs(minimum) * .1;
            if (padding == 0) padding = Math.ulp(minimum);
            minimum -= padding;
            maximum += padding;
        }
        return SwChart.ticks(minimum, maximum, horizontal ? xTickCount : yTickCount);
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalStateException(message);
        return value;
    }
    private static String escape(String value) { return Strings.escapeMarkup(Objects.toString(value, "")).toString(); }
    private static String num(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite scatter coordinate");
        return String.format(Locale.ROOT, "%.5f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
    private static double fraction(double value, double[] ticks) {
        return (value - ticks[0]) / (ticks[ticks.length - 1] - ticks[0]);
    }

    private String renderChart(String id) {
        Snapshot s = snapshot();
        StringBuilder b = new StringBuilder("<figcaption class=\"sw-chart-title\" id=\"")
                .append(id).append("-title\">").append(escape(s.title)).append("</figcaption>");
        if (s.plotted.isEmpty()) {
            b.append("<p class=\"sw-chart-empty\" role=\"status\">")
                    .append(escape(getString(s.rows.isEmpty() ? "SwScatterChart.empty" : "SwScatterChart.noPairs"))).append("</p>");
        } else renderPlot(b, s, id);
        if (legend) renderLegend(b, s);
        if (!s.rows.isEmpty()) renderTable(b, s, id);
        return b.toString();
    }

    private void renderPlot(StringBuilder b, Snapshot s, String id) {
        b.append("<p class=\"sw-chart-sr-only\" id=\"").append(id).append("-help\">")
                .append(escape(getString(tooltip ? "SwScatterChart.instructions" : "SwScatterChart.dataInstructions"))).append("</p>");
        if (yAxis) b.append("<p class=\"sw-scatter-y-label\" aria-hidden=\"true\">").append(escape(s.yLabel)).append("</p>");
        b.append("<div class=\"sw-chart-container\" data-y-axis=\"").append(yAxis).append("\">");
        if (yAxis) renderYAxis(b, s);
        b.append("<div class=\"sw-chart-plot sw-scatter-plot\" role=\"group\" aria-labelledby=\"").append(id)
                .append("-title\" aria-describedby=\"").append(id).append("-help\" data-row-count=\"").append(s.rows.size())
                .append("\" data-point-count=\"").append(s.plotted.size()).append("\" data-tooltip=\"").append(tooltip)
                .append("\"><div class=\"sw-scatter-surface\">");
        renderGrid(b, s);
        renderPoints(b, s);
        b.append("</div><div class=\"sw-chart-tooltip-content\" role=\"tooltip\" hidden></div></div>");
        if (xAxis) {
            if (yAxis) b.append("<span aria-hidden=\"true\"></span>");
            renderXAxis(b, s);
        }
        b.append("</div>");
    }

    private void renderYAxis(StringBuilder b, Snapshot s) {
        b.append("<div class=\"sw-chart-y-axis sw-scatter-y-axis\" aria-hidden=\"true\">");
        for (int i = s.yTicks.length - 1; i >= 0; i--)
            b.append("<span>").append(escape(format(s.yTicks[i], yFormat))).append("</span>");
        b.append("</div>");
    }

    private void renderXAxis(StringBuilder b, Snapshot s) {
        b.append("<div aria-hidden=\"true\"><div class=\"sw-scatter-x-axis\">");
        for (double tick : s.xTicks) b.append("<span><span>").append(escape(format(tick, xFormat))).append("</span></span>");
        b.append("</div><p class=\"sw-scatter-x-label\">").append(escape(s.xLabel)).append("</p></div>");
    }

    private void renderGrid(StringBuilder b, Snapshot s) {
        b.append("<svg class=\"sw-scatter-grid-svg\" viewBox=\"0 0 1000 240\" preserveAspectRatio=\"none\" aria-hidden=\"true\">");
        if (grid) {
            for (double tick : s.xTicks) {
                String x = num(fraction(tick, s.xTicks) * 1000);
                b.append("<line class=\"sw-chart-grid\" x1=\"").append(x).append("\" x2=\"").append(x).append("\" y1=\"0\" y2=\"240\"/>");
            }
            for (double tick : s.yTicks) {
                String y = num(240 * (1 - fraction(tick, s.yTicks)));
                b.append("<line class=\"sw-chart-grid\" x1=\"0\" x2=\"1000\" y1=\"").append(y).append("\" y2=\"").append(y).append("\"/>");
            }
        }
        b.append("</svg>");
    }

    private void renderPoints(StringBuilder b, Snapshot s) {
        b.append("<svg class=\"sw-scatter-points\" width=\"100%\" height=\"240\" aria-hidden=\"true\" focusable=\"false\">");
        for (int row : s.plotted) {
            Point point = s.rows.get(row);
            SeriesStyle part = s.series.get(point.series);
            b.append("<svg class=\"sw-scatter-mark sw-chart-series-").append(part.color).append("\" data-index=\"").append(row)
                    .append("\" x=\"").append(num(100 * fraction(point.x, s.xTicks))).append("%\" y=\"")
                    .append(num(240 * (1 - fraction(point.y, s.yTicks)))).append("\" width=\"1\" height=\"1\" overflow=\"visible\">");
            renderMarker(b, part.shape, part.radius);
            b.append("</svg>");
        }
        for (int row : s.plotted) {
            Point point = s.rows.get(row);
            SeriesStyle part = s.series.get(point.series);
            b.append("<circle class=\"sw-scatter-hit\" data-index=\"").append(row).append("\" cx=\"")
                    .append(num(100 * fraction(point.x, s.xTicks))).append("%\" cy=\"")
                    .append(num(240 * (1 - fraction(point.y, s.yTicks)))).append("\" r=\"").append(num(Math.max(12, part.radius + 3)))
                    .append("\"><title>").append(escape(point.name + "; " + part.label + "; " + s.xLabel + ": " + point.xText
                            + "; " + s.yLabel + ": " + point.yText)).append("</title></circle>");
        }
        b.append("</svg>");
    }

    private static void renderMarker(StringBuilder b, ScatterSeries.Shape shape, double size) {
        String radius = num(size);
        switch (shape) {
            case CIRCLE -> b.append("<circle class=\"sw-scatter-point\" cx=\"0\" cy=\"0\" r=\"").append(radius).append("\"/>");
            case SQUARE -> b.append("<rect class=\"sw-scatter-point\" x=\"-").append(radius).append("\" y=\"-").append(radius)
                    .append("\" width=\"").append(num(size * 2)).append("\" height=\"").append(num(size * 2)).append("\"/>");
            case DIAMOND -> b.append("<path class=\"sw-scatter-point\" d=\"M0,-").append(radius).append(" L").append(radius)
                    .append(",0 L0,").append(radius).append(" L-").append(radius).append(",0 Z\"/>");
        }
    }

    private void renderLegend(StringBuilder b, Snapshot s) {
        b.append("<ul class=\"sw-chart-legend-content\" aria-label=\"").append(escape(getString("SwScatterChart.series"))).append("\">");
        for (SeriesStyle part : s.series) {
            b.append("<li class=\"sw-chart-legend-item sw-chart-series-").append(part.color)
                    .append("\"><svg class=\"sw-scatter-legend-mark\" viewBox=\"-14 -14 28 28\" aria-hidden=\"true\">");
            renderMarker(b, part.shape, 10);
            b.append("</svg><span>").append(escape(part.label)).append("</span></li>");
        }
        b.append("</ul>");
    }

    private void renderTable(StringBuilder b, Snapshot s, String id) {
        b.append("<details class=\"sw-chart-data\"><summary>").append(escape(getString("SwScatterChart.data")))
                .append("</summary><div class=\"sw-chart-table-scroll\" tabindex=\"0\" role=\"region\" aria-labelledby=\"")
                .append(id).append("-title\"><table class=\"sw-table\"><caption class=\"sw-chart-sr-only\">")
                .append(escape(s.title)).append("</caption><thead><tr><th scope=\"col\">").append(escape(s.pointHeader))
                .append("</th><th scope=\"col\">").append(escape(getString("SwScatterChart.series")))
                .append("</th><th scope=\"col\">").append(escape(s.xLabel)).append("</th><th scope=\"col\">")
                .append(escape(s.yLabel)).append("</th></tr></thead><tbody>");
        for (Point row : s.rows) {
            SeriesStyle part = s.series.get(row.series);
            b.append("<tr data-color=\"sw-chart-series-").append(part.color).append("\"><th scope=\"row\">").append(escape(row.name))
                    .append("</th><td>").append(escape(part.label)).append("</td><td>").append(escape(row.xText))
                    .append("</td><td>").append(escape(row.yText)).append("</td></tr>");
        }
        b.append("</tbody></table></div></details>");
    }

    @Override public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptHeaderItem.forReference(SCATTER_JS));
    }

    @Override protected void onDetach() {
        snapshot = null;
        title.detach(); xLabel.detach(); yLabel.detach();
        if (pointHeader != null) pointHeader.detach();
        series.forEach(ScatterSeries::detach);
        super.onDetach();
    }
}

package dev.jbaby.shadwick.components.chart;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.util.*;
import java.io.Serializable;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.*;
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
    private final IModel<? extends List<T>> data;
    private final SerializableFunction<T, ?> category;
    private final List<ChartSeries<T>> series = new ArrayList<>();
    private IModel<String> title = Model.of("Chart"), categoryHeader = Model.of("Category");
    private SerializableBiFunction<Number, Locale, String> valueFormat;
    private SerializableBiFunction<Object, Locale, String> categoryFormat;
    private ValueFormat namedFormat = ValueFormat.NUMBER;
    private Currency currency;
    private ZoneId timeZone = ZoneOffset.UTC;
    private boolean grid = true, xAxis = true, yAxis = true, legend = true, tooltip = true;
    private transient Snapshot snapshot;
    private record Row(String category, List<Double> values, List<String> text) implements Serializable {}
    private record Snapshot(List<Row> rows, List<String> labels, double[][] low, double[][] high, double[] ticks)
            implements Serializable {}
    public SwChart(String id, IModel<? extends List<T>> data, SerializableFunction<T, ?> category) {
        super(id);
        this.data = Objects.requireNonNull(data);
        this.category = Objects.requireNonNull(category);
        setOutputMarkupId(true);
        var figure = new WebMarkupContainer("figure");
        figure.add(new StyledBehavior("sw-chart"));
        figure.setOutputMarkupId(true);
        add(figure);
        figure.add(new Label("content", (IModel<String>) () -> renderChart(figure.getMarkupId()))
                        .setEscapeModelStrings(false));
    }

    public SwChart<T> addSeries(ChartSeries<T> value) {
        series.add(Objects.requireNonNull(value));
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

    private String category(Object value) {
        if (categoryFormat != null)
            return Objects.toString(categoryFormat.apply(value, getLocale()), "");
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
        if (value == null)
            return "";
        if (valueFormat != null)
            return Objects.toString(valueFormat.apply(value, getLocale()), "");
        NumberFormat f = switch (namedFormat) {
            case CURRENCY -> NumberFormat.getCurrencyInstance(getLocale());
            case PERCENT -> NumberFormat.getPercentInstance(getLocale());
            case COMPACT -> NumberFormat.getCompactNumberInstance(getLocale(), NumberFormat.Style.SHORT);
            case INTEGER -> NumberFormat.getIntegerInstance(getLocale());
            default -> NumberFormat.getNumberInstance(getLocale());
        };
        if (namedFormat == ValueFormat.CURRENCY) {
            if (currency == null)
                throw new IllegalStateException("Currency formatting requires setCurrency(Currency)");
            f.setCurrency(currency);
            f.setMinimumFractionDigits(currency.getDefaultFractionDigits());
            f.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        } else if (namedFormat != ValueFormat.INTEGER) {
            f.setMaximumFractionDigits(namedFormat == ValueFormat.COMPACT ? 1 : 2);
        }
        return f.format(value);
    }

    private Snapshot snapshot() {
        if (snapshot != null)
            return snapshot;
        if (title.getObject() == null || title.getObject().isBlank())
            throw new IllegalStateException("A chart needs a title for its accessible name");
        if (series.isEmpty())
            throw new IllegalStateException("A chart needs at least one series");
        var labels = series.stream().map(s -> Objects.toString(s.label.getObject(), "")).toList();
        if (labels.stream().anyMatch(String::isBlank))
            throw new IllegalStateException("Every chart series needs a label");
        var rows = new ArrayList<Row>();
        List<T> items = data.getObject();
        if (items != null)
            for (T item : items) {
                var values = new ArrayList<Double>();
                var text = new ArrayList<String>();
                for (var s : series) {
                    Number value = s.value.apply(item);
                    double d = value == null ? 0 : value.doubleValue();
                    if (!Double.isFinite(d) || Math.abs(d) > 1e100)
                        throw new IllegalArgumentException("Chart values must be finite, with magnitude at most 1e100");
                    values.add(value == null ? null : d);
                    text.add(format(value));
                }
                rows.add(new Row(
                        category(category.apply(item)), Collections.unmodifiableList(values), List.copyOf(text)));
            }
        double[][] low = new double[series.size()][rows.size()], high = new double[series.size()][rows.size()];
        double min = 0, max = 0;
        for (int r = 0; r < rows.size(); r++) {
            Map<String, double[]> stacks = new HashMap<>();
            for (int s = 0; s < series.size(); s++) {
                Double v = rows.get(r).values.get(s);
                if (v == null)
                    continue;
                ChartSeries<T> part = series.get(s);
                double base = 0;
                if (part.stack != null && part.type != ChartSeries.Type.LINE) {
                    double[] sum = stacks.computeIfAbsent(part.type + ":" + part.stack, k -> new double[2]);
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
        return snapshot = new Snapshot(List.copyOf(rows), labels, low, high, ticks(min, max));
    }
    static double[] ticks(double min, double max) {
        if (min == max)
            max = min + 1;
        double raw = Math.max(Double.MIN_VALUE, (max - min) / 4);
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
        return "sw-chart-series-" + (series.get(s).color == 0 ? s % 5 + 1 : series.get(s).color);
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
            return b.toString();
        }
        b.append("<p class=\"sw-chart-sr-only\" id=\"")
                .append(id)
                .append("-help\">")
                .append(escape(tooltip ? getString("SwChart.instructions", null,
                                                 "Use Left and Right arrows to explore values, Home and End to jump, "
                                                 + "Escape to dismiss. Exact values are available in the data table.")
                                       : getString("SwChart.dataInstructions", null,
                                                 "Exact values are available in the data table.")))
                .append("</p>");
        b.append("<div class=\"sw-chart-container\" data-y-axis=\"").append(yAxis).append("\">");
        if (yAxis) {
            b.append("<div class=\"sw-chart-y-axis\" aria-hidden=\"true\">");
            for (int i = snap.ticks.length - 1; i >= 0; i--)
                b.append("<span>").append(escape(format(snap.ticks[i]))).append("</span>");
            b.append("</div>");
        }
        b.append("<div class=\"sw-chart-plot\" role=\"group\" aria-labelledby=\"")
                .append(id)
                .append("-title\" aria-describedby=\"")
                .append(id)
                .append("-help\" data-tooltip=\"")
                .append(tooltip)
                .append("\">");
        b.append("<svg class=\"sw-chart-svg\" viewBox=\"0 0 1000 240\" preserveAspectRatio=\"none\" role=\"img\" "
                 + "aria-label=\"")
                .append(name)
                .append("\">");
        if (grid)
            for (double tick : snap.ticks)
                b.append("<line class=\"sw-chart-grid\" x1=\"0\" x2=\"1000\" y1=\"")
                        .append(num(y(tick, snap.ticks)))
                        .append("\" y2=\"")
                        .append(num(y(tick, snap.ticks)))
                        .append("\"/>");
        // Bar stacks share a horizontal slot; unstacked series each receive one.
        List<String> barGroups = new ArrayList<>();
        for (int s = 0; s < series.size(); s++)
            if (series.get(s).type == ChartSeries.Type.BAR) {
                String group = series.get(s).stack == null ? "series:" + s : "stack:" + series.get(s).stack;
                if (!barGroups.contains(group))
                    barGroups.add(group);
            }
        for (int s = 0; s < series.size(); s++) {
            var part = series.get(s);
            String css = color(s);
            b.append("<g class=\"").append(css).append("\" data-series=\"").append(s).append("\">");
            if (part.type == ChartSeries.Type.BAR) {
                int slot = barGroups.indexOf(part.stack == null ? "series:" + s : "stack:" + part.stack);
                double width = 720.0 / count / barGroups.size();
                for (int r = 0; r < count; r++)
                    if (snap.rows.get(r).values.get(s) != null) {
                        double top = y(Math.max(snap.low[s][r], snap.high[s][r]), snap.ticks),
                               bottom = y(Math.min(snap.low[s][r], snap.high[s][r]), snap.ticks);
                        b.append("<rect class=\"sw-chart-bar\" x=\"")
                                .append(num((r + .14) * 1000 / count + slot * width))
                                .append("\" y=\"")
                                .append(num(top))
                                .append("\" width=\"")
                                .append(num(width * .9))
                                .append("\" height=\"")
                                .append(num(bottom - top))
                                .append("\" rx=\"4\"><title>")
                                .append(escape(snap.rows.get(r).category + ": " + snap.labels.get(s) + " "
                                        + snap.rows.get(r).text.get(s)))
                                .append("</title></rect>");
                    }
            } else {
                for (int start = 0; start < count;) {
                    while (start < count && snap.rows.get(start).values.get(s) == null) start++;
                    if (start == count)
                        break;
                    int end = start;
                    while (end + 1 < count && snap.rows.get(end + 1).values.get(s) != null) end++;
                    String path = path(snap, s, start, end, false, part.curve);
                    if (part.type == ChartSeries.Type.AREA) {
                        StringBuilder area =
                                new StringBuilder(path)
                                        .append(path(snap, s, end, start, true, part.curve).replaceFirst("^M", " L"))
                                        .append(" Z");
                        b.append("<path class=\"sw-chart-area\" fill-opacity=\"")
                                .append(num(part.opacity))
                                .append("\" d=\"")
                                .append(area)
                                .append("\"/>");
                    }
                    b.append("<path class=\"sw-chart-line\" d=\"")
                            .append(path)
                            .append("\"")
                            .append(part.dashed ? " stroke-dasharray=\"5 5\"" : "")
                            .append("/>");
                    if (part.dots || end == start)
                        for (int r = start; r <= end; r++)
                            b.append("<circle class=\"sw-chart-dot\" cx=\"")
                                    .append(num((r + .5) * 1000 / count))
                                    .append("\" cy=\"")
                                    .append(num(y(snap.high[s][r], snap.ticks)))
                                    .append("\" r=\"4\"/>");
                    start = end + 1;
                }
            }
            b.append("</g>");
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
        b.append("</svg><div class=\"sw-chart-tooltip-content\" role=\"tooltip\" hidden></div></div>");
        if (xAxis) {
            if (yAxis)
                b.append("<span aria-hidden=\"true\"></span>");
            b.append("<div class=\"sw-chart-x-axis\" aria-hidden=\"true\">");
            int every = Math.max(1, (int) Math.ceil(count / 8.0));
            for (int r = 0; r < count; r++)
                b.append("<span>").append(r % every == 0 ? escape(snap.rows.get(r).category) : "").append("</span>");
            b.append("</div>");
        }
        b.append("</div>");
        if (legend) {
            b.append("<ul class=\"sw-chart-legend-content\" aria-label=\"")
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
        b.append("<details class=\"sw-chart-data\"><summary>")
                .append(escape(getString("SwChart.data", null, "View data")))
                .append("</summary><div class=\"sw-chart-table-scroll\"><table class=\"sw-table\"><caption "
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
            b.append("<tr><th scope=\"row\">").append(escape(row.category)).append("</th>");
            for (String text : row.text) b.append("<td>").append(escape(text)).append("</td>");
            b.append("</tr>");
        }
        return b.append("</tbody></table></div></details>").toString();
    }

    private static String nativeTip(Snapshot snap, int row) {
        StringJoiner text = new StringJoiner("; ");
        text.add(snap.rows.get(row).category);
        for (int s = 0; s < snap.labels.size(); s++)
            if (!snap.rows.get(row).text.get(s).isEmpty())
                text.add(snap.labels.get(s) + ": " + snap.rows.get(row).text.get(s));
        return text.toString();
    }

    private static String path(Snapshot snap, int s, int start, int end, boolean lower, ChartSeries.Curve curve) {
        int count = snap.rows.size();
        StringBuilder b = new StringBuilder("M");
        double[][] values = lower ? snap.low : snap.high;
        int direction = start <= end ? 1 : -1;
        for (int r = start; direction > 0 ? r <= end : r >= end; r += direction) {
            double x = (r + .5) * 1000 / count, yy = y(values[s][r], snap.ticks);
            if (r == start)
                b.append(num(x)).append(',').append(num(yy));
            else if (curve == ChartSeries.Curve.STEP) {
                if (direction > 0)
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

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptHeaderItem.forReference(CHART_JS));
    }

    @Override
    protected void onDetach() {
        snapshot = null;
        data.detach();
        title.detach();
        categoryHeader.detach();
        series.forEach(s -> s.label.detach());
        super.onDetach();
    }
}

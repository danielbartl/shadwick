package dev.jbaby.shadwick.components.chart;

import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.util.*;
import java.io.Serializable;
import java.math.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.IntStream;
import org.apache.wicket.markup.head.*;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.string.Strings;

/** Server-rendered part-to-whole chart, with an exact table and optional keyboard/pointer tooltips. */
public class SwDonutChart<T> extends Panel {
    /** Part-to-whole geometry; both shapes share the same exact data and interactions. */
    public enum Shape { DONUT, PIE }

    public static final int DEFAULT_MAX_SEGMENTS = 5;
    public static final JavaScriptResourceReference DONUT_JS = new JavaScriptResourceReference(Shadwick.class, "js/donut-chart.js");
    private final IModel<? extends List<T>> data;
    private final SerializableFunction<T, String> category;
    private final SerializableFunction<T, ? extends Number> value;
    private IModel<String> title = Model.of("Chart"), categoryHeader, valueHeader, totalLabel;
    private SerializableBiFunction<Number, Locale, String> valueFormat;
    private SwChart.ValueFormat namedFormat = SwChart.ValueFormat.NUMBER;
    private Currency currency;
    private Shape shape = Shape.DONUT;
    private int maxSegments = DEFAULT_MAX_SEGMENTS;
    private boolean groupSmallSegments = true, legend = true, tooltip = true;
    private transient Snapshot snapshot;
    private record Row(String category, BigDecimal value, String text, String share) implements Serializable {}
    private record Part(String category, BigDecimal value, String text, String share, int color) implements Serializable {}
    private record Snapshot(String title, String categoryHeader, String valueHeader, String totalLabel,
                            List<Row> rows, List<Part> parts, BigDecimal total, String totalText, boolean grouped) implements Serializable {}

    public SwDonutChart(String id, IModel<? extends List<T>> data, SerializableFunction<T, String> category,
                        SerializableFunction<T, ? extends Number> value) {
        super(id);
        this.data = Objects.requireNonNull(data);
        this.category = Objects.requireNonNull(category);
        this.value = Objects.requireNonNull(value);
        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(figure());
    }

    private WebMarkupContainer figure() {
        var figure = new WebMarkupContainer("figure");
        figure.add(new StyledBehavior("sw-chart sw-donut-chart"));
        figure.setOutputMarkupId(true);
        figure.add(new Label("content", (IModel<String>) () -> renderChart(figure.getMarkupId())).setEscapeModelStrings(false));
        return figure;
    }

    /** Default DONUT shows a center total; PIE draws filled sectors with the total in the data table. */
    public SwDonutChart<T> setShape(Shape value) { shape = Objects.requireNonNull(value); return this; }

    public SwDonutChart<T> setTitle(String value) { return setTitle(Model.of(value)); }
    public SwDonutChart<T> setTitle(IModel<String> value) { title = Objects.requireNonNull(value); return this; }
    public SwDonutChart<T> setCategoryHeader(String value) { return setCategoryHeader(Model.of(value)); }
    public SwDonutChart<T> setCategoryHeader(IModel<String> value) { categoryHeader = Objects.requireNonNull(value); return this; }
    public SwDonutChart<T> setValueHeader(String value) { return setValueHeader(Model.of(value)); }
    public SwDonutChart<T> setValueHeader(IModel<String> value) { valueHeader = Objects.requireNonNull(value); return this; }
    public SwDonutChart<T> setTotalLabel(String value) { return setTotalLabel(Model.of(value)); }
    public SwDonutChart<T> setTotalLabel(IModel<String> value) { totalLabel = Objects.requireNonNull(value); return this; }
    public SwDonutChart<T> setValueFormat(SerializableBiFunction<Number, Locale, String> value) { valueFormat = value; return this; }
    public SwDonutChart<T> setValueFormat(SwChart.ValueFormat value) {
        namedFormat = Objects.requireNonNull(value); valueFormat = null; return this;
    }
    public SwDonutChart<T> setCurrency(Currency value) {
        currency = Objects.requireNonNull(value); namedFormat = SwChart.ValueFormat.CURRENCY; valueFormat = null; return this;
    }
    /** Keep this many positive parts, including Other when grouping is enabled. At least two; default five. */
    public SwDonutChart<T> setMaxSegments(int value) {
        if (value < 2) throw new IllegalArgumentException("maxSegments must be at least two");
        maxSegments = value; return this;
    }
    /** Disable to show every positive row. Colors repeat after the fifth part. */
    public SwDonutChart<T> setGroupSmallSegments(boolean value) { groupSmallSegments = value; return this; }
    public SwDonutChart<T> setShowLegend(boolean value) { legend = value; return this; }
    public SwDonutChart<T> setShowTooltip(boolean value) { tooltip = value; return this; }

    private String format(Number value) { return ChartValues.format(value, getLocale(), namedFormat, currency, valueFormat); }
    private String share(BigDecimal value, BigDecimal total) {
        if (value == null) return "";
        var percent = NumberFormat.getPercentInstance(getLocale());
        percent.setMaximumFractionDigits(1);
        return percent.format(total.signum() == 0 ? BigDecimal.ZERO : value.divide(total, MathContext.DECIMAL128));
    }
    private String label(IModel<String> model, String key) {
        return model == null ? getString("SwDonutChart." + key) : Objects.toString(model.getObject(), "");
    }
    private static BigDecimal amount(Number value) {
        if (value == null) return null;
        double finite = value.doubleValue();
        if (!Double.isFinite(finite) || Math.abs(finite) > 1e100)
            throw new IllegalArgumentException("Donut values must be finite, nonnegative and at most 1e100");
        BigDecimal result;
        try { result = value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString()); }
        catch (NumberFormatException ignored) { result = BigDecimal.valueOf(finite); }
        if (result.signum() < 0 || result.compareTo(BigDecimal.ONE.scaleByPowerOfTen(100)) > 0)
            throw new IllegalArgumentException("Donut values must be finite, nonnegative and at most 1e100");
        return result;
    }
    private Snapshot snapshot() {
        if (snapshot != null) return snapshot;
        String name = title.getObject();
        if (name == null || name.isBlank()) throw new IllegalStateException("A chart needs a title for its accessible name");
        List<Row> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        List<T> items = data.getObject();
        if (items != null) for (T item : items) {
            Number original = value.apply(item);
            BigDecimal number = amount(original);
            rows.add(new Row(Objects.toString(category.apply(item), ""), number, format(original), ""));
            if (number != null) total = total.add(number);
        }
        List<Row> exact = new ArrayList<>(), positive = new ArrayList<>();
        for (Row row : rows) {
            var complete = new Row(row.category, row.value, row.text, share(row.value, total));
            exact.add(complete);
            if (row.value != null && row.value.signum() > 0) positive.add(complete);
        }
        boolean grouped = groupSmallSegments && positive.size() > maxSegments;
        List<Row> shown = positive;
        if (grouped) {
            // Rank row indexes, not equality: identical rows must each contribute once.
            List<Integer> ranked = new ArrayList<>(IntStream.range(0, positive.size()).boxed().toList());
            ranked.sort(Comparator.comparing((Integer i) -> positive.get(i).value).reversed());
            Set<Integer> kept = new HashSet<>(ranked.subList(0, maxSegments - 1));
            shown = new ArrayList<>(); BigDecimal other = BigDecimal.ZERO;
            for (int i = 0; i < positive.size(); i++) {
                if (kept.contains(i)) shown.add(positive.get(i)); else other = other.add(positive.get(i).value);
            }
            shown.add(new Row(getString("SwDonutChart.other"), other, format(other), share(other, total)));
        }
        List<Part> parts = new ArrayList<>();
        for (int i = 0; i < shown.size(); i++) {
            Row row = shown.get(i);
            parts.add(new Part(row.category, row.value, row.text, row.share, i % 5 + 1));
        }
        return snapshot = new Snapshot(name, label(categoryHeader, "category"), label(valueHeader, "value"),
                label(totalLabel, "total"), List.copyOf(exact), List.copyOf(parts), total, format(total), grouped);
    }
    private static String escape(String value) { return Strings.escapeMarkup(Objects.toString(value, "")).toString(); }
    private static String num(double value) { return value == 0 ? "0" : Double.toString(value); }
    private String renderChart(String id) {
        Snapshot s = snapshot();
        String name = escape(s.title);
        StringBuilder b = new StringBuilder("<figcaption class=\"sw-chart-title\" id=\"").append(id).append("-title\">").append(name).append("</figcaption>");
        if (s.rows.isEmpty()) return b.append("<p class=\"sw-chart-empty\" role=\"status\">").append(escape(getString("SwDonutChart.empty"))).append("</p>").toString();
        appendPlot(b, s, id);
        if (s.parts.isEmpty()) b.append("<p class=\"sw-donut-note\" role=\"status\">").append(escape(getString("SwDonutChart.noPositiveValues"))).append("</p>");
        appendLegend(b, s);
        if (s.grouped) b.append("<p class=\"sw-donut-note\">").append(escape(getString("SwDonutChart.grouped"))).append("</p>");
        appendDataTable(b, s, id);
        return b.toString();
    }

    private void appendPlot(StringBuilder b, Snapshot s, String id) {
        b.append("<p class=\"sw-chart-sr-only\" id=\"").append(id).append("-help\">")
                .append(escape(getString(tooltip && !s.parts.isEmpty() ? "SwDonutChart.instructions" : "SwDonutChart.dataInstructions"))).append("</p>")
                .append("<div class=\"sw-chart-plot sw-donut-plot\" role=\"group\" aria-labelledby=\"").append(id).append("-title\" aria-describedby=\"")
                .append(id).append("-help\" data-shape=\"").append(shape == Shape.PIE ? "pie" : "donut")
                .append("\" data-tooltip=\"").append(tooltip).append("\">");
        b.append("<svg class=\"sw-donut-svg\" viewBox=\"0 0 240 240\" aria-hidden=\"true\">");
        if (shape == Shape.PIE) b.append("<circle class=\"sw-pie-track\" cx=\"120\" cy=\"120\" r=\"112\"/>");
        else b.append("<circle class=\"sw-donut-track\" cx=\"120\" cy=\"120\" r=\"96\"/>");
        double offset = 0;
        for (int i = 0; i < s.parts.size(); i++) {
            Part part = s.parts.get(i);
            double length = part.value.divide(s.total, MathContext.DECIMAL128).doubleValue() * 100;
            if (shape == Shape.PIE) appendPieSegment(b, part, i, offset, length, s.parts.size() == 1);
            else appendDonutSegment(b, part, i, offset, length, s.parts.size() > 1);
            offset += length;
        }
        b.append("</svg>");
        if (shape == Shape.DONUT) appendCenterTotal(b, s);
        b.append("<div class=\"sw-chart-tooltip-content\" role=\"tooltip\" hidden></div></div>");
    }

    private void appendDonutSegment(StringBuilder b, Part part, int index, double offset, double length, boolean separated) {
        double gap = separated ? Math.min(.6, length * .15) : 0;
        b.append("<circle class=\"sw-donut-segment sw-chart-series-").append(part.color).append("\" data-index=\"").append(index)
                    .append("\" cx=\"120\" cy=\"120\" r=\"96\" pathLength=\"100\" stroke-dasharray=\"").append(num(length - gap)).append(' ').append(num(100 - length + gap))
                    .append("\" stroke-dashoffset=\"").append(num(-offset - gap / 2)).append("\" transform=\"rotate(-90 120 120)\"><title>")
                    .append(escape(part.category + ": " + part.text + " (" + part.share + ")")).append("</title></circle>");
    }

    private void appendPieSegment(StringBuilder b, Part part, int index, double offset, double length, boolean whole) {
        b.append("<path class=\"sw-donut-segment sw-pie-segment sw-chart-series-").append(part.color)
                .append("\" data-index=\"").append(index).append("\" d=\"");
        if (length < 1e-12) {
            // Preserve the part in the legend without inventing a visible radial line for a rounded-away share.
            b.append("M120 120");
        } else if (whole || length >= 100) {
            // One SVG arc cannot describe a full circle when its two endpoints coincide.
            b.append("M120 8 A112 112 0 1 1 120 232 A112 112 0 1 1 120 8 Z");
        } else {
            double start = Math.toRadians(offset * 3.6 - 90);
            double end = Math.toRadians((offset + length) * 3.6 - 90);
            b.append("M120 120 L").append(num(120 + 112 * Math.cos(start))).append(' ').append(num(120 + 112 * Math.sin(start)))
                    .append(" A112 112 0 ").append(length > 50 ? '1' : '0').append(" 1 ")
                    .append(num(120 + 112 * Math.cos(end))).append(' ').append(num(120 + 112 * Math.sin(end))).append(" Z");
        }
        b.append("\"><title>").append(escape(part.category + ": " + part.text + " (" + part.share + ")")).append("</title></path>");
    }

    private void appendCenterTotal(StringBuilder b, Snapshot s) {
        String size = s.totalText.codePointCount(0, s.totalText.length()) > 14 ? "small" : s.totalText.length() > 8 ? "medium" : "large";
        b.append("<div class=\"sw-donut-center\"><span class=\"sw-donut-total\" data-size=\"").append(size).append("\" title=\"")
                .append(escape(s.totalText)).append("\">").append(escape(s.totalText)).append("</span><span class=\"sw-donut-total-label\">")
                .append(escape(s.totalLabel)).append("</span></div>");
    }

    private void appendLegend(StringBuilder b, Snapshot s) {
        b.append("<ul class=\"sw-donut-legend\" aria-label=\"").append(escape(getString("SwDonutChart.legend"))).append('"').append(legend ? "" : " hidden").append('>');
        for (int i = 0; i < s.parts.size(); i++) {
            Part part = s.parts.get(i);
            b.append("<li class=\"sw-donut-legend-item sw-chart-series-").append(part.color).append("\" data-index=\"").append(i).append("\">")
                    .append("<span class=\"sw-chart-legend-indicator\" aria-hidden=\"true\"></span><span class=\"sw-donut-name\">").append(escape(part.category))
                    .append("</span><span class=\"sw-donut-value\">").append(escape(part.text)).append("</span><span class=\"sw-donut-share\">")
                    .append(escape(part.share)).append("</span></li>");
        }
        b.append("</ul>");
    }

    private void appendDataTable(StringBuilder b, Snapshot s, String id) {
        b.append("<details class=\"sw-chart-data\"><summary>").append(escape(getString("SwDonutChart.data")))
                .append("</summary><div class=\"sw-chart-table-scroll\" tabindex=\"0\" role=\"region\" aria-labelledby=\"").append(id).append("-title\">")
                .append("<table class=\"sw-table\"><caption class=\"sw-chart-sr-only\">").append(escape(s.title)).append("</caption><thead><tr><th scope=\"col\">")
                .append(escape(s.categoryHeader)).append("</th><th scope=\"col\">").append(escape(s.valueHeader)).append("</th><th scope=\"col\">")
                .append(escape(getString("SwDonutChart.share"))).append("</th></tr></thead><tbody>");
        for (Row row : s.rows) b.append("<tr><th scope=\"row\">").append(escape(row.category)).append("</th><td>").append(escape(row.text))
                .append("</td><td>").append(escape(row.share)).append("</td></tr>");
        b.append("</tbody><tfoot><tr><th scope=\"row\">").append(escape(s.totalLabel)).append("</th><td>").append(escape(s.totalText))
                .append("</td><td>").append(escape(share(s.total, s.total))).append("</td></tr></tfoot></table></div></details>");
    }
    @Override public void renderHead(IHeaderResponse response) {
        super.renderHead(response); response.render(JavaScriptHeaderItem.forReference(DONUT_JS));
    }
    @Override protected void onDetach() {
        snapshot = null; data.detach(); title.detach();
        if (categoryHeader != null) categoryHeader.detach();
        if (valueHeader != null) valueHeader.detach();
        if (totalLabel != null) totalLabel.detach();
        super.onDetach();
    }
}

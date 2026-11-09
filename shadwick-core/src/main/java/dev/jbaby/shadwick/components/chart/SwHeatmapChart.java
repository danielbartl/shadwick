package dev.jbaby.shadwick.components.chart;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import org.apache.wicket.markup.head.*;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.string.Strings;

/** Native table heatmap with SVG tiles, five shared intensity bands and optional two-dimensional exploration. */
public class SwHeatmapChart<T> extends Panel {
    private static final JavaScriptResourceReference HEATMAP_JS = new JavaScriptResourceReference(Shadwick.class, "js/heatmap-chart.js");
    private final IModel<? extends List<T>> data;
    private final SerializableFunction<T, String> row;
    private final List<HeatmapColumn<T>> columns = new ArrayList<>();
    private IModel<String> title = Model.of("Chart"), rowHeader, valueHeader;
    private IModel<? extends Number> minimum, maximum;
    private SwChart.ValueFormat namedFormat = SwChart.ValueFormat.NUMBER;
    private SerializableBiFunction<Number, Locale, String> valueFormat;
    private Currency currency;
    private int color = 1;
    private boolean values = true, legend = true, tooltip = true;
    private transient Snapshot snapshot;
    private record Cell(BigDecimal value, String text) implements Serializable {}
    private record Row(String label, List<Cell> cells) implements Serializable {}
    private record Band(BigDecimal lower, BigDecimal upper, String text) implements Serializable {}
    private record Snapshot(String title, String rowHeader, String valueHeader, List<String> columns, List<Row> rows,
                            List<Band> bands, String missing, boolean measured) implements Serializable {}

    public SwHeatmapChart(String id, IModel<? extends List<T>> data, SerializableFunction<T, String> row) {
        super(id);
        this.data = Objects.requireNonNull(data);
        this.row = Objects.requireNonNull(row);
        setOutputMarkupId(true);
    }
    @Override protected void onInitialize() {
        super.onInitialize();
        add(figure());
    }
    private WebMarkupContainer figure() {
        var figure = new WebMarkupContainer("figure");
        figure.add(new StyledBehavior("sw-chart sw-heatmap-chart"));
        figure.setOutputMarkupId(true);
        figure.add(new Label("content", (IModel<String>) () -> renderChart(figure.getMarkupId())).setEscapeModelStrings(false));
        return figure;
    }
    public SwHeatmapChart<T> addColumn(HeatmapColumn<T> value) {
        Objects.requireNonNull(value);
        if (columns.contains(value)) throw new IllegalArgumentException("A heatmap column must be registered exactly once");
        columns.add(value);
        return this;
    }
    public SwHeatmapChart<T> setTitle(String value) { return setTitle(Model.of(value)); }
    public SwHeatmapChart<T> setTitle(IModel<String> value) { title = Objects.requireNonNull(value); return this; }
    public SwHeatmapChart<T> setRowHeader(String value) { return setRowHeader(Model.of(value)); }
    public SwHeatmapChart<T> setRowHeader(IModel<String> value) { rowHeader = Objects.requireNonNull(value); return this; }
    public SwHeatmapChart<T> setValueHeader(String value) { return setValueHeader(Model.of(value)); }
    public SwHeatmapChart<T> setValueHeader(IModel<String> value) { valueHeader = Objects.requireNonNull(value); return this; }
    public SwHeatmapChart<T> setRange(Number lower, Number upper) {
        return setRange(Model.of(Objects.requireNonNull(lower)), Model.of(Objects.requireNonNull(upper)));
    }
    /** Shared color range; bounds must be defined and increasing, with every measured value inside. */
    public SwHeatmapChart<T> setRange(IModel<? extends Number> lower, IModel<? extends Number> upper) {
        replaceRange(Objects.requireNonNull(lower), Objects.requireNonNull(upper));
        return this;
    }
    public SwHeatmapChart<T> setAutoScale() { replaceRange(null, null); return this; }
    private void replaceRange(IModel<? extends Number> lower, IModel<? extends Number> upper) {
        if (minimum != null && minimum != lower && minimum != upper) minimum.detach();
        if (maximum != null && maximum != minimum && maximum != lower && maximum != upper) maximum.detach();
        minimum = lower;
        maximum = upper;
    }
    /** Palette token 1–5; default one. Intensity always increases from lower to higher values. */
    public SwHeatmapChart<T> setColor(int value) {
        if (value < 1 || value > 5) throw new IllegalArgumentException("Heatmap color must be between 1 and 5");
        color = value;
        return this;
    }
    public SwHeatmapChart<T> setShowValues(boolean value) { values = value; return this; }
    public SwHeatmapChart<T> setShowLegend(boolean value) { legend = value; return this; }
    public SwHeatmapChart<T> setShowTooltip(boolean value) { tooltip = value; return this; }
    public SwHeatmapChart<T> setValueFormat(SwChart.ValueFormat value) {
        namedFormat = Objects.requireNonNull(value);
        valueFormat = null;
        return this;
    }
    public SwHeatmapChart<T> setValueFormat(SerializableBiFunction<Number, Locale, String> value) { valueFormat = value; return this; }
    public SwHeatmapChart<T> setCurrency(Currency value) {
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
        return model == null ? getString("SwHeatmapChart." + key)
                : required(model.getObject(), "A heatmap needs a " + key + " header");
    }
    private static BigDecimal amount(Number value) {
        if (value == null) return null;
        double number = value.doubleValue();
        if (!Double.isFinite(number) || Math.abs(number) > 1e100)
            throw new IllegalArgumentException("Heatmap values must be finite with magnitude at most 1e100");
        BigDecimal result;
        try { result = value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString()); }
        catch (NumberFormatException ignored) { result = BigDecimal.valueOf(number); }
        if (result.abs().compareTo(BigDecimal.ONE.scaleByPowerOfTen(100)) > 0)
            throw new IllegalArgumentException("Heatmap values must be finite with magnitude at most 1e100");
        return result;
    }
    private Snapshot snapshot() {
        if (snapshot != null) return snapshot;
        String name = required(title.getObject(), "A heatmap needs a title");
        String rowName = header(rowHeader, "row"), valueName = header(valueHeader, "value");
        if (columns.isEmpty()) throw new IllegalStateException("A heatmap needs at least one column");
        List<String> labels = new ArrayList<>();
        for (HeatmapColumn<T> column : columns)
            labels.add(required(column.label.getObject(), "Every heatmap column needs a label"));
        List<Row> rows = readRows();
        BigDecimal lower = BigDecimal.ZERO, upper = BigDecimal.ZERO;
        boolean measured = false;
        for (Row row : rows) for (Cell cell : row.cells) if (cell.value != null) {
            measured = true;
            lower = lower.min(cell.value);
            upper = upper.max(cell.value);
        }
        if (minimum == null) {
            if (lower.compareTo(upper) == 0) upper = BigDecimal.ONE;
        } else {
            lower = amount(minimum.getObject());
            upper = amount(maximum.getObject());
            if (lower == null || upper == null || lower.compareTo(upper) >= 0)
                throw new IllegalArgumentException("Heatmap range bounds must be defined and increasing");
            // Only observations, rather than the automatic zero anchor, constrain an explicit range.
            for (Row row : rows) for (Cell cell : row.cells) if (cell.value != null
                    && (cell.value.compareTo(lower) < 0 || cell.value.compareTo(upper) > 0))
                throw new IllegalArgumentException("Heatmap values must stay within the configured range");
        }
        return snapshot = new Snapshot(name, rowName, valueName, List.copyOf(labels), List.copyOf(rows), bands(lower, upper),
                getString("SwHeatmapChart.missing"), measured);
    }
    private List<Row> readRows() {
        List<Row> rows = new ArrayList<>();
        List<T> items = data.getObject();
        if (items == null) return rows;
        for (T item : items) {
            String name = required(row.apply(item), "Every heatmap row needs a label");
            List<Cell> cells = new ArrayList<>();
            for (HeatmapColumn<T> column : columns) {
                Number original = column.value.apply(item);
                cells.add(new Cell(amount(original), format(original)));
            }
            rows.add(new Row(name, List.copyOf(cells)));
        }
        return rows;
    }
    private List<Band> bands(BigDecimal lower, BigDecimal upper) {
        BigDecimal step = upper.subtract(lower).divide(BigDecimal.valueOf(5));
        List<Band> bands = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            BigDecimal start = lower.add(step.multiply(BigDecimal.valueOf(i))), end = start.add(step);
            bands.add(new Band(start, end, format(start) + " – " + format(end)));
        }
        return List.copyOf(bands);
    }
    private static int level(Snapshot s, BigDecimal value) {
        for (int i = 0; i < 4; i++) if (value.compareTo(s.bands.get(i).upper) < 0) return i;
        return 4;
    }
    private static String escape(String value) { return Strings.escapeMarkup(Objects.toString(value, "")).toString(); }
    private String renderChart(String id) {
        Snapshot s = snapshot();
        StringBuilder b = new StringBuilder("<figcaption class=\"sw-chart-title\" id=\"").append(id).append("-title\">")
                .append(escape(s.title)).append("</figcaption><div class=\"sw-heatmap-content sw-chart-series-").append(color)
                .append("\" data-values=\"").append(values).append("\">");
        if (s.rows.isEmpty()) emptyState(b, "empty");
        else {
            if (!s.measured) emptyState(b, "noValues");
            renderMatrix(b, s, id);
            if (legend) renderLegend(b, s, id);
        }
        return b.append("</div>").toString();
    }
    private void emptyState(StringBuilder b, String key) {
        b.append("<p class=\"sw-chart-empty\" role=\"status\">").append(escape(getString("SwHeatmapChart." + key))).append("</p>");
    }
    private void renderMatrix(StringBuilder b, Snapshot s, String id) {
        b.append("<p class=\"sw-chart-sr-only\" id=\"").append(id).append("-help\">")
                .append(escape(getString(tooltip ? "SwHeatmapChart.instructions" : "SwHeatmapChart.dataInstructions"))).append("</p>")
                .append("<div class=\"sw-heatmap-plot\" tabindex=\"0\" role=\"region\" aria-labelledby=\"").append(id)
                .append("-title\" aria-describedby=\"").append(id).append("-help").append(legend ? " " + id + "-scale" : "")
                .append("\" data-tooltip=\"").append(tooltip).append("\" data-rows=\"").append(s.rows.size())
                .append("\" data-columns=\"").append(s.columns.size()).append("\" data-value-label=\"").append(escape(s.valueHeader))
                .append("\" data-color=\"sw-chart-series-").append(color).append("\"><table class=\"sw-heatmap-matrix\">")
                .append("<caption class=\"sw-chart-sr-only\">").append(escape(s.title + "; " + s.valueHeader))
                .append("</caption><thead><tr><th scope=\"col\">").append(escape(s.rowHeader)).append("</th>");
        for (String column : s.columns) b.append("<th scope=\"col\">").append(escape(column)).append("</th>");
        b.append("</tr></thead><tbody>");
        for (int r = 0; r < s.rows.size(); r++) {
            Row row = s.rows.get(r);
            b.append("<tr><th scope=\"row\">").append(escape(row.label)).append("</th>");
            for (int c = 0; c < s.columns.size(); c++) renderCell(b, s, r, c);
            b.append("</tr>");
        }
        b.append("</tbody></table></div><div class=\"sw-chart-tooltip-content sw-heatmap-tooltip\" role=\"tooltip\" hidden></div>");
    }
    private void renderCell(StringBuilder b, Snapshot s, int r, int c) {
        Row row = s.rows.get(r);
        Cell cell = row.cells.get(c);
        b.append("<td class=\"sw-heatmap-cell\" data-index=\"").append(r * s.columns.size() + c)
                .append("\" data-row=\"").append(r).append("\" data-column=\"").append(c).append('"');
        if (cell.value == null) b.append(" data-tooltip-value=\"").append(escape(s.missing))
                .append("\" aria-label=\"").append(escape(s.missing)).append('"');
        b.append(" title=\"").append(escape(row.label + "; " + s.columns.get(c) + "; " + s.valueHeader + ": "
                        + (cell.value == null ? s.missing : cell.text)))
                .append("\"><svg class=\"sw-heatmap-tile\" width=\"100%\" height=\"100%\" aria-hidden=\"true\" focusable=\"false\">")
                .append("<rect class=\"sw-heatmap-base\" width=\"100%\" height=\"100%\" rx=\"3\"/>");
        if (cell.value == null) b.append("<rect class=\"sw-heatmap-missing\" x=\"1\" y=\"1\" width=\"96%\" height=\"94%\" rx=\"3\"/>")
                .append("<line class=\"sw-heatmap-missing-mark\" x1=\"44%\" y1=\"40%\" x2=\"56%\" y2=\"60%\"/>")
                .append("<line class=\"sw-heatmap-missing-mark\" x1=\"56%\" y1=\"40%\" x2=\"44%\" y2=\"60%\"/>");
        else b.append("<rect class=\"sw-heatmap-fill sw-heatmap-level-").append(level(s, cell.value))
                .append("\" width=\"100%\" height=\"100%\" rx=\"3\"/>");
        b.append("</svg><span class=\"sw-heatmap-value\">").append(escape(cell.text)).append("</span></td>");
    }
    private void renderLegend(StringBuilder b, Snapshot s, String id) {
        b.append("<div class=\"sw-heatmap-legend\" id=\"").append(id).append("-scale\"><p class=\"sw-heatmap-scale-title\">")
                .append(escape(getString("SwHeatmapChart.scale") + ": " + s.valueHeader)).append("</p><ol class=\"sw-heatmap-bands\">");
        for (int i = 0; i < s.bands.size(); i++) b.append("<li><svg class=\"sw-heatmap-swatch\" aria-hidden=\"true\" focusable=\"false\">")
                .append("<rect class=\"sw-heatmap-base\" width=\"100%\" height=\"100%\" rx=\"2\"/><rect class=\"sw-heatmap-fill sw-heatmap-level-")
                .append(i).append("\" width=\"100%\" height=\"100%\" rx=\"2\"/></svg><span>").append(escape(s.bands.get(i).text)).append("</span></li>");
        b.append("</ol><p class=\"sw-heatmap-missing-key\">").append(escape("× " + s.missing)).append("</p></div>");
    }
    @Override public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptHeaderItem.forReference(HEATMAP_JS));
    }
    @Override protected void onDetach() {
        snapshot = null;
        data.detach();
        title.detach();
        if (rowHeader != null) rowHeader.detach();
        if (valueHeader != null) valueHeader.detach();
        if (minimum != null) minimum.detach();
        if (maximum != null && maximum != minimum) maximum.detach();
        columns.forEach(column -> column.label.detach());
        super.onDetach();
    }
}

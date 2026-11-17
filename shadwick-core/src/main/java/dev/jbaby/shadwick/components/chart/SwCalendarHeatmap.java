package dev.jbaby.shadwick.components.chart;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.util.SerializableBiFunction;
import dev.jbaby.shadwick.util.SerializableFunction;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.string.Strings;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * A calendar heatmap: one cell per day, the weeks as columns and the weekdays as rows, each day's value as one of five
 * intensity bands shared with {@link SwHeatmapChart}. The grid is a native table that is also its data: every day is a
 * cell with its full date and exact value, so it reads without the colours, without JavaScript and in a screen reader.
 * <pre>
 * add(new SwCalendarHeatmap&lt;&gt;("activity", Model.ofList(days), Day::date, Day::commits)
 *         .setTitle("Contributions").setValueHeader("Commits").setMissingValue(0));
 * </pre>
 * By default the period runs from the first to the last date in the data; {@link #setPeriod} fixes it (at most three
 * years). A day the data has no entry for is not measured, shown as a dashed tile, unless {@link #setMissingValue}
 * gives such days a value, such as zero for counts. Each date may appear once: aggregate per day first.
 *
 * @param <T> the type of the data's entries
 */
public class SwCalendarHeatmap<T> extends Panel {
    private static final JavaScriptResourceReference HEATMAP_JS = new JavaScriptResourceReference(Shadwick.class, "js/heatmap-chart.js");
    private static final int MAXIMUM_DAYS = 1096;

    private final IModel<? extends List<T>> data;
    private final SerializableFunction<T, LocalDate> date;
    private final SerializableFunction<T, ? extends Number> value;
    private IModel<String> title = Model.of("Chart"), valueHeader;
    private IModel<LocalDate> from, to;
    private IModel<? extends Number> minimum, maximum;
    private Number missingValue;
    private DayOfWeek firstDayOfWeek;
    private SwChart.ValueFormat namedFormat = SwChart.ValueFormat.NUMBER;
    private SerializableBiFunction<Number, Locale, String> valueFormat;
    private Currency currency;
    private int color = 1;
    private boolean legend = true, tooltip = true;
    private transient Snapshot snapshot;

    private record Day(LocalDate date, BigDecimal value, String text, String label) implements Serializable {}
    private record Snapshot(String title, String valueHeader, LocalDate from, LocalDate to, LocalDate start, int weeks,
                            Map<LocalDate, Day> days, List<HeatmapScale.Band> bands, boolean measured, boolean missing) implements Serializable {}

    public SwCalendarHeatmap(String id, IModel<? extends List<T>> data, SerializableFunction<T, LocalDate> date,
                             SerializableFunction<T, ? extends Number> value) {
        super(id);
        this.data = Objects.requireNonNull(data);
        this.date = Objects.requireNonNull(date);
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
        figure.add(new StyledBehavior("sw-chart sw-heatmap-chart sw-calendar-heatmap"));
        figure.setOutputMarkupId(true);
        figure.add(new Label("content", (IModel<String>) () -> renderChart(figure.getMarkupId())).setEscapeModelStrings(false));
        return figure;
    }

    public SwCalendarHeatmap<T> setTitle(String value) { return setTitle(Model.of(value)); }
    public SwCalendarHeatmap<T> setTitle(IModel<String> value) { title = Objects.requireNonNull(value); return this; }
    /** The name of the measured value, as in "Commits: 5"; by default the resource {@code SwCalendarHeatmap.value}. */
    public SwCalendarHeatmap<T> setValueHeader(String value) { return setValueHeader(Model.of(value)); }
    public SwCalendarHeatmap<T> setValueHeader(IModel<String> value) { valueHeader = Objects.requireNonNull(value); return this; }

    /** The days shown, both included: increasing and at most three years. By default the data's first to last date. */
    public SwCalendarHeatmap<T> setPeriod(LocalDate from, LocalDate to) {
        return setPeriod(Model.of(Objects.requireNonNull(from)), Model.of(Objects.requireNonNull(to)));
    }
    public SwCalendarHeatmap<T> setPeriod(IModel<LocalDate> from, IModel<LocalDate> to) {
        detachPeriod();
        this.from = Objects.requireNonNull(from);
        this.to = Objects.requireNonNull(to);
        return this;
    }
    /** Back to the data's own first and last dates. */
    public SwCalendarHeatmap<T> setPeriodFromData() {
        detachPeriod();
        from = to = null;
        return this;
    }

    /**
     * The value of a day the data has no entry for, such as zero for counts of visits or commits. Without it, which is
     * the default, such a day is not measured: a dashed tile, distinct from a measured zero. {@code null} restores that.
     */
    public SwCalendarHeatmap<T> setMissingValue(Number value) {
        HeatmapScale.amount(value);
        missingValue = value;
        return this;
    }

    /** The weekday the weeks start on; by default the locale's ({@link WeekFields#of(Locale)}). {@code null} restores it. */
    public SwCalendarHeatmap<T> setFirstDayOfWeek(DayOfWeek value) { firstDayOfWeek = value; return this; }

    public SwCalendarHeatmap<T> setRange(Number lower, Number upper) {
        return setRange(Model.of(Objects.requireNonNull(lower)), Model.of(Objects.requireNonNull(upper)));
    }
    /** Shared color range; bounds must be defined and increasing, with every measured value inside. */
    public SwCalendarHeatmap<T> setRange(IModel<? extends Number> lower, IModel<? extends Number> upper) {
        replaceRange(Objects.requireNonNull(lower), Objects.requireNonNull(upper));
        return this;
    }
    public SwCalendarHeatmap<T> setAutoScale() { replaceRange(null, null); return this; }
    private void replaceRange(IModel<? extends Number> lower, IModel<? extends Number> upper) {
        if (minimum != null && minimum != lower && minimum != upper) minimum.detach();
        if (maximum != null && maximum != minimum && maximum != lower && maximum != upper) maximum.detach();
        minimum = lower;
        maximum = upper;
    }
    /** Palette token 1–5; default one. Intensity always increases from lower to higher values. */
    public SwCalendarHeatmap<T> setColor(int value) {
        if (value < 1 || value > 5) throw new IllegalArgumentException("Heatmap color must be between 1 and 5");
        color = value;
        return this;
    }
    public SwCalendarHeatmap<T> setShowLegend(boolean value) { legend = value; return this; }
    public SwCalendarHeatmap<T> setShowTooltip(boolean value) { tooltip = value; return this; }
    public SwCalendarHeatmap<T> setValueFormat(SwChart.ValueFormat value) {
        namedFormat = Objects.requireNonNull(value);
        valueFormat = null;
        return this;
    }
    public SwCalendarHeatmap<T> setValueFormat(SerializableBiFunction<Number, Locale, String> value) { valueFormat = value; return this; }
    public SwCalendarHeatmap<T> setCurrency(Currency value) {
        currency = Objects.requireNonNull(value);
        namedFormat = SwChart.ValueFormat.CURRENCY;
        valueFormat = null;
        return this;
    }

    private String format(Number number) { return ChartValues.format(number, getLocale(), namedFormat, currency, valueFormat); }

    private static String required(String text, String message) {
        if (text == null || text.isBlank()) throw new IllegalStateException(message);
        return text;
    }

    private Snapshot snapshot() {
        if (snapshot != null) return snapshot;
        String name = required(title.getObject(), "A calendar heatmap needs a title");
        String valueName = valueHeader == null ? getString("SwCalendarHeatmap.value")
                : required(valueHeader.getObject(), "A calendar heatmap needs a value header");
        DateTimeFormatter full = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(getLocale());
        Map<LocalDate, Day> days = new HashMap<>();
        List<T> items = data.getObject();
        if (items != null) for (T item : items) {
            LocalDate day = Objects.requireNonNull(date.apply(item), "Every calendar heatmap entry needs a date");
            if (days.containsKey(day))
                throw new IllegalArgumentException("Each date may appear once in a calendar heatmap; aggregate per day first: " + day);
            Number original = value.apply(item);
            days.put(day, new Day(day, HeatmapScale.amount(original), original == null ? null : format(original), day.format(full)));
        }
        LocalDate first = from == null ? days.keySet().stream().min(LocalDate::compareTo).orElse(null) : from.getObject();
        LocalDate last = to == null ? days.keySet().stream().max(LocalDate::compareTo).orElse(null) : to.getObject();
        if (from != null && (first == null || last == null || last.isBefore(first)))
            throw new IllegalArgumentException("A calendar heatmap period must be defined and increasing");
        if (first != null && ChronoUnit.DAYS.between(first, last) >= MAXIMUM_DAYS)
            throw new IllegalArgumentException("A calendar heatmap shows at most three years (" + MAXIMUM_DAYS + " days)");
        if (first != null) for (LocalDate day : days.keySet())
            if (day.isBefore(first) || day.isAfter(last))
                throw new IllegalArgumentException("Calendar heatmap data must stay within its period: " + day);
        // Days without an entry: a value the application gave them, or not measured
        BigDecimal fill = HeatmapScale.amount(missingValue);
        boolean missing = false;
        if (first != null) for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1))
            if (!days.containsKey(day)) {
                days.put(day, new Day(day, fill, fill == null ? null : format(missingValue), day.format(full)));
                if (fill == null) missing = true;
            }
        missing |= days.values().stream().anyMatch(day -> day.value() == null);
        List<BigDecimal> observed = days.values().stream().map(Day::value).filter(Objects::nonNull).toList();
        BigDecimal[] bounds = HeatmapScale.bounds(observed, minimum, maximum);
        LocalDate start = first == null ? null : first.with(TemporalAdjusters.previousOrSame(weekStart()));
        int weeks = first == null ? 0 : (int) (ChronoUnit.DAYS.between(start, last) / 7 + 1);
        return snapshot = new Snapshot(name, valueName, first, last, start, weeks, Map.copyOf(days),
                HeatmapScale.bands(bounds[0], bounds[1], this::format), !observed.isEmpty(), missing);
    }

    private DayOfWeek weekStart() {
        return firstDayOfWeek != null ? firstDayOfWeek : WeekFields.of(getLocale()).getFirstDayOfWeek();
    }

    private static String escape(String text) { return Strings.escapeMarkup(Objects.toString(text, "")).toString(); }

    private String renderChart(String id) {
        Snapshot s = snapshot();
        StringBuilder b = new StringBuilder("<figcaption class=\"sw-chart-title\" id=\"").append(id).append("-title\">")
                .append(escape(s.title)).append("</figcaption><div class=\"sw-heatmap-content sw-chart-series-").append(color)
                .append("\" data-values=\"false\">");
        if (s.from == null) emptyState(b, "empty");
        else {
            if (!s.measured) emptyState(b, "noValues");
            renderGrid(b, s, id);
            if (legend) renderLegend(b, s, id);
        }
        return b.append("</div>").toString();
    }

    private void emptyState(StringBuilder b, String key) {
        b.append("<p class=\"sw-chart-empty\" role=\"status\">").append(escape(getString("SwCalendarHeatmap." + key))).append("</p>");
    }

    private void renderGrid(StringBuilder b, Snapshot s, String id) {
        b.append("<p class=\"sw-chart-sr-only\" id=\"").append(id).append("-help\">")
                .append(escape(getString(tooltip ? "SwCalendarHeatmap.instructions" : "SwCalendarHeatmap.dataInstructions"))).append("</p>")
                .append("<div class=\"sw-heatmap-plot\" tabindex=\"0\" role=\"region\" aria-labelledby=\"").append(id)
                .append("-title\" aria-describedby=\"").append(id).append("-help").append(legend ? " " + id + "-scale" : "")
                .append("\" data-tooltip=\"").append(tooltip).append("\" data-rows=\"7\" data-columns=\"").append(s.weeks)
                .append("\" data-value-label=\"").append(escape(s.valueHeader))
                .append("\" data-color=\"sw-chart-series-").append(color).append("\"><table class=\"sw-heatmap-matrix\">")
                .append("<caption class=\"sw-chart-sr-only\">").append(escape(s.title + "; " + s.valueHeader)).append("</caption>")
                .append("<thead><tr><td></td>");
        List<String> months = monthLabels(s);
        DateTimeFormatter medium = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(getLocale());
        for (int c = 0; c < s.weeks; c++) {
            String week = new StringResourceModel("SwCalendarHeatmap.week", this).setParameters(s.start.plusWeeks(c).format(medium)).getString();
            b.append("<th scope=\"col\"><span class=\"sw-chart-sr-only\">").append(escape(week)).append("</span>");
            if (months.get(c) != null) b.append("<span class=\"sw-calendar-heatmap-month\" aria-hidden=\"true\">").append(escape(months.get(c))).append("</span>");
            b.append("</th>");
        }
        b.append("</tr></thead><tbody>");
        for (int r = 0; r < 7; r++) {
            DayOfWeek weekday = s.start.plusDays(r).getDayOfWeek();
            b.append("<tr><th scope=\"row\"><span class=\"sw-chart-sr-only\">").append(escape(weekday.getDisplayName(TextStyle.FULL_STANDALONE, getLocale())))
                    .append("</span><span aria-hidden=\"true\">").append(escape(weekday.getDisplayName(TextStyle.SHORT_STANDALONE, getLocale())))
                    .append("</span></th>");
            for (int c = 0; c < s.weeks; c++) renderDay(b, s, r, c);
            b.append("</tr>");
        }
        b.append("</tbody></table></div><div class=\"sw-chart-tooltip-content sw-heatmap-tooltip\" role=\"tooltip\" hidden></div>");
    }

    /**
     * The visible month labels, on the week in which each month's first shown day falls. A label that would crowd the
     * next one (fewer than four weeks before it, as a short first month can) is left out; the year is added to the
     * first label and to January's when the period spans more than one year.
     */
    private List<String> monthLabels(Snapshot s) {
        List<Integer> columns = new ArrayList<>();
        List<LocalDate> months = new ArrayList<>();
        for (LocalDate day = s.from; !day.isAfter(s.to); day = day.withDayOfMonth(1).plusMonths(1)) {
            int column = (int) (ChronoUnit.DAYS.between(s.start, day) / 7);
            if (!columns.isEmpty() && column - columns.get(columns.size() - 1) < 4) {
                columns.remove(columns.size() - 1);
                months.remove(months.size() - 1);
            }
            columns.add(column);
            months.add(day);
        }
        boolean years = s.from.getYear() != s.to.getYear();
        DateTimeFormatter month = DateTimeFormatter.ofPattern("LLL", getLocale()), monthYear = DateTimeFormatter.ofPattern("LLL uuuu", getLocale());
        List<String> labels = new ArrayList<>();
        for (int c = 0; c < s.weeks; c++) labels.add(null);
        for (int i = 0; i < columns.size(); i++) {
            boolean withYear = years && (i == 0 || months.get(i).getMonthValue() == 1);
            labels.set(columns.get(i), months.get(i).format(withYear ? monthYear : month));
        }
        return labels;
    }

    private void renderDay(StringBuilder b, Snapshot s, int r, int c) {
        LocalDate day = s.start.plusDays(c * 7L + r);
        int index = r * s.weeks + c;
        b.append("<td class=\"sw-heatmap-cell\" data-index=\"").append(index).append("\" data-row=\"").append(r)
                .append("\" data-column=\"").append(c).append('"');
        if (day.isBefore(s.from) || day.isAfter(s.to)) {
            b.append(" data-outside=\"\"></td>");
            return;
        }
        Day entry = s.days.get(day);
        String missing = getString("SwCalendarHeatmap.missing");
        String shown = entry.value() == null ? missing : entry.text();
        b.append(" data-tooltip-label=\"").append(escape(entry.label())).append('"');
        if (entry.value() == null) b.append(" data-tooltip-value=\"").append(escape(missing)).append('"');
        b.append(" title=\"").append(escape(entry.label() + "; " + s.valueHeader + ": " + shown))
                .append("\"><svg class=\"sw-heatmap-tile\" width=\"100%\" height=\"100%\" aria-hidden=\"true\" focusable=\"false\">")
                .append("<rect class=\"sw-heatmap-base\" width=\"100%\" height=\"100%\" rx=\"2\"/>");
        if (entry.value() == null) b.append("<rect class=\"sw-heatmap-missing\" x=\"1\" y=\"1\" width=\"86%\" height=\"86%\" rx=\"2\"/>")
                .append("<line class=\"sw-heatmap-missing-mark\" x1=\"35%\" y1=\"35%\" x2=\"65%\" y2=\"65%\"/>")
                .append("<line class=\"sw-heatmap-missing-mark\" x1=\"65%\" y1=\"35%\" x2=\"35%\" y2=\"65%\"/>");
        else b.append("<rect class=\"sw-heatmap-fill sw-heatmap-level-").append(HeatmapScale.level(s.bands, entry.value()))
                .append("\" width=\"100%\" height=\"100%\" rx=\"2\"/>");
        b.append("</svg><span class=\"sw-heatmap-value\">").append(escape(shown)).append("</span></td>");
    }

    private void renderLegend(StringBuilder b, Snapshot s, String id) {
        b.append("<div class=\"sw-heatmap-legend\" id=\"").append(id).append("-scale\"><p class=\"sw-heatmap-scale-title\">")
                .append(escape(getString("SwCalendarHeatmap.scale") + ": " + s.valueHeader)).append("</p><ol class=\"sw-heatmap-bands\">");
        for (HeatmapScale.Band band : s.bands) b.append("<li><svg class=\"sw-heatmap-swatch\" aria-hidden=\"true\" focusable=\"false\">")
                .append("<rect class=\"sw-heatmap-base\" width=\"100%\" height=\"100%\" rx=\"2\"/><rect class=\"sw-heatmap-fill sw-heatmap-level-")
                .append(s.bands.indexOf(band)).append("\" width=\"100%\" height=\"100%\" rx=\"2\"/></svg><span>").append(escape(band.text())).append("</span></li>");
        b.append("</ol>");
        if (s.missing) b.append("<p class=\"sw-heatmap-missing-key\">").append(escape("× " + getString("SwCalendarHeatmap.missing"))).append("</p>");
        b.append("</div>");
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptHeaderItem.forReference(HEATMAP_JS));
    }

    private void detachPeriod() {
        if (from != null) from.detach();
        if (to != null && to != from) to.detach();
    }

    @Override
    protected void onDetach() {
        snapshot = null;
        data.detach();
        title.detach();
        if (valueHeader != null) valueHeader.detach();
        detachPeriod();
        if (minimum != null) minimum.detach();
        if (maximum != null && maximum != minimum) maximum.detach();
        super.onDetach();
    }
}

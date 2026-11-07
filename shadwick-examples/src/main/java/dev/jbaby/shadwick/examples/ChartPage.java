package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwCard;
import dev.jbaby.shadwick.components.chart.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;
import org.apache.wicket.model.*;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/** Server SVG and exact tables, enhanced with keyboard/hover tooltips. */
public class ChartPage extends BasePage {
    public record Month(String name, Double desktop, Double mobile) implements Serializable {}
    public record Client(String name, Double delivered, Double planned) implements Serializable {}
    public record Region(String name, Number revenue) implements Serializable {}
    public record Trend(String team, List<Integer> values) implements Serializable {}
    public record Revenue(LocalDate date, BigDecimal amount) implements Serializable {}
    public record Invoice(String client, String reference, BigDecimal amount) implements Serializable {}
    private static List<Revenue> revenue() {
        return List.of(new Revenue(LocalDate.of(2026, 4, 1), new BigDecimal("12345.67")),
                new Revenue(LocalDate.of(2026, 5, 1), null), new Revenue(LocalDate.of(2026, 6, 1), new BigDecimal("24890.50")),
                new Revenue(LocalDate.of(2026, 7, 1), BigDecimal.ZERO), new Revenue(LocalDate.of(2026, 8, 1), new BigDecimal("-1250.25")));
    }
    private static SwChart<Revenue> revenueChart(String id, String title, IModel<List<Revenue>> model) {
        return new SwChart<>(id, model, Revenue::date).setTitle(title).setCategoryHeader("Report date")
                .setAxisCategoryFormat(SwChart.CategoryFormat.MONTH_SHORT).setAxisValueFormat(SwChart.ValueFormat.COMPACT)
                .setTooltipLabelFormat(SwChart.CategoryFormat.DATE).setTooltipCurrency(Currency.getInstance("EUR"))
                .addSeries(ChartSeries.bar("Net revenue (EUR)", Revenue::amount).setValueLabels(ChartSeries.ValueLabels.LAST));
    }
    private static List<Region> regions() {
        return List.of(new Region("North", 40), new Region("South", 25), new Region("East", 15),
                new Region("West", 10), new Region("Central", 5), new Region("Islands", 3), new Region("Overseas", 2));
    }
    private static List<Client> clients() {
        return List.of(new Client("Northstar Foundation accessibility improvements", 84.0, 96.0),
                new Client("Harbor & Pine retail platform", 68.0, 72.0),
                new Client("Clover Studio internal tools", 52.0, 48.0),
                new Client("Greenfield community website", 36.0, 40.0));
    }
    public static List<Month> months() {
        return List.of(new Month("January", 186.0, 80.0), new Month("February", 305.0, 200.0),
                new Month("March", 237.0, 120.0), new Month("April", 73.0, 190.0), new Month("May", 209.0, 130.0),
                new Month("June", 214.0, 140.0));
    }
    private static List<Month> forecastMonths() {
        return List.of(new Month("January", 120.0, null), new Month("February", 260.0, null), new Month("March", 410.0, null),
                new Month("April", null, 560.0), new Month("May", null, 700.0), new Month("June", null, 850.0));
    }
    private static SwChart<Month> forecast(String id, String title, IModel<List<Month>> data, boolean area) {
        ChartSeries<Month> actual = area ? ChartSeries.area("Actual", Month::desktop) : ChartSeries.line("Actual", Month::desktop);
        ChartSeries<Month> projected = area ? ChartSeries.area("Forecast", Month::mobile) : ChartSeries.line("Forecast", Month::mobile);
        actual.setColor(area ? 2 : 1).setDots(true);
        projected.setColor(area ? 2 : 1).setDashed(true).setDots(true).setFillOpacity(.15)
                .setJoin(actual).setValueLabels(ChartSeries.ValueLabels.LAST);
        return new SwChart<>(id, data, Month::name).setTitle(title).setCategoryHeader("Month").addSeries(actual).addSeries(projected);
    }

    public ChartPage(PageParameters parameters) {
        super(parameters);
        IModel<List<Integer>> delivered = Model.ofList(List.of(48, 52, 45, 68, 72, 84));
        var hoursCard = new SwCard("hoursCard");
        hoursCard.setOutputMarkupId(true);
        hoursCard.add(new Label("value", (IModel<String>) () -> NumberFormat.getIntegerInstance(getLocale()).format(delivered.getObject().get(delivered.getObject().size() - 1)) + " h"));
        hoursCard.add(new SwSparkline("trend", delivered).setFilled(true));
        add(hoursCard);
        var projectsCard = new SwCard("projectsCard");
        projectsCard.add(new SwSparkline("trend", Model.ofList(List.of(8, 10, 9, 13, 11, 12))).setColor(2));
        add(projectsCard);
        var satisfactionCard = new SwCard("satisfactionCard");
        satisfactionCard.add(new Label("value", (IModel<String>) () -> NumberFormat.getNumberInstance(getLocale()).format(4.8)));
        satisfactionCard.add(new SwSparkline("trend", Model.ofList(Arrays.asList(4.4, 4.5, null, 4.6, 4.7, 4.8))).setColor(3));
        add(satisfactionCard);
        add(new ListView<>("trendRows", List.of(new Trend("Design", List.of(24, 30, 28, 36, 40, 42)),
                new Trend("Engineering", List.of(48, 50, 56, 52, 64, 68)), new Trend("Support", Arrays.asList(20, null, 18, 24, null, 24)),
                new Trend("Research", List.of(16, 16, 16, 16)), new Trend("New team", Collections.singletonList(8)))) {
            @Override protected void populateItem(ListItem<Trend> item) {
                Trend row = item.getModelObject();
                item.add(new Label("team", row.team()));
                item.add(new Label("current", NumberFormat.getIntegerInstance(getLocale()).format(row.values().get(row.values().size() - 1))));
                item.add(new SwSparkline("trend", Model.ofList(row.values())));
            }
        });
        add(chart("bars")
                        .setTitle("Visitors by device")
                        .addSeries(ChartSeries.bar("Desktop", Month::desktop))
                        .addSeries(ChartSeries.bar("Mobile", Month::mobile)));
        add(chart("stacked")
                        .setTitle("A shared picture")
                        .addSeries(ChartSeries.bar("Desktop", Month::desktop).setStack("visitors"))
                        .addSeries(ChartSeries.bar("Mobile", Month::mobile).setStack("visitors")));
        add(chart("lines")
                        .setTitle("Steady progress")
                        .addSeries(ChartSeries.line("Desktop", Month::desktop).setDots(true))
                        .addSeries(ChartSeries.line("Mobile", Month::mobile).setDashed(true)));
        add(chart("labeledBars").setTitle("Highest and latest months")
                        .addSeries(ChartSeries.bar("Visitors", Month::desktop).setValueLabels(ChartSeries.ValueLabels.HIGHEST_AND_LAST)));
        add(chart("labeledLines").setTitle("Latest values at a glance")
                        .addSeries(ChartSeries.line("Desktop", Month::desktop).setValueLabels(ChartSeries.ValueLabels.LAST))
                        .addSeries(ChartSeries.line("Mobile", Month::mobile).setDashed(true).setValueLabels(ChartSeries.ValueLabels.LAST)));
        add(new SwChart<>("labeledHorizontal", Model.ofList(clients()), Client::name)
                        .setOrientation(SwChart.Orientation.HORIZONTAL).setTitle("Hours without hovering")
                        .setCategoryHeader("Client").setShowTooltip(false)
                        .addSeries(ChartSeries.bar("Delivered", Client::delivered).setValueLabels(ChartSeries.ValueLabels.ALL)));
        add(new SwChart<>("denseLabels", Model.ofList(java.util.stream.IntStream.range(0, 24)
                .mapToObj(i -> new Month("Day " + (i + 1), 100.0 + i % 3, 0.0)).toList()), Month::name)
                        .setTitle("Labels that make room")
                        .setShowXAxis(false).setCategoryHeader("Day")
                        .addSeries(ChartSeries.line("Daily", Month::desktop).setValueLabels(ChartSeries.ValueLabels.ALL)));
        add(chart("areas")
                        .setTitle("Room to grow")
                        .addSeries(ChartSeries.area("Desktop", Month::desktop).setStack("visitors"))
                        .addSeries(ChartSeries.area("Mobile", Month::mobile).setStack("visitors")));
        add(chart("composed")
                        .setTitle("Revenue and target")
                        .setCurrency(Currency.getInstance("EUR"))
                        .addSeries(ChartSeries.bar("Revenue", Month::desktop))
                        .addSeries(ChartSeries.line("Target", Month::mobile).setDashed(true)));
        add(forecast("forecastLine", "Actual into forecast", Model.ofList(forecastMonths()), false));
        add(forecast("forecastArea", "A forecast with context", Model.ofList(forecastMonths()), true));
        var gapped = new ArrayList<>(forecastMonths());
        gapped.set(4, new Month("May", null, null));
        add(forecast("forecastGaps", "A forecast with a missing report", Model.ofList(gapped), false));
        IModel<List<Month>> forecastData = Model.ofList(forecastMonths());
        var liveForecast = forecast("liveForecast", "The handoff moves forward", forecastData, true);
        add(liveForecast);
        add(revenueChart("formattedRevenue", "Revenue, from overview to detail", Model.ofList(revenue())));
        add(revenueChart("formattedHorizontal", "Monthly revenue, side by side", Model.ofList(revenue()))
                .setOrientation(SwChart.Orientation.HORIZONTAL));
        add(new SwChart<>("formattedInvoices", Model.ofList(List.of(
                new Invoice("Northstar Foundation", "INV-2041", new BigDecimal("4800.50")),
                new Invoice("Harbor & Pine", "INV-2042", new BigDecimal("3250.00")),
                new Invoice("Greenfield Community", "INV-2043", new BigDecimal("1900.75")))), Invoice::client)
                .setTitle("The right context for an invoice").setCategoryHeader("Client")
                .setOrientation(SwChart.Orientation.HORIZONTAL).setAxisValueFormat(SwChart.ValueFormat.COMPACT)
                .setAxisCategoryFormat((value, locale) -> value.toString().split(" ")[0])
                .setTooltipLabel(row -> row.client() + " · " + row.reference())
                .setTooltipValueFormat((value, locale) -> NumberFormat.getNumberInstance(locale).format(value) + " USD")
                .addSeries(ChartSeries.bar("Balance (USD)", Invoice::amount)));
        IModel<List<Revenue>> currentRevenue = Model.ofList(revenue());
        var liveFormatted = revenueChart("liveFormatted", "Fresh figures, familiar formats", currentRevenue);
        add(liveFormatted);
        var brandedActual = ChartSeries.area("Actual", Month::desktop).setColors("#047857", "#34d399")
                .setCurve(ChartSeries.Curve.NATURAL).setStrokeWidth(3);
        add(new SwChart<>("branded", Model.ofList(forecastMonths()), Month::name).setTitle("A forecast in your colors")
                .setLegendPosition(SwChart.LegendPosition.TOP).setLegendAlignment(SwChart.LegendAlignment.START)
                .addSeries(brandedActual).addSeries(ChartSeries.area("Forecast", Month::mobile).setColors("#047857", "#34d399")
                        .setCurve(ChartSeries.Curve.NATURAL).setDashed(true).setJoin(brandedActual).setFillOpacity(.12).setStrokeWidth(3)));
        add(chart("rounded").setTitle("A softer monthly report").setTooltipIndicator(SwChart.Indicator.LINE)
                .addSeries(ChartSeries.bar("Desktop", Month::desktop).setColors("#4338ca", "#a5b4fc").setBarRadius(12, 12, 0, 0)));
        add(new SwChart<>("roundedHorizontal", Model.ofList(clients()), Client::name).setTitle("Rounded ends, exact totals")
                .setOrientation(SwChart.Orientation.HORIZONTAL).setCategoryHeader("Client")
                .setTooltipIndicator(SwChart.Indicator.DASHED)
                .addSeries(ChartSeries.bar("Delivered", Client::delivered).setStack("hours").setBarRadius(0))
                .addSeries(ChartSeries.<Client>bar("Remaining", row -> Math.max(0, row.planned() - row.delivered()))
                        .setStack("hours").setBarRadius(0, 8, 8, 0)));
        add(new SwChart<>("activity", Model.ofList(java.util.stream.IntStream.range(0, 24)
                .mapToObj(i -> new Revenue(LocalDate.of(2026, 11, i + 1), BigDecimal.valueOf(24 + i * 2 + i % 4 * 7))).toList()), Revenue::date)
                .setTitle("Daily studio activity").setCategoryHeader("Date").setAxisCategoryFormat(SwChart.CategoryFormat.DAY)
                .setValueTickCount(4).setMaxCategoryTicks(6).setMinCategoryTickGap(12)
                .setLegendPosition(SwChart.LegendPosition.TOP).setLegendAlignment(SwChart.LegendAlignment.END)
                .setShowTooltipLabel(false).setShowTooltipIndicator(false).setShowTooltipCursor(false)
                .addSeries(ChartSeries.line("Activity", Revenue::amount).setCurve(ChartSeries.Curve.STEP_MIDDLE)));
        var appearanceSeries = ChartSeries.bar("Delivered", Month::desktop).setColors("#047857", "#34d399").setBarRadius(0, 10, 10, 0);
        IModel<List<Month>> appearanceData = Model.ofList(months());
        var liveAppearance = new SwChart<>("liveAppearance", appearanceData, Month::name).setTitle("An adaptable report")
                .setOrientation(SwChart.Orientation.HORIZONTAL).setTooltipIndicator(SwChart.Indicator.LINE)
                .setLegendPosition(SwChart.LegendPosition.TOP).setLegendAlignment(SwChart.LegendAlignment.START).addSeries(appearanceSeries);
        add(liveAppearance);
        int[] appearanceUpdates = {0};
        add(new SwChart<>("horizontal", Model.ofList(clients()), Client::name)
                        .setOrientation(SwChart.Orientation.HORIZONTAL)
                        .setTitle("Hours by client")
                        .setCategoryHeader("Client")
                        .addSeries(ChartSeries.bar("Delivered", Client::delivered)));
        add(new SwChart<>("horizontalGrouped", Model.ofList(clients()), Client::name)
                        .setOrientation(SwChart.Orientation.HORIZONTAL)
                        .setTitle("Planned and delivered")
                        .setCategoryHeader("Client")
                        .addSeries(ChartSeries.bar("Delivered", Client::delivered))
                        .addSeries(ChartSeries.bar("Planned", Client::planned)));
        add(new SwChart<>("horizontalStacked",
                Model.ofList(List.of(new Month("Design systems", 16.0, 8.0), new Month("Platform engineering", -12.0, -8.0),
                        new Month("Research and discovery", -8.0, 12.0), new Month("Client support", 0.0, null))),
                Month::name)
                        .setOrientation(SwChart.Orientation.HORIZONTAL)
                        .setTitle("Change in planned hours")
                        .setCategoryHeader("Team")
                        .addSeries(ChartSeries.bar("This week", Month::desktop).setStack("change"))
                        .addSeries(ChartSeries.bar("Next week", Month::mobile).setStack("change")));
        add(new SwDonutChart<>("donut", Model.ofList(clients()), Client::name, Client::delivered)
                        .setTitle("Delivered hours by client").setCategoryHeader("Client").setValueHeader("Hours"));
        add(new SwDonutChart<>("donutGrouped", Model.ofList(regions()), Region::name, Region::revenue)
                        .setTitle("Revenue by region").setCategoryHeader("Region").setValueHeader("Revenue")
                        .setCurrency(Currency.getInstance("EUR")));
        add(new SwDonutChart<>("donutAll", Model.ofList(regions()), Region::name, Region::revenue)
                        .setTitle("Every region in view").setCategoryHeader("Region").setGroupSmallSegments(false));
        add(new SwDonutChart<>("donutZero",
                Model.ofList(List.of(new Region("Not started", 0), new Region("Not reported", null))), Region::name, Region::revenue)
                        .setTitle("Before the first share"));
        add(new SwDonutChart<Region>("donutEmpty", Model.ofList(List.of()), Region::name, Region::revenue)
                        .setTitle("Your next breakdown"));
        IModel<List<Region>> liveRegions = Model.ofList(regions());
        var liveDonut = new SwDonutChart<>("liveDonut", liveRegions, Region::name, Region::revenue)
                                .setTitle("Live regional breakdown").setCategoryHeader("Region");
        add(liveDonut);
        IModel<List<Month>> changes = Model.ofList(months());
        var live = new SwChart<>("live", changes, Month::name)
                           .setTitle("Live report")
                           .addSeries(ChartSeries.area("Desktop", Month::desktop).setValueLabels(ChartSeries.ValueLabels.LAST));
        add(live);
        var liveHorizontal = new SwChart<>("liveHorizontal", changes, Month::name)
                                     .setOrientation(SwChart.Orientation.HORIZONTAL)
                                     .setTitle("Live horizontal report")
                                     .setCategoryHeader("Month")
                                     .addSeries(ChartSeries.bar("Desktop", Month::desktop))
                                     .addSeries(ChartSeries.bar("Mobile", Month::mobile));
        add(liveHorizontal);
        add(SwButton.onClick("refresh", "Refresh report", target -> {
            changes.setObject(List.of(new Month("July", 250.0, 100.0), new Month("August", 320.0, 120.0),
                    new Month("September", 280.0, 110.0)));
            liveRegions.setObject(List.of(new Region("Europe", 75), new Region("Americas", 25)));
            delivered.setObject(List.of(52, 45, 68, 72, 84, 96));
            forecastData.setObject(List.of(new Month("January", 120.0, null), new Month("February", 260.0, null),
                    new Month("March", 410.0, null), new Month("April", 560.0, null), new Month("May", null, 700.0), new Month("June", null, 850.0)));
            currentRevenue.setObject(List.of(new Revenue(LocalDate.of(2026, 9, 1), new BigDecimal("18000.50")),
                    new Revenue(LocalDate.of(2026, 10, 1), new BigDecimal("22500.75"))));
            appearanceData.setObject(changes.getObject());
            if (++appearanceUpdates[0] == 1) appearanceSeries.setColors("#6d28d9", "#c4b5fd").setBarRadius(10, 0, 0, 10);
            else appearanceSeries.setColor(3).setBarRadius(0);
            target.add(live, liveHorizontal, liveDonut, hoursCard, liveForecast, liveFormatted, liveAppearance);
        }));
        add(new SwChart<Month>("empty", Model.ofList(List.of()), Month::name)
                        .setTitle("Your next report")
                        .addSeries(ChartSeries.bar("Visitors", Month::desktop)));
        add(new SwChart<>("gaps",
                Model.ofList(List.of(new Month("January", 20.0, 10.0), new Month("February", null, -5.0),
                        new Month("March", -10.0, 15.0), new Month("April", 30.0, 25.0))),
                Month::name)
                        .setTitle("Honest gaps and negative values")
                        .addSeries(ChartSeries.line("Actual", Month::desktop).setCurve(ChartSeries.Curve.LINEAR))
                        .addSeries(ChartSeries.line("Previous", Month::mobile).setCurve(ChartSeries.Curve.STEP)));
    }

    private static SwChart<Month> chart(String id) {
        return new SwChart<>(id, Model.ofList(months()), Month::name).setCategoryHeader("Month");
    }

    @Override
    protected String getTitle() {
        return "Charts";
    }
}

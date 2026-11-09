package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwCard;
import dev.jbaby.shadwick.components.chart.*;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.*;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;

/** Server SVG and exact tables, enhanced with keyboard/hover tooltips. */
public class ChartPage extends BasePage {

    public record Month(String name, Double desktop, Double mobile) implements Serializable {}

    public record Client(String name, Double delivered, Double planned) implements Serializable {}

    public record Region(String name, Number revenue) implements Serializable {}

    public record Measurement(String name, Number hours, Number revenue) implements Serializable {}

    public record Capability(String name, Number current, Number target) implements Serializable {}

    public record RadialMeasure(String name, Number value, int color) implements Serializable {}

    public record HeatmapRow(String name, Number mon, Number tue, Number wed, Number thu, Number fri) implements Serializable {}

    public record Trend(String team, List<Integer> values) implements Serializable {}

    public record Revenue(LocalDate date, BigDecimal amount) implements Serializable {}

    public record Invoice(String client, String reference, BigDecimal amount)
            implements Serializable {}

    public static List<Month> months() {
        return List.of(
                new Month("January", 186.0, 80.0),
                new Month("February", 305.0, 200.0),
                new Month("March", 237.0, 120.0),
                new Month("April", 73.0, 190.0),
                new Month("May", 209.0, 130.0),
                new Month("June", 214.0, 140.0));
    }

    public ChartPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        IModel<List<Integer>> delivered = Model.ofList(List.of(48, 52, 45, 68, 72, 84));
        SwCard hoursCard = summaryCards(delivered);
        addTeamTrends();
        addChartTypes();
        addPercentCharts();
        addReferenceCharts();
        addLiveReferenceCharts();
        addScatterCharts();
        addLiveScatterChart();
        addRadarCharts();
        addLiveRadarChart();
        addRadialCharts();
        addLiveRadialChart();
        addHeatmaps();
        addLiveHeatmap();
        addValueLabels();
        addForecasts();
        addFormattingExamples();
        addAppearanceExamples();
        addHorizontalCharts();
        addDonutCharts();
        addPieCharts();
        addLiveCharts(delivered, hoursCard);
        addBoundaryCharts();
    }

    private SwCard summaryCards(IModel<List<Integer>> delivered) {
        var hoursCard = new SwCard("hoursCard");
        hoursCard.setOutputMarkupId(true);
        hoursCard.add(
                new Label(
                        "value",
                        (IModel<String>)
                                () ->
                                        NumberFormat.getIntegerInstance(getLocale())
                                                        .format(
                                                                delivered
                                                                        .getObject()
                                                                        .get(
                                                                                delivered
                                                                                                .getObject()
                                                                                                .size()
                                                                                        - 1))
                                                + " h"));
        hoursCard.add(new SwSparkline("trend", delivered).setFilled(true));
        add(hoursCard);
        var projectsCard = new SwCard("projectsCard");
        projectsCard.add(
                new SwSparkline("trend", Model.ofList(List.of(8, 10, 9, 13, 11, 12))).setColor(2));
        add(projectsCard);
        var satisfactionCard = new SwCard("satisfactionCard");
        satisfactionCard.add(
                new Label(
                        "value",
                        (IModel<String>)
                                () -> NumberFormat.getNumberInstance(getLocale()).format(4.8)));
        satisfactionCard.add(
                new SwSparkline("trend", Model.ofList(Arrays.asList(4.4, 4.5, null, 4.6, 4.7, 4.8)))
                        .setColor(3));
        add(satisfactionCard);
        return hoursCard;
    }

    private void addTeamTrends() {
        add(
                new ListView<>(
                        "trendRows",
                        List.of(
                                new Trend("Design", List.of(24, 30, 28, 36, 40, 42)),
                                new Trend("Engineering", List.of(48, 50, 56, 52, 64, 68)),
                                new Trend("Support", Arrays.asList(20, null, 18, 24, null, 24)),
                                new Trend("Research", List.of(16, 16, 16, 16)),
                                new Trend("New team", Collections.singletonList(8)))) {

                    @Override
                    protected void populateItem(ListItem<Trend> item) {
                        Trend row = item.getModelObject();
                        item.add(new Label("team", row.team()));
                        item.add(
                                new Label(
                                        "current",
                                        NumberFormat.getIntegerInstance(getLocale())
                                                .format(
                                                        row.values()
                                                                .get(row.values().size() - 1))));
                        item.add(new SwSparkline("trend", Model.ofList(row.values())));
                    }
                });
    }

    private void addChartTypes() {
        add(
                chart("bars")
                        .setTitle("Visitors by device")
                        .addSeries(ChartSeries.bar("Desktop", Month::desktop))
                        .addSeries(ChartSeries.bar("Mobile", Month::mobile)));
        add(
                chart("stacked")
                        .setTitle("A shared picture")
                        .addSeries(ChartSeries.bar("Desktop", Month::desktop).setStack("visitors"))
                        .addSeries(ChartSeries.bar("Mobile", Month::mobile).setStack("visitors")));
        add(
                chart("lines")
                        .setTitle("Steady progress")
                        .addSeries(ChartSeries.line("Desktop", Month::desktop).setDots(true))
                        .addSeries(ChartSeries.line("Mobile", Month::mobile).setDashed(true)));
        add(
                chart("areas")
                        .setTitle("Room to grow")
                        .addSeries(ChartSeries.area("Desktop", Month::desktop).setStack("visitors"))
                        .addSeries(ChartSeries.area("Mobile", Month::mobile).setStack("visitors")));
        add(
                chart("composed")
                        .setTitle("Revenue and target")
                        .setCurrency(Currency.getInstance("EUR"))
                        .addSeries(ChartSeries.bar("Revenue", Month::desktop))
                        .addSeries(ChartSeries.line("Target", Month::mobile).setDashed(true)));
    }

    private void addPercentCharts() {
        IModel<List<Month>> mix = Model.ofList(List.of(
                new Month("January", 30.0, 70.0),
                new Month("February", 120.0, 80.0),
                new Month("March", 0.0, 0.0),
                new Month("April", null, 50.0),
                new Month("May", 60.0, 40.0),
                new Month("June", 80.0, 20.0)));
        add(percentBars("percentBars", "Device share by month", mix));
        add(percentBars("percentHorizontal", "Device share across rows", mix)
                .setOrientation(SwChart.Orientation.HORIZONTAL));
        add(percentAreas("percentAreas", "A changing device mix", mix));
    }

    private SwChart<Month> percentBars(String id, String title, IModel<List<Month>> data) {
        return new SwChart<>(id, data, Month::name)
                .setTitle(title)
                .setCategoryHeader("Month")
                .setStackMode(SwChart.StackMode.PERCENT)
                .addSeries(ChartSeries.bar("Desktop", Month::desktop).setStack("devices").setBarRadius(0))
                .addSeries(ChartSeries.bar("Mobile", Month::mobile).setStack("devices").setBarRadius(0));
    }

    private SwChart<Month> percentAreas(String id, String title, IModel<List<Month>> data) {
        return new SwChart<>(id, data, Month::name)
                .setTitle(title)
                .setCategoryHeader("Month")
                .setStackMode(SwChart.StackMode.PERCENT)
                .addSeries(ChartSeries.area("Desktop", Month::desktop).setStack("devices").setCurve(ChartSeries.Curve.LINEAR))
                .addSeries(ChartSeries.area("Mobile", Month::mobile).setStack("devices").setCurve(ChartSeries.Curve.LINEAR));
    }

    private void addReferenceCharts() {
        var revenue = referenceRevenue("referenceRevenue", "Revenue against a target", Model.ofList(months()));
        revenue.addReference(ChartReference.band("Planning range", 150, 250).setColor(2));
        revenue.addReference(ChartReference.line("Quarter target", 350).setColor(3));
        add(revenue);

        var hours = new SwChart<>("referenceHours", Model.ofList(clients()), Client::name)
                .setTitle("Hours with a delivery goal")
                .setCategoryHeader("Client")
                .setOrientation(SwChart.Orientation.HORIZONTAL)
                .addSeries(ChartSeries.bar("Delivered", Client::delivered).setBarRadius(0))
                .addReference(ChartReference.band("Expected hours", 50, 80).setColor(2))
                .addReference(ChartReference.line("Delivery goal", 70).setColor(3).setDashed(false));
        add(hours);

        add(percentAreas("referencePercent", "A balanced device mix", Model.ofList(months()))
                .addReference(ChartReference.band("Balanced desktop share", .4, .6).setColor(3))
                .addReference(ChartReference.line("Desktop share goal", .5).setColor(3)));
    }

    private SwChart<Month> referenceRevenue(String id, String title, IModel<List<Month>> data) {
        return new SwChart<>(id, data, Month::name)
                .setTitle(title)
                .setCategoryHeader("Month")
                .setCurrency(Currency.getInstance("EUR"))
                .addSeries(ChartSeries.bar("Revenue", Month::desktop).setBarRadius(0));
    }

    private void addLiveReferenceCharts() {
        IModel<List<Month>> data = Model.ofList(months());
        IModel<Integer> goal = Model.of(300), lower = Model.of(150), upper = Model.of(250);
        var vertical = referenceRevenue("liveReference", "An updated revenue target", data)
                .addReference(ChartReference.line("Target", goal).setColor(3))
                .addReference(ChartReference.band("Planning range", lower, upper).setColor(2));
        var horizontal = referenceRevenue("liveReferenceHorizontal", "An updated revenue target by row", data)
                .setOrientation(SwChart.Orientation.HORIZONTAL)
                .addReference(ChartReference.line("Target", goal).setColor(3))
                .addReference(ChartReference.band("Planning range", lower, upper).setColor(2));
        add(vertical, horizontal);
        add(SwButton.onClick("refreshReferences", "Refresh targets", target -> {
            goal.setObject(350);
            lower.setObject(200);
            upper.setObject(300);
            data.setObject(List.of(new Month("July", 250.0, 100.0), new Month("August", 320.0, 120.0),
                    new Month("September", 280.0, 110.0)));
            target.add(vertical, horizontal);
        }));
    }

    private void addScatterCharts() {
        add(projectScatter("scatter", "Effort and revenue")
                .addSeries(ScatterSeries.of("Design", Model.ofList(designMeasurements()),
                        Measurement::name, Measurement::hours, Measurement::revenue).setPointRadius(5))
                .addSeries(ScatterSeries.of("Engineering", Model.ofList(engineeringMeasurements()),
                        Measurement::name, Measurement::hours, Measurement::revenue)
                        .setShape(ScatterSeries.Shape.SQUARE).setPointRadius(5)));
        add(projectScatter("scatterSigned", "Gains and setbacks")
                .setXLabel("Change in hours").setYLabel("Change in revenue").setIncludeZero(true)
                .addSeries(ScatterSeries.of("Changes", Model.ofList(List.of(
                        new Measurement("Reduced scope", -20, -1000), new Measurement("Same effort", 0, 100),
                        new Measurement("Expanded scope", 20, 1400), new Measurement("Rework", 10, -300))),
                        Measurement::name, Measurement::hours, Measurement::revenue).setColor(3)));
        add(projectScatter("scatterOverlap", "Overlapping measurements")
                .addSeries(ScatterSeries.of("Research", Model.ofList(List.of(
                        new Measurement("Same coordinate A", 20, 3000), new Measurement("Same coordinate B", 20, 3000),
                        new Measurement("Separate point", 40, 5000))), Measurement::name, Measurement::hours, Measurement::revenue)
                        .setShape(ScatterSeries.Shape.DIAMOND).setPointRadius(7).setColor(4)));
        add(projectScatter("scatterQuiet", "A quiet relationship").setShowTooltip(false).setXTickCount(9)
                .addSeries(ScatterSeries.of("Design", Model.ofList(designMeasurements()),
                        Measurement::name, Measurement::hours, Measurement::revenue)));
        add(projectScatter("scatterSingle", "One completed project")
                .addSeries(ScatterSeries.of("Delivery", Model.ofList(List.of(new Measurement("First project", 20, 3000))),
                        Measurement::name, Measurement::hours, Measurement::revenue)));
        add(projectScatter("scatterMissing", "Waiting for complete reports")
                .addSeries(ScatterSeries.of("Delivery", Model.ofList(List.of(
                        new Measurement("Hours only", 20, null), new Measurement("Revenue only", null, 3000))),
                        Measurement::name, Measurement::hours, Measurement::revenue)));
        add(projectScatter("scatterEmpty", "The next relationship")
                .addSeries(ScatterSeries.of("Delivery", Model.ofList(List.<Measurement>of()),
                        Measurement::name, Measurement::hours, Measurement::revenue)));
    }

    private SwScatterChart projectScatter(String id, String title) {
        return new SwScatterChart(id).setTitle(title).setXLabel("Effort (hours)").setYLabel("Revenue")
                .setPointHeader("Project").setYCurrency(Currency.getInstance("EUR"));
    }

    private static List<Measurement> designMeasurements() {
        return List.of(new Measurement("Aster", 12, 2400), new Measurement("Birch", 20, 3600),
                new Measurement("Cedar", 32, 5200), new Measurement("Dune", 44, 4800),
                new Measurement("Awaiting invoice", 1000, null));
    }

    private static List<Measurement> engineeringMeasurements() {
        return List.of(new Measurement("Atlas", 16, 2800), new Measurement("Beacon", 28, 4200),
                new Measurement("Cirrus", 40, 6200));
    }

    private void addLiveScatterChart() {
        IModel<List<Measurement>> design = Model.ofList(designMeasurements());
        IModel<List<Measurement>> engineering = Model.ofList(engineeringMeasurements());
        var engineeringSeries = ScatterSeries.of("Engineering", engineering, Measurement::name, Measurement::hours, Measurement::revenue)
                .setShape(ScatterSeries.Shape.SQUARE);
        var chart = projectScatter("liveScatter", "Fresh delivery relationships")
                .addSeries(ScatterSeries.of("Design", design, Measurement::name, Measurement::hours, Measurement::revenue))
                .addSeries(engineeringSeries);
        add(chart);
        add(SwButton.onClick("refreshScatter", "Refresh measurements", target -> {
            updateMeasurements(design, engineering);
            engineeringSeries.setShape(ScatterSeries.Shape.DIAMOND).setColor(3).setPointRadius(6);
            target.add(chart);
        }));
    }

    private static void updateMeasurements(IModel<List<Measurement>> design, IModel<List<Measurement>> engineering) {
        design.setObject(List.of(new Measurement("Clover launch", 48, 7200), new Measurement("River launch", 36, 6100)));
        engineering.setObject(List.of(new Measurement("Summit launch", 52, 8500), new Measurement("Forecast pending", 60, null)));
    }

    private void addRadarCharts() {
        add(radarProfile("radar", "Team capability profile", Model.ofList(capabilities())));
        add(radarProfile("radarMissing", "An incomplete assessment", Model.ofList(List.of(
                new Capability("Delivery", 72, 90), new Capability("Quality", 88, 92),
                new Capability("Communication", 80, 85), new Capability("Planning", null, 82),
                new Capability("Research", 78, 88), new Capability("Support", 84, 86)))));
        add(radarProfile("radarLong", "Detailed capability dimensions", Model.ofList(List.of(
                new Capability("Clear communication across distributed project teams", 78, 90),
                new Capability("Quality of independently reviewed deliverables", 88, 92),
                new Capability("Reliability of milestone and dependency planning", 65, 85),
                new Capability("Research that answers the client's underlying question", 80, 90),
                new Capability("Support throughout handover and adoption", 82, 88),
                new Capability("Consistent delivery under changing requirements", 72, 90)))));
        add(radarProfile("radarAuto", "An automatic workload scale", Model.ofList(List.of(
                new Capability("Design", 45, 60), new Capability("Engineering", 120, 160),
                new Capability("Support", 80, 100), new Capability("Research", 65, 75))))
                .setCategoryHeader("Team").setAutoScale());
        add(radarProfile("radarZero", "A measured zero", Model.ofList(List.of(
                new Capability("Delivery", 0, 0), new Capability("Quality", 0, 0), new Capability("Planning", 0, 0))))
                .setAutoScale());
        add(radarProfile("radarShort", "First assessment", Model.ofList(List.of(
                new Capability("Delivery", 72, 90), new Capability("Quality", 88, 92)))));
        add(radarProfile("radarUnknown", "No measurements yet", Model.ofList(List.of(
                new Capability("Delivery", null, null), new Capability("Quality", null, null), new Capability("Planning", null, null)))));
        add(radarProfile("radarEmpty", "The next team profile", Model.ofList(List.of())));
    }

    private SwRadarChart<Capability> radarProfile(String id, String title, IModel<List<Capability>> data) {
        return new SwRadarChart<>(id, data, Capability::name).setTitle(title).setMaximum(100)
                .addSeries(RadarSeries.of("Current", Capability::current))
                .addSeries(RadarSeries.of("Target", Capability::target).setDashed(true).setFillOpacity(0).setDots(false));
    }

    private static List<Capability> capabilities() {
        return List.of(new Capability("Delivery", 72, 90), new Capability("Quality", 88, 92),
                new Capability("Communication", 80, 85), new Capability("Planning", 65, 82),
                new Capability("Research", 78, 88), new Capability("Support", 84, 86));
    }

    private void addLiveRadarChart() {
        IModel<List<Capability>> data = Model.ofList(capabilities());
        IModel<Integer> maximum = Model.of(100);
        IModel<String> label = Model.of("Current");
        var current = RadarSeries.of(label, Capability::current);
        var chart = new SwRadarChart<>("liveRadar", data, Capability::name)
                .setTitle("A changing capability profile").setMaximum(maximum).addSeries(current)
                .addSeries(RadarSeries.of("Target", Capability::target).setDashed(true).setFillOpacity(0).setDots(false));
        add(chart);
        add(SwButton.onClick("refreshRadar", "Refresh assessment", target -> {
            updateAssessment(data, maximum, label);
            current.setColor(3).setFillOpacity(.22);
            target.add(chart);
        }));
    }

    private static void updateAssessment(IModel<List<Capability>> data, IModel<Integer> maximum, IModel<String> label) {
        maximum.setObject(120);
        label.setObject("Latest");
        data.setObject(List.of(new Capability("Delivery", 97, 110), new Capability("Quality", 111, 115),
                new Capability("Communication", 102, 108), new Capability("Planning", 88, 100)));
    }

    private void addRadialCharts() {
        add(radialReport("radial", "Delivery goals", Model.ofList(radialMeasures())));
        add(radialReport("radialSweep", "A three-quarter comparison", Model.ofList(radialMeasures()))
                .setStartAngle(135).setSweepAngle(270).setRounded(true).setInnerRadius(.45).setGap(.25));
        add(radialReport("radialHalf", "A quiet half-circle", Model.ofList(radialMeasures()))
                .setStartAngle(180).setSweepAngle(180).setShowLegend(false).setShowTooltip(false));
        add(radialReport("radialAuto", "Monthly support hours", Model.ofList(List.of(
                new RadialMeasure("Design", 45, 1), new RadialMeasure("Engineering", 85, 2),
                new RadialMeasure("Research", 70, 3)))).setAutoScale().setValueHeader("Hours"));
        add(radialReport("radialMixed", "Measured and pending", Model.ofList(List.of(
                new RadialMeasure("Delivered", 60, 1), new RadialMeasure("Recorded zero", 0, 2),
                new RadialMeasure("Awaiting a report", null, 3), new RadialMeasure("A very small measurement", new BigDecimal("1e-30"), 4))))
                .setValueFormat((number, locale) -> number instanceof BigDecimal decimal
                        ? decimal.toEngineeringString() : NumberFormat.getNumberInstance(locale).format(number)));
        add(radialReport("radialSingle", "One launch target", Model.ofList(List.of(new RadialMeasure("Launch readiness", 82, 3))))
                .setRounded(true).setInnerRadius(.7).setGap(.1));
        add(radialReport("radialZero", "A measured zero report", Model.ofList(List.of(
                new RadialMeasure("Design", 0, 1), new RadialMeasure("Engineering", 0, 2)))).setAutoScale());
        add(radialReport("radialUnknown", "Waiting for measurements", Model.ofList(List.of(
                new RadialMeasure("Design", null, 1), new RadialMeasure("Engineering", null, 2)))));
        add(radialReport("radialEmpty", "The next radial report", Model.ofList(List.of())));
    }

    private SwRadialChart<RadialMeasure> radialReport(String id, String title, IModel<List<RadialMeasure>> data) {
        return new SwRadialChart<>(id, data, RadialMeasure::name, RadialMeasure::value)
                .setTitle(title).setMaximum(100).setValueHeader("Score").setColor(RadialMeasure::color);
    }

    private static List<RadialMeasure> radialMeasures() {
        return List.of(new RadialMeasure("Delivery", 78, 1), new RadialMeasure("Quality", 92, 2),
                new RadialMeasure("Planning", 64, 3), new RadialMeasure("Research", 86, 4));
    }

    private void addLiveRadialChart() {
        IModel<List<RadialMeasure>> data = Model.ofList(radialMeasures());
        IModel<Integer> maximum = Model.of(100);
        IModel<String> label = Model.of("Score");
        var chart = radialReport("liveRadial", "A changing delivery report", data)
                .setMaximum(maximum).setValueHeader(label).setStartAngle(135).setSweepAngle(270);
        add(chart);
        add(SwButton.onClick("refreshRadial", "Refresh radial report", target -> {
            updateRadialReport(data, maximum, label);
            chart.setStartAngle(180).setSweepAngle(180).setRounded(true);
            target.add(chart);
        }));
    }

    private static void updateRadialReport(IModel<List<RadialMeasure>> data, IModel<Integer> maximum, IModel<String> label) {
        maximum.setObject(120);
        label.setObject("Latest");
        data.setObject(List.of(new RadialMeasure("Delivery", 96, 3), new RadialMeasure("Quality", 111, 2),
                new RadialMeasure("Planning", 90, 5)));
    }

    private void addHeatmaps() {
        add(weeklyHeatmap("heatmap", "Weekly team workload", Model.ofList(weeklyWorkload()), Model.of("Monday"))
                .setRange(0, 50));
        add(weeklyHeatmap("heatmapColors", "A quieter workload overview", Model.ofList(weeklyWorkload()), Model.of("Monday"))
                .setRange(0, 50).setColor(2).setShowValues(false));
        add(weeklyHeatmap("heatmapSigned", "Changes against the plan", Model.ofList(List.of(
                new HeatmapRow("Design", -4, 0, 5, -2, 8), new HeatmapRow("Engineering", 6, -8, 12, 0, -6),
                new HeatmapRow("Support", -10, 2, null, 4, -1))), Model.of("Monday"))
                .setRange(-10, 15).setColor(4).setValueHeader("Change in hours"));
        add(longLabelHeatmap());
        add(weeklyHeatmap("heatmapZero", "A measured zero week", Model.ofList(List.of(
                new HeatmapRow("Design", 0, 0, 0, 0, 0))), Model.of("Monday")));
        add(weeklyHeatmap("heatmapUnknown", "Waiting for weekly reports", Model.ofList(List.of(
                new HeatmapRow("Design", null, null, null, null, null))), Model.of("Monday")));
        add(weeklyHeatmap("heatmapEmpty", "The next workload matrix", Model.ofList(List.of()), Model.of("Monday")));
        add(new SwHeatmapChart<>("heatmapSingle", Model.ofList(List.of(new HeatmapRow("Design", 7, null, null, null, null))), HeatmapRow::name)
                .setTitle("One team, one report").setRowHeader("Team").setValueHeader("Hours")
                .addColumn(HeatmapColumn.of("Monday", HeatmapRow::mon)));
        add(weeklyHeatmap("heatmapTiny", "Small measurements, exact bands", Model.ofList(List.of(
                new HeatmapRow("Measured", new BigDecimal("1e-1000"), new BigDecimal("4e-1001"), 0, null, new BigDecimal("2e-1001")))), Model.of("Monday"))
                .setValueFormat((number, locale) -> number.toString()));
    }

    private SwHeatmapChart<HeatmapRow> weeklyHeatmap(String id, String title, IModel<List<HeatmapRow>> data, IModel<String> firstDay) {
        return new SwHeatmapChart<>(id, data, HeatmapRow::name).setTitle(title).setRowHeader("Team").setValueHeader("Hours")
                .addColumn(HeatmapColumn.of(firstDay, HeatmapRow::mon))
                .addColumn(HeatmapColumn.of("Tuesday", HeatmapRow::tue))
                .addColumn(HeatmapColumn.of("Wednesday", HeatmapRow::wed))
                .addColumn(HeatmapColumn.of("Thursday", HeatmapRow::thu))
                .addColumn(HeatmapColumn.of("Friday", HeatmapRow::fri));
    }

    private static List<HeatmapRow> weeklyWorkload() {
        return List.of(new HeatmapRow("Design", 12, 18, 24, 8, 0), new HeatmapRow("Engineering", 30, 36, 42, 20, 16),
                new HeatmapRow("Support", 8, 12, null, 18, 24));
    }

    private SwHeatmapChart<HeatmapRow> longLabelHeatmap() {
        return new SwHeatmapChart<>("heatmapLong", Model.ofList(List.of(
                new HeatmapRow("Independent research and distributed collaboration", 12, 18, 24, 8, 0),
                new HeatmapRow("Quality review across the project portfolio", 30, 36, 42, 20, 16))), HeatmapRow::name)
                .setTitle("Long team and reporting labels").setRowHeader("Project team").setValueHeader("Hours").setColor(3)
                .setShowTooltip(false)
                .addColumn(HeatmapColumn.of("Planning and handover", HeatmapRow::mon))
                .addColumn(HeatmapColumn.of("Research and experiments", HeatmapRow::tue))
                .addColumn(HeatmapColumn.of("Delivery and coordination", HeatmapRow::wed))
                .addColumn(HeatmapColumn.of("Independent review", HeatmapRow::thu))
                .addColumn(HeatmapColumn.of("Customer follow-through", HeatmapRow::fri));
    }

    private void addLiveHeatmap() {
        IModel<List<HeatmapRow>> data = Model.ofList(weeklyWorkload());
        IModel<Integer> lower = Model.of(0), upper = Model.of(50);
        IModel<String> firstDay = Model.of("Monday"), valueHeader = Model.of("Hours");
        var chart = weeklyHeatmap("liveHeatmap", "A changing workload matrix", data, firstDay)
                .setRange(lower, upper).setValueHeader(valueHeader);
        add(chart);
        add(SwButton.onClick("refreshHeatmap", "Refresh workload matrix", target -> {
            updateWorkload(data, upper, firstDay, valueHeader);
            chart.setColor(5).setShowValues(false);
            target.add(chart);
        }));
    }

    private static void updateWorkload(IModel<List<HeatmapRow>> data, IModel<Integer> upper, IModel<String> firstDay, IModel<String> valueHeader) {
        upper.setObject(60);
        firstDay.setObject("Latest Monday");
        valueHeader.setObject("Updated hours");
        data.setObject(List.of(new HeatmapRow("Delivery", 34, 42, 18, null, 60), new HeatmapRow("Research", 8, 12, 24, 30, 16)));
    }

    private void addValueLabels() {
        add(
                chart("labeledBars")
                        .setTitle("Highest and latest months")
                        .addSeries(
                                ChartSeries.bar("Visitors", Month::desktop)
                                        .setValueLabels(ChartSeries.ValueLabels.HIGHEST_AND_LAST)));
        add(
                chart("labeledLines")
                        .setTitle("Latest values at a glance")
                        .addSeries(
                                ChartSeries.line("Desktop", Month::desktop)
                                        .setValueLabels(ChartSeries.ValueLabels.LAST))
                        .addSeries(
                                ChartSeries.line("Mobile", Month::mobile)
                                        .setDashed(true)
                                        .setValueLabels(ChartSeries.ValueLabels.LAST)));
        add(
                new SwChart<>("labeledHorizontal", Model.ofList(clients()), Client::name)
                        .setOrientation(SwChart.Orientation.HORIZONTAL)
                        .setTitle("Hours without hovering")
                        .setCategoryHeader("Client")
                        .setShowTooltip(false)
                        .addSeries(
                                ChartSeries.bar("Delivered", Client::delivered)
                                        .setValueLabels(ChartSeries.ValueLabels.ALL)));
        add(
                new SwChart<>(
                                "denseLabels",
                                Model.ofList(
                                        java.util.stream.IntStream.range(0, 24)
                                                .mapToObj(
                                                        i ->
                                                                new Month(
                                                                        "Day " + (i + 1),
                                                                        100.0 + i % 3,
                                                                        0.0))
                                                .toList()),
                                Month::name)
                        .setTitle("Labels that make room")
                        .setShowXAxis(false)
                        .setCategoryHeader("Day")
                        .addSeries(
                                ChartSeries.line("Daily", Month::desktop)
                                        .setValueLabels(ChartSeries.ValueLabels.ALL)));
    }

    private void addForecasts() {
        add(
                forecast(
                        "forecastLine",
                        "Actual into forecast",
                        Model.ofList(forecastMonths()),
                        false));
        add(
                forecast(
                        "forecastArea",
                        "A forecast with context",
                        Model.ofList(forecastMonths()),
                        true));
        var gapped = new ArrayList<>(forecastMonths());
        gapped.set(4, new Month("May", null, null));
        add(
                forecast(
                        "forecastGaps",
                        "A forecast with a missing report",
                        Model.ofList(gapped),
                        false));
    }

    private void addFormattingExamples() {
        add(
                revenueChart(
                        "formattedRevenue",
                        "Revenue, from overview to detail",
                        Model.ofList(revenue())));
        add(
                revenueChart(
                                "formattedHorizontal",
                                "Monthly revenue, side by side",
                                Model.ofList(revenue()))
                        .setOrientation(SwChart.Orientation.HORIZONTAL));
        add(
                new SwChart<>(
                                "formattedInvoices",
                                Model.ofList(
                                        List.of(
                                                new Invoice(
                                                        "Northstar Foundation",
                                                        "INV-2041",
                                                        new BigDecimal("4800.50")),
                                                new Invoice(
                                                        "Harbor & Pine",
                                                        "INV-2042",
                                                        new BigDecimal("3250.00")),
                                                new Invoice(
                                                        "Greenfield Community",
                                                        "INV-2043",
                                                        new BigDecimal("1900.75")))),
                                Invoice::client)
                        .setTitle("The right context for an invoice")
                        .setCategoryHeader("Client")
                        .setOrientation(SwChart.Orientation.HORIZONTAL)
                        .setAxisValueFormat(SwChart.ValueFormat.COMPACT)
                        .setAxisCategoryFormat((value, locale) -> value.toString().split(" ")[0])
                        .setTooltipLabel(row -> row.client() + " · " + row.reference())
                        .setTooltipValueFormat(
                                (value, locale) ->
                                        NumberFormat.getNumberInstance(locale).format(value)
                                                + " USD")
                        .addSeries(ChartSeries.bar("Balance (USD)", Invoice::amount)));
    }

    private void addAppearanceExamples() {
        var brandedActual =
                ChartSeries.area("Actual", Month::desktop)
                        .setColors("#047857", "#34d399")
                        .setCurve(ChartSeries.Curve.NATURAL)
                        .setStrokeWidth(3);
        add(
                new SwChart<>("branded", Model.ofList(forecastMonths()), Month::name)
                        .setTitle("A forecast in your colors")
                        .setLegendPosition(SwChart.LegendPosition.TOP)
                        .setLegendAlignment(SwChart.LegendAlignment.START)
                        .addSeries(brandedActual)
                        .addSeries(
                                ChartSeries.area("Forecast", Month::mobile)
                                        .setColors("#047857", "#34d399")
                                        .setCurve(ChartSeries.Curve.NATURAL)
                                        .setDashed(true)
                                        .setJoin(brandedActual)
                                        .setFillOpacity(.12)
                                        .setStrokeWidth(3)));
        add(
                chart("rounded")
                        .setTitle("A softer monthly report")
                        .setTooltipIndicator(SwChart.Indicator.LINE)
                        .addSeries(
                                ChartSeries.bar("Desktop", Month::desktop)
                                        .setColors("#4338ca", "#a5b4fc")
                                        .setBarRadius(12, 12, 0, 0)));
        add(
                new SwChart<>("roundedHorizontal", Model.ofList(clients()), Client::name)
                        .setTitle("Rounded ends, exact totals")
                        .setOrientation(SwChart.Orientation.HORIZONTAL)
                        .setCategoryHeader("Client")
                        .setTooltipIndicator(SwChart.Indicator.DASHED)
                        .addSeries(
                                ChartSeries.bar("Delivered", Client::delivered)
                                        .setStack("hours")
                                        .setBarRadius(0))
                        .addSeries(
                                ChartSeries.<Client>bar(
                                                "Remaining",
                                                row -> Math.max(0, row.planned() - row.delivered()))
                                        .setStack("hours")
                                        .setBarRadius(0, 8, 8, 0)));
        add(
                new SwChart<>(
                                "activity",
                                Model.ofList(
                                        java.util.stream.IntStream.range(0, 24)
                                                .mapToObj(
                                                        i ->
                                                                new Revenue(
                                                                        LocalDate.of(
                                                                                2026, 11, i + 1),
                                                                        BigDecimal.valueOf(
                                                                                24 + i * 2
                                                                                        + i % 4
                                                                                                * 7)))
                                                .toList()),
                                Revenue::date)
                        .setTitle("Daily studio activity")
                        .setCategoryHeader("Date")
                        .setAxisCategoryFormat(SwChart.CategoryFormat.DAY)
                        .setValueTickCount(4)
                        .setMaxCategoryTicks(6)
                        .setMinCategoryTickGap(12)
                        .setLegendPosition(SwChart.LegendPosition.TOP)
                        .setLegendAlignment(SwChart.LegendAlignment.END)
                        .setShowTooltipLabel(false)
                        .setShowTooltipIndicator(false)
                        .setShowTooltipCursor(false)
                        .addSeries(
                                ChartSeries.line("Activity", Revenue::amount)
                                        .setCurve(ChartSeries.Curve.STEP_MIDDLE)));
    }

    private void addHorizontalCharts() {
        add(
                new SwChart<>("horizontal", Model.ofList(clients()), Client::name)
                        .setOrientation(SwChart.Orientation.HORIZONTAL)
                        .setTitle("Hours by client")
                        .setCategoryHeader("Client")
                        .addSeries(ChartSeries.bar("Delivered", Client::delivered)));
        add(
                new SwChart<>("horizontalGrouped", Model.ofList(clients()), Client::name)
                        .setOrientation(SwChart.Orientation.HORIZONTAL)
                        .setTitle("Planned and delivered")
                        .setCategoryHeader("Client")
                        .addSeries(ChartSeries.bar("Delivered", Client::delivered))
                        .addSeries(ChartSeries.bar("Planned", Client::planned)));
        add(
                new SwChart<>(
                                "horizontalStacked",
                                Model.ofList(
                                        List.of(
                                                new Month("Design systems", 16.0, 8.0),
                                                new Month("Platform engineering", -12.0, -8.0),
                                                new Month("Research and discovery", -8.0, 12.0),
                                                new Month("Client support", 0.0, null))),
                                Month::name)
                        .setOrientation(SwChart.Orientation.HORIZONTAL)
                        .setTitle("Change in planned hours")
                        .setCategoryHeader("Team")
                        .addSeries(ChartSeries.bar("This week", Month::desktop).setStack("change"))
                        .addSeries(ChartSeries.bar("Next week", Month::mobile).setStack("change")));
    }

    private void addDonutCharts() {
        add(
                new SwDonutChart<>(
                                "donut", Model.ofList(clients()), Client::name, Client::delivered)
                        .setTitle("Delivered hours by client")
                        .setCategoryHeader("Client")
                        .setValueHeader("Hours"));
        add(
                new SwDonutChart<>(
                                "donutGrouped",
                                Model.ofList(regions()),
                                Region::name,
                                Region::revenue)
                        .setTitle("Revenue by region")
                        .setCategoryHeader("Region")
                        .setValueHeader("Revenue")
                        .setCurrency(Currency.getInstance("EUR")));
        add(
                new SwDonutChart<>(
                                "donutAll", Model.ofList(regions()), Region::name, Region::revenue)
                        .setTitle("Every region in view")
                        .setCategoryHeader("Region")
                        .setGroupSmallSegments(false));
        add(
                new SwDonutChart<>(
                                "donutZero",
                                Model.ofList(
                                        List.of(
                                                new Region("Not started", 0),
                                                new Region("Not reported", null))),
                                Region::name,
                                Region::revenue)
                        .setTitle("Before the first share"));
        add(
                new SwDonutChart<Region>(
                                "donutEmpty",
                                Model.ofList(List.of()),
                                Region::name,
                                Region::revenue)
                        .setTitle("Your next breakdown"));
    }

    private void addPieCharts() {
        add(pieChart("pie", "Revenue shares", Model.ofList(regions())));
        add(pieChart("pieAll", "Every slice in view", Model.ofList(regions()))
                .setGroupSmallSegments(false));
        add(pieChart("pieSingle", "One active region", Model.ofList(List.of(new Region("Europe", 100)))));
        add(pieChart("pieZero", "Before the first slice", Model.ofList(List.of(
                new Region("Not started", 0), new Region("Not reported", null)))));
    }

    private SwDonutChart<Region> pieChart(String id, String title, IModel<List<Region>> data) {
        return new SwDonutChart<>(id, data, Region::name, Region::revenue)
                .setShape(SwDonutChart.Shape.PIE)
                .setTitle(title)
                .setCategoryHeader("Region")
                .setValueHeader("Revenue")
                .setCurrency(Currency.getInstance("EUR"));
    }

    private void addLiveCharts(IModel<List<Integer>> delivered, SwCard hoursCard) {
        IModel<List<Month>> forecastData = Model.ofList(forecastMonths());
        var liveForecast =
                forecast("liveForecast", "The handoff moves forward", forecastData, true);
        add(liveForecast);
        IModel<List<Revenue>> currentRevenue = Model.ofList(revenue());
        var liveFormatted =
                revenueChart("liveFormatted", "Fresh figures, familiar formats", currentRevenue);
        add(liveFormatted);
        var appearanceSeries =
                ChartSeries.bar("Delivered", Month::desktop)
                        .setColors("#047857", "#34d399")
                        .setBarRadius(0, 10, 10, 0);
        IModel<List<Month>> appearanceData = Model.ofList(months());
        var liveAppearance =
                new SwChart<>("liveAppearance", appearanceData, Month::name)
                        .setTitle("An adaptable report")
                        .setOrientation(SwChart.Orientation.HORIZONTAL)
                        .setTooltipIndicator(SwChart.Indicator.LINE)
                        .setLegendPosition(SwChart.LegendPosition.TOP)
                        .setLegendAlignment(SwChart.LegendAlignment.START)
                        .addSeries(appearanceSeries);
        add(liveAppearance);
        int[] appearanceUpdates = {0};
        IModel<List<Region>> liveRegions = Model.ofList(regions());
        var liveDonut =
                new SwDonutChart<>("liveDonut", liveRegions, Region::name, Region::revenue)
                        .setTitle("Live regional breakdown")
                        .setCategoryHeader("Region");
        add(liveDonut);
        var livePie = pieChart("livePie", "Live revenue shares", liveRegions);
        add(livePie);
        IModel<List<Month>> changes = Model.ofList(months());
        var livePercent = percentBars("livePercent", "Live device proportions", changes);
        var livePercentHorizontal = percentBars("livePercentHorizontal", "Live device proportions by row", changes)
                .setOrientation(SwChart.Orientation.HORIZONTAL);
        var livePercentArea = percentAreas("livePercentArea", "Live device proportions over time", changes);
        add(livePercent, livePercentHorizontal, livePercentArea);
        var live =
                new SwChart<>("live", changes, Month::name)
                        .setTitle("Live report")
                        .addSeries(
                                ChartSeries.area("Desktop", Month::desktop)
                                        .setValueLabels(ChartSeries.ValueLabels.LAST));
        add(live);
        var liveHorizontal =
                new SwChart<>("liveHorizontal", changes, Month::name)
                        .setOrientation(SwChart.Orientation.HORIZONTAL)
                        .setTitle("Live horizontal report")
                        .setCategoryHeader("Month")
                        .addSeries(ChartSeries.bar("Desktop", Month::desktop))
                        .addSeries(ChartSeries.bar("Mobile", Month::mobile));
        add(liveHorizontal);
        add(
                SwButton.onClick(
                        "refresh",
                        "Refresh report",
                        target -> {
                            changes.setObject(
                                    List.of(
                                            new Month("July", 250.0, 100.0),
                                            new Month("August", 320.0, 120.0),
                                            new Month("September", 280.0, 110.0)));
                            liveRegions.setObject(
                                    List.of(new Region("Europe", 75), new Region("Americas", 25)));
                            delivered.setObject(List.of(52, 45, 68, 72, 84, 96));
                            forecastData.setObject(
                                    List.of(
                                            new Month("January", 120.0, null),
                                            new Month("February", 260.0, null),
                                            new Month("March", 410.0, null),
                                            new Month("April", 560.0, null),
                                            new Month("May", null, 700.0),
                                            new Month("June", null, 850.0)));
                            currentRevenue.setObject(
                                    List.of(
                                            new Revenue(
                                                    LocalDate.of(2026, 9, 1),
                                                    new BigDecimal("18000.50")),
                                            new Revenue(
                                                    LocalDate.of(2026, 10, 1),
                                                    new BigDecimal("22500.75"))));
                            appearanceData.setObject(changes.getObject());
                            if (++appearanceUpdates[0] == 1)
                                appearanceSeries
                                        .setColors("#6d28d9", "#c4b5fd")
                                        .setBarRadius(10, 0, 0, 10);
                            else appearanceSeries.setColor(3).setBarRadius(0);
                            target.add(
                                    live,
                                    liveHorizontal,
                                    livePercent,
                                    livePercentHorizontal,
                                    livePercentArea,
                                    liveDonut,
                                    livePie,
                                    hoursCard,
                                    liveForecast,
                                    liveFormatted,
                                    liveAppearance);
                        }));
    }

    private void addBoundaryCharts() {
        add(
                new SwChart<Month>("empty", Model.ofList(List.of()), Month::name)
                        .setTitle("Your next report")
                        .addSeries(ChartSeries.bar("Visitors", Month::desktop)));
        add(
                new SwChart<>(
                                "gaps",
                                Model.ofList(
                                        List.of(
                                                new Month("January", 20.0, 10.0),
                                                new Month("February", null, -5.0),
                                                new Month("March", -10.0, 15.0),
                                                new Month("April", 30.0, 25.0))),
                                Month::name)
                        .setTitle("Honest gaps and negative values")
                        .addSeries(
                                ChartSeries.line("Actual", Month::desktop)
                                        .setCurve(ChartSeries.Curve.LINEAR))
                        .addSeries(
                                ChartSeries.line("Previous", Month::mobile)
                                        .setCurve(ChartSeries.Curve.STEP)));
    }

    private static SwChart<Month> chart(String id) {
        return new SwChart<>(id, Model.ofList(months()), Month::name).setCategoryHeader("Month");
    }

    private static List<Client> clients() {
        return List.of(
                new Client("Northstar Foundation accessibility improvements", 84.0, 96.0),
                new Client("Harbor & Pine retail platform", 68.0, 72.0),
                new Client("Clover Studio internal tools", 52.0, 48.0),
                new Client("Greenfield community website", 36.0, 40.0));
    }

    private static SwChart<Month> forecast(
            String id, String title, IModel<List<Month>> data, boolean area) {
        ChartSeries<Month> actual =
                area
                        ? ChartSeries.area("Actual", Month::desktop)
                        : ChartSeries.line("Actual", Month::desktop);
        ChartSeries<Month> projected =
                area
                        ? ChartSeries.area("Forecast", Month::mobile)
                        : ChartSeries.line("Forecast", Month::mobile);
        actual.setColor(area ? 2 : 1).setDots(true);
        projected
                .setColor(area ? 2 : 1)
                .setDashed(true)
                .setDots(true)
                .setFillOpacity(.15)
                .setJoin(actual)
                .setValueLabels(ChartSeries.ValueLabels.LAST);
        return new SwChart<>(id, data, Month::name)
                .setTitle(title)
                .setCategoryHeader("Month")
                .addSeries(actual)
                .addSeries(projected);
    }

    private static List<Month> forecastMonths() {
        return List.of(
                new Month("January", 120.0, null),
                new Month("February", 260.0, null),
                new Month("March", 410.0, null),
                new Month("April", null, 560.0),
                new Month("May", null, 700.0),
                new Month("June", null, 850.0));
    }

    private static SwChart<Revenue> revenueChart(
            String id, String title, IModel<List<Revenue>> model) {
        return new SwChart<>(id, model, Revenue::date)
                .setTitle(title)
                .setCategoryHeader("Report date")
                .setAxisCategoryFormat(SwChart.CategoryFormat.MONTH_SHORT)
                .setAxisValueFormat(SwChart.ValueFormat.COMPACT)
                .setTooltipLabelFormat(SwChart.CategoryFormat.DATE)
                .setTooltipCurrency(Currency.getInstance("EUR"))
                .addSeries(
                        ChartSeries.bar("Net revenue (EUR)", Revenue::amount)
                                .setValueLabels(ChartSeries.ValueLabels.LAST));
    }

    private static List<Revenue> revenue() {
        return List.of(
                new Revenue(LocalDate.of(2026, 4, 1), new BigDecimal("12345.67")),
                new Revenue(LocalDate.of(2026, 5, 1), null),
                new Revenue(LocalDate.of(2026, 6, 1), new BigDecimal("24890.50")),
                new Revenue(LocalDate.of(2026, 7, 1), BigDecimal.ZERO),
                new Revenue(LocalDate.of(2026, 8, 1), new BigDecimal("-1250.25")));
    }

    private static List<Region> regions() {
        return List.of(
                new Region("North", 40),
                new Region("South", 25),
                new Region("East", 15),
                new Region("West", 10),
                new Region("Central", 5),
                new Region("Islands", 3),
                new Region("Overseas", 2));
    }

    @Override
    protected String getTitle() {
        return "Charts";
    }
}

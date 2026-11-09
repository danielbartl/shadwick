package dev.jbaby.shadwick.components.chart;

import dev.jbaby.shadwick.components.SwButton;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.*;
import org.apache.wicket.util.resource.*;
import org.apache.wicket.util.tester.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.assertj.core.api.Assertions.*;

class SwScatterChartTest {
    record Row(String name, Number x, Number y) implements Serializable {}
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() { tester.destroy(); }
    ScatterSeries<Row> series(String label, List<Row> data) {
        return ScatterSeries.of(label, Model.ofList(data), Row::name, Row::x, Row::y);
    }
    SwScatterChart chart(List<Row> rows) { return new SwScatterChart("chart").setTitle("Measurements").addSeries(series("Team", rows)); }
    String html() { return tester.getLastResponseAsString(); }
    List<TagTester> tags(String css) {
        return TagTester.createTags(html(), tag -> Arrays.asList(Objects.toString(tag.getAttributes().get("class"), "").split(" ")).contains(css), false);
    }
    List<TagTester> points() { return tags("sw-scatter-mark"); }

    @Test void bothCoordinatesDeterminePositionsInsteadOfSpacingPointsByRowOrder() {
        tester.startComponentInPage(chart(List.of(new Row("Origin", 0, 0), new Row("Near", 1, 10), new Row("Far", 10, 20))));
        assertThat(points()).extracting(point -> point.getAttribute("x")).containsExactly("0%", "10%", "100%");
        assertThat(points()).extracting(point -> point.getAttribute("y")).containsExactly("240", "120", "0");
        assertThat(tags("sw-scatter-points").get(0).getAttribute("viewBox")).isNull();
        assertThat(points().get(0).getValue()).contains("r=\"4\"");
        assertThat(tags("sw-scatter-hit")).hasSize(3).allSatisfy(point -> assertThat(point.getAttribute("r")).isEqualTo("12"));
        assertThat(html()).contains("<td>1</td><td>10</td>", "Near; Team; X: 1; Y: 10")
                .doesNotContain("sw-chart-line", "style=", "NaN", "Infinity");
    }

    @Test void signedCoordinatesPutZeroAtTheSharedAxisIntersection() {
        tester.startComponentInPage(chart(List.of(new Row("Negative", -10, -20), new Row("Zero", 0, 0), new Row("Positive", 10, 20))));
        assertThat(points().get(1).getAttribute("x")).isEqualTo("50%");
        assertThat(points().get(1).getAttribute("y")).isEqualTo("120");
        assertThat(tags("sw-scatter-y-axis").get(0).getValue()).contains("-20", "20");
    }

    @Test void aZeroBaselineIsAnExplicitChoiceRatherThanAnAutomaticCategoryChartAssumption() {
        tester.startComponentInPage(chart(List.of(new Row("First", 2, 2), new Row("Second", 8, 8))));
        assertThat(points().get(0).getAttribute("x")).isEqualTo("0%");
        tester.startComponentInPage(chart(List.of(new Row("First", 2, 2), new Row("Second", 8, 8))).setIncludeZero(true));
        assertThat(points().get(0).getAttribute("x")).isEqualTo("25%");
    }

    @ParameterizedTest @ValueSource(doubles={0, 5, -5, Double.MIN_VALUE, 1e100, -1e100})
    void aSingleOrFlatMeasurementHasFinitePaddedAxes(double value) {
        tester.startComponentInPage(chart(List.of(new Row("Single", value, value))));
        assertThat(points()).hasSize(1);
        assertThat(html()).doesNotContain("NaN", "Infinity");
        assertThat(Double.parseDouble(points().get(0).getAttribute("x").replace("%", ""))).isBetween(0.0, 100.0);
        assertThat(Double.parseDouble(points().get(0).getAttribute("y"))).isBetween(0.0, 240.0);
    }

    @Test void missingPairsStayInTheExactTableAndDoNotStretchTheNumericDomains() {
        tester.startComponentInPage(chart(List.of(new Row("A", 0, 0), new Row("Missing Y", 1e100, null),
                new Row("B", 10, 10), new Row("Missing X", null, -1e100))));
        assertThat(points()).extracting(point -> point.getAttribute("data-index")).containsExactly("0", "2");
        assertThat(points()).extracting(point -> point.getAttribute("x")).containsExactly("0%", "100%");
        assertThat(tags("sw-scatter-plot").get(0).getAttribute("data-row-count")).isEqualTo("4");
        assertThat(tags("sw-scatter-plot").get(0).getAttribute("data-point-count")).isEqualTo("2");
        assertThat(html()).contains("Missing Y</th><td>Team</td><td>", "Missing X</th><td>Team</td><td></td>");
    }

    @Test void independentSeriesPreserveInputOrderDuplicateCoordinatesAndMissingMeasurements() {
        var first = series("Design", List.of(new Row("Same A", 1, 2), new Row("Same B", 1, 2))).setPointRadius(6);
        var second = series("Engineering", List.of(new Row("Other", 3, 4), new Row("Missing", null, 5)))
                .setColor(4).setShape(ScatterSeries.Shape.SQUARE);
        tester.startComponentInPage(new SwScatterChart("chart").addSeries(first).addSeries(second));
        assertThat(points()).hasSize(3);
        assertThat(points().get(0).getAttribute("x")).isEqualTo(points().get(1).getAttribute("x"));
        assertThat(points().get(0).getValue()).contains("r=\"6\"");
        assertThat(points().get(2).getAttribute("class")).contains("sw-chart-series-4");
        assertThat(points().get(2).getValue()).contains("<rect", "width=\"8\"");
        assertThat(html()).contains("Same A", "Same B", "Engineering", "Missing");
        assertThat(html().indexOf("Same B</th>")).isLessThan(html().indexOf("Other</th>"));
    }

    @ParameterizedTest @EnumSource(ScatterSeries.Shape.class)
    void nativePixelShapesAndPaletteOptionsRemainPerSeries(ScatterSeries.Shape shape) {
        tester.startComponentInPage(new SwScatterChart("chart").addSeries(series("A", List.of(new Row("A", 1, 2)))
                .setShape(shape).setPointRadius(12).setColor(5)));
        assertThat(points().get(0).getAttribute("class")).contains("sw-chart-series-5");
        assertThat(points().get(0).getValue()).contains(switch(shape) {
            case CIRCLE -> "r=\"12\"";
            case SQUARE -> "width=\"24\"";
            case DIAMOND -> "M0,-12 L12,0 L0,12 L-12,0 Z";
        });
        assertThat(tags("sw-scatter-hit").get(0).getAttribute("r")).isEqualTo("15");
    }

    @Test void emptyAndUnpairedDatasetsHaveDifferentStatesAndKeepKnownCells() {
        tester.startComponentInPage(chart(List.of()));
        assertThat(html()).contains("No data to display").doesNotContain("<table", "sw-scatter-plot");
        tester.startComponentInPage(chart(List.of(new Row("Hours", 10, null), new Row("Revenue", null, 20))));
        assertThat(html()).contains("No complete coordinate pairs", "Hours</th><td>Team</td><td>10</td><td></td>", "Revenue</th><td>Team</td><td></td><td>20</td>")
                .doesNotContain("sw-scatter-plot");
    }

    @Test void independentFormatsUseTheLocaleAndPassOriginalNumbersToCustomCallbacks() {
        tester.getSession().setLocale(Locale.GERMANY);
        var x = new BigDecimal("0.123456789123456789");
        var y = new BigDecimal("1234.56");
        tester.startComponentInPage(chart(List.of(new Row("A", x, y)))
                .setXValueFormat((value, locale) -> value.toString()).setYCurrency(Currency.getInstance("EUR"))
                .setXLabel("Share").setYLabel("Revenue"));
        assertThat(html()).contains("0.123456789123456789", "1.234,56", "€", "<th scope=\"col\">Share</th>", "<th scope=\"col\">Revenue</th>");
        tester.startComponentInPage(chart(List.of(new Row("A", .25, .5))).setXValueFormat(SwChart.ValueFormat.PERCENT));
        assertThat(html().replace('\u00a0', ' ')).contains("25 %", "0,5");
    }

    @Test void captionsHeadersNamesAndFormattedValuesStayEscaped() {
        tester.startComponentInPage(new SwScatterChart("chart").setTitle("<Title>").setXLabel("<X>").setYLabel("<Y>")
                .setPointHeader("<Point>").setYValueFormat((value, locale) -> "<value>")
                .addSeries(series("<Series>", List.of(new Row("<img src=x>", 1, 2)))));
        assertThat(html()).contains("&lt;Title&gt;", "&lt;Point&gt;", "&lt;Series&gt;", "&lt;img", "&lt;value&gt;")
                .doesNotContain("<img", "<value>", "style=");
    }

    @ParameterizedTest @CsvSource({"de,Punkt,Datenreihe,Daten anzeigen", "fr,Point,Série,Voir les données", "ja,点,系列,データを表示"})
    void builtInTableLabelsFollowTheLocale(String language, String point, String series, String data) {
        tester.getSession().setLocale(Locale.forLanguageTag(language));
        tester.startComponentInPage(chart(List.of(new Row("A", 1, 2))));
        assertThat(html()).contains(point, series, data);
    }

    @Test void hiddenGridAxesLegendAndExplorationDoNotRemoveTheUnderlyingPointsOrData() {
        tester.startComponentInPage(chart(List.of(new Row("A", 1, 2))).setShowGrid(false).setShowXAxis(false)
                .setShowYAxis(false).setShowLegend(false).setShowTooltip(false));
        assertThat(points()).hasSize(1);
        assertThat(html()).contains("data-tooltip=\"false\"", "<table").doesNotContain("sw-scatter-x-axis", "sw-scatter-y-axis",
                "sw-chart-grid", "sw-chart-legend-content", "Use arrow keys");
    }

    @ParameterizedTest @ValueSource(doubles={Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1e101})
    void invalidCoordinatesFailEvenWhenTheOtherCoordinateIsMissing(double value) {
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(new Row("Bad", value, null)))))
                .hasStackTraceContaining("Scatter coordinates must be finite");
    }

    @Test void requiredNamesSeriesRegistrationAndPresentationBoundsFailClearly() {
        assertThatThrownBy(() -> tester.startComponentInPage(new SwScatterChart("chart"))).hasStackTraceContaining("at least one series");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of()).setTitle(" "))).hasStackTraceContaining("needs a title");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of()).setXLabel(" "))).hasStackTraceContaining("X label");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of()).setPointHeader(" "))).hasStackTraceContaining("point header");
        assertThatThrownBy(() -> tester.startComponentInPage(new SwScatterChart("chart").addSeries(series(" ", List.of()))))
                .hasStackTraceContaining("series needs a label");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(new Row("", 1, 2))))).hasStackTraceContaining("point needs a name");
        var series = series("A", List.of());
        assertThatIllegalArgumentException().isThrownBy(() -> new SwScatterChart("chart").addSeries(series).addSeries(series));
        assertThatIllegalArgumentException().isThrownBy(() -> series.setColor(6));
        assertThatIllegalArgumentException().isThrownBy(() -> series.setPointRadius(Double.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> series.setPointRadius(13));
        assertThatIllegalArgumentException().isThrownBy(() -> new SwScatterChart("chart").setXTickCount(1));
        assertThatIllegalArgumentException().isThrownBy(() -> new SwScatterChart("chart").setYTickCount(21));
        assertThatNullPointerException().isThrownBy(() -> series.setShape(null));
    }

    @Test void modelsAndExtractorsAreReadOnceAndDetachAfterLifecycleAssembly() {
        AtomicInteger loads = new AtomicInteger(), names = new AtomicInteger(), xs = new AtomicInteger(), ys = new AtomicInteger(), detaches = new AtomicInteger();
        IModel<List<Row>> data = new IModel<>() {
            public List<Row> getObject() { loads.incrementAndGet(); return List.of(new Row("A", 1, 2), new Row("B", 3, 4)); }
            public void detach() { detaches.incrementAndGet(); }
        };
        IModel<String> label = new IModel<>() {
            public String getObject() { return "Caption"; }
            public void detach() { detaches.incrementAndGet(); }
        };
        var chart = new SwScatterChart("chart").setTitle(label).setXLabel(label).setYLabel(label).setPointHeader(label)
                .addSeries(ScatterSeries.of(label, data, row -> { names.incrementAndGet(); return row.name; },
                        row -> { xs.incrementAndGet(); return row.x; }, row -> { ys.incrementAndGet(); return row.y; }));
        assertThat(chart.get("figure")).isNull();
        tester.startComponentInPage(chart);
        assertThat(chart.get("figure")).isNotNull();
        assertThat(loads.get()).isEqualTo(1);
        assertThat(names.get()).isEqualTo(2);
        assertThat(xs.get()).isEqualTo(2);
        assertThat(ys.get()).isEqualTo(2);
        assertThat(detaches.get()).isEqualTo(6);
    }

    @Test void serializedDatasetsCallbacksAndMarkerConfigurationSurviveAnAjaxUpdate() {
        tester.startPage(ScatterPage.class);
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        var restored = (WebPage) serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));
        tester.startPage(restored);
        tester.clickLink("refresh");
        assertThat(html()).contains("<component", "Updated team", "New point", "sw-chart-series-3", "M0,-6")
                .doesNotContain("<redirect");
        assertThat(points()).hasSize(1);
    }

    public static class ScatterPage extends WebPage implements IMarkupResourceStreamProvider {
        @Override protected void onInitialize() {
            super.onInitialize();
            IModel<List<Row>> data = Model.ofList(List.of(new Row("Original", 1, 2)));
            IModel<String> label = Model.of("Team");
            var series = ScatterSeries.of(label, data, Row::name, Row::x, Row::y);
            var chart = new SwScatterChart("chart").addSeries(series);
            add(chart);
            add(SwButton.onClick("refresh", "Refresh", target -> {
                data.setObject(List.of(new Row("New point", 7, 9)));
                label.setObject("Updated team");
                series.setShape(ScatterSeries.Shape.DIAMOND).setColor(3).setPointRadius(6);
                target.add(chart);
            }));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><body><div wicket:id='chart'></div><a wicket:id='refresh'>Refresh</a></body></html>");
        }
    }
}

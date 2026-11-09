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

class SwChartPercentTest {
    record Row(String category, Number first, Number second) implements Serializable {}
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() { tester.destroy(); }

    SwChart<Row> chart(List<Row> data) {
        return new SwChart<>("chart", Model.ofList(data), Row::category).setStackMode(SwChart.StackMode.PERCENT);
    }
    SwChart<Row> bars(List<Row> data) {
        return chart(data).addSeries(ChartSeries.bar("First", Row::first).setStack("total"))
                .addSeries(ChartSeries.bar("Second", Row::second).setStack("total"));
    }
    SwChart<Row> areas(List<Row> data) {
        return chart(data).addSeries(ChartSeries.area("First", Row::first).setStack("total").setCurve(ChartSeries.Curve.LINEAR))
                .addSeries(ChartSeries.area("Second", Row::second).setStack("total").setCurve(ChartSeries.Curve.LINEAR));
    }
    String html() { return tester.getLastResponseAsString(); }
    List<TagTester> tags(String css) { return TagTester.createTagsByAttribute(html(), "class", css, false); }
    List<TagTester> bars() { return tags("sw-chart-bar"); }
    double number(TagTester tag, String attribute) { return Double.parseDouble(tag.getAttribute(attribute)); }

    @ParameterizedTest @EnumSource(SwChart.Orientation.class)
    void categoriesNormalizeIndependentlyWhileTablesTooltipsAndLabelsKeepOriginalValues(SwChart.Orientation orientation) {
        var first = ChartSeries.bar("First", Row::first).setStack("total").setValueLabels(ChartSeries.ValueLabels.ALL);
        var second = ChartSeries.bar("Second", Row::second).setStack("total");
        tester.startComponentInPage(chart(List.of(new Row("Small", 1, 3), new Row("Large", 100, 100)))
                .setOrientation(orientation).setCurrency(Currency.getInstance("EUR"))
                .addSeries(first).addSeries(second));
        double firstSize = number(bars().get(0), orientation == SwChart.Orientation.VERTICAL ? "height" : "width");
        assertThat(firstSize).isEqualTo(orientation == SwChart.Orientation.VERTICAL ? 60 : 250);
        var axis = tags(orientation == SwChart.Orientation.VERTICAL ? "sw-chart-y-axis" : "sw-chart-value-axis");
        assertThat(axis.get(0).getValue()).contains("0%", "100%").doesNotContain("€");
        assertThat(html()).contains("data-stack-mode=\"percent\"", "<td>€1.00</td>", "<td>€3.00</td>",
                "First: €1.00", ">€100.00</text>").doesNotContain("NaN", "Infinity", "style=");
    }

    @Test void differentNamedStacksAndDifferentSeriesTypesHaveIndependentTotals() {
        tester.startComponentInPage(chart(List.of(new Row("Period", 1, 3)))
                .setOrientation(SwChart.Orientation.HORIZONTAL)
                .addSeries(ChartSeries.bar("A", Row::first).setStack("one"))
                .addSeries(ChartSeries.bar("B", Row::second).setStack("one"))
                .addSeries(ChartSeries.<Row>bar("C", row -> 100).setStack("two")));
        assertThat(bars()).hasSize(3);
        assertThat(number(bars().get(0), "width")).isEqualTo(250);
        assertThat(number(bars().get(1), "width")).isEqualTo(750);
        assertThat(number(bars().get(2), "width")).isEqualTo(1000);
        assertThat(number(bars().get(2), "y")).isGreaterThan(number(bars().get(0), "y"));

        tester.startComponentInPage(bars(List.of(new Row("Period", 1, 3)))
                .addSeries(ChartSeries.<Row>area("Area", row -> 100).setStack("total").setCurve(ChartSeries.Curve.LINEAR)));
        assertThat(number(bars().get(0), "height")).isEqualTo(60);
        assertThat(tags("sw-chart-line").get(0).getAttribute("d")).endsWith(",0");
    }

    @ParameterizedTest @EnumSource(SwChart.Orientation.class)
    void zeroTotalsAreUnplottedWhileMissingAndZeroOriginalCellsRemainDistinct(SwChart.Orientation orientation) {
        tester.startComponentInPage(bars(List.of(new Row("Zero", 0, 0), new Row("Missing", null, null),
                new Row("Partial", null, 7), new Row("Real zero", 0, 5))).setOrientation(orientation));
        assertThat(bars()).hasSize(3);
        assertThat(html()).contains("<th scope=\"row\">Zero</th><td>0</td><td>0</td>",
                "<th scope=\"row\">Missing</th><td></td><td></td>",
                "<th scope=\"row\">Partial</th><td></td><td>7</td>").doesNotContain("NaN", "Infinity");
        assertThat(bars()).anySatisfy(bar -> assertThat(number(bar, orientation == SwChart.Orientation.VERTICAL ? "height" : "width")).isZero());
    }

    @Test void zeroTotalAreasCreateGapsInsteadOfInventingPercentagesOrConnectingAcrossThem() {
        tester.startComponentInPage(areas(List.of(new Row("Before", 1, 3), new Row("Zero", 0, 0),
                new Row("After", 3, 1))));
        assertThat(tags("sw-chart-area")).hasSize(4);
        assertThat(tags("sw-chart-line")).extracting(tag -> tag.getAttribute("d"))
                .containsExactly("M166.66667,180", "M833.33333,60", "M166.66667,0", "M833.33333,0");
        assertThat(html()).contains("<th scope=\"row\">Zero</th><td>0</td><td>0</td>");
    }

    @Test void decimalNormalizationHandlesUnderflowLargeValuesAndFractionalTotals() {
        tester.startComponentInPage(bars(List.of(new Row("Tiny", new BigDecimal("1e-1000"), new BigDecimal("3e-1000")),
                new Row("Large", new BigDecimal("1e100"), new BigDecimal("1e100")),
                new Row("Decimals", new BigDecimal("0.1"), new BigDecimal("0.3"))))
                .setOrientation(SwChart.Orientation.HORIZONTAL).setValueFormat((n, locale) -> n.toString()));
        assertThat(bars()).extracting(tag -> number(tag, "width")).containsExactly(250.0, 750.0, 500.0, 500.0, 250.0, 750.0);
        assertThat(html()).contains("1E-1000", "3E-1000", "1E+100").doesNotContain("NaN", "Infinity");
    }

    @ParameterizedTest @ValueSource(ints={2, 3, 4, 5, 7, 12, 20})
    void percentageAxisAlwaysKeepsZeroAndOneAsItsDomain(int count) {
        tester.startComponentInPage(bars(List.of(new Row("One", 1, 2))).setValueTickCount(count));
        assertThat(tags("sw-chart-y-axis").get(0).getValue()).startsWith("<span>100%</span>").endsWith("<span>0%</span>");
        double totalHeight = bars().stream().mapToDouble(tag -> number(tag, "height")).sum();
        assertThat(totalHeight).isCloseTo(240, within(.00002));
    }

    @Test void customAxisFormattingOverridesThePercentDefaultWithoutChangingOtherParts() {
        tester.getSession().setLocale(Locale.GERMANY);
        tester.startComponentInPage(bars(List.of(new Row("A", 1, 3)))
                .setAxisValueFormat((n, locale) -> "<" + n.doubleValue() * 100 + ">")
                .setTooltipValueFormat((n, locale) -> "<original " + n + ">"));
        assertThat(tags("sw-chart-y-axis").get(0).getValue()).contains("&lt;100.0&gt;");
        assertThat(html()).contains("data-tooltip-value=\"&lt;original 1&gt;\"", "<td", ">1</td>");
        assertThat(html()).doesNotContain("<original");
        tester.startComponentInPage(bars(List.of(new Row("A", 1, 3))));
        assertThat(tags("sw-chart-y-axis").get(0).getValue().replace('\u00a0', ' ')).contains("100 %");
    }

    @ParameterizedTest @ValueSource(strings={"-1", "-1e-1000"})
    void negativeValuesCannotEnterANonnegativePercentageStack(String negative) {
        assertThatThrownBy(() -> tester.startComponentInPage(bars(List.of(new Row("Bad", new BigDecimal(negative), 5)))))
                .hasStackTraceContaining("100% stacks require nonnegative values");
    }

    @Test void unstackedBlankAndLineSeriesCannotMixInRawUnitsOnAPercentageAxis() {
        for (ChartSeries<Row> part : List.of(ChartSeries.bar("Unstacked", Row::first),
                ChartSeries.bar("Blank", Row::first).setStack(" "), ChartSeries.line("Line", Row::first).setStack("total"))) {
            assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(new Row("A", 1, 2))).addSeries(part)))
                    .hasStackTraceContaining("100% stacks require bar/area series with a nonblank stack name");
        }
        assertThatNullPointerException().isThrownBy(() -> bars(List.of()).setStackMode(null));
    }

    @Test void normalizationDoesNotRereadModelsAndTheFigureWaitsForInitialization() {
        AtomicInteger loads = new AtomicInteger(), reads = new AtomicInteger(), detaches = new AtomicInteger();
        IModel<List<Row>> data = new IModel<>() {
            public List<Row> getObject() { loads.incrementAndGet(); return List.of(new Row("A", 1, 3)); }
            public void detach() { detaches.incrementAndGet(); }
        };
        var chart = new SwChart<>("chart", data, Row::category).setStackMode(SwChart.StackMode.PERCENT)
                .addSeries(ChartSeries.<Row>bar("A", row -> { reads.incrementAndGet(); return row.first; }).setStack("total"))
                .addSeries(ChartSeries.<Row>bar("B", row -> { reads.incrementAndGet(); return row.second; }).setStack("total"));
        assertThat(chart.get("figure")).isNull();
        tester.startComponentInPage(chart);
        assertThat(chart.get("figure")).isNotNull();
        assertThat(loads.get()).isEqualTo(1);
        assertThat(reads.get()).isEqualTo(2);
        assertThat(detaches.get()).isPositive();
    }

    @Test void emptyAndAllZeroChartsKeepNativeStatesAndAValidPercentageScale() {
        tester.startComponentInPage(bars(List.of()));
        assertThat(html()).contains("No data to display").doesNotContain("<svg");
        tester.startComponentInPage(bars(List.of(new Row("Zero", 0, 0))).setShowTooltip(false));
        assertThat(bars()).isEmpty();
        assertThat(html()).contains("100%", "<td>0</td>", "data-tooltip=\"false\"").doesNotContain("NaN", "Infinity");
    }

    @Test void forecastJoinsBorrowDefinedPercentCoordinatesAndDoNotBridgeZeroTotals() {
        var actual = ChartSeries.area("Actual", Row::first).setStack("total").setCurve(ChartSeries.Curve.LINEAR);
        var forecast = ChartSeries.area("Forecast", Row::second).setStack("total")
                .setDashed(true).setJoin(actual).setCurve(ChartSeries.Curve.LINEAR);
        tester.startComponentInPage(chart(List.of(new Row("Actual", 1, null), new Row("Zero", 0, 0),
                new Row("Forecast", null, 2), new Row("Next", null, 3))).addSeries(actual).addSeries(forecast));
        var group = TagTester.createTagsByAttribute(html(), "data-series", "1", false).get(0);
        assertThat(group.getAttribute("data-join-index")).isEqualTo("0");
        assertThat(tags("sw-chart-line")).extracting(tag -> tag.getAttribute("d"))
                .containsExactly("M125,0", "M625,0 L875,0");
        assertThat(html()).contains("<th scope=\"row\">Actual</th><td>1</td><td></td>");
    }

    @Test void highestLabelsStillRankOriginalAmountsRatherThanNormalizedShares() {
        tester.startComponentInPage(chart(List.of(new Row("High share", 100, 1), new Row("Highest amount", 150, 1000),
                new Row("Latest", 20, 80)))
                .addSeries(ChartSeries.bar("First", Row::first).setStack("total")
                        .setValueLabels(ChartSeries.ValueLabels.HIGHEST_AND_LAST))
                .addSeries(ChartSeries.bar("Second", Row::second).setStack("total")));
        var group = tags("sw-chart-value-labels").get(0);
        var left = TagTester.createTagsByAttribute(group.getValue(), "data-label-dir", "ltr", false).get(0);
        assertThat(TagTester.createTagsByAttribute(left.getValue(), "class", "sw-chart-value-label", false))
                .extracting(TagTester::getValue).containsExactlyInAnyOrder("150", "20");
    }

    @Test void serializedPagesRetainTheModeAndCanRestoreAbsoluteValuesThroughAjax() {
        tester.startPage(PercentPage.class);
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        var page = (WebPage) serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));
        tester.startPage(page);
        assertThat(html()).contains("data-stack-mode=\"percent\"");
        tester.clickLink("refresh");
        assertThat(html()).contains("<component", "data-stack-mode=\"absolute\"", "Updated", "€40.00").doesNotContain("<redirect");
        assertThat(tags("sw-chart-y-axis").get(0).getValue()).contains("€").doesNotContain("%");
    }

    public static class PercentPage extends WebPage implements IMarkupResourceStreamProvider {
        @Override protected void onInitialize() {
            super.onInitialize();
            IModel<List<Row>> data = Model.ofList(List.of(new Row("Original", 1, 3)));
            var chart = new SwChart<>("chart", data, Row::category).setStackMode(SwChart.StackMode.PERCENT)
                    .setCurrency(Currency.getInstance("EUR"))
                    .addSeries(ChartSeries.bar("First", Row::first).setStack("total"))
                    .addSeries(ChartSeries.bar("Second", Row::second).setStack("total"));
            add(chart);
            add(SwButton.onClick("refresh", "Refresh", target -> {
                data.setObject(List.of(new Row("Updated", 40, 60)));
                chart.setStackMode(SwChart.StackMode.ABSOLUTE);
                target.add(chart);
            }));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><body><div wicket:id='chart'></div><a wicket:id='refresh'>Refresh</a></body></html>");
        }
    }
}

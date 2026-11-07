package dev.jbaby.shadwick.components.chart;

import static org.assertj.core.api.Assertions.*;

import java.io.Serializable;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.wicket.model.*;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.tester.TagTester;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SwChartTest {
    record Row(String name, Double actual, Double previous) implements Serializable {}
    final List<Row> rows = List.of(new Row("Jan", 12.0, 10.0), new Row("Feb", null, 20.0), new Row("Mar", -8.0, 15.0));
    WicketTester tester;
    @BeforeEach
    void setup() {
        tester = new WicketTester();
        tester.getSession().setLocale(Locale.US);
    }
    @AfterEach
    void stop() {
        tester.destroy();
    }
    SwChart<Row> chart() {
        return new SwChart<>("chart", Model.ofList(rows), Row::name);
    }
    String html() {
        return tester.getLastResponseAsString();
    }
    @ParameterizedTest
    @EnumSource(ChartSeries.Type.class)
    void nativeMarksAndExactDataExistWithoutScript(ChartSeries.Type type) {
        var series = switch (type) {
            case BAR -> ChartSeries.bar("Actual", Row::actual);
            case LINE -> ChartSeries.line("Actual", Row::actual);
            case AREA -> ChartSeries.area("Actual", Row::actual);
        };
        tester.startComponentInPage(chart().addSeries(series).setTitle("Revenue"));
        assertThat(html())
                .contains("<svg", "<details", "<caption", "Revenue", "Jan", "-8", "View data")
                .doesNotContain("style=", "NaN", "Infinity");
        assertThat(html()).contains(switch (type) {
            case BAR -> "sw-chart-bar";
            case LINE -> "sw-chart-line";
            case AREA -> "sw-chart-area";
        });
    }
    @Test
    void nullValuesLeaveSeparateLineRunsAndDashedSeriesKeepTheirLegend() {
        tester.startComponentInPage(
                chart().addSeries(ChartSeries.line("Actual", Row::actual))
                        .addSeries(ChartSeries.line("Forecast", Row::previous).setDashed(true)));
        assertThat(html().split("class=\"sw-chart-line\"", -1)).hasSize(4);
        assertThat(html()).contains("stroke-dasharray=\"5 5\"", "data-dashed=\"true\"", "Forecast");
    }
    @Test
    void stackDomainAccountsForSumsAndNegativeValues() {
        tester.startComponentInPage(
                chart().addSeries(ChartSeries.bar("A", Row::actual).setStack("total"))
                        .addSeries(ChartSeries.bar("B", Row::previous).setStack("total")));
        assertThat(html()).contains("sw-chart-series-1", "sw-chart-series-2").doesNotContain("NaN");
        assertThat(SwChart.ticks(-40, 90)).containsExactly(-50, 0, 50, 100);
    }
    @Test
    void eachValueAndDataModelAreReadOncePerRenderAndModelsDetach() {
        AtomicInteger loads = new AtomicInteger(), reads = new AtomicInteger(), detaches = new AtomicInteger();
        IModel<List<Row>> model = new IModel<>() {
            public List<Row> getObject() {
                loads.incrementAndGet();
                return rows;
            }
            public void detach() {
                detaches.incrementAndGet();
            }
        };
        tester.startComponentInPage(new SwChart<>("chart", model, Row::name).addSeries(ChartSeries.line("A", r -> {
            reads.incrementAndGet();
            return r.actual;
        })));
        assertThat(loads.get()).isEqualTo(1);
        assertThat(reads.get()).isEqualTo(3);
        assertThat(detaches.get()).isPositive();
    }
    @ParameterizedTest
    @EnumSource(SwChart.Orientation.class)
    void allLabelsAndNumbersAreEscapedRatherThanInsertedAsMarkup(SwChart.Orientation orientation) {
        tester.startComponentInPage(new SwChart<>(
                "chart", Model.ofList(List.of(new Row("</script><img src=x onerror=alert(1)>", 2.0, null))), Row::name)
                        .addSeries(ChartSeries.bar("<Series>", Row::actual))
                        .setOrientation(orientation)
                        .setTitle("<Title>")
                        .setValueFormat((v, l) -> "<Value>"));
        assertThat(html())
                .contains("&lt;Title&gt;", "&lt;Series&gt;", "&lt;Value&gt;")
                .doesNotContain("<img", "</script><img");
    }
    @Test
    void currencyFormattingFollowsTheSessionLocale() {
        tester.getSession().setLocale(Locale.GERMANY);
        tester.startComponentInPage(
                chart().addSeries(ChartSeries.bar("Actual", Row::actual)).setCurrency(Currency.getInstance("EUR")));
        assertThat(html()).contains("12,00", "€");
    }
    @Test
    void dateOnlyCategoriesAndInstantsUseExplicitTimeSemantics() {
        tester.startComponentInPage(new SwChart<>("chart", Model.ofList(List.of(LocalDate.of(4, 2, 29))), d -> d)
                        .addSeries(ChartSeries.bar("A", d -> 1)));
        assertThat(html()).contains("Feb 29, 4");
        tester.startComponentInPage(
                new SwChart<>("chart", Model.ofList(List.of(Instant.parse("2026-11-05T23:00:00Z"))), d -> d)
                        .setTimeZone(ZoneId.of("Asia/Tokyo"))
                        .addSeries(ChartSeries.bar("A", d -> 1)));
        assertThat(html()).contains("11/6/26");
    }
    @ParameterizedTest
    @EnumSource(SwChart.Orientation.class)
    void emptyDataShowsARealEmptyState(SwChart.Orientation orientation) {
        tester.startComponentInPage(new SwChart<Row>("chart", Model.ofList(List.of()), Row::name)
                        .setOrientation(orientation)
                        .addSeries(ChartSeries.bar("A", Row::actual)));
        assertThat(html()).contains("No data to display.").doesNotContain("<svg", "NaN");
    }
    @Test
    void invalidConfigurationAndNonFiniteValuesFailClearly() {
        assertThatThrownBy(() -> tester.startComponentInPage(chart())).hasStackTraceContaining("at least one series");
        assertThatThrownBy(
                () -> tester.startComponentInPage(chart().setTitle("").addSeries(ChartSeries.bar("A", Row::actual))))
                .hasStackTraceContaining("accessible name");
        assertThatThrownBy(() -> tester.startComponentInPage(chart().addSeries(ChartSeries.bar("A", r -> Double.NaN))))
                .hasStackTraceContaining("finite");
    }
    @Test
    void smallestFiniteValuesAndHiddenPartsRenderWithoutInvalidGeometry() {
        assertThat(SwChart.ticks(0, Double.MIN_VALUE)).containsExactly(0, Double.MIN_VALUE);
        tester.startComponentInPage(
                chart().addSeries(ChartSeries.area("A", r -> Double.MIN_VALUE))
                        .setShowGrid(false)
                        .setShowXAxis(false)
                        .setShowYAxis(false)
                        .setShowLegend(false)
                        .setShowTooltip(false));
        assertThat(html())
                .contains("data-tooltip=\"false\"")
                .doesNotContain("NaN", "sw-chart-grid", "sw-chart-x-axis", "sw-chart-y-axis", "sw-chart-legend-content",
                        "Use Left and Right");
    }

    List<TagTester> bars() {
        return TagTester.createTagsByAttribute(html(), "class", "sw-chart-bar", false);
    }
    double coordinate(TagTester tag, String attribute) {
        return Double.parseDouble(tag.getAttribute(attribute));
    }
    @Test
    void horizontalBarsUseACommonScaleAndSeparateGroupSlotsAroundZero() {
        tester.startComponentInPage(new SwChart<>("chart",
                Model.ofList(List.of(new Row("Positive", 20.0, 10.0), new Row("Negative", -20.0, null))), Row::name)
                        .setOrientation(SwChart.Orientation.HORIZONTAL)
                        .addSeries(ChartSeries.bar("A", Row::actual))
                        .addSeries(ChartSeries.bar("B", Row::previous)));
        var bars = bars();
        assertThat(bars).hasSize(3);
        // Equal magnitudes occupy equal widths on either side of the same zero line.
        assertThat(coordinate(bars.get(0), "width")).isEqualTo(coordinate(bars.get(2), "width"));
        assertThat(coordinate(bars.get(0), "width")).isEqualTo(500);
        assertThat(coordinate(bars.get(1), "width")).isEqualTo(250);
        assertThat(coordinate(bars.get(2), "x") + coordinate(bars.get(2), "width"))
                .isEqualTo(coordinate(bars.get(0), "x"));
        assertThat(coordinate(bars.get(1), "y")).isGreaterThan(coordinate(bars.get(0), "y"));
        assertThat(html()).contains("Use Up and Down", "Positive", "Negative", "<td></td>")
                .doesNotContain("style=", "NaN", "Infinity");
    }
    @Test
    void horizontalStacksAccumulateEachSignIndependentlyAndKeepOtherStacksSeparate() {
        tester.startComponentInPage(new SwChart<>("chart", Model.ofList(List.of(new Row("Team", 10.0, -5.0))), Row::name)
                        .setOrientation(SwChart.Orientation.HORIZONTAL)
                        .addSeries(ChartSeries.bar("A", Row::actual).setStack("total"))
                        .addSeries(ChartSeries.<Row>bar("B", r -> 20).setStack("total"))
                        .addSeries(ChartSeries.bar("C", Row::previous).setStack("total"))
                        .addSeries(ChartSeries.<Row>bar("D", r -> -10).setStack("total"))
                        .addSeries(ChartSeries.<Row>bar("Other", r -> 4).setStack("other")));
        var bars = bars();
        assertThat(bars).hasSize(5);
        assertThat(coordinate(bars.get(0), "x") + coordinate(bars.get(0), "width"))
                .isCloseTo(coordinate(bars.get(1), "x"), within(.00002));
        assertThat(coordinate(bars.get(3), "x") + coordinate(bars.get(3), "width"))
                .isCloseTo(coordinate(bars.get(2), "x"), within(.00002));
        assertThat(coordinate(bars.get(2), "x") + coordinate(bars.get(2), "width"))
                .isCloseTo(coordinate(bars.get(0), "x"), within(.00002));
        assertThat(bars.subList(0, 4)).extracting(t -> t.getAttribute("y")).containsOnly("8");
        assertThat(coordinate(bars.get(4), "y")).isGreaterThan(8);
    }
    @Test
    void horizontalAxesCanBeHiddenAndAllNullZeroAndTinyValuesHaveFiniteGeometry() {
        tester.startComponentInPage(new SwChart<>("chart",
                Model.ofList(List.of(new Row("Missing", null, null), new Row("Zero", 0.0, null),
                        new Row("Tiny", Double.MIN_VALUE, null))), Row::name)
                        .setOrientation(SwChart.Orientation.HORIZONTAL)
                        .setShowGrid(false).setShowXAxis(false).setShowYAxis(false)
                        .setShowLegend(false).setShowTooltip(false)
                        .addSeries(ChartSeries.bar("Actual", Row::actual)));
        assertThat(bars()).hasSize(2);
        assertThat(html()).contains("Missing", "Zero", "Tiny", "data-tooltip=\"false\"")
                .doesNotContain("sw-chart-category", "sw-chart-value-axis", "sw-chart-grid", "sw-chart-legend-content",
                        "Use Up and Down", "NaN", "Infinity");
        assertThat(coordinate(bars().get(0), "width")).isZero();
        assertThat(coordinate(bars().get(1), "width")).isEqualTo(1000);
    }
    @ParameterizedTest
    @EnumSource(value = ChartSeries.Type.class, names = {"LINE", "AREA"})
    void horizontalOrientationRejectsNonBarSeriesRatherThanSilentlyChangingTheirMeaning(ChartSeries.Type type) {
        var part = type == ChartSeries.Type.LINE ? ChartSeries.line("A", Row::actual) : ChartSeries.area("A", Row::actual);
        assertThatThrownBy(() -> tester.startComponentInPage(chart().addSeries(part).setOrientation(SwChart.Orientation.HORIZONTAL)))
                .hasStackTraceContaining("Horizontal charts support bar series only");
        assertThatNullPointerException().isThrownBy(() -> chart().setOrientation(null));
    }

    List<TagTester> labels() {
        return TagTester.createTagsByAttribute(html(), "data-label-dir", "ltr", false).stream()
                .flatMap(group -> TagTester.createTagsByAttribute(group.getValue(), "class", "sw-chart-value-label", false).stream()).toList();
    }
    @ParameterizedTest @EnumSource(ChartSeries.Type.class)
    void highestAndLatestLabelsUseCurrentValuesSkipNullsAndDoNotRelabelCumulativeStacks(ChartSeries.Type type) {
        var part = switch (type) {
            case BAR -> ChartSeries.bar("A", Row::actual);
            case LINE -> ChartSeries.line("A", Row::actual);
            case AREA -> ChartSeries.area("A", Row::actual);
        };
        tester.startComponentInPage(chart().addSeries(part.setValueLabels(ChartSeries.ValueLabels.HIGHEST_AND_LAST)));
        assertThat(labels()).extracting(t -> t.getAttribute("data-index")).containsExactlyInAnyOrder("0", "2");
        assertThat(labels()).extracting(TagTester::getValue).containsExactlyInAnyOrder("12", "-8");
        tester.startComponentInPage(new SwChart<>("chart", Model.ofList(List.of(new Row("High", 20.0, 10.0), new Row("Missing", null, null))), Row::name)
                .addSeries(ChartSeries.bar("A", Row::actual).setStack("total").setValueLabels(ChartSeries.ValueLabels.HIGHEST_AND_LAST))
                .addSeries(ChartSeries.bar("B", Row::previous).setStack("total").setValueLabels(ChartSeries.ValueLabels.LAST)));
        assertThat(labels()).extracting(TagTester::getValue).containsExactlyInAnyOrder("20", "10");
    }
    @ParameterizedTest @EnumSource(SwChart.Orientation.class)
    void allLabelsKeepZerosUseTheSameFormatterAndStayOutOfScaledGeometry(SwChart.Orientation orientation) {
        tester.startComponentInPage(new SwChart<>("chart", Model.ofList(List.of(new Row("Zero", 0.0, null), new Row("Actual", 10.0, null))), Row::name)
                .setOrientation(orientation).setValueFormat((n,l) -> "<" + n.intValue() + ">")
                .addSeries(ChartSeries.bar("A", Row::actual).setValueLabels(ChartSeries.ValueLabels.ALL)));
        assertThat(labels()).extracting(TagTester::getValue).containsExactly("&lt;0&gt;", "&lt;10&gt;");
        var overlays = TagTester.createTagsByAttribute(html(), "class", "sw-chart-value-labels", false);
        assertThat(overlays).allSatisfy(tag -> assertThat(tag.getAttribute("viewBox")).isNull());
        assertThat(html()).contains("data-label-dir=\"rtl\"").doesNotContain("<0>", "style=", "NaN");
    }
    @Test void labelsAreOptInAndLatestMeansTheLastNonMissingValue() {
        tester.startComponentInPage(chart().addSeries(ChartSeries.bar("A", Row::actual)));
        assertThat(labels()).isEmpty();
        tester.startComponentInPage(new SwChart<>("chart", Model.ofList(Arrays.asList(4, 8, null, null)), n -> "Month")
                .addSeries(ChartSeries.<Integer>line("A", n -> n).setValueLabels(ChartSeries.ValueLabels.LAST)));
        assertThat(labels()).singleElement().satisfies(tag -> {
            assertThat(tag.getValue()).isEqualTo("8"); assertThat(tag.getAttribute("data-index")).isEqualTo("1");
        });
        assertThatNullPointerException().isThrownBy(() -> ChartSeries.bar("A", Row::actual).setValueLabels(null));
    }
}

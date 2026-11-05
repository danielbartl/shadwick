package dev.jbaby.shadwick.components.chart;

import static org.assertj.core.api.Assertions.*;

import java.io.Serializable;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.wicket.model.*;
import org.apache.wicket.util.tester.WicketTester;
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
    @Test
    void allLabelsAndNumbersAreEscapedRatherThanInsertedAsMarkup() {
        tester.startComponentInPage(new SwChart<>(
                "chart", Model.ofList(List.of(new Row("</script><img src=x onerror=alert(1)>", 2.0, null))), Row::name)
                        .addSeries(ChartSeries.bar("<Series>", Row::actual))
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
    @Test
    void emptyDataShowsARealEmptyState() {
        tester.startComponentInPage(new SwChart<Row>("chart", Model.ofList(List.of()), Row::name)
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
}

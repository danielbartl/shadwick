package dev.jbaby.shadwick.components.chart;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
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

class SwForecastTest {
    record Row(String name, Double actual, Double forecast) implements Serializable {}
    static List<Row> data() { return List.of(new Row("Jan", 10.0, null), new Row("Feb", 20.0, null), new Row("Mar", null, 30.0), new Row("Apr", null, 40.0)); }
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() { tester.destroy(); }
    String html() { return tester.getLastResponseAsString(); }
    TagTester group(int index) {
        return TagTester.createTagsByAttribute(html(), "data-series", String.valueOf(index), false).stream()
                .filter(tag -> tag.getAttribute("class").startsWith("sw-chart-series-")).findFirst().orElseThrow();
    }
    List<TagTester> marks(int index, String css) { return TagTester.createTagsByAttribute(group(index).getValue(), "class", css, false); }
    void render(List<Row> rows, ChartSeries<Row> actual, ChartSeries<Row> projected) {
        tester.startComponentInPage(new SwChart<>("chart", Model.ofList(rows), Row::name).addSeries(actual).addSeries(projected));
    }
    static Stream<Arguments> curves() {
        return Stream.of(ChartSeries.Type.LINE, ChartSeries.Type.AREA).flatMap(type -> Arrays.stream(ChartSeries.Curve.values()).map(curve -> Arguments.of(type, curve)));
    }
    @ParameterizedTest @MethodSource("curves")
    void geometryContinuesTheSourceWhileDataDotsAndLabelsStayReal(ChartSeries.Type type, ChartSeries.Curve curve) {
        var actual = type == ChartSeries.Type.LINE ? ChartSeries.line("Actual", Row::actual) : ChartSeries.area("Actual", Row::actual);
        var forecast = type == ChartSeries.Type.LINE ? ChartSeries.line("Forecast", Row::forecast) : ChartSeries.area("Forecast", Row::forecast);
        render(data(), actual.setDots(true), forecast.setDashed(true).setDots(true).setJoin(actual).setCurve(curve).setValueLabels(ChartSeries.ValueLabels.ALL));
        var point = marks(0, "sw-chart-dot").get(1);
        assertThat(group(1).getAttribute("data-join-index")).isEqualTo("1");
        assertThat(marks(1, "sw-chart-line")).singleElement().satisfies(path -> assertThat(path.getAttribute("d"))
                .startsWith("M" + point.getAttribute("cx") + "," + point.getAttribute("cy")));
        assertThat(marks(1, "sw-chart-dot")).hasSize(2);
        assertThat(html()).contains("<th scope=\"row\">Feb</th><td>20</td><td></td>")
                .doesNotContain("Feb; Actual: 20; Forecast", "NaN", "Infinity", "style=");
        var labels = TagTester.createTagsByAttribute(html(), "class", "sw-chart-value-label", false).stream()
                .filter(tag -> tag.getAttribute("data-series").equals("1")).toList();
        assertThat(labels).extracting(tag -> tag.getAttribute("data-index")).containsOnly("2", "3");
    }
    @Test void gapsAreNotFilledBeforeOrAfterTheForecastStarts() {
        var actual = ChartSeries.line("Actual", Row::actual);
        var forecast = ChartSeries.line("Forecast", Row::forecast).setDashed(true).setJoin(actual).setDots(true);
        render(List.of(new Row("Jan", 10.0, null), new Row("Feb", null, null), new Row("Mar", null, 30.0),
                new Row("Apr", null, null), new Row("May", null, 50.0)), actual, forecast);
        assertThat(marks(1, "sw-chart-line")).hasSize(2);
        assertThat(marks(1, "sw-chart-dot")).hasSize(2);
        assertThat(marks(1, "sw-chart-line").get(0).getAttribute("d")).startsWith("M500,");
    }
    @Test void zeroNegativeAndSingleFutureValuesCanJoinWithoutFabricatingMeasurements() {
        for (double value : new double[] {0, -10}) {
            var actual = ChartSeries.line("Actual", Row::actual).setDots(true);
            render(List.of(new Row("Jan", value, null), new Row("Feb", null, 5.0)), actual,
                    ChartSeries.line("Forecast", Row::forecast).setDashed(true).setJoin(actual).setDots(true));
            assertThat(group(1).getAttribute("data-join-index")).isEqualTo("0");
            assertThat(marks(1, "sw-chart-dot")).hasSize(1);
            assertThat(marks(1, "sw-chart-line").get(0).getAttribute("d")).startsWith("M250,");
        }
    }
    @Test void emptySidesAndAnAlreadyStartedForecastDoNotAcquireAnAnchor() {
        for (var rows : List.of(List.of(new Row("Jan", null, 10.0)), List.of(new Row("Jan", 10.0, null)),
                List.of(new Row("Jan", 10.0, 20.0), new Row("Feb", 30.0, 40.0)))) {
            var actual = ChartSeries.line("Actual", Row::actual);
            render(rows, actual, ChartSeries.line("Forecast", Row::forecast).setDashed(true).setJoin(actual));
            assertThat(group(1).getAttribute("data-join-index")).isNull();
        }
    }
    @Test void sharedAreaStacksBorrowTheSourceGeometryWithoutCountingTheAnchorTwice() {
        var actual = ChartSeries.area("Actual", Row::actual).setStack("total").setDots(true);
        var forecast = ChartSeries.area("Forecast", Row::forecast).setStack("total").setDashed(true).setJoin(actual);
        tester.startComponentInPage(new SwChart<>("chart", Model.ofList(data()), Row::name)
                .addSeries(ChartSeries.<Row>area("Base", row -> 5).setStack("total"))
                .addSeries(actual).addSeries(forecast));
        var point = marks(1, "sw-chart-dot").get(1);
        assertThat(marks(2, "sw-chart-line").get(0).getAttribute("d")).startsWith("M" + point.getAttribute("cx") + "," + point.getAttribute("cy"));
        assertThat(html()).contains("<th scope=\"row\">Feb</th><td>5</td><td>20</td><td></td>");
    }
    @Test void sourcesAreReferencedByIdentityAndSurviveLabelChangesClearingAndSerialization() {
        tester.startPage(ForecastPage.class);
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        var restored = (ForecastPage) serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));
        assertThat(restored.forecast.join).isSameAs(restored.actual);
        assertThat(restored.actual.label).isSameAs(restored.actualLabel);
        tester.startPage(restored);
        assertThat(group(1).getAttribute("data-join-index")).isEqualTo("1");
        tester.clickLink("rename");
        assertThat(html()).contains("Ist-Werte", "data-join-index=\"1\"");
        tester.clickLink("clear");
        assertThat(html()).doesNotContain("data-join-index").contains("d=\"M625,");
    }
    public static class ForecastPage extends WebPage implements IMarkupResourceStreamProvider {
        final IModel<String> actualLabel = Model.of("Actual");
        final ChartSeries<Row> actual = ChartSeries.line(actualLabel, Row::actual);
        final ChartSeries<Row> forecast = ChartSeries.line("Forecast", Row::forecast).setDashed(true).setJoin(actual);
        public ForecastPage() {
            var chart = new SwChart<>("chart", Model.ofList(data()), Row::name).addSeries(actual).addSeries(forecast);
            add(chart);
            add(new org.apache.wicket.ajax.markup.html.AjaxLink<Void>("rename") {
                @Override public void onClick(org.apache.wicket.ajax.AjaxRequestTarget target) {
                    actualLabel.setObject("Ist-Werte"); target.add(chart);
                }
            });
            add(new org.apache.wicket.ajax.markup.html.AjaxLink<Void>("clear") {
                @Override public void onClick(org.apache.wicket.ajax.AjaxRequestTarget target) {
                    forecast.setJoin(null); target.add(chart);
                }
            });
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><body><div wicket:id='chart'></div><a wicket:id='rename'>Rename</a><a wicket:id='clear'>Clear</a></body></html>");
        }
    }
    @Test void joinsReuseTheRequestSnapshotWithoutExtraExtractorReadsOrMutatingRows() {
        AtomicInteger actualReads = new AtomicInteger(), futureReads = new AtomicInteger();
        var rows = data();
        var actual = ChartSeries.<Row>line("Actual", row -> { actualReads.incrementAndGet(); return row.actual; });
        var forecast = ChartSeries.<Row>line("Forecast", row -> { futureReads.incrementAndGet(); return row.forecast; }).setDashed(true).setJoin(actual);
        render(rows, actual, forecast);
        assertThat(actualReads.get()).isEqualTo(4); assertThat(futureReads.get()).isEqualTo(4); assertThat(rows.get(1).forecast).isNull();
    }
    @Test void chainsUseRealSourceSamplesRegardlessOfRegistrationOrder() {
        var actual = ChartSeries.line("Actual", Row::actual);
        var forecast = ChartSeries.line("Forecast", Row::forecast).setDashed(true).setJoin(actual);
        var later = ChartSeries.<Row>line("Later", row -> row.name.equals("Apr") ? 50 : null).setDashed(true).setJoin(forecast);
        tester.startComponentInPage(new SwChart<>("chart", Model.ofList(data()), Row::name)
                .addSeries(later).addSeries(forecast).addSeries(actual));
        assertThat(group(0).getAttribute("data-join-index")).isEqualTo("2");
        assertThat(group(1).getAttribute("data-join-index")).isEqualTo("1");
        assertThat(html()).contains("<th scope=\"row\">Feb</th><td></td><td></td><td>20</td>");
    }
    @Test void invalidRelationshipsFailClearly() {
        var source = ChartSeries.line("Actual", Row::actual);
        assertThatThrownBy(() -> render(data(), source, ChartSeries.line("Forecast", Row::forecast).setJoin(source)))
                .hasStackTraceContaining("requires a dashed line or area");
        assertThatThrownBy(() -> render(data(), source, ChartSeries.bar("Forecast", Row::forecast).setDashed(true).setJoin(source)))
                .hasStackTraceContaining("requires a dashed line or area");
        assertThatThrownBy(() -> render(data(), source, ChartSeries.line("Forecast", Row::forecast).setDashed(true).setJoin(ChartSeries.line("Foreign", Row::actual))))
                .hasStackTraceContaining("must belong to the same chart");
        var bar = ChartSeries.bar("Actual", Row::actual);
        assertThatThrownBy(() -> render(data(), bar, ChartSeries.line("Forecast", Row::forecast).setDashed(true).setJoin(bar)))
                .hasStackTraceContaining("source must be a line or area");
        var cycle = ChartSeries.line("Cycle", Row::forecast).setDashed(true);
        cycle.setJoin(cycle);
        assertThatThrownBy(() -> render(data(), source, cycle)).hasStackTraceContaining("must not contain a cycle");
        var other = ChartSeries.line("Other", Row::actual).setDashed(true).setJoin(cycle);
        cycle.setJoin(other);
        assertThatThrownBy(() -> render(data(), other, cycle)).hasStackTraceContaining("must not contain a cycle");
        var duplicate = ChartSeries.line("Forecast", Row::forecast).setDashed(true).setJoin(source);
        assertThatThrownBy(() -> tester.startComponentInPage(new SwChart<>("chart", Model.ofList(data()), Row::name)
                .addSeries(source).addSeries(source).addSeries(duplicate))).hasStackTraceContaining("registered exactly once");
        var area = ChartSeries.area("Actual", Row::actual).setStack("one");
        assertThatThrownBy(() -> render(data(), area, ChartSeries.area("Forecast", Row::forecast).setDashed(true).setStack("two").setJoin(area)))
                .hasStackTraceContaining("must share the same stack");
    }
}

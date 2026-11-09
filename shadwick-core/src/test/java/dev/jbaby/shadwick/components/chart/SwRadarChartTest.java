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

class SwRadarChartTest {
    record Row(String name, Number value) implements Serializable {}
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() { tester.destroy(); }
    SwRadarChart<Row> chart(List<Row> rows) {
        return new SwRadarChart<>("chart", Model.ofList(rows), Row::name).setMaximum(100)
                .addSeries(RadarSeries.of("Current", Row::value));
    }
    List<Row> rows() { return List.of(new Row("A", 100), new Row("B", 50), new Row("C", 0), new Row("D", 25)); }
    String html() { return tester.getLastResponseAsString(); }
    List<TagTester> tags(String css) { return TagTester.createTags(html(), tag -> { CharSequence value = tag.getAttribute("class"); return value != null && Arrays.asList(value.toString().split(" ")).contains(css); }, false); }
    TagTester group(int series) { return TagTester.createTagsByAttribute(html(), "data-series", Integer.toString(series), false).get(0); }

    @Test void valuesMapLinearlyToRadiiAndTheDimensionKeyMatchesSpokeOrder() {
        tester.startComponentInPage(chart(rows()));
        assertThat(tags("sw-radar-area")).hasSize(1);
        assertThat(tags("sw-radar-area").get(0).getAttribute("points")).isEqualTo("160,40 220,160 160,160 130,160");
        assertThat(tags("sw-radar-grid")).hasSize(4);
        assertThat(tags("sw-radar-spoke")).hasSize(4);
        assertThat(tags("sw-chart-hit")).hasSize(4);
        assertThat(tags("sw-radar-name")).extracting(TagTester::getValue).containsExactly("A", "B", "C", "D");
        assertThat(tags("sw-radar-points").get(0).getAttribute("viewBox")).isNull();
        assertThat(html()).contains("A; Current: 100", "Scale: 0 – 100", "data-chart-index=\"0\"", "<td>50</td>")
                .doesNotContain("style=", "NaN", "Infinity");
    }

    @Test void incompleteSeriesLeaveOnlyAdjacentMeasuredEdgesIncludingTheAngularSeam() {
        var data = List.of(new Row("A", 80), new Row("B", 60), new Row("C", 70),
                new Row("D", null), new Row("E", 50), new Row("F", 40));
        tester.startComponentInPage(chart(data).addSeries(RadarSeries.<Row>of("Target", row -> 90)));
        assertThat(TagTester.createTagsByAttribute(group(0).getValue(), "class", "sw-radar-area", false)).isEmpty();
        assertThat(TagTester.createTagsByAttribute(group(0).getValue(), "class", "sw-radar-line", false)).hasSize(4);
        assertThat(TagTester.createTagsByAttribute(group(1).getValue(), "class", "sw-radar-area", false)).hasSize(1);
        assertThat(html()).contains("D; Current: Not measured; Target: 90", "<td data-tooltip-value=\"Not measured\"></td>");
        assertThat(tags("sw-radar-dot")).hasSize(11);
    }

    @Test void automaticAndExplicitScalesShareZeroWithoutClampingReportedValues() {
        var data = List.of(new Row("A", 42), new Row("B", 70), new Row("C", 85));
        tester.startComponentInPage(chart(data).setAutoScale());
        assertThat(tags("sw-radar-area").get(0).getAttribute("points")).startsWith("160,109.6 ");
        assertThat(tags("sw-radar-scale").get(0).getValue()).isEqualTo("Scale: 0 – 100");
        tester.startComponentInPage(chart(data).setMaximum(200));
        assertThat(tags("sw-radar-area").get(0).getAttribute("points")).startsWith("160,134.8 ");
        assertThat(html()).contains("<td>85</td>");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(data).setMaximum(50)))
                .hasStackTraceContaining("must not exceed the configured maximum");
    }

    @Test void decimalRatiosRemainDrawableBelowDoubleRange() {
        var data = List.of(new Row("A", new BigDecimal("1e-1000")), new Row("B", new BigDecimal("5e-1001")),
                new Row("C", new BigDecimal("2.5e-1001")));
        tester.startComponentInPage(chart(data).setAutoScale().setValueFormat((n, locale) -> n.toString()));
        assertThat(tags("sw-radar-area").get(0).getAttribute("points")).startsWith("160,40 ");
        assertThat(html()).contains("1E-1000", "5E-1001").doesNotContain("NaN", "Infinity");
        tester.startComponentInPage(chart(data).setMaximum(new BigDecimal("2e-1000")));
        assertThat(tags("sw-radar-area").get(0).getAttribute("points")).startsWith("160,100 ");
    }

    @Test void allZeroMeasurementsCollapseToTheCenterRatherThanBecomingMissing() {
        tester.startComponentInPage(chart(List.of(new Row("A", 0), new Row("B", 0), new Row("C", 0))).setAutoScale());
        assertThat(tags("sw-radar-area").get(0).getAttribute("points")).isEqualTo("160,160 160,160 160,160");
        assertThat(tags("sw-radar-dot")).hasSize(3);
        assertThat(html()).contains("Scale: 0 – 1", "<td>0</td>").doesNotContain("No measurements", "NaN");
    }

    @Test void emptyShortAndUnmeasuredDataHaveUsefulDistinctNativeStates() {
        tester.startComponentInPage(chart(List.of()));
        assertThat(html()).contains("No data to display").doesNotContain("<table", "sw-radar-plot");
        tester.startComponentInPage(chart(List.of(new Row("Only", 30), new Row("Second", 40))));
        assertThat(html()).contains("At least three dimensions", "<td>30</td>", "<td>40</td>").doesNotContain("sw-radar-plot");
        tester.startComponentInPage(chart(List.of(new Row("A", null), new Row("B", null), new Row("C", null))));
        assertThat(html()).contains("No measurements to plot", "data-tooltip-value=\"Not measured\"").doesNotContain("sw-radar-plot");
    }

    @ParameterizedTest @ValueSource(doubles={-1, Double.NaN, Double.POSITIVE_INFINITY, 1e101})
    void invalidMeasurementsFailClearly(double value) {
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(new Row("A", value), new Row("B", 20), new Row("C", 30)))))
                .hasStackTraceContaining("Radar values must be finite, nonnegative");
    }

    @Test void exactDecimalBoundsCannotHideBehindDoubleRoundingOrUnderflow() {
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(new Row("A", new BigDecimal("-1e-1000"))))))
                .hasStackTraceContaining("nonnegative");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(new Row("A", new BigDecimal("1.000000000000000000001"))))
                .setMaximum(1))).hasStackTraceContaining("must not exceed the configured maximum");
    }

    @ParameterizedTest @ValueSource(doubles={0, -1, Double.NaN, Double.POSITIVE_INFINITY, 1e101})
    void configuredMaximumMustBePositiveAndFinite(double value) {
        assertThatThrownBy(() -> tester.startComponentInPage(chart(rows()).setMaximum(value)))
                .hasStackTraceContaining(value == 0 ? "maximum must be positive" : "finite, nonnegative");
    }

    @Test void missingMaximumIsAConfigurationErrorAndAutoScaleReleasesThePreviousModel() {
        assertThatThrownBy(() -> tester.startComponentInPage(chart(rows()).setMaximum(Model.<Number>of((Number) null))))
                .hasStackTraceContaining("maximum must be positive");
        AtomicInteger detaches = new AtomicInteger();
        IModel<Number> maximum = new IModel<>() {
            public Number getObject() { return 100; }
            public void detach() { detaches.incrementAndGet(); }
        };
        var chart = chart(rows()).setMaximum(maximum);
        chart.setAutoScale();
        assertThat(detaches.get()).isEqualTo(1);
        tester.startComponentInPage(chart);
        assertThat(detaches.get()).isEqualTo(1);
    }

    @Test void seriesAppearanceAndHiddenPartsPreserveTheNativeData() {
        var first = RadarSeries.of("Current", Row::value).setDots(false).setColor(3).setDashed(true).setFillOpacity(.3).setStrokeWidth(4);
        tester.startComponentInPage(new SwRadarChart<>("chart", Model.ofList(rows()), Row::name).addSeries(first)
                .setMaximum(100).setLevels(6).setShowGrid(false).setShowLegend(false).setShowTooltip(false));
        assertThat(tags("sw-radar-grid")).isEmpty();
        assertThat(tags("sw-radar-spoke")).isEmpty();
        assertThat(tags("sw-radar-dot")).isEmpty();
        assertThat(tags("sw-radar-line").get(0).getAttribute("stroke-width")).isEqualTo("4");
        assertThat(group(0).getAttribute("class")).isEqualTo("sw-chart-series-3");
        assertThat(html()).contains("stroke-dasharray=\"5 5\"", "fill-opacity=\"0.3\"", "data-tooltip=\"false\"", "<table")
                .doesNotContain("sw-chart-legend-content", "Use Left and Right");
    }

    @Test void configuredFormattersReceiveOriginalNumbersAndEscapeTheirOutput() {
        tester.startComponentInPage(chart(List.of(new Row("<img src=x>", new BigDecimal("0.123456789123456789")),
                new Row("B", .5), new Row("C", .75))).setTitle("<Title>").setCategoryHeader("<Dimension>")
                .setMaximum(1).setValueFormat((n, locale) -> "<" + n + ">"));
        assertThat(html()).contains("&lt;img", "&lt;Title&gt;", "&lt;Dimension&gt;", "&lt;0.123456789123456789&gt;")
                .doesNotContain("<img", "<Title>", "style=");
    }

    @ParameterizedTest @CsvSource({"de,Dimensionen,Skala,Nicht gemessen", "fr,Dimensions,Échelle,Non mesuré", "ja,項目,目盛,未測定"})
    void dimensionScaleAndMissingLabelsFollowTheLocale(String language, String dimensions, String scale, String missing) {
        tester.getSession().setLocale(Locale.forLanguageTag(language));
        tester.startComponentInPage(chart(List.of(new Row("A", 20), new Row("B", null), new Row("C", 30)))
                .setCurrency(Currency.getInstance("EUR")));
        assertThat(tags("sw-radar-dimensions").get(0).getAttribute("aria-label")).isEqualTo(dimensions);
        assertThat(tags("sw-radar-scale").get(0).getValue()).contains(scale, Currency.getInstance("EUR").getSymbol(tester.getSession().getLocale()));
        assertThat(html()).contains(missing);
    }

    @Test void labelsRegistrationAndPresentationRangesHaveClearValidation() {
        assertThatThrownBy(() -> tester.startComponentInPage(new SwRadarChart<Row>("chart", Model.ofList(List.of()), Row::name)))
                .hasStackTraceContaining("at least one series");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(rows()).setTitle(" "))).hasStackTraceContaining("needs a title");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(rows()).setCategoryHeader(" "))).hasStackTraceContaining("dimension header");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(new Row(" ", 20))))).hasStackTraceContaining("dimension needs a label");
        assertThatThrownBy(() -> tester.startComponentInPage(new SwRadarChart<>("chart", Model.ofList(rows()), Row::name)
                .addSeries(RadarSeries.of(" ", Row::value)))).hasStackTraceContaining("series needs a label");
        var part = RadarSeries.of("A", Row::value);
        assertThatIllegalArgumentException().isThrownBy(() -> chart(rows()).addSeries(part).addSeries(part));
        assertThatIllegalArgumentException().isThrownBy(() -> chart(rows()).setLevels(1));
        assertThatIllegalArgumentException().isThrownBy(() -> chart(rows()).setLevels(11));
        assertThatIllegalArgumentException().isThrownBy(() -> part.setColor(6));
        assertThatIllegalArgumentException().isThrownBy(() -> part.setFillOpacity(Double.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> part.setStrokeWidth(21));
    }

    @Test void modelsAndExtractorsAreReadOnceAndDetachWithTheChart() {
        AtomicInteger loads = new AtomicInteger(), categories = new AtomicInteger(), reads = new AtomicInteger(), detaches = new AtomicInteger();
        IModel<List<Row>> data = new IModel<>() {
            public List<Row> getObject() { loads.incrementAndGet(); return rows(); }
            public void detach() { detaches.incrementAndGet(); }
        };
        IModel<String> text = new IModel<>() {
            public String getObject() { return "Caption"; }
            public void detach() { detaches.incrementAndGet(); }
        };
        IModel<Number> maximum = new IModel<>() {
            public Number getObject() { return 100; }
            public void detach() { detaches.incrementAndGet(); }
        };
        var chart = new SwRadarChart<>("chart", data, row -> { categories.incrementAndGet(); return row.name; })
                .setTitle(text).setCategoryHeader(text).setMaximum(maximum)
                .addSeries(RadarSeries.of(text, row -> { reads.incrementAndGet(); return row.value; }));
        assertThat(chart.get("figure")).isNull();
        tester.startComponentInPage(chart);
        assertThat(chart.get("figure")).isNotNull();
        assertThat(loads.get()).isEqualTo(1);
        assertThat(categories.get()).isEqualTo(4);
        assertThat(reads.get()).isEqualTo(4);
        assertThat(detaches.get()).isEqualTo(5);
    }

    @Test void serializationRetainsModelsAndAjaxCanChangeDimensionsMaximumAndAppearance() {
        tester.startPage(RadarPage.class);
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        var restored = (WebPage) serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));
        tester.startPage(restored);
        tester.clickLink("refresh");
        assertThat(html()).contains("<component", "Latest", "Scale: 0 – 120", "sw-chart-series-3", "New dimension")
                .doesNotContain("<redirect");
        assertThat(tags("sw-chart-hit")).hasSize(3);
    }

    public static class RadarPage extends WebPage implements IMarkupResourceStreamProvider {
        @Override protected void onInitialize() {
            super.onInitialize();
            IModel<List<Row>> data = Model.ofList(List.of(new Row("A", 50), new Row("B", 60), new Row("C", 70), new Row("D", 80)));
            IModel<Number> maximum = Model.of(100);
            IModel<String> label = Model.of("Current");
            var part = RadarSeries.of(label, Row::value);
            var chart = new SwRadarChart<>("chart", data, Row::name).setMaximum(maximum).addSeries(part);
            add(chart);
            add(SwButton.onClick("refresh", "Refresh", target -> {
                data.setObject(List.of(new Row("New dimension", 90), new Row("B", 100), new Row("C", 110)));
                maximum.setObject(120);
                label.setObject("Latest");
                part.setColor(3).setFillOpacity(.2);
                target.add(chart);
            }));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><body><div wicket:id='chart'></div><a wicket:id='refresh'>Refresh</a></body></html>");
        }
    }
}

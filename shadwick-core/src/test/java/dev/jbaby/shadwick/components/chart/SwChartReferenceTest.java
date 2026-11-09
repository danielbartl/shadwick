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

class SwChartReferenceTest {
    record Row(String name, Number value) implements Serializable {}
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() { tester.destroy(); }
    SwChart<Row> chart() {
        return new SwChart<>("chart", Model.ofList(List.of(new Row("First", 20), new Row("Second", 40))), Row::name)
                .addSeries(ChartSeries.bar("Value", Row::value));
    }
    String html() { return tester.getLastResponseAsString(); }
    List<TagTester> tags(String css) {
        return TagTester.createTags(html(), tag -> Arrays.asList(Objects.toString(tag.getAttributes().get("class"), "").split(" ")).contains(css), false);
    }
    double number(TagTester tag, String key) { return Double.parseDouble(tag.getAttribute(key)); }

    @ParameterizedTest @EnumSource(SwChart.Orientation.class)
    void aTargetExtendsTheScaleAndIsDescribedWithoutAddingATableSeries(SwChart.Orientation orientation) {
        tester.startComponentInPage(chart().setOrientation(orientation).addReference(ChartReference.line("Target", 80)));
        var line = tags("sw-chart-reference-line").get(0);
        assertThat(number(line, orientation == SwChart.Orientation.VERTICAL ? "y1" : "x1"))
                .isEqualTo(orientation == SwChart.Orientation.VERTICAL ? 0 : 1000);
        assertThat(line.getAttribute("stroke-dasharray")).isEqualTo("5 5");
        assertThat(tags("sw-chart-reference-value")).extracting(TagTester::getValue).containsExactly("80");
        var plot = tags("sw-chart-plot").get(0);
        assertThat(plot.getAttribute("aria-describedby")).endsWith("-references");
        assertThat(html()).contains("<th scope=\"row\">First</th><td>20</td>", "<th scope=\"row\">Second</th><td>40</td>")
                .doesNotContain("<th scope=\"col\">Target", "style=", "NaN", "Infinity");
    }

    @Test void bandsExtendBothEndsOfAnAbsoluteScaleAndCanIncludeNegativeValues() {
        tester.startComponentInPage(chart().addReference(ChartReference.band("Range", -20, 60)));
        var band = tags("sw-chart-reference-band").get(0);
        assertThat(number(band, "y")).isZero();
        assertThat(number(band, "height")).isEqualTo(240);
        assertThat(html()).contains("-20 – 60");
    }

    @ParameterizedTest @EnumSource(SwChart.Orientation.class)
    void clippingKeepsTheDataDomainAndTrimsBandsWithoutInventingOutsideLines(SwChart.Orientation orientation) {
        tester.startComponentInPage(chart().setOrientation(orientation)
                .addReference(ChartReference.line("Outside", 80).setExtendDomain(false))
                .addReference(ChartReference.line("Boundary", 40).setExtendDomain(false))
                .addReference(ChartReference.band("Clipped", -10, 20).setExtendDomain(false))
                .addReference(ChartReference.band("Outside range", 50, 70).setExtendDomain(false)));
        int repetitions = orientation == SwChart.Orientation.VERTICAL ? 1 : 2;
        assertThat(tags("sw-chart-reference-line")).hasSize(repetitions);
        assertThat(tags("sw-chart-reference-band")).hasSize(repetitions);
        var band = tags("sw-chart-reference-band").get(0);
        assertThat(number(band, orientation == SwChart.Orientation.VERTICAL ? "height" : "width"))
                .isEqualTo(orientation == SwChart.Orientation.VERTICAL ? 120 : 500);
        assertThat(tags("sw-chart-reference-name")).extracting(TagTester::getValue)
                .containsExactly("Outside", "Boundary", "Clipped", "Outside range");
    }

    @Test void horizontalGuidesRepeatPerRowAndLinesStayAboveNativeRoundedBars() {
        tester.startComponentInPage(new SwChart<>("chart", Model.ofList(List.of(new Row("A", 20), new Row("B", 40))), Row::name)
                .setOrientation(SwChart.Orientation.HORIZONTAL)
                .addSeries(ChartSeries.bar("Value", Row::value).setBarRadius(5))
                .addReference(ChartReference.line("Middle", 20).setExtendDomain(false).setColor(3).setDashed(false))
                .addReference(ChartReference.band("Middle range", 10, 30).setExtendDomain(false).setColor(2).setFillOpacity(.2)));
        assertThat(tags("sw-chart-reference-line")).hasSize(2).allSatisfy(line -> {
            assertThat(number(line, "x1")).isEqualTo(500);
            assertThat(number(line, "y2")).isEqualTo(36);
            assertThat(line.getAttribute("stroke-dasharray")).isNull();
        });
        assertThat(tags("sw-chart-reference-band")).allSatisfy(band -> {
            assertThat(number(band, "x")).isEqualTo(250);
            assertThat(number(band, "width")).isEqualTo(500);
            assertThat(band.getAttribute("fill-opacity")).isEqualTo("0.2");
        });
        assertThat(html().indexOf("sw-chart-reference-band")).isLessThan(html().indexOf("sw-chart-native-bars"));
        assertThat(html().indexOf("sw-chart-reference-overlay")).isGreaterThan(html().indexOf("sw-chart-native-bars"));
        assertThat(tags("sw-chart-reference-item")).allSatisfy(item -> assertThat(item.getAttribute("class")).contains("sw-chart-series-"));
    }

    @ParameterizedTest @EnumSource(SwChart.Orientation.class)
    void percentReferencesUseFractionsAndKeepTheirPercentTextIndependentOfRawAmounts(SwChart.Orientation orientation) {
        tester.startComponentInPage(new SwChart<>("chart", Model.ofList(List.of(new Row("A", 40))), Row::name)
                .addSeries(ChartSeries.bar("Value", Row::value).setStack("total"))
                .setStackMode(SwChart.StackMode.PERCENT).setOrientation(orientation)
                .setCurrency(Currency.getInstance("EUR"))
                .addReference(ChartReference.line("Target", .5))
                .addReference(ChartReference.band("Range", .25, .75))
                .addSeries(ChartSeries.<Row>bar("Other", row -> 20).setStack("total")));
        var line = tags("sw-chart-reference-line").get(0);
        assertThat(number(line, orientation == SwChart.Orientation.VERTICAL ? "y1" : "x1"))
                .isEqualTo(orientation == SwChart.Orientation.VERTICAL ? 120 : 500);
        assertThat(tags("sw-chart-reference-value")).extracting(TagTester::getValue).containsExactly("50%", "25% – 75%");
        assertThat(html()).contains("<td>€40.00</td>", "<td>€20.00</td>");
    }

    @ParameterizedTest @ValueSource(strings={"-0.1", "1.01", "-1e-1000", "1.000000000000000000000000001"})
    void percentReferencesOutsideZeroAndOneFailEvenWhenTheirDoubleRoundsInside(String value) {
        var chart = new SwChart<>("chart", Model.ofList(List.of(new Row("A", 1))), Row::name)
                .setStackMode(SwChart.StackMode.PERCENT).addSeries(ChartSeries.bar("Value", Row::value).setStack("total"))
                .addReference(ChartReference.line("Bad", new BigDecimal(value)).setExtendDomain(false));
        assertThatThrownBy(() -> tester.startComponentInPage(chart)).hasStackTraceContaining("Percent chart references must be between 0 and 1");
    }

    @ParameterizedTest @ValueSource(doubles={Double.NaN, Double.POSITIVE_INFINITY, 1e101})
    void nonfiniteAndExcessiveReferenceValuesFailClearly(double value) {
        assertThatThrownBy(() -> tester.startComponentInPage(chart().addReference(ChartReference.line("Bad", value))))
                .hasStackTraceContaining("Reference values must be finite");
    }

    @Test void invalidOrDegenerateRangesAndBlankLabelsFailRatherThanSilentlyChangingTheMeaning() {
        for (int upper : List.of(10, 20)) {
            assertThatThrownBy(() -> tester.startComponentInPage(chart().addReference(ChartReference.band("Bad", 20, upper))))
                    .hasStackTraceContaining("lower value must be less than its upper value");
        }
        assertThatThrownBy(() -> tester.startComponentInPage(chart().addReference(ChartReference.line(" ", 20))))
                .hasStackTraceContaining("needs a label");
        assertThatIllegalArgumentException().isThrownBy(() -> ChartReference.line("A", 1).setColor(6));
        assertThatIllegalArgumentException().isThrownBy(() -> ChartReference.band("A", 1, 2).setFillOpacity(Double.NaN));
        assertThatIllegalStateException().isThrownBy(() -> ChartReference.line("A", 1).setFillOpacity(.5));
        assertThatIllegalStateException().isThrownBy(() -> ChartReference.band("A", 1, 2).setDashed(false));
        var reference = ChartReference.line("A", 1);
        assertThatIllegalArgumentException().isThrownBy(() -> chart().addReference(reference).addReference(reference));
    }

    @Test void hiddenOrMissingReferenceValuesOmitGuidesAndDescriptions() {
        tester.startComponentInPage(chart().addReference(ChartReference.line("Hidden", Double.NaN).setVisible(false))
                .addReference(ChartReference.line("Missing", Model.<Number>of((Number) null)))
                .addReference(ChartReference.band("Incomplete", Model.of(10), Model.<Number>of((Number) null))));
        assertThat(tags("sw-chart-references")).isEmpty();
        assertThat(tags("sw-chart-reference-line")).isEmpty();
        assertThat(tags("sw-chart-plot").get(0).getAttribute("aria-describedby")).doesNotContain("-references");
    }

    @Test void captionsRemainUsefulWithEmptyDataOrHiddenAxesGridAndSeriesLegend() {
        tester.startComponentInPage(new SwChart<Row>("chart", Model.ofList(List.of()), Row::name)
                .addSeries(ChartSeries.bar("Value", Row::value)).addReference(ChartReference.line("Known target", 100)));
        assertThat(html()).contains("No data to display", "Known target", ">100</span>").doesNotContain("<svg");
        tester.startComponentInPage(chart().setShowGrid(false).setShowLegend(false).setShowXAxis(false).setShowYAxis(false)
                .setShowTooltip(false).addReference(ChartReference.line("Still a target", 30)));
        assertThat(tags("sw-chart-reference-line")).hasSize(1);
        assertThat(html()).contains("Still a target").doesNotContain("sw-chart-grid", "sw-chart-legend-content");
    }

    @Test void originalReferenceNumbersAndLabelsAreEscapedAndIgnoreCompactTickOverrides() {
        tester.startComponentInPage(chart().setAxisValueFormat(SwChart.ValueFormat.COMPACT)
                .setValueFormat((number, locale) -> "<" + number + ">")
                .addReference(ChartReference.band("<range>", new BigDecimal("1000.123456"), new BigDecimal("2000.654321"))));
        assertThat(html()).contains("&lt;range&gt;", "&lt;1000.123456&gt; – &lt;2000.654321&gt;")
                .doesNotContain("<range>", "<1000.123456>", "style=");
    }

    @ParameterizedTest @CsvSource({"de,Referenzwerte", "fr,Repères", "ja,基準値"})
    void referenceCaptionsUseLocalizedLabelsAndSharedCurrencyFormatting(String language, String label) {
        tester.getSession().setLocale(Locale.forLanguageTag(language));
        tester.startComponentInPage(chart().setCurrency(Currency.getInstance("EUR")).addReference(ChartReference.line("Target", 30)));
        assertThat(tags("sw-chart-references").get(0).getAttribute("aria-label")).isEqualTo(label);
        assertThat(tags("sw-chart-reference-value").get(0).getValue())
                .contains(Currency.getInstance("EUR").getSymbol(tester.getSession().getLocale()));
    }

    @Test void eachReferenceModelIsReadOncePerRequestAndHiddenModelsStillDetach() {
        AtomicInteger reads = new AtomicInteger(), labels = new AtomicInteger(), detaches = new AtomicInteger();
        IModel<Number> value = new IModel<>() {
            public Number getObject() { reads.incrementAndGet(); return 30; }
            public void detach() { detaches.incrementAndGet(); }
        };
        IModel<String> label = new IModel<>() {
            public String getObject() { labels.incrementAndGet(); return "Target"; }
            public void detach() { detaches.incrementAndGet(); }
        };
        tester.startComponentInPage(chart().setOrientation(SwChart.Orientation.HORIZONTAL).addReference(ChartReference.line(label, value)));
        assertThat(reads.get()).isEqualTo(1);
        assertThat(labels.get()).isEqualTo(1);
        assertThat(detaches.get()).isEqualTo(2);
        reads.set(0); labels.set(0); detaches.set(0);
        tester.startComponentInPage(chart().addReference(ChartReference.line(label, value).setVisible(false)));
        assertThat(reads.get()).isZero();
        assertThat(labels.get()).isZero();
        assertThat(detaches.get()).isEqualTo(2);
    }

    @Test void serializationKeepsModelReferencesAndAjaxRefreshUpdatesTheirLabelsAndGeometry() {
        tester.startPage(ReferencePage.class);
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        var restored = (WebPage) serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));
        tester.startPage(restored);
        tester.clickLink("refresh");
        assertThat(html()).contains("<component", "Updated target", ">80</span>", "Updated data").doesNotContain("<redirect");
        assertThat(number(tags("sw-chart-reference-line").get(0), "y1")).isZero();
    }

    public static class ReferencePage extends WebPage implements IMarkupResourceStreamProvider {
        @Override protected void onInitialize() {
            super.onInitialize();
            IModel<List<Row>> data = Model.ofList(List.of(new Row("Original", 20)));
            IModel<String> label = Model.of("Target");
            IModel<Integer> value = Model.of(40);
            var chart = new SwChart<>("chart", data, Row::name).addSeries(ChartSeries.bar("Value", Row::value))
                    .addReference(ChartReference.line(label, value));
            add(chart);
            add(SwButton.onClick("refresh", "Refresh", target -> {
                label.setObject("Updated target");
                value.setObject(80);
                data.setObject(List.of(new Row("Updated data", 20)));
                target.add(chart);
            }));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><body><div wicket:id='chart'></div><a wicket:id='refresh'>Refresh</a></body></html>");
        }
    }
}

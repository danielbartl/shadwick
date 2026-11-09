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

class SwRadialChartTest {
    record Row(String name, Number value) implements Serializable {}
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() { tester.destroy(); }
    SwRadialChart<Row> chart(List<Row> rows) {
        return new SwRadialChart<>("chart", Model.ofList(rows), Row::name, Row::value).setMaximum(100);
    }
    List<Row> rows() { return List.of(new Row("Delivery", 75), new Row("Quality", 50), new Row("Research", 100)); }
    String html() { return tester.getLastResponseAsString(); }
    List<TagTester> tags(String css) {
        return TagTester.createTags(html(), tag -> { CharSequence classes = tag.getAttribute("class");
            return classes != null && Arrays.asList(classes.toString().split(" ")).contains(css); }, false);
    }
    TagTester ring(int index) { return TagTester.createTagsByAttribute(html(), "data-ring", Integer.toString(index), false).get(0); }
    TagTester bar(int index) { return TagTester.createTagsByAttribute(ring(index).getValue(), "class", "sw-radial-bar", false).get(0); }

    @Test void ringLengthsUseTheSharedScaleAndFullCirclesUseTwoArcs() {
        tester.startComponentInPage(chart(List.of(new Row("Half", 50), new Row("Full", 100))).setInnerRadius(0).setGap(0));
        assertThat(bar(0).getAttribute("d")).isEqualTo("M160,58 A102,102 0 0 1 262,160 A102,102 0 0 1 160,262");
        assertThat(bar(1).getAttribute("d")).isEqualTo("M160,126 A34,34 0 0 1 160,194 A34,34 0 0 1 160,126");
        assertThat(bar(0).getAttribute("stroke-width")).isEqualTo("68");
        assertThat(tags("sw-radial-track")).hasSize(2);
        assertThat(tags("sw-radial-hit")).hasSize(2);
        assertThat(tags("sw-radial-name")).extracting(TagTester::getValue).containsExactly("Half", "Full");
        assertThat(html()).contains("Scale: 0 – 100", "Half; Value: 50", "data-orientation=\"radial\"")
                .doesNotContain("NaN", "Infinity", "style=", "tfoot");
    }

    @Test void partialSweepsAndStartAnglesApplyToTracksHitsAndMeasurements() {
        tester.startComponentInPage(chart(List.of(new Row("Half", 50))).setInnerRadius(0).setGap(.5)
                .setStartAngle(0).setSweepAngle(180).setRounded(true));
        assertThat(bar(0).getAttribute("d")).isEqualTo("M228,160 A68,68 0 0 1 208.08326,208.08326 A68,68 0 0 1 160,228");
        assertThat(bar(0).getAttribute("stroke-width")).isEqualTo("68");
        assertThat(bar(0).getAttribute("stroke-linecap")).isEqualTo("round");
        assertThat(tags("sw-radial-track").get(0).getAttribute("d")).endsWith("92,160");
        assertThat(tags("sw-radial-hit").get(0).getAttribute("stroke-width")).isEqualTo("136");
    }

    @Test void missingAndZeroRowsKeepTheirOriginalRingsColorsAndExactCells() {
        tester.startComponentInPage(chart(List.of(new Row("Observed", 40), new Row("Zero", 0), new Row("Unknown", null), new Row("Observed", 70))));
        assertThat(tags("sw-radial-bar")).hasSize(2);
        assertThat(tags("sw-radial-track")).hasSize(4);
        assertThat(tags("sw-radial-hit")).hasSize(4);
        assertThat(ring(3).getAttribute("class")).isEqualTo("sw-chart-series-4");
        assertThat(html()).contains("Unknown; Value: Not measured", "data-tooltip-value=\"Not measured\"")
                .contains("<td data-color=\"sw-chart-series-2\">0</td>");
        assertThat(tags("sw-radial-name")).extracting(TagTester::getValue).containsExactly("Observed", "Zero", "Unknown", "Observed");
    }

    @Test void automaticScaleAndConfiguredModelsDoNotClampReportedValues() {
        var chart = chart(rows()).setAutoScale();
        tester.startComponentInPage(chart);
        assertThat(tags("sw-radial-scale").get(0).getValue()).isEqualTo("Scale: 0 – 100");
        IModel<Number> maximum = Model.of(200);
        chart.setMaximum(maximum);
        tester.startComponentInPage(chart);
        assertThat(tags("sw-radial-scale").get(0).getValue()).isEqualTo("Scale: 0 – 200");
        assertThat(html()).contains(">75</td>");
        maximum.setObject(50);
        assertThatThrownBy(() -> tester.startComponentInPage(chart)).hasStackTraceContaining("must not exceed the configured maximum");
    }

    @Test void decimalRatiosAndExactBoundsSurviveDoubleUnderflowAndRounding() {
        var data = List.of(new Row("Tiny", new BigDecimal("1e-1000")), new Row("Half", new BigDecimal("5e-1001")));
        tester.startComponentInPage(chart(data).setAutoScale().setValueFormat((n, locale) -> n.toString()));
        assertThat(html()).contains("1E-1000", "5E-1001").doesNotContain("NaN", "Infinity");
        String full = bar(0).getAttribute("d");
        tester.startComponentInPage(chart(data).setMaximum(new BigDecimal("2e-1000")));
        assertThat(bar(0).getAttribute("d")).isNotEqualTo(full);
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(new Row("Negative", new BigDecimal("-1e-1000"))))))
                .hasStackTraceContaining("nonnegative");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(new Row("Above", new BigDecimal("1.0000000000000000001")))).setMaximum(1)))
                .hasStackTraceContaining("must not exceed the configured maximum");
    }

    @Test void zeroUnknownAndEmptyReportsHaveDistinctNativeStates() {
        tester.startComponentInPage(chart(List.of(new Row("Zero", 0))).setAutoScale());
        assertThat(tags("sw-radial-bar")).isEmpty();
        assertThat(tags("sw-radial-track")).hasSize(1);
        assertThat(html()).contains("Scale: 0 – 1").doesNotContain("No measurements");
        tester.startComponentInPage(chart(List.of(new Row("Unknown", null))));
        assertThat(html()).contains("No measurements to plot", "<table", "data-tooltip-value=\"Not measured\"").doesNotContain("sw-radial-plot");
        tester.startComponentInPage(chart(List.of()));
        assertThat(html()).contains("No data to display").doesNotContain("sw-radial-plot", "<table");
        tester.startComponentInPage(new SwRadialChart<Row>("chart", Model.ofList(null), Row::name, Row::value));
        assertThat(html()).contains("No data to display");
    }

    @Test void colorsCanFollowDomainKeysAndHiddenPresentationKeepsOriginalData() {
        var chart = chart(rows()).setColor(row -> row.name.equals("Quality") ? 5 : 0)
                .setShowTracks(false).setShowLegend(false).setShowTooltip(false);
        tester.startComponentInPage(chart);
        assertThat(ring(1).getAttribute("class")).isEqualTo("sw-chart-series-5");
        assertThat(tags("sw-radial-track")).isEmpty();
        assertThat(tags("sw-radial-legend")).isEmpty();
        assertThat(tags("sw-radial-bar")).hasSize(3);
        assertThat(html()).contains("data-tooltip=\"false\"", "data-color=\"sw-chart-series-5\"", "<table").doesNotContain("Use arrow keys");
        tester.startComponentInPage(chart.setColor(null));
        assertThat(ring(1).getAttribute("class")).isEqualTo("sw-chart-series-2");
        assertThatThrownBy(() -> tester.startComponentInPage(chart.setColor(row -> 6))).hasStackTraceContaining("color must be between 0 and 5");
    }

    @Test void formattersReceiveOriginalNumbersAndAllTextIsEscaped() {
        tester.startComponentInPage(chart(List.of(new Row("<img src=x>", new BigDecimal("0.123456789123456789"))))
                .setTitle("<Title>").setCategoryHeader("<Category>").setValueHeader("<Value>")
                .setMaximum(1).setValueFormat((n, locale) -> "<" + n + ">"));
        assertThat(html()).contains("&lt;img", "&lt;Title&gt;", "&lt;Category&gt;", "&lt;Value&gt;", "&lt;0.123456789123456789&gt;")
                .doesNotContain("<img", "<Title>", "style=");
        tester.startComponentInPage(chart(rows()).setValueFormat((n, locale) -> ""));
        assertThat(tags("sw-radial-bar")).hasSize(3);
        assertThat(html()).doesNotContain("data-tooltip-value=");
    }

    @ParameterizedTest @ValueSource(doubles={-1, Double.NaN, Double.POSITIVE_INFINITY, 1e101})
    void invalidMeasurementsFailClearly(double value) {
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(new Row("Bad", value)))))
                .hasStackTraceContaining("Radial values must be finite, nonnegative");
    }
    @ParameterizedTest @ValueSource(doubles={0, -1, Double.NaN, Double.POSITIVE_INFINITY, 1e101})
    void fixedMaximumMustBePositiveAndFinite(double value) {
        assertThatThrownBy(() -> tester.startComponentInPage(chart(rows()).setMaximum(value)))
                .hasStackTraceContaining(value == 0 ? "maximum must be positive" : "finite, nonnegative");
    }
    @ParameterizedTest @CsvSource({"de,Kategorien,Skala,Nicht gemessen", "fr,Catégories,Échelle,Non mesuré", "ja,項目,目盛,未測定"})
    void legendScaleMissingAndCurrencyFollowTheLocale(String language, String legend, String scale, String missing) {
        tester.getSession().setLocale(Locale.forLanguageTag(language));
        tester.startComponentInPage(chart(List.of(new Row("A", 20), new Row("B", null))).setCurrency(Currency.getInstance("EUR")));
        assertThat(tags("sw-radial-legend").get(0).getAttribute("aria-label")).isEqualTo(legend);
        assertThat(tags("sw-radial-scale").get(0).getValue()).contains(scale, Currency.getInstance("EUR").getSymbol(tester.getSession().getLocale()));
        assertThat(html()).contains(missing);
    }

    @Test void labelsAndGeometryRangesHaveClearValidation() {
        assertThatThrownBy(() -> tester.startComponentInPage(chart(rows()).setTitle(" "))).hasStackTraceContaining("needs a title");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(rows()).setCategoryHeader(" "))).hasStackTraceContaining("category header");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(rows()).setValueHeader(" "))).hasStackTraceContaining("value header");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(new Row(" ", 30))))).hasStackTraceContaining("category needs a label");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(rows()).setMaximum(Model.<Number>of((Number) null)))).hasStackTraceContaining("maximum must be positive");
        assertThatIllegalArgumentException().isThrownBy(() -> chart(rows()).setStartAngle(361));
        assertThatIllegalArgumentException().isThrownBy(() -> chart(rows()).setStartAngle(Double.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> chart(rows()).setSweepAngle(0));
        assertThatIllegalArgumentException().isThrownBy(() -> chart(rows()).setSweepAngle(361));
        assertThatIllegalArgumentException().isThrownBy(() -> chart(rows()).setSweepAngle(Double.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> chart(rows()).setInnerRadius(.9));
        assertThatIllegalArgumentException().isThrownBy(() -> chart(rows()).setInnerRadius(-.1));
        assertThatIllegalArgumentException().isThrownBy(() -> chart(rows()).setGap(.9));
        assertThatIllegalArgumentException().isThrownBy(() -> chart(rows()).setGap(-.1));
    }

    @Test void modelsAndExtractorsAreReadOnceAndOwnedModelsDetach() {
        AtomicInteger loads = new AtomicInteger(), categories = new AtomicInteger(), values = new AtomicInteger(), colors = new AtomicInteger(), detaches = new AtomicInteger();
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
        var chart = new SwRadialChart<>("chart", data, row -> { categories.incrementAndGet(); return row.name; }, row -> { values.incrementAndGet(); return row.value; })
                .setTitle(text).setCategoryHeader(text).setValueHeader(text).setMaximum(maximum)
                .setColor(row -> { colors.incrementAndGet(); return 3; });
        assertThat(chart.get("figure")).isNull();
        tester.startComponentInPage(chart);
        assertThat(chart.get("figure")).isNotNull();
        assertThat(loads.get()).isEqualTo(1);
        assertThat(categories.get()).isEqualTo(3);
        assertThat(values.get()).isEqualTo(3);
        assertThat(colors.get()).isEqualTo(3);
        assertThat(detaches.get()).isEqualTo(5);
        chart.setAutoScale();
        assertThat(detaches.get()).isEqualTo(6);
    }

    @Test void serializationAndAjaxPreserveConfigurationAndReplaceTheReport() {
        tester.startPage(RadialPage.class);
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        var restored = (WebPage) serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));
        tester.startPage(restored);
        tester.clickLink("refresh");
        assertThat(html()).contains("<component", "Latest", "Scale: 0 – 120", "sw-chart-series-4", "New category", "stroke-linecap=\"round\"")
                .doesNotContain("<redirect");
        assertThat(tags("sw-radial-hit")).hasSize(2);
    }
    public static class RadialPage extends WebPage implements IMarkupResourceStreamProvider {
        @Override protected void onInitialize() {
            super.onInitialize();
            IModel<List<Row>> data = Model.ofList(List.of(new Row("A", 50), new Row("B", 60), new Row("C", 70)));
            IModel<Integer> maximum = Model.of(100);
            IModel<String> label = Model.of("Current");
            var chart = new SwRadialChart<>("chart", data, Row::name, Row::value).setValueHeader(label).setMaximum(maximum);
            add(chart);
            add(SwButton.onClick("refresh", "Refresh", target -> {
                data.setObject(List.of(new Row("New category", 90), new Row("Second", 110)));
                maximum.setObject(120);
                label.setObject("Latest");
                chart.setRounded(true).setSweepAngle(270).setColor(row -> 4);
                target.add(chart);
            }));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><body><div wicket:id='chart'></div><a wicket:id='refresh'>Refresh</a></body></html>");
        }
    }
}

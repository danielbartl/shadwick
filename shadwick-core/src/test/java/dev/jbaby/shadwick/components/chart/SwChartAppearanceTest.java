package dev.jbaby.shadwick.components.chart;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.*;
import org.apache.wicket.util.tester.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.assertj.core.api.Assertions.*;

class SwChartAppearanceTest {
    record Row(String name, Double value) implements Serializable {}
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() { tester.destroy(); }
    String html() { return tester.getLastResponseAsString(); }
    List<Row> rows() { return List.of(new Row("First", 20.0), new Row("Missing", null), new Row("Negative", -10.0), new Row("Zero", 0.0)); }
    SwChart<Row> chart(ChartSeries<Row> series) { return new SwChart<>("chart", Model.ofList(rows()), Row::name).addSeries(series); }
    @ParameterizedTest @ValueSource(strings={"#abc", "#abcd", "#123456", "#12345678", "rebeccapurple", "oklch(0.55 0.2 260)",
            "hsl(120 50% 30% / 0.5)", "var(--brand)", "color(display-p3 0.4 0.5 0.3)"})
    void safeColorsProduceDeterministicScopedRulesAndReachAllSeriesIdentities(String value) {
        var series = ChartSeries.line("Value", Row::value).setColors(value, "#aabbcc");
        String identity = ChartAppearance.className(series);
        assertThat(identity).matches("sw-chart-custom-[a-f0-9]{24}");
        assertThat(ChartAppearance.className(ChartSeries.line("Different label", Row::value).setColors(value, "#aabbcc"))).isEqualTo(identity);
        tester.startComponentInPage(chart(series));
        assertThat(ChartAppearance.css(series)).contains("@supports(color:" + value + ")", "@supports(color:#aabbcc)", ".dark .sw-chart ." + identity);
        assertThat(html()).contains("data-color=\"sw-chart-series-1 " + identity + "\"").doesNotContain("style=");
    }
    @ParameterizedTest @ValueSource(strings={"", "#12", "#12345", "url(image.svg)", "red;display:none", "</style><script>alert(1)</script>",
            "var(--color,red)", "rgb(var(--r) 0 0)", "blue!important", "red/*comment*/"})
    void unsafeOrUnsupportedExpressionsFailBeforeEnteringAStylesheet(String value) {
        assertThatThrownBy(() -> ChartSeries.line("A", Row::value).setColor(value)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void paletteSetterClearsCustomColorsAndSharedRulesAreDeduplicated() {
        var first = ChartSeries.line("A", Row::value).setColor("#123456");
        var second = ChartSeries.line("B", Row::value).setColors("#123456", null);
        tester.startComponentInPage(chart(first).addSeries(second));
        assertThat(ChartAppearance.className(first)).isEqualTo(ChartAppearance.className(second));
        assertThat(ChartAppearance.css(first)).isEqualTo(ChartAppearance.css(second));
        first.setColor(3);
        assertThat(ChartAppearance.className(first)).isEmpty();
        assertThat(first.lightColor).isNull(); assertThat(first.darkColor).isNull();
    }
    @ParameterizedTest @EnumSource(SwChart.Orientation.class)
    void explicitCornerRadiiUseNativePixelMasksWithoutChangingOriginalTableData(SwChart.Orientation orientation) {
        var series = ChartSeries.bar("Value", Row::value).setBarRadius(12, 4, 0, 8);
        tester.startComponentInPage(chart(series)
                .setOrientation(orientation));
        var masks = TagTester.createTags(html(), tag -> tag.getName().equals("mask"), false);
        assertThat(masks).hasSize(4);
        assertThat(masks).extracting(tag -> tag.getAttribute("id")).doesNotHaveDuplicates();
        assertThat(html()).contains("sw-chart-native-bars",
                "<th scope=\"row\">Missing</th><td></td>", "<th scope=\"row\">Zero</th><td>0</td>",
                "<th scope=\"row\">Negative</th><td>-10</td>").doesNotContain("style=", "NaN", "Infinity");
        assertThat(TagTester.createTagsByAttribute(html(), "class", "sw-chart-corner", false)).hasSize(16);
        assertThat(html()).contains("rx=\"12\" ry=\"12\"", "rx=\"0\" ry=\"0\"");
    }
    @Test void invalidGeometryAndTickSettingsFailClearly() {
        assertThatThrownBy(() -> ChartSeries.line("A", Row::value).setBarRadius(2)).hasMessageContaining("bar series");
        for (double value : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, 1001})
            assertThatThrownBy(() -> ChartSeries.bar("A", Row::value).setBarRadius(1, value, 2, 3)).isInstanceOf(IllegalArgumentException.class);
        for (double value : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, 21})
            assertThatThrownBy(() -> ChartSeries.line("A", Row::value).setStrokeWidth(value)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chart(ChartSeries.bar("A", Row::value)).setValueTickCount(1)).hasMessageContaining("tick count");
        assertThatThrownBy(() -> chart(ChartSeries.bar("A", Row::value)).setValueTickCount(21)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chart(ChartSeries.bar("A", Row::value)).setMaxCategoryTicks(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chart(ChartSeries.bar("A", Row::value)).setMinCategoryTickGap(-1)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void naturalSplinePreservesSamplesAndHasContinuousFirstAndSecondDerivatives() {
        double[] points = {2, 14, -3, 8, 4};
        double[][] c = SwChart.naturalControls(points);
        assertThat(6 * (points[0] - 2 * c[0][0] + c[1][0])).isCloseTo(0, within(1e-10));
        int last = points.length - 2;
        assertThat(6 * (points[last + 1] - 2 * c[1][last] + c[0][last])).isCloseTo(0, within(1e-10));
        for (int i = 0; i < last; i++) {
            assertThat(3 * (points[i + 1] - c[1][i])).isCloseTo(3 * (c[0][i + 1] - points[i + 1]), within(1e-10));
            assertThat(6 * (points[i + 1] - 2 * c[1][i] + c[0][i]))
                    .isCloseTo(6 * (points[i + 1] - 2 * c[0][i + 1] + c[1][i + 1]), within(1e-10));
        }
        assertThat(SwChart.naturalControls(new double[]{0, 9})[0]).containsExactly(3);
        assertThat(SwChart.naturalControls(new double[]{0, 9})[1]).containsExactly(6);
    }
    static Stream<Arguments> steps() {
        return Stream.of(Arguments.of(ChartSeries.Curve.STEP, " H750 V0"), Arguments.of(ChartSeries.Curve.STEP_AFTER, " H750 V0"),
                Arguments.of(ChartSeries.Curve.STEP_BEFORE, " V0 H750"), Arguments.of(ChartSeries.Curve.STEP_MIDDLE, " H500 V0 H750"));
    }
    @ParameterizedTest @MethodSource("steps")
    void stepChoicesPlaceTheHandoffWithoutInventingExtraSamples(ChartSeries.Curve curve, String segment) {
        tester.startComponentInPage(new SwChart<>("chart", Model.ofList(List.of(new Row("First", 0.0), new Row("Last", 10.0))), Row::name)
                .addSeries(ChartSeries.line("Value", Row::value).setCurve(curve)));
        assertThat(html()).contains("d=\"M250,240" + segment + "\"");
        assertThat(TagTester.createTagsByAttribute(html(), "class", "sw-chart-hit", false)).hasSize(2);
    }
    @Test void naturalRunsAreClippedToThePlotAndKeepNullGapsAndStrokeWidth() {
        tester.startComponentInPage(chart(ChartSeries.area("Value", Row::value).setCurve(ChartSeries.Curve.NATURAL).setStrokeWidth(3.5)));
        assertThat(html()).contains("-natural-clip)", "stroke-width=\"3.5\"", "<td></td>").doesNotContain("NaN", "Infinity");
        assertThat(TagTester.createTagsByAttribute(html(), "class", "sw-chart-line", false)).hasSize(2);
    }
    @ParameterizedTest @EnumSource(SwChart.Orientation.class)
    void legendPlacementAndTooltipOptionsArePresentationOnly(SwChart.Orientation orientation) {
        tester.startComponentInPage(chart(ChartSeries.bar("Value", Row::value)).setOrientation(orientation)
                .setLegendPosition(SwChart.LegendPosition.TOP).setLegendAlignment(SwChart.LegendAlignment.END)
                .setTooltipIndicator(SwChart.Indicator.DASHED).setShowTooltipIndicator(false).setShowTooltipLabel(false).setShowTooltipCursor(false));
        assertThat(html().indexOf("class=\"sw-chart-legend-content\"")).isLessThan(html().indexOf("class=\"sw-chart-container\""));
        assertThat(html()).contains("data-align=\"end\"", "data-position=\"top\"", "data-tooltip-indicator=\"dashed\"",
                "data-tooltip-cursor=\"false\"", "data-tooltip-label-visible=\"false\"", "First; Value: 20", "<td>20</td>");
    }
    @Test void numericTicksCoverTheDomainWhileCategoryTicksHaveACompleteNativeFallback() {
        assertThat(SwChart.ticks(-40, 90, 3)).containsExactly(-100, 0, 100);
        assertThat(SwChart.ticks(0, Double.MIN_VALUE, 20)).containsExactly(0, Double.MIN_VALUE);
        var data = java.util.stream.IntStream.range(0, 25).mapToObj(i -> new Row("Day " + i, (double) i)).toList();
        tester.startComponentInPage(new SwChart<>("chart", Model.ofList(data), Row::name)
                .addSeries(ChartSeries.bar("Value", Row::value)).setValueTickCount(3).setMaxCategoryTicks(4).setMinCategoryTickGap(12));
        assertThat(html()).contains("data-max-ticks=\"4\"", "data-tick-gap=\"12\"");
        assertThat(TagTester.createTagsByAttribute(html(), "data-tick-index", "0", false)).hasSize(1);
        assertThat(TagTester.createTagsByAttribute(html(), "data-native-hidden", "true", false)).hasSize(21);
        assertThat(html()).contains("Day 24</span>", "<th scope=\"row\">Day 24</th>");
    }
    @Test void appearanceRenderingDoesNotRereadNumericExtractors() {
        AtomicInteger calls = new AtomicInteger();
        tester.startComponentInPage(chart(ChartSeries.<Row>bar("Value", row -> { calls.incrementAndGet(); return row.value; })
                .setColors("#123456", "#abcdef").setBarRadius(8)).setValueTickCount(8).setMaxCategoryTicks(2));
        assertThat(calls.get()).isEqualTo(4);
    }
    @Test void serializedPagesCanChangeColorsAndCornersWithComponentHtmlInTheirAjaxResponse() {
        tester.startPage(AppearancePage.class);
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        var page = serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));
        tester.startPage((WebPage) page); tester.clickLink("refresh");
        assertThat(html()).contains("<component", "sw-chart-native-bars", "#abcdef", "rx=\"0\" ry=\"0\"").doesNotContain("<redirect");
    }
    public static class AppearancePage extends WebPage implements IMarkupResourceStreamProvider {
        public AppearancePage() {
            var series = ChartSeries.bar("Value", Row::value).setColors("#123456", "#654321").setBarRadius(8);
            var chart = new SwChart<>("chart", Model.ofList(List.of(new Row("First", 20.0))), Row::name).addSeries(series);
            add(chart);
            add(new AjaxLink<Void>("refresh") {
                @Override public void onClick(AjaxRequestTarget target) {
                    series.setColor("#abcdef").setBarRadius(8, 0, 0, 8); target.add(chart);
                }
            });
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><body><div wicket:id='chart'></div><a wicket:id='refresh'>Refresh</a></body></html>");
        }
    }
}

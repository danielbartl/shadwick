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

class SwHeatmapChartTest {
    record Row(String name, List<Number> values) implements Serializable {}
    static Row row(String name, Number... values) { return new Row(name, Arrays.asList(values)); }
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() { tester.destroy(); }
    SwHeatmapChart<Row> chart(List<Row> rows, int width) {
        var chart = new SwHeatmapChart<>("chart", Model.ofList(rows), Row::name);
        String[] names = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        for (int i = 0; i < width; i++) {
            int index = i;
            chart.addColumn(HeatmapColumn.of(names[i], row -> row.values.get(index)));
        }
        return chart;
    }
    String html() { return tester.getLastResponseAsString(); }
    List<TagTester> tags(String css) {
        return TagTester.createTags(html(), tag -> { CharSequence value = tag.getAttribute("class");
            return value != null && Arrays.asList(value.toString().split(" ")).contains(css); }, false);
    }
    List<String> levels() {
        return tags("sw-heatmap-fill").stream().limit(tags("sw-heatmap-cell").size())
                .map(tag -> tag.getAttribute("class")).toList();
    }

    @Test void sharedBandsUseExactThresholdsAndKeepBothNativeHeaderAxes() {
        tester.startComponentInPage(chart(List.of(row("Design", 0, 20, 40, 60, 100)), 5).setRange(0, 100));
        assertThat(levels()).containsExactly("sw-heatmap-fill sw-heatmap-level-0", "sw-heatmap-fill sw-heatmap-level-1",
                "sw-heatmap-fill sw-heatmap-level-2", "sw-heatmap-fill sw-heatmap-level-3", "sw-heatmap-fill sw-heatmap-level-4");
        assertThat(tags("sw-heatmap-value")).extracting(TagTester::getValue).containsExactly("0", "20", "40", "60", "100");
        assertThat(tags("sw-heatmap-cell")).hasSize(5);
        assertThat(html()).contains("<th scope=\"row\">Design</th>", "<th scope=\"col\">Monday</th>", "0 – 20", "80 – 100",
                "Design; Friday; Value: 100", "data-columns=\"5\"", "data-rows=\"1\"").doesNotContain("style=", "NaN", "Infinity");
    }

    @Test void automaticRangeIncludesZeroForPositiveNegativeAndMixedMeasurements() {
        tester.startComponentInPage(chart(List.of(row("Positive", 5, 10)), 2));
        assertThat(html()).contains("0 – 2", "8 – 10");
        tester.startComponentInPage(chart(List.of(row("Negative", -10, -5)), 2));
        assertThat(html()).contains("-10 – -8", "-2 – 0");
        tester.startComponentInPage(chart(List.of(row("Mixed", -2, 0, 2)), 3));
        assertThat(levels()).containsExactly("sw-heatmap-fill sw-heatmap-level-0", "sw-heatmap-fill sw-heatmap-level-2", "sw-heatmap-fill sw-heatmap-level-4");
    }

    @Test void explicitRangesNeedNotIncludeTheAutomaticZeroAnchor() {
        tester.startComponentInPage(chart(List.of(row("Positive", 1, 1.5, 2)), 3).setRange(1, 2));
        assertThat(levels()).containsExactly("sw-heatmap-fill sw-heatmap-level-0", "sw-heatmap-fill sw-heatmap-level-2", "sw-heatmap-fill sw-heatmap-level-4");
        tester.startComponentInPage(chart(List.of(row("Negative", -2, -1.5, -1)), 3).setRange(-2, -1));
        assertThat(html()).contains("-2 – -1.8");
    }

    @Test void missingZeroAndDuplicatesRetainMatrixCoordinatesAndOriginalCells() {
        tester.startComponentInPage(chart(List.of(row("Same", 0, null), row("Same", 10, 20)), 2));
        assertThat(tags("sw-heatmap-cell")).hasSize(4);
        assertThat(tags("sw-heatmap-missing")).hasSize(1);
        assertThat(tags("sw-heatmap-value")).extracting(TagTester::getValue).containsExactly("0", "", "10", "20");
        assertThat(tags("sw-heatmap-cell").get(1).getAttribute("data-row")).isEqualTo("0");
        assertThat(tags("sw-heatmap-cell").get(1).getAttribute("data-column")).isEqualTo("1");
        assertThat(tags("sw-heatmap-cell").get(1).getAttribute("aria-label")).isEqualTo("Not measured");
        assertThat(html()).contains("Same; Tuesday; Value: Not measured", "data-tooltip-value=\"Not measured\"");
    }

    @Test void emptyUnknownAndAllZeroMatricesRemainUsefulWithoutScript() {
        tester.startComponentInPage(chart(List.of(), 1));
        assertThat(html()).contains("No data to display").doesNotContain("<table");
        tester.startComponentInPage(chart(null, 1));
        assertThat(html()).contains("No data to display");
        tester.startComponentInPage(chart(List.of(row("Unknown", (Number) null)), 1));
        assertThat(html()).contains("No measurements to plot", "<table", "Unknown", "data-tooltip-value=\"Not measured\"");
        tester.startComponentInPage(chart(List.of(row("Zero", 0)), 1));
        assertThat(html()).contains("0 – 0.2", "0.8 – 1").doesNotContain("No measurements");
        assertThat(tags("sw-heatmap-missing")).isEmpty();
        assertThat(tags("sw-heatmap-cell")).hasSize(1);
    }

    @Test void decimalThresholdsStayExactBelowDoubleRangeAndPastDoublePrecision() {
        tester.startComponentInPage(chart(List.of(row("Tiny", new BigDecimal("1e-1000"), new BigDecimal("4e-1001"))), 2)
                .setValueFormat((n, locale) -> n.toString()));
        assertThat(levels()).containsExactly("sw-heatmap-fill sw-heatmap-level-4", "sw-heatmap-fill sw-heatmap-level-2");
        assertThat(html()).contains("1E-1000", "4E-1001").doesNotContain("NaN", "Infinity");
        tester.startComponentInPage(chart(List.of(row("Precise", new BigDecimal("0.1999999999999999999999999999999999999999"), new BigDecimal("0.2"))), 2)
                .setRange(0, 1));
        assertThat(levels()).containsExactly("sw-heatmap-fill sw-heatmap-level-0", "sw-heatmap-fill sw-heatmap-level-1");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(row("Above", new BigDecimal("1.00000000000000000001"))), 1).setRange(0, 1)))
                .hasStackTraceContaining("within the configured range");
    }

    @Test void hiddenPresentationKeepsTheExactTableAndNativeScrollFocus() {
        tester.startComponentInPage(chart(List.of(row("A", 10, 20)), 2).setColor(5).setShowValues(false).setShowLegend(false).setShowTooltip(false));
        assertThat(html()).contains("data-values=\"false\"", "sw-chart-series-5", "data-tooltip=\"false\"", "tabindex=\"0\"")
                .doesNotContain("sw-heatmap-legend", "Use arrow keys");
        assertThat(tags("sw-heatmap-value")).extracting(TagTester::getValue).containsExactly("10", "20");
        assertThat(tags("sw-heatmap-plot").get(0).getAttribute("aria-describedby")).doesNotContain("-scale");
    }

    @Test void formattersReceiveOriginalNumbersAndTextIsEscapedInAllContexts() {
        tester.startComponentInPage(chart(List.of(row("<img src=x>", new BigDecimal("0.123456789123456789"))), 1)
                .setTitle("<Title>").setRowHeader("<Row>").setValueHeader("<Value>").setRange(0, 1)
                .setValueFormat((n, locale) -> "<" + n + ">"));
        assertThat(html()).contains("&lt;img", "&lt;Title&gt;", "&lt;Row&gt;", "&lt;Value&gt;", "&lt;0.123456789123456789&gt;")
                .doesNotContain("<img", "<Title>");
        tester.startComponentInPage(chart(List.of(row("Known", 10)), 1).setValueFormat((n, locale) -> null));
        assertThat(tags("sw-heatmap-missing")).isEmpty();
        assertThat(tags("sw-heatmap-value")).extracting(TagTester::getValue).containsExactly("");
    }

    @ParameterizedTest @ValueSource(doubles={Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1e101, -1e101})
    void invalidMeasurementsFailClearly(double value) {
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(row("Bad", value)), 1)))
                .hasStackTraceContaining("Heatmap values must be finite with magnitude at most 1e100");
    }

    @Test void invalidConfiguredBoundsAndOutOfRangeValuesFailRatherThanClamping() {
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(row("A", 10)), 1).setRange(0, 0)))
                .hasStackTraceContaining("defined and increasing");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(row("A", 10)), 1).setRange(2, 1)))
                .hasStackTraceContaining("defined and increasing");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(row("A", 10)), 1)
                .setRange(Model.<Number>of((Number) null), Model.of(100)))).hasStackTraceContaining("defined and increasing");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(row("A", 10)), 1).setRange(0, Double.NaN)))
                .hasStackTraceContaining("must be finite");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(row("A", -1)), 1).setRange(0, 10)))
                .hasStackTraceContaining("within the configured range");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(row("A", new BigDecimal("1.000000000000000000001e100"))), 1)))
                .hasStackTraceContaining("magnitude at most 1e100");
    }

    @Test void registrationAndNonblankLabelsHaveClearValidation() {
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(), 0))).hasStackTraceContaining("at least one column");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(), 1).setTitle(" "))).hasStackTraceContaining("needs a title");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(), 1).setRowHeader(" "))).hasStackTraceContaining("row header");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(), 1).setValueHeader(" "))).hasStackTraceContaining("value header");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(row(" ", 10)), 1))).hasStackTraceContaining("row needs a label");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(), 1).addColumn(HeatmapColumn.of(" ", row -> 1))))
                .hasStackTraceContaining("column needs a label");
        var column = HeatmapColumn.of("A", (Row r) -> r.values.get(0));
        assertThatIllegalArgumentException().isThrownBy(() -> chart(List.of(), 1).addColumn(column).addColumn(column));
        assertThatIllegalArgumentException().isThrownBy(() -> chart(List.of(), 1).setColor(0));
        assertThatIllegalArgumentException().isThrownBy(() -> chart(List.of(), 1).setColor(6));
    }

    @ParameterizedTest @CsvSource({"de,Zeile,Skala,Nicht gemessen", "fr,Ligne,Échelle,Non mesuré", "ja,行,目盛,未測定"})
    void builtInLabelsMissingStatesAndCurrencyFollowTheLocale(String language, String rowLabel, String scale, String missing) {
        tester.getSession().setLocale(Locale.forLanguageTag(language));
        tester.startComponentInPage(chart(List.of(row("A", 20, null)), 2).setCurrency(Currency.getInstance("EUR")));
        assertThat(html()).contains("<th scope=\"col\">" + rowLabel + "</th>", missing, scale,
                Currency.getInstance("EUR").getSymbol(tester.getSession().getLocale()));
    }

    @Test void replacedBoundsDetachOnlyWhenReleasedAndAutoScalingCanBeRestored() {
        AtomicInteger detaches = new AtomicInteger();
        IModel<Number> first = countedBound(0, detaches), second = countedBound(100, detaches), third = countedBound(200, detaches);
        var chart = chart(List.of(row("A", 10)), 1).setRange(first, second);
        chart.setRange(first, third);
        assertThat(detaches.get()).isEqualTo(1);
        chart.setRange(third, first);
        assertThat(detaches.get()).isEqualTo(1);
        chart.setAutoScale();
        assertThat(detaches.get()).isEqualTo(3);
        tester.startComponentInPage(chart);
        assertThat(html()).contains("8 – 10");
        assertThat(detaches.get()).isEqualTo(3);
    }
    static IModel<Number> countedBound(Number value, AtomicInteger detaches) {
        return new IModel<>() {
            public Number getObject() { return value; }
            public void detach() { detaches.incrementAndGet(); }
        };
    }

    @Test void snapshotsReadDataAndExtractorsOnceAndDetachAllOwnedModels() {
        AtomicInteger loads = new AtomicInteger(), names = new AtomicInteger(), measures = new AtomicInteger(), labels = new AtomicInteger(), detaches = new AtomicInteger();
        IModel<List<Row>> data = new IModel<>() {
            public List<Row> getObject() { loads.incrementAndGet(); return List.of(row("A", 10, 20), row("B", 30, 40)); }
            public void detach() { detaches.incrementAndGet(); }
        };
        IModel<String> text = new IModel<>() {
            public String getObject() { labels.incrementAndGet(); return "Caption"; }
            public void detach() { detaches.incrementAndGet(); }
        };
        var chart = new SwHeatmapChart<>("chart", data, row -> { names.incrementAndGet(); return row.name; })
                .setTitle(text).setRowHeader(text).setValueHeader(text)
                .setRange(countedBound(0, detaches), countedBound(100, detaches))
                .addColumn(HeatmapColumn.of(text, row -> { measures.incrementAndGet(); return row.values.get(0); }))
                .addColumn(HeatmapColumn.of(text, row -> { measures.incrementAndGet(); return row.values.get(1); }));
        assertThat(chart.get("figure")).isNull();
        tester.startComponentInPage(chart);
        assertThat(chart.get("figure")).isNotNull();
        assertThat(loads.get()).isEqualTo(1);
        assertThat(names.get()).isEqualTo(2);
        assertThat(measures.get()).isEqualTo(4);
        assertThat(labels.get()).isEqualTo(5);
        assertThat(detaches.get()).isEqualTo(8);
    }

    @Test void serializationAndAjaxUpdateRowsColumnsLabelsPaletteAndBounds() {
        tester.startPage(HeatmapPage.class);
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        tester.startPage((WebPage) serializer.deserialize(serializer.serialize(tester.getLastRenderedPage())));
        tester.clickLink("refresh");
        assertThat(html()).contains("<component", "Latest", "New day", "sw-chart-series-5", "data-values=\"false\"", "96 – 120",
                "data-rows=\"2\"", "data-columns=\"3\"").doesNotContain("<redirect");
        assertThat(tags("sw-heatmap-cell")).hasSize(6);
    }
    public static class HeatmapPage extends WebPage implements IMarkupResourceStreamProvider {
        @Override protected void onInitialize() {
            super.onInitialize();
            IModel<List<Row>> data = Model.ofList(List.of(row("A", 10, 20, 30), row("B", 40, 50, 60), row("C", 70, 80, 90)));
            IModel<Integer> lower = Model.of(0), upper = Model.of(100);
            IModel<String> heading = Model.of("Current");
            var chart = new SwHeatmapChart<>("chart", data, Row::name).setRange(lower, upper)
                    .addColumn(HeatmapColumn.of(heading, r -> r.values.get(0)))
                    .addColumn(HeatmapColumn.of("Second", r -> r.values.get(1)));
            add(chart);
            add(SwButton.onClick("refresh", "Refresh", target -> {
                data.setObject(List.of(row("New team", 90, 100, 110), row("Other team", 80, 90, 100)));
                heading.setObject("Latest");
                upper.setObject(120);
                chart.addColumn(HeatmapColumn.of("New day", r -> r.values.get(2))).setColor(5).setShowValues(false);
                target.add(chart);
            }));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><body><div wicket:id='chart'></div><a wicket:id='refresh'>Refresh</a></body></html>");
        }
    }
}

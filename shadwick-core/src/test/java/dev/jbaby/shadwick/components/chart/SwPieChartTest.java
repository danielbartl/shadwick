package dev.jbaby.shadwick.components.chart;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import dev.jbaby.shadwick.components.SwButton;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.*;
import org.apache.wicket.util.tester.*;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class SwPieChartTest {
    record Row(String name, Number value) implements Serializable {}
    WicketTester tester;

    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() { tester.destroy(); }

    SwDonutChart<Row> pie(List<Row> rows) {
        return new SwDonutChart<>("chart", Model.ofList(rows), Row::name, Row::value)
                .setShape(SwDonutChart.Shape.PIE).setTitle("Revenue shares");
    }
    String html() { return tester.getLastResponseAsString(); }
    List<TagTester> slices() {
        return TagTester.createTags(html(), tag -> tag.getName().equals("path"), false);
    }

    @Test void filledSectorsMatchSharesAndRetainTheExactTotalOutsideThePlot() {
        tester.startComponentInPage(pie(List.of(new Row("North", 75), new Row("South", 25))));
        assertThat(slices()).hasSize(2);
        assertThat(slices().get(0).getAttribute("d")).startsWith("M120 120 L120.0 8.0 A112 112 0 1 1");
        assertThat(slices().get(1).getAttribute("d")).contains(" A112 112 0 0 1 ");
        assertThat(html()).contains("data-shape=\"pie\"", "75%", "25%", "<tfoot>", "<td>100</td>", "scope=\"row\"")
                .doesNotContain("sw-donut-center", "sw-donut-track", "stroke-dasharray", "style=", "NaN", "Infinity");
    }

    @Test void onePositiveValueDrawsAWholeDiskWithTwoArcs() {
        tester.startComponentInPage(pie(List.of(new Row("Only region", 1), new Row("Zero", 0), new Row("Missing", null))));
        assertThat(slices()).hasSize(1);
        assertThat(slices().get(0).getAttribute("d"))
                .isEqualTo("M120 8 A112 112 0 1 1 120 232 A112 112 0 1 1 120 8 Z");
        assertThat(html()).contains("100%", "<th scope=\"row\">Missing</th><td></td><td></td>");
    }

    @Test void zeroAndMissingDataKeepAnEmptyDiskAndTheTableWithoutInventingSlices() {
        tester.startComponentInPage(pie(List.of(new Row("Zero", 0), new Row("Missing", null))));
        assertThat(slices()).isEmpty();
        assertThat(html()).contains("sw-pie-track", "No positive values", "<tfoot>", "<td>0%</td>")
                .doesNotContain("sw-donut-center", "NaN", "Infinity");
    }

    @Test void anEmptyModelUsesTheExistingNativeEmptyState() {
        tester.startComponentInPage(pie(List.of()));
        assertThat(html()).contains("No data to display.").doesNotContain("sw-pie-track", "<svg");
    }

    @Test void groupingPreservesOriginalRowsAndExactDecimalTotals() {
        tester.startComponentInPage(pie(List.of(new Row("A", new BigDecimal("0.10")),
                new Row("B", new BigDecimal("0.20")), new Row("C", new BigDecimal("0.30"))))
                .setMaxSegments(2).setCurrency(Currency.getInstance("EUR")));
        assertThat(slices()).hasSize(2);
        assertThat(html()).contains("Other", "€0.60", "<th scope=\"row\">A</th>", "<th scope=\"row\">B</th>");
    }

    @Test void extremeSharesDoNotCollapseTheDominantDiskOrInventARadialLine() {
        tester.startComponentInPage(pie(List.of(new Row("Large", new BigDecimal("1e100")),
                new Row("Tiny", new BigDecimal("1e-1000")))).setGroupSmallSegments(false));
        assertThat(slices()).hasSize(2);
        assertThat(slices().get(0).getAttribute("d")).startsWith("M120 8 A112");
        assertThat(slices().get(1).getAttribute("d")).isEqualTo("M120 120");
        assertThat(html()).contains("Tiny").doesNotContain("NaN", "Infinity");
    }

    @Test void pieLabelsAndFormattedValuesStayEscapedWithoutEnhancedTooltips() {
        tester.startComponentInPage(pie(List.of(new Row("<img src=x>", 10)))
                .setValueFormat((n, locale) -> "<value>").setShowTooltip(false).setShowLegend(false));
        assertThat(html()).contains("&lt;img", "&lt;value&gt;", "data-tooltip=\"false\"", " hidden")
                .doesNotContain("<img", "Use the arrow keys");
    }

    @Test void configurationRemainsAvailableBeforeLifecycleAssemblyAndTheDataModelDetaches() {
        AtomicInteger reads = new AtomicInteger(), detaches = new AtomicInteger();
        IModel<List<Row>> data = new IModel<>() {
            public List<Row> getObject() { reads.incrementAndGet(); return List.of(new Row("A", 1)); }
            public void detach() { detaches.incrementAndGet(); }
        };
        var chart = new SwDonutChart<>("chart", data, Row::name, Row::value);
        assertThat(chart.get("figure")).isNull();
        chart.setShape(SwDonutChart.Shape.PIE).setShape(SwDonutChart.Shape.DONUT).setShape(SwDonutChart.Shape.PIE);
        assertThatNullPointerException().isThrownBy(() -> chart.setShape(null));
        tester.startComponentInPage(chart);
        assertThat(chart.get("figure")).isNotNull();
        assertThat(reads.get()).isEqualTo(1);
        assertThat(detaches.get()).isPositive();
        assertThat(html()).contains("data-shape=\"pie\"");
    }

    @Test void aSerializedPageCanSwitchShapeAndReplaceDataThroughAjax() {
        tester.startPage(PiePage.class);
        assertThat(html()).contains("data-shape=\"donut\"");
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        var restored = (WebPage) serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));
        tester.startPage(restored);
        tester.clickLink("refresh");
        assertThat(html()).contains("<component", "data-shape=\"pie\"", "Updated", "100%")
                .doesNotContain("<redirect", "sw-donut-center");
    }

    public static class PiePage extends WebPage implements IMarkupResourceStreamProvider {
        @Override protected void onInitialize() {
            super.onInitialize();
            IModel<List<Row>> data = Model.ofList(List.of(new Row("Original", 10)));
            var chart = new SwDonutChart<>("chart", data, Row::name, Row::value);
            add(chart);
            add(SwButton.onClick("refresh", "Refresh", target -> {
                data.setObject(List.of(new Row("Updated", 20)));
                chart.setShape(SwDonutChart.Shape.PIE);
                target.add(chart);
            }));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><body><div wicket:id='chart'></div><a wicket:id='refresh'>Refresh</a></body></html>");
        }
    }
}

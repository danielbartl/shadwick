package dev.jbaby.shadwick.components.chart;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.model.*;
import org.apache.wicket.util.tester.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.assertj.core.api.Assertions.*;

class SwSparklineTest {
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(); }
    @AfterEach void stop() { tester.destroy(); }
    void render(List<? extends Number> values) { render(new SwSparkline("trend", Model.ofList(values))); }
    void render(SwSparkline spark) { tester.startComponentInPage(spark, Markup.of("<span wicket:id='trend'></span>")); }
    String html() { return tester.getLastResponseAsString(); }
    List<TagTester> tags(String css) { return TagTester.createTagsByAttribute(html(), "class", css, false); }
    @Test void nativeTrendIsDecorativeAndUsesTheLastKnownPoint() {
        render(Arrays.asList(3, 5, 4, 8, null));
        assertThat(html()).contains("aria-hidden=\"true\"", "sw-sparkline", "focusable=\"false\"").doesNotContain("style=", "tabindex");
        assertThat(tags("sw-sparkline-line")).hasSize(1);
        assertThat(tags("sw-sparkline-last").get(0).getAttribute("cx")).isEqualTo("75.0%");
        assertThat(tags("sw-sparkline-last").get(0).getAttribute("cy")).isEqualTo("3.0");
    }
    @Test void nullsSplitBothTheLineAndTheFilledAreaWithoutShiftingPositions() {
        render(new SwSparkline("trend", Model.ofList(Arrays.asList(1, 2, null, 3, 4))).setFilled(true));
        assertThat(tags("sw-sparkline-line")).hasSize(2); assertThat(tags("sw-sparkline-area")).hasSize(2);
        assertThat(tags("sw-sparkline-line").get(0).getAttribute("points")).startsWith("0,29.0 ");
        assertThat(tags("sw-sparkline-line").get(1).getAttribute("points")).startsWith("75.0,");
    }
    @Test void isolatedValuesAndFlatSingleNegativeAndTinyRangesHaveFiniteGeometry() {
        render(Arrays.asList(1, null, 3));
        assertThat(tags("sw-sparkline-line")).isEmpty(); assertThat(tags("sw-sparkline-point")).hasSize(1); assertThat(tags("sw-sparkline-last")).hasSize(1);
        render(List.of(-4, -4));
        assertThat(tags("sw-sparkline-last").get(0).getAttribute("cy")).isEqualTo("16.0");
        render(List.of(8));
        assertThat(tags("sw-sparkline-last").get(0).getAttribute("cx")).isEqualTo("50.0%");
        render(List.of(-Double.MIN_VALUE, 0.0, Double.MIN_VALUE));
        assertThat(html()).doesNotContain("NaN", "Infinity");
        assertThat(tags("sw-sparkline-last").get(0).getAttribute("cy")).isEqualTo("3.0");
    }
    @Test void emptyAndAllMissingModelsHaveNoInventedTrend() {
        render(List.of()); assertThat(html()).doesNotContain("<svg");
        render(Arrays.asList(null, null)); assertThat(html()).doesNotContain("<svg");
        render(new SwSparkline("trend", Model.ofList(null))); assertThat(html()).doesNotContain("<svg");
    }
    @Test void accentAndFillCanBeConfiguredAndHiddenEndpointDoesNotProduceAnAccent() {
        render(new SwSparkline("trend", Model.ofList(List.of(2, 3))).setColor(4).setFilled(true).setShowLastPoint(false));
        assertThat(html()).contains("sw-chart-series-4", "sw-sparkline-area").doesNotContain("sw-sparkline-last");
        assertThatIllegalArgumentException().isThrownBy(() -> new SwSparkline("trend", Model.ofList(List.of())).setColor(0));
        assertThatIllegalArgumentException().isThrownBy(() -> new SwSparkline("trend", Model.ofList(List.of())).setColor(6));
    }
    @ParameterizedTest @ValueSource(doubles={Double.NaN, Double.POSITIVE_INFINITY, -1e101, 1e101})
    void invalidValuesFailClearly(double number) {
        assertThatThrownBy(() -> render(List.of(1, number))).hasStackTraceContaining("finite, with magnitude at most 1e100");
    }
    static class CountedNumber extends Number {
        final AtomicInteger reads;
        CountedNumber(AtomicInteger reads) { this.reads = reads; }
        public int intValue() { return 1; } public long longValue() { return 1; }
        public float floatValue() { return 1; } public double doubleValue() { reads.incrementAndGet(); return 1; }
    }
    @Test void modelAndNumbersAreReadOnceAndDefaultModelDetaches() {
        AtomicInteger loads = new AtomicInteger(), reads = new AtomicInteger(), detaches = new AtomicInteger();
        IModel<List<Number>> values = new IModel<>() {
            public List<Number> getObject() { loads.incrementAndGet(); return List.of(new CountedNumber(reads), new CountedNumber(reads)); }
            public void detach() { detaches.incrementAndGet(); }
        };
        render(new SwSparkline("trend", values));
        assertThat(loads.get()).isEqualTo(1); assertThat(reads.get()).isEqualTo(2); assertThat(detaches.get()).isPositive();
    }
}

package dev.jbaby.shadwick.components.chart;

import java.io.Serializable;
import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.wicket.model.*;
import org.apache.wicket.util.tester.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.assertj.core.api.Assertions.*;

class SwDonutChartTest {
    record Row(String name, Number value) implements Serializable {}
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() { tester.destroy(); }
    SwDonutChart<Row> chart(List<Row> rows) { return new SwDonutChart<>("chart", Model.ofList(rows), Row::name, Row::value); }
    String html() { return tester.getLastResponseAsString(); }
    List<TagTester> tags(String css) { return TagTester.createTagsByAttribute(html(), "class", css, false); }
    List<TagTester> segments() {
        return TagTester.createTagsByAttribute(html(), "cx", "120", false).stream()
                .filter(tag -> tag.getAttribute("class").startsWith("sw-donut-segment")).toList();
    }
    @Test void nativeGeometrySharesAndTheExactTotalAgree() {
        tester.startComponentInPage(chart(List.of(new Row("North", 75), new Row("South", 25))).setTitle("Regions"));
        assertThat(segments()).hasSize(2);
        assertThat(tags("sw-donut-share")).extracting(TagTester::getValue).containsExactly("75%", "25%");
        assertThat(tags("sw-donut-total")).extracting(TagTester::getValue).containsExactly("100");
        // Each dash fits its actual share, leaving only its separator gap; offsets keep the two parts disjoint.
        double first = Double.parseDouble(segments().get(0).getAttribute("stroke-dasharray").split(" ")[0]);
        double second = Double.parseDouble(segments().get(1).getAttribute("stroke-dasharray").split(" ")[0]);
        assertThat(first).isCloseTo(74.4, within(.000001)); assertThat(second).isCloseTo(24.4, within(.000001));
        assertThat(Double.parseDouble(segments().get(1).getAttribute("stroke-dashoffset"))).isCloseTo(-75.3, within(.000001));
        assertThat(html()).contains("<tfoot>", "scope=\"row\"", "View data", "tabindex=\"0\"", "role=\"region\"")
                .doesNotContain("style=", "NaN", "Infinity");
    }
    @Test void groupingKeepsLargestPartsInInputOrderAndPreservesEveryOriginalRow() {
        tester.startComponentInPage(chart(List.of(new Row("Small", 1), new Row("North", 40), new Row("South", 25),
                new Row("East", 15), new Row("West", 10), new Row("Island", 4), new Row("Overseas", 5))));
        assertThat(tags("sw-donut-name")).extracting(TagTester::getValue).containsExactly("North", "South", "East", "West", "Other");
        assertThat(tags("sw-donut-value")).extracting(TagTester::getValue).containsExactly("40", "25", "15", "10", "10");
        assertThat(html()).contains("<th scope=\"row\">Small</th>", "<th scope=\"row\">Island</th>", "<th scope=\"row\">Overseas</th>");
        assertThat(segments()).hasSize(5);
    }
    @Test void equalRowsEachContributeOnceAndStableTiesChooseTheFirst() {
        tester.startComponentInPage(chart(List.of(new Row("Large", 50), new Row("Same", 10), new Row("Same", 10),
                new Row("Same", 10), new Row("Small", 5))).setMaxSegments(3));
        assertThat(tags("sw-donut-name")).extracting(TagTester::getValue).containsExactly("Large", "Same", "Other");
        assertThat(tags("sw-donut-value")).extracting(TagTester::getValue).containsExactly("50", "10", "25");
        assertThat(tags("sw-donut-total")).extracting(TagTester::getValue).containsExactly("85");
    }
    @Test void groupingCanBeDisabledAndTheSinglePartIsAFullRing() {
        tester.startComponentInPage(chart(List.of(new Row("A", 5), new Row("B", 4), new Row("C", 3)))
                .setMaxSegments(2).setGroupSmallSegments(false));
        assertThat(segments()).hasSize(3); assertThat(tags("sw-donut-name")).extracting(TagTester::getValue).doesNotContain("Other");
        tester.startComponentInPage(chart(List.of(new Row("Only", 9))));
        assertThat(segments().get(0).getAttribute("stroke-dasharray")).isEqualTo("100.0 0");
        assertThat(tags("sw-donut-share")).extracting(TagTester::getValue).containsExactly("100%");
    }
    @Test void missingAndZeroValuesStayDistinctWithoutCreatingPhantomSlices() {
        tester.startComponentInPage(chart(List.of(new Row("Missing", null), new Row("Zero", 0), new Row("Actual", 2))));
        assertThat(segments()).hasSize(1);
        assertThat(html()).contains("<th scope=\"row\">Missing</th><td></td><td></td>",
                "<th scope=\"row\">Zero</th><td>0</td><td>0%</td>");
        tester.startComponentInPage(chart(List.of(new Row("Missing", null), new Row("Zero", 0))));
        assertThat(segments()).isEmpty(); assertThat(html()).contains("No positive values", "sw-donut-track").doesNotContain("NaN", "Infinity");
        tester.startComponentInPage(chart(List.of()));
        assertThat(html()).contains("No data to display.").doesNotContain("<svg");
    }
    @Test void decimalAndLargeIntegerTotalsPreserveTheirInputPrecision() {
        tester.startComponentInPage(chart(List.of(new Row("A", new BigDecimal("0.10")), new Row("B", new BigDecimal("0.20"))))
                .setCurrency(Currency.getInstance("EUR")));
        assertThat(tags("sw-donut-total")).extracting(TagTester::getValue).containsExactly("€0.30");
        tester.startComponentInPage(chart(List.of(new Row("A", 9007199254740993L), new Row("B", 2L))));
        assertThat(tags("sw-donut-total")).extracting(TagTester::getValue).containsExactly("9,007,199,254,740,995");
    }
    @Test void verySmallPositiveNumbersStillFormCorrectShares() {
        tester.startComponentInPage(chart(List.of(new Row("A", new BigDecimal("1e-1000")), new Row("B", new BigDecimal("1e-1000")))));
        assertThat(tags("sw-donut-share")).extracting(TagTester::getValue).containsExactly("50%", "50%");
        assertThat(segments()).hasSize(2); assertThat(html()).doesNotContain("NaN", "Infinity");
    }
    @ParameterizedTest @ValueSource(doubles={-1, Double.NaN, Double.POSITIVE_INFINITY, 1e101})
    void invalidAmountsFailInsteadOfChangingTheData(double number) {
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(new Row("Bad", number)))))
                .hasStackTraceContaining("finite, nonnegative and at most 1e100");
    }
    @Test void negativeDecimalsCannotHideBehindFloatingPointUnderflow() {
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(new Row("Bad", new BigDecimal("-1e-1000"))))))
                .hasStackTraceContaining("nonnegative");
        assertThatIllegalArgumentException().isThrownBy(() -> chart(List.of()).setMaxSegments(1));
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of()).setTitle(" "))).hasStackTraceContaining("accessible name");
    }
    @Test void labelsAndCustomNumbersAreEscapedEverywhere() {
        tester.startComponentInPage(chart(List.of(new Row("<img src=x onerror=alert(1)>", 1)))
                .setTitle("<Title>").setTotalLabel("<Total>").setValueHeader("<Value>").setValueFormat((n,l) -> "<Number>"));
        assertThat(html()).contains("&lt;img", "&lt;Title&gt;", "&lt;Total&gt;", "&lt;Value&gt;", "&lt;Number&gt;").doesNotContain("<img", "style=");
    }
    @ParameterizedTest @CsvSource({"de,Sonstige,Gesamt,Anteil", "hr,Ostalo,Ukupno,Udio", "ja,その他,合計,割合"})
    void groupingHeadersAndValuesFollowTheLocale(String language, String other, String total, String share) {
        tester.getSession().setLocale(Locale.forLanguageTag(language));
        tester.startComponentInPage(chart(List.of(new Row("A", 1), new Row("B", 2), new Row("C", 3)))
                .setMaxSegments(2).setCurrency(Currency.getInstance("EUR")));
        assertThat(html()).contains(other, total, share, "€");
        if (language.equals("de")) assertThat(tags("sw-donut-total").get(0).getValue()).contains("6,00");
    }
    @Test void valuesAreSnapshottedOnceAndAllConfiguredModelsDetach() {
        AtomicInteger reads = new AtomicInteger(), loads = new AtomicInteger(), detaches = new AtomicInteger();
        IModel<List<Row>> data = new IModel<>() {
            public List<Row> getObject() { loads.incrementAndGet(); return List.of(new Row("A", 1), new Row("B", 2)); }
            public void detach() { detaches.incrementAndGet(); }
        };
        IModel<String> text = new IModel<>() {
            public String getObject() { return "Custom"; }
            public void detach() { detaches.incrementAndGet(); }
        };
        tester.startComponentInPage(new SwDonutChart<>("chart", data, Row::name, row -> { reads.incrementAndGet(); return row.value(); })
                .setTitle(text).setCategoryHeader(text).setValueHeader(text).setTotalLabel(text).setShowLegend(false).setShowTooltip(false));
        assertThat(loads.get()).isEqualTo(1); assertThat(reads.get()).isEqualTo(2); assertThat(detaches.get()).isGreaterThanOrEqualTo(5);
        assertThat(html()).contains("data-tooltip=\"false\"").doesNotContain("Use the arrow keys");
    }
}

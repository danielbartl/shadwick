package dev.jbaby.shadwick.components.chart;

import dev.jbaby.shadwick.util.SerializableBiFunction;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
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

class SwChartFormatTest {
    record Sale(LocalDate day, BigDecimal revenue, String report) implements Serializable {}
    static List<Sale> sales() {
        return List.of(new Sale(LocalDate.of(2026, 1, 1), new BigDecimal("1234.56789"), "First report"),
                new Sale(LocalDate.of(2026, 2, 1), null, "Missing report"),
                new Sale(LocalDate.of(2026, 3, 1), new BigDecimal("-2000.50"), "Correction"),
                new Sale(LocalDate.of(2026, 4, 1), BigDecimal.ZERO, "No sales"));
    }
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() { tester.destroy(); }
    String html() { return tester.getLastResponseAsString(); }
    List<TagTester> tags(String css) { return TagTester.createTagsByAttribute(html(), "class", css, false); }
    List<TagTester> cells(String name) { return TagTester.createTags(html(), tag -> tag.getName().equals(name), false); }
    SwChart<Sale> chart() {
        return new SwChart<>("chart", Model.ofList(sales()), Sale::day)
                .addSeries(ChartSeries.bar("Revenue", Sale::revenue).setValueLabels(ChartSeries.ValueLabels.ALL));
    }
    @ParameterizedTest @EnumSource(SwChart.Orientation.class)
    void axesAndTooltipsCanDifferWhileTableAndValueLabelsKeepOriginalPrecision(SwChart.Orientation orientation) {
        tester.startComponentInPage(chart().setOrientation(orientation).setValueFormat((value, locale) -> value.toString())
                .setCategoryFormat(SwChart.CategoryFormat.DATE).setAxisCategoryFormat(SwChart.CategoryFormat.MONTH_SHORT)
                .setAxisValueFormat(SwChart.ValueFormat.COMPACT).setTooltipLabelFormat(SwChart.CategoryFormat.MONTH_YEAR)
                .setTooltipCurrency(Currency.getInstance("EUR")));
        assertThat(tags(orientation == SwChart.Orientation.VERTICAL ? "sw-chart-y-axis" : "sw-chart-value-axis").get(0).getValue())
                .contains("2K", "-3K").doesNotContain("€", "1234.56789");
        var categoryText = tags(orientation == SwChart.Orientation.VERTICAL ? "sw-chart-x-axis" : "sw-chart-category");
        assertThat(categoryText.get(0).getValue()).contains("Jan").doesNotContain("2026");
        assertThat(cells("td").get(0).getValue()).isEqualTo("1234.56789");
        assertThat(cells("td").get(0).getAttribute("data-tooltip-value")).isEqualTo("€1,234.57");
        assertThat(cells("td").get(1).getValue()).isEmpty();
        assertThat(cells("td").get(1).getAttribute("data-tooltip-value")).isNull();
        assertThat(cells("td").get(2).getAttribute("data-tooltip-value")).isEqualTo("-€2,000.50");
        assertThat(cells("td").get(3).getAttribute("data-tooltip-value")).isEqualTo("€0.00");
        assertThat(html()).contains(
                "data-tooltip-label=\"Jan 2026\">Jan 1, 2026</th>", "Jan 2026; Revenue: €1,234.57",
                "Feb 2026</title>", ">1234.56789</text>").doesNotContain("NaN", "style=");
    }
    @Test void currencyOverridesRemainIndependentOfSharedFormatAndSetterOrder() {
        tester.startComponentInPage(chart().setAxisCurrency(Currency.getInstance("USD"))
                .setTooltipCurrency(Currency.getInstance("JPY")).setCurrency(Currency.getInstance("EUR")));
        assertThat(tags("sw-chart-y-axis").get(0).getValue()).contains("$2,000.00");
        assertThat(cells("td").get(0).getValue()).isEqualTo("€1,234.57");
        assertThat(cells("td").get(0).getAttribute("data-tooltip-value")).isEqualTo("¥1,235");
        tester.startComponentInPage(chart().setAxisValueFormat(SwChart.ValueFormat.CURRENCY)
                .setTooltipValueFormat(SwChart.ValueFormat.CURRENCY).setCurrency(Currency.getInstance("EUR")));
        assertThat(tags("sw-chart-y-axis").get(0).getValue()).contains("€2,000.00");
        assertThat(html()).doesNotContain("data-tooltip-value");
    }
    @Test void namedOverridesReplaceCustomCallbacksAndCustomOverridesReplaceNamedFormats() {
        tester.startComponentInPage(chart().setAxisValueFormat((value, locale) -> "unused")
                .setAxisValueFormat(SwChart.ValueFormat.COMPACT).setTooltipValueFormat(SwChart.ValueFormat.PERCENT)
                .setTooltipValueFormat((value, locale) -> "exact " + value)
                .setAxisCategoryFormat((value, locale) -> "unused").setAxisCategoryFormat(SwChart.CategoryFormat.MONTH)
                .setTooltipLabelFormat(SwChart.CategoryFormat.MONTH).setTooltipLabelFormat((value, locale) -> "Report " + value));
        assertThat(html()).contains("January", "Report 2026-01-01", "exact 1234.56789", "2K").doesNotContain("unused");
    }
    @Test void namedPercentAndIntegerValuesDoNotChangeTableNumbers() {
        tester.startComponentInPage(new SwChart<>("chart", Model.ofList(List.of(.256)), value -> "Rate")
                .addSeries(ChartSeries.bar("Share", value -> value)).setAxisValueFormat(SwChart.ValueFormat.INTEGER)
                .setTooltipValueFormat(SwChart.ValueFormat.PERCENT));
        assertThat(cells("td").get(0).getValue()).isEqualTo("0.26");
        assertThat(cells("td").get(0).getAttribute("data-tooltip-value")).isEqualTo("25.6%");
        assertThat(tags("sw-chart-y-axis").get(0).getValue()).doesNotContain("%", "0.3");
    }
    @Test void callbacksReceiveOriginalValuesAndLocaleAndExtractorsAreReadOnce() {
        tester.getSession().setLocale(Locale.GERMANY);
        AtomicInteger categories = new AtomicInteger(), values = new AtomicInteger(), labels = new AtomicInteger(), formatted = new AtomicInteger();
        tester.startComponentInPage(new SwChart<>("chart", Model.ofList(sales()), row -> { categories.incrementAndGet(); return row.day; })
                .addSeries(ChartSeries.bar("Revenue", row -> { values.incrementAndGet(); return row.revenue; }))
                .setTooltipLabel(row -> { labels.incrementAndGet(); return row.report; })
                .setTooltipLabelFormat((value, locale) -> { assertThat(locale).isEqualTo(Locale.GERMANY); return "<" + value + ">"; })
                .setTooltipValueFormat((value, locale) -> {
                    assertThat(value).isInstanceOf(BigDecimal.class); assertThat(locale).isEqualTo(Locale.GERMANY);
                    formatted.incrementAndGet(); return "<" + value + ">";
                }).setAxisCategoryFormat((value, locale) -> "<" + ((LocalDate) value).getMonthValue() + ">")
                .setAxisValueFormat((value, locale) -> "<tick>"));
        assertThat(categories.get()).isEqualTo(4); assertThat(values.get()).isEqualTo(4); assertThat(labels.get()).isEqualTo(4);
        assertThat(formatted.get()).isEqualTo(3);
        assertThat(html()).contains("&lt;First report&gt;", "&lt;1234.56789&gt;", "&lt;tick&gt;", "&lt;1&gt;", "1.234,57")
                .doesNotContain("<First", "<tick>", "<1234");
    }
    @ParameterizedTest @EnumSource(SwChart.Orientation.class)
    void alternateLabelsAndOverridesAreEscapedInBothAttributesAndNativeTitles(SwChart.Orientation orientation) {
        String attack = "\"><img src=x onerror=alert(1)>&'";
        tester.startComponentInPage(chart().setOrientation(orientation).setTooltipLabel(row -> attack)
                .setTooltipValueFormat((value, locale) -> attack).setAxisValueFormat((value, locale) -> attack)
                .setAxisCategoryFormat((value, locale) -> attack));
        assertThat(html()).doesNotContain("<img", "\" onerror=").contains("&lt;img", "&quot;");
        assertThat(cells("td").get(0).getAttribute("data-tooltip-value")).isEqualTo(attack);
    }
    @Test void blankTooltipFormatsAreExplicitAndNeverFallBackToNonemptyCells() {
        tester.startComponentInPage(chart().setTooltipLabelFormat((value, locale) -> null)
                .setTooltipValueFormat((value, locale) -> null));
        assertThat(cells("td").get(0).getValue()).isEqualTo("1,234.57");
        assertThat(cells("td").get(0).getAttribute("data-tooltip-value")).isEmpty();
        assertThat(html()).contains("data-tooltip-label=\"\"").doesNotContain("Revenue: ");
    }
    @Test void clearingOverridesRestoresInheritanceWithoutDuplicatingCallbackReadsOrPayload() {
        AtomicInteger reads = new AtomicInteger();
        tester.startComponentInPage(chart().setAxisCategoryFormat(SwChart.CategoryFormat.MONTH)
                .setAxisCategoryFormat((SerializableBiFunction<Object, Locale, String>) null)
                .setTooltipLabelFormat(SwChart.CategoryFormat.MONTH).setTooltipLabelFormat((SerializableBiFunction<Object, Locale, String>) null)
                .setAxisValueFormat(SwChart.ValueFormat.INTEGER).setAxisValueFormat((SerializableBiFunction<Number, Locale, String>) null)
                .setTooltipCurrency(Currency.getInstance("JPY")).setTooltipValueFormat((SerializableBiFunction<Number, Locale, String>) null)
                .setTooltipLabel(Sale::report).setTooltipLabel(null).setValueFormat((value, locale) -> { reads.incrementAndGet(); return "value " + value; }));
        assertThat(reads.get()).isEqualTo(9); // Three real values plus six axis ticks.
        assertThat(html()).doesNotContain("data-tooltip-value=", "data-tooltip-label=", "First report").contains("value 1234.56789");
    }
    static Stream<Arguments> instantFormats() {
        return Stream.of(Arguments.of(SwChart.CategoryFormat.DAY, "Apr 2"), Arguments.of(SwChart.CategoryFormat.MONTH_SHORT, "Apr"),
                Arguments.of(SwChart.CategoryFormat.MONTH, "April"), Arguments.of(SwChart.CategoryFormat.MONTH_YEAR, "Apr 2026"),
                Arguments.of(SwChart.CategoryFormat.DATE, "Apr 2, 2026"), Arguments.of(SwChart.CategoryFormat.TIME, "8:00 AM"));
    }
    @ParameterizedTest @MethodSource("instantFormats")
    void namedDateAndTimeFormatsUseTheExplicitZone(SwChart.CategoryFormat format, String expected) {
        tester.startComponentInPage(new SwChart<>("chart", Model.ofList(List.of(Instant.parse("2026-04-01T23:00:00Z"))), value -> value)
                .addSeries(ChartSeries.bar("Count", value -> 1)).setTimeZone(ZoneId.of("Asia/Tokyo"))
                .setCategoryFormat((value, locale) -> "Original").setAxisCategoryFormat(format).setTooltipLabelFormat(format));
        assertThat(tags("sw-chart-x-axis").get(0).getValue().replace('\u202f', ' ')).contains(expected);
        assertThat(html().replace('\u202f', ' ')).contains("Original</th>", expected + "; Count: 1");
    }
    static Stream<Arguments> calendarFormats() {
        return Stream.of(Arguments.of(LocalDate.of(2026, 4, 1), SwChart.CategoryFormat.DAY, "Apr 1"),
                Arguments.of(LocalDateTime.of(2026, 4, 1, 23, 0), SwChart.CategoryFormat.TIME, "11:00 PM"),
                Arguments.of(YearMonth.of(2026, 4), SwChart.CategoryFormat.MONTH, "April"),
                Arguments.of(YearMonth.of(2026, 4), SwChart.CategoryFormat.TIME, "Apr 2026"),
                Arguments.of(LocalTime.of(10, 0), SwChart.CategoryFormat.TIME, "10:00 AM"),
                Arguments.of(OffsetDateTime.parse("2026-04-01T01:00:00+02:00"), SwChart.CategoryFormat.DAY, "Mar 31"),
                Arguments.of(ZonedDateTime.parse("2026-04-01T01:00:00+02:00[Europe/Berlin]"), SwChart.CategoryFormat.TIME, "4:00 PM"),
                Arguments.of("2026-04-01", SwChart.CategoryFormat.MONTH, "2026-04-01"),
                Arguments.of(42, SwChart.CategoryFormat.DATE, "42"));
    }
    @ParameterizedTest @MethodSource("calendarFormats")
    void calendarValuesNeverShiftAndIncompatibleKindsAreNotParsed(Object value, SwChart.CategoryFormat format, String expected) {
        tester.startComponentInPage(new SwChart<>("chart", Model.ofList(List.of(value)), category -> category)
                .addSeries(ChartSeries.bar("Count", row -> 1)).setTimeZone(ZoneId.of("America/Los_Angeles"))
                .setCategoryFormat(format));
        assertThat(html().replace('\u202f', ' ')).contains(expected + "</th>");
    }
    @ParameterizedTest @CsvSource({"hr, travanj", "pl, kwiecień"})
    void fullMonthLabelsUseTheStandaloneGrammaticalForm(String language, String month) {
        tester.getSession().setLocale(Locale.forLanguageTag(language));
        tester.startComponentInPage(new SwChart<>("chart", Model.ofList(List.of(LocalDate.of(2026, 4, 1))), day -> day)
                .addSeries(ChartSeries.bar("Count", day -> 1)).setCategoryFormat(SwChart.CategoryFormat.MONTH));
        assertThat(html()).contains(month + "</th>", month + "; Count: 1");
        assertThat(tags("sw-chart-x-axis").get(0).getValue()).contains(month);
    }
    @Test void missingCurrencyFailsClearlyInEitherOverride() {
        assertThatThrownBy(() -> tester.startComponentInPage(chart().setAxisValueFormat(SwChart.ValueFormat.CURRENCY)))
                .hasStackTraceContaining("Currency formatting requires");
        assertThatThrownBy(() -> tester.startComponentInPage(chart().setTooltipValueFormat(SwChart.ValueFormat.CURRENCY)))
                .hasStackTraceContaining("Currency formatting requires");
    }
    @Test void overrideCallbacksAndLocaleChangesSurvivePageSerializationAndAjax() {
        tester.startPage(FormatPage.class);
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        var restored = (FormatPage) serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));
        tester.startPage(restored); tester.clickLink("refresh");
        assertThat(html()).contains("März", "data-tooltip-value=\"1.234,568 EUR\"", "data-tooltip-label=\"First report\"");
    }
    public static class FormatPage extends WebPage implements IMarkupResourceStreamProvider {
        String unit = " USD";
        public FormatPage() {
            var chart = new SwChart<>("chart", Model.ofList(sales()), Sale::day)
                    .addSeries(ChartSeries.line("Revenue", Sale::revenue))
                    .setAxisCategoryFormat(SwChart.CategoryFormat.MONTH).setTooltipLabel(Sale::report)
                    .setTooltipValueFormat((value, locale) -> java.text.NumberFormat.getNumberInstance(locale).format(value) + unit);
            add(chart);
            add(new AjaxLink<Void>("refresh") {
                @Override public void onClick(AjaxRequestTarget target) {
                    unit = " EUR"; getSession().setLocale(Locale.GERMANY); target.add(chart);
                }
            });
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><body><div wicket:id='chart'></div><a wicket:id='refresh'>Refresh</a></body></html>");
        }
    }
}

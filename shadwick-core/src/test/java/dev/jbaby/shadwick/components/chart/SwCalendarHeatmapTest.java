package dev.jbaby.shadwick.components.chart;

import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** One cell per day in weeks and weekdays, the period, missing days, the shared scale and its labels. */
class SwCalendarHeatmapTest {
    record Day(LocalDate date, Number value) implements Serializable {}

    WicketTester tester;

    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() {
        if (tester.getLastResponse() != null)
            assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        tester.destroy();
    }

    String html() { return tester.getLastResponseAsString(); }
    SwCalendarHeatmap<Day> chart(List<Day> days) {
        return new SwCalendarHeatmap<>("chart", Model.ofList(days), Day::date, Day::value).setTitle("Activity").setValueHeader("Visits");
    }
    List<TagTester> cells() { return TagTester.createTagsByAttribute(html(), "class", "sw-heatmap-cell", false); }
    List<TagTester> days() { return cells().stream().filter(cell -> cell.getAttribute("data-outside") == null).toList(); }
    static Day day(String date, Number value) { return new Day(LocalDate.parse(date), value); }
    /** The cell of a day, found by its full date (cells come in row order: all Sundays, then all Mondays, ...). */
    TagTester cell(String label) {
        return days().stream().filter(d -> label.equals(d.getAttribute("data-tooltip-label"))).findFirst().orElseThrow();
    }

    @Test void weeksAreColumnsAndWeekdaysRowsStartingOnTheLocalesFirstDay() {
        // 2026-03-04 is a Wednesday; an en-US week starts on Sunday, so March 1 opens the first column
        tester.startComponentInPage(chart(List.of(day("2026-03-04", 3), day("2026-03-10", 5))));
        assertThat(html()).contains("data-rows=\"7\"", "data-columns=\"2\"", "<span aria-hidden=\"true\">Sun</span>");
        assertThat(cells()).hasSize(14);
        assertThat(days()).hasSize(7); // March 4 to 10
        // The first day of the period is in row 3 (Wednesday) of the first column
        assertThat(cell("Wednesday, March 4, 2026").getAttribute("data-row")).isEqualTo("3");
        assertThat(cell("Wednesday, March 4, 2026").getAttribute("data-column")).isEqualTo("0");
        // Germany starts its weeks on Monday
        tester.getSession().setLocale(Locale.GERMANY);
        tester.startComponentInPage(chart(List.of(day("2026-03-04", 3), day("2026-03-10", 5))));
        assertThat(html()).contains("<span aria-hidden=\"true\">Mo</span>", "data-columns=\"2\"");
        assertThat(cell("Mittwoch, 4. März 2026").getAttribute("data-row")).isEqualTo("2");
        // An explicit first day wins over the locale
        tester.startComponentInPage(chart(List.of(day("2026-03-04", 3))).setFirstDayOfWeek(DayOfWeek.WEDNESDAY));
        assertThat(cell("Mittwoch, 4. März 2026").getAttribute("data-row")).isEqualTo("0");
    }

    @Test void daysWithoutAnEntryAreNotMeasuredUnlessTheyHaveAMissingValue() {
        var data = List.of(day("2026-03-02", 4), day("2026-03-04", 8));
        tester.startComponentInPage(chart(data));
        var tuesday = days().stream().filter(d -> "Tuesday, March 3, 2026".equals(d.getAttribute("data-tooltip-label"))).findFirst().orElseThrow();
        assertThat(tuesday.getAttribute("data-tooltip-value")).isEqualTo("Not measured");
        assertThat(tuesday.getAttribute("title")).isEqualTo("Tuesday, March 3, 2026; Visits: Not measured");
        assertThat(html()).contains("× Not measured", "sw-heatmap-missing");
        // Counted as zero: a measured value in the lowest band, and no missing key
        tester.startComponentInPage(chart(data).setMissingValue(0));
        tuesday = days().stream().filter(d -> "Tuesday, March 3, 2026".equals(d.getAttribute("data-tooltip-label"))).findFirst().orElseThrow();
        assertThat(tuesday.getAttribute("data-tooltip-value")).isNull();
        assertThat(tuesday.getAttribute("title")).isEqualTo("Tuesday, March 3, 2026; Visits: 0");
        assertThat(html()).doesNotContain("× Not measured").contains("sw-heatmap-level-0");
        // An entry with a null value stays not measured even then
        tester.startComponentInPage(chart(List.of(day("2026-03-02", null), day("2026-03-04", 8))).setMissingValue(0));
        assertThat(html()).contains("× Not measured");
    }

    @Test void thePeriodPadsWeeksAndFixesTheDaysShown() {
        tester.startComponentInPage(chart(List.of(day("2026-03-04", 3))).setPeriod(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)));
        assertThat(days()).hasSize(31);
        assertThat(html()).contains("data-columns=\"5\"");
        // Leap day and a year boundary
        tester.startComponentInPage(chart(List.of(day("2028-02-29", 1))).setPeriod(LocalDate.of(2027, 12, 30), LocalDate.of(2028, 3, 1)));
        assertThat(days()).hasSize(63);
        assertThat(html()).contains("Tuesday, February 29, 2028");
    }

    @Test void monthsAreLabelledOnTheWeekTheyStartWithTheYearWhenThePeriodSpansTwo() {
        tester.startComponentInPage(chart(List.of()).setPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 30)));
        assertThat(TagTester.createTagsByAttribute(html(), "class", "sw-calendar-heatmap-month", false))
                .extracting(TagTester::getValue).containsExactly("Jan", "Feb", "Mar", "Apr");
        // A short first month that would crowd the next is left out; the year goes on the first label and January
        tester.startComponentInPage(chart(List.of()).setPeriod(LocalDate.of(2025, 11, 25), LocalDate.of(2026, 2, 10)));
        assertThat(TagTester.createTagsByAttribute(html(), "class", "sw-calendar-heatmap-month", false))
                .extracting(TagTester::getValue).containsExactly("Dec 2025", "Jan 2026", "Feb");
        assertThat(html()).contains("<span class=\"sw-chart-sr-only\">Week of Nov 23, 2025</span>");
    }

    @Test void invalidDataPeriodsAndRangesFailClearly() {
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(day("2026-03-04", 1), day("2026-03-04", 2)))))
                .hasStackTraceContaining("aggregate per day first");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(day("2026-05-01", 1)))
                .setPeriod(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))).hasStackTraceContaining("within its period");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of())
                .setPeriod(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 1)))).hasStackTraceContaining("increasing");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of())
                .setPeriod(LocalDate.of(2023, 1, 1), LocalDate.of(2026, 1, 1)))).hasStackTraceContaining("at most three years");
        assertThatThrownBy(() -> tester.startComponentInPage(chart(List.of(day("2026-03-04", 50))).setRange(0, 10)))
                .hasStackTraceContaining("within the configured range");
        assertThatThrownBy(() -> chart(List.of()).setMissingValue(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chart(List.of()).setColor(6)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void emptyStatesTitlesAreEscapedAndValuesUseTheSharedFormats() {
        tester.startComponentInPage(chart(List.of()));
        assertThat(html()).contains("No data to display.").doesNotContain("<table");
        tester.startComponentInPage(chart(List.of(day("2026-03-04", null))));
        assertThat(html()).contains("No measurements to plot.", "<table");
        tester.startComponentInPage(chart(List.of(day("2026-03-04", new BigDecimal("1234.5"))))
                .setTitle("<b>Sales</b>").setCurrency(java.util.Currency.getInstance("EUR")));
        assertThat(html()).contains("&lt;b&gt;Sales&lt;/b&gt;", "€1,234.50").doesNotContain("<b>Sales</b>");
    }

    @Test void theSnapshotIsReadOncePerRequestAndTheModelsDetach() {
        List<Day> data = new ArrayList<>(List.of(day("2026-03-04", 3)));
        int[] reads = {0};
        var chart = new SwCalendarHeatmap<>("chart", Model.ofList(data), d -> { reads[0]++; return d.date(); }, Day::value);
        tester.startComponentInPage(chart);
        assertThat(reads[0]).isEqualTo(1);
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        assertThat(serializer.deserialize(serializer.serialize(chart))).isNotNull();
    }
}

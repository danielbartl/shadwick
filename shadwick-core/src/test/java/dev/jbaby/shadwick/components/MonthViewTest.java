package dev.jbaby.shadwick.components;

import static org.assertj.core.api.Assertions.*;

import java.time.*;
import java.util.Locale;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MonthViewTest {
    WicketTester tester;
    @BeforeEach
    void setup() {
        tester = new WicketTester();
        tester.getSession().setLocale(Locale.US);
    }
    @AfterEach
    void stop() {
        tester.destroy();
    }
    String html() {
        return tester.getLastResponseAsString();
    }
    @Test
    void serverGridHasLocaleHeadersCaptionTodayAndOutsideDays() {
        tester.startComponentInPage(
                new SwMonthView("view", Model.of(YearMonth.of(2026, 10))).setToday(LocalDate.of(2026, 10, 23)));
        assertThat(tester.getTagByWicketId("caption").getValue()).isEqualTo("October 2026");
        assertThat(TagTester.createTagsByAttribute(html(), "class", "sw-month-view-day", false)).hasSize(35);
        assertThat(html())
                .contains("aria-label=\"Sunday\"", "Friday, October 23, 2026", "aria-current=\"date\"")
                .doesNotContain("role=\"grid\"", "style=");
    }
    @Test
    void localeWeekRulesAndOverrideApplyToServerCells() {
        tester.startComponentInPage(new SwMonthView("view", Model.of(YearMonth.of(2026, 1)))
                        .setCalendarLocale(Locale.UK)
                        .setShowWeekNumber(true));
        assertThat(tester.getTagByWicketId("weekdays").getAttribute("aria-label")).isEqualTo("Monday");
        assertThat(TagTester.createTagsByAttribute(html(), "class", "sw-month-view-week-number", false))
                .extracting(TagTester::getValue)
                .containsExactly("1", "2", "3", "4", "5");
        tester.startComponentInPage(new SwMonthView("view", Model.of(YearMonth.of(2026, 1)))
                        .setCalendarLocale(Locale.UK)
                        .setWeekStartsOn(DayOfWeek.SUNDAY));
        assertThat(tester.getTagByWicketId("weekdays").getAttribute("aria-label")).isEqualTo("Sunday");
    }
    @Test
    void hiddenOutsideDaysHaveNoContentAndLeapMonthHas29OwnDays() {
        var renderedDays = new java.util.concurrent.atomic.AtomicInteger();
        tester.startComponentInPage(new SwMonthView("view", Model.of(YearMonth.of(2024, 2)))
                        .setShowOutsideDays(false)
                        .setDayContent((id, day) -> {
                            renderedDays.incrementAndGet();
                            return new Label(id, "Day " + day.number());
                        }));
        assertThat(renderedDays.get()).isEqualTo(29);
        assertThat(html()).contains("Day 29").doesNotContain("Day 30", "Day 31");
        assertThat(TagTester.createTagsByAttribute(html(), "data-hidden", "true", false))
                .isNotEmpty()
                .allSatisfy(tag -> assertThat(tag.getValue()).isEmpty());
    }
    @Test
    void dayFactorySeesTypedContextAndEscapesCustomContent() {
        tester.startComponentInPage(new SwMonthView("view", Model.of(YearMonth.of(2026, 11)))
                        .setDayContent(
                                (id, day)
                                        -> new Label(
                                                id, day.date().equals(LocalDate.of(2026, 11, 10)) ? "<Booking>" : "")));
        assertThat(html()).contains("&lt;Booking&gt;").doesNotContain("<Booking>");
    }
    @Test
    void dayLinksAreBookmarkableAndIncludeTheIsoDate() {
        tester.startComponentInPage(new SwMonthView("view", Model.of(YearMonth.of(2026, 11))).setDayParameter("day"));
        assertThat(html()).contains("day=2026-11-10", "sw-month-view-day-link");
        assertThat(tester.getTagByWicketId("body").getName()).isEqualTo("a");
    }
    @ParameterizedTest
    @ValueSource(ints = {1, 9999})
    void yearBoundariesHaveNoInvalidDayLinks(int year) {
        tester.startComponentInPage(new SwMonthView("view", Model.of(YearMonth.of(year, year == 1 ? 1 : 12)))
                        .setDayParameter("date")
                        .setDayContent((id, day) -> {
                            assertThat(day.date().getYear()).isBetween(1, 9999);
                            return new Label(id, "");
                        }));
        assertThat(html()).doesNotContain("date=0000-", "date=%2B10000-");
        assertThat(tester.getTagByWicketId(year == 1 ? "previous" : "next").getAttribute("href")).isNull();
    }
}

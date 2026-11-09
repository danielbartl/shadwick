package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.components.*;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.time.*;
import java.util.*;

/** Native month links, per-day content and a separate Ajax-controlled month. */
public class MonthViewPage extends BasePage {

    public MonthViewPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        PageParameters parameters = getPageParameters();
        add(
                new SwMonthView("agenda")
                        .setToday(LocalDate.of(2026, 11, 5))
                        .setCalendarLocale(Locale.UK)
                        .setShowWeekNumber(true)
                        .setDayParameter("day")
                        .setDayContent((id, day) -> new Label(id, event(day.date()))));
        add(
                new Label(
                        "selected",
                        parameters.get("day").isEmpty()
                                ? "Choose a day to open its details."
                                : "Selected day: " + parameters.get("day").toString()));
        add(
                new SwMonthView("compact")
                        .setMonthParameter("rota")
                        .setToday(LocalDate.of(2026, 11, 5))
                        .setShowOutsideDays(false)
                        .setNavSize(SwMonthView.NavSize.ICON_LG)
                        .setDayContent(
                                (id, day) ->
                                        new Label(
                                                id,
                                                day.date().getDayOfWeek() == DayOfWeek.FRIDAY
                                                        ? "Review"
                                                        : "")));
        Model<YearMonth> month = Model.of(YearMonth.of(2026, 11));
        var live =
                new SwMonthView("live", month)
                        .setToday(LocalDate.of(2026, 11, 5))
                        .setDayContent(
                                (id, day) -> new Label(id, day.number() == 10 ? "Planning" : ""));
        add(live);
        add(
                SwButton.onClick(
                        "advance",
                        "Next month over Ajax",
                        target -> {
                            month.setObject(month.getObject().plusMonths(1));
                            target.add(live);
                        }));
    }

    private static String event(LocalDate date) {
        return switch (date.getDayOfMonth()) {
            case 5 -> "Kick-off";
            case 10 -> "3 bookings";
            case 17 -> "Design review";
            case 24 -> "Ship it";
            default -> "";
        };
    }

    @Override
    protected String getTitle() {
        return "Month view";
    }
}

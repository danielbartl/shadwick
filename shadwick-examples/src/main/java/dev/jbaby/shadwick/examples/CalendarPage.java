package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.CalendarBehavior;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.components.form.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;
import java.time.*;
import java.time.format.FormatStyle;
import java.util.Locale;

/** Inline calendars and composed popup pickers, with native date-only values. */
public class CalendarPage extends BasePage {

    public static final LocalDate TODAY = LocalDate.of(2026, 11, 5);

    public static class Booking implements Serializable {

        public LocalDate day = LocalDate.of(2026, 11, 10);
        public DateRange stay =
                new DateRange(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12));
    }

    public CalendarPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        addNormalForm();
        addAjaxForm();
        addCalendarVariants();
        addLiveDateForm();
        addLiveRangeForm();
        addManagedAndOverlayDates();
    }

    private void addNormalForm() {
        var saved = new Label("saved", Model.of(""));
        var form =
                new Form<Booking>("form", new CompoundPropertyModel<>(new Booking())) {

                    @Override
                    protected void onSubmit() {
                        saved.setDefaultModelObject(summary(getModelObject()));
                    }
                };
        fields(form);
        form.add(new Button("save").add(Sw.Behaviors.button()), saved);
        add(form);
    }

    private void addAjaxForm() {
        var ajax = new Form<Booking>("ajaxForm", new CompoundPropertyModel<>(new Booking()));
        fields(ajax);
        var ajaxSaved = new Label("saved", Model.of(""));
        ajaxSaved.setOutputMarkupId(true);
        ajax.add(
                SwSubmitButton.onSubmit(
                        "save",
                        "Save booking",
                        target -> {
                            ajaxSaved.setDefaultModelObject(summary(ajax.getModelObject()));
                            target.add(ajaxSaved);
                        }),
                ajaxSaved);
        add(ajax);
    }

    private void addCalendarVariants() {
        var inline =
                new SwCalendar("inline", "Delivery day", Model.of(LocalDate.of(2026, 11, 10)))
                        .setToday(TODAY)
                        .setDefaultMonth(YearMonth.of(2026, 11))
                        .setMin(LocalDate.of(2026, 11, 1))
                        .setMax(LocalDate.of(2026, 12, 31))
                        .setDisabledDates("sat,sun,2026-11-26..2026-11-27")
                        .setShowWeekNumber(true)
                        .setWeekStartsOn(DayOfWeek.MONDAY)
                        .setCalendarLocale(Locale.UK)
                        .setDescription(
                                "Weekends and 26–27 November are unavailable. Week numbering"
                                    + " follows the locale.");
        add(inline);
        add(
                new SwRangeCalendar(
                                "range",
                                "Planning window",
                                Model.of(
                                        new DateRange(
                                                LocalDate.of(2026, 11, 10),
                                                LocalDate.of(2026, 11, 13))))
                        .setToday(TODAY)
                        .setDefaultMonth(YearMonth.of(2026, 11))
                        .setNumberOfMonths(2)
                        .setWeekStartsOn(DayOfWeek.MONDAY)
                        .setDescription(
                                "Choose a start, then an end. Earlier ends swap; a complete range"
                                    + " starts again."));
        add(
                new SwDatePicker("birthday", "Date of birth", Model.of(LocalDate.of(1996, 2, 29)))
                        .setCaption(CalendarBehavior.Caption.DROPDOWN)
                        .setStartMonth(YearMonth.of(1900, 1))
                        .setEndMonth(YearMonth.of(2026, 12))
                        .setFormat(FormatStyle.MEDIUM)
                        .setToday(TODAY)
                        .setDescription(
                                "Month/year selects make distant dates easier. This example"
                                    + " includes a leap day."));
    }

    private void addLiveDateForm() {
        Model<LocalDate> live = Model.of(LocalDate.of(2026, 11, 10));
        var current = new Label("current", (IModel<String>) () -> "Current: " + live.getObject());
        current.setOutputMarkupId(true);
        var choice =
                new SwDatePicker("day", "Live appointment", live)
                        .setToday(TODAY)
                        .setMin(LocalDate.of(2026, 11, 1))
                        .setMax(LocalDate.of(2026, 12, 31))
                        .setRequired(true);
        choice.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        target.add(current);
                    }
                });
        var liveForm = new Form<Void>("liveForm");
        liveForm.add(
                choice,
                current,
                SwButton.onClick("refresh", "Refresh picker", target -> target.add(choice)));
        add(liveForm);
    }

    private void addLiveRangeForm() {
        Model<DateRange> liveRange =
                Model.of(new DateRange(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12)));
        var rangeCurrent =
                new Label(
                        "current",
                        (IModel<String>)
                                () ->
                                        "Current range: "
                                                + (liveRange.getObject() == null
                                                        ? "none"
                                                        : liveRange.getObject().from()
                                                                + " to "
                                                                + liveRange.getObject().to()));
        rangeCurrent.setOutputMarkupId(true);
        var rangeChoice =
                new SwDateRangePicker("range", "Live stay", liveRange)
                        .setToday(TODAY)
                        .setRequired(true)
                        .setMin(LocalDate.of(2026, 11, 1))
                        .setMax(LocalDate.of(2026, 12, 31));
        rangeChoice.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        target.add(rangeCurrent);
                    }
                });
        var rangeForm = new Form<Void>("liveRangeForm");
        rangeForm.add(rangeChoice, rangeCurrent);
        add(rangeForm);
    }

    private void addManagedAndOverlayDates() {
        var managed = new SwCalendar("managed", "Managed date", Model.of(TODAY)).setToday(TODAY);
        managed.setEnabled(false);
        add(managed);
        add(
                new SwCalendar("readonly", "Reference date", Model.of(TODAY))
                        .setReadOnly(true)
                        .setToday(TODAY)
                        .setDescription("Browse months without changing the date."));
        add(new SwDatePicker("dialogDate", "Dialog date", Model.of(TODAY)).setToday(TODAY));
        add(new SwDatePicker("popoverDate", "Popover date", Model.of(TODAY)).setToday(TODAY));
    }

    private static String summary(Booking booking) {
        return "Saved: " + booking.day + "; " + booking.stay.from() + " – " + booking.stay.to();
    }

    private static void fields(Form<Booking> form) {
        form.add(
                new SwDatePicker("day", "Departure", null)
                        .setRequired(true)
                        .setToday(TODAY)
                        .setFormat(FormatStyle.MEDIUM)
                        .setMin(LocalDate.of(2026, 11, 1))
                        .setMax(LocalDate.of(2026, 12, 31))
                        .setDisabledDates("sat,sun,2026-11-26..2026-11-27")
                        .setPlaceholder("Choose departure")
                        .setDescription(
                                "A weekday in November or December. Unavailable dates also fail"
                                    + " server validation."));
        form.add(
                new SwDateRangePicker("stay", "Stay", null)
                        .setRequired(true)
                        .setToday(TODAY)
                        .setNumberOfMonths(2)
                        .setFormat(FormatStyle.MEDIUM)
                        .setMin(LocalDate.of(2026, 11, 1))
                        .setMax(LocalDate.of(2026, 12, 31))
                        .setDescription(
                                "Both dates are required. The second pick completes the range."));
    }

    @Override
    protected String getTitle() {
        return "Calendar & date picker";
    }
}

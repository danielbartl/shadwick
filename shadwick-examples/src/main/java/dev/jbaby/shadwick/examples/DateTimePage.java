package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.components.form.SwDateField;
import dev.jbaby.shadwick.components.form.SwDateTimeLocalField;
import dev.jbaby.shadwick.components.form.SwMonthField;
import dev.jbaby.shadwick.components.form.SwTimeField;
import dev.jbaby.shadwick.components.form.SwWeekField;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.RangeValidator;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Native date/time inputs with locale-independent conversion and in-place Ajax feedback. */
public class DateTimePage extends BasePage {
    private static final LocalDate FIRST_DAY = LocalDate.of(2026, 1, 1);
    private static final LocalDate LAST_DAY = LocalDate.of(2026, 12, 31);

    public DateTimePage(PageParameters parameters) {
        super(parameters);
        Label saved = new Label("saved", Model.of(""));
        Form<Schedule> form = new Form<>("form", new CompoundPropertyModel<>(new Schedule())) {
            @Override
            protected void onSubmit() { saved.setDefaultModelObject("Saved: " + getModelObject()); }
        };
        addFields(form);
        form.add(new Button("save").add(Sw.Behaviors.button()));
        form.add(saved);
        add(form);

        Label ajaxSaved = new Label("saved", Model.of(""));
        ajaxSaved.setOutputMarkupId(true);
        Form<Schedule> ajaxForm = new Form<>("ajaxForm", new CompoundPropertyModel<>(new Schedule()));
        addFields(ajaxForm);
        ajaxForm.add(SwSubmitButton.onSubmit("save", "Save schedule", target -> {
            ajaxSaved.setDefaultModelObject("Saved: " + ajaxForm.getModelObject());
            target.add(ajaxSaved);
        }));
        ajaxForm.add(ajaxSaved);
        add(ajaxForm);

        Model<LocalDate> liveDate = Model.of();
        Label current = new Label("current", (IModel<String>) () -> liveDate.getObject() == null
                ? "No date selected." : "Selected: " + liveDate.getObject() + ".");
        current.setOutputMarkupId(true);
        Form<Void> liveForm = new Form<>("liveForm");
        liveForm.add(boundedDate(new SwDateField("liveDate", "Preview date", liveDate))
                .setDescription("Optional. Choose a date in 2026 to update the summary.")
                .add(new AjaxFormComponentUpdatingBehavior("change") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) { target.add(current); }
                }));
        liveForm.add(current);
        add(liveForm);

        var disabled = new SwDateField("disabledDate", "Managed date", Model.of(LocalDate.of(2026, 11, 4)));
        disabled.setEnabled(false);
        add(disabled);
        add(new SwTimeField("preciseTime", "Time with seconds", Model.of(LocalTime.of(10, 30, 15, 123_456_789)))
                .addToInput(AttributeModifier.replace("step", "0.001"))
                .setDescription("Native millisecond precision. Sub-millisecond model precision is omitted when rendered."));
    }

    private static SwDateField boundedDate(SwDateField field) {
        return field.addValidator(RangeValidator.range(FIRST_DAY, LAST_DAY))
                .addToInput(AttributeModifier.replace("min", FIRST_DAY.toString()),
                        AttributeModifier.replace("max", LAST_DAY.toString()));
    }

    private static void addFields(Form<Schedule> form) {
        form.add(boundedDate(new SwDateField("date")).setRequired(true)
                .setDescription("Choose a date in 2026. A Wicket validator enforces the same bounds as the picker."));
        form.add(new SwTimeField("time").setRequired(true).setDescription("Local wall-clock time."));
        form.add(new SwDateTimeLocalField("local").setRequired(true)
                .addToInput(AttributeModifier.replace("step", "0.001"))
                .setDescription("No timezone or UTC conversion. Seconds and milliseconds are supported."));
        form.add(new SwMonthField("month").setRequired(true).setDescription("A month in YYYY-MM format."));
        form.add(new SwWeekField("week").setRequired(true).setDescription("An ISO week-year and week number, such as 2026-W45."));
    }

    public static class Schedule implements Serializable {
        public LocalDate date;
        public LocalTime time = LocalTime.of(9, 30);
        public LocalDateTime local = LocalDateTime.of(2026, 11, 4, 9, 30);
        public String month = "2026-11";
        public String week = "2026-W45";

        @Override
        public String toString() {
            return "date " + date + "; time " + time + "; local " + local + "; month " + month + "; week " + week + ".";
        }
    }

    @Override
    protected String getTitle() { return "Dates & time"; }
}

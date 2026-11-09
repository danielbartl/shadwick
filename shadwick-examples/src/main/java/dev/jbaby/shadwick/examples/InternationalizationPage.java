package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.ThemeMode;
import dev.jbaby.shadwick.behaviors.ThemeBehavior;
import dev.jbaby.shadwick.behaviors.ToastBehavior;
import dev.jbaby.shadwick.components.*;
import dev.jbaby.shadwick.components.form.*;
import dev.jbaby.shadwick.components.table.SwActionsColumn;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.lang.Bytes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Application prose is English; library-owned controls follow the visitor's Wicket locale. */
public class InternationalizationPage extends BasePage {

    private static final List<String> CITIES =
            List.of(
                    "Amsterdam",
                    "Berlin",
                    "Lisbon",
                    "London",
                    "Madrid",
                    "Paris",
                    "Rome",
                    "São Paulo",
                    "Tokyo",
                    "Toronto",
                    "Vienna",
                    "Zürich");

    public InternationalizationPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(Sw.Behaviors.toasts(), Sw.Behaviors.feedbackToasts());
        add(new Label("locale", (IModel<String>) () -> getLocale().toLanguageTag()));
        addTravelForm();
        addLocalizedControls();
        addNotificationActions();
        addCitiesTable();
    }

    private void addTravelForm() {
        Form<Void> form = new Form<>("form");
        form.setOutputMarkupId(true);
        form.setMultiPart(true);
        form.setMaxSize(Bytes.kilobytes(256));
        form.add(new SwCombobox<>("city", "City", Model.of(), Model.ofList(CITIES)));
        form.add(
                new SwMultiCombobox<>(
                        "destinations",
                        "Destinations",
                        Model.ofList(new ArrayList<>(List.of("Berlin", "Tokyo"))),
                        Model.ofList(CITIES)));
        form.add(
                new SwDatePicker("date", "Travel date", Model.of())
                        .setToday(CalendarPage.TODAY)
                        .setDefaultMonth(YearMonth.of(2026, 11)));
        form.add(
                new SwDateField("nativeDate", "Native date", Model.of(LocalDate.of(2026, 11, 10)))
                        .setRequired(true));
        form.add(
                new SwFileDropzone("files", "Documents", Model.ofList(new ArrayList<>()))
                        .setAccept(".txt")
                        .setMaxFiles(2)
                        .setMaxFileSize(Bytes.kilobytes(64)));
        form.add(
                SwSubmitButton.onSubmit(
                        "save",
                        "Validate fields",
                        target ->
                                info(
                                        "Validation passed. Application messages remain yours to"
                                            + " translate.")));
        form.add(
                SwButton.onClick(
                        "refresh", "Refresh fields with Ajax", target -> target.add(form)));
        add(form);
    }

    private void addLocalizedControls() {
        add(
                new SwCalendar("calendar", "Delivery day", Model.of(CalendarPage.TODAY))
                        .setToday(CalendarPage.TODAY)
                        .setDefaultMonth(YearMonth.of(2026, 11))
                        .setShowWeekNumber(true));
        add(new SwDialog("dialog", "A localized dialog").setTrigger("Open dialog"));
        var themePreview = new WebMarkupContainer("themePreview");
        themePreview.add(
                new ThemeBehavior()
                        .setRememberChoice(false)
                        .setDefaultMode(Appearance.isDark() ? ThemeMode.DARK : ThemeMode.LIGHT));
        themePreview.add(new SwThemeToggle("theme").setTarget(themePreview));
        add(themePreview);
    }

    private void addNotificationActions() {
        add(
                SwButton.onClick(
                        "notify",
                        "Show notification",
                        target ->
                                Sw.toast(
                                        target,
                                        "This message is application content.",
                                        ToastBehavior.Variant.SUCCESS,
                                        "Ready",
                                        java.time.Duration.ZERO)));
        add(
                SwButton.onClick(
                        "feedback",
                        "Show feedback toast",
                        target -> info("A Wicket feedback message.")));
    }

    private void addCitiesTable() {
        var cityColumn =
                new AbstractColumn<String, String>(Model.of("City")) {

                    @Override
                    public void populateItem(
                            Item<ICellPopulator<String>> item, String id, IModel<String> row) {
                        item.add(new Label(id, row));
                    }
                };
        var actions = new SwActionsColumn<String, String>();
        actions.addAction("View city", (target, city) -> Sw.toast(target, city));
        var provider =
                new SortableDataProvider<String, String>() {

                    @Override
                    public Iterator<? extends String> iterator(long first, long count) {
                        return CITIES.subList(
                                        (int) first, (int) Math.min(first + count, CITIES.size()))
                                .iterator();
                    }

                    @Override
                    public long size() {
                        return CITIES.size();
                    }

                    @Override
                    public IModel<String> model(String city) {
                        return Model.of(city);
                    }
                };
        add(
                new SwDataTable<>("cities", List.of(cityColumn, actions), provider, 3)
                        .setPageSizes(3, 6, 12));
    }

    @Override
    protected String getTitle() {
        return "Internationalization";
    }
}

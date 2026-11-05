package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.util.SerializableBiFunction;
import dev.jbaby.shadwick.util.SerializableFunction;
import java.io.Serializable;
import java.time.*;
import java.time.format.*;
import java.time.temporal.WeekFields;
import java.util.*;
import org.apache.wicket.*;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/** Server-rendered agenda. Navigation is bookmarkable and preserves other page parameters. */
public class SwMonthView extends Panel {
    public static final String CONTENT_ID = "content";
    public enum NavSize { DEFAULT, ICON_XS, ICON_SM, ICON, ICON_LG }
    public record Day(LocalDate date, String label, boolean outside, boolean today) implements Serializable {
        public String iso() {
            return date.toString();
        }
        public int number() {
            return date.getDayOfMonth();
        }
    }

    private final IModel<YearMonth> month;
    private IModel<LocalDate> today = Model.of();
    private Locale locale;
    private DayOfWeek weekStartsOn;
    private boolean outsideDays = true, weekNumbers;
    private String monthParameter = "month", dayParameter;
    private NavSize navSize = NavSize.DEFAULT;
    private Class<? extends Page> dayPage;
    private SerializableFunction<Day, PageParameters> dayParameters;
    private SerializableBiFunction<String, Day, Component> content;
    private transient YearMonth rendered;
    public SwMonthView(String id) {
        this(id, null);
    }

    public SwMonthView(String id, IModel<YearMonth> month) {
        super(id);
        this.month = month;
        setOutputMarkupId(true);
        var root = new WebMarkupContainer("view");
        root.add(new StyledBehavior("sw-month-view"));
        add(root);
        root.add(AttributeModifier.replace("data-nav-size",
                (IModel<String>) ()
                        -> navSize == NavSize.DEFAULT ? null
                                                      : navSize.name().toLowerCase(Locale.ROOT).replace('_', '-')));
        root.add(navigation("previous", -1), navigation("next", 1));
        root.add(new Label("caption",
                (IModel<String>) () -> shownMonth().format(DateTimeFormatter.ofPattern("LLLL uuuu", actualLocale()))));
        root.add(new WebMarkupContainer("weekHeader") {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(weekNumbers);
            }
        }.add(new Label("text", new StringResourceModel("SwMonthView.week", this).setDefaultValue("Week"))));
        root.add(new ListView<DayOfWeek>("weekdays",
                (IModel<List<DayOfWeek>>) ()
                        -> java.util.stream.IntStream.range(0, 7).mapToObj(i -> firstDay().plus(i)).toList()) {
            @Override
            protected void populateItem(ListItem<DayOfWeek> item) {
                var day = item.getModelObject();
                item.add(AttributeModifier.replace("aria-label", day.getDisplayName(TextStyle.FULL, actualLocale())));
                item.add(new Label("name", day.getDisplayName(TextStyle.SHORT, actualLocale())));
            }
        });
        root.add(new ListView<List<LocalDate>>("weeks", (IModel<List<List<LocalDate>>>) SwMonthView.this::weeks) {
            @Override
            protected void populateItem(ListItem<List<LocalDate>> item) {
                var number = new Label("week", item.getModelObject().get(0).get(weekFields().weekOfWeekBasedYear()));
                number.setVisible(weekNumbers);
                number.add(AttributeModifier.replace("aria-label",
                        getString("SwMonthView.week", null, "Week") + " " + number.getDefaultModelObject()));
                item.add(number);
                item.add(new ListView<LocalDate>("days", item.getModel()) {
                    @Override
                    protected void populateItem(ListItem<LocalDate> cell) {
                        LocalDate date = cell.getModelObject();
                        boolean outside = !YearMonth.from(date).equals(shownMonth());
                        boolean hidden = outside && !outsideDays || date.getYear() < 1 || date.getYear() > 9999;
                        Day day = new Day(date,
                                date.format(
                                        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(actualLocale())),
                                outside, date.equals(today()));
                        if (outside)
                            cell.add(AttributeModifier.replace("data-outside", "true"));
                        if (day.today())
                            cell.add(AttributeModifier.replace("data-today", "true"));
                        if (hidden)
                            cell.add(AttributeModifier.replace("data-hidden", "true"));
                        cell.add(AttributeModifier.replace("data-date", date.toString()));
                        WebMarkupContainer body;
                        if (!hidden && (dayParameter != null || dayPage != null)) {
                            PageParameters parameters = dayParameters == null
                                    ? new PageParameters(getPage().getPageParameters())
                                              .set(dayParameter, date.toString())
                                    : dayParameters.apply(day);
                            Class<? extends Page> destination = dayPage == null ? getPage().getClass() : dayPage;
                            body = new BookmarkablePageLink<Void>("body", destination, parameters);
                            body.add(new StyledBehavior("sw-month-view-day-link"));
                        } else {
                            body = new WebMarkupContainer("body") {
                                @Override
                                protected void onComponentTag(ComponentTag tag) {
                                    super.onComponentTag(tag);
                                    tag.setName("div");
                                }
                            };
                            body.add(new StyledBehavior("sw-month-view-day-body"));
                        }
                        body.setVisible(!hidden);
                        if (day.today())
                            body.add(AttributeModifier.replace("aria-current", "date"));
                        body.add(new Label("number", day.number()), new Label("label", day.label()),
                                hidden ? new Label(CONTENT_ID, "").setVisible(false)
                                       : newDayContent(CONTENT_ID, Model.of(day)));
                        cell.add(body);
                    }
                });
            }
        });
    }

    protected Component newDayContent(String id, IModel<Day> day) {
        return content == null ? new Label(id, "").setVisible(false)
                               : Objects.requireNonNull(content.apply(id, day.getObject()));
    }

    public SwMonthView setDayContent(SerializableBiFunction<String, Day, Component> value) {
        content = value;
        return this;
    }

    public SwMonthView setDayParameter(String value) {
        dayParameter = Objects.requireNonNull(value);
        return this;
    }

    public SwMonthView setDayPage(Class<? extends Page> page, SerializableFunction<Day, PageParameters> parameters) {
        dayPage = Objects.requireNonNull(page);
        dayParameters = Objects.requireNonNull(parameters);
        return this;
    }

    public SwMonthView setMonthParameter(String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("month parameter is required");
        monthParameter = value;
        return this;
    }

    public SwMonthView setToday(LocalDate value) {
        return setToday(Model.of(value));
    }

    public SwMonthView setToday(IModel<LocalDate> value) {
        today = Objects.requireNonNull(value);
        return this;
    }

    public SwMonthView setCalendarLocale(Locale value) {
        locale = value;
        return this;
    }

    public SwMonthView setWeekStartsOn(DayOfWeek value) {
        weekStartsOn = value;
        return this;
    }

    public SwMonthView setShowWeekNumber(boolean value) {
        weekNumbers = value;
        return this;
    }

    public SwMonthView setShowOutsideDays(boolean value) {
        outsideDays = value;
        return this;
    }

    public SwMonthView setNavSize(NavSize value) {
        navSize = Objects.requireNonNull(value);
        return this;
    }

    private Locale actualLocale() {
        return locale == null ? getLocale() : locale;
    }

    private LocalDate today() {
        return today.getObject() == null ? LocalDate.now() : today.getObject();
    }

    private WeekFields weekFields() {
        var rules = WeekFields.of(actualLocale());
        return WeekFields.of(firstDay(), rules.getMinimalDaysInFirstWeek());
    }

    private DayOfWeek firstDay() {
        return weekStartsOn == null ? WeekFields.of(actualLocale()).getFirstDayOfWeek() : weekStartsOn;
    }

    private YearMonth shownMonth() {
        if (rendered == null) {
            rendered = month == null ? null : month.getObject();
            if (rendered == null)
                try {
                    rendered = YearMonth.parse(getPage().getPageParameters().get(monthParameter).toString(""));
                    if (rendered.getYear() < 1 || rendered.getYear() > 9999)
                        rendered = null;
                } catch (DateTimeParseException ignored) {
                }
            if (rendered == null)
                rendered = YearMonth.from(today());
            if (rendered.getYear() < 1 || rendered.getYear() > 9999)
                throw new IllegalArgumentException("Month view supports years 1–9999");
        }
        return rendered;
    }

    private List<List<LocalDate>> weeks() {
        LocalDate first = shownMonth().atDay(1);
        int offset = Math.floorMod(first.getDayOfWeek().getValue() - firstDay().getValue(), 7);
        LocalDate start = first.minusDays(offset);
        int count = (offset + shownMonth().lengthOfMonth() + 6) / 7;
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(
                        w -> java.util.stream.IntStream.range(0, 7).mapToObj(d -> start.plusDays(w * 7L + d)).toList())
                .toList();
    }

    private WebMarkupContainer navigation(String id, int offset) {
        var link = new WebMarkupContainer(id);
        link.add(AttributeModifier.replace("role", "link"));
        IModel<Boolean> enabled = () -> {
            int year = shownMonth().plusMonths(offset).getYear();
            return year >= 1 && year <= 9999;
        };
        link.add(AttributeModifier.replace("href",
                (IModel<String>) ()
                        -> enabled.getObject()
                        ? urlFor(getPage().getClass(),
                                  new PageParameters(getPage().getPageParameters())
                                          .set(monthParameter, shownMonth().plusMonths(offset).toString()))
                                  .toString()
                        : null));
        link.add(
                AttributeModifier.replace("aria-disabled", (IModel<String>) () -> enabled.getObject() ? null : "true"));
        link.add(new StyledBehavior("sw-month-view-nav-button"),
                AttributeModifier.replace("aria-label",
                        new StringResourceModel(offset < 0 ? "SwMonthView.previous" : "SwMonthView.next", this)
                                .setDefaultValue(offset < 0 ? "Go to the previous month" : "Go to the next month")));
        return link;
    }

    @Override
    protected void onDetach() {
        rendered = null;
        if (month != null)
            month.detach();
        today.detach();
        super.onDetach();
    }
}

package dev.jbaby.shadwick.components.form;

import org.apache.wicket.ConverterLocator;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converter.LocalDateConverter;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeFieldsTest {
    enum Kind {
        DATE("date", "2028-02-29", "2026-12-24", LocalDate.of(2026, 12, 24)),
        TIME("time", "10:30", "14:05:30.125", LocalTime.of(14, 5, 30, 125_000_000)),
        DATE_TIME("datetime-local", "2028-02-29T10:30", "2026-12-24T14:05", LocalDateTime.of(2026, 12, 24, 14, 5)),
        MONTH("month", "2028-02", "2026-12", "2026-12"),
        WEEK("week", "2026-W53", "2027-W01", "2027-W01");

        final String inputType;
        final String initial;
        final String submitted;
        final Object converted;

        Kind(String inputType, String initial, String submitted, Object converted) {
            this.inputType = inputType;
            this.initial = initial;
            this.submitted = submitted;
            this.converted = converted;
        }
    }

    private WicketTester tester;

    @BeforeEach
    void setUp() { tester = new WicketTester(); }

    @AfterEach
    void tearDown() {
        if (tester.getLastResponse() != null) {
            assertThat(tester.getLastResponseAsString()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        }
        tester.destroy();
    }

    private BaseSwField<?, ?, ?> field(Kind kind) {
        return switch (kind) {
            case DATE -> new SwDateField("value", "Date", Model.of(LocalDate.parse(kind.initial)));
            case TIME -> new SwTimeField("value", "Time", Model.of(LocalTime.parse(kind.initial)));
            case DATE_TIME -> new SwDateTimeLocalField("value", "Local date/time", Model.of(LocalDateTime.parse(kind.initial)));
            case MONTH -> new SwMonthField("value", "Month", Model.of(kind.initial));
            case WEEK -> new SwWeekField("value", "Week", Model.of(kind.initial));
        };
    }

    private FormTester form(BaseSwField<?, ?, ?> field) {
        Form<Void> form = new Form<>("form");
        form.add(field);
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='value'></div></form>"));
        return tester.newFormTester("form");
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void valuesRenderInHtmlFormatInEverySessionLocale(Kind kind) {
        for (Locale locale : List.of(Locale.US, Locale.GERMANY, Locale.forLanguageTag("ar-EG"))) {
            tester.getSession().setLocale(locale);
            tester.startComponentInPage(field(kind).setDescription("Help <text>"));
            var input = tester.getTagByWicketId("input");
            assertThat(input.getAttribute("type")).isEqualTo(kind.inputType);
            assertThat(input.getAttribute("value")).isEqualTo(kind.initial);
            assertThat(input.getAttribute("class")).isEqualTo("sw-input");
            assertThat(tester.getTagByWicketId("label").getAttribute("for")).isEqualTo(input.getAttribute("id"));
            assertThat(input.getAttribute("aria-describedby")).contains(tester.getTagByWicketId("description").getAttribute("id"));
            assertThat(tester.getTagByWicketId("description").getValue()).isEqualTo("Help &lt;text&gt;");
        }
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void browserValuesConvertAndUpdateModelsWithoutLocaleChanges(Kind kind) {
        tester.getSession().setLocale(Locale.GERMANY);
        var field = field(kind);
        FormTester form = form(field);
        form.setValue("value:input", kind.submitted);
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(field.getModelObject()).isEqualTo(kind.converted);
        assertThat(tester.getSession().getLocale()).isEqualTo(Locale.GERMANY);
    }

    static Stream<Arguments> invalidValues() {
        return Stream.of(
                Arguments.of(Kind.DATE, "2027-02-29"), Arguments.of(Kind.DATE, "2028-04-31"),
                Arguments.of(Kind.DATE, "24.12.2026"), Arguments.of(Kind.DATE, "0000-01-01"),
                Arguments.of(Kind.DATE, "2026-12-24Z"), Arguments.of(Kind.DATE, "2026-1-01"),
                Arguments.of(Kind.TIME, "24:00"), Arguments.of(Kind.TIME, "12:60"),
                Arguments.of(Kind.TIME, "12:00:60"), Arguments.of(Kind.TIME, "10:30Z"),
                Arguments.of(Kind.TIME, "10:30:00.1234"), Arguments.of(Kind.TIME, "9:05"),
                Arguments.of(Kind.DATE_TIME, "2027-02-29T12:00"), Arguments.of(Kind.DATE_TIME, "2026-12-24T12:00Z"),
                Arguments.of(Kind.DATE_TIME, "2026-12-24T12:00+01:00"), Arguments.of(Kind.DATE_TIME, "2026-12-24T24:00"),
                Arguments.of(Kind.MONTH, "2026-13"), Arguments.of(Kind.MONTH, "2026-00"),
                Arguments.of(Kind.MONTH, "0000-01"), Arguments.of(Kind.MONTH, "2026-1"),
                Arguments.of(Kind.WEEK, "2021-W53"), Arguments.of(Kind.WEEK, "2026-W00"),
                Arguments.of(Kind.WEEK, "2026-W54"), Arguments.of(Kind.WEEK, "0000-W01"),
                Arguments.of(Kind.WEEK, "2026-W1"), Arguments.of(Kind.WEEK, "<script>"));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidValues")
    void malformedAndImpossibleValuesAreRejectedOnTheServer(Kind kind, String invalid) {
        var field = field(kind);
        Object original = field.getModelObject();
        FormTester form = form(field);
        form.setValue("value:input", invalid);
        form.submit();
        assertThat(tester.getTagByWicketId("input").getAttribute("aria-invalid")).isEqualTo("true");
        assertThat(tester.getTagByWicketId("error").getValue()).contains("Expected");
        assertThat(field.getModelObject()).isEqualTo(original);
        assertThat(tester.getLastResponseAsString()).doesNotContain("<script>");
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void optionalBlankValuesClearTheirModels(Kind kind) {
        var field = field(kind);
        FormTester form = form(field);
        form.setValue("value:input", "");
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(field.getModelObject()).isNull();
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void requiredFieldsRejectBlankInput(Kind kind) {
        var field = field(kind).setRequired(true);
        Object original = field.getModelObject();
        FormTester form = form(field);
        form.setValue("value:input", "");
        form.submit();
        assertThat(tester.getTagByWicketId("error").getValue()).contains("required");
        assertThat(field.getModelObject()).isEqualTo(original);
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void disabledFieldsKeepTheirModelValues(Kind kind) {
        var field = field(kind);
        Object original = field.getModelObject();
        field.setEnabled(false);
        FormTester form = form(field);
        form.setValue("value:input", kind.submitted);
        form.submit();
        assertThat(tester.getTagByWicketId("input").getAttribute("disabled")).isNotNull();
        assertThat(field.getModelObject()).isEqualTo(original);
    }

    @Test
    void timesOmitZeroSecondsAndTruncateNanosecondsToHtmlMilliseconds() {
        tester.startComponentInPage(new SwTimeField("value", Model.of(LocalTime.of(10, 30))));
        assertThat(tester.getTagByWicketId("input").getAttribute("value")).isEqualTo("10:30");
        tester.startComponentInPage(new SwTimeField("value", Model.of(LocalTime.of(10, 30, 15, 123_456_789))));
        assertThat(tester.getTagByWicketId("input").getAttribute("value")).isEqualTo("10:30:15.123");
        tester.startComponentInPage(new SwDateTimeLocalField("value", Model.of(LocalDateTime.of(2028, 2, 29, 10, 30, 0, 123_456_789))));
        assertThat(tester.getTagByWicketId("input").getAttribute("value")).isEqualTo("2028-02-29T10:30:00.123");
    }

    @Test
    void aSpaceSeparatedLocalDateTimeNormalizesWithoutAddingATimezone() {
        var field = new SwDateTimeLocalField("value", "Appointment", new Model<LocalDateTime>());
        FormTester form = form(field);
        form.setValue("value:input", "2026-03-29 02:30");
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(field.getModelObject()).isEqualTo(LocalDateTime.of(2026, 3, 29, 2, 30));
        assertThat(tester.getTagByWicketId("input").getAttribute("value")).isEqualTo("2026-03-29T02:30");
    }

    @Test
    void nativeConvertersOverrideOnlyTheirOwnField() {
        LocalDateConverter applicationConverter = new LocalDateConverter() {
            @Override
            protected DateTimeFormatter getDateTimeFormatter() { return DateTimeFormatter.ofPattern("dd.MM.uuuu"); }
        };
        ((ConverterLocator) tester.getApplication().getConverterLocator()).set(LocalDate.class, applicationConverter);
        tester.startComponentInPage(new SwTextField<>("value", Model.of(LocalDate.of(2026, 12, 24))));
        assertThat(tester.getTagByWicketId("input").getAttribute("value")).isEqualTo("24.12.2026");
        tester.startComponentInPage(new SwDateField("value", Model.of(LocalDate.of(2026, 12, 24))));
        assertThat(tester.getTagByWicketId("input").getAttribute("value")).isEqualTo("2026-12-24");
        assertThat(tester.getApplication().getConverterLocator().getConverter(LocalDate.class)).isSameAs(applicationConverter);
    }

    @Test
    void convertersSurviveSerializationAndPreserveIsoWeekBoundaries() throws Exception {
        List<IConverter<?>> converters = List.of(Html5Converters.date(), Html5Converters.time(), Html5Converters.dateTimeLocal(),
                Html5Converters.month(), Html5Converters.week());
        for (Kind kind : Kind.values()) {
            IConverter<?> converter = converters.get(kind.ordinal());
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(converter); }
            try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                IConverter<?> restored = (IConverter<?>) input.readObject();
                assertThat(restored.convertToObject(kind.submitted, Locale.GERMANY)).isEqualTo(kind.converted);
            }
        }
        assertThat(Html5Converters.week().convertToObject("2020-W53", Locale.US)).isEqualTo("2020-W53");
        assertThat(Html5Converters.week().convertToObject("2021-W01", Locale.US)).isEqualTo("2021-W01");
        tester.startComponentInPage(new SwWeekField("value", Model.of("2020-W53")));
    }

    public static class Schedule implements Serializable {
        public LocalDate date;
        public LocalTime time;
        public LocalDateTime local;
        public String month;
        public String week;
    }

    @Test
    void emptyCompoundModelsSupplyValuesForAllFiveFieldTypes() {
        Schedule schedule = new Schedule();
        Form<Schedule> form = new Form<>("form", new CompoundPropertyModel<>(schedule));
        form.add(new SwDateField("date", "Date"), new SwTimeField("time", "Time"),
                new SwDateTimeLocalField("local", "Local date/time"), new SwMonthField("month", "Month"),
                new SwWeekField("week", "Week"));
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='date'></div><div wicket:id='time'></div>"
                + "<div wicket:id='local'></div><div wicket:id='month'></div><div wicket:id='week'></div></form>"));
        FormTester ft = tester.newFormTester("form");
        ft.setValue("date:input", "2028-02-29");
        ft.setValue("time:input", "09:30");
        ft.setValue("local:input", "2028-02-29T09:30");
        ft.setValue("month:input", "2028-02");
        ft.setValue("week:input", "2020-W53");
        ft.submit();
        tester.assertNoErrorMessage();
        assertThat(schedule.date).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(schedule.time).isEqualTo(LocalTime.of(9, 30));
        assertThat(schedule.local).isEqualTo(LocalDateTime.of(2028, 2, 29, 9, 30));
        assertThat(schedule.month).isEqualTo("2028-02");
        assertThat(schedule.week).isEqualTo("2020-W53");
    }
}

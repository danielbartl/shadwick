package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.behaviors.CalendarBehavior;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.*;
import java.util.Locale;
import static org.assertj.core.api.Assertions.*;

class CalendarTest {
    enum Kind { CALENDAR, PICKER, RANGE, RANGE_PICKER }
    private WicketTester tester;
    private static final LocalDate START=LocalDate.of(2026,11,10), END=LocalDate.of(2026,11,12);
    @BeforeEach void setup(){tester=new WicketTester();}
    @AfterEach void cleanup(){tester.destroy();}
    private boolean range(Kind kind){return kind==Kind.RANGE||kind==Kind.RANGE_PICKER;}
    private BaseSwCalendar<?,?,?> field(Kind kind){return switch(kind){
        case CALENDAR->new SwCalendar("choice","Travel date",Model.of(START));
        case PICKER->new SwDatePicker("choice","Travel date",Model.of(START));
        case RANGE->new SwRangeCalendar("choice","Stay",Model.of(new DateRange(START,END)));
        case RANGE_PICKER->new SwDateRangePicker("choice","Stay",Model.of(new DateRange(START,END)));
    };}
    private void render(BaseSwCalendar<?,?,?> field){Form<Void> form=new Form<>("form");form.add(field);tester.startComponentInPage(form,Markup.of("<form wicket:id='form'><div wicket:id='choice'></div></form>"));}
    private void submit(Kind kind,String from,String to){var form=tester.newFormTester("form");form.setValue(range(kind)?"choice:input:from":"choice:input",from);if(range(kind))form.setValue("choice:input:to",to);form.submit();}
    @ParameterizedTest @EnumSource(Kind.class)
    void nativeMarkupHasIsoValuesNamesAndFeedback(Kind kind){
        var field=field(kind).setDescription("Choose available dates").setCaption(CalendarBehavior.Caption.DROPDOWN).setShowWeekNumber(true).setNumberOfMonths(2).setCalendarLocale(Locale.GERMANY).setRequired(true);render(field);
        String html=tester.getLastResponseAsString();assertThat(html).contains("sw-calendar","2026-11-10","data-locale=\"de-DE\"","data-week-starts-on=\"1\"","data-first-week-min-days=\"4\"").doesNotContainPattern("\\sstyle=").doesNotContain("onclick=");
        String id=range(kind)?tester.getTagByWicketId("from").getAttribute("id"):tester.getTagByWicketId("input").getAttribute("id");assertThat(tester.getTagByWicketId("label").getAttribute("for")).isEqualTo(id);
        if(range(kind))assertThat(tester.getTagByWicketId("to").getAttribute("aria-describedby")).contains(tester.getTagByWicketId("error").getAttribute("id"));
    }
    @ParameterizedTest @EnumSource(Kind.class)
    void normalSubmissionUpdatesTheActualModel(Kind kind){var field=field(kind);render(field);submit(kind,"2026-11-16","2026-11-18");tester.assertNoErrorMessage();assertThat(field.getDefaultModelObject()).isEqualTo(range(kind)?new DateRange(LocalDate.of(2026,11,16),LocalDate.of(2026,11,18)):LocalDate.of(2026,11,16));}
    @ParameterizedTest @EnumSource(Kind.class)
    void boundsAndDisabledWeekdaysAreEnforcedWithoutJavascript(Kind kind){
        for(String date:new String[]{"2025-11-10","2026-11-14","2026-12-25"}){
            var field=field(kind).setMin(LocalDate.of(2026,1,1)).setMax(LocalDate.of(2026,12,31)).setDisabledDates("sat,sun,2026-12-24..2026-12-31");render(field);submit(kind,date,date);
            assertThat(field.getInput().hasErrorMessage()).isTrue();assertThat(field.getDefaultModelObject()).isEqualTo(range(kind)?new DateRange(START,END):START);
        }
    }
    @ParameterizedTest @EnumSource(Kind.class)
    void requiredFieldsRejectEmptyAndMalformedDatesAtomically(Kind kind){
        for(String date:new String[]{"","2026-02-30","0000-01-01","10000-01-01"}){
            var field=field(kind).setRequired(true);render(field);submit(kind,date,date);
            assertThat(tester.getLastResponseAsString()).contains("aria-invalid=\"true\"");assertThat(field.getDefaultModelObject()).isEqualTo(range(kind)?new DateRange(START,END):START);
        }
    }
    @ParameterizedTest @EnumSource(Kind.class)
    void readOnlyRejectsChangedValues(Kind kind){var field=field(kind).setReadOnly(true);render(field);submit(kind,"2026-11-16","2026-11-18");assertThat(field.getInput().hasErrorMessage()).isTrue();assertThat(field.getDefaultModelObject()).isEqualTo(range(kind)?new DateRange(START,END):START);}
    @ParameterizedTest @EnumSource(value=Kind.class,names={"RANGE","RANGE_PICKER"})
    void rangeRejectsReversalAndUnavailableInteriorAndAllowsOpenEnds(Kind kind){
        var field=field(kind).setDisabledDates("2026-11-11");render(field);submit(kind,"2026-11-10","2026-11-12");assertThat(field.getInput().hasErrorMessage()).isTrue();
        field=field(kind);render(field);submit(kind,"2026-11-12","2026-11-10");assertThat(field.getInput().hasErrorMessage()).isTrue();
        field=field(kind);render(field);submit(kind,"2026-11-16","");tester.assertNoErrorMessage();assertThat(field.getDefaultModelObject()).isEqualTo(new DateRange(LocalDate.of(2026,11,16),null));
    }
    @Test void disabledRulesAreStrictAndLargeRangesDoNotRequireDailyIteration(){
        assertThatIllegalArgumentException().isThrownBy(()->new DisabledDates("2026-02-30"));assertThatIllegalArgumentException().isThrownBy(()->new DisabledDates("holiday"));
        assertThat(new DisabledDates("sun").intersects(new DateRange(LocalDate.of(1,1,1),LocalDate.of(9999,12,31)))).isTrue();
        assertThatIllegalArgumentException().isThrownBy(()->new CalendarBehavior().setNumberOfMonths(13));
    }
    @Test void leapDayAndEarlyYearsRemainDateOnly(){
        var field=field(Kind.PICKER);render(field);submit(Kind.PICKER,"0004-02-29",null);tester.assertNoErrorMessage();assertThat(field.getDefaultModelObject()).isEqualTo(LocalDate.of(4,2,29));
    }
    @Test void immediateRangeAjaxProcessesBothCurrentInputsAtomically(){
        var field=new SwRangeCalendar("choice","Stay",Model.of(new DateRange(START,END)));
        field.add(new org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior("change"){
            @Override protected void onUpdate(org.apache.wicket.ajax.AjaxRequestTarget target){}
        });render(field);
        tester.getRequest().getPostParameters().setParameterValue(field.getInput().getFrom().getInputName(),"2026-11-16");
        tester.getRequest().getPostParameters().setParameterValue(field.getInput().getTo().getInputName(),"2026-11-18");
        tester.executeAjaxEvent(field.getInput(),"change");
        assertThat(field.getModelObject()).isEqualTo(new DateRange(LocalDate.of(2026,11,16),LocalDate.of(2026,11,18)));
        tester.getRequest().getPostParameters().setParameterValue(field.getInput().getFrom().getInputName(),"2026-02-30");
        tester.getRequest().getPostParameters().setParameterValue(field.getInput().getTo().getInputName(),"2026-11-20");
        tester.executeAjaxEvent(field.getInput(),"change");assertThat(field.getInput().isValid()).isFalse();
        assertThat(field.getModelObject()).isEqualTo(new DateRange(LocalDate.of(2026,11,16),LocalDate.of(2026,11,18)));
    }

    @ParameterizedTest @EnumSource(value=Kind.class,names={"PICKER","RANGE_PICKER"})
    void pickerServerTextPreservesValidRawDatesWhenAnotherFieldFails(Kind kind){
        var field=field(kind);var required=new org.apache.wicket.markup.html.form.TextField<String>("required",Model.of());required.setRequired(true);
        Form<Void> form=new Form<>("form");form.add(field,required);tester.startComponentInPage(form,Markup.of("<form wicket:id='form'><div wicket:id='choice'></div><input wicket:id='required'></form>"));
        submit(kind,"2026-11-16","2026-11-18");assertThat(required.hasErrorMessage()).isTrue();
        assertThat(field.getDefaultModelObject()).isEqualTo(range(kind)?new DateRange(START,END):START);
        assertThat(tester.getTagByWicketId("value").getValue()).contains("16");
        if(range(kind))assertThat(tester.getTagByWicketId("value").getValue()).contains("18");
    }

}

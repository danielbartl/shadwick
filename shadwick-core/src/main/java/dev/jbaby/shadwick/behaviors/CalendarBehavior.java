package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.components.form.DateRange;
import dev.jbaby.shadwick.components.form.DisabledDates;
import dev.jbaby.shadwick.components.form.SwCalendar;
import dev.jbaby.shadwick.util.LocalizedText;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import java.time.*;
import java.time.temporal.WeekFields;
import java.util.*;

/** Month grid over native date inputs; field components also enforce its date rules on the server. */
public class CalendarBehavior extends StyledBehavior {
    public enum Caption { LABEL, DROPDOWN, DROPDOWN_MONTHS, DROPDOWN_YEARS }
    public static final JavaScriptResourceReference CALENDAR_JS=new JavaScriptResourceReference(Shadwick.class,"js/calendar.js");
    private Caption caption=Caption.LABEL;
    private int months=1;
    private DayOfWeek weekStartsOn;
    private boolean outsideDays=true,weekNumbers,readOnly;
    private Locale locale;
    private IModel<YearMonth> defaultMonth,startMonth,endMonth;
    private IModel<LocalDate> today,min,max;
    private IModel<String> disabledDates=Model.of("");
    public CalendarBehavior(){super("sw-calendar");}
    public CalendarBehavior setCaption(Caption value){caption=Objects.requireNonNull(value);return this;}
    public CalendarBehavior setNumberOfMonths(int value){if(value<1||value>12)throw new IllegalArgumentException("months must be 1–12");months=value;return this;}
    public CalendarBehavior setShowOutsideDays(boolean value){outsideDays=value;return this;}
    public CalendarBehavior setShowWeekNumber(boolean value){weekNumbers=value;return this;}
    public CalendarBehavior setWeekStartsOn(DayOfWeek value){weekStartsOn=value;return this;}
    public CalendarBehavior setLocale(Locale value){locale=value;return this;}
    public CalendarBehavior setReadOnly(boolean value){readOnly=value;return this;}
    public boolean isReadOnly(){return readOnly;}
    public CalendarBehavior setDefaultMonth(YearMonth value){return setDefaultMonth(Model.of(value));}
    public CalendarBehavior setDefaultMonth(IModel<YearMonth> value){defaultMonth=value;return this;}
    public CalendarBehavior setStartMonth(YearMonth value){return setStartMonth(Model.of(value));}
    public CalendarBehavior setStartMonth(IModel<YearMonth> value){startMonth=value;return this;}
    public CalendarBehavior setEndMonth(YearMonth value){return setEndMonth(Model.of(value));}
    public CalendarBehavior setEndMonth(IModel<YearMonth> value){endMonth=value;return this;}
    public CalendarBehavior setToday(LocalDate value){return setToday(Model.of(value));}
    public CalendarBehavior setToday(IModel<LocalDate> value){today=value;return this;}
    public CalendarBehavior setMin(LocalDate value){return setMin(Model.of(value));}
    public CalendarBehavior setMin(IModel<LocalDate> value){min=value;return this;}
    public CalendarBehavior setMax(LocalDate value){return setMax(Model.of(value));}
    public CalendarBehavior setMax(IModel<LocalDate> value){max=value;return this;}
    public CalendarBehavior setDisabledDates(String value){new DisabledDates(value);return setDisabledDates(Model.of(value));}
    public CalendarBehavior setDisabledDates(IModel<String> value){disabledDates=value;return this;}
    public Locale locale(Component component){return locale==null?component.getLocale():locale;}
    private static <T> T value(IModel<T> model){return model==null?null:model.getObject();}
    public LocalDate minimum(){return value(min);}
    public LocalDate maximum(){return value(max);}
    public boolean allows(LocalDate date){
        return date==null || date.getYear()>=1&&date.getYear()<=9999
            &&(minimum()==null||!date.isBefore(minimum()))&&(maximum()==null||!date.isAfter(maximum()))
            &&!new DisabledDates(value(disabledDates)).contains(date);
    }
    public boolean allows(DateRange range){return range==null || allows(range.from())&&allows(range.to())&&!new DisabledDates(value(disabledDates)).intersects(range);}
    @Override public void onComponentTag(Component component,ComponentTag tag){
        super.onComponentTag(component,tag);tag.put("role","group");
        for(Object date:Arrays.asList(value(defaultMonth),value(startMonth),value(endMonth),value(today),minimum(),maximum())){
            int year=date instanceof YearMonth month?month.getYear():date instanceof LocalDate day?day.getYear():1;
            if(year<1||year>9999)throw new IllegalArgumentException("Calendar dates use years 1–9999");
        }
        Locale actual=locale(component);WeekFields weeks=WeekFields.of(actual);
        tag.put("data-locale",actual.toLanguageTag());
        tag.put("data-week-starts-on",(weekStartsOn==null?weeks.getFirstDayOfWeek():weekStartsOn).getValue()%7);
        tag.put("data-first-week-min-days",weeks.getMinimalDaysInFirstWeek());
        tag.put("data-caption",caption.name().toLowerCase(Locale.ROOT).replace('_','-'));
        tag.put("data-number-of-months",months);tag.put("data-show-outside-days",String.valueOf(outsideDays));tag.put("data-show-week-number",String.valueOf(weekNumbers));
        put(tag,"data-default-month",value(defaultMonth));put(tag,"data-start-month",value(startMonth));put(tag,"data-end-month",value(endMonth));put(tag,"data-today",value(today));
        String rules=value(disabledDates);new DisabledDates(rules);put(tag,"data-disabled-dates",rules);
        if(minimum()!=null&&maximum()!=null&&minimum().isAfter(maximum()))throw new IllegalArgumentException("minimum date must not exceed maximum");
        if(value(startMonth)!=null&&value(endMonth)!=null&&value(startMonth).isAfter(value(endMonth)))throw new IllegalArgumentException("start month must not exceed end month");
        for(String[] text:new String[][]{{"previous","Go to the previous month"},{"next","Go to the next month"},{"month","Month"},{"year","Year"},{"week-number","Week"},{"unavailable","Choose a range without unavailable dates."}})
            tag.put("data-"+text[0],LocalizedText.get(component,SwCalendar.class,"SwCalendar."+text[0],text[1]));
    }
    private static void put(ComponentTag tag,String name,Object value){if(value!=null)tag.put(name,value.toString());}
    @Override public void detach(Component component){for(IModel<?> model:Arrays.asList(defaultMonth,startMonth,endMonth,today,min,max,disabledDates))if(model!=null)model.detach();super.detach(component);}
    @Override public void renderHead(Component component,IHeaderResponse response){super.renderHead(component,response);response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));response.render(JavaScriptHeaderItem.forReference(CALENDAR_JS));}
}

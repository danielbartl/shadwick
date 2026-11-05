package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.behaviors.CalendarBehavior;
import dev.jbaby.shadwick.components.SwIcon;
import org.apache.wicket.*;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

/** Shared field composition for native date-backed calendars and popup pickers. */
public abstract class BaseSwCalendar<T,C extends FormComponent<T>,F extends BaseSwCalendar<T,C,F>> extends BaseSwField<T,C,F> {
    protected final CalendarBehavior calendar=new CalendarBehavior();
    private final boolean picker;
    private IModel<String> placeholder;
    private FormatStyle format=FormatStyle.LONG;
    protected BaseSwCalendar(String id,IModel<String> label,IModel<T> model,IModel<String> description,boolean picker){super(id,label,model,description);this.picker=picker;}
    @SuppressWarnings("unchecked") private F field(){return (F)this;}
    public F setCaption(CalendarBehavior.Caption value){calendar.setCaption(value);return field();}
    public F setNumberOfMonths(int value){calendar.setNumberOfMonths(value);return field();}
    public F setShowOutsideDays(boolean value){calendar.setShowOutsideDays(value);return field();}
    public F setShowWeekNumber(boolean value){calendar.setShowWeekNumber(value);return field();}
    public F setWeekStartsOn(DayOfWeek value){calendar.setWeekStartsOn(value);return field();}
    public F setCalendarLocale(Locale value){calendar.setLocale(value);return field();}
    public F setReadOnly(boolean value){calendar.setReadOnly(value);return field();}
    public F setDefaultMonth(YearMonth value){calendar.setDefaultMonth(value);return field();}
    public F setDefaultMonth(IModel<YearMonth> value){calendar.setDefaultMonth(value);return field();}
    public F setStartMonth(YearMonth value){calendar.setStartMonth(value);return field();}
    public F setStartMonth(IModel<YearMonth> value){calendar.setStartMonth(value);return field();}
    public F setEndMonth(YearMonth value){calendar.setEndMonth(value);return field();}
    public F setEndMonth(IModel<YearMonth> value){calendar.setEndMonth(value);return field();}
    public F setToday(LocalDate value){calendar.setToday(value);return field();}
    public F setToday(IModel<LocalDate> value){calendar.setToday(value);return field();}
    public F setMin(LocalDate value){calendar.setMin(value);return field();}
    public F setMin(IModel<LocalDate> value){calendar.setMin(value);return field();}
    public F setMax(LocalDate value){calendar.setMax(value);return field();}
    public F setMax(IModel<LocalDate> value){calendar.setMax(value);return field();}
    public F setDisabledDates(String value){calendar.setDisabledDates(value);return field();}
    public F setDisabledDates(IModel<String> value){calendar.setDisabledDates(value);return field();}
    public F setFormat(FormatStyle value){format=Objects.requireNonNull(value);return field();}
    @Override public F setPlaceholder(String value){return setPlaceholder(Model.of(value));}
    @Override public F setPlaceholder(IModel<String> value){placeholder=value;return field();}
    private String placeholder(){return placeholder==null?getString("SwDatePicker.placeholder",null,"Pick a date"):Objects.toString(placeholder.getObject(),"");}
    private String formatted(){
        Object selected=getModelObject();
        if(getInput() instanceof RangeDateInput range)selected=new DateRange(shownDate(range.getFrom()),shownDate(range.getTo()));
        else if(getInput() instanceof Html5TextField<?> input){
            @SuppressWarnings("unchecked") Html5TextField<LocalDate> date=(Html5TextField<LocalDate>)input;
            selected=shownDate(date);
        }
        DateTimeFormatter formatter=DateTimeFormatter.ofLocalizedDate(format).withLocale(calendar.locale(this));
        if(selected instanceof LocalDate date)return formatter.format(date);
        if(selected instanceof DateRange range){
            String from=range.from()==null?null:formatter.format(range.from()),to=range.to()==null?null:formatter.format(range.to());
            if(from!=null&&to!=null)return getString("SwDatePicker.range",null,"{0} – {1}").replace("{0}",from).replace("{1}",to);
            return from==null?to:from;
        }
        return null;
    }
    private LocalDate shownDate(Html5TextField<LocalDate> input){
        if(!input.hasRawInput())return input.getModelObject();
        String value=input.getRawInput();if(value==null||value.isBlank())return null;
        try{return Html5Converters.date().convertToObject(value,getLocale());}
        catch(org.apache.wicket.util.convert.ConversionException invalid){return null;}
    }
    @Override protected void onInitialize(){
        super.onInitialize();get("label").setOutputMarkupId(true);
        if(getInput() instanceof RangeDateInput)getInput().add(new Behavior(){
            @Override public void onComponentTag(Component component,ComponentTag tag){tag.remove("aria-required");tag.put("role","group");tag.put("aria-labelledby",get("label").getMarkupId());}
        });
        var drawing=new TransparentWebMarkupContainer("calendar");drawing.add(calendar);
        drawing.add(AttributeModifier.replace("aria-labelledby",(IModel<String>)()->get("label").getMarkupId()));
        drawing.add(AttributeModifier.replace("aria-describedby",(IModel<String>)()->getDescribedByMarkupIds()));
        add(drawing);
        var wrapper=new TransparentWebMarkupContainer("picker");wrapper.setRenderBodyOnly(!picker);add(wrapper);
        var popup=new TransparentWebMarkupContainer("popup");popup.setRenderBodyOnly(!picker);popup.setOutputMarkupId(picker);
        popup.add(AttributeModifier.replace("aria-label",(IModel<String>)()->getString("SwDatePicker.dialog",null,"Choose a date")));add(popup);
        var trigger=new WebMarkupContainer("trigger");trigger.setVisible(picker);trigger.setOutputMarkupId(true);
        trigger.add(AttributeModifier.replace("popovertarget",(IModel<String>)popup::getMarkupId));
        trigger.add(AttributeModifier.replace("aria-controls",(IModel<String>)popup::getMarkupId));
        trigger.add(AttributeModifier.replace("disabled",(IModel<String>)()->isEnabledInHierarchy()?null:"disabled"));
        trigger.add(new SwIcon("icon","calendar"));
        var value=new Label("value",(IModel<String>)()->formatted()==null?placeholder():formatted());value.setOutputMarkupId(true);trigger.add(value);
        trigger.add(AttributeModifier.replace("aria-labelledby",(IModel<String>)()->get("label").getMarkupId()+" "+value.getMarkupId()));
        trigger.add(AttributeModifier.replace("aria-describedby",(IModel<String>)this::getDescribedByMarkupIds));
        trigger.add(AttributeModifier.replace("aria-invalid",(IModel<String>)()->String.valueOf(getInput().hasErrorMessage())));add(trigger);
        wrapper.add(new Behavior(){@Override public void onComponentTag(Component component,ComponentTag tag){if(picker){tag.put("data-placeholder",placeholder());tag.put("data-date-format",format.name().toLowerCase(Locale.ROOT));tag.put("data-range-format",getString("SwDatePicker.range",null,"{0} – {1}"));}}});
    }
    /** The popup trigger, useful for explicit Ajax focus requests. Hidden for inline calendars. */
    public WebMarkupContainer getTrigger(){return (WebMarkupContainer)get("trigger");}
    protected Html5TextField<LocalDate> dateInput(String id,IModel<LocalDate> model){
        var input=new Html5TextField<LocalDate>(id,model,LocalDate.class,"date",Html5Converters.date());
        input.add(new Behavior(){@Override public void onComponentTag(Component component,ComponentTag tag){
            if(calendar.minimum()!=null)tag.put("min",calendar.minimum().toString());if(calendar.maximum()!=null)tag.put("max",calendar.maximum().toString());
            if(calendar.isReadOnly())tag.put("readonly","readonly");
        }});
        return input;
    }
    @Override protected void onDetach(){if(placeholder!=null)placeholder.detach();super.onDetach();}
}

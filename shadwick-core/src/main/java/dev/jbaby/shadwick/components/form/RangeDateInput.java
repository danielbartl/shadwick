package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.behaviors.CalendarBehavior;
import dev.jbaby.shadwick.util.SerializableSupplier;
import org.apache.wicket.*;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.request.cycle.RequestCycle;
import java.util.Set;
import java.util.HashSet;
import org.apache.wicket.model.*;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.ValidationError;
import java.time.LocalDate;
import java.util.Objects;

/** Native date pair converted atomically, with Oat DateRange's open/required/order semantics. */
public class RangeDateInput extends FormComponentPanel<DateRange> {
    private static final MetaDataKey<Set<FormComponent<?>>> VALIDATED=new MetaDataKey<>(){};
    private final Html5TextField<LocalDate> from,to;
    RangeDateInput(String id,IModel<DateRange> model,CalendarBehavior rules,SerializableSupplier<String> describedBy){
        super(id,model);setOutputMarkupId(true);
        from=date("from",true,rules,describedBy);to=date("to",false,rules,describedBy);add(from,to);
        add(validatable->{DateRange range=validatable.getValue();
            if(range!=null&&range.from()!=null&&range.to()!=null&&range.to().isBefore(range.from()))validatable.error(new ValidationError().addKey("SwCalendar.order"));
            else if(!rules.allows(range))validatable.error(new ValidationError().addKey("SwCalendar.unavailable"));
            if(rules.isReadOnly()&&!Objects.equals(range,getModelObject()))validatable.error(new ValidationError().addKey("SwCalendar.readonly"));
        });
    }
    private Html5TextField<LocalDate> date(String id,boolean first,CalendarBehavior rules,SerializableSupplier<String> describedBy){
        var field=new Html5TextField<LocalDate>(id,new IModel<>(){
            public LocalDate getObject(){DateRange value=getModelObject();return value==null?null:first?value.from():value.to();}
            public void setObject(LocalDate value){}
        },LocalDate.class,"date",Html5Converters.date()){
            @Override public void validate(){
                super.validate();
                RequestCycle cycle=RequestCycle.get();Set<FormComponent<?>> validated=cycle.getMetaData(VALIDATED);
                if(validated==null){validated=new HashSet<>();cycle.setMetaData(VALIDATED,validated);}validated.add(this);
            }
        };field.setOutputMarkupId(true);
        field.setLabel(new StringResourceModel(first?"SwCalendar.from":"SwCalendar.to",this).setDefaultValue(first?"From":"To"));
        field.add(new Behavior(){@Override public void onComponentTag(Component component,ComponentTag tag){
            tag.put("aria-label",field.getLabel().getObject()+" "+RangeDateInput.this.getLabel().getObject());
            tag.put("aria-invalid",String.valueOf(RangeDateInput.this.hasErrorMessage()||field.hasErrorMessage()));
            if(describedBy.get()!=null)tag.put("aria-describedby",describedBy.get());
            if(RangeDateInput.this.isRequired())tag.put("aria-required","true");
            if(rules.minimum()!=null)tag.put("min",rules.minimum().toString());if(rules.maximum()!=null)tag.put("max",rules.maximum().toString());
            if(rules.isReadOnly())tag.put("readonly","readonly");
        }});
        return field;
    }
    public Html5TextField<LocalDate> getFrom(){return from;}
    public Html5TextField<LocalDate> getTo(){return to;}
    @Override public void validate(){
        // A field-level Ajax behavior validates the panel alone. Normal form processing
        // already visited its children; record that per request to avoid duplicate validators.
        for(FormComponent<?> child:new FormComponent<?>[]{from,to}){
            Set<FormComponent<?>> validated=RequestCycle.get().getMetaData(VALIDATED);
            if(validated==null||!validated.contains(child)){child.inputChanged();child.validate();}
        }
        super.validate();
    }
    @Override public void convertInput(){setConvertedInput(from.getConvertedInput()==null&&to.getConvertedInput()==null?null:new DateRange(from.getConvertedInput(),to.getConvertedInput()));}
    @Override public boolean checkRequired(){return !isRequired()||!Strings.isEmpty(from.getInput())&&!Strings.isEmpty(to.getInput());}
}

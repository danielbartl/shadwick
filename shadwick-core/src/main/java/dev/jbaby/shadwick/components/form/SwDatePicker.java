package dev.jbaby.shadwick.components.form;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.ValidationError;
import java.time.LocalDate;
import java.util.Objects;

/** Popup date picker, backed by native ISO date inputs. */
public class SwDatePicker extends BaseSwCalendar<LocalDate,Html5TextField<LocalDate>,SwDatePicker> {
    public SwDatePicker(String id){this(id,null,null,null);}
    public SwDatePicker(String id,IModel<LocalDate> model){this(id,null,model,null);}
    public SwDatePicker(String id,String label){this(id,Model.of(label),null,null);}
    public SwDatePicker(String id,String label,IModel<LocalDate> model){this(id,Model.of(label),model,null);}
    public SwDatePicker(String id,IModel<String> label,IModel<LocalDate> model){this(id,label,model,null);}
    public SwDatePicker(String id,IModel<String> label,IModel<LocalDate> model,IModel<String> description){super(id,label,model,description,true);}
    @Override protected Html5TextField<LocalDate> newInput(String id,IModel<LocalDate> model){
        var input=dateInput(id,model);
        input.add(validatable->{
            LocalDate date=validatable.getValue();
            if(!calendar.allows(date))validatable.error(new ValidationError().addKey("SwCalendar.unavailable"));
            if(calendar.isReadOnly()&&!Objects.equals(date,getModelObject()))validatable.error(new ValidationError().addKey("SwCalendar.readonly"));
        });
        return input;
    }
}

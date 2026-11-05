package dev.jbaby.shadwick.components.form;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.ValidationError;
import java.time.LocalDate;
import java.util.Objects;

/** Popup date-range picker, backed by native ISO date inputs. */
public class SwDateRangePicker extends BaseSwCalendar<DateRange,RangeDateInput,SwDateRangePicker> {
    public SwDateRangePicker(String id){this(id,null,null,null);}
    public SwDateRangePicker(String id,IModel<DateRange> model){this(id,null,model,null);}
    public SwDateRangePicker(String id,String label){this(id,Model.of(label),null,null);}
    public SwDateRangePicker(String id,String label,IModel<DateRange> model){this(id,Model.of(label),model,null);}
    public SwDateRangePicker(String id,IModel<String> label,IModel<DateRange> model){this(id,label,model,null);}
    public SwDateRangePicker(String id,IModel<String> label,IModel<DateRange> model,IModel<String> description){super(id,label,model,description,true);}
    @Override protected RangeDateInput newInput(String id,IModel<DateRange> model){
        return new RangeDateInput(id,model,calendar,this::getDescribedByMarkupIds);
    }
    @Override protected Component getLabelTarget(){return getInput().getFrom();}
}

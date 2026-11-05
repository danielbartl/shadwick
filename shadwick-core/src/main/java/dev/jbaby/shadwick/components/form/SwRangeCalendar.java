package dev.jbaby.shadwick.components.form;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.ValidationError;
import java.time.LocalDate;
import java.util.Objects;

/** Inline date-range calendar, backed by native ISO date inputs. */
public class SwRangeCalendar extends BaseSwCalendar<DateRange,RangeDateInput,SwRangeCalendar> {
    public SwRangeCalendar(String id){this(id,null,null,null);}
    public SwRangeCalendar(String id,IModel<DateRange> model){this(id,null,model,null);}
    public SwRangeCalendar(String id,String label){this(id,Model.of(label),null,null);}
    public SwRangeCalendar(String id,String label,IModel<DateRange> model){this(id,Model.of(label),model,null);}
    public SwRangeCalendar(String id,IModel<String> label,IModel<DateRange> model){this(id,label,model,null);}
    public SwRangeCalendar(String id,IModel<String> label,IModel<DateRange> model,IModel<String> description){super(id,label,model,description,false);}
    @Override protected RangeDateInput newInput(String id,IModel<DateRange> model){
        return new RangeDateInput(id,model,calendar,this::getDescribedByMarkupIds);
    }
    @Override protected Component getLabelTarget(){return getInput().getFrom();}
}

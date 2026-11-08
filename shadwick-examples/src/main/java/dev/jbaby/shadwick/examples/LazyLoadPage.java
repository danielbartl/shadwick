package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.AlertBehavior;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.SkeletonBehavior;
import dev.jbaby.shadwick.components.*;
import dev.jbaby.shadwick.components.form.SwNumberField;
import dev.jbaby.shadwick.util.SerializableConsumer;
import org.apache.wicket.Component;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;
import java.math.BigDecimal;

/** Deferred request work, application readiness and an application-owned recovery flow. */
public class LazyLoadPage extends BasePage {
    private final Label receipt = new Label("receipt", Model.of("Reports load after the page is shown."));
    private SwLazyLoadPanel<Component> queued, recoverable;
    private boolean queuedReady;
    private int generation=1, failuresRemaining=1;

    public LazyLoadPage(PageParameters parameters) {
        super(parameters); receipt.setOutputMarkupId(true); add(receipt);
        add(SwLazyLoadPanel.of("revenue", id->new ReportContent(id,"Revenue report")));
        queued=newQueuedPanel(); add(queued);
        recoverable=newRecoverablePanel(); add(recoverable);
        add(SwButton.onClick("ready", "Complete queued report", target->{
            queuedReady=true; say(target,"Queued report is ready; the shared timer will pick it up.");
        }).setVariant(ButtonBehavior.Variant.OUTLINE));
        add(SwButton.onClick("replace", "Replace queued panel", target->{
            queuedReady=false; generation++; queued=newQueuedPanel(); addOrReplace(queued); target.add(queued);
            say(target,"Replaced queued panel with generation "+generation+".");
        }).setVariant(ButtonBehavior.Variant.OUTLINE));
    }
    private SwLazyLoadPanel<Component> newQueuedPanel() {
        int current=generation;
        return new SwLazyLoadPanel<>("queued") {
            @Override protected boolean isContentReady(){return queuedReady;}
            @Override public Component getLazyLoadComponent(String id){return new ReportContent(id,"Queued report "+current);}
        }.setPlaceholder(SkeletonBehavior.Shape.BOX,1);
    }
    private SwLazyLoadPanel<Component> newRecoverablePanel() {
        return SwLazyLoadPanel.of("recoverable",id->{
            try {
                if(failuresRemaining>0){failuresRemaining--;throw new ReportUnavailable();}
                return new ReportContent(id,"Recovered report");
            } catch(ReportUnavailable expected) {
                return new RetryContent(id,target->{
                    recoverable=newRecoverablePanel();addOrReplace(recoverable);target.add(recoverable);
                    say(target,"Retrying the report in a fresh panel.");
                });
            }
        }).setPlaceholder(SkeletonBehavior.Shape.LINE,2);
    }
    private void say(AjaxRequestTarget target,String message){receipt.setDefaultModelObject(message);target.add(receipt);}
    private static final class ReportUnavailable extends RuntimeException {}
    public static class Report implements Serializable { public Integer seats=12; }

    /** Real normal/Ajax form controls are constructed only once this report loads. */
    public static class ReportContent extends Panel {
        public ReportContent(String id,String title) {
            super(id);add(new Label("title",title));
            var report=Model.of(new Report());
            var total=new Label("total",(IModel<String>)()->"Seats "+report.getObject().seats+"; projected total "+
                    new BigDecimal("125.50").multiply(BigDecimal.valueOf(report.getObject().seats)).toPlainString()+".");
            total.setOutputMarkupId(true);
            var form=new Form<Report>("editor",new CompoundPropertyModel<>(report));
            form.add(AttributeModifier.replace("aria-label",title+" editor"));
            form.add(new SwNumberField<Integer>("seats","Seats",null).setRequired(true).setMin(1).setMax(99));
            form.add(new Button("normal").add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE)));
            form.add(SwSubmitButton.onSubmit("save","Update report",target->target.add(total)));
            add(form,total);
        }
    }
    /** Expected failures render application-owned content; retry replaces the lazy panel. */
    public static class RetryContent extends Panel {
        public RetryContent(String id,SerializableConsumer<AjaxRequestTarget> retry) {
            super(id);var error=new WebMarkupContainer("error");error.add(Sw.Behaviors.alert(AlertBehavior.Variant.DESTRUCTIVE));
            error.add(SwButton.onClick("retry","Retry report",retry).setBusyIndicator(true));add(error);
        }
    }
    @Override protected String getTitle(){return "Lazy-loaded panels";}
}

package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.AjaxBusyBehavior;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.components.*;
import dev.jbaby.shadwick.components.form.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;

/** Native value controls and opt-in Ajax busy feedback; demo actions briefly simulate server work. */
public class StatusControlsPage extends BasePage {
    public static class Survey implements Serializable { public String name; public Integer rating; }
    public StatusControlsPage(PageParameters parameters) {
        super(parameters);
        Model<Integer> completed = Model.of(25);
        var values = new WebMarkupContainer("values"); values.setOutputMarkupId(true);
        values.add(new SwProgress("progress", completed).setLabel("Project progress"));
        values.add(new SwProgress("unknown", Model.of()).setLabel("Preparing files"));
        values.add(new SwRadialProgress("ring", completed).setLabel("Profile completion"));
        values.add(new SwRadialProgress("unknownRing", Model.of()).setLabel("Preparing account"));
        values.add(new Label("completed", (IModel<String>) () -> completed.getObject() + "% complete"));
        values.add(new SwMeter("storage", Model.of(45), Model.of(0), Model.of(100), Model.of(60), Model.of(85), Model.of(20)).setLabel("Storage used"));
        values.add(new SwMeter("warning", Model.of(75), Model.of(0), Model.of(100), Model.of(60), Model.of(85), Model.of(20)).setLabel("Storage nearing capacity"));
        values.add(new SwMeter("critical", Model.of(95), Model.of(0), Model.of(100), Model.of(60), Model.of(85), Model.of(20)).setLabel("Storage nearly full"));
        add(values);
        add(SwButton.onClick("advance", "Advance work", target -> { completed.setObject((completed.getObject() + 25) % 125); target.add(values); }));
        Model<Integer> requests = Model.of(0);
        Label status = new Label("busyStatus", (IModel<String>) () -> "Completed requests: " + requests.getObject()); status.setOutputMarkupId(true); add(status);
        SwButton busy = SwButton.onClick("busy", "Run server action", target -> { pause(); requests.setObject(requests.getObject() + 1); target.add(status); }).setBusyIndicator(true);
        busy.setMarkupId("busy-action"); add(busy);
        SwButton replace = SwButton.onClick("replace", "Replace busy button", target -> { pause(); target.add(get("replace")); }).setBusyIndicator(true);
        replace.setMarkupId("busy-replace"); add(replace);
        var icon = new SwButton("iconBusy") {
            @Override public void onClick(AjaxRequestTarget target) { pause(); requests.setObject(requests.getObject() + 1); target.add(status); }
        }.setSize(ButtonBehavior.Size.ICON).setBusyIndicator(true);
        icon.setMarkupId("busy-icon"); icon.add(new SwIcon("refreshIcon", "refresh-cw")); add(icon);
        var link = new AjaxLink<Void>("busyLink") { @Override public void onClick(AjaxRequestTarget target) { pause(); requests.setObject(requests.getObject() + 1); target.add(status); } };
        link.add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE), new AjaxBusyBehavior()); link.setMarkupId("busy-link"); add(link);
        var cancel = new SwButton("cancel", "Cancel before sending") {
            @Override public void onClick(AjaxRequestTarget target) { throw new IllegalStateException("A cancelled request must not reach the server"); }
            @Override protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes); attributes.getAjaxCallListeners().add(new AjaxCallListener().onPrecondition("return false;"));
            }
        }.setBusyIndicator(true); cancel.setMarkupId("busy-cancel"); add(cancel);
        add(new SwDialog("dialog", "Confirm server action") {
            @Override protected void onConfirm(AjaxRequestTarget target) { pause(); requests.setObject(requests.getObject() + 1); target.add(status); }
        }.setBusyIndicator(true).setTrigger("Open busy dialog"));

        add(new SwRating("rating", 4.3)); add(new SwRating("unrated", Model.of())); add(new SwRating("tenStars", 7.5).setMax(10));
        Label receipt = new Label("receipt", Model.of("No rating submitted yet.")); receipt.setOutputMarkupId(true);
        var form = new Form<Survey>("form", new CompoundPropertyModel<>(new Survey())) {
            @Override protected void onSubmit() {
                pause(); receipt.setDefaultModelObject("Saved " + getModelObject().name + ": " + getModelObject().rating + " stars.");
            }
        };
        form.add(new SwTextField<String>("name", "Your name").setRequired(true));
        form.add(new SwRatingField("rating", "Your rating", null).setRequired(true).setDescription("Use the arrow keys to choose a whole-star rating."));
        form.add(new SwRatingField("disabled", "Managed rating", Model.of(3)).setEnabled(false));
        form.add(new Button("normal").add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE)));
        form.add(SwSubmitButton.onSubmit("ajax", "Save rating with Ajax", target -> target.add(receipt), target -> target.add(receipt)).setBusyIndicator(true));
        form.add(SwSubmitButton.onSubmit("native", "Save with input", target -> target.add(receipt), target -> target.add(receipt)).setBusyIndicator(true));
        form.add(receipt); add(form);
    }
    private static void pause() {
        try { Thread.sleep(400); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }
    @Override protected String getTitle() { return "Progress & ratings"; }
}

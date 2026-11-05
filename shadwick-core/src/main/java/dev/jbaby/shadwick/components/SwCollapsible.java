package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.CollapsibleBehavior;
import dev.jbaby.shadwick.behaviors.DisclosureTriggerBehavior;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Objects;

/** A native details/summary disclosure with escaped label and an arbitrary content slot. Bind to <details>. */
public class SwCollapsible extends Panel {
    public static final String CONTENT_ID = "content";
    private final CollapsibleBehavior appearance = new CollapsibleBehavior();
    private final WebMarkupContainer trigger = new WebMarkupContainer("trigger");
    private final WebMarkupContainer body;
    private final Label label;
    private final ButtonBehavior button = new ButtonBehavior();
    private boolean styledButton;

    public SwCollapsible(String id, String label) { this(id, Model.of(label)); }
    public SwCollapsible(String id, IModel<String> label) {
        super(id); add(appearance); setOutputMarkupId(true);
        this.label = new Label("label", OncePerRequestModel.of(Objects.requireNonNull(label)));
        trigger.add(new DisclosureTriggerBehavior(DisclosureTriggerBehavior.Kind.COLLAPSIBLE));
        trigger.add(this.label, new SwIcon("icon", "chevron-down")); add(trigger);
        body = new WebMarkupContainer("body") {
            @Override protected void onConfigure() {
                super.onConfigure(); setVisible(SwCollapsible.this.isEnabledInHierarchy() || appearance.isOpen());
            }
        };
        body.add(new StyledBehavior("sw-collapsible-content"));
        body.add(new WebMarkupContainer(CONTENT_ID).setVisible(false)); add(body);
    }
    public SwCollapsible setLabel(String label) { return setLabel(Model.of(label)); }
    public SwCollapsible setLabel(IModel<String> label) { this.label.setDefaultModel(OncePerRequestModel.of(Objects.requireNonNull(label))); return this; }
    public SwCollapsible setOpen(boolean open) { appearance.setOpen(open); return this; }
    public boolean isOpen() { return appearance.isOpen(); }
    public SwCollapsible setContent(String content) { return setContent(Model.of(content)); }
    public SwCollapsible setContent(IModel<String> content) { return setContent(new Label(CONTENT_ID, Objects.requireNonNull(content))); }
    public SwCollapsible setContent(Component content) {
        if (!CONTENT_ID.equals(content.getId())) throw new IllegalArgumentException("The collapsible content must use id \"content\"");
        body.replace(content); return this;
    }
    private void styleButton() { if (!styledButton) { trigger.add(button); styledButton = true; } }
    public SwCollapsible setVariant(ButtonBehavior.Variant variant) { styleButton(); button.setVariant(variant); return this; }
    public SwCollapsible setSize(ButtonBehavior.Size size) { styleButton(); button.setSize(size); return this; }
    public WebMarkupContainer getTrigger() { return trigger; }
    public Component getContent() { return body.get(CONTENT_ID); }
}

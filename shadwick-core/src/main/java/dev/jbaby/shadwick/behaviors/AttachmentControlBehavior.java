package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import java.util.Objects;

/** A named attachment action or stretched trigger. Adds button naming checks only if absent. */
public class AttachmentControlBehavior extends StyledBehavior {
    public enum Kind { ACTION, TRIGGER }
    private final Kind kind;
    private IModel<String> label;
    public AttachmentControlBehavior(Kind kind) {
        super(kind == Kind.ACTION ? "sw-attachment-action" : "sw-attachment-trigger");
        this.kind = Objects.requireNonNull(kind);
    }
    public AttachmentControlBehavior setLabel(String label) { return setLabel(Model.of(label)); }
    public AttachmentControlBehavior setLabel(IModel<String> label) { this.label = label; return this; }
    @Override public void bind(Component component) {
        super.bind(component);
        if (component.getBehaviors(ButtonBehavior.class).isEmpty()) {
            component.add(new ButtonBehavior(ButtonBehavior.Variant.GHOST).setSize(ButtonBehavior.Size.ICON));
        }
    }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        if (label != null) AttachmentBehavior.attribute(tag, "aria-label", label.getObject());
        if ("button".equals(tag.getName()) && tag.getAttribute("type") == null) tag.put("type", "button");
        if (!component.isEnabledInHierarchy()) {
            if ("button".equals(tag.getName())) tag.put("disabled", "disabled");
            if ("a".equals(tag.getName())) { tag.remove("href"); tag.put("aria-disabled", "true"); tag.put("tabindex", "-1"); }
        }
    }
    @Override public void detach(Component component) { if (label != null) label.detach(); super.detach(component); }
}

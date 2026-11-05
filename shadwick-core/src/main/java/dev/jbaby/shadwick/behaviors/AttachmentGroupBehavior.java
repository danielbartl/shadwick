package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import java.util.Objects;

/** A scrolling row, or a wrapping grid. Label rows to enable native keyboard scrolling. */
public class AttachmentGroupBehavior extends StyledBehavior {
    public enum Layout { ROW, GRID }
    private Layout layout = Layout.ROW;
    private IModel<String> label;
    public AttachmentGroupBehavior() { super("sw-attachment-group"); }
    public AttachmentGroupBehavior setLayout(Layout layout) { this.layout = Objects.requireNonNull(layout); return this; }
    public AttachmentGroupBehavior setLabel(String label) { return setLabel(Model.of(label)); }
    public AttachmentGroupBehavior setLabel(IModel<String> label) { this.label = label; return this; }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        AttachmentBehavior.attribute(tag, "data-layout", layout == Layout.GRID ? "grid" : null);
        String name = label == null ? null : label.getObject();
        if (label != null) AttachmentBehavior.attribute(tag, "aria-label", name);
        boolean named = name != null && !name.isBlank() || tag.getAttribute("aria-labelledby") != null
                || tag.getAttribute("aria-label") != null && !tag.getAttribute("aria-label").isBlank();
        AttachmentBehavior.attribute(tag, "role", named ? "group" : null);
        AttachmentBehavior.attribute(tag, "tabindex", named && layout == Layout.ROW ? "0" : null);
    }
    @Override public void detach(Component component) { if (label != null) label.detach(); super.detach(component); }
}

package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/** Button appearance and native disabled/current semantics for a page link. */
public class PaginationLinkBehavior extends ButtonBehavior {
    private IModel<Boolean> current = Model.of(false);
    public PaginationLinkBehavior() { super(Variant.GHOST); setSize(Size.ICON); }
    public PaginationLinkBehavior setCurrent(boolean current) { return setCurrent(Model.of(current)); }
    public PaginationLinkBehavior setCurrent(IModel<Boolean> current) { this.current = current; return this; }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag); tag.append("class", "sw-pagination-link", " ");
        boolean active = current != null && Boolean.TRUE.equals(current.getObject());
        tag.put("data-variant", active ? "outline" : "ghost");
        if (active) tag.put("aria-current", "page"); else tag.remove("aria-current");
        if (!component.isEnabledInHierarchy()) {
            tag.remove("href"); tag.put("aria-disabled", "true"); tag.put("role", "link"); tag.put("tabindex", "-1");
        }
    }
    @Override public void detach(Component component) { if (current != null) current.detach(); super.detach(component); }
}

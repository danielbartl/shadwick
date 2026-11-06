package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import dev.jbaby.shadwick.components.SwPagination;
import dev.jbaby.shadwick.util.LocalizedText;

/** A named navigation landmark for native page links. */
public class PaginationBehavior extends StyledBehavior {
    private IModel<String> label;
    public PaginationBehavior() { super("sw-pagination"); }
    public PaginationBehavior setLabel(String label) { return setLabel(Model.of(label)); }
    public PaginationBehavior setLabel(IModel<String> label) { this.label = label; return this; }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        if (!"nav".equals(tag.getName())) throw new IllegalArgumentException("Pagination requires <nav> markup");
        super.onComponentTag(component, tag);
        if (label != null && label.getObject() != null) tag.put("aria-label", label.getObject());
        if (tag.getAttribute("aria-label") == null && tag.getAttribute("aria-labelledby") == null)
            tag.put("aria-label", LocalizedText.get(component, SwPagination.class, "SwPagination.label", "Pagination"));
    }
    @Override public void detach(Component component) { if (label != null) label.detach(); super.detach(component); }
}

package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;

/** A native disclosure's server-rendered initial state. User toggles stay in the browser. */
public class CollapsibleBehavior extends StyledBehavior {
    private boolean open;
    public CollapsibleBehavior() { super("sw-collapsible"); }
    public boolean isOpen() { return open; }
    public CollapsibleBehavior setOpen(boolean open) { this.open = open; return this; }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        if (!"details".equals(tag.getName())) throw new IllegalArgumentException("A collapsible requires <details> markup");
        super.onComponentTag(component, tag);
        if (open) tag.put("open", "open"); else tag.remove("open");
        if (!component.isEnabledInHierarchy()) tag.put("data-disabled", "true"); else tag.remove("data-disabled");
    }
}

package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;

/** Styles a native dl whose rows use sw-description-list-item, with dt/dd children. */
public class DescriptionListBehavior extends StyledBehavior {
    public DescriptionListBehavior() { super("sw-description-list"); }

    @Override public void onComponentTag(Component component, ComponentTag tag) {
        if (!tag.getName().equalsIgnoreCase("dl"))
            throw new IllegalArgumentException("DescriptionListBehavior requires a <dl> element");
        super.onComponentTag(component, tag);
    }
}

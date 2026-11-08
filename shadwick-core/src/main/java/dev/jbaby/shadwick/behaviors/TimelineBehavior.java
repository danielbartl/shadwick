package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;

/** Styles a native ordered history whose li children use TimelineEventBehavior. */
public class TimelineBehavior extends StyledBehavior {
    public TimelineBehavior() { super("sw-timeline"); }

    @Override public void onComponentTag(Component component, ComponentTag tag) {
        if (!tag.getName().equalsIgnoreCase("ol"))
            throw new IllegalArgumentException("TimelineBehavior requires an <ol> element");
        super.onComponentTag(component, tag);
        // Preserve list semantics when list-style is suppressed, including in WebKit.
        tag.put("role", "list");
    }
}

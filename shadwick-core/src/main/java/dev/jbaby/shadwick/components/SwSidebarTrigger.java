package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.SidebarTriggerBehavior;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.panel.Panel;

/** Icon button, bound to a native button tag. For your own button use SidebarTriggerBehavior. */
public class SwSidebarTrigger extends Panel {
    public SwSidebarTrigger(String id, Component sidebar) {
        super(id);
        add(new SidebarTriggerBehavior(sidebar));
        add(new ButtonBehavior(ButtonBehavior.Variant.GHOST).setSize(ButtonBehavior.Size.ICON));
    }
    @Override protected void onComponentTag(ComponentTag tag) { checkComponentTag(tag, "button"); super.onComponentTag(tag); }
}

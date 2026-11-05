package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;

/** Styles a native summary and keeps disabled disclosures out of ordinary keyboard/pointer navigation. */
public class DisclosureTriggerBehavior extends StyledBehavior {
    public enum Kind { ACCORDION, COLLAPSIBLE }
    public DisclosureTriggerBehavior(Kind kind) {
        super(kind == Kind.ACCORDION ? "sw-accordion-trigger" : "sw-collapsible-trigger");
    }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        if (!"summary".equals(tag.getName())) throw new IllegalArgumentException("A disclosure trigger requires <summary> markup");
        super.onComponentTag(component, tag);
        if (!component.isEnabledInHierarchy()) { tag.put("aria-disabled", "true"); tag.put("tabindex", "-1"); }
    }
}

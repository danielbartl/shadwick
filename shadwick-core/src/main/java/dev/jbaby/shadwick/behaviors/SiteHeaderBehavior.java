package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;

/** Shadleaf's header row on caller-owned header markup; use siteHeaderEnd for end-aligned actions. */
public class SiteHeaderBehavior extends StyledBehavior {
    private boolean inset;
    public SiteHeaderBehavior() { super("sw-site-header"); }
    /** An interior page header, without its own background or bottom border. */
    public SiteHeaderBehavior setInset(boolean inset) { this.inset = inset; return this; }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        if (inset) tag.put("data-inset", "true"); else tag.remove("data-inset");
    }
}

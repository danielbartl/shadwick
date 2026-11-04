package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.SiteHeaderBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;

/** A native header with caller-owned branding, navigation and actions, like SwCard's composition API. */
public class SwSiteHeader extends WebMarkupContainer {
    private final SiteHeaderBehavior behavior = new SiteHeaderBehavior();
    public SwSiteHeader(String id) { super(id); add(behavior); }
    public SwSiteHeader setInset(boolean inset) { behavior.setInset(inset); return this; }
}

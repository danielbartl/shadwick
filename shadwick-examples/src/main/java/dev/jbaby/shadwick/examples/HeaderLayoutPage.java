package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.behaviors.SidebarBehavior;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/** The same workspace content, composed with navigation in the site header. */
public class HeaderLayoutPage extends AppLayoutPage {
    public HeaderLayoutPage(PageParameters parameters) { super(parameters); }
    @Override protected SidebarBehavior.Placement sidebarPlacement() { return SidebarBehavior.Placement.HEADER; }
}

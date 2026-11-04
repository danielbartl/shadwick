package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.resource.loader.ComponentStringResourceLoader;

/** Breadcrumb styling and a localized fallback name for caller-owned nav markup. */
public class BreadcrumbBehavior extends StyledBehavior {
    public BreadcrumbBehavior() { super("sw-breadcrumb"); }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        if (tag.getAttribute("aria-label") == null && tag.getAttribute("aria-labelledby") == null) {
            String fallback = new ComponentStringResourceLoader().loadStringResource(BreadcrumbBehavior.class,
                    "BreadcrumbBehavior.label", component.getLocale(), component.getStyle(), component.getVariation());
            tag.put("aria-label", component.getString("BreadcrumbBehavior.label", null, fallback));
        }
    }
}

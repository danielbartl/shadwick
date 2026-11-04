package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.Shadwick;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.resource.loader.ComponentStringResourceLoader;

import java.util.Objects;

/** A native button targeting a sidebar. Core JS supplies desktop toggle and mobile focus/state. */
public class SidebarTriggerBehavior extends StyledBehavior {

    private final Component sidebar;

    public SidebarTriggerBehavior(Component sidebar) {
        super("sw-sidebar-trigger");
        this.sidebar = Objects.requireNonNull(sidebar);
        sidebar.setOutputMarkupId(true);
    }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.put("type", "button");
        tag.put("popovertarget", sidebar.getMarkupId());
        tag.put("aria-controls", sidebar.getMarkupId());
        tag.put("aria-expanded", "false");
        if (tag.getAttribute("aria-label") == null && tag.getAttribute("aria-labelledby") == null) {
            String fallback = new ComponentStringResourceLoader().loadStringResource(SidebarTriggerBehavior.class,
                    "SidebarTriggerBehavior.label", component.getLocale(), component.getStyle(), component.getVariation());
            tag.put("aria-label", component.getString("SidebarTriggerBehavior.label", null, fallback));
        }
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
    }
}

package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.Shadwick;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;

/** Shadleaf-style input group, with a direct input/textarea followed by its addons in DOM order. */
public class InputGroupBehavior extends StyledBehavior {
    public InputGroupBehavior() { super("sw-input-group"); }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.put("role", "group");
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
    }
}

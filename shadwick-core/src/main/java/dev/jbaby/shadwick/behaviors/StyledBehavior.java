package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.Shadwick;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;

/** A component class and its stylesheet, also usable on existing Wicket components. */
public class StyledBehavior extends Behavior {
    private final String cssClass;

    public StyledBehavior(String cssClass) {
        this.cssClass = cssClass;
    }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        tag.append("class", cssClass, " ");
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        Shadwick.renderResources(response);
    }
}

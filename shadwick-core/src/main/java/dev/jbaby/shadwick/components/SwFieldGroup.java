package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.FieldGroupBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;

/**
 * A container that stacks Shadwick fields with the skin's spacing and lets
 * {@link dev.jbaby.shadwick.components.form.BaseSwField.Orientation#RESPONSIVE responsive} fields put their label
 * beside the control when it is at least 28rem wide. The markup inside is the caller's; see {@link FieldGroupBehavior}
 * to style an existing container, such as the form.
 */
public class SwFieldGroup extends WebMarkupContainer {
    public SwFieldGroup(String id) { super(id); add(new FieldGroupBehavior()); }
}

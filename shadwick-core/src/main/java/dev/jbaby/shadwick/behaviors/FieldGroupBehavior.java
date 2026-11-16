package dev.jbaby.shadwick.behaviors;

/**
 * Stacks the Shadwick fields inside a container with the skin's spacing, as Shadleaf's field group does, and is the
 * container a field with {@link dev.jbaby.shadwick.components.form.BaseSwField.Orientation#RESPONSIVE} measures: at
 * 28rem and wider its label sits beside the control. Put it on a {@code <div>}, a {@code <fieldset>} or the form itself:
 * <pre>
 * form.add(Sw.Behaviors.fieldGroup());
 * </pre>
 * Field groups nest, with less space between the fields of an inner group.
 */
public class FieldGroupBehavior extends StyledBehavior {
    public FieldGroupBehavior() { super("sw-field-group"); }
}

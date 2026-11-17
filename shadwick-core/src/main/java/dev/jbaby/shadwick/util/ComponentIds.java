package dev.jbaby.shadwick.util;

import org.apache.wicket.Component;

/**
 * Checks that a component given to a Shadwick component uses the id it must: the one a factory was handed, or the
 * fixed id of a slot such as a dialog's content. Wicket would otherwise fail later, while rendering, with an error that
 * no longer names the factory or slot.
 */
public final class ComponentIds {
    private ComponentIds() {}

    /**
     * The component a factory returned, if it uses the id the factory was given.
     *
     * @param what names the component for the message, e.g. "A wizard step" or "The editor"
     * @throws IllegalArgumentException if it is null or has another id
     */
    public static <C extends Component> C fromFactory(C component, String id, String what) {
        if (component == null || !id.equals(component.getId())) {
            throw new IllegalArgumentException(what + " must use the id its factory is given (\"" + id + "\"), but was "
                    + found(component));
        }
        return component;
    }

    /**
     * The component for a slot with a fixed id, if it uses that id.
     *
     * @param what names the slot for the message, e.g. "The dialog's content ({@code SwDialog.CONTENT_ID})"
     * @throws IllegalArgumentException if it is null or has another id
     */
    public static <C extends Component> C forSlot(C component, String id, String what) {
        if (component == null || !id.equals(component.getId())) {
            throw new IllegalArgumentException(what + " must use the id \"" + id + "\", but was " + found(component));
        }
        return component;
    }

    private static String found(Component component) {
        return component == null ? "null" : "\"" + component.getId() + "\"";
    }
}

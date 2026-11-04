package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.util.SerializableConsumer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;

/** Native button markup for a dialog's arbitrary-content slot. */
public class ToastActionPanel extends Panel {
    public ToastActionPanel(String id, String label, SerializableConsumer<AjaxRequestTarget> callback) {
        super(id);
        add(SwButton.onClick("notify", label, callback));
    }
}

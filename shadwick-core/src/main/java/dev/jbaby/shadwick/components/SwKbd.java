package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.StyledBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/** A keyboard key. Bind to a kbd element; sw-kbd-group groups a shortcut's keys. */
public class SwKbd extends Label {
    public SwKbd(String id, String key) { this(id, Model.of(key)); }
    public SwKbd(String id, IModel<?> key) { super(id, key); add(new StyledBehavior("sw-kbd")); }
}

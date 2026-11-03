package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.SeparatorBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;

/** A divider, hidden from assistive technology unless setDecorative(false). */
public class SwSeparator extends WebMarkupContainer {
    private final SeparatorBehavior behavior = new SeparatorBehavior();

    public SwSeparator(String id) { super(id); add(behavior); }
    public SwSeparator setOrientation(SeparatorBehavior.Orientation orientation) { behavior.setOrientation(orientation); return this; }
    public SwSeparator setDecorative(boolean decorative) { behavior.setDecorative(decorative); return this; }
}

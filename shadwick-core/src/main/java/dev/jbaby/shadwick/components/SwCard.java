package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.CardBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;

/** A card with caller-owned markup. Use sw-card-* classes for its header, title, description, content and footer. */
public class SwCard extends WebMarkupContainer {
    private final CardBehavior behavior = new CardBehavior();

    public SwCard(String id) { super(id); add(behavior); }

    public SwCard setSize(CardBehavior.Size size) { behavior.setSize(size); return this; }
}

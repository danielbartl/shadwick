package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.SkipLinkBehavior;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

/** A localized, escaped skip link. Bind to an anchor first in body, with a focusable target. No JavaScript. */
public class SwSkipLink extends Label {
    public SwSkipLink(String id) { this(id, new SkipLinkBehavior()); }
    public SwSkipLink(String id, String targetId) { this(id, new SkipLinkBehavior(targetId)); }
    public SwSkipLink(String id, Component target) { this(id, new SkipLinkBehavior(target)); }
    private SwSkipLink(String id, SkipLinkBehavior behavior) {
        super(id, Model.of(""));
        setDefaultModel(new StringResourceModel("SwSkipLink.label", this).setDefaultValue("Skip to main content"));
        add(behavior);
    }
    public SwSkipLink setLabel(String label) { return setLabel(Model.of(label)); }
    public SwSkipLink setLabel(IModel<String> label) { setDefaultModel(label); return this; }
}

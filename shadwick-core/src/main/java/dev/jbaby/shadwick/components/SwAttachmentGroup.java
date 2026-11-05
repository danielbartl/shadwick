package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.AttachmentGroupBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

/** Caller-owned markup; ListView rows should render their attachment roots as direct children. */
public class SwAttachmentGroup extends WebMarkupContainer {
    private final AttachmentGroupBehavior appearance = new AttachmentGroupBehavior();
    public SwAttachmentGroup(String id) { super(id); add(appearance); }
    public SwAttachmentGroup setLayout(AttachmentGroupBehavior.Layout layout) { appearance.setLayout(layout); return this; }
    public SwAttachmentGroup setLabel(String label) { appearance.setLabel(label); return this; }
    public SwAttachmentGroup setLabel(IModel<String> label) { appearance.setLabel(label); return this; }
}

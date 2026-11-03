package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.SpinnerBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.markup.ComponentTag;

/** An inline SVG loading status. Use setLabel to describe what is loading. */
public class SwSpinner extends SwIcon {
    public SwSpinner(String id) { this(id, SpinnerBehavior.Size.DEFAULT); }
    public SwSpinner(String id, SpinnerBehavior.Size size) { super(id, "loader-circle"); add(new SpinnerBehavior(size)); }
    public SwSpinner(String id, IModel<SpinnerBehavior.Size> size) { super(id, "loader-circle"); add(new SpinnerBehavior(size)); }

    @Override
    public SwSpinner setLabel(String label) { super.setLabel(label); return this; }

    @Override
    public SwSpinner setLabel(IModel<String> label) { super.setLabel(label); return this; }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        if (accessibleLabel() != null) { tag.put("aria-label", accessibleLabel()); }
    }
}

package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import java.util.Objects;

/** Display state only. Supply readable status text too; this behavior uploads nothing. */
public class AttachmentBehavior extends StyledBehavior {
    public enum State { DONE, IDLE, UPLOADING, PROCESSING, ERROR }
    public enum Size { DEFAULT, SMALL, EXTRA_SMALL }
    public enum Orientation { HORIZONTAL, VERTICAL }
    private State state = State.DONE;
    private Size size = Size.DEFAULT;
    private Orientation orientation = Orientation.HORIZONTAL;
    private boolean fullWidth;
    public AttachmentBehavior() { super("sw-attachment"); }
    public AttachmentBehavior setState(State state) { this.state = Objects.requireNonNull(state); return this; }
    public State getState() { return state; }
    public AttachmentBehavior setSize(Size size) { this.size = Objects.requireNonNull(size); return this; }
    public AttachmentBehavior setOrientation(Orientation orientation) { this.orientation = Objects.requireNonNull(orientation); return this; }
    public AttachmentBehavior setFullWidth(boolean fullWidth) { this.fullWidth = fullWidth; return this; }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        attribute(tag, "data-state", state == State.DONE ? null : state.name().toLowerCase(java.util.Locale.ROOT));
        attribute(tag, "data-size", size == Size.DEFAULT ? null : size == Size.SMALL ? "sm" : "xs");
        attribute(tag, "data-orientation", orientation == Orientation.VERTICAL ? "vertical" : null);
        attribute(tag, "data-width", fullWidth ? "full" : null);
        attribute(tag, "aria-busy", state == State.UPLOADING || state == State.PROCESSING ? "true" : null);
    }
    static void attribute(ComponentTag tag, String name, String value) {
        if (value == null) tag.remove(name); else tag.put(name, value);
    }
}

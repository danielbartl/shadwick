package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.Shadwick;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.time.Duration;
import java.util.Objects;

/**
 * A short, plain-text tooltip on an existing focusable component. Its API follows Oat's TooltipBehavior.
 * Native title supplies a no-script fallback; Shadwick replaces that with a styled, hoverable tooltip.
 * Existing descriptions are preserved. A tooltip supplements a control's accessible name; it does not name it.
 */
public class TooltipBehavior extends Behavior {
    /** LEFT and RIGHT follow the inline start/end, swapping on RTL pages. */
    public enum Placement { TOP, BOTTOM, LEFT, RIGHT }
    /** LABEL repeats an existing name without adding a duplicate screen-reader description. */
    public enum Mode { DESCRIPTION, LABEL }

    private final IModel<String> label;
    private Placement placement = Placement.TOP;
    private Mode mode = Mode.DESCRIPTION;
    private Duration delay = Duration.ofMillis(300);

    public TooltipBehavior(String label) { this(Model.of(label)); }
    public TooltipBehavior(IModel<String> label) { this.label = Objects.requireNonNull(label); }

    public TooltipBehavior setPlacement(Placement placement) { this.placement = Objects.requireNonNull(placement); return this; }
    public TooltipBehavior setMode(Mode mode) { this.mode = Objects.requireNonNull(mode); return this; }
    public TooltipBehavior setDelay(Duration delay) {
        Objects.requireNonNull(delay);
        if (delay.isNegative() || delay.compareTo(Duration.ofMillis(Integer.MAX_VALUE)) > 0) {
            throw new IllegalArgumentException("Tooltip delay must be between zero and Integer.MAX_VALUE milliseconds");
        }
        this.delay = delay;
        return this;
    }

    @Override public void bind(Component component) { super.bind(component); component.setOutputMarkupId(true); }

    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        String text = label.getObject();
        if (text == null || text.isBlank()) return;
        tag.put("title", text);
        tag.put("data-sw-tooltip", text);
        tag.put("data-tooltip-placement", placement.name().toLowerCase(java.util.Locale.ROOT));
        tag.put("data-tooltip-delay", (int) delay.toMillis());
        tag.put("data-tooltip-mode", mode.name().toLowerCase(java.util.Locale.ROOT));
    }

    @Override public void renderHead(Component component, IHeaderResponse response) {
        Shadwick.renderResources(response);
        response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
    }

    @Override public void detach(Component component) { label.detach(); super.detach(component); }
}

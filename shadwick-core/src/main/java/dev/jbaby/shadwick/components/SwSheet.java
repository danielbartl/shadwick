package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

/**
 * A native modal side panel with SwDialog's content, form validation and Ajax lifecycle.
 * START and END follow the page's text direction. The header and footer remain visible
 * while the body scrolls. The corner close button, Escape, Cancel and backdrop dismiss it.
 * Content uses {@link #CONTENT_ID}; confirming calls {@link #onConfirm(AjaxRequestTarget)}.
 */
public class SwSheet extends SwDialog {
    public enum Side { START, END, TOP, BOTTOM }
    private Side side = Side.END;

    public SwSheet(String id, String title) { this(id, Model.of(title)); }

    public SwSheet(String id, IModel<String> title) {
        super(id, title);
        getDialogElement().add(AttributeModifier.replace("data-side",
                (IModel<String>) () -> side.name().toLowerCase(Locale.ROOT)));
    }

    /** The screen edge; END by default. */
    public SwSheet setSide(Side side) { this.side = Objects.requireNonNull(side); return this; }
    public Side getSide() { return side; }

    @Override protected Duration getCloseTransition() { return Duration.ofMillis(250); }

    @Override public SwSheet setContent(Component content) { super.setContent(content); return this; }
    @Override public SwSheet setTitle(String title) { super.setTitle(title); return this; }
    @Override public SwSheet setTitle(IModel<String> title) { super.setTitle(title); return this; }
    @Override public SwSheet setDescription(String description) { super.setDescription(description); return this; }
    @Override public SwSheet setDescription(IModel<String> description) { super.setDescription(description); return this; }
    @Override public SwSheet setCancelLabel(String label) { super.setCancelLabel(label); return this; }
    @Override public SwSheet setCancelLabel(IModel<String> label) { super.setCancelLabel(label); return this; }
    @Override public SwSheet setConfirmLabel(String label) { super.setConfirmLabel(label); return this; }
    @Override public SwSheet setConfirmLabel(IModel<String> label) { super.setConfirmLabel(label); return this; }
    @Override public SwSheet setConfirmVariant(ButtonBehavior.Variant variant) { super.setConfirmVariant(variant); return this; }
    @Override public SwSheet setCloseButton(boolean shown) { super.setCloseButton(shown); return this; }
    @Override public SwSheet setFooterVisible(boolean visible) { super.setFooterVisible(visible); return this; }
    @Override public SwSheet setTrigger(String label) { super.setTrigger(label); return this; }
    @Override public SwSheet setTrigger(IModel<String> label) { super.setTrigger(label); return this; }
    @Override public SwSheet setTriggerVariant(ButtonBehavior.Variant variant) { super.setTriggerVariant(variant); return this; }
    @Override public SwSheet open(AjaxRequestTarget target) { super.open(target); return this; }
    @Override public SwSheet open(Component content, AjaxRequestTarget target) { super.open(content, target); return this; }
    @Override public SwSheet close(AjaxRequestTarget target) { super.close(target); return this; }
}

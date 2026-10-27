package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.AccessibilityChecks;
import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.ShadwickSettings;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Response;
import org.apache.wicket.response.StringResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * Behavior that styles any component as a shadcn button: a {@code <button>}, an {@code <a>}, an
 * {@code <input type="submit">}. It adds the {@code sw-button} class, and {@code data-variant} and
 * {@code data-size} for anything but the defaults:
 *
 * <pre>
 * add(new BookmarkablePageLink&lt;&gt;("home", HomePage.class)
 *         .add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE)));
 * </pre>
 *
 * A disabled {@code <a>}, which Wicket renders without an {@code href}, gets {@code aria-disabled="true"}.
 * <p>
 * With {@link ShadwickSettings#setAccessibilityChecks accessibility checks} on, a button without an accessible name
 * fails to render ({@code FAIL}, the default in development mode) or is logged ({@code WARN}, the default in
 * deployment): one with no text, no {@code aria-label} and no labelled icon, which a screen reader announces only as
 * "button". An icon-only button ({@link Size#ICON}) needs an {@code aria-label}.
 */
public class ButtonBehavior extends Behavior {

    private static final Logger LOG = LoggerFactory.getLogger(ButtonBehavior.class);

    /** shadcn's button variants, as {@code data-variant}. */
    public enum Variant implements Serializable {
        DEFAULT(null),
        SECONDARY("secondary"),
        OUTLINE("outline"),
        GHOST("ghost"),
        DESTRUCTIVE("destructive"),
        LINK("link");

        private final String value;

        Variant(String value) {
            this.value = value;
        }

        /** The {@code data-variant} value, or {@code null} for {@link #DEFAULT}, which sets none. */
        public String value() {
            return value;
        }
    }

    /** shadcn's button sizes, as {@code data-size}. */
    public enum Size implements Serializable {
        DEFAULT(null),
        SMALL("sm"),
        LARGE("lg"),
        /** Square, for a single icon. Name it for screen readers with an {@code aria-label}. */
        ICON("icon");

        private final String value;

        Size(String value) {
            this.value = value;
        }

        /** The {@code data-size} value, or {@code null} for {@link #DEFAULT}, which sets none. */
        public String value() {
            return value;
        }
    }

    private IModel<Variant> variantModel;
    private IModel<Size> sizeModel = Model.of(Size.DEFAULT);
    // While a button renders under the accessibility checks: the response it would have
    // been written to, as it is captured to be checked first
    private transient Response original;

    public ButtonBehavior() {
        this(Variant.DEFAULT);
    }

    public ButtonBehavior(Variant variant) {
        this(Model.of(variant));
    }

    public ButtonBehavior(IModel<Variant> variantModel) {
        this.variantModel = variantModel;
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        Shadwick.renderResources(response);
    }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.append("class", "sw-button", " ");

        Variant variant = variantModel.getObject();
        if (variant != null && variant.value() != null) {
            tag.put("data-variant", variant.value());
        }

        Size size = sizeModel.getObject();
        if (size != null && size.value() != null) {
            tag.put("data-size", size.value());
        }

        // A disabled link has no href, so it can't be followed; this says so to screen readers
        if (tag.getName().equalsIgnoreCase("a") && !component.isEnabledInHierarchy()) {
            tag.put("aria-disabled", "true");
        }
    }

    @Override
    public void beforeRender(Component component) {
        super.beforeRender(component);
        if (!component.getRenderBodyOnly() && checks(component) != AccessibilityChecks.OFF) {
            original = component.getRequestCycle().setResponse(new StringResponse());
        }
    }

    @Override
    public void afterRender(Component component) {
        if (original != null) {
            CharSequence html = ((StringResponse) component.getRequestCycle().setResponse(original)).getBuffer();
            original.write(html);
            original = null;
            if (!AccessibleNames.hasName(html)) {
                if (checks(component) == AccessibilityChecks.FAIL) {
                    throw new IllegalStateException(unnamed(component));
                }
                LOG.warn(unnamed(component));
            }
        }
        super.afterRender(component);
    }

    @Override
    public void onException(Component component, RuntimeException exception) {
        if (original != null) {
            component.getRequestCycle().setResponse(original);
            original = null;
        }
        super.onException(component, exception);
    }

    private static AccessibilityChecks checks(Component component) {
        return ShadwickSettings.get(component.getApplication()).getAccessibilityChecks();
    }

    private static String unnamed(Component component) {
        Class<?> type = component.getClass();
        while (type.isAnonymousClass()) {
            type = type.getSuperclass();
        }
        return "The button \"" + component.getPageRelativePath() + "\" (" + type.getSimpleName() + " on "
                + component.getPage().getClass().getSimpleName() + ") has no accessible name, so a screen reader announces it only as \"button\". Give it a"
                + " label, or an aria-label (add(AttributeModifier.replace(\"aria-label\", ...))) if it shows only an icon."
                + " Shadwick checks this in development mode; ShadwickSettings.setAccessibilityChecks(AccessibilityChecks.OFF)"
                + " turns the check off.";
    }

    public ButtonBehavior setVariant(Variant variant) {
        this.variantModel = Model.of(variant);
        return this;
    }

    public ButtonBehavior setVariant(IModel<Variant> variantModel) {
        this.variantModel = variantModel;
        return this;
    }

    public ButtonBehavior setSize(Size size) {
        this.sizeModel = Model.of(size);
        return this;
    }

    public ButtonBehavior setSize(IModel<Size> sizeModel) {
        this.sizeModel = sizeModel;
        return this;
    }

    @Override
    public void detach(Component component) {
        super.detach(component);
        variantModel.detach();
        sizeModel.detach();
    }
}

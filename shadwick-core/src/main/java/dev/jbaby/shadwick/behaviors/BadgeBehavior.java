package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import java.util.Objects;

/** Styles an existing component as a shadcn badge; variants are specific to this component. */
public class BadgeBehavior extends StyledBehavior {
    public enum Variant {
        DEFAULT(null), SECONDARY("secondary"), OUTLINE("outline"), GHOST("ghost"), DESTRUCTIVE("destructive"), LINK("link");
        private final String value;
        Variant(String value) { this.value = value; }
        public String value() { return value; }
    }

    private IModel<Variant> variantModel;

    public BadgeBehavior() { this(Variant.DEFAULT); }
    public BadgeBehavior(Variant variant) { this(Model.of(Objects.requireNonNull(variant))); }
    public BadgeBehavior(IModel<Variant> variant) {
        super("sw-badge");
        variantModel = Objects.requireNonNull(variant);
    }

    public BadgeBehavior setVariant(Variant variant) { return setVariant(Model.of(Objects.requireNonNull(variant))); }
    public BadgeBehavior setVariant(IModel<Variant> variant) {
        variantModel = Objects.requireNonNull(variant);
        return this;
    }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        Variant variant = variantModel.getObject();
        if (variant != null && variant.value() != null) {
            tag.put("data-variant", variant.value());
        } else {
            tag.remove("data-variant");
        }
    }

    @Override
    public void detach(Component component) {
        variantModel.detach();
        super.detach(component);
    }
}

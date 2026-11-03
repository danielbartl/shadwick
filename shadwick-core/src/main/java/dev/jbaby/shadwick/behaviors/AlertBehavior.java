package dev.jbaby.shadwick.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import java.util.Objects;

/** Styles an existing component as a shadcn alert; variants are specific to this component. */
public class AlertBehavior extends StyledBehavior {
    public enum Variant {
        DEFAULT(null), DESTRUCTIVE("destructive");
        private final String value;
        Variant(String value) { this.value = value; }
        public String value() { return value; }
    }

    private IModel<Variant> variantModel;

    public AlertBehavior() { this(Variant.DEFAULT); }
    public AlertBehavior(Variant variant) { this(Model.of(Objects.requireNonNull(variant))); }
    public AlertBehavior(IModel<Variant> variant) {
        super("sw-alert");
        variantModel = Objects.requireNonNull(variant);
    }

    public AlertBehavior setVariant(Variant variant) { return setVariant(Model.of(Objects.requireNonNull(variant))); }
    public AlertBehavior setVariant(IModel<Variant> variant) {
        variantModel = Objects.requireNonNull(variant);
        return this;
    }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.put("role", "alert");
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

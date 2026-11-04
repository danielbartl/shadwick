package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.behaviors.ToggleBehavior;
import dev.jbaby.shadwick.behaviors.ToggleGroupBehavior;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;

/** Common styling and accessible markup for single- and multiple-selection native toggle groups. */
public abstract class BaseSwToggleGroup<T, C extends FormComponent<T>, F extends BaseSwToggleGroup<T, C, F>>
        extends BaseSwField<T, C, F> {
    private final ToggleBehavior items = new ToggleBehavior();
    private final ToggleGroupBehavior layout = new ToggleGroupBehavior();

    protected BaseSwToggleGroup(String id, IModel<String> label, IModel<T> model, IModel<String> description) {
        super(id, label, model, description);
        get("label").setOutputMarkupId(true);
    }

    public F setVariant(ToggleBehavior.Variant variant) { items.setVariant(variant); return self(); }
    public F setSize(ToggleBehavior.Size size) { items.setSize(size); return self(); }
    public F setSpacing(int spacing) { layout.setSpacing(spacing); return self(); }
    public F setOrientation(ToggleGroupBehavior.Orientation orientation) { layout.setOrientation(orientation); return self(); }

    protected final C configureGroup(C input) {
        input.add(layout);
        input.add(AttributeModifier.replace("aria-labelledby", (IModel<String>) () -> get("label").getMarkupId()));
        return input;
    }

    protected final IValueMap toggleAttributes(boolean invalid) {
        IValueMap attributes = new ValueMap();
        attributes.put("class", "sw-toggle-input");
        attributes.put("aria-describedby", getDescribedByMarkupIds());
        attributes.put("aria-invalid", invalid ? "true" : "false");
        return attributes;
    }

    protected final IValueMap toggleLabelAttributes() { return items.itemAttributes(); }
}

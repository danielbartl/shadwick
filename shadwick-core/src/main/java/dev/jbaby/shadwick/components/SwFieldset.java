package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.StyledBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

/** Native form section. Add fields inside the caller-owned {@code <fieldset>} body. */
public class SwFieldset extends Border {
    private final Label legend;
    private final Label description;

    /** Legend looked up using the component ID, falling back to that ID. */
    public SwFieldset(String id) {
        this(id, (IModel<String>) null);
    }

    public SwFieldset(String id, String legend) {
        this(id, Model.of(legend));
    }

    public SwFieldset(String id, IModel<String> legend) {
        super(id);
        add(new StyledBehavior("sw-field-set"));
        setOutputMarkupId(true);
        this.legend = new Label("legend", legend != null ? legend : new StringResourceModel(id, this).setDefaultValue(id));
        description = new Label("description", new Model<String>()) {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(getDefaultModelObject() != null && !getDefaultModelObjectAsString().isBlank());
            }
        };
        description.setOutputMarkupId(true);
        addToBorder(this.legend, description);
    }

    public SwFieldset setDescription(String value) {
        return setDescription(Model.of(value));
    }

    public SwFieldset setDescription(IModel<String> value) {
        description.setDefaultModel(value != null ? value : new Model<String>());
        return this;
    }

    /** The escaped legend's model. */
    @SuppressWarnings("unchecked")
    public IModel<String> getLegend() {
        return (IModel<String>) legend.getDefaultModel();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        checkComponentTag(tag, "fieldset");
        super.onComponentTag(tag);
        if (!isEnabledInHierarchy()) tag.put("disabled", "disabled");
        if (description.isVisible()) tag.append("aria-describedby", description.getMarkupId(), " ");
    }
}

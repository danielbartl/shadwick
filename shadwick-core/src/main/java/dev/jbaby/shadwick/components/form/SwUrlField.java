package dev.jbaby.shadwick.components.form;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.UrlTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * A shadcn-styled form field wrapping a native {@code <input type="url">}
 * ({@link UrlTextField}).
 */
public class SwUrlField extends BaseSwInputField<String, UrlTextField, SwUrlField> {

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwUrlField(String id) {
        this(id, null, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwUrlField(String id, IModel<String> model) {
        this(id, null, model, null);
    }

    /** Label supplied explicitly; model inherited from a CompoundPropertyModel. */
    public SwUrlField(String id, String label) {
        this(id, Model.of(label), null, null);
    }

    public SwUrlField(String id, String label, IModel<String> model) {
        this(id, Model.of(label), model, null);
    }

    public SwUrlField(String id, IModel<String> label, IModel<String> model) {
        this(id, label, model, null);
    }

    public SwUrlField(String id, IModel<String> label, IModel<String> model, IModel<String> description) {
        super(id, label, model, description);
    }

    @Override
    protected UrlTextField newInput(String id, IModel<String> model) {
        return new UrlTextField(id, model) {
            @Override
            protected void onComponentTag(ComponentTag tag) {
                tag.put("type", "url");
                super.onComponentTag(tag);
            }
        };
    }
}

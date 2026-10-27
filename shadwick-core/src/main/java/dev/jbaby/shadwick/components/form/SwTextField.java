package dev.jbaby.shadwick.components.form;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * A shadcn-styled form field wrapping a native {@code <input type="text">} ({@link TextField}).
 *
 * @param <T> the model object type
 */
public class SwTextField<T> extends BaseSwField<T, TextField<T>, SwTextField<T>> {

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited from a parent
     * {@code CompoundPropertyModel}.
     */
    public SwTextField(String id) {
        this(id, null, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwTextField(String id, IModel<T> model) {
        this(id, null, model, null);
    }

    public SwTextField(String id, String label, IModel<T> model) {
        this(id, Model.of(label), model, null);
    }

    public SwTextField(String id, IModel<String> label, IModel<T> model) {
        this(id, label, model, null);
    }

    public SwTextField(String id, IModel<String> label, IModel<T> model, IModel<String> description) {
        super(id, label, model, description);
    }

    @Override
    protected TextField<T> newInput(String id, IModel<T> model) {
        return new TextField<>(id, model) {
            @Override
            protected void onComponentTag(ComponentTag tag) {
                tag.put("type", "text");
                super.onComponentTag(tag);
            }
        };
    }
}

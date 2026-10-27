package dev.jbaby.shadwick.components.form;

import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * A shadcn-styled form field wrapping a native {@code <textarea>} ({@link TextArea}), which grows with its content
 * where the browser supports it.
 *
 * @param <T> the model object type
 */
public class SwTextArea<T> extends BaseSwField<T, TextArea<T>, SwTextArea<T>> {

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited from a parent
     * {@code CompoundPropertyModel}.
     */
    public SwTextArea(String id) {
        this(id, null, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwTextArea(String id, IModel<T> model) {
        this(id, null, model, null);
    }

    /** Model inherited from a parent {@code CompoundPropertyModel}. */
    public SwTextArea(String id, String label) {
        this(id, Model.of(label), null, null);
    }

    public SwTextArea(String id, String label, IModel<T> model) {
        this(id, Model.of(label), model, null);
    }

    public SwTextArea(String id, IModel<String> label, IModel<T> model) {
        this(id, label, model, null);
    }

    public SwTextArea(String id, IModel<String> label, IModel<T> model, IModel<String> description) {
        super(id, label, model, description);
    }

    @Override
    protected TextArea<T> newInput(String id, IModel<T> model) {
        return new TextArea<>(id, model);
    }
}

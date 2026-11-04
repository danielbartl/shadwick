package dev.jbaby.shadwick.components.form;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * A shadcn-styled form field wrapping a native {@code <input type="password">}
 * ({@link PasswordTextField}).
 */
public class SwPasswordField extends BaseSwInputField<String, PasswordTextField, SwPasswordField> {

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwPasswordField(String id) {
        this(id, null, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwPasswordField(String id, IModel<String> model) {
        this(id, null, model, null);
    }

    /** Label supplied explicitly; model inherited from a CompoundPropertyModel. */
    public SwPasswordField(String id, String label) {
        this(id, Model.of(label), null, null);
    }

    public SwPasswordField(String id, String label, IModel<String> model) {
        this(id, Model.of(label), model, null);
    }

    public SwPasswordField(String id, IModel<String> label, IModel<String> model) {
        this(id, label, model, null);
    }

    public SwPasswordField(String id, IModel<String> label, IModel<String> model, IModel<String> description) {
        super(id, label, model, description);
    }

    @Override
    protected PasswordTextField newInput(String id, IModel<String> model) {
        return new PasswordTextField(id, model) {
            @Override
            protected void onComponentTag(ComponentTag tag) {
                tag.put("type", "password");
                super.onComponentTag(tag);
            }
        };
    }
}

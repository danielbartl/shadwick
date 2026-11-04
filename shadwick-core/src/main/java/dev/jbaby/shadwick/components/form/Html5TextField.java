package dev.jbaby.shadwick.components.form;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

/** A native HTML5 text input, with Wicket's usual model conversion and escaping. */
public class Html5TextField<T> extends TextField<T> {
    private final String inputType;
    private final IConverter<T> converter;

    public Html5TextField(String id, IModel<T> model, Class<T> type, String inputType) {
        this(id, model, type, inputType, null);
    }

    /** A field-specific converter for input types with a locale-independent wire format. */
    public Html5TextField(String id, IModel<T> model, Class<T> type, String inputType, IConverter<T> converter) {
        super(id, model, type);
        this.inputType = inputType;
        this.converter = converter;
    }

    @Override
    protected String[] getInputTypes() {
        return new String[] {inputType};
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        tag.put("type", inputType);
        super.onComponentTag(tag);
    }

    @Override
    protected IConverter<?> createConverter(Class<?> type) {
        if (converter != null && getType() != null && getType().isAssignableFrom(type)) {
            return converter;
        }
        return super.createConverter(type);
    }
}

package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.behaviors.TagInputBehavior;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.convert.IConverter;

/**
 * Freeform tags backed by a native comma-separated text field. JavaScript adds editable chips;
 * pending text is included in submission. Values use {@link String#trim()}, omit empty entries
 * and retain the first occurrence of each exact duplicate. Commas delimit tags and cannot occur
 * inside a model value. Empty input converts to an empty, mutable list.
 */
public class SwTagInput extends BaseSwField<List<String>, TextField<List<String>>, SwTagInput> {
    private final TagInputBehavior appearance;
    private final Label hint;

    public SwTagInput(String id) { this(id, null, null, null); }
    public SwTagInput(String id, IModel<List<String>> model) { this(id, null, model, null); }
    public SwTagInput(String id, String label, IModel<List<String>> model) { this(id, Model.of(label), model, null); }
    public SwTagInput(String id, IModel<String> label, IModel<List<String>> model) { this(id, label, model, null); }
    public SwTagInput(String id, IModel<String> label, IModel<List<String>> model, IModel<String> description) {
        super(id, label, model, description);
        appearance = new TagInputBehavior(getInput());
        add(new TransparentWebMarkupContainer("taginput").add(appearance));
        hint = new Label("hint", new StringResourceModel("SwTagInput.hint", this)
                .setDefaultValue("Separate tags with commas."));
        hint.setOutputMarkupId(true);
        add(hint);
    }

    @Override public SwTagInput setPlaceholder(IModel<String> value) { appearance.setPlaceholder(value); return this; }
    @Override protected String getAdditionalDescribedByMarkupIds() { return hint == null ? null : hint.getMarkupId(); }

    @Override @SuppressWarnings({"unchecked", "rawtypes"})
    protected TextField<List<String>> newInput(String id, IModel<List<String>> model) {
        TextField<List<String>> field = new TextField<List<String>>(id, model, (Class) List.class) {
            @Override protected IConverter<?> createConverter(Class<?> type) {
                return List.class.isAssignableFrom(type) ? TAGS : super.createConverter(type);
            }
            @Override public void validate() {
                super.validate();
                if (isValid() && isRequired() && getConvertedInput() != null && getConvertedInput().isEmpty()) reportRequiredError();
            }
        };
        field.setConvertEmptyInputStringToNull(false);
        return field;
    }

    private static final IConverter<List<String>> TAGS = new IConverter<>() {
        @Override public List<String> convertToObject(String value, Locale locale) {
            LinkedHashSet<String> tags = new LinkedHashSet<>();
            if (value != null) for (String part : value.split(",", -1)) {
                String tag = part.trim();
                if (!tag.isEmpty()) tags.add(tag);
            }
            return new ArrayList<>(tags);
        }
        @Override public String convertToString(List<String> value, Locale locale) {
            LinkedHashSet<String> tags = new LinkedHashSet<>();
            if (value != null) for (String part : value) {
                if (part == null) continue;
                if (part.contains(",")) throw new IllegalArgumentException("A tag cannot contain a comma");
                String tag = part.trim();
                if (!tag.isEmpty()) tags.add(tag);
            }
            return String.join(",", tags);
        }
    };
}

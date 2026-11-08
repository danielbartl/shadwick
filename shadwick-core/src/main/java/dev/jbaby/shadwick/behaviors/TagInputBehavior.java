package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.components.form.SwTagInput;
import dev.jbaby.shadwick.util.LocalizedText;
import java.util.Objects;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

/** Enhances a wrapper containing a direct native comma-separated text input. Conversion belongs to the input. */
public class TagInputBehavior extends StyledBehavior {
    public static final JavaScriptResourceReference TAG_INPUT_JS = new JavaScriptResourceReference(Shadwick.class, "js/tag-input.js");
    private final FormComponent<?> input;
    private Component owner;
    private Behavior inputStyle;
    private boolean customPlaceholder;
    private IModel<String> placeholder;

    public TagInputBehavior(FormComponent<?> input) {
        super("sw-tag-input sw-combobox");
        this.input = Objects.requireNonNull(input);
    }
    public TagInputBehavior setPlaceholder(String value) { return setPlaceholder(Model.of(value)); }
    public TagInputBehavior setPlaceholder(IModel<String> value) { placeholder = value; customPlaceholder = true; return this; }
    @Override public void bind(Component component) {
        super.bind(component);
        if (owner != null) throw new IllegalStateException("TagInputBehavior is already bound");
        owner = component;
        component.setOutputMarkupId(true);
        input.setOutputMarkupId(true);
        inputStyle = new Behavior() {
            @Override public void onComponentTag(Component field, ComponentTag tag) {
                tag.append("class", "sw-tag-input-native sw-input", " ");
                String value = customPlaceholder ? (placeholder == null ? null : placeholder.getObject())
                        : text(field, "placeholder", "Add tags…");
                if (value == null) tag.remove("placeholder"); else tag.put("placeholder", value);
            }
        };
        input.add(inputStyle);
    }
    @Override public void unbind(Component component) {
        if (inputStyle != null) input.remove(inputStyle);
        inputStyle = null;
        owner = null;
        super.unbind(component);
    }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.put("data-input", input.getMarkupId());
        tag.put("data-multiple", "true");
        tag.put("data-remove", text(component, "remove", "Remove {0}"));
        tag.put("data-added", text(component, "added", "Added {0}"));
        tag.put("data-removed", text(component, "removed", "Removed {0}"));
        tag.put("data-duplicate", text(component, "duplicate", "Already added {0}"));
        tag.put("data-instructions", text(component, "instructions", "Press Enter to add a tag. Use the remove buttons to delete tags."));
    }
    private static String text(Component component, String key, String fallback) {
        return LocalizedText.get(component, SwTagInput.class, "SwTagInput." + key, fallback);
    }
    @Override public void detach(Component component) {
        if (placeholder != null) placeholder.detach();
        super.detach(component);
    }
    @Override public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
        response.render(JavaScriptHeaderItem.forReference(TAG_INPUT_JS));
    }
}

package dev.jbaby.shadwick.components.form;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Field base with model-driven prefix/suffix text and native datalist suggestions.
 * Addons use Shadleaf input-group markup, while the input keeps its field:input path.
 * Prefixes and suffixes describe the input without changing its accessible name.
 */
public abstract class BaseSwInputField<T, C extends FormComponent<T>, F extends BaseSwInputField<T, C, F>> extends BaseSwField<T, C, F> {

    private final Label prefix;
    private final Label suffix;
    private final WebMarkupContainer datalist;
    private IModel<? extends List<?>> suggestions;

    /**
     * @param id the component id, also the resource key for a missing label
     * @param label the label, or {@code null} to look it up by {@code id}
     * @param model the model, or {@code null} to inherit one from a parent {@code CompoundPropertyModel}
     * @param description help text, or {@code null}
     */
    public BaseSwInputField(String id, IModel<String> label, IModel<T> model, IModel<String> description) {
        super(id, label, model, description);

        // Transparent, so the input and addons stay children of this panel and keep
        // their component paths whether or not the group is rendered
        add(new TransparentWebMarkupContainer("group") {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                // Checks the models: the addons are configured after the group
                setRenderBodyOnly(!hasAddons());
            }
        });

        prefix = newAddon("prefix");
        suffix = newAddon("suffix");
        add(prefix, suffix);

        datalist = new WebMarkupContainer("suggestions") {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(!isMultiline() && hasSuggestions());
            }
        };
        prefix.setOutputMarkupId(true);
        suffix.setOutputMarkupId(true);
        datalist.setOutputMarkupId(true);
        datalist.add(new ListView<Object>("option", (IModel<List<Object>>) () -> {
            List<?> values = suggestions != null ? suggestions.getObject() : null;
            return values != null ? List.copyOf(values) : List.of();
        }) {
            @Override
            protected void populateItem(ListItem<Object> item) {
                item.add(AttributeModifier.replace("value", String.valueOf(item.getModelObject())));
            }
        });
        add(datalist);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        // The browser offers the datalist's values as the user types
        if (!isMultiline()) {
            getInput().add(AttributeModifier.replace("list", (IModel<String>) () -> hasSuggestions() ? datalist.getMarkupId() : null));
        }
        getInput().add(new Behavior() {
            @Override
            public void onComponentTag(Component component, ComponentTag tag) {
                if (!hasAddons()) return;
                String existing = tag.getAttribute("class");
                List<String> classes = new ArrayList<>(Arrays.asList((existing != null ? existing : "").split("\\s+")));
                classes.remove("sw-input");
                classes.remove("sw-textarea");
                classes.add(isMultiline() ? "sw-input-group-textarea" : "sw-input-group-input");
                tag.put("class", String.join(" ", classes));
            }
        });
    }

    protected boolean isMultiline() { return false; }

    private boolean hasAddons() {
        return prefix.getDefaultModelObject() != null || suffix.getDefaultModelObject() != null;
    }

    @Override
    protected String getAdditionalDescribedByMarkupIds() {
        String ids = prefix.getDefaultModelObject() != null ? prefix.getMarkupId() : "";
        if (suffix.getDefaultModelObject() != null) ids += " " + suffix.getMarkupId();
        return ids.trim();
    }

    private boolean hasSuggestions() {
        return suggestions != null && suggestions.getObject() != null && !suggestions.getObject().isEmpty();
    }

    private Label newAddon(String id) {
        // An explicit (empty) model, so the label never inherits one from a parent
        // CompoundPropertyModel by its id
        return new Label(id, new Model<String>()) {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(getDefaultModelObject() != null);
            }
        };
    }

    /**
     * Values the browser suggests as the user types, from a native {@code <datalist>}:
     * e.g. existing tags, cities or product codes. The user can still type anything else;
     * add a validator to restrict the value.
     */
    public F setSuggestions(List<String> suggestions) {
        return setSuggestions(Model.ofList(suggestions));
    }

    /** Values the browser suggests as the user types, read on every render. */
    public F setSuggestions(IModel<? extends List<?>> suggestions) {
        if (isMultiline()) throw new IllegalStateException("Datalist suggestions require a single-line input");
        this.suggestions = suggestions;
        return self();
    }

    @Override
    protected void onDetach() {
        if (suggestions != null) {
            suggestions.detach();
        }
        super.onDetach();
    }

    /** Text shown before the input, such as {@code €} or {@code https://}. */
    public F setPrefix(String text) {
        return setPrefix(Model.of(text));
    }

    /** Text shown before the input; a {@code null} model or object hides it. */
    public F setPrefix(IModel<String> text) {
        prefix.setDefaultModel(text != null ? text : new Model<String>());
        return self();
    }

    /** Text shown after the input, such as {@code kg} or {@code %}. */
    public F setSuffix(String text) {
        return setSuffix(Model.of(text));
    }

    /** Text shown after the input; a {@code null} model or object hides it. */
    public F setSuffix(IModel<String> text) {
        suffix.setDefaultModel(text != null ? text : new Model<String>());
        return self();
    }
}

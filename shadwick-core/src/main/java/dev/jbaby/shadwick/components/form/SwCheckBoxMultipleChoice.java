package dev.jbaby.shadwick.components.form;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.AbstractChoice;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;

import java.util.Collection;
import java.util.List;

/**
 * A shadcn-styled group of checkboxes ({@link CheckBoxMultipleChoice}) in a
 * {@code <fieldset>}, labelled by its {@code <legend>}, bound to a collection of the
 * selected choices. The choices are stacked; {@link #setInline} puts them in a row.
 *
 * @param <T> the type of the choices
 */
public class SwCheckBoxMultipleChoice<T> extends BaseSwField<Collection<T>, CheckBoxMultipleChoice<T>, SwCheckBoxMultipleChoice<T>> {

    private final IModel<? extends List<? extends T>> choices;
    private final IChoiceRenderer<? super T> renderer;
    private boolean inline;

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwCheckBoxMultipleChoice(String id, IModel<? extends List<? extends T>> choices) {
        this(id, null, null, choices, null, null);
    }

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwCheckBoxMultipleChoice(String id, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, null, null, choices, renderer, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwCheckBoxMultipleChoice(String id, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices) {
        this(id, null, model, choices, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwCheckBoxMultipleChoice(String id, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, null, model, choices, renderer, null);
    }

    public SwCheckBoxMultipleChoice(String id, String label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices) {
        this(id, Model.of(label), model, choices, null, null);
    }

    public SwCheckBoxMultipleChoice(String id, String label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, Model.of(label), model, choices, renderer, null);
    }

    public SwCheckBoxMultipleChoice(String id, IModel<String> label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices) {
        this(id, label, model, choices, null, null);
    }

    public SwCheckBoxMultipleChoice(String id, IModel<String> label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, label, model, choices, renderer, null);
    }

    @SuppressWarnings("unchecked")
    public SwCheckBoxMultipleChoice(String id, IModel<String> label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer, IModel<String> description) {
        super(id, label, (IModel<Collection<T>>) model, description);
        this.choices = choices;
        this.renderer = renderer;
        get("label").setOutputMarkupId(true);
        // ARIA group does not support aria-required; marking every checkbox required
        // would mean all must be checked, rather than at least one.
        getInput().add(AttributeModifier.remove("aria-required"));
    }

    /** Lays the choices out in a row instead of a column. */
    public SwCheckBoxMultipleChoice<T> setInline(boolean inline) {
        this.inline = inline;
        return this;
    }

    @Override
    protected CheckBoxMultipleChoice<T> newInput(String id, IModel<Collection<T>> model) {
        CheckBoxMultipleChoice<T> choice = new CheckBoxMultipleChoice<>(id, model, choices, renderer) {
            @Override
            public void validate() {
                super.validate();
                // Wicket checks required against raw IDs. Unknown IDs resolve to an
                // empty collection, which must not satisfy a required selection.
                if (isValid() && isRequired() && getConvertedInput().isEmpty()) {
                    reportRequiredError();
                }
            }

            @Override
            protected IValueMap getAdditionalAttributes(int index, T option) {
                IValueMap attributes = new ValueMap();
                attributes.put("class", "sw-checkbox");
                attributes.put("aria-describedby", getDescribedByMarkupIds());
                attributes.put("aria-invalid", hasErrorMessage() ? "true" : "false");
                return attributes;
            }

            @Override
            protected IValueMap getAdditionalAttributesForLabel(int index, T option) {
                IValueMap attributes = new ValueMap();
                attributes.put("class", "sw-label");
                return attributes;
            }
        };
        choice.setLabelPosition(AbstractChoice.LabelPosition.WRAP_AFTER);
        // Keep Wicket's label/value escaping and native selection logic. The static
        // sibling icon overlays the checkbox without rewriting Wicket's generated HTML.
        choice.setPrefix("<div class=\"sw-checkbox-option\">");
        choice.setSuffix("<svg class=\"sw-checkbox-indicator\" xmlns=\"http://www.w3.org/2000/svg\""
                + " viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\""
                + " stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\" focusable=\"false\">"
                + "<path d=\"m20 6-11 11-5-5\"/></svg></div>");
        choice.add(AttributeModifier.replace("data-orientation", (IModel<String>) () -> inline ? "horizontal" : null));
        choice.add(AttributeModifier.replace("aria-labelledby", (IModel<String>) () -> get("label").getMarkupId()));
        return choice;
    }
}

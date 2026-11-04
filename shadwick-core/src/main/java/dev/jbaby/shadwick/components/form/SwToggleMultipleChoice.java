package dev.jbaby.shadwick.components.form;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.AbstractChoice;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;

import java.util.Collection;
import java.util.List;

/**
 * A multiple-selection toggle group of native checkboxes ({@link CheckBoxMultipleChoice}) in a
 * {@code <fieldset>}, labelled by its {@code <legend>}, bound to a collection of the
 * selected choices. Space toggles each option independently.
 *
 * @param <T> the type of the choices
 */
public class SwToggleMultipleChoice<T> extends BaseSwToggleGroup<Collection<T>, CheckBoxMultipleChoice<T>, SwToggleMultipleChoice<T>> {

    private final IModel<? extends List<? extends T>> choices;
    private final IChoiceRenderer<? super T> renderer;

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwToggleMultipleChoice(String id, IModel<? extends List<? extends T>> choices) {
        this(id, null, null, choices, null, null);
    }

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwToggleMultipleChoice(String id, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, null, null, choices, renderer, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwToggleMultipleChoice(String id, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices) {
        this(id, null, model, choices, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwToggleMultipleChoice(String id, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, null, model, choices, renderer, null);
    }

    public SwToggleMultipleChoice(String id, String label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices) {
        this(id, Model.of(label), model, choices, null, null);
    }

    public SwToggleMultipleChoice(String id, String label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, Model.of(label), model, choices, renderer, null);
    }

    public SwToggleMultipleChoice(String id, IModel<String> label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices) {
        this(id, label, model, choices, null, null);
    }

    public SwToggleMultipleChoice(String id, IModel<String> label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, label, model, choices, renderer, null);
    }

    @SuppressWarnings("unchecked")
    public SwToggleMultipleChoice(String id, IModel<String> label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer, IModel<String> description) {
        super(id, label, (IModel<Collection<T>>) model, description);
        this.choices = choices;
        this.renderer = renderer;
        // ARIA group does not support aria-required; marking every checkbox required
        // would mean all must be checked, rather than at least one.
        getInput().add(AttributeModifier.remove("aria-required"));
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
                return toggleAttributes(hasErrorMessage());
            }

            @Override
            protected IValueMap getAdditionalAttributesForLabel(int index, T option) {
                return toggleLabelAttributes();
            }
        };
        choice.setLabelPosition(AbstractChoice.LabelPosition.WRAP_AFTER);
        choice.setPrefix("").setSuffix("");
        return configureGroup(choice);
    }
}

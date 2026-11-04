package dev.jbaby.shadwick.components.form;

import org.apache.wicket.markup.html.form.AbstractChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;

import java.util.List;

/**
 * A single-selection toggle group of native radios in a labelled fieldset.
 * Labels and values are escaped by Wicket. Use AjaxFormChoiceComponentUpdatingBehavior for selection callbacks.
 *
 * @param <T> the choice type
 */
public class SwToggleChoice<T> extends BaseSwToggleGroup<T, RadioChoice<T>, SwToggleChoice<T>> {

    private final IModel<? extends List<? extends T>> choices;
    private final IChoiceRenderer<? super T> renderer;

    /** Label and selected model inherited by id, as on other Shadwick fields. */
    public SwToggleChoice(String id, IModel<? extends List<? extends T>> choices) {
        this(id, null, null, choices, null, null);
    }

    public SwToggleChoice(String id, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, null, null, choices, renderer, null);
    }

    public SwToggleChoice(String id, IModel<T> model, IModel<? extends List<? extends T>> choices) {
        this(id, null, model, choices, null, null);
    }

    public SwToggleChoice(String id, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, null, model, choices, renderer, null);
    }

    public SwToggleChoice(String id, String label, IModel<T> model, IModel<? extends List<? extends T>> choices) {
        this(id, Model.of(label), model, choices, null, null);
    }

    public SwToggleChoice(String id, IModel<String> label, IModel<T> model, IModel<? extends List<? extends T>> choices) {
        this(id, label, model, choices, null, null);
    }

    public SwToggleChoice(String id, String label, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, Model.of(label), model, choices, renderer, null);
    }

    public SwToggleChoice(String id, IModel<String> label, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, label, model, choices, renderer, null);
    }

    public SwToggleChoice(String id, IModel<String> label, IModel<T> model, IModel<? extends List<? extends T>> choices,
                         IChoiceRenderer<? super T> renderer, IModel<String> description) {
        super(id, label, model, description);
        this.choices = choices;
        this.renderer = renderer;
    }

    @Override
    protected RadioChoice<T> newInput(String id, IModel<T> model) {
        RadioChoice<T> choice = new RadioChoice<>(id, model, choices, renderer) {
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

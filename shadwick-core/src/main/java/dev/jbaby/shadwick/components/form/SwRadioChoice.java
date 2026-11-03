package dev.jbaby.shadwick.components.form;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.AbstractChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;

import java.util.List;

/**
 * A native RadioChoice in a fieldset with a legend. Choices are stacked by default; setInline puts them in a row.
 * Labels and values are escaped by Wicket. Use AjaxFormChoiceComponentUpdatingBehavior for selection callbacks.
 *
 * @param <T> the choice type
 */
public class SwRadioChoice<T> extends BaseSwField<T, RadioChoice<T>, SwRadioChoice<T>> {

    private final IModel<? extends List<? extends T>> choices;
    private final IChoiceRenderer<? super T> renderer;
    private boolean inline;

    /** Label and selected model inherited by id, as on other Shadwick fields. */
    public SwRadioChoice(String id, IModel<? extends List<? extends T>> choices) {
        this(id, null, null, choices, null, null);
    }

    public SwRadioChoice(String id, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, null, null, choices, renderer, null);
    }

    public SwRadioChoice(String id, IModel<T> model, IModel<? extends List<? extends T>> choices) {
        this(id, null, model, choices, null, null);
    }

    public SwRadioChoice(String id, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, null, model, choices, renderer, null);
    }

    public SwRadioChoice(String id, String label, IModel<T> model, IModel<? extends List<? extends T>> choices) {
        this(id, Model.of(label), model, choices, null, null);
    }

    public SwRadioChoice(String id, IModel<String> label, IModel<T> model, IModel<? extends List<? extends T>> choices) {
        this(id, label, model, choices, null, null);
    }

    public SwRadioChoice(String id, String label, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, Model.of(label), model, choices, renderer, null);
    }

    public SwRadioChoice(String id, IModel<String> label, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, label, model, choices, renderer, null);
    }

    public SwRadioChoice(String id, IModel<String> label, IModel<T> model, IModel<? extends List<? extends T>> choices,
                         IChoiceRenderer<? super T> renderer, IModel<String> description) {
        super(id, label, model, description);
        this.choices = choices;
        this.renderer = renderer;
        get("label").setOutputMarkupId(true);
    }

    public SwRadioChoice<T> setInline(boolean inline) {
        this.inline = inline;
        return this;
    }

    @Override
    protected RadioChoice<T> newInput(String id, IModel<T> model) {
        RadioChoice<T> choice = new RadioChoice<>(id, model, choices, renderer) {
            @Override
            protected IValueMap getAdditionalAttributes(int index, T option) {
                IValueMap attributes = new ValueMap();
                attributes.put("class", "sw-radio-group-item");
                attributes.put("aria-describedby", getDescribedByMarkupIds());
                attributes.put("aria-invalid", hasErrorMessage() ? "true" : "false");
                return attributes;
            }

            @Override
            protected IValueMap getAdditionalAttributesForLabel(int index, T option) {
                IValueMap attributes = new ValueMap();
                attributes.put("class", "sw-label sw-radio-label");
                return attributes;
            }
        };
        choice.setLabelPosition(AbstractChoice.LabelPosition.WRAP_AFTER);
        choice.setPrefix("").setSuffix("");
        choice.add(AttributeModifier.replace("data-orientation", (IModel<String>) () -> inline ? "horizontal" : null));
        choice.add(AttributeModifier.replace("aria-labelledby", (IModel<String>) () -> get("label").getMarkupId()));
        return choice;
    }
}

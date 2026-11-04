package dev.jbaby.shadwick.components.form;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Collection;
import java.util.List;

/**
 * A shadcn-styled multi-select ({@code <select multiple>}, {@link ListMultipleChoice}),
 * bound to a collection of the selected choices.
 *
 * @param <T> the type of the choices
 */
public class SwListMultipleChoice<T> extends BaseSwField<Collection<T>, ListMultipleChoice<T>, SwListMultipleChoice<T>> {

    private final IModel<? extends List<? extends T>> choices;
    private final IChoiceRenderer<? super T> renderer;

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwListMultipleChoice(String id, IModel<? extends List<? extends T>> choices) {
        this(id, null, null, choices, null, null);
    }

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwListMultipleChoice(String id, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, null, null, choices, renderer, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwListMultipleChoice(String id, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices) {
        this(id, null, model, choices, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwListMultipleChoice(String id, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, null, model, choices, renderer, null);
    }

    public SwListMultipleChoice(String id, String label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices) {
        this(id, Model.of(label), model, choices, null, null);
    }

    public SwListMultipleChoice(String id, String label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, Model.of(label), model, choices, renderer, null);
    }

    public SwListMultipleChoice(String id, IModel<String> label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices) {
        this(id, label, model, choices, null, null);
    }

    public SwListMultipleChoice(String id, IModel<String> label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, label, model, choices, renderer, null);
    }

    @SuppressWarnings("unchecked")
    public SwListMultipleChoice(String id, IModel<String> label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer, IModel<String> description) {
        super(id, label, (IModel<Collection<T>>) model, description);
        this.choices = choices;
        this.renderer = renderer;
    }

    /** The number of visible rows of the list. */
    public SwListMultipleChoice<T> setMaxRows(int maxRows) {
        getInput().setMaxRows(maxRows);
        return this;
    }

    @Override
    protected ListMultipleChoice<T> newInput(String id, IModel<Collection<T>> model) {
        return new ListMultipleChoice<>(id, model, choices, renderer) {
            @Override
            public void validate() {
                super.validate();
                // Wicket checks required against raw IDs. Unknown IDs resolve to an
                // empty collection, which must not satisfy a required selection.
                if (isValid() && isRequired() && getConvertedInput().isEmpty()) {
                    reportRequiredError();
                }
            }
        };
    }
}

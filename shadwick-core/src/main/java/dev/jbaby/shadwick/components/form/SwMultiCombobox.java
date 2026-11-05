package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.util.SerializableFunction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.apache.wicket.util.string.Strings;

/**
 * A searchable multiple-choice field with chips, backed by {@link ListMultipleChoice},
 * bound to a collection of the selected choices.
 *
 * @param <T> the type of the choices
 */
public class SwMultiCombobox<T> extends BaseSwField<Collection<T>, ListMultipleChoice<T>, SwMultiCombobox<T>> {

    private final ComboboxOptions<T> options;

    public SwMultiCombobox(String id) { this(id, null, null, null, null, null); }
    public SwMultiCombobox(String id, String label, IModel<? extends Collection<T>> model) { this(id, Model.of(label), model, null, null, null); }

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwMultiCombobox(String id, IModel<? extends List<? extends T>> choices) {
        this(id, null, null, choices, null, null);
    }

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwMultiCombobox(String id, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, null, null, choices, renderer, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwMultiCombobox(String id, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices) {
        this(id, null, model, choices, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwMultiCombobox(String id, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, null, model, choices, renderer, null);
    }

    public SwMultiCombobox(String id, String label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices) {
        this(id, Model.of(label), model, choices, null, null);
    }

    public SwMultiCombobox(String id, String label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, Model.of(label), model, choices, renderer, null);
    }

    public SwMultiCombobox(String id, IModel<String> label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices) {
        this(id, label, model, choices, null, null);
    }

    public SwMultiCombobox(String id, IModel<String> label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, label, model, choices, renderer, null);
    }

    @SuppressWarnings("unchecked")
    public SwMultiCombobox(String id, IModel<String> label, IModel<? extends Collection<T>> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer, IModel<String> description) {
        super(id, label, (IModel<Collection<T>>) model, description);
        this.options = new ComboboxOptions<>(choices,renderer);
    }

    public SwMultiCombobox<T> setDisplay(SerializableFunction<? super T, String> value) { options.display=Objects.requireNonNull(value); return this; }
    public SwMultiCombobox<T> setChoiceId(SerializableFunction<? super T, String> value) { options.id=Objects.requireNonNull(value); return this; }
    public SwMultiCombobox<T> setGroup(SerializableFunction<? super T, String> value) { options.group=Objects.requireNonNull(value); return this; }
    public SwMultiCombobox<T> setIcon(SerializableFunction<? super T, String> value) { options.icon=Objects.requireNonNull(value); return this; }
    public SwMultiCombobox<T> setDisabledWhen(SerializableFunction<? super T, Boolean> value) { options.disabled=Objects.requireNonNull(value); return this; }
    public SwMultiCombobox<T> setSeparatorBefore(SerializableFunction<? super T, Boolean> value) { options.separator=Objects.requireNonNull(value); return this; }
    /** Bounded server search. Requires stable identity and a resolver for current authorized values. */
    public SwMultiCombobox<T> setChoices(SerializableFunction<String, ? extends List<? extends T>> value) { options.search=Objects.requireNonNull(value); return this; }
    public SwMultiCombobox<T> setChoiceResolver(SerializableFunction<String, ? extends T> value) { options.resolver=Objects.requireNonNull(value); return this; }
    public SwMultiCombobox<T> setMaxChoices(int value) { if (value<1||value>200) throw new IllegalArgumentException("maxChoices must be between 1 and 200"); options.maxChoices=value; return this; }
    public SwMultiCombobox<T> setShowClear(boolean value) { options.appearance.setShowClear(value); return this; }
    public SwMultiCombobox<T> setAutoHighlight(boolean value) { options.appearance.setAutoHighlight(value); return this; }
    public SwMultiCombobox<T> setEmpty(String value) { return setEmpty(Model.of(value)); }
    public SwMultiCombobox<T> setEmpty(IModel<String> value) { options.appearance.setEmpty(value); return this; }
    @Override public SwMultiCombobox<T> setPlaceholder(String value) { return setPlaceholder(Model.of(value)); }
    @Override public SwMultiCombobox<T> setPlaceholder(IModel<String> value) { options.appearance.setPlaceholder(value); return this; }
    private Collection<T> selectedForRender() {
        List<T> values = new ArrayList<>();
        if (getModelObject() != null) values.addAll(getModelObject());
        if (getInput().hasRawInput() && getInput().isValid() && getInput().getConvertedInput() != null) values.addAll(getInput().getConvertedInput());
        return values;
    }
    @Override protected void onInitialize() {
        super.onInitialize();
        getInput().add(options.callback);
        add(new TransparentWebMarkupContainer("comboboxWrapper").add(options.appearance));
    }
    @Override protected ListMultipleChoice<T> newInput(String id, IModel<Collection<T>> model) {
        return new ListMultipleChoice<T>(id,model,options.snapshot(this::selectedForRender),options) {
            @Override public void onComponentTagBody(MarkupStream stream, ComponentTag tag) {
                String selected=getValue();
                String html=options.render(getChoices(), index -> {
                    T choice=getChoices().get(index);
                    AppendingStringBuffer option=new AppendingStringBuffer();
                    super.appendOptionHtml(option,choice,index,selected);
                    return options.decorate(option.toString(),choice);
                });
                replaceComponentTagBody(stream,tag,html);
            }
            @Override protected boolean isDisabled(T value,int index,String selected) { return options.isDisabled(value); }
            @Override protected Collection<T> convertValue(String[] ids) {
                ArrayList<T> selected=new ArrayList<>();
                if (ids != null) for (String id : new java.util.LinkedHashSet<>(List.of(ids))) {
                    if (id.isEmpty()) throw new org.apache.wicket.util.convert.ConversionException("Empty choice ID").setResourceKey("SwCombobox.unavailable");
                    selected.add(options.resolve(id,getChoicesModel()));
                }
                return selected;
            }
            @Override public void validate() {
                super.validate();
                if (isValid() && isRequired() && getConvertedInput().isEmpty()) reportRequiredError();
            }
        };
    }
}

package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.util.SerializableFunction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.apache.wicket.util.string.Strings;

/**
 * A searchable single-choice field backed by a real {@link DropDownChoice}.
 *
 * @param <T> the type of the selectable choices
 */
public class SwCombobox<T> extends BaseSwField<T, DropDownChoice<T>, SwCombobox<T>> {

    private final ComboboxOptions<T> options;

    public SwCombobox(String id) { this(id, null, null, null, null, null); }
    public SwCombobox(String id, String label, IModel<T> model) { this(id, Model.of(label), model, null, null, null); }

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwCombobox(String id, IModel<? extends List<? extends T>> choices) {
        this(id, null, null, choices, null, null);
    }

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwCombobox(String id, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, null, null, choices, renderer, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwCombobox(String id, IModel<T> model, IModel<? extends List<? extends T>> choices) {
        this(id, null, model, choices, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwCombobox(String id, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, null, model, choices, renderer, null);
    }

    public SwCombobox(String id, String label, IModel<T> model, IModel<? extends List<? extends T>> choices) {
        this(id, Model.of(label), model, choices, null, null);
    }

    public SwCombobox(String id, String label, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, Model.of(label), model, choices, renderer, null);
    }

    public SwCombobox(String id, IModel<String> label, IModel<T> model, IModel<? extends List<? extends T>> choices) {
        this(id, label, model, choices, null, null);
    }

    public SwCombobox(String id, IModel<String> label, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, label, model, choices, renderer, null);
    }

    public SwCombobox(String id, IModel<String> label, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer, IModel<String> description) {
        super(id, label, model, description);
        this.options = new ComboboxOptions<>(choices,renderer);
    }

    public SwCombobox<T> setDisplay(SerializableFunction<? super T, String> value) { options.display=Objects.requireNonNull(value); return this; }
    public SwCombobox<T> setChoiceId(SerializableFunction<? super T, String> value) { options.id=Objects.requireNonNull(value); return this; }
    public SwCombobox<T> setGroup(SerializableFunction<? super T, String> value) { options.group=Objects.requireNonNull(value); return this; }
    public SwCombobox<T> setIcon(SerializableFunction<? super T, String> value) { options.icon=Objects.requireNonNull(value); return this; }
    public SwCombobox<T> setDisabledWhen(SerializableFunction<? super T, Boolean> value) { options.disabled=Objects.requireNonNull(value); return this; }
    public SwCombobox<T> setSeparatorBefore(SerializableFunction<? super T, Boolean> value) { options.separator=Objects.requireNonNull(value); return this; }
    /** Bounded server search. Requires stable identity and a resolver for current authorized values. */
    public SwCombobox<T> setChoices(SerializableFunction<String, ? extends List<? extends T>> value) { options.search=Objects.requireNonNull(value); return this; }
    public SwCombobox<T> setChoiceResolver(SerializableFunction<String, ? extends T> value) { options.resolver=Objects.requireNonNull(value); return this; }
    public SwCombobox<T> setMaxChoices(int value) { if (value<1||value>200) throw new IllegalArgumentException("maxChoices must be between 1 and 200"); options.maxChoices=value; return this; }
    public SwCombobox<T> setShowClear(boolean value) { options.appearance.setShowClear(value); return this; }
    public SwCombobox<T> setAutoHighlight(boolean value) { options.appearance.setAutoHighlight(value); return this; }
    public SwCombobox<T> setEmpty(String value) { return setEmpty(Model.of(value)); }
    public SwCombobox<T> setEmpty(IModel<String> value) { options.appearance.setEmpty(value); return this; }
    @Override public SwCombobox<T> setPlaceholder(String value) { return setPlaceholder(Model.of(value)); }
    @Override public SwCombobox<T> setPlaceholder(IModel<String> value) { options.appearance.setPlaceholder(value); return this; }
    private Collection<T> selectedForRender() {
        List<T> values = new ArrayList<>();
        if (getModelObject() != null) values.add(getModelObject());
        if (getInput().hasRawInput() && getInput().isValid() && getInput().getConvertedInput() != null) values.add(getInput().getConvertedInput());
        return values;
    }
    @Override protected void onInitialize() {
        super.onInitialize();
        getInput().add(options.callback);
        add(new TransparentWebMarkupContainer("comboboxWrapper").add(options.appearance));
    }
    @Override protected DropDownChoice<T> newInput(String id, IModel<T> model) {
        return new DropDownChoice<T>(id,model,options.snapshot(this::selectedForRender),options) {
            @Override public void onComponentTagBody(MarkupStream stream, ComponentTag tag) {
                String selected=getValue();
                String html=options.render(getChoices(), index -> {
                    T choice=getChoices().get(index);
                    AppendingStringBuffer option=new AppendingStringBuffer();
                    super.appendOptionHtml(option,choice,index,selected);
                    return options.decorate(option.toString(),choice);
                });
                html="<option value=\"\" data-placeholder=\"true\" hidden"+(Strings.isEmpty(selected)?" selected=\"selected\"":"")+">"+Strings.escapeMarkup(options.appearance.getPlaceholderText(this))+"</option>"+html;
                replaceComponentTagBody(stream,tag,html);
            }
            @Override protected boolean isDisabled(T value,int index,String selected) { return options.isDisabled(value); }
            @Override protected T convertChoiceIdToChoice(String id) { return options.resolve(id,getChoicesModel()); }
        };
    }
}

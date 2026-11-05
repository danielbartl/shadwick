package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.behaviors.SelectBehavior;
import dev.jbaby.shadwick.components.SwIcon;
import dev.jbaby.shadwick.util.SerializableFunction;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.apache.wicket.util.string.Strings;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A custom single-choice picker with a real {@link DropDownChoice} for binding,
 * validation, reset and submission. Native customizable select is used where supported;
 * other modern browsers receive a progressively enhanced listbox. Without script the
 * platform picker remains available. Searchable selection belongs to combobox.
 *
 * @param <T> the type of the selectable choices
 */
public class SwSelect<T> extends BaseSwField<T, DropDownChoice<T>, SwSelect<T>> {

    private final IModel<? extends List<? extends T>> choices;
    private final IChoiceRenderer<? super T> renderer;
    private IModel<String> placeholder;
    private SerializableFunction<? super T, String> group;
    private SerializableFunction<? super T, String> icon;
    private SerializableFunction<? super T, Boolean> disabled;
    private SerializableFunction<? super T, Boolean> separator;
    private final SelectBehavior appearance = new SelectBehavior();

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwSelect(String id, IModel<? extends List<? extends T>> choices) {
        this(id, null, null, choices, null, null);
    }

    /**
     * Label looked up by {@code id} in the {@code .properties} files, model inherited
     * from a parent {@code CompoundPropertyModel}.
     */
    public SwSelect(String id, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, null, null, choices, renderer, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwSelect(String id, IModel<T> model, IModel<? extends List<? extends T>> choices) {
        this(id, null, model, choices, null, null);
    }

    /** Label looked up by {@code id} in the {@code .properties} files. */
    public SwSelect(String id, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, null, model, choices, renderer, null);
    }

    public SwSelect(String id, String label, IModel<T> model, IModel<? extends List<? extends T>> choices) {
        this(id, Model.of(label), model, choices, null, null);
    }

    public SwSelect(String id, String label, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, Model.of(label), model, choices, renderer, null);
    }

    public SwSelect(String id, IModel<String> label, IModel<T> model, IModel<? extends List<? extends T>> choices) {
        this(id, label, model, choices, null, null);
    }

    public SwSelect(String id, IModel<String> label, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer) {
        this(id, label, model, choices, renderer, null);
    }

    public SwSelect(String id, IModel<String> label, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<? super T> renderer, IModel<String> description) {
        super(id, label, model, description);
        this.choices = choices;
        this.renderer = renderer;
    }

    /** Standard or compact trigger geometry. */
    public SwSelect<T> setSize(SelectBehavior.Size size) { appearance.setSize(size); return this; }

    /** Adjacent choices with the same nonblank group label share a native optgroup. */
    public SwSelect<T> setGroup(SerializableFunction<? super T, String> group) { this.group = group; return this; }

    /** Optional registered Lucide icon name for each choice; null omits the icon. */
    public SwSelect<T> setIcon(SerializableFunction<? super T, String> icon) { this.icon = icon; return this; }

    /** Disabled choices are rejected on the server as well as in the picker. */
    public SwSelect<T> setDisabledWhen(SerializableFunction<? super T, Boolean> disabled) { this.disabled = disabled; return this; }

    /** Insert a visual separator before matching choices, preserving choice order and IDs. */
    public SwSelect<T> setSeparatorBefore(SerializableFunction<? super T, Boolean> separator) { this.separator = separator; return this; }

    @Override public SwSelect<T> setPlaceholder(String placeholder) { return setPlaceholder(Model.of(placeholder)); }
    @Override public SwSelect<T> setPlaceholder(IModel<String> placeholder) { this.placeholder = placeholder; return this; }

    @Override protected void onInitialize() {
        super.onInitialize();
        add(new TransparentWebMarkupContainer("selectWrapper").add(appearance));
    }

    @Override protected void onDetach() {
        if (placeholder != null) placeholder.detach();
        super.onDetach();
    }

    @Override
    protected DropDownChoice<T> newInput(String id, IModel<T> model) {
        // Snapshot a dynamic choice model once per request; Wicket also reads it during conversion.
        IModel<List<? extends T>> snapshot = new IModel<>() {
            private transient List<? extends T> current;
            @Override public List<? extends T> getObject() {
                if (current == null) {
                    List<? extends T> loaded = choices == null ? null : choices.getObject();
                    current = loaded == null ? Collections.emptyList() : List.copyOf(loaded);
                }
                return current;
            }
            @Override public void detach() {
                current = null;
                if (choices != null) choices.detach();
            }
        };
        return new DropDownChoice<T>(id, model, snapshot, renderer) {
            @Override public void onComponentTagBody(MarkupStream stream, ComponentTag tag) {
                AppendingStringBuffer html = new AppendingStringBuffer("<button type=\"button\"><selectedcontent></selectedcontent></button>");
                String selected = getValue();
                html.append(getDefaultChoice(selected));
                String previous = null;
                List<? extends T> values = getChoices();
                for (int i = 0; i < values.size(); i++) {
                    T choice = values.get(i);
                    String heading = group == null ? null : group.apply(choice);
                    if (heading != null && heading.isBlank()) heading = null;
                    boolean split = separator != null && Boolean.TRUE.equals(separator.apply(choice));
                    if (!Objects.equals(previous, heading) || split) {
                        if (previous != null) html.append("</optgroup>");
                        if (split && i > 0) html.append("<hr class=\"sw-select-separator\">");
                        if (heading != null) html.append("<optgroup class=\"sw-select-group\" label=\"").append(Strings.escapeMarkup(heading)).append("\">");
                    }
                    previous = heading;
                    appendOptionHtml(html, choice, i, selected);
                }
                if (previous != null) html.append("</optgroup>");
                replaceComponentTagBody(stream, tag, html);
            }

            @Override protected CharSequence getDefaultChoice(String selected) {
                String text = placeholder == null ? getString("SwSelect.placeholder", null, "Select an option") : placeholder.getObject();
                return "<option class=\"sw-select-item\" data-placeholder=\"true\" value=\"\""
                        + (isNullValid() ? "" : " hidden") + (Strings.isEmpty(selected) ? " selected=\"selected\"" : "")
                        + ">" + Strings.escapeMarkup(text == null ? "" : text) + "</option>";
            }

            @Override protected void setOptionAttributes(AppendingStringBuffer html, T choice, int index, String selected) {
                super.setOptionAttributes(html, choice, index, selected);
                html.append(" class=\"sw-select-item\"");
            }

            @Override protected void appendOptionHtml(AppendingStringBuffer html, T choice, int index, String selected) {
                // Keep Wicket's display converter, localization and escaping contract.
                AppendingStringBuffer option = new AppendingStringBuffer();
                super.appendOptionHtml(option, choice, index, selected);
                String markup = option.toString();
                int start = markup.indexOf('>') + 1;
                String name = icon == null ? null : icon.apply(choice);
                html.append(markup.substring(0, start));
                if (name != null) html.append(SwIcon.decorativeMarkup(name));
                html.append(markup.substring(start, markup.lastIndexOf("</option>")));
                html.append(SwIcon.decorativeMarkup("check").replace("class=\"sw-icon\"", "class=\"sw-icon sw-select-item-indicator\""));
                html.append("</option>");
            }

            @Override protected boolean isDisabled(T choice, int index, String selected) {
                return disabled != null && Boolean.TRUE.equals(disabled.apply(choice));
            }

            @Override protected T convertChoiceIdToChoice(String id) {
                if (Strings.isEmpty(id)) return null;
                List<? extends T> values = getChoices();
                for (int i = 0; i < values.size(); i++) {
                    T choice = values.get(i);
                    if (Objects.equals(id, getChoiceRenderer().getIdValue(choice, i))) {
                        if (isDisabled(choice, i, id)) break;
                        T converted = super.convertChoiceIdToChoice(id);
                        if (converted != null && (disabled == null || !Boolean.TRUE.equals(disabled.apply(converted)))) return converted;
                        break;
                    }
                }
                throw new ConversionException("The selected option is unavailable.")
                        .setResourceKey("SwSelect.unavailable");
            }
        };
    }
}

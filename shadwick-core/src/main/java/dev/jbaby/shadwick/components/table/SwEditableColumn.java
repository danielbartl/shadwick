package dev.jbaby.shadwick.components.table;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.behaviors.NestedFormButtonBehavior;
import dev.jbaby.shadwick.components.SwIcon;
import dev.jbaby.shadwick.components.form.ShowsErrorsInline;
import org.apache.wicket.Component;
import dev.jbaby.shadwick.util.SerializableBiConsumer;
import dev.jbaby.shadwick.util.SerializableFunction;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import java.util.Optional;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.StyledBehavior;
import dev.jbaby.shadwick.util.LocalizedText;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.IValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A data table column whose cells can be edited in place - a quantity, a price, a
 * name. A cell shows its value as a button; clicking it (or Enter/Space) turns it into
 * a text field with Save and Cancel. Enter saves, Escape cancels.
 * <pre>{@code
 * columns.add(new SwEditableColumn<Line, String, Integer>(Model.of("Quantity"), "quantity",
 *                 Line::quantity, Line::setQuantity)
 *         .setType(Integer.class)
 *         .setRequired(true)
 *         .addValidator(RangeValidator.minimum(1))
 *         .setOnSave((target, line) -> { lines.save(line); target.add(totals); }));
 * }</pre>
 * The typed text is converted to the column's type and validated; an invalid value
 * shows its error under the field and isn't set. A valid one is set on the row with
 * the setter, then {@link #setOnSave} runs, e.g. to store it. Focus goes back to the cell
 * afterwards, so keyboard users can move on. The value is exported to CSV like any
 * {@link LambdaColumn}.
 *
 * @param <T> the row type
 * @param <S> the sort property type
 * @param <V> the value type
 */
public class SwEditableColumn<T, S, V> extends LambdaColumn<T, S> {

    private final SerializableFunction<T, V> getter;
    private final SerializableBiConsumer<T, V> setter;
    private final List<IValidator<? super V>> validators = new ArrayList<>();
    private Class<V> type;
    private boolean required;
    private SerializableFunction<T, Boolean> editableWhen = row -> true;
    /** Rechecked before opening and committing an editor. The application owns authorization and persistence. */
    public SwEditableColumn<T, S, V> setEditableWhen(SerializableFunction<T, Boolean> predicate) { editableWhen = Objects.requireNonNull(predicate); return this; }
    private boolean editable(T row) { return row != null && Boolean.TRUE.equals(editableWhen.apply(row)); }
    private SerializableBiConsumer<AjaxRequestTarget, T> onSave = (target, row) -> { };

    public SwEditableColumn(IModel<String> displayModel, SerializableFunction<T, V> getter, SerializableBiConsumer<T, V> setter) {
        this(displayModel, null, getter, setter);
    }

    public SwEditableColumn(IModel<String> displayModel, S sortProperty, SerializableFunction<T, V> getter,
                             SerializableBiConsumer<T, V> setter) {
        super(displayModel, sortProperty, getter::apply);
        this.getter = Objects.requireNonNull(getter);
        this.setter = Objects.requireNonNull(setter);
    }

    /** The value's type, for converting the typed text, e.g. {@code Integer.class}; text by default. */
    public SwEditableColumn<T, S, V> setType(Class<V> type) {
        this.type = type;
        return this;
    }

    public SwEditableColumn<T, S, V> setRequired(boolean required) {
        this.required = required;
        return this;
    }

    public SwEditableColumn<T, S, V> addValidator(IValidator<? super V> validator) {
        validators.add(Objects.requireNonNull(validator));
        return this;
    }

    /** Runs after a valid value was set. Target is null on native submission. An exception restores the in-memory
     * value and propagates; the application must supply its own persistence transaction. */
    public SwEditableColumn<T, S, V> setOnSave(SerializableBiConsumer<AjaxRequestTarget, T> onSave) {
        this.onSave = Objects.requireNonNull(onSave);
        return this;
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> item, String componentId, IModel<T> rowModel) {
        item.add(new EditableCell(componentId, rowModel));
    }

    final class EditableCell extends Panel implements ShowsErrorsInline {

        private final IModel<T> row;

        EditableCell(String id, IModel<T> row) {
            super(id);
            this.row = row;
            setOutputMarkupId(true);
            add(view());
        }

        @Override
        public boolean showsErrorsOf(Component reporter) {
            return reporter.getParent() instanceof Form<?> && "input".equals(reporter.getId()) && reporter.findParent(EditableCell.class) == this;
        }

        private Fragment view() {
            Fragment view = new Fragment("content", "viewFragment", this);
            AjaxFallbackLink<Void> edit = new AjaxFallbackLink<>("edit") {
                @Override public boolean isEnabled() { return super.isEnabled() && editable(row.getObject()); }
                @Override
                public void onClick(Optional<AjaxRequestTarget> target) {
                    if (!editable(row.getObject())) return;
                    Fragment editor = editor();
                    EditableCell.this.replace(editor);
                    target.ifPresent(t -> { t.add(EditableCell.this); t.focusComponent(editor.get("form:input")); });
                }
            };
            edit.add(new ButtonBehavior(ButtonBehavior.Variant.GHOST).setSize(ButtonBehavior.Size.SMALL), new StyledBehavior("sw-table-editable"));
            edit.setOutputMarkupId(true);
            IModel<V> value = () -> getter.apply(row.getObject());
            edit.add(new Label("value", () -> value.getObject() == null ? "\u2014" : value.getObject()) {
                @Override
                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);
                    if (value.getObject() == null) {
                        tag.append("class", "sw-table-empty-value", " ");
                    }
                }
            });
            edit.add(AttributeModifier.replace("aria-label", (IModel<String>) () -> java.text.MessageFormat.format(
                    text("edit", "Edit {0}: {1}"), getDisplayModel().getObject(), shownText(value.getObject()))));
            view.add(edit);
            return view;
        }

        /** The value as the cell shows it, for the button's name. */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private String shownText(Object value) {
            if (value == null) {
                return "";
            }
            IConverter converter = getConverter(value.getClass());
            String text = converter.convertToString(value, getLocale());
            return text == null ? String.valueOf(value) : text;
        }

        private Fragment editor() {
            Fragment editor = new Fragment("content", "editFragment", this);
            V original = getter.apply(row.getObject());
            Form<Void> form = new Form<>("form");
            form.add(AttributeModifier.replace("data-sw-table-editor", ""));
            editor.add(form);

            TextField<V> input = new TextField<>("input", new IModel<>() {
                private V draft = original;
                @Override
                public V getObject() {
                    return draft;
                }

                @Override
                public void setObject(V value) {
                    draft = value;
                }
            }, type);
            input.add(new StyledBehavior("sw-input"));
            input.setOutputMarkupId(true);
            input.setRequired(required);
            input.setLabel(getDisplayModel());
            validators.forEach(input::add);
            input.add(AttributeModifier.replace("aria-label", getDisplayModel()));
            input.add(AttributeModifier.replace("aria-invalid", () -> input.hasErrorMessage() ? "true" : null));
            input.add(AttributeModifier.replace("aria-describedby", () -> input.hasErrorMessage() ? input.getMarkupId() + "-error" : null));
            if (type != null && Number.class.isAssignableFrom(type)) {
                input.add(AttributeModifier.replace("inputmode", "decimal"));
            }
            form.add(input);

            Label error = new Label("error", () -> input.hasErrorMessage()
                    ? input.getFeedbackMessages().first(FeedbackMessage.ERROR).getMessage() : null) {
                @Override
                protected void onConfigure() {
                    super.onConfigure();
                    setVisible(input.hasErrorMessage());
                }
            };
            error.add(AttributeModifier.replace("id", () -> input.getMarkupId() + "-error"));
            form.add(error);

            AjaxFallbackButton save = new AjaxFallbackButton("save", form) {
                @Override protected void onSubmit(Optional<AjaxRequestTarget> target) {
                    T current = row.getObject();
                    if (!editable(current)) { input.error(text("unavailable", "This cell can no longer be edited.")); refresh(target, input); return; }
                    if (!Objects.equals(original, getter.apply(current))) {
                        input.error(text("conflict", "The value changed. Cancel and reopen the editor to load it.")); refresh(target, input); return;
                    }
                    try {
                        setter.accept(current, input.getModelObject());
                        onSave.accept(target.orElse(null), current);
                    } catch (RuntimeException failure) {
                        try { setter.accept(current, original); } catch (RuntimeException rollback) { failure.addSuppressed(rollback); }
                        throw failure;
                    }
                    showView(target);
                }
                @Override protected void onError(Optional<AjaxRequestTarget> target) { refresh(target, input); }
            };
            save.add(new ButtonBehavior().setSize(ButtonBehavior.Size.ICON));
            save.add(AttributeModifier.replace("aria-label", (IModel<String>) () -> text("save", "Save")));
            save.add(new SwIcon("icon", "check"));
            // The cell's form is nested in a page's form around the table
            save.add(new NestedFormButtonBehavior());
            form.add(save);
            form.setDefaultButton(save);

            AjaxFallbackLink<Void> cancel = new AjaxFallbackLink<>("cancel") {
                @Override public void onClick(Optional<AjaxRequestTarget> target) { showView(target); }
            };
            cancel.add(new ButtonBehavior(ButtonBehavior.Variant.GHOST).setSize(ButtonBehavior.Size.ICON));
            cancel.add(AttributeModifier.replace("data-sw-table-cancel", ""));
            cancel.add(AttributeModifier.replace("aria-label", (IModel<String>) () -> text("cancel", "Cancel")));
            cancel.add(new SwIcon("icon", "x"));
            form.add(cancel);
            return editor;
        }

        private String text(String key, String fallback) { return LocalizedText.get(this, SwEditableColumn.class, "SwEditableColumn." + key, fallback); }
        private void refresh(Optional<AjaxRequestTarget> target, Component input) {
            target.ifPresent(t -> { t.add(this); t.focusComponent(input); });
        }
        private void showView(Optional<AjaxRequestTarget> target) {
            Fragment view = view();
            replace(view);
            target.ifPresent(t -> { t.add(this); t.focusComponent(view.get("edit")); });
        }

        @Override
        public void renderHead(IHeaderResponse response) {
            super.renderHead(response);
            response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS)); // Escape cancels
        }
    }
}

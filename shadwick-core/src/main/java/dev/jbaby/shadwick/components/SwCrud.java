package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.DropdownMenuItemBehavior;
import dev.jbaby.shadwick.components.table.SwActionsColumn;
import dev.jbaby.shadwick.util.SerializableBiConsumer;
import dev.jbaby.shadwick.util.SerializableBiFunction;
import dev.jbaby.shadwick.util.SerializableFunction;
import dev.jbaby.shadwick.util.SerializableSupplier;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.feedback.FeedbackCollector;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A table of records with everything to manage them: "New" above the table, Edit and Delete in every row's actions
 * menu, a dialog to edit a record in, and a confirmation before deleting one. It follows Wicket Oat's
 * {@code OatCrud}:
 * <pre>{@code
 * SwCrud<Customer, String> customers = new SwCrud<>("customers", columns, provider, 20)
 *         .setEditor((id, customer) -> new CustomerFields(id, customer))   // your fields, see below
 *         .setNewItem(Customer::new)
 *         .setTitle(Customer::name)
 *         .setRowKey(Customer::id)
 *         .setOnSave((target, customer) -> repository.save(customer))
 *         .setOnDelete((target, customer) -> repository.delete(customer));
 * add(customers);
 * }</pre>
 * on {@code <div wicket:id="customers"></div>}.
 * <ul>
 * <li>The editor is any component made of form fields: a panel created with the id it is given and a
 * {@link CompoundPropertyModel} over the record. Pass the model to the panel's {@code super(id, model)}, and Shadwick
 * fields with a property's name as their id - {@code new SwTextField<>("name")} - find their value and label by
 * themselves.</li>
 * <li>Save validates the fields, each showing its own error. Only when they are all valid are they written to the
 * record and {@link #setOnSave} called; Cancel leaves the record as it was. Report an error there, e.g.
 * {@code nameField.getInput().error("Already taken")} to show it at the field, or on the editor's form
 * ({@code crud.getEditor().getForm().error(...)}) to show it in a summary above the fields; either keeps the dialog
 * open. Otherwise it closes, the table is re-rendered and screen readers hear "Saved …".</li>
 * <li>A record to edit is loaded through the provider's {@code model(...)}, so with a
 * {@code LoadableDetachableModel} it is fresh from the database when it is saved. Fields are written to that object
 * before {@code onSave} runs, so give the editor a copy or a freshly loaded entity, not an object other users of the
 * application share.</li>
 * <li>"New" shows only with {@link #setNewItem}, Edit only with an editor, and Delete only with {@link #setOnDelete}.
 * A record deleted meanwhile (its model loads {@code null}) is reported, not saved or deleted again.</li>
 * <li>{@link #setRowKey} gives every row's actions menu an id that survives re-rendering, so the focus returns to the
 * row's actions button after editing.</li>
 * </ul>
 * Reading, sorting and paging work without JavaScript; creating, editing and deleting need it. The texts
 * ({@code SwCrud.*}) are translated; {@link #getDataTable()} gives the table for its other features - selection, a
 * column chooser, CSV export. {@code onSave} and {@code onDelete} own persistence, authorization and concurrency.
 *
 * @param <T> the record type
 * @param <S> the sort property type
 */
public class SwCrud<T, S> extends Panel {

    private final List<? extends IColumn<T, S>> columns;
    private final ISortableDataProvider<T, S> provider;
    private final long rowsPerPage;

    private SerializableBiFunction<String, IModel<T>, ? extends Component> editorFactory;
    private SerializableSupplier<T> newItem;
    private SerializableFunction<T, String> title = String::valueOf;
    private SerializableBiConsumer<AjaxRequestTarget, T> onSave = (target, item) -> { };
    private SerializableBiConsumer<AjaxRequestTarget, T> onDelete;
    private SerializableFunction<String, ? extends Component> search;
    private SerializableFunction<T, ?> rowKey;

    private SwDataTable<T, S> table;
    private SwActionsColumn<T, S> actions;
    private SwDialog editor;
    private SwConfirmDialog confirm;
    private Label status;
    private final Model<String> statusText = Model.of("");

    /** The record being edited, and whether it is a new one. */
    private IModel<T> editing;
    private boolean editingNew;

    public SwCrud(String id, List<? extends IColumn<T, S>> columns, ISortableDataProvider<T, S> provider,
                  long rowsPerPage) {
        super(id);
        this.columns = List.copyOf(Objects.requireNonNull(columns));
        this.provider = Objects.requireNonNull(provider);
        if (rowsPerPage <= 0) throw new IllegalArgumentException("Rows per page must be positive");
        this.rowsPerPage = rowsPerPage;
        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        build();
        add(table, editor, confirm, status);
    }

    /**
     * Builds the parts once: on initialization, or earlier when a getter asks for one, e.g. to configure the table
     * right after construction as with Oat.
     */
    private void build() {
        if (table != null) return;
        table = newTable();
        editor = newEditor();
        confirm = newConfirm();
        status = new Label("status", statusText) {
            @Override
            protected void onAfterRender() {
                super.onAfterRender();
                // Said once: a later render doesn't repeat it
                statusText.setObject("");
            }
        };
        status.setOutputMarkupId(true);
    }

    private SwDataTable<T, S> newTable() {
        actions = new SwActionsColumn<>();
        actions.setRowLabel(row -> title.apply(row));
        if (rowKey != null) actions.setRowKey(rowKey);
        actions.addAction(new StringResourceModel("SwCrud.edit", this), this::edit)
                .setVisibleWhen(row -> editorFactory != null);
        actions.addAction(new StringResourceModel("SwCrud.delete", this), this::askToDelete)
                .setVariant(DropdownMenuItemBehavior.Variant.DESTRUCTIVE)
                .setVisibleWhen(row -> onDelete != null);
        List<IColumn<T, S>> all = new ArrayList<>(columns);
        all.add(actions);
        table = new SwDataTable<>("table", all, provider, rowsPerPage);
        if (rowKey != null) table.setRowKey(rowKey);
        table.setToolbar(this::newToolbar);
        // Where the focus goes after a delete when there is no New button
        table.getTable().add(AttributeModifier.replace("tabindex", "-1"));
        // Re-rendered on its own by refresh(...)
        table.get("summary").setOutputMarkupId(true);
        return table;
    }

    private Component newToolbar(String id) {
        Fragment toolbar = new Fragment(id, "toolbar", this);
        toolbar.add(search != null ? checked(search.apply("search"), "search", "search")
                : new WebMarkupContainer("search").setVisible(false));
        SwButton create = new SwButton("new") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                create(target);
            }

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(newItem != null && editorFactory != null);
            }
        };
        create.add(new SwIcon("icon", "plus"), new Label("label", new StringResourceModel("SwCrud.new", this)));
        create.setOutputMarkupId(true);
        toolbar.add(create);
        return toolbar;
    }

    private SwDialog newEditor() {
        editor = new SwDialog("editor", (IModel<String>) this::editorTitle) {
            @Override
            protected void onConfirm(AjaxRequestTarget target) {
                save(target);
            }

            @Override
            protected void onClose(AjaxRequestTarget target) {
                // Saved or cancelled: the dialog dropped the fields, and the record goes too
                if (editing != null) editing.detach();
                editing = null;
            }
        };
        editor.setConfirmLabel(new StringResourceModel("SwCrud.save", this));
        editor.setBusyIndicator(true);
        // Before the fields are written to the record: one deleted meanwhile is reported, not written to
        editor.getForm().add(new AbstractFormValidator() {
            @Override
            public FormComponent<?>[] getDependentFormComponents() {
                return null;
            }

            @Override
            public void validate(Form<?> form) {
                if (editing != null && !editingNew && editing.getObject() == null) {
                    form.error(new StringResourceModel("SwCrud.gone", SwCrud.this).getObject());
                    RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(SwCrud.this::renderRows);
                }
            }
        });
        return editor;
    }

    private SwConfirmDialog newConfirm() {
        confirm = new SwConfirmDialog("confirm");
        confirm.setSize(SwConfirmDialog.Size.SMALL)
                .setIcon("trash-2")
                .setIconVariant(SwConfirmDialog.IconVariant.DESTRUCTIVE)
                .setConfirmLabel(new StringResourceModel("SwCrud.delete", this));
        return confirm;
    }

    private String editorTitle() {
        if (editingNew || editing == null) {
            return new StringResourceModel("SwCrud.newTitle", this).getObject();
        }
        T record = editing.getObject();
        return new StringResourceModel("SwCrud.editTitle", this)
                .setParameters(record == null ? "" : title.apply(record)).getObject();
    }

    /**
     * The fields to edit a record with, created with the id they are given ({@link SwDialog#CONTENT_ID}) and a
     * {@link CompoundPropertyModel} over the record each time the dialog opens.
     */
    public SwCrud<T, S> setEditor(SerializableBiFunction<String, IModel<T>, ? extends Component> editor) {
        this.editorFactory = editor;
        return this;
    }

    /** Creates the record "New" edits, e.g. {@code Customer::new}; without one there's no "New". */
    public SwCrud<T, S> setNewItem(SerializableSupplier<T> newItem) {
        this.newItem = newItem;
        return this;
    }

    /** A record's name in the dialog's title, the actions menu and the delete question, e.g. {@code Customer::name}. */
    public SwCrud<T, S> setTitle(SerializableFunction<T, String> title) {
        this.title = Objects.requireNonNull(title);
        return this;
    }

    /** Stores a valid record, new or changed. Report an error on the dialog's fields to keep it open. */
    public SwCrud<T, S> setOnSave(SerializableBiConsumer<AjaxRequestTarget, T> onSave) {
        this.onSave = Objects.requireNonNull(onSave);
        return this;
    }

    /** Deletes a record once the user confirmed it; without this there's no Delete. */
    public SwCrud<T, S> setOnDelete(SerializableBiConsumer<AjaxRequestTarget, T> onDelete) {
        this.onDelete = onDelete;
        return this;
    }

    /**
     * A record's stable, unique key, e.g. {@code Customer::id}: the rows' actions menus keep their ids when the table
     * re-renders, so the focus can return to the row it left. Set it before the component is added to a page.
     */
    public SwCrud<T, S> setRowKey(SerializableFunction<T, ?> rowKey) {
        this.rowKey = Objects.requireNonNull(rowKey);
        if (table != null) {
            // Built early by a getter
            table.setRowKey(rowKey);
            actions.setRowKey(rowKey);
        }
        return this;
    }

    /**
     * A component beside "New", e.g. a search field, created with the id it is given. Have the provider read its
     * model, and call {@link #refresh} from its Ajax handler.
     */
    public SwCrud<T, S> setSearch(SerializableFunction<String, ? extends Component> search) {
        this.search = search;
        if (table != null) table.setToolbar(this::newToolbar);
        return this;
    }

    /**
     * Shows the first page of the provider's current records, e.g. after the search changed: re-renders the rows, the
     * summary, the pager and the selection bar, but not the toolbar, so typing in the search goes on undisturbed.
     */
    public SwCrud<T, S> refresh(AjaxRequestTarget target) {
        build();
        table.getTable().setCurrentPage(0);
        renderRows(target);
        return this;
    }

    /** Re-renders the rows and what counts them, but not the toolbar with its search and New. */
    private void renderRows(AjaxRequestTarget target) {
        target.add(table.getTable());
        for (String part : List.of("summary", "size", "navigator", "bulkActions")) {
            Component component = table.get(part);
            if (component != null && component.getOutputMarkupId()) target.add(component);
        }
    }

    /** Opens the dialog for a new record. */
    public SwCrud<T, S> create(AjaxRequestTarget target) {
        if (newItem == null || editorFactory == null) {
            throw new IllegalStateException("Creating needs setNewItem(...) and setEditor(...)");
        }
        open(target, new RecordModel<>(newItem.get()), true);
        return this;
    }

    /** Opens the dialog for a record. */
    public SwCrud<T, S> edit(AjaxRequestTarget target, T item) {
        if (editorFactory == null) {
            throw new IllegalStateException("Editing needs setEditor(...)");
        }
        open(target, provider.model(item), false);
        return this;
    }

    @SuppressWarnings("unchecked")
    private void open(AjaxRequestTarget target, IModel<?> model, boolean isNew) {
        if (editing != null) editing.detach();
        editing = (IModel<T>) model;
        editingNew = isNew;
        Component fields = editorFactory.apply(SwDialog.CONTENT_ID, new CompoundPropertyModel<>(editing));
        checked(fields, SwDialog.CONTENT_ID, "editor");
        // The fields under a summary of errors reported on the form or the dialog, e.g. a conflict found on saving;
        // a field's own errors show at the field. Removed again when the dialog closes.
        Fragment content = new Fragment(SwDialog.CONTENT_ID, "editorContent", this);
        content.add(new SwFormErrors("errors", editor.getForm()).setAutofocus(true), fields);
        editor.open(content, target);
    }

    private void save(AjaxRequestTarget target) {
        T record = editing == null ? null : editing.getObject();
        if (record == null) {
            announce(target, new StringResourceModel("SwCrud.gone", this).getObject());
            return;
        }
        onSave.accept(target, record);
        if (!hasErrors()) {
            announce(target, new StringResourceModel("SwCrud.saved", this).setParameters(title.apply(record)).getObject());
        }
    }

    /** Whether onSave refused: the same errors the dialog stays open for (its own, not left-over session ones). */
    private boolean hasErrors() {
        return !new FeedbackCollector(editor, false).collect(FeedbackMessage::isError).isEmpty();
    }

    private void askToDelete(AjaxRequestTarget target, T item) {
        IModel<T> model = provider.model(item);
        String name = title.apply(item);
        confirm.ask(target,
                new StringResourceModel("SwCrud.deleteQuestion", this).setParameters(name),
                new StringResourceModel("SwCrud.deleteDetail", this),
                confirmed -> {
                    try {
                        // Loaded afresh: the question was asked in an earlier request
                        T current = model.getObject();
                        if (current == null) {
                            announce(confirmed, new StringResourceModel("SwCrud.gone", this).getObject());
                        } else {
                            onDelete.accept(confirmed, current);
                            announce(confirmed, new StringResourceModel("SwCrud.deleted", this).setParameters(name).getObject());
                        }
                    } finally {
                        model.detach();
                    }
                    // The row's actions button has gone: New, else the table
                    Component create = table.get(SwDataTable.TOOLBAR_ID + ":new");
                    confirmed.focusComponent(create != null && create.isVisibleInHierarchy() ? create : table.getTable());
                });
        // Held until the answer, not the record it loaded now
        model.detach();
    }

    private void announce(AjaxRequestTarget target, String message) {
        statusText.setObject(message);
        renderRows(target);
        target.add(status);
    }

    private static Component checked(Component component, String id, String part) {
        if (component == null || !id.equals(component.getId())) {
            throw new IllegalArgumentException("The " + part + " must use the id passed to the factory (\"" + id
                    + "\"), but was " + (component == null ? "null" : "\"" + component.getId() + "\""));
        }
        return component;
    }

    /** The table, e.g. for selection and bulk actions, a column chooser or CSV export. */
    public SwDataTable<T, S> getDataTable() {
        build();
        return table;
    }

    /** The edit dialog. */
    public SwDialog getEditor() {
        build();
        return editor;
    }

    /** The delete confirmation. */
    public SwConfirmDialog getConfirmDialog() {
        build();
        return confirm;
    }

    @Override
    protected void onDetach() {
        if (editing != null) {
            editing.detach();
        }
        super.onDetach();
    }

    /** A new record, held by the page while its dialog is open. */
    private static final class RecordModel<T> implements IModel<T> {
        private T record;

        RecordModel(T record) {
            this.record = record;
        }

        @Override
        public T getObject() {
            return record;
        }

        @Override
        public void setObject(T object) {
            record = object;
        }
    }
}

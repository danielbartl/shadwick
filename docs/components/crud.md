# CRUD

A table of records with everything to manage them: **New** above the table, **Edit** and **Delete** in every row's
actions menu, a dialog to edit a record in, and a confirmation before deleting one.

> **New in 0.2.0, not yet released.** `SwCrud` isn't in 0.1.0, the version on Maven Central.

`SwCrud<T, S>` follows Wicket Oat's `OatCrud` and is built from a [data table](data-table.md), a [dialog](dialog.md)
and a [confirmation](confirmation-dialog.md):

```java
SwCrud<Customer, String> customers = new SwCrud<>("customers", columns, provider, 20)
        .setEditor(CustomerFields::new)          // a panel of fields, see below
        .setNewItem(Customer::new)
        .setTitle(Customer::name)
        .setRowKey(Customer::id)
        .setOnSave((target, customer) -> repository.save(customer))
        .setOnDelete((target, customer) -> repository.delete(customer));
add(customers);
```

```html
<div wicket:id="customers"></div>
```

The editor is any component made of form fields, created with the id it is given and a `CompoundPropertyModel` over the
record. Fields named after the record's properties find their value and label by themselves:

```java
public class CustomerFields extends Panel {
    public CustomerFields(String id, IModel<Customer> customer) {
        super(id, customer);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(new SwTextField<String>("name", "Name").setRequired(true));
        add(new SwEmailField("email", "Email"));
    }
}
```

- **Saving.** Save validates the fields, each showing its own error. Only when all are valid are they written to the
  record and `onSave` called. To refuse a save, report an error there: on a field's input
  (`nameField.getInput().error("Already taken")`) to show it at the field, or on the editor's form
  (`crud.getEditor().getForm().error(...)`) to show it in a summary above the fields. Either keeps the dialog open.
  Otherwise it closes, the table re-renders and screen readers hear "Saved Acme." (`SwCrud.saved`).
- **Which record.** A record to edit is loaded through the provider's `model(...)`, so with a
  `LoadableDetachableModel` it is fresh when it's saved. The fields are written to that object before `onSave` runs, so
  load a copy or an entity, not an object the rest of the application shares. Cancel and Escape change nothing.
- **Deleting** asks "Delete Acme?" first. The record is loaded again on confirming; if it was deleted meanwhile, nothing
  happens and screen readers hear that it no longer exists.
- **Focus.** `setRowKey` gives every row's actions menu an id that survives re-rendering, so after editing the focus
  returns to that row's actions button. After a delete the row has gone, so the focus moves to **New**, or to the
  table when there is no New.
- **What shows.** New shows only with `setNewItem` and an editor, Edit only with an editor, and Delete only with
  `setOnDelete`. `setTitle` names a record in the dialog title, the actions menu and the question.
- **Search.** `setSearch(id -> component)` puts a component, such as a search field, beside New. Have the provider
  read its model and call `crud.refresh(target)` from its Ajax handler: that shows the first page again and re-renders
  the rows, summary and pager, but not the field, so typing goes on undisturbed.

`getDataTable()`, `getEditor()` and `getConfirmDialog()` give the parts for their other options, such as a column
chooser or CSV export. Reading, sorting and paging work without JavaScript; creating, editing and deleting need it.
`onSave` and `onDelete` own persistence, authorization and concurrency. Don't put a CRUD inside another form.

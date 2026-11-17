package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.components.form.SwTextField;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** New, Edit and Delete around a data table: validation in the dialog, cancelling, confirming, and what is said. */
class SwCrudTest {
    private static final String DIALOG_FORM = "crud:editor:dialog:form";
    WicketTester tester;

    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() {
        if (tester.getLastResponse() != null)
            assertThat(tester.getLastResponseAsString()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        tester.destroy();
    }

    String html() { return tester.getLastResponseAsString(); }

    public static final class Customer implements Serializable {
        final int id;
        String name, city;
        Customer(int id, String name, String city) { this.id = id; this.name = name; this.city = city; }
        public String getName() { return name; }
        public String getCity() { return city; }
    }

    /** The editor: fields named after the properties, on the record's CompoundPropertyModel. */
    public static class CustomerFields extends Panel implements IMarkupResourceStreamProvider {
        public CustomerFields(String id, IModel<Customer> model) {
            super(id, model);
            add(new SwTextField<String>("name", "Name").setRequired(true));
            add(new SwTextField<String>("city", "City"));
        }

        @Override
        public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<wicket:panel><div wicket:id='name'></div><div wicket:id='city'></div></wicket:panel>");
        }
    }

    public static class CrudPage extends WebPage implements IMarkupResourceStreamProvider {
        public final List<Customer> customers = new ArrayList<>(List.of(new Customer(1, "ACME", "Vienna"), new Customer(2, "Globex", "Berlin")));
        public final List<String> log = new ArrayList<>();
        public final SwCrud<Customer, String> crud;

        public CrudPage() { this(true, true, true); }

        public CrudPage(boolean editor, boolean create, boolean delete) {
            List<IColumn<Customer, String>> columns = List.of(
                    new LambdaColumn<>(Model.of("Name"), Customer::getName),
                    new LambdaColumn<>(Model.of("City"), Customer::getCity));
            crud = new SwCrud<>("crud", columns, new SortableDataProvider<Customer, String>() {
                @Override public Iterator<? extends Customer> iterator(long first, long count) {
                    return customers.stream().skip(first).limit(count).iterator();
                }
                @Override public long size() { return customers.size(); }
                // Loaded by id, as from a database, and kept until detached: null once deleted
                @Override public IModel<Customer> model(Customer customer) {
                    int id = customer.id;
                    return new org.apache.wicket.model.LoadableDetachableModel<>() {
                        @Override protected Customer load() {
                            return customers.stream().filter(c -> c.id == id).findFirst().orElse(null);
                        }
                    };
                }
            }, 10).setTitle(Customer::getName).setRowKey(c -> c.id);
            if (editor) crud.setEditor(CustomerFields::new);
            if (create) crud.setNewItem(() -> new Customer(customers.size() + 1, null, null));
            crud.setOnSave((target, customer) -> {
                if ("Inline".equals(customer.name)) {
                    ((dev.jbaby.shadwick.components.form.BaseSwField<?, ?, ?>) crud.getEditor().getForm().get("content:content:name"))
                            .getInput().error("Pick another name");
                    return;
                }
                if ("Twice".equals(customer.name)) {
                    var input = ((dev.jbaby.shadwick.components.form.BaseSwField<?, ?, ?>) crud.getEditor().getForm().get("content:content:name")).getInput();
                    input.error("First problem");
                    input.error("Second problem");
                    return;
                }
                if ("Dialog".equals(customer.name)) {
                    crud.getEditor().error("Someone else changed this customer");
                    return;
                }
                if ("Taken".equals(customer.name)) {
                    crud.getEditor().getForm().get("content:content:name").error("That name is taken");
                    return;
                }
                if (!customers.contains(customer)) customers.add(customer);
                log.add("saved " + customer.name + "/" + customer.city);
            });
            if (delete) crud.setOnDelete((target, customer) -> {
                customers.remove(customer);
                log.add("deleted " + customer.name);
            });
            add(crud);
        }

        @Override
        public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><head></head><body><div wicket:id='crud'></div></body></html>");
        }
    }

    /** An entry of a row's actions menu: 0 Edit, 1 Delete (of those shown). */
    private Component menuItem(WebPage page, int row, int index) {
        List<SwDropdown<?>> menus = new ArrayList<>();
        page.visitChildren(SwDropdown.class, (menu, visit) -> menus.add((SwDropdown<?>) menu));
        return menus.get(row).get("menu:items:" + index);
    }

    @Test void createsARecordAfterAnInvalidSave() {
        CrudPage page = tester.startPage(CrudPage.class);
        assertThat(html()).contains(">New<");
        tester.clickLink("crud:table:toolbar:new");
        assertThat(html()).contains(">New entry<").contains(">Save<");

        FormTester form = tester.newFormTester(DIALOG_FORM);
        form.setValue("content:content:name:input", "");
        tester.executeAjaxEvent(DIALOG_FORM + ":confirm", "click");
        assertThat(page.log).isEmpty();
        assertThat(page.crud.getEditor().isOpen()).isTrue();

        form = tester.newFormTester(DIALOG_FORM);
        form.setValue("content:content:name:input", "Initech");
        form.setValue("content:content:city:input", "Munich");
        tester.executeAjaxEvent(DIALOG_FORM + ":confirm", "click");
        assertThat(page.log).containsExactly("saved Initech/Munich");
        assertThat(page.customers).hasSize(3);
        assertThat(page.crud.getEditor().isOpen()).isFalse();
        assertThat(html()).contains("Initech").contains("Saved Initech.");
    }

    @Test void editsARecordAndAnInvalidSaveLeavesItUntouched() {
        CrudPage page = tester.startPage(CrudPage.class);
        tester.executeAjaxEvent(menuItem(page, 0, 0), "click");
        assertThat(html()).contains(">Edit ACME<").contains("value=\"Vienna\"");

        FormTester form = tester.newFormTester(DIALOG_FORM);
        form.setValue("content:content:name:input", "");
        form.setValue("content:content:city:input", "Graz");
        tester.executeAjaxEvent(DIALOG_FORM + ":confirm", "click");
        assertThat(page.customers.get(0).city).isEqualTo("Vienna");

        form = tester.newFormTester(DIALOG_FORM);
        form.setValue("content:content:name:input", "ACME GmbH");
        form.setValue("content:content:city:input", "Graz");
        tester.executeAjaxEvent(DIALOG_FORM + ":confirm", "click");
        assertThat(page.log).containsExactly("saved ACME GmbH/Graz");
        assertThat(html()).contains("Saved ACME GmbH.");
    }

    @Test void anErrorReportedOnSaveKeepsTheDialogOpen() {
        CrudPage page = tester.startPage(CrudPage.class);
        tester.clickLink("crud:table:toolbar:new");
        FormTester form = tester.newFormTester(DIALOG_FORM);
        form.setValue("content:content:name:input", "Taken");
        tester.executeAjaxEvent(DIALOG_FORM + ":confirm", "click");
        assertThat(page.crud.getEditor().isOpen()).isTrue();
        assertThat(html()).contains("That name is taken").doesNotContain("Saved");
        assertThat(page.customers).hasSize(2);
    }

    @Test void anErrorOnAFieldsInputShowsAtTheField() {
        CrudPage page = tester.startPage(CrudPage.class);
        tester.clickLink("crud:table:toolbar:new");
        FormTester form = tester.newFormTester(DIALOG_FORM);
        form.setValue("content:content:name:input", "Inline");
        tester.executeAjaxEvent(DIALOG_FORM + ":confirm", "click");
        assertThat(page.crud.getEditor().isOpen()).isTrue();
        // Updated in place, keeping the focus and what was typed
        assertThat(html()).contains("Shadwick.fieldFeedback").contains("Pick another name");
    }

    @Test void aSecondErrorOnAnInputAndAnErrorOnTheDialogAreShownAndNothingIsAnnounced() {
        CrudPage page = tester.startPage(CrudPage.class);
        tester.clickLink("crud:table:toolbar:new");
        FormTester form = tester.newFormTester(DIALOG_FORM);
        form.setValue("content:content:name:input", "Twice");
        tester.executeAjaxEvent(DIALOG_FORM + ":confirm", "click");
        // The field shows the first; the form re-renders so its summary shows the second
        assertThat(html()).contains("First problem").contains("Second problem").doesNotContain("Saved");

        form = tester.newFormTester(DIALOG_FORM);
        form.setValue("content:content:name:input", "Dialog");
        tester.executeAjaxEvent(DIALOG_FORM + ":confirm", "click");
        assertThat(page.crud.getEditor().isOpen()).isTrue();
        assertThat(html()).doesNotContain("Saved");
        assertThat(page.customers).hasSize(2);
    }

    @Test void anOldSessionMessageIsNotTakenForAFailedSave() {
        CrudPage page = tester.startPage(CrudPage.class);
        tester.clickLink("crud:table:toolbar:new");
        tester.getSession().error("Left over from earlier");
        FormTester form = tester.newFormTester(DIALOG_FORM);
        form.setValue("content:content:name:input", "Initech");
        tester.executeAjaxEvent(DIALOG_FORM + ":confirm", "click");
        assertThat(page.crud.getEditor().isOpen()).isFalse();
        assertThat(page.log).containsExactly("saved Initech/null");
        assertThat(html()).contains("Saved Initech.");
    }

    @Test void savingARecordDeletedMeanwhileReportsItInTheDialog() {
        CrudPage page = tester.startPage(CrudPage.class);
        tester.executeAjaxEvent(menuItem(page, 1, 0), "click");
        assertThat(html()).contains(">Edit Globex<");
        page.customers.remove(1);
        FormTester form = tester.newFormTester(DIALOG_FORM);
        form.setValue("content:content:name:input", "Globex AG");
        tester.executeAjaxEvent(DIALOG_FORM + ":confirm", "click");
        assertThat(page.log).isEmpty();
        assertThat(page.crud.getEditor().isOpen()).isTrue();
        // In the summary above the fields, and the row has gone from the table
        assertThat(html()).contains("This entry no longer exists.").doesNotContain(">Globex<");
    }

    @Test void savingReRendersTheRowsButNotTheToolbar() {
        CrudPage page = tester.startPage(CrudPage.class);
        tester.executeAjaxEvent(menuItem(page, 0, 0), "click");
        FormTester form = tester.newFormTester(DIALOG_FORM);
        form.setValue("content:content:name:input", "ACME GmbH");
        tester.executeAjaxEvent(DIALOG_FORM + ":confirm", "click");
        assertThat(html()).contains("<component id=\"" + page.crud.getDataTable().getTable().getMarkupId() + "\"")
                .doesNotContain("<component id=\"" + page.crud.getDataTable().getMarkupId() + "\"");
    }

    @Test void cancellingTheEditorDropsItsFields() {
        CrudPage page = tester.startPage(CrudPage.class);
        tester.executeAjaxEvent(menuItem(page, 0, 0), "click");
        assertThat(page.crud.getEditor().getForm().get("content:content:name")).isNotNull();

        tester.executeAjaxEvent("crud:editor:dialog", "close");
        assertThat(page.crud.getEditor().isOpen()).isFalse();
        assertThat(page.crud.getEditor().getForm().get("content:content:name")).isNull();
        assertThat(page.log).isEmpty();
    }

    @Test void deletesAfterConfirmingAndMovesTheFocusToNew() {
        CrudPage page = tester.startPage(CrudPage.class);
        tester.executeAjaxEvent(menuItem(page, 1, 1), "click");
        assertThat(html()).contains("Delete Globex?").contains("This can&#039;t be undone.");
        assertThat(page.customers).hasSize(2);

        tester.executeAjaxEvent("crud:confirm:dialog:form:confirm", "click");
        assertThat(page.log).containsExactly("deleted Globex");
        assertThat(page.customers).hasSize(1);
        String create = page.crud.getDataTable().get("toolbar:new").getMarkupId();
        assertThat(html()).contains("Deleted Globex.").contains("Wicket.Focus.setFocusOnId('" + create + "')");
    }

    @Test void aRecordDeletedMeanwhileIsReportedNotDeletedAgain() {
        CrudPage page = tester.startPage(new CrudPage(true, false, true));
        tester.executeAjaxEvent(menuItem(page, 1, 1), "click");
        page.customers.remove(1);
        tester.executeAjaxEvent("crud:confirm:dialog:form:confirm", "click");
        assertThat(page.log).isEmpty();
        // Without New, the focus goes to the table
        String table = page.crud.getDataTable().getTable().getMarkupId();
        assertThat(html()).contains("This entry no longer exists.").contains("Wicket.Focus.setFocusOnId('" + table + "')");
    }

    @Test void newEditAndDeleteShowOnlyWhenConfigured() {
        CrudPage page = tester.startPage(new CrudPage(false, true, false));
        tester.assertInvisible("crud:table:toolbar:new");
        // Neither Edit nor Delete: the rows have no actions menu
        List<SwDropdown<?>> menus = new ArrayList<>();
        page.visitChildren(SwDropdown.class, (menu, visit) -> { if (menu.isVisibleInHierarchy()) menus.add((SwDropdown<?>) menu); });
        assertThat(menus).isEmpty();
        assertThatThrownBy(() -> page.crud.create(null)).isInstanceOf(IllegalStateException.class);
    }

    @Test void theEditorMustUseItsIdAndTheTableIsThereBeforeTheCrudIsAdded() {
        var page = new CrudPage();
        page.crud.setEditor((id, model) -> new Label("other"));
        tester.startPage(page);
        assertThatThrownBy(() -> tester.clickLink("crud:table:toolbar:new")).hasStackTraceContaining("must use the id passed to the factory");

        var early = new SwCrud<Customer, String>("crud", List.of(new LambdaColumn<>(Model.of("Name"), Customer::getName)),
                new SortableDataProvider<>() {
                    @Override public Iterator<? extends Customer> iterator(long first, long count) { return List.<Customer>of().iterator(); }
                    @Override public long size() { return 0; }
                    @Override public IModel<Customer> model(Customer object) { return Model.of(object); }
                }, 5);
        assertThat(early.getDataTable().setColumnChooser(true)).isNotNull();
        assertThat(early.getEditor()).isNotNull();
    }

    @Test void rowMenusKeepTheirIdsAcrossRendersSoTheFocusCanReturn() {
        CrudPage page = tester.startPage(CrudPage.class);
        String trigger = ((SwDropdown<?>) menuItem(page, 0, 0).findParent(SwDropdown.class)).getTrigger().getMarkupId();
        assertThat(trigger).contains("-actions-");
        tester.startPage(page);
        assertThat(((SwDropdown<?>) menuItem(page, 0, 0).findParent(SwDropdown.class)).getTrigger().getMarkupId()).isEqualTo(trigger);
    }

    @Test void refreshGoesToTheFirstPageWithoutReRenderingTheToolbar() {
        CrudPage page = tester.startPage(CrudPage.class);
        page.crud.getDataTable().getTable().setCurrentPage(1);
        tester.executeAjaxEvent(menuItem(page, 0, 0), "click");
        var target = tester.getApplication().newAjaxRequestTarget(page);
        page.crud.refresh(target);
        assertThat(page.crud.getDataTable().getTable().getCurrentPage()).isZero();
        List<Component> added = new ArrayList<>(target.getComponents());
        assertThat(added).contains(page.crud.getDataTable().getTable(), page.crud.getDataTable().get("summary"))
                .doesNotContain(page.crud.getDataTable().get("toolbar"), page.crud.getDataTable());
    }

    @Test void isTranslatedAndSerializes() throws Exception {
        tester.getSession().setLocale(Locale.GERMAN);
        CrudPage page = tester.startPage(CrudPage.class);
        assertThat(html()).contains(">Neu<");
        tester.executeAjaxEvent(menuItem(page, 1, 1), "click");
        assertThat(html()).contains("Globex löschen?");
        try (var out = new java.io.ObjectOutputStream(new java.io.ByteArrayOutputStream())) {
            out.writeObject(tester.getLastRenderedPage());
        }
    }
}

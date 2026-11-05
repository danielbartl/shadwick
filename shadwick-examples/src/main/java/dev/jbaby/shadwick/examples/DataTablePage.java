package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.behaviors.*;
import dev.jbaby.shadwick.components.*;
import dev.jbaby.shadwick.components.form.*;
import dev.jbaby.shadwick.components.table.*;
import org.apache.wicket.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.*;
import org.apache.wicket.model.util.SetModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import java.io.Serializable;
import java.math.*;
import java.time.LocalDate;
import java.util.*;

/** Session-scoped demo records. URL navigation is native; row CRUD and the optional stateful mode use Ajax. */
public class DataTablePage extends BasePage {
    private static final MetaDataKey<Store> STORE = new MetaDataKey<>() {};
    enum Status { OPEN("Open"), OVERDUE("Overdue"), PAID("Paid"); final String label; Status(String label) { this.label = label; } }
    public record Invoice(long id, String number, String customer, String email, Status status, long cents, LocalDate due, boolean locked) implements Serializable {
        BigDecimal amount() { return BigDecimal.valueOf(cents, 2); }
        Invoice withStatus(Status status) { return new Invoice(id, number, customer, email, status, cents, due, locked); }
    }
    static final class Store implements Serializable {
        final List<Invoice> invoices = new ArrayList<>(); long nextId = 25;
        Store() {
            String[] names = {"Ada Lovelace", "Grace Hopper", "Barbara Liskov", "Margaret Hamilton", "Edsger Dijkstra", "Donald Knuth", "Mary Jackson", "Alan Turing", "Katherine Johnson", "Frances Allen", "Ken Thompson", "Hedy Lamarr", "Radia Perlman", "John McCarthy", "Annie Easley", "Claude Shannon", "Jean Bartik", "Tim Berners-Lee", "Dorothy Vaughan", "James Gosling", "Karen Spärck Jones", "Guido van Rossum", "Sophie Wilson", "Linus Torvalds"};
            for (int i = 0; i < names.length; i++) {
                Status status = i % 4 == 0 ? Status.PAID : i % 4 == 2 ? Status.OVERDUE : Status.OPEN;
                invoices.add(new Invoice(i + 1, "INV-" + (2001 + i), names[i], names[i].toLowerCase(Locale.ROOT).replace(' ', '.') + "@example.com", status,
                        12500L + i * 3775L, LocalDate.of(2026, 11, 12).plusDays(i), i == 4));
            }
        }
    }
    static Store store() {
        Store store = Session.get().getMetaData(STORE); if (store == null) { store = new Store(); Session.get().setMetaData(STORE, store); } return store;
    }
    static Invoice invoice(long id) { return store().invoices.stream().filter(row -> row.id == id).findFirst().orElse(null); }
    private final String search, statusFilter;
    private final Provider provider = new Provider();
    private final IModel<Set<Long>> selected = new SetModel<>(new HashSet<>());
    private final SwDataTable<Invoice, String> table;
    private final Label status;
    private final WebMarkupContainer stats;
    private final SwDialog editor;
    private final SwConfirmDialog confirmation;
    private final Model<String> customer = Model.of(""), email = Model.of("");
    private final Model<BigDecimal> amount = Model.of(new BigDecimal("125.00"));
    private final Model<LocalDate> due = Model.of(LocalDate.of(2026, 11, 30));
    private Long editingId;

    public DataTablePage(PageParameters parameters) {
        super(parameters); store();
        search = parameters.get("q").toString("").strip();
        statusFilter = parameters.get("status").toString("");
        boolean ajax = parameters.get("navigation").toString("").equals("ajax");
        status = new Label("status", Model.of("A clear view of the work you do.")); status.setOutputMarkupId(true); add(status);
        stats = new WebMarkupContainer("stats"); stats.setOutputMarkupId(true); add(stats);
        stats.add(new Label("count", (IModel<Integer>) () -> store().invoices.size()));
        stats.add(new Label("open", (IModel<String>) () -> money(store().invoices.stream().filter(row -> row.status != Status.PAID).mapToLong(Invoice::cents).sum(), getLocale())));
        stats.add(new Label("paid", (IModel<Long>) () -> store().invoices.stream().filter(row -> row.status == Status.PAID).count()));
        editor = new SwDialog("editor", "New invoice") {
            @Override protected void onConfirm(AjaxRequestTarget target) {
                Invoice old = editingId == null ? null : invoice(editingId);
                if (editingId != null && (old == null || old.locked)) { getForm().error("This invoice is no longer available for editing."); return; }
                long id = old == null ? store().nextId++ : old.id;
                Invoice saved = new Invoice(id, old == null ? "INV-" + (2000 + id) : old.number, customer.getObject(), email.getObject(), old == null ? Status.OPEN : old.status,
                        amount.getObject().setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact(), due.getObject(), false);
                if (old != null) store().invoices.removeIf(row -> row.id == id); store().invoices.add(saved);
                announce(target, saved.number + " saved.");
            }
        };
        editor.setDescription("Keep the details clear. The rest can wait.").setConfirmLabel("Save invoice"); editor.setContent(new Editor(SwDialog.CONTENT_ID)); add(editor);
        confirmation = new SwConfirmDialog("confirmation").setConfirmLabel("Delete invoice"); add(confirmation);
        List<IColumn<Invoice, String>> columns = new ArrayList<>();
        columns.add(new SwSelectionColumn<Invoice, String>(selected, Invoice::id, ids -> store().invoices.stream().filter(row -> ids.contains(row.id)).toList())
                .setRowLabel(Invoice::number).setSelectableWhen(row -> !row.locked));
        columns.add(SwLinkColumn.toPage(Model.of("Invoice"), "number", Invoice::number, InvoicePage.class,
                row -> new PageParameters(getPageParameters()).set("invoice", row.id)));
        columns.add(new AbstractColumn<Invoice, String>(Model.of("Customer"), "customer") {
            @Override public void populateItem(Item<ICellPopulator<Invoice>> item, String id, IModel<Invoice> row) { item.add(new CustomerCell(id, row)); }
        });
        columns.add(new SwBadgeColumn<>(Model.of("Status"), "status", row -> row.status.label,
                row -> row.status == Status.OVERDUE ? BadgeBehavior.Variant.DESTRUCTIVE : row.status == Status.PAID ? BadgeBehavior.Variant.SECONDARY : BadgeBehavior.Variant.OUTLINE));
        columns.add(new SwNumberColumn<Invoice, String>(Model.of("Amount"), "amount", Invoice::amount).setCurrency(Currency.getInstance("EUR")));
        columns.add(new SwDateColumn<>(Model.of("Due"), "due", Invoice::due));
        var actions = new SwActionsColumn<Invoice, String>().setRowLabel(Invoice::number).setRowKey(Invoice::id);
        actions.addAction("Edit invoice", (target, row) -> edit(target, row)).setVisibleWhen(row -> !row.locked);
        actions.addAction("Mark as paid", (target, row) -> {
            store().invoices.replaceAll(current -> current.id == row.id ? current.withStatus(Status.PAID) : current); announce(target, row.number + " marked as paid.");
        }).setVisibleWhen(row -> !row.locked && row.status != Status.PAID);
        actions.addAction("Delete invoice", (target, row) -> confirmation.ask(target, "Delete " + row.number + "?", "This removes the invoice from your demo workspace.", confirmed -> {
            Invoice current = invoice(row.id);
            if (current != null && !current.locked) { store().invoices.removeIf(value -> value.id == row.id); selected.getObject().remove(row.id); }
            announce(confirmed, row.number + " deleted."); confirmed.focusComponent(get("create"));
        })).setVisibleWhen(row -> !row.locked).setVariant(DropdownMenuItemBehavior.Variant.DESTRUCTIVE);
        columns.add(actions);
        provider.setSort("number", org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder.ASCENDING);
        table = new SwDataTable<>("invoices", columns, provider, 5).setLabel("Workspace invoices").setPaginationLabel("Invoice pages")
                .setPageSizes(5, 10, 20).setMaximumPageSize(50).setStickyFirstColumn(true).setQuery(new TableQuery(parameters));
        if (ajax) table.setQuery(null); table.setMarkupId("invoice-table");
        table.setEmptyState(id -> new SwEmptyState(id, "A little breathing room.", "No invoices match these filters. Try another name or status."));
        table.setToolbar(id -> new SearchToolbar(id));
        table.addBulkAction("Mark selected as paid", (target, rows) -> {
            Set<Long> ids = new HashSet<>(); rows.stream().filter(row -> !row.locked).forEach(row -> ids.add(row.id));
            store().invoices.replaceAll(row -> ids.contains(row.id) ? row.withStatus(Status.PAID) : row); table.clearSelection(); announce(target, ids.size() + " invoices marked as paid."); target.focusComponent(get("create"));
        }); add(table);
        add(SwButton.onClick("create", "New invoice", target -> edit(target, null)));
        add(mode("bookmarkable", "native", !ajax), mode("ajax", "ajax", ajax));
    }
    private BookmarkablePageLink<Void> mode(String id, String mode, boolean active) {
        var parameters = new PageParameters(getPageParameters()); parameters.remove("page"); parameters.set("navigation", mode);
        var link = new BookmarkablePageLink<Void>(id, DataTablePage.class, parameters);
        link.add(new ButtonBehavior(active ? ButtonBehavior.Variant.SECONDARY : ButtonBehavior.Variant.GHOST).setSize(ButtonBehavior.Size.SMALL));
        if (active) link.add(AttributeModifier.replace("aria-current", "page")); return link;
    }
    private void edit(AjaxRequestTarget target, Invoice row) {
        editingId = row == null ? null : row.id; customer.setObject(row == null ? "" : row.customer); email.setObject(row == null ? "" : row.email);
        amount.setObject(row == null ? new BigDecimal("125.00") : row.amount()); due.setObject(row == null ? LocalDate.of(2026, 11, 30) : row.due);
        editor.setTitle(row == null ? "New invoice" : "Edit " + row.number); editor.getForm().clearInput(); editor.open(target);
    }
    private void announce(AjaxRequestTarget target, String message) { status.setDefaultModelObject(message); target.add(table, stats, status); }
    static String money(long cents, Locale locale) { var format = java.text.NumberFormat.getCurrencyInstance(locale); format.setCurrency(Currency.getInstance("EUR")); return format.format(BigDecimal.valueOf(cents, 2)); }
    private TableQuery currentQuery() {
        var values = new PageParameters(getPageParameters()).set("size", table.getItemsPerPage()).set("page", table.getCurrentPage());
        if (provider.getSort() != null) values.set("sort", provider.getSort().getProperty() + (provider.getSort().isAscending() ? ",asc" : ",desc"));
        return new TableQuery(values);
    }
    public class SearchToolbar extends Panel {
        SearchToolbar(String id) {
            super(id); var form = new WebMarkupContainer("form");
            form.add(AttributeModifier.replace("action", (IModel<String>) () -> urlFor(DataTablePage.class, new PageParameters()).toString())); add(form);
            var input = new WebMarkupContainer("query"); input.add(AttributeModifier.replace("value", search)); form.add(input);
            form.add(new SwQueryParameters("params", (IModel<PageParameters>) () -> currentQuery().forSearch("q", "status")));
            for (Status value : Status.values()) form.add(new WebMarkupContainer(value.name()).add(AttributeModifier.replace("selected", value.name().equals(statusFilter) ? "selected" : null)));
            form.add(new SwIcon("searchIcon", "search"));
            var clear = new BookmarkablePageLink<Void>("clear", DataTablePage.class, currentQuery().forSearch("q", "status")); clear.add(new ButtonBehavior(ButtonBehavior.Variant.GHOST).setSize(ButtonBehavior.Size.SMALL)); form.add(clear);
        }
    }
    public class Editor extends Panel {
        Editor(String id) {
            super(id);
            add(new SwTextField<>("customer", "Customer", customer).setRequired(true));
            add(new SwEmailField("email", "Email", email).setRequired(true));
            add(new SwNumberField<>("amount", "Amount (EUR)", amount).setType(BigDecimal.class).setMin(BigDecimal.ZERO).setMax(new BigDecimal("100000")).setStep(new BigDecimal("0.01")).setRequired(true));
            add(new SwDateField("due", "Due date", due).setRequired(true));
        }
    }
    public static class CustomerCell extends Panel {
        CustomerCell(String id, IModel<Invoice> row) { super(id); add(new Label("name", row.map(Invoice::customer)), new Label("email", row.map(Invoice::email))); }
    }
    private class Provider extends SortableDataProvider<Invoice, String> {
        private List<Invoice> matching() {
            return store().invoices.stream().filter(row -> (search.isEmpty() || (row.customer + " " + row.email + " " + row.number).toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))
                    && (statusFilter.isEmpty() || row.status.name().equals(statusFilter))).toList();
        }
        @Override public Iterator<? extends Invoice> iterator(long first, long count) {
            Comparator<Invoice> comparator = switch (getSort() == null ? "number" : getSort().getProperty()) {
                case "customer" -> Comparator.comparing(Invoice::customer, String.CASE_INSENSITIVE_ORDER);
                case "status" -> Comparator.comparing(Invoice::status);
                case "amount" -> Comparator.comparingLong(Invoice::cents);
                case "due" -> Comparator.comparing(Invoice::due);
                default -> Comparator.comparing(Invoice::number);
            };
            if (getSort() != null && !getSort().isAscending()) comparator = comparator.reversed();
            return matching().stream().sorted(comparator.thenComparingLong(Invoice::id)).skip(first).limit(count).iterator();
        }
        @Override public long size() { return matching().size(); }
        @Override public IModel<Invoice> model(Invoice row) { return LoadableDetachableModel.of(() -> invoice(row.id)); }
    }
    @Override protected String getTitle() { return "Data tables"; }
}

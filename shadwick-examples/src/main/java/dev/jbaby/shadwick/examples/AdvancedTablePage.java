package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.behaviors.BadgeBehavior;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.components.*;
import dev.jbaby.shadwick.components.table.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.RangeValidator;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;

/**
 * A mutable, page-local work list: visibility, filtered exports, lazy details and validated inline
 * edits.
 */
public class AdvancedTablePage extends BasePage {

    private final List<Work> work = sampleWork();

    private final Model<String> query = Model.of("");

    private final IModel<Set<Long>> selected = Model.ofSet(new HashSet<>()),
            expanded = Model.ofSet(new HashSet<>());

    private String message = "Choose columns, open the details, or edit an estimate.";

    private final Provider provider = new Provider();

    private SwDataTable<Work, String> table;

    private Label receipt;

    public AdvancedTablePage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        addReceipt();
        addWorkTable();
        addFilterForm();
    }

    private void addReceipt() {
        receipt = new Label("receipt", (IModel<String>) () -> message);
        receipt.setOutputMarkupId(true);
        add(receipt);
    }

    private void addWorkTable() {
        List<IColumn<Work, String>> columns = workColumns();
        table =
                new SwDataTable<>("work", columns, provider, 5)
                        .setRowKey(row -> row.id)
                        .setLabel("Project work")
                        .setColumnChooser(true)
                        .setCsvExport("shadwick-work.csv")
                        .setPageSizes(5, 10, 20);
        table.setToolbar(
                id -> new Label(id, (IModel<String>) () -> provider.size() + " projects in view"));
        table.addBulkAction(
                "Mark reviewed",
                (target, rows) -> {
                    message = "Reviewed " + rows.size() + " selected projects.";
                    table.clearSelection(target);
                    target.add(receipt);
                });
        add(table);
    }

    private void addFilterForm() {
        Form<Void> filter =
                new Form<>("filter") {

                    @Override
                    protected void onSubmit() {
                        table.setCurrentPage(0);
                    }
                };
        filter.add(new TextField<>("query", query));
        var apply =
                new AjaxFallbackButton("apply", filter) {

                    @Override
                    protected void onSubmit(Optional<AjaxRequestTarget> target) {
                        table.setCurrentPage(0);
                        target.ifPresent(t -> t.add(table));
                    }
                };
        apply.add(new ButtonBehavior(ButtonBehavior.Variant.OUTLINE));
        filter.add(apply);
        add(filter);
    }

    private List<IColumn<Work, String>> workColumns() {
        var details =
                new SwRowDetailsColumn<Work, String>(
                                (id, row) -> new WorkDetails(id, row), expanded, row -> row.id)
                        .setRowLabel(row -> row.name);
        var selection =
                new SwSelectionColumn<Work, String>(
                                selected,
                                row -> row.id,
                                keys ->
                                        work.stream()
                                                .filter(row -> keys.contains(row.id) && !row.locked)
                                                .toList())
                        .setRowLabel(row -> row.name)
                        .setSelectableWhen(row -> !row.locked);
        var estimate =
                new SwEditableColumn<Work, String, Integer>(
                                Model.of("Hours"),
                                "hours",
                                row -> row.hours,
                                (row, value) -> row.hours = value)
                        .setType(Integer.class)
                        .setRequired(true)
                        .addValidator(RangeValidator.range(1, 80))
                        .setEditableWhen(row -> !row.locked)
                        .setOnSave(
                                (target, row) -> {
                                    message = "Saved " + row.hours + " hours for " + row.name + ".";
                                    if (target != null) target.add(receipt);
                                });
        List<IColumn<Work, String>> columns =
                List.of(
                        details,
                        selection,
                        new LambdaColumn<>(Model.of("Project"), "name", row -> row.name),
                        estimate,
                        new SwBadgeColumn<>(
                                Model.of("Status"),
                                "status",
                                row -> row.status,
                                row ->
                                        row.locked
                                                ? BadgeBehavior.Variant.SECONDARY
                                                : BadgeBehavior.Variant.OUTLINE),
                        new LambdaColumn<>(Model.of("Owner"), "owner", row -> row.owner),
                        new SwDateColumn<>(Model.of("Due"), "due", row -> row.due));
        return columns;
    }

    @Override
    protected String getTitle() {
        return "Advanced tables";
    }

    private static final class Work implements Serializable {

        final long id;

        final String name, owner, status, description;

        final boolean locked;

        final LocalDate due;

        Integer hours;

        Work(
                long id,
                String name,
                String owner,
                String status,
                int hours,
                String description,
                boolean locked) {
            this.id = id;
            this.name = name;
            this.owner = owner;
            this.status = status;
            this.hours = hours;
            this.description = description;
            this.locked = locked;
            due = LocalDate.of(2026, 11, 9).plusDays(id - 1001);
        }
    }

    private class Provider extends SortableDataProvider<Work, String> {

        Provider() {
            setSort("name", SortOrder.ASCENDING);
        }

        private List<Work> filtered() {
            String term =
                    Optional.ofNullable(query.getObject())
                            .orElse("")
                            .strip()
                            .toLowerCase(Locale.ROOT);
            var rows =
                    work.stream()
                            .filter(
                                    row ->
                                            (row.name + " " + row.owner + " " + row.status)
                                                    .toLowerCase(Locale.ROOT)
                                                    .contains(term));
            Comparator<Work> comparator = Comparator.comparing(row -> row.name);
            if (getSort() != null) {
                comparator =
                        switch (getSort().getProperty()) {
                            case "hours" -> Comparator.comparing(row -> row.hours);
                            case "status" -> Comparator.comparing(row -> row.status);
                            case "owner" -> Comparator.comparing(row -> row.owner);
                            case "due" -> Comparator.comparing(row -> row.due);
                            default -> comparator;
                        };
                if (!getSort().isAscending()) comparator = comparator.reversed();
            }
            return rows.sorted(comparator.thenComparingLong(row -> row.id)).toList();
        }

        @Override
        public Iterator<Work> iterator(long first, long count) {
            return filtered().stream().skip(first).limit(count).iterator();
        }

        @Override
        public long size() {
            return filtered().size();
        }

        @Override
        public IModel<Work> model(Work row) {
            return LoadableDetachableModel.of(
                    () -> work.stream().filter(w -> w.id == row.id).findFirst().orElse(null));
        }
    }

    public static class WorkDetails extends Panel {

        WorkDetails(String id, IModel<Work> row) {
            super(id);
            this.row = row;
        }

        @Override
        protected void onInitialize() {
            super.onInitialize();
            add(new Label("name", (IModel<String>) () -> row.getObject().name));
            add(new Label("description", (IModel<String>) () -> row.getObject().description));
            add(new Label("reference", (IModel<String>) () -> "TASK-" + row.getObject().id));
            add(new Label("owner", (IModel<String>) () -> row.getObject().owner));
            add(
                    new Label(
                            "editing",
                            (IModel<String>)
                                    () ->
                                            row.getObject().locked
                                                    ? "Approved estimate · read only"
                                                    : "Editable estimate · 1 to 80 hours"));
        }

        private final IModel<Work> row;
    }

    private static List<Work> sampleWork() {
        return new ArrayList<>(
                List.of(
                        new Work(
                                1001,
                                "Customer portal",
                                "Maya Chen",
                                "In progress",
                                8,
                                "Bring account settings, invoices and support into one calm"
                                        + " workspace.",
                                false),
                        new Work(
                                1002,
                                "Design system audit",
                                "Luka Novak",
                                "Ready",
                                5,
                                "Review contrast, keyboard behavior and shared component"
                                        + " patterns across the product.",
                                false),
                        new Work(
                                1003,
                                "Autumn release",
                                "Amelia Reed",
                                "Approved",
                                12,
                                "Approved estimates are locked. Open the details to read the"
                                        + " release checklist.",
                                true),
                        new Work(
                                1004,
                                "Onboarding flow",
                                "Noah Williams",
                                "In progress",
                                6,
                                "Help a new team find its first useful action, with clear"
                                        + " defaults and fewer steps.",
                                false),
                        new Work(
                                1005,
                                "Usage reports",
                                "Sara Kovač",
                                "Ready",
                                4,
                                "Make weekly activity easier to understand, with a readable"
                                        + " table behind every chart.",
                                false),
                        new Work(
                                1006,
                                "Billing preferences",
                                "Maya Chen",
                                "Ready",
                                3,
                                "Let account owners choose a billing contact and download their"
                                        + " records.",
                                false),
                        new Work(
                                1007,
                                "Search improvements",
                                "Luka Novak",
                                "In progress",
                                7,
                                "Improve empty states, query matching and keyboard access to"
                                        + " search results.",
                                false),
                        new Work(
                                1008,
                                "Mobile navigation",
                                "Amelia Reed",
                                "Ready",
                                5,
                                "Keep the most useful destinations within reach on smaller"
                                        + " screens.",
                                false),
                        new Work(
                                1009,
                                "Account security",
                                "Noah Williams",
                                "Approved",
                                10,
                                "A reviewed security checklist with clear status and a short"
                                        + " recovery path.",
                                true)));
    }
}

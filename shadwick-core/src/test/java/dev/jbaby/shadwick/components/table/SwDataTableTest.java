package dev.jbaby.shadwick.components.table;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.behaviors.BadgeBehavior;
import dev.jbaby.shadwick.behaviors.DropdownMenuItemBehavior;
import dev.jbaby.shadwick.components.*;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.*;
import org.apache.wicket.model.util.SetModel;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class SwDataTableTest {
    WicketTester tester; TablePage page;
    @BeforeEach void before() {
        tester = new WicketTester(); Shadwick.install(tester.getApplication()); tester.getSession().setLocale(Locale.US);
        page = new TablePage(new PageParameters()); tester.startPage(page);
    }
    @AfterEach void after() {
        if (tester.getLastResponse() != null) assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=\"");
        tester.destroy();
    }
    String html() { return tester.getLastResponseAsString(); }
    void sort() { tester.executeAjaxEvent(sortLink(), "click"); }
    Component sortLink() { return find(org.apache.wicket.ajax.markup.html.AjaxFallbackLink.class).stream().filter(c -> c.getMarkupId().endsWith("-sort-1")).findFirst().orElseThrow(); }
    <C extends Component> List<C> find(Class<C> type) { var result = new ArrayList<C>(); page.visitChildren(type, (c, visit) -> result.add(type.cast(c))); return result; }
    void check(Component component, boolean checked) {
        var box = (org.apache.wicket.markup.html.form.FormComponent<?>) component;
        if (checked) tester.getRequest().getPostParameters().setParameterValue(box.getInputName(), "on");
        tester.executeBehavior(box.getBehaviors(org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior.class).get(0));
    }
    void next() { tester.executeAjaxEvent("table:navigator:navigation:menu:items:3:link", "click"); }
    void selectRow(int index, boolean checked) {
        Component box = find(org.apache.wicket.ajax.markup.html.form.AjaxCheckBox.class).stream()
                .filter(c -> c.getPageRelativePath().contains(":body:rows:")).toList().get(index);
        check(box, checked);
    }
    List<TagTester> tags(String name, String value) { return TagTester.createTagsByAttribute(html(), name, value, false); }

    @Test void nativeHeadersRowsFormattingAndAccessibleControlNamesAreRendered() {
        tester.assertComponent("table:table", DataTable.class);
        assertThat(html()).contains("sw-table", "scope=\"col\"", "&lt;Ada&gt;", "$1,234.50", "Oct 9, 2026", "aria-label=\"Select &lt;Ada&gt;\"", "aria-label=\"Actions for &lt;Ada&gt;\"");
        assertThat(html()).contains("1\u20132 of 4", "sw-pagination", "sw-pagination-size");
        assertThat(tags("data-align", "end").stream().filter(tag -> tag.getName().equals("th")).toList()).hasSize(2); // amount and action headers
        assertThat(tags("role", "menuitem")).hasSize(4); // paid Ada only Edit; Ben Edit/Pay/Delete
    }
    @Test void ajaxSortingResetsThePageAndReportsOnlyOneSortedHeader() {
        next(); assertThat(page.table.getCurrentPage()).isEqualTo(1);
        sort(); assertThat(page.table.getCurrentPage()).isZero(); tester.assertComponentOnAjaxResponse(page.table);
        assertThat(tags("aria-sort", "ascending")).hasSize(1); assertThat(html()).contains("aria-sort=\"ascending\"");
        sort(); assertThat(tags("aria-sort", "descending")).hasSize(1);
    }
    @Test void nativeSortFallbackUsesTheSameServerSortState() {
        tester.clickLink(sortLink().getPageRelativePath(), false);
        assertThat(page.provider.getSort().getProperty()).isEqualTo("name"); assertThat(page.provider.getSort().isAscending()).isTrue();
    }
    @Test void pagingReplacesTheWholeWrapperSoSummaryAndControlsFollowTheRows() {
        next(); tester.assertComponentOnAjaxResponse(page.table);
        assertThat(page.table.getCurrentPage()).isEqualTo(1); assertThat(html()).contains("3\u20134 of 4", "Cyd", "Dora");
        assertThat(page.table.getPageCount()).isEqualTo(2); assertThat(page.table.getItemCount()).isEqualTo(4);
    }
    @Test void customEmptyContentSpansAllColumnsAndReplacesInsteadOfStacking() {
        page.rows.clear(); page.table.setEmptyState(id -> new Label(id, "First")).setEmptyState(id -> new Label(id, "No matching people"));
        tester.startPage(page); assertThat(html()).contains("No matching people", "colspan=\"7\"").doesNotContain("First", "No Records Found");
        assertThat(tags("aria-disabled", "true")).hasSizeGreaterThanOrEqualTo(2);
    }
    @Test void toolbarAndCaptionAreEscapedAndInvalidSlotIdsAreRejected() {
        page.table.setToolbar(id -> new Label(id, "<Toolbar>")).setCaption("<Caption>"); tester.startPage(page);
        assertThat(html()).contains("&lt;Toolbar&gt;", "&lt;Caption&gt;");
        assertThatIllegalArgumentException().isThrownBy(() -> page.table.setToolbar(id -> new Label("wrong")));
        assertThatIllegalArgumentException().isThrownBy(() -> page.table.setEmptyState(id -> null));
    }
    @Test void selectedKeysSurvivePagingAndBulkActionsLoadCurrentRows() {
        selectRow(0, true); assertThat(page.selected.getObject()).containsExactly(1L); assertThat(html()).contains("1 selected");
        tester.assertComponentOnAjaxResponse(page.table.get("bulkActions"));
        next(); selectRow(1, true); assertThat(page.selected.getObject()).containsExactlyInAnyOrder(1L, 4L);
        page.rows.set(0, page.rows.get(0).renamed("Ada updated"));
        tester.executeAjaxEvent("table:bulkActions:actions:0:action", "click");
        assertThat(page.events).containsExactly("Ada updated", "Dora"); assertThat(page.selected.getObject()).isEmpty();
    }
    @Test void selectAllUsesRenderedRowsAndLeavesLockedRowsAndOtherPagesAlone() {
        int queries = page.queries;
        Component all = find(org.apache.wicket.ajax.markup.html.form.AjaxCheckBox.class).stream().filter(c -> c.getPageRelativePath().contains(":headers:")).findFirst().orElseThrow();
        check(all, true);
        assertThat(page.selected.getObject()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(page.queries).isEqualTo(queries + 1); // only the changed table's ordinary re-render
        next(); assertThat(page.selected.getObject()).doesNotContain(3L); // locked checkbox rejects updates
    }
    @Test void partialSelectionSeedsNativeIndeterminateAndClearSelectionWorks() {
        selectRow(0, true); assertThat(html()).contains("data-sw-indeterminate=\"true\"");
        tester.executeAjaxEvent("table:bulkActions:clear", "click"); assertThat(page.selected.getObject()).isEmpty();
    }
    @Test void disabledRowsHaveNamedDisabledCheckboxes() {
        next(); var locked = TagTester.createTagByAttribute(html(), "aria-label", "Select Cyd");
        assertThat(locked.getAttribute("disabled")).isNotNull();
    }
    @Test void aRowActionRechecksItsPredicateAfterTheModelReloads() {
        page.rows.set(1, page.rows.get(1).markPaid());
        var menu = find(SwDropdown.class).get(1);
        tester.executeAjaxEvent(menu.get("menu:items:1"), "click");
        assertThat(page.events).isEmpty();
    }
    @Test void bookmarkableLinksPreserveFiltersAndWhitelistOnlyKnownSortProperties() {
        page = new TablePage(new PageParameters().set("bookmark", "true").set("q", "Ada lé").add("tag", "one").add("tag", "two")
                .set("sort", "name,desc").set("page", 1).set("size", 2)); tester.startPage(page);
        assertThat(page.provider.getSort().isAscending()).isFalse(); assertThat(page.table.getCurrentPage()).isEqualTo(1);
        assertThat(html()).contains("sort=name,asc", "tag=one", "tag=two", "page=0", "size=2");
        page = new TablePage(new PageParameters().set("bookmark", "true").set("sort", "secret,desc").set("size", 999999)); tester.startPage(page);
        assertThat(page.provider.getSort()).isNull(); assertThat(page.table.getItemsPerPage()).isEqualTo(200);
    }
    @Test void localeFormattingCanBeCustomizedWithoutChangingTheRawValue() {
        tester.getSession().setLocale(Locale.GERMANY); tester.startPage(page);
        assertThat(html()).contains("1.234,50", "09.10.2026");
        assertThat(page.rows.get(0).total()).isEqualByComparingTo("1234.50");
    }
    @Test void publicPageableMethodsAndValidationMatchStandardDataTable() {
        page.table.setItemsPerPage(3); assertThat(page.table.getPageCount()).isEqualTo(2);
        page.table.setCurrentPage(1); assertThat(page.table.getTable().getCurrentPage()).isEqualTo(1);
        assertThatIllegalArgumentException().isThrownBy(() -> page.table.setItemsPerPage(0));
        assertThatIllegalArgumentException().isThrownBy(() -> page.table.setMaximumPageSize(0));
        assertThatIllegalArgumentException().isThrownBy(() -> new SwDataTable<>("bad", List.of(), page.provider, 10));
        var plain = new SwDataTable<>("plain", List.of(new LambdaColumn<Person, String>(Model.of("Name"), Person::name)), page.provider, 10);
        assertThatIllegalStateException().isThrownBy(() -> plain.addBulkAction("Export", (target, rows) -> {}));
    }
    @Test void aKeyLoaderCannotAccidentallyReturnUnselectedRows() {
        var column = new SwSelectionColumn<Person, String>(new SetModel<>(new HashSet<>(Set.of(1L))), Person::id, keys -> page.rows);
        assertThat(column.getSelectedRows(page.provider)).extracting(Person::id).containsExactly(1L);
    }
    public record Person(long id, String name, boolean paid, BigDecimal total, LocalDate due, boolean locked) implements Serializable {
        Person renamed(String name) { return new Person(id, name, paid, total, due, locked); }
        Person markPaid() { return new Person(id, name, true, total, due, locked); }
    }
    public static class TablePage extends WebPage implements IMarkupResourceStreamProvider {
        final List<Person> rows = new ArrayList<>(List.of(
                new Person(1, "<Ada>", true, new BigDecimal("1234.50"), LocalDate.of(2026, 10, 9), false),
                new Person(2, "Ben", false, new BigDecimal("99"), null, false),
                new Person(3, "Cyd", false, null, null, true), new Person(4, "Dora", false, null, null, false)));
        final IModel<Set<Long>> selected = new SetModel<>(new HashSet<>());
        final List<String> events = new ArrayList<>(); int queries;
        final Provider provider = new Provider(); final SwDataTable<Person, String> table;
        public TablePage() { this(new PageParameters()); }
        public TablePage(PageParameters parameters) {
            super(parameters);
            List<IColumn<Person, String>> columns = new ArrayList<>();
            columns.add(new SwSelectionColumn<Person, String>(selected, Person::id, keys -> rows.stream().filter(p -> keys.contains(p.id())).toList())
                    .setRowLabel(Person::name).setSelectableWhen(p -> !p.locked()));
            columns.add(new LambdaColumn<>(Model.of("Name"), "name", Person::name));
            columns.add(new SwBadgeColumn<>(Model.of("Status"), "status", p -> p.paid() ? "Paid" : "Open", p -> p.paid() ? BadgeBehavior.Variant.SECONDARY : BadgeBehavior.Variant.OUTLINE));
            columns.add(new SwNumberColumn<Person, String>(Model.of("Total"), "total", Person::total).setCurrency(Currency.getInstance("USD")));
            columns.add(new SwDateColumn<>(Model.of("Due"), "due", Person::due));
            columns.add(new SwBooleanColumn<>(Model.of("Paid"), Person::paid));
            var actions = new SwActionsColumn<Person, String>().setRowLabel(Person::name);
            actions.addAction("Edit", (target, person) -> events.add("edit " + person.name()));
            actions.addAction("Pay", (target, person) -> events.add("pay " + person.name())).setVisibleWhen(p -> !p.paid());
            actions.addAction("Delete", (target, person) -> events.add("delete " + person.name())).setVisibleWhen(p -> !p.paid()).setVariant(DropdownMenuItemBehavior.Variant.DESTRUCTIVE);
            columns.add(actions); table = new SwDataTable<>("table", columns, provider, 2).setLabel("People");
            table.addBulkAction("Export", (target, people) -> { people.forEach(p -> events.add(p.name())); table.clearSelection(target); });
            if (parameters.get("bookmark").toBoolean(false)) table.setQuery(new TableQuery(parameters)); add(table);
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><head><title>People</title></head><body><div wicket:id='table'></div></body></html>");
        }
        private class Provider extends SortableDataProvider<Person, String> {
            @Override public Iterator<? extends Person> iterator(long first, long count) {
                queries++; var values = new ArrayList<>(rows);
                if (getSort() != null) { var comparator = Comparator.comparing(Person::name); if (!getSort().isAscending()) comparator = comparator.reversed(); values.sort(comparator.thenComparingLong(Person::id)); }
                return values.stream().skip(first).limit(count).iterator();
            }
            @Override public long size() { return rows.size(); }
            @Override public IModel<Person> model(Person person) { return LoadableDetachableModel.of(() -> rows.stream().filter(p -> p.id() == person.id()).findFirst().orElse(null)); }
        }
    }
}

package dev.jbaby.shadwick.components.table;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.components.*;
import dev.jbaby.shadwick.components.form.NotShownInlineFilter;
import org.apache.wicket.*;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.*;
import org.apache.wicket.util.resource.*;
import org.apache.wicket.util.tester.*;
import org.apache.wicket.validation.validator.RangeValidator;
import org.junit.jupiter.api.*;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class SwAdvancedTableTest {
    WicketTester tester; TablePage page;
    @BeforeEach void before() { tester = new WicketTester(); Shadwick.install(tester.getApplication()); tester.getSession().setLocale(Locale.US); page = new TablePage(); tester.startPage(page); }
    @AfterEach void after() { tester.destroy(); }
    String html() { return tester.getLastResponseAsString(); }
    <C extends Component> List<C> find(Class<C> type) { var all = new ArrayList<C>(); page.visitChildren(type, (c,v) -> all.add(type.cast(c))); return all; }
    Component first(String id) { return find(Component.class).stream().filter(c -> c.getId().equals(id)).findFirst().orElseThrow(); }
    void click(String id) { tester.executeAjaxEvent(first(id), "click"); }
    Form<?> edit() { click("edit"); return find(Form.class).stream().filter(f -> f.getId().equals("form")).findFirst().orElseThrow(); }
    void save(Form<?> form, String value, boolean ajax) {
        var f = tester.newFormTester(form.getPageRelativePath()); f.setValue("input", value);
        if (ajax) tester.executeAjaxEvent(form.get("save"), "click"); else f.submit("save");
    }
    @Test void chooserHidesHeaderAndCellsWithoutLosingPagingSelectionOrSort() {
        page.selected.getObject().add(3L); page.table.setCurrentPage(1); page.provider.setSort("name", org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder.DESCENDING);
        tester.startPage(page);
        var toggle = page.get("table:tools:chooser:menu:columns:1:toggle"); tester.executeAjaxEvent(toggle, "click");
        assertThat(page.table.isColumnVisible(2)).isFalse(); assertThat(page.table.getVisibleColumnCount()).isEqualTo(3);
        assertThat(page.table.getCurrentPage()).isEqualTo(1); assertThat(page.selected.getObject()).containsExactly(3L);
        assertThat(page.provider.getSort().isAscending()).isFalse();
        assertThat(html()).contains("sw-table-column-hidden", "hidden=\"hidden\"", "aria-checked=\"false\"");
        tester.assertComponentOnAjaxResponse(page.table);
        tester.clickLink("table:tools:chooser:menu:columns:1:toggle", false);
        assertThat(page.table.isColumnVisible(2)).isTrue();
    }
    @Test void structuralColumnsAndLastNamedDataColumnCannotBeHidden() {
        assertThatIllegalArgumentException().isThrownBy(() -> page.table.setColumnVisible(0, false));
        assertThatIllegalArgumentException().isThrownBy(() -> page.table.setColumnVisible(3, false));
        assertThatIndexOutOfBoundsException().isThrownBy(() -> page.table.isColumnVisible(-1));
        page.table.setColumnVisible(2, false);
        assertThatIllegalArgumentException().isThrownBy(() -> page.table.setColumnVisible(1, false));
        tester.startPage(page); assertThat(html()).contains("aria-disabled=\"true\"");
    }
    @Test void emptyStateUsesVisibleColumnCountAndToolbarCoexistsWithTools() {
        page.table.setToolbar(id -> new Label(id, "Search here")).setColumnVisible(2, false);
        page.rows.clear(); tester.startPage(page);
        assertThat(html()).contains("Search here", "Export CSV", "Columns", "colspan=\"3\"", "No results.");
    }
    @Test void detailsAreLazyAccessibleAndRememberStableKeysAcrossPaging() {
        assertThat(page.detailsCreated).isZero(); click("toggle");
        assertThat(page.detailsCreated).isEqualTo(1); assertThat(page.expanded.getObject()).containsExactly(1L);
        assertThat(html()).contains("aria-expanded=\"true\"", "Hide details for Alpha", "Notes for Alpha", "colspan=\"4\"");
        var toggle = first("toggle"); var controls = TagTester.createTagByAttribute(html(), "aria-expanded", "true").getAttribute("aria-controls");
        assertThat(controls).isNotBlank(); tester.assertComponentOnAjaxResponse(toggle);
        page.table.setCurrentPage(1); tester.startPage(page); assertThat(page.expanded.getObject()).containsExactly(1L);
        page.table.setCurrentPage(0); page.rows.set(0, new Row(1, "Alpha renewed", 2)); tester.startPage(page);
        assertThat(html()).contains("Notes for Alpha renewed", "aria-expanded=\"true\"");
        tester.clickLink(first("toggle").getPageRelativePath(), false); assertThat(page.expanded.getObject()).isEmpty();
    }
    @Test void detailsRespectChangedPredicateAndFactoryId() {
        page.details.setExpandableWhen(r -> false); tester.startPage(page); assertThat(html()).contains("aria-disabled=\"true\"");
        page.details.setExpanded(page.rows.get(0), true); assertThat(page.expanded.getObject()).isEmpty();
        var bad = new SwRowDetailsColumn<Row,String>((id,row) -> new Label("wrong"));
        assertThatIllegalArgumentException().isThrownBy(() -> bad.newDetails("details", Model.of(page.rows.get(0))));
    }
    @Test void validatedAjaxEditCommitsOnceAndReturnsFocus() {
        var form = edit(); assertThat(page.rows.get(0).quantity).isEqualTo(2);
        save(form,"8",true); assertThat(page.rows.get(0).quantity).isEqualTo(8); assertThat(page.saves).isEqualTo(1);
        assertThat(html()).contains("Edit Quantity: 8");
    }
    @Test void conversionRequiredAndRangeErrorsLeaveRowUnchangedAndStayInline() {
        for (String value : List.of("broken", "", "0")) {
            var form = edit(); save(form,value,true);
            assertThat(page.rows.get(0).quantity).isEqualTo(2); assertThat(page.saves).isZero();
            assertThat(html()).contains("aria-invalid=\"true\"", "aria-describedby=", "role=\"status\"");
            var input = (FormComponent<?>) form.get("input");
            assertThat(new NotShownInlineFilter().accept(input.getFeedbackMessages().first(org.apache.wicket.feedback.FeedbackMessage.ERROR))).isFalse();
            click("cancel");
        }
    }
    @Test void cancelDiscardsDraftAndPermissionIsRecheckedOnSave() {
        var form = edit(); save(form, "0", true); click("cancel"); assertThat(page.rows.get(0).quantity).isEqualTo(2);
        form = edit(); page.rows.get(0).locked = true; save(form, "9", true);
        assertThat(page.rows.get(0).quantity).isEqualTo(2); assertThat(page.saves).isZero(); assertThat(html()).contains("can no longer be edited");
    }
    @Test void concurrentValueChangeIsNotOverwritten() {
        var form = edit(); page.rows.get(0).quantity = 7; save(form, "9", true);
        assertThat(page.rows.get(0).quantity).isEqualTo(7); assertThat(page.saves).isZero(); assertThat(html()).contains("The value changed.");
    }
    @Test void nativeEditingSavesAndNativeCancelDiscardsInvalidInput() {
        tester.clickLink(first("edit").getPageRelativePath(), false);
        var form = find(Form.class).get(0); save(form,"6",false);
        assertThat(page.rows.get(0).quantity).isEqualTo(6); assertThat(page.saves).isEqualTo(1); assertThat(page.nativeSave).isTrue();
        tester.clickLink(first("edit").getPageRelativePath(), false);
        form = find(Form.class).get(0); save(form,"bad",false);
        tester.clickLink(first("cancel").getPageRelativePath(), false); assertThat(page.rows.get(0).quantity).isEqualTo(6);
    }
    @Test void saveFailureRollsBackInMemoryValueAndPropagates() {
        page.editable.setOnSave((target,row) -> { throw new IllegalStateException("Persistence failed"); }); var form=edit();
        assertThatThrownBy(() -> save(form,"8",true)).hasStackTraceContaining("Persistence failed"); assertThat(page.rows.get(0).quantity).isEqualTo(2);
    }
    @Test void csvDownloadIncludesHiddenColumnsAndEveryFilteredRow() {
        page.table.setColumnVisible(2,false); tester.startPage(page); tester.clickLink("table:tools:export",false);
        assertThat(tester.getLastResponse().getContentType()).startsWith("text/csv");
        assertThat(tester.getLastResponse().getHeader("Content-Disposition")).contains("attachment", "work.csv");
        assertThat(html()).startsWith("\ufeffName,Quantity\r\n").contains("Alpha,2\r\n", "Gamma,4\r\n");
    }
    @Test void csvQuotesAndDefusesOnlyTextWhileKeepingTypesMachineReadable() throws Exception {
        List<IColumn<Row,String>> columns=List.of(
                new LambdaColumn<>(Model.of("=Name"), r -> " \t=SUM(1,2)\n\"quoted\""),
                new LambdaColumn<>(Model.of("Amount"), r -> new BigDecimal("-12.50")),
                new LambdaColumn<>(Model.of("Date"), r -> LocalDate.of(2026,11,6)),
                new LambdaColumn<>(Model.of("Flag"), r -> true));
        var output = new ByteArrayOutputStream(); SwCsvExport.write(page.provider,columns,Locale.GERMANY,output);
        assertThat(output.toString(StandardCharsets.UTF_8)).startsWith("\ufeff'=Name,Amount,Date,Flag\r\n")
                .contains("\"' \t=SUM(1,2)\n\"\"quoted\"\"\",-12.50,2026-11-06,true\r\n");
    }
    @Test void csvBatchesAndDetachesEvenWhenValueOrOutputFails() throws Exception {
        page.rows.clear(); for(int i=0;i<1001;i++) page.rows.add(new Row(i,"Row"+i,i)); page.provider.batches.clear();
        var columns=List.of(new LambdaColumn<Row,String>(Model.of("Name"),r->r.name));
        SwCsvExport.write(page.provider,columns,Locale.US,new ByteArrayOutputStream());
        assertThat(page.provider.batches).containsExactly(500L,500L,1L);
        int detaches=page.provider.detaches, rowDetaches=page.provider.rowDetaches;
        var bad=List.of(new LambdaColumn<Row,String>(Model.of("Name"),r->{throw new IllegalStateException("bad value");}));
        assertThatThrownBy(() -> SwCsvExport.write(page.provider,bad,Locale.US,new ByteArrayOutputStream())).hasMessage("bad value");
        assertThat(page.provider.detaches).isEqualTo(detaches+1); assertThat(page.provider.rowDetaches).isGreaterThan(rowDetaches);
        int after=page.provider.detaches;
        assertThatIOException().isThrownBy(() -> SwCsvExport.write(page.provider,columns,Locale.US,new OutputStream(){@Override public void write(int b) throws IOException {throw new IOException("closed");}}));
        assertThat(page.provider.detaches).isEqualTo(after+1);
    }
    @Test void nativeInvalidEditorSurvivesFreshEntityModelsAndColumnChanges() {
        var old=page.rows.get(0); tester.clickLink(first("edit").getPageRelativePath(),false);
        var form=find(Form.class).get(0); save(form,"bad",false);
        page.rows.set(0,new Row(1,"Fresh Alpha",2));
        tester.clickLink("table:tools:chooser:menu:columns:0:toggle",false);
        assertThat(html()).contains("Fresh Alpha", "aria-invalid=\"true\"", "value=\"bad\"");
        form=find(Form.class).get(0); save(form,"11",false);
        assertThat(page.rows.get(0).quantity).isEqualTo(11); assertThat(old.quantity).isEqualTo(2);
    }
    @Test void pagingAwayDiscardsAnUnfinishedEditor() {
        edit(); page.table.setCurrentPage(1); tester.startPage(page);
        assertThat(find(Form.class)).isEmpty(); page.table.setCurrentPage(0); tester.startPage(page);
        assertThat(find(Form.class)).isEmpty(); assertThat(page.rows.get(0).quantity).isEqualTo(2);
    }
    @Test void nestedEditingScopesValidationAndLeavesTheOuterFormAlone() {
        page=new NestedPage(); tester.startPage(page); var form=edit();
        var button=TagTester.createTagByAttribute(html(),"wicket:id","save"); assertThat(button.getAttribute("type")).isEqualTo("button");
        save(form,"12",true); assertThat(page.rows.get(0).quantity).isEqualTo(12);
        assertThat(((NestedPage)page).outerSubmits).isZero(); assertThat(((Form<?>)page.get("outer")).hasError()).isFalse();
    }
    @Test void csvSupportsApplicationConvertersAndDetachesWhenSizeFails() throws Exception {
        var converter=(org.apache.wicket.ConverterLocator)tester.getApplication().getConverterLocator();
        converter.set(Row.class,new org.apache.wicket.util.convert.IConverter<Row>() {
            @Override public Row convertToObject(String value,Locale locale){throw new UnsupportedOperationException();}
            @Override public String convertToString(Row value,Locale locale){return "="+locale.getLanguage()+value.name;}
        });
        var output=new ByteArrayOutputStream();
        SwCsvExport.write(page.provider,List.of(new LambdaColumn<Row,String>(Model.of("Custom"),r->r)),Locale.GERMANY,output);
        assertThat(output.toString(StandardCharsets.UTF_8)).contains("'"+"=deAlpha");
        final boolean[] detached={false};
        var broken=new SortableDataProvider<Row,String>() {
            @Override public long size(){throw new IllegalStateException("count failed");}
            @Override public Iterator<Row> iterator(long first,long count){throw new AssertionError();}
            @Override public IModel<Row> model(Row row){return Model.of(row);}
            @Override public void detach(){detached[0]=true;}
        };
        assertThatThrownBy(()->SwCsvExport.write(broken,List.of(new LambdaColumn<Row,String>(Model.of("Name"),r->r.name)),Locale.US,output)).hasMessage("count failed");
        assertThat(detached[0]).isTrue();
    }
    @Test void detachedEntitiesAreNotRetainedInSerializedEditorPages() {
        DetachedProvider.current=new Entity(); var detached=new DetachedPage(); tester.startPage(detached);
        var links=new ArrayList<Component>(); detached.visitChildren(AjaxFallbackLink.class,(c,v)->{if(c.getId().equals("edit")) links.add(c);});
        tester.clickLink(links.get(0).getPageRelativePath(),false);
        var serializer=tester.getApplication().getFrameworkSettings().getSerializer();
        byte[] bytes=serializer.serialize(detached); assertThat(bytes).isNotEmpty();
        var restored=(DetachedPage)serializer.deserialize(bytes); tester.startPage(restored);
        var forms=new ArrayList<Form<?>>(); restored.visitChildren(Form.class,(c,v)->forms.add((Form<?>)c)); assertThat(forms).hasSize(1);
        var form=tester.newFormTester(forms.get(0).getPageRelativePath()); form.setValue("input","7"); form.submit("save");
        assertThat(DetachedProvider.current.quantity).isEqualTo(7);
    }
    @Test void filenamesRejectPathsAndResponseHeaderControls() {
        for(String name:List.of("", "..", "a/b.csv", "a\\b.csv", "a\r\nInjected.csv"))
            assertThatIllegalArgumentException().isThrownBy(() -> page.table.setCsvExport(name));
        page.table.setCsvExport(null); tester.startPage(page); assertThat(html()).doesNotContain("Export CSV");
    }
    static class Row implements Serializable { final long id; String name; Integer quantity; boolean locked; Row(long id,String name,Integer quantity){this.id=id;this.name=name;this.quantity=quantity;} }
    // Deliberately not Serializable; a production provider would load this entity from a database.
    static class Entity { Integer quantity=3; }
    static class DetachedProvider extends SortableDataProvider<Entity,String> {
        static Entity current=new Entity();
        @Override public Iterator<Entity> iterator(long first,long count){return List.of(current).iterator();}
        @Override public long size(){return 1;}
        @Override public IModel<Entity> model(Entity entity){return LoadableDetachableModel.of(()->current);}
    }
    public static class DetachedPage extends WebPage implements IMarkupResourceStreamProvider {
        public DetachedPage(){add(new SwDataTable<>("table",List.of(new SwEditableColumn<Entity,String,Integer>(Model.of("Quantity"),r->r.quantity,(r,v)->r.quantity=v).setType(Integer.class)),new DetachedProvider(),5));}
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer c,Class<?> t){return new StringResourceStream("<html xmlns:wicket='http://wicket.apache.org'><body><div wicket:id='table'></div></body></html>");}
    }
    public static class NestedPage extends TablePage {
        int outerSubmits;
        public NestedPage(){table.remove();var outer=new Form<Void>("outer"){@Override protected void onSubmit(){outerSubmits++;}};
            outer.add(new TextField<String>("required",Model.of("")).setRequired(true)); outer.add(table); add(outer);}
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer c,Class<?> t){return new StringResourceStream("<html xmlns:wicket='http://wicket.apache.org'><body><form wicket:id='outer'><input wicket:id='required'/><div wicket:id='table'></div></form></body></html>");}
    }
    public static class TablePage extends WebPage implements IMarkupResourceStreamProvider {
        final List<Row> rows=new ArrayList<>(List.of(new Row(1,"Alpha",2),new Row(2,"Beta",3),new Row(3,"Gamma",4)));
        final IModel<Set<Long>> expanded=Model.ofSet(new HashSet<>()),selected=Model.ofSet(new HashSet<>());
        final Provider provider=new Provider(); int detailsCreated,saves; boolean nativeSave;
        final SwRowDetailsColumn<Row,String> details=new SwRowDetailsColumn<Row,String>((id,row)-> {detailsCreated++;return new Label(id,(IModel<String>)()->"Notes for "+row.getObject().name);},expanded,r->r.id).setRowLabel(r->r.name);
        final SwEditableColumn<Row,String,Integer> editable=new SwEditableColumn<Row,String,Integer>(Model.of("Quantity"),"quantity",r->r.quantity,(r,v)->r.quantity=v)
                .setType(Integer.class).setRequired(true).addValidator(RangeValidator.range(1,100)).setEditableWhen(r->!r.locked)
                .setOnSave((target,row)->{saves++;nativeSave=target==null;});
        final SwDataTable<Row,String> table=new SwDataTable<>("table",List.of(details,new LambdaColumn<Row,String>(Model.of("Name"),"name",r->r.name),editable,new SwSelectionColumn<Row,String>(selected,r->r.id)),provider,2)
                .setRowKey(r->r.id).setColumnChooser(true).setCsvExport("work.csv");
        public TablePage(){add(table);}
        class Provider extends SortableDataProvider<Row,String>{
            int detaches,rowDetaches; final List<Long> batches=new ArrayList<>();
            @Override public Iterator<Row> iterator(long first,long count){batches.add(count);return rows.stream().skip(first).limit(count).iterator();}
            @Override public long size(){return rows.size();}
            @Override public IModel<Row> model(Row row){return new IModel<>(){public Row getObject(){return rows.stream().filter(r->r.id==row.id).findFirst().orElse(null);}@Override public void detach(){rowDetaches++;}};}
            @Override public void detach(){detaches++;}
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer c,Class<?> t){return new StringResourceStream("<html xmlns:wicket='http://wicket.apache.org'><body><div wicket:id='table'></div></body></html>");}
    }
}

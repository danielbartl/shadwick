package dev.jbaby.shadwick.components.form;

import com.github.openjson.JSONArray;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

class ComboboxTest {
    enum Kind { SINGLE, MULTIPLE }
    record Person(String id,String name,boolean locked) implements Serializable {}
    private static final List<Person> PEOPLE=List.of(new Person("alex","Alex",false),new Person("sam","Sam",false),new Person("locked","Locked",true));
    private WicketTester tester;
    @BeforeEach void setup() { tester=new WicketTester(); }
    @AfterEach void cleanup() { tester.destroy(); }
    private BaseSwField<?,?,?> local(Kind kind) {
        var choices=Model.ofList(PEOPLE); var renderer=new ChoiceRenderer<Person>("name","id");
        return kind==Kind.SINGLE
            ? new SwCombobox<>("choice","Person",Model.of(PEOPLE.get(0)),choices,renderer).setDisabledWhen(Person::locked).setGroup(p->"People").setIcon(p->"user")
            : new SwMultiCombobox<>("choice","People",Model.ofList(new ArrayList<>(List.of(PEOPLE.get(0)))),choices,renderer).setDisabledWhen(Person::locked).setGroup(p->"People").setIcon(p->"user");
    }
    private BaseSwField<?,?,?> remote(Kind kind) {
        return kind==Kind.SINGLE
            ? new SwCombobox<Person>("choice","Person",Model.of(PEOPLE.get(0))).setChoices(q->PEOPLE).setChoiceId(Person::id).setChoiceResolver(ComboboxTest::resolve).setDisplay(Person::name).setDisabledWhen(Person::locked).setMaxChoices(2)
            : new SwMultiCombobox<Person>("choice","People",Model.ofList(new ArrayList<>(List.of(PEOPLE.get(0))))).setChoices(q->PEOPLE).setChoiceId(Person::id).setChoiceResolver(ComboboxTest::resolve).setDisplay(Person::name).setDisabledWhen(Person::locked).setMaxChoices(2);
    }
    private static Person resolve(String id) { return PEOPLE.stream().filter(p->p.id().equals(id)).findFirst().orElse(null); }
    private void render(BaseSwField<?,?,?> field) {
        Form<Void> form=new Form<>("form");form.add(field);
        tester.startComponentInPage(form,Markup.of("<form wicket:id='form'><div wicket:id='choice'></div></form>"));
    }
    private void submit(String... values) {
        var form=tester.newFormTester("form");
        var input=(org.apache.wicket.markup.html.form.FormComponent<?>)tester.getComponentFromLastRenderedPage("form:choice:input");
        tester.getRequest().getPostParameters().setParameterValues(input.getInputName(), Arrays.stream(values).map(org.apache.wicket.util.string.StringValue::valueOf).toList());
        form.submit();
    }
    private void chosen(BaseSwField<?,?,?> field,Kind kind,Person person) {
        if (kind==Kind.SINGLE) assertThat(field.getDefaultModelObject()).isEqualTo(person);
        else assertThat((Collection<?>)field.getDefaultModelObject()).isEqualTo(List.of(person));
    }
    @ParameterizedTest @EnumSource(Kind.class)
    void markupUsesNativeChoicesAndFeedbackPaths(Kind kind) {
        var field=local(kind).setDescription("Choose people").setRequired(true); render(field);
        String html=tester.getLastResponseAsString();
        assertThat(html).contains("sw-combobox", "sw-combobox-select", "sw-combobox-group", "sw-icon", "aria-required=\"true\"").doesNotContain("style=","onclick=");
        assertThat(tester.getTagByWicketId("input").getAttribute("aria-describedby")).contains(tester.getTagByWicketId("error").getAttribute("id"));
        assertThat(tester.getTagByWicketId("label").getAttribute("for")).isEqualTo(field.getInput().getMarkupId());
    }
    @ParameterizedTest @EnumSource(Kind.class)
    void submissionsBindChoiceObjectsByStableId(Kind kind) {
        var field=local(kind);render(field);submit("sam");tester.assertNoErrorMessage();chosen(field,kind,PEOPLE.get(1));
    }
    @ParameterizedTest @EnumSource(Kind.class)
    void unknownAndDisabledIdsDoNotUpdateTheModel(Kind kind) {
        for (String id:List.of("foreign","locked")) {
            var field=local(kind);render(field);submit(id);
            assertThat(field.getInput().hasErrorMessage()).isTrue();chosen(field,kind,PEOPLE.get(0));
        }
    }
    @ParameterizedTest @EnumSource(Kind.class)
    void remoteChoicesResolveFreshObjectsWithoutAResultCache(Kind kind) {
        var field=remote(kind);render(field);submit("sam");tester.assertNoErrorMessage();chosen(field,kind,PEOPLE.get(1));
    }
    @ParameterizedTest @EnumSource(Kind.class)
    void remoteResolverRejectsUnavailableAndDisabledIds(Kind kind) {
        for(String id:List.of("gone","locked")) {
            var field=remote(kind);render(field);submit(id);assertThat(field.getInput().hasErrorMessage()).isTrue();chosen(field,kind,PEOPLE.get(0));
        }
    }
    @ParameterizedTest @EnumSource(Kind.class)
    void searchesAreBoundedJsonAndDoNotMutateSelection(Kind kind) {
        var field=remote(kind);render(field);
        tester.getRequest().getPostParameters().setParameterValue("q","a");
        tester.executeBehavior(field.getInput().getBehaviors(AbstractAjaxBehavior.class).get(0));
        JSONArray results=new JSONArray(tester.getLastResponseAsString());assertThat(results.length()).isEqualTo(2);
        assertThat(results.getJSONObject(0).getString("id")).isEqualTo("alex");chosen(field,kind,PEOPLE.get(0));
    }
    @ParameterizedTest @EnumSource(Kind.class)
    void nativeOptionalClearingAndRequiredValidationWork(Kind kind) {
        var field=local(kind);render(field);submit(kind==Kind.SINGLE?new String[]{""}:new String[]{});tester.assertNoErrorMessage();
        if(kind==Kind.SINGLE) assertThat(field.getDefaultModelObject()).isNull();else assertThat((Collection<?>)field.getDefaultModelObject()).isEmpty();
        field=local(kind).setRequired(true);render(field);submit(kind==Kind.SINGLE?new String[]{""}:new String[]{});
        assertThat(field.getInput().hasErrorMessage()).isTrue();chosen(field,kind,PEOPLE.get(0));
    }
    @Test void multiSelectionDeduplicatesIdsAndRejectsOneBadIdAtomically() {
        var field=local(Kind.MULTIPLE);render(field);submit("sam","sam");tester.assertNoErrorMessage();chosen(field,Kind.MULTIPLE,PEOPLE.get(1));
        submit("alex","locked");assertThat(field.getInput().hasErrorMessage()).isTrue();chosen(field,Kind.MULTIPLE,PEOPLE.get(1));
    }
    @Test void remoteConfigurationMustSupplyStableIdentityAndResolver() {
        var field=new SwCombobox<String>("choice","Choice",Model.of()).setChoices(q->List.of("One"));
        assertThatThrownBy(()->render(field)).hasStackTraceContaining("setChoiceId").hasStackTraceContaining("setChoiceResolver");
        assertThatIllegalArgumentException().isThrownBy(()->field.setMaxChoices(0));
        assertThatIllegalArgumentException().isThrownBy(()->field.setMaxChoices(201));
    }
    @Test void dynamicChoicesSnapshotOnceAndDetachEvenWhenInvisible() {
        AtomicInteger reads=new AtomicInteger(),detaches=new AtomicInteger();
        IModel<List<Person>> choices=new IModel<>() {
            public List<Person> getObject(){reads.incrementAndGet();return PEOPLE;}
            public void detach(){detaches.incrementAndGet();}
        };
        var field=new SwCombobox<>("choice","Choice",Model.of(PEOPLE.get(0)),choices,new ChoiceRenderer<>("name","id"));render(field);
        assertThat(reads.get()).isEqualTo(1);assertThat(detaches.get()).isPositive();
        detaches.set(0);field.setVisible(false);tester.startPage(tester.getLastRenderedPage());assertThat(detaches.get()).isPositive();
    }
    @Test void displayGroupAndPlaceholderAreEscaped() {
        var field=new SwCombobox<>("choice","Choice",Model.of("<unsafe>"),Model.ofList(List.of("<unsafe>"))).setGroup(x->"\"<group>").setPlaceholder("<prompt>");render(field);
        assertThat(tester.getLastResponseAsString()).contains("&lt;unsafe&gt;","&quot;&lt;group&gt;","&lt;prompt&gt;").doesNotContain("<unsafe>","<prompt>");
    }
    @Test void queryLengthIsBoundedAndCurrentResolvedIdentityIsChecked() {
        AtomicInteger length=new AtomicInteger();
        var field=new SwCombobox<Person>("choice","Choice",Model.of(PEOPLE.get(0))).setChoices(q->{length.set(q.length());return PEOPLE;})
                .setChoiceId(Person::id).setChoiceResolver(id->PEOPLE.get(0)).setDisplay(Person::name);
        render(field);tester.getRequest().getPostParameters().setParameterValue("q","x".repeat(300));
        tester.executeBehavior(field.getInput().getBehaviors(AbstractAjaxBehavior.class).get(0));assertThat(length.get()).isEqualTo(256);
        submit("sam");assertThat(field.getInput().hasErrorMessage()).isTrue();
    }
    @ParameterizedTest @EnumSource(Kind.class)
    void remoteChoicesSurviveAnotherFieldsFailedNormalSubmission(Kind kind) {
        var field=remote(kind);
        if(field instanceof SwCombobox<?> single)single.setMaxChoices(1);
        if(field instanceof SwMultiCombobox<?> many)many.setMaxChoices(1);
        var required=new org.apache.wicket.markup.html.form.TextField<String>("required",Model.of());required.setRequired(true);
        Form<Void> form=new Form<>("form");form.add(field,required);
        tester.startComponentInPage(form,Markup.of("<form wicket:id='form'><div wicket:id='choice'></div><input wicket:id='required'></form>"));
        submit("sam");assertThat(required.hasErrorMessage()).isTrue();chosen(field,kind,PEOPLE.get(0));
        var selected=org.apache.wicket.util.tester.TagTester.createTagByAttribute(tester.getLastResponseAsString(),"value","sam");
        assertThat(selected).isNotNull();assertThat(selected.getAttribute("selected")).isNotNull();
    }

}

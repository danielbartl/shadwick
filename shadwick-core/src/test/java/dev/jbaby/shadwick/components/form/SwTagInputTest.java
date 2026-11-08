package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.TagInputBehavior;
import java.util.*;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.*;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.validation.ValidationError;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class SwTagInputTest {
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(); }
    @AfterEach void stop() { tester.destroy(); }
    void render(SwTagInput field) {
        var form = new Form<Void>("form"); form.add(field);
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='tags'></div></form>"));
    }
    void submit(String value) { var f = tester.newFormTester("form"); f.setValue("tags:input", value); f.submit(); }
    @Test void convertsTrimsAndDeduplicatesExactValuesInOrder() {
        var model = Model.ofList(new ArrayList<>(List.of("old")));
        render(new SwTagInput("tags", "Tags", model));
        submit("  java, wicket ,,java,Java, <script>, café ,日本語,  ");
        tester.assertNoErrorMessage();
        assertThat(model.getObject()).containsExactly("java", "wicket", "Java", "<script>", "café", "日本語");
        assertThat(tester.getTagByWicketId("input").getAttribute("value")).contains("<script>");
        assertThat(tester.getLastResponseAsString()).contains("&lt;script&gt;");
        model.getObject().add("mutable");
    }
    @ParameterizedTest @ValueSource(strings={"", " ", ", ,,,", "\t,\n,\r"})
    void optionalEmptyInputCreatesAnEmptyList(String input) {
        var model = Model.ofList(new ArrayList<>(List.of("old")));
        render(new SwTagInput("tags", model)); submit(input);
        tester.assertNoErrorMessage(); assertThat(model.getObject()).isEmpty();
    }
    @ParameterizedTest @ValueSource(strings={"", " ", ", ,,,"})
    void requiredEmptyListsDoNotReplaceThePreviousModel(String input) {
        var model = Model.ofList(new ArrayList<>(List.of("old")));
        render(new SwTagInput("tags", "Topics", model).setRequired(true)); submit(input);
        assertThat(model.getObject()).containsExactly("old");
        assertThat(tester.getTagByWicketId("input").getAttribute("aria-invalid")).isEqualTo("true");
        assertThat(tester.getTagByWicketId("error").getValue()).contains("Topics");
        submit("new"); tester.assertNoErrorMessage(); assertThat(model.getObject()).containsExactly("new");
    }
    @Test void trimmingMatchesJavaRatherThanRemovingNonbreakingSpaces() {
        var model = Model.ofList(new ArrayList<String>()); render(new SwTagInput("tags", model));
        submit("\u0001 java \u001f,\u00a0java\u00a0,java");
        assertThat(model.getObject()).containsExactly("java", "\u00a0java\u00a0");
    }
    @Test void rendersNullModelAndNormalizesModelValuesWithoutMutatingThem() {
        var values = new ArrayList<>(Arrays.asList(" java ", "", null, "java", "Java"));
        var model = Model.ofList(values); render(new SwTagInput("tags", model));
        assertThat(tester.getTagByWicketId("input").getAttribute("value")).isEqualTo("java,Java");
        assertThat(model.getObject()).hasSize(5);
        model.setObject(null); tester.startPage(tester.getLastRenderedPage());
        assertThat(tester.getTagByWicketId("input").getAttribute("value")).isEmpty();
    }
    @Test void rejectsAmbiguousModelTagsContainingCommas() {
        assertThatThrownBy(() -> render(new SwTagInput("tags", Model.ofList(List.of("a,b"))))).hasStackTraceContaining("A tag cannot contain a comma");
    }
    @Test void validatorsReceiveTheListAndRetainInvalidRawInput() {
        var model = Model.ofList(new ArrayList<>(List.of("old")));
        var field = new SwTagInput("tags", model);
        field.addValidator(validatable -> { if (validatable.getValue().size() > 2) validatable.error(new ValidationError().setMessage("Choose at most two")); });
        render(field); submit("a, b, c");
        assertThat(model.getObject()).containsExactly("old");
        assertThat(tester.getTagByWicketId("input").getAttribute("value")).isEqualTo("a, b, c");
        assertThat(tester.getTagByWicketId("error").getValue()).isEqualTo("Choose at most two");
    }
    @Test void compoundPropertyModelsKeepTheNativeInputPath() {
        var form = new Form<>("form", new CompoundPropertyModel<>(new Data()));
        form.add(new SwTagInput("tags"));
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='tags'></div></form>"));
        submit("wicket, tags"); tester.assertNoErrorMessage();
        assertThat(form.getModelObject().tags).containsExactly("wicket", "tags");
    }
    @Test void labelsDescriptionsHintsAndRequiredSemanticsBelongToTheNativeInput() {
        var field = new SwTagInput("tags", "Topics <all>", Model.ofList(List.of("a"))).setRequired(true).setDescription("Project labels");
        render(field);
        var tag = tester.getTagByWicketId("input");
        assertThat(tag.getAttribute("type")).isEqualTo("text");
        assertThat(tag.getAttribute("aria-required")).isEqualTo("true");
        assertThat(tag.getAttribute("required")).isNull();
        assertThat(tag.getAttribute("aria-describedby")).contains(tester.getTagByWicketId("hint").getAttribute("id"), tester.getTagByWicketId("error").getAttribute("id"), tester.getTagByWicketId("description").getAttribute("id"));
        assertThat(tester.getTagByWicketId("label").getAttribute("for")).isEqualTo(tag.getAttribute("id"));
        assertThat(tester.getTagByWicketId("label").getValue()).isEqualTo("Topics &lt;all&gt;");
    }
    @Test void disabledComponentsIgnoreForgedSubmission() {
        var model = Model.ofList(new ArrayList<>(List.of("old")));
        var field = new SwTagInput("tags", model); field.setEnabled(false); render(field); submit("forged");
        assertThat(model.getObject()).containsExactly("old");
    }
    @Test void placeholderModelsCanBeReplacedOrClearedAndDetach() {
        var placeholder = new CountingModel("Custom tags");
        var field = new SwTagInput("tags").setPlaceholder(placeholder); render(field);
        assertThat(placeholder.detaches).isEqualTo(1);
        assertThat(tester.getTagByWicketId("input").getAttribute("placeholder")).isEqualTo("Custom tags");
        field.setPlaceholder("Other"); tester.startPage(tester.getLastRenderedPage());
        assertThat(tester.getTagByWicketId("input").getAttribute("placeholder")).isEqualTo("Other");
        field.setPlaceholder((IModel<String>) null); tester.startPage(tester.getLastRenderedPage());
        assertThat(tester.getTagByWicketId("input").getAttribute("placeholder")).isNull();
    }
    @ParameterizedTest @ValueSource(strings={"en", "de", "fr", "es", "it", "ja", "nl", "pt", "hr"})
    void everyLocaleContainsItsOwnHintsAndActionMessages(String language) {
        tester.getSession().setLocale(Locale.forLanguageTag(language)); render(new SwTagInput("tags"));
        assertThat(tester.getTagByWicketId("taginput").getAttribute("data-remove")).contains("{0}");
        assertThat(tester.getTagByWicketId("hint").getValue()).isNotBlank();
        if (!language.equals("en")) {
            assertThat(tester.getTagByWicketId("input").getAttribute("placeholder")).isNotEqualTo("Add tags…");
            assertThat(tester.getTagByWicketId("hint").getValue()).isNotEqualTo("Separate tags with commas.");
        }
    }
    @Test void behaviorOnlyUnbindsItsOwnInputDecorationAndFacadeUsesIt() {
        var input = new TextField<String>("input", Model.of("a,b"));
        input.add(AttributeModifier.replace("title", "Caller title"));
        var root = new WebMarkupContainer("root"); var behavior = Sw.Behaviors.tagInput(input);
        root.add(input); root.add(behavior); root.remove(behavior);
        assertThat(input.getBehaviors()).hasSize(1);
        assertThatThrownBy(() -> new TagInputBehavior(null)).isInstanceOf(NullPointerException.class);
    }
    public static class Data implements java.io.Serializable { public List<String> tags = new ArrayList<>(); }
    static class CountingModel extends Model<String> {
        int detaches;
        CountingModel(String value) { super(value); }
        @Override public void detach() { detaches++; }
    }
}

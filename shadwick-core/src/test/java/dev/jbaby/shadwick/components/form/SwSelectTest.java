package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.behaviors.SelectBehavior;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

class SwSelectTest {
    private WicketTester tester;
    private static final List<Team> TEAMS = List.of(new Team("design", "Design", "Product"),
            new Team("engineering", "Engineering", "Product"), new Team("support", "Support", "Service"));
    record Team(String id, String name, String group) implements Serializable {}
    @BeforeEach void setup() { tester = new WicketTester(); }
    @AfterEach void cleanup() { tester.destroy(); }
    private SwSelect<Team> field() {
        return new SwSelect<>("choice", "Team", Model.of(TEAMS.get(0)), Model.ofList(TEAMS), new ChoiceRenderer<>("name", "id"));
    }
    private void render(SwSelect<?> field) {
        Form<Void> form = new Form<>("form"); form.add(field);
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='choice'></div></form>"));
    }
    private String html() { return tester.getLastResponseAsString(); }
    private void submit(String value) {
        tester.newFormTester("form").setValue("choice:input", value).submit();
    }
    @Test void markupKeepsNativeBindingAndFieldAccessibility() {
        var field = field().setGroup(Team::group).setIcon(t -> "users").setSeparatorBefore(t -> t.id().equals("support"))
                .setSize(SelectBehavior.Size.SMALL).setDescription("Choose a team").setRequired(true);
        render(field);
        assertThat(html()).contains("sw-select-wrapper", "data-size=\"sm\"", "<selectedcontent>", "sw-select-item-indicator",
                "<optgroup class=\"sw-select-group\" label=\"Product\">", "<hr class=\"sw-select-separator\">")
                .doesNotContain("style=", "onclick=");
        var input = tester.getTagByWicketId("input");
        assertThat(input.getName()).isEqualTo("select");
        assertThat(input.getAttribute("aria-required")).isEqualTo("true");
        assertThat(input.getAttribute("aria-describedby")).contains(tester.getTagByWicketId("error").getAttribute("id"));
        assertThat(tester.getTagByWicketId("label").getAttribute("for")).isEqualTo(input.getAttribute("id"));
    }
    @Test void validStableIdUpdatesTheActualObject() {
        var field = field(); render(field); submit("support");
        tester.assertNoErrorMessage(); assertThat(field.getModelObject()).isEqualTo(TEAMS.get(2));
    }
    @Test void optionalEmptyChoiceClearsModel() {
        var field = field(); field.getInput().setNullValid(true); render(field); submit("");
        tester.assertNoErrorMessage(); assertThat(field.getModelObject()).isNull();
    }
    @Test void requiredEmptyChoiceShowsInlineErrorAndKeepsModel() {
        var field = field().setRequired(true); render(field); submit("");
        assertThat(field.getInput().hasErrorMessage()).isTrue();
        assertThat(field.getModelObject()).isEqualTo(TEAMS.get(0));
        assertThat(html()).contains("aria-invalid=\"true\"", "required");
    }
    @Test void disabledChoiceCannotBeForged() {
        var field = field().setDisabledWhen(t -> t.id().equals("support")); render(field); submit("support");
        assertThat(field.getInput().hasErrorMessage()).isTrue(); assertThat(field.getModelObject()).isEqualTo(TEAMS.get(0));
    }
    @Test void unknownChoiceCannotBeForged() {
        var field = field(); render(field); submit("missing");
        assertThat(field.getInput().hasErrorMessage()).isTrue(); assertThat(field.getModelObject()).isEqualTo(TEAMS.get(0));
    }
    @Test void rendererIsNotAskedToLoadAnUnknownId() {
        AtomicInteger loads = new AtomicInteger();
        var renderer = new ChoiceRenderer<Team>("name", "id") {
            @Override public Team getObject(String id, IModel<? extends List<? extends Team>> choices) { loads.incrementAndGet(); return TEAMS.get(2); }
        };
        var field = new SwSelect<>("choice", "Team", Model.of(TEAMS.get(0)), Model.ofList(TEAMS), renderer);
        render(field); submit("foreign"); assertThat(loads.get()).isZero(); assertThat(field.getInput().hasErrorMessage()).isTrue();
    }
    @Test void freshlyLoadedDisabledObjectsAreRejectedToo() {
        var renderer = new ChoiceRenderer<Team>("name", "id") {
            @Override public Team getObject(String id, IModel<? extends List<? extends Team>> choices) {
                return new Team(id, "Locked", "Restricted");
            }
        };
        var field = new SwSelect<>("choice", "Team", Model.of(TEAMS.get(0)), Model.ofList(TEAMS), renderer)
                .setDisabledWhen(t -> t.group().equals("Restricted"));
        render(field); submit("support"); assertThat(field.getInput().hasErrorMessage()).isTrue();
        assertThat(field.getModelObject()).isEqualTo(TEAMS.get(0));
    }
    @Test void choicesRemovedBetweenRequestsAreRejected() {
        var choices = Model.ofList(TEAMS); var field = new SwSelect<>("choice", "Team", Model.of(TEAMS.get(0)), choices, new ChoiceRenderer<>("name","id"));
        render(field); choices.setObject(List.of(TEAMS.get(0))); submit("support");
        assertThat(field.getInput().hasErrorMessage()).isTrue(); assertThat(field.getModelObject()).isEqualTo(TEAMS.get(0));
    }
    @Test void dynamicModelsAreReadOnceAndDetached() {
        AtomicInteger reads = new AtomicInteger(), detaches = new AtomicInteger();
        IModel<List<Team>> choices = new IModel<>() {
            public List<Team> getObject() { reads.incrementAndGet(); return TEAMS; }
            public void detach() { detaches.incrementAndGet(); }
        };
        var field = new SwSelect<>("choice", "Team", Model.of(TEAMS.get(0)), choices, new ChoiceRenderer<>("name","id"));
        render(field); assertThat(reads.get()).isEqualTo(1); assertThat(detaches.get()).isPositive();
    }
    @Test void invisibleFieldsStillDetachTheirChoiceModels() {
        AtomicInteger detaches = new AtomicInteger();
        IModel<List<Team>> choices = new IModel<>() {
            public List<Team> getObject() { return TEAMS; }
            public void detach() { detaches.incrementAndGet(); }
        };
        var field = new SwSelect<>("choice", "Team", Model.of(TEAMS.get(0)), choices);
        field.setVisible(false); render(field); assertThat(detaches.get()).isPositive();
    }
    @Test void textAndAttributesAreEscaped() {
        var field = new SwSelect<>("choice", "Choice", Model.of("<img src=x>"), Model.ofList(List.of("<img src=x>")))
                .setGroup(t -> "\"<unsafe>").setPlaceholder("<script>bad</script>");
        render(field);
        assertThat(html()).contains("&lt;img", "&quot;&lt;unsafe&gt;", "&lt;script&gt;").doesNotContain("<img", "<script>bad");
    }
    @Test void nullAndEmptyChoiceModelsRemainUsable() {
        var field = new SwSelect<String>("choice", "Choice", Model.of(), null); render(field);
        assertThat(html()).contains("data-placeholder=\"true\"", " hidden"); submit(""); tester.assertNoErrorMessage();
    }
    @Test void compoundPropertyModelKeepsShortInputPath() {
        class Settings implements Serializable { public Team choice = TEAMS.get(0); }
        var settings = new Settings(); var field = new SwSelect<>("choice", Model.ofList(TEAMS), new ChoiceRenderer<Team>("name","id"));
        var form = new Form<>("form", new CompoundPropertyModel<>(settings)); form.add(field);
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='choice'></div></form>"));
        submit("engineering"); tester.assertNoErrorMessage(); assertThat(settings.choice).isEqualTo(TEAMS.get(1));
    }
}

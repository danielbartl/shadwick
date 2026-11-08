package org.example.shadwick;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.components.chart.ChartSeries;
import dev.jbaby.shadwick.components.chart.SwChart;
import dev.jbaby.shadwick.components.form.SwNumberField;
import dev.jbaby.shadwick.components.form.SwTextField;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwSplitButton;
import dev.jbaby.shadwick.components.SwDescriptionList;
import dev.jbaby.shadwick.components.SwTimeline;
import dev.jbaby.shadwick.components.SwLazyLoadPanel;
import dev.jbaby.shadwick.behaviors.TimelineEventBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Compiles and runs as an application outside every library package, using Java 17 source syntax. */
class ApiConsumerTest {
    private final WicketTester tester = new WicketTester();

    @AfterEach void stop() { tester.destroy(); }

    @Test
    void fluentFieldsInheritedModelsAndAjaxCallbacksWorkAfterSerialization() throws Exception {
        Form<Draft> original = new Form<>("form", new CompoundPropertyModel<>(new Draft()));
        SwTextField<String> name = new SwTextField<String>("name").setRequired(true).setPlaceholder("Project name");
        SwNumberField<Integer> hours = new SwNumberField<Integer>("hours").setRequired(true).setMin(1).setMax(80);
        original.add(name, hours, Sw.Components.submitButton("save", Model.of("Save"),
                target -> original.getModelObject().saved++));

        Form<Draft> restored = roundTrip(original);
        tester.startComponentInPage(restored, Markup.of("<form wicket:id='form'><div wicket:id='name'></div>"
                + "<div wicket:id='hours'></div><button wicket:id='save'></button></form>"));
        var input = tester.newFormTester("form");
        input.setValue("name:input", ""); input.setValue("hours:input", "0");
        tester.executeAjaxEvent("form:save", "click");
        assertThat(restored.getModelObject().saved).isZero();
        assertThat(tester.getMessages(FeedbackMessage.ERROR)).isNotEmpty();

        input = tester.newFormTester("form");
        input.setValue("name:input", "Customer portal"); input.setValue("hours:input", "8");
        tester.executeAjaxEvent("form:save", "click");
        tester.assertNoErrorMessage();
        assertThat(restored.getModelObject().name).isEqualTo("Customer portal");
        assertThat(restored.getModelObject().hours).isEqualTo(8);
        assertThat(restored.getModelObject().saved).isEqualTo(1);
        assertThat(original.getModelObject().saved).isZero();
    }

    @Test
    void groupedAndSplitActionsUseConsumerCallbacksAfterSerialization() throws Exception {
        var count = Model.of(0);
        var original = new WebMarkupContainer("root", count);
        original.add(Sw.Components.buttonGroup("group", Model.ofList(List.of("One", "Two")), (item, value) ->
                item.add(SwButton.onClick("button", value, target -> count.setObject(count.getObject() + 1))))
                .setLabel("Tools"));
        var split = new SwSplitButton("split", Model.of("Save"), target -> count.setObject(count.getObject() + 10));
        split.addAction("Draft", target -> count.setObject(count.getObject() + 100)); original.add(split);
        WebMarkupContainer restored = roundTrip(original);
        tester.startComponentInPage(restored, Markup.of("<div wicket:id='root'><menu wicket:id='group'>"
                + "<li wicket:id='items'><button wicket:id='button'></button></li></menu><div wicket:id='split'></div></div>"));
        tester.executeAjaxEvent("root:group:items:1:button", "click");
        tester.executeAjaxEvent("root:split:primary", "click");
        tester.executeAjaxEvent("root:split:menu:items:0", "click");
        assertThat(restored.getDefaultModelObject()).isEqualTo(111);
        assertThat(original.getDefaultModelObject()).isEqualTo(0);
    }

    @Test
    void recordDetailsAndTypedHistoriesRemainUsableFromApplicationPackages() throws Exception {
        Draft draft = new Draft(); draft.name = "Customer portal"; draft.hours = 1234;
        var model = Model.of(draft); var root = new WebMarkupContainer("root", model);
        root.add(new SwDescriptionList("details", model).addProperty("name").addProperty("hours")
                .addItem(Model.of("Status"), id -> new org.apache.wicket.markup.html.basic.Label(id, "Active"))
                .setTimeZone(java.time.ZoneId.of("Europe/Berlin")));
        root.add(new SwTimeline<>("history", Model.ofList(List.of(new Project("Created", 8))), Project::name,
                project -> java.time.LocalDate.of(2026,11,8))
                .setDescription(project -> "Hours " + project.hours())
                .setVariant(project -> TimelineEventBehavior.Variant.OUTLINE));
        WebMarkupContainer restored = roundTrip(root);
        tester.startComponentInPage(restored, Markup.of("<div wicket:id='root'><dl wicket:id='details'></dl><div wicket:id='history'></div></div>"));
        assertThat(tester.getLastResponseAsString()).contains("Customer portal", "Active", "Created", "Hours 8", "datetime=\"2026-11-08\"");
    }

    @Test
    void typedLazyFactoriesKeepTheirModelsAndCallbacksAcrossSerialization() throws Exception {
        Model<Integer> calls=Model.of(0);
        var original=new ConsumerLazy("lazy",calls);
        SwLazyLoadPanel<?> restored=roundTrip(original);tester.startComponentInPage(restored);
        assertThat(restored.getDefaultModelObject()).isEqualTo(0);
        tester.executeAllTimerBehaviors(tester.getLastRenderedPage());
        assertThat(restored.getDefaultModelObject()).isEqualTo(1);
        assertThat(tester.getLastResponseAsString()).contains("Consumer report 1");
        assertThat(original.getDefaultModelObject()).isEqualTo(0);
    }

    @Test
    void typedChartFactoriesAndConsumerMethodReferencesSurviveSerialization() throws Exception {
        SwChart<Project> original = new SwChart<>("chart", Model.ofList(List.of(new Project("Customer portal", 8))), Project::name);
        original.setTitle("Planned hours").setValueFormat(SwChart.ValueFormat.INTEGER)
                .addSeries(ChartSeries.bar("Hours", Project::hours));
        SwChart<Project> restored = roundTrip(original);
        tester.startComponentInPage(restored);
        assertThat(tester.getLastResponseAsString()).contains("Customer portal", "Planned hours", "Hours", ">8<");
    }

    @Test
    void tagFieldsKeepTypedModelsConvertersAndValidatorsAfterSerialization() throws Exception {
        var original = new dev.jbaby.shadwick.components.form.SwTagInput("tags", "Topics", Model.ofList(List.of("original")))
                .setPlaceholder("Add a topic").setRequired(true);
        original.addValidator(value -> { if (value.getValue().size() > 2) value.error(new org.apache.wicket.validation.ValidationError().setMessage("Two tags maximum")); });
        var restored = roundTrip(original);
        var form = new Form<Void>("form"); form.add(restored);
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='tags'></div></form>"));
        var input = tester.newFormTester("form"); input.setValue("tags:input", "a,b,c"); input.submit();
        assertThat(restored.getModelObject()).containsExactly("original");
        input = tester.newFormTester("form"); input.setValue("tags:input", " wicket,java,wicket "); input.submit();
        tester.assertNoErrorMessage(); assertThat(restored.getModelObject()).containsExactly("wicket", "java");
        assertThat(original.getModelObject()).containsExactly("original");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Serializable> T roundTrip(T value) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var output = new ObjectOutputStream(bytes)) { output.writeObject(value); }
        try (var input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) { return (T) input.readObject(); }
    }

    @Test
    void stepsKeepTheirModelsAndSelectionCallbackAfterSerialization() throws Exception {
        var current = Model.of(2);
        var original = new dev.jbaby.shadwick.components.SwSteps("steps", List.of("Details", "Review", "Publish"), current)
                .setLabel("Project stages").setOnSelect((target, index) -> current.setObject(index));
        var restored = roundTrip(original); tester.startComponentInPage(restored);
        tester.executeAjaxEvent("steps:list:items:1:link", "click");
        assertThat(current.getObject()).isEqualTo(2);
        tester.startComponentInPage(restored); tester.assertLabel("steps:list:items:1:link:status", "Current");
    }

    @Test
    void avatarGroupFactoriesKeepTypedPopulationAndSizeAfterSerialization() throws Exception {
        var original = Sw.Components.avatarGroup("group", Model.ofList(List.of("Ada Lovelace", "Grace Hopper")),
                dev.jbaby.shadwick.components.SwAvatarGroup.Size.LARGE,
                (item, name) -> item.add(new dev.jbaby.shadwick.components.SwAvatar("avatar",
                        dev.jbaby.shadwick.components.SwAvatar.initialsOf(name)).setLabel(name))).setLabel("Team");
        var restored = roundTrip(original);
        tester.startComponentInPage(restored, Markup.of("<ul wicket:id='group'><li wicket:id='avatars'><span wicket:id='avatar'></span></li></ul>"));
        tester.assertLabel("group:avatars:0:avatar:container:initials", "AL");
        tester.assertLabel("group:avatars:1:avatar:container:initials", "GH");
        assertThat(tester.getTagByWicketId("group").getAttribute("data-size")).isEqualTo("lg");
    }

    public static final class Draft implements Serializable {
        public String name;
        public Integer hours;
        public int saved;
    }

    public static final class ConsumerLazy extends SwLazyLoadPanel<org.apache.wicket.markup.html.basic.Label> {
        public ConsumerLazy(String id,Model<Integer> model){super(id,model);}
        @Override public org.apache.wicket.markup.html.basic.Label getLazyLoadComponent(String id){
            int calls=(Integer)getDefaultModelObject()+1;setDefaultModelObject(calls);
            return new org.apache.wicket.markup.html.basic.Label(id,"Consumer report "+calls);
        }
    }

    private record Project(String name, Integer hours) implements Serializable {}
}

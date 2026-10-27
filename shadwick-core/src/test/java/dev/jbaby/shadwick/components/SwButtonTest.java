package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SwButtonTest {

    private WicketTester tester;
    private boolean clicked;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
        clicked = false;
    }

    @AfterEach
    void tearDown() {
        tester.destroy();
    }

    private SwButton button(String label) {
        return new SwButton("button", label) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                clicked = true;
            }
        };
    }

    @Test
    void itRendersItsLabelWithoutInnerMarkup() {
        tester.startComponentInPage(button("Click <Me>"), Markup.of("<a wicket:id=\"button\"></a>"));

        TagTester tag = tester.getTagByWicketId("button");
        assertThat(tag.getAttribute("class")).isEqualTo("sw-button");
        assertThat(tag.getValue()).isEqualTo("Click &lt;Me&gt;");
    }

    @Test
    void onAButtonTagItNeverSubmitsAForm() {
        tester.startComponentInPage(button("Click Me"), Markup.of("<button wicket:id=\"button\"></button>"));

        assertThat(tester.getTagByWicketId("button").getAttribute("type")).isEqualTo("button");
    }

    @Test
    void withoutALabelItRendersItsMarkupBody() {
        SwButton icon = new SwButton("button") {
            @Override
            public void onClick(AjaxRequestTarget target) {
            }
        }.setSize(ButtonBehavior.Size.ICON).setVariant(ButtonBehavior.Variant.DESTRUCTIVE);
        tester.startComponentInPage(icon, Markup.of("<button wicket:id=\"button\" aria-label=\"Delete\"><svg></svg></button>"));

        TagTester tag = tester.getTagByWicketId("button");
        assertThat(tag.getValue()).isEqualTo("<svg></svg>");
        assertThat(tag.getAttribute("data-size")).isEqualTo("icon");
        assertThat(tag.getAttribute("data-variant")).isEqualTo("destructive");
    }

    @Test
    void clickingRunsOnClick() {
        tester.startComponentInPage(button("Click Me"), Markup.of("<a wicket:id=\"button\"></a>"));
        tester.clickLink("button");

        assertThat(clicked).isTrue();
    }

    @Test
    void theFactoryTakesAModelLabel() {
        SwButton button = Sw.Components.button("button", Model.of("Click Me"), target -> clicked = true);
        tester.startComponentInPage(button, Markup.of("<a wicket:id=\"button\"></a>"));

        assertThat(tester.getTagByWicketId("button").getValue()).isEqualTo("Click Me");
        tester.clickLink("button");
        assertThat(clicked).isTrue();
    }

    @Test
    void theSubmitButtonRunsOnSubmitOrOnError() {
        List<String> calls = new ArrayList<>();
        Model<String> name = Model.of("");
        Form<Void> form = new Form<>("form");
        form.add(new TextField<>("name", name).setRequired(true));
        form.add(Sw.Components.submitButton("save", "Save", target -> calls.add("submit"), target -> calls.add("error")));
        tester.startComponentInPage(form, Markup.of(
                "<form wicket:id='form'><input wicket:id='name'/><button wicket:id='save'></button></form>"));

        TagTester save = tester.getTagByWicketId("save");
        assertThat(save.getValue()).isEqualTo("Save");
        assertThat(save.getAttribute("class")).isEqualTo("sw-button");

        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("name", "");
        tester.executeAjaxEvent("form:save", "click");
        assertThat(calls).containsExactly("error");

        formTester = tester.newFormTester("form");
        formTester.setValue("name", "Ada");
        tester.executeAjaxEvent("form:save", "click");
        assertThat(calls).containsExactly("error", "submit");
        assertThat(name.getObject()).isEqualTo("Ada");
    }

    @Test
    void theSubmitButtonOnAnInputUsesTheLabelAsValue() {
        Form<Void> form = new Form<>("form");
        form.add(new SwSubmitButton("save", "Save").setVariant(ButtonBehavior.Variant.SECONDARY));
        tester.startComponentInPage(form, Markup.of(
                "<form wicket:id='form'><input type='submit' wicket:id='save'/></form>"));

        TagTester save = tester.getTagByWicketId("save");
        assertThat(save.getAttribute("value")).isEqualTo("Save");
        assertThat(save.getAttribute("data-variant")).isEqualTo("secondary");
    }

    @Test
    void aSubmitButtonOutsideItsFormSubmitsIt() {
        Model<String> name = Model.of("");
        Form<Void> form = new Form<>("form");
        form.add(new TextField<>("name", name));
        WebMarkupContainer toolbar = new WebMarkupContainer("toolbar");
        toolbar.add(SwSubmitButton.onSubmit("save", "Save", form, target -> clicked = true));
        WebMarkupContainer page = new WebMarkupContainer("page");
        page.add(form, toolbar);
        tester.startComponentInPage(page, Markup.of("<div wicket:id='page'><form wicket:id='form'><input wicket:id='name'/></form>"
                + "<div wicket:id='toolbar'><button wicket:id='save'></button></div></div>"));

        tester.newFormTester("page:form").setValue("name", "Ada");
        tester.executeAjaxEvent("page:toolbar:save", "click");

        assertThat(clicked).isTrue();
        assertThat(name.getObject()).isEqualTo("Ada");
    }

    @Test
    void inANestedFormTheSubmitButtonIsNoSubmitButtonOfTheOuterForm() {
        Form<Void> outer = new Form<>("outer");
        Form<Void> inner = new Form<>("inner");
        inner.add(new SwSubmitButton("save", "Save"));
        outer.add(inner);
        tester.startComponentInPage(outer, Markup.of(
                "<form wicket:id='outer'><form wicket:id='inner'><button wicket:id='save'></button></form></form>"));

        assertThat(tester.getTagByWicketId("save").getAttribute("type")).isEqualTo("button");
    }
}

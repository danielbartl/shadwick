package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.components.form.SwTextField;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Steps in order, each validated on Next, Back without validating, finished steps revisited, and plain posts. */
class SwWizardTest {
    WicketTester tester;
    final Model<String> name = new Model<>(), email = new Model<>();
    final List<String> finished = new ArrayList<>();
    final List<Boolean> finishedWithAjax = new ArrayList<>();

    @BeforeEach void start() {
        tester = new WicketTester();
        tester.getSession().setLocale(Locale.US);
        tester.getApplication().getApplicationSettings().setFeedbackMessageCleanupFilter(
                new org.apache.wicket.feedback.DefaultCleanupFeedbackMessageFilter());
    }
    @AfterEach void stop() {
        if (tester.getLastResponse() != null)
            assertThat(tester.getLastResponseAsString()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        tester.destroy();
    }

    String html() { return tester.getLastResponseAsString(); }
    List<TagTester> tags(String attribute, String value) { return TagTester.createTagsByAttribute(html(), attribute, value, false); }

    SwWizard wizard() {
        return Sw.Components.wizard("wizard", target -> {
                    finished.add(name.getObject() + " " + email.getObject());
                    finishedWithAjax.add(target != null);
                })
                .addStep("Name", id -> new SwTextField<>(id, "Name", name).setRequired(true))
                .addStep("Email", id -> new SwTextField<>(id, "Email", email).setRequired(true))
                .addStep("Review", id -> new Label(id, "All set"));
    }

    @Test void stepsGoInOrderEachValidatedOnNext() {
        SwWizard wizard = wizard();
        tester.startComponentInPage(wizard);
        assertThat(tags("wicket:id", "items")).extracting(s -> s.getAttribute("data-state")).containsExactly("current", "upcoming", "upcoming");
        tester.assertInvisible("wizard:form:back");
        tester.assertInvisible("wizard:form:finish");
        assertThat(tester.getTagByWicketId("group").getAttribute("aria-label")).isEqualTo("Step 1 of 3: Name");

        // Next with the required field empty stays and shows the error next to the field
        tester.executeAjaxEvent("wizard:form:next", "click");
        assertThat(wizard.getCurrentStep()).isZero();
        assertThat(html()).contains("&#039;Name&#039; is required.");

        FormTester form = tester.newFormTester("wizard:form");
        form.setValue("group:step:input", "Ada");
        tester.executeAjaxEvent("wizard:form:next", "click");
        assertThat(wizard.getCurrentStep()).isEqualTo(1);
        assertThat(tags("wicket:id", "items")).extracting(s -> s.getAttribute("data-state")).containsExactly("done", "current", "upcoming");
        // The new step takes the focus
        assertThat(html()).contains("Wicket.Focus.setFocusOnId('" + wizard.get("form:group").getMarkupId() + "')");

        // Back to a finished step by its number, keeping its value, then forward again
        tester.clickLink(wizard.get("steps:list:items:0:link"));
        assertThat(wizard.getCurrentStep()).isZero();
        assertThat(tester.getTagByWicketId("input").getAttribute("value")).isEqualTo("Ada");
        tester.executeAjaxEvent("wizard:form:next", "click");
        form = tester.newFormTester("wizard:form");
        form.setValue("group:step:input", "ada@example.com");
        tester.executeAjaxEvent("wizard:form:next", "click");
        assertThat(wizard.getCurrentStep()).isEqualTo(2);
        tester.assertInvisible("wizard:form:next");

        tester.executeAjaxEvent("wizard:form:finish", "click");
        assertThat(finished).containsExactly("Ada ada@example.com");
        assertThat(finishedWithAjax).containsExactly(true);
    }

    @Test void backGoesWithoutValidating() {
        SwWizard wizard = wizard().setCurrentStep(1);
        tester.startComponentInPage(wizard);
        tester.executeAjaxEvent("wizard:form:back", "click");
        assertThat(wizard.getCurrentStep()).isZero();
        assertThat(html()).doesNotContain("is required.");
    }

    @Test void onNextCanKeepTheUserOnAStepWithAnErrorInTheSummary() {
        SwWizard wizard = new SwWizard("wizard") {
            @Override protected void onNext(int step, AjaxRequestTarget target) { getForm().error("Not yet"); }
            @Override protected void onFinish(AjaxRequestTarget target) { }
        };
        wizard.addStep("One", id -> new Label(id, "1")).addStep("Two", id -> new Label(id, "2"));
        tester.startComponentInPage(wizard);
        tester.executeAjaxEvent("wizard:form:next", "click");
        assertThat(wizard.getCurrentStep()).isZero();
        assertThat(html()).contains("sw-form-errors", "Not yet");
    }

    @Test void withoutJavaScriptTheButtonsPostTheFormAndOnFinishGetsNoTarget() {
        SwWizard wizard = wizard();
        tester.startComponentInPage(wizard);
        FormTester form = tester.newFormTester("wizard:form");
        form.setValue("group:step:input", "Grace");
        form.submit("next");
        assertThat(wizard.getCurrentStep()).isEqualTo(1);
        form = tester.newFormTester("wizard:form");
        form.submit("next");
        assertThat(wizard.getCurrentStep()).isEqualTo(1);
        assertThat(html()).contains("&#039;Email&#039; is required.");
        form = tester.newFormTester("wizard:form");
        form.setValue("group:step:input", "grace@example.com");
        form.submit("next");
        form = tester.newFormTester("wizard:form");
        form.submit("finish");
        assertThat(finished).containsExactly("Grace grace@example.com");
        assertThat(finishedWithAjax).containsExactly(false);
    }

    @Test void enterMovesOnAndOnTheLastStepFinishes() {
        SwWizard wizard = wizard();
        tester.startComponentInPage(wizard);
        var form = (org.apache.wicket.markup.html.form.Form<?>) wizard.get("form");
        assertThat(form.getDefaultButton()).isSameAs(wizard.get("form:next"));
        wizard.setCurrentStep(2);
        tester.startComponentInPage(wizard);
        assertThat(form.getDefaultButton()).isSameAs(wizard.get("form:finish"));
    }

    @Test void stepsAndIdsAreChecked() {
        assertThatThrownBy(() -> tester.startComponentInPage(Sw.Components.wizard("w", target -> { })))
                .isInstanceOf(IllegalStateException.class);
        SwWizard wizard = Sw.Components.wizard("w", target -> { }).addStep("One", id -> new Label(id));
        assertThatThrownBy(() -> wizard.setCurrentStep(1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> tester.startComponentInPage(Sw.Components.wizard("w", target -> { })
                .addStep("One", id -> new Label("other")))).hasStackTraceContaining("A wizard step must use the id its factory is given (\"step\"), but was \"other\"");
    }

    @Test void labelsAreTranslatedAndTheWizardSerializes() {
        tester.getSession().setLocale(Locale.GERMAN);
        // Built from local models only: the test class itself isn't serializable
        Model<String> city = new Model<>();
        SwWizard wizard = Sw.Components.wizard("wizard", target -> city.setObject("done"))
                .addStep("Name", id -> new SwTextField<>(id, "Name", city))
                .addStep("Email", id -> new Label(id, "-"))
                .addStep("Review", id -> new Label(id, "-"));
        tester.startComponentInPage(wizard);
        assertThat(html()).contains("Weiter", "Schritt 1 von 3: Name");
        try (var out = new java.io.ObjectOutputStream(new java.io.ByteArrayOutputStream())) {
            out.writeObject(wizard);
        } catch (java.io.IOException e) {
            throw new AssertionError("Not serializable: " + e, e);
        }
    }
}

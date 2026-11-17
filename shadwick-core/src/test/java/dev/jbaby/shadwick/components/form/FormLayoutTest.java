package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.components.SwFeedbackPanel;
import dev.jbaby.shadwick.components.SwFieldGroup;
import dev.jbaby.shadwick.components.SwFormErrors;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.*;

/** Field orientation, field groups and the form error summary. */
class FormLayoutTest {
    private WicketTester tester;

    @BeforeEach void setUp() {
        tester = new WicketTester();
        // As in an application: messages shown in one request are gone in the next
        tester.getApplication().getApplicationSettings().setFeedbackMessageCleanupFilter(new org.apache.wicket.feedback.DefaultCleanupFeedbackMessageFilter());
    }

    @AfterEach void tearDown() {
        if (tester.getLastResponse() != null) {
            assertThat(tester.getLastResponseAsString()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        }
        tester.destroy();
    }

    private String html() { return tester.getLastResponseAsString(); }

    @Test
    void aFieldCanPutItsLabelBesideTheControlAlwaysOrWhenItsGroupIsWide() {
        SwTextField<String> name = new SwTextField<>("name", "Display name", Model.of("Ada"));
        tester.startComponentInPage(name);
        assertThat(TagTester.createTagByAttribute(html(), "class", "sw-field").getAttribute("data-orientation")).isNull();
        assertThat(name.getOrientation()).isEqualTo(BaseSwField.Orientation.VERTICAL);

        tester.startComponentInPage(name.setOrientation(BaseSwField.Orientation.RESPONSIVE));
        assertThat(TagTester.createTagByAttribute(html(), "class", "sw-field").getAttribute("data-orientation")).isEqualTo("responsive");
        tester.startComponentInPage(name.setOrientation(BaseSwField.Orientation.HORIZONTAL));
        assertThat(TagTester.createTagByAttribute(html(), "class", "sw-field").getAttribute("data-orientation")).isEqualTo("horizontal");
        tester.startComponentInPage(name.setOrientation(null));
        assertThat(TagTester.createTagByAttribute(html(), "class", "sw-field").getAttribute("data-orientation")).isNull();
        // The input keeps its short path
        tester.assertComponent("name:input", FormComponent.class);
    }

    @Test
    void aCheckboxOrSwitchKeepsItsOwnControlThenLabelLayout() {
        tester.startComponentInPage(new SwCheckBox("agree", "Agree", Model.of(false)).setOrientation(BaseSwField.Orientation.RESPONSIVE));
        assertThat(TagTester.createTagByAttribute(html(), "class", "sw-field").getAttribute("data-orientation")).isEqualTo("horizontal");
        tester.startComponentInPage(new SwSwitch("quiet", "Quiet hours", Model.of(true)).setOrientation(BaseSwField.Orientation.VERTICAL));
        assertThat(TagTester.createTagByAttribute(html(), "class", "sw-field").getAttribute("data-orientation")).isEqualTo("horizontal");
    }

    @Test
    void aFieldGroupIsAContainerOrABehaviorOnTheForm() {
        tester.startComponentInPage(new SwFieldGroup("group"), Markup.of("<div wicket:id='group'></div>"));
        assertThat(tester.getTagByWicketId("group").getAttribute("class")).isEqualTo("sw-field-group");
        Form<Void> form = new Form<>("form");
        form.add(Sw.Behaviors.fieldGroup());
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form' class='settings'></form>"));
        assertThat(tester.getTagByWicketId("form").getAttribute("class")).isEqualTo("settings sw-field-group");
    }

    /** A sign-up form with a form-level rule, an outer feedback panel and the summary. */
    public static class SignUpPage extends WebPage implements IMarkupResourceStreamProvider {
        final Form<Void> form = new Form<>("form");
        final SwFormErrors errors = new SwFormErrors("errors", form);
        final SwTextField<String> email = new SwTextField<>("email", "Email", Model.of(""));
        final SwPasswordField password = new SwPasswordField("password", "Password", Model.of(""));
        final SwPasswordField repeat = new SwPasswordField("repeat", "Repeat password", Model.of(""));

        public SignUpPage() {
            add(new SwFeedbackPanel("pageFeedback"));
            add(form);
            form.add(errors);
            email.setRequired(true);
            form.add(email, password, repeat);
            form.add(new AbstractFormValidator() {
                @Override public FormComponent<?>[] getDependentFormComponents() { return new FormComponent<?>[] {password.getInput(), repeat.getInput()}; }
                @Override public void validate(Form<?> f) {
                    if (!java.util.Objects.equals(password.getInput().getConvertedInput(), repeat.getInput().getConvertedInput())) {
                        f.error("The passwords <don't> match.");
                    }
                }
            });
            form.add(new Button("save"));
            form.add(new AjaxButton("ajaxSave") {
                @Override protected void onError(AjaxRequestTarget target) { }
            });
        }

        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><head></head><body><div wicket:id='pageFeedback'></div><form wicket:id='form'>"
                    + "<div wicket:id='errors'></div><div wicket:id='email'></div><div wicket:id='password'></div>"
                    + "<div wicket:id='repeat'></div><button wicket:id='save'>Save</button><button wicket:id='ajaxSave'>Save</button>"
                    + "</form></body></html>");
        }
    }

    private void submitWithErrors(String button) {
        FormTester form = tester.newFormTester("form");
        form.setValue("password:input", "secret1");
        form.setValue("repeat:input", "secret2");
        form.submit(button);
    }

    private TagTester summary() { return TagTester.createTagByAttribute(html(), "class", "sw-alert sw-form-errors"); }

    @Test
    void theSummaryShowsNothingWithoutErrorsAndTheFormsOwnErrorsByDefault() {
        SignUpPage page = tester.startPage(SignUpPage.class);
        assertThat(summary()).isNull();
        submitWithErrors("save");
        // The required email is shown inline by its field; the form-level rule only here, escaped, with the title
        assertThat(summary()).isNotNull();
        assertThat(summary().getAttribute("data-variant")).isEqualTo("destructive");
        assertThat(summary().getAttribute("tabindex")).isEqualTo("-1");
        assertThat(summary().getAttribute("role")).isNull();
        assertThat(html()).contains("There is a problem", "The passwords &lt;don&#039;t&gt; match.");
        assertThat(TagTester.createTagsByAttribute(html(), "class", "sw-form-errors-item", false)).hasSize(1);
        // The outer panel leaves the form's messages to the summary
        assertThat(html().split("The passwords", -1)).hasSize(2);
        assertThat(page.errors.getShow()).isEqualTo(SwFormErrors.Show.GLOBAL);
    }

    @Test
    void showingAllListsTheFormsErrorsFirstAndLinksFieldErrorsToTheirControls() {
        SignUpPage page = tester.startPage(SignUpPage.class);
        page.errors.setShow(SwFormErrors.Show.ALL);
        submitWithErrors("save");
        List<TagTester> items = TagTester.createTagsByAttribute(html(), "class", "sw-form-errors-item", false);
        assertThat(items).hasSize(2);
        assertThat(items.get(0).getValue()).contains("The passwords");
        TagTester link = items.get(1).getChild("a");
        assertThat(link).isNotNull();
        assertThat(link.getAttribute("href")).isEqualTo("#" + page.email.getInput().getMarkupId());
        assertThat(link.getValue()).contains("Email");
        // The field still shows its own error too
        assertThat(tester.getTagByWicketId("error").getValue()).contains("Email");
    }

    @Test
    void theTitleCanBeAHeadingOrTextOfYourOwnAndIsTranslated() {
        SignUpPage page = tester.startPage(SignUpPage.class);
        page.errors.setHeadingLevel(2).setTitle("Check <these> details");
        submitWithErrors("save");
        TagTester title = TagTester.createTagByAttribute(html(), "class", "sw-alert-title");
        assertThat(title.getAttribute("role")).isEqualTo("heading");
        assertThat(title.getAttribute("aria-level")).isEqualTo("2");
        assertThat(title.getValue()).isEqualTo("Check &lt;these&gt; details");
        assertThatIllegalArgumentException().isThrownBy(() -> page.errors.setHeadingLevel(7));

        page.errors.setHeadingLevel(null).setTitle((String) null);
        tester.getSession().setLocale(Locale.ITALIAN);
        submitWithErrors("save");
        TagTester italian = TagTester.createTagByAttribute(html(), "class", "sw-alert-title");
        assertThat(italian.getAttribute("role")).isNull();
        assertThat(italian.getValue()).isEqualTo("C&#039;è un problema");
        tester.getSession().setLocale(Locale.JAPANESE);
        submitWithErrors("save");
        assertThat(html()).contains("問題があります");
    }

    @Test
    void autofocusMarksTheSummaryInAFullRenderAndFocusesItAfterAnAjaxSubmit() {
        SignUpPage page = tester.startPage(SignUpPage.class);
        page.errors.setAutofocus(true);
        submitWithErrors("save");
        assertThat(summary().getAttribute("autofocus")).isEqualTo("autofocus");

        tester.startPage(page);
        FormTester form = tester.newFormTester("form");
        form.setValue("password:input", "secret1");
        form.setValue("repeat:input", "secret2");
        tester.executeAjaxEvent("form:ajaxSave", "click");
        String ajax = tester.getLastResponseAsString();
        assertThat(ajax).contains("Wicket.Focus.setFocusOnId('" + page.errors.get("feedbackul").getMarkupId() + "')");
    }

    @Test
    void withNothingToShowAutofocusGoesToTheFirstControlWithAnError() {
        SignUpPage page = tester.startPage(SignUpPage.class);
        page.errors.setAutofocus(true);
        // Only the required email fails, and its field shows that error itself
        FormTester form = tester.newFormTester("form");
        form.setValue("password:input", "same");
        form.setValue("repeat:input", "same");
        form.submit("save");
        assertThat(summary()).isNull();
        assertThat(html()).contains("document.getElementById(\"" + page.email.getInput().getMarkupId() + "\")");

        tester.startPage(page);
        form = tester.newFormTester("form");
        form.setValue("password:input", "same");
        form.setValue("repeat:input", "same");
        tester.executeAjaxEvent("form:ajaxSave", "click");
        assertThat(tester.getLastResponseAsString()).contains("Wicket.Focus.setFocusOnId('" + page.email.getInput().getMarkupId() + "')");
    }

    @Test
    void theSummaryJoinsAjaxResponsesWhileItHasErrorsAndClearsWhenTheyAreFixed() {
        tester.startPage(SignUpPage.class);
        FormTester form = tester.newFormTester("form");
        form.setValue("password:input", "secret1");
        form.setValue("repeat:input", "secret2");
        tester.executeAjaxEvent("form:ajaxSave", "click");
        assertThat(tester.getLastResponseAsString()).contains("The passwords");

        form = tester.newFormTester("form");
        form.setValue("email:input", "ada@example.com");
        form.setValue("password:input", "same");
        form.setValue("repeat:input", "same");
        tester.executeAjaxEvent("form:ajaxSave", "click");
        String ajax = tester.getLastResponseAsString();
        assertThat(ajax).doesNotContain("The passwords");
        // Still part of the response, so the old messages disappear from the page
        assertThat(ajax).contains("id=\"" + tester.getComponentFromLastRenderedPage("form:errors").getMarkupId() + "\"");
    }

    @Test
    void theSummarySerializesWithItsPage() throws Exception {
        tester.startPage(SignUpPage.class);
        // The page store serializes every page: the summary's message order must survive it
        try (var out = new java.io.ObjectOutputStream(new java.io.ByteArrayOutputStream())) {
            out.writeObject(tester.getLastRenderedPage());
        }
    }
}

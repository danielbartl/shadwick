package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.components.form.SwTextField;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
    }

    @AfterEach
    void tearDown() { tester.destroy(); }

    /** A form with a Shadwick field, a SwFeedbackPanel, and Ajax actions that report messages. */
    public static class FeedbackPage extends WebPage implements IMarkupResourceStreamProvider {

        public FeedbackPage(boolean toasts) {
            if (toasts) {
                add(Sw.Behaviors.feedbackToasts());
            }
            add(new SwFeedbackPanel("feedback").setVisible(!toasts));

            Form<Void> form = new Form<>("form");
            add(form);
            form.add(new SwTextField<String>("name", "Name", Model.of("")).setRequired(true));
            form.add(new AjaxButton("submit") {
                @Override
                protected void onError(AjaxRequestTarget target) {
                    form.error("Please fix the errors.");
                }
            });

            // Report messages without adding anything to the AjaxRequestTarget
            add(new AjaxLink<Void>("success") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    success("Saved.");
                }
            });
            add(new AjaxLink<Void>("nothing") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                }
            });
            add(new AjaxLink<Void>("several") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    fatal("Fatal.");
                    error("Failed.");
                    warn("Careful.");
                    info("Noted.");
                }
            });
        }

        @Override
        public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><body>"
                    + "<div wicket:id='feedback'></div>"
                    + "<form wicket:id='form'><div wicket:id='name'></div><button wicket:id='submit'>Go</button></form>"
                    + "<a wicket:id='success'>success</a><a wicket:id='nothing'>nothing</a><a wicket:id='several'>several</a>"
                    + "</body></html>");
        }
    }

    @Test
    void feedbackPanelRendersEachMessageAsAnAlertWithTheLevelsVariant() {
        FeedbackPage page = new FeedbackPage(false);
        page.info("Info message");
        page.success("Success message");
        page.warn("Warning message");
        page.error("Error message");
        tester.startPage(page);

        List<TagTester> alerts = TagTester.createTagsByAttribute(tester.getLastResponseAsString(), "role", "alert", false);
        assertThat(alerts).extracting(TagTester::getValue)
                .containsExactly("<span wicket:id=\"message\" class=\"sw-alert-description\">Info message</span>",
                        "<span wicket:id=\"message\" class=\"sw-alert-description\">Success message</span>",
                        "<span wicket:id=\"message\" class=\"sw-alert-description\">Warning message</span>",
                        "<span wicket:id=\"message\" class=\"sw-alert-description\">Error message</span>");
        assertThat(alerts).extracting(tag -> tag.getAttribute("data-variant"))
                .containsExactly(null, null, null, "destructive");
    }

    @Test
    void feedbackPanelLeavesOutErrorsTheFieldsShowInline() {
        tester.startPage(new FeedbackPage(false));

        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("name:input", "");
        tester.executeAjaxEvent("form:submit", "click");

        tester.assertComponentOnAjaxResponse("feedback");
        String response = tester.getLastResponseAsString();
        assertThat(response).contains("Please fix the errors.");
        // The field shows the required error itself, so it appears once
        assertThat(response).contains("Shadwick.fieldFeedback(");
        assertThat(response.split("is required", -1)).hasSize(2);
    }

    @Test
    void feedbackPanelAddsItselfToAjaxResponsesWhileItHasMessagesToShowOrClear() {
        tester.startPage(new FeedbackPage(false));

        tester.clickLink("success");
        tester.assertComponentOnAjaxResponse("feedback");
        assertThat(tester.getLastResponseAsString()).contains("Saved.");

        // The next request has no messages, but the panel must clear the old one.
        // WicketTester keeps rendered messages so tests can assert them; drop them as a real request would
        tester.cleanupFeedbackMessages();
        tester.clickLink("nothing");
        tester.assertComponentOnAjaxResponse("feedback");
        assertThat(tester.getLastResponseAsString()).doesNotContain("Saved.");

        // Nothing shown and nothing to show: the panel stays out of the response
        tester.cleanupFeedbackMessages();
        tester.clickLink("nothing");
        assertThat(tester.getLastResponseAsString()).doesNotContain("id=\"feedback");
    }

    @Test
    void feedbackToastsShowMessagesOnAFullPageRender() {
        FeedbackPage page = new FeedbackPage(true);
        page.info("Info message");
        page.error("Error message");
        tester.startPage(page);

        assertThat(tester.getLastResponseAsString())
                .contains("Shadwick.toast(\"Info message\",\"info\",\"Dismiss notification\",\"Notifications (Alt+T)\")")
                .contains("Shadwick.toast(\"Error message\",\"error\",\"Dismiss notification\",\"Notifications (Alt+T)\")");
    }

    @Test
    void feedbackToastsShowTheMostSevereMessageLastInSeverityOrderOfTheRequest() {
        FeedbackPage page = new FeedbackPage(true);
        page.error("First error");
        page.success("Saved.");
        page.info("Info message");
        page.error("Second error");
        tester.startPage(page);

        assertThat(toastMessages()).containsExactly("Info message", "Saved.", "First error", "Second error");
    }

    @Test
    void feedbackToastsShowAnAjaxRequestsMessagesByLevelTheMostSevereLast() {
        tester.startPage(new FeedbackPage(true));

        tester.clickLink("several");
        assertThat(toastMessages()).containsExactly("Noted.", "Careful.", "Failed.", "Fatal.");
    }

    private List<String> toastMessages() {
        return Pattern.compile("Shadwick\\.toast\\(\"([^\"]*)\"").matcher(tester.getLastResponseAsString())
                .results().map(match -> match.group(1)).toList();
    }

    @Test
    void feedbackToastsShowSessionMessages() {
        tester.getSession().success("Welcome back.");
        tester.startPage(new FeedbackPage(true));

        assertThat(tester.getLastResponseAsString()).contains("Shadwick.toast(\"Welcome back.\",\"success\",\"Dismiss notification\",\"Notifications (Alt+T)\")");
    }

    @Test
    void multipleToastBehaviorsConsumeAMessageOnlyOnceAndQuoteScriptDelimiters() {
        FeedbackPage page = new FeedbackPage(true);
        page.add(Sw.Behaviors.feedbackToasts());
        page.info("</script><script>alert('unsafe')</script>");
        tester.startPage(page);

        assertThat(tester.getLastResponseAsString()).containsOnlyOnce("Shadwick.toast(")
                .contains("\\u003cscript>")
                .doesNotContain("<script>alert('unsafe')</script>");
    }

    @Test
    void feedbackToastsShowAjaxMessagesOnceWithoutAddingAnythingToTheTarget() {
        tester.startPage(new FeedbackPage(true));

        tester.clickLink("success");
        String response = tester.getLastResponseAsString();
        assertThat(response).containsOnlyOnce("Shadwick.toast(\"Saved.\"");
        assertThat(response).contains(",\"success\",");

        tester.clickLink("nothing");
        assertThat(tester.getLastResponseAsString()).doesNotContain("Shadwick.toast");
    }

    @Test
    void feedbackToastsLeaveOutErrorsTheFieldsShowInline() {
        tester.startPage(new FeedbackPage(true));

        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("name:input", "");
        tester.executeAjaxEvent("form:submit", "click");

        String response = tester.getLastResponseAsString();
        assertThat(response).contains("Shadwick.toast(\"Please fix the errors.\",\"error\",\"Dismiss notification\",\"Notifications (Alt+T)\")");
        assertThat(response).doesNotContain("Shadwick.toast(\"'Name' is required.\"");
    }

    /** A form with a panel for its own messages, beside a page-wide panel or toasts. */
    public static class FencePage extends WebPage implements IMarkupResourceStreamProvider {

        public final Form<Void> form;

        public FencePage(boolean toasts) {
            if (toasts) {
                add(Sw.Behaviors.feedbackToasts());
            }
            add(new SwFeedbackPanel("feedback").setVisible(!toasts));

            form = new Form<>("form") {
                @Override
                protected void onError() {
                    error("Please fix the errors.");
                }
            };
            add(form);
            form.add(new SwFeedbackPanel("formFeedback", form));
            form.add(new SwTextField<String>("name", "Name", Model.of("")).setRequired(true));
            form.add(new AjaxButton("ajax") {
            });
            form.add(new org.apache.wicket.markup.html.form.Button("plain"));
            add(new AjaxLink<Void>("outside") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    success("Saved.");
                }
            });
        }

        @Override
        public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><body>"
                    + "<div wicket:id='feedback'></div>"
                    + "<form wicket:id='form'><div wicket:id='formFeedback'></div><div wicket:id='name'></div>"
                    + "<button wicket:id='ajax'>Ajax</button><button wicket:id='plain'>Plain</button></form>"
                    + "<a wicket:id='outside'>outside</a>"
                    + "</body></html>");
        }
    }

    private void submitEmptyName(String button) {
        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("name:input", "");
        if (button.equals("ajax")) {
            tester.executeAjaxEvent("form:ajax", "click");
        } else {
            formTester.submit(button);
        }
    }

    private int count(String text) {
        return tester.getLastResponseAsString().split(Pattern.quote(text), -1).length - 1;
    }

    @Test
    void aFencedPanelShowsItsFormsMessagesAndThePagePanelLeavesThemOut() {
        for (String button : List.of("ajax", "plain")) {
            tester.startPage(new FencePage(false));
            submitEmptyName(button);

            assertThat(count("Please fix the errors.")).as(button).isEqualTo(1);
            if (button.equals("ajax")) {
                tester.assertComponentOnAjaxResponse("form:formFeedback");
            } else {
                assertThat(tester.getTagByWicketId("formFeedback").getValue()).contains("Please fix the errors.");
            }
        }
    }

    @Test
    void feedbackToastsLeaveOutTheMessagesOfAFencedPanel() {
        for (String button : List.of("ajax", "plain")) {
            tester.startPage(new FencePage(true));
            submitEmptyName(button);

            assertThat(count("Please fix the errors.")).as(button).isEqualTo(1);
            assertThat(toastMessages()).as(button).isEmpty();
        }
    }

    @Test
    void messagesOutsideTheFenceGoToThePageWideFeedback() {
        tester.startPage(new FencePage(true));

        tester.clickLink("outside");
        assertThat(toastMessages()).containsExactly("Saved.");
    }

    @Test
    void removingAFencedPanelLiftsItsFence() {
        FencePage page = new FencePage(true);
        tester.startPage(page);
        assertThat(SwFeedbackPanel.isFence(page.form)).isTrue();

        page.form.get("formFeedback").remove();
        assertThat(SwFeedbackPanel.isFence(page.form)).isFalse();
    }

    /** The whole page is the fence of its feedback panel, and has the toasts too. */
    public static class PageFencePage extends WebPage implements IMarkupResourceStreamProvider {

        public PageFencePage() {
            add(Sw.Behaviors.feedbackToasts());
            add(new SwFeedbackPanel("feedback", this));
            add(new AjaxLink<Void>("save") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    success("Saved.");
                    getSession().info("Welcome back.");
                }
            });
        }

        @Override
        public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><body><div wicket:id='feedback'></div><a wicket:id='save'>save</a></body></html>");
        }
    }

    @Test
    void feedbackToastsOnAFenceShowOnlyTheSessionsMessages() {
        tester.startPage(new PageFencePage());

        tester.clickLink("save");

        tester.assertComponentOnAjaxResponse("feedback");
        assertThat(count("Saved.")).isEqualTo(1);
        assertThat(toastMessages()).containsExactly("Welcome back.");
    }

}

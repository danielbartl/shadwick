package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SwConfirmDialogTest {
    private WicketTester tester;
    @BeforeEach void setUp() { tester = new WicketTester(); }
    @AfterEach void tearDown() { tester.destroy(); }

    public static class ConfirmPage extends WebPage implements IMarkupResourceStreamProvider {
        final List<String> actions = new ArrayList<>();
        int closes;
        final SwConfirmDialog confirm = new SwConfirmDialog("confirm") {
            @Override protected void onClose(AjaxRequestTarget target) { super.onClose(target); closes++; }
        };
        public ConfirmPage() {
            add(confirm);
            add(SwButton.onClick("delete", "Delete", target -> confirm.ask(target,
                    "Delete <invoice>?", "This <cannot> be undone.", t -> actions.add("deleted"))));
            add(SwButton.onClick("archive", "Archive", target -> confirm.setConfirmLabel("Archive")
                    .setConfirmVariant(ButtonBehavior.Variant.DEFAULT)
                    .ask(target, "Archive invoice?", (String) null, t -> actions.add("archived"))));
            add(SwButton.onClick("chain", "Delete all", target -> confirm.ask(target, "Delete all?", null,
                    t -> confirm.ask(t, "Really delete all?", null, next -> actions.add("all")))));
            add(SwButton.onClick("close", "Close", confirm::close));
            add(SwButton.onClick("refresh", "Refresh", target -> target.add(confirm)));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><body><div wicket:id='confirm'></div>"
                    + "<button wicket:id='delete'></button><button wicket:id='archive'></button>"
                    + "<button wicket:id='chain'></button><button wicket:id='close'></button>"
                    + "<button wicket:id='refresh'></button></body></html>");
        }
    }

    private String html() { return tester.getLastResponseAsString(); }
    private void confirm() { tester.executeAjaxEvent("confirm:dialog:form:confirm", "click"); }

    @Test void askRendersAnEscapedNamedAlertDialogWithCancelAsTheSafeInitialFocus() {
        tester.startPage(ConfirmPage.class);
        tester.clickLink("delete");
        TagTester dialog = TagTester.createTagByName(html(), "dialog");
        assertThat(dialog.getAttribute("class")).isEqualTo("sw-alert-dialog");
        assertThat(dialog.getAttribute("role")).isEqualTo("alertdialog");
        assertThat(dialog.getAttribute("closedby")).isEqualTo("closerequest");
        assertThat(dialog.getAttribute("aria-labelledby")).isEqualTo(tester.getTagByWicketId("title").getAttribute("id"));
        assertThat(dialog.getAttribute("aria-describedby")).isEqualTo(tester.getTagByWicketId("description").getAttribute("id"));
        assertThat(tester.getTagByWicketId("cancel").hasAttribute("autofocus")).isTrue();
        assertThat(TagTester.createTagByAttribute(html(), "type", "submit").getAttribute("data-variant"))
                .isEqualTo("destructive");
        tester.assertInvisible("confirm:dialog:form:close");
        assertThat(html()).contains("Delete &lt;invoice&gt;?", "This &lt;cannot&gt; be undone.", "Shadwick.showDialog(")
                .doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
    }

    @Test void confirmingConsumesTheActionBeforeClosingAndAReplayDoesNothing() {
        ConfirmPage page = tester.startPage(ConfirmPage.class);
        tester.clickLink("delete");
        assertThat(page.actions).isEmpty();
        confirm();
        assertThat(page.actions).containsExactly("deleted");
        assertThat(page.confirm.isOpen()).isFalse();
        assertThat(page.closes).isEqualTo(1);
        assertThat(html()).contains(".close()");
        confirm();
        assertThat(page.actions).containsExactly("deleted");
    }

    @Test void cancellingForgetsTheActionEvenIfAConfirmIsSentAfterwards() {
        ConfirmPage page = tester.startPage(ConfirmPage.class);
        tester.clickLink("delete");
        tester.executeAjaxEvent("confirm:dialog", "close");
        assertThat(page.confirm.isOpen()).isFalse();
        confirm();
        assertThat(page.actions).isEmpty();
    }

    @Test void serverCloseAlsoForgetsTheAction() {
        ConfirmPage page = tester.startPage(ConfirmPage.class);
        tester.clickLink("delete");
        tester.clickLink("close");
        confirm();
        assertThat(page.actions).isEmpty();
    }

    @Test void aNewQuestionReplacesTheActionAndRemovesAnAbsentDescription() {
        ConfirmPage page = tester.startPage(ConfirmPage.class);
        tester.clickLink("delete");
        tester.clickLink("archive");
        tester.assertInvisible("confirm:dialog:form:description");
        assertThat(TagTester.createTagByName(html(), "dialog").getAttribute("aria-describedby")).isNull();
        tester.assertLabel("confirm:dialog:form:confirm:confirmLabel", "Archive");
        confirm();
        assertThat(page.actions).containsExactly("archived");
    }

    @Test void anAnswerCanAskAnotherQuestionWithoutClosingOrRunningItsAction() {
        ConfirmPage page = tester.startPage(ConfirmPage.class);
        tester.clickLink("chain");
        confirm();
        assertThat(page.confirm.isOpen()).isTrue();
        assertThat(page.closes).isZero();
        assertThat(page.actions).isEmpty();
        tester.assertLabel("confirm:dialog:form:title", "Really delete all?");
        assertThat(html()).contains("Shadwick.showDialog(").doesNotContain(".close()");
        confirm();
        assertThat(page.actions).containsExactly("all");
        assertThat(page.confirm.isOpen()).isFalse();
    }

    @Test void confirmRequestsUseAnActiveChannelInsteadOfQueuingDoubleClicks() {
        tester.startPage(ConfirmPage.class);
        assertThat(html()).containsPattern("sw-confirm-[^\"]*\\|a");
    }

    @Test void refreshingAnOpenQuestionKeepsItOpenWithoutLosingThePendingAction() {
        ConfirmPage page = tester.startPage(ConfirmPage.class);
        tester.clickLink("delete");
        tester.clickLink("refresh");
        assertThat(html()).contains("Shadwick.showDialog(", "data-sw-instant");
        confirm();
        assertThat(page.actions).containsExactly("deleted");
    }

    @Test void questionModelsDetachAndCallbackMustBePresent() {
        ConfirmPage page = tester.startPage(ConfirmPage.class);
        int[] detached = {0, 0};
        IModel<String> title = new Model<>("Reactive question") {
            @Override public void detach() { detached[1]++; }
        };
        IModel<String> description = new Model<>("Reactive description") {
            @Override public void detach() { detached[0]++; }
        };
        page.addOrReplace(SwButton.onClick("delete", "Delete", target ->
                page.confirm.ask(target, title, description, t -> page.actions.add("reactive"))));
        tester.startPage(page);
        tester.clickLink("delete");
        assertThat(detached[0]).isPositive();
        assertThat(detached[1]).isPositive();
        confirm();
        assertThat(page.actions).containsExactly("reactive");
        assertThatNullPointerException().isThrownBy(() -> page.confirm.ask(null, "Question", null, null));
    }
}

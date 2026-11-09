package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.ToastBehavior;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwDialog;
import dev.jbaby.shadwick.components.SwSheet;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.time.Duration;
import java.util.Locale;

/** General notifications without Wicket feedback messages, plus full-render and modal examples. */
public class ToastPage extends BasePage {

    private final boolean welcome;

    public ToastPage(PageParameters parameters) {
        super(parameters);
        welcome = parameters.get("welcome").toBoolean(false);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(Sw.Behaviors.toasts());
        addToastActions();
        addNavigationAndOverlays();
        Label status = new Label("status", "No feedback messages were created.");
        add(status);
    }

    private void addToastActions() {
        for (ToastBehavior.Variant variant : ToastBehavior.Variant.values()) {
            String name = variant.name().toLowerCase(Locale.ROOT);
            add(
                    SwButton.onClick(
                            name,
                            "Show " + name + " toast",
                            target ->
                                    Sw.toast(
                                            target,
                                            "Your workspace is ready for the next step.",
                                            variant,
                                            title(variant))));
        }
        add(
                SwButton.onClick(
                        "persistent",
                        "Keep until dismissed",
                        target ->
                                Sw.toast(
                                        target,
                                        "This message waits for you to dismiss it.",
                                        ToastBehavior.Variant.INFO,
                                        "Take your time",
                                        Duration.ZERO)));
        add(
                SwButton.onClick(
                        "brief",
                        "Show brief toast",
                        target ->
                                Sw.toast(
                                        target,
                                        "A short-lived preview; hover or focus to pause.",
                                        ToastBehavior.Variant.SUCCESS,
                                        "Copied",
                                        Duration.ofSeconds(2))));
        add(
                SwButton.onClick(
                        "literal",
                        "Show literal toast",
                        target ->
                                Sw.toast(
                                        target,
                                        "<strong>Zoë & 李</strong> stays plain text.",
                                        ToastBehavior.Variant.DEFAULT,
                                        "<b>Plain title</b>",
                                        Duration.ZERO)));
        add(
                SwButton.onClick(
                        "several",
                        "Show three toasts",
                        target -> {
                            Sw.toast(
                                    target,
                                    "First message.",
                                    ToastBehavior.Variant.DEFAULT,
                                    "One",
                                    Duration.ZERO);
                            Sw.toast(
                                    target,
                                    "Second message.",
                                    ToastBehavior.Variant.WARNING,
                                    "Two",
                                    Duration.ZERO);
                            Sw.toast(
                                    target,
                                    "Third message.",
                                    ToastBehavior.Variant.ERROR,
                                    "Three",
                                    Duration.ZERO);
                        }));
        add(
                SwButton.onClick(
                        "replace",
                        "Replace this button",
                        target -> {
                            Sw.toast(
                                    target,
                                    "The Ajax update keeps this notification visible.",
                                    ToastBehavior.Variant.SUCCESS,
                                    "Updated",
                                    Duration.ZERO);
                            target.add(get("replace"));
                        }));
    }

    private void addNavigationAndOverlays() {
        add(
                new BookmarkablePageLink<>(
                        "fullPage", ToastPage.class, new PageParameters().add("welcome", true)));
        add(
                new SwDialog("dialog", "Notifications in a dialog") {

                    @Override
                    protected void onConfirm(AjaxRequestTarget target) {
                        Sw.toast(
                                target,
                                "The dialog has closed and this toast stays usable.",
                                ToastBehavior.Variant.SUCCESS,
                                "Done",
                                Duration.ZERO);
                    }
                }.setTrigger("Open toast dialog")
                        .setConfirmLabel("Notify and close")
                        .setContent(
                                new ToastActionPanel(
                                        SwDialog.CONTENT_ID,
                                        "Notify inside dialog",
                                        target ->
                                                Sw.toast(
                                                        target,
                                                        "You can dismiss this without closing the"
                                                            + " dialog.",
                                                        ToastBehavior.Variant.INFO,
                                                        "Still here",
                                                        Duration.ZERO))));
        add(
                new SwSheet("sheet", "Notifications in a sheet")
                        .setTrigger("Open toast sheet")
                        .setFooterVisible(false)
                        .setContent(
                                new ToastActionPanel(
                                        SwSheet.CONTENT_ID,
                                        "Notify inside sheet",
                                        target ->
                                                Sw.toast(
                                                        target,
                                                        "The sheet remains open when this toast is"
                                                            + " dismissed.",
                                                        ToastBehavior.Variant.WARNING,
                                                        "Review",
                                                        Duration.ZERO))));
    }

    private static String title(ToastBehavior.Variant variant) {
        return switch (variant) {
            case DEFAULT -> "Workspace updated";
            case SUCCESS -> "Changes saved";
            case INFO -> "For your information";
            case WARNING -> "Review your settings";
            case ERROR -> "Could not save";
        };
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        if (welcome && getRequestCycle().find(AjaxRequestTarget.class).isEmpty())
            response.render(
                    OnDomReadyHeaderItem.forScript(
                            ToastBehavior.script(
                                    this,
                                    "A notification from a full page render.",
                                    ToastBehavior.Variant.SUCCESS,
                                    "Welcome back",
                                    Duration.ZERO)));
    }

    @Override
    protected String getTitle() {
        return "Toasts";
    }
}

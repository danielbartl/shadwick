package dev.jbaby.shadwick.components;

import com.github.openjson.JSONObject;
import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.behaviors.NestedFormButtonBehavior;
import dev.jbaby.shadwick.components.form.BaseSwField;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackCollector;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.WicketTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * A modal dialog using the native &lt;dialog&gt; element, in shadcn's anatomy: a header with a title and an optional
 * description, the content, and a footer with Cancel and Confirm. It works like Wicket's own {@code ModalDialog}:
 * <ul>
 * <li>Set its content with {@link #setContent}, using the id {@link #CONTENT_ID}, open it from any Ajax handler with
 * {@link #open}, which re-renders it first, and close it with {@link #close}. {@link #open(Component, AjaxRequestTarget)}
 * sets content that is removed again when the dialog closes. With a trigger label it also renders a button that opens
 * it in the browser, without waiting for the server (an invoker command, {@code commandfor}/{@code command}).</li>
 * <li>The server knows whether it is open ({@link #isOpen}), so re-rendering it - with {@code target.add(dialog)}, or a
 * container around it - keeps it open.</li>
 * <li>The content sits inside the dialog's own {@link #getForm() form}, so form fields in it are validated when Confirm
 * is clicked. On a validation error the dialog stays open and shows the errors; otherwise {@link #onConfirm} runs, and
 * the dialog closes unless an error was reported in it there. Shadwick fields show their errors in place, so the focus
 * stays where it is; an error reported on anything else re-renders the dialog's form.</li>
 * <li>Cancel, the &times; in the corner, the Escape key and a click on the backdrop close it in the browser, which
 * tells the server. Once the dialog is out of sight, its fields show the models again, without what was changed but
 * not confirmed. {@link #onClose} runs whenever it closes, however that happened.</li>
 * <li>It can sit inside a form of the page: submitting that form leaves the dialog's fields out, so they are neither
 * validated nor stored.</li>
 * <li>Give it a tag of its own, e.g. {@code <div wicket:id="dialog"></div>}, which takes no room in the layout, so
 * {@code target.add(dialog)} can re-render it. On a {@code <wicket:container>}, which Wicket leaves out in deployment
 * mode, open and close it only with {@link #open} and {@link #close}.</li>
 * </ul>
 *
 * <pre>
 * SwDialog dialog = new SwDialog("dialog", Model.of("Delete item")) {
 *     &#64;Override
 *     protected void onConfirm(AjaxRequestTarget target) { ... }
 * };
 * dialog.setDescription(Model.of("This action cannot be undone."));
 * add(dialog);
 * // elsewhere, e.g. in an SwButton.onClick: dialog.open(target);
 * </pre>
 *
 * The button labels come from the {@code SwDialog.cancel}, {@code SwDialog.confirm} and {@code SwDialog.close}
 * resource keys, so they can be translated or overridden in your {@code .properties} files, or set per dialog with
 * {@link #setCancelLabel} and {@link #setConfirmLabel}.
 */
public class SwDialog extends Panel {

    /** The component id the content passed to {@link #setContent(Component)} must use. */
    public static final String CONTENT_ID = "content";

    /** Marks a close the server asked for, so the browser doesn't report it back. */
    private static final String CLOSING = "data-sw-closing";

    /** How long the browser takes to close a dialog (the skins' transition), before telling the server. */
    private static final Duration CLOSE_TRANSITION = Duration.ofMillis(150);

    private final WebMarkupContainer dialog;
    private final Form<Void> form;
    private final Label titleLabel;
    private final Label descriptionLabel;
    private final ButtonBehavior triggerButton;
    private final WebMarkupContainer closeButton;
    private final WebMarkupContainer footer;
    private final WebMarkupContainer cancel;
    private final Label cancelLabel;
    private final Label confirmLabel;
    private final ButtonBehavior confirmButton;
    private final AjaxButton confirm;
    private IModel<String> description;
    private boolean open;
    /** Set by {@link #open}, so a dialog reopened in {@link #onConfirm} isn't closed after it. */
    private boolean reopened;
    private boolean removeContentOnClose;
    /** Set by {@link #open} on a closed dialog, which then fades in; one open already shows at once. */
    private boolean appearing;
    /** The browser still shows content removed on a close the server asked for, until the form renders again. */
    private boolean staleContent;

    /**
     * A dialog without a trigger button; open it with {@link #open}.
     */
    public SwDialog(String id, IModel<String> title) {
        this(id, null, title);
    }

    /**
     * A dialog without a trigger button; open it with {@link #open}.
     */
    public SwDialog(String id, String title) {
        this(id, Model.of(title));
    }

    /**
     * @param triggerLabel the label of a button that opens the dialog, or {@code null} for none
     */
    public SwDialog(String id, String triggerLabel, String title) {
        this(id, triggerLabel != null ? Model.of(triggerLabel) : null, Model.of(title));
    }

    /**
     * @param triggerLabel the label of a button that opens the dialog, or {@code null} for none
     */
    public SwDialog(String id, IModel<String> triggerLabel, IModel<String> title) {
        super(id);
        // Its own tag, so target.add(dialog) re-renders it; the stylesheet gives the tag no
        // box of its own, so the trigger lays out as if it stood there by itself
        setOutputMarkupId(true);
        add(AttributeAppender.append("class", "sw-dialog-root"));

        dialog = new WebMarkupContainer("dialog") {
            /** Shows the dialog again when it is rendered while open, by {@link #open} or a re-render around it. */
            @Override
            public void renderHead(IHeaderResponse response) {
                super.renderHead(response);
                if (open) {
                    response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
                    response.render(OnDomReadyHeaderItem.forScript("Shadwick.showDialog(" + JSONObject.quote(getMarkupId()) + ")"));
                }
            }

            @Override
            protected void onAfterRender() {
                super.onAfterRender();
                appearing = false;
            }
        };
        dialog.setOutputMarkupId(true);
        dialog.add(AttributeModifier.replace("closedby", "any"));
        // Rendered again while open, it shows at once rather than fading in again
        dialog.add(AttributeModifier.replace("data-sw-instant", (IModel<String>) () -> open && !appearing ? "" : null));
        dialog.add(new ClosedInBrowser());
        add(dialog);

        WebMarkupContainer trigger = new WebMarkupContainer("trigger");
        trigger.add(new Label("triggerLabel", triggerLabel));
        trigger.add(AttributeModifier.replace("commandfor", (IModel<String>) dialog::getMarkupId));
        triggerButton = new ButtonBehavior(ButtonBehavior.Variant.OUTLINE);
        trigger.add(triggerButton);
        // The browser opens the dialog at once; this tells the server it is open
        trigger.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                open = true;
                if (staleContent) {
                    target.add(form);
                }
            }
        });
        trigger.setVisible(triggerLabel != null);
        add(trigger);

        // The dialog's own form, re-rendered after a failed confirm.
        // Inside a page's form it is nested; submitting that form leaves it out, so the
        // fields of a closed dialog can't block the page's submit.
        form = new Form<Void>("form") {
            @Override
            protected boolean wantSubmitOnParentFormSubmit() {
                return false;
            }

            @Override
            protected void onAfterRender() {
                super.onAfterRender();
                staleContent = false;
            }
        };
        form.setOutputMarkupId(true);
        dialog.add(form);

        titleLabel = new Label("title", title);
        titleLabel.setOutputMarkupId(true);
        form.add(titleLabel);
        descriptionLabel = new Label("description", (IModel<String>) () -> description != null ? description.getObject() : null) {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(hasDescription());
            }
        };
        descriptionLabel.setOutputMarkupId(true);
        form.add(descriptionLabel);
        // The dialog is named by its title, described by its description, described by its description
        dialog.add(AttributeModifier.replace("aria-labelledby", (IModel<String>) () -> titleLabel.getMarkupId()));
        dialog.add(AttributeModifier.replace("aria-describedby", (IModel<String>) () -> hasDescription() ? descriptionLabel.getMarkupId() : null));
        form.add(new WebMarkupContainer(CONTENT_ID));

        // Transparent, so the buttons keep their paths ("form:confirm"); hidden with them
        footer = new TransparentWebMarkupContainer("footer");
        form.add(footer);

        cancel = new WebMarkupContainer("cancel");
        cancelLabel = new Label("cancelLabel", new StringResourceModel("SwDialog.cancel", this).setDefaultValue("Cancel"));
        cancel.add(cancelLabel);
        cancel.add(AttributeModifier.replace("commandfor", (IModel<String>) dialog::getMarkupId));
        cancel.add(new ButtonBehavior(ButtonBehavior.Variant.OUTLINE));
        form.add(cancel);

        confirm = new AjaxButton("confirm") {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                open = true; // it is, since its button was clicked
                reopened = false;
                onConfirm(target);
                if (hasErrorMessages()) {
                    showErrors(target);
                } else if (!reopened) {
                    close(target);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                open = true;
                showErrors(target);
            }
        };
        // Inside a page's form, so Enter in a page field can't click this hidden button
        confirm.add(new NestedFormButtonBehavior());
        confirmLabel = new Label("confirmLabel", new StringResourceModel("SwDialog.confirm", this).setDefaultValue("Confirm"));
        confirm.add(confirmLabel);
        confirmButton = new ButtonBehavior();
        confirm.add(confirmButton);
        form.add(confirm);
        // Enter in a field of the content confirms, like clicking the button
        form.setDefaultButton(confirm);

        // The × in the corner, closing it in the browser like Cancel. Last in the markup, so
        // the browser focuses the first field (or button) of the dialog on opening, not this.
        closeButton = new WebMarkupContainer("close");
        closeButton.add(AttributeModifier.replace("commandfor", (IModel<String>) dialog::getMarkupId));
        closeButton.add(AttributeModifier.replace("aria-label", new StringResourceModel("SwDialog.close", this).setDefaultValue("Close")));
        closeButton.add(new ButtonBehavior(ButtonBehavior.Variant.GHOST).setSize(ButtonBehavior.Size.ICON));
        form.add(closeButton);
    }

    /**
     * Called when Confirm is clicked and the content's fields are valid. The dialog closes afterwards unless an error
     * message was reported on it or a component in it, so e.g. {@code nameField.getInput().error("Already taken")}
     * keeps it open, or unless it was opened again here, e.g. with the next record's content.
     */
    protected void onConfirm(AjaxRequestTarget target) {
    }

    /**
     * Called once whenever the dialog closes: by Cancel, the &times;, the Escape key or a click on the backdrop in the
     * browser, after a successful confirm, or by {@link #close}. E.g. to forget the record it was editing.
     */
    protected void onClose(AjaxRequestTarget target) {
    }

    /**
     * Opens the dialog, re-rendering it first so it reflects the current models. Input left over from an earlier
     * failed confirm is discarded.
     */
    public SwDialog open(AjaxRequestTarget target) {
        form.clearInput();
        appearing = !open;
        open = true;
        reopened = true;
        // The panel; on a <wicket:container>, which deployment mode leaves out, the <dialog>
        // inside, which has an id of its own
        target.add(getMarkupTag() instanceof WicketTag ? dialog : this);
        return this;
    }

    /**
     * Opens the dialog with the given content, which is removed again when it closes, e.g. the fields for the record a
     * row's Edit button was clicked for.
     *
     * @param content the content; its id must be {@link #CONTENT_ID}
     */
    public SwDialog open(Component content, AjaxRequestTarget target) {
        setContent(content);
        removeContentOnClose = true;
        return open(target);
    }

    /**
     * Closes the dialog, if it is open, and runs {@link #onClose}. Opened again in the same request, e.g. by
     * {@code onClose}, it stays open.
     */
    public SwDialog close(AjaxRequestTarget target) {
        // Closed in the browser at the end of the request: the script runs after the
        // components are re-rendered, so it would also close a dialog opened again since
        target.addListener(new AjaxRequestTarget.IListener() {
            @Override
            public void onBeforeRespond(Map<String, Component> map, AjaxRequestTarget respondingTarget) {
                if (!open) {
                    respondingTarget.appendJavaScript("(function(d){if(d&&d.open){d.setAttribute('" + CLOSING + "','');d.close();}})"
                            + "(document.getElementById('" + dialog.getMarkupId() + "'))");
                }
            }
        });
        closed(target, false);
        return this;
    }

    /** Whether the dialog is open. */
    public boolean isOpen() {
        return open;
    }

    /** Notes that the dialog closed, removes content set to go with it, and runs onClose once. */
    private void closed(AjaxRequestTarget target, boolean inBrowser) {
        if (!open) {
            return;
        }
        open = false;
        if (removeContentOnClose) {
            removeContentOnClose = false;
            form.replace(new WebMarkupContainer(CONTENT_ID));
            // Not rendered yet, so the dialog doesn't close empty; opening it renders the form
            staleContent = true;
        }
        if (inBrowser) {
            // Out of sight by now (ClosedInBrowser waits): the fields show the models again,
            // without input that wasn't confirmed or the errors of a failed confirm
            form.clearInput();
            target.add(form);
        }
        onClose(target);
    }

    /**
     * Sets the dialog's content. Its id must be {@link #CONTENT_ID}. It can be replaced at any time, e.g. right before
     * {@link #open}.
     */
    public SwDialog setContent(Component content) {
        if (!CONTENT_ID.equals(content.getId())) {
            throw new IllegalArgumentException("The dialog's content must use the id SwDialog.CONTENT_ID (\""
                    + CONTENT_ID + "\"), but was \"" + content.getId() + "\"");
        }
        form.addOrReplace(content);
        removeContentOnClose = false;
        return this;
    }

    /** Whether the &times; in the corner closes the dialog, like Cancel; on by default, as in shadcn's. */
    public SwDialog setCloseButton(boolean shown) {
        closeButton.setVisible(shown);
        return this;
    }

    /**
     * Whether the footer with the Cancel and Confirm buttons shows; on by default. Without them, e.g. for a dialog that
     * only shows something, the &times;, the Escape key and a click on the backdrop close it, and Enter in a field
     * submits nothing.
     */
    public SwDialog setFooterVisible(boolean visible) {
        footer.setVisible(visible);
        cancel.setVisible(visible);
        confirm.setVisible(visible);
        return this;
    }

    /** The {@code <dialog>} element, e.g. for a subclass to style it. */
    protected final WebMarkupContainer getDialogElement() {
        return dialog;
    }

    /** The dialog's title, which names it for screen readers, e.g. to reuse one dialog for different records. */
    public SwDialog setTitle(IModel<String> title) {
        titleLabel.setDefaultModel(title);
        return this;
    }

    /** Supporting text below the title, which describes the dialog for screen readers. */
    public SwDialog setDescription(String description) {
        return setDescription(Model.of(description));
    }

    /** Supporting text below the title; a {@code null} or empty one shows none. */
    public SwDialog setDescription(IModel<String> description) {
        this.description = description;
        return this;
    }

    private boolean hasDescription() {
        return description != null && !Strings.isEmpty(description.getObject());
    }

    public SwDialog setCancelLabel(IModel<String> label) {
        cancelLabel.setDefaultModel(label);
        return this;
    }

    public SwDialog setConfirmLabel(IModel<String> label) {
        confirmLabel.setDefaultModel(label);
        return this;
    }

    /** E.g. {@code DESTRUCTIVE} for a destructive action. */
    public SwDialog setConfirmVariant(ButtonBehavior.Variant variant) {
        confirmButton.setVariant(variant);
        return this;
    }

    /** The variant of the button that opens the dialog; {@code OUTLINE} by default. */
    public SwDialog setTriggerVariant(ButtonBehavior.Variant variant) {
        triggerButton.setVariant(Objects.requireNonNull(variant));
        return this;
    }

    /** The dialog's own form, which wraps its header, content and buttons. */
    public Form<Void> getForm() {
        return form;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        Shadwick.renderResources(response);
        // Invoker commands where a browser lacks them, and Enter in a field does nothing without a footer
        response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
    }

    @Override
    protected void onDetach() {
        if (description != null) {
            description.detach();
        }
        super.onDetach();
    }

    private boolean hasErrorMessages() {
        return !new FeedbackCollector(this).collect(FeedbackMessage::isError).isEmpty();
    }

    /**
     * Shows the errors of a failed confirm. Shadwick fields show their own in place, keeping the focus where it is and
     * announcing the message; only an error reported elsewhere, e.g. on the dialog for a feedback panel in the content,
     * re-renders the form.
     */
    private void showErrors(AjaxRequestTarget target) {
        boolean elsewhere = new FeedbackCollector(this).collect(FeedbackMessage::isError).stream()
                .map(FeedbackMessage::getReporter)
                .anyMatch(reporter -> reporter == null || !(reporter instanceof BaseSwField<?, ?, ?>)
                        && reporter.findParent(BaseSwField.class) == null);
        if (elsewhere) {
            target.add(form);
        }
    }

    /**
     * Tells the server when the dialog closed in the browser: by Cancel, the &times;, the Escape key or a click on the
     * backdrop. Not for a close the server asked for, which it knows about, nor for a dialog element replaced by a
     * re-render. Once the dialog is out of sight, so re-rendering its form doesn't show while it fades out; not at all
     * if it was opened again by then.
     */
    private final class ClosedInBrowser extends AjaxEventBehavior {

        ClosedInBrowser() {
            super("close");
        }

        @Override
        protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
            super.updateAjaxAttributes(attributes);
            attributes.setThrottlingSettings(new ThrottlingSettings(CLOSE_TRANSITION));
            // The element the event fired on: looked up by id, it would be the new one
            attributes.getAjaxCallListeners().add(new AjaxCallListener().onPrecondition(
                    "var d=attrs.event&&attrs.event.target;if(!d||!d.isConnected){return false;}"
                            + "if(d.hasAttribute('" + CLOSING + "')){d.removeAttribute('" + CLOSING + "');return false;}"
                            + "return !d.open;"));
        }

        @Override
        protected void onEvent(AjaxRequestTarget target) {
            closed(target, true);
        }
    }
}

package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.util.SerializableConsumer;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Objects;

/**
 * An alert dialog asking permission for a server-side action, following Wicket Oat's ask API.
 * The action is consumed before it runs and forgotten on close. Cancel receives initial focus,
 * Escape cancels, and clicking the backdrop does not dismiss it. Confirmation defaults to DESTRUCTIVE.
 * Confirm requests use an ACTIVE Ajax channel: repeated clicks while an answer is pending are ignored,
 * including when that answer asks another question.
 *
 * <pre>{@code
 * SwConfirmDialog confirm = new SwConfirmDialog("confirm").setConfirmLabel("Delete");
 * add(confirm);
 * confirm.ask(target, "Delete invoice?", "This cannot be undone.", t -> service.delete(invoice));
 * }</pre>
 */
public class SwConfirmDialog extends SwDialog {
    private SerializableConsumer<AjaxRequestTarget> action;

    public SwConfirmDialog(String id) {
        super(id, new Model<String>());
        getDialogElement().add(AttributeModifier.replace("closedby", "closerequest"));
        getDialogElement().add(AttributeModifier.replace("role", "alertdialog"));
        getForm().get("cancel").add(AttributeModifier.replace("autofocus", "autofocus"));
        setContent(new WebMarkupContainer(CONTENT_ID).setVisible(false));
        setCloseButton(false);
        setConfirmVariant(ButtonBehavior.Variant.DESTRUCTIVE);
    }

    /** Opens a question, replacing any pending one. The callback runs only when confirmed. */
    public SwConfirmDialog ask(AjaxRequestTarget target, String title, String description,
                               SerializableConsumer<AjaxRequestTarget> onConfirm) {
        return ask(target, Model.of(title), Model.of(description), onConfirm);
    }

    /** Reactive title and description models are detached with the dialog. */
    public SwConfirmDialog ask(AjaxRequestTarget target, IModel<String> title, IModel<String> description,
                               SerializableConsumer<AjaxRequestTarget> onConfirm) {
        Objects.requireNonNull(onConfirm, "onConfirm");
        setTitle(title);
        setDescription(description);
        action = onConfirm;
        open(target);
        return this;
    }

    @Override
    protected void updateConfirmAjaxAttributes(AjaxRequestAttributes attributes) {
        super.updateConfirmAjaxAttributes(attributes);
        attributes.setChannel(new AjaxChannel("sw-confirm-" + getMarkupId(), AjaxChannel.Type.ACTIVE));
    }

    @Override
    protected void onConfirm(AjaxRequestTarget target) {
        SerializableConsumer<AjaxRequestTarget> pending = action;
        action = null;
        if (pending != null) pending.accept(target);
    }

    /** Subclasses overriding this must call super to discard the pending action. */
    @Override
    protected void onClose(AjaxRequestTarget target) {
        action = null;
        super.onClose(target);
    }

    @Override public SwConfirmDialog setContent(Component content) { super.setContent(content); return this; }
    @Override public SwConfirmDialog setTitle(String title) { super.setTitle(title); return this; }
    @Override public SwConfirmDialog setTitle(IModel<String> title) { super.setTitle(title); return this; }
    @Override public SwConfirmDialog setDescription(String description) { super.setDescription(description); return this; }
    @Override public SwConfirmDialog setDescription(IModel<String> description) { super.setDescription(description); return this; }
    @Override public SwConfirmDialog setCancelLabel(String label) { super.setCancelLabel(label); return this; }
    @Override public SwConfirmDialog setCancelLabel(IModel<String> label) { super.setCancelLabel(label); return this; }
    @Override public SwConfirmDialog setConfirmLabel(String label) { super.setConfirmLabel(label); return this; }
    @Override public SwConfirmDialog setConfirmLabel(IModel<String> label) { super.setConfirmLabel(label); return this; }
    @Override public SwConfirmDialog setConfirmVariant(ButtonBehavior.Variant variant) { super.setConfirmVariant(variant); return this; }
    @Override public SwConfirmDialog setCloseButton(boolean shown) { super.setCloseButton(shown); return this; }
    @Override public SwConfirmDialog setFooterVisible(boolean visible) { super.setFooterVisible(visible); return this; }
    @Override public SwConfirmDialog setTrigger(String label) { super.setTrigger(label); return this; }
    @Override public SwConfirmDialog setTrigger(IModel<String> label) { super.setTrigger(label); return this; }
    @Override public SwConfirmDialog setTriggerVariant(ButtonBehavior.Variant variant) { super.setTriggerVariant(variant); return this; }
    @Override public SwConfirmDialog open(AjaxRequestTarget target) { super.open(target); return this; }
    @Override public SwConfirmDialog open(Component content, AjaxRequestTarget target) { super.open(content, target); return this; }
    @Override public SwConfirmDialog close(AjaxRequestTarget target) { super.close(target); return this; }
}

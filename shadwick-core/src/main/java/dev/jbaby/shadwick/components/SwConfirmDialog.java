package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.util.SerializableConsumer;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.markup.ComponentTag;
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
 * {@link #setSize(Size) setSize(Size.SMALL)} makes a narrower dialog with a centred header and the two buttons side
 * by side in equal halves; they stack, the action on top, when a translated label does not fit its half.
 * {@link #setIcon(String)} shows a decorative icon above the title (beside it in a default-size dialog on a wide
 * screen), on a destructive background with {@link IconVariant#DESTRUCTIVE}.
 */
public class SwConfirmDialog extends SwDialog {

    /** The dialog's width: shadcn's default, or a narrow one for a short question. */
    public enum Size { DEFAULT, SMALL }

    /** The background behind {@link #setIcon(String) the icon}. */
    public enum IconVariant { DEFAULT, DESTRUCTIVE }

    private SerializableConsumer<AjaxRequestTarget> action;
    private Size size = Size.DEFAULT;
    private String icon;
    private IconVariant iconVariant = IconVariant.DEFAULT;
    private final WebMarkupContainer media;

    public SwConfirmDialog(String id) {
        super(id, new Model<String>());
        getDialogElement().add(AttributeModifier.replace("closedby", "closerequest"));
        getDialogElement().add(AttributeModifier.replace("role", "alertdialog"));
        getForm().get("cancel").add(AttributeModifier.replace("autofocus", "autofocus"));
        setContent(new WebMarkupContainer(CONTENT_ID).setVisible(false));
        setCloseButton(false);
        setConfirmVariant(ButtonBehavior.Variant.DESTRUCTIVE);
        getDialogElement().add(AttributeModifier.replace("data-size", (IModel<String>) () -> size == Size.SMALL ? "sm" : null));
        media = newMedia();
        getForm().add(media);
    }

    /** The header's icon container, hidden until {@link #setIcon(String)} names an icon. */
    private WebMarkupContainer newMedia() {
        WebMarkupContainer container = new WebMarkupContainer("media") {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(icon != null);
            }

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                if (iconVariant == IconVariant.DESTRUCTIVE) tag.put("data-variant", "destructive");
                else tag.remove("data-variant");
            }
        };
        container.add(SwIcon.ofNullable("icon", null));
        return container;
    }

    /** The dialog's width; {@link Size#DEFAULT} unless set. Null restores the default. */
    public SwConfirmDialog setSize(Size size) {
        this.size = size != null ? size : Size.DEFAULT;
        return this;
    }

    public Size getSize() {
        return size;
    }

    /**
     * A decorative icon above the title, by {@link SwIcon} name (e.g. {@code "trash-2"}); {@code null} removes it.
     * The title and description still carry the meaning: the icon is hidden from screen readers.
     */
    public SwConfirmDialog setIcon(String name) {
        media.addOrReplace(SwIcon.ofNullable("icon", name));
        this.icon = name;
        return this;
    }

    /** The background behind the icon; {@link IconVariant#DEFAULT} (muted) unless set. Null restores the default. */
    public SwConfirmDialog setIconVariant(IconVariant variant) {
        this.iconVariant = variant != null ? variant : IconVariant.DEFAULT;
        return this;
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

    @Override public SwConfirmDialog setBusyIndicator(boolean enabled) { super.setBusyIndicator(enabled); return this; }
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

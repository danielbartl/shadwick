package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.behaviors.AttachmentBehavior;
import dev.jbaby.shadwick.behaviors.AttachmentControlBehavior;
import org.apache.wicket.Component;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import java.util.Objects;

/**
 * A stored file or image, independent of upload fields. URLs are application-owned and must be authorized by the app.
 * Models are escaped, read once per request and detached. The optional actions slot accepts arbitrary Wicket markup.
 * The stretched trigger is last in tab order, behind actions. No live region or upload/progress transport is added.
 */
public class SwAttachment extends Panel {
    public static final String ACTIONS_ID = "actions";
    public static final String MEDIA_ID = "mediaContent";
    public static final String TRIGGER_ID = "trigger";
    private final AttachmentBehavior appearance = new AttachmentBehavior();
    private IModel<String> title, description, imageUrl, imageAlt;
    private boolean showContent = true, customMedia, hasActions, hasTrigger;
    private final WebMarkupContainer media, content, actions;
    private final SwIcon icon;

    public SwAttachment(String id, String title) { this(id, Model.of(title)); }
    public SwAttachment(String id, IModel<String> title) {
        super(id); add(appearance);
        this.title = OncePerRequestModel.of(Objects.requireNonNull(title));
        media = new WebMarkupContainer("media"); add(media);
        media.add(AttributeModifier.replace("role", (IModel<String>) () -> !showContent ? "img" : null));
        media.add(AttributeModifier.replace("aria-label", (IModel<String>) () -> !showContent ? mediaLabel() : null));
        media.add(AttributeModifier.replace("data-variant", (IModel<String>) () -> hasImage() && !customMedia ? "image" : null));
        WebMarkupContainer defaults = new WebMarkupContainer("defaultMedia") {
            @Override protected void onConfigure() { super.onConfigure(); setVisible(!customMedia); }
        }; defaults.setRenderBodyOnly(true); media.add(defaults);
        media.add(new WebMarkupContainer(MEDIA_ID).setVisible(false));
        icon = new SwIcon("icon", "file-text"); defaults.add(icon);
        var image = new WebMarkupContainer("image") {
            @Override protected void onConfigure() { super.onConfigure(); setVisible(hasImage()); }
        };
        image.add(AttributeModifier.replace("src", (IModel<String>) () -> value(imageUrl)));
        image.add(AttributeModifier.replace("alt", (IModel<String>) () -> Objects.requireNonNullElse(showContent ? value(imageAlt) : mediaLabel(), "")));
        image.add(AttributeModifier.replace("aria-hidden", (IModel<String>) () -> !showContent ? "true" : null));
        defaults.add(image);
        content = new WebMarkupContainer("content"); add(content);
        content.add(new Label("title", (IModel<String>) () -> value(this.title)));
        var detail = new Label("description", (IModel<String>) () -> value(description)) {
            @Override protected void onConfigure() { super.onConfigure(); setVisible(value(description) != null && !value(description).isBlank()); }
        }; content.add(detail);
        actions = new WebMarkupContainer("actionArea"); actions.add(new WebMarkupContainer(ACTIONS_ID).setVisible(false)); add(actions);
        add(new WebMarkupContainer(TRIGGER_ID).setVisible(false));
    }
    private static String value(IModel<String> model) { return model == null ? null : model.getObject(); }
    private String mediaLabel() { String alt = value(imageAlt); return alt != null ? alt : value(title); }
    private boolean hasImage() { String url = value(imageUrl); return url != null && !url.isBlank(); }
    public SwAttachment setTitle(String title) { return setTitle(Model.of(title)); }
    public SwAttachment setTitle(IModel<String> title) { this.title = OncePerRequestModel.of(Objects.requireNonNull(title)); return this; }
    public SwAttachment setDescription(String description) { return setDescription(Model.of(description)); }
    public SwAttachment setDescription(IModel<String> description) { this.description = OncePerRequestModel.of(description); return this; }
    public SwAttachment setImageUrl(String url) { return setImageUrl(Model.of(url)); }
    public SwAttachment setImageUrl(IModel<String> url) { imageUrl = OncePerRequestModel.of(url); return this; }
    /** Decorative beside a filename by default; media-only cards use the title. */
    public SwAttachment setImageAlt(String alt) { return setImageAlt(Model.of(alt)); }
    public SwAttachment setImageAlt(IModel<String> alt) { imageAlt = OncePerRequestModel.of(alt); return this; }
    public SwAttachment setIcon(String name) { return setIcon(Model.of(name)); }
    public SwAttachment setIcon(IModel<String> name) { icon.setDefaultModel(OncePerRequestModel.of(Objects.requireNonNull(name))); return this; }
    public SwAttachment setShowContent(boolean show) { showContent = show; return this; }
    public SwAttachment setState(AttachmentBehavior.State state) { appearance.setState(state); return this; }
    public AttachmentBehavior.State getState() { return appearance.getState(); }
    public SwAttachment setSize(AttachmentBehavior.Size size) { appearance.setSize(size); return this; }
    public SwAttachment setOrientation(AttachmentBehavior.Orientation orientation) { appearance.setOrientation(orientation); return this; }
    public SwAttachment setFullWidth(boolean fullWidth) { appearance.setFullWidth(fullWidth); return this; }
    public SwAttachment setMedia(Component media) { requireId(media, MEDIA_ID); this.media.replace(media); customMedia = true; return this; }
    public SwAttachment setActions(Component actions) { requireId(actions, ACTIONS_ID); this.actions.replace(actions); hasActions = true; return this; }
    /** Bind links to the template's anchor; a custom button can change its tag in onComponentTag. Name the control. */
    public SwAttachment setTrigger(Component trigger) {
        requireId(trigger, TRIGGER_ID);
        if (trigger.getBehaviors(AttachmentControlBehavior.class).isEmpty()) trigger.add(new AttachmentControlBehavior(AttachmentControlBehavior.Kind.TRIGGER));
        replace(trigger); hasTrigger = true; return this;
    }
    public SwAttachment setLink(String url, String label) { return setLink(Model.of(url), Model.of(label)); }
    public SwAttachment setLink(IModel<String> url, IModel<String> label) {
        var link = new ExternalLink(TRIGGER_ID, OncePerRequestModel.of(url));
        link.add(new AttachmentControlBehavior(AttachmentControlBehavior.Kind.TRIGGER).setLabel(OncePerRequestModel.of(label)));
        return setTrigger(link);
    }
    /** A native invoker: pass the actual dialog element (e.g. preview.get("dialog")), not its Panel wrapper. */
    public SwAttachment setPreview(Component dialog, String label) { return setPreview(dialog, Model.of(label)); }
    public SwAttachment setPreview(Component dialog, IModel<String> label) {
        Objects.requireNonNull(dialog).setOutputMarkupId(true);
        var trigger = new WebMarkupContainer(TRIGGER_ID) {
            @Override protected void onComponentTag(ComponentTag tag) { super.onComponentTag(tag); tag.setName("button"); }
        };
        trigger.add(AttributeModifier.replace("commandfor", (IModel<String>) dialog::getMarkupId));
        trigger.add(AttributeModifier.replace("command", "show-modal"));
        trigger.add(new AttachmentControlBehavior(AttachmentControlBehavior.Kind.TRIGGER).setLabel(OncePerRequestModel.of(label)));
        return setTrigger(trigger);
    }
    public Component getTrigger() { return get(TRIGGER_ID); }
    private static void requireId(Component component, String id) {
        if (!id.equals(component.getId())) throw new IllegalArgumentException("The attachment slot must use id \"" + id + "\"");
    }
    @Override protected void onConfigure() {
        super.onConfigure(); content.setVisible(showContent); actions.setVisible(hasActions); get(TRIGGER_ID).setVisible(hasTrigger);
    }
    @Override public void renderHead(IHeaderResponse response) {
        super.renderHead(response); Shadwick.renderResources(response);
        if (hasImage() || hasTrigger) response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
    }
    @Override protected void onDetach() {
        super.onDetach();
        for (IModel<String> model : java.util.Arrays.asList(title, description, imageUrl, imageAlt)) if (model != null) model.detach();
    }
}

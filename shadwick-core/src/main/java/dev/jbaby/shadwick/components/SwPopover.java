package dev.jbaby.shadwick.components;

import com.github.openjson.JSONObject;
import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.util.SerializableFunction;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.Strings;

import java.util.Objects;

/**
 * A native, non-modal popover containing any Wicket component, following OatPopover's factory API.
 * Its trigger opens it without a request. Escape, light dismissal and moving focus outside close it.
 * Update {@link #getContent()} over Ajax to retain the open panel; replacing the entire component closes it.
 * CSS anchor positioning supplies placement and flipping, without inline styles.
 */
public class SwPopover extends Panel {
    public static final String CONTENT_ID = "content";
    public enum Side { TOP, BOTTOM, START, END }
    public enum Align { START, CENTER, END }

    private final WebMarkupContainer popover;
    private final WebMarkupContainer trigger;
    private final Label title;
    private final Label description;
    private final ButtonBehavior triggerButton = new ButtonBehavior(ButtonBehavior.Variant.OUTLINE);
    private Side side = Side.BOTTOM;
    private Align align = Align.CENTER;

    public SwPopover(String id, String triggerLabel, SerializableFunction<String, ? extends Component> content) {
        this(id, Model.of(triggerLabel), content);
    }

    /** The factory's result must use the supplied CONTENT_ID. */
    public SwPopover(String id, IModel<String> triggerLabel, SerializableFunction<String, ? extends Component> content) {
        super(id);
        Objects.requireNonNull(triggerLabel, "triggerLabel");
        Objects.requireNonNull(content, "content");
        setOutputMarkupId(true);
        add(AttributeModifier.append("class", "sw-popover"));

        popover = new WebMarkupContainer("popover");
        popover.setOutputMarkupId(true);
        popover.add(AttributeModifier.replace("data-side", (IModel<String>) () -> switch (side) {
            case START -> "left"; case END -> "right"; default -> side.name().toLowerCase(java.util.Locale.ROOT);
        }));
        popover.add(AttributeModifier.replace("data-align", (IModel<String>) () -> align.name().toLowerCase(java.util.Locale.ROOT)));
        add(popover);

        trigger = new WebMarkupContainer("trigger");
        trigger.add(new Label("triggerLabel", triggerLabel));
        trigger.add(triggerButton);
        trigger.add(AttributeModifier.replace("popovertarget", (IModel<String>) popover::getMarkupId));
        trigger.add(AttributeModifier.replace("aria-controls", (IModel<String>) popover::getMarkupId));
        add(trigger);

        title = optionalLabel("title");
        description = optionalLabel("description");
        WebMarkupContainer header = new WebMarkupContainer("header") {
            @Override protected void onConfigure() {
                super.onConfigure();
                setVisible(hasText(title) || hasText(description));
            }
        };
        header.add(title, description);
        popover.add(header);
        popover.add(AttributeModifier.replace("aria-labelledby", (IModel<String>) () -> hasText(title) ? title.getMarkupId() : null));
        popover.add(AttributeModifier.replace("aria-label", (IModel<String>) () -> hasText(title) ? null : triggerLabel.getObject()));
        popover.add(AttributeModifier.replace("aria-describedby", (IModel<String>) () -> hasText(description) ? description.getMarkupId() : null));
        setContent(content.apply(CONTENT_ID));
    }

    private static boolean hasText(Label label) { return !Strings.isEmpty(label.getDefaultModelObjectAsString()); }
    private static Label optionalLabel(String id) {
        Label label = new Label(id, new Model<String>()) {
            @Override protected void onConfigure() { super.onConfigure(); setVisible(hasText(this)); }
        };
        label.setOutputMarkupId(true);
        return label;
    }

    public WebMarkupContainer getTrigger() { return trigger; }
    public Component getContent() { return popover.get(CONTENT_ID); }
    public SwPopover setContent(Component content) {
        if (content == null || !CONTENT_ID.equals(content.getId())) {
            throw new IllegalArgumentException("The popover content must use SwPopover.CONTENT_ID (\"content\")");
        }
        content.setOutputMarkupId(true);
        popover.addOrReplace(content);
        return this;
    }

    public SwPopover setTitle(String title) { return setTitle(Model.of(title)); }
    public SwPopover setTitle(IModel<String> title) { this.title.setDefaultModel(title); return this; }
    public SwPopover setDescription(String description) { return setDescription(Model.of(description)); }
    public SwPopover setDescription(IModel<String> description) { this.description.setDefaultModel(description); return this; }
    public SwPopover setSide(Side side) { this.side = Objects.requireNonNull(side); return this; }
    public SwPopover setAlign(Align align) { this.align = Objects.requireNonNull(align); return this; }
    public SwPopover setTriggerVariant(ButtonBehavior.Variant variant) { triggerButton.setVariant(variant); return this; }

    /** Closes from a submit/click handler, restoring focus when it was in the popover. */
    public SwPopover close(IPartialPageRequestHandler target) {
        target.appendJavaScript("Shadwick.closePopover(" + JSONObject.quote(popover.getMarkupId()) + ");");
        return this;
    }

    @Override public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        Shadwick.renderResources(response);
        response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
    }
}

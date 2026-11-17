package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.util.ComponentIds;
import dev.jbaby.shadwick.Shadwick;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * An empty state with title, optional description, icon and caller-supplied actions. A dashed outline marks out its
 * area by default; {@link #setBorder(boolean) setBorder(false)} leaves it out, for example inside a card or table.
 */
public class SwEmptyState extends Panel {
    public static final String CONTENT_ID = "content";
    private final WebMarkupContainer media;
    private boolean border = true;

    public SwEmptyState(String id, String title) { this(id, title, null); }
    public SwEmptyState(String id, String title, String message) { this(id, Model.of(title), Model.of(message)); }
    public SwEmptyState(String id, IModel<String> title, IModel<String> message) {
        super(id);
        // Transparent, so the title, message, media and content keep their paths directly under the panel.
        add(new TransparentWebMarkupContainer("empty") {
            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                if (border) tag.put("data-border", "");
                else tag.remove("data-border");
            }
        });
        add(new Label("title", title));
        add(new Label("message", message) {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                Object value = getDefaultModelObject();
                setVisible(value != null && !value.toString().isBlank());
            }
        });
        media = new WebMarkupContainer("media");
        media.setVisible(false);
        media.add(new WebMarkupContainer("icon").setVisible(false));
        add(media);
        add(new WebMarkupContainer(CONTENT_ID).setVisible(false));
    }

    public SwEmptyState setIcon(String name) {
        media.addOrReplace(SwIcon.ofNullable("icon", name));
        media.setVisible(name != null);
        return this;
    }

    /** Whether a dashed outline marks out the empty state's area; on by default. */
    public SwEmptyState setBorder(boolean border) {
        this.border = border;
        return this;
    }

    public boolean isBorder() {
        return border;
    }

    /** The component must have id CONTENT_ID; a Fragment or Panel can contain arbitrary actions. */
    public SwEmptyState setContent(Component content) {
        ComponentIds.forSlot(content, CONTENT_ID, "The empty state's content (SwEmptyState.CONTENT_ID)");
        addOrReplace(content);
        return this;
    }

    @Override
    public void renderHead(IHeaderResponse response) { super.renderHead(response); Shadwick.renderResources(response); }
}

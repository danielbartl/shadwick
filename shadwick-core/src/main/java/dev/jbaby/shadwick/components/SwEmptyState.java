package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Shadwick;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/** An empty state with title, optional description, icon and caller-supplied actions. */
public class SwEmptyState extends Panel {
    public static final String CONTENT_ID = "content";
    private final WebMarkupContainer media;

    public SwEmptyState(String id, String title) { this(id, title, null); }
    public SwEmptyState(String id, String title, String message) { this(id, Model.of(title), Model.of(message)); }
    public SwEmptyState(String id, IModel<String> title, IModel<String> message) {
        super(id);
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

    /** The component must have id CONTENT_ID; a Fragment or Panel can contain arbitrary actions. */
    public SwEmptyState setContent(Component content) {
        if (!CONTENT_ID.equals(content.getId())) { throw new IllegalArgumentException("Empty-state content must have id " + CONTENT_ID); }
        addOrReplace(content);
        return this;
    }

    @Override
    public void renderHead(IHeaderResponse response) { super.renderHead(response); Shadwick.renderResources(response); }
}

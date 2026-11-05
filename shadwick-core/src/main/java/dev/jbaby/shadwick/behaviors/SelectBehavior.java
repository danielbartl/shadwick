package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.Shadwick;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import java.util.Objects;

/** Progressive custom select styling/runtime for a wrapper containing a real single select. */
public class SelectBehavior extends StyledBehavior {
    public enum Size { DEFAULT, SMALL }
    public static final JavaScriptResourceReference SELECT_JS = new JavaScriptResourceReference(Shadwick.class, "js/select.js");
    private Size size = Size.DEFAULT;
    public SelectBehavior() { super("sw-select-wrapper"); }
    public SelectBehavior setSize(Size size) { this.size = Objects.requireNonNull(size); return this; }
    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.put("data-size", size == Size.SMALL ? "sm" : "default");
    }
    @Override public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
        response.render(JavaScriptHeaderItem.forReference(SELECT_JS));
    }
}

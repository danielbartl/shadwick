package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.util.LocalizedText;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

/** Opt-in busy feedback and repeated-activation protection for a component's Ajax requests. Requires Shadwick.install. */
public class AjaxBusyBehavior extends Behavior {
    public static final JavaScriptResourceReference BUSY_JS = new JavaScriptResourceReference(Shadwick.class, "js/busy.js");

    @Override public void onComponentTag(Component component, ComponentTag tag) {
        tag.put("data-sw-busy-label", LocalizedText.get(component, AjaxBusyBehavior.class, "AjaxBusyBehavior.loading", "Loading"));
    }
    @Override public void renderHead(Component component, IHeaderResponse response) {
        Shadwick.renderResources(response);
        response.render(JavaScriptHeaderItem.forReference(BUSY_JS));
    }
    private static final class BusyListener extends AjaxCallListener {
        BusyListener() {
            onInit("Shadwick.busy.start(attrs);");
            onPrecondition("return !attrs.swBusyRejected;");
            onDone("Shadwick.busy.done(attrs);");
        }
    }
    /** Registered once by the installer; also covers AjaxButton and AjaxEventBehavior on caller-owned controls. */
    public static final class Listener implements AjaxRequestTarget.IListener {
        @Override public void updateAjaxAttributes(AbstractDefaultAjaxBehavior behavior, AjaxRequestAttributes attributes) {
            Component component = behavior.getComponent();
            if (component == null || component.getBehaviors(AjaxBusyBehavior.class).isEmpty()) return;
            if (attributes.getAjaxCallListeners().stream().noneMatch(BusyListener.class::isInstance))
                attributes.getAjaxCallListeners().add(new BusyListener());
        }
    }
}

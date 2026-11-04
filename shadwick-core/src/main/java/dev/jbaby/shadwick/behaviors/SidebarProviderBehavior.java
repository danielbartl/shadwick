package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.Shadwick;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.http.WebRequest;

import java.util.Locale;
import java.util.Objects;

/** Caller-owned wrapper of one sidebar and its content, with an optional desktop preference cookie. */
public class SidebarProviderBehavior extends StyledBehavior {

    public enum State { EXPANDED, COLLAPSED }

    private State defaultState = State.EXPANDED;
    private boolean rememberState = true;
    private String cookieName = "sw-sidebar-state";

    public SidebarProviderBehavior() {
        super("sw-sidebar-provider");
    }

    public SidebarProviderBehavior setDefaultState(State state) {
        defaultState = Objects.requireNonNull(state);
        return this;
    }

    public SidebarProviderBehavior setRememberState(boolean remember) {
        rememberState = remember;
        return this;
    }

    /** Distinct names let independent sidebars remember separate preferences. */
    public SidebarProviderBehavior setCookieName(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Use a non-empty cookie name with letters, numbers, _ or -");
        }
        cookieName = name;
        return this;
    }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        String state = defaultState.name().toLowerCase(Locale.ROOT);
        if (rememberState && component.getRequest() instanceof WebRequest request) {
            var cookie = request.getCookie(cookieName);
            if (cookie != null && ("expanded".equals(cookie.getValue()) || "collapsed".equals(cookie.getValue()))) {
                state = cookie.getValue();
            }
        }
        tag.put("data-state", state);
        tag.put("data-sidebar-remember", String.valueOf(rememberState));
        tag.put("data-sidebar-cookie", cookieName);
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
    }
}

package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.components.SwFeedbackPanel;
import dev.jbaby.shadwick.components.form.NotShownInlineFilter;
import dev.jbaby.shadwick.Shadwick;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import com.github.openjson.JSONObject;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.feedback.FeedbackCollector;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.resource.loader.ComponentStringResourceLoader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shows feedback messages ({@code info()}, {@code success()}, {@code warn()},
 * {@code error()}) as Shadwick toasts, errors styled as destructive, other levels as neutral.
 * Add it to a page, or to a base page so every page gets it:
 * {@code add(Sw.Behaviors.feedbackToasts())}.
 * <p>
 * It shows the messages of the component it is added to and of its children, plus
 * session messages - so a {@code getSession().success(...)} before
 * {@code setResponsePage(...)} appears on the next page. Messages are shown on a full
 * page render and during Ajax requests, without adding anything to the
 * {@code AjaxRequestTarget}. By default, errors that Shadwick form fields already show
 * inline are left out ({@link NotShownInlineFilter}), and so are the messages of a
 * container that an {@link SwFeedbackPanel} shows, its fence: all but the session
 * messages when the toasts are added to the fence or inside it. Fences of Wicket's own
 * {@code FencedFeedbackPanel} aren't known to the toasts, and a fenced panel that is
 * hidden still fences its container.
 * <p>
 * Messages are shown in order of severity, most severe last. Toasts remain until dismissed,
 * so a message never expires while someone is reading or operating it.
 * <p>
 * Each message is shown once: it is marked rendered, so a feedback panel showing all
 * messages would display it too, but a second toast behavior would not.
 */
public class FeedbackToastsBehavior extends Behavior {

    private final IFeedbackMessageFilter filter;

    public FeedbackToastsBehavior() {
        this(new NotShownInlineFilter());
    }

    /**
     * @param filter the messages to show, or {@code null} for all messages
     */
    public FeedbackToastsBehavior(IFeedbackMessageFilter filter) {
        this.filter = filter;
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        Shadwick.renderResources(response);
        response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
        String script = consumeMessages(component);
        if (!script.isEmpty()) {
            response.render(OnDomReadyHeaderItem.forScript(script));
        }
    }

    @Override
    public void onEvent(Component component, IEvent<?> event) {
        super.onEvent(component, event);
        // Wicket broadcasts the AjaxRequestTarget to every component before responding
        if (event.getPayload() instanceof AjaxRequestTarget target) {
            String script = consumeMessages(component);
            if (!script.isEmpty()) {
                target.appendJavaScript(script);
            }
        }
    }

    private String consumeMessages(Component component) {
        // Inside a fence, a SwFeedbackPanel shows the components' messages
        FeedbackCollector collector = isInFence(component) ? new FeedbackCollector() : new FeedbackCollector(component, true) {
            @Override
            protected boolean shouldRecurseInto(Component child) {
                return !SwFeedbackPanel.isFence(child);
            }
        };
        // Keep message order stable within each severity.
        // A copy: collect() doesn't promise a list that can be sorted.
        List<FeedbackMessage> messages = new ArrayList<>(collector.collect(this::accept));
        messages.sort(Comparator.comparingInt(FeedbackMessage::getLevel));
        StringBuilder script = new StringBuilder();
        String closeLabel = null;
        for (FeedbackMessage message : messages) {
            if (closeLabel == null) {
                String key = "FeedbackToastsBehavior.close";
                String fallback = new ComponentStringResourceLoader().loadStringResource(FeedbackToastsBehavior.class,
                        key, component.getLocale(), component.getStyle(), component.getVariation());
                closeLabel = component.getString(key, null, fallback);
            }
            message.markRendered();
            script.append("Shadwick.toast(").append(quote(String.valueOf(message.getMessage())))
                    .append(',').append(quote(message.getLevelAsString().toLowerCase(java.util.Locale.ROOT)))
                    .append(',').append(quote(closeLabel))
                    .append(");");
        }
        return script.toString();
    }

    private static boolean isInFence(Component component) {
        for (Component c = component; c != null; c = c.getParent()) {
            if (SwFeedbackPanel.isFence(c)) {
                return true;
            }
        }
        return false;
    }

    private static String quote(String text) {
        return JSONObject.quote(text).replace("<", "\\u003c").replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
    }

    private boolean accept(FeedbackMessage message) {
        return !message.isRendered() && (filter == null || filter.accept(message));
    }
}

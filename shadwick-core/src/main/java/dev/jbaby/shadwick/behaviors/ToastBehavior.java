package dev.jbaby.shadwick.behaviors;

import com.github.openjson.JSONObject;
import dev.jbaby.shadwick.Shadwick;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.resource.loader.ComponentStringResourceLoader;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

/**
 * Oat's toast helpers, with Shadleaf's variants and text-only title/description anatomy.
 * Attach this behavior to the page (usually the base page) to supply its resources before
 * using Ajax helpers or {@link #script(String, Variant, String)} in an OnDomReadyHeaderItem.
 * The message is the body, and an optional title appears above it. No markup is accepted.
 */
public class ToastBehavior extends Behavior {
    public enum Variant { DEFAULT, SUCCESS, INFO, WARNING, ERROR }
    public static final Duration DEFAULT_DURATION = Duration.ofSeconds(5);
    private static final String CLOSE = "Dismiss notification";
    private static final String REGION = "Notifications (Alt+T)";

    @Override public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        renderResources(response);
    }

    public static void toast(IPartialPageRequestHandler handler, String message) {
        toast(handler, message, Variant.DEFAULT);
    }
    public static void toast(IPartialPageRequestHandler handler, String message, Variant variant) {
        toast(handler, message, variant, null);
    }
    public static void toast(IPartialPageRequestHandler handler, String message, Variant variant, String title) {
        toast(handler, message, variant, title, DEFAULT_DURATION);
    }
    /** Duration.ZERO keeps a toast until it is dismissed. Positive durations pause during interaction. */
    public static void toast(IPartialPageRequestHandler handler, String message, Variant variant, String title, Duration duration) {
        Objects.requireNonNull(handler);
        String script = handler.getPage() instanceof Component component
                ? script(component, message, variant, title, duration) : script(message, variant, title, duration);
        handler.appendJavaScript(script);
    }

    public static String script(String message, Variant variant, String title) {
        return script(message, variant, title, DEFAULT_DURATION);
    }
    /** Pure script builder with English control labels; use the component overload for localized labels. */
    public static String script(String message, Variant variant, String title, Duration duration) {
        return buildScript(message, variant, title, duration, CLOSE, REGION);
    }
    /** Full-render helper resolving ToastBehavior.close and ToastBehavior.label on the given component. */
    public static String script(Component component, String message, Variant variant, String title, Duration duration) {
        Objects.requireNonNull(component);
        return buildScript(message, variant, title, duration,
                label(component, "ToastBehavior.close", CLOSE), label(component, "ToastBehavior.label", REGION));
    }

    private static String buildScript(String message, Variant variant, String title, Duration duration, String close, String region) {
        Objects.requireNonNull(message); Objects.requireNonNull(duration);
        long millis;
        try { millis = duration.toMillis(); }
        catch (ArithmeticException exception) { throw new IllegalArgumentException("Toast duration is too large", exception); }
        if (duration.isNegative() || millis > Integer.MAX_VALUE || millis == 0 && !duration.isZero()) {
            throw new IllegalArgumentException("Toast duration must be zero or between 1 and 2147483647 milliseconds");
        }
        JSONObject options = new JSONObject()
                .put("variant", (variant == null ? Variant.DEFAULT : variant).name().toLowerCase(Locale.ROOT))
                .put("duration", millis).put("dismissLabel", close).put("regionLabel", region);
        if (title != null && !title.isBlank()) options.put("title", title);
        return ("Shadwick.toast(" + JSONObject.quote(message) + "," + options + ");")
                .replace("<", "\\u003c").replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
    }

    private static String label(Component component, String key, String defaultValue) {
        String fallback = new ComponentStringResourceLoader().loadStringResource(ToastBehavior.class,
                key, component.getLocale(), component.getStyle(), component.getVariation());
        return component.getString(key, null, fallback == null ? defaultValue : fallback);
    }
    private static void renderResources(IHeaderResponse response) {
        Shadwick.renderResources(response);
        response.render(JavaScriptHeaderItem.forReference(Shadwick.SHADWICK_JS));
    }
}

package dev.jbaby.shadwick.behaviors;

import com.github.openjson.JSONObject;
import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.ShadwickSettings;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.resource.loader.ComponentStringResourceLoader;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

/**
 * Oat's toast helpers, with Shadleaf's variants and text-only title/description anatomy.
 * Attach this behavior to the page (usually the base page) to supply its resources before
 * using Ajax helpers or {@link #script(String, Variant, String)} in an OnDomReadyHeaderItem.
 * The message is the body, and an optional title appears above it. No markup is accepted.
 * <p>
 * Toasts stack, as in Sonner and Shadleaf: the newest in front and at most {@link #DEFAULT_VISIBLE_TOASTS} showing,
 * spreading out while the pointer is on them or one has the keyboard focus; older ones wait, with their time stopped,
 * until a newer one closes. {@link ShadwickSettings#setToastPosition(Position)} and
 * {@link ShadwickSettings#setVisibleToasts(int)} set where they appear and how many show, and {@link #configure}
 * changes both for one page. In a browser without constructable stylesheets they appear in a plain scrolling column.
 */
public class ToastBehavior extends Behavior {
    public enum Variant { DEFAULT, SUCCESS, INFO, WARNING, ERROR }

    /**
     * Where toasts appear: a corner, or the middle of the top or bottom edge. Start and end follow the text
     * direction, so they swap on a right-to-left page. On a narrow screen toasts take the full width.
     */
    public enum Position {
        TOP_START, TOP_CENTER, TOP_END, BOTTOM_START, BOTTOM_CENTER, BOTTOM_END;

        /** The {@code data-position} value, e.g. {@code bottom-end}. */
        public String value() { return name().toLowerCase(Locale.ROOT).replace('_', '-'); }
    }

    public static final Duration DEFAULT_DURATION = Duration.ofSeconds(5);
    /** How many toasts show at once unless {@link ShadwickSettings#setVisibleToasts(int)} says otherwise. */
    public static final int DEFAULT_VISIBLE_TOASTS = 3;
    /** The most toasts that can show at once. */
    public static final int MAX_VISIBLE_TOASTS = 10;
    private static final String CLOSE = "Dismiss notification";
    private static final String REGION = "Notifications (Alt+T)";

    @Override public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        renderResources(response);
        renderToasterSettings(component, response);
    }

    /**
     * Renders the application's toast position and stack size ({@link ShadwickSettings}) for the page. Both toast
     * behaviors call this; the script is the same for both, so it is rendered once.
     */
    static void renderToasterSettings(Component component, IHeaderResponse response) {
        ShadwickSettings settings = ShadwickSettings.get(component.getApplication());
        response.render(OnDomReadyHeaderItem.forScript(configureScript(settings.getToastPosition(), settings.getVisibleToasts())));
    }

    /**
     * Changes where toasts appear and how many show, for this page only, in an Ajax response: for example from a
     * user's preference. The application's defaults come from {@link ShadwickSettings}.
     */
    public static void configure(IPartialPageRequestHandler handler, Position position, int visibleToasts) {
        Objects.requireNonNull(handler).appendJavaScript(configureScript(position, visibleToasts));
    }

    /** The script {@link #configure} sends, for an {@code OnDomReadyHeaderItem} in a full render. */
    public static String configureScript(Position position, int visibleToasts) {
        Objects.requireNonNull(position, "position");
        JSONObject options = new JSONObject().put("position", position.value())
                .put("visibleToasts", requireVisibleToasts(visibleToasts));
        return "Shadwick.configureToasts(" + options + ");";
    }

    /** Checks a number of visible toasts: 1 to {@link #MAX_VISIBLE_TOASTS}. */
    private static int requireVisibleToasts(int count) {
        if (count < 1 || count > MAX_VISIBLE_TOASTS) {
            throw new IllegalArgumentException("The number of visible toasts must be between 1 and " + MAX_VISIBLE_TOASTS + ", not " + count);
        }
        return count;
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

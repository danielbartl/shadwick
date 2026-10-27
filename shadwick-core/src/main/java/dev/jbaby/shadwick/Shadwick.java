package dev.jbaby.shadwick;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

import java.util.EnumMap;
import java.util.Map;

/**
 * Installs Shadwick in a WebApplication, and holds its resources.
 * <p>
 * The resource references are public so they can be swapped with Wicket's own mechanism, e.g. to serve the
 * stylesheet from a CDN:
 * {@code addResourceReplacement(Shadwick.css(Skin.VEGA), new UrlResourceReference(Url.parse("https://.../shadwick-vega.css")))}.
 */
public final class Shadwick {

    private static final Map<Skin, CssResourceReference> CSS = new EnumMap<>(Skin.class);

    static {
        for (Skin skin : Skin.values()) {
            CSS.put(skin, new CssResourceReference(Shadwick.class, "css/shadwick-" + skin.id() + ".css"));
        }
    }

    /**
     * Shadwick's script, for the components that need one (form fields, dialogs). They render it themselves; it
     * isn't added to every page.
     */
    public static final JavaScriptResourceReference SHADWICK_JS = new JavaScriptResourceReference(Shadwick.class, "js/shadwick.js");

    private Shadwick() {
    }

    /**
     * Installs Shadwick into the given application: adds the stylesheet of the configured skin to every page. Nothing
     * is added to Wicket's strict, nonce-based Content Security Policy, as Shadwick renders no inline scripts, event
     * handlers or style attributes, and loads no images, fonts or data: URLs.
     *
     * @param app the application to configure
     * @return the application's {@link ShadwickSettings}, to choose the skin
     */
    public static ShadwickSettings install(WebApplication app) {
        // Add the stylesheet to every page, unless the application opted out
        app.getHeaderContributorListeners().add(response -> {
            if (ShadwickSettings.get(app).isAddResources()) {
                renderResources(response);
            }
        });
        return ShadwickSettings.get(app);
    }

    /**
     * Renders the stylesheet of the current request's skin. {@link #install} does this on every page, and every
     * Shadwick component or behavior on the pages it renders on; Wicket adds it once.
     */
    public static void renderResources(IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(css(ShadwickSettings.get().getCurrentSkin())));
    }

    /** The stylesheet of a skin: Tailwind's preflight, the page's base styles, the tokens and every component. */
    public static CssResourceReference css(Skin skin) {
        return CSS.get(skin);
    }
}

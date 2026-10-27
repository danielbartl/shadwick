package dev.jbaby.shadwick;

import org.apache.wicket.Application;
import org.apache.wicket.MetaDataKey;

import java.io.Serializable;
import java.util.Objects;

/**
 * Application-wide Shadwick settings, returned by {@link Shadwick#install}:
 *
 * <pre>
 * Shadwick.install(this)
 *         .setSkin(Skin.LYRA);
 * </pre>
 */
public class ShadwickSettings implements Serializable {

    private static final MetaDataKey<ShadwickSettings> KEY = new MetaDataKey<>() {};

    private Skin skin = Skin.VEGA;
    private SkinStore skinStore;
    private boolean addResources = true;
    private AccessibilityChecks accessibilityChecks;

    /** The settings of the current application. */
    public static ShadwickSettings get() {
        return get(Application.get());
    }

    public static synchronized ShadwickSettings get(Application application) {
        ShadwickSettings settings = application.getMetaData(KEY);
        if (settings == null) {
            settings = new ShadwickSettings();
            settings.accessibilityChecks = application.usesDevelopmentConfig() ? AccessibilityChecks.FAIL : AccessibilityChecks.WARN;
            application.setMetaData(KEY, settings);
        }
        return settings;
    }

    /** The app-wide skin; {@code VEGA} by default. */
    public Skin getSkin() {
        return skin;
    }

    /** The skin every page is rendered with, unless the {@link #setSkinStore skin store} has another one. */
    public ShadwickSettings setSkin(Skin skin) {
        this.skin = Objects.requireNonNull(skin);
        return this;
    }

    /** The skin of the current request: the {@link #getSkinStore() store}'s, or else the app-wide one. */
    public Skin getCurrentSkin() {
        Skin stored = skinStore != null ? skinStore.load() : null;
        return stored != null ? stored : skin;
    }

    /** The store of a skin other than the app-wide one, or {@code null} if there is none (the default). */
    public SkinStore getSkinStore() {
        return skinStore;
    }

    /**
     * Where a skin other than the app-wide one is kept, e.g. in the session for a showcase that switches skins;
     * {@code null} for none, so every page has the app-wide skin.
     */
    public ShadwickSettings setSkinStore(SkinStore skinStore) {
        this.skinStore = skinStore;
        return this;
    }

    public boolean isAddResources() {
        return addResources;
    }

    /**
     * Whether Shadwick's stylesheet is added to every page (the default). Turned off, only the pages with a Shadwick
     * component or behavior get it, as each adds it itself; {@link Shadwick#renderResources} adds it to others.
     */
    public ShadwickSettings setAddResources(boolean addResources) {
        this.addResources = addResources;
        return this;
    }

    public AccessibilityChecks getAccessibilityChecks() {
        return accessibilityChecks;
    }

    /**
     * What happens with an accessibility mistake Shadwick can detect, for now a button without an accessible name:
     * {@code FAIL} in Wicket's development mode, so that tests and local runs catch it, {@code WARN} in deployment.
     */
    public ShadwickSettings setAccessibilityChecks(AccessibilityChecks accessibilityChecks) {
        this.accessibilityChecks = Objects.requireNonNull(accessibilityChecks);
        return this;
    }
}

package dev.jbaby.shadwick.util;

import org.apache.wicket.Component;
import org.apache.wicket.resource.loader.ComponentStringResourceLoader;

/** Resource lookup for parts and behaviors that do not inherit their owning component's bundle. */
public final class LocalizedText {
    private LocalizedText() {}

    /** Normal Wicket resource lookup wins; the library scope supplies the locale-aware fallback. */
    public static String get(Component component, Class<?> scope, String key, String defaultValue) {
        String fallback = new ComponentStringResourceLoader().loadStringResource(scope, key,
                component.getLocale(), component.getStyle(), component.getVariation());
        return component.getString(key, null, fallback == null ? defaultValue : fallback);
    }
}

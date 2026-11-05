package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.ShadwickSettings;
import dev.jbaby.shadwick.ThemeMode;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Applies a browser-owned color preference. Put on html to apply storage/system preference before first paint.
 * A separate preview container can own an independent preference and opt out of storage.
 * Configuration is a fallback, not the browser's current choice; no session or server callback is needed.
 */
public class ThemeBehavior extends Behavior {
    private ThemeMode defaultMode;
    private String storageKey;
    private boolean rememberChoice = true;

    public ThemeBehavior setDefaultMode(ThemeMode mode) { defaultMode = Objects.requireNonNull(mode); return this; }
    public ThemeBehavior setStorageKey(String key) { storageKey = validateStorageKey(key); return this; }
    public ThemeBehavior setRememberChoice(boolean remember) { rememberChoice = remember; return this; }

    public static String validateStorageKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9_.:-]{1,128}")) {
            throw new IllegalArgumentException("Use a storage key of 1–128 letters, numbers, _, ., : or -");
        }
        return key;
    }

    @Override public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        ShadwickSettings settings = ShadwickSettings.get();
        ThemeMode mode = defaultMode != null ? defaultMode : settings.getDefaultThemeMode();
        tag.put("data-sw-theme-root", "");
        tag.put("data-sw-theme-default", mode.id());
        tag.put("data-sw-theme-storage-key", storageKey != null ? storageKey : settings.getThemeStorageKey());
        tag.put("data-sw-theme-remember", String.valueOf(rememberChoice));
        String classes = Objects.toString(tag.getAttribute("class"), "");
        String rest = Arrays.stream(classes.split("\\s+")).filter(value -> !value.isBlank() && !value.equals("dark"))
                .collect(Collectors.joining(" "));
        if (mode == ThemeMode.DARK) rest = (rest + " dark").trim();
        if (rest.isEmpty()) tag.remove("class"); else tag.put("class", rest);
    }

    @Override public void renderHead(Component component, IHeaderResponse response) {
        Shadwick.renderThemeScript(response);
        Shadwick.renderResources(response);
    }
}

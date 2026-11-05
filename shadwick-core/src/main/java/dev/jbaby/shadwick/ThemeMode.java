package dev.jbaby.shadwick;

import java.util.Locale;

/** Browser color-mode preference; SYSTEM follows prefers-color-scheme. Independent of the skin and color tokens. */
public enum ThemeMode {
    LIGHT, DARK, SYSTEM;
    public String id() { return name().toLowerCase(Locale.ROOT); }
}

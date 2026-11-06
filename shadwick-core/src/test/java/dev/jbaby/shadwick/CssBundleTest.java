package dev.jbaby.shadwick;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import dev.jbaby.shadwick.Bundle;

import static org.assertj.core.api.Assertions.assertThat;

/** The bundles the build compiled from src/main/tailwind: one per skin, each also minified. */
class CssBundleTest {

    @ParameterizedTest
    @EnumSource(Skin.class)
    void everySkinHasABundleAndAMinifiedOne(Skin skin) throws IOException {
        assertThat(read("shadwick-" + skin.id() + ".css")).isNotBlank();
        assertThat(read("shadwick-" + skin.id() + ".min.css")).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(Skin.class)
    void aBundleHasTheTokensForLightAndDarkAndTheComponents(Skin skin) throws IOException {
        String css = read("shadwick-" + skin.id() + ".css");

        assertThat(css).contains(":root {").contains(".dark {").contains("--primary:").contains("--radius:");
        assertThat(css).contains(".sw-button", ".sw-field", ".sw-input", ".sw-dialog", ".sw-alert-dialog", ".sw-sheet");
    }

    @ParameterizedTest
    @EnumSource(Skin.class)
    void aBundleEmitsNoUtilityClasses(Skin skin) throws IOException {
        // Without comments (the banner's URL) and escaped dots
        String css = read("shadwick-" + skin.id() + ".min.css").replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("url\\([^)]*\\)", "").replace("\\.", "");

        // Every class selector is a component's, so an application's own Tailwind utilities always win
        assertThat(css).doesNotContainPattern("\\.(?!sw-|dark\\b)[a-z][\\w-]*[\\s{:,\\[>~+)]");
    }

    private static String read(String name) throws IOException {
        try (InputStream in = Shadwick.class.getResourceAsStream("css/" + name)) {
            assertThat(in).as(name + " on the classpath").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @ParameterizedTest
    @EnumSource(Skin.class)
    void embeddedBundlesHaveComponentsAndTokensWithoutPreflightOrTailwindThemeVariables(Skin skin) throws IOException {
        for (String suffix : new String[] {".css", ".min.css"}) {
            String css = read("shadwick-" + skin.id() + Bundle.EMBEDDED.suffix() + suffix);
            assertThat(css).contains("--primary:").contains(".dark").contains(".sw-card")
                    .contains(".sw-badge").contains(".sw-alert").contains(".sw-avatar")
                    .contains(".sw-skeleton").contains(".sw-spinner").contains(".sw-toast")
                    .contains(".sw-alert-dialog", ".sw-sheet-body-content", "--sw-sheet-slide")
                    .contains(".sw-popover-content", ".sw-popover-header", ".sw-tooltip-content", "position-area:");
            assertThat(css).contains(".sw-dropdown-menu-content", ".sw-dropdown-menu-item", ".sw-dropdown-menu-checkbox-item",
                    ".sw-dropdown-menu-radio-item", ".sw-dropdown-menu-separator", ".sw-dropdown-menu-shortcut");
            assertThat(css).contains(".sw-site-header", ".sw-site-header-end", ".sw-skip-link", ".sw-breadcrumb-list", ".sw-breadcrumb-link", ".sw-breadcrumb-separator-icon");
            assertThat(css).contains(".sw-sidebar-provider", ".sw-sidebar-inset", ".sw-sidebar-trigger", ".sw-sidebar-menu-button", "--sw-sidebar-width-icon");
            assertThat(css).contains(".sw-app-layout", ".sw-sidebar-menu-sub-button", ".sw-sidebar-menu-action", "data-placement", "data-panel-only");
            assertThat(css).contains(".sw-combobox", ".sw-combobox-input", ".sw-combobox-chip", ".sw-combobox-content");
            assertThat(css).contains(".sw-month-view", ".sw-month-view-day-link", ".sw-chart-plot", ".sw-chart-tooltip-content", ".sw-chart-series-5");
            assertThat(css).contains(".sw-select-wrapper", ".sw-select-item", ".sw-select-content", "base-select", "selectedcontent");
            assertThat(css).contains(".sw-table", ".sw-table-container", ".sw-table-sort", ".sw-table-footer", ".sw-pagination", ".sw-pagination-content", "--sw-table-scrolled");
            assertThat(css).contains(".sw-attachment", ".sw-attachment-trigger", ".sw-attachment-group", "sw-attachment-shimmer", "sw-attachment-scroll-fade");
            assertThat(css).contains(".sw-tabs-list", ".sw-tabs-trigger", ".sw-tabs-content", ".sw-accordion-trigger", ".sw-collapsible-trigger", "--sw-accordion-duration");
            assertThat(css).contains(".sw-theme-toggle", ".sw-theme-toggle-dark", ".sw-theme-toggle-light", "color-scheme:");
            assertThat(css).contains(".sw-user-menu", ".sw-user-menu-label", ".sw-user-menu-text", ".sw-user-menu-trigger");
            assertThat(css).contains(".sw-toaster", ".sw-toast-title", ".sw-toast-description", ".sw-toast-close");
            assertThat(css).doesNotContain("-webkit-text-size-adjust", "font-synthesis", "@layer theme {")
                    .doesNotContainPattern("(?s)(?:^|[{}])\\s*\\*[^{}]*\\{[^{}]*box-sizing\\s*:");
            assertThat(css).doesNotContainPattern("--(?:spacing|font-[\\w-]+|color-[\\w-]+|text-[\\w-]+|radius-[\\w-]+):");
            assertThat(css).doesNotContainPattern("var\\(--(?:spacing|font-|color-|text-|radius-|animate-)");
            assertThat(css).contains("@keyframes spin", "@keyframes pulse");
        }
    }

    @ParameterizedTest
    @EnumSource(Skin.class)
    void choiceControlsAreAvailableInBothBundlesWithoutImageRequests(Skin skin) throws IOException {
        for (Bundle bundle : Bundle.values()) {
            String css = read("shadwick-" + skin.id() + bundle.suffix() + ".css");
            // Sketch alone requests fonts, all bundled locally. Choice controls still use native CSS drawings.
            if (skin == Skin.SKETCH) {
                assertThat(css).contains("../fonts/balsamiq-sans/balsamiq-sans-latin-400-normal.woff2",
                        "../fonts/balsamiq-sans/balsamiq-sans-latin-700-normal.woff2");
                css = css.replaceAll("url\\(['\"]?\\.\\./fonts/balsamiq-sans/balsamiq-sans-latin-(?:400|700)-normal\\.woff2['\"]?\\)", "");
            }
            assertThat(css).contains(".sw-checkbox-wrapper", ".sw-checkbox-indicator", ".sw-radio-group-item", ".sw-switch",
                            ".sw-native-select-wrapper", ".sw-native-select-icon", ".sw-checkbox-group", ".sw-checkbox-option",
                            ".sw-toggle-input", ".sw-toggle-group", ".sw-input-group", ".sw-input-group-addon",
                            ".sw-file-upload", ".sw-file-upload-input", ".sw-file-upload-browse")
                    .contains("radial-gradient(", "forced-colors: active", ":dir(rtl)")
                    .doesNotContain("url(", "data:image");
        }
    }

    @Test
    void sketchFontsArePackagedAndEmbeddedTypographyDoesNotStyleTheHostBody() throws IOException {
        for (String weight : new String[] {"400", "700"}) {
            try (InputStream in = Shadwick.class.getResourceAsStream("fonts/balsamiq-sans/balsamiq-sans-latin-" + weight + "-normal.woff2")) {
                assertThat(in).isNotNull();
                assertThat(new String(in.readNBytes(4), StandardCharsets.US_ASCII)).isEqualTo("wOF2");
            }
        }
        try (InputStream in = Shadwick.class.getResourceAsStream("fonts/balsamiq-sans/OFL.txt")) {
            assertThat(in).isNotNull();
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).contains("SIL OPEN FONT LICENSE");
        }
        assertThat(read("shadwick-sketch-embedded.css")).contains("Balsamiq Sans")
                .doesNotContainPattern("(?s)(?:^|[{}])\\s*body\\s*\\{");
        assertThat(read("shadwick-sketch.css")).contains("body {");
    }
}

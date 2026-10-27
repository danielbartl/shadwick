package dev.jbaby.shadwick;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
        assertThat(css).contains(".sw-button").contains(".sw-field").contains(".sw-input").contains(".sw-dialog");
    }

    @ParameterizedTest
    @EnumSource(Skin.class)
    void aBundleEmitsNoUtilityClasses(Skin skin) throws IOException {
        // Without comments (the banner's URL) and escaped dots
        String css = read("shadwick-" + skin.id() + ".min.css").replaceAll("(?s)/\\*.*?\\*/", "").replace("\\.", "");

        // Every class selector is a component's, so an application's own Tailwind utilities always win
        assertThat(css).doesNotContainPattern("\\.(?!sw-|dark\\b)[a-z][\\w-]*[\\s{:,\\[>~+)]");
    }

    private static String read(String name) throws IOException {
        try (InputStream in = Shadwick.class.getResourceAsStream("css/" + name)) {
            assertThat(in).as(name + " on the classpath").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

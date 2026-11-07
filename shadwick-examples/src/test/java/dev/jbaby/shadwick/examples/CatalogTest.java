package dev.jbaby.shadwick.examples;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogTest {
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(new ExamplesApplication()); }
    @AfterEach void stop() { tester.destroy(); }

    @Test void rootAndExistingStudioBookmarksRenderDirectly() {
        for (String path : List.of("", "?view=projects&q=Sunday", "studio?view=projects&q=Sunday")) {
            tester.executeUrl(path);
            tester.assertRenderedPage(StudioPage.class);
            assertThat(tester.getLastResponse().getStatus()).isEqualTo(200);
            if (!path.isEmpty()) assertThat(tester.getLastResponseAsString()).contains("Sunday Coffee").doesNotContain("Common Ground</strong>");
        }
    }
    @Test void componentOverviewHasItsOwnRouteAndGroupedInventory() {
        tester.executeUrl("components");
        tester.assertRenderedPage(HomePage.class);
        assertThat(tester.getLastResponseAsString()).contains("A good place to build.", "Inputs &amp; forms", "Tables &amp; data", "Find a component");
        assertThat(CatalogPages.ALL).extracting(CatalogPages.Example::route).doesNotHaveDuplicates();
        assertThat(CatalogPages.ALL).extracting(CatalogPages.Example::page).doesNotHaveDuplicates();
    }
    @Test void navigationCarriesAppearanceAndLocaleWithoutUnrelatedDemoParameters() {
        tester.startPage(ButtonPage.class, new PageParameters().set("skin", "nova").set("mode", "dark").set("lang", "de").set("q", "private-demo-filter"));
        var link = (BookmarkablePageLink<?>)tester.getLastRenderedPage().get("catalogNavigation:groups:0:examples:1:link");
        assertThat(link.getPageParameters().get("skin").toString()).isEqualTo("nova");
        assertThat(link.getPageParameters().get("lang").toString()).isEqualTo("de");
        assertThat(link.getPageParameters().get("mode").toString()).isEqualTo("dark");
        assertThat(link.getPageParameters().get("q").isEmpty()).isTrue();
    }
    static Stream<Class<? extends Page>> examples() {
        return Stream.concat(CatalogPages.ALL.stream().map(CatalogPages.Example::page), Stream.of(UserSignInPage.class));
    }
    @ParameterizedTest @MethodSource("examples")
    void eachDemoDisplaysItsActualPackagedJavaAndMarkup(Class<? extends Page> page) throws Exception {
        tester.startPage(page);
        var files = ExampleSources.files(page);
        assertThat(files).extracting(ExampleSources.File::name).contains(page.getSimpleName() + ".java");
        assertThat(files).anyMatch(f -> f.name().endsWith(".html"));
        for (var file : files) {
            Path source = Path.of("src/main", file.name().endsWith(".java") ? "java" : "resources", "dev/jbaby/shadwick/examples", file.name());
            assertThat(ExampleSources.read(file.name())).isEqualTo(Files.readString(source).stripTrailing());
        }
        assertThat(tester.getLastResponseAsString()).contains("Build this example", "Copy code: " + page.getSimpleName() + ".java", "&lt;wicket:extend");
    }
    @Test void supportingPanelMarkupAndInheritedLayoutSourceAreIncluded() {
        assertThat(ExampleSources.files(SidebarPage.class)).extracting(ExampleSources.File::name).contains("SidebarPage$Brand.html");
        assertThat(ExampleSources.files(HeaderLayoutPage.class)).extracting(ExampleSources.File::name)
                .contains("HeaderLayoutPage.java", "AppLayoutPage.java", "AppLayoutPage.html");
    }
    @Test void theCatalogAndSourceBindingsSurvivePagePassivation() {
        tester.startPage(ButtonPage.class);
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        var restored = (ButtonPage)serializer.deserialize(serializer.serialize(tester.getLastRenderedPage()));
        tester.startPage(restored);
        tester.clickLink("small");
        assertThat(tester.getLastResponseAsString()).doesNotContain("<redirect>");
    }
}

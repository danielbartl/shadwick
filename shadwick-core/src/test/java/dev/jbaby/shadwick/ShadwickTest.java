package dev.jbaby.shadwick;

import org.apache.wicket.mock.MockApplication;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.apache.wicket.settings.RequestCycleSettings;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ShadwickTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new MockApplication() {
            @Override
            protected void init() {
                super.init();
                // MockApplication disables CSP for tests; restore WebApplication's real default
                getCspSettings().blocking().strict();
                Shadwick.install(this);
                // Render in one pass so the CSP header is on the page response, not a redirect
                getRequestCycleSettings().setRenderStrategy(RequestCycleSettings.RenderStrategy.ONE_PASS_RENDER);
            }
        });
    }

    @AfterEach
    void tearDown() {
        tester.destroy();
    }

    @Test
    void installKeepsWicketsStrictCspUnchanged() {
        tester.startPage(PlainPage.class);
        String csp = tester.getLastResponse().getHeader("Content-Security-Policy");

        assertThat(csp)
                .contains("script-src 'strict-dynamic' 'nonce-")
                .contains("style-src 'nonce-")
                .contains("img-src 'self';")
                .doesNotContain("data:")
                .doesNotContain("unsafe-inline")
                .doesNotContain("unsafe-eval");
    }

    @Test
    void installLeavesAPolicyTheApplicationTurnedOffOff() {
        WicketTester withoutCsp = new WicketTester(new MockApplication() {
            @Override
            protected void init() {
                super.init(); // turns the policy off, as an application can
                Shadwick.install(this);
                getRequestCycleSettings().setRenderStrategy(RequestCycleSettings.RenderStrategy.ONE_PASS_RENDER);
            }
        });
        withoutCsp.startPage(PlainPage.class);

        assertThat(withoutCsp.getLastResponse().getHeader("Content-Security-Policy")).isNull();
        withoutCsp.destroy();
    }

    @Test
    void installAddsTheDefaultSkinsStylesheetToEveryPage() {
        tester.startPage(PlainPage.class);

        assertThat(tester.getLastResponseAsString()).containsPattern("css/shadwick-vega[^\"]*\\.css").doesNotContain("shadwick-lyra");
    }

    @Test
    void theConfiguredSkinsStylesheetIsAdded() {
        ShadwickSettings.get(tester.getApplication()).setSkin(Skin.LYRA);
        tester.startPage(PlainPage.class);

        assertThat(tester.getLastResponseAsString()).containsPattern("css/shadwick-lyra[^\"]*\\.css").doesNotContain("shadwick-vega");
    }

    @Test
    void theSkinStoreChoosesTheSkinPerRequest() {
        Skin[] stored = {Skin.LYRA};
        ShadwickSettings.get(tester.getApplication()).setSkinStore(new SkinStore() {
            @Override
            public Skin load() {
                return stored[0];
            }

            @Override
            public void save(Skin skin) {
                stored[0] = skin;
            }
        });

        tester.startPage(PlainPage.class);
        assertThat(tester.getLastResponseAsString()).contains("shadwick-lyra").doesNotContain("shadwick-vega");

        // Nothing stored: the app-wide skin
        stored[0] = null;
        tester.startPage(PlainPage.class);
        assertThat(tester.getLastResponseAsString()).contains("shadwick-vega").doesNotContain("shadwick-lyra");
    }

    /** A skin store holding one skin for every request, as a session would for one user. */
    private static SkinStore memoryStore() {
        return new SkinStore() {
            private Skin skin;

            @Override
            public Skin load() {
                return skin;
            }

            @Override
            public void save(Skin skin) {
                this.skin = skin;
            }
        };
    }

    @Test
    void anAjaxUpdateKeepsTheSkinThePageWasRenderedWith() {
        ShadwickSettings.get(tester.getApplication()).setSkinStore(memoryStore());
        SkinSwitchPage page = tester.startPage(SkinSwitchPage.class);
        assertThat(tester.getLastResponseAsString()).contains("shadwick-vega");

        tester.clickLink("switch");

        // The browser has vega's stylesheet: lyra's on top would mix the two
        tester.assertComponentOnAjaxResponse("switch");
        assertThat(tester.getLastResponseAsString()).contains("shadwick-vega").doesNotContain("shadwick-lyra");

        // Rendered as a whole again, the page has the new skin
        tester.startPage(page);
        assertThat(tester.getLastResponseAsString()).contains("shadwick-lyra").doesNotContain("shadwick-vega");
    }

    @Test
    void switchingTheSkinNeedsASkinStore() {
        tester.startPage(PlainPage.class);

        assertThatIllegalStateException()
                .isThrownBy(() -> ShadwickSettings.get(tester.getApplication()).setCurrentSkin(Skin.LYRA))
                .withMessageContaining("setSkinStore");
    }

    @Test
    void theStylesheetCanBeReplacedForExampleWithACdn() {
        tester.getApplication().addResourceReplacement(Shadwick.css(Skin.VEGA),
                new UrlResourceReference(Url.parse("https://cdn.example.com/shadwick-vega.css")));
        tester.startPage(PlainPage.class);

        assertThat(tester.getLastResponseAsString())
                .contains("https://cdn.example.com/shadwick-vega.css")
                .doesNotContainPattern("Shadwick/css/shadwick-vega[^\"]*\\.css");
    }

    @Test
    void addingTheStylesheetToEveryPageCanBeTurnedOff() {
        ShadwickSettings.get(tester.getApplication()).setAddResources(false);
        tester.startPage(PlainPage.class);

        assertThat(tester.getLastResponseAsString()).doesNotContain("shadwick-vega");
    }

    @Test
    void turnedOffTheStylesheetIsStillAddedWhereAComponentIsUsed() {
        ShadwickSettings.get(tester.getApplication()).setAddResources(false);
        tester.startPage(ButtonPage.class);

        assertThat(tester.getLastResponseAsString()).containsOnlyOnce("shadwick-vega");
    }

    @Test
    void theStylesheetIsAddedOnceWithComponentsAndInstall() {
        tester.startPage(ButtonPage.class);

        assertThat(tester.getLastResponseAsString()).containsOnlyOnce("shadwick-vega");
    }

    @Test
    void accessibilityChecksFailInDevelopmentAndWarnInDeployment() {
        // WicketTester's MockApplication runs in development mode
        assertThat(ShadwickSettings.get(tester.getApplication()).getAccessibilityChecks()).isEqualTo(AccessibilityChecks.FAIL);

        WicketTester deployment = new WicketTester(new MockApplication() {
            @Override
            public org.apache.wicket.RuntimeConfigurationType getConfigurationType() {
                return org.apache.wicket.RuntimeConfigurationType.DEPLOYMENT;
            }
        });
        assertThat(ShadwickSettings.get(deployment.getApplication()).getAccessibilityChecks()).isEqualTo(AccessibilityChecks.WARN);
        deployment.destroy();
    }
}

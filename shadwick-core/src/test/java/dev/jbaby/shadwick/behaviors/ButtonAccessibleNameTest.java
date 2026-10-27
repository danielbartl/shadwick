package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.AccessibilityChecks;
import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.ShadwickSettings;
import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwSubmitButton;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.mock.MockApplication;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ButtonAccessibleNameTest {

    // Not a constant: Markup.of needs the tester's application
    private static Markup iconButton() {
        return Markup.of("<button wicket:id=\"button\"><svg></svg></button>");
    }

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
    }

    private static SwButton button(IModel<String> label) {
        return new SwButton("button", label) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                target.add(this);
            }
        };
    }

    @Test
    void anIconOnlyButtonWithoutAnAccessibleNameFailsToRender() {
        SwButton button = button(null).setSize(ButtonBehavior.Size.ICON);

        assertThatIllegalStateException()
                .isThrownBy(() -> tester.startComponentInPage(button, iconButton()))
                .withMessageContaining("The button \"button\" (SwButton on ")
                .withMessageContaining("has no accessible name")
                .withMessageContaining("aria-label")
                .withMessageContaining("ShadwickSettings.setAccessibilityChecks(AccessibilityChecks.OFF)");
    }

    @Test
    void anAriaLabelAddedAfterTheButtonNamesIt() {
        SwButton button = button(null).setSize(ButtonBehavior.Size.ICON);
        button.add(AttributeModifier.replace("aria-label", "Delete"));

        tester.startComponentInPage(button, iconButton());

        assertThat(tester.getTagByWicketId("button").getAttribute("aria-label")).isEqualTo("Delete");
    }

    @Test
    void warningOnlyTheButtonStillRenders() {
        ShadwickSettings.get(tester.getApplication()).setAccessibilityChecks(AccessibilityChecks.WARN);

        tester.startComponentInPage(button(null), Markup.of("<a wicket:id=\"button\"></a>"));

        tester.assertComponent("button", SwButton.class);
    }

    @Test
    void theButtonIsRenderedOnceAndUnchanged() {
        tester.startComponentInPage(button(Model.of("Save")), Markup.of("<a wicket:id=\"button\"></a>"));

        assertThat(tester.getLastResponseAsString()).containsOnlyOnce("wicket:id=\"button\"");
        assertThat(tester.getTagByWicketId("button").getValue()).isEqualTo("Save");
    }

    @Test
    void anAjaxUpdateIsCheckedToo() {
        IModel<String> label = Model.of("Save");
        SwButton button = button(label);
        button.setOutputMarkupId(true);
        tester.startComponentInPage(button, Markup.of("<a wicket:id=\"button\"></a>"));

        tester.clickLink("button");
        assertThat(tester.getLastResponseAsString()).containsOnlyOnce("Save</a>");

        label.setObject("");
        assertThatIllegalStateException().isThrownBy(() -> tester.clickLink("button"))
                .withMessageContaining("has no accessible name");
    }

    @Test
    void anyComponentStyledAsAButtonIsChecked() {
        WebMarkupContainer link = new WebMarkupContainer("link");
        link.add(Sw.Behaviors.button());

        assertThatIllegalStateException()
                .isThrownBy(() -> tester.startComponentInPage(link, Markup.of("<a wicket:id=\"link\" href=\"#\"><svg></svg></a>")))
                .withMessageContaining("The button \"link\" (WebMarkupContainer on ");
    }

    @Test
    void aSubmitInputIsNamedByItsValueOrTheBrowser() {
        Form<Void> form = new Form<>("form");
        form.add(new SwSubmitButton("save"));

        tester.startComponentInPage(form, Markup.of("<form wicket:id=\"form\"><input type=\"submit\" wicket:id=\"save\"></form>"));

        tester.assertComponent("form:save", SwSubmitButton.class);
    }

    @Test
    void theChecksCanBeTurnedOff() {
        ShadwickSettings.get(tester.getApplication()).setAccessibilityChecks(AccessibilityChecks.OFF);

        tester.startComponentInPage(button(null), Markup.of("<a wicket:id=\"button\"></a>"));

        tester.assertComponent("button", SwButton.class);
    }

    @Test
    void theChecksFailInDevelopmentAndOnlyWarnInDeployment() {
        assertThat(ShadwickSettings.get(tester.getApplication()).getAccessibilityChecks()).isEqualTo(AccessibilityChecks.FAIL);

        WicketTester deployment = new WicketTester(new MockApplication() {
            @Override
            public RuntimeConfigurationType getConfigurationType() {
                return RuntimeConfigurationType.DEPLOYMENT;
            }
        });
        assertThat(ShadwickSettings.get(deployment.getApplication()).getAccessibilityChecks()).isEqualTo(AccessibilityChecks.WARN);
        // Logged, not thrown
        deployment.startComponentInPage(button(null), Markup.of("<a wicket:id=\"button\"></a>"));
        deployment.assertComponent("button", SwButton.class);
        deployment.destroy();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            <button>Save</button>                                                    | true
            <button><wicket:message key="save">Save</wicket:message></button>        | true
            <button aria-label="Delete"><svg></svg></button>                         | true
            <button aria-labelledby="delete-label"><svg></svg></button>              | true
            <button title="Delete"><svg></svg></button>                              | true
            <button><svg role="img" aria-label="Delete"></svg></button>              | true
            <button><span class="sr-only">Delete</span><svg></svg></button>          | true
            <button><img src="trash.png" alt="Delete"></button>                      | true
            <button><img src="trash.png" aria-hidden="true"> Delete</button>         | true
            <button><span aria-hidden="true"><span>×</span></span>Delete</button>    | true
            <input type="submit" wicket:id="save">                                   | true
            ''                                                                       | true
            <button></button>                                                        | false
            <button/>                                                                | false
            <button aria-label=" "><svg></svg></button>                              | false
            <button>&nbsp;</button>                                                  | false
            <button> <svg aria-hidden="true"></svg> </button>                        | false
            <button><svg aria-hidden="true"></svg></button>                          | false
            <button><svg aria-hidden="true"><title>Delete</title></svg></button>     | false
            <button><span aria-hidden="true">×</span></button>                       | false
            <button><span hidden>Delete</span></button>                              | false
            <button><img src="trash.png" alt=""></button>                            | false
            <a href="#"><span aria-hidden="true"><b>Delete</b></span></a> Next       | false
            """)
    void accessibleNames(String html, boolean named) {
        assertThat(AccessibleNames.hasName(html == null ? "" : html)).isEqualTo(named);
    }
}

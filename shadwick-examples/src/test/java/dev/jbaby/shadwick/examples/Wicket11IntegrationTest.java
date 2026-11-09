package dev.jbaby.shadwick.examples;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.components.form.SwTextField;
import java.security.SecureRandom;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.core.request.mapper.CryptoMapper;
import org.apache.wicket.core.util.crypt.ApplicationKeyCryptFactory;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.settings.RequestCycleSettings;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.validation.validator.PatternValidator;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Shadwick forms under Wicket 11's authenticated URL mapping and page serialization. */
class Wicket11IntegrationTest {
    WicketTester tester;
    @BeforeEach void start() { tester = new WicketTester(new MigratedApplication()); }
    @AfterEach void stop() { tester.destroy(); }

    @Test void encryptedBookmarkableLinksPreserveParametersCspAndNativeValidation() {
        var parameters = new PageParameters().add("reference", "référence-2026");
        String encrypted = tester.getRequestCycle().urlFor(MigratedPage.class, parameters).toString();
        assertThat(encrypted).doesNotContain("MigratedPage", "reference=", "r%C3%A9f");
        tester.executeUrl(encrypted);
        tester.assertRenderedPage(MigratedPage.class);
        assertThat(tester.getLastRenderedPage().getPageParameters().get("reference").toString()).isEqualTo("référence-2026");
        assertThat(tester.getLastResponse().getHeader("Content-Security-Policy"))
                .contains("'strict-dynamic'", "'nonce-").doesNotContain("'unsafe-inline'", "'unsafe-eval'");
        String inputId = ((SwTextField<?>) tester.getComponentFromLastRenderedPage("form:code")).getInput().getMarkupId();
        var input = TagTester.createTagByAttribute(tester.getLastResponseAsString(), "id", inputId);
        assertThat(input.getAttribute("required")).isNull();
        assertThat(input.getAttribute("pattern")).isNull();
        var form = tester.newFormTester("form");
        form.setValue("code:input", "invalid");
        form.submit("nativeSave");
        assertThat(tester.getLastResponseAsString()).contains("aria-invalid=\"true\"");
        tester.assertLabel("receipt", "Nothing saved.");
        form = tester.newFormTester("form");
        form.setValue("code:input", "VALID");
        form.submit("nativeSave");
        tester.assertNoErrorMessage();
        tester.assertLabel("receipt", "Saved VALID.");
    }

    @Test void serializedPagesKeepAjaxValidationCallbacksUnderEncryptedMapping() {
        tester.startPage(MigratedPage.class);
        var serializer = tester.getApplication().getFrameworkSettings().getSerializer();
        tester.startPage((WebPage) serializer.deserialize(serializer.serialize(tester.getLastRenderedPage())));
        tester.executeAjaxEvent("form:ajaxSave", "click");
        assertThat(tester.getLastResponseAsString()).contains("Shadwick.fieldFeedback", "is required");
        tester.newFormTester("form").setValue("code:input", "VALID");
        tester.executeAjaxEvent("form:ajaxSave", "click");
        tester.assertNoErrorMessage();
        tester.assertLabel("receipt", "Saved VALID.");
        tester.assertComponentOnAjaxResponse("receipt");
    }

    public static class MigratedApplication extends WebApplication {
        @Override public Class<? extends Page> getHomePage() { return LandingPage.class; }
        @Override protected void init() {
            super.init();
            Shadwick.install(this);
            getMarkupSettings().setStripWicketTags(true);
            getRequestCycleSettings().setRenderStrategy(RequestCycleSettings.RenderStrategy.ONE_PASS_RENDER);
            getSecuritySettings().setCryptFactory(new ApplicationKeyCryptFactory(new SecureRandom()));
            setRootRequestMapper(new CryptoMapper(getRootRequestMapper(), this));
        }
    }
    public static class LandingPage extends WebPage {}

    public static class MigratedPage extends WebPage implements IMarkupResourceStreamProvider {
        private final IModel<String> code = Model.of(""), receipt = Model.of("Nothing saved.");
        public MigratedPage(PageParameters parameters) { super(parameters); }
        @Override protected void onInitialize() {
            super.onInitialize();
            var status = new Label("receipt", receipt).setOutputMarkupId(true);
            add(status, profileForm(status));
        }
        private Form<Void> profileForm(org.apache.wicket.Component status) {
            var form = new Form<Void>("form") {
                @Override protected void onSubmit() { receipt.setObject("Saved " + code.getObject() + "."); }
            };
            form.add(new SwTextField<>("code", "Reference", code).setRequired(true).addValidator(new PatternValidator("[A-Z]+")));
            form.add(new Button("nativeSave"));
            form.add(SwSubmitButton.onSubmit("ajaxSave", "Save", target -> target.add(status)));
            return form;
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><body><p wicket:id='receipt'></p><form wicket:id='form'><div wicket:id='code'></div>"
                    + "<button wicket:id='nativeSave'>Save</button><button wicket:id='ajaxSave'>Save with Ajax</button></form></body></html>");
        }
    }
}

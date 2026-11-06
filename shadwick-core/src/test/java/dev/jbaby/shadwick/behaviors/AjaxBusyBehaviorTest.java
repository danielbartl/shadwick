package dev.jbaby.shadwick.behaviors;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.components.*;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.mock.MockApplication;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import java.util.Locale;
import static org.assertj.core.api.Assertions.*;

class AjaxBusyBehaviorTest {
    WicketTester tester;
    @BeforeEach void setup() { tester = new WicketTester(new MockApplication() {
        @Override protected void init() { super.init(); Shadwick.install(this); Shadwick.install(this); }
    }); }
    @AfterEach void cleanup() { tester.destroy(); }
    public static class BusyPage extends WebPage implements IMarkupResourceStreamProvider {
        int clicks;
        public BusyPage() {
            add(SwButton.onClick("busy", "Save", target -> clicks++).setBusyIndicator(true));
            add(SwButton.onClick("ordinary", "Ordinary", target -> clicks++));
            Form<Void> form = new Form<>("form"); form.add(SwSubmitButton.onSubmit("save", "Save", target -> clicks++).setBusyIndicator(true)); add(form);
            add(new SwDialog("dialog", "Confirm").setTrigger("Open").setBusyIndicator(true));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> type) {
            return new StringResourceStream("<html><head></head><body><button wicket:id='busy'></button><button wicket:id='ordinary'></button><form wicket:id='form'><button wicket:id='save'></button></form><div wicket:id='dialog'></div></body></html>");
        }
    }
    @Test void installerRegistersOneListenerAndOnlyOptedInRequestsGetTheLifecycle() {
        int listeners = 0; for (AjaxRequestTarget.IListener listener : tester.getApplication().getAjaxRequestTargetListeners())
            if (listener instanceof AjaxBusyBehavior.Listener) listeners++;
        assertThat(listeners).isEqualTo(1);
        tester.getSession().setLocale(Locale.forLanguageTag("hr")); BusyPage page = tester.startPage(BusyPage.class);
        String html = tester.getLastResponseAsString();
        assertThat(html).contains("busy.js", "Shadwick.busy.start(attrs)", "Shadwick.busy.done(attrs)", "return !attrs.swBusyRejected", "data-sw-busy-label=\"Učitavanje\"");
        assertThat(tester.getTagByWicketId("ordinary").getAttribute("data-sw-busy-label")).isNull();
        tester.clickLink("busy"); assertThat(page.clicks).isEqualTo(1);
    }
    @Test void fluentSettersAreIdempotentAndCanBeTurnedOff() {
        SwButton button = SwButton.onClick("button", "Save", target -> {}).setBusyIndicator(true).setBusyIndicator(true);
        assertThat(button.getBehaviors(AjaxBusyBehavior.class)).hasSize(1);
        button.setBusyIndicator(false); assertThat(button.getBehaviors(AjaxBusyBehavior.class)).isEmpty();
        SwSubmitButton submit = SwSubmitButton.onSubmit("save", "Save", target -> {}).setBusyIndicator(true).setBusyIndicator(true);
        assertThat(submit.getBehaviors(AjaxBusyBehavior.class)).hasSize(1); submit.setBusyIndicator(false);
        assertThat(submit.getBehaviors(AjaxBusyBehavior.class)).isEmpty();
    }
}

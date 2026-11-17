package dev.jbaby.shadwick.components;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/** A list beside the selected item's details: selecting, going back, the one-pane switch and plain links. */
class SwMasterDetailTest {
    WicketTester tester;

    @BeforeEach void start() { tester = new WicketTester(); tester.getSession().setLocale(Locale.US); }
    @AfterEach void stop() {
        if (tester.getLastResponse() != null)
            assertThat(tester.getLastResponseAsString()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        tester.destroy();
    }

    public static class MasterDetailPage extends WebPage implements IMarkupResourceStreamProvider {
        public final SwMasterDetail<String> customers = new SwMasterDetail<>("customers", new Model<>());

        public MasterDetailPage() {
            add(customers);
            customers.setMaster(id -> {
                Fragment list = new Fragment(id, "list", this);
                list.add(new ListView<>("items", List.of("ACME", "Globex")) {
                    @Override
                    protected void populateItem(ListItem<String> item) {
                        item.add(new AjaxLink<Void>("link") {
                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                customers.select(target, item.getModelObject());
                            }
                        }.setBody(item.getModel()));
                        // The same choice without JavaScript
                        item.add(new Link<Void>("plain") {
                            @Override
                            public void onClick() {
                                customers.select(item.getModelObject());
                            }
                        }.setBody(item.getModel()));
                    }
                });
                return list;
            });
            customers.setDetail((id, customer) -> new Label(id, () -> "Details of " + customer.getObject()));
        }

        @Override
        public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><body><div wicket:id='customers'></div>"
                    + "<wicket:fragment wicket:id='list'><ul><li wicket:id='items'><a wicket:id='link'></a><a wicket:id='plain'></a></li></ul></wicket:fragment>"
                    + "</body></html>");
        }
    }

    @Test void selectingShowsTheDetailsAndBackTheList() {
        MasterDetailPage page = tester.startPage(MasterDetailPage.class);
        TagTester root = tester.getTagByWicketId("customers");
        assertThat(root.getAttribute("class")).isEqualTo("sw-master-detail");
        assertThat(root.getAttribute("data-showing")).isEqualTo("master");
        assertThat(tester.getLastResponseAsString()).contains("Select an item to see its details.");

        tester.clickLink("customers:master:items:1:link");
        assertThat(page.customers.getSelection().getObject()).isEqualTo("Globex");
        assertThat(page.customers.isSelected("Globex")).isTrue();
        // The detail is re-rendered and the pane switch runs, without re-rendering the list
        assertThat(tester.getLastResponseAsString()).contains("Details of Globex")
                .contains("Shadwick.masterDetail(\"" + page.customers.getMarkupId() + "\",\"detail\")")
                .doesNotContain("wicket:id=\"items\"");

        tester.clickLink("customers:detailArea:back");
        assertThat(tester.getLastResponseAsString()).contains("Shadwick.masterDetail(\"" + page.customers.getMarkupId() + "\",\"master\")");
        assertThat(page.customers.getSelection().getObject()).isEqualTo("Globex");
    }

    @Test void withoutJavaScriptTheLinksRenderThePaneToShow() {
        MasterDetailPage page = tester.startPage(MasterDetailPage.class);
        tester.clickLink("customers:master:items:0:plain");
        assertThat(page.customers.getSelection().getObject()).isEqualTo("ACME");
        assertThat(tester.getTagByWicketId("customers").getAttribute("data-showing")).isEqualTo("detail");
        assertThat(tester.getLastResponseAsString()).contains("Details of ACME");
        tester.clickLink("customers:detailArea:back", false);
        assertThat(tester.getTagByWicketId("customers").getAttribute("data-showing")).isEqualTo("master");
        assertThat(tester.getLastResponseAsString()).contains("Details of ACME");
    }

    @Test void aSelectionSetThroughTheModelAlsoShowsItsDetails() {
        MasterDetailPage page = new MasterDetailPage();
        page.customers.getSelection().setObject("Globex");
        tester.startPage(page);
        assertThat(tester.getLastResponseAsString()).contains("Details of Globex");
    }

    @Test void factoriesMustUseTheIdTheyAreGiven() {
        SwMasterDetail<String> md = new SwMasterDetail<>("md", new Model<>());
        assertThatIllegalArgumentException().isThrownBy(() -> md.setMaster(id -> new Label("other")));
        md.getSelection().setObject("x");
        assertThatIllegalArgumentException().isThrownBy(() -> md.setDetail((id, item) -> new Label("other")));
    }

    @Test void theBackLabelAndEmptyStateAreTranslatedAndThePageSerializes() throws Exception {
        tester.getSession().setLocale(Locale.GERMAN);
        tester.startPage(MasterDetailPage.class);
        assertThat(tester.getLastResponseAsString()).contains("Zurück", "Wählen Sie einen Eintrag");
        try (var out = new java.io.ObjectOutputStream(new java.io.ByteArrayOutputStream())) {
            out.writeObject(tester.getLastRenderedPage());
        }
    }
}

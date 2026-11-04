package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.components.form.SwTextField;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SwSheetTest {
    private WicketTester tester;
    @BeforeEach void setUp() { tester = new WicketTester(); }
    @AfterEach void tearDown() { tester.destroy(); }

    public static class SheetPage extends WebPage implements IMarkupResourceStreamProvider {
        final Model<String> name = Model.of("Ada");
        int saved, closed;
        final SwSheet sheet = new SwSheet("sheet", "Edit profile") {
            @Override protected void onConfirm(AjaxRequestTarget target) { saved++; }
            @Override protected void onClose(AjaxRequestTarget target) { closed++; }
        };
        public SheetPage() {
            var fields = new Fragment(SwSheet.CONTENT_ID, "fields", this);
            fields.add(new SwTextField<String>("name", "Name", name).setRequired(true));
            sheet.setDescription("Update your profile.").setContent(fields).setConfirmLabel("Save").setTrigger("Edit");
            add(sheet);
            add(SwButton.onClick("open", "Open", sheet::open));
            add(SwButton.onClick("refresh", "Refresh", target -> target.add(sheet)));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><body><div wicket:id='sheet'></div><button wicket:id='open'></button>"
                    + "<button wicket:id='refresh'></button><wicket:fragment wicket:id='fields'>"
                    + "<div wicket:id='name'></div></wicket:fragment></body></html>");
        }
    }

    private String html() { return tester.getLastResponseAsString(); }
    private TagTester dialog() { return TagTester.createTagByName(html(), "dialog"); }

    @Test void itIsANamedNativeDialogAtTheLogicalEndWithAnAccessibleCloseButton() {
        tester.startPage(SheetPage.class);
        assertThat(dialog().getAttribute("class")).isEqualTo("sw-sheet");
        assertThat(dialog().getAttribute("data-side")).isEqualTo("end");
        assertThat(dialog().getAttribute("closedby")).isEqualTo("any");
        assertThat(dialog().getAttribute("aria-labelledby")).isEqualTo(tester.getTagByWicketId("title").getAttribute("id"));
        assertThat(dialog().getAttribute("aria-describedby")).isEqualTo(tester.getTagByWicketId("description").getAttribute("id"));
        TagTester close = tester.getTagByWicketId("close");
        assertThat(close.getAttribute("class")).contains("sw-sheet-close", "sw-button");
        assertThat(close.getAttribute("aria-label")).isEqualTo("Close");
        assertThat(close.getAttribute("commandfor")).isEqualTo(dialog().getAttribute("id"));
        assertThat(html()).contains("sw-sheet-body-content", "\"d\":250")
                .doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
    }

    @ParameterizedTest @EnumSource(SwSheet.Side.class)
    void everyEdgeIsSupported(SwSheet.Side side) {
        SheetPage page = new SheetPage();
        page.sheet.setSide(side);
        tester.startPage(page);
        assertThat(dialog().getAttribute("data-side")).isEqualTo(side.name().toLowerCase(java.util.Locale.ROOT));
        assertThatNullPointerException().isThrownBy(() -> page.sheet.setSide(null));
    }

    @Test void ajaxValidationKeepsTheSheetOpenAndValidConfirmationUpdatesTheModelAndCloses() {
        SheetPage page = tester.startPage(SheetPage.class);
        tester.clickLink("open");
        tester.newFormTester("sheet:dialog:form").setValue("content:name:input", "");
        tester.executeAjaxEvent("sheet:dialog:form:confirm", "click");
        assertThat(page.sheet.isOpen()).isTrue();
        assertThat(page.saved).isZero();
        assertThat(html()).contains("Shadwick.fieldFeedback(", "is required.").doesNotContain(".close()");
        tester.newFormTester("sheet:dialog:form").setValue("content:name:input", "Grace");
        tester.executeAjaxEvent("sheet:dialog:form:confirm", "click");
        assertThat(page.name.getObject()).isEqualTo("Grace");
        assertThat(page.saved).isEqualTo(1);
        assertThat(page.closed).isEqualTo(1);
        assertThat(page.sheet.isOpen()).isFalse();
    }

    @Test void browserCloseClearsUnconfirmedInputAndReopeningKeepsTheModel() {
        SheetPage page = tester.startPage(SheetPage.class);
        tester.clickLink("open");
        tester.newFormTester("sheet:dialog:form").setValue("content:name:input", "");
        tester.executeAjaxEvent("sheet:dialog:form:confirm", "click");
        tester.executeAjaxEvent("sheet:dialog", "close");
        assertThat(page.closed).isEqualTo(1);
        assertThat(page.sheet.isOpen()).isFalse();
        assertThat(page.name.getObject()).isEqualTo("Ada");
        assertThat(page.sheet.getForm().get("content:name:input").getDefaultModelObject()).isEqualTo("Ada");
        tester.clickLink("open");
        assertThat(html()).contains("value=\"Ada\"", "Shadwick.showDialog(");
    }

    @Test void refreshingAnOpenSheetKeepsItModalAndFooterlessSheetsRetainTheirCloseButton() {
        SheetPage page = tester.startPage(SheetPage.class);
        tester.clickLink("open");
        tester.clickLink("refresh");
        assertThat(dialog().hasAttribute("data-sw-instant")).isTrue();
        assertThat(html()).contains("Shadwick.showDialog(");
        page.sheet.setFooterVisible(false);
        tester.startPage(page);
        tester.assertInvisible("sheet:dialog:form:footer");
        tester.assertVisible("sheet:dialog:form:close");
        assertThat(html()).doesNotContain("sw-sheet-footer");
    }
}

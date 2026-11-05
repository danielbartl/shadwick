package dev.jbaby.shadwick.components;

import dev.jbaby.shadwick.Shadwick;
import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.*;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.mock.MockApplication;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

class SwAttachmentTest {
    WicketTester tester;
    @BeforeEach void before() {
        tester = new WicketTester(new MockApplication() {
            @Override protected void init() { super.init(); Shadwick.install(this); getMarkupSettings().setStripWicketTags(true); }
        });
    }
    @AfterEach void after() { tester.destroy(); }
    String html() { return tester.getLastResponseAsString(); }
    void render(SwAttachment file) { tester.startComponentInPage(file, Markup.of("<div wicket:id='file'></div>")); }
    @Test void defaultsEscapeTextAndOmitUnusedPartsAndState() {
        render(new SwAttachment("file", "<Notes>.txt").setDescription("A <file>"));
        assertThat(html()).contains("sw-attachment", "&lt;Notes&gt;.txt", "A &lt;file&gt;")
                .doesNotContain("data-state=", "aria-busy=", "data-size=", "data-orientation=", "sw-attachment-actions", "sw-attachment-trigger", "<img", "style=", "role=\"alert\"", "aria-live=");
    }
    @ParameterizedTest @EnumSource(AttachmentBehavior.State.class)
    void statesArePresentationWithBusyOnlyWhileWorking(AttachmentBehavior.State state) {
        var file = new SwAttachment("file", "Notes.txt").setDescription("Status in text").setState(state); render(file);
        assertThat(file.getState()).isEqualTo(state);
        if (state == AttachmentBehavior.State.DONE) assertThat(html()).doesNotContain("data-state=");
        else assertThat(html()).contains("data-state=\"" + state.name().toLowerCase(java.util.Locale.ROOT) + "\"");
        if (state == AttachmentBehavior.State.UPLOADING || state == AttachmentBehavior.State.PROCESSING) assertThat(html()).contains("aria-busy=\"true\"");
        else assertThat(html()).doesNotContain("aria-busy=");
        assertThat(html()).doesNotContain("aria-live=", "role=\"alert\"");
        file.setState(AttachmentBehavior.State.DONE); render(file); assertThat(html()).doesNotContain("data-state=", "aria-busy=");
    }
    @ParameterizedTest @EnumSource(AttachmentBehavior.Size.class)
    void sizesAndVerticalFullWidthAreIndependent(AttachmentBehavior.Size size) {
        render(new SwAttachment("file", "Notes").setSize(size).setOrientation(AttachmentBehavior.Orientation.VERTICAL).setFullWidth(true));
        assertThat(html()).contains("data-orientation=\"vertical\"", "data-width=\"full\"");
        if (size == AttachmentBehavior.Size.DEFAULT) assertThat(html()).doesNotContain("data-size=");
        else assertThat(html()).contains("data-size=\"" + (size == AttachmentBehavior.Size.SMALL ? "sm" : "xs") + "\"");
    }
    @Test void thumbnailsAreDecorativeBesideFilenameAndMediaOnlyFallbackIsNamed() {
        var file = new SwAttachment("file", "Green hills").setImageUrl("/hills.svg"); render(file);
        assertThat(html()).contains("data-variant=\"image\"", "src=\"/hills.svg\"", "alt=\"\"", "sw-attachment-fallback");
        file.setShowContent(false); render(file);
        assertThat(html()).contains("alt=\"Green hills\"", "role=\"img\"", "aria-label=\"Green hills\"").doesNotContain("sw-attachment-content");
        file.setImageAlt("A winding trail"); render(file); assertThat(html()).contains("alt=\"A winding trail\"", "aria-label=\"A winding trail\"");
        file.setImageUrl(" "); render(file); assertThat(html()).doesNotContain("<img", "data-variant=\"image\"");
    }
    @Test void applicationLinksAreNativeNamedLastAndCanBeDisabled() {
        var file = new SwAttachment("file", "Notes").setLink("/files/notes?download=1&v=2", "Open Notes"); render(file);
        assertThat(html()).contains("href=\"/files/notes?download=1&amp;v=2\"", "aria-label=\"Open Notes\"");
        assertThat(html().indexOf("sw-attachment-trigger")).isGreaterThan(html().indexOf("sw-attachment-title"));
        file.setEnabled(false); render(file);
        assertThat(html()).contains("aria-disabled=\"true\"", "tabindex=\"-1\"").doesNotContain("href=\"/files/");
    }
    @Test void nativePreviewButtonTargetsTheActualDialogAndDoesNotSubmit() {
        var dialog = new WebMarkupContainer("dialog"); dialog.setMarkupId("preview-dialog");
        render(new SwAttachment("file", "Picture").setPreview(dialog, "Preview Picture"));
        assertThat(html()).contains("<button", "commandfor=\"preview-dialog\"", "command=\"show-modal\"", "type=\"button\"", "aria-label=\"Preview Picture\"");
    }
    @Test void arbitraryMediaActionsAndTriggersKeepTheirModelsAndRejectWrongSlotIds() {
        var file = new SwAttachment("file", "Notes").setMedia(new Label(SwAttachment.MEDIA_ID, "Custom media"))
                .setActions(new Label(SwAttachment.ACTIONS_ID, "Download actions")); render(file);
        assertThat(html()).contains("Custom media", "Download actions", "sw-attachment-actions").doesNotContain("sw-attachment-fallback");
        assertThatIllegalArgumentException().isThrownBy(() -> file.setMedia(new Label("wrong")));
        assertThatIllegalArgumentException().isThrownBy(() -> file.setActions(new Label("wrong")));
        assertThatIllegalArgumentException().isThrownBy(() -> file.setTrigger(new Label("wrong")));
    }
    @Test void titleIsLoadedOncePerRequestAndDetached() {
        AtomicInteger reads = new AtomicInteger(), detaches = new AtomicInteger();
        IModel<String> title = new IModel<>() {
            @Override public String getObject() { reads.incrementAndGet(); return "Notes"; }
            @Override public void detach() { detaches.incrementAndGet(); }
        };
        var file = new SwAttachment("file", title).setShowContent(false); render(file);
        assertThat(reads.get()).isEqualTo(1); assertThat(detaches.get()).isEqualTo(1);
        render(file); assertThat(reads.get()).isEqualTo(2); assertThat(detaches.get()).isEqualTo(2);
    }
    @Test void unnamedTriggersAndActionsFailTheDevelopmentAccessibilityCheck() {
        assertThatThrownBy(() -> render(new SwAttachment("file", "Notes").setLink("/notes", "")))
                .hasStackTraceContaining("has no accessible name");
    }
    @Test void actionBehaviorReusesAnExistingButtonBehaviorAndKeepsNativeDisabledSemantics() {
        var action = new WebMarkupContainer("action");
        action.add(new ButtonBehavior(ButtonBehavior.Variant.OUTLINE));
        action.add(Sw.Behaviors.attachmentAction().setLabel("Remove Notes")); action.setEnabled(false);
        tester.startComponentInPage(action, Markup.of("<button wicket:id='action'></button>"));
        assertThat(action.getBehaviors(ButtonBehavior.class)).hasSize(1);
        assertThat(html()).contains("sw-attachment-action", "aria-label=\"Remove Notes\"", "disabled=\"disabled\"", "type=\"button\"", "data-variant=\"outline\"");
    }
    @Test void plainActionAddsOneCompactGhostButtonAndUnnamedActionsFail() {
        var action = new WebMarkupContainer("action"); action.add(Sw.Behaviors.attachmentAction().setLabel("Download Notes"));
        tester.startComponentInPage(action, Markup.of("<a wicket:id='action' href='/notes' download='notes.txt'></a>"));
        assertThat(html()).contains("data-variant=\"ghost\"", "data-size=\"icon\"", "download=\"notes.txt\"");
        var unnamed = new WebMarkupContainer("action"); unnamed.add(Sw.Behaviors.attachmentAction());
        assertThatThrownBy(() -> tester.startComponentInPage(unnamed, Markup.of("<button wicket:id='action'></button>")))
                .hasStackTraceContaining("has no accessible name");
    }
    @Test void groupLabelMakesOnlyRowsFocusableAndModelsFollowRenders() {
        var label = Model.of("Project files"); var group = new SwAttachmentGroup("group").setLabel(label);
        tester.startComponentInPage(group, Markup.of("<div wicket:id='group'></div>"));
        assertThat(html()).contains("role=\"group\"", "aria-label=\"Project files\"", "tabindex=\"0\"").doesNotContain("data-layout=");
        group.setLayout(AttachmentGroupBehavior.Layout.GRID); label.setObject("Gallery");
        tester.startComponentInPage(group, Markup.of("<div wicket:id='group'></div>"));
        assertThat(html()).contains("data-layout=\"grid\"", "aria-label=\"Gallery\"").doesNotContain("tabindex=");
        label.setObject(" "); tester.startComponentInPage(group, Markup.of("<div wicket:id='group'></div>"));
        assertThat(html()).doesNotContain("role=\"group\"", "tabindex=");
    }
    @Test void callerMarkupCanSupplyTheGroupAccessibleNameAndAttachmentBehavior() {
        var group = new WebMarkupContainer("group"); group.add(Sw.Behaviors.attachmentGroup());
        tester.startComponentInPage(group, Markup.of("<div wicket:id='group' aria-labelledby='files-title'></div>"));
        assertThat(html()).contains("role=\"group\"", "tabindex=\"0\"", "aria-labelledby=\"files-title\"");
        var file = new WebMarkupContainer("file"); file.add(Sw.Behaviors.attachment().setState(AttachmentBehavior.State.ERROR));
        tester.startComponentInPage(file, Markup.of("<article wicket:id='file'><p>Upload failed</p></article>"));
        assertThat(html()).contains("<article", "sw-attachment", "data-state=\"error\"", "Upload failed");
    }
    @Test void optionalModelsLoadOnceAndDetachAfterEveryRender() {
        var reads = new AtomicInteger(); var detaches = new AtomicInteger();
        var file = new SwAttachment("file", "Landscape").setDescription(tracked("Illustration", reads, detaches))
                .setImageUrl(tracked("/picture.svg", reads, detaches)).setImageAlt(tracked("Green hills", reads, detaches));
        render(file); assertThat(reads.get()).isEqualTo(3); assertThat(detaches.get()).isEqualTo(3);
        render(file); assertThat(reads.get()).isEqualTo(6); assertThat(detaches.get()).isEqualTo(6);
    }
    private static IModel<String> tracked(String value, AtomicInteger reads, AtomicInteger detaches) {
        return new IModel<>() {
            @Override public String getObject() { reads.incrementAndGet(); return value; }
            @Override public void detach() { detaches.incrementAndGet(); }
        };
    }

    @Test void aNullAltModelKeepsInlineThumbnailsDecorativeAndCompactFallbacksNamed() {
        var file = new SwAttachment("file", "Landscape").setImageUrl("/picture.svg").setImageAlt(Model.of((String) null));
        render(file); assertThat(html()).contains("alt=\"\"");
        file.setShowContent(false); render(file);
        assertThat(html()).contains("alt=\"Landscape\"", "aria-label=\"Landscape\"", "aria-hidden=\"true\"");
    }

}

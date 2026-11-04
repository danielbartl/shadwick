package dev.jbaby.shadwick.components.form;

import dev.jbaby.shadwick.components.SwButton;
import dev.jbaby.shadwick.components.SwSubmitButton;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class FileUploadFieldsTest {
    enum Kind { NATIVE, DROPZONE }
    private WicketTester tester;
    @TempDir Path directory;

    @BeforeEach void setUp() { tester = new WicketTester(); }
    @AfterEach void tearDown() {
        if (tester.getLastResponse() != null) {
            assertThat(html()).doesNotContain("style=").doesNotContainPattern("\\son[a-z]+=");
        }
        tester.destroy();
    }
    private String html() { return tester.getLastResponseAsString(); }
    private BaseSwFileUpload<?> field(Kind kind) {
        return kind == Kind.NATIVE
                ? new SwFileUpload("files", "Attachments", Model.ofList(new ArrayList<>()))
                : new SwFileDropzone("files", "Attachments", Model.ofList(new ArrayList<>()));
    }
    private FormTester form(BaseSwFileUpload<?> field, List<String> received) {
        Form<Void> form = new Form<>("form") {
            @Override
            protected void onSubmit() {
                if (field.getModelObject() != null) field.getModelObject().forEach(file ->
                        received.add(file.getClientFileName() + ":" + new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8)));
            }
        };
        form.setMultiPart(true);
        form.add(field);
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='files'></div></form>"));
        return tester.newFormTester("form");
    }
    private org.apache.wicket.util.file.File file(String name, String content) throws Exception {
        Path path = directory.resolve(name);
        Files.writeString(path, content);
        return new org.apache.wicket.util.file.File(path.toString());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void uploadsAreNativeMultipartFieldsWithLabelsAndDescriptions(Kind kind) {
        var field = field(kind).setDescription("Up to three documents.").setAccept(".txt").setMaxFiles(3);
        form(field, new ArrayList<>());
        var input = tester.getTagByWicketId("input");
        assertThat(input.getAttribute("type")).isEqualTo("file");
        assertThat(input.getAttribute("multiple")).isNotNull();
        assertThat(input.getAttribute("accept")).isEqualTo(".txt");
        assertThat(input.getAttribute("value")).isNull();
        assertThat(tester.getTagByWicketId("label").getAttribute("for")).isEqualTo(input.getAttribute("id"));
        assertThat(input.getAttribute("aria-describedby")).contains(tester.getTagByWicketId("description").getAttribute("id"));
        assertThat(tester.getTagByWicketId("form").getAttribute("enctype")).isEqualTo("multipart/form-data");
        assertThat(field.getInput().isMultiPart()).isTrue();
        if (kind == Kind.DROPZONE) {
            assertThat(input.getAttribute("class")).isEqualTo("sw-file-upload-input");
            assertThat(input.getAttribute("hidden")).isNull();
            assertThat(tester.getTagByWicketId("browse").getName()).isEqualTo("label");
            assertThat(tester.getTagByWicketId("browse").getAttribute("for")).isEqualTo(input.getAttribute("id"));
            assertThat(tester.getTagByWicketId("upload").getAttribute("data-max-files")).isEqualTo("3");
            assertThat(html()).doesNotContain("blob:", "x-data=");
        }
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void multipartFilesAreReadableInTheCallbackAndClearedOnDetach(Kind kind) throws Exception {
        var field = field(kind);
        List<String> received = new ArrayList<>();
        FormTester form = form(field, received);
        form.setFile("files:input", file("one.txt", "alpha"), "text/plain");
        form.setFile("files:input", file("two.txt", "beta"), "text/plain");
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(received).containsExactly("one.txt:alpha", "two.txt:beta");
        assertThat(field.getModelObject()).isNull();
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void requiredEmptySelectionsFailButOptionalEmptySelectionsSucceed(Kind kind) {
        var field = field(kind).setRequired(true);
        form(field, new ArrayList<>()).submit();
        assertThat(tester.getTagByWicketId("input").getAttribute("aria-invalid")).isEqualTo("true");
        assertThat(tester.getTagByWicketId("error").getValue()).contains("required");
        field.setRequired(false);
        tester.newFormTester("form").submit();
        tester.assertNoErrorMessage();
        assertThat(tester.getTagByWicketId("error").getValue()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void singleModeRejectsForgedMultipleUploadsOnTheServer(Kind kind) throws Exception {
        var field = field(kind).setMultiple(false);
        List<String> received = new ArrayList<>();
        FormTester form = form(field, received);
        assertThat(tester.getTagByWicketId("input").getAttribute("multiple")).isNull();
        form.setFile("files:input", file("one.txt", "a"), "text/plain");
        form.setFile("files:input", file("two.txt", "b"), "text/plain");
        form.submit();
        assertThat(received).isEmpty();
        assertThat(tester.getTagByWicketId("error").getValue()).contains("at most 1");
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void configuredCountAndSizeLimitsRejectFilesOnTheServer(Kind kind) throws Exception {
        var field = field(kind).setMaxFiles(1).setMaxFileSize(Bytes.bytes(3));
        List<String> received = new ArrayList<>();
        FormTester form = form(field, received);
        form.setFile("files:input", file("large.txt", "large"), "text/plain");
        form.submit();
        assertThat(received).isEmpty();
        assertThat(tester.getTagByWicketId("error").getValue()).contains("at most 3 bytes");
        form = tester.newFormTester("form");
        form.setFile("files:input", file("one.txt", "a"), "text/plain");
        form.setFile("files:input", file("two.txt", "b"), "text/plain");
        form.submit();
        assertThat(tester.getTagByWicketId("error").getValue()).contains("at most 1");
        assertThat(received).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void aNamedZeroByteFileSatisfiesRequired(Kind kind) throws Exception {
        List<String> received = new ArrayList<>();
        FormTester form = form(field(kind).setRequired(true), received);
        form.setFile("files:input", file("empty.txt", ""), "text/plain");
        form.submit();
        tester.assertNoErrorMessage();
        assertThat(received).containsExactly("empty.txt:");
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void disabledUploadsIgnoreForgedFileParts(Kind kind) throws Exception {
        var field = field(kind);
        field.setEnabled(false);
        List<String> received = new ArrayList<>();
        FormTester form = form(field, received);
        form.setFile("files:input", file("one.txt", "a"), "text/plain");
        form.submit();
        assertThat(received).isEmpty();
        assertThat(tester.getTagByWicketId("input").getAttribute("disabled")).isNotNull();
    }

    public static class Settings implements Serializable { public List<FileUpload> files; }

    @Test
    void inheritedUploadModelsArePopulatedOnlyDuringTheRequest() throws Exception {
        Settings settings = new Settings();
        List<String> received = new ArrayList<>();
        Form<Settings> form = new Form<>("form", new CompoundPropertyModel<>(settings)) {
            @Override protected void onSubmit() { received.add(settings.files.get(0).getClientFileName()); }
        };
        form.setMultiPart(true);
        form.add(new SwFileUpload("files", "Documents"));
        tester.startComponentInPage(form, Markup.of("<form wicket:id='form'><div wicket:id='files'></div></form>"));
        FormTester ft = tester.newFormTester("form");
        ft.setFile("files:input", file("inherited.txt", "data"), "text/plain");
        ft.submit();
        assertThat(received).containsExactly("inherited.txt");
        assertThat(settings.files).isNull();
    }

    public static class AjaxPage extends WebPage implements IMarkupResourceStreamProvider {
        final SwFileDropzone files = new SwFileDropzone("files", "Documents", Model.ofList(new ArrayList<>())).setRequired(true);
        public AjaxPage() {
            Form<Void> form = new Form<>("form");
            form.setMultiPart(true);
            form.add(files);
            form.add(SwSubmitButton.onSubmit("save", "Save", target -> files.clearSelection(target)));
            add(form);
            add(SwButton.onClick("elsewhere", "Elsewhere", target -> {}));
        }
        @Override public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><head></head><body><form wicket:id='form'><div wicket:id='files'></div>"
                    + "<button wicket:id='save'></button></form><button wicket:id='elsewhere'></button></body></html>");
        }
    }

    @Test
    void ajaxUploadErrorsRemainAfterAnUnrelatedRequest() {
        tester.startPage(new AjaxPage());
        tester.executeAjaxEvent("form:save", "click");
        assertThat(html()).contains("Shadwick.fieldFeedback(", "required");
        tester.clickLink("elsewhere");
        assertThat(html()).doesNotContain("Shadwick.fieldFeedback(");
    }

    @Test
    void invalidLimitsAreRejectedAndNullRemovesTheSizeLimit() {
        var field = new SwFileUpload("files", Model.ofList(new ArrayList<>()));
        assertThatIllegalArgumentException().isThrownBy(() -> field.setMaxFiles(0));
        assertThatIllegalArgumentException().isThrownBy(() -> field.setMaxFileSize(Bytes.bytes(0)));
        field.setMaxFileSize(Bytes.bytes(1)).setMaxFileSize(null);
        form(field, new ArrayList<>());
    }
}

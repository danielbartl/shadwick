package dev.jbaby.shadwick.examples;

import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.StringWriter;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/** Compile the exact Java printed in the docs, then validate the matching HTML and form behavior. */
class DocumentationExamplesTest {
    @TempDir static Path classes;
    static URLClassLoader loader;
    WicketTester tester;
    ClassLoader previousLoader;

    @BeforeAll static void compileExamples() throws Exception {
        Path sources=Files.isDirectory(Path.of("docs/examples"))?Path.of("docs/examples"):Path.of("../docs/examples");
        var compiler=ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).as("Documentation examples require the build JDK").isNotNull();
        var errors=new StringWriter();
        try(var manager=compiler.getStandardFileManager(null,null,null);var files=Files.list(sources)) {
            var javaFiles=files.filter(p->p.toString().endsWith(".java")).map(Path::toFile).toList();
            var units=manager.getJavaFileObjectsFromFiles(javaFiles);
            var options=List.of("--release","21","-proc:none","-classpath",System.getProperty("surefire.test.class.path",System.getProperty("java.class.path")),"-d",classes.toString());
            assertThat(compiler.getTask(errors,manager,null,options,null,units).call()).as(errors.toString()).isTrue();
        }
        Files.copy(sources.resolve("QuickStartPage.html"),classes.resolve("dev/example/QuickStartPage.html"));
        loader=new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()},DocumentationExamplesTest.class.getClassLoader());
    }
    @AfterAll static void closeLoader() throws Exception { if(loader!=null)loader.close(); }
    @BeforeEach void start() throws Exception {
        previousLoader=Thread.currentThread().getContextClassLoader();Thread.currentThread().setContextClassLoader(loader);
        tester=new WicketTester((WebApplication)loader.loadClass("dev.example.QuickStartApplication").getConstructor().newInstance());
        tester.startPage(tester.getApplication().getHomePage());
    }
    @AfterEach void stop() { if(tester!=null)tester.destroy();Thread.currentThread().setContextClassLoader(previousLoader); }
    @Test void nativeFormValidatesAndSavesTheDocumentedModel() {
        tester.newFormTester("form").submit("saveNative");
        assertThat(tester.getLastResponseAsString()).contains("aria-invalid=\"true\"").doesNotContain("Saved profile for");
        var form=tester.newFormTester("form");form.setValue("name:input","Ada");form.submit("saveNative");
        tester.assertLabel("status","Saved profile for Ada.");
    }
    @Test void ajaxErrorsStayInlineAndSaveUpdatesTheReceipt() {
        tester.executeAjaxEvent("form:save","click");
        assertThat(tester.getLastResponseAsString()).contains("Shadwick.fieldFeedback", "is required");
        tester.assertLabel("status","No changes saved yet.");
        tester.newFormTester("form").setValue("name:input","Luka");tester.executeAjaxEvent("form:save","click");
        tester.assertLabel("status","Saved profile for Luka.");tester.assertComponentOnAjaxResponse("status");
    }
}

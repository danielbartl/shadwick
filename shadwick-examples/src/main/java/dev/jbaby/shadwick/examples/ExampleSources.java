package dev.jbaby.shadwick.examples;

import org.apache.wicket.Page;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Reads packaged source, so displayed examples match the running JAR without a source checkout. */
final class ExampleSources {
    private static final String ROOT = "/META-INF/showcase-source/dev/jbaby/shadwick/examples/";
    record File(String name) implements Serializable {
        String language() { return name.endsWith(".java") ? "Java" : "Wicket markup"; }
        String github() {
            return "https://github.com/danielbartl/shadwick/blob/main/shadwick-examples/src/main/"
                    + (name.endsWith(".java") ? "java" : "resources") + "/dev/jbaby/shadwick/examples/" + name;
        }
    }
    static List<File> files(Class<? extends Page> page) {
        List<File> result = new ArrayList<>();
        Class<?> owner = page;
        do {
            String name = owner.getSimpleName();
            String java = read(name + ".java");
            result.add(new File(name + ".java"));
            addMarkup(result, name + ".html");
            var classes = Pattern.compile("\\bclass\\s+(\\w+)").matcher(java);
            while (classes.find()) addMarkup(result, name + "$" + classes.group(1) + ".html");
            owner = owner.getSuperclass();
        } while (owner == AppLayoutPage.class);
        return List.copyOf(result);
    }
    private static void addMarkup(List<File> files, String name) {
        if (ExampleSources.class.getResource(ROOT + name) != null && files.stream().noneMatch(f -> f.name().equals(name))) files.add(new File(name));
    }
    static String read(String name) {
        try (var stream = ExampleSources.class.getResourceAsStream(ROOT + name)) {
            if (stream == null) throw new IllegalStateException("Missing packaged example source: " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).stripTrailing();
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read example source: " + name, error);
        }
    }
    private ExampleSources() {}
}

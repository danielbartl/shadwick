package dev.jbaby.shadwick.compatibility;

import dev.jbaby.shadwick.Shadwick;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/** The candidate release API, including protected subclass hooks and generic signatures. */
class PublicApiTest {
    private static final Path BASELINE = Path.of("src/test/resources/dev/jbaby/shadwick/compatibility/api-0.1.txt");
    private static final String HEADER = "# Shadwick 0.1 candidate API. Update only after reviewing the compatibility impact.\n";

    @Test
    void publicApiMatchesTheReviewedBaseline() throws Exception {
        var actual = signatures();
        if (Boolean.getBoolean("shadwick.api.update")) {
            Files.createDirectories(BASELINE.getParent());
            Files.writeString(BASELINE, HEADER + String.join("\n", actual) + "\n", StandardCharsets.UTF_8);
        }
        assertThat(Files.exists(BASELINE)).as("API baseline; see docs/CONTRIBUTING.md before updating it").isTrue();
        var expected = Files.readAllLines(BASELINE, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.isBlank() && !line.startsWith("#")).toList();
        assertThat(actual).as("public/protected API; review removals, additions, generics and subclass hooks")
                .containsExactlyElementsOf(expected);
    }

    @Test
    void exportedSignaturesAreUsableOutsideTheLibraryPackage() throws Exception {
        List<String> problems = new ArrayList<>();
        for (Class<?> type : exportedTypes()) {
            check(type.getGenericSuperclass(), type.getName(), new HashSet<>(), problems);
            for (Type parent : type.getGenericInterfaces()) check(parent, type.getName(), new HashSet<>(), problems);
            for (Field field : type.getDeclaredFields()) if (visible(field.getModifiers()) && !field.isSynthetic())
                check(field.getGenericType(), field.toString(), new HashSet<>(), problems);
            for (Method method : type.getDeclaredMethods()) if (visible(method.getModifiers()) && !method.isSynthetic()) {
                check(method.getGenericReturnType(), method.toString(), new HashSet<>(), problems);
                checkExecutable(method, problems);
            }
            for (Constructor<?> constructor : type.getDeclaredConstructors()) if (visible(constructor.getModifiers()))
                checkExecutable(constructor, problems);
            for (TypeVariable<?> parameter : type.getTypeParameters()) check(parameter, type.getName(), new HashSet<>(), problems);
        }
        assertThat(problems).as("inaccessible types in exported signatures").isEmpty();
    }

    @Test
    void everyLibraryClassCanBeLoadedOnJava17() throws Exception {
        for (Path path : classFiles()) try (var input = new DataInputStream(Files.newInputStream(path))) {
            assertThat(input.readInt()).as("class magic: %s", path).isEqualTo(0xCAFEBABE);
            input.readUnsignedShort();
            assertThat(input.readUnsignedShort()).as("Java 17 bytecode ceiling: %s", path).isLessThanOrEqualTo(61);
        }
    }

    private static List<String> signatures() throws Exception {
        SortedSet<String> lines = new TreeSet<>();
        for (Class<?> type : exportedTypes()) {
            lines.add("TYPE " + type.toGenericString()
                    + (type.getGenericSuperclass() == null ? "" : " extends " + type.getGenericSuperclass().getTypeName())
                    + " implements " + Arrays.toString(Arrays.stream(type.getGenericInterfaces()).map(Type::getTypeName).toArray()));
            if (type.isEnum()) lines.add("ENUM " + type.getName() + " "
                    + Arrays.toString(Arrays.stream(type.getEnumConstants()).map(value -> ((Enum<?>) value).name()).toArray()));
            if (type.isRecord()) lines.add("RECORD " + type.getName() + " " + Arrays.toString(type.getRecordComponents()));
            for (Constructor<?> constructor : type.getDeclaredConstructors()) if (visible(constructor.getModifiers()) && !constructor.isSynthetic())
                lines.add("CTOR " + constructor.toGenericString());
            for (Method method : type.getDeclaredMethods()) if (visible(method.getModifiers()) && !method.isSynthetic() && !method.isBridge())
                lines.add("METHOD " + method.toGenericString());
            for (Field field : type.getDeclaredFields()) if (visible(field.getModifiers()) && !field.isSynthetic()) {
                String constant = "";
                if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())
                        && (field.getType().isPrimitive() || field.getType() == String.class)) {
                    field.setAccessible(true);
                    Object value = field.get(null);
                    String text = String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"")
                            .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
                    constant = " = " + (value instanceof String ? "\"" + text + "\"" : text);
                }
                lines.add("FIELD " + field.toGenericString() + constant);
            }
        }
        return List.copyOf(lines);
    }

    private static List<Class<?>> exportedTypes() throws Exception {
        List<Class<?>> types = new ArrayList<>();
        Path root = classesRoot();
        for (Path file : classFiles()) {
            String name = root.relativize(file).toString().replace(java.io.File.separatorChar, '.').replaceAll("\\.class$", "");
            Class<?> type = Class.forName(name, false, Shadwick.class.getClassLoader());
            if (accessible(type)) types.add(type);
        }
        assertThat(types).as("compiled library API types").isNotEmpty();
        return types;
    }

    private static Path classesRoot() throws Exception {
        return Path.of(Shadwick.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private static List<Path> classFiles() throws Exception {
        try (var files = Files.walk(classesRoot().resolve("dev/jbaby/shadwick"))) {
            return files.filter(path -> path.toString().endsWith(".class")).sorted().toList();
        }
    }

    private static boolean visible(int modifiers) { return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers); }

    private static boolean accessible(Class<?> type) {
        if (type.isArray()) return accessible(type.getComponentType());
        return type.isPrimitive() || visible(type.getModifiers())
                && (type.getEnclosingClass() == null || accessible(type.getEnclosingClass()));
    }

    private static void checkExecutable(Executable executable, List<String> problems) {
        for (Type parameter : executable.getGenericParameterTypes()) check(parameter, executable.toString(), new HashSet<>(), problems);
        for (Type exception : executable.getGenericExceptionTypes()) check(exception, executable.toString(), new HashSet<>(), problems);
        for (TypeVariable<?> parameter : executable.getTypeParameters()) check(parameter, executable.toString(), new HashSet<>(), problems);
    }

    private static void check(Type type, String context, Set<Type> visiting, List<String> problems) {
        if (type == null || !visiting.add(type)) return;
        if (type instanceof Class<?> value && !accessible(value)) problems.add(context + " -> " + value.getName());
        if (type instanceof ParameterizedType value) {
            check(value.getRawType(), context, visiting, problems);
            check(value.getOwnerType(), context, visiting, problems);
            for (Type argument : value.getActualTypeArguments()) check(argument, context, visiting, problems);
        }
        if (type instanceof GenericArrayType value) check(value.getGenericComponentType(), context, visiting, problems);
        if (type instanceof TypeVariable<?> value) for (Type bound : value.getBounds()) check(bound, context, visiting, problems);
        if (type instanceof WildcardType value) {
            for (Type bound : value.getUpperBounds()) check(bound, context, visiting, problems);
            for (Type bound : value.getLowerBounds()) check(bound, context, visiting, problems);
        }
        visiting.remove(type);
    }
}

# Documentation maintenance

The sources in this directory serve two readers: Markdown on GitHub and the generated `/docs/` website.
Keep the prose useful in both. The static docs build uses only Node built-ins; no documentation runtime or
package installation is added to consuming Wicket applications.

## Add or change a guide

1. Edit `guides/` or `components/` Markdown, keeping the catalog's exact H1 title.
2. Add or update its `catalog.json` entry: route, title, description, group, API source paths and example class.
3. Link to other guides with relative `.md` paths, with fragments when appropriate.
4. Build and check the website. Every public `Sw*` component under the components source tree must appear in
   at least one guide's API source list. Missing coverage fails the build.

The website includes source-derived constructors, methods, protected hooks and nested enums for the listed types.
Anonymous callbacks, nested implementation classes and routine Wicket lifecycle overrides are excluded. Keep usage,
behavioral contracts and accessibility guidance in the Markdown; a signature alone does not explain when to use it.
The source links point at the repository's main branch. Update the README summary when a new family needs an anchor;
existing README headings intentionally remain as compatibility links.

## Markdown format

Use one H1 title, H2–H4 subsections, paragraphs, inline code, ordinary links, emphasis, simple ordered/unordered lists,
blockquotes, fenced code and pipe tables. Code languages include `java`, `html`, `xml`, `css`, `properties`, `sh`
and `javascript`. Raw HTML, images, indented code, nested lists, reference links and custom directives are not part
of the web renderer's authoring format; use an ordinary paragraph, a subsection or a fenced example instead.
Unsupported block syntax fails explicitly. All source text/code is escaped, and unsafe link schemes are rejected.

Keep component IDs consistent across Java and HTML. Distinguish complete files from illustrative fragments that
need application-owned models or services. Avoid HTML as a layout tool in the source.

## Complete examples

The installation and form guides print the exact files in `examples/`. A `snippet` provenance comment immediately
precedes each complete code fence. The build compares the fence to that file and rejects drift. The examples test
compiles these Java files using the build JDK with `--release 21`, loads their matching HTML and exercises native
and Ajax validation/submission. Update the source file and the printed fence together.

## Verify

From the repository root, after building the library CSS:

```sh
./shadwick-core/target/node/node --test --test-isolation=none site/docs/*.test.mjs
node site/build.mjs
node site/check.mjs
./mvnw -B -pl shadwick-examples -am -Dtest=DocumentationExamplesTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -B -pl shadwick-examples -am -Pbrowser -Dtest=DocumentationBrowserTest -Dsurefire.failIfNoSpecifiedTests=false test
node site/serve.mjs 4173
```

Preview `http://127.0.0.1:4173/shadwick/docs/`. Check desktop/mobile, keyboard search/navigation, code copy,
API disclosures, no-script links, light/dark system appearance and a narrow 320px viewport. The build validates
all internal HTML links/fragments and content-versioned assets, including project-subpath navigation. Run browser
accessibility checks when layout or interaction changes. Stop the owned preview afterward.

Generated `site/dist/` output is ignored and must not be edited or committed. The Website workflow builds the docs
with the landing page; changing `docs/**` triggers it. Publishing the static guides does not deploy a Wicket server.

## Readable Wicket composition

Write component code so a reader can understand the page structure first, then navigate by method name into
one field, section or action. This convention applies to new code and refactoring in the library and examples.
The aim is easy navigation and understanding; a lower line count is not the measure of success.

### Choose the lifecycle deliberately

| Location | Responsibility |
| --- | --- |
| Constructor | Store parameters, models and configuration; validate invariants; initialize state needed by layout hooks. |
| `onInitialize()` | Call `super.onInitialize()` first, then assemble the initial component tree once per component instance. |
| `onConfigure()` | Recompute visibility and enabled state from current models and application state. |
| Repeater hooks and event handlers | Populate dynamic items and perform the replacements or updates appropriate to that interaction. |

Prefer `onInitialize()` for page and application-panel composition. It runs after subclass construction and makes
parent/page context available. Do not invoke overridable component factories from a superclass constructor: an
override may depend on subclass fields that have not been initialized yet. Let Wicket invoke lifecycle hooks.
Keep changing state out of one-time initialization, and do not rebuild the initial tree on every render.

Library components may intentionally expose children before attachment. Fluent setters, `getInput()`, dialog
forms and content slots already support eager or lazy access. Preserve those contracts and extract private
factories around the existing timing. Moving creation to `onInitialize()` is a behavioral change even when the
public signatures stay the same. Keep existing getters returning the same child instance.

### Keep one level of detail in each method

`onInitialize()` should read as a short overview of the forms or sections. A form method names its fields and
buttons; field factories contain binding, validation, descriptions and behaviors. Substantial callbacks delegate
to named action methods. Keep small, obvious expressions inline when extracting them adds no useful meaning.

This illustrative fragment shows the structure; its model and field/button factories belong to the page:

```java
@Override
protected void onInitialize() {
    super.onInitialize();
    add(profileForm());
}

private Form<Profile> profileForm() {
    Form<Profile> form = new Form<>("form", profileModel);
    form.add(name(), email(), saveButton());
    return form;
}
```

See [QuickStartPage.java](examples/QuickStartPage.java) for a complete, compiled example, and the
[form guide](components/forms.md#complete-example) for matching markup.

### Make names and ownership clear

- Use names that explain purpose: `profileForm()`, `email()`, `addNavigation()` and `saveProject()`.
- Factories return components; methods named `add…` attach them. Keep attachment ownership apparent at the call site.
- Keep factories private unless subclass customization is an intentional part of the API. Return concrete component
  types when callers need their configuration methods.
- Keep components local unless another method, callback or documented API needs the reference. Pass dependencies
  explicitly instead of promoting unrelated locals to page fields merely to extract a method.
- Keep models serializable and detachable. Extracting a callback must preserve its original component/form context,
  including where feedback is reported and which components are refreshed.
- Put related methods near their composition entry point. Use one statement per line, readable fluent chains and
  comments that explain lifecycle or interaction decisions.
- Prefer ordinary Wicket composition and small private methods. Avoid shared builders, general-purpose base classes,
  boolean-heavy configuration helpers or new protected hooks solely to hide a few lines of setup.

Preserve component IDs, matching markup, model ownership/detachment, fluent configuration, validation order,
serialization, Ajax target scope and focus behavior. Use the existing relevant tests; add lifecycle regression
coverage when changing initialization behavior, rather than tests that merely restate the extracted private methods.

## Candidate API compatibility

The library's normal tests compare its compiled public/protected declarations with
`shadwick-core/src/test/resources/dev/jbaby/shadwick/compatibility/api-0.1.txt`. The baseline includes inherited
type relationships, generic signatures, subclass hooks, enum order, record components and scalar constant values.
It excludes anonymous, private and package-private types and compiler bridges. Changes to implementation bodies
do not require a baseline update. Exported additions, removals and signature changes do.

Review the diff before accepting it: check existing call sites and documentation, fluent return types, overload
ambiguity, detachable models, serializable callbacks and protected hooks. Adding an abstract interface method
can break existing implementations even when no old declaration is removed. A baseline match does not prove
behavioral or serialized-session compatibility. Read [Compatibility](guides/compatibility.md) for that boundary.

After intentionally reviewing a Java API change, regenerate the baseline from the repository root:

```sh
./mvnw -B -pl shadwick-core -am test -Dtest=PublicApiTest -Dsurefire.failIfNoSpecifiedTests=false -Dshadwick.api.update=true
git diff -- shadwick-core/src/test/resources/dev/jbaby/shadwick/compatibility/api-0.1.txt
./mvnw -B test
```

Keep the update flag out of CI and normal test commands. A baseline update accepts declarations; it is not a
release, a version bump or authorization to publish. The guard runs automatically in the existing Java 21/25 CI
matrix without an additional dependency. The consumer tests compile in an external package and exercise
serialized models/callbacks through real Wicket form and chart usage.

Standard Javadoc is also checked during first-release preparation:

```sh
./mvnw -B -pl shadwick-core org.apache.maven.plugins:maven-javadoc-plugin:3.12.0:javadoc -Ddoclint=all,-missing
```

Malformed tags and links must be fixed rather than disabling doclint. The `release` profile attaches source/Javadoc
artifacts; the separate `central` profile signs and stages them. Follow [release preparation](guides/releases.md)
for the manual workflow, required credentials and local bundle dry run.

## Agent resources

The same build writes `llms.txt` from `catalog.json` and publishes every guide at its HTML directory's
`index.md`. Markdown companions include the current source-derived API declarations; code fences are preserved
and guide links point to their Markdown counterparts. HTML pages advertise the Markdown/index resources with
`alternate` and `describedby` link relations. Keep these exports generated from the same source as the HTML.

The portable skill lives in `../shadwick/` and is copied to `skills/shadwick/` on the static site. Keep its
instructions focused on decisions that matter to consuming Wicket applications. Update the task-to-guide
references when introducing a new family or changing a guide route. Skill Markdown references must stay inside
the package or use explicit external URLs. Keep `agents/openai.yaml` consistent with the skill entrypoint.

The skill's three `QuickStart*` assets are byte-identical copies of `docs/examples/` and are validated against
those compiled examples. When changing an example, update its printed guide fence and skill asset together.
Keep the skill's license synchronized with the repository license. `site/agents.mjs` checks portability, source
parity, guide coverage and exported link targets; its tests run with the documentation tests above. Skill-only
changes trigger the Website workflow too. Do not edit the copies under `site/dist/`.

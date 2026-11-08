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
compiles these Java files using the build JDK with `--release 17`, loads their matching HTML and exercises native
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
release, a version bump or authorization to publish. The guard runs automatically in the existing Java 17/25 CI
matrix without an additional dependency. The consumer tests compile in an external package and exercise
serialized models/callbacks through real Wicket form and chart usage.

Standard Javadoc is also checked during first-release preparation:

```sh
./mvnw -B -pl shadwick-core org.apache.maven.plugins:maven-javadoc-plugin:3.12.0:javadoc -Ddoclint=all,-missing
```

Malformed tags and links must be fixed rather than disabling doclint. The later release automation checkpoint
will attach source/Javadoc artifacts and configure signing/publication.

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

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
node --test site/docs/*.test.mjs
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

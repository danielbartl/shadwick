# Shadwick

shadcn/ui-style UI components for Apache Wicket — the Wicket counterpart of
Shadleaf (shadcn/ui for Thymeleaf), with the Java API of Wicket Oat.

## Goal

A component library that gives Wicket applications shadcn/ui's look, tokens and
component catalog, rendered on the server by Wicket. A consuming app adds one
Maven dependency and is done: no Node, no Tailwind, no CSS build on their side.

- Coordinates: `dev.jbaby:shadwick` (groupId `dev.jbaby`, namespace verified on Maven Central)
- Java package: `dev.jbaby.shadwick`
- License: Apache 2.0

## Resume point — 2026-11-07

**Current checkpoint: Studio homepage and a navigable component catalog.** The user requested Clover Studio
at `/`, the catalog at `/components`, improved catalog navigation and Wicket code on each demo page.
The `/studio` alias remains a direct-rendering compatibility route; generated Studio links use `/`.
`CatalogPages` is the shared inventory for 35 demo routes, seven navigation groups, overview cards and
guide links. BasePage now has a compact header, responsive/searchable sidebar (Ctrl/Command+K), active
links and a native preferences disclosure. Navigation carries skin/mode/locale without unrelated demo state.
Layout demos retain their own layout preview and link back to the catalog.
Studio also has Components/Komponenten as the last sidebar link, opening `/components` with its current
skin/mode/locale parameters. It uses normal navigation into the catalog; Studio-only links remain Ajax.

Every catalog demo has Java and matching HTML, including supporting panel markup and inherited header-layout
source. Maven packages these exact files under `META-INF/showcase-source`; `ExampleSourcePanel` renders
escaped source through detachable models. Files have native disclosures, guide/GitHub links, copy controls
and selection fallback. No runtime GitHub fetch or new dependency is needed. Source remains readable without
script. Source panels use bounded scrolling, adequate contrast and matching visible/accessibility labels.

Validation: 880 JVM tests, 25 catalog browser scenarios (all nine skins, desktop/mobile, Chromium/Firefox,
search, copy/fallback, no-script and all demo routes), 29 Studio scenarios and 14 targeted existing showcase
regressions pass. Java 17 Docker build and both-browser HTTPS container smoke pass, including actual Chromium
clipboard copying, strict CSP/secure cookies, canonical root links and exact packaged source. Reviewed
catalog/source screenshots under ignored `shadwick-examples/target/catalog-screenshots/`. Static site/docs
checks and landing checks at 1440/1024/800/700/390/320 px pass. Adding Live demo exposed a positional mobile
nav selector; it now hides explicit hrefs so Live demo stays visible without overflowing the header.
Logs: `/tmp/shadwick-catalog-{verify,browser,regression-browser,image-build,production-smoke,site}.log`.

The public Railway URL is https://shadwick-production.up.railway.app/ and the catalog is `/components`.
Landing/README/guide links now use these real URLs. Publication uses the existing Railway GitHub integration
and GitHub Pages workflow; use `git log`, commit statuses and live checks for the exact deployed revision.
No Railway CLI login/integration is required for these automatic source deployments. Next: agent guidance/
llms.txt, followed by release preparation and Maven Central. Broader Oat/chart variants remain separate.

**Railway deployment preparation.** The user selected Railway and authorized deploying Clover Studio.
The repository is now connected to Railway project `26a6a91d-41b3-4a54-999d-650612a8957d`, service
`794b0087-a4ca-44ff-8ecd-375e87f53a24` (shadwick), environment `83c1bd75-8d11-4b8f-8dae-2631f28bfa3c`.
These IDs were verified from Railway's GitHub commit-status URL. Its first build was rejected before
execution because cache mounts lacked IDs. Both mounts now use Railway's literal
`s/<service UUID>-<target path>` IDs; variables/build args cannot be used for this validation. Keep the
service UUID synchronized in both mounts if the service changes. Railway accepted the cache correction and
reported a successful deployment. Account access is still needed for Railway dashboard/API operations.
The root Dockerfile builds both modules/CSS with Java 17, then runs an unprivileged JRE image in Wicket
deployment mode. `railway.json` configures one replica, no sleeping, `/healthz` readiness and graceful
teardown. The Railway Spring profile handles forwarded HTTPS headers, secure/HttpOnly/SameSite=Lax
cookies and 30-minute session expiry. The stateless health resource neither binds a session nor caches.
The Studio guide documents GitHub-repository deployment and a local Docker check.

Java 17's compiler crashed on the local Cash record inside Overview's constructor lambda (also affected
the earlier CI job). Moving it to a private member record fixes compilation while retaining the Java 17
baseline. The complete 839 JVM tests pass inside the Java 17 builder. The runtime image passed Chromium
and Firefox checks behind a local HTTPS proxy with one CPU/512 MB, verifying deployment mode, assigned
PORT, a non-root user, secure sessions/CSP, task/payment Ajax, fresh repeated-search HTML and German/Nova
preferences without document reloads or console errors. Website/docs checks pass. Logs:
`/tmp/shadwick-railway-{image-build,java17-tests,runtime,smoke,site-check}.log`.

The user connected GitHub to Railway directly and supplied https://shadwick-production.up.railway.app/.
There is no Railway CLI login, environment token or repository secret. GitHub status updates track
automatic deployments. For dashboard/API operations, use Railway access if subsequently provided; do not
ask for deployment authorization again. The current routing/catalog checkpoint above supersedes the old
root-catalog setup. Continue agent guidance/llms.txt and release preparation/Maven Central after publication.

**Publication checkpoint: Clover Studio.** The user requested committing and pushing the completed Studio
application, fixes, tests and landing/documentation updates. The landing links to its local-run guide; the
Website workflow publishes the static site after the push. GitHub Actions and `git log` record the exact
source revision and deployment outcome. The Java backend is now hosted on Railway as described above.
Publication checks pass: 838 JVM tests, all 61 Studio browser scenarios, three documentation renderer/API
checks and the website build/check (55 HTML pages, 4,206 internal references). Logs are under
`/tmp/shadwick-studio-publication-{verify,browser,docs,site}.log`; generated output remains ignored.
Next: choose a Java host, deploy Clover Studio and link its public URL; then agent guidance/llms.txt and
release preparation/Maven Central. Do not move Java hosting into GitHub Pages.

**Latest follow-up: Studio Ajax composition.** All scripted commands, internal navigation, filters,
preferences and table controls now use partial Ajax responses. Studio's former setResponsePage callbacks
caused the observed full-page reloads; no component API change was needed. Live models/ViewState bindings
patch only changed regions, and normal Studio GETs render directly. Native links/forms remain fallbacks.
Explicit searches/clear filters always return fresh result, count and empty-state HTML, including identical
matches; do not suppress those patches using ViewState comparisons. `CloverStudio.url` only updates browser
history. The 12 focused search/history checks pass in Chromium/Firefox after this follow-up.
The verified executable is `shadwick-examples/target/shadwick-examples-0.1.0-SNAPSHOT-ajax.jar`.
Original executables/processes were preserved. 838 JVM tests pass; the 61-case Studio browser suite and
16 focused command/context checks pass, plus a packaged-JAR smoke. The publication checkpoint above
records the subsequent commit/push request.

**Clover Studio is implemented locally.** The user approved a composed agency-workspace showcase.
The Wicket examples now mount `/studio`: overview, searchable/filterable/sortable projects, project
workspace and sortable/exportable invoices. A dedicated SwAppLayout composition uses a warm paper/green
palette, the existing clover mark, responsive sidebar, skin/language settings and browser-owned themes.
The component catalog remains at `/`. No new dependency was added. Public backend hosting remains a separate checkpoint.

The resettable session-owned StudioWorkspace connects creation/editing, task owners/estimates/completion,
derived progress, milestones, scope preview/download, confirmed client handoff/ZIP and invoice payments.
Overview metrics/chart/activity reflect the same records. Generation, revision and task-state checks
reject stale actions; payments are idempotent and delivered projects are locked. Scope/ZIP use native
session-checked downloads. New projects do not automatically issue invoices. The complete Studio interface supports English/German only;
client/project/task content stays as entered. Built-in messages and date/number formats follow that locale.

Read docs/guides/clover-studio.md for run instructions, the full journey, limits and source locations.
It joins the searchable docs (53 guides now); the landing links to local-run instructions until a Java
host is available. Next: deploy this actual Wicket application alongside the static landing/docs, then
agent guidance/llms.txt and release preparation/Maven Central. Keep deployment configuration separate
from GitHub Pages: Pages cannot run the Java backend. Detailed verification/handoff is appended below.

**Publication checkpoint: component documentation.** The existing static site now builds a searchable
`/docs/` section from 52 Markdown guides and source-based references for 150 API types. Every public Sw
component is covered. README compatibility headings link to the guides; landing/catalog links use the
new docs routes. Docs include Java/HTML, configuration, native/Ajax behavior and accessibility guidance.
The complete installation/form files are compiled and tested. No dependency or hosting service was added.

The docs reader supports keyboard search, copy/fallback, native navigation/disclosures, light/dark system
appearance, responsive widths and print. Website builds validate all HTML/Markdown links, fragments,
versioned assets, component coverage and synchronized examples. Docs changes trigger the Pages workflow.
See docs/README.md and docs/CONTRIBUTING.md for content and maintenance. Generated site/dist stays ignored.
The user requested committing and pushing this group. It joins the landing at
https://danielbartl.github.io/shadwick/docs/ through the Website workflow; the workflow records the source
revision and deployment result. Previous published baseline is 0ef053f. Next: host the interactive Wicket
showcase, then agent skill/llms.txt and release preparation/Maven Central. Broader Oat/chart gaps remain
separately scoped. Detailed verification and the handoff are recorded at the end of this file.

**Previous publication checkpoint: busy/value controls and advanced tables.**
The user requested committing and pushing all accumulated work and publishing the updated landing page.
This checkpoint includes busy Ajax buttons, progress bars/rings, native meters, rating display/input,
CSV export, column choices, lazy expandable details and validated inline editing. The `/status-controls`
and `/advanced-tables` examples, nine-language messages, README, NOTICE and landing catalog are included.
No new dependency was added. Use `git log -1 --oneline` and the Website workflow to identify the source
revision and deployment result; 0d4a69a is the previous published internationalization checkpoint.

CSV includes all filtered rows in provider sort order, including hidden exportable columns. Visibility is
presentation, not authorization. Structural columns and at least one named data column remain visible.
Inline forms stage drafts, recheck edit predicates and reject stale values. Application callbacks own
persistence, authorization and database concurrency. Native standalone forms work; nested forms need Ajax.
Editors retain server-side validation while their row remains visible. Stable `setRowKey` IDs let fresh
provider models replace old delegates; paging/filtering a row away discards its editor. Details retain keys.
The final component verification and implementation decisions remain recorded at the end of this file.
Next roadmap group: a hosted interactive Wicket showcase, then agent skill/llms.txt and release
preparation/Maven Central. Component documentation is now implemented locally. Further Oat families/chart
variants remain scoped follow-ups.

The library ships English plus German, Spanish, French, Italian, Japanese, Dutch, Portuguese and Croatian
built-in messages through Wicket's resource lookup: 280 translated core bundles across eight non-English
languages. The standard examples have a native language picker; /internationalization?lang=hr-HR and
/internationalization?lang=ja exercise controls, Ajax and native fallbacks. README covers locale selection
and page/application overrides. Internationalization and Croatian are included in the publication
checkpoint below; use git log and the Website workflow for the source and deployment revision.
Maia, Mira, Luma, Rhea, Sera and Sketch join Vega, Lyra and Nova, including standalone/embedded bundles
and selectors. All nine skins are included in this publication checkpoint; use `git log -1 --oneline`
and the Website workflow for the current source/deployed revision.
All main Shadleaf component families now have implementations. Wicket Oat has a broader catalog;
its remaining gaps are recorded below. Review the recorded variant gaps before
claiming full Shadleaf parity. Maven Central publication is still future work.
Read Wicket Oat's corresponding APIs/tests and Shadleaf's markup/CSS/runtime first. Native
select/date fields, custom single select, searchable single/multiple combobox and calendar/date
picker (single and range), server-rendered month view and SVG charts are implemented.
Reuse existing forms, feedback and overlays.
Ask before any further library dependency; wicket-extensions is now available at Wicket 10.11.0.

User menu, browser theme preferences, app layouts/navigation depth, tabs, accordion,
collapsible, attachment displays, data tables/pagination, custom select, combobox and calendar/date
picker, month view and charts are included in this publication checkpoint, with verification
records below. Attachment displays complement the completed upload controls.

Current publication checkpoint — controls and advanced tables, 2026-11-06:

- Includes all locally completed busy/progress/meter/rating and advanced-table work. Landing catalog
  copy highlights CSV, column choices, expandable details, inline edits, progress, meters and ratings.
  Detailed runnable behavior is linked through README and the local Wicket showcase; the interactive
  hosted showcase remains future work. Maven Central publication is also pending.
- Final local JVM verification: 750 core and 62 example tests (812 total). New advanced-table browser
  checks passed 72 cases across all nine skins/light-dark; existing table checks passed 144 cases, with
  54 relevant geometry rechecks after the mobile refinement. Final model serialization is covered by
  the JVM suite. Earlier busy-control checks and exact validation scope remain in the records below.
- Chromium/Firefox checks include desktop/390px/320px screenshots and axe audits, nine message locales
  and native fallbacks. Landing screenshot/axe/console/overflow checks passed all 18 cases across three
  widths and Vega/Sera/Sketch. Site build/check passes 166 references and 105 unique IDs.
- GitHub Pages deploys main through .github/workflows/pages.yml. Landing:
  https://danielbartl.github.io/shadwick/ . The Website workflow records deployment outcome and revision.
- Prior baseline 0d4a69a had successful Website and Build workflows, including Java 17/25 and browser checks.
- Next: documentation and hosted showcase, followed by agent guidance and release preparation.

Previous publication checkpoint — internationalization, 2026-11-06:

- User requested committing/pushing internationalization, Croatian and the landing update. This checkpoint
  includes all nine languages, translated native/enhanced controls, application override support, the
  showcase language picker and example, documentation and the landing's explicit nine-language panel.
- The landing remains English and names English, German, Spanish, French, Italian, Japanese, Dutch,
  Portuguese and Croatian. Its new catalog panel links directly to README internationalization setup.
  Desktop/390px/320px Chromium and Firefox screenshot/axe checks passed. The panel's body text contrast
  was corrected before publication. Site build/check: 166 references and 105 unique IDs.
- JVM verification passed 696 core and 60 showcase tests; Croatian/i18n focused browser checks passed
  26 Chromium cases plus four Croatian Chromium/Firefox visual audits. Earlier initial-i18n checks and
  their scope are recorded below; no full nine-skin browser-green claim is inferred from focused checks.
- The previous b6edbd6 Build workflow had failed 16 of 3312 browser cases. Its publication follow-up
  fixes missing explicit inset-header styling in Maia/Mira/Luma/Rhea/Sera/Sketch, makes the sidebar
  provider shrink inside grid layouts (Sera long-name RTL overflow), and checks each RTL switch
  background layer rather than assuming every skin uses a single image. All 54 targeted cases across
  nine skins/light-dark passed; log: /tmp/shadwick-publish-ci-repair2.log.
- GitHub Pages builds/deploys main through .github/workflows/pages.yml. The workflow run records
  the final source revision and deployment result. Landing: https://danielbartl.github.io/shadwick/ .
- Next: full component documentation and a hosted interactive showcase, then agent skill/llms.txt,
  release preparation and Maven Central. Chart variant follow-ups remain in Later phases.

Previous publication checkpoint — all nine skins, 2026-11-06:

- The user requested committing/pushing all six remaining skins and updating the landing page.
  This checkpoint includes all nine skin identities, standalone/embedded entries, Sketch's
  bundled fonts/license, showcase selectors, README/NOTICE and the updated landing preview.
- Landing: https://danielbartl.github.io/shadwick/ . Hero, feature and status copy now describe
  nine skins; the preview loads each actual embedded bundle through versioned asset URLs.
  Sketch's font stays inside the component preview. The full component catalog remains live.
- Verification: 661 core and 49 showcase-page tests passed. The recorded browser checks include
  all nine skins' geometry, all new skins' page audits, desktop/mobile screenshots and focused
  rechecks after the attachment/disclosure repairs. Detailed coverage and limits remain below;
  a single final green 660-case run is not claimed.
- Static site checks pass 165 references and 104 IDs, including all nine skin assets and the
  font files/license. Site output remains generated and ignored. GitHub Pages deploys through
  .github/workflows/pages.yml; its run records the deployment result and source revision.
- At this publication, translations were next; the subsequent local group is recorded above.

Previous publication checkpoint — 2026-11-05:

- The user requested committing/pushing all accumulated work and updating the landing page.
  This checkpoint gathers the complete component catalog, Nova, showcases, docs and landing
  demos on main. Use `git log -1 --oneline` for the current source revision; the Website
  workflow records the deployed commit and URL.
- Landing: https://danielbartl.github.io/shadwick/ . The catalog now lists all implemented
  families, highlights calendars/month views/charts and names Vega, Lyra and Nova. It no longer
  labels advanced controls as future work. Maven Central remains pending.
- Final JVM checks: 630 core tests and 43 showcase-page tests. Nova's full browser run passed
  368 cases; the final cross-skin geometry run passed 18 cases. Earlier broad/focused browser
  results and their limits remain recorded below; do not replace them with a full-green claim.
- Publication landing checks passed 24 desktop/mobile × skin/mode × sidebar/header combinations
  plus two native/no-script widths. They cover the ten catalog groups, three skin choices,
  chart keys, month navigation, invoice filtering, nested date picker Escape and project submit,
  WCAG axe audits, console errors and overflow. All 12 README links point to current headings.
  Site build/check passed 155 internal references and 104 unique IDs (about 1,228 KB).
- Website workflow: https://github.com/danielbartl/shadwick/actions/workflows/pages.yml .
  It builds/tests the library, checks the static site and deploys main to GitHub Pages.
- Landing visual repair: button tabs now receive the selected skin's card-content inset,
  with single-column actions in narrow cards. Marketing headings no longer override dialog
  titles. The attachment has explicit thumbnail sizing, a fallback icon and a proportional
  preview. Content-versioned asset URLs prevent fresh markup from reusing old CSS/scripts,
  including dynamically selected skins and table icons; the build checker validates hashes.
  Screenshot/geometry/download checks passed all 36 Chromium/Firefox × six widths (320–1440)
  × three skins, including light/dark image dialogs. The 24 layout/skin/mode accessibility
  checks and two no-script cases also passed. Cache blocking, failed-image fallback, build
  determinism and rejection of stale asset hashes passed. WebKit was unavailable locally
  because required system libraries are absent. Site checks now pass 159 internal references.
- That publication preceded the remaining skins; all six are now implemented locally. Next: translations,
  documentation/tooling and release preparation.

Previous published baseline (historical):

- Branch `main`, commit `1909d8052d0d2a12f0f837096d6afd761fadf505`
  (`Add app-shell navigation and update the landing studio`), committed and pushed.
- Site header, breadcrumb, skip link and responsive sidebar are implemented, documented
  and published alongside the completed forms, display/feedback and overlay groups.
- Landing page: https://danielbartl.github.io/shadwick/ . Its studio previews the sidebar,
  header and breadcrumb with the actual library CSS/JS in both skins and color modes.
- Website deployment succeeded: https://github.com/danielbartl/shadwick/actions/runs/41914029752 .
  Live desktop/mobile checks passed for both skins/modes, including WCAG axe audits,
  native no-script navigation and disabled sidebar persistence.
- Published-baseline JVM verification: 438 core tests and 26 showcase-page tests passed. All 28
  focused sidebar browser cases passed after fixing nested-menu Escape/Tab focus races.
  The broad 308-case run and subsequent fixes are recorded under "Phase 5 sidebar";
  it was not a single completely green full-suite run after the final fixes.
- Preview servers were stopped. The working tree was clean after publication;
  this handoff update is a later documentation change.

The roadmap below includes the latest Shadleaf parity review. In particular, explicitly
include tabs/accordion/collapsible and attachment displays, which the earlier roadmap
omitted. Native selects/date fields, file upload and attachment displays are complete;
month view and charts now complete the main catalog at the family level. Remaining work includes
documentation/tooling, release preparation and variant-level gaps. Internationalization is now implemented locally.

## Two references, two roles

| Reference | Take from it | Don't take |
|---|---|---|
| **Wicket Oat** — https://github.com/danielbartl/wicket-oat (MIT, same author) | The Java API and its feel, Wicket integration patterns, tests, CSP handling, translations, agent skill | Oat's markup, CSS and JS |
| **Shadleaf** — https://github.com/wimdeblauwe/shadleaf (Apache 2.0), docs https://shadleaf.dev | Markup structure, CSS per skin, tokens, native-HTML behaviour, accessibility details | Its Thymeleaf/Spring integration and tag-attribute API |

- Clone both outside this repo (e.g. `/tmp/wicket-oat`, `/tmp/shadleaf`).
  Never commit them, never add either as a dependency.
- Wicket Oat's Java code may be copied and adapted freely (same author).
  Copy patterns first; only extract a shared module once duplication is proven.
- When adapting CSS or markup from Shadleaf, keep Apache 2.0 attribution in
  `NOTICE`. shadcn/ui itself is MIT — credit it in `NOTICE` too.
- README states clearly: inspired by Shadleaf, not affiliated with or endorsed by it.

## API design: same feel as Wicket Oat

A developer who knows Wicket Oat should feel at home immediately, and moving an
app from one library to the other should be mostly renaming. Before designing
any Shadwick API, read the equivalent Wicket Oat class and its tests, and match
its shape unless shadcn genuinely differs. Concretely:

- **Plain Wicket components**, created with `new`, bound to models, subclassable.
- **Lambda factories** for callback-driven components, like Wicket's
  `AjaxLink.onClick(...)`: `XxxButton.onClick("save", "Save", target -> ...)`,
  `XxxSubmitButton.onSubmit(...)`, `XxxAccordion.of(id, listModel, (item, obj) -> ...)`.
- **Component + Behavior for most elements**: a component when the library owns
  the markup, a behavior to style an existing Wicket component
  (`anyLink.add(Xxx.Behaviors.button().setVariant(...))`).
- **Facades**: `Xxx.Behaviors.*` and `Xxx.Components.*` as single entry points.
- **Form fields wrap label + input + inline feedback/hint** (Wicket Oat's
  `BaseOatField` pattern): work with `CompoundPropertyModel`, take labels from
  `.properties`, wire `aria-describedby` / `aria-invalid` automatically, update
  their error state in place after Ajax validation without `target.add(...)`,
  expose the wrapped component as child `"input"` with `getInput()` and
  `addToInput(...)`; fluent setters keep the concrete type.
- **Dialog API** as `OatDialog`: `setContent(...)` with `CONTENT_ID`,
  `open(target)` / `close(target)`, `onConfirm` / `onClose`, server-side
  `isOpen()`, re-rendering keeps an open dialog open.
- **Feedback**: a feedback panel rendering messages as alerts, and a
  feedback-toasts behavior; both skip errors fields already show inline.
- **Install**: `Shadwick.install(this)` in `Application.init()`, returning fluent
  settings (`.setSkin(...)`, later `.setDefaultTheme(...)`).
- **Dev-mode accessibility checks**: e.g. an icon-only button without an
  accessible name fails to render in DEVELOPMENT mode with a fix-it message,
  logs a warning in DEPLOYMENT mode.
- **CSP**: works under Wicket's default strict, nonce-based CSP, as Wicket Oat does.

Where shadcn differs, follow shadcn:

- **Variants are per component**, not one shared enum: button has default,
  outline, ghost, destructive, secondary, link; badge and alert have their own
  sets. Each component gets its own `Variant`/`Size` enums (nested, as in
  Wicket Oat's `ButtonBehavior.Style` / `.Size`).
- **Skins** (app-wide geometry, chosen at install) replace Oat's density setting.
  Per-user **themes** (token sets via `data-theme`, with a theme store and
  switcher like Wicket Oat's) come in a later phase; Phase 1 has light/dark only.

### Class prefix (decided in Phase 1)

- `Sw` replaces `Oat`: `SwButton`, `SwDialog`, `SwTextField`, facade `Sw`,
  installer `Shadwick`. CSS classes use `sw-`. See Conventions.

## Architecture (decided)

### Stack

- Apache Wicket: latest stable major. Verify on Maven Central — do not rely on
  remembered versions. Java baseline = what that Wicket major requires.
  (Wicket Oat currently builds on Wicket 10.11.0, Java 17.)
- No Spring dependency in the library module.
- No client-side framework (no Alpine, no React). Behaviour is built on native
  HTML: `<dialog>`, Popover API, `<details>`, invoker commands
  (`commandfor` / `command`), plus small vanilla JS only where unavoidable.
- No inline scripts, inline event handlers or `style` attributes in rendered markup.

### Modules

```
shadwick-parent (pom)
├── shadwick-core/     – the library (published as dev.jbaby:shadwick)
├── shadwick-examples  – runnable showcase app (not published)
└── shadwick/          – agent skill directory (SKILL.md + references/), later phase
```

Mirror Wicket Oat's module layout and examples app setup where it makes sense.

### CSS

- Styled with Tailwind CSS v4, compiled at **library** build time. The jar
  ships finished CSS.
- Markup uses component classes, never Tailwind utilities:
  `sw-button`, `sw-dialog-header`, `sw-field-error`.
  Variants/sizes via data attributes: `data-variant="outline" data-size="sm"`.
  State via native/ARIA attributes: `disabled`, `aria-invalid`, `[open]`.
- Component rules live in `@layer components`, written with `@apply`.
  The bundles emit **no utility classes**, so an app's own Tailwind utilities
  always win over component styles.
- Tokens: shadcn/ui's CSS variable names, unprefixed (`--background`,
  `--foreground`, `--primary`, `--primary-foreground`, `--muted-foreground`,
  `--border`, `--input`, `--ring`, `--radius`, …) on `:root` and `.dark`,
  inside a cascade layer so app overrides win. Any shadcn theme (generator,
  tweakcn) must drop in unchanged. Defaults: shadcn's neutral theme
  (take values from ui.shadcn.com/docs/theming).
- Dark mode: `class="dark"` on `<html>`.
- Skins: same markup, same tokens, different geometry / spacing / type size /
  shadow. Phase 1: `vega` (default, shadcn classic) and `lyra` (boxy, square
  corners, small type, thin focus ring).
  Structure (decided, Shadleaf's): `components/*.css` hold structure only,
  each `skins/<skin>.css` holds the full look. See Conventions.
- Bundle variants:
  - `standalone` (default): Tailwind preflight + page base styles
    (background/foreground/border from tokens) + tokens + components.
  - `embedded`: tokens + components only — no preflight, no Tailwind
    theme variables (values compiled inline) — for apps supplying their own reset
    and page styles, such as those running their own Tailwind build.

Source layout (library module):

```
src/main/tailwind/
  tokens.css        – :root/.dark tokens + @theme inline mapping (bg-primary → var(--primary) …)
  page.css          – page base styles (standalone only)
  components.css    – imports every component, base and button first
  components/       – one file per component (structure only)
  skins/            – one file per skin (the components' full look)
  entries/          – one entry file per skin × bundle variant
```

Output: `target/classes/dev/jbaby/shadwick/css/shadwick-<skin>.css`

### Build

- `frontend-maven-plugin` (com.github.eirslett) in the library module:
  installs a pinned Node LTS locally, runs `npm ci` and `npm run build:css`
  in `generate-resources`.
- `tailwindcss` and `@tailwindcss/cli` pinned in `package.json` devDependencies.
- One command builds all bundles; `npm run watch:css` writes into
  `target/classes` so the running examples app picks up CSS changes without restart.
- `mvn clean verify` must succeed on a machine without Node installed.
- The jar contains compiled CSS/JS only — no `package.json`, `node_modules`
  or Tailwind sources.

### Wicket integration

- Settings from `Shadwick.install(...)` are stored as Application metadata.
- Every Shadwick component/behavior contributes the CSS (and JS when needed)
  in `renderHead` via the configured `CssResourceReference` — apps never add
  `<link>` tags manually. Wicket de-duplicates header items.
- JS: one small vanilla script as `JavaScriptResourceReference`. Anything that
  initialises DOM must also run after Wicket Ajax updates and must be
  idempotent. Reuse Wicket Oat's solutions (e.g. how `OatDialog` keeps an open
  dialog open across re-renders) instead of reinventing them.
- The library's own texts (dialog buttons, validation hints, …) live in
  `.properties` files, following Wicket Oat's translation set in later phases.

## Phase 1 — vertical slice (COMPLETE)

Goal: prove the whole pipeline — build, CSS, skins, Wicket Oat-style API — with
three components before porting more.

1. **Button** — `XxxButton` (with `onClick` factory), `XxxSubmitButton`
   (with `onSubmit` factory) and `Xxx.Behaviors.button()` for any Link/Button.
   Variants: default, outline, ghost, destructive, secondary, link.
   Sizes: default, sm, lg, icon. Works on `<button>` and `<a>`.
   Icon size requires an accessible name (dev-mode check).
2. **Field + Text input** — `XxxTextField` (and `XxxTextArea` if cheap) on a
   base field class modelled on `BaseOatField`: label, input, description/hint,
   inline error. Normal submit and Ajax submit, errors updated in place.
3. **Dialog** — `XxxDialog` on native `<dialog>` with header, title,
   description, footer; same API as `OatDialog` (see above). Client-side
   open/close via invoker commands also supported.

Also in Phase 1:
- Neutral tokens, skins `vega` + `lyra`, standalone bundle, light + dark.
- Examples page: all button variants/sizes; a form with validation (normal +
  Ajax submit); dialogs (client-opened, Ajax-opened, one containing a form);
  skin and dark-mode switching for side-by-side comparison.
- Tests: WicketTester (JUnit 5), modelled on Wicket Oat's tests for the same
  components — rendered classes and data attributes, ARIA wiring, feedback
  display, Ajax open/close.

### Acceptance criteria

- `mvn clean verify` green without a local Node install.
- Jar contents as described under Build.
- Examples app: no CSP violations in the browser console; all components
  correct in both skins, light and dark.
- API parity: the examples code reads like the equivalent Wicket Oat example
  code, apart from class names and variant enums.
- Conventions section below filled in with the decisions taken.

### Out of scope for Phase 1

Other components, embedded bundle, per-user themes, translations beyond
English, docs site, agent skill, Maven Central publishing.

## Phase 2 — display and feedback (COMPLETE)

Implemented: embedded bundle; card, badge, alert, avatar, separator, kbd,
skeleton, spinner, empty state and inline SVG icons; feedback panel and feedback
toasts. The Display and Feedback showcase pages exercise them in both skins and
modes. The landing page is deployed on GitHub Pages.

## Phase 3 — forms (COMPLETE)

The first group implements `SwCheckBox`, `SwRadioChoice<T>` and `SwSwitch` on
the existing field base, with both skins and bundles. The Choices showcase page
demonstrates normal submit, Ajax submit, inline errors, immediate change callbacks,
disabled controls and the small switch size.

The agreed form groups are implemented.

### Everyday inputs (implemented)

Implemented email, password, search, telephone, URL and number inputs, including
number conversion, min/max and step support. The Inputs showcase covers normal and
Ajax submit. The landing-page form now demonstrates bounded seat counts.

### Native selections (implemented)

Implemented SwDropdownChoice, SwListMultipleChoice and SwCheckBoxMultipleChoice.
The Selections showcase covers normal and Ajax submit, immediate changes, stable
renderer IDs and disabled states. The landing page includes a native plan selector.

### Native dates and times (implemented)

Implemented SwDateField, SwTimeField, SwDateTimeLocalField, SwMonthField and
SwWeekField. The Dates & time showcase covers ISO conversion, required fields,
server-validated date bounds, normal/Ajax submit, immediate changes and precision.
The landing-page project dialog has an optional native launch date.

### Toggles and input groups (implemented)

Implemented SwToggle, SwToggleChoice and SwToggleMultipleChoice, plus text addons,
native suggestions and composable input-group behaviors. The Groups showcase covers
normal/Ajax submit, immediate formatting, keyboard interaction and addon actions.
The landing page previews joined digest toggles and a decorated email input.

### File upload (implemented)

Implemented SwFileUpload and SwFileDropzone with native multipart handling,
request-scoped upload models, file count/size validation, a progressive selection
list, drag/drop and removal. The Uploads showcase covers plain and Ajax submission
and request limits. The landing dialog includes an optional local reference file.

Phase 4 is complete: confirmations, sheets, popovers, tooltips, dropdown menus and
the general-purpose toast API are implemented. Phase 5 app shell is also implemented locally.

For each group, include showcase examples, validation and Ajax tests, both skins
and modes, and appropriate landing-page updates.

## Phase 4 — overlays (COMPLETE)

Implemented SwConfirmDialog (reusable ask callbacks, native alertdialog semantics,
safe initial focus, duplicate-click guard) and SwSheet (four logical edges, native
modality, independent body scrolling and the existing dialog form/Ajax lifecycle).
Also implemented SwPopover (arbitrary content, native non-modal dismissal/focus and
CSS anchor positioning) and TooltipBehavior (plain-text hover/focus hints, descriptions,
reactive models and Ajax cleanup). The Overlays and Popovers & tooltips showcases,
plus landing-page demos, cover these groups.

Also implemented SwDropdown action menus and caller-owned menu/item behaviors, including
server-backed checkbox/radio choices, links and grouped content. The Menus showcase
and landing Project actions demo cover them.

General-purpose ToastBehavior / Sw.toast helpers are now implemented, including
titles, variants, timing, full-render scripts, keyboard access and native modal context.
Feedback messages retain their existing persistent behavior.

## Phase 5 — app shell (COMPLETE locally)

Site header, breadcrumb and skip link are implemented. SwSiteHeader composes
caller-owned native markup, SwBreadcrumb matches Oat's generic trail API and
reuses the existing dropdown menu for hidden levels, and SwSkipLink gives native
keyboard access to focusable content. The /shell showcase and landing studio
exercise this group; all showcase pages have a skip link.

Sidebar is implemented with native navigation, groups, badges and responsive desktop/mobile behavior.
The /sidebar showcase and landing studio exercise it.

SwUserMenu is implemented for the sidebar footer and site header, including anonymous
sign-in links, account links, avatars/initials and stateless POST sign-out hooks. The
/user-menu and /demo-sign-in showcase pages and local landing preview exercise it.

SwThemeToggle and ThemeBehavior provide reusable Light/Dark/System controls, browser
preferences, synchronized controls and pre-paint application. The /theme showcase and
local landing preview exercise them. The planned Wicket per-user named theme store/switcher
remains additional project scope.

SwAppLayout now composes reusable sidebar/header base pages. MenuItem supports native
submenus and page links with sibling disclosures; SwSidebar has item/group action hooks,
panel-only entries and header placement. The /layouts and /header-layout showcases share
page content. The landing studio can switch the same navigation between placements.

## Phase 6 — disclosure and attachment display (COMPLETE LOCALLY)

SwTabs, SwAccordion and SwCollapsible are implemented, with the /disclosure showcase and
landing Actions/Details preview. Browser tabs have native bookmarkable fallbacks, keyboard,
vertical/manual/nested views and find-in-page support. Native details handle disclosures
without script. Server-lazy ITab/AjaxTabbedPanel integration remains separate later scope.

SwAttachment and SwAttachmentGroup display files/images with optional thumbnails, actions,
native preview/download triggers, sizes, states and row/grid groups. The /attachments showcase
includes working downloads, image previews/fallbacks, Ajax removal and state updates. The
landing studio has a native illustration preview/download. Upload/storage/authorization stay
application-owned. Phase 6 is complete locally; data components are next.

## Phase 7 — data (COMPLETE LOCALLY)

SwTable/TableBehavior style native cells, compact/grouped/nested tables and sticky first columns.
SwDataTable wraps standard Wicket columns and sortable providers, with accessible sorting,
formatted cells, selection by key, row menus, bulk callbacks and configurable toolbar/empty slots.
SwPagination, SwPagingNavigator, SwPaginationSummary, SwPaginationSize, TableQuery and
SwQueryParameters provide independent paging and bookmarkable filter/sort/page state.
The /tables, /data, /invoice and /pagination showcases exercise native/Ajax paths and session
scoped demo CRUD; the landing studio includes a transient invoice preview. The official
wicket-extensions dependency was added after the user asked to continue with the proposed step.

## Later phases (rough order)

8. **Advanced controls and visualization** (complete at family level locally): custom select, combobox
   with server-side search, calendar/date picker, month view and SVG charts are implemented.
   Charts cover bars/lines/areas, composed and stacked series, axes/grid, legends, tooltips and native data tables.
9. **Visual breadth** (complete locally): all nine skins — Vega, Lyra, Nova, Maia, Mira, Luma, Rhea, Sera and Sketch.
10. **Internationalization** (complete locally): English plus eight translated built-in message sets,
    locale-aware fallbacks and overrides, showcase language selection and Ajax/native verification.
11. **Additional Wicket Oat controls** (implemented locally): busy buttons, linear/radial progress,
    meters, star-rating display and editable rating fields, plus advanced tables (CSV export, column
    chooser, expandable row details and inline edits). Other confirmed gaps include forms, workflows,
    layouts, trees/CRUD, messaging, async content and extra chart types. Scope these deliberately.
12. **Documentation and distribution:** searchable documentation is implemented (53 guides and source-based
    API references). Clover Studio is the implemented interactive agency application at `/studio`.
    Next: host Clover Studio, then agent guidance/llms.txt and a Maven Central release.

Chart variant follow-ups: forecast joins, arbitrary per-series light/dark colors, natural splines
and per-corner bar radii. Native SVG is an intentional alternative to Shadleaf’s Recharts runtime.

For broader Shadleaf parity after/alongside these groups:

- All nine skins now have implementations, with shared light/dark token support.
- Built-in messages now ship in nine languages; additional languages can use application resource bundles.
- Component documentation is implemented locally. Host the runnable interactive Wicket showcase
  alongside the static landing/documentation site.
- Agent skill + llms.txt and a Maven Central release.

Parity review references (checked against the published docs on 2026-11-04):

- Catalog: https://shadleaf.dev/current/components/
- Sidebar depth/layouts: https://shadleaf.dev/current/components/sidebar/
- Skins: https://shadleaf.dev/current/guides/adjustability/#4-skin

After the local app-shell, disclosure, attachment, data, custom select, combobox and calendar/date-picker
implementations, plus month view and charts, no main UI component family is entirely missing.
This is a family-level comparison, not a claim that every part/variant has full parity.

## Phase 8 — advanced controls (IMPLEMENTED LOCALLY)

SwSelect is implemented for single choice, using customizable native select with a progressive
listbox fallback. Groups, registered icons, separators, disabled choices, placeholders, both sizes,
normal/Ajax validation and dynamic choices are covered by /custom-select and the landing plan field.
SwCombobox/SwMultiCombobox now cover local/server search, removable chips, native fallbacks and normal/Ajax
validation in /combobox and the landing Project teams field. SwCalendar/SwRangeCalendar and
SwDatePicker/SwDateRangePicker cover native date-backed grids and popups, with normal/live/Ajax
validation in /calendar and working landing previews. SwMonthView provides a native agenda with
custom day slots, links and independent months in /month-view. SwChart/ChartSeries provide native
SVG bars/lines/areas, mixed/stacked series, keyboard tooltips and exact data in /charts.
The landing previews both. All nine skins, internationalization and component documentation are implemented
locally. Next: a hosted interactive showcase, then tooling and release preparation.

## Working agreements

- Verify current versions (Wicket, Tailwind, Maven plugins, Node LTS) on
  Maven Central / npm before using them.
- Ask before adding any dependency to the library module.
- Use plan mode before each new component group; small, focused commits.
- For every component: read the Wicket Oat equivalent (API + tests) and the
  Shadleaf equivalent (markup + CSS) before writing code.
- Record every decision under Conventions as soon as it is made.

## Conventions

### Clover Studio application (2026-11-06)

- Current Studio languages are English/German only, per user request after the initial component-only
  switch appeared to do nothing. Localize all interface text with StudioPage resource keys. Keep the
  English .properties ASCII/Unicode-escaped and German .utf8.properties UTF-8. Unsupported old session
  locales fall back to English; the library's nine language bundles remain unchanged.
- Activity records store keys/arguments so switching languages also translates past system activity.
  Model errors expose stable message keys; translate at the UI boundary. Preserve entered project/task
  content and client names. The native Apply preferences form preserves filters and skin selection.
- Project filtering uses a native GET form with stable `view`, `q`, `status`, `sort` parameter names and
  stable choice values. Translate labels, not query tokens. StudioPageMapper now directly renders normal
  bookmarkable Studio GETs with NEVER_REDIRECT (GET200). Keep listener/hybrid/POST/Ajax requests on
  Wicket defaults; never change the global rendering strategy just to simplify navigation.
- Studio interactions now use live models and per-component ViewState snapshots, adding only changed
  components to AjaxRequestTarget and skipping descendants already covered by an updated parent. Commands
  must not call setResponsePage. The sole remaining use is the native/no-script preferences fallback.
  Task rows retain their last-rendered done state for conflict checks. Collection changes replace the
  collection region; task completion replaces one row, progress/count and dependent controls, not the view.
- Internal links and search progressively enhance native hrefs/GET forms with Wicket Ajax, updating history
  through studio.js. Ajax transport uses unique studioQ/Status/Sort names to avoid stale callback-URL filters.
  Accept submitted input values without replacing their DOM; history/clear updates inputs when needed.
  Use page-level Ajax history for cross-view navigation. Downloads remain native and generation-checked.
- Preference updates rebuild localized regions and preserve invoice table sort, page size and visible columns.
  Ordinary Shadwick Ajax resources intentionally pin the original page skin. Studio's global skin action
  explicitly loads the public CSS resource URL, carrying the document's style nonce, and activates it after
  load. Keep prior bundles disabled for switching back. Do not replace a running user's executable JAR.

- `/studio` is the composed Wicket application; `/` retains focused component references. Use stable
  `view`/`project` bookmarkable parameters. Dedicated composition CSS belongs to the examples module,
  never the library skins. Keep demo utilities separate from the product workflow. Reuse the clover SVG.
- Session metadata owns StudioWorkspace. Use immutable snapshots, bounded collections and fresh ID lookups
  for callbacks. Recheck generation after reset, project revisions on edit, task state on completion and
  handoff eligibility on confirmation. Call Session.dirty() after changes; redirect feedback is session-owned.
- Generated downloads recheck the current session/generation. ZIP entry paths and filenames are controlled.
  This is session-lifetime sample data; no real payment gateway, invoice issuing, upload storage or database.
- Dialog form-level feedback must be fenced inside the modal; inline field errors remain at the fields.
- Avoid two serializable bound references to inherited Component.getMarkupId() on different receiver types
  in SwSidebar: Java lambda deserialization can cast the label to SwSidebar. Explicit lambdas give distinct
  identities. Core and composed-page regression tests cover round-trip serialization and restored actions.
- StudioBrowserTest covers the journey in Chromium/Firefox and all four screens in every skin at desktop
  light/mobile dark sizes. Wait for saved state and finite animations, rather than racing redirects. Use
  distinct bookmarkable page URLs for separate-tab tests. The CI browser setup installs Firefox as well.

### Component documentation (2026-11-06)

- Repository Markdown lives in docs/guides and docs/components. docs/catalog.json declares routes,
  groups, descriptions, Java API sources and actual showcase source classes. All public Sw component
  files must appear in the API registry. Package-private helpers are not public documentation APIs.
- Keep the README's established headings as compatibility entry points. Use source-relative .md links
  between guides; the build rewrites them to subpath-safe HTML routes. Examples/API source links point
  at the GitHub main branch. Generated output is ignored and must not be edited or committed.
- site/docs builds the static reader with Node built-ins. Escaped, deliberately bounded Markdown;
  fenced code is highlighted without executing it. No raw HTML or unsafe URL schemes. API signatures,
  nested/root enums, record declarations and useful descriptions come from current Java source.
- Complete examples under docs/examples match their printed fences byte-for-byte. DocumentationExamplesTest
  compiles them with --release 17 and tests native/Ajax validation. DocumentationBrowserTest reuses the
  existing Playwright/axe dependencies and the Node preview on an ephemeral port; no new dependency.
- Search/copy are progressive enhancements. Navigation, reading and API disclosures work without script.
  Default appearance follows the OS; print uses light tokens. Respect project-subpath URLs, keyboard
  focus, 320px layouts and copy labels matching visible state. Search failure falls back to navigation.
- Component docs are static. The publicly hosted interactive Wicket application is a separate next task.
  This work does not imply commit/push/publication. Use the Sites skill's local workflow for the repo.

### Busy buttons and progress/meter/rating controls (2026-11-06)

- User selected the first proposed component group: busy Ajax buttons, linear/radial progress,
  native meters, rating display and rating input. Advanced table features remain a later decision.
- Match Wicket Oat constructors/models and setBusyIndicator API; use component-specific shadcn
  styling and all nine skins. No new library/runtime dependency. Native progress/meter semantics,
  SVG for radial progress and display stars, native radio inputs for the rating field.
- Busy handling is opt-in, installed once through Shadwick.install and Ajax request listeners.
  Mark before queueing, reject repeated activation, restore on success/error/cancel, preserve focus
  and original attributes. Do not disable native submit controls before Wicket serializes the form.
  Use a separate external runtime, no inline handlers/styles, and a localized loading message.
- Clamp finite progress/rating values to their bounds; reject nonfinite numbers and invalid maxima.
  Null progress is indeterminate; null meter uses the native minimum (meters are never indeterminate).
  Read dynamic numeric models once per render. Preserve native min/max/low/high/optimum for meters.
- Ship every new built-in message in all nine languages. New pages cover normal/Ajax validation,
  replacement, failure/cancel handling, native no-script operation, RTL, reduced motion and forced colors.
- References read: Oat AjaxBusyBehavior/install hook, Progress/Meter/RadialProgress/Rating/RatingField
  and tests; Shadleaf button loading structure/docs; existing shadcn tokens/skin geometry and field anatomy.


### Internationalization (2026-11-06)

- Use Wicket's existing component/page/application resource lookup and session/component locale.
  Ship class-scoped _de/_es/_fr/_it/_ja/_nl/_pt/_hr.utf8.properties. Follow Wicket Oat's
  UTF-8 naming and original seven-language set, extended with Croatian at the user's request. English base bundles remain the fallback; regional locales
  inherit their language. No global locale, custom message source or new dependency.
- Translate built-in controls, labels and validation, including JS messages emitted in data
  attributes. Application-provided titles, row actions, field labels and demo prose stay owned
  by the application. Preserve all {0} and ${variable} placeholders and literal date wire formats.
- Behaviors on caller-owned markup and table parts use LocalizedText to find the correct
  library bundle as fallback while retaining page overrides and any configured application loaders.
  Preserve Wicket loader order: a global application override can explicitly prepend
  ClassStringResourceLoader(App.class), following Oat; do not silently install a new loader. Calendar locale
  overrides continue to control date formatting; message lookup follows the component locale.
- Default combobox placeholders are resolved during rendering for both native and enhanced
  fields; explicit empty/null/custom placeholder models keep their existing meaning. Data-table
  names and default action headings are also resources. Feedback toast regions and close
  controls are localized without changing persistent timing or legacy severity styling.
- The Wicket showcase includes locale selection and a dedicated internationalization example.
  The static marketing site stays English; document the shipped languages and next roadmap task.
- References read: Wicket Oat's TranslationsTest, OatHtml/OatHtmlTest and UTF-8 bundles;
  Shadleaf's MessageSource, message bundles and placeholder/override tests; Wicket 10.11.0
  ComponentStringResourceLoader source. Shadleaf's Spring message source is not used in Wicket.

### Remaining skins (2026-11-06)

- Each of Maia, Mira, Luma, Rhea, Sera and Sketch owns a complete identity and independent
  standalone/embedded entries. Adapt Shadleaf's full skins with sw- component/custom-property
  names, preserving native Wicket form wrappers, logical sheet edges, checkbox/menu indicators,
  upload rows, data-table controls and native SVG charts. Shared structures, tokens and JS stay
  unchanged. No Maven/npm dependency or CSP change.
- Maia has 36px pill controls and spacious cards; Mira 28px controls and dense type; Luma
  36px soft borderless fields; Rhea 32px soft controls with tighter spacing; Sera 40px underlined
  fields, square corners and uppercase type. Sketch uses 36px controls, asymmetric pencil
  borders, hatched skeletons and offset shadows. Keep each skin's focus/error/readonly styling.
- Sketch bundles the unmodified Balsamiq Sans Latin regular/bold WOFF2 and OFL license under
  dev/jbaby/shadwick/fonts/balsamiq-sans. Stylesheets use relative same-origin URLs. Standalone
  sets the body's font; embedded scopes it to sw- components and leaves host typography alone.
  The landing copies the fonts alongside assets and retains its marketing font.
- All showcase layouts expose every enum skin. site/skins.mjs is the build/check catalog for
  nine content-versioned CSS options; the landing applies extra card shadows only to Vega/Lyra.
- References read: Shadleaf ac29987's nine skins, adjustability guide and SkinCompletenessTest;
  Wicket Oat's OatTheme/OatThemeTest and existing Shadwick Skin/resource/CSS/browser tests.
  Oat's color themes do not have equivalent geometry skins to port.

### Nova skin (2026-11-05)

- Skin.NOVA is an independent, complete identity file and two CSS entries, not a cascade over
  another skin. Shared component structure and color tokens are unchanged. Port Shadleaf's
  compact geometry while preserving Wicket-specific wrappers, legible contrast mixes and native
  input/Ajax/overlay behavior. No dependency or CSP change.
- Default controls are 32px, small buttons/selects 28px, large buttons 36px; fields/buttons use
  larger radii and controls drop their shadows. Input text stays 16px on mobile. Disabled fields
  get Nova's tinted surfaces; read-only states and focus/error rings remain accessible.
- Cards use 16px default spacing (12px small), with a divided muted footer flush to the bottom.
  Wicket dialog/alert-dialog form wrappers use 16px gaps/padding and flush muted footers; adapt
  close-button positions to those wrappers. Calendar cells are 28px with 8px padding; menus,
  selects/comboboxes, disclosures, sidebar spacing and pagination follow Nova's compact identity.
  Month view and native chart parts are included.
- All showcase skin controls (including the custom app/header layouts) expose Nova. The landing
  copies Nova's actual embedded bundle and derives its label from the selected option; custom
  decorative card shadows exclude Nova. Browser tests use Skin.values() by default, with an
  optional shadwick.browser.skins filter for focused verification of one skin.
- References: Shadleaf's complete Vega/Nova skins and geometry differences, Skin enum and
  SkinCompletenessTest; existing Shadwick skin/store/resource and CSS tests. Wicket Oat has no
  matching Nova skin API to port.

### Month view and charts (2026-11-05)

- SwMonthView is a server-rendered table, with locale-aware weeks, optional outside days/week
  numbers and typed Day context for custom Wicket content. Month links preserve the current
  page's parameters; independent views can name their own month parameter. Optional day links
  use Wicket's bookmarkable page URLs. No JavaScript is needed for navigation or day content.
- SwChart<T> follows Oat's data model/category extractor/named-series API and SVG/data-disclosure
  approach, adapting Shadleaf's skins, tooltip and legend structure. ChartSeries<T> composes
  bars, lines and areas, including grouped/stacked bars/areas, positive/negative domains, gaps,
  dashes, points and linear/step/monotone curves. No new dependency: no React/Recharts runtime.
- All category/value extraction is snapshotted once per request and detached after rendering.
  Formatted labels/values are escaped. Native exact-value tables work without script; optional
  vanilla keyboard/pointer tooltips read those same cells and clean up after Ajax replacement.
  Series colors use five chart tokens through CSS classes, with no inline style/JSON script.
- Date-only categories are formatted on the server without zone conversion; instant categories
  use an explicit ZoneId (UTC by default). Value formatting supports locale numbers, compact,
  integer, percent, currency and serializable application formatters.
- References read: OatChart/OatXYChart/OatBarChart/OatLineChart and TierB chart tests; Shadleaf
  month-view template/structure/both skins and binding/hit-area tests; chart docs, runtime,
  formatter/loader, structure/both skins and server/browser tests. Oat has no month-view peer.

### Calendar and date picker (2026-11-05)

- SwCalendar/SwDatePicker bind LocalDate through native ISO date inputs; matching
  SwRangeCalendar/SwDateRangePicker bind an Oat-style DateRange(from,to) through two
  date inputs. BaseSwField owns labels/feedback; ranges allow open ends unless required,
  reject reversed bounds on the server, and update the model atomically. Calendar clicks
  sort the endpoints. Native date fields remain available; no new dependency or CSP change.
- CalendarBehavior supports caller-owned native markup. Shared Java settings configure
  captions, 1–12 visible months, outside days, week numbers/locale week rules, month limits,
  today/default month and disabled dates (date/range/weekday). The same rules validate
  server input, including range interiors. Bounds/readonly also validate on the server.
- Port Shadleaf structure/skin identity and small vanilla runtime, with no Alpine/inline
  styles. Use Gregorian UTC arithmetic/formatting for date-only navigation, with today
  taken from the explicit configured clock/date or the user's local day. Native values
  stay ISO. WeekFields/Locale configure labels and weeks; no time zone conversion of values.
- Date pickers compose the existing native popup focus/dismissal patterns. Keep native
  fallbacks, normal/Ajax validation, reset, strict CSP, RTL, forced colors, reduced motion,
  dynamic attributes, Ajax cleanup and native modal/sidebar nesting covered.
- References: OatDateField, OatDateRangeField/DateRange and their conversion/required/order
  tests; Shadleaf calendar/date-picker docs, templates, both CSS structures/skins, full
  runtimes and binding/browser tests. Oat has no custom calendar/picker counterpart.


### Combobox (2026-11-05)

- SwCombobox<T> and SwMultiCombobox<T> wrap real DropDownChoice/ListMultipleChoice
  components and share BaseSwField feedback. Native selects remain without script/APIs.
  Editable input/listbox and removable chips follow Shadleaf, with small vanilla JS,
  implicit CSS anchors, both independent skins and no dependency/CSP change.
- Keep familiar choice-model/renderer constructors and Oat-style search/display/maxChoices
  setters. Remote setChoices(query -> ...) requires stable setChoiceId and setChoiceResolver
  callbacks. Resolve and validate submitted IDs afresh; do not accumulate a page cache of
  results. Search is a bounded Wicket-owned JSON callback, not an arbitrary HTML endpoint.
- Match keyboard/IME, filtering without case/accents, explicit selection (no free text),
  clear/reset, multiple chip keys, live status, Ajax replacement/cleanup and stale-request
  cancellation. Search leaves chosen options intact. Application resolvers own authorization.
- References: OatAutoCompleteField/OatMultiSelectField and LaterRoadmap/TierC tests;
  Shadleaf combobox docs/templates/CSS/both skins, full runtime and binding/browser tests.


### Custom select (2026-11-05)

- SwSelect<T> retains SwDropdownChoice's Wicket constructor/model/renderer shape and
  BaseSwField feedback paths. It is a single-choice control; searchable/multiple
  selection is provided by the combobox group. Existing native selects stay available.
- A real select submits and validates in every mode. Customizable select browsers use
  base-select; other Popover API browsers progressively get a combobox/listbox. Without
  script or those APIs the platform picker remains. No dependency or CSP change.
- Group/icon/separator/disabled callbacks decorate the current flat choice model,
  retaining renderer IDs and order. Conversion rejects unknown/disabled IDs before
  consulting a custom renderer. Placeholder defaults to hidden; nullValid makes it
  an explicit clearing choice. Both sizes and skins use common structural CSS.
- A separate small vanilla select resource owns its own initialization/cleanup and
  Ajax observation. Native form reset/change/feedback remain the source of truth.
  Popup placement uses implicit CSS anchors with a viewport fallback, never inline styles.
- References read: OatDropdownChoice and ChoiceFieldsTest; Shadleaf select docs,
  templates, CSS, both skins, full runtime and keyboard/pointer/reset browser tests;
  cached official Wicket AbstractChoice/AbstractSingleSelectChoice/DropDownChoice sources.


### Data tables and pagination (2026-11-05)

- Reuse Wicket's native columns/providers/DataTable through official wicket-extensions
  at the managed Wicket version (10.11.0). The user asked to continue after that dependency
  was proposed. Basic SwTable/TableBehavior, generic SwPagination and paging/query helpers
  themselves use wicket-core interfaces; the library brings extensions transitively.
- Port Shadleaf's table and pagination structure and independent skin geometry. Rows,
  cells and grouped headers stay native HTML; direct-child selectors isolate nested tables.
  Support compact density, sticky first columns, RTL, forced colors, print and overflow.
- TableQuery preserves decoded, repeated/unrelated query values with PageParameters copies.
  Paging changes only the page; sorting/size/search reset that table's page. Qualifiers isolate
  multiple tables. Bookmarkable mode uses real page navigation and browser history; default
  stateful mode can use Ajax fallback links. Do not simulate history with unsynchronized DOM.
- Application callbacks own row CRUD and authorization. Selection can use stable keys/loaders;
  named controls, current-sort/page semantics, empty results and model detachment are required.
  No commit, push or publication is implied.

### Attachment displays (2026-11-05)

- SwAttachment owns escaped title/description and optional thumbnail/icon media, with
  caller-supplied actions and trigger slots. AttachmentBehavior also styles caller-owned
  markup. States are presentation only; applications supply status text and own storage,
  authorization, downloads and any live announcements. Never retain request FileUpload objects.
- Native links/downloads and named buttons remain separate siblings; the optional stretched
  trigger is last and behind actions. Reuse button naming checks once per control.
- SwAttachmentGroup is a caller-owned row/grid container. Named rows take focus for native
  scrolling; grids wrap. Port Shadleaf geometry independently to both skins, including motion,
  forced-colors and RTL. No dependency, CSP exception or publication is implied.

### Disclosure components (2026-11-05)

- SwAccordion follows OatAccordion's generic ListView constructors, exclusivity setter
  and populateItem/of factories. Its native details rows use transparent trigger/heading/
  body containers so callback-added title/content retain ordinary Wicket paths. Fresh
  native group names per render avoid old/new Ajax markup joining during replacement.
- SwCollapsible is a details Panel with a first-child summary and an arbitrary content
  slot. Initial open state is server configuration; native user toggles do not post it.
  Closed disabled disclosures omit content, and disabled summaries leave the tab order.
- SwTabs follows OatTabs' caller-owned container and paired populateTab/populatePanel
  lists. Render all panels, server-selected ARIA/IDs and hidden state; add browser keyboard
  navigation, scoped nested tabs, find-in-page and optional bookmarkable tab URLs. Keep
  client selection separate from server initial state and reset on whole Ajax replacement.
- Use Shadleaf's component/skin styles, native disclosures and tabs pattern. No new
  dependency, inline script/style, custom element or CSP exception. OatTabbedPanel's
  lazy ITab/AjaxTabbedPanel API remains separate future integration. Phase 7 now adds
  wicket-extensions, making its dependency available for that later tab API.

### App layouts and navigation depth (2026-11-05)

- SwAppLayout follows OatAppLayout's WebPage constructors and protected name/title,
  menu/link and header/footer factories. Compose the existing SwSidebar, SwSiteHeader,
  SwSkipLink and ThemeBehavior; content stays in subclass wicket:extend markup. Sidebar
  and header placement share one navigation element, including the mobile panel.
- Preserve MenuItem's five/six-argument constructors and groups. Add explicit submenus
  with leaf children, including a page link with a sibling disclosure; group headings
  remain separate from submenu summaries. Keep native details/summary and ordinary links.
  Hooks supply named item/group action controls; do not put buttons inside summaries.
- Header placement follows Shadleaf: desktop row scrolls inside its available space,
  never collapses or writes state, and panel-only entries stay mobile-only. The mobile
  panel retains existing focus, inertness and nested-menu handling. No new dependencies
  or CSP changes. This group does not imply commit/push/publication.

### Naming

- Java prefix `Sw` for components (`SwButton`, `SwSubmitButton`, `SwTextField`,
  `SwTextArea`, `SwDialog`, `BaseSwField`). Behaviors are unprefixed, as in
  Wicket Oat (`ButtonBehavior`). Entry points: `Sw.Behaviors.*`,
  `Sw.Components.*`. Install: `Shadwick.install(app)` → `ShadwickSettings`.
- Packages mirror Wicket Oat: `dev.jbaby.shadwick` (install, settings, `Skin`,
  `SkinStore`, `AccessibilityChecks`, `Sw`), `.behaviors`, `.components`,
  `.components.form`, `.util` (`Serializable*` functional interfaces).
- Variant/size enums are nested in the component's behavior
  (`ButtonBehavior.Variant`, `ButtonBehavior.Size`). Java constants keep Oat's
  words (`SMALL`, `LARGE`, `ICON`); their values are shadcn's (`sm`, `lg`,
  `icon`). `DEFAULT` writes no attribute (CSS matches `:not([data-variant])`).
- CSS classes: `sw-<component>[-<part>]` (`sw-dialog-header`, `sw-field-error`).
  `data-sw-*` for Shadwick's own state attributes (`data-sw-instant`,
  `data-sw-closing`).
- Resource keys: `<ClassName>.<key>` in `<ClassName>.properties` next to the
  class (`SwDialog.cancel`).
- Modules: the library lives in `shadwick-core/` with artifactId `shadwick`.
  `shadwick/` at the root is reserved for the agent skill.

### Where Shadwick follows shadcn over Wicket Oat

- Dialog: title + description (`setTitle`, `setDescription`; Oat has
  `setHeader`), the × close button shown by default and placed last in the
  DOM, so the browser focuses the first field on opening (no JS for that).
  The trigger button is `setTrigger(label)`, not Oat's
  `(id, triggerLabel, header)` constructor, whose two Strings were easy to swap.
  A failed confirm leaves field errors to the fields (in place, focus kept) and
  re-renders the form only for errors reported elsewhere.
- Field: description and error are separate elements (`p.sw-field-description`,
  `div.sw-field-error`); Oat swaps the hint for the error. The error element is
  always rendered (`role="status"`, empty while valid, hidden via `:empty`) so
  in-place updates are announced; `aria-describedby` lists description + error.
- Accessibility checks: `AccessibilityChecks.FAIL` (default in DEVELOPMENT),
  `WARN` (logs; default in DEPLOYMENT) or `OFF`, instead of Oat's boolean.

### CSS structure

- Shadleaf's split, not deltas: `components/<name>.css` hold structure only
  (layout, display, states, forced-colors); each `skins/<skin>.css` holds the
  full look of every component (radius, border, colours, sizes, type, ring,
  motion). Skins never define colours; they consume tokens. Reason: deltas on
  top of vega would have to undo vega rule by rule; this ports 1:1 from Shadleaf.
- Every rule is in `@layer components` and written with `@apply`; entries use
  `@import "tailwindcss" source(none)`, so no utility class is ever emitted
  (`CssBundleTest` checks this).
- A skin rule that must beat a size/variant rule of another component uses the
  same specificity and comes later (e.g. `.sw-button.sw-dialog-close`).
- Adding a component: `components/<name>.css` + a block per skin + an import in
  `components.css`. Adding a skin: `skins/<skin>.css` + `entries/shadwick-<skin>.css`
  + a `Skin` constant.
- Each CSS file adapted from Shadleaf says so in its header comment (Apache-2.0,
  see NOTICE).

### Build

- `scripts/build-css.mjs` builds every `entries/*.css` into
  `target/classes/dev/jbaby/shadwick/css/<entry>.css` and `<entry>.min.css`
  (Wicket serves `.min` in deployment mode). It runs the CLI with the current
  Node, without a shell, and pipes the CLI's stderr to stdout (Maven would log it
  as errors).
- `npm run watch:css` watches `src/main/tailwind` itself (Tailwind's `--watch`
  with `source(none)` only sees the entry file) and rebuilds the unminified
  bundles.
- `npm ci --ignore-scripts`: the Tailwind CLI's native parts come prebuilt.
- Versions verified 2026-10-27: Wicket 10.11.0, Tailwind 4.3.3, Node v24.22.1,
  frontend-maven-plugin 2.0.2, Spring Boot 4.1.2, junit-bom 6.1.3,
  Playwright 1.64.0, axe-core-playwright 4.14.0.

### Wicket integration

- `Shadwick.install` adds the stylesheet of the current skin to every page
  (`setAddResources(false)` opts out); every component and behavior also renders
  it in `renderHead`. Nothing is added to Wicket's CSP: no `data:` URLs, inline
  SVG icons in markup.
- Skins: `setSkin(Skin)` is the app-wide default; an optional `SkinStore`
  (`load()`/`save()`, shaped like `OatThemeStore`) overrides it per user, and
  `setCurrentSkin(Skin)` switches it (clear error without a store). The skin is
  worked out once per request; a page keeps the skin of its last full render
  for its Ajax updates (page metadata), so it never loads two skins.
- `ShadwickSettings.get` reads application metadata without a lock; only the
  first creation is synchronized.
- Accessibility checks under `WARN` run once per button (page class + path,
  list indexes folded to `*`) and log at most once; `FAIL` checks every render.
- Every public setter taking an `IModel<String>` also takes a `String`.
- `BaseSwField.add` routes validators and every form-component behavior
  (`AjaxFormComponentUpdatingBehavior`, `FormComponentUpdatingBehavior`,
  `AjaxFormValidatingBehavior`) to the input. The input's type comes from an
  `IObjectClassAwareModel`, else from the model's value, else `setType(...)`.
- The jar carries `NOTICE` and `LICENSE` in `META-INF`.
- `shadwick.js` (`Shadwick.*`): an IIFE guarded by `window.Shadwick`, only
  document-level delegated listeners (so it survives Ajax replacement), reads
  only attributes. Holds `fieldFeedback`, `showDialog`, fallbacks for invoker
  commands and `closedby="any"`, and Enter-in-footerless-dialog. Rendered by the
  components that need it, not on every page.
- Server → browser calls go through `target.appendJavaScript` /
  `OnDomReadyHeaderItem` with `JSONObject.quote(...)` arguments, as in Wicket Oat.

### Tests

- Library: WicketTester + JUnit Jupiter + AssertJ, one test class per Wicket Oat
  test it ports (`SwDialogTest` ≈ `OatDialogTest`, `AjaxFieldErrorsTest`, …).
  Test pages implement `IMarkupResourceStreamProvider` or have `.html` in
  `src/test/resources`.
- After `startComponentInPage`, `getLastResponseAsString()` holds only the
  component; assert on page-level things (stylesheets, CSP) with a real page.
- `Markup.of(...)` needs the tester's application: never in a static field.
- Every component test asserts no `style=` and no `on*=` attributes.
- Examples: `ExamplesPagesTest` (WicketTester on `ExamplesApplication`) runs in
  the build; `ShowcaseBrowserTest` (`@Tag("browser")`, `-Pbrowser`) runs
  Playwright + axe-core (WCAG 2.2 AA) and fails on console errors, for every
  page × skin × light/dark.

### Phase 2 display and feedback

- `Bundle.STANDALONE` remains the default; `setBundle(Bundle.EMBEDDED)` is an
  install-time setting independent of the skin. Embedded entry files import
  `tailwindcss/theme.css` with `theme(inline)` and `utilities.css` with
  `source(none)`, then tokens, component rules and the skin. No preflight or page
  base import. Tailwind's `--tw-*` runtime properties remain where needed for
  effects; its theme variables are compiled inline. See Tailwind's official
  preflight documentation: https://tailwindcss.com/docs/preflight#disabling-preflight.
- `Shadwick.css(skin)` preserves the standalone resource reference;
  `Shadwick.css(skin, bundle)` selects either. Embedded filenames add `-embedded`.
- Display components follow Wicket Oat's container/Label APIs; existing components
  can use the matching `Sw.Behaviors.*` factories. `StyledBehavior` contributes a
  component class and Shadwick's stylesheet. Cards retain caller-owned markup,
  using `sw-card-*` parts. Badge variants are DEFAULT, SECONDARY, OUTLINE, GHOST,
  DESTRUCTIVE and LINK; alert variants are DEFAULT and DESTRUCTIVE. No shared enum.
- `SwAlert` is an escaped Label, with a block layout for plain text. Composed
  callouts use `AlertBehavior` and `sw-alert-title` / `sw-alert-description`.
- Avatars read their URL and initials once per request. Their image and fallback
  share Shadleaf's layered anatomy; delegated load/error listeners plus a DOM
  observer handle source changes and cached images inserted over Ajax. `setLabel`
  names whichever representation is currently visible. Pictures without a label
  or initials are decorative. Only same-origin pictures work under the default CSP.
- `SwIcon` ports Wicket Oat's Lucide registry and inline SVG API, with the bundled
  loader-circle added for `SwSpinner`. Names are checked, custom trusted shapes
  can be registered, labels are escaped, and decorative SVGs are aria-hidden.
  Skeletons are decorative; spinners are named statuses. Skeleton shapes add
  CIRCLE to Oat's LINE/BOX. Separator and kbd have no Oat equivalent; their anatomy
  and semantics follow Shadleaf.
- `SwEmptyState` owns its title, reactive optional description and optional icon;
  `setContent` accepts a component with `CONTENT_ID` for application actions.
- `SwFeedbackPanel` ports Oat's fenced panel and automatic Ajax refresh/clear.
  `NotShownInlineFilter` skips the first error of a `BaseSwField` input; extra
  errors still reach feedback. Toasts respect Shadwick's fence metadata and mark
  consumed messages rendered, including session messages after navigation.
- Feedback uses the alert's shadcn variants (error/fatal → DESTRUCTIVE; all
  others → DEFAULT), with `data-level` preserving severity. Toasts are a plain
  column in a manual popover, remain until dismissed, and use textContent plus
  quoted script arguments. No inline styles, timed expiry, stack geometry or
  client framework. General-purpose toast APIs belong to the overlay group.
- No dependencies were added. Reference checkouts used for Phase 2 live outside
  the repository, under `/tmp/wicket-oat` and `/tmp/shadleaf`.


### Landing page

- `site/` is a static GitHub Pages landing page, independent of the Wicket showcase.
  HTML/CSS/vanilla JS, with locally hosted Geist (OFL license in the font directory)
  and an SVG icon sprite. No site runtime dependencies or third-party asset requests.
- Build core first, then run `node site/build.mjs` and `node site/check.mjs`. The
  build copies the actual minified embedded Vega/Lyra/Nova bundles into ignored `site/dist/`.
  The studio changes their stylesheet reference and scopes dark mode to the preview.
- Demo interactions are browser-only, explicitly identified as such. Inputs aren't
  submitted or persisted. Preserve the accurate snapshot/Maven Central status.
- Preview at a project subpath with `node site/serve.mjs`; verify narrow mobile
  layouts, both skins/modes, keyboard modal operation and code copying after edits.
  Honor reduced motion and keep secondary text at AA contrast.
- `.github/workflows/pages.yml` builds/checks PRs and deploys main to GitHub Pages
  through the official Pages artifact workflow. The repository must use GitHub
  Actions as its Pages publishing source. See `site/README.md`.

### Phase 3 choice fields

- `SwCheckBox`, `SwSwitch` and `SwRadioChoice<T>` retain Wicket Oat's constructor
  shapes, plus explicit String-label conveniences consistent with our text fields.
  They use `BaseSwField`'s model binding and in-place Ajax feedback; no new dependency
  or client-side state manager is needed.
- Checkbox and switch wrappers use `sw-field[data-orientation=horizontal]`, with
  the input in column one and its real `<label>`, description and error in column
  two. The checkbox's inline Lucide check/minus indicators share the native input's
  grid cell; CSS follows `:checked` / `:indeterminate`. No image URLs or CSP changes.
- `SwSwitch` is a native Wicket `CheckBox` with `SwitchBehavior`; the browser owns
  checked state. There is no separate aria-checked attribute to keep synchronized.
  Size DEFAULT/SMALL maps to shadcn's default/sm. `Sw.Behaviors.swSwitch()` mirrors
  Oat's `oatSwitch()` (switch itself is a Java keyword). Switch thumb gradients follow
  RTL and use the internal `--sw-switch-thumb` variable. Forced colors restore the
  browser's checkbox/radio rendering and hide decorative check indicators.
- Native Wicket checkbox semantics are preserved: setRequired does not require a
  true value. Use a Boolean validator for consent; the showcase demonstrates it.
- RadioChoice generates its native options, escaping renderer labels and IDs.
  WRAP_AFTER labels receive `sw-label sw-radio-label`; each radio receives
  `sw-radio-group-item`, aria-describedby and aria-invalid through Wicket's
  additional-attribute hooks. The fieldset legend also names the inner radiogroup
  through aria-labelledby. No extra break separators. setInline(true) emits
  data-orientation=horizontal, otherwise the group stacks.
- Radio choice updating behaviors route through BaseSwField.add to the input just
  as single-control updating behaviors do. Error-only Ajax responses change ARIA
  attributes on the group and all its options without replacing them or their focus.
- Reference APIs/tests: Wicket Oat's CheckBox, Switch, RadioChoice, ChoiceFieldsTest
  and CoreFormTest. CSS/anatomy: Shadleaf checkbox, radio-group and switch components,
  with full visual identity blocks in both skins. General-purpose checkbox groups,
  custom choice widgets and other field types remain separate work.


### Phase 3 everyday inputs

- SwEmailField, SwPasswordField, SwSearchField, SwTelField, SwUrlField and
  SwNumberField<N> follow the corresponding Oat constructor shapes (plus String
  labels with inherited models). BaseSwInputField now adds prefix/suffix and datalist
  support; BaseSwField retains common model binding, labels and inline feedback.
- EmailTextField, UrlTextField, PasswordTextField and NumberTextField retain native
  Wicket validation/conversion. Html5TextField supports search/tel without forcing
  text back to type=text. Every input reuses sw-input and the existing Shadleaf
  geometry in both skins; no new dependency, CSS fork or custom client state.
- Password defaults remain Wicket's: required, reset on detach and never echoed.
  The showcase never prints a password in its summary. Ajax feedback keeps the
  existing browser input while full renders clear it.
- SwNumberField offers setMin/setMax/setStep for values or models. Wicket enforces
  min/max and uses HTML numeric formatting; step is a native browser hint only,
  including the zero/any convention. Applications enforce step alignment explicitly
  when needed. Empty numeric models need setType or an object-class-aware model.
- Constraint models passed through SwNumberField are detached by the wrapper:
  Wicket 10.11 NumberTextField does not detach those models itself.
- InputPage (/inputs) uses novalidate forms to expose server validation, and covers
  email, URL, password, search, telephone, integer seats and decimal budgets.
  Browser checks assert invalid feedback and Ajax password/focus preservation;
  JVM tests also cover malicious text number submissions and German-locale decimals.

### Phase 3 native selections

- This group adds SwDropdownChoice<T>, SwListMultipleChoice<T> and
  SwCheckBoxMultipleChoice<T>, following Oat's constructor/model/renderer shapes.
  Custom searchable select and combobox widgets remain in the advanced phase.
- Selects keep Wicket's native DropDownChoice and ListMultipleChoice semantics,
  including null selection, collection updates, option escaping and choice-model
  detachment. SwListMultipleChoice.setMaxRows controls visible list-box rows.
- Native-select structure and both complete skin blocks come from Shadleaf. A
  static inline Lucide chevron overlays only the single select. The browser draws
  the options in Canvas/CanvasText colours. No script or CSP change is required.
- Checkbox groups retain Wicket's generated wrapped labels and escaped option IDs.
  Static prefix/suffix markup adds each indicator as a sibling overlay; CSS follows
  the native checked/disabled state. The fieldset legend also names the inner group,
  and each checkbox references the help/error. aria-required is omitted on role=group
  because that role does not support it; required still enforces at least one choice
  on the server. Native required on every checkbox would incorrectly require all.
- Required multi-selections also check the converted collection. Wicket 10.11's raw
  required check alone accepts forged non-empty IDs that resolve to no options.
- SelectPage (/selections) exercises normal/Ajax submit and each control's immediate
  change behavior, plus custom stable IDs, disabled controls and both group layouts.
  Browser coverage includes selection/focus preservation, keyboard multi-selection,
  forced-colour fallback and axe in both skins/modes. The landing form has a native
  plan selector using the same embedded bundles.
- Multiple-select selected rows use Highlight/HighlightText system colours, including
  while blurred: the otherwise necessary Canvas option background hides Chromium's
  inactive selection fill. Checkbox groups have explicit spacing below the legend.

### Phase 3 native date/time fields

- SwDateField, SwTimeField and SwDateTimeLocalField use LocalDate, LocalTime and
  LocalDateTime models. SwMonthField and SwWeekField keep Oat's String model API.
  Constructor shapes match Oat, plus inherited-model String labels like other fields.
  They reuse BaseSwInputField for optional text addons; custom calendar pickers are later.
- Html5TextField accepts an optional per-field converter. Existing search/tel inputs
  still use their application's converters. Html5Converters fixes the date/time wire
  format without changing the application-wide converter locator or session locale.
- Server conversion checks HTML syntax and calendar validity, including month/week
  strings, positive years, leap days and whether an ISO week-year has week 53.
  Local date/time has no offset or timezone; time precision renders at milliseconds,
  omitting zero seconds. Format rules follow the HTML Standard's date/time microsyntaxes.
- Min/max/step HTML attributes remain browser hints on these TextFields. Applications
  must add Wicket validators for bounds or scheduling rules; these fields do not
  promise server enforcement of arbitrary input attributes.
- Native controls reuse Shadleaf's sw-input markup and both skins. The browser owns
  the picker and localized presentation; no date-picker dependency or custom JS.
- DateTimePage (/dates) demonstrates all five types with normal and Ajax submit,
  Wicket RangeValidator date bounds mirrored to HTML min/max, an optional live date,
  a disabled date and millisecond time input. Picker availability is browser/OS-owned.
- Temporal sw-input controls set their own light/dark color-scheme for legible native
  calendar/clock affordances and pickers, without changing the embedded host's scheme.
- The landing project dialog includes an optional launch date; the static preview
  validates browser input and announces the chosen date without persisting it.

### Phase 3 toggles and input groups

- Oat has no dedicated toggle equivalent. SwToggle follows its native CheckBox
  field API; SwToggleChoice<T> and SwToggleMultipleChoice<T> follow RadioChoice and
  CheckBoxMultipleChoice constructor/model/renderer shapes. BaseSwToggleGroup shares
  styling and accessible option attributes. Shadleaf owns toggle markup and CSS.
- Toggles retain checkbox/radio semantics, including Space/arrow keys, native checked
  state and Wicket's Boolean required behavior. Single groups use radios, multiple
  groups use checkboxes; there is no synthetic aria-pressed state. Multiple groups
  omit unsupported aria-required on role=group and validate resolved required choices.
- ToggleBehavior has DEFAULT/OUTLINE variants and DEFAULT/SMALL/LARGE sizes.
  ToggleGroupBehavior controls horizontal/vertical orientation and spacing 0–4;
  zero joins the items. SwToggle supports decorative icons with a visually hidden
  text caption for icon-only controls. Choice renderer labels remain escaped text.
- BaseSwInputField follows Oat's prefix/suffix/suggestions APIs and now backs all
  text-like fields, including textarea. Plain fields omit the input-group wrapper;
  grouped fields keep the same field:input path and use Shadleaf's control/addon
  classes. Textarea prefix/suffix appear above/below the control; native datalists
  apply only to single-line inputs and are suggestions, not a validation allowlist.
- Prefixes/suffixes use explicit models, escape text, detach normally and describe
  the input through aria-describedby. They do not become additional accessible names.
  BaseSwField provides an additional-description hook for this while retaining the
  same error IDs and in-place feedback updates.
- Sw.Behaviors exposes inputGroup, inputGroupAddon, inputGroupInput,
  inputGroupTextArea, inputGroupText and inputGroupButton for custom markup. Addons
  follow the control in DOM order; CSS places them. The small/ICON group button uses
  ButtonBehavior's variants and accessible-name checks. A delegated listener adapted
  from Shadleaf focuses controls when noninteractive addon space is clicked, including
  after Ajax replacement; buttons, links and disabled controls keep their behavior.
- Automatic input groups retain the input's single label and do not repeat its name
  on the wrapper. Custom multi-control groups may have a separate group name.
- Disabled groups fade the native control, while static addon text keeps full contrast:
  unlike the disabled input, units and other explanatory text are still read by users.
- GroupPage (/groups) covers normal/Ajax submit, immediate checkbox/radio keyboard
  callbacks, input validation, datalists and grouped textareas, joined/vertical and
  disabled states, and custom addon/button actions surviving Ajax replacement.
  Landing-page digest radios use joined toggle styling; the email input has an addon.

### Phase 3 file upload

- SwFileUpload and SwFileDropzone follow Oat's List<FileUpload> model/constructor
  shapes, plus inherited-model String labels. BaseSwFileUpload shares native Wicket
  FileUploadField behavior, accept, multiple, maximum count and per-file size settings.
- Both default to multiple selection. setMultiple(false) or setMaxFiles(1) renders a
  single picker and enforces one file server-side. Count and per-file size validators
  run on the submitted files; form request/file limits still protect multipart parsing.
  accept is a picker/drop hint, not server-side content validation.
- Uploads remain request-scoped: Wicket closes their streams and clears their model
  on detach. Applications process/persist them inside the submit callback. The
  showcase retains only escaped names and byte counts, never FileUpload instances.
- Dropzone anatomy/styles follow Shadleaf's file-upload and empty components. The
  native, visually hidden input is the keyboard tab stop and source of the submitted
  FileList; its Browse label works without JavaScript. A delegated enhancement merges
  picks/drops, deduplicates, checks hints, lists/removes files and announces changes.
- The dropzone enhances native selection only. It does not implement transport
  progress, image/blob previews or automatic submission. clearSelection(target)
  clears successful Ajax selections without another change event or replacing focus.
  Invalid Ajax submits keep browser selections; an unrelated Ajax request does not
  clear inline errors merely because Wicket detached an upload model.
- UploadPage (/uploads) uses explicit multipart forms and parser request/file/count
  limits, plus field-level count/size and example extension validation. It reads bytes
  during callbacks and retains only names/counts. Native, Ajax and single-file flows
  are covered, including failed submits, unrelated requests and explicit reset on success.
- The landing project dialog has an optional native reference-file input. Only its
  filename is shown locally; nothing is submitted or persisted. The public status
  now says forms complete, while preserving the unreleased snapshot.

### Phase 4 confirmations and sheets

- SwConfirmDialog adapts OatConfirmDialog's ask(target, title, description, callback)
  API. A pending callback is consumed before invocation and discarded on close;
  another ask replaces it and can be called from the callback to keep the dialog open.
  Subclasses overriding onClose must call super. Confirm requests use a per-component
  ACTIVE Ajax channel to discard clicks while the previous answer is in flight.
- Confirmation markup and both skin blocks follow Shadleaf's alert-dialog. The
  native dialog has role=alertdialog and closedby=closerequest: Escape cancels,
  outside clicks do not. Cancel has native autofocus; the corner close button is
  absent by default. The description supplies aria-describedby; the optional content
  placeholder is hidden unless application content is provided.
- SwSheet adapts OatDrawer's SwDialog subclass pattern and Shadleaf's sheet anatomy.
  Side.START/END follow text direction; TOP/BOTTOM span the viewport. It retains
  SwDialog's title/description, trigger, content, validation and lifecycle API with
  covariant fluent methods. The body scrolls separately from the header/footer.
- New dialog markup uses sw-alert-dialog/sw-sheet instead of also inheriting the
  centered sw-dialog class. The common form/footer classes preserve the delegated
  footerless-Enter behavior. Scroll locking and instant Ajax rendering cover all
  three modal types; sheet client-close cleanup waits 250 ms for its 200 ms exit.
- Native sheet opening can focus Chromium's implicitly focusable scroll container.
  A delegated toggle handler prefers the first visible, enabled form field only
  when that container received focus; explicit autofocus and informational-body
  focus remain native. The server-open and invoker fallbacks use the same helper.
- OverlayPage (/overlays) covers reusable and chained questions, non-destructive
  confirmation, sheet validation/application errors, nested confirmation, refresh
  while open, scrolling and all four edges. Browser checks include RTL positioning,
  safe initial focus, focus return, duplicate clicks and WCAG checks in both skins/modes.
- The landing page's Delete sample opens a browser-only confirmation; Preview a
  sheet opens a themed details panel. It uses the real compiled component styles
  and keeps the existing project-dialog demo. Phase 4 remains in progress; the
  next component group was popover and tooltip. No dependency or CSP change was made.

### Phase 4 popovers and tooltips

- SwPopover adapts OatPopover's constructor/content factory, CONTENT_ID, getTrigger,
  getContent and close(IPartialPageRequestHandler) API. It adds optional reactive
  title/description, Side.TOP/BOTTOM/START/END, Align.START/CENTER/END and trigger variant.
  Its own root tag supports Ajax replacement; content-only updates retain the native
  open panel, while replacing the full component resets it to closed. Open state is
  browser-owned, with no server toggle round trip or modal form machinery.
- Native popover=auto supplies the top layer and light dismissal. Delegated handlers
  synchronize aria-expanded, focus the first visible control (respecting autofocus),
  close when focus leaves and return focus after Escape/explicit Ajax close. Nested
  popovers preserve their parent's open state. The panel has role=dialog and is named
  by the title, or the trigger label when there is no title.
- TooltipBehavior retains Oat's String/IModel and Placement API. Native title is the
  no-script fallback; initialization replaces it with escaped plain-text content in
  a manual popover. Mode.DESCRIPTION appends its aria-describedby reference without
  replacing field help/errors; Mode.LABEL repeats an existing name visually only.
  Null/blank text adds nothing. Models detach through Wicket's behavior lifecycle.
- Pointer hover waits 300 ms by default (configurable Duration); keyboard focus is
  immediate. Pointer transfer has a short grace period, hover over the tooltip keeps
  it open, and Escape dismisses only the tooltip. Touch focus suppresses hover hints.
  Only one tooltip opens at a time. Hiding is immediate to avoid stale top-layer
  hints during popup closure; opening retains the skin's fade/zoom motion.
- Tooltip nodes are created beside their controls to inherit theme tokens and modal
  ancestry, outside wrapping labels so hints do not rename checkbox/toggle controls.
  WeakMap state and the existing DOM observer clean up removed/replaced
  hosts, cancel timers and repair descriptions after Ajax feedback changes. Text uses
  textContent; no HTML payloads, interactive tooltip content or inline styles.
- Shared popup structure and both skin identity blocks follow Shadleaf. CSS anchor
  positioning uses the native implicit source, logical sides, alignment and flipping;
  JS measures only the tooltip arrow's actual side. Older Popover API browsers without
  anchor positioning use a viewport panel. No positioning dependency or CSP change.
- PopupPage (/popups) covers Ajax forms, inline validation, content refresh, nested
  popovers, reactive/removed hints, label mode, disabled controls and sheet nesting.
  Browser tests cover touch, RTL, mobile boundaries, keyboard/hover persistence and
  cleanup. Hover-transfer tests move the pointer directly: Playwright's Locator.hover
  may scroll an anchored top-layer element after measuring it. Accessibility audits
  wait for finite CSS transitions to avoid measuring a partly faded closing popup.
- The landing Java badge opens a popover and preview controls have tooltip hints.
  site/build.mjs copies the actual library script alongside its CSS; the site's
  duplicate input-group focus handler was removed. Existing modal demos remain.
  Dropdown menus were implemented next (see below); a general-purpose toast API remains.


### Phase 4 dropdown menus

- SwDropdown<T> matches OatDropdown's String/IModel constructors, populateItem, four
  of factories, getTrigger and close(IPartialPageRequestHandler). Each repeated
  native button gets DropdownMenuItemBehavior and needs a "label" child. Empty/null
  list models disable the trigger. getMenu, setSide/setAlign and setTriggerVariant
  expose native menu composition/appearance. Whole-component Ajax replacement closes it.
- DropdownMenuBehavior connects caller-owned menu markup to a native button trigger,
  sets native popover/role/label/controls/expanded attributes and renders core JS.
  Use one behavior per menu; unbinding removes only its own trigger attributes.
  DropdownMenuItemBehavior styles native buttons or links, disables unavailable
  items/strips disabled link hrefs, supports destructive/inset rows, and reads/detaches
  server-owned checkbox/radio models. Callbacks own checked-state changes/exclusivity.
- Sw.Behaviors provides menu/item/checked item/label/separator/group/shortcut helpers.
  Shortcut hints are presentation only. Link/form semantics stay with Wicket;
  SwDropdownChoice remains the form select, and the app-shell user menu remains later.
- Core delegated JS extends shared popup state/cleanup for menus. Keyboard opening
  focuses first/last enabled row; pointer opening focuses root. Arrows/Home/End/
  PageUp/PageDown, 500 ms label typeahead and repeated-character cycling skip
  disabled rows and shortcut hints. Space activates links. Escape/actions return
  focus; Tab closes and continues page navigation. Pointer movement focuses rows.
- Capture-phase activation guards disabled/closed/repeated actions, restores the
  trigger before synchronous dialog actions and defers hiding until after native
  defaults. This handles Wicket callbacks that stop bubbling and keeps native
  navigation/submit actions intact. Ajax callbacks render checked state; no
  optimistic client changes. Opening resets transient state; mutation cleanup
  covers replaced/removed menus and newly added items.
- Geometry/markup/interaction adapt Shadleaf dropdown-menu and reuse popup.css native
  CSS anchors, logical directions, viewport fallback, reduced motion and forced
  colors. Both skins and embedded bundles contain all styles, with no inline styles.
- MenuPage (/menus) covers factories, disabled/destructive actions, empty lists,
  Ajax replacement, checked groups, navigation, confirmation focus and sheet nesting.
  Landing Project actions uses real core script/CSS and local demo-only callbacks.
  Next: general-purpose toast API; feedback toasts are already implemented.
- Verification for this group: 392 core tests and 23 example-page tests passed;
  all 228 Chromium browser cases passed under strict CSP with WCAG axe checks.
  Landing checks passed at 1440/390 px in both skins/modes, including open menus,
  confirmation focus return and axe; native submit defaults/submitter values and
  long labels at 320 px also passed. Generated site build/check and source
  whitespace checks passed. Changes remain local, uncommitted and unpublished.


### Phase 4 general-purpose toasts

- ToastBehavior ports OatToastBehavior's static toast(handler,message[,variant[,title]])
  and script(message,variant,title) helpers. Sw.toast is the facade. Variant is local
  to ToastBehavior: DEFAULT/SUCCESS/INFO/WARNING/ERROR, following Shadleaf; no shared
  variant enum. Optional Duration overloads use five seconds by default, zero for
  persistent notifications. Validate browser timer bounds and sub-millisecond values.
- Add Sw.Behaviors.toasts() to the page/base page to render CSS/core JS before calls.
  Wicket's PartialHeaderResponse ignores resource items outside renderHead; direct
  imperative header-response rendering in Ajax callbacks does not load resources.
  Do not claim on-demand loading. Pure script builders use English labels; component
  overloads/Ajax helpers resolve ToastBehavior.close and ToastBehavior.label through
  Wicket resources. All payloads quote JSON and escape < plus U+2028/U+2029.
- Shared Shadwick.toast accepts message plus options (variant,title,duration,
  dismissLabel,regionLabel). Retain its existing feedback (message,level,closeLabel)
  signature with persistent timing and severity attributes/destructive error styling.
  Feedback collectors, fences, filtering and mark-rendered semantics remain unchanged.
- Toasts adapt Shadleaf's title/description/icon/close anatomy, live roles and modal
  context; both skins keep a scrollable CSS column at logical bottom/end. No inline
  stack measurements/styles, client framework or new dependencies. All text uses
  textContent, all Lucide SVG shapes are fixed. ERROR uses alert; others use status.
- Timers pause on hover, keyboard focus and document.hidden, leaving at least a
  second on resume. Persistent feedback/general messages never expire. Showing a
  notification does not focus it. Alt+T focuses the newest close button; Escape
  on a toast closes only it and does not close a surrounding dialog. Dismissal moves
  to a neighbor, then back to the original control. Pagehide and DOM cleanup cancel
  timers; a disconnected viewport is dropped rather than resurrected.
- Move the viewport into the active native modal so it is not inert, and back to
  body on close. Observe modal toggles and Ajax DOM changes. ToastPage (/toasts)
  exercises variants, timers, literals, full-page scripts, replacement and modals.
- Landing notifications now use real core JS/CSS; remove the duplicate site-toast
  markup/styles/timer. Mirror studio theme tokens/mode onto the notifications viewport.
  Forms/overlays remain browser-only demos. Phase 4 is complete; next is app shell,
  beginning with site header, breadcrumb and skip link.
- Verification: 402 core tests and 24 example-page tests passed. The full 256-case
  Chromium run passed 252 cases and exposed a forced-color focus issue after touch;
  the outline must use :focus rather than :focus-visible, since Alt+T can retain
  the same button that a touch dismissal focused. After that CSS fix, all 12 affected
  forced-color/touch/keyboard/feedback browser cases passed, plus the 35 focused JVM
  tests (CSS bundles, toast helpers and feedback). Other browser cases were unchanged.
  Desktop/mobile (1440/390 px), both skins/modes: landing and packaged showcase
  screenshots, modal focus and WCAG axe checks passed. Site build/check and source
  whitespace checks passed. No commit, push or deployment was requested for this group.


### Clover identity and overlay publication

- The project logo is an original four-leaf clover: four heart-shaped leaves, a short
  curved stem, forest green and pale lime. It expresses a lucky discovery and a good
  developer experience, with a botanical connection to Shadleaf. Keep it distinct
  from Shadleaf's own mark. Master SVG: site/assets/logo.svg; matching favicon,
  sprite clover, README, header/footer, hero accent and social preview use it.
- Landing copy now marks forms and overlays complete, with app shell next. The
  live studio includes dialogs, confirmation, sheet, popover, tooltip, menu and
  library-backed toast demos alongside the completed form controls.
- User authorized committing and pushing all accumulated overlay work, updating
  the landing page and publishing through the existing GitHub Pages workflow.


### Phase 5 site header, breadcrumb and skip link

- References: OatBreadcrumb + its tests and OncePerRequestModel; OatAppLayout's
  native skip link; OatPageHeader was read but is a distinct title/subtitle Border,
  not Shadleaf's site header. No OatSiteHeader/standalone skip component exists.
- SwSiteHeader uses SwCard's caller-owned WebMarkupContainer API, with a matching
  SiteHeaderBehavior and siteHeaderEnd helper for end-aligned content. Native header
  placement determines landmark semantics. setInset(true) removes its ground/border.
  Header/end rows wrap and grow; no sticky option or responsive sidebar/menu yet.
- SwBreadcrumb<T> ports the generic constructor, populateItem/of, setCurrent,
  setLabel and setMaxItems. Reuse existing OncePerRequestModel for levels/current.
  Each row supplies link + label children; root is a native nav. All levels render
  by default, otherwise first + an ellipsis menu + tail, counting current text in
  maxItems (minimum 2). Last link or escaped current text gets aria-current=page.
- Breadcrumb markup/identity adapt Shadleaf: ol/li structure, decorative sibling
  separators, RTL chevrons, long-word wrapping, per-skin type/gaps/focus. Existing
  DropdownMenuBehavior/ItemBehavior own ellipsis keyboard/placement/cleanup; no
  second menu implementation. Empty/null levels hide via an Ajax-safe placeholder;
  replacing a trail closes it. Resource keys: SwBreadcrumb.label/more.
- Standalone BreadcrumbBehavior and Sw.Behaviors list/item/link/page/separator
  helpers support caller markup, preserving aria-label/aria-labelledby. The behavior's
  fallback key is BreadcrumbBehavior.label; application localization remains Wicket's.
- SwSkipLink is an escaped Label on an anchor, with default/main, literal target id
  and Component target constructors, plus String/IModel setLabel. Key SwSkipLink.label.
  SkipLinkBehavior sets an encoded native fragment href. With a Component it adds
  an id and tabindex=-1 only where missing and not a native control/contenteditable;
  explicit tabindex is preserved. Unbinding removes only the focusability behavior
  it installed. Literal-id targets need their own tabindex if not naturally focusable.
- CSS-only skip-link reveal + native navigation moves focus and works without JS;
  forced-color focus is explicit. No extra client script/dependency/CSP relaxation.
- BasePage uses transparent header/main containers so existing component paths stay
  stable; every showcase page gets a first-in-body skip link and focusable #main.
  ShellPage demonstrates header actions, native breadcrumb routes, Ajax clear/restore,
  current text updates, long labels and inset context. Its clover fixture copies the
  canonical site logo for the example's packaged resource only.
- Landing replaces its custom skip styling with core sw-skip-link, gives #main a
  tabindex, and adds a real sw-site-header/breadcrumb studio row. App shell remains
  in progress; sidebar is next. No commit/push/publish is implied by component work.
- Browser helper: axe injects scripts and stalls in a JavaScript-disabled context.
  Keep native no-script interaction assertions there, and run WCAG axe audits on
  the normal page/context passes. Do not imply the no-script pass runs axe.
- Verification: 422 core tests and 25 showcase page tests passed; all 280 Chromium
  browser cases passed, including 20 focused shell cases and all existing flows.
  Landing and packaged showcase checks passed at 1440/390 px in both skins/modes,
  with WCAG axe audits, open breadcrumb menus, native skip focus and long labels.
  Native no-JavaScript navigation passed separately. Long skip labels in embedded
  bundles without a host reset now use border-box, auto height and per-skin minimum
  height; both skins/modes fit at 320 px without clipping and focus the target.
  Preview servers were stopped. Changes remain local and uncommitted; sidebar is next.

### Phase 5 sidebar

- References: OatAppLayout, MenuItem, CurrentPage and its navigation/group/badge tests;
  Shadleaf sidebar/provider/inset/trigger/menu templates, sidebar.js, menu-tooltip.js,
  both skins, sidebar CSS and docs. App-layout base pages and user menus remain later work.
- SwSidebar is a Panel bound directly to <nav> inside one sidebar provider. MenuItem
  retains Oat's constructors/factories/groups/badges and adds optional withIcon
  String/IModel (Lucide; generic circle fallback). Groups are one level deep, use native
  details/summary, and open for a current child. Exact page class and supplied parameters
  determine aria-current=page; protected isCurrentPage/newMenuLink/newMenuIcon/isAuthorized
  hooks permit application customization. Authorization filters links and empty groups.
- Badges are escaped sibling Labels, hidden for null/blank/numeric zero; visible badges
  describe their links even when visually hidden in the icon rail. Link labels and the
  item-list model use OncePerRequestModel. Header/footer components use HEADER_ID/FOOTER_ID. getMenu() is the
  Ajax refresh target for retaining an open mobile panel; a full replacement closes it.
- SidebarBehavior/SidebarProviderBehavior/SidebarTriggerBehavior and Sw.Behaviors helpers
  support caller markup. SwSidebarTrigger is a native icon button. Provider controls
  default EXPANDED/COLLAPSED, optional persistence, and a validated distinct cookie name
  (default sw-sidebar-state). Only validated expanded/collapsed values are read; desktop
  toggles write a root-path, one-year, SameSite=Lax preference, Secure on HTTPS.
- Plain/floating/inset variants, logical start/end (including RTL), offcanvas/icon/none
  desktop collapse at 48rem. One native auto-popover nav is a normal sticky desktop
  column and a top-layer mobile panel. JavaScript synchronizes invoker ARIA, focuses the
  current/first link on mobile, traps Tab, owns/restores only newly added inert attributes,
  and handles Escape, light dismiss, links, resize, pagehide and Ajax teardown/replacement.
  Ctrl/Cmd+B scopes to the focused/single provider; text fields, modal dialogs, Alt and
  Shift combinations keep their normal behavior. sw-sidebar-toggle exposes state/open/mobile.
- Nested menu Escape and Tab must consume only their own layer, including the interval
  before a deferred focus timer moves focus off their invoker. The sidebar checks open
  nested content as well as the focused ancestor. Tab wrapping uses the actual active
  element after the menu's capture handler returns focus, rather than its stale event target.
  Toasts adopt an open mobile sidebar's context as they already do native modal dialogs.
- Icon-only hints reuse TooltipBehavior in label mode, showing only in a collapsed desktop
  icon rail, on the logical outside edge. Ordinary links use normal Tab order, not ARIA menu
  roles. Resource keys: SwSidebar.label/close, SidebarBehavior.label, SidebarTriggerBehavior.label.
- Native navigation and mobile opening work without script; desktop collapse styles are
  gated on scripting:enabled, so a stored collapsed preference cannot hide no-script links.
  Mobile focus/inert enhancement needs script. Dedicated reduced-motion, forced-color and
  print styles; explicit component box sizing keeps the embedded bundle usable without a
  host reset. Printing an open panel releases the root's scroll lock.
- SidebarPage (/sidebar) demonstrates routes, current groups, badges, header/footer slots,
  appearance controls, shortcuts and Ajax menu/full-panel refresh. Its skip link targets
  the inset past both global and sidebar navigation. The landing studio uses the same
  provider/nav/trigger anatomy in a bounded desktop composition and a native mobile panel,
  with persistence disabled. Next: user menu, then app layouts and per-user themes.
- Verification: 438 core tests and 26 showcase-page tests passed. The full 308-case
  Chromium run passed 305 cases and exposed three nested-menu focus races. After
  fixing Escape before initial menu focus, a focused run exposed the equivalent Tab
  race; the final 28-case sidebar run passed in both skins/modes, including repeated
  rapid Escape/Tab, forced colors, reduced motion, remembered no-script fallback,
  mobile inertness/focus, Ajax replacement, RTL/end placement and toast context.
  The remaining previously passing cases were unaffected by those sidebar-only fixes.
  Landing desktop/mobile checks passed in both skins/modes, including open panels,
  WCAG axe audits, no-script controls and disabled persistence. Both embedded skins
  in light/dark fit at desktop/320 px without a host reset and release scroll locking
  for printing. CSS/site build/check, JS syntax and whitespace checks passed.
  Changes remain local and uncommitted; no push or publication was requested.

### App-shell publication

- Completed: accumulated site header, breadcrumb, skip link and sidebar work, examples,
  docs and landing updates were committed and pushed to main as 1909d80. The Website
  workflow deployed successfully (run 41914029752); the live site passed desktop/mobile
  checks in both skins/modes, including accessibility and no-script navigation. The
  working tree was clean after publication. The next roadmap task remains user menu.

### Phase 5 user menu — local implementation, 2026-11-05

- Read OatUserMenu and all its tests, plus Shadleaf's user-menu template, component CSS,
  both skin identities, documentation, browser tests and rendering tests. References were
  cloned again into /tmp/wicket-oat and /tmp/shadleaf after the previous temporary copies
  had gone. They are reference sources only, with no new runtime/build dependencies.
- SwUserMenu follows Oat's name-model constructor, setDetail, setPicture, addLink,
  setSignInPage, isSignedIn, signOut and onSignedOut APIs; public String/model overloads
  cover detail/picture and link labels. A null name means anonymous unless isSignedIn
  is overridden. Blank/absent authenticated names display the localized "Your account";
  initials can fall back to the detail's email local part, otherwise "?".
- Default SIDEBAR placement shows avatar/name/detail and uses the existing sidebar menu
  button/rail styles; HEADER is an avatar-only round button, named "Account: {0}".
  setSide/setAlign override the default END/END sidebar and BOTTOM/END header menus.
  Names and details are escaped; unauthorized page links and their extra separator hide.
  Exact page-class/parameter matches get aria-current. getTrigger/getMenu expose controls.
- Reuse SwAvatar, DropdownMenuBehavior/ItemBehavior and TooltipBehavior. Name/detail/picture
  are read once per request. Header fallback initials are decorative CSS-generated glyphs
  from escaped data-initials; sidebar-only name/detail are not rendered in header triggers.
  This keeps icon-button naming consistent without duplicate initials/email labels.
  A picture covers the fallback after loading; failed decoding retains initials.
- Sign out is a native menu submit button associated by form id with a separate hidden
  StatelessForm outside role=menu. Put the component outside other forms. POST is required;
  disabled roots and requests Wicket Fetch Metadata identifies as cross-site are refused.
  Preserve Oat's stateless and expired-session processing, default session invalidation and
  home-page redirect; applications override signOut/onSignedOut for their auth framework.
  getSignOutForm permits form customization. No Spring Security dependency is introduced.
- Anonymous Sign in is a bookmarkable page link, hidden on the configured sign-in page
  and subclasses; no configured page means no visible anonymous content. Whole-component
  Ajax replacement refreshes state and closes the menu. Sign-in/out and menu opening work
  without JavaScript; existing scripts enhance keyboard navigation, focus and hints.
- Shared tooltip runtime hides a sidebar hint when its menu opens and refuses to show
  that hint while aria-expanded=true. Existing nested-menu Escape/Tab handling is reused.
- UserMenuPage (/user-menu) demonstrates header/sidebar menus sharing a demo session
  identity, native account/settings routes, native POST sign-out, anonymous sign-in through
  UserSignInPage (/demo-sign-in), Ajax rename, picture and initials restoration. This is a
  credential-free demo; its signOut override changes demo identity without invalidating
  the appearance preferences. Set a custom footer markup id after SwSidebar.setFooter:
  Wicket replacement preserves the old slot's id, overwriting one assigned beforehand.
- Landing studio uses both account-menu anatomies and transient browser-only identity
  state. My account/Settings focus the corresponding fields; Sign out/Sign in toggle
  both previews. These actions send no authentication requests and store no identity.
  Account changes use their focused controls to convey state; no toast covers the mobile
  footer. Restrict the site's muted small-text brand rule to the sidebar header so the
  account detail inherits the proper hover/focus foreground with sufficient contrast.
- New resource keys: SwUserMenu.account/signIn/signOut/unnamed. README, showcase navigation,
  CSS bundle checks and site documentation include the component. Next: theme controls.
- User requested continuing the next roadmap topic, not commit/push/publication. Keep
  this group's changes local; the saved-state edit from the prior session is preserved.
- Verification: 464 core tests and 28 showcase-page tests passed in the final normal
  verify run. All 20 initial user-menu browser cases passed after correcting the showcase
  footer id, decorative header fallback and animation timing. The full 344-case browser
  run passed 340 cases; four new image-failure cases used an overly narrow URL route that
  missed Wicket's version suffix. After changing it to **/clover*.svg*, all four passed,
  covering bad-image fallback, long names, 320px/RTL, forced colors and reduced motion.
  No production change followed the full run. Thus all 24 user-menu cases passed across
  the focused runs, with the remaining 320 regression cases passing in the full run.
- Landing checks at 1440/390px passed in both skins/modes, for both placements, open-menu
  WCAG axe audits, preview sign-out/in and focus navigation. Native no-JavaScript mobile
  sign-out passed in the Wicket browser cases. Site build/check, JS syntax and whitespace
  checks passed. Preview servers were stopped. Changes remain local and uncommitted.

### Phase 5 theme toggle and preferences — local implementation, 2026-11-05

- Read OatTheme, OatThemeStore, CookieThemeStore, SessionThemeStore, OatThemeBehavior,
  OatThemeSwitcher and OatThemeTest; read Shadleaf's bootstrap, theme-toggle runtime,
  template, CSS, documentation, browser and rendering tests. Oat's named color palettes
  and server-side user stores are broader than this browser appearance task and remain
  future scope. No new dependency, framework or CSP exception was introduced.
- ThemeMode LIGHT/DARK/SYSTEM is independent of Skin and named color token sets.
  ShadwickSettings defaults to SYSTEM and storage key shadwick-theme. ThemeBehavior
  (Sw.Behaviors.theme()) goes on html in a base page, writes data-sw-theme-root/default/
  storage-key/remember, preserves unrelated classes and renders the DARK fallback on
  the server. SYSTEM's no-script fallback is light. Per-root setters override defaults;
  storage keys validate 1–128 letters/numbers/underscore/dot/colon/hyphen.
- Shadwick.THEME_JS and renderThemeScript use an external parser-blocking priority header
  item before styles/body. The script immediately resolves flagged html, before parsing
  body or painting. Do not opt every page into browser preferences through install():
  existing server-controlled modes must remain possible. A toggle alone can initialize
  a document interactively, but the root behavior supplies the before-paint configuration.
- SwThemeToggle is a Panel on caller-owned container markup. Default MENU presentation
  composes existing native dropdown behavior and Light/Dark/System radio items; BUTTON
  switches light/dark and uses aria-pressed. Both use existing button/skin identities,
  decorative sun/moon icons and native disabled attributes. setLabel String/IModel,
  setVariant, setSide, setAlign, setTarget, getTrigger and getMenu are available. Labels
  are SwThemeToggle.label/darkMode/light/dark/system; custom label models read once per
  request and detach. Controls hide without scripting and while printing.
- Browser-owned localStorage values are light/dark/system. Unlike Shadleaf's key removal,
  System is stored explicitly so it can override a configured Light/Dark fallback; missing
  or invalid data uses that fallback. Blocked storage retains a choice for this page.
  OS changes affect only SYSTEM; storage events synchronize tabs and same-key remembered
  roots synchronize in-document. Pageshow restoration reloads preferences. Printing
  temporarily resolves light without changing the saved mode and restores afterwards.
- One delegated runtime and child-list observer initialize/reconcile Ajax controls and
  independent roots, removing disconnected roots. Root .dark and native color-scheme
  follow the resolved state; controls' radio checks, pressed state and data-sw-theme-dark
  icons stay synchronized even when the toggle sits outside its explicit target. No
  per-component event listeners, inline scripts/styles, server callback or session state.
  ShadwickTheme.getMode/isDark/setMode/refresh accept an optional root element/id;
  sw-theme-change bubbles from that root with {mode,dark,rootId}.
- Independent previews on a light host use ThemeBehavior.setRememberChoice(false) and
  SwThemeToggle.setTarget(container). Do not claim nested light roots reset an outer
  dark palette's inherited custom tokens. The landing studio uses an explicit target,
  Light fallback, no storage and the core appearance menu; host ivory stays independent.
  Its event bridge updates dialogs, sheets, toasts and the System/resolved description.
- ThemePage (/theme) has two menus and a simple button, Ajax control replacement, full
  navigation and a status output. It uses shadwick-example-theme. A BasePage opt-in hook
  gives this page browser preferences while preserving the other examples' URL-based
  reference modes; old mode links hide on ThemePage. README and site docs cover the API.
- Verification: 473 core tests and 29 showcase-page tests passed in the final normal
  verify. The complete 372-case Chromium browser suite passed, including all 24 theme
  cases and the prior user-menu/image-failure regressions, under strict CSP and WCAG
  axe checks. Theme cases cover pre-body/first-frame application, keyboard/radio state,
  OS changes, cross-tab storage/clear, invalid/blocked storage, printing, Ajax, native
  navigation/history, no-script fallback, RTL/320px, forced colors and reduced motion.
- Landing checks passed for all 12 desktop/mobile, skin and Light/Dark/System combinations:
  menu/account/dialog WCAG axe audits, preview/overlay/icon synchronization, OS following,
  no storage writes, reload fallback and no-script skip navigation. The first diagnostic
  run sampled an OS change before its browser event; waiting for the resolved state made
  it deterministic, with no production change. Settled desktop/mobile showcase and
  landing screenshots were reviewed. CSS/site builds/checks, JS syntax and whitespace
  checks passed. Preview servers were stopped. Next: reusable app layouts and navigation depth. User-menu and theme
  groups remain local, uncommitted and unpublished; no publication was requested.

### Phase 5 reusable app layouts and navigation depth — local implementation, 2026-11-05

- Read OatAppLayout, MenuItem, LayoutAndNavigationTest, its parameterized/Ajax fixture
  pages and markup; read Shadleaf's application-shell, sidebar/header layouts, sidebar
  parts, runtime, component and skin CSS, sidebar/header browser tests. Reuse the existing
  component styles and native runtime. No dependency or CSP policy changes.
- SwAppLayout is an abstract WebPage with no-arg/PageParameters/IModel constructors.
  Hooks match Oat's appTitleModel/appNameModel/sidebarMenuItemsModel/newMenuLink/
  newTopNavExtra/newSidebarFooter. Additional hooks expose icons, authorization/current
  matching, sidebar/header placement, variant/collapse/provider settings, branding and
  native item/group actions. Build in onInitialize, after subclass fields exist. Public
  getSidebar/getTopNav/getMain/getProvider expose the initialized composition.
- Default shell: inset sidebar with icon collapse, remembered provider state, locale lang,
  priority ThemeBehavior and a header SwThemeToggle. Subclass content uses wicket:extend
  and ordinary page.add; transparent provider/workspace/main preserve component resolution.
  One main with stable id sw-main and tabindex=-1 is the first-in-body skip link's target.
  In header placement, omit the workspace's outer DOM tag, put the single sidebar/nav in
  the site header and main below it. Header/footer branding slots are panel-only there.
  App owns content spacing/type; SwAppLayout.title/name are resource defaults.
- MenuItem preserves its five/six-argument constructors, adding leaf children and panelOnly.
  submenu(String/IModel, children) gives a pageless summary; withChildren(children) keeps
  an existing page link beside its separate named disclosure. Labelled groups may contain
  submenus, but groups cannot nest and submenu children must be leaf page links. Validation
  does not load label models. withIcon/withBadge/withPanelOnly preserve all other fields.
- SwSidebar composes native details/summary and existing sub-button skin styles. Authorized
  current descendants open both the submenu and containing group. Denied page parents hide
  the whole entry; pageless submenus with no authorized leaves hide; an authorized page
  parent stays available without an empty disclosure when all children hide. Existing link
  paths and newMenuLink/newMenuIcon/isCurrentPage/isAuthorized hooks remain usable.
- newMenuAction/newGroupAction supply components bound to native buttons with decorative
  ellipsis/plus bodies, requiring caller-provided accessible names. Defaults hide. Group
  actions sit outside summaries; submenu parents reserve their action for disclosure.
  SidebarActionBehavior and Sw.Behaviors.sidebarMenuAction/sidebarGroupAction support
  caller-owned controls, reuse button accessible-name checks, name String/model setters,
  native disabled state and optional show-on-hover. Separate space accommodates a badge
  plus an action/disclosure. SwSidebar.pages localizes the sibling disclosure's name.
- SidebarBehavior.Placement.HEADER renders the same nav as a desktop row/mobile panel.
  Runtime finds the provider's own descendant nav without adopting a nested provider's.
  Desktop header ignores collapse state/shortcut/cookie writes, removes trigger popup
  state and scrolls links within available width; panel-only parts hide above 48rem.
  Group labels remain accessible, pageless submenu disclosures/children hide in the row,
  and page-parent links remain. A collapsed icon-rail submenu activation expands its
  provider and opens that native disclosure. Existing mobile inert/focus/cleanup and
  nested-menu Escape behavior remain shared. Header dropdowns open below their invoker.
- Both skins adapt Shadleaf's header/inset selectors. Group actions are 24px on desktop
  too: the reference's 20px control overlaps the native collapsible heading's hit area,
  so it cannot use the spacing exception. Explicit CanvasText on plain branding/footer
  text preserves contrast in forced colors. Existing normal skin colors remain token-based.
- AppLayoutPage (/layouts) and HeaderLayoutPage (/header-layout) share content, view routes,
  persistent theme preference, grouped submenus and item/group badge actions. Ajax menu
  refresh preserves the open mobile panel; full navigation replacement closes it. The
  examples use a distinct shadwick-layout-sidebar cookie and override current-page matching
  to ignore appearance query parameters. Their own page sizing explicitly uses border-box
  so embedded bundles need no global reset.
- Landing Layout selector moves its existing nav/header between compositions, preserving
  IDs/state and closing an open panel/nested menu before moving it. Header branding/footer
  become panel-only; Explore is a native submenu; Collection actions reuse dropdown behavior.
  Layout selection and sidebar/theme previews store no preference. No-script preview keeps
  its native sidebar; responsive toolbar wraps its selectors. README and site docs updated.
- Browser test helper now polls current finite animations with a bounded wait, rather than
  retaining finished promises from disappearing transitions. An initial focused diagnostic
  run stalled at the old promise wait; no-script cases skip script-based animation settling.
  Keep real native no-script navigation assertions separate from normal WCAG axe audits.
- Verification: the full 408-case Chromium regression suite passed under strict CSP,
  including all 28 new layout cases and the existing user-menu/theme/sidebar/overlay/form
  cases. Core verification passed 482 tests and showcase-page verification passed 31.
  Layout coverage includes permission filtering/current descendants, native disclosures,
  page-parent routes, rail expansion, actions/badges, Ajax menu/full-nav replacement,
  header overflow/cookie/shortcut behavior, mobile focus/inertness/resize, native no-script
  navigation, RTL/320px, forced colors and printing. Full normal verify packages both modules.
- Settled showcase screenshots and WCAG axe audits passed at 1440/320px for both skins,
  both modes and both compositions, with open mobile panels/forced colors. Both embedded
  skins passed at 320px with no global reset, using only the example's own explicit sizing.
  The diagnostic initially tried an inline host rule; strict CSP blocked it as expected.
  The final check uses the actual external example stylesheet, with no policy bypass.
- Landing checks passed all desktop/mobile, skin/mode and sidebar/header/back-to-sidebar
  combinations, including nested action menus, focus return, open disclosures, WCAG axe
  audits and no horizontal page overflow. Native no-script panel/disclosure passed; no
  sidebar cookie, localStorage preference or duplicated navigation IDs were produced.
  CSS/site build/check, JS syntax and whitespace checks passed. Source site check counts:
  98 internal references, 70 unique IDs, about 660KB built output.
- Final normal verify passed with 482 core and 31 page tests; the built site check passed
  again after packaging. Temporary showcase and landing preview servers were stopped.
- Next: Phase 6 disclosure components (tabs, accordion, standalone collapsible), then
  attachment displays. All accumulated user-menu, theme and app-layout changes remain
  local, uncommitted and unpublished. Published baseline is still 1909d80.

### Phase 6 tabs, accordion and collapsible — local implementation, 2026-11-05

- Read OatAccordion/AccordionBehavior, OatTabs, OatTabbedPanel and their factory,
  component/example tests; read Shadleaf's templates, runtime, component/skin CSS,
  rendering tests, disclosure/tabs browser tests and docs. No new dependency or CSP
  exception. Server-lazy ITab/AjaxTabbedPanel integration requires wicket-extensions,
  which is not a library dependency; keep that as separate future scope.
- SwTabs<T> preserves OatTabs' caller-owned tablist/tabButtons/tabPanels and paired
  populateTab/populatePanel hooks, plus of/Sw.Components.tabs factories. Shared list
  models load once and detach. Visibility/enabled hooks configure both lists, hidden
  entries omit both rendered parts, selection falls back to the first enabled visible
  entry, and empty roots hide with an Ajax-safe placeholder. All-disabled groups keep
  the first visible panel as read-only context. Render all panels up front.
- Framework-owned stable markup IDs derive from the root and index; use setMarkupId
  on list items, not tag.put(id), so Wicket's IDs agree with DOM/ARIA and no warnings are
  emitted. Server writes roles, selected/disabled state, panel labels and initial hidden
  state. setSelectedTab is initial server configuration, not a browser-bound model.
- TabsBehavior/Sw.Behaviors.tabs supplies caller-owned root styling/runtime; enums cover
  orientation, activation and list variant. SwTabs has label String/model, selected index,
  orientation/activation/variant setters and getTabList. Protected tabUrl returns a native
  bookmarkable URL: anchors replace buttons, normal clicks switch without a request and
  replace the address while preserving history state, modifier/target/download/external
  links retain native behavior. The application reads route parameters on full navigation.
- One delegated tabs runtime in shadwick.js: scoped WeakMap state, wrapping arrows with
  RTL/vertical axes, Home/End, manual Enter/Space, roving tabindex and panel focus. Manual
  focus survives unrelated DOM updates; leaving an unchosen tab restores the selected tab
  stop. A child-list observer initializes Ajax roots and synchronizes existing roots;
  whole-root replacement resets to server configuration, in-place content changes retain
  the view. Nested widgets stay independent. No per-widget listeners or client framework.
- Enabled inactive panels become hidden=until-found; beforematch reveals the matching
  tab through nested roots, preserving fragment addresses. Disabled panels stay plainly
  hidden. Non-bubbling sw-tabs-show fires on newly shown panels, not initialization.
  Native URL tabs work with JavaScript off; button tabs keep their initial panel then.
- Hidden panel geometry is an invariant: zero border/padding/margin/min size and absolute
  position with !important, and no pointer interception. Plain hidden additionally has
  display:none!important. Host unlayered card/grid styling otherwise restores padding
  onto an until-found panel and can block pointer clicks on the tabs above it.
- SwAccordion<T> follows Oat's generic ListView constructors, isExclusive/setExclusive,
  populateItem and of/Sw.Components.accordion factories. Native details rows have transparent
  trigger/heading/body containers; callbacks still add title/content directly to the row.
  Heading level defaults to 3, validates 1–6. setOpenItems is initial state (first configured
  index in source order wins for exclusive). Refresh the enclosing wrapper over Ajax.
- AccordionBehavior shares one fresh UUID-based native name per render for exclusive rows;
  multiple rows have no name. Distinct render names prevent new Ajax markup joining an old
  open group during replacement. Caller-owned items can supply a name explicitly. Disabled
  rows skip/fade their native summary and omit closed body content; initially open disabled
  content stays readable. No extra accordion keyboard script; native Enter/Space, expanded
  state, fragment/find opening and optional height-to-auto animation are the foundation.
- SwCollapsible is a Panel on details, with first-child summary, escaped label String/model
  and CONTENT_ID slot (Component or String/model content). getTrigger/getContent expose its
  parts; setOpen/isOpen describe initial server state, not native user toggles. Optional
  setVariant/setSize uses existing button identity. Disabled summaries skip pointer/Tab;
  closed disabled content is omitted and initially open content remains. CollapsibleBehavior,
  DisclosureTriggerBehavior and facade helpers support caller markup; apps manage its disabled
  content visibility. Plain disclosures need no script; height does not animate.
- Ported structure/identity for both skins and embedded bundles, with attribution. Tabs
  explicitly reset native button backgrounds and inherit the host font, so embedded usage
  needs no global preflight. Long labels scroll within the tab list, vertical lists reserve
  room for their panels, and collapsible labels wrap with per-skin minimum button heights.
  Native headings/content wrap long words. Reduced motion and forced-color focus are retained.
- DisclosurePage (/disclosure) covers URL-backed tabs, vertical/manual/nested views, hidden
  and disabled tabs, input preservation, Ajax reset, exclusive/multiple/nested accordions,
  disabled open/closed sections and native fragment links. New showcase/Home links included.
  User query selection is validated without substring assumptions (empty/unknown values fall back).
- Landing button card now has Actions/Details tabs, native exclusive FAQ and a collapsible,
  with consistent inner padding and core styles/runtime. Both preview layouts/skins/modes
  work without storing state; no-script keeps Actions visible. Static FAQ uses one fixed
  unique name because it is not Ajax-replaced. README/status/site docs and roadmap updated.
- Verification: a complete 444-case Chromium browser run passed. Later disclosure-only
  responsive/embedded refinements passed all 32 new disclosure cases again in the final
  focused run; the remaining 412 previously passing cases are unaffected. Cases cover native
  Control-click (observe BrowserContext new-page events, not popup events), URL/reload,
  no-script routes, keyboard/roving/manual/nested state, find/fragment reveals, Ajax replacement,
  disabled content, RTL/320px, reduced motion, forced-color focus and unusually long labels.
  Ajax assertions wait for the actual old wrapper to disconnect before inspecting new names.
- Normal WCAG axe audits and settled screenshots passed at 1440/320px, both skins/modes,
  including open native sections. Both embedded skins passed without a global reset. Initial
  fragment reveal and long-label geometry passed separately. Forced-color views were inspected
  using screenshots/computed system colors and focus assertions; contrast auditing uses normal
  palettes (the forced-color diagnostic reported raw author-token contrast despite system-colored
  painted text). No palette workaround was retained for that diagnostic.
- Landing checks passed all 16 desktop/mobile, skin/mode and sidebar/header combinations,
  with tabs, open disclosures, return to the actions menu and WCAG axe audits. Initial no-script
  panel and native skip link passed; no preview cookie/localStorage was created. Built site:
  101 references, 76 unique IDs, about 694KB. CSS/site builds, JS syntax and whitespace checks pass.
- Next: attachment displays. All accumulated local user-menu/theme/layout/disclosure changes
  remain uncommitted and unpublished; published baseline remains 1909d80.
- Final normal verify passed 494 core and 32 showcase-page tests and packaged both modules.
  Site build/check passed again after packaging. Temporary preview servers were stopped.


### Phase 6 attachment displays — local implementation, 2026-11-05

- Implemented SwAttachment, SwAttachmentGroup, AttachmentBehavior, AttachmentGroupBehavior
  and AttachmentControlBehavior, plus Sw.Behaviors factories. Escaped filename/description,
  optional thumbnail/icon or arbitrary media, arbitrary actions and named stretched triggers.
  Horizontal/vertical, full-width, three sizes and five presentation states follow Shadleaf.
- Oat has no corresponding attachment display API; reviewed its upload/avatar/card patterns
  and upload tests. Read Shadleaf's attachment docs, templates, component/browser tests,
  structural CSS and both skin blocks before implementing. Upload fields remain separate;
  applications own persisted URLs, authorization, storage, actions and readable status text.
- Actions are siblings above the stretched trigger and precede it in tab order. Native links
  and downloads remain native; preview invokers target an actual dialog element. External
  native invokers do not synchronize SwDialog.isOpen(); use a custom Ajax trigger calling
  open(target) when server state/replacement is needed. README documents this distinction.
- Models detach and load once per request; null alt models preserve decorative inline images
  or named compact fallback media. Reused avatar load/error handlers now cover attachment
  images, including loading, failures, cached URLs, src changes and Ajax replacements.
- Both skins include attachment identity and compact actions. Triggers reset ordinary button
  geometry/background with enough specificity to remain transparent on hover. Native focus,
  reduced motion, forced colors, RTL scrolling, row/grid groups and print text are covered.
  Scroll-driven group fades use non-document timelines; browser settling ignores those.
- Added /attachments, navigation/home links and real SVG/text resource downloads. Showcase
  includes native image previews, file removal/restoration with restored focus, one polite
  status region and a server-rendered state cycle. Note sample is 114 bytes; the final source
  corrects that display-only label after verification. No API/behavior changed with that label.
- Landing studio has the actual embedded attachment anatomy/CSS/runtime, an original SVG
  illustration, separate native download and labelled preview dialog. Theme synchronization
  includes this dialog. README, site docs and saved roadmap now include attachment displays.
- Final normal verify passed 515 core tests and 33 showcase-page tests. Final focused browser
  run passed all 28 attachment cases in both skins/modes, including no-script, embedded CSS,
  print, keyboard, downloads, state/Ajax changes, fallback images and WCAG audits.
- Full browser run exercised 472 cases: all 444 existing cases and 26 new cases passed;
  two new dark/forced-color audits failed because axe mixed authored colors with the system
  canvas. Fixed the test setup by restoring authored colors before the shared audit, following
  the existing showcase tests. Final focused run verifies those fixes plus four new embedded/
  print cases (476 total browser cases now defined). The full suite was not repeated after
  localized attachment hover/print/alt-model refinements; do not describe a single green
  final 476-case run. No old component regression remained in the broad run.
- Landing checks passed 16 desktop/mobile × skin/mode × layout combinations, with native
  downloads, previews, focus return, transparent hover, axe and no overflow/storage. Native
  download/preview/close passed without JavaScript at desktop/mobile widths. Stable Wicket
  visual checks passed eight 1280/320px skin/mode combinations with axe and no overflow;
  a deliberately delayed thumbnail showed the fallback before revealing the loaded image.
  Site build/check passed 108 references, 79 unique IDs and about 723 KB. Diff whitespace clean.
- Verification logs: /tmp/shadwick-attachment-final-verify.log,
  /tmp/shadwick-attachment-full-browser.log, /tmp/shadwick-attachment-browser-final.log,
  /tmp/shadwick-attachment-site-final2.log, /tmp/shadwick-attachment-visual-final.log.
- Next: Phase 7 table/data table, then pagination and bookmarkable sort/filter/page state.
  Eight main component families remain. All accumulated local user-menu/theme/layout/
  disclosure/attachment changes remain uncommitted and unpublished; baseline is 1909d80.
  No dependency or CSP changes were made. Preview servers are stopped at handoff.


### Phase 7 data tables and pagination — local implementation, 2026-11-05

- Implemented SwTable/TableBehavior and SwDataTable with standard Wicket columns,
  sortable providers, native headers/aria-sort, escaped captions/labels, toolbar/empty slots,
  density, sticky first columns and a separate responsive footer. Helpers: SwBadgeColumn,
  SwNumberColumn, SwDateColumn, SwBooleanColumn, SwLinkColumn, SwActionsColumn,
  SwSelectionColumn and reusable SwHeadersToolbar. Column aliases map end/center alignment
  onto native header attributes so sort-button padding follows the correct skin geometry.
- Added official wicket-extensions at the existing managed Wicket 10.11.0 version, with
  parent dependency management. Asked as required by this file; the user's "can you please
  continue" followed the proposed module and was treated as authorization to proceed with
  that recommendation. No other dependency/version change and no CSP exception.
- References read: OatDataTable, OatPagination, paging navigator, header/navigation toolbars,
  formatted/action/selection column APIs and their feature/key-selection tests; Shadleaf
  table/pagination/query-params docs, templates, CSS and both skins, component tests and
  scroll/size/selection browser tests. Official cached Wicket sources verified DataTable,
  header/link contracts and paging behavior. Oat CSS/markup were not ported; Apache Wicket
  DataTable markup adaptation and attribution are recorded in NOTICE.
- SwPagingNavigator implements bounded first/last/sibling/ellipsis windows over IPageable,
  including single-page gaps and empty results. SwPagination retains Oat's caller-owned
  nav/menu/items factory. SwPaginationSummary locale-formats ranges/pages. SwPaginationSize
  is a named native popover of radio links, always including the current size. Controls
  accept real URL factories or Ajax fallback links with protected update hooks.
- TableQuery copies decoded PageParameters, preserving unrelated/repeated/indexed values.
  Page changes preserve filters; sorting/size/search reset only that table's page. Qualifiers
  and configurable parameter names isolate multiple tables. SwQueryParameters emits escaped
  native GET hidden inputs. setQuery opts a data table into bookmarkable navigation with
  sortable-column allowlisting and capped URL page sizes. Default mode stays stateful Ajax
  with normal Wicket fallbacks; it does not simulate browser history.
- Key selection has optional loaders for current selected rows across filters, named checkboxes,
  locked-row predicates and page-local select-all. Headers use rendered row models, avoiding
  an extra row query. Native indeterminate is seeded on initial and replaced markup. Bulk
  callbacks receive fresh rows and clear selection explicitly; the count is not a live region.
  Selection/predicates are rechecked on invocation. Application callbacks own authorization
  and persistence. Full CSV/column chooser/row-details/inline-edit APIs remain later extras.
- Row actions reuse SwDropdown. Optional unique row keys produce stable trigger/menu IDs
  across Ajax replacements. Dialog focus now remembers a menu's trigger by ID for server
  opens, so replacing an opener while editing still returns focus on close. Native/default
  focus and explicit application focus choices remain in charge where valid. Focus records
  are cleaned up with removed dialogs; the shared runtime gained no framework or inline JS.
- Both skins port Shadleaf table/pagination identity, with common direct-child structure for
  nested tables, native column groups, compact spacing, RTL/pinned edges, print and forced
  colors. Table checkbox labels add 24px hit areas without enlarging the visible checkboxes.
  The showcase's known-wide text report has an explicitly named, focusable scroll wrapper.
- Added /tables, /data, /invoice and /pagination with navigation/home links. The invoice demo
  has session-scoped records, stable sorting with an ID tie-breaker, native GET filters,
  bookmarkable/Ajax modes, detail links, modal create/edit with validation, locked rows,
  paid-status changes, confirmed deletion and selection/bulk updates. Qualified project/archive
  pagers demonstrate independent URL state. No-JavaScript sorting/filtering/paging/size menus
  and invoice links work; modal CRUD/live selection require script, as documented.
- Landing studio now includes a full-width invoice table using the actual embedded CSS,
  with transient filtering/sorting/selection/paging and native previews. Original rows remain
  visible without script. No persistent state or network mutation is used. README, site docs
  and roadmap include the new APIs. Data work and all earlier local groups remain uncommitted
  and unpublished; published baseline stays 1909d80. No commit/push/publication was requested.
- Final normal verify passed 544 core and 37 showcase-page tests. Final focused data browser
  suite passed all 32 skin/mode cases, including CRUD, validation, URL reload/history, native
  and Ajax fallbacks without script, selection/focus, two pagers, RTL, print, embedded/no-reset,
  forced colors and WCAG audits. All four new page families are in the broad page audits.
- The full 524-case browser run passed 523 cases; the sole failure was the pre-existing
  sidebarForcedColorsAndReducedMotionKeepCurrentAndFocusedLinksVisible (Vega/light) focus
  timing case. Its isolated recheck passed all four variants without changing sidebar code.
  The later localized header-alignment attribute refinement passed final core/page tests and
  the focused 32-case suite. Do not describe a single green final 524-case run.
- Landing checks passed all 16 desktop/mobile × skin/mode × navigation-placement combinations
  for sorting, filtering/empty results, selection/mixed state, paging, preview/close/focus,
  no overflow, WCAG axe and no storage/cookies. Desktop/mobile no-script checks retained all
  six native rows. Stable Wicket visual checks passed 16 data/pagination × 1280/320px × skin/
  mode combinations with axe and no overflow. Screenshots were reviewed. Site build/check
  passed 128 internal references, 94 unique IDs and about 761 KB; syntax/whitespace checks pass.
- Logs: /tmp/shadwick-data-final-verify2.log, /tmp/shadwick-data-browser-final.log,
  /tmp/shadwick-data-full-browser.log, /tmp/shadwick-data-sidebar-recheck.log,
  /tmp/shadwick-data-site1.log and /tmp/shadwick-data-visual1.log. Preview servers stopped.
- Next: custom select, then combobox; calendar/date picker, month view and charts follow.
  Six main families remain. Wicket extensions is now available for a later lazy ITab/
  AjaxTabbedPanel integration, but that API was not included in this group.


### Phase 8 custom select — local implementation, 2026-11-05

- Implemented SwSelect<T>, SelectBehavior and Sw.Behaviors.select(), retaining the native
  DropDownChoice constructor/model/renderer shape, CompoundPropertyModel and short input paths.
  Groups preserve flat choice order/IDs; icons come from the shared trusted SwIcon registry
  through decorativeMarkup. Separators, placeholders/optional clear choice, disabled predicates,
  default/small sizes and normal/Ajax inline validation are supported. No new dependency.
- Choice models snapshot once per request and detach even for invisible fields. Conversion
  allowlists current renderer IDs before consulting a custom loader and rejects disabled,
  missing/removed IDs and newly loaded disabled objects. Text/attributes remain Wicket-escaped.
- Native base-select uses styled browser picker/selectedcontent; the vanilla select.js resource
  enhances other Popover API browsers with a combobox/listbox and leaves the native source in
  charge of values, reset, submission, validation and events. No APIs or no script retains the
  platform picker. There are no inline styles/handlers, framework or CSP changes.
- Keyboard/pointer, bounded navigation, paging, typeahead/repeated letters, commit/cancel,
  form reset, programmatic input/change, dynamic options, inherited fieldset disabling, ARIA
  name/description/error/required mirroring, page history sync, idempotence and Ajax cleanup
  are covered. Fallback native inner buttons leave the tab order; separators are decorative.
  CSS uses implicit anchors and an older-browser viewport fallback. Both skins and bundles
  include geometry, RTL, forced colors and reduced motion. Native/fallback select Escape inside
  shared popovers/mobile sidebars leaves the parent overlay open; core popup focus stays separate.
- Added /custom-select with normal/Ajax/live forms, refreshed choices, reset, icons/groups,
  disabled options/field, long list and fields inside native dialogs/popovers. Added navigation,
  home and page-audit coverage. The landing plan field now uses actual custom-select markup,
  CSS/runtime, preserving its existing transient demo values and native no-script fallback.
  README, site docs, NOTICE and current roadmap updated. Next: combobox; five families remain.
- Final normal verify passed 558 core tests and 38 showcase-page tests. The final focused
  browser suite passed all 36 cases in both skins/modes, including native and forced fallback,
  normal/Ajax submissions, refreshed options, native reset, focus, naming, disabled fieldsets,
  typeahead reset, history sync, unavailable enhancement APIs, no-script, embedded CSS, RTL,
  forced colors, reduced motion, nested overlays and WCAG audits. Syntax/whitespace checks pass.
- The full 560-case browser run passed every case, including all older components and the new
  custom-select page audits. Later localized refinements reset typeahead on close, sync history,
  guard unsupported APIs and detach/revalidate current choice models. Final focused/normal
  suites above passed after those changes. Four additional API-fallback cases make 564 browser
  cases now defined; do not claim a final all-564 run. No old regression remained in the broad run.
- Landing checks passed 32 desktop/mobile × skin/mode × navigation-layout × native/fallback
  combinations with keyboard selection/cancel, no overflow, WCAG axe and no select inline styles.
  No-script plan selection passed at desktop/mobile widths. Eight additional mobile-sidebar
  skin/mode/picker checks verified first-Escape picker dismissal and second-Escape sidebar close.
  Stable Wicket visual checks passed 16 1280/320px × skin/mode × picker combinations with open
  list audits, screenshots and no overflow. Screenshots were reviewed. Final site build/check
  passed 135 internal references, 94 unique IDs and about 800 KB.
- Logs: /tmp/shadwick-select-final-verify.log, /tmp/shadwick-select-browser-final.log,
  /tmp/shadwick-select-full-browser.log, /tmp/shadwick-select-site-final.log,
  /tmp/shadwick-select-sidebar-final.log and /tmp/shadwick-select-visual2.log.
- All accumulated local groups remain uncommitted/unpublished; baseline stays 1909d80.
  No commit/push/publication requested. Preview servers stopped at handoff. Next: combobox.


### Phase 8 combobox — local implementation, 2026-11-05

- Implemented SwCombobox<T>, SwMultiCombobox<T>, ComboboxBehavior and Sw.Behaviors.combobox().
  Native DropDownChoice/ListMultipleChoice own IDs, values, normal/Ajax submission and BaseSwField
  feedback. Constructors retain native choice-model/renderer shape; remote entry points use
  Oat-style setChoices/setDisplay/setMaxChoices, plus explicit stable choice IDs/resolvers.
- Shared ComboboxOptions snapshots local choices once per request, detaches even when hidden,
  escapes display/group/placeholder text, checks duplicate/nonempty IDs and rejects unknown,
  disabled and removed choices atomically. Remote submission resolves current authorized
  objects, checks their ID and disabled predicate again, and never caches search result objects
  in the page. Wicket owns a bounded same-origin JSON GET callback (256 query characters;
  default 10, configurable 1–200 results). Services must also bound their underlying query.
- Valid converted remote values join the rendered choices when another form field fails normal
  submission, preserving the user's selection while the model remains unchanged. Raw-input
  checks prevent stale converted values from becoming suggestions after a model/reset change.
  Both single/multiple cases have WicketTester coverage. Stable IDs distinguish duplicate labels.
- Vanilla combobox.js adapts Shadleaf's editable-input/listbox/chip pattern, including case/accent
  filtering, groups/separators/icons, explicit selection/optional first-match highlighting,
  clear, chip keyboard removal, IME, native reset/default restoration, Ajax teardown and history
  sync. ARIA names/description/error/required and inherited fieldset disabling follow the real
  select; label focus reaches the input. Search preserves chosen options, aborts superseded
  requests, rejects redirects/bad content and supports a cancelable sw-combobox-search event.
  Busy/empty/error messages and delayed result status are localizable through field resources.
- CSS uses implicit popup anchors with a viewport fallback, no inline coordinates/styles.
  Both independent skin identities and bundles include forced colors, RTL, reduced motion and
  component-scoped border-box sizing for hosts without a reset. Manual lists stay above native
  dialogs/popovers/sidebar, close on outside interaction/parent close, and consume one Escape
  layer. Existing shared popup autofocus remains separate. No library dependency or CSP change.
- Added /combobox with local single/multiple normal/Ajax forms, live validation/replacement,
  single/multiple server search, native reset, disabled selections and overlay examples.
  Navigation/home/page audits include it. Landing Project teams uses the actual combobox runtime
  and shows chosen teams in its transient save toast; without script it is a native multiple
  select. README, site docs, NOTICE and current roadmap updated. Next: calendar/date picker;
  four main families remain. All accumulated groups remain local, uncommitted and unpublished.
- Final normal verify passed 579 core tests and 39 showcase-page tests, including the new
  single/multiple full-form remote-choice preservation cases. Final browser recheck passed all
  56 cases: all 48 combobox cases and eight existing popover/data-CRUD skin/mode cases. Checks
  include filtering, no accidental choice, icons/groups, events, clear/reset/chip keys, native
  no-script/API fallbacks, Ajax feedback/focus/replacement, single/multiple server search,
  debounce/cancellation, a deliberately delayed stale response, search parameters/error recovery,
  ARIA, IME, fieldset disabling, history/idempotence, embedded/RTL/mobile, forced colors,
  reduced motion, nested overlays, strict CSP and WCAG audits.
- The broad 616-case browser run passed 614 cases; one existing popover tooltip check and one
  existing data-dialog contrast audit failed. Recheck showed the tooltip was correctly hovered
  after native popup placement moved it under the stationary pointer. The focus-dismissal test
  now moves the pointer away before its assertion. The data audit now uses the shared finite
  animation settle helper before checking the open dialog. No tooltip or data-table behavior
  was changed. All eight variants passed in the final focused run. Do not claim a single green
  final all-616 run; the later localized remote full-form retention fix passed final core/focused
  checks, and the full suite was not repeated after those refinements.
- Landing checks passed all 16 desktop/mobile × skin/mode × navigation-layout combinations for
  filtering, chips, clear, keyboard removal, save toast, WCAG axe and no overflow. Both widths
  passed native no-script multiple selection. Four additional mobile-sidebar skin/mode checks
  verified one-layer Escape dismissal after settling the sidebar's initial focus timer. The
  component emits no inline styles; axe can leave an empty style attribute on its measured input,
  so the harness checks initial markup and rejects any nonempty generated styling afterward.
- Stable Wicket visual checks passed eight 1280/320px × skin/mode combinations, each with open
  filtered-list and selected-chip audits/screenshots (16 views). Screenshots were reviewed.
  Final site build/check passed 136 internal references, 95 unique IDs and about 864 KB.
  JavaScript syntax and diff whitespace checks pass. Jar includes the new APIs/resources.
- Logs: /tmp/shadwick-combobox-final-verify1.log, /tmp/shadwick-combobox-browser-final2.log,
  /tmp/shadwick-combobox-full-browser.log, /tmp/shadwick-combobox-popover-recheck.log,
  /tmp/shadwick-combobox-site-final3.log and /tmp/shadwick-combobox-visual2.log.
- No commit, push or publication was requested. All accumulated groups remain local and
  uncommitted, published baseline 1909d80. Preview servers stopped at handoff. Next: calendar,
  then date picker, month view and charts (four main families remain).

### Phase 8 calendar and date picker — local implementation, 2026-11-05

- Implemented SwCalendar/SwDatePicker for LocalDate and SwRangeCalendar/SwDateRangePicker
  for inclusive DateRange(from,to), plus CalendarBehavior and Sw.Behaviors.calendar().
  Reused BaseSwField labels/feedback and native ISO date conversion, with normal form,
  CompoundPropertyModel and live/Ajax updates. No dependency or CSP change.
- RangeDateInput validates both native endpoints atomically, including field-level Ajax;
  required ranges need both ends, optional ranges permit open ends, and empty pairs bind null.
  The runtime enables recursive serialization only for the range input panel through Wicket's
  public Ajax event hook. Already-validated children are recorded per request to avoid running
  their validators twice during full-form processing. Invalid submissions preserve the model.
- Shared server rules reject reversed ranges, min/max, disabled dates/ranges/weekdays and
  read-only changes. Complete ranges also check unavailable interior dates efficiently.
  Picker labels retain raw submitted dates when another field blocks model updates.
- Adapted Shadleaf's grid/caption/range structures and independent Vega/Lyra skins, with one
  small vanilla calendar.js resource. Supports 1–12 months, month/year selects, locale week
  rules/names, outside days, today, keyboard/roving focus, RTL and range hover. Date arithmetic
  and Intl formatting use Gregorian UTC date-only values, including early years and skipped
  civil days; values/configuration use years 1–9999. Navigation limits remain distinct from
  server input bounds. Dates always submit as ISO strings.
- Pickers compose existing native non-modal popovers, anchor placement and focus/Escape return;
  nested dialogs/popovers keep one-layer dismissal. Native date fallback remains usable without
  script or Popover API. Reset/canceled reset, native input replacement, Ajax cleanup, dynamic
  attributes, disabled fieldsets, forced colors and reduced motion are covered.
- Added /calendar with inline single/two-month range grids, birthday dropdowns, normal/Ajax
  booking forms, live single/range validation, refresh, disabled/read-only dates and nested
  overlays. Navigation/home/page audits include it. Landing studio adds an inline catch-up
  calendar; its project dialog now uses the actual date-picker runtime and existing save toast.
  README, site docs, NOTICE and current roadmap updated.
- Focused browser verification passed all 56 calendar cases (14 cases × both skins/modes),
  including keyboard, range event coherence, required/native/server validation, normal/Ajax/live
  submissions, resets/replacement, locales/leap day, no-script/API fallback, nested overlays,
  Apia's skipped 2011-12-30, embedded 320px/RTL, strict CSP, forced colors and WCAG axe audits.
- Landing checks passed 16 desktop/mobile × skin/mode × navigation-layout combinations,
  including picker focus/dismissal inside the project dialog, saved launch date, axe and no
  overflow; two no-script widths passed. Stable Wicket visual checks passed eight desktop/320px
  × skin/mode combinations, with inline/open range audits and screenshots (16 views).
  Reviewed desktop/mobile Vega/Lyra screenshots. Final site build/check passed 138 internal
  references, 98 unique IDs and about 929 KB. JavaScript syntax and diff whitespace checks pass.
- Full regression passed all 606 core tests and all 676 browser cases, with no failures
  or skips (/tmp/shadwick-calendar-final-browser.log). The browser profile runs the browser
  class in place of ordinary showcase-page tests. This broad run preceded the localized
  change hiding outside-month dates in year 0/10000; the subsequent focused calendar recheck
  also covers keyboard navigation at both supported year limits.
- Final focused recheck passed all 56 calendar browser cases, including both year limits
  (/tmp/shadwick-calendar-final-focused.log). Final normal verify passed 606 core tests and
  40 showcase-page tests (/tmp/shadwick-calendar-final-verify.log). No failures or skips.
- Additional logs: /tmp/shadwick-calendar-site2.log and /tmp/shadwick-calendar-visual1.log.
  Temporary showcase/landing preview servers were stopped. No commit, push or publication
  was requested; all accumulated groups remain local and uncommitted. Published baseline
  remains 1909d80. Next: month view, then charts (two main component families remain).

### Phase 8 month view and charts — local implementation, 2026-11-05

- Implemented SwMonthView: native server-rendered table, locale/overridden week starts,
  week numbers, outside/today states, independent month parameters, bookmarkable month/day
  links preserving filters, custom typed Day content, explicit month/today models and nav sizes.
  Years 1–9999 are supported; outside boundary dates have no content/links and navigation
  stops at the limits. Disabled navigation retains a link role for its accessible name.
- Implemented SwChart<T>/ChartSeries<T> in components.chart. Follow Oat's model/category/
  serializable-extractor pattern and native SVG/data table, using Shadleaf's independent
  tooltip/legend skin identity. No charting dependency or CSP change. Bars, lines, areas,
  composed series, grouped/stacked bars/areas, separate negative stacks, null gaps, dashed
  series, points, linear/step/monotone curves, grid/axes, legend and empty state are covered.
- Category/value data is snapshotted once per request, all labels/values escaped, and data,
  title/category and series-label models detached. Native table keeps original formatted
  values; SVG uses finite doubles of magnitude at most 1e100, including subnormal values.
  Locale formatting supports number/integer/compact/percent/currency and application
  callbacks. Date-only values stay calendar values; instants use an explicit ZoneId (UTC
  default), independent of the browser. Five palette tokens need no generated inline styles.
- Small vanilla chart.js adds pointer/keyboard tooltips from the same data cells, roving by
  Left/Right/Home/End with RTL order, Escape before a containing dialog, live announcements,
  Ajax listener/observer cleanup and idempotent/history-clone enhancement. Native SVG titles
  are suppressed while enhanced to avoid duplicate browser tooltips. SVG/table work without
  script. Charts are a native alternative to Recharts, with variant gaps explicitly recorded:
  forecast joins, arbitrary per-series light/dark colors, natural splines, per-corner bar radii.
- Added /month-view and /charts, navigation/home links, page render tests and broad audits.
  Examples include normal/native links, day selection/filter preservation, independent months,
  Ajax month/chart updates, all chart types, currency, gaps/negatives and an empty report.
  Landing adds a Plans and progress chart with native data table and an October–December
  agenda using native fragment links. Chart marks explicitly override host icon strokes;
  returning to the first month preserves its visibility. README/site docs/NOTICE updated.
- Focused browser recheck passed all 40 cases across both skins/modes, including native/no-script
  paths, filter preservation, independent months, Ajax replacement, cloned history markup,
  year limits, touch targets, keyboard/hover tooltips, exact data/legend, one-layer modal Escape,
  320px embedded RTL, forced colors/reduced motion, CSP/console and WCAG axe audits.
- Stable showcase visual checks passed all 16 route × desktop/320px × skin/mode combinations.
  Landing checks passed all 16 desktop/mobile × skin/mode × navigation-layout combinations,
  plus two native/no-script widths. Screenshots reviewed for Vega/Lyra at desktop/mobile widths.
  Logs: /tmp/shadwick-month-chart-browser2.log and /tmp/shadwick-month-chart-visual3.log.
- The broad 724-case browser run passed 723 cases. One pre-existing calendar Ajax replacement
  case logged an initialization attempt while the native input was temporarily absent. Calendar
  scanning now waits for the native inputs, trigger/value and popup before registering a picker;
  range panels also wait for both endpoints. The calendar replacement test now deterministically
  removes/reinserts its native input across an event-loop turn. This is a runtime readiness fix,
  not a suppressed console assertion.
- Final focused verification passed all 96 calendar/month-view/chart browser cases, with no
  failures or skips (/tmp/shadwick-month-chart-final-focused.log). It includes the readiness
  fix, real touch category picking, and the final month-view correction that avoids invoking
  custom content/link factories for hidden or unsupported dates. The full 724-case run was not
  repeated after these localized refinements; do not claim a single green final full-suite run.
- Final site build/check passed 149 internal references, 104 unique IDs and about 999 KB.
  JavaScript syntax and diff whitespace checks pass. Temporary preview servers were stopped.
  No commit, push or publication requested: all accumulated groups remain local/uncommitted,
  with published baseline 1909d80. Next implementation group: additional skins, Nova first.
- Final normal verify passed all 625 core tests and 42 showcase-page tests, with no failures
  or skips (/tmp/shadwick-month-chart-final-verify2.log). Jar contents include both new APIs,
  markup/messages, chart.js, skin bundles and updated NOTICE. All main component families now
  have implementations; detailed variant parity and the broader roadmap remain separate work.

### Phase 9 Nova skin — local implementation, 2026-11-05

- Added Skin.NOVA, a full independent nova.css identity and standalone/embedded entry files.
  Shared component structure/tokens and existing Vega/Lyra files are unchanged. Nova follows
  Shadleaf's complete skin geometry while preserving Wicket-specific form/dialog wrappers,
  native fallbacks, contrast fixes, Ajax lifecycle and the library's component namespace.
- Covers all implemented families: compact 32px default/28px small/36px large buttons,
  rounder controls without control shadows, tinted disabled and distinct read-only inputs,
  16px mobile text, tighter cards/fields/disclosures/menus/sidebar/pagination, muted flush
  card/dialog footers, calendar/date-picker, month view and native chart identities.
- Resource registration automatically includes Nova through Skin.values(). Both standard
  showcase and custom app/header layout controls expose it. Skin-store page tests cover Nova
  persistence. CSS/resource tests run for every enum skin, including no utilities, no embedded
  reset/theme-variable leakage, both color modes and packaged minified variants.
- Landing copies the actual Nova embedded bundle, exposes its selector option and uses the
  selected option's label. Extra studio card shadows remain specific to Vega/Lyra. Build checks
  require the offered skin assets. README, site README, NOTICE and roadmap updated; six skins
  remain (Maia next, then Mira/Luma/Rhea/Sera/Sketch).
- Browser skin lists derive from Skin.values(); optional -Dshadwick.browser.skins=nova runs all
  scenarios for one skin without changing the default full matrix. Added visual behavior checks
  for real button dimensions/radii, card/footer geometry, mobile input size and disabled/readonly
  appearance. Initial Nova page/identity checks passed all 74 cases in light/dark.
- Visual verification passed 28 route × desktop/mobile × light/dark views across display cards,
  inputs, native dialogs, menus, open range pickers, charts and month view. Eight landing checks
  passed desktop/mobile × light/dark × sidebar/header layouts, including chart keys, native
  month navigation, nested picker Escape, project submission and switches between all three skins.
  Every view passed WCAG axe/overflow/console checks; representative screenshots reviewed.
  Logs: /tmp/shadwick-nova-browser1.log and /tmp/shadwick-nova-visual1.log.
- Full Nova interaction regression passed all 368 browser cases in light/dark, with no failures
  or skips (/tmp/shadwick-nova-full-browser.log). Final geometry/identity comparison passed all
  18 Vega/Lyra/Nova cases (/tmp/shadwick-nova-final-geometry2.log), including Nova's 10px default
  button radius. Lyra's existing 12px mobile input identity is preserved; Vega/Nova use 16px.
- Final site build/check passed 149 internal references, 104 unique IDs and about 1,226 KB.
  JavaScript syntax and diff whitespace checks pass. Jar contains all four Nova CSS resources.
  Temporary preview servers were stopped. No commit, push or publication requested; all
  accumulated groups remain local/uncommitted, published baseline 1909d80. Next: Maia.
- Final ordinary verify passed 630 core tests and 43 showcase-page tests, with no failures
  or skips (/tmp/shadwick-nova-final-verify.log). Packaged standalone/embedded Nova resources
  and updated NOTICE are included; no runtime dependency or CSP changes.

### Phase 9 remaining skins — local implementation, 2026-11-06

- Added Skin.MAIA, MIRA, LUMA, RHEA, SERA and SKETCH, each with an independent complete
  identity and standalone/embedded entries. The library now packages 36 CSS resources across
  nine skins, including minified variants. Existing Vega/Lyra/Nova sources, shared structures,
  color tokens, component Java APIs and JS runtimes are unchanged.
- Ports cover every implemented family, including Wicket form wrappers in dialogs/sheets,
  logical sheet edges, native menu/checkbox indicators, upload rows, data-table controls and
  native SVG charts. Outline buttons own their text color, tabs keep legible contrast on the
  landing palette, and sidebar group actions retain Shadwick's 24px minimum target.
- Sketch includes unmodified Balsamiq Sans Latin regular/bold WOFF2 and its OFL license in the
  jar. Same-origin relative font URLs work under the existing Wicket CSP. Embedded typography
  is scoped to sw- components; standalone pages also opt into body handwriting. NOTICE and
  resource checks cover the font assets and license; no Maven/npm dependency was added.
- CSS compilation now runs at most four compiler processes, preventing 36 simultaneous
  compiler/output listeners as the skin catalog expands. All bundles still build automatically.
- Showcase skin controls include all nine choices, including app/header layouts and session
  persistence tests. Geometry tests now describe each skin's actual controls, card spacing and
  mobile type; Sketch's joined toggle border is intentionally 2px. The landing uses the real
  nine embedded bundles, versioned dynamic URLs and local Sketch fonts. Extra decorative card
  shadows remain specific to Vega/Lyra; marketing copy and README/site docs list nine skins.
- Ordinary verify passed 661 core tests and 49 showcase-page tests with no failures or skips
  (/tmp/shadwick-skins-verify5.log). Jar inspection confirmed all 36 CSS bundles and three font
  assets/license files. The first geometry/embedded browser pass passed all 72 nine-skin/mode
  cases (/tmp/shadwick-skins-browser-geometry1.log).
- Screenshot/accessibility/overflow checks passed 168 new-skin × desktop/320px × light/dark ×
  route views, including open menus and range pickers. Representative screenshots were reviewed
  for all six skins (/tmp/shadwick-skins-visual2.log). Later Wicket-specific geometry and focus
  repairs received the focused rechecks recorded below.
- Final landing checks passed all 108 nine-skin × 1440/390/320px × light/dark × sidebar/header
  combinations, plus two no-script widths (/tmp/shadwick-skins-landing6.log). They include
  native dialogs/date pickers, image previews, chart keys, focus return, fonts, WCAG axe audits,
  overflow and console errors. All 24 new-skin Firefox desktop/mobile/light/dark checks passed,
  including actual downloads and disclosures (/tmp/shadwick-skins-firefox1.log).
- Light/dark comparison screenshots: /tmp/shadwick-six-skins-light.png and
  /tmp/shadwick-six-skins-dark.png. Site build/check passes 165 references, 104 IDs and about
  2,665 KB, including all nine optional skin bundles; the page initially loads only Vega.
- The broader 660-case run passed 622 and identified missing Wicket attachment hit-area and
  wrapped-summary geometry adaptations, plus old assertions that assumed all borders were
  1px. All 408 route/skin/mode page audits passed. Restored transparent full-card attachment
  triggers and auto-height summaries with skin-specific minimum heights. Also mapped Wicket's
  input-group icon size, overflow breadcrumb button and combobox active-item focus ring.
- The follow-up passed 142 of 144 affected cases, including all disclosure, upload-state,
  native/no-script, print, focus and embedded paths. The two remaining failures were another
  copy of the old attachment border tolerance; corrected both dimension assertions and the
  final all-nine-skin attachment checks passed all 36 cases, with no failures or skips
  (/tmp/shadwick-skins-browser-final-attachments.log). Earlier affected results are in
  /tmp/shadwick-skins-browser-affected5.log; do not claim one final green 660-case run.
- Final ordinary verify again passed 661 core and 49 showcase-page tests, with no failures or
  skips (/tmp/shadwick-skins-final-verify.log). Final affected landing checks passed 64 cases
  across both browsers, desktop/320px, modes and layouts, including the full-card hit area,
  separate native downloads, wrapped labels, WCAG axe and overflow
  (/tmp/shadwick-skins-final-landing.log).
- Final repaired attachment/disclosure screenshots passed all 48 new-skin × desktop/320px ×
  light/dark × route views (/tmp/shadwick-skins-final-visual.log); representative Sketch/Sera
  images were reviewed. Temporary showcase and static preview servers were stopped.
- The final CSS/JS syntax and diff whitespace checks pass; no dependency or CSP change.
  The implementation initially remained local; the user subsequently requested commit, push
  and landing publication, recorded in the current checkpoint above. Next: translations, then
  docs/tooling/release.


## Phase 10 — internationalization (2026-11-06, implemented locally)

- Added 217 UTF-8 bundles across all 31 existing message scopes: German, Spanish, French,
  Italian, Japanese, Dutch and Portuguese, plus the English base bundles. Key/placeholder parity,
  packaged resources, regional/unsupported-language fallbacks and component locale overrides are tested.
- Resource lookup follows Wicket's normal page/component hierarchy. An application can explicitly
  prioritize its own ClassStringResourceLoader; the library does not change loader order. Behavior-only
  markup uses a class-scoped fallback. Calendar locale overrides still affect date presentation only.
- Default combobox search placeholders now resolve on render for both enhanced and native fields.
  Explicit custom/empty/null values retain their meaning. Data-table names and action headings are
  resources; feedback toast dismiss/region labels are localized through the backward-compatible
  fourth legacy toast argument, retaining severity, safe text rendering and persistent timing.
- The standard showcase header has a native POST language picker, stable language-tag option values,
  localized labels and a locale-derived html lang. Skin/mode links preserve current page parameters.
  /internationalization demonstrates localized fields, calendars, dialogs, theme menus, notifications,
  uploads and table pagination. The theme control targets its own preview without changing reference mode.
  Application prose, titles, field labels, choices and actions remain application-owned.
- Browser audits caught pagination visible/accessibility-name mismatches in de/fr/it/ja/pt. Corrected
  translations now include the visible labels; all seven language audits pass. No accessibility rule
  was disabled. Regional locale tags are recommended for country-specific week/date/number conventions.
- Final ./mvnw -B verify passed: 691 core tests and 59 showcase-page tests. TranslationsTest has
  30 cases, including UTF-8 native date errors in all seven languages, page/application overrides,
  locale changes before Ajax replacement and translated full-render/Ajax feedback toasts. Existing
  pagination tests also cover German grouping and Japanese reordered range/page message arguments.
- Focused Chromium browser run passed 19 cases: all seven translated language controls with Ajax and
  dialog audits; Japanese mobile uploads, both toast APIs and pagination in Vega/Sera/Sketch light/dark;
  native language switching and combobox fallback without JavaScript in the same six skin/mode cases.
  Log: /tmp/shadwick-i18n-browser2.log. This is focused verification, not a full nine-skin suite run.
- Additional script passed 78 checks: 20 desktop/mobile screenshot+axe audits (Chromium Vega/Sera/Sketch,
  Firefox Vega/Sketch, German light and Japanese dark); 56 page/locale audits (seven languages across
  dates, uploads, data, user-menu, layouts, month-view, charts and custom-select); two landing widths.
  Checks include console errors and document overflow. Script/log: /tmp/shadwick-i18n-visual.cjs and
  /tmp/shadwick-i18n-visual.log; screenshots /tmp/shadwick-i18n-*.png. German/Japanese desktop/mobile
  screenshots and the narrow picker/header were visually inspected. WebKit remains unavailable locally.
- README documents shipped languages, locale selection, document language, browser-native field chrome,
  explicit overrides, resource-loader configuration and extending languages. NOTICE credits reused Oat
  translation strings. Static landing copy links to the new internationalization documentation while
  remaining English. Site build/check passed: 165 internal references and 104 unique IDs.
- No dependency was added. No commit/push/publication requested for this group. All changes remain
  local on top of published b6edbd6. Next: full component documentation and a hosted interactive
  showcase, then agent skill/llms.txt, release preparation and Maven Central. Chart variant follow-ups
  remain recorded in Later phases. Temporary preview servers were stopped at handoff.

### Croatian extension (2026-11-06)

- User requested Croatian after the initial internationalization group. Added all 31 _hr.utf8.properties
  core bundles, covering the same 83 distinct messages as English. There are now 248 translated core
  bundles and nine languages including English. No new dependency, loader, component API or JS change.
- Croatian uses hr; regional hr-HR falls back to these messages while retaining Croatia's formatting
  and week rules. Keep all ${variable}/{0} placeholders. Count messages use neutral wording (for example,
  Broj rezultata: {0}) so one template works without incorrect Croatian noun inflections.
- The picker includes hrvatski with localized Jezik/Primijeni labels. Existing language order is preserved
  by appending hr. Showcase introduction, home link, README, landing copy/site README and current roadmap
  list nine languages. README includes a Locale.forLanguageTag("hr-HR") setup example.
- JVM verification passed: 696 core and 60 showcase tests. Coverage includes Croatian dialog defaults,
  hr-HR fallback, UTF-8 date conversion errors/ISO formats, all keys/placeholders/packaged resources,
  localized table headings and Ajax refresh; Croatian regional range/page/empty summaries are tested.
  Log: /tmp/shadwick-croatian-verify2.log.
- Focused Chromium browser verification passed 26 cases: eight translated language control/Ajax/dialog
  audits; six Croatian mobile cases in Vega/Sera/Sketch light/dark with accented filenames, upload counts,
  direct/feedback notifications and pagination; twelve no-script Japanese/Croatian language changes and
  native combobox fallbacks in the same skins/modes. Log: /tmp/shadwick-croatian-browser.log.
- Four additional Croatian screenshot/axe checks passed in Chromium and Firefox: Sera desktop/light and
  Sketch mobile/dark, including accents, hrvatski/Jezik, Monday-first hr-HR weeks, console errors and
  document overflow. Visually inspected desktop Chromium and mobile Firefox screenshots. Script/log:
  /tmp/shadwick-croatian-visual.cjs and .log; screenshots /tmp/shadwick-croatian-{browser}-{width}.png.
- Site build/check passed: 165 internal references and 104 unique IDs. The temporary showcase server was
  stopped. All internationalization work, including Croatian, remains local/uncommitted on b6edbd6;
  no commit, push or publication was requested. Next roadmap group remains documentation/hosted showcase.


### Internationalization publication follow-up (2026-11-06)

- User authorized committing/pushing all internationalization and Croatian work and updating the landing.
  This supersedes the earlier local-only handoff notes. The current publication checkpoint is at the top.
- The landing has a responsive language panel under the catalog naming all nine languages and linking
  to README's internationalization section. Body contrast was strengthened after axe caught the initial
  color. Six Chromium/Firefox checks passed at 1440/390/320 px; desktop and narrow screenshots inspected.
- Checked previous Build run 42203649638: Java 17/25 passed; browser suite failed 16 of 3312 cases.
  Missing data-inset selectors in the six newest skins caused twelve inset-header failures; restored
  the same explicit contract as Vega/Lyra/Nova. Sera's provider retained a grid item's min-content width
  after long account names/RTL/forced colors; min-w-0 on the shared sidebar provider fixes the overflow
  without clipping content. Sketch uses five switch background layers; the RTL assertion now waits
  for every layer's expected position instead of comparing the computed list to a scalar string.
- All 54 related browser cases passed across nine skins and both modes. Existing accessibility,
  forced-color, focus, RTL and overflow checks remain enabled. No new dependency or browser threshold.
  Do not describe this as a complete full-catalog run. Log: /tmp/shadwick-publish-ci-repair2.log.
- Landing previews are generated from the rebuilt CSS/JS with content hashes. Site check now counts
  166 internal references and 105 unique IDs. Browser script: /tmp/shadwick-i18n-publish-site.cjs.


## Busy buttons and progress/meter/rating handoff (2026-11-06)

- User selected busy buttons and progress/meter/rating controls as the first additional Oat group;
  decide the next group afterwards. Implemented AjaxBusyBehavior, SwProgress, SwRadialProgress,
  SwMeter, SwRating and SwRatingField, with optional ProgressBehavior/MeterBehavior and facade methods.
  Busy setters are fluent on SwButton, SwSubmitButton, SwDialog, SwSheet and SwConfirmDialog.
- Shadwick.install adds one Ajax lifecycle listener, even if called twice. The opt-in busy runtime
  marks before queueing, preserves original aria attributes/focus and blocks repeated activation.
  It clears on success, cancellation and transport errors, respects replacement nodes, and resets
  after persisted pageshow. Native submitters remain enabled while serialized; their loading label
  is a sibling status indicator. Icon buttons replace their leading SVG with the spinner temporarily.
- busy.js can initialize the shared namespace before shadwick.js. The core now guards its own
  _coreInstalled flag so form feedback/dialogs still initialize and repeated core loads stay harmless.
  Browser form validation exposed this ordering case; the final checks cover it with strict CSP.
- Progress and meter are native elements, with finite model values, clamping and validated bounds.
  Null progress is indeterminate; a null meter is its minimum. Meter quality matches native threshold
  bands. Radial progress uses SVG attributes with no inline styles, localized percentage/optional
  escaped text, clamped accessible values, no zero-value dot and a reduced-motion-aware unknown state.
  Percentage truncation uses decimal arithmetic (29% remains 29%, avoiding binary rounding errors).
- Read-only ratings round to half-stars and distinguish null from zero. Editable ratings are native
  radios in the standard fieldset, bound to Integer with maximum 1..10, normal/Ajax validation and
  disabled support. Empty stars have SVG outlines, selected/hovered stars fill; focus and shapes
  remain clear in forced colors, and display half-stars mirror in RTL. Labels are resource text
  escaped inside fixed trusted SVG/span markup. A malicious page-resource label is regression-tested.
- Wicket's optional RadioChoice maps unknown IDs to null; the rating field rejects those IDs through
  convertChoiceIdToChoice so forged values cannot clear/overwrite an existing model. Native posted
  values are 1..max, not choice indices. Required, compound-property and disabled submissions are tested.
- Added AjaxBusyBehavior.loading, SwRating.label/none and SwRatingField.stars/unavailable in all nine
  languages. There are now 34 English bundle scopes and 272 translated core bundles. Rating radio
  names use neutral count wording, including Croatian, avoiding singular/plural-template mismatches.
- /status-controls shows linear/radial known/unknown progress, native meter bands, read-only ratings,
  editable/disabled ratings, normal/Ajax/native-input submitters, plain-link busy feedback, cancellation,
  Ajax replacement, icon-only controls and dialog confirmation. Demo handlers pause 400 ms to show busy
  feedback. The standard navigation and homepage link to it; skin/mode/locale controls remain available.
- Final ./mvnw -B verify passed: 730 core and 61 example-page tests. Logs:
  /tmp/shadwick-controls-verify6.log. Includes finite/clamped/invalid values, meter bands, model detachment,
  dynamic snapshots, exact percentages, locale/placeholder bundles, escaping and native field validation.
- Final main browser run passed 99 focused cases across all nine skins/light-dark and nine locales:
  duplicate activation, Ajax replacement, cancelled precondition, intentional network failure/recovery,
  native submitter serialization, validation/keyboard operation, dialog confirmation, native no-script
  forms, dynamic values, RTL half-stars, forced colors and reduced motion. Log:
  /tmp/shadwick-controls-browser4.log. A subsequent icon-only busy addition passed all 18 skin/mode
  rechecks in /tmp/shadwick-controls-icon-browser.log. Full catalog browser CI was not rerun locally.
- Twelve additional Chromium/Firefox visual/axe checks passed in Vega/Sera/Sketch, desktop/light and
  390px/dark, using hr-HR and real native-input Ajax submission. Busy states were held and captured in
  both browsers. Desktop Chromium, mobile Firefox and held-busy Firefox screenshots were inspected.
  Script/log: /tmp/shadwick-controls-visual.cjs and .log; screenshots /tmp/shadwick-controls-*.png.
- README documents APIs, bounds/unknown semantics, native markup and busy lifecycle. NOTICE credits Oat
  controls/lifecycle. Landing catalog copy includes progress/meters/ratings; site build/check passed
  166 references and 105 unique IDs. Generated site output stays ignored. No dependency was added.
- No commit/push/publication requested for this group. All changes remain local on top of published
  0d4a69a. Suggested next component group: advanced tables (CSV, column chooser, expandable row details,
  inline edits); docs/hosted showcase, agent skill/llms.txt and Maven Central remain separate roadmap work.
- Final landing browser checks also passed six Chromium/Firefox cases at 1440/390/320 px, covering
  axe, console errors and overflow with the rebuilt assets. Log: /tmp/shadwick-controls-site-browser.log.
  Both temporary preview servers were stopped. Working tree remains intentionally uncommitted.

### Advanced table features — local implementation, 2026-11-06

- User selected the next Wicket Oat group: CSV export, column chooser, expandable row details and
  inline edits. The earlier busy/progress/meter/rating changes remain intact and uncommitted.
  No dependency was added. References: /tmp/shadwick-parity-wicket-oat at 76562314a52eb37b517321632cfb6ef2f5187781
  and /tmp/shadwick-parity-shadleaf at ac2998704d3ed9590266ffca49bcc6455c70a025.
  Shadleaf's table docs explicitly leave visibility/details/editable cells to consumers; Java/API
  patterns follow Oat, with Shadwick's existing table, button, field and native-menu styles.
- SwDataTable.setColumnChooser/setColumnVisible/isColumnVisible/getVisibleColumnCount control named
  data columns. Structural selection/details/actions columns remain visible, and the last named data
  column cannot be hidden. Headers/cells share hidden state; empty/detail colspans follow it. Custom
  toolbar and tools coexist. Grouped/spanning headers reject visibility operations rather than render
  inconsistent geometry. The original column list and sort/selection/page state remain intact.
- setCsvExport adds a native attachment link. SwCsvExport streams all filtered rows in provider sort
  order, including hidden named IExportableColumns; it excludes structural columns and unfinished drafts.
  UTF-8 BOM, CRLF, quoting, formula-text neutralization (including leading whitespace/controls), raw
  numeric/boolean/Java-time values and application converters are covered. Fetches are bounded to 500
  rows. Value/row/provider models detach on failures, response streams stay open, filenames reject
  paths/control characters, and downloads have zero cache duration. Providers own export consistency
  and authorization; hiding a column is only presentation.
- SwRowDetailsColumn supports lazy factories with DETAILS_ID, expansion by row equality or stable keys,
  row names and rechecked expandable predicates. Native/Ajax disclosure links expose aria-expanded/
  controls and restore focus. Markup keeps real sibling tr elements inside a single tbody, with Wicket
  containers stripped; there are no invalid wrapping divs. A table accepts one details column.
- SwEditableColumn uses a separate draft with typed conversion, required/range/custom validators,
  rechecked edit predicates and an equality conflict check against the original value. Save callbacks
  run after the setter; exceptions restore the in-memory value and propagate. Applications still own
  authorization, transactions and database concurrency. onSave's target is null on native submissions.
  Enter saves; Escape/cancel discard drafts and restore focus. First inline errors have linked ARIA
  state and are excluded from page/toast duplicates through ShowsErrorsInline/NotShownInlineFilter.
- Wicket normally recreates rows on a full render, losing native editors. Editable tables now reuse
  visible rows by identity with forwarding models that adopt fresh provider delegates. setRowKey takes
  stable unique serializable keys; default row equality releases entity references on detach. The
  page-serialization regression uses a non-serializable entity behind a detachable model, restores an
  open editor and submits it successfully. Paging/filtering a row away discards its unfinished editor;
  browser input not posted to the server can reset during unrelated replacements. Nested cell forms
  scope validation/default buttons to the cell and require JavaScript, as documented.
- Shared layout rules and all nine skin identities cover editing/details. Sticky columns follow the
  first visible cell. Screenshot review corrected narrow tables: details stay within the visible
  scrolling area through container units/sticky positioning, and focus restoration scrolls the input
  fully into view. Fields/errors remain readable at 320px. Native menus retain the existing popup
  runtime; their transitions must settle before screenshot capture.
- New built-in table/edit messages ship in all nine languages; translated core bundle total is now
  280. /advanced-tables demonstrates filtering, paging, selection, locked projects, editable estimates,
  lazy project briefs, column choices and filtered CSV downloads. Standard nav/home, README, NOTICE,
  landing catalog and site README were updated locally. The static studio remains a transient preview.
- Final JVM verify passed 750 core and 62 example-page tests (812 total), including 20 advanced-table
  cases. Log: /tmp/shadwick-advanced-verify4.log. Earlier intermediate runs exposed and fixed native row
  recreation; the final serialization checks pass in /tmp/shadwick-advanced-serialization2.log.
- Advanced browser checks passed all 72 cases across nine skins/light-dark, including keyboard edits,
  inline validation, locked rows, details, chooser/selection, full/filtered CSV, native no-script paths,
  mobile overflow and explicit input/details geometry assertions. Log: /tmp/shadwick-advanced-browser2.log.
  Existing table browser checks passed 144 cases before the mobile refinement; the relevant sticky,
  nested/grouped, RTL, print and embedded layouts passed all 54 rechecks after it. Logs:
  /tmp/shadwick-advanced-table-regressions.log and /tmp/shadwick-advanced-geometry-regressions.log.
- Chromium/Firefox visual/axe checks passed 18 desktop/390px/320px cases in Vega/Sera/Sketch with Croatian,
  plus nine locale checks and two Firefox native cases (29 total). Script/log:
  /tmp/shadwick-advanced-visual.cjs and /tmp/shadwick-advanced-visual2.log. Screenshots:
  /tmp/shadwick-advanced-{chromium,firefox}-{vega,sera,sketch}-{1440,390,320}.png plus chooser variants.
  Corrected desktop Chromium, 320px Firefox Sketch and settled Firefox Sera chooser images were inspected.
  These UI/browser runs preceded only the final model-detachment/serialization refinement, which is
  covered by the final JVM suite; there is no full catalog browser-green claim for this working tree.
- Landing build/check passed 166 internal references and 105 IDs. Eighteen Chromium/Firefox screenshot/
  axe/console/overflow checks passed across three widths and Vega/Sera/Sketch. Log:
  /tmp/shadwick-advanced-site.log. Generated dist remains ignored. Temporary previews and the initial
  failed smoke-check browser process were stopped. No commit/push/publication requested for this group;
  published baseline remains 0d4a69a. Docs/hosted showcase, agent guidance/llms.txt and Maven Central
  remain on the roadmap, alongside deliberately scoped broader Oat families and chart variants.

### Publication request — controls and advanced tables, 2026-11-06

- User selected the next checkpoint: commit/push all accumulated changes and publish the updated
  landing page. This supersedes the earlier groups' local/uncommitted status statements.
- The checkpoint includes code, resources/translations, tests, examples, README/NOTICE, landing copy
  and this roadmap. Generated build/site output stays ignored. Existing final JVM and browser results
  are recorded above; publication runs fresh CI through the Build and Website workflows on main.
- Live URL: https://danielbartl.github.io/shadwick/ . Use the Website run's source SHA and deployment
  result, and `git log`, for the exact checkpoint rather than hard-coding the commit inside itself.

### Component documentation — local implementation, 2026-11-06

- User selected component documentation after publishing controls/advanced tables at 0ef053f. Implemented
  a static documentation section in the existing GitHub Pages project; no new hosting or dependency.
- docs/catalog.json and 52 focused Markdown guides cover all public Sw components and related behaviors,
  with setup, typed models, markup, options, keyboard/native/Ajax behavior, accessibility and application
  responsibilities. Six getting-started/integration guides and 46 component/family guides share the
  clover/forest identity. Code is selected for syntax highlighting but never executed in the reader.
- Migrated the large README reference into guides. Preserved all 49 original README headings as compatibility
  entry points and retained the quick start/build instructions. Source Markdown has its own readable index.
  Longer guides have feature-level anchors (CSV, details, inline edits, search, validation and options).
- site/docs/markdown.mjs is a tested, escaped authoring subset (documented in docs/CONTRIBUTING.md), with
  stable unique anchors, code IDs across callouts, safe URL validation, tables and accessible copy controls.
  API references expose direct constructors/methods/hooks, class inheritance, enums and record declarations
  for 150 public types. Routine lifecycle overrides/anonymous internals are excluded. Raw/private helpers
  and path traversal are rejected. New public Sw components require guide registration before a build passes.
- The responsive reader has a native mobile browse disclosure, native on-page navigation and API disclosures,
  static prev/next links and code copy with selected-text fallback. Keyboard search uses title/API/full-text
  indexing, Ctrl/Command+K, Enter/ArrowDown/Escape, no-result handling and network-failure recovery. Stale search
  requests cannot replace the current query. Search and copy hide without script; core content remains readable.
  Copy labels follow the visible Copy/Copied/Selected state. System dark mode, reduced motion, forced colors
  and light print tokens are included. No storage, external search service or client framework is introduced.
- Existing site/build.mjs generates docs and includes routes in the sitemap. site/check.mjs now checks every
  HTML page plus cross-page anchors, source Markdown links, search inventory and content-versioned assets.
  Landing links now open the appropriate docs and retain the original preview. Pages watches docs/** and README
  and runs renderer tests. The preview now serves nested directory indexes, preserves redirect queries and
  supports ephemeral ports/JSON responses. Source links remain relative under a GitHub project subpath.
- Complete docs/examples/QuickStartApplication.java, QuickStartPage.java and matching HTML are printed in the
  installation/forms guides. The build rejects stale fences. New DocumentationExamplesTest compiles those exact
  files with Java 17 release settings, loads the resources and tests native/Ajax validation/model submission.
  Existing public component runtime code was unchanged in this documentation group.
- Final normal Maven verify passed 750 core and 64 example tests (814 total). Log:
  /tmp/shadwick-docs-verify.log. The new compiled-example tests are two of the 64 example cases.
- Node tests passed all 34 checks covering escaping, URL validation, heading/code-ID behavior, Java API scanning,
  complete builds, coverage gaps, path traversal, stale snippets and invalid guide links. Log:
  /tmp/shadwick-docs-node-final.log. Final static build/check passed 54 HTML pages, 4069 internal references,
  748 IDs and Markdown/search links, about 3894 KB. Node worker tests need normal execution permissions locally;
  CI uses the Maven-installed Node and needs no new package installation.
- DocumentationBrowserTest passed 111 Chromium cases: every guide plus index at desktop/light and mobile/dark,
  keyboard search, real clipboard copy, denied-clipboard selection fallback, search network recovery, query-preserving
  redirects, API disclosures and native no-script navigation. Log: /tmp/shadwick-docs-browser2.log. The suite runs
  with the existing browser profile and starts/stops its own ephemeral static preview. It precedes only final
  editorial section headings/installation prose and print-token adjustments, checked separately below.
- Additional Chromium/Firefox visual/axe checks passed 42 cases over six representative pages at 1440/390/320,
  including no-script navigation and print. Final long-guide section navigation/readability rechecks passed 18
  cases after adding headings. Logs/scripts: /tmp/shadwick-docs-visual.cjs and .log,
  /tmp/shadwick-docs-final-reading.cjs and .log. Desktop advanced-table, mobile Firefox forms and dark overview
  screenshots were inspected. Browser review fixed the original preview's root-only routing and the copy button's
  visible/accessibility-name mismatch. The final images are /tmp/shadwick-docs-{chromium,firefox}-*.png.
- Landing regression audits passed 18 Chromium/Firefox cases at three widths in Vega/Sera/Sketch, covering axe,
  console errors, overflow and updated links. Log: /tmp/shadwick-docs-landing.log. Generated output remains ignored.
- Documentation is ready locally, intentionally uncommitted/unpublished. Published baseline remains 0ef053f.
  The temporary preview was stopped after final checks. Next roadmap work is hosting the actual Wicket showcase, then
  agent guidance/llms.txt and release/Maven Central; broader Oat families and chart variants remain separate.

### Documentation publication checkpoint — 2026-11-06

- User requested committing/pushing the docs and asked whether they belong to the landing page.
  They are part of the same GitHub Pages build at /shadwick/docs/, linked by the main navigation,
  catalog entries and internationalization links. The landing and docs publish together on main.
- This publication supersedes the local/uncommitted status recorded for the documentation group.
  Includes the 52 guides, searchable reader, 150 API references, exact compiled examples, docs browser
  tests, compatibility README, source/build/link checks, preview routing and workflow changes.
  Generated output remains ignored; no new dependency or hosting service was added.
- Before this push, the previous 0ef053f full browser run finished with 3 failures out of 3617 cases.
  Java 17/25 and Website had passed. One failure exposed chart initialization on an incomplete header;
  two rating assertions in Sera were affected by hover after responsive scrolling. Repair e78807d waits
  for a complete header/rows/SVG and tests staged rendering. Keyboard-rating checks move the pointer
  away before asserting the selected fill. All 54 targeted skin/mode cases passed in
  /tmp/shadwick-docs-publish-ci-fixes.log. This is focused verification, not a full-suite-green claim.
- Final local docs checks from the implementation remain recorded above. Publication JVM verification
  is in /tmp/shadwick-docs-publish-verify.log; Website runs fresh core and Node tests and checks the
  combined artifact. The new Build run also runs both Java versions and the complete browser profile.
- Live guides: https://danielbartl.github.io/shadwick/docs/ . Use git log and the Website workflow's
  head SHA/result for the exact source and deployment rather than embedding a self-referential commit.
- Next roadmap step remains hosting the interactive Wicket showcase; agent tooling and Maven Central
  follow. The guide website is static and the example source links do not imply a hosted Wicket backend.

## Clover Studio handoff — 2026-11-06 (implemented locally)

- User approved Clover Studio as a concrete agency workspace, rather than another component grid.
  `/studio` mounts StudioPage with overview/projects/project/invoices views. The existing component catalog
  remains at `/`, with a prominent Studio entry. No commit, push or publication was requested this turn.
- StudioWorkspace lives in bound Wicket session metadata. Immutable project/task/invoice snapshots, stable IDs,
  derived metrics/chart, task estimates, generated planning milestones and a capped activity feed share one
  model. Reset creates a new generation; project revisions/task-state checks reject conflicts. Delivery locks
  tasks, payment recording is idempotent, and mutation callbacks mark the session dirty.
- Working journey: edit Fern & Field, complete its remaining tasks, confirm delivery, download the real ZIP,
  inspect CLV-2402, record its payment, and see updated project counts/cash/activity. Scope preview/download
  works before delivery. New projects start empty and accept scoped tasks, without automatically issuing invoices.
  Project search/status/sort use native bookmarkable forms. Invoices provide sorting, pagination, column choices
  and CSV. Native browsing/filters/available downloads work without script; editing/modals/payments need script.
- Warm paper/forest palette, existing clover artwork, editorial client covers, responsive sidebar, themed controls,
  all nine skins and English/German interface locales. Demo utilities explain the language choices and session lifetime. No database, external service, file-storage dependency or payment integration was added.
- The longer journey exposed an existing SwSidebar serialization failure: inherited getMarkupId method references
  on Label and SwSidebar could deserialize through the wrong receiver cast. Replaced them with distinct explicit
  lambdas; grouped navigation and the entire Studio page now round-trip and perform restored actions in JVM tests.
- Final `./mvnw -B verify`: 751 core +79 example tests =830, all passing. Log:
  `/tmp/shadwick-studio-serialization-verify2.log`. Covers data transitions, conflict rejection, ZIP contents,
  immutable snapshots, workspace/page serialization, validated forms, reset/stale actions, preferences and visitors.
- Final StudioBrowserTest:22 scenarios, all passing; log `/tmp/shadwick-studio-browser4.log`. Chromium checks all
  four screens in every skin at 1440px light/390px dark, plus demo-settings panels (90 audits); both Chromium and
  Firefox complete creation/editing/task/handoff/ZIP/payment/CSV/reset and independent-visitor/stale-tab journeys.
  Mobile320px checks keyboard sidebar/Escape/focus, invalid modal submission, Nova/Croatian preference persistence;
  native browsing/filtering is checked without JS. 72 full-page screen captures are under the ignored
  `shadwick-examples/target/studio-screenshots/`. Reviewed desktop overview/projects/workspace/invoices and dark
  mobile screenshots. Browser checks wait for saved state and finite animations; no WebKit claim.
- Added docs/guides/clover-studio.md to the searchable catalog:53 guides now. README includes run instructions.
  Landing caption links to local-run instructions; no unavailable public-app URL was invented. Node docs tests
  pass; site build/check passes55 HTML pages,4204 internal references,760 IDs and3918KB. `git diff --check` passes.
- CI browser setup now installs Firefox and its system libraries alongside Chromium. No live Java preview is
  left running; test-owned servers close with their test JVM. The executable examples jar is built.
- Next checkpoint: host Clover Studio on a Java-capable service, configure deployment mode/session lifecycle and
  connect the public URL from landing/docs. Keep the existing GitHub Pages static site. Then agent skill/llms.txt,
  release preparation and Maven Central. Oat/chart variant gaps remain separately scoped.

## Clover Studio English/German and GET search follow-up — 2026-11-06

- User observed that Croatian selection barely changed Studio, then explicitly requested full English/German
  support and no other language options. Studio now normalizes unsupported old/query locales to English and
  offers exactly English/Deutsch. This is Studio scope; the core library's nine translations remain intact.
- Added matching StudioPage.properties and StudioPage_de.utf8.properties. Navigation, headings, forms,
  confirmations, feedback, status badges, milestones, attachments, accessible names, charts, tables and static
  markup use Wicket resource keys. English .properties uses Unicode escapes; German remains UTF-8. Client
  names and project/task content are preserved. Activity stores keys/arguments and is translated when rendered;
  model exceptions expose stable keys for translated errors. Generated handoff documents remain sample data.
- Select a language and submit Apply preferences/Einstellungen anwenden. That native form preserves filters,
  skin and page parameters, replacing explicit locale parameters so switching back to English also works.
- User asked about POST, GET302, GET200 when searching. The old Wicket filter Form submitted a POST,
  redirected to a bookmarkable results URL, then Wicket redirected to its buffered stateful render URL.
  Replaced search with a native GET form, stable q/status/sort names and renderer values plus hidden view.
  Localized labels do not change query tokens. Wicket's default render strategy is unchanged. Packaged-app
  smoke trace confirms only GET302 → GET200, with no POST or application form redirect.
- Full `./mvnw -B test`:751 core +83 example tests =834, passing. Log:
  `/tmp/shadwick-studio-en-de-get-tests.log`. Added real preference-form switching, all screen translations,
  preserved content/progress, unsupported locale fallback, two-choice picker, equal bundle keys and GET markup.
- StudioBrowserTest final:42 passing scenarios. Log `/tmp/shadwick-studio-en-de-get-browser.log`. All four
  screens/settings audited in EN/DE across nine skins at1440px light/390px dark (144 screen captures); real
  language switching/forms/payment persistence in Chromium/Firefox, native no-script German preferences and
  GET search requests, plus existing workflow/reset/session tests. Reviewed German mobile screenshot.
- Docs/README describe the two Studio languages, applying preferences and GET behavior. Static docs tests,
  site build/check and git diff --check pass:55 HTML pages,4204 references,760 IDs,3919KB.
- User's original app is running `shadwick-examples-0.1.0-SNAPSHOT.jar`. Do not stop it or overwrite that JAR.
  Installed current core locally and used a temporary Maven descriptor (removed in finally) to package a new
  `shadwick-examples/target/shadwick-examples-0.1.0-SNAPSHOT-en-de.jar`. Original JAR SHA-256 stayed unchanged.
  Packaging log `/tmp/shadwick-studio-en-de-package.log`. New executable passed German/picker/search smoke
  on a temporary random port; that owned process was stopped. Smoke log `/tmp/shadwick-studio-en-de-smoke.log`.
  User can stop their old process and run the new -en-de.jar. A later normal package rebuilds the standard JAR.
- Changes remain local; no commit, push, deployment or live user-server restart was requested.

## Direct search rendering follow-up — 2026-11-06

- User explicitly requested removing Wicket's remaining redirect from project searches. Added StudioPageMapper,
  mounted at /studio, overriding only processBookmarkable. Normal non-Ajax HTTP GETs with view=projects use
  the original PageProvider with NEVER_REDIRECT. Null page parameters, other views, POSTs, hybrid page URLs
  and component listeners retain normal framework handling; the global REDIRECT_TO_BUFFER setting stays intact.
- Project filtering, initial directory loads, clear filters and reload now return a single GET200 and retain
  the bookmarkable query URL. Callback URLs still carry their own page IDs, so Ajax dialogs and saves work.
- Full `./mvnw -B verify` passes751 core +83 example tests =834. Log:
  `/tmp/shadwick-studio-direct-search-verify.log`. Final focused browser run passes14 cases in Chromium/Firefox:
  eight engine × script/no-script × EN/DE search cases verify fresh-session200/no redirects, one navigation,
  filtered contents, reload, back/forward, clear and Ajax project creation; existing native and connected
  edit/handoff/payment/language journeys pass. API scope checks confirm other views and POST still return302.
  Log `/tmp/shadwick-studio-direct-search-final-browser.log`.
- Packaged executable smoke test confirms GET200, zero redirects and the exact original filtered query URL.
  Temporary random-port server was stopped. Log `/tmp/shadwick-studio-direct-search-smoke.log`.
  No user JAR process was running before packaging. Both standard and -en-de JARs under examples/target now
  contain the fix (the old -en-de artifact was refreshed only after checking it was not running).
- Docs now explain the scoped direct renderer. Docs tests/build/check and git diff --check pass:
  55 pages,4204 references,760 IDs,3920KB. No additional skin/visual changes or dependencies were introduced;
  this focused run follows the prior42-case bilingual/nine-skin browser suite. Changes remain local/uncommitted.


## Studio Ajax composition handoff — 2026-11-06

- User reported redirects/full-page refresh after task completion and asked whether Shadwick or Studio was
  responsible. Root cause: Studio callbacks explicitly called setResponsePage after receiving Ajax targets,
  and its constructor captured display snapshots. SwButton/SwDialog/SwDataTable already support partial Ajax.
- Studio now uses live models and app-local ViewState behaviors. A partial refresh compares rendered values,
  adds only changed components and avoids descendant targets covered by a parent. Task50→75% updates exactly
  five components: the affected row, native progress, percentage, completed count and feedback. Unchanged
  rows, shell/header/sidebar, summary container, budget, files, milestones and handoff container retain DOM.
  Reopen and100%/handoff eligibility update dependent controls. Task collection additions replace that list.
- Project create/edit, add/complete/reopen tasks, handoff, payment and reset all stay in the same document.
  Creation/reset intentionally change the view using Ajax. Payments update the current project/register.
  Status-sorted tables rerender only when row order changes; otherwise only badge/totals change. Busy guards
  prevent repeated UI submissions. Only native/no-script preference submission still calls setResponsePage.
- Internal links/sidebar and browser history use Ajax with real native href fallbacks. Different views update
  content/header/active-menu state and title, preserving the shell. Revisiting the current view preserves its
  widgets. Heading receives focus after view navigation. Filters/clear and repeated searches update current
  results and bookmarkable history; submitted input nodes retain identity. Native no-script search stays GET200.
- StudioPageMapper directly renders all normal bookmarkable Studio GETs, not only searches; component/hybrid,
  Ajax and POST handling and the application's global request strategy remain unchanged. Native file downloads
  remain request-based, generation-checked and respond409 for stale sessions rather than redirecting.
- Preferences use AjaxFallbackButton and preserve invoice sorting, page size and column choices. Shadwick
  intentionally pins ordinary Ajax CSS to the page's original skin. Studio's explicit global switch loads
  the selected public CSS URL with the document nonce, activates after load and keeps older links disabled
  for switching back. EN/DE remains the only Studio language choice; core language bundles are unchanged.
- Added dialog CommandContext tokens, consumed on success/close/replacement, and stable task-project targets.
  They reject stale dialog instances and old task callback contexts after view changes. Generation/revision/
  expected-task-state safeguards remain. UI validation stays inside the dialog and preserves the current page.
- Final full JVM verification (`./mvnw -B test`):751 core +87 examples =838 passing. Log:
  `/tmp/shadwick-studio-ajax-verify.log`. Unit checks assert the exact five task component IDs, same Page instance,
  no redirect XML, current-project payment patches, wrong project callback rejection and stale invoice contexts.
- Full Studio browser suite:59 passing scenarios; log `/tmp/shadwick-studio-ajax-complete-browser.log`. Includes
  all four views/settings across nine skins, EN/DE,1440px light/390px dark (144 screen captures), Chromium/Firefox
  tasks/payment DOM identity and zero document navigation, eight engine/script/locale search/history cases,
  repeated skin switching with preserved table state, current-view preservation, sidebar/history and successive
  searches. After adding context guards,16 focused command/navigation cases passed in
  `/tmp/shadwick-studio-ajax-context-browser.log`; both single-request checks passed in
  `/tmp/shadwick-studio-ajax-request-count.log`. Final close-token lifecycle is covered by JVM and packaged smoke.
- Packaged with a temporary Maven descriptor (removed in finally) as
  `shadwick-examples/target/shadwick-examples-0.1.0-SNAPSHOT-ajax.jar`; original JARs untouched. Build log:
  `/tmp/shadwick-studio-ajax-package.log`. Installed the matching current core locally before packaging.
  Packaged app passed a Chromium smoke: exactly one task AjaxPOST, preserved nodes, payment, sidebar navigation,
  search and German/Nova preferences, with zero document navigations or console errors. Temporary random-port
  server was stopped. `/tmp/shadwick-studio-ajax-smoke.log`, `/tmp/shadwick-studio-ajax-packaged.png`.
- README/guide describe the Ajax composition, native fallbacks, request behavior and concurrency boundaries.
  Docs tests/site build/check and git diff --check pass. Changes remain local/uncommitted. Next roadmap checkpoint
  is hosting the Java app alongside GitHub Pages, then agent guidance/llms.txt and release/Maven Central.

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
the general-purpose toast API are implemented. App shell is next.

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

Next phase: app shell. Start with site header, breadcrumb and skip link, then sidebar,
user menu, layouts and per-user themes.

## Later phases (rough order)

5. App shell: sidebar, site header, user menu, breadcrumb, skip link,
   theme toggle, app layouts; per-user themes with theme store + switcher.
6. Data: data table (columns, sorting, paging, CRUD — follow Wicket Oat's
   table API), pagination.
7. Advanced: custom select, combobox (server-side search), calendar,
   date picker, month view, charts.

Then: remaining skins, translations, agent skill + llms.txt, Maven Central
release, docs/showcase site.

## Working agreements

- Verify current versions (Wicket, Tailwind, Maven plugins, Node LTS) on
  Maven Central / npm before using them.
- Ask before adding any dependency to the library module.
- Use plan mode before each new component group; small, focused commits.
- For every component: read the Wicket Oat equivalent (API + tests) and the
  Shadleaf equivalent (markup + CSS) before writing code.
- Record every decision under Conventions as soon as it is made.

## Conventions

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
  build copies the actual minified embedded Vega/Lyra bundles into ignored `site/dist/`.
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

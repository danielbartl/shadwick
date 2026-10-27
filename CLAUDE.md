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

### Open decision (settle in the Phase 1 plan)

- Java class prefix replacing `Oat` (e.g. `Sw` → `SwButton`, `SwDialog`,
  `SwTextField`). Must avoid clashes with Wicket core names (Button, Label,
  Link, Form, TextField, …). CSS classes use `sw-`.

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
├── shadwick           – the library (published)
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
  Proposed structure: `components/*.css` hold the base (vega) rules,
  `skins/<skin>.css` hold only the deltas. Check how Shadleaf structures its
  skins first; adopt the better approach and record it under Conventions.
- Bundle variants:
  - `standalone` (default, Phase 1): Tailwind preflight + page base styles
    (background/foreground/border from tokens) + tokens + components.
  - `embedded` (later): tokens + components only — no preflight, no Tailwind
    theme variables (values compiled inline) — for apps running their own
    Tailwind build. Research the correct Tailwind v4 mechanism when we get there.

Source layout (library module, proposal):

```
src/main/tailwind/
  tokens.css        – :root/.dark tokens + @theme inline mapping (bg-primary → var(--primary) …)
  base.css          – page base styles (standalone only)
  components/       – one file per component (base rules)
  skins/            – per-skin deltas
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

## Phase 1 — vertical slice (CURRENT)

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

## Later phases (rough order)

2. Embedded bundle; display components: card, badge, alert, avatar, separator,
   kbd, skeleton, spinner, empty state, icon; feedback panel + feedback toasts.
3. Forms: the remaining field types (follow Wicket Oat's list), checkbox,
   radio group, switch, toggle, toggle group, input group, file upload.
4. Overlays: confirm/alert dialog, sheet (≈ `OatDrawer`), popover,
   dropdown menu, tooltip, toast.
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

_Claude: fill in as decisions are made — class prefix, naming, file layout,
CSS structure, skin structure, JS patterns, test patterns._

# Shadwick

shadcn/ui-style UI components for [Apache Wicket](https://wicket.apache.org): shadcn's look, tokens and component
catalog, rendered on the server by Wicket. Add one Maven dependency and you're done: no Node, no Tailwind, no CSS
build in your application.

Shadwick is inspired by [Shadleaf](https://github.com/wimdeblauwe/shadleaf), shadcn/ui for Thymeleaf, and adapts its
markup and CSS. It is not affiliated with or endorsed by Shadleaf or shadcn/ui. Its Java API follows
[Wicket Oat](https://github.com/danielbartl/wicket-oat): if you know Wicket Oat, moving to Shadwick is mostly
renaming `Oat` to `Sw`.

> **Status: Phase 2.** Buttons, text fields, dialogs, display components and feedback, in two skins (vega, lyra),
> light and dark. Standalone and embedded CSS bundles. Not yet on Maven Central.

## Getting started

Requires Wicket 10 and Java 17. Until the first release, install it locally with `./mvnw install`, then:

```xml
<dependency>
    <groupId>dev.jbaby</groupId>
    <artifactId>shadwick</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Install it in your application's `init()`:

```java
@Override
protected void init() {
    super.init();
    Shadwick.install(this);                    // vega, shadcn's classic look
    // Shadwick.install(this).setSkin(Skin.LYRA); // boxy, small type, square corners
}
```

That adds Shadwick's stylesheet to every page. It works under Wicket's default strict, nonce-based Content Security
Policy and adds nothing to it.

### Buttons

```java
// A button that runs over Ajax
add(SwButton.onClick("save", "Save", target -> save()));
add(SwButton.onClick("delete", "Delete", target -> delete())
        .setVariant(ButtonBehavior.Variant.DESTRUCTIVE));

// Any link or button styled as a shadcn button
add(new BookmarkablePageLink<>("home", HomePage.class)
        .add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE).setSize(ButtonBehavior.Size.SMALL)));
```

```html
<button wicket:id="save"></button>
<a wicket:id="home">Home</a>
```

Variants: `DEFAULT`, `SECONDARY`, `OUTLINE`, `GHOST`, `DESTRUCTIVE`, `LINK`. Sizes: `DEFAULT`, `SMALL`, `LARGE`,
`ICON`. An icon-only button needs an `aria-label`: in development mode, a button without an accessible name fails
to render with a message saying how to fix it, and in deployment it is logged.

### Form fields

A field wraps a label, the input, an optional description and the inline error, and wires `aria-describedby` and
`aria-invalid` for you. It works with `CompoundPropertyModel` and takes its label from your `.properties` files:

```java
Form<Profile> form = new Form<>("form", new CompoundPropertyModel<>(profile));
form.add(new SwTextField<String>("name").setRequired(true));
form.add(new SwTextField<String>("email").addValidator(EmailAddressValidator.getInstance())
        .setDescription("We never share it."));
form.add(new SwTextArea<String>("bio"));
form.add(SwSubmitButton.onSubmit("save", "Save", target -> service.save(profile)));
```

```html
<form wicket:id="form">
    <div wicket:id="name"></div>
    <div wicket:id="email"></div>
    <div wicket:id="bio"></div>
    <button wicket:id="save"></button>
</form>
```

After an Ajax submit, the fields show their errors themselves: no `target.add(...)` needed, and the input keeps its
focus and what the user typed.

### Dialog

A modal on the native `<dialog>` element, with the same API as Wicket Oat's `OatDialog`:

```java
SwDialog dialog = new SwDialog("delete", "Delete the project?") {
    @Override
    protected void onConfirm(AjaxRequestTarget target) {
        projects.delete(project);
    }
};
dialog.setDescription("This cannot be undone.");
dialog.setConfirmLabel("Delete").setConfirmVariant(ButtonBehavior.Variant.DESTRUCTIVE);
add(dialog);
add(SwButton.onClick("openDelete", "Delete project", dialog::open));
```

Give it content with `setContent(...)` (id `SwDialog.CONTENT_ID`); its form fields are validated on Confirm, and it
stays open while one has an error. `setTrigger("About")` adds a button that opens it in the browser, without a
request.

### Theming

Shadwick uses shadcn/ui's CSS variables (`--background`, `--primary`, `--radius`, …), so any shadcn theme, from the
[theme generator](https://ui.shadcn.com/themes) or [tweakcn](https://tweakcn.com), drops in unchanged: redefine the
variables in your own stylesheet. Dark mode is `class="dark"` on `<html>`.

If your application supplies its own reset and page styles (for example, its own Tailwind build), use the embedded
bundle. It includes the same tokens and components, with no preflight, page base styles or Tailwind theme variables:

```java
Shadwick.install(this).setSkin(Skin.LYRA).setBundle(Bundle.EMBEDDED);
```

Configure the bundle at application startup. The default is `Bundle.STANDALONE`.

### Display components

```java
add(new SwCard("card").setSize(CardBehavior.Size.SMALL));
add(new SwBadge("badge", "New", BadgeBehavior.Variant.SECONDARY));
add(new SwAlert("alert", "The project could not be saved.", AlertBehavior.Variant.DESTRUCTIVE));
add(new SwAvatar("avatar", "AL").setLabel("Ada Lovelace"));
add(new SwSeparator("separator"));
add(new SwKbd("key", "K"));
add(new SwSkeleton("placeholder", SkeletonBehavior.Shape.CIRCLE));
add(new SwSpinner("loading").setLabel("Loading projects"));
add(new SwIcon("icon", "search").setLabel("Search"));
add(new SwEmptyState("empty", "No projects yet", "Create one to get started.").setIcon("folder"));
```

Cards keep your markup: use `sw-card-header`, `sw-card-title`, `sw-card-description`, `sw-card-content` and
`sw-card-footer` for their parts. `Sw.Behaviors.card()`, `.badge()`, `.alert()`, `.separator()`, `.kbd()`,
`.skeleton()` and `.spinner()` style existing Wicket components. A composed alert uses `sw-alert-title` and
`sw-alert-description`; `SwAlert` is a plain, escaped message.

Avatars accept a picture URL model, optional initials model and size, as in Wicket Oat. Initials show while the
picture loads or if it fails, including after Ajax replacement. Icons are bundled Lucide SVGs; `SwIcon.names()` lists
them, `setLabel(...)` names a standalone icon, and an unnamed icon is decorative. Bind icons and spinners to `<svg>`
or a wrapper tag, keys to `<kbd>`, and other display components to a container. Skeletons are decorative; put an
accessible loading status on their surrounding region. Separators are decorative unless `.setDecorative(false)`.

### Feedback

```java
// A panel for this form's messages; updates and clears itself during Ajax requests.
form.add(new SwFeedbackPanel("feedback", form));

// On a page: messages outside fenced panels, including session messages after navigation.
add(Sw.Behaviors.feedbackToasts());
```

Both skip the first field error already shown inline. Panels render alerts; errors use the destructive variant,
other levels use the default. Toasts preserve the feedback level, render message text safely and remain until
dismissed. Panel filters and fences work as with Wicket's `FencedFeedbackPanel`; a fenced panel reserves its
container's messages even while hidden. Set up the fences when constructing the page.

## Building

```sh
./mvnw clean verify
```

No Node needed: the build installs a pinned Node LTS into `target/` and compiles the CSS with Tailwind CSS v4. The
jar ships only the finished CSS.

Run the showcase, with Buttons, Form, Dialog, Display and Feedback pages, and a skin and light/dark switch on every page:

```sh
./mvnw install -DskipTests
java -jar shadwick-examples/target/shadwick-examples-0.1.0-SNAPSHOT.jar
```

Then open http://localhost:8080. While working on the CSS, `npm run watch:css` in `shadwick-core` (with a local
Node) rebuilds the bundles into `target/classes` on every change; reload the page to see it.

The browser checks (Playwright and axe-core, every page in both skins, light and dark) run with:

```sh
./mvnw verify -pl shadwick-examples -am -Pbrowser
```

## Landing page

The static website lives in [`site/`](site/README.md), with an interactive component studio and a GitHub Pages
deployment workflow. See the site README for local preview and publishing instructions.

## License

Apache License 2.0. See [LICENSE](LICENSE), and [NOTICE](NOTICE) for the projects Shadwick adapts code from
(Shadleaf, shadcn/ui, Basecoat, Lucide).

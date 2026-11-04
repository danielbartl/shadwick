<img src="site/assets/logo.svg" alt="Shadwick four-leaf clover" width="56" height="56">

# Shadwick

shadcn/ui-style UI components for [Apache Wicket](https://wicket.apache.org): shadcn's look, tokens and component
catalog, rendered on the server by Wicket. Add one Maven dependency and you're done: no Node, no Tailwind, no CSS
build in your application.

Shadwick is inspired by [Shadleaf](https://github.com/wimdeblauwe/shadleaf), shadcn/ui for Thymeleaf, and adapts its
markup and CSS. It is not affiliated with or endorsed by Shadleaf or shadcn/ui. Its Java API follows
[Wicket Oat](https://github.com/danielbartl/wicket-oat): if you know Wicket Oat, moving to Shadwick is mostly
renaming `Oat` to `Sw`.

> **Status: Forms and overlays complete; app shell in progress.** Buttons, native input fields, choices, toggles, input groups, dialogs,
> sheets, popovers, tooltips, dropdown menus, toasts, display components and feedback, in two skins (vega, lyra), light and dark. Standalone and embedded CSS bundles.
> Not yet on Maven Central.

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
form.add(new SwEmailField("email").setDescription("We never share it."));
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

### Everyday inputs

`SwEmailField`, `SwPasswordField`, `SwSearchField`, `SwTelField`, `SwUrlField` and
`SwNumberField<N>` share the same field wrapper, models and inline Ajax feedback:

```java
form.add(new SwEmailField("email").setRequired(true));
form.add(new SwPasswordField("password"));
form.add(new SwSearchField("query").setPlaceholder("Search projects"));
form.add(new SwTelField("telephone"));
form.add(new SwUrlField("website"));
form.add(new SwNumberField<Integer>("seats").setMin(1).setMax(10).setStep(1));
```

This example inherits models from a parent `CompoundPropertyModel`. Bind each field to a `<div wicket:id="...">`.
Email and URL use Wicket's server validators. Search and telephone accept arbitrary text; add application validators
when you need stricter rules. Password fields are required by default, clear their model on detach and never echo
values into HTML, following Wicket's `PasswordTextField`. Use `.setRequired(false)` for an optional password.

Number fields support integer and decimal types (including `BigDecimal`), min/max/step values or models, and native
HTML number formatting independent of the session locale. For an empty plain model, provide `.setType(Integer.class)`
(or the appropriate number class); compound models know their property type. Min/max are enforced by Wicket on the
server. **Step is a browser hint**, not a server validator: add a validator if alignment to the step is a domain rule.
A zero step renders Wicket's `step="any"` convention. Access the native component with `getInput()` for further options.

Native email, URL and number controls may perform browser validation before a submit. The showcase's **Inputs** page
uses `novalidate` on its forms so it can demonstrate Wicket's inline server errors, normal submission and Ajax focus
preservation. After an invalid Ajax submit the password stays in the existing browser input without being sent back
in the response; a full page render leaves it blank.

### Native selections

`SwDropdownChoice<T>` wraps Wicket's native `DropDownChoice<T>`. `SwListMultipleChoice<T>` is a native
`<select multiple>` list box, and `SwCheckBoxMultipleChoice<T>` presents a collection as labelled checkboxes
inside a fieldset. All three accept a choices model and an optional `IChoiceRenderer`, use the standard field
wrapper, and inherit selected models and `.properties` labels by id:

```java
var teams = Model.ofList(List.of("Design", "Engineering", "Support"));
form.add(new SwDropdownChoice<>("team", teams).setRequired(true));
form.add(new SwListMultipleChoice<>("teams", teams).setMaxRows(3).setRequired(true));
form.add(new SwCheckBoxMultipleChoice<>("channels", Model.ofList(List.of("Email", "SMS", "In-app")))
        .setInline(true).setRequired(true));
```

Bind each field to a `<div wicket:id="...">`. The dropdown property holds one choice; multi-selection properties
hold a collection, such as a mutable `List<T>`. Wicket updates existing collections in place. A required
multi-selection needs at least one valid choice; unrecognised submitted IDs cannot satisfy that requirement.
Optional multi-selections can be cleared. Use `dropdown.getInput().setNullValid(true)` to keep a selectable null
option in an optional dropdown. In page properties, `input.nullValid` supplies its text and `input.null`
labels the initial prompt, following Wicket's resource lookup for the wrapped control named `input`.

Use `AjaxFormComponentUpdatingBehavior("change")` for either native select and
`AjaxFormChoiceComponentUpdatingBehavior` for the checkbox group. Pass these to `.add(...)` to attach them to the
wrapped control. Inline feedback updates without replacing the select, checkbox nodes, current selection or focus.
The **Selections** showcase (`/selections`) demonstrates normal and Ajax submits, immediate updates, a custom
renderer with stable IDs, inline and stacked checkbox groups, and disabled selections. Multi-select keyboard
interaction follows the browser and operating system; checkbox groups provide an alternative without modifier keys.

### Native dates and times

All five fields reuse the standard label, description and inline Ajax feedback:

| Component | Model type | HTML value format |
|---|---|---|
| `SwDateField` | `LocalDate` | `2026-12-24` |
| `SwTimeField` | `LocalTime` | `14:05` or `14:05:30.125` |
| `SwDateTimeLocalField` | `LocalDateTime` | `2026-12-24T14:05` |
| `SwMonthField` | `String` | `2026-12` |
| `SwWeekField` | `String` | `2026-W52` (ISO week-year) |

```java
form.add(new SwDateField("date").setRequired(true));
form.add(new SwTimeField("time"));
form.add(new SwDateTimeLocalField("appointment"));
form.add(new SwMonthField("month"));
form.add(new SwWeekField("week"));
```

These inherit selected models from a parent `CompoundPropertyModel`; bind each to a `<div wicket:id="...">`.
The browser owns the picker and localized display. Values use [HTML's fixed date/time formats](https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#dates-and-times)
regardless of the Wicket session locale or application-wide converters. Server conversion rejects malformed values,
impossible dates and invalid month/week values, including week 53 in a 52-week ISO year. Optional blank fields set
the model to `null`. HTML dates require positive years within Java's supported date range.

`LocalDateTime` is a local wall-clock value: it carries no timezone and Shadwick performs no UTC conversion.
Time values omit zero seconds and truncate model nanoseconds to milliseconds when rendered. For seconds use
`.addToInput(AttributeModifier.replace("step", "1"))`; for milliseconds use `"0.001"`. Without this, the browser's
default step is one minute. The underlying model retains its original precision until the form updates it.

HTML `min`, `max` and `step` attributes are browser hints on these text fields. Add Wicket validators for server-side
bounds or scheduling rules, for example:

```java
LocalDate first = LocalDate.of(2026, 1, 1);
LocalDate last = LocalDate.of(2026, 12, 31);
form.add(new SwDateField("date").setRequired(true)
        .addValidator(RangeValidator.range(first, last))
        .addToInput(AttributeModifier.replace("min", first.toString()),
                AttributeModifier.replace("max", last.toString())));
```

Use `AjaxFormComponentUpdatingBehavior("change")` for immediate validation. The **Dates & time** showcase (`/dates`)
includes all five types, normal/Ajax submits, date bounds, an optional live date, disabled state and millisecond input.
Picker availability depends on the browser and operating system; server conversion still applies. Custom calendar
and date-picker components remain on the later roadmap.

### Toggles and input groups

`SwToggle` is a Boolean checkbox drawn as a toggle button. `SwToggleChoice<T>` is a single-selection radio group;
`SwToggleMultipleChoice<T>` binds a collection of selected values. Their labels, descriptions and errors use the same
field API as the other controls:

```java
form.add(new SwToggle("highlight").setIcon("pencil").setIconOnly(true)
        .setVariant(ToggleBehavior.Variant.OUTLINE));
form.add(new SwToggleChoice<>("alignment", Model.ofList(List.of("Left", "Center", "Right")))
        .setRequired(true).setVariant(ToggleBehavior.Variant.OUTLINE).setSpacing(0));
form.add(new SwToggleMultipleChoice<>("formats", Model.ofList(List.of("Bold", "Italic", "Underline")))
        .setSize(ToggleBehavior.Size.SMALL));
```

Each binds to a `<div wicket:id="...">` and inherits its value and resource label by id. Toggles expose native checked
state: Space changes a checkbox, arrow keys move through a single-selection group, and clicking an already selected
radio keeps it selected. A Boolean toggle's `setRequired(true)` follows Wicket's checkbox semantics and allows `false`;
use a validator when `true` is required. Required groups need at least one valid choice. Group constructors support
explicit models and `IChoiceRenderer`, like `SwRadioChoice` and `SwCheckBoxMultipleChoice`; renderer labels stay escaped
text. An icon-only toggle keeps its text caption as the accessible name.

Variants are `DEFAULT` and `OUTLINE`; sizes are `DEFAULT`, `SMALL` and `LARGE`. Group `.setSpacing(0)` joins items;
1–4 select a gap (default 2). `.setOrientation(ToggleGroupBehavior.Orientation.VERTICAL)` stacks them. Use
`AjaxFormComponentUpdatingBehavior("change")` for `SwToggle`, and `AjaxFormChoiceComponentUpdatingBehavior` for either
group. Passing these to `.add(...)` attaches them to the underlying inputs. The browser owns selection and focus;
feedback updates in place.

Text-like fields now offer `.setPrefix(...)` and `.setSuffix(...)` for text or `IModel<String>` addons:

```java
form.add(new SwTextField<String>("workspace").setPrefix("https://").setSuffix(".example"));
form.add(new SwNumberField<Integer>("weight").setMin(1).setMax(100).setSuffix("kg"));
form.add(new SwTextArea<String>("notes").setPrefix("Draft").setSuffix("Markdown supported"));
form.add(new SwTextField<String>("project").setSuggestions(List.of("Shadwick", "Atlas", "Nimbus")));
```

A null addon model or model value hides it. The group wrapper appears only when an addon is present. Prefixes and
suffixes describe the input without changing its name or submitted value, remain escaped, and keep the same
`field:input` path. Textarea addons sit above/below the control. Datalist suggestions are native browser hints on
single-line inputs, with browser-dependent availability; they do not restrict submitted values. Add validators to
limit allowed values. Prefix/suffix and suggestion models are detached with the field.

For custom markup with icons or action buttons, compose the `Sw.Behaviors` helpers:

```java
group.add(Sw.Behaviors.inputGroup());
input.add(Sw.Behaviors.inputGroupInput());       // or inputGroupTextArea()
addon.add(Sw.Behaviors.inputGroupAddon()
        .setAlign(InputGroupAddonBehavior.Align.INLINE_END));
button.add(Sw.Behaviors.inputGroupButton());    // small ghost button by default
```

```html
<label for="search-input">Search library</label>
<div wicket:id="group" aria-label="Search tools">
    <input wicket:id="input" id="search-input" />
    <div wicket:id="addon"><button wicket:id="button" type="button">Clear</button></div>
</div>
```

Keep the control before addons in the DOM so it comes first in the tab order; CSS places `INLINE_START`, `INLINE_END`,
`BLOCK_START` and `BLOCK_END` addons. `.setBorder(true)` separates a block addon from its control.
`Sw.Behaviors.inputGroupText()` styles addon text. Group buttons use `ButtonBehavior.Variant` and `Size.SMALL` or
`Size.ICON`; name icon-only buttons. Use the behavior on a plain Wicket button or link; `SwButton` already has a
button behavior. Clicking noninteractive addon space focuses the control, including after Ajax replacement; buttons
and links keep their own actions. Rich groups should have a separate group name, as in the example above.

The **Groups** showcase (`/groups`) demonstrates normal and Ajax submissions, immediate keyboard-driven formatting,
joined and vertical groups, icon-only and disabled toggles, validation, datalists, textareas, and a custom group with
Clear and Ajax replacement actions.

### File upload

`SwFileUpload` is a native file input; `SwFileDropzone` adds Shadleaf's dropzone appearance, a selected-file list,
removal buttons and announcements. Both bind to `IModel<List<FileUpload>>` or an inherited `List<FileUpload>` property,
use the regular field label/description/error API, and default to multiple selection:

```java
Form<Documents> form = new Form<>("form", new CompoundPropertyModel<>(documents));
form.setMultiPart(true);
form.setMaxSize(Bytes.megabytes(10));       // whole multipart request, including overhead
form.setFileMaxSize(Bytes.megabytes(5));    // parser limit for each file
form.add(new SwFileDropzone("files").setRequired(true).setAccept(".pdf,.txt")
        .setMaxFiles(3).setMaxFileSize(Bytes.megabytes(2))
        .setDescription("Up to three documents, 2 MB each."));
```

```html
<form wicket:id="form" method="post">
    <div wicket:id="files"></div>
    <button wicket:id="save" type="submit">Upload</button>
</form>
```

Add a normal Wicket submit button or `SwSubmitButton` to process the form. **Read or persist every `FileUpload` inside
the submit callback.** Wicket closes upload streams and clears the upload model at request detach; do not retain
`FileUpload` objects in page/session state or asynchronous work. The library does not store files automatically.
Use `getInputStream()` for streaming application processing; use application storage keys rather than treating a
client filename as a destination path.

`setMultiple(false)` or `setMaxFiles(1)` renders a single picker and enforces that limit server-side.
`setMaxFiles(...)` and `setMaxFileSize(Bytes)` validate the submitted files, including requests that bypass the browser.
A named zero-byte file counts as a selection. Parser-level failures occur before field validation: keep form request
limits in place and show a `SwFeedbackPanel` for form-level errors. In an Ajax form, add that panel to the target in
the error callback; inline field errors update themselves. `setAccept(...)` is a picker/drop hint, not server-side
content validation—add application validators appropriate to the files you process.

With the dropzone, repeated picks and drops append files, matching name/size/modification-time duplicates are ignored,
and files can be removed before submission. Files rejected by its type/count/size hints are listed separately and
excluded from the native `FileList`. Native selection and keyboard access still work without JavaScript. If the browser
lacks the `DataTransfer` support needed for enhancement, selection uses the native picker and removal buttons stay disabled.
The component enhances selection; transport progress, image previews and automatic submission are outside this API.

After an invalid Ajax submit, leave the upload component out of `target.add(...)` to keep the selection and focus.
The browser sends those files again on the next submit. After success, clear the browser selection explicitly:

```java
SwFileDropzone files = new SwFileDropzone("files", "Documents", Model.ofList(new ArrayList<>()));
form.add(files);
form.add(SwSubmitButton.onSubmit("save", "Upload", target -> {
    service.process(files.getModelObject()); // consume files during this request
    files.clearSelection(target);           // no extra change event or submit
}));
```

A full page render clears the selection, as native file inputs cannot be populated from the server. The **Uploads**
showcase (`/uploads`) covers native and Ajax submissions, a single-file picker, server count/size/type validation,
request limits, drag/drop/removal and a disabled dropzone. Its receipts keep only escaped filenames and byte counts;
file content is discarded. The landing-page reference-file demo selects a local file without uploading it.

### Checkboxes, switches and radio groups

These use the same field wrapper, model binding, `.properties` labels and inline feedback as text fields:

```java
SwCheckBox accepted = new SwCheckBox("accepted", "Accept the terms", Model.of(false))
        .setDescription("Please read the terms before continuing.");
form.add(accepted);
form.add(new SwSwitch("notifications", "Email notifications", Model.of(true))
        .setSize(SwitchBehavior.Size.SMALL));
form.add(new SwRadioChoice<>("contact", "Contact preference", Model.of("Email"),
        Model.ofList(List.of("Email", "SMS", "Phone")))
        .setRequired(true).setInline(true));
```

Bind each to a `<div wicket:id="..."></div>`, as with text fields. `SwRadioChoice<T>` wraps Wicket's
`RadioChoice<T>` with a fieldset, legend, individually labelled native radios and an optional `IChoiceRenderer`.
Choices stack by default; `.setInline(true)` puts them in a wrapping row. Help text and validation errors describe
both the group and each radio. The checkbox and switch remain native Boolean controls, including keyboard and
disabled behavior; `Sw.Behaviors.swSwitch()` styles an existing Wicket `CheckBox` as a switch.

As with Wicket's `CheckBox`, unchecked means `false`, and `setRequired(true)` does **not** require it to be checked.
For consent or another rule that requires `true`, add a validator:

```java
accepted.addValidator(value -> {
    if (!Boolean.TRUE.equals(value.getValue())) {
        value.error(new ValidationError().setMessage("Accept the terms to continue."));
    }
});
```

Use `AjaxFormComponentUpdatingBehavior("change")` on a checkbox or switch, and
`AjaxFormChoiceComponentUpdatingBehavior` on a radio group. Passing them to the field's `.add(...)` routes them to
the underlying input. Plain and Ajax form submits work too; field errors update in place without resetting focus
or the current selection. The showcase's **Choices** page demonstrates all of these paths.

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

### Confirmation dialog

`SwConfirmDialog` follows Wicket Oat's reusable `ask(...)` API. Add one to the page, then ask it from any Ajax callback:

```java
SwConfirmDialog confirm = new SwConfirmDialog("confirm").setConfirmLabel("Delete");
add(confirm);
add(SwButton.onClick("delete", "Delete invoice", target ->
        confirm.ask(target, "Delete invoice?", "This cannot be undone.", t -> {
            invoices.delete(invoice);
            t.add(table);
        })));
```

Render it on `<div wicket:id="confirm"></div>`. The native dialog has `role="alertdialog"`, takes its accessible name
and description from the question, and focuses Cancel initially. Escape and Cancel dismiss it; clicking the backdrop
leaves it open. There is no corner close button by default. The confirm button defaults to `DESTRUCTIVE`; use
`setConfirmVariant(...)` and `setConfirmLabel(...)` for other actions.

The callback stays on the server, is consumed before it runs, and is forgotten when the dialog closes. Asking another
question replaces the pending callback. The callback may call `ask(...)` again to show a follow-up question without
closing. An ACTIVE Ajax channel ignores repeated confirm clicks while a request is in flight. Titles and descriptions
also accept `IModel<String>`. Subclasses overriding `onClose` must call `super.onClose(target)` to forget the callback.

### Sheet

`SwSheet` is a native modal panel with `SwDialog`'s content, validation and open/close API, following OatDrawer's role:

```java
SwSheet settings = new SwSheet("settings", "Project settings") {
    @Override
    protected void onConfirm(AjaxRequestTarget target) {
        service.save(project); // content fields have validated and updated their models
    }
};
settings.setDescription("Edit the project, then save your changes.")
        .setContent(new SettingsPanel(SwSheet.CONTENT_ID, project))
        .setConfirmLabel("Save")
        .setTrigger("Edit project")
        .setSide(SwSheet.Side.END);
add(settings);
```

Render it on `<div wicket:id="settings"></div>`. `Side.END` is the default; `START` and `END` follow text direction,
while `TOP` and `BOTTOM` span the viewport width. The header and footer stay visible while the body scrolls. The
corner close button, Cancel, Escape and backdrop dismiss it. `setFooterVisible(false)` supports informational sheets.

Open it from any Ajax handler with `open(target)`, or use `open(content, target)` for temporary content removed on
close. Re-rendering an open sheet keeps it modal. Invalid fields update in place and keep focus; unconfirmed browser
input is cleared when it closes. Native modality handles the inert background and focus return. The **Overlays**
showcase (`/overlays`) covers confirmations, follow-up questions, nested overlays and all four sheet edges. The landing
page previews a confirmation and an end sheet as browser-only demos.

### Popover

`SwPopover` follows OatPopover's content-factory API and renders a native, non-modal popover:

```java
SwPopover filters = new SwPopover("filters", "Filters", id -> new FilterPanel(id, filter))
        .setTitle("Filter projects")
        .setDescription("Choose which projects to show.")
        .setSide(SwPopover.Side.BOTTOM)
        .setAlign(SwPopover.Align.START);
add(filters); // <div wicket:id="filters"></div>
```

The factory receives `SwPopover.CONTENT_ID`. `getTrigger()` exposes the native button for attributes or behaviors;
`setTriggerVariant(...)` changes its default `OUTLINE` appearance. `setContent(...)` can replace the content later.
The optional title names the panel; without it, the trigger label supplies its accessible name. Titles, descriptions
and trigger labels accept models.

The button opens it without a request. Focus moves to its first control (or the panel for informational content),
and Escape, outside clicks or tabbing out dismiss it. The page stays interactive. A nested popover's Escape closes
that panel and returns focus to its trigger. `Side.START`/`END` follow text direction; `TOP`/`BOTTOM` and
`Align.START`/`CENTER`/`END` control placement. CSS anchors position and flip the panel without inline styles. Older
Popover API browsers without CSS anchor positioning show a viewport panel instead.

Re-render `getContent()` to update an open popover; replacing the entire `SwPopover` resets it to closed. A content
panel may own a Wicket `Form` and `SwSubmitButton`, including normal inline field feedback. After applying changes,
call `filters.close(target)` to dismiss it and return focus when focus was inside it. Open state stays in the browser.

### Tooltip

Add `TooltipBehavior` to an existing control, or use the facade:

```java
button.add(Sw.Behaviors.tooltip("Download a CSV copy."));
field.addToInput(new TooltipBehavior(Model.of("Use the public project name."))
        .setPlacement(TooltipBehavior.Placement.BOTTOM)
        .setDelay(Duration.ofMillis(300)));
```

The API retains Oat's `Placement.TOP`/`BOTTOM`/`LEFT`/`RIGHT`; left/right follow the inline start/end in RTL.
Tooltips contain escaped plain text and supplement an existing accessible name. Attach them to the actual focusable
component, such as a field's input. `Mode.DESCRIPTION` preserves existing `aria-describedby` help/error references;
`Mode.LABEL` visually repeats an existing name without adding a duplicate description, for example on an icon button
with its own `aria-label`. Null or blank labels produce no tooltip.

Keyboard focus shows a tooltip immediately; pointer hover uses a 300 ms default delay. It stays while the pointer is
over the trigger or tooltip, has no interactive content, and Escape dismisses it without closing a surrounding
popover or dialog. Touch does not show hover hints. Model changes and Ajax replacement refresh the text and remove
stale tooltip elements. Without JavaScript, the native `title` remains available as a fallback. Keep essential help
in visible text or an interactive popover.

The **Popovers & tooltips** showcase (`/popups`) covers forms, nested panels, reactive hints, label mode, touch and
positioning. The landing page uses the same library script for its Java badge popover and preview-control hints.

### Dropdown menus

`SwDropdown<T>` follows OatDropdown's action-menu API. It uses native buttons in a non-modal popover:

```java
add(SwDropdown.of("actions", "Export as", Model.ofList(List.of("CSV", "JSON", "XML")),
        format -> format,
        (target, format) -> export(format)));
```

Bind it to `<div wicket:id="actions"></div>`. The action factory invokes the Ajax callback and closes the menu;
`getTrigger()` and `getMenu()` expose its parts. An empty or null list disables the trigger. Replacing the whole
component over Ajax resets it to closed. `setSide(DropdownMenuBehavior.Side.END)` and
`setAlign(DropdownMenuBehavior.Align.END)` change placement; start/end follow the text direction.

For custom rows, subclass and implement `populateItem(ListItem<T>)`, or use the four-argument `of` factory.
Supply a `Label("label", ...)` child for each repeated button. Use `item.setEnabled(false)` for unavailable actions,
and configure its existing `DropdownMenuItemBehavior` for destructive styling or inset text. Add the action behavior
you need; the client dismisses the menu after activation, and `close(target)` is available to server callbacks.

For navigation, groups or checked choices, compose behaviors on your own markup:

```java
WebMarkupContainer trigger = new WebMarkupContainer("trigger");
trigger.add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE));
WebMarkupContainer menu = new WebMarkupContainer("menu");
menu.add(Sw.Behaviors.dropdownMenu(trigger));
menu.add(new BookmarkablePageLink<>("settings", SettingsPage.class)
        .add(Sw.Behaviors.dropdownMenuItem()));
add(trigger, menu);
```

```html
<button wicket:id="trigger" type="button">Account</button>
<div wicket:id="menu"><a wicket:id="settings">Settings</a></div>
```

Use one menu behavior per menu, with a native `type="button"` trigger. Other helpers are `dropdownMenuLabel()`,
`dropdownMenuSeparator()`, `dropdownMenuGroup()` and `dropdownMenuShortcut()`. Give groups an accessible name.
Shortcut text is a visual hint; it does not register a key binding. Use buttons or Wicket links for actionable rows,
and native form controls for ordinary form fields (`SwDropdownChoice<T>` is the native select field).

`dropdownMenuCheckboxItem(IModel<Boolean>)` and `dropdownMenuRadioItem(IModel<Boolean>)` render server-owned
`aria-checked` states and detach their models. Your callback updates the value, enforces radio exclusivity and adds
the item or menu to the Ajax target. The browser does not change checked state optimistically. Disabled items have
ARIA/native disabled state; disabled links lose their destination, and Wicket enforces server-side enabled state.

Enter/Space or Arrow Down opens with the first enabled item focused; Arrow Up opens at the last. Arrow keys,
Home/End and Page Up/Down navigate, and typing searches labels (repeated letters cycle matches). Disabled items
are skipped. Pointer opening focuses the menu; pointer movement focuses rows. Actions and Escape return focus
to the trigger, Tab/Shift+Tab close and continue through the page, and outside interaction dismisses the menu.
Placement, viewport limits, reduced motion and forced colors use the shared popup CSS in both skins.

The **Menus** showcase (`/menus`) covers Ajax actions/replacement, empty lists, checked choices, links,
confirmations and menus inside a sheet. The landing page's **Project actions** menu uses the same library script.

### Toasts

Use `Sw.toast(...)` for a direct notification from an Ajax callback, without creating a Wicket feedback message.
Its overloads follow Oat's message, variant and optional title API. Add the resource behavior once to your page or
base page before using the helpers:

```java
add(Sw.Behaviors.toasts());
add(SwButton.onClick("save", "Save", target -> {
    service.save();
    Sw.toast(target, "Your workspace is ready.", ToastBehavior.Variant.SUCCESS, "Changes saved");
}));
```

Variants are `ToastBehavior.Variant.DEFAULT`, `SUCCESS`, `INFO`, `WARNING` and `ERROR`. The message is the body;
an optional title appears above it. Both are plain text, including any HTML-looking characters. Error notifications
use an alert live region; other variants use a polite status. Showing a toast leaves keyboard focus in place.

General toasts close after five seconds. Set a duration on a particular toast, or use zero to keep it until dismissed:

```java
Sw.toast(target, "Review your settings before continuing.", ToastBehavior.Variant.WARNING,
        "Take your time", Duration.ZERO);
Sw.toast(target, "The link is ready.", ToastBehavior.Variant.SUCCESS, "Copied", Duration.ofSeconds(8));
```

Durations are zero or 1–2147483647 milliseconds. Timers pause while the pointer is over the notifications, while a
toast has keyboard focus and while the page is hidden. Resuming leaves at least one second. Keep essential results
visible on the page; choose persistent messages for information that needs more reading time.

Alt+T focuses the newest toast's close button. The close button or Escape dismisses the focused toast; focus moves
to a neighboring toast, then back to the control it came from after the last one. Escape elsewhere leaves toasts
alone. A named Notifications region holds a scrollable column at the logical bottom/end of the viewport, with
mobile gutters. While a native modal is open, the notifications move into it so they stay usable; they return to the
page when it closes. Removing the viewport clears its timers; navigating away clears old messages, including in
browser history snapshots. Toasts have no interactive action content besides dismissal.

For a full render, keep the same resource behavior and use the script helper from `renderHead`:

```java
response.render(OnDomReadyHeaderItem.forScript(ToastBehavior.script(this,
        "You can continue where you left off.", ToastBehavior.Variant.INFO, "Welcome back", Duration.ZERO)));
```

The component overload and Ajax helpers resolve `ToastBehavior.close` and `ToastBehavior.label` from Wicket
resources (defaults: “Dismiss notification” and “Notifications (Alt+T)”). The pure
`ToastBehavior.script(message, variant, title[, duration])` overloads use English control labels. For local browser
interactions the same runtime is available as
`Shadwick.toast(message, {variant: 'success', title: 'Copied', duration: 5000})`; options also accept `dismissLabel`
and `regionLabel`. `duration: 0` keeps the message. No inline styles or client framework are required.

The **Toasts** showcase (`/toasts`) covers all variants, timing, plain text, Ajax replacement, full-page scripts and
modal context. The landing page uses the library's toast runtime for its demo notifications. Existing
`FeedbackToastsBehavior` continues to handle Wicket messages and fences, with persistent, manually dismissed toasts.

### App shell

App-shell components now include a responsive sidebar, site header, breadcrumb trail and skip link. User menu,
layouts and per-user themes come next.

`SwSiteHeader` is a container on caller-owned `<header>` markup, following `SwCard`'s composition API. Add your
branding, ordinary navigation links and actions. `Sw.Behaviors.siteHeaderEnd()` pushes an action container to the
logical end; the row wraps and grows on narrow screens:

```java
SwSiteHeader header = new SwSiteHeader("header");
header.add(new BookmarkablePageLink<>("brand", HomePage.class));
WebMarkupContainer end = new WebMarkupContainer("end");
end.add(Sw.Behaviors.siteHeaderEnd());
header.add(end);
add(header);
```

```html
<header wicket:id="header">
    <a wicket:id="brand">My workspace</a>
    <div wicket:id="end"><button type="button">Save view</button></div>
</header>
```

Use `Sw.Behaviors.siteHeader()` on an existing container. `setInset(true)` removes the header's own background and
bottom border for a header within the page. Native `<header>` placement determines its landmark semantics; no
extra banner role is added. Headers scroll with the page; pair them with a sidebar trigger for responsive navigation.

`SwBreadcrumb<T>` follows OatBreadcrumb's list-model, `populateItem`, `of`, `setCurrent`, `setLabel` and `setMaxItems`
API. Bind it to a `<nav>`. Each item needs a `"link"` child wrapping a `"label"` child:

```java
add(SwBreadcrumb.of("path", Model.ofList(folders), (item, folder) ->
        item.add(new BookmarkablePageLink<>("link", FolderPage.class, folder.parameters())
                .add(new Label("label", folder.name()))))
        .setCurrent("Details")
        .setMaxItems(3));
```

Without current text, the final link gets `aria-current="page"`; `setCurrent(String/IModel<String>)` renders separate
escaped current-page text instead. A null current value returns to the last-link behavior. With more than
`maxItems` levels (minimum 2, counting current text), the first and final levels remain visible and an ellipsis opens
a menu of the middle ones. It reuses the dropdown menu's native popover, keyboard navigation, typeahead, dismissal
and focus return. The ellipsis does not count toward the limit. Labels come from `SwBreadcrumb.label` (Breadcrumb)
and `SwBreadcrumb.more` (Show more levels); `setLabel` or markup ARIA naming can distinguish multiple trails.

Models are read once per request and detached. Re-render the breadcrumb over Ajax to update its links, current
label or collapsed ranges; replacing an open trail closes its menu. Null or empty levels hide the trail, including
when current text is present. A markup placeholder lets a cleared trail reappear over Ajax. The list wraps long
labels, separators are hidden from assistive technology, and chevrons reverse in RTL.

For caller-owned markup, `Sw.Behaviors` also supplies `breadcrumb()`, `breadcrumbList()`, `breadcrumbItem()`,
`breadcrumbLink()`, `breadcrumbPage()` and `breadcrumbSeparator()`. Supply the actual `<nav>/<ol>/<li>/<a>/<span>`
structure and decorative separator content yourself. Explicit `aria-label` or `aria-labelledby` wins over the
standalone behavior's localized `BreadcrumbBehavior.label` fallback.

Put a skip link first in `<body>`, before repeated navigation. With a supplied component target it gets a markup
id and a missing `tabindex="-1"` is added to a non-interactive container; existing tabindex and native controls
retain their behavior:

```java
WebMarkupContainer main = new WebMarkupContainer("content");
main.setMarkupId("main");
add(main, new SwSkipLink("skip", main));
```

```html
<a wicket:id="skip"></a>
<!-- Header/navigation -->
<main wicket:id="content">...</main>
```

`new SwSkipLink("skip")` targets `id="main"`; `new SwSkipLink("skip", "results")` targets another id. For these
id-only forms, give a non-interactive target `tabindex="-1"` yourself. The localized label is `SwSkipLink.label`
(Skip to main content); `setLabel(String/IModel<String>)` replaces it. `Sw.Behaviors.skipLink([target])` enhances an
existing anchor. The link uses native fragment navigation and works without JavaScript: Tab reveals it, Enter
moves focus to the content, and the next Tab continues from there. Forced colors retain visible focus.

The **App shell** showcase (`/shell`) covers composition, real breadcrumb navigation, Ajax updates, empty trails,
long labels, RTL and keyboard access. Every showcase page now has the skip link; the landing studio uses the same
header/breadcrumb styles and a focusable native skip target.

### Sidebar

`SwSidebar` renders native navigation links and disclosure groups using Oat's `MenuItem` API. Bind its `<nav>`
directly inside a provider, beside the content inset:

```java
WebMarkupContainer provider = new WebMarkupContainer("layout");
provider.add(Sw.Behaviors.sidebarProvider());
SwSidebar sidebar = new SwSidebar("sidebar", Model.ofList(List.of(
        MenuItem.of("Overview", OverviewPage.class).withIcon("house"),
        MenuItem.group("Workspace",
                MenuItem.of("Inbox", InboxPage.class).withIcon("mail").withBadge(unreadCount),
                MenuItem.of("Projects", ProjectsPage.class).withIcon("folder")))))
        .setCollapsible(SidebarBehavior.Collapsible.ICON);
WebMarkupContainer content = new WebMarkupContainer("content");
content.add(Sw.Behaviors.sidebarInset());
content.add(new SwSidebarTrigger("toggle", sidebar));
provider.add(sidebar, content);
add(provider);
```

```html
<div wicket:id="layout">
    <nav wicket:id="sidebar"></nav>
    <main wicket:id="content">
        <header><button wicket:id="toggle"></button></header>
        <!-- Page content -->
    </main>
</div>
```

At 48rem and above, the sidebar is a sticky column. `setCollapsible` chooses `OFFCANVAS` (default), `ICON`, or
`NONE`; `setVariant` chooses `SIDEBAR` (default), `FLOATING`, or `INSET`. `setSide(END)` puts it on the logical end,
including RTL. On smaller screens the same navigation opens as a native popover with a close button. The enhanced
panel focuses the current link, makes the background inert, traps Tab, and returns focus on Escape or dismissal.
The trigger and Ctrl/Cmd+B toggle it; the shortcut leaves text fields and modal dialogs alone. Link navigation
closes the mobile panel. Reduced motion, forced colors and printing have dedicated styles.

Desktop collapse is a browser preference, remembered in the `sw-sidebar-state` cookie. Configure the provider
with `setDefaultState`, `setRememberState(false)` or a distinct `setCookieName` for independently remembered
sidebars. The cookie contains only `expanded`/`collapsed` and is not server-side open state. Native mobile opening
and navigation also work without JavaScript; desktop navigation stays expanded in that case. Focus trapping,
background inertness, shortcuts, icon hints and desktop collapse use the shared library script.

Links retain ordinary Tab navigation and have no menu roles. The current page uses `aria-current="page"`, matching
its page class and any supplied parameters exactly. Override `isCurrentPage` for application-specific route matching
or `newMenuLink` for custom links, including Ajax links. Unauthorized pages and groups without authorized children
are omitted. Groups are one level deep and open when one of their children is current. Labels and badges remain
escaped, and a missing icon uses a generic circle. Badges hide when null, blank or numeric zero; visible badges
describe their links, including in the icon rail. Hover/focus hints appear only in the desktop icon rail.

Use `setHeader`/`setFooter` with components named `SwSidebar.HEADER_ID`/`FOOTER_ID` for caller-owned content.
`setLabel(String/IModel<String>)` names the navigation; resource keys are `SwSidebar.label`, `SwSidebar.close` and
`SidebarTriggerBehavior.label`. `target.add(sidebar.getMenu())` refreshes badges/links without closing the mobile
panel. Replacing the whole sidebar or provider closes it and releases background inertness. Caller-owned inert
regions stay inert. The `sw-sidebar-toggle` browser event exposes `{state, open, mobile}`.

`Sw.Behaviors.sidebar()`, `sidebarProvider()`, `sidebarTrigger(component)` and `sidebarInset()` also support
caller-owned anatomy; header/content/footer/menu/menu-item/menu-button/menu-label helpers provide the corresponding
classes. The **Sidebar** showcase (`/sidebar`) demonstrates layouts, real routes and Ajax updates. The landing
studio includes the same responsive navigation with persistence disabled.

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

Run the showcase to explore the components, including Menus and Toasts, with a skin and light/dark switch on every page:

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

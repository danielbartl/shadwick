# Freeform tags

`SwTagInput` edits a `List<String>` of application-defined tags. It follows Wicket Oat’s constructor and
conversion API and uses the existing Shadwick field and chip styles in all nine skins.

```java
var tags = Model.ofList(new ArrayList<>(List.of("wicket", "java")));
form.add(new SwTagInput("tags", "Project tags", tags)
        .setRequired(true)
        .setPlaceholder("Add a project tag")
        .setDescription("Enter up to three tags.")
        .addValidator(value -> {
            if (value.getValue().size() > 3) {
                value.error(new ValidationError().setMessage("Choose at most three tags."));
            }
        }));
```

```html
<div wicket:id="tags"></div>
```

Constructors accept an optional selected model, String/model label and description model. An omitted model
inherits from a parent `CompoundPropertyModel`, keyed by the component ID. Missing labels use Wicket resources
with that same key. The native input is the direct Wicket child `input`: use `tags:input` in a `FormTester`.
`getInput()` returns its `TextField<List<String>>` for validators, updating behaviors and native attributes.

## Values and validation

The native field submits a comma-separated string. Conversion splits on commas, applies Java `String.trim()`
to each part, removes empty entries and de-duplicates exact matches in their first-seen order. Case and accents
are preserved: `java`, `Java` and `jáva` are separate tags. Nonbreaking spaces are preserved by `String.trim()`;
the browser uses the same rule. An optional blank value becomes a mutable empty list. A required field rejects
blank or comma-only input without replacing its previous model.

Rendering applies the same normalization without changing the supplied list. A null list renders empty;
null/blank elements are omitted. A model tag containing a comma is rejected because it cannot round-trip.
There is no quoting or escaping syntax for commas inside tags. Use an application-specific compound field if
individual values must contain commas. Tags and labels are escaped on the server and inserted as text by the
browser; strings that look like markup remain literal text.

Validators receive the converted list. Applications own tag count/length limits, permitted content, persistence
and authorization. `setEnabled(false)` follows the normal Wicket hierarchy and ignores forged submissions.
For read-only presentation, add a native `readonly` attribute through `addToInput`; treat it as a user-interface
hint and enforce immutable values in application code when necessary. There is no suggestion service or choice
resolver; use [Combobox](combobox.md) for an authorized set of choices.

## Editing and submission

With JavaScript, the native input is visually hidden and an ordinary text editor and named remove buttons are
created. The native Wicket input remains the only submitted field. Every edit synchronizes its value, including
pending text, so native submission, Ajax form submission and input updating behaviors can receive the current
draft without waiting for blur. `input` events accompany value changes; `change` accompanies a chip commit or
removal. Application code can update the native value and dispatch `input` or `change` to refresh the chips.

Enter commits the draft and prevents implicit form submission. Comma commits the preceding parts; pasting
comma-separated text leaves its final part as the draft. Blur outside the chip control commits it too. Input
composition is respected, including Enter used to accept an IME candidate. Exact duplicates announce that the
tag was already added. Removing a tag preserves any pending draft.

Native/Ajax submission uses the usual [form validation](forms.md). In-place Ajax feedback updates the error
message and the editor’s invalid state while preserving chips, draft, node identity and focus. An unrelated Ajax
response leaves the editor untouched. Explicitly replacing the field rebuilds it from the current server model.
The `/tag-input` example demonstrates both paths, fieldset disabling, replacement and application presets.

Native form reset restores the values rendered with that form and clears the draft, unless the reset event is
cancelled. Reset itself does not update the server model. Replaced/disconnected controls release their listeners
and observers. Re-inserting a cloned history snapshot enhances it once and restores its synchronized draft.

## Accessibility, localization and fallback

The editor keeps the field label and the original input’s description, hint, required and invalid relationships.
It is a textbox; the widget adds no combobox or listbox role. Each chip has a normal Tab-accessible button named
`Remove {tag}`. Enter/Space activates that button; Delete/Backspace removes its focused tag. Arrow keys move
between remove buttons using the document’s direction; Home goes to the first and End returns to the editor.
Backspace in an empty editor first focuses the final remove button. Add, duplicate and remove announcements use
a separate polite status region. Disabled and read-only fields keep visible tags and disable their removal.

Without JavaScript, the labeled text field stays visible with the hint “Separate tags with commas.” It uses the
same server conversion and validation. Hints, placeholders, editing instructions, button names and announcements
ship in English, German, French, Spanish, Italian, Japanese, Dutch, Portuguese and Croatian. Override
`SwTagInput.placeholder`, `.hint`, `.remove`, `.added`, `.removed`, `.duplicate` and `.instructions` through normal
Wicket resources. Action/announcement patterns use a literal `{0}` placeholder. `setPlaceholder` accepts a String
or detachable model; an explicit null clears it. Model labels, descriptions and configured placeholders follow
Wicket’s detach lifecycle.

Long tags wrap inside the control. Focus and chip borders remain visible in forced colors, and the widget works
in standalone and embedded bundles under the existing strict CSP. It adds no dependency, inline styles or network
requests of its own. The low-level `TagInputBehavior(input)` / `Sw.Behaviors.tagInput(input)` can enhance an
application-owned wrapper containing that input as a direct HTML child; its converter, label, hint and validation
remain application-owned. See [Compatibility](../guides/compatibility.md) for the browser baseline.

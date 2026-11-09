# Form fields

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
focus and what the user typed. Shadwick keeps `required` and pattern validation on the server so browser popups
do not bypass those inline messages. Wicket 11's derived native constraints and label-derived placeholder are
removed by the field's initial input behaviors. Use `setPlaceholder(...)` for an explicit hint; applications that
need native constraints can add them explicitly with later `addToInput(AttributeModifier...)` behaviors.

## Complete example

This standalone page works with the application in the [installation guide](../guides/installation.md#complete-example). The native button submits the same form without Ajax.

`onInitialize()` shows the page structure. `profileForm()`, `name()` and `saveButton()` hold the component
configuration, while `saveProfile()` handles the submitted values.

<!-- snippet: docs/examples/QuickStartPage.java -->
```java
package dev.example;

import dev.jbaby.shadwick.Sw;
import dev.jbaby.shadwick.behaviors.ButtonBehavior;
import dev.jbaby.shadwick.components.SwSubmitButton;
import dev.jbaby.shadwick.components.form.SwTextField;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class QuickStartPage extends WebPage {

    private final IModel<String> name = Model.of("");
    private final IModel<String> receipt = Model.of("No changes saved yet.");

    @Override
    protected void onInitialize() {
        super.onInitialize();
        Label status = status();
        add(status, profileForm(status));
    }

    private Label status() {
        Label status = new Label("status", receipt);
        status.setOutputMarkupId(true);
        return status;
    }

    private Form<Void> profileForm(Label status) {
        Form<Void> form =
                new Form<>("form") {

                    @Override
                    protected void onSubmit() {
                        saveProfile();
                    }
                };
        form.add(name(), saveButton(status));
        form.add(new Button("saveNative").add(Sw.Behaviors.button(ButtonBehavior.Variant.OUTLINE)));
        return form;
    }

    private void saveProfile() {
        // Replace this demo result with an authorized application service call.
        receipt.setObject("Saved profile for " + name.getObject() + ".");
    }

    private SwTextField<String> name() {
        return new SwTextField<>("name", "Your name", name).setRequired(true);
    }

    private SwSubmitButton saveButton(Label status) {
        return SwSubmitButton.onSubmit(
                        "save",
                        "Save with Ajax",
                        target -> target.add(status),
                        target -> target.add(status))
                .setBusyIndicator(true);
    }
}
```

<!-- snippet: docs/examples/QuickStartPage.html -->
```html
<!doctype html>
<html lang="en" xmlns:wicket="http://wicket.apache.org">
<head><meta charset="utf-8"><title>Profile</title></head>
<body>
<main>
  <h1>Your profile</h1>
  <form wicket:id="form">
    <div wicket:id="name"></div>
    <button wicket:id="save" type="submit"></button>
    <button wicket:id="saveNative" type="submit">Save without Ajax</button>
  </form>
  <p wicket:id="status" role="status"></p>
</main>
</body>
</html>
```

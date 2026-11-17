# Wizard

A form filled in over several steps, each checked before the next, with Back, Next and Finish.

> **New in 0.2.0, not yet released.** `SwWizard` isn't in 0.1.0, the version on Maven Central.

`SwWizard` follows Wicket Oat's `OatWizard`. Give it its steps, each a title and a factory for the step's component,
and what to do at the end:

```java
SwWizard wizard = new SwWizard("signup") {
    @Override
    protected void onFinish(AjaxRequestTarget target) {
        accounts.create(signup);
        setResponsePage(WelcomePage.class);
    }
};
wizard.addStep("Account", id -> new SwEmailField(id, "Work email", new PropertyModel<>(signup, "email")).setRequired(true));
wizard.addStep("Profile", id -> new ProfileStep(id, signup));
wizard.addStep("Review", id -> new ReviewStep(id, signup));
add(wizard);
```

```html
<div wicket:id="signup"></div>
```

Without a subclass, `Sw.Components.wizard(id, target -> ...)` (or `SwWizard.onFinish`) gives the callback directly.

## Steps and data

A step's component is created when the step is shown, with the id its factory is given (`SwWizard.STEP_ID`): usually
one Shadwick field, or a panel or fragment with several. Keep the data in a model all steps share, like `signup` above,
so a step that is created again starts from what was entered. `getCurrentStep()` and `setCurrentStep(int)` read and set
the step shown, for example to start somewhere else than at the first.

The numbered row above the form is a [steps indicator](steps.md): finished steps are links back, and the current and
later steps are text.

## Checking a step

**Next** validates the current step's fields (only that step is in the form) and shows their errors next to them, as
every Shadwick field does. **Back** and the numbers of finished steps go back without validating. To check more than
the fields - an email already taken, a combination that doesn't work - override `onNext(int step, AjaxRequestTarget)`
and report an error: on a field (`emailField.getInput().error(...)`) it shows at the field, on the form
(`getForm().error(...)`) in the [error summary](forms.md#error-summary) above the step. Either keeps the user on the step.
**Finish** checks the last step the same way, then calls `onFinish`. Enter in a field clicks Next, or Finish on the last
step.

## Focus, keyboard and screen readers

After Next or Back over Ajax, the keyboard focus moves to the new step, a group named "Step 2 of 3: Profile"; after a
failed Next it moves to the error summary, or to the first field with an error. The buttons and the step links are
native, so Tab, Enter and Space work as everywhere. The texts are the `SwWizard.back`, `SwWizard.next`,
`SwWizard.finish`, `SwWizard.steps` and `SwWizard.step` resources, in all nine languages.

## Without JavaScript

The buttons are Wicket fallback buttons: without JavaScript they submit the form normally and the page renders again
with the next step. `onNext` and `onFinish` then receive `null` for their target, so check it before adding components
to it. The finished-step links need JavaScript and show as plain text without it; Back still works.

Don't put a wizard inside another form: a form inside a form isn't HTML, so only Ajax could submit it.

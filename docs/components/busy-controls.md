# Busy controls

Opt in to a spinner, localized loading text and repeated-activation protection while a request runs:

```java
add(SwButton.onClick("run", "Run report", target -> runReport()).setBusyIndicator(true));
form.add(SwSubmitButton.onSubmit("save", "Save", target -> save()).setBusyIndicator(true));
dialog.setBusyIndicator(true); // also available on sheets and confirmation dialogs

// Ordinary Wicket Ajax links/buttons and AjaxEventBehavior controls:
link.add(Sw.Behaviors.ajaxBusy());
```

Requires `Shadwick.install(this)`. Busy handling starts before queueing and restores the control after success,
validation failure, transport failure or a cancelled precondition. Labels and keyboard focus are preserved; Ajax
replacement leaves the new element's state intact. Native submit controls stay enabled for Wicket's form
serialization while `aria-busy`/`aria-disabled` describe the temporary state. Input submitters get an adjacent
loading indicator. No inline styles/handlers or new CSP allowances are required.

`setBusyIndicator(false)` removes the behavior. Override `AjaxBusyBehavior.loading` through normal Wicket
resources. The guard covers repeated activation of that control; application-level idempotency remains the
application's responsibility.

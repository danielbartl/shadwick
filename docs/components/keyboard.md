# Keyboard keys

`SwKbd` displays a key name using native `kbd` markup. Group several keys with `sw-kbd-group`.

```java
add(new SwKbd("modifier", "Ctrl"));
add(new SwKbd("key", "K"));
```

```html
<span class="sw-kbd-group"><kbd wicket:id="modifier"></kbd><kbd wicket:id="key"></kbd></span>
```

String and model constructors render escaped key text; `Sw.Behaviors.kbd()` styles an existing Wicket label or
container. Choose shortcut names appropriate to the operating system and provide context about the action.
Displaying keys does not register a keyboard shortcut or make a control interactive. Add the actual shortcut in
your application, avoid overriding essential browser/assistive shortcuts and keep a normal visible action available.

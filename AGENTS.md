# Project conventions

Follow the [readable Wicket composition conventions](docs/CONTRIBUTING.md#readable-wicket-composition)
when changing the library, examples or complete documentation examples.

- Keep constructors focused on parameters, models, configuration and invariants. Assemble initial page/panel
  component trees in `onInitialize()`, starting with `super.onInitialize()`.
- Make the composition entry point read as an overview. Delegate field configuration and substantial callbacks
  to plainly named private methods, keeping one level of detail in each method.
- Component factories return components; `add…` methods attach them. Preserve clear ownership, useful concrete
  return types and intentional subclass hooks. Do not call overridable factories from superclass constructors.
- Preserve library children that fluent setters, getters or content slots need before attachment. Retain their
  existing eager/lazy timing and identity rather than mechanically moving everything to `onInitialize()`.
- Keep changing visibility/enabled state in `onConfigure()` and dynamic content in the appropriate repeater or
  interaction hooks. Preserve IDs/markup, model detachment, feedback context, serialization and Ajax/focus behavior.
- Prefer small ordinary Wicket methods over additional builders/base classes. Keep obvious expressions inline,
  dependencies explicit, statements separate and related methods easy to navigate.

[QuickStartPage.java](docs/examples/QuickStartPage.java) is the complete reference example. See
[CLAUDE.md](CLAUDE.md) for implementation history and the current project checkpoint.

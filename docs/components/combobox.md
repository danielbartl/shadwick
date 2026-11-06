# Combobox

`SwCombobox<T>` searches a single choice; `SwMultiCombobox<T>` searches several choices displayed as removable
chips. Both wrap real Wicket selects and share the usual label, description and inline feedback:

```java
form.add(new SwCombobox<>("city", "City", selectedCity, cities,
        new ChoiceRenderer<City>("name", "id"))
        .setGroup(City::region)
        .setIcon(city -> "map-pin")
        .setDisabledWhen(City::unavailable)
        .setPlaceholder("Find a city")
        .setShowClear(true)
        .setRequired(true));

form.add(new SwMultiCombobox<>("teams", "Teams", selectedTeams, teams)
        .setAutoHighlight(true)
        .setShowClear(true)
        .setPlaceholder("Add teams"));
```

Use `<div wicket:id="city"></div>` / `<div wicket:id="teams"></div>`. Constructors follow the native choice
fields: selected model and choices model, optional `IChoiceRenderer`, String/model labels and description.
Without a selected model, `CompoundPropertyModel` supplies it by component ID. Multiple selection binds a
`Collection<T>`, including mutable lists/sets, through Wicket's collection update contract. Stable renderer IDs
are recommended for changing lists. The choices model snapshots once per request and detaches normally.

## Local search and choice identity

Typing filters locally without case or accents. A group hides when none of its choices match; separators hide
while searching. `setDisplay`, `setGroup`, `setIcon`, `setSeparatorBefore` and `setDisabledWhen` decorate the
options without reordering IDs. `setEmpty(String/IModel<String>)` customizes empty results. Only choices are
submitted; unmatched typed text is never accepted as a value. Disabled and unknown IDs fail conversion atomically.

## Keyboard behavior

By default typing highlights nothing. `setAutoHighlight(true)` highlights the first match for Enter. Arrows and
PageUp/PageDown move among enabled results while focus stays in the input; Home/End keep their caret behavior.
Enter chooses (or toggles a multiple choice), Escape restores the chosen text, and Tab closes without choosing.
Emptying a single input and leaving it clears its value. In a multiple field, Backspace in an empty input removes
the last chip. ArrowLeft from its start focuses the chip remove buttons; Left/Right move between them and
Delete/Backspace removes a chip. Clear/trigger/chip buttons have equivalent keyboard paths and stay out of Tab order.

## Server search

For larger lists, supply an Oat-style server search callback with stable identity and a resolver:

```java
form.add(new SwCombobox<City>("city", "City", selectedCity)
        .setChoices(text -> cityService.searchVisibleCities(text, 20))
        .setChoiceId(City::id)
        .setChoiceResolver(id -> cityService.findVisibleCity(id))
        .setDisplay(City::name)
        .setMaxChoices(20)
        .setAutoHighlight(true)
        .setShowClear(true));
```

The resolver returns a currently authorized choice or null; it runs again on submission and its returned ID must
match. `setDisabledWhen` also checks newly resolved objects. Search results are never accumulated in the Wicket
page. The search callback receives at most 256 characters; `setMaxChoices` limits each answer (default 10,
allowed 1–200). Keep the service query itself bounded. The empty query supplies the initial choices unless an
explicit choices model was provided; current selected objects are included for display and native fallback.

Search waits 300 ms after typing, calls a same-origin Wicket JSON endpoint and cancels older requests. The
server filters; the browser preserves chosen options omitted from an answer, hides them from the current results,
and restores initial options plus current choices when search ends. Busy, empty and error states have localized
messages and a delayed result announcement. HTTP errors, redirects and invalid responses show an error and retain
the selection. `sw-combobox-search` bubbles from the select before each request; `event.detail.query` and
`event.detail.url` allow adding parameters, and `preventDefault()` cancels it. URLs remain same-origin.

## Forms and Ajax

Normal/Ajax submission and `AjaxFormComponentUpdatingBehavior("change")` use the native select. Inline Ajax
feedback updates in place and preserves the generated input's focus; `target.add(field)` initializes a replacement
and cleans up removed listeners, observers, timers and requests. Valid remote choices are also retained
when another field blocks model updates on a normal full-page validation response. Programmatic select value/option changes should
fire `input` or `change`. Native form reset restores rendered defaults without change events or server updates;
a canceled reset leaves the control alone. All skins/bundles, disabled fieldsets, RTL, IME composition, forced
colors, reduced motion and nested dialogs/popovers are supported without inline styles or CSP changes.

## Caller-owned markup and fallbacks

`Sw.Behaviors.combobox()` also enhances caller-owned wrappers containing a labelled
`<select class="sw-combobox-select">`. It supplies the input/list/chip anatomy without a client framework.
Behavior setters include placeholder/empty text, clear button and auto-highlight; typed fields supply their
own search URL. A caller-owned same-origin endpoint may return a JSON array of `{id, text, disabled?, group?,
separator?, icon?}`; icons are optional drawing-only SVG, sanitized before insertion. No-script/API fallback stays
a native single/multiple select, with only initially rendered choices available. The **Combobox** showcase
(`/combobox`) exercises local/server choices, validation, reset, live replacement and overlays; the landing
studio includes a searchable multiple-team picker using the actual CSS/runtime.

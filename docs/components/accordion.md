# Accordion

`SwAccordion<T>` follows OatAccordion's repeating `ListView` API and factories. It uses native `details` groups,
without JavaScript. A callback supplies `title` and `content`; transparent trigger, heading and body containers add
the disclosure structure while keeping those component paths on the list item:

```java
WebMarkupContainer faq = new WebMarkupContainer("faq");
faq.setOutputMarkupId(true);
faq.add(SwAccordion.of("questions", Model.ofList(List.of("Shipping", "Returns")), (item, question) -> {
    item.add(new Label("title", question), new Label("content", "About " + question));
}).setHeadingLevel(3).setOpenItems(0));
add(faq);
```

```html
<div wicket:id="faq" class="sw-accordion">
    <details wicket:id="questions">
        <summary wicket:id="trigger">
            <span wicket:id="heading"><span wicket:id="title"></span></span>
            <svg class="sw-accordion-trigger-icon" viewBox="0 0 24 24" fill="none"
                 stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg>
        </summary>
        <div wicket:id="body"><div wicket:id="content"></div></div>
    </details>
</div>
```

The summary is the first child of each details; its inner span is a heading (level 3 by default, configurable 1–6).
The default exclusive group permits one section open, and permits closing all. The constructor's `exclusive` argument
or `setExclusive(false)` makes sections independent. `setOpenItems(int...)` configures initial indexes; for an exclusive
group the earliest configured index in document order wins. Refresh the enclosing wrapper over Ajax, rather than the
repeating ListView. Each render generates a fresh group name, so new markup does not join the old exclusive group
while Wicket replaces it. Nested accordions have independent groups.

Calling `item.setEnabled(false)` fades/skips its summary and omits closed content; an initially open disabled item
keeps its content. Normal keyboard activation, expanded state and opening a section for a fragment/find match come
from the browser. Height and chevron motion respect reduced motion; browsers without height-to-auto interpolation
open immediately. The runtime adds no accordion keyboard shortcuts. Initial open indexes are server configuration,
not live browser state. `Sw.Behaviors.accordion([exclusive])` supports caller-owned items (attach the same behavior to
the ListView and its items); `.setName(...)` can supply an explicitly shared name. The generated name is preferable
for Ajax replacement. Trigger/content helpers style caller-owned markup; applications manage its headings and
disabled content visibility.

# Agent guidance

Use the Shadwick coding skill and machine-readable guides to build Wicket interfaces with the current Java API.

## The Shadwick skill

The reusable skill is in the repository's `shadwick/` directory. It covers component selection, matching Java
and HTML, models, form validation, partial Ajax updates, data tables, appearance and localization. It preserves
the consuming application's framework, structure and service boundaries.

Read [SKILL.md](https://danielbartl.github.io/shadwick/skills/shadwick/SKILL.md), or get the complete
[skill directory from the repository](https://github.com/danielbartl/shadwick/tree/main/shadwick).
Copy that directory, including `references/`, `assets/`, `agents/` and `LICENSE.txt`, into the skills directory
supported by your coding agent. Loading only the entrypoint omits its supporting references and working templates.
This repository publishes the skill; it does not install it into your agent's configuration automatically.

For an agent that uses a local `.agents/skills/` directory, copy the package into that directory in the consuming
project. Use your agent's documented skill location if it differs:

```sh
mkdir -p /path/to/your-app/.agents/skills
cp -R /path/to/shadwick-repository/shadwick /path/to/your-app/.agents/skills/shadwick
```

Then ask for a concrete interface. For example:

```text
Use the Shadwick skill to add a profile form to this Wicket application.
Keep its existing base page and service layer. Show required-field errors inline
and update the saved-state message with Ajax. Provide the matching Java and HTML.
```

The skill description supports automatic selection by compatible agents. The optional `agents/openai.yaml`
metadata provides a display name and a prompt using `$shadwick` for clients that support that invocation.

## Working form templates

The skill includes `QuickStartApplication.java`, `QuickStartPage.java` and `QuickStartPage.html`. They are copies
of the complete examples compiled and exercised by the documentation tests. The website build rejects drift
between those sources and the skill's assets. Adapt their package/class names and reuse an existing application's
configuration rather than replacing it wholesale.

These files demonstrate field-owned markup, a typed model, validation, native submission and an Ajax receipt
update. Application persistence and authorization still belong in the consuming application's service layer.
See [installation](installation.md), [Wicket integration](wicket-integration.md) and [forms](../components/forms.md).

## Documentation for agents

[llms.txt](https://danielbartl.github.io/shadwick/llms.txt) is the small documentation index. It links to the skill
and every registered guide as plain Markdown, grouped by topic. Start there and fetch only the relevant guides.
The index uses the build's Java, Wicket and repository-version metadata and reports the current installation status.

Each HTML documentation page has a Markdown companion at `index.md` under the same page directory. For example,
the [forms Markdown](https://danielbartl.github.io/shadwick/docs/components/forms/index.md) includes the same
usage guide plus API declarations derived from Java source. Links between guides resolve to their Markdown
companions, and complete Java/HTML code fences remain intact. HTML pages advertise their companion with
`rel="alternate"` and their documentation index with `rel="describedby"`.

The generated index and Markdown companions follow the [llms.txt proposal](https://llmstxt.org/). They are built
alongside the website; do not edit generated files in `site/dist/`.

## Verify generated code

Ask the agent to use the existing dependency configuration, match Java IDs to `wicket:id` at the correct hierarchy,
and use the component's documented markup ownership. For local updates, target the changed components instead of
replacing the page. Check validation and keyboard/focus behavior as appropriate to the change.

The [live catalog](https://shadwick-production.up.railway.app/components) shows the running examples and their
actual Java/Wicket source. [Clover Studio](https://shadwick-production.up.railway.app/) demonstrates a connected
application. The linked API guides and source remain the authority for method signatures and supported behavior.

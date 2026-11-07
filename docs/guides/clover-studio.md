# Clover Studio

A working agency workspace that composes Shadwick components into a connected project-to-payment workflow.

Clover Studio runs in the existing Wicket examples application at `/studio`. It has its own responsive
application shell, with an overview, project directory, project workspace and invoice register. The component
catalog remains at `/` for focused API examples. No new library or frontend dependency is required.

## Run locally

From the repository root:

```sh
./mvnw -B package
java -jar shadwick-examples/target/shadwick-examples-0.1.0-SNAPSHOT.jar
```

Open `http://localhost:8080/studio`. The `PORT` environment variable changes the server port.
GitHub Pages serves the static marketing site and these guides; the Wicket application needs a Java host.
A public backend deployment is a separate roadmap checkpoint.

## Try a complete journey

1. Open **Fern & Field** from the overview or project directory.
2. Edit the project details, preview or download its scope, and complete the two remaining tasks.
3. Select **Prepare client handoff**, confirm and download the ZIP. Delivery locks the completed task checklist.
4. Open invoice **CLV-2402** in the project workspace and select **Record payment**.
5. Return to the overview. Active projects, outstanding fees, collected payments, chart and activity reflect the changes.
6. Open **Demo settings** and reset the sample data to try again.

Project creation starts with an empty scope. Add tasks with an owner and estimated hours to build it out.
New projects do not automatically issue invoices. Seeded invoices let visitors try the payment workflow;
this example does not send invoices or process real payments.

## Screens and components

| Screen | Working behavior | Shadwick composition |
| --- | --- | --- |
| Overview | Derived metrics, nearest deadline, project links, activity and cash view | App layout, sidebar, chart, progress |
| Projects | Create, search, status filter and sort through bookmarkable URLs | Validated form, custom selects, dialog, badges, progress |
| Project workspace | Edit details, add/complete/reopen tasks, view milestones, preview/download scope and prepare/download handoff | Fields, dialog, confirmation, attachments, progress, buttons |
| Invoices | Sort, paginate, choose columns, inspect invoices, record payments and export CSV | Data table, date/number/link/badge/actions columns, dialog |

The header offers a browser-owned Light/Dark/System preference. **Demo settings** chooses any of nine skins
and either English or German. Select **Apply preferences** to update the complete interface, including
navigation, dialogs, feedback, chart/table labels and activity. Component messages, dates and amounts follow
the same locale. Client names and entered project/task content remain unchanged. Other language preferences
from a previous demo session fall back to English. Skin and language changes preserve workspace data and filters.
The library itself still ships nine built-in component message languages; Studio deliberately offers only two.

## Ajax updates and navigation

Studio commands use Shadwick's existing Ajax callbacks and `AjaxRequestTarget`. Completing or reopening a
task, saving project details, adding tasks, preparing a handoff, recording payments and resetting the demo
return partial responses. They do not request another page. Payments stay in the current project workspace
or invoice register.

The application binds displayed values to the current session model and remembers each component's last
rendered value. It adds only changed components to the Ajax target, avoiding child replacements when a parent
is already being updated. Completing a task from 50% to 75% replaces its row, progress bar, percentage,
completed count and feedback. The other rows, project summary container, budget, files, milestones, handoff
region and application shell keep their DOM nodes. Reaching 100% also changes the status and handoff controls.
Adding a task changes the task collection; handing off changes the controls on every task row. A payment
updates its status and any visible affected totals, and only reorders the table if the active sort requires it.

Internal navigation replaces the view and changed header/navigation state over Ajax, with bookmarkable URLs
and browser Back/Forward support. Every search and clear-filter request returns freshly rendered HTML for
the result collection, count and empty state, including repeated searches with identical matches. The
existing form inputs stay in place when their browser values are already correct. The response's
`CloverStudio.url(...)` script only updates browser history; it does not load another page. Table sorting, paging and column choices
use the library's existing Ajax behavior. Global language/skin changes update localized regions and styles;
table sorting, page size and column choices are preserved. Studio explicitly loads the chosen skin bundle
for this global change, while ordinary component Ajax updates retain Shadwick’s pinned page skin. Prior
stylesheets stay available for switching back. Downloads remain native file requests.

This behavior is application composition. Shadwick buttons, dialogs and tables already support Ajax. The
initial Studio implementation explicitly called `setResponsePage` after commands, causing redirects and
full-page replacement; those calls have been removed from scripted interactions. Closing a native dialog
still sends its normal Ajax state-synchronization callback, without document navigation.

Native links and project filters remain available without script. Their GETs return HTTP 200 directly,
without a page-ID redirect: `StudioPageMapper` scopes `NEVER_REDIRECT` to normal bookmarkable Studio GETs.
Listeners, page-instance requests and POSTs retain Wicket's normal handling, and the global render strategy
is unchanged. The native preference form retains a POST/redirect fallback. Editing, modal commands and
payments require JavaScript; chart values remain available in the native data-table disclosure.

## Data and lifecycle

`StudioWorkspace` lives in Wicket session metadata, with immutable project/task/invoice snapshots and
bounded sample collections. Separate visitors have independent workspaces. The demo intentionally has no
shared database, login, payment processor or file-storage service. Data lasts for the Wicket session and
can be discarded by reset, session expiry or server restart. The demo date is anchored when data is seeded.

Derived metrics and charts read the same invoice/project records as the action screens. Payment recording
is idempotent. Dialog submissions are tied to the instance that was opened and invalidated on success,
cancel or replacement. A stale submission cannot act on another invoice/project after Ajax navigation.
Task commands also verify the request's project context. Project revisions and task-state checks reject conflicting edits. Every mutation/download
rechecks the workspace generation, so a stale tab cannot change a newly reset workspace. Handoff checks
all tasks again on the server before locking the project. A new project supports up to 50 tasks, and the
workspace supports up to 30 projects; the activity feed retains the latest 12 events.

The scope Markdown is generated from the current project. The handoff ZIP contains a scope summary,
completed checklist and next steps. These are real, generated sample documents, rather than links to
unavailable client assets. No uploads or request-scoped file objects are retained.

For a production application, replace the session store with application services and enforce your own
authorization, persistence, transactions, audit history and storage lifecycle. The example demonstrates
component composition and local concurrency checks, rather than a production agency-management backend.

## Source and verification

The example uses `StudioPage.java`, its panel markup files and `studio.css` under the Wicket examples module.
`StudioWorkspace.java` owns the data and transitions. `StudioPageMapper.java` scopes direct GET rendering
to bookmarkable Studio pages, without changing the application-wide request strategy. The dedicated `StudioWorkspaceTest`, `StudioPageTest`
and `StudioBrowserTest` cover data consistency, stale edits, serialization, per-session isolation,
validation, handoff/CSV downloads, Chromium/Firefox workflows and accessible desktop/mobile layouts.

```sh
./mvnw -B verify
./mvnw -B -pl shadwick-examples -am -Pbrowser -Dtest=StudioBrowserTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Browser screenshots are written to `shadwick-examples/target/studio-screenshots/` and remain ignored.
See [app layouts](../components/app-layout.md), [data tables](../components/data-table.md),
[attachments](../components/attachments.md) and [forms](../components/forms.md) for the reusable APIs.

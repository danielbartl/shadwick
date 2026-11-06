# User menu

`SwUserMenu` follows OatUserMenu's model-backed API. It shows an account menu while the name is non-null, and an
optional Sign in link otherwise. Its default placement is the sidebar footer:

```java
SwUserMenu user = new SwUserMenu(SwSidebar.FOOTER_ID, () -> MySession.get().getUserName())
        .setDetail(() -> MySession.get().getEmail())
        .setPicture(() -> MySession.get().getPictureUrl())
        .setSignInPage(SignInPage.class);
user.addLink("My account", AccountPage.class).setIcon("user");
user.addLink("Settings", SettingsPage.class).setIcon("settings");
sidebar.setFooter(user);
```

For the site header, bind to a caller-owned `<div wicket:id="account"></div>` and use
`setPlacement(SwUserMenu.Placement.HEADER)`. The header button shows the avatar alone and is named “Account: …”; the
sidebar button shows the name and optional detail, and narrows to the avatar in an icon rail. Initials provide the
fallback while a picture loads or when none is available. Header initials are decorative CSS glyphs, so the button's
accessible name stays the full account name. All skins use a round header avatar button.

The sidebar menu opens beside the trigger and aligns to its end; the header menu opens below it. Existing mobile
sidebar styles place the menu above/below its trigger. `setSide` and `setAlign` override these defaults. Icon-rail
name hints reuse the tooltip behavior and stay hidden while the menu is open. Native popovers and the existing
menu runtime provide arrow keys, Home/End, typeahead, Tab, Escape and focus return. Opening and submitting Sign out
also work without JavaScript.

`addLink` accepts String or `IModel<String>` labels, a page class and optional `PageParameters`, returning a
`MenuLink` with `setIcon`. Links are escaped, unauthorized pages are omitted, and exact current-page matches get
`aria-current="page"`. `setDetail` and `setPicture` accept String or model values. Name/detail/picture models are read
once per request and detached. Refresh the whole component with `target.add(user)` after an account update; an open
menu closes on replacement. `getTrigger()` and `getMenu()` expose the composed controls.

`setSignInPage` supplies an ordinary bookmarkable link, hidden on that page and its subclasses. Without a sign-in
page, an anonymous menu has no visible content. Override `isSignedIn()` for an authenticated user whose name can be
null; the display falls back to “Your account”, with initials from the detail's email local part if available.

Sign out submits a separate hidden `StatelessForm` outside `role="menu"`, through a native submit button associated
by `form`. Place the user-menu component outside other forms. It keeps an otherwise stateless page stateless and
processes a previously rendered sign-out action after session expiry. The default `signOut()` invalidates the Wicket
session, then `onSignedOut()` redirects to the home page. Override these hooks for application authentication and
redirect handling; no Spring Security dependency is introduced. The form rejects non-POST requests and requests
Wicket's Fetch Metadata isolation policy identifies as cross-site. `getSignOutForm()` exposes the form for application
integration.

Labels are Wicket resources: `SwUserMenu.account`, `SwUserMenu.signIn`, `SwUserMenu.signOut` and `SwUserMenu.unnamed`.
The **User menu** showcase (`/user-menu`) demonstrates both placements, Ajax name/picture changes and a session-backed
demo sign-in/out flow. The landing studio previews the same anatomy with browser-only demo actions.

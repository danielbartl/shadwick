# Shadwick landing page

A static, responsive website for GitHub Pages. Plain HTML, CSS and JavaScript; no site package installation, client framework or external runtime requests. Geist is self-hosted under its SIL Open Font License (`assets/fonts/OFL.txt`). The icon sprite uses Lucide shapes (ISC; see the root NOTICE).

The component studio uses **the library's compiled embedded CSS and component script**, with a custom green token palette. Skin selection loads the actual Vega or Lyra bundle. Light/dark mode applies to the studio and its overlays. The studio sidebar, header, breadcrumb and first-in-body skip link use core styles too. Demo forms, buttons, dialogs, sheets, dropdown menus, popovers, tooltips and toasts run locally in the browser; no information is submitted or persisted. The site explicitly distinguishes this static preview from Wicket's server-side behavior.

## Build and preview

From the repository root:

```sh
./mvnw -B -pl shadwick-core -am verify
node site/build.mjs
node site/check.mjs
node site/serve.mjs
```

Open **http://127.0.0.1:4173/shadwick/**. The preview deliberately uses a project subpath to catch asset URL problems before deployment. Stop it with Ctrl+C. An optional positional argument changes the port (`node site/serve.mjs 4174`).

If Node isn't on your PATH, use the Node installed by Maven: `./shadwick-core/target/node/node` instead of `node` (Windows: `shadwick-core\target\node\node.exe`).

`build.mjs` writes the ignored `site/dist/`, copying the two minified component bundles and `shadwick.js` from `shadwick-core/target/classes`. Never edit these copied assets or commit generated site output. Edit the page in `index.html`, presentation in `assets/site.css` and site interactions in `assets/site.js`. All asset URLs are relative.

## GitHub Pages

1. In the repository's **Settings → Pages**, select **GitHub Actions** as the build and deployment source.
2. Commit and push the site and the library sources it uses to `main`.
3. The **Website** workflow builds and tests the library, builds and checks the static site, uploads its artifact, and deploys it to the `github-pages` environment. Pull requests build and check the artifact without deploying. The workflow can also be run manually.

The default URL for this repository is `https://danielbartl.github.io/shadwick/`. The workflow derives canonical and social URLs from `GITHUB_REPOSITORY`. To use a custom domain, set `SITE_URL` in the website build step and configure the domain in GitHub Pages settings. See [GitHub's custom workflow documentation](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages).

## Design and checks

The page uses an ivory canvas, forest-green accents, Geist typography and a live component composition. Shadwick’s original four-leaf clover represents a lucky find and a welcoming developer experience, with a botanical nod to Shadleaf. The vector mark lives in `assets/logo.svg`, with the same artwork in the favicon and a one-color version in the icon sprite; the social card carries the same identity. A reduced-motion preference disables decorative animation. Navigation, skin/mode controls, code copying, field validation, menus and overlays work with a keyboard. The page remains readable with JavaScript disabled; interactive controls are progressively enabled.

`check.mjs` verifies IDs, local anchors, SVG symbols, fonts, required assets, license files and subpath-safe references. Before shipping visual changes, inspect desktop and narrow mobile layouts in a browser, try both skins and modes, test invalid form input, menu arrows/typeahead/Tab, toast Alt+T/dismissal/timer pausing, Escape and focus return on overlays (including the menu’s delete confirmation), and copy a code snippet. Browser testing should serve the built `dist/` at `/shadwick/`, as the preview does.

The studio includes the library sidebar: desktop icon rail, expandable navigation and a native mobile panel.
Its provider uses `data-sidebar-remember="false"`, so the preview stores no sidebar cookie.

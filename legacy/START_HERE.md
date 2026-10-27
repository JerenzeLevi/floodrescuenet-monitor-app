# FloodRescueNet Monitor — integrated source

This package contains the complete website with the integrated 3D prototype page.
It is ready for local preview. This integration has not been published to GitHub
or the live website.

## Contents

- `site/` — the complete website source, images, video, local JavaScript libraries,
  simulator, Vercel configuration and automated tests.
- `integration.patch` — the integration commit, for applying to an existing clone.
  This is optional; it is not a website asset.

Source commit: `dfbfd4cf4278f7ed75f8de764163d7dde9b5bc8d`
Base commit: `cfa16f6faa7dec18aef9f94ba333da82279d323b`
Original integration author and committer: AejoAbay <abayaenonojohn@gmail.com>
No co-author trailers are included. Existing team history and third-party license
notices retain their original attribution.

## Preview on Windows

1. Extract this ZIP first.
2. Open a terminal inside the extracted `site` folder.
3. If Python is installed, run:

```powershell
py -m http.server 8000 --bind 127.0.0.1
```

4. Open `http://localhost:8000/` in your browser.
5. Click **3D Prototype** in the navigation, or open
   `http://localhost:8000/simulator/` directly.
6. Press Ctrl+C in the terminal when finished.

On macOS or Linux, use `python3` instead of `py`. A static development server
with `site` as its root also works. Use a local server rather than double-clicking
an HTML file because navigation and asset links start at the website root.
No package installation or build command is required for the website itself.

## What is integrated

- A dedicated `/simulator/` page, linked from the homepage header, hero and footer.
- Shared desktop and mobile navigation, with links back to the homepage sections.
- The Monitor logo, Sora/Space Grotesk fonts and teal/cyan color palette.
- Mouse-drag rotation, side +/− zoom buttons and component inspection views.
- Automatic and manual deployment demonstrations, local water-level controls,
  threshold settings, and an activity log.
- A reset representing manual inspection and repacking.

The simulator uses local browser state. It is not connected to a mobile app,
Firebase, an ESP32, or a physical motor. Its geometry is illustrative and does
not validate rescue performance. This repository does not contain the mobile
application source.

## Apply the integration to your existing Git repository

Use this option if you want to keep the integration commit attributed to AejoAbay.
A source ZIP alone does not preserve Git commit history.

Start from a clean working tree in your existing local clone. Create a new branch
(if the name already exists, choose another), then set the repository-local identity:

```powershell
git switch -c feat/tabletop-simulator
git config user.name "AejoAbay"
git config user.email "abayaenonojohn@gmail.com"
```

Apply the included patch, replacing the example path with its actual extracted path:

```powershell
git am --3way "C:/path/to/FloodRescueNet-Integration/integration.patch"
git show -s --format=fuller HEAD
```

`git am` retains the patch author. The configured identity supplies the committer.
Verify that both fields show AejoAbay and the email above. The commit message has
no co-author trailers. If Git reports conflicts because the team changed these
files, resolve them before continuing; `git am --abort` cancels the attempted import.

Alternatively, copy the contents of `site/` into your existing clone, review the
diff, then commit with the repository identity above. The ZIP includes a complete
snapshot, so review any newer team changes before replacing files.

Publish the branch when GitHub access is available. Only the contents of `site/`
belong in the website root; keep this package guide and patch outside the deployed
website. The included `vercel.json` contains the simulator route and existing
security headers. Preview the deployment before merging into the live branch.

## Validation

Seven automated integration checks passed before packaging. To run them locally,
use Node 18+ from the `site` folder:

```powershell
node tests/simulator.test.cjs
```

These check navigation destinations, asset routes, script policy, mobile-menu
behavior, deployment/reset behavior, threshold validation and net geometry.
The integration still needs visual verification in a browser/Vercel preview;
the remote browser could not reach the local preview during preparation.

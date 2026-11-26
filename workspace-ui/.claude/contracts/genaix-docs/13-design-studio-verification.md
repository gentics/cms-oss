# Design Studio: verification

GenAIx checks a revision by rendering it, the way a visitor would see it, through the target's
preview portal. The check is a quality gate of the design modules (`08-workflow-modules.md` §7): a
blocking `script` gate the harness runs, never the model, at `step_end` of the build step and at
`before_release`. A `subagent` gate with vision adds advisory findings. The viewports and
thresholds below are the pilot's defaults, to be confirmed on the first real site.

## 1. What is rendered

GenAIx renders every page of the revision through the preview portal, in a pinned Chromium on
Linux, with fixed fonts and font-hinting settings, after the page's web fonts have loaded:

| Viewport (CSS px) | Device scale | States |
|---|---|---|
| 390 × 844 | 2 | Page as loaded; main menu opened |
| 768 × 1024 | 1 | Page as loaded |
| 1440 × 900 | 1 | Page as loaded |

Each viewport is rendered in **every language of the target**. Before rendering, GenAIx checks that
the portal serves the revision under test; an older one is a failure, not a comparison.

## 2. What is compared

The rendered portal page against the revision's baseline, which is the revision's own page as built
or imported:

- **Structure:** the normalised content DOM (headings, landmarks, links, the regions of the
  mapping and their order).
- **Layout:** the bounding boxes of the mapped regions, within a few pixels.
- **Navigation:** every navigation link resolves to a page of the target; the opened mobile menu
  shows the same entries.
- **Pixels:** a screenshot difference outside agreed masks (dates, rotating content).
- **Locked imports:** the hashes of the imported files (`12-design-studio-files.md` §3).

## 3. Verdicts

| Verdict | When |
|---|---|
| `pass` | Structure, layout and navigation match; locked files unchanged; no differing pixel outside the masks |
| `warn` | An approved font substitute, or a visual-only difference of at most 0.1 % of a screenshot's pixels on work that is not locked. The designer can acknowledge it (`03-genaix-api.md` §4.2) |
| `fail` | The portal serves another revision; a font or asset is missing; a locked file changed; a structural or navigation regression; a visual difference above the threshold |

The verdict and its findings arrive as `check.updated` (`<module>/chk-render-check`). The evidence,
`report.json` and `screenshots/`, is written by the GenAIx harness into the revision's `evidence/`
directory; the UI reads it there, and GenAIx's tool gate refuses the model's calls on any `evidence/`
path (`12-design-studio-files.md` §6).

## 4. Linux to Linux, and the designer's Windows screenshots

Text rendering differs between Windows and Linux (hinting, gamma, stem weight), so pixel equality
across operating systems is not possible. Pixel comparison therefore only compares renders made by
the same pinned browser and fonts. A designer's screenshots, mostly taken on Windows, go to the
advisory vision reviewer, which reports hierarchy, clipping and spacing problems as `warn` findings.
The reviewer can never turn a script `fail` into a pass. No Windows browser service is used.

## 5. Fonts

- The web fonts a source uses are kept in the revision's `fonts/` and served with it.
- For common Windows fonts the renderer has metric-compatible substitutes that keep line breaks and
  layout: Liberation Sans, Serif and Mono for Arial, Times New Roman and Courier New, Carlito for
  Calibri, Caladea for Cambria, Selawik for Segoe UI.
- **No Windows font file is ever shipped** in a GenAIx image; Microsoft's font licence forbids
  copying them to servers.
- For each key region the report names the requested font family and the font actually drawn
  (Chrome DevTools Protocol `CSS.getPlatformFontsForNode`), with the glyph count. A substitute is a
  `warn` that asks for a licensed web font; a font that is neither loaded nor substituted is a
  `fail`. A font is never replaced silently.

`report.json`, per page, viewport and language:

```json
{
  "revision_id": "r0004-7f3a", "page_key": "home", "language": "de", "viewport": "390x844@2",
  "portal_revision": "r0004-7f3a", "verdict": "warn",
  "fonts": [
    { "region_id": "rg-header", "requested": "Segoe UI", "drawn": "Selawik", "glyphs": 214, "status": "substituted" },
    { "region_id": "rg-teaser", "requested": "Inter", "drawn": "Inter", "glyphs": 388, "status": "loaded" }
  ],
  "findings": [
    { "severity": "warning", "region_id": "rg-header", "message": "Segoe UI is drawn as Selawik; provide a licensed web font." }
  ],
  "screenshot": "evidence/screenshots/home.de.390x844.png"
}
```

## 6. Safety of rendering

Rendering an imported site runs its JavaScript. The renderer runs inside the run sandbox, with no
CMS credential and no access to GenAIx's services, and reaches only the preview portal and the
asset hosts the page names. The runner image ships the pinned browser and the fonts above; today it
has no browser, which the build change adds.

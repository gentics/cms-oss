# Design Studio: where a design lives

The CMS is the single source of truth for a design: GenAIx stores no design metadata of its own
(24 Nov sync). A design target's design files live in its temporary devtools package, in a
**private file storage** that the CMS never serves by URL, and both the Design Studio UI and GenAIx
read and write them only through the authenticated CMS REST API. Devtools package synchronisation between the packages filesystem and
the CMS is never used, for any write, preview or promotion.

## 1. The CMS API used

The package file API (GPU-2677; on cms-oss `dev`, not yet in a release) with the private storage
`files-private` that Gentics adds for this (Gentics TODO G1, `10-design-studio.md` §5):

| Call | Purpose |
|---|---|
| `GET /rest/devtools/packages/{package}/files-private?path=design/…` | List a directory |
| `GET /rest/devtools/packages/{package}/files-private/design/{path}` | Read a file |
| `POST /rest/devtools/packages/{package}/files-private/design/{dir}` | Upload one or more files into one directory (multipart, one part per file) |
| `DELETE /rest/devtools/packages/{package}/files-private/design/{path}` | Delete a file |

There is deliberately **no static URL** for this storage. Today the API knows two storages and the
CMS serves both without a login: `files` at `/static/{package}/files/…`, meant for a site's public
frontend, and `files-internal` at `/internal/{package}/files/…` (verified on CMS 6.5, 2026-11-24).
Neither is a place for a customer's unpublished design.

Properties that shape the rules below: an upload overwrites with no version, lock or ETag; one
upload writes into one directory; the routes need devtools package rights, and the write routes
check nothing more than the read routes. Guidelines use the same API on `files-internal`
(`files-internal/guidelines/`). GenAIx reaches the design files through the design file tools of the
CMS MCP server (`15-design-studio-cms-and-si.md` §1), never through a filesystem.

## 2. Layout

```
packages/design-<target>/
  constructs/  templates/            the target's objects, added through the package REST API
  files-private/design/
    design.json                      the only file that changes: current revision and an index (§4)
    identity.json                    copies of shared objects and their originals (10 §3.3)
    base/                            the original package as it was when the target was created (14 §2)
    revisions/r0004-7f3a/            written once, never changed
      revision.json                  id, previous revision, writer, time
      pages/<page_key>.html          one file per page and language, e.g. home.de.html
      assets/  fonts/                stylesheets, scripts, images; the web fonts the pages use
      mapping.json                   regions and what each becomes in the CMS (§3)
      implementation/                the target's constructs, templates and portal implementation
                                     files as this revision uses them, in the form of base/ (14 §2)
      evidence/                      the render check: report.json, screenshots/ (13); written by
                                     the GenAIx harness only
    templates/<template>.json        companion metadata per template (§5)
```

A revision id is a sequence number and four random hex digits (`r0004-7f3a`), so two writers that
start a revision at the same time never write into the same directory. `implementation/` freezes
what the CMS objects were when the revision was written: templates and constructs have no history
in the CMS, so approving and promoting a revision always means this snapshot, never the target's
objects as they are later. A revision the UI writes changes only design files and has no
`implementation/` until GenAIx builds it into the CMS and verifies it.

`design.json`:

```json
{
  "target_id": "umdasch-2026-11",
  "source_node_id": 3,
  "node_id": 41,
  "base": "base/",
  "current_revision": "r0004-7f3a",
  "revisions": [
    { "id": "r0004-7f3a", "previous": "r0003-c21e", "status": "verified", "by": "genaix", "at": "2026-11-26T10:12:00Z" }
  ]
}
```

The index in `design.json` is a convenience: each revision's own `revision.json` names its previous
revision, so the history can always be rebuilt from the directory listing, even if an index update
was lost. The UI renders a page of a revision as described in §6: it loads the page and its assets
through the REST API and shows them in a sandboxed frame.

## 3. The mapping

`mapping.json` says what each region of each page becomes. A region is an element of a page with a
stable identifier.

```json
{
  "revision_id": "r0004-7f3a",
  "locked_files": { "pages/home.de.html": "sha256:9f1c…" },
  "regions": [
    { "region_id": "rg-header", "page_key": "*", "selector": "header.site-header",
      "kind": "template", "cms_ref": { "type": "template", "id": "88", "node_id": 41 } },
    { "region_id": "rg-main-nav", "page_key": "*", "selector": "nav.main",
      "kind": "navigation", "proposed_key": "nav_main", "source": "folder tree of the node" },
    { "region_id": "rg-teaser", "page_key": "home", "selector": "[data-gx-region=rg-teaser]",
      "kind": "construct", "cms_ref": { "type": "construct", "id": "17", "node_id": 41 },
      "field_bindings": { "headline": "h2", "image": "img@src", "link": "a@href" } },
    { "region_id": "rg-events", "page_key": "news", "selector": "section.events",
      "kind": "integration", "integration": "events", "operation": "list", "placeholder": true }
  ]
}
```

- `kind` is `static` (stays markup in the template), `construct`, `template`, `navigation` or
  `integration`.
- `cms_ref` names an existing CMS object; `proposed_key` a new one this revision creates.
- `field_bindings` map a construct's parts to where their values appear in the region.
- `integration` names a registered integration and its operation
  (`15-design-studio-cms-and-si.md` §2); `placeholder: true` when none provides the data yet.
- **Region identifiers stay the same across revisions** while the region exists, so comments and
  corrections keep pointing at it. In a page GenAIx builds, a region carries a `data-gx-region`
  attribute; in a locked import the `selector` alone identifies it and the files stay unchanged.

## 4. Two writers: the UI and GenAIx

Both write a design the same way:

1. Write a complete new revision directory under a fresh id (`revisions/r0005-2b91/…`), one upload
   per directory, `revision.json` naming the revision the edit started from.
2. Read `design.json` and check that `current_revision` is still the revision the edit started from.
3. Only then upload `design.json` with the new revision as current.
4. Read `design.json` again; if it names another revision, the update was lost to a concurrent
   writer.

A revision directory is never changed after it is written, so a half-written revision is never
current and every earlier revision stays readable. If step 2 or 4 finds another current revision,
the writer does not move the pointer again: GenAIx reports the conflict and builds again on top of
the current revision; a client's own request to GenAIx that names an outdated revision gets
`409 design_revision_conflict`. A revision whose update was lost is not gone: it is still in
`revisions/`, and two revisions with the same previous one show the fork.

**Limit.** The package file API has no conditional write, so this narrows the race between steps 2
and 3 and detects a lost update, but does not prevent it. A conditional upload (`If-Match` on an ETag,
or "create only if absent") is on the Gentics TODO list (`10-design-studio.md` §5, G3); with it step 3 becomes
atomic and step 4 goes.

The UI tells GenAIx about a revision it wrote with a message naming it as a `design_revision`
reference; GenAIx's next run starts from the current revision in either case.

## 5. Companion metadata after promotion

A promoted template keeps a companion file in the **original** package,
`files-private/design/templates/<template>.json`: its regions, what each maps to and its field
bindings, taken from the promoted revision's mapping. Today a template says nothing about what each
part was designed for; with the companion file a later Design Studio session, or GenAIx, starts
from the same region identifiers instead of decomposing the template again.

## 6. Security

- **Rendering in the UI.** Design files can hold imported HTML and JavaScript. The Design Studio UI
  reads a revision's page and the assets it references through the REST API, with the designer's
  session, turns each asset into a `blob:` URL and rewrites the page's references to them, and shows
  the result **only** in an `<iframe sandbox="allow-scripts">` without `allow-same-origin`. The page
  then runs with an opaque origin: it cannot read the Workspace, the CMS's cookies or storage, or call
  the CMS REST API with the designer's session. The UI never opens a design file as a top-level page.
- **Rendering in GenAIx.** Only inside the run sandbox, without credentials
  (`13-design-studio-verification.md` §6).
- **Evidence.** A revision's `evidence/` is written by the GenAIx harness after its render check.
  GenAIx's tool gate refuses the model's design file calls on any `evidence/` path, read or write, as
  it keeps all gate evidence away from the model (`08-workflow-modules.md` §7.5). The UI reads it.
- **Confidentiality.** Design files are drafts of a customer's site, so they are reachable only
  through the authenticated REST API. Two CMS tasks make that true (`10-design-studio.md` §5): the
  private storage (G1), and making sure the package URLs that are served cannot reach other folders
  of a package (G2). **Until both ship,** the pilot keeps the same layout under `files-internal/design/`
  and stores only designs of non-confidential sites there, since that storage is served without a
  login.

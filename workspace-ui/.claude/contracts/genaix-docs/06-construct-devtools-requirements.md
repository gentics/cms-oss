# Construct and devtools API requirements

This document covers construct (tag type) creation through the CMS REST API: how the requirement
to create constructs together with their Handlebars templates is resolved, the REST call
sequence for a Tier 1 construct, the boundary between what REST can do and what remains
filesystem-only, the validation and similarity checks that run before creation, and the
requirements this places on the CMS.

## 1. Scope and resolution

Constructs must be creatable together with their Handlebars templates, replacing a previous
filesystem-based synchronization approach that pushed generated files directly onto the CMS's
filesystem and triggered an import. A separate question is whether construct implementation
assets — JavaScript and CSS — need a CMS-side delivery mechanism as well; these resolve
differently.

**Templates** — the Handlebars template inside a construct's part — are a plain REST value (the
template text is a part's default string value, §2) and are already available through the
existing construct REST API. No CMS change is needed for this half of the requirement.

**JavaScript/CSS** are construct *implementation* assets, a portal-side rendering concern rather
than a CMS content concern. The devtools package directories that could theoretically hold such
files are scoped to CMS-internal edit-mode assets, not portal implementation, and have no REST
surface at all (§2.6). This half is out of the current scope, not because it is blocked, but
because a Tier 1 construct's visual output is server-rendered HTML from its Handlebars template,
dropped into the existing page-content field, requiring no separate asset pipeline (§4). How
portals receive frontend assets in the future is left to later architectural work.

So the requirement is satisfied as: templates are supported now via REST; JavaScript/CSS delivery
is deliberately deferred and not required for a Tier 1 construct. Handlebars is the only template
engine in scope for generated constructs; no other template part type is used.

## 2. Verified facts

### 2.1 Construct and part shape

A construct (REST name) is a tag type (devtools/editor name): an identity (id, global id,
keyword, name, description), behavior flags (subtag rules, auto-enable, editor control style,
edit-on-insert, live editor tag name, external editor URL), a category reference, and a list of
parts. Each part carries a keyword, name, `typeId` (the authoritative field), a string type
mirror with no numeric mapping documented in the public REST schema, editable/mandatory/hidden
flags, part order, an optional regex, select/overview settings, and a default property object
carrying the actual default value (a string value for text-shaped types).

### 2.2 Part type discovery, the typeId table, and datasources

The REST schema's `type` enum (`STRING`, `RICHTEXT`, `BOOLEAN`, `FILE`, `IMAGE`, `SELECT`,
`OVERVIEW`, `TEMPLATETAG`, and others) has no numeric-to-string mapping documented in the schema
itself — no distinct `HANDLEBARS` literal exists. Rather than hard-coding which numeric typeIds
exist, a construct-creation tool discovers them at runtime: `GET /parttype` (optionally filtered
by a `q` search term) returns the full list of part types known to the CMS, each as
`{ id, name, description, auto, deprecated }`. The read tool `list_part_types` wraps this
endpoint and filters its result against an **installation-level allowlist** — configuration on
the CMS side, not a value baked into the tool — that restricts which typeIds may be offered for
an AI-generated construct's editable parts. The default allowlist is the 15 ordinary editor part
types: typeIds 1, 2, 3, 4, 6, 8, 9, 10, 21, 25, 29, 30, 31, 38, and 39.

The ground truth for what each typeId means and which property type it reports is the
server-side mapping (`Property.java`):

| typeId | Meaning | Reported property type | Status |
|---|---|---|---|
| 1 | Text | `STRING` | Allowed by default |
| 2 | Text/HTML | `RICHTEXT` | Allowed by default |
| 3 | HTML | `RICHTEXT` | Allowed by default |
| 4 | URL (page) | `PAGE` | Allowed by default |
| 6 | URL (image) | `IMAGE` | Allowed by default |
| 8 | URL (file) | `FILE` | Allowed by default |
| 9 | Text (short) | `STRING` | Allowed by default |
| 10 | Text/HTML (long) | `RICHTEXT` | Allowed by default |
| 21 | HTML (long) | `RICHTEXT` | Allowed by default |
| 25 | URL (folder) | `FOLDER` | Allowed by default |
| 29 | Select (single) | `SELECT` | Allowed by default; requires a `datasource_id` (below) |
| 30 | Select (multiple) | `MULTISELECT` | Allowed by default; requires a `datasource_id` (below) |
| 31 | Checkbox | `BOOLEAN` | Allowed by default |
| 38 | URL (file) | `FILE` | Allowed by default |
| 39 | URL (folder) | `FOLDER` | Allowed by default |
| 13 | Overview | `OVERVIEW` | Out of the current scope |
| 32 | Datasource | `DATASOURCE` | Out of the current scope |
| 33 | Velocity | `VELOCITY` | Out of the current scope (legacy template engine) |
| 34 | Breadcrumb | `BREADCRUMB` | Out of the current scope |
| 35 | Navigation | `NAVIGATION` | Out of the current scope |
| 41 | Form | `FORM` | Out of the current scope |
| 42 | CMS Form | `CMSFORM` | Out of the current scope |
| **43** | **Handlebars** | **`RICHTEXT`** | **Reserved — the construct's own template part only, never offered as an editable-part choice** |

A handful of older or custom-form-only typeIds (legacy editors, list types, select variants that
also report `STRING`, and similar) exist in the schema but fall outside both the default
allowlist and the explicit out-of-scope list above; they are not addressed by this document and
are excluded from `list_part_types`'s filtered result by the same allowlist mechanism.

**typeId 43 is the Handlebars part.** It is not a distinct value in the REST type enum; the
Handlebars part type extends the general text part type and reports its property type as
`RICHTEXT`. The template source text lives in the part's string value, exactly like any
text-based part. An on-disk filename convention (`part.<keyword>.hbs`) exists only inside the
devtools filesystem synchronizer — the REST API itself has no `.hbs` concept, only a string
value.

**Inside the template a part is addressed as `cms.tag.parts.<keyword>`** — `{{ cms.tag.parts.headline }}`,
`{{{ cms.tag.parts.body }}}` for HTML. That is the one form the CMS Handlebars part type resolves;
the shorter `tag.parts.<keyword>` and a bare `{{keyword}}` render nothing, silently. Every example
in this kit, the `create_construct` schema and the S3 transcript use the full form (`1.0.0-rc.2`;
earlier revisions of this document wrote the short form). The template part itself is never listed
among a construct's editable `parts` in a tool call or a client-rendered draft: `create_construct`
takes it as `handlebarsTemplate` and builds the type 43 part.

**Select parts and datasources.** A select or multiselect part (typeId 29 or 30) must reference
an existing datasource via `selectSettings.datasourceId`; the CMS exposes `GET /datasource`
(paged, searchable list) and `GET /datasource/{id}` for discovery, with each datasource reported
as `{ id, globalId, type: STATIC|SITEMINDER, name }`. The read tools `list_datasources` and
`get_datasource` wrap these endpoints. Although `POST /datasource` exists in the REST API,
**creating a new datasource is out of the current scope**: a construct using a select part must
reference one of the datasources that already exist on the installation, discovered through
these two read tools, never created on the fly.

### 2.3 One-call create with node assignment

A single `POST` to the construct endpoint, with one or more target node ids as query parameters,
creates the construct, its parts — including a Handlebars part with the template in its default
string value — and assigns it to every given node, in one transaction, one HTTP call. The node id
parameter is required. There is no filesystem step and no package staging: the write commits
directly to the database and is immediately visible to a subsequent read.

### 2.4 Full-replacement update

Updating a construct takes the same construct body, with node ids again as query parameters, now
as a full desired-state list: the CMS diffs the node's current construct assignments against the
supplied list and adds or removes accordingly; omitting the parameter leaves assignments
untouched. Updating the parts list replaces the whole array — there is no partial update. An
incremental edit must read the construct, merge the change client-side, then write the full list
back.

### 2.5 Categories by id only

A category reference on write must point to an already-existing category; the create/update path
does not resolve or auto-create a category by name. A new category needs a separate create-category
call first — two calls minimum if a new category is wanted.

### 2.6 No REST surface for package-level static assets

The devtools package directories that could hold static files are plain, static directories
served outside the REST API entirely — no upload, list, or delete route exists for either. One is
scoped to CMS-internal edit-mode assets (custom tag or property editors), explicitly not for
frontend implementation assets; even the more general-purpose directory is not a deployment
mechanism for a separate frontend — making its contents available to a portal is a step outside
the CMS. Package-level JavaScript or CSS delivery must go through this filesystem mechanism
regardless of how the construct record itself was created.

### 2.7 Comparison with the previous synchronization approach

The previous approach wrote a devtools-package folder — a structure file, a template file, and
sibling part-value files — onto the CMS's filesystem, then triggered an import, because no REST
path existed for construct creation at the time. A single create call now does the same content
creation in one database transaction, with no filesystem step — REST is a strict functional
superset for construct content and template creation. Two things are not replicated:
git-versionable file artifacts matching an export/import review loop (a workflow property, not a
CMS capability REST lacks — an already-created construct can still be exported to files
afterward), and package-level static-asset management, which has no REST equivalent (§2.6) and
was not something the previous approach handled either. Neither approach solves editor refresh
after creation: the editor's construct list loads once per session with no live push, so a
freshly created construct may not appear in an already-open editor without a manual refresh (§7).

**Net**: writing a template into a CMS-visible construct and having the editor render it is fully
reproducible with plain REST. Package-level static-asset delivery is not, which is exactly the
half of the original requirement treated as out of scope for now.

## 3. Exact REST call sequence

Worked example: a construct with a Handlebars wrapper template and two editable parts — a
short-text headline and a rich-text body.

### 3.1 Create construct with Handlebars template and two editable parts

```
POST /rest/construct?nodeId=3
Content-Type: application/json
```
```json
{
  "keyword": "legalnote",
  "nameI18n": { "en": "Legal note", "de": "Rechtlicher Hinweis" },
  "descriptionI18n": {
    "en": "Highlighted legal notice box with a headline and rich-text body.",
    "de": ""
  },
  "mayBeSubtag": true,
  "mayContainSubtags": false,
  "autoEnable": true,
  "editorControlStyle": "ABOVE",
  "categoryId": 7,
  "parts": [
    {
      "keyword": "template",
      "typeId": 43,
      "editable": false,
      "hidden": true,
      "partOrder": 1,
      "nameI18n": { "en": "Template", "de": "Template" },
      "defaultProperty": {
        "type": "RICHTEXT",
        "stringValue": "<div class=\"legal-note\"><h4>{{ cms.tag.parts.headline }}</h4><div class=\"legal-note__body\">{{{ cms.tag.parts.body }}}</div></div>"
      }
    },
    {
      "keyword": "headline",
      "typeId": 9,
      "editable": true,
      "liveEditable": true,
      "mandatory": true,
      "partOrder": 2,
      "nameI18n": { "en": "Headline", "de": "Überschrift" },
      "defaultProperty": { "type": "STRING", "stringValue": "Legal note" }
    },
    {
      "keyword": "body",
      "typeId": 21,
      "editable": true,
      "liveEditable": true,
      "partOrder": 3,
      "nameI18n": { "en": "Body text", "de": "Text" },
      "defaultProperty": { "type": "RICHTEXT", "stringValue": "<p></p>" }
    }
  ]
}
```

`typeId: 9` suits a single-line headline; `typeId: 21` gives an editable rich-text body field,
distinct from the `typeId: 43` template part holding the non-editable wrapper (§2.2). Both 9 and
21 are in the default allowlist, so an agent would discover them through `list_part_types` (§2.2,
§5.1) rather than assume them. The category referenced by `categoryId` must already exist (§2.5);
if not, run §3.1a first. The response returns a complete, live construct — usable immediately,
with no further step.

### 3.1a (if needed) Create the category first

```
POST /rest/construct/category
{ "nameI18n": { "en": "Legal", "de": "Recht" } }
```
The response's category id becomes the `categoryId` used in §3.1.

### 3.2 Assign to an additional node

If node 3 was the only node in §3.1 and the construct is later needed on node 5 too:

```
PUT /rest/node/5/constructs/42
```
(construct id 42 from the §3.1 response). An equivalent bulk form exists for linking or unlinking
many constructs and nodes at once.

### 3.3 Update the template

Full-replacement semantics (§2.4) — read first, mutate the template part, write the whole array
back:

```
GET /rest/construct/42
```
```json
PUT /rest/construct/42?nodeId=3
{
  "keyword": "legalnote",
  "nameI18n": { "en": "Legal note", "de": "Rechtlicher Hinweis" },
  "categoryId": 7,
  "parts": [
    {
      "keyword": "template",
      "typeId": 43,
      "editable": false,
      "hidden": true,
      "partOrder": 1,
      "nameI18n": { "en": "Template", "de": "Template" },
      "defaultProperty": {
        "type": "RICHTEXT",
        "stringValue": "<div class=\"legal-note legal-note--warning\"><h4>{{ cms.tag.parts.headline }}</h4><div class=\"legal-note__body\">{{{ cms.tag.parts.body }}}</div></div>"
      }
    },
    { "keyword": "headline", "typeId": 9, "editable": true, "liveEditable": true, "mandatory": true, "partOrder": 2, "nameI18n": { "en": "Headline", "de": "Überschrift" } },
    { "keyword": "body", "typeId": 21, "editable": true, "liveEditable": true, "partOrder": 3, "nameI18n": { "en": "Body text", "de": "Text" } }
  ]
}
```
The headline and body parts are resent unchanged — omitting them would delete them, per the
full-replacement semantics in §2.4.

### 3.4 Add to a devtools package

```
PUT /rest/devtools/packages/genaix-poc/constructs/legalnote
```
Adds the already-existing construct, by keyword, to the named package. This is packaging and
export bookkeeping, not a way to create a construct — a devtools package cannot be used as the
sole persistence target: creating new template content still requires §3.1 first.

### 3.5 Render a preview of a page using the construct

Assuming a page (id `789`) on node 3 already carries a `legalnote` tag:

```
GET /rest/devtools/preview/page/789?nodeId=3
```
Returns raw HTML — a direct server-rendered snapshot, bypassing the editor's own inline-editing
frame entirely, the recommended preview path for a non-editor consumer such as an AI assist
surface. In-editor preview also works unchanged: the Handlebars part's rendering runs server-side
and forces its own output to read-only even inside inline edit mode, so that output is never
inline-editable, while the sibling headline/body parts get normal inline-edit affordances.

## 4. Tier 1 vs Tier 2

- **Tier 1 — inline Handlebars construct** (this document's worked example): renders HTML into
  the existing page-content rich-text field. No content-repository schema change, no tagmap
  entry, no repair, no portal template or fragment edit — fully live immediately after creation,
  aside from the editor-refresh gap (§2.7), which is exactly what §3.1's single call produces.
- **Tier 2 — structured-field construct**: a part's value is meant to surface as its own field in
  a downstream content repository, requiring a new mapping entry, a content-repository repair
  (schema migration, not instant), a republish, and, for some frontends, a template edit. None of
  this is a single call.

**The current scope is Tier 1 only.** A Tier 2 part is still creatable through the identical
create call — a structured field is "just another part" — but making it live as a distinct,
queryable field needs the non-automatic repair, republish, and portal-edit sequence. A
construct-field mapping tool may exist for this, but is deliberately human-gated: it may create a
mapping entry if a suitable REST route is confirmed, but never triggers the repair itself,
returning a pending-repair status instead — avoiding an agent silently kicking off a non-instant
schema migration.

## 5. Tools for construct creation

Some of these tools wrap a CMS REST endpoint directly; others run entirely on the GenAIx side and
never reach the CMS. Each is marked accordingly.

### 5.1 Part type and datasource discovery

Three read tools wrap CMS endpoints (§2.2):

- **`list_part_types { q?: string }`** — wraps `GET /parttype`, filtered against the
  installation's allowlist, returning only the typeIds an AI-generated construct may use for an
  editable part. Replaces any hard-coded list of permitted part types in the tool itself.
- **`list_datasources { q?: string, page?: int, page_size?: int }`** — wraps `GET /datasource`,
  for finding an existing datasource to reference from a select or multiselect part.
- **`get_datasource { id: string }`** — wraps `GET /datasource/{id}`, returning one datasource's
  details for confirmation before use.

These three are read-only; none of them, nor any other tool in this document, creates a new
datasource — that is out of the current scope (§2.2).

### 5.2 Template validation

Not a CMS REST call. Runs entirely on the GenAIx side, before construct creation or update ever
reaches the CMS:

```
validate_handlebars_template(template: string) -> {
  valid: bool,
  issues: [{ line: int, column: int, message: string }],
  helpers_used: string[],
  forbidden_patterns_found: string[]
}
```

- **Compiles** the template in strict mode with a standard Handlebars implementation, catching
  syntax errors before they reach the CMS, where a broken template would otherwise only surface
  at page-render time.
- **Known CMS helpers**: the CMS's Handlebars runtime exposes helpers beyond stock Handlebars,
  including one to render a nested tag or part and one to resolve a localized string; the
  validator's allowlist must include these, and the full, authoritative helper list must be
  confirmed against the CMS (§7).
- **Forbidden patterns**: reject templates containing script tags, inline event-handler
  attributes, or unescaped triple-stash output of anything other than the construct's own
  declared parts — a conservative allowlist for AI-generated Tier 1 content, not a
  general-purpose sanitizer.

### 5.3 Similarity check before create

Before creating a new construct, the agent lists existing constructs and runs a similarity pass
comparing: name/keyword similarity (normalized string distance, catching near-duplicates);
template similarity (structural comparison against existing Handlebars part values, ignoring
whitespace and attribute-order differences); and, if the optional construct-keyword index
attribute described in the Content RAG contract is added, keyword-tag similarity via search —
an aggregation by that attribute shows how often a similar construct is already used across
content. If a close match is found, the agent surfaces it for confirmation before creating — the
same "avoid duplicates" principle applied to pages in the Content RAG contract.

## 6. Known limitations

1. No numeric typeId is documented in the public REST schema (§2.2) — the reported property type
   for each typeId still relies on the table in §2.2, even though which typeIds are offered to an
   agent is now resolved at runtime through `list_part_types` and the installation's allowlist.
2. A category cannot be created by name in the same call as construct creation (§2.5).
3. Updating a construct requires the full parts array every time (§2.4).
4. No REST route exists for a construct or part icon.
5. The editor's construct-list caching gap (§2.7) applies regardless of creation path.
6. Package-level static assets remain out of reach of any REST-based construct workflow (§2.6).

## 7. Requirements towards the CMS

1. Confirm there is genuinely no REST field for a construct or part icon; if so, define how an
   AI-generated construct should set an icon without the devtools filesystem.
2. Confirm whether updating a construct truly requires the full parts array every time, or
   whether an undocumented partial-update behavior exists worth relying on.
3. Publish and maintain the authoritative typeId-to-property-type table (§2.2) and the
   CMS-specific Handlebars helpers, so both can be hard-coded into tool validation where
   `list_part_types` does not already cover them.
4. The installation-level allowlist restricting which part types `list_part_types` returns is
   CMS configuration, not a GenAIx-side setting: confirm the default value (the 15 editor part
   types in §2.2) and how an installation can adjust it.
5. Define the intended mechanism, if any, for a Tier 2 field mapping's content-repository repair
   to be requested programmatically, versus always routed through a human administrative step.
6. Confirm whether any REST route is planned for package-level static assets, since today's
   answer is filesystem-only.
7. Confirm the devtools preview endpoint is available on any targeted deployment, and define an
   editor live-refresh mechanism — a construct-list invalidation event the editor can subscribe
   to — so a newly created construct appears without a manual reload.
8. Define the CMS permission that gates construct and category creation, so the MCP server can
   correctly omit or reject these tools for users without the appropriate role.

## 8. What the Chief Content Creator role may create versus developer work

The Agentic Workspace is for content creation; a separate, more technical surface is where
implementation work happens. This splits tag types into two classes: **content tag types** (this
document's scope) — Tier 1 Handlebars constructs assembled from existing, simple part types that
render into page content, created by the Chief Content Creator role through the Workspace
interface, mediated by the agent and the tools in §5, requiring no content-repository schema
knowledge, portal code, or devtools package literacy; and **base/implementation tag types** (out
of the current scope) — Tier 2 structured-field constructs, new content-repository field
mappings, schema changes, portal template or fragment code, and package-level static assets
(§2.6), which remain developer work requiring further tooling. The tools and call sequences
defined here are intended to extend forward into that later work rather than be superseded by it.

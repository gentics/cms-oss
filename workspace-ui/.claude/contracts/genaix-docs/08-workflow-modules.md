# Workflow modules and quality gates

A workflow in GenAIx is not a state machine the model interprets. It is a **module**: a directory of
data (manifest, prompts, skills) plus a small amount of code (gate scripts and reviewer
definitions), loaded and executed by the GenAIx harness. The model never reads the manifest, never
decides which gates run, and never decides whether a gate passed.

Since `1.0.0-rc.4` a module carries out **plans**, not sessions. A session is one
conversation; the built-in **session agent** reads and answers in it and drafts a plan when the
user asks for a change, and every plan runs under one workflow module with its own steps, checks,
gates, derived settings, previews, publications and lifecycle (`Plan.status`). Where this document
says "the plan", it means the plan a module's runs work for; before `1.0.0-rc.4` that was the
session as a whole.

Read alongside `03-genaix-api.md` §6 (event model) and §8 (the workflow view) for the wire shapes,
`04-mcp-scope.md` §3–§5 for tool groups and the scenario coverage matrix,
`05-content-rag-contract.md` §5 for the retrieval skill, and
`06-construct-devtools-requirements.md` §5 for the two GenAIx-side construct checks that become
gates here.

## 1. Principles

1. **Modules are data plus small code; the harness owns execution.** Whatever can be declared is
   declared in `manifest.yaml`; whatever must compute is a gate script or a reviewer prompt. There is
   no workflow DSL, no branching language and no expression evaluator.
2. **Gates are enforced by the harness, not by the model.** A verdict comes from code the harness ran
   or an agent it spawned, and blocking comes from the manifest. A model self-report can never set
   `blocking: true`.
3. **The model reaches the workflow rail only through harness tools** — `report_step`, `update_plan`,
   `report_check`, `request_interaction`, `write_artifact`, `set_derived_settings`, and since
   `1.0.0-rc.4` `draft_plan` and `recall` — in-process MCP tools under the `genaix` namespace,
   validated before they reach the event log (§6.3).
4. **The tool surface is the module's, narrowed by the user's permissions** (§6.2), and neither half
   is widenable by the model. The session agent's surface is core: read tools only.
5. **Modules are versioned and pinned.** `workflow_version` is stored on the plan when it is
   drafted (on the session before `1.0.0-rc.4`), so a module upgrade never changes a plan already
   in flight; the next plan of the same conversation uses the module then loaded.
6. **Gates fail closed.** A blocking gate that times out, crashes or returns an unparseable verdict
   has failed. Silence is never success.
7. **Nothing in a module is user-authored at runtime.** Modules ship with the deployment and are
   reviewed like any other code. The agent cannot create, edit or disable a gate.
8. **A module tightens, it never loosens** (`1.0.0-rc.4`). The core rules — only the user accepts
   a plan, a write must target an object of the executing plan, reading is free, the session agent
   never writes — are enforced by the harness. A module declares and tightens on top of them
   (`plan_policy`, the repo-wide `defaults.yaml`); the loader refuses one that would loosen them
   (§5).

What this buys: the verbatim-text requirement stops being a promise in prose and becomes a sha256
comparison the harness performed, with both digests in the event log. The same mechanism covers link
existence, construct allowlists, Handlebars compilation and preview rendering.

## 2. Module layout on disk

Modules live in the **workflows repository**, deployed under a configured directory,
`GENAIX_WORKFLOWS_DIR`, default `<app_root>/workflows/`. One top-level directory per module, named
by the module id, plus a few repo-wide files.

```
workflows/
  defaults.yaml                 repo-wide plan_policy defaults every module inherits (rc.4)
  POLICY.md                     the invariants and core rules, for module authors; never loaded
  CLAUDE.md                     development guide for module authors and their coding
  .claude/                      agents (rules/, skills/); never loaded by GenAIx
  session/
    manifest.yaml               the session agent: id, version, title, model, skills
    prompts/system.md           the session agent's prompt fragment
    skills/choose-workflow.md   e.g. how to pick a module for a new plan
  content_create/
    manifest.yaml               the whole declaration; the only required file besides system.md
    genaix.yaml                 optional harness-side knobs the manifest schema does not carry
    prompts/
      system.md                 module system prompt fragment
      step-st-5.md              optional per-step prompt fragment, keyed by step id
      release.md                optional prompt used when the plan is released
      release_self_check.md     prompt gate: the extra turn it injects
    skills/
      verbatim-handling.md      frontmatter name + description, markdown body
      construct-selection.md
    gates/
      verbatim_identical.py     script gate; argv in, JSON verdict on stdout
      references_exist.py
      constructs_allowed.py
      draft_unpublished.py
      guideline_compliance.md   subagent gate: reviewer system prompt
      guideline_compliance.yaml subagent gate: tools, verdict schema, limits
    tests/
      fixtures/happy-path.yaml  scripted conversation with canned MCP responses
      fixtures/verbatim-drift.yaml
      gates/test_verbatim_identical.py
      replay/s1-content-create.expected.yaml
  tests/                        repo-wide conformance tests (§9)
```

Conventions the loader enforces:

- The directory name equals `manifest.yaml`'s `id`, which matches `^[a-z][a-z0-9_]{2,39}$`. Ids are
  public API (`Plan.workflow`, `GET /workflows`) and freeze on release.
- A `script` gate has exactly one file, `gates/<gate id>.py` or `.sh`. A `subagent` gate has
  `gates/<gate id>.md` and `gates/<gate id>.yaml`. A `prompt` gate has `prompts/<gate id>.md`; a
  `human` gate needs no file.
- `prompts/system.md` is required; everything else is optional.
- **GenAIx loads an allowlist, not a directory** (since `1.0.0-rc.4`). It reads only top-level
  directories holding a `manifest.yaml`, and inside them only `manifest.yaml`, `genaix.yaml`,
  `prompts/*.md`, the `skills/<name>.md` files the manifest declares and its `gates/` directory
  (the declared gate files and the helpers they import, such as `gates/cms_client.py`, which a gate
  sees read-only in its sandbox); plus the repo-wide `defaults.yaml`. Nothing else is read, however it is named:
  `CLAUDE.md`, `AGENTS.md`, `.claude/**`, `README.md`, `POLICY.md`, `tests/`, and any directory
  whose name starts with `.` or `_`. A module's `tests/` run in the workflows repo's CI, never in
  GenAIx.
- Files are read once at startup and held in memory. Nothing is written into a module directory at
  runtime, and it should be mounted read-only.

**Module `session`** (`1.0.0-rc.4`). The session agent, which replaces the `free_chat` module, is
built in: its powers are core (every read group of the installation's connectors, `recall`,
`draft_plan`, `request_interaction`; never a CMS write tool, §6.2). What the workflows repo
supplies is its voice: `session/prompts/system.md`, its `skills`, and its `model`. Its manifest
is validated against the same schema, and the loader refuses one that declares `quality_gates`, a
non-empty `steps_template`, `plan_approval`, `plan_policy` or a write group: those belong to plans,
and the session agent has none. `session` is not a catalogue entry (`GET /workflows` does not list
it, `capabilities.workflows` does not name it) and cannot be named in `SessionCreate.workflow` or
`draft_plan`.

**`defaults.yaml`** (`1.0.0-rc.4`) holds the repo-wide `plan_policy` defaults every module
inherits (§3, §4). A module's own `plan_policy` may only tighten them; §5 has the rule.

**The development guide.** The repo may carry its own guide for the people and coding agents who
write modules: `CLAUDE.md` (conventions, layout, the invariants and core rules in short, how to run
the conformance tests), `.claude/rules/` (manifest, prompts, gates, `plan_policy`) and
`.claude/skills/` (for instance *new module*, *add a gate*, *bind write targets*, *replay a
transcript*). It is development material only. GenAIx never loads it (the allowlist above), the
Claude runtime of a run loads no settings sources and works outside the modules directory, and the
images carry the modules without it. A skill a module offers its own agent stays in the module's
`skills/`, under a different path and loader.

## 3. The manifest, by example

`workflows/content_create/manifest.yaml`. Four of its eight quality gates are shown, one per kind
this module uses; §8.1 lists the full set:

```yaml
id: content_create
version: 1.0.0
title: Create content from source material
description: >-
  Turn an uploaded document into an unpublished CMS page draft: search existing content to avoid
  duplicates and find pages to link, derive folder, template and language, use only constructs
  the template allows, keep designated passages character-identical, then release for review.
scenario: S1
roles: [content_creator, chief_content_creator]
optional: false
produces: [page]
plan_approval: true

plan_policy:
  concurrency: exclusive
  on_discard: keep
  write_targets:
    create_folder: { object: folder, from: parentFolderId, creates: folder }
    create_page: { object: folder, from: folderId, creates: page }
    upload_file: { object: folder, from: folderId, creates: file }
    update_page_tags: { object: page, from: pageId }
    add_page_tag: { object: page, from: pageId }
    update_page_properties: { object: page, from: pageId }

entry_hints:
  - create a new page from an uploaded document
  - write a landing page, news item or FAQ page
  - turn source material into a page draft

inputs_schema:
  type: object
  additionalProperties: false
  required: [node_id]
  properties:
    node_id:
      type: integer
      description: CMS node the page is created in.
    folder_id:
      type: integer
    language:
      type: string
      pattern: "^[a-z]{2}(-[A-Z]{2})?$"
    verbatim_mode:
      type: string
      enum: [off, marked_spans, whole_file]
      default: marked_spans
    guidelines:
      type: array
      items: { type: string }

required_connectors:
  - type: cms
    optional: false

tool_groups:
  cms: [identity, search, content_read, content_write, constructs]
  genaix: [harness, session_files]

tool_denylist:
  - mcp__cms__delete_page
  - mcp__cms__take_offline
  - mcp__cms__restore_page_version
  - mcp__cms__publish_page
  - mcp__cms__translate_page
  - mcp__cms__create_construct
  - mcp__cms__update_construct
  - mcp__cms__ensure_construct_category
  - mcp__cms__assign_construct_to_nodes
  - mcp__cms__add_construct_to_package
  - mcp__cms__request_tier2_field_mapping

steps_template:
  - id: st-1
    label: Read source material
  - id: st-2
    label: Derive folder, template and language
  - id: st-3
    label: Search existing content
  - id: st-4
    label: Propose plan
  - id: st-5
    label: Create page draft
  - id: st-6
    label: Verify and preview

model:
  default: claude-opus-5
  effort: high
  reviewer:
    model: claude-sonnet-5
    effort: medium

skills:
  - cms-content-search
  - verbatim-handling
  - construct-selection

quality_gates:
  - id: no_duplicate
    label: No duplicate page on this topic
    kind: script
    blocking: false
    when: [step_end]
    steps: [st-3]
    description: Similarity score of the closest existing page stays under the threshold.
    inputs:
      threshold: 0.82
      types: [page]
    timeout_seconds: 20
    on_error: warn

  - id: verbatim_identical
    label: Verbatim passages are character-identical
    kind: script
    blocking: true
    when: [step_end, before_release, before_publish]
    steps: [st-5, st-6]
    description: >-
      Each designated source passage is byte-identical to the text stored in the target tag,
      proven by sha256 over NFC-normalised UTF-8.
    inputs:
      unit: chars
      max_deviations_reported: 20
    timeout_seconds: 30
    allow_mcp:
      cms: [content_read]
    source:
      type: requirement
      id: verbatim
      label: Verbatim source material

  # constructs_allowed, references_exist, draft_unpublished and alt_text follow the same shape;
  # section 8.1 lists all eight gates of this module with their triggers and pass criteria.

  - id: guideline_compliance
    label: Editorial guideline review
    kind: subagent
    blocking: false
    when: [before_release]
    description: >-
      A reviewer agent reads the rendered draft and the applicable guidelines and reports
      concrete, located findings.
    timeout_seconds: 180
    retries: 1
    on_error: warn

  - id: release_self_check
    label: Agent self-check before release
    kind: prompt
    blocking: false
    when: [before_release]
    description: The agent restates what it changed and what it is unsure about.
```

Five things to read out of that example. `plan_approval: true` makes the plan's proposing run stop
at the proposed plan; the plan is carried out only once the user accepts it with their next
`execute` message (§6.5). `required_connectors` names connector **types**, not URLs: a module
says it needs a `cms` connector, and the session binds one of the user's `cms` connections to it.
`tool_groups` is keyed by the same connector types, plus the reserved key `genaix` for the
in-process harness server, so the module can use the CMS `content_write` group while
`tool_denylist` removes every write this scenario has no business calling: the destructive ones,
`publish_page` (publishing is the plan's transition, never a model call), and the writes of the
`constructs` group, which the module grants for its catalogue reads. It keeps `create_folder`
(`1.0.0-rc.4`), so "put it in a new folder" needs no human detour, bound like `create_page` to a
folder the plan targets, and the folder it creates joins the plan's objects. It keeps `upload_file`
too, but the model never sees that tool: the `session_files` group brings GenAIx's
`upload_session_file` (§6.3), which stands in for it. `content_edit` denies both, since it changes
existing pages only. And `allow_mcp` on a gate is a second, narrower allowlist over the same types:
the `verbatim_identical` script may read the page back and do nothing else.

The fifth is `plan_policy` (`1.0.0-rc.4`), which says how the module's plans behave in a session.
`concurrency: exclusive` means no new plan may be drafted in the session while this one is open;
`allow_while_released` would let a new plan be drafted and executed once this one is released and
only waits to be published. Two plans never execute at once either way. `on_discard: keep` leaves
the pages a discarded plan wrote as unpublished drafts; `restore` would restore each page's version
from before the plan in a short run of the harness, not of the model. Both are served as
`Workflow.plan_policy`. A member the module leaves out comes from the repo-wide `defaults.yaml`,
and a member it sets may only be stricter than the default (§5), so with the defaults below
`allow_while_released` is not available to any module until the repository allows it:

```yaml
# workflows/defaults.yaml
plan_policy:
  concurrency: exclusive
  on_discard: keep
```

`write_targets` is the part only the harness reads. It binds every write tool the module keeps
after the denylist to the plan's objects, keyed by the tool's bare name:

| Member | Meaning |
|---|---|
| `from` | The tool argument holding the target id (`pageId`, `folderId`) |
| `object` | The CMS object type of that target, matched against the plan items' `target`s and the objects the plan created |
| `creates` | The object type the call creates; the new object joins the plan's objects, so later writes to it are in scope |
| `item_kind` | For a tool without a target argument (`create_construct`): bind it to the plan's items of this `kind` |

So `create_page` is allowed only into a folder some plan item targets, the page it creates becomes
an object of the plan, and `update_page_tags` and its siblings are allowed only on a page the plan
targets or created. A write tool the module grants without a binding is refused at every call
(§6.2), which is why the conformance tests fail such a module before it is deployed (§9).

`entry_hints` (`1.0.0-rc.4`) are short phrases the session agent reads when it chooses a module for
a new plan (§6.1). They are prompt material for the agent, never served by the API and never a
routing rule: the choice is the agent's `draft_plan` call, and the user still accepts the plan.

## 4. Manifest schema

The machine-readable schema is `schemas/workflow-manifest.schema.json` (JSON Schema 2020-12). The
loader validates every manifest against it before registering the module. The fields are those of the
example above; the constraints worth stating in prose are:

| Field | Constraint |
|---|---|
| `id` | `^[a-z][a-z0-9_]{2,39}$`, equal to the directory name, frozen on release |
| `version` | `^\d+\.\d+\.\d+$` |
| `scenario` | `S1`, `S2`, `S3`, `S4` or `none`; `none` is served as `scenario: null` in the catalogue (`content_edit`) |
| `roles` | at least one of `content_creator`, `chief_content_creator`, `cmp_administrator`, `any`. Documentation for clients and administrators only: roles never narrow which workflows a subject sees or may start; `Workflow.available` and `capabilities.workflows` depend on authorized connections and configuration alone, and the CMS enforces permissions per tool call (`1.0.0-rc.2`) |
| `produces` | `page`, `construct`, `report`, `user`, `none` |
| `plan_approval` | boolean, default `false`. `true` makes the plan's proposing run propose and end without any CMS write; the user's `plan` part on an `execute` message accepts the plan, as proposed or adjusted (§6.5). `false` (`1.0.0-rc.4`): a module with a CMS write tool still needs **one user confirmation** of the drafted plan before it writes; a module without one (`content_research`) runs at once, because reading is free. Served unchanged as `Workflow.plan_approval` (`1.0.0-rc.3`; per plan since `1.0.0-rc.4`) |
| `plan_policy.concurrency` | `exclusive` (default) or `allow_while_released` (§3). Served as `Workflow.plan_policy.concurrency` (`1.0.0-rc.4`) |
| `plan_policy.on_discard` | `keep` (default) or `restore` (§3). Served as `Workflow.plan_policy.on_discard` (`1.0.0-rc.4`) |
| `plan_policy.write_targets` | map keyed by a write tool's bare name (`^[a-z][a-z0-9_]{1,63}$`) to `{object, from, creates, item_kind}`, at least one member. Harness only, never served (`1.0.0-rc.4`) |
| `entry_hints` | up to 20 unique phrases of 3 to 200 characters, read by the session agent when it chooses a module for a new plan. Never served (`1.0.0-rc.4`) |
| `required_connectors[]` | `{type, optional}`; `type` is a connector type slug such as `cms` |
| `tool_groups` | object keyed by connector type plus the reserved `genaix` |
| `steps_template[].id` | `^st-[0-9]+(-[0-9]+)?$`, recursive through `substeps` |
| `quality_gates[]` | `{id, label, kind, blocking, when}` required; `steps`, `inputs`, `timeout_seconds` (1-600), `retries` (0-2), `on_error`, `allow_mcp`, `source`, `description` optional |
| `quality_gates[].kind` | `script`, `subagent`, `prompt`, `human` |
| `quality_gates[].when` | one or more of `step_end`, `before_release`, `before_publish`. The catalogue publishes every trigger as `QualityGate.triggers[]` and the first one as `when` / `step_id` (`1.0.0-rc.2`) |

Two constraints are enforced by the schema rather than by convention: a `prompt` gate cannot be
blocking, because the model's own word is never a gate, and a gate triggered on `step_end` must name
the `steps` it applies to, so a gate cannot silently run after every step.

`plan_policy` and `entry_hints` are optional and additive (`1.0.0-rc.4`): a `1.0.0-rc.3` manifest
still validates. It inherits `defaults.yaml`, and without `write_targets` every CMS write it grants
is refused at the call, which is fail-closed and which the conformance tests (§9) catch before such
a module is deployed.

## 5. Loading, validation and versioning

Discovery happens once at startup, before the HTTP server accepts traffic. The loader reads the
allowlisted files of `GENAIX_WORKFLOWS_DIR` (§2), validates `defaults.yaml` and each
`manifest.yaml` against the schema, and then performs the semantic checks the schema cannot
express:

- every key of `tool_groups` is either a `required_connectors` type or the reserved `genaix`, and
  every named group exists in that connector type's published group list — for `cms` the groups in
  `mcp/tools.json` (`identity`, `search`, `content_read`, `content_write`, `constructs`, `admin`),
  for `genaix` the harness's own;
- every `tool_denylist` entry is a tool the allowlist would otherwise have granted;
- every `plan_policy.write_targets` binding names a CMS write tool the module grants, and its
  `from`, when present, is an argument of that tool's input schema in `mcp/tools.json`
  (`1.0.0-rc.4`);
- the module's `plan_policy` only **tightens** `defaults.yaml`: `exclusive` over
  `allow_while_released`, `restore` over `keep`. A loosening override fails validation
  (`1.0.0-rc.4`);
- no gate's `allow_mcp` names a write group (§10), and `constructs` counts as one;
- every `quality_gates[].steps` entry exists in `steps_template`, substeps included;
- every `script` and `subagent` gate has its files on disk, and every script gate compiles;
- every `skills` entry resolves to `skills/<name>.md` or to a globally registered skill;
- gate ids are unique within the module;
- the `session` module declares no gates, steps, plan settings or write group (§2).

A granted write tool **without** a binding does not stop the module from registering: the loader
logs it at error level and the harness refuses that tool at every call (§6.2), which is the
fail-closed reading of a gap. The conformance tests below keep such a module out of a deployment.

A module that fails validation is **not registered**, and the failure is logged at error level with
the module id, the failing rule and the file. One broken optional module must not take the
installation down, so startup continues; if a module in the required set (`session`,
`content_create`, `content_edit`, `content_research`, `construct_create`; `session` replaces
`free_chat` since `1.0.0-rc.4`) fails, or `defaults.yaml` does, startup aborts. The optional
`admin_user` module's failure is a warning.

The catalogue is what `GET /workflows` serves (field list in §12), `plan_approval` and
`plan_policy` (without `write_targets`) included; `session` is not in it. Its `quality_gates`
entries are the declaration subset only — `{id, label, kind, blocking, when, description}` —
without `inputs`, `allow_mcp`, `timeout_seconds` or file paths: a client must tell the user what
will be checked and what will block, and thresholds are tuning data rather than contract.
`capabilities.workflows` in `GET /me` narrows the catalogue to what the calling user can run: an
authorised connection for every non-optional `required_connectors` type. A module with no
authorised `cms` connection is listed with `available: false` and a `reason`, not hidden, so the
client can explain what to fix.

A plan resolves its module once, when it is drafted (by `draft_plan`, or by `POST /sessions` with
`workflow`), and stores `workflow` and `workflow_version` on the `Plan` (`1.0.0-rc.4`; on the
session before). Both appear on `Plan`, `PlanSummary` and `PlanState`, and every run of the plan
uses the stored version, while the next plan of the conversation takes the module as then loaded.
Since modules load only at startup, stored and loaded versions can diverge only across a restart;
the harness then logs a warning, uses the loaded module, and sets `workflow_version_drift: true` on
the plan's `PlanState`. `version` is semver read narrowly: patch for prompt and threshold changes,
minor for additive steps or non-blocking gates, major for a changed step id, a removed gate or a new
blocking gate.

**Conformance tests in the workflows repo** (`1.0.0-rc.4`). The repo's own CI runs, for every
module, the checks a deployment must never discover at startup: a module with a CMS write group
sets `plan_approval: true` or is explicitly one-confirmation (`plan_approval: false` with a write
tool, §6.5); `publish_page` is denied, because publishing is a plan transition; every granted write
tool has a `write_targets` binding; no gate gets a write group; `plan_policy` only tightens
`defaults.yaml`. A failing module never reaches a deployment. GenAIx's own tests guard the loader
allowlist: a repository with a `CLAUDE.md`, `.claude/**` and stray files at every level loads
exactly the allowlisted files, and a run's agent runtime sees none of the rest.

Out of scope for the first release: hot reload, per-installation overrides, modules uploaded through
the API, a gate-authoring UI and module feature flags. Modules ship inside the image, so reloading
means a restart.

## 6. How a module drives a run

### 6.1 System prompt and context assembly

The harness composes the system prompt of a plan's run in a fixed order, so a prompt diff is
readable:

1. the GenAIx base prompt (identity, safety, output conventions), shared by all modules;
2. `prompts/system.md` of the plan's module;
3. the step template as a numbered list carrying the exact `Step.id` values the agent must report,
   and the rule that `report_step` is the only way to move the rail;
4. the plan context block: `Session.context` references, the plan's `inputs`, the known
   `derived_settings`, and the uploaded files with their `mode` (`source` or `verbatim`);
5. the **gate contract block**: one line per gate with `id`, `label`, `kind`, `blocking` and
   `description`, stating that the harness runs these, that a blocking failure stops the release,
   and that the agent should satisfy them rather than claim they pass;
6. the skill sitemap for this module's `skills` — name plus the "Use when…" description, bodies
   fetched on demand through the `load_skill` tool;
7. the optional per-step fragment `prompts/step-<id>.md`, injected when that step starts.

A run of the session agent uses the same order with the `session` module in place of the plan's:
its `prompts/system.md`, no step template and no gate contract, and in place of item 4 the
catalogue it may draft plans under, each module with its `title`, `description`, `entry_hints` and
whether it needs the user's acceptance (`1.0.0-rc.4`).

Telling the agent what the gates are is not the same as relying on it. It raises the first-attempt
pass rate, which is a latency and cost argument, while the verdict still comes from the harness.

**The conversation context** (`1.0.0-rc.4`, normative behaviour). Every run, of a plan or of the
session agent, sees the conversation in this order, after the system prompt:

1. **The current task**, always first: the open plan's id, workflow, status, items with status and
   target, what waits for the user, and the user's last decision on it. In a run of a plan this is
   that plan; a session-agent run with no open plan says so.
2. **The ledger**, always and never compacted: for each closed plan its workflow, accepted version
   and items, the objects written with their versions, the check outcomes and the publication
   outcomes; and every user decision (acceptance and adjustment, settings confirmations,
   interaction answers, transitions with their comment). The ledger is stored in its own table as
   each plan closes and each decision happens, kept for the session's lifetime and erased with it;
   event retention never shortens it.
3. **The recent conversation** verbatim within a budget, structured parts rendered compactly
   (tables as rows, citations as reference lists, page structures as block summaries).
4. **Older conversation** as a rolling summary, made at turn boundaries only and never while a plan
   executes. Each compaction emits `status` with `kind: context_compacted`. Items 1 and 2 are never
   compacted.

What the summary leaves out, the agent reaches exactly through `recall` (§6.3). A **side run**
(§6.2) gets the same context with the executing plan as the current task, marked read-only: it may
describe the plan and answer about it, never change it.

### 6.2 Connections and tool allowlist enforcement

A run first resolves connections. For each `required_connectors` type the harness takes the
connection named in `SessionContext.connection_ids`, falling back to the user's `default: true`
connection of that type; a non-optional type with no authorised connection fails the run with
`auth.required {connection_id, connector, reason, how_to_fix}` before any model call.

It then expands the plan module's `tool_groups` into exact qualified tool names, using the group
metadata in `mcp/tools.json` per connector type and its own registry for `genaix`, subtracts
`tool_denylist`, and passes the result to the agent runtime as the complete set of permitted tools,
with no built-in tools loaded and host-level settings ignored. Each resolved connection becomes one
remote MCP server entry keyed by connector type, carrying **that connection's** URL and **that
user's** credential for it: `{"cms": {"type": "http", "url": <connection.url>, "headers":
{"Authorization": "Bearer <this user's token for this connection>"}}}`. The binding is per run and
per user, never shared, and the connection id is recorded on the run for audit and on
`cms_objects[].connection_id` (of the session and of the plan) for every object touched.

A tool outside the module's allowlist is absent from the model's context, not merely denied, so it
cannot be attempted: an agent runtime that loads a server's whole `tools/list` is given the ungranted
names as disallowed tools so they leave the model's context, and an attempt at one is ignored, never
announced on the stream. A tool the user's CMS permissions forbid is absent too, because `cms-mcp`
lists tools dynamically per user and per tool (`04-mcp-scope.md` §2.4). The run sees the
intersection, so the narrower of "what this workflow is for" and "what this person may do on this
connection" always wins. `tool_groups` grants groups; a group with mixed access, such as
`constructs`, brings its reads and its writes, and `tool_denylist` removes the writes a module must
never make (`content_create` grants `constructs` for its catalogue reads and denies its writes,
`1.0.0-rc.2`).

**Which run gets which tools** (`1.0.0-rc.4`). The expanded allowlist is the ceiling; the run's
`mode` (`Run.mode`) decides how much of it the model sees:

| `Run.mode` | CMS tools | `genaix` tools |
|---|---|---|
| `session` | every **read** group of the installation's connectors (`identity`, `search`, `content_read`, the reads of `constructs`), whatever any module grants; never a write | `draft_plan`, `recall`, `request_interaction`, `load_skill`, `read_file` |
| `side` | the same reads | `recall`, `load_skill`, `read_file`; nothing that asks the user or changes state |
| `propose` | the module's reads; **every write withheld** | the plan tools, `recall`, the rest of the module's `genaix` groups |
| `execute` | the module's reads and its bound writes (`write_targets`) | as `propose` |
| `transition` | what the transition needs (`publish_page` for `publish` and `request_review`, the restore for `on_discard: restore`), called by the harness, not by the model | none |

The session agent's tool surface is core, not the `session` module's: no manifest can give it a
write. Reading is free in every mode, so a question never waits for a plan. A **side run** is the
session agent started by a user message while a plan's run is active (`Session.side_run_id`), at
most one at a time: the user keeps asking while long work runs, and the plan's run is unaffected. A
message that accepts a plan or answers the plan run's interaction is never a side run.

**Write-target enforcement** (`1.0.0-rc.4`). In an `execute` run, every CMS write tool call is
checked by the harness **before** it reaches the CMS. The harness looks up the tool's
`write_targets` binding, reads the target id from the `from` argument, and allows the call only
when that object is one of the executing plan's objects: an item's `target`, or an object the plan
created through a `creates` binding. A tool bound by `item_kind` is allowed only while the plan has
items of that kind. Anything the harness cannot map — no binding, the argument missing or not an
id, an object outside the plan — is refused; there is no allow-with-warning. The refusal is a
normal tool outcome on the stream: `tool.started`, then `tool.failed` carrying a `422` problem with
`genaix_code: plan_scope_violation` that names the object, then `error` with `recoverable: true`.
The agent gets the problem as its tool result and tells the user the change would need a plan of
its own, which the user then accepts like any other; it cannot widen the accepted plan itself,
because an `update_plan` after acceptance never adds a target (§6.3).
`examples/s7-continuous-session.sse` shows the refusal. Objects another plan of the session wrote
are outside this plan like any other.

### 6.3 Harness tools

Eight in-process tools form the `harness` group, alongside the existing `genaix` tools for
retrieval, reading attachments and loading skills (`mcp/tools.json` `genaix_side_tools`). Four of
them are **plan tools**, offered only to a plan's runs (`propose`, `execute`); `draft_plan` is
offered only to the session agent; `recall` is read-only and offered to every run (`1.0.0-rc.4`).

| Tool | Runs | Effect | Emits |
|---|---|---|---|
| `report_step` | plan | Set a step's or substep's status and detail. Ids are validated against `steps_template`; an unknown id is a tool error, not an invention. | `step.updated` |
| `update_plan` | plan | Replace the whole plan **of the run's own plan**, never another; the harness increments `plan.version`. Before acceptance it shapes the proposal. Once the plan is accepted (§6.5), a change needs a `reason`, which the event carries, and may merge, split, reorder or drop items but never add a target the accepted version did not have. | `plan.updated` |
| `report_check` | plan | Report an advisory check. The harness forces `blocking: false` and sets `kind: prompt` with a non-`gate` `source.type`, so a client can tell a self-report from a gate verdict. | `check.updated` |
| `set_derived_settings` | plan | Propose folder, template, language, filename, url. Fields the user corrected are ignored and reported back as already decided. A module whose `produces` is empty or only `report` has no settings card (`derived_settings` is `null`) and the tool is an error for it; so does `content_edit` (§8.5). | `derived_settings.updated` |
| `request_interaction` | plan, session | Ask the user (`ask_user`, `choice`, `confirm`, `form`, `settings_review`) and block until answered, timed out or cancelled. The interaction carries the run's `plan_id`, `null` for the session agent. | `interaction.requested`, `interaction.resolved` |
| `write_artifact` | plan | Write a path-confined file into `<base>/sessions/<id>/artifacts/`. | `artifact.created`, `artifact.updated` |
| `draft_plan` | session | `{workflow, summary?, inputs?}`: draft a new plan under a catalogue module. `inputs` are the plan's module inputs, validated against the module's `inputs_schema` (for `content_edit` the pages as `references`) and kept as `PlanState.inputs`; invalid inputs are a tool error. The harness creates the plan (`drafting`, version 0, `workflow_version` pinned), supersedes an open plan that is still `proposed`, and starts the plan's first run once the session agent's turn ends: the proposing run, or for a module without a CMS write tool the run that carries the plan out (§6.5). Refused as a tool error when the module is unknown, unavailable to the user, or `session`, or when the open plan's `plan_policy.concurrency` does not allow a new plan. | `plan.updated` (and `plan.updated` `superseded` for the replaced plan) |
| `recall` | every run | Read-only. Return earlier turns, parts, tool results, ledger entries and still retained events **exactly**, by plan, object, time range or text, from this session and the sessions it `continued_from` (same installation and subject only). | none |

**`upload_session_file`** (`1.0.0-rc.4`, `genaix` group `session_files`). The model can see an
attachment but must not reproduce its bytes in a tool call, so it cannot call
`mcp__cms__upload_file` itself in any useful way. `upload_session_file {file_id, folderId, name?,
description?, overwrite?}` does it on the model's behalf: it calls the CMS tool `upload_file` on
the run's own `cms` connection with the run's credential, first with `source: {kind: url, url,
expiresAt}`, a signed GenAIx link to the session file that lives 300 s (`03-genaix-api.md` §10),
and, when the CMS refuses that (`URL source refused: ...`, for instance because the GenAIx host is
not on its allowlist `mcp.upload_url_hosts`, `09-integration-guide.md` §4), once more with `source:
{kind: base64}` for a file of at most 5 MB. When a module's allowlist grants it, the raw
`mcp__cms__upload_file` is hidden from the model, and the harness treats `upload_session_file` as
`upload_file` for every write rule: withheld from a proposing run, bound by the module's
`write_targets` binding for `upload_file`, gated and reviewed. The uploaded file is recorded in the
plan's `cms_objects` as `created`.

The asymmetry is deliberate: `report_check` can only produce advisory checks. Blocking checks come
from gates, and gates are not callable by the model. The same holds for acceptance: no tool
accepts a plan, moves it to `released` or discards it. Those are the user's message (§6.5) and the
user's transitions (§6.4).

### 6.4 Step lifecycle and the trigger points

A step moves `pending` → `running` → `done` | `skipped` | `failed`, with `waiting` while an
interaction is open. When the agent reports a step `done` and that step id appears in a gate's
`steps`, the harness holds the tool result, runs the matching `step_end` gates, emits the resulting
`check.updated` and `GateResult` updates, then returns the tool result to the agent — including, for
a failed gate, the verdict summary and findings. The agent learns about the failure immediately and
can fix it in the same run. That is the most useful property of the design: a gate is a feedback
signal during the run, not only a barrier at the end.

`before_release` gates run inside the plan's transition route,
`POST /sessions/{session_id}/plans/{plan_id}/transitions {action: release}` (`1.0.0-rc.4`; the
session's own `POST /sessions/{id}/transitions` before, which now keeps only `archive` and
`unarchive`), **before** the plan's active run is cancelled: a release the gates refuse leaves the
run untouched, so a user never loses a run to a refusal (`1.0.0-rc.2`, aligned with `openapi.yaml`,
which has the gates run before the state change). `before_publish` gates run inside the same route
for `action: publish` and `action: request_review`, before the first CMS write of the transition's
run. Gates run per plan: only the plan's own module's gates, against the plan's own objects,
steps and runs, never another plan's. A transition-time gate that needs the run's searches or parts
gets **the plan's latest run's** (a `propose` or `execute` run of that plan, never a session-agent
or side run): the harness keeps every run's tool calls with their inputs and results, and a
`before_release` reviewer re-runs the searches of the run that produced the report.

The settings review has its own place in this order. A setting the run inferred from the user's text
must be confirmed before the first step that writes to the CMS, so the `settings_review` interaction
lands inside the plan or review step of each showcase scenario — after the reading and searching
steps, which may run on inferred settings, and before the first `content_write` call. With
`plan_approval: true` that is the plan's proposing run, so the confirmed settings are part of what
the user accepts. Either side may raise it (`1.0.0-rc.2`): the agent through
`request_interaction(kind: settings_review)` with its own wording and the template options it found,
or the harness itself before the first write when the agent did not ask; one review per run either
way, and the write-tool call waits for the answer. The settings card is the plan's:
`derived_settings.updated` carries its `plan_id`. It is not a gate and it needs nothing in the
manifest: no `quality_gates` entry, no new trigger point, no change to the module layout. Quality
gates are unaffected by it, and a review that expires or is cancelled ends the run the same way any
unanswered interaction does, with no write having happened.

### 6.5 Plans: drafting, proposing, accepting

Since `1.0.0-rc.4` every piece of work a module does is a **plan** with its own lifecycle
(`PlanStatus`):

```
drafting ──► proposed ──► accepted ──► running ──► done ──► released ──► published
   │            │  ▲                                          │  ▲       review_requested
   │            │  └── adjusted (version + 1)                 │  └── reopen
   │            └──► superseded (a newer proposal)            ▼
   └────────────────────────────────────────────────────► discarded (by the user, any open status)
```

Open is `drafting` to `released`; executing is `accepted` and `running`; the rest is closed, and a
closed plan never changes again. Every lifecycle change emits `plan.updated` with the whole plan,
its `id`, `workflow` and `status` included. **At most one plan of a session executes at a time.**

**Drafting.** A plan starts in one of two ways: the session agent calls `draft_plan` (§6.3) when the
user asks for a change, or `POST /sessions` names a `workflow` (a CMS context action such as "Create
page here") and the first plan is drafted at once. Either way the plan is `drafting` at version 0,
pinned to the module's `workflow_version`, and its first run starts. Plain questions never need a
plan: the session agent answers them with its read tools. Whether a new plan may be drafted while
another is open is the open plan's `plan_policy.concurrency` (§3); a new proposal always replaces an
open plan that is still `proposed`, which becomes `superseded` without anything having been written.

**How a plan reaches execution** depends on the module:

| Module | First run | Acceptance |
|---|---|---|
| `plan_approval: true` (`content_create`, `content_edit`) | `propose`: reads, searches, raises the settings review where one is due, proposes the plan through `update_plan` and ends with every write withheld; the plan is `proposed` | the user's next `execute` message (below), which starts an `execute` run |
| `plan_approval: false` with a CMS write tool (`construct_create`, `admin_user`) | `execute`, with every write held until the plan is accepted; the plan is `proposed` while the confirmation waits | **one confirmation**: the user answers the `confirm` interaction the run raises with the plan's `plan_id`, from the plan card or in the conversation; there is no separate proposing pass |
| no CMS write tool (`content_research`) | `execute`, at once: `drafting` → `running` | none: reading is free |

In the proposing run nothing is written and no interaction asks for the acceptance, because the
user's answer is an ordinary message, not a reply to a question. The plan's `PlanState.state` is
`awaiting_feedback` while it is `proposed`.

**Acceptance is the user's own decision, and nothing else** (`1.0.0-rc.4` core rule). The model
never accepts a plan, and no tool or gate does; an interaction answer counts only as the one
confirmation of a `plan_approval: false` module above. For every other plan, a message accepts the
open proposed plan only when it has `options.mode: execute` and carries one `plan` part naming
the plan (`plan_id`) and its current version (`based_on_version`), since `1.0.0-rc.4`; a message
without one never accepts a plan and goes to the session agent:

- without `items` the plan is accepted as proposed, `reason` "Accepted as proposed."; or
- with `items` it is accepted as the user adjusted it, which GenAIx applies (`03-genaix-api.md` §5.1): items in the part's order;
  an item whose `id` names a current step keeps that step's `kind` and `target` unless the part
  sets them; an item without a known id is a new step with an id the harness assigns; a step the
  part leaves out is removed. `reason` is "Adjusted by the user: …", summarising what was kept,
  added and removed.

A `plan` part that cannot be applied never reaches a run: `409 plan_outdated` when
`based_on_version` is not the plan's current `version`, `409 plan_closed` when the plan is closed
or not of this session, `409 plan_not_proposed` when it is still `drafting` or already accepted. An
accepting message that arrives while a run of a plan is active is `409 run_already_active`. The
harness records `accepted_version`, `accepted_by` (the subject) and `accepted_at` on the plan,
writes the decision into the ledger (§6.1), and emits `plan.updated` at version current + 1 holding
exactly the plan the run will execute, first `accepted`, then `running`.

**Executing.** The agent then works through that plan, its writes confined to the plan's objects
(§6.2). When it has to deviate — merge two steps, split one, drop one it cannot carry out — it calls
`update_plan` with a `reason`, so a further `plan.updated` says why; a run never deviates silently,
and a deviation never adds a target (§6.3). Steps reported `done` in the proposing run stay done,
and the execute run continues with the next pending step of the template. When the run has
finished the plan's items and its `step_end` gates have run, the plan is `done` and waits for the
user's `release`. The session stays `active` throughout; a message sent while the plan's run is
active starts a read-only side run (§6.2) instead of interrupting it.

`options.mode: plan_only` is independent of the flag and valid for every module: it ends a run at
the plan whether or not the module asks for approval. The gates are unaffected; a `step_end` gate
bound to a step of the proposing run runs there, and the transition gates run at the plan's
transition as before.

### 6.6 Mapping to the API and events

Nothing new appears on the wire beyond the fields in §12. Steps, plan, checks, derived settings,
the approval chain, previews and publications keep their existing events and full-replace
semantics (`03-genaix-api.md` §6), and since `1.0.0-rc.4` each of those events carries the
`plan_id` it belongs to, so a client keys plan-scoped state on `plan_id` plus the object's own id.
An event without `plan_id`, replayed from a session recorded before `1.0.0-rc.4`, belongs to that
session's only plan. The snapshot of one plan is `PlanState`,
`GET /sessions/{session_id}/plans/{plan_id}`, which is rc.3's `WorkflowState` scoped to the plan:
it carries `gates: [GateResult]`, and `Check` carries `gate_id`, `kind`, `evidence` and `plan_id`.
`GET /sessions/{session_id}/workflow` and `WorkflowState` remain, deprecated, and answer for the
open plan, else the latest.

`GateResult` is the gate's own record, distinct from the `Check` it produces:

```json
{
  "gate_id": "verbatim_identical",
  "label": "Verbatim passages are character-identical",
  "kind": "script",
  "blocking": true,
  "when": "step_end",
  "step_id": "st-5",
  "status": "passed",
  "attempt": 1,
  "started_at": "2026-10-07T09:24:15Z",
  "ended_at": "2026-10-07T09:24:16Z",
  "duration_ms": 940,
  "verdict": "pass",
  "check_ids": ["chk-verbatim-identical"],
  "evidence_file_id": "f3a9...",
  "run_id": "c4e7a2b1-9d3f-4a86-b0c5-7e1f8d2a6b94",
  "plan_id": "5b0e7c2a-91d4-4f6b-8a3e-2c7d9f1e4a60"
}
```

`status` is the execution outcome (`pending`, `running`, `passed`, `failed`, `skipped`, `error`),
`verdict` the judgement (`pass`, `warn`, `fail`, `error`). They differ when `on_error: warn` turns an
`error` verdict into a non-blocking warning: `status: error`, `verdict: error`, and a `Check` with
`severity: warn`.

A gate-produced `Check` keeps the shape `03-genaix-api.md` §8 defines and adds three fields, next
to the `plan_id` every check carries since `1.0.0-rc.4`. Its id
is `chk-<gate id with underscores replaced by hyphens>`, with `-1`, `-2` appended when one gate
reports several checks. `source` comes from the gate's manifest `source` block when present, so a
gate enforcing a named guideline keeps `source.type: guideline` and the guideline id while still
carrying `gate_id` and `kind`. Gates with no `source` block get `source.type: gate` and `source.id` =
the gate id.

## 7. Quality gates

### 7.1 The four kinds

**`script` — deterministic code with a JSON verdict contract.** The harness runs `gates/<id>.py` (or
`.sh`) and reads a single JSON object from stdout. Invocation is an `argv` list, never a shell
string. The script takes one argument, the path to a JSON input document the harness wrote into the
gate's scratch directory:

```json
{
  "gate": { "id": "verbatim_identical", "inputs": { "unit": "chars", "max_deviations_reported": 20 } },
  "session": { "id": "9f1b..." },
  "plan": { "id": "5b0e...", "version": 2, "workflow": "content_create", "workflow_version": "1.0.0" },
  "trigger": { "when": "step_end", "step_id": "st-5", "run_id": "c4e7..." },
  "state": {
    "cms_objects": [{ "type": "page", "id": 9142, "node_id": 3, "operation": "created" }],
    "derived_settings": { "template": { "type": "template", "id": 17 }, "language": "de" },
    "files": [{ "id": "6b0d...", "name": "novelle.pdf", "mode": "verbatim", "sha256": "4e1c..." }],
    "verbatim_spans": [{ "file_id": "6b0d...", "start": 10240, "end": 10552, "target_tag": "content_3" }],
    "tool_calls": [{ "tool": "update_page_tags", "ok": true, "objects": [{ "type": "page", "id": 9142 }] }]
  },
  "mcp": { "cms": { "url": "https://cms.example/mcp", "token_env": "GATE_CMS_TOKEN", "allowed_groups": ["content_read"] } },
  "paths": { "session_dir": "/data/sessions/9f1b...", "scratch_dir": "/data/sessions/9f1b.../gates/5b0e.../verbatim_identical-1" }
}
```

`state` is the plan's (`1.0.0-rc.4`): its objects, derived settings and recorded tool calls, never
another plan's; `files` are the session's, because uploads belong to the conversation. A gate
input document of `1.0.0-rc.3` carried `workflow` and `workflow_version` on `session`; a script that
must read both shapes takes them from `plan` when present.

Exit code 0 with a schema-valid verdict on stdout is the only success path. Anything else — non-zero
exit, unparseable stdout, a verdict failing its schema, a timeout — is an `error` verdict handled per
`on_error`.

**`subagent` — a reviewer agent with its own prompt, read-only tools and a structured verdict.** The
harness builds a fresh request from `gates/<id>.md` as the system prompt and `gates/<id>.yaml` as
configuration, under the `model.reviewer` policy. The reviewer gets its own allowlist drawn from
read-only groups, plus one harness tool, `submit_verdict`, whose argument schema **is** the verdict
schema, so the verdict is a validated tool call rather than prose the harness must parse. A reviewer
that finishes without calling `submit_verdict` yields an `error` verdict. The sidecar:

```yaml
id: guideline_compliance
tool_groups:
  cms: [content_read, search]
max_turns: 12
max_tool_calls: 30
context:
  include: [rendered_preview, page_tags, guidelines, derived_settings, plan]
  exclude: [conversation_transcript]
```

Excluding the main conversation is the default and the point: a reviewer that has read the author's
reasoning tends to agree with it. It sees the artefact, not the argument.

**`prompt` — advisory self-check.** The harness injects `prompts/<gate id>.md` as an extra turn and
records what the agent reports through `report_check`. Never blocking, enforced by the schema. Useful
for "say what you changed and what you are unsure about" before release, and honest about being the
model's own word.

**`human` — explicit approval.** The harness issues `request_interaction` with `kind: confirm`,
carrying the gate label, description and a rendered summary of what is about to happen. Approval is a
`pass`, rejection a `fail` with the user's comment as the finding, expiry an `error`. Human gates fit
where the action is irreversible and the judgement is not mechanisable, the permission grant in the
administration scenario being the case in this release.

### 7.2 Triggers, ordering and budget

`when` takes `step_end`, `before_release` and `before_publish`. `before_publish` covers both the
`publish` and `request_review` transitions of a plan, since both cross the same line from draft to
something a reader or reviewer sees.

Within one trigger, gates run in manifest order and **all of them run**: no short-circuit on the
first failure, because the user should see every problem at once rather than one per attempt. Script
gates with no `allow_mcp` may run concurrently; gates touching the CMS run sequentially, to keep load
on `cms-mcp` and lock behaviour predictable.

Each trigger has a budget, `GENAIX_GATE_BUDGET_STEP_END_SECONDS` (default 120) for `step_end` and
`GENAIX_GATE_BUDGET_TRANSITION_SECONDS` (default 300) for the transition triggers; the transition
budget is also how long the API waits for the gate job before answering `503 service_unavailable`
with `retry_after_seconds`. Gates not started when it is exhausted get `status: skipped`, `verdict: error`,
handled per `on_error` — which for a blocking gate fails the transition. A transition that cannot be
checked is not allowed through.

### 7.3 The verdict schema

One schema for all four kinds, `schemas/gate-verdict.schema.json`. A gate returns exactly this
object, whether it is a script writing to stdout or a reviewer calling `submit_verdict`:

| Field | Type | Meaning |
|---|---|---|
| `verdict` | required, `pass` \| `warn` \| `fail` \| `error` | The judgement. |
| `summary` | required string, max 600 | One sentence for the `Check.message`. |
| `findings[]` | max 50 | `severity` (`info` \| `warn` \| `fail`) and `message` required; optional `target` (ObjectRef per `04-mcp-scope.md` §3.3), `location` (tag keyword, part keyword, template `line:column` or file offset), `expected`, `actual`, and `suggested_action {label, prompt}`. |
| `metrics` | object of number, string or boolean | Whatever the gate measured, e.g. a similarity score. |
| `verification` | object | Filled by identity-proving gates and copied verbatim into `Check.verification`: `locked`, `identical`, `unit` (`chars` \| `blocks` \| `files`), `matched_units`, `total_units`, `source_sha256`, `target_sha256`, and up to 20 `deviations`. |
| `evidence` | object | Gate-specific detail, capped at 16 KB inline; the full record goes to the evidence file (§7.5). |

`verdict` maps to `Check.severity` as `pass` → `pass`, `warn` → `warn`, `fail` → `fail`, `error` →
`fail` or `warn` depending on `on_error`. `Check.blocking` is the manifest's `blocking` flag **and**
a `fail` severity (`1.0.0-rc.2`): a blocking gate that answers `warn` yields a non-blocking `warn`
check, acknowledgeable like any other, so a session can never be stuck behind a warning it can
neither fix nor accept. Every gate execution yields exactly one `Check`, `chk-<gate id>` with `_`
as `-`, the findings folded into `message` one line each with location, expected and actual, the
`suggested_action`s into `suggested_actions`; a gate does not split its findings into several
checks. `verification` is how the verbatim proof reaches the UI unchanged: the script fills it, the
harness copies it into `Check.verification`, and the lock badge reads `identical`.

### 7.4 Blocking semantics

In-flight (`step_end`), a blocking failure does not stop the run. It sets the step to `failed`, emits
a `fail` `Check`, and hands the verdict back to the agent, which can correct the problem and report
the step again; the gate re-runs with `attempt` incremented. `attempt` counts every execution of the
gate **in the plan** (`1.0.0-rc.4`; in the session before), a retry of an infrastructure failure and
a transition's re-check included, and each execution writes its own evidence file (`1.0.0-rc.2`);
the plan's `PlanState` keeps the latest `GateResult` per gate. A second plan under the same module
starts again at attempt 1. `GENAIX_GATE_MAX_ATTEMPTS` (default 3) caps the agent's own `step_end`
re-reports, prevents a loop, and on exhaustion the step stays `failed` and the failure carries into
the transition triggers.

At a transition, `POST /sessions/{session_id}/plans/{plan_id}/transitions` runs the trigger's gates
of that plan before applying the status change (`1.0.0-rc.4`). If any blocking gate's verdict is
`fail` (or `error` with `on_error: fail`), the transition is refused with `409` and `genaix_code:
quality_gate_failed`, carrying the failing set so a client can render it without another call:

```json
{
  "type": "https://genaix.gentics.com/problems/quality-gate-failed",
  "status": 409,
  "genaix_code": "quality_gate_failed",
  "detail": "2 blocking quality gates failed. Fix them or ask the agent to, then try again.",
  "instance": "/api/v1/sessions/9f1b.../plans/5b0e.../transitions",
  "request_id": "req_01J...",
  "failing_checks": [ { "id": "chk-verbatim-identical", "blocking": true, "severity": "fail", "...": "the full Check" },
                      { "id": "chk-references-exist", "blocking": true, "severity": "fail", "...": "the full Check" },
                      { "id": "chk-alt-text", "blocking": false, "severity": "warn", "...": "the full Check" } ],
  "failing_gates": [
    { "gate_id": "verbatim_identical", "label": "Verbatim passages are character-identical",
      "kind": "script", "blocking": true, "when": "before_release", "status": "failed",
      "verdict": "fail", "attempt": 2, "check_ids": ["chk-verbatim-identical"],
      "evidence_file_id": "3a9c...", "started_at": "...", "ended_at": "...", "duration_ms": 412 },
    { "gate_id": "references_exist", "label": "All links and references resolve",
      "kind": "script", "blocking": true, "when": "before_release", "status": "failed",
      "verdict": "fail", "attempt": 1, "check_ids": ["chk-references-exist"],
      "evidence_file_id": "51e0...", "started_at": "...", "ended_at": "...", "duration_ms": 88 }
  ],
  "acknowledgeable_checks": ["chk-alt-text", "chk-guideline-compliance"]
}
```

`failing_checks` is every failing gate-sourced check, blocking first, then the non-blocking ones
not yet acknowledged; `failing_gates` is the full `GateResult` of each blocking gate, the same
object the plan's `PlanState` carries (`1.0.0-rc.2`; an earlier revision of this example showed a
shortened gate shape that `problem.schema.json` never had).

Non-blocking failures never refuse a transition on their own, but they are not silent either. A
transition whose trigger produced unacknowledged non-blocking `warn` or `fail` checks is refused
with the same `409`, an empty `failing_gates` and those checks in `acknowledgeable_checks`; the
client repeats the call with `acknowledge_checks: ["chk-alt-text", ...]` and it proceeds. The
acknowledgement is stored on the plan (`1.0.0-rc.4`; on the session before), attributed to the
user's subject, appears on the `Check` as `acknowledged_by` and `acknowledged_at` and on the
`GateResult`, and enters the ledger as a user decision (§6.1). It covers that plan's check only; the
next plan's warnings are acknowledged again. "I saw the warning and published anyway" is an audit
fact, not a UI state.

A blocking check cannot be acknowledged; listing one returns the same `409` with that check still in
`failing_gates`. There is no runtime override. A blocking gate that turns out to be wrong is made
non-blocking in the manifest, which is a reviewable change, rather than bypassed per transition,
which is not.

### 7.5 Evidence capture

Every gate execution writes one evidence record into the session file store as a `File` with `kind:
artifact` and `artifact_type: gate_evidence`, at
`<base>/sessions/<id>/artifacts/gates/<plan_id>/<gate id>-<attempt>.json` (`1.0.0-rc.4`;
`gates/<gate id>-<attempt>.json` before): the gate declaration, the input document, the raw verdict,
the script's stdout and stderr (64 KB each) or the reviewer's tool-call trace, and timing.
`GateResult.evidence_file_id` points at it; `Check.evidence` is the gate's own evidence object (its
gate-specific keys such as `unit`, `source_sha256`, `images_without_alt`) with the inline subset
`{gate_id, kind, verdict, summary, metrics, finding_count, evidence_file_id, duration_ms}` laid on
top, a key present in both being the subset's (`1.0.0-rc.2`).

A reviewer's activity stays off the session stream: a client sees the gate's `check.updated` and
the evidence file's `artifact.created`, never the reviewer's `tool.started` / `tool.completed`, and
the evidence file's `trace` holds every call the reviewer made with the objects it returned. A
reviewer's calls to the CMS carry `X-GenAIx-Gate: <gate id>` (`04-mcp-scope.md` §2.3), and
`X-GenAIx-Run` only when they happen inside a run.

This is what makes a gate auditable afterwards: "the verbatim check passed" becomes a file holding
both digests, the span offsets, the tool call that read the page back and the timestamp. For a
reviewer sub-agent it also records which objects the reviewer read, the only way to tell a grounded
review from a confident guess. Evidence files follow the session's retention and never reach the
model.

### 7.6 Timeouts, retries and failure handling

`timeout_seconds` defaults to 30 for `script`, 180 for `subagent` and 600 for `human`, capped at 600.
A timeout kills the process or cancels the reviewer run and yields `verdict: error`.

`retries` defaults to 0 for `script` and 1 for `subagent`, and covers infrastructure failure only: a
timeout, a spawn error, or a transient model-provider error. A clean `fail` is never retried; a gate
that says no is not asked again more politely. Each attempt gets its own `GateResult` and evidence
file.

`on_error` decides what an `error` verdict means: `fail` (the default, and the only defensible
setting for anything proving a factual claim), `warn` (non-blocking, right for the guideline
reviewer, whose unavailability should not stop a release), or `skip` (no check emitted; for genuinely
optional gates only, and the loader warns when a blocking gate declares it).

## 8. The gate catalogue

### 8.1 Content creation from source material, `content_create`

| Gate | Kind | Blocking | When | Inputs | Pass criteria |
|---|---|:-:|---|---|---|
| `no_duplicate` | script | no | `step_end` st-3 | `threshold` 0.82, `types` | The best `find_similar` score is below the threshold, or the agent recorded a link to the closest match instead of duplicating it. When the run recorded no `find_similar` call, the names of the recorded `search_content` hits are compared against the planned page's title on the same 0..1 scale, and the evidence names the `method` (`1.0.0-rc.2`). |
| `constructs_allowed` | script | **yes** | `step_end` st-5, `before_release` | none | The construct keywords on the draft (`get_page_tags`) are a subset of what the template allows (`get_template`), narrowed by what is assigned to the node when the run recorded a `list_constructs` read (the module grants the `constructs` group for its catalogue reads and denies its writes; the evidence says which half applied, `1.0.0-rc.2`). |
| `verbatim_identical` | script | **yes** | `step_end` st-5, st-6, `before_release`, `before_publish` | `unit: chars` for typed or quoted passages, `unit: blocks` for whole verbatim files, `max_deviations_reported` | Per declared span or file block, sha256 over NFC-normalised UTF-8 of the source equals that of the text read back from the target tag; `matched_units == total_units`; `identical: true`. |
| `references_exist` | script | **yes** | `step_end` st-5, `before_release` | `external_links: report_only` | Every internal reference resolves through `get_page`, `get_file` or `get_image`. Unresolvable ones are `fail` findings; external URLs are `info` and not fetched. |
| `draft_unpublished` | script | **yes** | `before_release` | none | `get_page(id, include=versions)` shows `online: false` and no published version, so review starts from a draft. |
| `alt_text` | script | no | `step_end` st-5, `before_release` | none | Every image tag has non-empty alternative text. Enforces guideline `accessibility-basics`. |
| `guideline_compliance` | subagent | no | `before_release` | reviewer sidecar | A reviewer reading the preview, the page tags and the guidelines returns `pass` or `warn` with located findings. `on_error: warn`. |
| `release_self_check` | prompt | no | `before_release` | none | Advisory: the agent states what it changed and what it is unsure about. |

`verbatim_identical` is the gate the verbatim requirement turns on. The script does not trust the
in-memory copy of what the agent wrote: it reads the page back through `get_page` with the user's own
token and compares the target tag's value against the source bytes at the declared offsets.
Normalisation stops at Unicode NFC, because anything more is the gate quietly tolerating a change.
Deviations become findings with `expected`, `actual` and a character offset, capped at 20.

The sources are the `verbatim` parts of the user's messages and the files uploaded with `mode: verbatim` (PDF and office documents included): the harness extracts a file's text once, splits it into blocks and hashes each, so a locked file is verified block by block with `unit: blocks`, while a `verbatim` part quoting a passage from a file (`source: <file id>`) is verified as characters like typed text.

What a locked file demands is fixed since `1.0.0-rc.2`. By default (`verbatim_mode: marked_spans`, the S1 reading) the file is a source the page quotes from, not a document it reproduces: a block counts as reused when the page carries it, its opening, or a stretch that resembles it; a shortened or re-worded copy is a deviation, an omitted block is not. A module input `verbatim_mode: whole_file` requires every block, and `off` disables the file check. The block rule, so that a third-party gate agrees with the harness: blocks are blank-line separated runs of the extracted text with inner whitespace collapsed, Unicode NFC; a PDF's blank lines come from its layout, a Word document's paragraphs and table rows are blocks, HTML block-level elements are blocks; each hash is SHA-256 over the NFC UTF-8 of the block.

`guideline_compliance` is non-blocking while four scripts block because a script verdict is
reproducible from its evidence file and a reviewer agent's is not: a false `fail` would stop a release
for a reason nobody can re-derive. An installation that wants it blocking flips one manifest field.

`content_create` has `plan_approval: true` (`1.0.0-rc.3`, §6.5), so an S1 plan stops at its
proposal. The plan is drafted either by the session agent, when the user asks for a new page in the
conversation, or at once by `POST /sessions` with `workflow: content_create` from the CMS context
action. Its proposing run covers st-1 to st-4: it reads the source, raises the settings review,
searches (`no_duplicate` runs at the end of st-3), proposes the plan and ends with nothing written;
the plan is `proposed`. The user's next `execute` message, accepting the plan or adjusting it with a
`plan` part naming it, starts the execute run, which opens with `plan.updated` at the next version,
creates the page in the folder the plan targets (st-5; the new page joins the plan's objects through
the `create_page` binding), fills it, and verifies and previews it (st-6), with the st-5 and st-6
gates bound as before. Every gate keeps its trigger; only the run it falls into changes. The plan
is then `done`, and `release` and `publish` are the user's transitions of this plan.
`examples/s1-content-create.sse` records the execute run as one run (the proposing half ends at its
first `plan.updated`); `examples/s7-continuous-session.sse` shows a `content_create` plan drafted
in the middle of a conversation, and a second one superseding it before acceptance.

### 8.2 Research, inventory and reporting, `content_research`

| Gate | Kind | Blocking | When | Inputs | Pass criteria |
|---|---|:-:|---|---|---|
| `citations_resolve` | script | **yes** | `step_end` st-3, st-4 | none | Every `citation` ref and every `table` row reference resolves through `content_read`, and each cited snippet occurs in the cited object's current text. |
| `counts_reproducible` | script | **yes** | `step_end` st-4 | `tolerance: 0` | Replaying each recorded search through `count_content` with the search's own inputs reproduces every total and aggregation bucket the answer states. |
| `result_verification` | subagent | no | `before_release` | reviewer sidecar | A reviewer re-runs a sample of the run's `search_content` calls (default 3, max 5), confirms the reported hits exist and the claims follow from them, and reports discrepancies. |

Since `1.0.0-rc.4` `content_research` is a module the session agent **enters for a gated report**:
when the user wants an inventory or a count they can rely on, with citations that resolve and totals
that reproduce. A plain question ("which pages mention …?") needs no plan at all; the session agent
answers it with its read tools, and nothing is gated. Because the module grants no CMS write tool,
its plan needs no acceptance: `draft_plan` starts the run that carries it out at once (`drafting` →
`running`), and the user can follow up the report with a writing plan in the same conversation.
`examples/s2-content-research.sse` is such a plan.

This workflow writes nothing to the CMS, so its quality problem is not damage but confident
wrongness. The two script gates take the mechanisable half: a citation that does not resolve, a
snippet that is not in the page, a count that does not reproduce. `counts_reproducible` is cheap
because the harness records every search call with its inputs and `count_content` counts exactly
what `search_content` matches (`05-content-rag-contract.md` §4), so the gate replays a recorded
call's inputs rather than guessing a query; `queryUsed` is display, not an input (`1.0.0-rc.2`).
`citations_resolve` runs at st-3 and again at st-4, because the citation part only exists once the
report is written: at st-3 it passes with nothing to check, at st-4 it judges the parts. The
reviewer samples rather than re-running everything, so its cost stays bounded. A script gate's input
document carries `state.parts` (the assistant parts emitted so far in the run, at a transition the
plan's latest run's) and, for every successful `search` call of the plan, `tool_calls[].input` and
`.result`. Searches the session agent made before the plan was drafted are not the plan's and are
not judged.

### 8.3 Construct creation, `construct_create`

| Gate | Kind | Blocking | When | Inputs | Pass criteria |
|---|---|:-:|---|---|---|
| `handlebars_compiles` | script | **yes** | `step_end` st-4 | helper allowlist incl. `gtx_render`, `gtx_i18n`; forbidden patterns | `validate_handlebars_template` returns `valid: true`, no undefined helper, and no forbidden pattern (`<script`, inline `on*=`, triple-stash over anything but declared parts). |
| `construct_similarity` | script | no | `step_end` st-2, st-4 | `name_threshold` 0.85, `template_threshold` 0.80 | No existing construct exceeds either threshold, or the user confirmed creating a new one anyway through the similarity interaction. The gate compares against the catalogue reads the run recorded (`list_constructs`, `get_construct`), since a gate is never allowed the `constructs` group itself (`1.0.0-rc.2`). |
| `parts_consistent` | script | **yes** | `step_end` st-4 | authoritative typeId table | Every part the template references as `cms.tag.parts.<keyword>` is declared with a known `type_id`, every declared part is referenced or marked unused, and no entry of `parts` is the type 43 template part, which lives in `template` (`1.0.0-rc.2`). |
| `preview_renders` | script | **yes** | `step_end` st-6 | expected marker text | `render_preview` on the proof page returns HTML containing the filled part values, with no Handlebars error marker and no unresolved `{{`. |
| `construct_review` | subagent | no | `before_release` | reviewer sidecar | A reviewer checks i18n names, part labels, editability flags and template markup against the request, and reports located findings. |

`handlebars_compiles` is `validate_handlebars_template` from `06-construct-devtools-requirements.md`
§5.1 promoted to a gate, which is this document's argument in miniature: a tool the model may call is
not a check, because a check has to run whether the model remembered or not. `construct_similarity`
is §5.2 of the same document, with the interaction it describes recorded as the gate's
acknowledgement path. `preview_renders` catches what a compile cannot, a template that compiles and
renders nothing useful.

`construct_create` keeps `plan_approval: false`, so since `1.0.0-rc.4` it is a one-confirmation
module (§6.5): its run raises a `confirm` interaction with the plan's `plan_id` before the first
construct write, and the user's answer is the acceptance (`examples/s3-construct-create.sse`). Its
writes have no page to target, so its `write_targets` bind them by item kind, for instance
`create_construct: { creates: construct, item_kind: construct }`; the construct it creates then
joins the plan's objects, and the later writes that name it are in scope.

### 8.4 Administration, `admin_user` (optional)

| Gate | Kind | Blocking | When | Inputs | Pass criteria |
|---|---|:-:|---|---|---|
| `permission_impact_previewed` | script | **yes** | `step_end` st-2 | none | This run's tool log holds a `preview_permission_impact` result for the exact `(user, add_groups, remove_groups)` about to be applied, with a non-empty before/after matrix. |
| `admin_confirm` | human | **yes** | `step_end` st-2, `before_publish` | rendered diff | The administrator approves the rendered permission diff through `interaction kind=confirm`. A rejection is a `fail` carrying their comment. |

This is the one workflow whose correct gate is a person. A permission grant is small, silent and
consequential, and no script can judge whether this user belongs in that group. What a gate can
guarantee is that nobody grants it blind: the preview must exist, and a human must have seen it.

### 8.5 Editing existing pages, `content_edit`

`content_edit` (`1.0.0-rc.3`) changes pages that already exist, online or not, from a chat
instruction. It has no showcase scenario of its own; its most common use is the second half of
"look it up, then change it". Since `1.0.0-rc.4` both halves live in **one session**: the session
agent finds the pages (with its read tools, or through a `content_research` plan), and when the user
asks for the change it drafts a `content_edit` plan for them. Before `1.0.0-rc.4` this took a second
session created with `continued_from`; that link remains for a new conversation that builds on an
old one (`03-genaix-api.md` §4.3), not for follow-up work in the same one. One plan handles up to
20 pages, with one acceptance, one release and one publish for all of them; more pages, or a second
round of changes, are a further plan. Canonical transcripts: `examples/s6-content-edit.sse` (a
new conversation continued from S2 and created with `workflow: content_edit`, three pages in two
folders with different publish permissions) and `examples/s7-continuous-session.sse` (the same work as a plan inside a longer
conversation).

```yaml
id: content_edit
version: 1.0.0
title: Edit existing pages
description: >-
  Change one or more existing CMS pages from a chat instruction: read the pages and their
  versions, propose one plan with per-page items, apply it once the user accepts or adjusts it,
  verify and preview each page, then release for review and publish.
scenario: none
roles: [content_creator, chief_content_creator]
optional: false
produces: [page]
plan_approval: true

plan_policy:
  concurrency: exclusive
  on_discard: keep
  write_targets:
    update_page_tags: { object: page, from: pageId }
    add_page_tag: { object: page, from: pageId }
    update_page_properties: { object: page, from: pageId }

entry_hints:
  - change text, links or metadata on existing pages
  - replace a value on the pages just found
  - update several pages the same way

inputs_schema:
  type: object
  additionalProperties: false
  required: [node_id, references]
  properties:
    node_id:
      type: integer
      description: CMS node the pages belong to.
    language:
      type: string
      pattern: "^[a-z]{2}(-[A-Z]{2})?$"
    references:
      type: array
      description: >-
        The plan's references: the session's context references, or the pages the session
        agent drafted the plan for. The pages to edit are its type page entries, in order;
        other entries (guidelines, files) are context as for any module.
      contains:
        type: object
        required: [type, id]
        properties:
          type: { const: page }
      minContains: 1
      maxContains: 20
    guidelines:
      type: array
      items: { type: string }

required_connectors:
  - type: cms
    optional: false

tool_groups:
  cms: [identity, search, content_read, content_write, constructs]
  genaix: [harness, session_files]

tool_denylist:
  - mcp__cms__create_page
  - mcp__cms__create_folder
  - mcp__cms__delete_page
  - mcp__cms__take_offline
  - mcp__cms__translate_page
  - mcp__cms__restore_page_version
  - mcp__cms__publish_page
  - mcp__cms__upload_file
  - mcp__cms__create_construct
  - mcp__cms__update_construct
  - mcp__cms__ensure_construct_category
  - mcp__cms__assign_construct_to_nodes
  - mcp__cms__add_construct_to_package
  - mcp__cms__request_tier2_field_mapping

steps_template:
  - id: st-1
    label: Read the pages and their versions
  - id: st-2
    label: Propose plan
  - id: st-3
    label: Apply changes
  - id: st-4
    label: Verify and preview

model:
  default: claude-opus-5
  effort: high
  reviewer:
    model: claude-sonnet-5
    effort: medium

skills:
  - cms-content-search
  - construct-selection

quality_gates:
  - id: constructs_allowed
    label: Only constructs the template and node allow
    kind: script
    blocking: true
    when: [step_end]
    steps: [st-3]
    description: >-
      On every changed page, the construct keywords are a subset of what the page's template
      allows, narrowed by what is assigned to the node.

  - id: references_exist
    label: Every internal reference resolves
    kind: script
    blocking: true
    when: [step_end]
    steps: [st-3]
    description: >-
      On every changed page, internal references resolve through the CMS; external URLs are
      reported, never fetched.
    inputs:
      external_links: report_only
    allow_mcp:
      cms: [content_read]

  - id: alt_text
    label: Every image has alternative text
    kind: script
    blocking: false
    when: [step_end]
    steps: [st-3]
    description: Every image tag on every changed page has non-empty alternative text.
    source:
      type: guideline
      id: accessibility-basics

  - id: base_version_current
    label: Nobody else changed the pages meanwhile
    kind: script
    blocking: true
    when: [before_release]
    description: >-
      Refuses the release if any changed page has a version newer than the one the agent read,
      other than the versions this plan's own writes produced, naming the page.
    timeout_seconds: 30
    allow_mcp:
      cms: [content_read]

  - id: release_self_check
    label: Agent self-check before release
    kind: prompt
    blocking: false
    when: [before_release]
    description: The agent restates what it changed on each page and what it is unsure about.
```

The module differs from `content_create` in four places.

**The pages are the plan's targets.** The pages to edit are the `type: page` entries of the
plan's `references`, 1 to 20, in order: the session's `context.references` when the session was
created with `workflow: content_edit`, or the pages the conversation found when the session agent
drafts the plan. `inputs_schema` states that bound, so a plan with none or with more than 20 is
refused when it is drafted (`POST /sessions` answers `422`, `draft_plan` a tool error). The bound is
**per plan** (`1.0.0-rc.4`; per session before): a conversation may edit more pages in a further
plan. Other reference types (a guideline, an uploaded file) are context as for any module.

**One plan, per-page items.** st-1 reads every page with `get_page` and `get_page_tags` and its
version history with `get_page_versions`; the version current at that read is the page's
**base version**, recorded by the harness from the tool result. st-2 proposes **one** plan for
all pages: every item carries the page it changes as its `target` (`kind: update` for content
and metadata, `link` for a new cross-reference), so a client can group the plan by page and the
user can remove a page from the work by deleting its items. The proposing run then ends at the
plan (§6.5). Those targets are also the plan's write scope: the `write_targets` bindings above map
`pageId` of every write to a page, and a write to a page no item targets is refused with
`plan_scope_violation` (§6.2), which S7 shows for a page the user removed from the plan. When the
user wants another page included before accepting, the session agent drafts a new proposal that
supersedes this one, so the added page's changes are approved like the others before anything is
written; after acceptance, more pages are a further plan.

**No settings card.** `derived_settings` is `null` and `set_derived_settings` is an error, even
though the module `produces: [page]`: every write targets an existing page whose node, folder,
template and language are already fixed, so there is nothing to derive and the write-governing
settings rule (`03-genaix-api.md` §9.1) does not apply. No `settings_review` is raised.

**Previews and publications per page.** st-3 applies the plan with `update_page_tags`,
`add_page_tag` and `update_page_properties`, item by item; st-4 reads each changed page back
and renders it with `render_preview`, so `preview.updated` arrives once per page and the plan's
`PlanState.previews` holds the latest preview of each. Every page the plan changed gets a
`Publication` with the plan's `plan_id` (`03-genaix-api.md` §4.2, §8); a page the plan only read
gets none, and a page another plan of the session wrote is never touched. At the plan's `publish`
each page is published directly or sent to review according to the permissions on its own folder,
so in the S6 transcript two pages are published and one goes to review: the **plan** ends
`review_requested`, while the session stays `active` for the next question or plan.

| Gate | Kind | Blocking | When | Inputs | Pass criteria |
|---|---|:-:|---|---|---|
| `constructs_allowed` | script | **yes** | `step_end` st-3 | none | As in §8.1, per changed page against that page's own template and node; each finding names its page as `target`. |
| `references_exist` | script | **yes** | `step_end` st-3 | `external_links: report_only` | As in §8.1, per changed page. |
| `alt_text` | script | no | `step_end` st-3 | none | As in §8.1, per changed page. Enforces guideline `accessibility-basics`. |
| `base_version_current` | script | **yes** | `before_release` | none | For every changed page, `get_page_versions` shows no version newer than its base version other than those the plan's own writes produced. A page changed by someone else after the agent read it, an earlier plan of the same session included, is a `fail` finding naming the page. |
| `release_self_check` | prompt | no | `before_release` | none | Advisory: the agent states what it changed on each page and what it is unsure about. |

There is no `draft_unpublished`: an edited page is usually online, and the release is reviewed
against the page as it stands. `base_version_current` takes its place as the release guard. A
review must never approve changes written over somebody else's edit that the agent never saw,
and a stale base is exactly that. The gate reads each changed page's version history through
`get_page_versions` with the user's own token and compares it with two things the harness
recorded: the base version from st-1, and the version each of the plan's own write tool calls
left the page at. Any other version newer than the base fails the gate. Fixing the failure means
re-reading the page and re-applying the change on top of the other edit, which the agent does in
a further run of the same plan: the refused release left it `done`, and the user's plain
`execute` message runs it again (`done` → `running`) within the scope already accepted; the gate
then passes against the new base.

Its verdict follows §7.3 like every other gate. One finding per page with a newer version,
carrying the page as `target`, and the version detail in `evidence`:

```json
{
  "verdict": "fail",
  "summary": "1 of 3 pages was changed by someone else after it was read: Serviceinformationen.",
  "findings": [
    {
      "severity": "fail",
      "message": "Page Serviceinformationen has version 22 (2026-10-07 11:41), newer than base version 21 read at 11:05.",
      "target": { "type": "page", "id": 9011, "node_id": 3, "label": "Serviceinformationen" },
      "expected": "21",
      "actual": "22",
      "suggested_action": { "label": "Re-read and re-apply", "prompt": "Read Serviceinformationen again and re-apply the planned changes on top of the current version." }
    }
  ],
  "metrics": { "pages_checked": 3, "pages_stale": 1 },
  "evidence": {
    "pages": [
      { "ref": { "type": "page", "id": 8644, "node_id": 3 }, "base_version": "14", "own_versions": ["15"], "current_version": "15", "foreign_versions": [] },
      { "ref": { "type": "page", "id": 8871, "node_id": 3 }, "base_version": "9", "own_versions": ["10", "11"], "current_version": "11", "foreign_versions": [] },
      { "ref": { "type": "page", "id": 9011, "node_id": 3 }, "base_version": "21", "own_versions": [], "current_version": "22",
        "foreign_versions": [ { "number": "22", "timestamp": 1791373260 } ] }
    ]
  }
}
```

`foreign_versions` lists only version numbers and timestamps; the editor `get_page_versions`
reports stays out of the evidence, which lives in the user's session store. A page whose version
history cannot be read is an `error` verdict for the gate, which, being blocking with the
default `on_error: fail`, refuses the release.

## 9. Testing a module

Three layers per module, all runnable without a CMS, plus the repo-wide conformance tests.

**Gate unit tests** (`tests/gates/test_<gate>.py`) call the gate script with a recorded input
document and assert the verdict. This is where threshold and edge-case behaviour is pinned: a
one-character deviation must fail `verbatim_identical`, a missing alt text must warn and not fail, a
template using `gtx_render` must not be flagged as calling an undefined helper.

**Fixture conversations** (`tests/fixtures/<name>.yaml`) script a whole plan: user turns (the
drafting request, the acceptance as a `plan` part, the transitions),
canned MCP responses in call order, and the expected step transitions, gate results and the
**terminal plan status** (`Plan.status`; the session status before `1.0.0-rc.4`, which now stays
`active`).
They run against a stub model adapter for harness-only assertions and against `mocks/cms-mcp-mock`
for the MCP-facing path. Minimum per module: a happy path plus one deliberate violation of each
blocking gate, and for a writing module one write outside the plan that must end in
`plan_scope_violation` with nothing sent to the CMS.

**Replay harness** (`tests/replay/`) takes an SSE transcript from `examples/`, reconstructs the plan
state each gate would have seen at every trigger point (cms objects, files, tool calls, plan,
derived settings; events are grouped by `plan_id`, and an event without one belongs to the
transcript's only plan), runs the module's script gates against it with `mocks/cms-mcp-mock` serving
the reads, and compares verdicts against `tests/replay/<transcript>.expected.yaml`. Because those
transcripts are the same fixtures the reference client and `mocks/genaix-mock` consume, a gate that
drifts from the documented scenario fails in continuous integration. Subagent gates are skipped in
replay unless `--with-model` is passed, since they cost tokens and are not deterministic.

**Conformance tests** (`test:workflows`, `1.0.0-rc.4`) run over the whole repository in its CI and
fail a module before it is deployed (§5): a module with a CMS write group sets `plan_approval: true`
or is explicitly one-confirmation; `publish_page` is denied; every granted write tool has a
`write_targets` binding; no gate's `allow_mcp` names a write group; `plan_policy` only tightens
`defaults.yaml`; the `session` module declares no gates, steps, plan settings or write group.

## 10. Security

Gate execution runs code and talks to the CMS, so its boundaries are stated explicitly.

- **Modules are trusted, reviewed code.** They ship in the image from the repository. No session,
  prompt, uploaded file or MCP response can add, modify or disable a gate. `write_artifact` is
  confined to the session directory; the modules directory is mounted read-only.
- **GenAIx loads an allowlist from the repository** (`1.0.0-rc.4`). Only the files §2 names reach
  the harness: `manifest.yaml`, `genaix.yaml`, `prompts/*.md`, the declared skills and gate files of
  each top-level module directory, and `defaults.yaml`. The development guide (`CLAUDE.md`,
  `AGENTS.md`, `.claude/**`), `README.md`, `POLICY.md`, `tests/` and every dot- or
  underscore-directory are never read, however they are named. The agent runtime of a run loads no
  settings sources and works in a directory outside the modules, so instructions written for the
  module authors' coding agents can never become instructions to a production run, and the app and
  runner images carry the modules without the guide.
- **A module tightens, it never loosens.** The core rules (only the user accepts a plan, a write
  must target an object of the executing plan, the session agent never writes, reading is free) are
  harness code, not manifest data. `plan_policy` may only tighten `defaults.yaml`, and the loader
  refuses a loosening override; an unmappable write is refused, never allowed with a warning
  (§6.2).
- **Script gates run as a subprocess with limits**, not in the event loop: an `argv` list with no
  shell, cwd set to the gate's scratch directory, an environment scrubbed to `PATH`, `LANG` and the
  injected MCP token variable, the harness-enforced timeout, and rlimits on CPU, address space
  (default 512 MB; a gate that spawns a Node child, such as the Handlebars checker, needs 1 GB) and
  file descriptors. Shell gates are allowed but discouraged, and the loader warns on `.sh`.
  An OS-level sandbox around every run's subprocesses (the agent runtime, gate scripts, reviewer
  agents; a per-run filesystem view, scrubbed environment and network mode) is **recommended and
  not required for Phase One**: a deployment that cannot provide user namespaces runs the same
  processes with the limits above, and nothing in this contract observes the difference.
- **No network except the run's own connections.** A gate with no `allow_mcp` gets no credential. A
  gate with `allow_mcp` reaches only the connections the run already resolved for those connector
  types, through a short-lived reference to the same per-user credential, scoped to the listed
  groups. Every call carries the run's `X-GenAIx-Session` audit header, `X-GenAIx-Run` when the gate
  runs inside a run, and `X-GenAIx-Gate: <gate id>` (`04-mcp-scope.md` §2.3), so the CMS access log
  attributes gate reads correctly. A
  gate cannot name an undeclared connector type or pick a different connection than the run is bound
  to. External URLs are therefore reported rather than fetched; an egress allowlist is a later
  extension.
- **Gates read, they do not write.** No gate in this release calls a `content_write` or `admin` tool
  or a write tool of `constructs`, and the loader rejects an `allow_mcp` entry naming a write group
  (`constructs` counts as one, because it holds writes; a gate that needs the construct catalogue
  reads what the run recorded, §8.3). A gate that repairs what it checks cannot be trusted to report
  on it.
- **Evidence is not a content log.** Evidence files live in the session store under the session's
  retention, visible to the owning user. They may contain the content under test, which is why they
  never reach the MCP access log, which by `04-mcp-scope.md` §6 carries no content at all.
- **Reviewer sub-agents are budgeted.** `max_turns` and `max_tool_calls` are hard caps enforced by
  the harness, and the reviewer holds the read-only allowlist only. It cannot start a run, cannot
  write, cannot ask the user anything and sees no other plan of the session.

## 11. Extensibility: Design Studio modules

Design Studio is outside this release, but the module shape was chosen so its workflows are new
directories rather than a new mechanism (`04-mcp-scope.md` §8.1).

A `template_set_import` module would declare a `cms` entry in `required_connectors` with a
`templates` tool group that does not exist yet, an `inputs_schema` taking the package and target
node, and mostly script gates: the imported set parses, every construct it references exists or is
created, and no existing template changes without an explicit diff.

A `page_decomposition` module, turning an existing HTML page into templates and constructs, is the
case that proves the design. Its central gate is a **render diff**: a script that renders the
original page and the decomposed result and compares the normalised DOM, blocking when the difference
exceeds a declared tolerance. That is the same kind of deterministic, evidence-producing script gate
as `verbatim_identical` on a different artefact, and nothing in the manifest schema, the verdict
schema, the trigger model or the blocking semantics has to change to carry it. Template edits have a
larger blast radius than page edits, so a `human` gate before the write is the natural companion,
already supported.

## 12. API surface

The fields this document depends on (`1.0.0-rc.4`; `openapi.yaml` is authoritative):

- `Workflow`: `id`, `title`, `description`, `scenario`, `roles`, `optional`, `produces`, `version`,
  `plan_approval` (`1.0.0-rc.3`; per plan and with the one-confirmation reading of `false` since
  `1.0.0-rc.4`), `plan_policy {concurrency, on_discard}` (`1.0.0-rc.4`), `inputs_schema`,
  `required_connectors` (connector types), `tool_groups`, `steps_template`,
  `quality_gates[{id, label, kind, blocking, when, description}]`, plus `available` and `reason` for
  the `GET /me` narrowing. `GET /workflows` lists every module a plan can run under; the session
  agent (module `session`) is not a catalogue entry, and `free_chat` is gone.
- `Plan` (`1.0.0-rc.4`): `id`, `workflow`, `workflow_version`, `status` (`PlanStatus`: `drafting`,
  `proposed`, `accepted`, `running`, `done`, `released`, `published`, `review_requested`,
  `discarded`, `superseded`), `version` (0 while drafting), `accepted_version`, `accepted_by`,
  `accepted_at`, `closed_at`, `summary`, `items`, `reason`. `PlanSummary` and `PlanPage` for the
  list.
- `PlanState` (`1.0.0-rc.4`): rc.3's `WorkflowState` scoped to one plan — `workflow`,
  `workflow_version`, `workflow_version_drift`, `state`, `plan`, `steps`, `checks`,
  `gates: [GateResult]`, `approval_chain`, `publications`, `previews`, `preview`,
  `derived_settings`, `cms_objects`, `pending_interaction`, `authorizations`. `WorkflowState` is
  deprecated and equal to `PlanState` for the open, else latest, plan.
- `Session`: `inputs`, `continued_from` (`1.0.0-rc.3`), `open_plan_id`, `executing_plan_id`,
  `plan_count`, `side_run_id` (`1.0.0-rc.4`); `workflow` and `workflow_version` moved to `Plan`.
  `SessionStatus` is `active`, `waiting_for_input` or `archived`. `SessionCreate.workflow` is
  optional and drafts the first plan; `SessionContext.connection_ids` selects the connections the
  session's runs bind to. List filters: `workflow` (any plan of the session), `plan_status`.
- `Run`: `plan_id` (`null` for the session agent and side runs) and `mode` (`session`, `side`,
  `propose`, `execute`, `transition`) (`1.0.0-rc.4`).
- Events: `plan_id` on `BaseEvent`, required on `plan.updated`, `step.updated`, `check.updated`,
  `derived_settings.updated`, `approval_chain.updated`, `preview.updated`, `publications.updated`
  and on `interaction.requested` (`null` for the session agent); `plan.updated` on every lifecycle
  change; `status` kind `context_compacted` (`1.0.0-rc.4`).
- `MessageCreate`: the `plan` user part (`UserPlan.plan_id`, required since `1.0.0-rc.4`, and
  `based_on_version`) with `options.mode: execute` (§6.5).
- `GateResult`: `gate_id`, `label`, `kind`, `blocking`, `when`, `step_id?`, `status`, `verdict?`,
  `attempt` (per plan), `started_at`, `ended_at`, `duration_ms`, `check_ids`, `evidence_file_id?`,
  `run_id?`, `plan_id` (`1.0.0-rc.4`).
- `Check`: `gate_id?`, `kind?`, `evidence?`, `acknowledged_by?`, `acknowledged_at?`, `plan_id` in
  addition to the fields in `03-genaix-api.md` §8. `Publication` carries `plan_id` too.
- `File.artifact_type`: includes `gate_evidence`, stored at `gates/<plan_id>/<gate>-<attempt>.json`.
- Routes: `GET /sessions/{session_id}/plans`, `GET /sessions/{session_id}/plans/{plan_id}`,
  `POST /sessions/{session_id}/plans/{plan_id}/transitions` (`TransitionRequest`: `release`,
  `reopen`, `publish`, `request_review`, `discard`, with `comment`, `at`, `acknowledge_checks`;
  `200` with the `Plan`, `202` with `TransitionAccepted {run_id, plan, session}`).
  `POST /sessions/{session_id}/transitions` keeps `archive` and `unarchive`
  (`SessionTransitionRequest`). `GET /sessions/{session_id}/workflow` is deprecated.
- `GenaixCode`: `quality_gate_failed` (409); `plan_outdated` (409, `1.0.0-rc.3`); since
  `1.0.0-rc.4` `plan_not_found` (404), `plan_closed` (409), `plan_not_proposed` (409),
  `plan_scope_violation` (422, as `tool.failed` followed by a recoverable `error`, never as an HTTP
  answer), `session_archived` (409); `run_already_active` (409) also covers a second side run.
- Problem document for `quality_gate_failed`: `failing_gates[]` and `acknowledgeable_checks[]`.
- `mcp/tools.json` `1.0.0-rc.4`: `genaix_side_tools` gains `load_skill`, `draft_plan`, `recall` and
  `upload_session_file`; the CMS tool `create_folder` is new and the URL source of `upload_file`
  normative.
- Schemas: `schemas/workflow-manifest.schema.json` (with `plan_policy` and `entry_hints` since
  `1.0.0-rc.4`), `schemas/gate-verdict.schema.json`.

No new event type is needed for gates or plans. `check.updated` carries gate-produced checks,
`step.updated` carries the step a gate failed, `plan.updated` carries every plan lifecycle change,
and `GateResult` reaches clients through `GET /sessions/{session_id}/plans/{plan_id}`. A dedicated
`gate.updated` event, so a client can show a gate running before its check exists, is a purely
additive extension if the Workspace UI wants a "checking…" indicator. Per-page publishing adds one
event outside the gate mechanism, `publications.updated` (`1.0.0-rc.3`, with `plan_id` since
`1.0.0-rc.4`).

## 13. Implementation note

The module mechanism reuses building blocks the GenAIx harness already carries rather than replacing
them. The agent runtime sits behind one streaming adapter interface, so remote MCP servers are
configured per run and tool access is locked down by passing an explicit allowlist, loading no
built-in tools and ignoring host-level settings. In-process MCP tools are async handlers registered
on one server namespace with request-scoped state in context variables, the pattern the harness
tools follow. Skills already work as an always-on sitemap plus an on-demand body fetch, so module
skills join that sitemap scoped to their module. Per-session file storage with path-safety primitives
confines `write_artifact` and holds gate evidence, and the pause-and-resume primitive behind
`request_interaction` generalises the existing ask-the-user tool.

What `1.0.0-rc.4` adds is mostly state, not mechanism: a `plans` table and a ledger table beside
the session, `plan_id` on checks, gate results, publications and events, and one guard in the tool
dispatcher that maps a write call to the executing plan's objects through `write_targets` before
the CMS sees it. The **context builder** (§6.1) assembles the current task and the ledger from those
tables on every run, renders recent turns compactly, and summarises older prose at turn boundaries.
`recall` is a read-only query over the same stores, the message history and the retained events;
its index (full text over turns and parts, by plan and by object) is an implementation choice, and
a first version may answer from the stored rows alone.

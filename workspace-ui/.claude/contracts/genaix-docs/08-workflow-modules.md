# Workflow modules and quality gates

A workflow in GenAIx is not a state machine the model interprets. It is a **module**: a directory of
data (manifest, prompts, skills) plus a small amount of code (gate scripts and reviewer
definitions), loaded and executed by the GenAIx harness. The model never reads the manifest, never
decides which gates run, and never decides whether a gate passed.

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
   `report_check`, `request_interaction`, `write_artifact`, `set_derived_settings` — in-process MCP
   tools under the `genaix` namespace, validated before they reach the event log.
4. **The tool surface is the module's, narrowed by the user's permissions** (§6.2), and neither half
   is widenable by the model.
5. **Modules are versioned and pinned.** `workflow_version` is stored on the session at creation, so
   a module upgrade never changes a session already in flight.
6. **Gates fail closed.** A blocking gate that times out, crashes or returns an unparseable verdict
   has failed. Silence is never success.
7. **Nothing in a module is user-authored at runtime.** Modules ship with the deployment and are
   reviewed like any other code. The agent cannot create, edit or disable a gate.

What this buys: the verbatim-text requirement stops being a promise in prose and becomes a sha256
comparison the harness performed, with both digests in the event log. The same mechanism covers link
existence, construct allowlists, Handlebars compilation and preview rendering.

## 2. Module layout on disk

Modules live under a configured directory, `GENAIX_WORKFLOWS_DIR`, default
`<app_root>/workflows/`. One directory per module, named by the module id.

```
workflows/
  content_create/
    manifest.yaml               the whole declaration; the only required file
    prompts/
      system.md                 module system prompt fragment
      step-st-5.md             optional per-step prompt fragment, keyed by step id
      release.md               optional prompt used when the session is released
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
```

Conventions the loader enforces:

- The directory name equals `manifest.yaml`'s `id`, which matches `^[a-z][a-z0-9_]{2,39}$`. Ids are
  public API (`Session.workflow`, `GET /workflows`) and freeze on release.
- A `script` gate has exactly one file, `gates/<gate id>.py` or `.sh`. A `subagent` gate has
  `gates/<gate id>.md` and `gates/<gate id>.yaml`. `prompt` and `human` gates need no file.
- `prompts/system.md` is required; everything else is optional.
- Files are read once at startup and held in memory. Nothing is written into a module directory at
  runtime, and it should be mounted read-only.

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

Three things to read out of that example. `required_connectors` names connector **types**, not URLs:
a module says it needs a `cms` connector, and the session binds one of the user's `cms` connections
to it. `tool_groups` is keyed by the same connector types, plus the reserved key `genaix` for the
in-process harness server, so the module can use the CMS `content_write` group while `tool_denylist`
still removes the three destructive tools this scenario has no business calling. And `allow_mcp` on a
gate is a second, narrower allowlist over the same types: the `verbatim_identical` script may read
the page back and do nothing else.

## 4. Manifest schema

The machine-readable schema is `schemas/workflow-manifest.schema.json` (JSON Schema 2020-12). The
loader validates every manifest against it before registering the module. The fields are those of the
example above; the constraints worth stating in prose are:

| Field | Constraint |
|---|---|
| `id` | `^[a-z][a-z0-9_]{2,39}$`, equal to the directory name, frozen on release |
| `version` | `^\d+\.\d+\.\d+$` |
| `scenario` | `S1`, `S2`, `S3`, `S4` or `none` |
| `roles` | at least one of `content_creator`, `chief_content_creator`, `cmp_administrator`, `any`. Documentation for clients and administrators only: roles never narrow which workflows a subject sees or may start; `Workflow.available` and `capabilities.workflows` depend on authorized connections and configuration alone, and the CMS enforces permissions per tool call (`1.0.0-rc.2`) |
| `produces` | `page`, `construct`, `report`, `user`, `none` |
| `required_connectors[]` | `{type, optional}`; `type` is a connector type slug such as `cms` |
| `tool_groups` | object keyed by connector type plus the reserved `genaix` |
| `steps_template[].id` | `^st-[0-9]+(-[0-9]+)?$`, recursive through `substeps` |
| `quality_gates[]` | `{id, label, kind, blocking, when}` required; `steps`, `inputs`, `timeout_seconds` (1-600), `retries` (0-2), `on_error`, `allow_mcp`, `source`, `description` optional |
| `quality_gates[].kind` | `script`, `subagent`, `prompt`, `human` |
| `quality_gates[].when` | one or more of `step_end`, `before_release`, `before_publish`. The catalogue publishes every trigger as `QualityGate.triggers[]` and the first one as `when` / `step_id` (`1.0.0-rc.2`) |

Two constraints are enforced by the schema rather than by convention: a `prompt` gate cannot be
blocking, because the model's own word is never a gate, and a gate triggered on `step_end` must name
the `steps` it applies to, so a gate cannot silently run after every step.

## 5. Loading, validation and versioning

Discovery happens once at startup, before the HTTP server accepts traffic. The loader scans
`GENAIX_WORKFLOWS_DIR`, reads each `manifest.yaml`, validates it against the schema, and then
performs the semantic checks the schema cannot express:

- every key of `tool_groups` is either a `required_connectors` type or the reserved `genaix`, and
  every named group exists in that connector type's published group list — for `cms` the groups in
  `mcp/tools.json` (`identity`, `search`, `content_read`, `content_write`, `constructs`, `admin`),
  for `genaix` the harness's own;
- every `tool_denylist` entry is a tool the allowlist would otherwise have granted;
- every `quality_gates[].steps` entry exists in `steps_template`, substeps included;
- every `script` and `subagent` gate has its files on disk, and every script gate compiles;
- every `skills` entry resolves to `skills/<name>.md` or to a globally registered skill;
- gate ids are unique within the module.

A module that fails validation is **not registered**, and the failure is logged at error level with
the module id, the failing rule and the file. One broken optional module must not take the
installation down, so startup continues; if a module in the required set (`content_create`,
`content_research`, `construct_create`, `free_chat`) fails, startup aborts. The optional `admin_user`
module's failure is a warning.

The catalogue is what `GET /workflows` serves (field list in §12). Its `quality_gates` entries are
the declaration subset only — `{id, label, kind, blocking, when, description}` — without `inputs`,
`allow_mcp`, `timeout_seconds` or file paths: a client must tell the user what will be checked and
what will block, and thresholds are tuning data rather than contract. `capabilities.workflows` in
`GET /me` narrows the catalogue to what the calling user can run: role match, plus an authorised
connection for every non-optional `required_connectors` type. A module with no authorised `cms`
connection is listed with `available: false` and a `reason`, not hidden, so the client can explain
what to fix.

`POST /sessions` resolves the module once and stores `workflow` and `workflow_version` on the
session; both appear on `Session` and in `GET /sessions/{id}/workflow`, and every run uses the stored
version. Since modules load only at startup, stored and loaded versions can diverge only across a
restart; the harness then logs a warning, uses the loaded module, and sets
`workflow_version_drift: true` on the workflow view. `version` is semver read narrowly: patch for
prompt and threshold changes, minor for additive steps or non-blocking gates, major for a changed
step id, a removed gate or a new blocking gate.

Out of scope for the first release: hot reload, per-installation overrides, modules uploaded through
the API, a gate-authoring UI and module feature flags. Modules ship inside the image, so reloading
means a restart.

## 6. How a module drives a run

### 6.1 System prompt assembly

The harness composes the system prompt in a fixed order, so a prompt diff is readable:

1. the GenAIx base prompt (identity, safety, output conventions), shared by all modules;
2. `prompts/system.md` of the module;
3. the step template as a numbered list carrying the exact `Step.id` values the agent must report,
   and the rule that `report_step` is the only way to move the rail;
4. the session context block: `Session.context` references, `inputs`, the known
   `derived_settings`, and the uploaded files with their `mode` (`source` or `verbatim`);
5. the **gate contract block**: one line per gate with `id`, `label`, `kind`, `blocking` and
   `description`, stating that the harness runs these, that a blocking failure stops the release,
   and that the agent should satisfy them rather than claim they pass;
6. the skill sitemap for this module's `skills` — name plus the "Use when…" description, bodies
   fetched on demand through the `load_skill` tool;
7. the optional per-step fragment `prompts/step-<id>.md`, injected when that step starts.

Telling the agent what the gates are is not the same as relying on it. It raises the first-attempt
pass rate, which is a latency and cost argument, while the verdict still comes from the harness.

### 6.2 Connections and tool allowlist enforcement

A run first resolves connections. For each `required_connectors` type the harness takes the
connection named in `SessionContext.connection_ids`, falling back to the user's `default: true`
connection of that type; a non-optional type with no authorised connection fails the run with
`auth.required {connection_id, connector, reason, how_to_fix}` before any model call.

It then expands `tool_groups` into exact qualified tool names, using the group metadata in
`mcp/tools.json` per connector type and its own registry for `genaix`, subtracts `tool_denylist`, and
passes the result to the agent runtime as the complete set of permitted tools, with no built-in tools
loaded and host-level settings ignored. Each resolved connection becomes one remote MCP server entry
keyed by connector type, carrying **that connection's** URL and **that user's** credential for it:
`{"cms": {"type": "http", "url": <connection.url>, "headers": {"Authorization": "Bearer <this user's
token for this connection>"}}}`. The binding is per run and per user, never shared, and the
connection id is recorded on the run for audit and on `Session.cms_objects[].connection_id` for every
object touched.

A tool outside the module's allowlist is absent from the model's context, not merely denied, so it
cannot be attempted: an agent runtime that loads a server's whole `tools/list` is given the ungranted
names as disallowed tools so they leave the model's context, and an attempt at one is ignored, never
announced on the stream. A tool the user's CMS permissions forbid is absent too, because `cms-mcp`
lists tools dynamically per user and per tool (`04-mcp-scope.md` §2.4). The run sees the
intersection, so the narrower of "what this workflow is for" and "what this person may do on this
connection" always wins. `tool_groups` grants groups; a group with mixed access, such as
`constructs`, brings its reads and its writes, and `tool_denylist` removes the writes a module must
never make (`content_create` grants `constructs` for its catalogue reads and denies its five writes,
`1.0.0-rc.2`).

### 6.3 Harness tools

Six in-process tools form the `harness` group, alongside the existing `genaix` tools for asking the
user, retrieval, reading attachments and loading skills.

| Tool | Effect | Emits |
|---|---|---|
| `report_step` | Set a step's or substep's status and detail. Ids are validated against `steps_template`; an unknown id is a tool error, not an invention. | `step.updated` |
| `update_plan` | Replace the whole plan; the harness increments `plan.version`. | `plan.updated` |
| `report_check` | Report an advisory check. The harness forces `blocking: false` and sets `kind: prompt` with a non-`gate` `source.type`, so a client can tell a self-report from a gate verdict. | `check.updated` |
| `request_interaction` | Ask the user (`ask_user`, `choice`, `confirm`, `form`) and block until answered, timed out or cancelled. | `interaction.requested`, `interaction.resolved` |
| `write_artifact` | Write a path-confined file into `<base>/sessions/<id>/artifacts/`. | `artifact.created`, `artifact.updated` |
| `set_derived_settings` | Propose folder, template, language, filename, url. Fields the user corrected are ignored and reported back as already decided. A module whose `produces` is empty or only `report` has no settings card (`derived_settings` is `null`) and the tool is an error for it. | `derived_settings.updated` |

The asymmetry is deliberate: `report_check` can only produce advisory checks. Blocking checks come
from gates, and gates are not callable by the model.

### 6.4 Step lifecycle and the trigger points

A step moves `pending` → `running` → `done` | `skipped` | `failed`, with `waiting` while an
interaction is open. When the agent reports a step `done` and that step id appears in a gate's
`steps`, the harness holds the tool result, runs the matching `step_end` gates, emits the resulting
`check.updated` and `GateResult` updates, then returns the tool result to the agent — including, for
a failed gate, the verdict summary and findings. The agent learns about the failure immediately and
can fix it in the same run. That is the most useful property of the design: a gate is a feedback
signal during the run, not only a barrier at the end.

`before_release` gates run inside `POST /sessions/{id}/transitions {action: release}`, **before**
the active run is cancelled: a release the gates refuse leaves the run untouched, so a user never
loses a run to a refusal (`1.0.0-rc.2`, aligned with `openapi.yaml`, which has the gates run before
the state change). `before_publish` gates run inside the same route for `action: publish` and
`action: request_review`, before the first CMS write of the transition's run. A transition-time gate
that needs the run's searches or parts gets the latest message run's: the harness keeps every run's
tool calls with their inputs and results, and a `before_release` reviewer re-runs the searches of
the run that produced the report.

The settings review has its own place in this order. A setting the run inferred from the user's
text must be confirmed before the first step that writes to the CMS, so the `settings_review`
interaction lands inside the plan or review step of each showcase scenario — after the reading and
searching steps, which may run on inferred settings, and before the first `content_write` call. Either
side may raise it (`1.0.0-rc.2`): the agent through `request_interaction(kind: settings_review)` with
its own wording and the template options it found, or the harness itself before the first write when
the agent did not ask; one review per run either way, and the write-tool call waits for the answer.
It is not a gate and it needs nothing in the manifest: no `quality_gates` entry, no new trigger point,
no change to the module layout. Quality gates are unaffected by it, and a review that expires or is
cancelled ends the run the same way any unanswered interaction does, with no write having
happened.

### 6.5 Mapping to the API and events

Nothing new appears on the wire beyond the fields in §12. Steps, plan, checks, derived settings and
the approval chain keep their existing events and full-replace semantics (`03-genaix-api.md` §6).
`GET /sessions/{id}/workflow` gains `gates: [GateResult]`, and `Check` gains `gate_id`, `kind` and
`evidence`.

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
  "run_id": "c4e7a2b1-9d3f-4a86-b0c5-7e1f8d2a6b94"
}
```

`status` is the execution outcome (`pending`, `running`, `passed`, `failed`, `skipped`, `error`),
`verdict` the judgement (`pass`, `warn`, `fail`, `error`). They differ when `on_error: warn` turns an
`error` verdict into a non-blocking warning: `status: error`, `verdict: error`, and a `Check` with
`severity: warn`.

A gate-produced `Check` keeps the shape `03-genaix-api.md` §8 defines and adds three fields. Its id
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
  "session": { "id": "9f1b...", "workflow": "content_create", "workflow_version": "1.0.0" },
  "trigger": { "when": "step_end", "step_id": "st-5", "run_id": "c4e7..." },
  "state": {
    "cms_objects": [{ "type": "page", "id": 9142, "node_id": 3, "operation": "created" }],
    "derived_settings": { "template": { "type": "template", "id": 17 }, "language": "de" },
    "files": [{ "id": "6b0d...", "name": "novelle.pdf", "mode": "verbatim", "sha256": "4e1c..." }],
    "verbatim_spans": [{ "file_id": "6b0d...", "start": 10240, "end": 10552, "target_tag": "content_3" }],
    "tool_calls": [{ "tool": "update_page_tags", "ok": true, "objects": [{ "type": "page", "id": 9142 }] }]
  },
  "mcp": { "cms": { "url": "https://cms.example/mcp", "token_env": "GATE_CMS_TOKEN", "allowed_groups": ["content_read"] } },
  "paths": { "session_dir": "/data/sessions/9f1b...", "scratch_dir": "/data/sessions/9f1b.../gates/verbatim_identical-1" }
}
```

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
`publish` and `request_review` transitions, since both cross the same line from draft to something a
reader or reviewer sees.

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
gate in the session, a retry of an infrastructure failure and a transition's re-check included, and
each execution writes its own evidence file (`1.0.0-rc.2`); the workflow view keeps the latest
`GateResult` per gate. `GENAIX_GATE_MAX_ATTEMPTS` (default 3) caps the agent's own `step_end`
re-reports, prevents a loop, and on exhaustion the step stays `failed` and the failure carries into
the transition triggers.

At a transition, `POST /sessions/{id}/transitions` runs the trigger's gates before applying the state
change. If any blocking gate's verdict is `fail` (or `error` with `on_error: fail`), the transition is
refused with `409` and `genaix_code: quality_gate_failed`, carrying the failing set so a client can
render it without another call:

```json
{
  "type": "https://genaix.gentics.com/problems/quality-gate-failed",
  "status": 409,
  "genaix_code": "quality_gate_failed",
  "detail": "2 blocking quality gates failed. Fix them or ask the agent to, then try again.",
  "instance": "/api/v1/sessions/9f1b.../transitions",
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
object the workflow view carries (`1.0.0-rc.2`; an earlier revision of this example showed a
shortened gate shape that `problem.schema.json` never had).

Non-blocking failures never refuse a transition on their own, but they are not silent either. A
transition whose trigger produced unacknowledged non-blocking `warn` or `fail` checks is refused with
the same `409`, an empty `failing_gates` and those checks in `acknowledgeable_checks`; the client
repeats the call with `acknowledge_checks: ["chk-alt-text", ...]` and it proceeds. The
acknowledgement is stored on the session, attributed to the CMS user, and appears on the `Check` as
`acknowledged_by` and `acknowledged_at`. "I saw the warning and published anyway" is an audit fact,
not a UI state.

A blocking check cannot be acknowledged; listing one returns the same `409` with that check still in
`failing_gates`. There is no runtime override. A blocking gate that turns out to be wrong is made
non-blocking in the manifest, which is a reviewable change, rather than bypassed per transition,
which is not.

### 7.5 Evidence capture

Every gate execution writes one evidence record into the session file store as a `File` with
`kind: artifact` and `artifact_type: gate_evidence`, at
`<base>/sessions/<id>/artifacts/gates/<gate id>-<attempt>.json`: the gate declaration, the input
document, the raw verdict, the script's stdout and stderr (64 KB each) or the reviewer's tool-call
trace, and timing. `GateResult.evidence_file_id` points at it; `Check.evidence` is the gate's own
evidence object (its gate-specific keys such as `unit`, `source_sha256`, `images_without_alt`) with
the inline subset `{gate_id, kind, verdict, summary, metrics, finding_count, evidence_file_id,
duration_ms}` laid on top, a key present in both being the subset's (`1.0.0-rc.2`).

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

### 8.2 Research, inventory and reporting, `content_research`

| Gate | Kind | Blocking | When | Inputs | Pass criteria |
|---|---|:-:|---|---|---|
| `citations_resolve` | script | **yes** | `step_end` st-3, st-4 | none | Every `citation` ref and every `table` row reference resolves through `content_read`, and each cited snippet occurs in the cited object's current text. |
| `counts_reproducible` | script | **yes** | `step_end` st-4 | `tolerance: 0` | Replaying each recorded search through `count_content` with the search's own inputs reproduces every total and aggregation bucket the answer states. |
| `result_verification` | subagent | no | `before_release` | reviewer sidecar | A reviewer re-runs a sample of the run's `search_content` calls (default 3, max 5), confirms the reported hits exist and the claims follow from them, and reports discrepancies. |

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
latest message's) and, for every successful `search` call, `tool_calls[].input` and `.result`.

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

### 8.4 Administration, `admin_user` (optional)

| Gate | Kind | Blocking | When | Inputs | Pass criteria |
|---|---|:-:|---|---|---|
| `permission_impact_previewed` | script | **yes** | `step_end` st-2 | none | This run's tool log holds a `preview_permission_impact` result for the exact `(user, add_groups, remove_groups)` about to be applied, with a non-empty before/after matrix. |
| `admin_confirm` | human | **yes** | `step_end` st-2, `before_publish` | rendered diff | The administrator approves the rendered permission diff through `interaction kind=confirm`. A rejection is a `fail` carrying their comment. |

This is the one workflow whose correct gate is a person. A permission grant is small, silent and
consequential, and no script can judge whether this user belongs in that group. What a gate can
guarantee is that nobody grants it blind: the preview must exist, and a human must have seen it.

## 9. Testing a module

Three layers, all runnable without a CMS.

**Gate unit tests** (`tests/gates/test_<gate>.py`) call the gate script with a recorded input
document and assert the verdict. This is where threshold and edge-case behaviour is pinned: a
one-character deviation must fail `verbatim_identical`, a missing alt text must warn and not fail, a
template using `gtx_render` must not be flagged as calling an undefined helper.

**Fixture conversations** (`tests/fixtures/<name>.yaml`) script a whole run: user turns, canned MCP
responses in call order, and the expected step transitions, gate results and terminal session status.
They run against a stub model adapter for harness-only assertions and against `mocks/cms-mcp-mock`
for the MCP-facing path. Minimum per module: a happy path plus one deliberate violation of each
blocking gate.

**Replay harness** (`tests/replay/`) takes an SSE transcript from `examples/`, reconstructs the
session state each gate would have seen at every trigger point (cms objects, files, tool calls, plan,
derived settings), runs the module's script gates against it with `mocks/cms-mcp-mock` serving the
reads, and compares verdicts against `tests/replay/<transcript>.expected.yaml`. Because those
transcripts are the same fixtures the reference client and `mocks/genaix-mock` consume, a gate that
drifts from the documented scenario fails in continuous integration. Subagent gates are skipped in
replay unless `--with-model` is passed, since they cost tokens and are not deterministic.

## 10. Security

Gate execution runs code and talks to the CMS, so its boundaries are stated explicitly.

- **Modules are trusted, reviewed code.** They ship in the image from the repository. No session,
  prompt, uploaded file or MCP response can add, modify or disable a gate. `write_artifact` is
  confined to the session directory; the modules directory is mounted read-only.
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
  write and cannot ask the user anything.

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

The fields this document depends on:

- `Workflow`: `id`, `title`, `description`, `scenario`, `roles`, `optional`, `produces`, `version`,
  `inputs_schema`, `required_connectors` (connector types), `tool_groups`, `steps_template`,
  `quality_gates[{id, label, kind, blocking, when, description}]`, plus `available` and `reason` for
  the `GET /me` narrowing.
- `Session`: `workflow`, `workflow_version`, `inputs`; `SessionContext.connection_ids` selects the
  connections a session's runs bind to.
- `WorkflowState`: `gates: [GateResult]`, `workflow_version`, `workflow_version_drift`.
- `GateResult`: `gate_id`, `label`, `kind`, `blocking`, `when`, `step_id?`, `status`, `verdict?`,
  `attempt`, `started_at`, `ended_at`, `duration_ms`, `check_ids`, `evidence_file_id?`, `run_id?`.
- `Check`: `gate_id?`, `kind?`, `evidence?`, `acknowledged_by?`, `acknowledged_at?` in addition to the
  fields in `03-genaix-api.md` §8.
- `File.artifact_type`: includes `gate_evidence`.
- `POST /sessions/{id}/transitions`: `acknowledge_checks: [check id]`.
- `GenaixCode`: `quality_gate_failed` (409).
- Problem document for `quality_gate_failed`: `failing_gates[]` and `acknowledgeable_checks[]`.
- Schemas: `schemas/workflow-manifest.schema.json`, `schemas/gate-verdict.schema.json`.

No new event type is needed. `check.updated` carries gate-produced checks, `step.updated` carries the
step a gate failed, and `GateResult` reaches clients through `GET /sessions/{id}/workflow`. A
dedicated `gate.updated` event, so a client can show a gate running before its check exists, is a
purely additive extension if the Workspace UI wants a "checking…" indicator.

## 13. Implementation note

The module mechanism reuses building blocks the GenAIx harness already carries rather than replacing
them. The agent runtime sits behind one streaming adapter interface, so remote MCP servers are
configured per run and tool access is locked down by passing an explicit allowlist, loading no
built-in tools and ignoring host-level settings. In-process MCP tools are async handlers registered
on one server namespace with request-scoped state in context variables, the pattern the six harness
tools follow. Skills already work as an always-on sitemap plus an on-demand body fetch, so module
skills join that sitemap scoped to their module. Per-session file storage with path-safety primitives
confines `write_artifact` and holds gate evidence, and the pause-and-resume primitive behind
`request_interaction` generalises the existing ask-the-user tool.

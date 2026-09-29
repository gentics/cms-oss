# Session lifecycle

How a client drives the GenAIx API v1 from start-up to a published page. Read this
next to `requests.http`, which contains the same sequence as runnable requests, and
`s1-content-create.sse`, which is the event stream the middle of this sequence
produces.

Sections 1 and 1b cover identity and MCP connections. The caller is a pseudonymous
**subject**, the CMS is one **connector type**, a subject holds **connections** of that type
with URLs the client supplies, and the client registers each connection's credential.

Integration specifics, such as how a particular client obtains a credential from the CMS and
what the installation's proxy has to do, are in the Integration guide rather than here.

Everything here is defined normatively in `../openapi.yaml`. This file explains the
order and the reasoning.

The API is client-agnostic: it describes data and state, never screens. Nothing below
prescribes a layout, and any remark about what a client does with a piece of data is an
example, not a requirement. Message parts are typed data whose presentation is entirely
the client's decision.

## 1. Who the caller is

Two things identify a request, and there is nothing else.

`Authorization: Bearer sk_gnx_...` is the **installation token**. It identifies one GenAIx
installation, not a person.

`X-GCMS-Subject` is the **only** identity header, required on every user route. Its value is
an opaque, installation-scoped pseudonymous subject: the owner key of every session, file,
connection and authorization, and nothing GenAIx can resolve to a person. No user id, login,
display name or group list is sent, and none is stored. `GET /me` therefore answers
`user: {subject}` and nothing more, and a client that wants to show who is signed in already
knows.

The value may instead be a compact JWS assertion carrying `sub`, `iss`, `iat` and a short
`exp`, and the producing side may put more into that payload. GenAIx does not decode
anything beyond those claims. An installation configured with the signing key has GenAIx
verify the signature and expiry and reject an unsigned, mis-issued or stale value with `401`
and `genaix_code: identity_invalid`; an installation with no key takes the header as sent.
Either way GenAIx trusts it only because the installation token authenticated the caller.

Sessions are owned by (installation token, subject). An unknown or foreign-installation
session id is `404`; a session of another subject under the same installation is `403`.

`DELETE /subjects/{subject}` erases every session, message, event, file, connection and
authorization of one subject. The installation calls it when the person behind that subject
is removed, which is the only way it can start: GenAIx cannot map a person to a subject. It
takes the installation token alone and no identity header, because the subject is in the
path.

## 1a. Start-up: three calls before any session exists

1. `GET /ping` with the installation bearer token only, to check that the token works. It is
   the only user-facing route that does not need `X-GCMS-Subject`.
2. `GET /me` with the installation token plus the subject header. The answer decides
   what a client can offer:
   - `capabilities.workflows` - which workflows the caller may start. The narrowing is
     server-side and combines whether an `authorized` connection covers each required
     connector, the module's own rules and the installation config. Group membership plays
     no part: GenAIx is not told which groups the caller is in, and the target system
     enforces permission on every tool call anyway. A client cannot recompute the list and
     must offer exactly these ids. Re-read them after registering a credential, because a
     connector that starts working widens the list.
   - `capabilities.upload_max_bytes` and `upload_media_types` - what an upload may be,
     so the client rejects an oversized file before sending 40 MB.
   - `mcp_connections` - one line per connection this user holds, with its
     `authorization_status`. `authorized` means the agent can act through it. Anything else
     (`unauthorized`, `pending`, `invalid`, `expired`) means it cannot, until a credential
     is registered. An empty list is normal for a new subject. See section 1b.
3. `GET /workflows` once, cached. Each entry is a workflow **module**: its `version`, the
   `inputs_schema` it expects, the `required_connectors` it binds, the
   `tool_groups` it may use, the `steps_template` to show before the agent reports
   anything, and the `quality_gates` that will be enforced.

## 1b. MCP connections: how GenAIx comes to act as the user

GenAIx reaches the systems an agent works on over MCP, and the model has two levels.

- A **connector** is a *kind* of system. Connector types are defined by GenAIx configuration
  and cannot be added, removed or changed through the API, so `/mcp/connectors` is read-only.
- A **connection** is a user's link to one concrete instance of a type, and **the URL belongs
  to the connection**. A user creates a connection of a type at `/mcp/connections`;
  `multiple` on the type says whether they may hold several.

GenAIx holds one credential per (user, connection), encrypted at rest, and sends it as
`Authorization: Bearer` on every call to that connection's URL. Every read and write
therefore runs as that user, on that instance, and is audited under their name. A credential
is independent of any browser session, so a running agent session outlives the user's
sign-in.

| Call | Purpose |
|---|---|
| `GET /mcp/connectors` | The connector types, and what each needs to be authorized |
| `GET /mcp/connections` | The caller's connections. The first call auto-creates a default when the installation configures one |
| `POST /mcp/connections` | Create one: `{connector, url, label?, default?}` |
| `GET`, `PATCH`, `DELETE` `/mcp/connections/{id}` | Inspect, re-point or rename, delete |
| `GET`, `PUT`, `DELETE` `/mcp/connections/{id}/authorization` | The optional standing credential: check live, register, forget |
| `GET` `/sessions/{id}/authorizations` | The credentials one session holds, checked live |
| `PUT`, `DELETE` `/sessions/{id}/authorizations/{connection_id}` | Register or renew, and forget, the credential this session uses |

**The client registers the credential; GenAIx never creates one.** For a `bearer` connector
the client obtains the token from the target system itself and `PUT`s it to the connection's
authorization resource as `{"auth_type": "bearer", "token": "...", "token_name": "..."}`.

GenAIx verifies the credential against the connection's URL **before** storing it, so a wrong
or foreign token is `422` with `mcp_authorization_rejected` and a previously working
credential is left alone. Read the authorization back afterwards: `identity` carries the
target system's own answer to "who am I", which is the only reliable confirmation that the
credential belongs to the user you think it does, on the instance you think it is.

### URL rules and reachability

`POST /mcp/connections` requires `https`, except that `http` is allowed for `localhost` and
`127.0.0.1`. Credentials embedded in the URL are rejected, because a credential belongs in
the authorization rather than in an address that gets logged, and so are a query string and a
fragment. The reachability probe is **informational**: a connection may be created while its
server is down, and `status.reachable` simply says `false`. The per-user cap is
`max_connections_per_connector`, default 10, and exceeding it is `409` with
`mcp_connection_limit`.

Re-pointing a connection with `PATCH {url}` **drops the stored credential**, because a token
issued by the old system proves nothing about the new one. A `PATCH` that only changes `label`
or `default` keeps it.

### Which connection a session uses

`context.connection_ids` names them. Omitted, a session uses the user's `default` connection
of each connector type the workflow requires. Either way a session **binds one connection per
required type**.

`Workflow.required_connectors` is a list of `{type, optional}` objects, the same shape the
module manifest uses (`schemas/workflow-manifest.schema.json`). A type with `optional: true`
is used when an authorized connection exists and skipped when it does not, so the workflow
still runs; that is how `free_chat` can reach the CMS without requiring it. A type that is not
optional must be covered by an authorized connection, or the workflow is absent from
`capabilities.workflows`.

`CmsObjectRef.connection_id` records which connection an object came from, so an object stays
unambiguous when a user is connected to several instances.

### Where the credential lives

A credential is registered at one of two scopes, and GenAIx creates neither: the client
obtains the token from the target system and registers it.

**Per session** is the integrated path, at
`PUT /sessions/{id}/authorizations/{connection_id}`. The client creates one token per
workflow session, names it after the session, gives it the installation's session
authorization lifetime (24 hours by default) and asks the target system to delete the token
row when it expires; the token request fields are `{name, expires, pruneOnExpiry}`. GenAIx
verifies it live, stores it encrypted and bound to the session, and uses it only for that
session's runs. Sending it on `POST /sessions` as `authorizations[]`, next to `message`,
makes the whole start one round trip.

The credential dies with the task. GenAIx stops using it and deletes it when the session
reaches `published`, `discarded` or `archived`, when the client deletes it, or when
`expires_at` passes. It is kept through `released` and `review_requested`, because
publishing and requesting review still act on the CMS. Nothing long-lived is therefore
stored, and a GenAIx-side incident reaches only the sessions open at the time.

**Standing, per user**, at `PUT /mcp/connections/{id}/authorization`, is optional and stays
for the non-integrated GenAIx UI and for tooling, where nothing mints a token per task. It
survives every session.

Where both exist for a connection, **the session authorization wins**. When neither is
usable, the run emits `auth.required` and fails with `mcp_authorization_required`; the
client registers a fresh credential and re-sends the turn.

The credential is never returned by any route, never logged, and **never reaches the
model**: the harness attaches it to outbound MCP calls outside the model's context, so it
is in no prompt, no event and no message part.

### Authorization types

`auth.type` and the surrounding field names follow the MCP Authorization specification,
revision 2026-07-28. `bearer` is the static token above. `oauth2` is the full flow, with
protected-resource metadata discovery (RFC 9728) and resource indicators (RFC 8707); GenAIx
runs it server-side, so the `PUT` body is just
`{"auth_type": "oauth2", "action": "start"}`, the answer carries an `authorization_url`, and
the status sits at `pending` until the callback completes. `none` is a connector needing no
credential. `resource`, `authorization_servers` and `scopes_supported` on a connector, and
`scopes_granted` on an authorization, carry their RFC meanings unchanged.

## 2. Create the session

`POST /sessions` with `workflow`, `title` and `context`. The workflow id is immutable
afterwards. There are two flows and both are supported.

### One call, when the turn has no attachments

Send `message` on the create call: the same body as `POST /sessions/{id}/messages`, and
`parts` is the shape to send. GenAIx creates the session, derives `intent` from the
message's plain rendering, appends it as the first user turn and starts its run. The `201`
carries `run_id`, `message_id` and `events_url` next to the session, so the client goes
straight to the stream with no second round trip. This is the normal path for a turn the
user just typed.

### Three calls, when the turn has files

Files are **per session**, stored under `<base>/sessions/<session_id>/uploads/`, so there
is no place to put a file before the session exists. A `message` on the create call
therefore cannot reference one: `message.files` and `file_ref` parts inside
`message.parts` are `422`. The order is:

1. `POST /sessions` without `message` - get the id.
2. `POST /sessions/{id}/files` once per file, with `mode: verbatim` for anything whose
   wording must survive unchanged and `mode: source` for material the agent may
   paraphrase.
3. `POST /sessions/{id}/messages` with the returned file ids, as `file_ref` parts or in
   `files`.

A client with drag-and-drop before the user has typed anything must create the session
eagerly on the first interaction. The alternative, a session-less upload endpoint, was
rejected because it has nowhere to store the bytes.

### Intent is derived, never sent

`intent` is the user's original request in their own words, and GenAIx derives it: it is
the plain rendering of the first user message, the same string that message's `content`
carries. It is read-only, it is empty on a session created without a message, and it is
what the session list's `q` searches next to `title`.

`context` is typed throughout. A page the user at-mentioned, a folder they picked in the
tree, a guideline and an uploaded file all arrive as a `ContextReference`, never as
prose inside the message text. That is what lets the agent resolve them through CMS
tools instead of guessing from a string.

## 3. One message, one run

`POST /sessions/{id}/messages` appends the user turn and starts exactly one run.

### The user turn is a list of typed parts

`parts` is the canonical structure of a user message: the elements the user composed, in
order. Six types, and the registry is closed.

| Part | Carries | Renders as | What GenAIx does with it |
|---|---|---|---|
| `text` | `text` | the text itself | the prose of the turn, and the spacing between the other parts |
| `verbatim` | `text`, `source` | the text itself | hashes it and feeds the hash to the `verbatim_identical` gate |
| `reference` | `ref` | `@[label]` | resolves it through the MCP connection and injects the object as context |
| `file_ref` | `file_id`, `mode` | `[[file name]]` | attaches the session file; `mode: verbatim` puts it under the same gate |
| `setting` | `key`, `value`, `label` | `#[key: value]` | makes it a constraint on the run, reflected in `derived_settings` |
| `filter` | `label`, `criteria` | `#[label]` | passes `criteria` into the search tool inputs |

`content` is the plain rendering of the parts: they are concatenated in order with
nothing inserted between them, and a literal `[` or `]` inside a label, a key, a value, a
file name or a text is escaped as `\[` and `\]`. A client with a part-aware composer
sends `parts` and lets the server derive `content`, `references` and `files`. A client
without one sends `content` plus `references` and `files`, and nothing else changes.

A worked turn: text, a folder reference "Campaigns", a language setting, a page reference
"Product launch 2025" and a verbatim title renders as

```
Create a landing page in @[Campaigns] in #[language: de], based on @[Product launch 2025]. The title must read exactly This is our Landingpage.
```

A setting does not have to be a chip. Plain text is the ordinary way to state one —
"auf Deutsch", "unter Richtlinien" — and GenAIx parses those out of the `text` parts
itself. A chip is simply the explicit form, needing no inference. What is inferred is
confirmed before it is used for a write, through the settings review in section 5.

Input parts are **strict**, which is the one place this contract does not degrade
gracefully. A part type outside the registry is `422` with
`genaix_code: validation_failed`, because ignoring it would silently drop what the user
said. Output parts are the opposite: an unknown assistant part falls back to its `text`
(rule 2 in section 4).

### Reading the result

Two ways:

| | `Accept: application/json` (default) | `Accept: text/event-stream` |
|---|---|---|
| Response | `202` with `message_id`, `run_id` | the SSE stream of this run |
| Survives a dropped connection | yes, reconnect to the events route | the events are persisted, so yes, but you must reconnect to the events route anyway |
| Several concurrent clients | one stream serves all of them | one stream each |
| Recommended for | long-lived interactive clients | command-line clients, the reference client |

Only one run may be active per session. A second message while a run is going is `409`
with `genaix_code: run_already_active`; cancel first with
`POST /sessions/{id}/runs/{run_id}/cancel`, which is idempotent. A message whose
`reply_to_interaction` names the waiting run's pending interaction is the exception: it is
accepted and joins the run it unblocks.

`options.mode: plan_only` makes the agent produce a plan and stop without writing to
the CMS. This is the "show me the plan first" path from the showcase scenarios.

## 4. Follow the stream

`GET /sessions/{id}/events` is the canonical stream and the only one a client needs.

- `seq` is a total order per session, starting at 1, gapless, never reused. It is also
  the SSE `id:`, so the browser's `EventSource` sends it back as `Last-Event-ID`
  automatically.
- Reconnect with `?after=<last seq>` or `Last-Event-ID`. The server replays the
  persisted events you missed and then follows live. `after=0` replays everything.
- `410` with `events_pruned` means the replay window has moved past your position.
  Re-hydrate from `GET /sessions/{id}/messages` and `GET /sessions/{id}/workflow`, then
  reconnect with `after=0`. In Phase One this cannot happen: events are kept for the life
  of the session and never pruned, and the status code is reserved so pruning can be added
  later without a breaking change. An implementation that prunes must keep at least seven
  days and must publish its window as `capabilities.events_retention_hours` in `GET /me`.
  An absent field means no pruning.
- A `heartbeat` arrives every 15 seconds of silence. Heartbeats consume a `seq` and are
  persisted, so there is no special case in the `after` arithmetic.
- By default the stream stays open across runs, so the UI holds one connection per open
  task. `?follow=false` closes it after the current run's terminal event.

Five rules, normative. A client that breaks them is not conformant, and the server is
free to add types without further notice.

1. A client **MUST** ignore an event whose `type` it does not know, and **MUST NOT**
   close the stream or discard later events because of it.
2. A client **MUST** tolerate a part whose `type` it does not know: render its `text`
   field when present, otherwise skip the part silently. It **MUST NOT** fail the
   message, drop the surrounding parts, or show raw JSON to the end user.
3. A client **MUST** treat `plan.updated`, `step.updated`, `check.updated`,
   `derived_settings.updated` and `approval_chain.updated` as **idempotent full
   replacements** keyed by id, never as deltas. Applying the same event twice **MUST**
   be a no-op.
4. A client **MUST** apply `part.delta` in `seq` order and **MUST** treat
   `part.completed` as authoritative over its accumulated copy, so a client that missed
   deltas still ends up correct.
5. A client **MUST** tolerate unknown enum members and unknown object properties.

Adding an event type, a part type, an enum member, an optional property or a route is
therefore not a breaking change. Producers include a `text` field on new part types so
that an older client degrades to something readable rather than nothing.

`part.delta` carries a string for `text` and `status_note`, and a JSON merge patch
(RFC 7386) for every structured part. `patch_format` says which, so nothing has to be
inferred from the JSON type. Merge patch replaces arrays wholesale, which is why
structured parts resend the whole `blocks` or `rows` array when one entry changes.

## 5. Interactions: the run blocks, the stream stays open

When the agent needs an answer it emits `interaction.requested` and the run moves to
`waiting_for_input`. The stream does not close; only heartbeats flow until the answer
arrives. Default lifetime is 10 minutes.

Answer either with `POST /sessions/{id}/interactions/{interaction_id}`, or by putting
`reply_to_interaction` on the next message when the user answers in free text instead
of picking one of the offered options. The shape of `answer` follows the interaction
`kind`:
`{"text": ...}` for `ask_user`, `{"selected": [...]}` for `choice`,
`{"approved": ...}` for `confirm`, `{"values": {...}}` for `form`,
`{"settings": [...]}` for `settings_review`.

**The settings review.** `settings_review` is the kind that confirms what GenAIx read out
of the user's words. It carries `proposed` (each entry with its `key`, `value`, `label`,
a `source` of `text`, `context` or `default`, and the quoted `evidence` behind an
inference) and `missing` (what the run needs and could not derive), and the client renders
both as prefilled, editable chips. The answer is a list of `setting` parts covering every
proposed and missing key. GenAIx then stores those parts as a user message carrying
`interaction_id`, names that message in `interaction.resolved.message_id`, and emits
`derived_settings.updated` with the confirmed keys in `corrected_by_user`. The
write-governing settings — `node`, `folder`, `template`, `language`, `publish_at` — must
be a chip, session context or a confirmed review before the first CMS write; at most one
review is raised per run.

`409` means it was already answered, typically a double submit from two tabs. `410`
means it expired and the agent has already been told the question timed out; send a new
message instead of retrying. A review that expires or is cancelled ends the run with no
write having happened.

## 6. The workflow state

`GET /sessions/{id}/workflow` is the snapshot counterpart of the workflow events.
Hydrate from it once after a reconnect and keep it current from the stream afterwards.
It carries:

- `plan` - the visible work plan, versioned. Every change bumps `version` and resends
  the whole plan.
- `steps` - the steps of the workflow, with `substeps` one level deep.
  `status: waiting` means blocked on a user interaction, not on the model.
- `checks` - findings, whether the agent's own or a quality gate's. `blocking` is a
  per-check property, not a function of `severity`: a `fail` may be advisory and a `warn`
  may block. Verbatim proof lives in `check.verification`. A check with
  `source.type: gate` carries `gate_id`, the gate `kind` and the `evidence` the gate
  produced.
- `gates` - one entry per quality gate the workflow module declares, with what happened to
  it here. Every declared gate appears, so the list is renderable before anything has run
  (`status: pending`). Gates have no event of their own: a gate publishes a `Check`, so
  `check.updated` keeps this current.
- `approval_chain` - ordered actor/status/reason steps, resolved server-side from the
  user's CMS permissions on the target folder at call time. This is what lets a client
  state whether publishing is direct or goes to review, and why, instead of relaying a
  flat message.
- `workflow_version` - the module revision this session started under. A session keeps
  reporting against it after the installation upgrades the module.
- `derived_settings` - the folder, template, language and filename the agent derived,
  every one of them correctable, with `permission_used` for the audit trail and
  `corrected_by_user` naming the fields the user decided rather than the agent: those
  sent as `setting` parts, and those confirmed through a settings review.
- `cms_objects` and `preview` - what was touched, and the CMS preview URL.

A user correction goes through `PATCH /sessions/{id}` with a complete `context` object.
The agent sees the change on the next turn and stops re-deriving the fields listed in
`corrected_by_user`.

## 7. Verbatim

Three granularities, all expressible, and `Verification.unit` says which one was
proven:

| Declared where | Unit | Proof |
|---|---|---|
| A whole upload: `mode: verbatim` on `POST /sessions/{id}/files`, or on a `file_ref` part | `blocks` | the extracted text split into blocks, each hashed, every reused block compared |
| A passage: a `verbatim` part on a message | `chars` | character count plus sha256 of source and target text |
| `page_structure` block with `verbatim: true` | `blocks` | per-block comparison |
| A file reused as a file | `files` | sha256 of the bytes |

Uploaded files are verbatim-capable whatever their format, PDF included, because **text
extraction runs in GenAIx, not in the model**. A PDF, a Word document or an HTML page
becomes text before the agent sees anything: the model never receives raw binary, and the
hashes on both sides come from the same extracted text.

The two file paths are distinct and both are available:

- **Lock the whole document.** Upload it with `mode: verbatim`, or put it in the turn as
  `file_ref {file_id, mode: verbatim}`. Every block of its extracted text is hashed and
  the gate verifies each reused block appears unchanged.
- **Lock one passage of it.** Send a `verbatim` part with `source: <file_id>`. GenAIx
  checks the passage really occurs in that file's extracted text and then hashes it like
  user-typed verbatim. This works whatever the file's own mode, so a passage of a
  `mode: source` document can be locked without locking the document.

A `mode: source` file with no `verbatim` part quoting it is reference material only: the
agent may read, summarise and paraphrase it, and nothing about it is locked.

The result is a `Check` with `blocking: true` and a `verification` object. `identical`
is the single boolean that says whether the lock held; `matched_units` and
`total_units` explain it; `deviations` lists the first differences when it failed. See `chk-verbatim-legal`
in `s1-content-create.sse`.

## 8. Release, review, publish

`POST /sessions/{id}/transitions` is the single route for every status change.

```
active <-> waiting_for_input
active  -> released            (release: runs the gates, then cancels the run)
released -> published | review_requested | active | discarded
any      -> archived
```

`release`, `reopen`, `discard` and `archive` are synchronous and return the updated
`Session`; a repeated `discard` or `archive` is `200` with no new event. `publish` and
`request_review` start a run and return `202` with a `run_id` and the session as it is at
that moment (`released`), because the write goes through the session's CMS connection;
`request_review` calls the CMS only for a user who cannot publish directly, since the CMS
queues a page for approval through `publish_page` and has no review-request call of its
own. Whether the page is published
directly or only submitted for review is **not** a client decision and not a cached
flag: the server resolves it from the user's permissions on the target folder and
reports the outcome in the approval chain. A `publish` that the CMS converts into a
review request still returns `202`; the final session status is `review_requested`.

`at` on a `publish` schedules it, which is the "publish on 1 January at midnight, when
the update comes into force" case.

**Quality gates guard the transition.** Before `release`, `publish` and `request_review`
the harness runs the module's gates for that moment and re-checks the earlier ones. The
transition is refused with `409` and `genaix_code: quality_gate_failed` while **any**
gate-sourced check is failing, blocking or not. The two kinds clear differently:

- **Blocking** cannot be overridden. Fix the finding, or have the agent fix it; the gate
  re-runs and the transition is then allowed. Listing a blocking check in
  `acknowledge_checks` changes nothing.
- **Non-blocking** must be acknowledged explicitly. Repeat the call with the ids in
  `acknowledge_checks` and it proceeds, with the acceptance recorded on each `Check` as
  `acknowledged_by` and `acknowledged_at`, attributed to the CMS user.

So a warning neither disappears silently nor blocks forever. The problem document carries
`failing_checks` (every failing check), `failing_gates` (the blocking ones) and
`acknowledgeable_checks` (the ids you may pass back), which is enough to render the refusal
without another call.

Gates exist because a model's own claim is not evidence. A `script` gate hashes the
verbatim text, compiles the Handlebars template, resolves the references. A `subagent` gate
is a separate reviewer with read-only tools. A `human` gate is an explicit confirmation. A
`prompt` gate is a model self-check and is never blocking.

The catalogue lives in `08-workflow-modules.md`, and the gate ids are snake_case. A
gate-produced check is named `chk-<gate-id-with-hyphens>`, so `verbatim_identical` reports
`chk-verbatim-identical`. A gate whose manifest names a guideline keeps
`source.type: guideline` and that guideline's id while still carrying `gate_id`, `kind` and
`evidence`: `chk-alt-text` in the S1 transcript is exactly that case. Gates without such a
block get `source.type: gate`.

`GateResult` separates `status`, the execution outcome, from `verdict`, the judgement, and
carries `attempt`, because a gate re-runs after the agent fixes what it reported. Its full
verdict shape is `schemas/gate-verdict.schema.json`, and the manifest behind a module is
`schemas/workflow-manifest.schema.json`; both are standalone files next to the three the
API generates.

`failed` is not a session status. A failed run leaves the session `active` so the user
can retry; the failure lives on the `Run` and in the `run.failed` event.

## 9. Errors

Every error is an RFC 9457 `application/problem+json` document. Branch on
`genaix_code`, not on `type` or `status`.

| Status | When | Typical `genaix_code` |
|---|---|---|
| 400 | malformed request, or the subject header is missing | `malformed_request`, `invalid_cursor`, `subject_missing` |
| 401 | the subject assertion is unsigned, stale or fails verification | `identity_invalid` |
| 401 | installation token missing, unknown or revoked | `invalid_installation_token` |
| 403 | exists, but belongs to another subject | `session_forbidden` |
| 404 | unknown id, or an id of another installation or user | `session_not_found`, `mcp_connection_not_found` |
| 409 | state conflict, a blocking gate, or too many connections | `run_already_active`, `session_released`, `workflow_transition_invalid`, `quality_gate_failed`, `mcp_connection_limit` |
| 410 | gone | `interaction_expired`, `events_pruned` |
| 413 | upload over the limit | `file_too_large` |
| 422 | body failed validation, or a server refused a credential | `validation_failed`, `unsupported_media_type`, `mcp_authorization_rejected` |
| 423 | CMS object locked by someone else | `cms_object_locked` |
| 424 | an MCP connection is unusable, or a CMS call failed | `mcp_authorization_required`, `mcp_unreachable`, `cms_request_failed` |
| 429 | rate limited | `rate_limited` |
| 503 | GenAIx cannot serve, retry | `service_unavailable` |

404 versus 403 is deliberate: unknown ids and ids belonging to a different installation
are indistinguishable, so nothing leaks across installations, while a session of a
different user under the same installation is 403 because the client may usefully say
so. This matches the conventions of the current GenAIx `/api/v1`.

Mid-run failures arrive as events rather than status codes. `tool.failed` is usually
recoverable - the agent sees the error and may take another route. `error` with
`recoverable: false` is followed immediately by `run.failed`, whose `error` is the same
Problem document the REST API would have returned.

`auth.required` is the credential case and deserves its own note. It names the
`connection_id` and `connector` the run could not use and a `reason` of `unauthorized`,
`invalid`, `expired` or `unreachable`, with `how_to_fix` naming the route. The run then **fails** with `424`
and `genaix_code: mcp_authorization_required`. There is deliberately no pause-and-resume:
the run does not sit in `waiting_for_input` waiting for a credential. Recovery is two
calls, register then re-send:

1. `PUT /sessions/{id}/authorizations/{connection_id}` with a fresh credential, or
   `PUT /mcp/connections/{connection_id}/authorization` when the client works from a
   standing one.
2. `POST /sessions/{id}/messages` with the message that failed.

`s5-failure-and-recovery.sse` is that whole sequence as a transcript, together with the
other unhappy paths: a tool failing on a locked CMS page, a recoverable `error`, and a
run the user stopped.

Nothing the run already wrote to the CMS is rolled back, and the session keeps its
history, so re-sending continues rather than restarting.

## 10. Reconnect checklist

A client that lost its connection, or a second client opening the same session, does
exactly this:

1. `GET /sessions/{id}` - status, and `active_run_id` to know whether to expect live
   events.
2. `GET /sessions/{id}/messages` - history: assistant turns as the same parts the stream
   sent, user turns as the same parts the client sent.
3. `GET /sessions/{id}/workflow` - plan, steps, checks, approvals, derived settings,
   preview, `pending_interaction` if the run is blocked on a question, and
   `authorizations` to see whether the session can still act.
4. `GET /sessions/{id}/events?after=<highest seq you have>` - or `after=0` when you
   have none.
5. If an `authorizations` entry is `expired` or `invalid`, or a required connection has no
   entry and no standing authorization, register a fresh credential with
   `PUT /sessions/{id}/authorizations/{connection_id}` before sending the next turn. A
   session credential can have run out while the client was away.

## Files in this directory

| File | What it is |
|---|---|
| `requests.http` | Every operation as a runnable request, in lifecycle order |
| `s1-content-create.sse` | S1, content creation from source material. 90 events: plan, steps with substeps, CMS writes, a streamed `page_structure` part, the verbatim proof, a `settings_review` interaction confirming folder, language and template, previews, an artifact |
| `s2-content-research.sse` | S2, research and reporting. 43 events: search calls, a streamed `table` part, citations, no CMS writes |
| `s3-construct-create.sse` | S3, construct creation. 56 events: catalogue check, a streamed `construct_draft` part, Handlebars validation, a `confirm` interaction, `create_construct`, an `api_call_log` part, preview |
| `s4-admin-user.sse` | S4, optional. 39 events: group lookup, a `form` interaction, permission impact simulation, user creation, group assignment |
| `s5-failure-and-recovery.sse` | Not a showcase scenario, the unhappy paths. 51 events across two runs on one stream: `tool.failed` on a locked page, a recoverable `error`, `artifact.created`, `tool.failed` plus `auth.required` plus `run.failed` on a dead connection credential, then after re-registering it a second run with `artifact.updated` and `run.cancelled` |
| `session-lifecycle.md` | This file |

Every `data:` payload in the five transcripts validates against
`../schemas/events.schema.json`, every nested timestamp is at or before the envelope `ts`
of the event carrying it, and `seq` runs gapless from 1 in each file.

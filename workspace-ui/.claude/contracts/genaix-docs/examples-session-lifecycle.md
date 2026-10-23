# Session lifecycle

How a client drives the GenAIx API v1 from start-up to a published page. Read this
next to `requests.http`, which contains the same sequence as runnable requests, and
`s1-content-create.sse`, which is the event stream the middle of this sequence
produces. Section 11 continues past the first page: accepting a plan, a second plan in the
same conversation that edits pages, publishing them per plan, and one plan revised as the user
changes their mind (`s7-continuous-session.sse`, `s6-content-edit.sse`, `s8-living-plan.sse`).

This describes `1.0.0-rc.5`, agreed by all parties on 2026-10-15. Its one idea: **a session is one conversation, and the work it
carries out is a set of living plans.** A plan covers one piece of work, uses one or more
workflow modules and has its own steps, checks, gates, derived settings, previews, publications
and lifecycle. A follow-up on the same work **revises** the plan as a new version with the same
id, instead of opening another (since `1.0.0-rc.5`; rc.4 froze a plan once it was accepted). A
session has no workflow of its own; it starts with the **session agent**, which reads and
answers freely, drafts a plan when the user asks for new work and revises one when the user
changes their mind about it.

Sections 1 and 1b cover identity and MCP connections. The caller is a pseudonymous
**subject**, derived from a CMS-signed assertion in production, the CMS is one **connector type**, a subject holds **connections** of that type
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

Two things identify a request, the installation and the user, and there is nothing else. How
they travel depends on the **caller mode** (`1.0.0-rc.3`), told apart by whether
`X-GenAIx-Installation` is present.

**Direct mode**, which this walkthrough uses, is for development, the mocks, tooling and the
conformance suite:

- `Authorization: Bearer sk_gnx_...` is the **installation token**. It identifies one GenAIx
  installation, not a person.
- `X-GCMS-Subject` carries the **subject**, required on every user route and used verbatim:
  an opaque, installation-scoped pseudonym, the owner key of every session, file, connection
  and authorization.

Direct mode is accepted only by an installation **without** a CMS key configured. An
installation with one answers a direct request on a user route with `401` and
`genaix_code: identity_invalid`.

**Proxied mode** is production, the path the Workspace UI takes. The browser calls the CMS's
existing custom proxy, which adds two headers of its own and replaces any the browser sent
under those names:

- `X-GenAIx-Installation: sk_gnx_...`, the installation token, a static header in the proxy
  configuration;
- `Authorization: Bearer <CMS identity assertion>`, an RS256 JWT the CMS signs per request.

GenAIx verifies the assertion against the installation's configured CMS public keys (`iss`
must be the expected issuer, `iat` within 60 seconds, `exp` and `nbf` honoured when present)
and derives the subject from its `sub` claim with a keyed one-way hash under a secret only
GenAIx holds. The same login therefore is the same subject from any device. The assertion
lives for the request only: never stored, logged, emitted or passed to the model. Of its
claims GenAIx uses `sub` alone, and it keeps nothing that names the person. `X-GCMS-Subject`
is ignored on a proxied request. A proxied request on a user route without an assertion is
`400 subject_missing`; an assertion that fails verification, or a proxied request to an
installation without a CMS key, is `401 identity_invalid`.

Everything after this section looks the same in both modes: only the headers differ. No user
id, login, display name or group list reaches the API's responses, and none is stored.
`GET /me` therefore answers `user: {subject}` and nothing more, and a client that wants to show
who is signed in already knows.

The proxy forwards only `DELETE`, `GET`, `HEAD`, `OPTIONS`, `POST` and `PUT`. Every `PATCH`
route therefore also accepts `POST` with `X-HTTP-Method-Override: PATCH`, with the same
meaning; this walkthrough writes `PATCH`, and a proxied client sends the override instead.

Sessions are owned by (installation token, subject). An unknown or foreign-installation
session id is `404`; a session of another subject under the same installation is `403`.

`DELETE /subjects/{subject}` and `POST /subjects/erase` erase every session, message, event,
file, connection and authorization of one subject. The installation calls one of them when it
deletes the user behind that subject, which is the only way erasure can start: GenAIx cannot
map a person to a subject. Both are installation-level: they take the installation token in
either header and no identity. `POST /subjects/erase` takes `{"cms_sub": "<login as in the
assertion's sub>"}` or `{"subject": "..."}`, so the CMS can erase by login without putting it
in a URL.

## 1a. Start-up: three calls before any session exists

1. `GET /ping` with the installation token only, to check that the token works. Like the two
   erasure routes it is installation-level and needs no identity.
2. `GET /me` with the installation token plus the identity (the subject header here, the
   assertion when proxied). The answer decides
   what a client can offer:
   - `capabilities.workflows` - which workflows a plan of the caller may run under. The narrowing is
     server-side and combines whether an `authorized` connection covers each required
     connector, the module's own rules and the installation config. Group membership plays
     no part: GenAIx is not told which groups the caller is in, and the target system
     enforces permission on every tool call anyway. A client cannot recompute the list and
     must offer exactly these ids. Re-read them after registering a credential, because a
     connector that starts working widens the list. The session agent is never in it: it is
     not a catalogue entry, and it is always there (since `1.0.0-rc.4`; `free_chat` was its
     predecessor and is gone). Without an authorized `cms` connection it answers, but cannot
     read the CMS.
   - `capabilities.upload_max_bytes` and `upload_media_types` - what an upload may be,
     so the client rejects an oversized file before sending 40 MB.
   - `mcp_connections` - one line per connection this user holds, with its
     `authorization_status`. `authorized` means the agent can act through it. Anything else
     (`unauthorized`, `pending`, `invalid`, `expired`) means it cannot, until a credential
     is registered. An empty list is normal for a new subject. See section 1b.
3. `GET /workflows` once, cached. Each entry is a workflow **module** a plan can run under:
   its `version`, the `inputs_schema` it expects, the `required_connectors` it binds, the
   `tool_groups` it may use, the `steps_template` to show before the agent reports
   anything, the `quality_gates` that will be enforced, `plan_approval` and `plan_policy`
   (`on_discard`; rc.4's `concurrency` is gone since `1.0.0-rc.5`, section 8), and what it
   `produces`, which a plan's proposing run picks modules by. No client picks one before a
   conversation starts, and one plan may use several.

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
credential is left alone. GenAIx verifies with the server's `whoami` and keeps the answer to
itself (since `1.0.0-rc.2`); a client that wants to confirm the credential belongs to the user
it thinks, on the instance it thinks, asks the target system itself with that token before
registering it.

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
of each connector type its plans' workflows require. Either way a session **binds one connection
per required type**.

`Workflow.required_connectors` is a list of `{type, optional}` objects, the same shape the
module manifest uses (`schemas/workflow-manifest.schema.json`). A type with `optional: true`
is used when an authorized connection exists and skipped when it does not, so the workflow
still runs. A type that is not optional must be covered by an authorized connection, or the
workflow is absent from `capabilities.workflows`. The session agent needs no entry here: it
reads through the session's `cms` connection when that one is authorized.

`CmsObjectRef.connection_id` records which connection an object came from, so an object stays
unambiguous when a user is connected to several instances.

### Where the credential lives

A credential is registered at one of two scopes, and GenAIx creates neither: the client
obtains the token from the target system and registers it.

**Per session** is the integrated path, at
`PUT /sessions/{id}/authorizations/{connection_id}`. The client creates one token per
session, names it after the session, gives it the installation's session
authorization lifetime (24 hours by default) and asks the target system to delete the token
row when it expires; the token request fields are `{name, expires, pruneOnExpiry}`. GenAIx
verifies it live, stores it encrypted and bound to the session, and uses it only for that
session's runs, whatever plan they work for. Sending it on `POST /sessions` as
`authorizations[]`, next to `message`, makes the whole start one round trip.

The credential lives as long as the conversation. GenAIx stops using it and deletes it when
the session is archived or deleted, when the client deletes it, or when `expires_at` passes
(since `1.0.0-rc.4`; before, also when the session was `published` or `discarded`). A plan's
transitions never touch it: the next plan of the conversation acts with the same credential,
and a conversation that outlives the token's lifetime registers a fresh one. Nothing
long-lived is therefore stored, and a GenAIx-side incident reaches only the sessions open at
the time.

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

`POST /sessions` with `title` and `context`, and usually nothing else: without `workflow`
the session starts with the session agent, which reads, answers, and drafts a plan when the
user asks for a change. That is the normal start of a conversation (since `1.0.0-rc.4`).
`workflow` is **optional** and meant for a CMS context action such as "Create page here": with
it, the session's first plan is drafted at once under that module and the first message goes
to that plan's proposing run, which may add the further modules the work needs. Either way the
session holds no workflow of its own afterwards; each plan names its modules, in run order, in
`Plan.workflows` (since `1.0.0-rc.5`; `Plan.workflow` and `workflow_version` before), and a
`PATCH` carrying `workflow` is `422`.

Optional `labels` tag the session for the client's own grouping, and an optional
`continued_from` names an earlier conversation this one builds on (section 11.5). There are
two flows and both are supported.

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
order. Seven types, and the registry is closed.

| Part | Carries | Renders as | What GenAIx does with it |
|---|---|---|---|
| `text` | `text` | the text itself | the prose of the turn, and the spacing between the other parts |
| `verbatim` | `text`, `source` | the text itself | hashes it and feeds the hash to the `verbatim_identical` gate |
| `reference` | `ref` | `@[label]` | resolves it through the MCP connection and injects the object as context |
| `file_ref` | `file_id`, `mode` | `[[file name]]` | attaches the session file; `mode: verbatim` puts it under the same gate |
| `setting` | `key`, `value`, `label` | `#[key: value]` | makes it a constraint on the run, reflected in `derived_settings` |
| `filter` | `label`, `criteria` | `#[label]` | passes `criteria` into the search tool inputs |
| `plan` | `plan` (`plan_id`, `based_on_version`, `items?`) | a numbered list of the item labels, or `Plan accepted as proposed.` without `items` | the only way to accept a plan: without `items` as proposed, with `items` as the user adjusted it (section 11) |

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

### Who answers, and runs side by side

A message goes to the **session agent** unless it accepts a plan, answers a plan run's
interaction or redirects an executing plan (since `1.0.0-rc.4`; redirects since `1.0.0-rc.5`).
The session agent reads and answers. When the user asks for new work, with unrelated objects
and an unrelated goal, it drafts a plan (`draft_plan`); when the message is about the objects
or the goal of an existing plan, it revises that plan (`revise_plan`, since `1.0.0-rc.5`).
Either way the plan's proposing run follows in a run of its own. `Run.mode` says which kind of
run a run is, `Run.plan_id` which plan it works for, and `Run.workflow` which module an
`execute` run carries out (since `1.0.0-rc.5`; `null` for every other mode):

| `mode` | Run | `plan_id` |
|---|---|---|
| `session` | the session agent: reads, answers, drafts or revises a plan; no CMS write tool | `null` |
| `side` | the session agent, read-only, while a plan's run is active | `null` |
| `propose` | a plan's proposing run, for a new plan or a revision: reads, picks the modules, plans; every write withheld | the plan |
| `execute` | a plan's run after the user accepted it, one per contiguous segment of one module's pending items: the only mode with CMS write tools | the plan |
| `transition` | the run of a plan's `publish` or `request_review` | the plan |

`options.mode: revise` (since `1.0.0-rc.5`) makes a message a revision of the plan
`options.plan_id`, by default `Session.open_plan_id`, whatever the session agent would decide.
Every plan but a `discarded` or `superseded` one can be revised; `409` with
`plan_not_revisable` otherwise, `404` with `plan_not_found` for a plan the session does not
have.

**Reading never waits for a plan.** While a plan's run is active, a question starts a **side
run**: the session agent with read tools and `recall` only, named by `Session.side_run_id`
next to the plan's `active_run_id`, so the user can keep asking while long work runs. At most
one side run at a time. A message that accepts a plan, or answers the plan run's interaction,
is never a side run.

**Changing direction never waits either** (since `1.0.0-rc.5`). A message while a plan executes
that changes what the plan does ("stop, use red instead") **redirects** it; GenAIx decides
between question and redirect, and `options.mode: revise` forces the redirect. The cancel is
requested at once; the executing run stops at the next tool boundary, never inside a CMS lock
cycle, and ends with `run.cancelled`, `run.cancel_reason: redirected`; the plan's proposing run
then starts with the message. Items whose write completed stay `done`, the item the run was
working on is `pending` again, and a step the cancelled segment did not finish ends `skipped`,
its `step_end` gates not run. A `drafting` plan is redirected the same way: its proposing run
is cancelled and started again with the message. S8 shows a redirect (section 11.4).

`409` with `genaix_code: run_already_active` is left for three cases: a message while the
session agent's own run is going, a second side run, and a message that accepts or adjusts a
plan (a `plan` part) while a run of that or any other plan of the session is active (at most one
plan executes, though several may be open). Wait, or cancel with
`POST /sessions/{id}/runs/{run_id}/cancel`, which is idempotent. A message whose
`reply_to_interaction` names the waiting run's pending interaction is always accepted and joins
the run it unblocks. A plan's status never blocks a message; an archived session does, with
`409` and `session_archived`.

`options.mode: plan_only` makes the agent produce a plan and stop without writing to the CMS,
for any workflow. A module with **plan approval** (`content_create`, `content_edit`;
`1.0.0-rc.3`) does that on its own: its proposing run ends at the proposed plan, and only the
user's `plan` part accepts it, as proposed (without `items`) or adjusted. A module
without a CMS write tool (`content_research`) needs no acceptance at all, because reading is
free. Section 11 walks through it.

## 4. Follow the stream

`GET /sessions/{id}/events` is the canonical stream and the only one a client needs.

- `seq` is a total order per session, starting at 1, gapless, never reused. It is also
  the SSE `id:`, so the browser's `EventSource` sends it back as `Last-Event-ID`
  automatically.
- Reconnect with `?after=<last seq>` or `Last-Event-ID`. The server replays the
  persisted events you missed and then follows live. `after=0` replays everything.
- `410` with `events_pruned` means the replay window has moved past your position.
  Re-hydrate from `GET /sessions/{id}/messages`, `GET /sessions/{id}/plans` and the plan
  snapshots, then reconnect with `after=0`. In Phase One this cannot happen: events are kept
  for the life of the session and never pruned, and the status code is reserved so pruning
  can be added later without a breaking change. An implementation that prunes must keep at
  least seven days and must publish its window as `capabilities.events_retention_hours` in
  `GET /me`. An absent field means no pruning.
- Event retention is about replaying the stream, not about what the agent remembers. The
  record of every plan and every user decision (acceptances and adjustments, confirmed
  settings, interaction answers, transitions with their comments) is GenAIx's **ledger**:
  written as it happens, kept for the session's lifetime, never compacted and never shortened
  by event retention (since `1.0.0-rc.4`). The plans themselves stay readable through
  `GET /sessions/{id}/plans` however old the session is.
- A `heartbeat` arrives every 15 seconds of silence. Heartbeats consume a `seq` and are
  persisted, so there is no special case in the `after` arithmetic.
- By default the stream stays open across runs, so the UI holds one connection per open
  conversation, whatever plans it goes through. `?follow=false` closes it after the current
  run's terminal event.
- Plan-scoped events carry `plan_id` (since `1.0.0-rc.4`): `plan.updated`, `step.updated`,
  `check.updated`, `derived_settings.updated`, `approval_chain.updated`, `preview.updated`,
  `publications.updated`, and `interaction.requested`, where `null` means a question of the
  session agent. Keep plan state per plan, keyed by `plan_id` plus the object's own id: step
  `content_edit/st-1` of one plan is not step `content_edit/st-1` of the next. Since
  `1.0.0-rc.5` step and check ids are always `<workflow>/<id>` and the objects carry
  `workflow`, so the steps and checks of two modules of one plan never collide and an id stays
  stable when a revision adds a module. An event without `plan_id`, replayed from a session
  older than `1.0.0-rc.4`, belongs to that session's only plan.
- `status` with `kind: context_compacted` says that older turns were summarised for the
  agent's context at a turn boundary (never while a plan executes). Nothing was deleted: the
  messages, the plans and the ledger stay exact, and the agent's `recall` tool returns them
  exactly. A client may show it or ignore it.

Five rules, normative. A client that breaks them is not conformant, and the server is
free to add types without further notice.

1. A client **MUST** ignore an event whose `type` it does not know, and **MUST NOT**
   close the stream or discard later events because of it.
2. A client **MUST** tolerate a part whose `type` it does not know: render its `text`
   field when present, otherwise skip the part silently. It **MUST NOT** fail the
   message, drop the surrounding parts, or show raw JSON to the end user.
3. A client **MUST** treat `plan.updated`, `step.updated`, `check.updated`,
   `derived_settings.updated`, `approval_chain.updated` and `publications.updated` as
   **idempotent full replacements** keyed by `plan_id` and id, never as deltas. Applying the
   same event twice **MUST** be a no-op.
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
review is raised per plan version (since `1.0.0-rc.5`; per run before), covering the keys the
modules of its pending items need. A `missing` entry may carry `recommended` and its `reason`,
and `other_allowed: true` when its `options` are suggestions. `file_folder` and
`image_folder` (since `1.0.0-rc.5`) are the advisory folder keys of an upload: asked when the
plan uploads a file or an image and the user named no folder, recommending the node's
`defaultFileFolderId` or `defaultImageFolderId`, and listed in `corrected_by_user` once
confirmed. S8's third version asks `file_folder`.

A `choice` may say which option GenAIx recommends and why (`recommended`, an option `reason`) and
allow the user's own words (`other_allowed`, answered as `other`, with `selected` then empty),
since the `1.0.0-rc.5` addition, 2026-10-15.

`409` means it was already answered, typically a double submit from two tabs. `410`
means it expired and the agent has already been told the question timed out; send a new
message instead of retrying. A review that expires or is cancelled ends the run with no
write having happened.

## 6. The plans and their state

Two reads, both since `1.0.0-rc.4`:

- `GET /sessions/{id}/plans` - every plan of the session, oldest first, in short
  (`PlanSummary`: `id`, `workflows` as module ids, `status`, `version`, `summary`,
  `item_count`, `publication_counts`, `created_at`, `accepted_at`, `closed_at`). These are the
  plan cards of the conversation and the record of its closed work.
- `GET /sessions/{id}/plans/{plan_id}` - the `PlanState` of one plan, the snapshot counterpart
  of the events carrying its `plan_id`; `404` with `plan_not_found` for a plan the session
  does not have. Hydrate from it once after a reconnect and keep it current from the stream
  afterwards.

`GET /sessions/{id}/workflow` is **deprecated**: it returns the `PlanState` of the open plan,
else of the latest one, else `404`, so a client written for one workflow per session keeps
working on a session with a single plan. `Session.open_plan_id` (the plan the conversation
currently works on), `open_plan_ids` (every open plan, since `1.0.0-rc.5`) and
`executing_plan_id` say which plans matter now.

A `PlanState` is what `rc.3` called the session's `WorkflowState`, scoped to one plan. It
carries:

- `plan` - the visible work plan, versioned, with its `id`, `workflows`, `status` and the
  acceptance fields (`accepted_version`, `accepted_by`, `accepted_at`, `closed_at`). A change
  of the items or the summary bumps `version` (an adjusted acceptance, a revision, a deviation);
  a change of `status` alone keeps it, so an acceptance as proposed keeps the version. While a
  revision is drafted the plan keeps its current version and items. Either way `plan.updated`
  resends the whole plan. Since `1.0.0-rc.5` it carries `versions`, one `PlanVersion` per
  version (optional `kind`: `proposal`, `revision`, `adjustment` or `deviation`; `reason`,
  `created_at`, `accepted_at`, `accepted_by` and how many items it added, changed and removed
  against the version accepted before it), and every item names its `workflow`, the `version`
  that added it, its `change` against the version accepted before the current one (`added`,
  `changed`, `removed`, `unchanged`; kept as accepted until the next revision; `added` for
  every item of a plan never accepted) and the items it `depends_on`.
- `steps` - the steps of the plan's modules, in plan order, with `substeps` one level deep.
  Each carries its `workflow`, and its id is `<workflow>/<id>`; a step of no module, such as
  a transition run's `plan/st-publish`, is `plan/<id>` without `workflow`. A step shows its
  latest state; there is no step history per version.
  `status: waiting` means blocked on a user interaction, not on the model.
- `checks` - findings, whether the agent's own or a quality gate's. `blocking` is a
  per-check property, not a function of `severity`: a `fail` may be advisory and a `warn`
  may block. Verbatim proof lives in `check.verification`. A check with
  `source.type: gate` carries `gate_id`, the gate `kind` and the `evidence` the gate
  produced. A gate's check carries `workflow` and the id `<workflow>/chk-<gate>`; every other
  check id is `<workflow>/<id>` of the module whose run reported it, `plan/<id>` for none.
- `gates` - one entry per quality gate the plan's workflow modules declare, each with its
  `workflow` and run over that module's items, with what happened to it in this plan;
  `attempt` counts per plan, `1` for the first execution, and a gate's evidence is stored as
  `gates/<plan_id>/<workflow>/<gate>-<attempt>.json`. Every declared gate appears, so the list is
  renderable before anything has run (`status: pending`). Gates have no event of their own: a
  gate publishes a `Check`, so `check.updated` keeps this current.
- `approval_chain` - ordered actor/status/reason steps, resolved server-side from the
  user's CMS permissions on the target folder at call time. This is what lets a client
  state whether publishing is direct or goes to review, and why, instead of relaying a
  flat message. With several pages it is the first page's chain.
- `publications` - one entry per page the plan created or changed, each with its own
  `status` (`pending`, `published`, `review_requested`, `failed`) and its own approval
  chain, resolved from the permissions on that page's folder (`1.0.0-rc.3`; per plan since
  `1.0.0-rc.4`). Publications and checks accumulate over the plan's versions: a page written
  by version 1 and again by version 4 is one entry. Pages another plan wrote are not in it.
- `workflows` - the modules this plan uses, in run order, each with the revision it was
  pinned to (since `1.0.0-rc.5`; one `workflow` and `workflow_version` before). A plan keeps
  reporting against them after the installation upgrades a module, and
  `workflow_version_drift` says when a newer one exists. `inputs` is keyed by module id.
- `derived_settings` - the folder, template, language and filename the agent derived,
  every one of them correctable, with `permission_used` for the audit trail and
  `corrected_by_user` naming the fields the user decided rather than the agent: those
  sent as `setting` parts, and those confirmed through a settings review. `null` for a
  workflow that writes nothing, and for `content_edit`, whose pages already have their
  folder, template and language.
- `cms_objects`, `preview` and `previews` - what the plan touched and targets, the most
  recent CMS preview, and the latest preview of each page. A write outside `cms_objects` is
  refused (section 11.1).
- `pending_interaction` and `authorizations` - the question the plan's run is blocked on, and
  whether the session's credentials can still act for this plan's module.

**Items, objects and findings** (since the `1.0.0-rc.5` addition, 2026-10-15). A create item says what it creates
(`creates`: type, label, path) and, once written, carries the object in `created`; tag fills and
verifications are sub-steps with `parent`, so every top-level write item is one object and a
client counts the plan at a glance from the top-level items. Checks carry the `plan_version` and
`item_ids` they are about; the proposing run reports what to know before accepting (a page that is
online, an object changed since it was read, a near-duplicate).

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
`total_units` explain it; `deviations` lists the first differences when it failed. See
`content_create/chk-verbatim-identical` in `s1-content-create.sse`.

## 8. Release, review, publish

Since `1.0.0-rc.4` there are two status machines: the session's, which is about the
conversation, and each plan's, which is about one piece of work. Revising, releasing,
publishing, requesting review and discarding move a **plan**; the session stays `active`
throughout.

The session (`SessionStatus`), through `POST /sessions/{id}/transitions` with
`archive` or `unarchive` and nothing else:

```
active <-> waiting_for_input
active | waiting_for_input -> archived     (archive: cancels a run, deletes the session credentials)
archived                   -> active       (unarchive: register a credential before the next turn)
```

Each plan (`PlanStatus`), through `POST /sessions/{id}/plans/{plan_id}/transitions`, except
where the harness or the user's message moves it:

```
drafting -> proposed -> accepted -> running -> done -> released -> published / review_requested
              ^  |          |          |        |        |              |
              |  +- adjust  +-- redirect --+    |        |              |
              |                                 |        |              |
              +------- revise (version + 1, finished items kept) ------+
any open status ---------------------------------------------------> discarded
```

| Move | Made by | Route or event |
|---|---|---|
| `drafting` | the session agent's `draft_plan`, or `POST /sessions` with `workflow` | `plan.updated`, `version: 0` |
| `drafting` -> `proposed` | the harness, when the proposing run ends | `plan.updated`, `version: 1`, or `version + 1` and `reason: "Revised: ..."` for a revision |
| `drafting` -> `running` | the harness, for a plan whose modules have no CMS write tool (no acceptance needed) | `plan.updated` |
| `proposed` -> `accepted` -> `running` | the user's message: an `execute` message with a `plan` part naming the plan | `POST .../messages`; the execute run's first `plan.updated`, at the same version when accepted as proposed, at `version + 1` when adjusted |
| `proposed`, `done`, `released`, `published`, `review_requested` -> `drafting` | a revision: the session agent's `revise_plan`, or a message with `options.mode: revise` (since `1.0.0-rc.5`; `closed_at` is cleared) | `plan.updated` (`drafting`, still the old version), then the proposing run |
| `accepted`, `running` -> `drafting` | a redirect: a message that changes what the executing plan does (since `1.0.0-rc.5`) | `run.cancelled` (`cancel_reason: redirected`), then `plan.updated` (`drafting`) and the proposing run |
| `drafting` -> `drafting` | a redirect of the proposing run: a message about the same work while the plan drafts (since `1.0.0-rc.5`) | `run.cancelled` (`cancel_reason: redirected`), then the proposing run again with the message |
| `drafting`, `proposed` -> `superseded` | the session agent, when the user starts unrelated work instead of an unaccepted plan and says so (since `1.0.0-rc.5`; a new proposal of the same work revises the plan) | `plan.updated` |
| `running` -> `done` | the harness, when the run finished the items and the `step_end` gates ran | `plan.updated` |
| `done` (or `running`, which stops it) -> `released` | the user: `release`, after the `before_release` gates of every module of the plan | `200` with the `Plan` |
| `released` -> `published` / `review_requested` | the user: `publish` or `request_review`, after the `before_publish` gates | `202` with `run_id`, `plan`, `session` |
| any open status -> `discarded` | the user: `discard`, following `plan_policy.on_discard` | `200` with the `Plan` |

The statuses fall into three classes (since `1.0.0-rc.5`). **Open** is `drafting` to
`released`, and executing is `accepted` and `running`. **Settled** is `published` and
`review_requested`: closed, out of `Session.open_plan_ids`, but revisable; its pages stay
online until the next `publish`. **Final** is `discarded` and `superseded`: such a plan never
changes again, a transition on it is `409` with `plan_closed` and a revision `409` with
`plan_not_revisable`. rc.4's `reopen` is gone: a revision replaces it. A revision resets the
plan's release, so the `before_release` gates run again at the next `release`.

**At most one plan executes** at a time (`Session.executing_plan_id`), but several may be open:
rc.4's `plan_policy.concurrency` is removed (since `1.0.0-rc.5`), because a follow-up now
revises its plan instead of waiting for it. `Session.open_plan_ids` lists every open plan and
`open_plan_id` is the one the conversation currently works on, the most recently drafted or
revised. A repeated `discard` is `200` with no new event; every real change emits
`plan.updated` with the new `status`.

`publish` and `request_review` start a run (`mode: transition`) and return `202` with a
`run_id`, the plan as it is at that moment (`released`) and the session, because the write
goes through the session's CMS connection; `request_review` calls the CMS only for a page the
user cannot publish directly, since the CMS queues a page for approval through `publish_page`
and has no review-request call of its own. A plan publishes **every page it created or
changed**, and none another plan wrote. Whether each page is published directly or only
submitted for review is **not** a client decision and not a cached flag: the server resolves
it per page from the user's permissions on that page's folder and reports each outcome in the
plan's `publications`. A `publish` that the CMS converts into a review request still returns
`202`. The plan ends `published` when every page is published, `review_requested` when at
least one went to review and none failed; a page that failed fails the run and leaves the
plan `released`, and a repeated `publish` retries only the pages still `pending` or `failed`.
Section 11 shows a mixed outcome.

`discard` follows `plan_policy.on_discard`, the strictest of the plan's modules (`restore`
over `keep`, since `1.0.0-rc.5`): `keep` (the default) leaves pages the plan already wrote as
drafts in the CMS, `restore` restores each page's version from before the plan. The session credential is untouched by every plan transition.

`at` on a `publish` schedules it, which is the "publish on 1 January at midnight, when
the update comes into force" case.

**Quality gates guard the transition.** Before `release`, `publish` and `request_review`
the harness runs the gates of the plan's modules for that moment, each over its own module's
items, and re-checks the earlier ones. The transition is refused with `409` and `genaix_code: quality_gate_failed` while
**any** gate-sourced check of that plan is failing, blocking or not. The two kinds clear
differently:

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
gate-produced check is named `<workflow>/chk-<gate-id-with-hyphens>` (the prefix since
`1.0.0-rc.5`), so `verbatim_identical` of `content_create` reports
`content_create/chk-verbatim-identical`, and the `release_self_check` gates of two modules of
one plan report two checks. A gate whose manifest names a guideline keeps
`source.type: guideline` and that guideline's id while still carrying `gate_id`, `kind` and
`evidence`: `content_create/chk-alt-text` in the S1 transcript is exactly that case. Gates without such a
block get `source.type: gate`.

`GateResult` separates `status`, the execution outcome, from `verdict`, the judgement, and
carries `attempt` (`1` for the first execution, `0` only while `pending`), because a gate re-runs after the agent fixes what it reported. Its full
verdict shape is `schemas/gate-verdict.schema.json`, and the manifest behind a module is
`schemas/workflow-manifest.schema.json`; both are standalone files next to the three the
API generates.

`failed` is neither a session nor a plan status. A failed run leaves the session `active`
and the plan where it was, so the user can retry; the failure lives on the `Run` and in the
`run.failed` event.

## 9. Errors

Every error is an RFC 9457 `application/problem+json` document. Branch on
`genaix_code`, not on `type` or `status`.

| Status | When | Typical `genaix_code` |
|---|---|---|
| 400 | malformed request (including an `X-HTTP-Method-Override` other than `PATCH`), or no identity: no subject header (direct), no assertion (proxied) | `malformed_request`, `invalid_cursor`, `subject_missing` |
| 401 | the CMS assertion fails verification, a proxied request reaches an installation without a CMS key, or a direct request reaches one with a key | `identity_invalid` |
| 401 | installation token missing, unknown or revoked | `invalid_installation_token` |
| 403 | exists, but belongs to another subject | `session_forbidden` |
| 404 | unknown id, or an id of another installation or user; a plan the session does not have, also as `options.plan_id` | `session_not_found`, `plan_not_found`, `mcp_connection_not_found` |
| 409 | state conflict, an archived session, a blocking gate, too many connections, a transition on a closed plan, a `plan` part naming a plan that has since changed, is closed, or is not waiting for acceptance, or a revision of a `discarded` or `superseded` plan | `run_already_active`, `session_archived`, `workflow_transition_invalid`, `quality_gate_failed`, `mcp_connection_limit`, `plan_outdated`, `plan_closed`, `plan_not_proposed`, `plan_not_revisable` |
| 410 | gone | `interaction_expired`, `events_pruned` |
| 413 | upload over the limit | `file_too_large` |
| 422 | body failed validation (a `plan` part without `execute` mode, a `plan` part that changes a `done` item or puts an item before one it `depends_on`, the latter with `errors[].code: dependency_order`, a `continued_from` that is not one of your sessions, an unknown `workflow`), or a server refused a credential; on the stream only, in `tool.failed`, a write outside the executing plan | `validation_failed`, `unknown_workflow`, `unsupported_media_type`, `mcp_authorization_rejected`, `plan_scope_violation` |
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

`session_released` is gone (since `1.0.0-rc.4`): a session is no longer released, its plans
are, and a plan's status never blocks a message, an upload or a credential. What does is an
archived session, `409` with `session_archived`; `unarchive` it first.

`plan_scope_violation` never answers a request. A write tool call whose target is not an
object of the executing plan (an item's target, or an object the plan created), or that its
module's `write_targets` does not allow, is refused by the harness before it reaches the CMS:
`tool.failed` with a `422` problem `plan_scope_violation`, then `error` with
`recoverable: true`. The run goes on, and the agent tells the user the change would need a
revision of the plan. S7 shows it.

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

1. `GET /sessions/{id}` - status, `active_run_id` and `side_run_id` to know whether to
   expect live events, and `open_plan_id`, `open_plan_ids` and `executing_plan_id` to know
   which plans matter now.
2. `GET /sessions/{id}/messages` - history: assistant turns as the same parts the stream
   sent, user turns as the same parts the client sent.
3. `GET /sessions/{id}/plans` - every plan in short, oldest first: the cards to draw
   (since `1.0.0-rc.4`).
4. `GET /sessions/{id}/plans/{plan_id}` for the open plans and the executing one - plan,
   steps, checks, gates, approvals, publications, derived settings, previews,
   `pending_interaction` if its run is blocked on a question, and `authorizations` to see
   whether the session can still act for it. A closed plan only when the user opens its card.
5. `GET /sessions/{id}/events?after=<highest seq you have>` - or `after=0` when you
   have none.
6. If an `authorizations` entry is `expired` or `invalid`, or a required connection has no
   entry and no standing authorization, register a fresh credential with
   `PUT /sessions/{id}/authorizations/{connection_id}` before sending the next turn. A
   session credential can have run out while the client was away.

## 11. Beyond one page: plans in one conversation

`1.0.0-rc.3` added plan approval, the `content_edit` module and per-page publishing;
`1.0.0-rc.4` puts them into one conversation, where each piece of work is a plan of its own;
`1.0.0-rc.5` makes those plans living: revised as the user changes their mind, and able to use
several modules. This section continues the S1 plan from section 3 (11.1), then follows
`s7-continuous-session.sse`, one session that answers a question, edits the pages it found,
publishes them and starts a second plan (11.2), shows publishing per plan with the mixed
outcome of `s6-content-edit.sse` (11.3), and walks `s8-living-plan.sse`, one plan revised four
times across two modules (11.4).

### 11.1 Accept or adjust the plan

The S1 plan `5b0e7c2a-91d4-4f6b-8a3e-2c7d9f1e4a60` runs under `content_create`, which has plan
approval. Its proposing run (`run.mode: propose`) therefore reads, confirms the settings,
searches and proposes the plan as `plan.updated` with `status: proposed` and version 1 (six
items, `p1` to `p6`), then ends with `run.completed`. Nothing has been written; the plan's
`state` is `awaiting_feedback`. No interaction is pending, so the user's answer is simply the
next message, and **only that message can accept the plan**: the model never does.

To accept as proposed, the plan card sends a `plan` part naming the plan and the version the
user saw, without `items`. Only the `plan` part accepts (since `1.0.0-rc.4`): a plain message,
for example "Passt so.", goes to the session agent, because `execute` is the default mode and the
user may as well be asking for something else.

To adjust, send the plan as the user wants it carried out. Here the user drops the webinar form
(`p5`) and adds a FAQ section before the cross-reference:

```json
{
  "options": {"mode": "execute"},
  "parts": [
    {"type": "text", "text": "Ohne Webinar-Formular, dafuer ein kurzer FAQ-Abschnitt.\n"},
    {"type": "plan", "plan": {"plan_id": "5b0e7c2a-91d4-4f6b-8a3e-2c7d9f1e4a60", "based_on_version": 1, "items": [
      {"id": "p1", "label": "Seite \"Neue Nutzungsbedingungen ab Q3\" anlegen"},
      {"id": "p2", "label": "Hero-Bereich"},
      {"id": "p3", "label": "Die drei wichtigsten Aenderungen"},
      {"id": "p4", "label": "Widerrufsbelehrung Absatz 1 (wortgleich)"},
      {"label": "FAQ-Abschnitt", "kind": "create", "workflow": "content_create", "depends_on": ["p1"]},
      {"id": "p6", "label": "Querverweis von \"Garantiebedingungen\""}
    ]}}
  ]
}
```

The rules are few. `plan_id` must name a proposed plan of the session (since
`1.0.0-rc.4`). Items in order are the order of work. An item whose `id` is a current step
keeps that step, with its `kind`, `target`, `workflow` and `depends_on` unless the item sets
them, so `p1` keeps its target folder and `p6` its target page. An item without an `id` is a
new step and GenAIx assigns one; it may name its module in `workflow` and the items it needs in
`depends_on` (since `1.0.0-rc.5`), and without `workflow` GenAIx picks the module. A step left
out, `p5` here, is removed. An order that puts an item before one it depends on is `422`
`validation_failed` with `code: dependency_order`. A `done` item of an earlier version is kept
whether listed or not, and changing it is `422`. At most one `plan` part per message, and
only in `execute` mode. Its plain rendering is the numbered list of the labels, so `content`
and the history read like what the user saw:

```
Ohne Webinar-Formular, dafuer ein kurzer FAQ-Abschnitt.
1. Seite "Neue Nutzungsbedingungen ab Q3" anlegen
2. Hero-Bereich
...
```

The execute run (`run.mode: execute`) opens with `plan.updated` version 2: the plan it is about
to carry out, with `accepted_version`, `accepted_by` and `accepted_at` set and `reason`
"Adjusted by the user: 5 kept, 1 added, 1 removed.". A plan accepted unchanged keeps its version
instead (since `1.0.0-rc.5`): the execute run opens with `plan.updated` at version 1, `status:
accepted`, because `version` changes only with the plan's content. Only then does it write. If the agent has to deviate from that plan, merging,
splitting or dropping a step, it says so in a further `plan.updated` and its `reason`; a
client therefore never has to diff the plan it sent against the plan being executed. A write
outside the plan's objects never reaches the CMS (section 9, `plan_scope_violation`).

Three refusals guard the acceptance, each `409` with no run started:

- `plan_outdated` - the plan moved to a newer version while the user was editing. Re-read
  `GET /sessions/{id}/plans/{plan_id}`, show the user the current plan, and let them apply the
  edit again.
- `plan_closed` - the plan was superseded or discarded meanwhile, is settled, or is not a plan
  of this session. Re-read `GET /sessions/{id}/plans` and redraw the cards.
- `plan_not_proposed` - the plan is still drafting, or was already accepted (a second tab).
- And `run_already_active` when another plan's run is still going: at most one plan executes.

A numbered list typed as text is just text. The only contract for an accepted plan is the
`plan` part.

Not every module waits like this. A module with `plan_approval: false` but a CMS write tool
(`construct_create`, `admin_user`) is accepted by one confirmation, the card's accept or the
answer to the `confirm` interaction its plan raises (S3). A module without any CMS write tool
(`content_research`) needs no acceptance: its plan goes from `drafting` to `running` at once,
because reading is free (S2).

A plan the user no longer wants is discarded, from any open status:
`POST /sessions/{id}/plans/{plan_id}/transitions {"action": "discard"}`. The conversation goes
on. A plan the user wants differently is not discarded but revised: they just say so (11.4).

### 11.2 A second plan in the same conversation

New work is a further plan of the same session (since `1.0.0-rc.4`; `rc.3` needed a second
session for it), and a follow-up on the same work revises its plan (since `1.0.0-rc.5`, 11.4).
`s7-continuous-session.sse` is one session, created without a `workflow`, in which the user
asks, edits, publishes and starts again:

| seq | Event | What happens |
|---|---|---|
| 1-10 | `run.started` `mode: session` ... `run.completed` | "Welche Seiten nennen noch die Versandkostenfrei-Grenze von 35 Euro?" The session agent searches and answers with three pages. No plan, so no plan event. |
| 11-15 | `tool.started` `draft_plan`, `plan.updated` | "Setze auf diesen drei Seiten die Grenze auf 50 Euro." The session agent drafts plan `a3f5c8e1-...`, expecting `content_edit`: `status: drafting`, `version: 0`, no items, `workflows` `content_edit`. |
| 16-31 | `run.started` `mode: propose`, `plan.updated` | The module reads the three pages and their versions and proposes one item per page, each with its page as `target`: `proposed`, version 1. No settings review; `derived_settings` stays `null`. |
| 32-33 | `run.started` `mode: execute`, `plan.updated` | The user took Serviceinformationen out in the card and accepted with a `plan` part (below). Version 2, `running`, "Adjusted by the user: 2 kept, 0 added, 1 removed." |
| 37, 50 | `publications.updated` | Each page joins the plan's publish set when it is first written, `pending`. |
| 39-47 | `run.started` `mode: side` ... `run.completed` | Meanwhile the user asks "Wie viele FAQ-Seiten gibt es?". A side run of the session agent, `plan_id: null`, read tools only, answers; the execute run goes on. |
| 51-53 | `tool.started`, `tool.failed`, `error` | The agent tries to write Serviceinformationen as well. Refused before the CMS: `422` `plan_scope_violation`, then `error` with `recoverable: true`. |
| 56 | `plan.updated` | `done`. The execute run ends. |
| 63-64 | `check.updated`, `plan.updated` | `release`: the gate `base_version_current` passes, the plan is `released`. No run; the session stays `active`. |
| 65-73 | `run.started` `mode: transition` ... `plan.updated` | `publish`: both pages go out, the plan is `published` and closed. |
| 74-81 | `mode: session`, then `mode: propose` | "Erstelle jetzt eine FAQ-Seite dazu." The agent's context starts with the record of plan `a3f5c8e1-...`. A new page with a goal of its own is new work, so it drafts plan `b6d2e9f4-...` (`content_create`), which is proposed at version 1. The FAQ page links to Versand und Lieferung but does not write it. |
| 82-89 | `revise_plan`, `plan.updated` twice, `mode: propose` | "Mach daraus lieber eine News-Meldung." before accepting: the same work, so the agent revises `b6d2e9f4-...`. It goes `drafting` (still version 1), and its proposing run ends it `proposed` at version 2, `reason: "Revised: ..."`. Version 1 was never accepted, so every item is `change: added` and the dropped FAQ-questions item is simply gone. rc.4 superseded the proposal and drafted a third plan here. |
| 90 | `session.updated` | `plan_count: 2`, `open_plan_id` and `open_plan_ids` the news plan, `executing_plan_id: null`. |

The acceptance of the first plan, as the card sends it:

```json
{
  "options": {"mode": "execute"},
  "parts": [
    {"type": "plan", "plan": {"plan_id": "a3f5c8e1-2b47-4d96-8e05-1c7b9d4f6a23", "based_on_version": 1, "items": [
      {"id": "p1", "label": "Versand und Lieferung: Grenze von 35 auf 50 Euro"},
      {"id": "p2", "label": "Garantiebedingungen: Grenze von 35 auf 50 Euro"}
    ]}}
  ]
}
```

What the client takes from it:

- **One card per `plan_id`**, replaced by every `plan.updated` with that id. After seq 90 the
  conversation shows two: a published edit and the news proposal at version 2, whose card
  replaced the FAQ proposal in place. `GET /sessions/{id}/plans` returns the same two, oldest
  first.
- **The run's `mode`** decides where its output goes: `session` and `side` answers belong in the
  conversation, `propose`, `execute` and `transition` runs to their plan's card.
- **Plans never mix.** Every write, check, publication and preview of the first plan carries
  its `plan_id`; the second plan starts with none of them. Pages the first plan wrote are not
  in the second one's `publications`.
- **The agent keeps the thread.** Its context leads with the open plan and a record of every
  closed plan and user decision, which is why "eine FAQ-Seite dazu" knows which pages changed.
  Older chat may be summarised (`status` with `kind: context_compacted`), but the record and
  the messages stay exact.
- **One credential.** The session's CMS token registered on `POST /sessions` serves every plan;
  publishing the first plan does not end it.

`content_edit` itself works as in `rc.3`. Its pages are the items' targets: in a conversation
the session agent drafts them from what the user named or the earlier answer found, and in a
session started from a CMS context action with `workflow: content_edit` they are the
`type: page` entries of `context.references`, 1 to 20, in order (S6). The proposing run reads
each page and its version history, which fixes each page's **base version**. There is no
settings review: the pages already have their folder, template and language. The execute run
applies the changes page by page, verifies them, and previews each page; one `preview.updated`
arrives per page, and the plan's `previews` holds the latest of each.

### 11.3 Release and publish: one plan, one outcome per page

`release` and `publish` are transitions of one plan,
`POST /sessions/{id}/plans/{plan_id}/transitions`, and the session stays `active`.

`release` runs the plan's gates as usual. `content_edit` adds `base_version_current`, which
blocks the release when any page has a version newer than its base version that this plan did
not write, that is, when someone else edited the page after the agent read it. The refusal
names the page.

`publish` returns `202` and runs. Each page of the plan is published directly or sent to review
according to the permissions on **its own** folder. In S6 the plan changed three pages, and the
user may publish in `Richtlinien` but not in `Publikationen`, so the run ends with:

```
"publications": [
  {"cms_ref": {"type": "page", "id": 8644, "node_id": 3}, "status": "published", "at": "2026-10-07T11:23:35Z", "approval_chain": [...]},
  {"cms_ref": {"type": "page", "id": 9011, "node_id": 3}, "status": "review_requested", "at": "2026-10-07T11:23:39Z", "approval_chain": [...]},
  {"cms_ref": {"type": "page", "id": 8871, "node_id": 3}, "status": "published", "at": "2026-10-07T11:23:43Z", "approval_chain": [...]}
]
```

The plan ends `review_requested`, because one page went to review and none failed, and is
settled: closed, out of `open_plan_ids`, but revisable, so a follow-up on these pages revises it
(11.4) rather than opening another plan. Had every page been published it would be
`published`, as in S7. Had a page failed, the run would fail, the plan would stay `released`
with the other pages still `published` or `review_requested`, and a repeated `publish` would
retry only the failed page. The plan-level `approval_chain` is the first page's chain, so a
one-page client keeps working unchanged.

There is no way to leave one page out of a publish. A page the user does not want changed is
taken out of the work earlier, by deleting its item from the plan before accepting it, as the
user did in S7. Pages of another plan of the same session are never part of this publish.

### 11.4 One plan, revised: living plans

`s8-living-plan.sse` (since `1.0.0-rc.5`) is the PNDemo session: one invitation page created
from an uploaded PDF and then changed three times. In rc.4 each change was a plan of its own,
and every follow-up was refused while the previous plan was still open. Here it is one plan,
`d4a7e1c9-...`, with four versions and two modules:

| seq | Event | What happens |
|---|---|---|
| 1-5 | `mode: session`, `draft_plan` | "Erstelle aus dem PDF eine Einladungsseite PNDemo." The plan is drafted, `version: 0`. |
| 6-38 | `mode: propose`, `interaction.requested` | Version 1 (`content_create`): the PDF is read, one `settings_review` confirms folder, template and language, `content_create/chk-no-duplicate` passes, and `p1` "create the page" is proposed. |
| 39-72 | `mode: execute`, `run.workflow: content_create` | Accepted as proposed. `p1` runs: page 9207 created and filled, its pending publication, the `step_end` checks, a preview. `done`. |
| 73-77 | `mode: session`, `revise_plan`, `plan.updated` | "Mach sie bunter." The plan is done and open; the message concerns its page, so the agent revises it. `drafting`, still version 1, `p1` done. |
| 78-96 | `mode: propose`, `plan.updated` | Version 2, `reason: "Revised: ..."`: `workflows` now lists `content_create` and `content_edit`; `p1` is `unchanged` and `done`, `p2` (`content_edit`, `depends_on: [p1]`) is `added`. No settings review: `content_edit` needs none. |
| 97-119 | `mode: execute`, `run.workflow: content_edit` | Accepted. Only `p2` runs; `p1` is not run again. The checks are `content_edit/chk-*`, next to the earlier `content_create/chk-*`. No new publication: the page already has one. |
| 120-151 | `revise_plan`, `mode: propose`, `interaction.requested` | "Lade das PDF hoch und verlinke es im Text." Version 3 adds `p3` (upload, `content_create`) and `p4` (link, `content_edit`, `depends_on: [p1, p3]`). The version's one settings review asks `file_folder`, recommending the node's default file folder Downloads (88) with `other_allowed: true`. |
| 152-168 | `mode: execute`, `run.cancelled` | Accepted; two segments follow, `content_create` first. While `p3` uploads, the user writes "Der Linktext soll 'Einladung (PDF)' heissen.": a redirect. The run stops at the next tool boundary, the upload has completed, `p3` is `done`, and the run ends `run.cancelled` with `cancel_reason: redirected`. The `content_edit` segment never starts. |
| 169-179 | `mode: propose`, `plan.updated` twice | The proposing run starts on the redirect message itself, no session run in between: `drafting` at version 3, then version 4 with `p4` `changed` and everything else `unchanged`. |
| 180-203 | `mode: execute`, `run.workflow: content_edit` | Accepted. `p4` runs: the link to file 9311, `content_edit/chk-references-exist` resolves it. `done`. |
| 204-209 | `check.updated`, `plan.updated` | `release`: the `before_release` gates of both modules, each with its own check id (`content_create/chk-release-self-check`, `content_edit/chk-release-self-check`), then `released`. |
| 210-217 | `mode: transition` ... `session.updated` | `publish`: the one page, written by versions 1, 2 and 4, goes online once; the uploaded PDF is no publication, the CMS publishes it under its own rules. The plan is `published`, settled; `open_plan_ids` is empty. |

The acceptance of each version is the same `plan` part as for a first proposal; for version 2:

```json
{
  "options": {"mode": "execute"},
  "parts": [
    {"type": "plan", "plan": {"plan_id": "d4a7e1c9-6b52-4f08-9e3d-2a7c5b8f1e60", "based_on_version": 2}}
  ]
}
```

What the client takes from it:

- **One card, many versions.** Every `plan.updated` carries the whole plan with `versions`; a
  card can list the history or show only what changed since the user last accepted. The item
  `change` says it without a diff: `added` and `changed` items are new to the user, `removed`
  ones are struck through until the version is accepted, `unchanged` ones are as accepted.
- **Done stays done.** An item finished in an earlier version keeps `status: done` and is never
  run, removed or changed again; undoing it would be a further item. The execute run carries
  out only the `pending` items.
- **Modules are an ordering, not a boundary.** `Plan.workflows` lists the modules in run order,
  each item names its own, and `depends_on` orders them: the upload before the link to the
  file. Each contiguous segment of one module's pending items is one execute run, named by
  `run.workflow`; the segments of one acceptance follow each other without another acceptance.
- **Stable ids across modules.** Steps and checks are keyed `<workflow>/<id>`, so a revision
  that adds `content_edit` never collides with what `content_create` already reported.
- **A redirect is not a failure.** `run.cancelled` with `cancel_reason: redirected` is followed
  at once by the plan's proposing run; nothing the cancelled run wrote is lost, and the plan
  goes on at the next version.
- **One record.** The plan's publications, checks and `cms_objects` accumulate over the
  versions, and the release covers all of them; a further revision would reset it.

How the agent tells a revision from new work is in section 3: the objects or the goal of an
existing plan make a revision, unrelated objects and an unrelated goal new work, and
`options.mode: revise` settles it for the client.

### 11.5 Finding related sessions

A new conversation that builds on an earlier one names it in `continued_from`
(`1.0.0-rc.3`): it must be one of the caller's own sessions, otherwise `422` pointing at
`/continued_from`, whether the id is unknown or someone else's. It is returned as
`Session.continued_from` and cannot be changed. The agent gets the earlier session's user
messages, final assistant messages and `cms_objects` as read-only context, and `recall` reaches
it. Nothing else carries over: no files, no plan, and no credential, so the new session needs
its own session authorization like any other (section 1b). S6 is such a session, building on
the research conversation of S2. Inside one conversation, follow-up work needs none of this:
it revises its plan (11.4), and new work is a further plan (11.2).

`GET /sessions?continued_from=2d8c5f71-0e46-4b9a-9c13-5a7f8e2b6d40` lists the follow-ups of the
S2 conversation, so a client can show "continued in" next to "continued from".

`GET /sessions?label=changeset:5c2e9a17-4b30-4d6f-8e21-7a9c0b3d5f48` lists every session
carrying that label. `label` is matched exactly and may be repeated; a session must carry all
of the given labels. The label is the client's own convention for grouping; since one plan
publishes all its pages, no group is needed for publishing.

`GET /sessions?workflow=content_edit` lists the sessions with at least one plan using that
module, and `plan_status` (since `1.0.0-rc.4`, repeatable) filters on the open plan, else the
latest: `?plan_status=proposed&plan_status=released` is a list of conversations waiting for
the user.

## Files in this directory

| File | What it is |
|---|---|
| `requests.http` | Every operation as a runnable request, in lifecycle order |
| `s1-content-create.sse` | S1, content creation from source material: the execute run of one `content_create` plan, accepted at version 1. 92 events: plan, steps with substeps, CMS writes, a streamed `page_structure` part, the verbatim proof, a `settings_review` interaction confirming folder, language and template, previews, an artifact |
| `s2-content-research.sse` | S2, research and reporting: a read-only `content_research` plan that runs without acceptance. 51 events: search calls, a streamed `table` part, citations, no CMS writes |
| `s3-construct-create.sse` | S3, construct creation. 61 events: catalogue check, a streamed `construct_draft` part, Handlebars validation, the `confirm` interaction that is the user's acceptance (the plan is `proposed` until then), `create_construct`, an `api_call_log` part, preview |
| `s4-admin-user.sse` | S4, optional. 43 events: group lookup, a `form` interaction, permission impact simulation, the `confirm` that accepts the plan, user creation, group assignment |
| `s5-failure-and-recovery.sse` | Not a showcase scenario, the unhappy paths. 53 events across two execute runs of one `content_edit` plan on one stream, the plan back to `accepted` after each: `tool.failed` on a locked page, a recoverable `error`, `artifact.created`, `tool.failed` plus `auth.required` plus `run.failed` on a dead connection credential, then after re-registering it a second run with `artifact.updated` and `run.cancelled` |
| `s6-content-edit.sse` | `content_edit` (`1.0.0-rc.3`), no showcase scenario. 114 events. A session started with `workflow: content_edit` and `continued_from` the S2 conversation, over three pages in two folders: the proposing run that stops at the plan, the execute run with per-page previews, and a publish with two pages published and one sent to review |
| `s7-continuous-session.sse` | One continuous session (`1.0.0-rc.4`; revisions since `1.0.0-rc.5`), no showcase scenario. 90 events, two plans: a question answered by the session agent, a `content_edit` plan adjusted and accepted in the card, a side run while it executes, a `plan_scope_violation`, release and publish, then a `content_create` plan revised to version 2 before acceptance (section 11.2) |
| `s8-living-plan.sse` | One living plan (`1.0.0-rc.5`), the PNDemo session, no showcase scenario. 217 events: one plan revised four times across `content_create` and `content_edit`, a settings review per version that needs one (`file_folder` with the node default), one execute run per module segment, a redirect while executing (`run.cancelled`, `redirected`), the `before_release` gates of both modules, one publish (section 11.4) |
| `session-lifecycle.md` | This file |

All transcripts use the `1.0.0-rc.5` shapes: `Plan.workflows`, `versions` and the items'
`workflow`, `version` and `change`; `<workflow>/<id>` step and check ids carrying `workflow`;
`run.workflow`; `Session.open_plan_ids`.

Every `data:` payload in the eight transcripts validates against
`../schemas/events.schema.json`, every nested timestamp is at or before the envelope `ts`
of the event carrying it, and `seq` runs gapless from 1 in each file.

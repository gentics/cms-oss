# GenAIx API v1 reference client

A tiny client that consumes the stage 1 GenAIx API v1 contract end to end: a chat
interface, a part-aware composer, structured message-part rendering, the live event
stream, interactions, MCP connections, quality gates and the session lifecycle.

Tracks `1.0.0-rc.2`.

**This is not the Agentic Workspace.** Gentics builds that UI, and it is still being
prototyped. This client exists to prove that the contract in `../openapi.yaml`,
`../schemas/events.schema.json` and `../schemas/parts.schema.json` is consumable by
an ordinary front end, and to give the mock and the spec something to be tested
against. The styling is deliberately plain and the naming is deliberately
UI-agnostic, because the API describes data and state, never screens.

No build step, no framework, no dependencies. Plain ES modules, vanilla JavaScript,
one stylesheet.

## Running it

### Against the mock

```bash
cd stage1/mocks/genaix-mock && <start the mock on :8080>    # separate terminal
cd stage1/client && ./serve.sh                              # serves stage1/ on :8081
```

Open <http://localhost:8081/client/>. In the connection panel set:

| Field | Value for the mock |
|---|---|
| Base URL | `http://localhost:8080/api/v1` |
| Installation token | `sk_gnx_mock` |
| Subject | any opaque string, for example `sub_c3f1a07d9e5b4826` |

Two headers are the whole contract: `Authorization: Bearer <installation token>` and
`X-GCMS-Subject`. There is no `X-GCMS-User-Id`, no `X-GCMS-User-Login`, no
`X-GCMS-User-Groups`, no `X-GCMS-SID`, no `X-GCMS-User-Token` and no
`X-GCMS-Session-Secret`.

### The subject is a pseudonym

`X-GCMS-Subject` is an opaque, installation-scoped key the CMS derives from the
signed-in user, for instance an HMAC of the user id under a secret only the CMS holds.
It may also be a compact JWS carrying `sub`, `iss`, `iat` and a short `exp`, which
GenAIx verifies when the installation configures the signing key.

**GenAIx never learns the person behind a subject.** It uses the value as the owner key
of sessions, files, connections and authorizations and for nothing else. No user id,
login, display name or group list is sent, and none is stored, so the same person keeps
their sessions across sign-ins while GenAIx holds no identifying data about them. The
CMS alone can map a subject back to a person.

Two consequences a client has to live with. `GET /me` returns `user: {subject}` and
nothing else, so the identity card here shows the subject and no name: a real UI already
knows who is signed in and shows that from its own session. And erasure has to be driven
from the CMS side, through `DELETE /subjects/{subject}`, because GenAIx cannot resolve a
person to a subject. `lib/api.js` has `deleteSubject` for completeness; no UI calls it.

Over `http(s)` the Base URL field starts at `<this origin>/api/v1`, which is what the
docker compose stack in `../deploy/` serves, and over `file://` at
`http://localhost:8080/api/v1`; a value already stored in `localStorage` always wins.

Press **Test connection**. It calls `GET /ping`, `GET /me`, `GET /mcp/connectors`,
`GET /mcp/connections`, then one authorization read per connection. The panel then
shows the subject, each connection with its authorization status, the
workflows this subject may start, and one card per connection. The values are kept in
`localStorage`; the installation token is typed into a password field but it is
still stored in the browser, so use a mock token, not a production one.

### Connectors, connections and authorization

GenAIx defines connector **types**. A user creates **connections** of a type, each
with a URL the client supplies, so `cms` means "a Gentics CMS" and the address lives
on the connection. The `cms` connector is a multi connector, so one user can reach
several CMS instances.

The first `GET /mcp/connections` auto-creates the installation's default `cms`
connection, so in the normal case there is nothing to create and only a token to
register. **New connection** is there anyway: pick a connector type, supply a URL
and a label. `https` is required, with `http` allowed for `localhost` and
`127.0.0.1`; no embedded credentials, query string or fragment. The reachability
probe on create is informational, so a connection can be registered before its
server is up, and the card then shows `unreachable` with the probe message while
still accepting a token.

Each card's **Edit connection** does `PATCH`: URL, label and default flag. Changing
the URL points the connection at a different system, so GenAIx drops the stored
credential; the form says so and the client asks for confirmation first. **Delete
connection** removes the connection and its credential and promotes another default
of that type.

GenAIx never creates a credential. For a `bearer` connector, which is what `cms` is,
the client obtains the token out of band (for the CMS, `POST /rest/admin/token`
against *that connection's* CMS with its own session, named
`genaix-<installation_slug>`) and registers it on the connection. Paste it into the
card and press **Register token**: that is
`PUT /mcp/connections/{id}/authorization` with `{auth_type: "bearer", token}`. GenAIx
verifies the token against the connection before storing it, so a `422`
`mcp_authorization_rejected` leaves any working credential untouched.

**Forget token** is `DELETE` on the authorization; the connection survives, so
re-registering is one call, and nothing is revoked on the server itself.
**Re-check** re-reads the authorization, which is verified live rather than read
back from storage. The card shows the identity the server reports, which is the only
reliable way to confirm the token belongs to the user you think it does, and the
`server_info` from the MCP `initialize` probe.

For an `oauth2` connector the card offers **Start authorization** instead, which is
`PUT` with `{auth_type: "oauth2", action: "start"}`; GenAIx answers `pending` with an
authorization URL, the code exchange happens on its own callback, and the client
only polls. No Phase One connector uses this.

`serve.sh` serves `stage1/`, not `client/`, so that replay mode can fetch
`../examples/*.sse`.

### Replay mode, with no server at all

Pick one of the five recorded transcripts in the app bar, choose a speed and press
**Replay**. `s5-failure-and-recovery.sse` is the one worth watching: a locked CMS
object, a recoverable error, an MCP server that refuses its credential, a failed
run, a re-sent message and a cancel. The transcript is fed through exactly the same SSE parser, part reducer,
state reducer and renderers that the live stream uses, so the full event model is
demonstrable before the mock runs.

Over `file://` the browser blocks `fetch` of sibling files. Press **Open file** and
pick a `.sse` file from `stage1/examples/` instead; everything else works.

### The test

```bash
cd stage1/client && node tests/parse-transcripts.mjs
```

It replays all five transcripts through the real modules under node, and exercises
the composer's parts builder, the plain rendering and the request bodies as pure
functions. 701 assertions, covering:

- every `data:` frame parses, and the SSE `id:` and `event:` fields agree with the
  payload's `seq` and `type`
- `seq` starts at 1, is gapless and strictly increasing
- no event falls into the unknown-event-type branch
- every part type in the transcripts has a dedicated renderer, the registry has no
  entry outside `PartType`, and the unknown-part fallback still renders a `text`
  field for a type from a later version
- every reduced part renders without throwing
- the reduced plan, steps with substeps, checks, verbatim proofs, derived settings,
  approval chain, artifacts, preview, tool calls, interactions, terminal run
  statuses and recorded Problems match the transcripts
- a gate-backed check names its gate, its kind and its evidence, and its id follows
  `chk-<gate-id>`
- no event carries a `GateResult`, so the reducer never invents one
- `auth.required` is keyed by `connection_id` and carries the `connector` type, so
  two connections of the same type are distinct entries
- a CMS object carrying a `connection_id` is labelled with its connection
- the connection card, the create form and the connector catalogue behave: an
  unreachable connection still accepts a token, a single-connection connector type
  already held is disabled, an `oauth2` connector offers the flow and no token field
- a blocking check is never offered for acknowledgement, whatever the server says
- the gate, gate-failure and MCP server renderers produce what a reviewer needs
- replaying a whole stream twice changes nothing, which is client rule 3
- `part.completed` overrides an accumulated copy that missed a delta, which is rule 4
- the plain rendering of a user message matches the worked example in `openapi.yaml`
  character for character, one rule per `UserPartType`, brackets escaped in every
  position they can appear in, and nothing inserted between parts
- the parts builder drops empty settings and incomplete tokens, merges adjacent text,
  trims the message edges, keeps a `file_ref` free of a client-supplied name, carries
  the chip's own `source` or `verbatim` mode, and refuses a part type the API would
  answer `422` to
- a user message in the history renders from its parts, with the verbatim passage
  highlighted, references as chips, a verbatim file tagged and a source file not,
  settings lifted into a tag row, and user text HTML-escaped
- a message body carries `parts` and never `content`, `references` or `files`, and a
  session-create body carries the first message in `message`, never an `intent`,
  drops empty context fields, and refuses a `file_ref` on that first message
- the header contract is exactly two headers, the installation token and
  `X-GCMS-Subject`, with the removed ones absent, no subject on `GET /ping`, and a
  signed assertion passed through verbatim rather than decoded
- a session credential defaults to a 24 hour expiry, is refused locally when the token
  is under the API's minimum length or the expiry is in the past, rides along with the
  create call, and names the connection only where there is no path segment for it
- the session credential cards render both the summary `Session` carries and the
  detailed form the list route returns, recover the connector from the connection list,
  show an identity and a refusal reason, give a required-but-missing connection its own
  row, offer nothing to register in replay, and label the per-user card as standing
- a `settings_review` renders one prefilled row per `proposed` entry with its source
  tag and evidence quote and one empty required row per `missing` entry, turns an
  options list into a select without losing a value the list does not carry, and shows
  the whole editor disabled in replay
- the review's answer carries one `setting` part per key, is refused locally when a
  required key is blank or a part is not a setting, and allows an extra key
- resolving the review stores a user-role turn whose parts are exactly the confirmed
  settings, pointing back at the interaction, not duplicated when the event is
  replayed and not written at all when the review timed out
- the derived-settings card marks every key in `corrected_by_user` as the user's own
  and names a confirmed key that has no row of its own
- the `s1_two_turns` history example hydrates: the review's message keeps its
  `interaction_id` through `GET /sessions/{id}/messages` and renders the same as it did
  from the stream, while the typed request that preceded it carries no setting part at
  all, because the user wrote the language into the sentence
- the S1 walk end to end: folder from context, language from the user's text with its
  evidence, template missing with two options, the recorded answer covering all three,
  the stored turn rendering as `#[folder: 42]#[language: de]#[template: 17]`,
  `corrected_by_user` listing all three, reads running before the review and no write
  starting before it is answered

Node 18 or newer. No dependencies.

## Module layout

```
client/
  index.html                  structure only
  styles.css                  light, neutral, no theming ambitions
  app.js                      wiring: panels, fetch calls, event fan-out, replay
  lib/
    api.js                    one method per operationId, Problem parsing
    sse.js                    SSE frame parser and fetch-based stream reader
    parts.js                  part reducer: started / delta / completed, merge patch
    state.js                  event stream -> the state a client draws
    compose.js                user message parts: build them, render them plain,
                              render a stored user message, build the message and
                              session-create bodies
    render-parts.js           the assistant message-part renderer registry
    render-workflow.js        plan, steps, gates, checks, approvals, interactions,
                              the refused-transition problem, MCP connection cards
    markdown.js               markdown-lite for TextPart
    dom.js                    escaping and formatting helpers
  tests/parse-transcripts.mjs transcript conformance test
  serve.sh                    python3 -m http.server over stage1/
```

### What a real client should take from this

`lib/sse.js`, `lib/parts.js` and `lib/state.js`. Those three are pure, have no DOM
dependency, and encode the parts of the contract that are easy to get wrong:

- **`sse.js`** implements the event-stream line rules, including `:` comment lines,
  CRLF, multi-line `data:` and a stream that ends without a trailing blank line. It
  is fetch-based on purpose: the browser's `EventSource` cannot send the
  `Authorization` or `X-GCMS-*` headers, so `Last-Event-ID` is set by hand, which
  `openapi.yaml` accepts next to `?after=`.
- **`parts.js`** implements RFC 7386 JSON merge patch, including arrays being
  replaced wholesale, and the `append` versus `merge_patch` split.
- **`compose.js`** implements the plain-rendering table from the `postMessage`
  operation, including the bracket escaping, the builder that turns composer state
  into `parts`, and the two request bodies that carry them. It is what makes the live preview under the composer
  trustworthy: the client derives `content` with the same rules the server does, so
  what the user reads before pressing Send is what gets stored.
- **`state.js`** folds the stream into plan, steps, checks, derived settings,
  approval chain, preview, artifacts, tool calls, interactions and messages, and
  returns which regions changed so the caller can re-render one part instead of the
  whole transcript. It implements the five normative client rules from
  `../examples/session-lifecycle.md`.

The renderer registries are worth copying as a *shape*, not as markup.

Not worth copying: `app.js` (one global mutable object, full re-renders where a real
UI would diff), `styles.css`, and the use of `window.prompt` for adding a reference,
correcting the context, and entering a transition comment. Those are shortcuts that
keep the contract visible instead of burying it in UI code.

## MCP credentials are session-scoped

A credential is registered **for one session**, used only by that session's runs, and
dropped when the session ends. That is the integrated path, and it is what this client
does by default.

- `PUT /sessions/{id}/authorizations/{connection_id}` registers one,
  `GET /sessions/{id}/authorizations` lists them, `DELETE` forgets one.
- `POST /sessions` takes `authorizations` next to `message`, so the whole start of a
  workflow is one round trip: create the token on the target system, then create the
  session, register the credential and send the first turn in a single call.
- GenAIx deletes the stored token when the session reaches `published`, `discarded` or
  `archived`, when the client deletes it, or when `expires_at` passes. It is kept
  through `released` and `review_requested`, because publishing still acts on the CMS.

The **new-session form** therefore carries one credential row per connection the chosen
workflow requires: a token field and an expiry, defaulted to now plus 24 hours, the
installation's session authorization lifetime. Leaving a token empty is allowed: the
session is created without it, and the credential can be registered afterwards.

The **session view** shows `Session.authorizations` above the transcript, with the
status, the time left, the token name, the identity the connection reports and, for an
`invalid` one, what the connection said when it refused. Each row registers or revokes.
A connection the workflow requires but the session has no entry for gets a row of its
own, because that is the thing that fails a run, and the API models it as an absence
rather than as an `unauthorized` status.

When a run fails with `auth.required`, the banner offers **Register for this session**
first and the standing authorization second, in the order the run looked for them.
There is no pause-and-resume: register, then send the message again.

The per-user cards in the Connection panel stay, relabelled **standing authorization**.
They belong to the user rather than to a session, they are the path for the internal
GenAIx UI and for tooling, and where both exist the session credential wins.

The token itself is never stored by this client, never echoed back by the API, and
never reaches the model: it goes into the request and the field is cleared.

## Starting a session is sending the first message

`POST /sessions` takes an optional `message`, the same body a turn uses. One call
creates the session, appends the message as its first user turn and starts the run,
and the `201` comes back as a `SessionCreated`: the session with `run_id`,
`message_id` and `events_url` added.

So this client has no separate "intent" box. **New session** puts the conversation
column into new-session mode: a small header for the workflow, the title and the
context fields, and below it the ordinary composer, with its button reading **Start**.
Pressing it sends one `POST /sessions`, then opens the session and follows its event
stream. The `run_id` from the response primes the run state before any event arrives,
so the transcript shows the run as live and **Cancel run** works from the first paint.

`intent` is derived and read-only: the server sets it to the plain rendering of that
first message. The client never sends it, and the session list shows it under the
title, because it is what the user actually asked for in their own words.

Two things are deliberately not offered while composing the first message:

- **Attach file** is disabled. Files live under `<base>/sessions/<session_id>/uploads/`,
  so none can exist before the session id does, and a `file_ref` part or a
  `message.files` on this call is a `422`. A first turn with attachments is the
  three-call flow: create the session, upload, post the message. `lib/compose.js`
  exports `validateFirstMessageParts` for exactly this rule, and the preview under
  the box reports it rather than letting the server explain it.
- The two-call flow still works. Pressing **Start** with an empty box creates a
  session with no message, whose intent stays empty until the first turn.

## The composer: a user message is typed parts

`POST /sessions/{id}/messages` takes `parts`, an ordered list of typed elements in
the order the user composed them, and derives `content` from them. This client sends
`parts` and never `content`.

The composer is a `contenteditable` box rather than a textarea, because the part
model is positional: a reference the user at-mentioned mid-sentence is a part at that
position, not an entry in a list beside the text. What the box holds maps one to one
onto the parts:

| In the box | Part |
|---|---|
| typed text | `text` |
| a highlighted passage, from **Mark as verbatim** on a selection | `verbatim`, `source: user` |
| a blue chip, from **Add reference**, inserted at the caret | `reference` |
| a green chip, from **Attach file**, inserted at the caret | `file_ref`, carrying that chip's mode |
| the settings row above the box | `setting`, one per filled field |

Chips are inserted at the caret; when the caret was never in the box, they are
appended instead. A chip carries an `x` to remove it.

A file chip also carries its own **source | verbatim** switch. The upload-mode
selector sets it when the file is attached, and the switch changes it afterwards,
because `file_ref.mode` overrides for one message the mode the file was uploaded
with. A chip set to `verbatim` wears a small `verbatim` tag, the same tag the message
shows in the history, and puts the whole file under the `verbatim_identical` gate
with `unit: files`. The mode is always sent explicitly rather than left to the
file's own, so what the chip shows is what the message says. **Mark as verbatim** toggles:
with the caret inside a marked passage it unmarks it, and a selection that caught a
chip keeps the chip, moved after the passage, because a `verbatim` part is text only.
Pasting is forced to plain text, so markup from another page cannot reach the parts.

The settings row emits `setting` parts with the keys the API maps onto
`SessionContext` and `DerivedSettings`: `language`, `folder`, `template`. The
session's own context is shown as each field's *placeholder*, never as its value: a
`setting` part is a correction the user made for this request and is reported in
`derived_settings.corrected_by_user`, so echoing back what the agent already derived
would misattribute it. The row is cleared after a send, because a setting constrains
one request.

The row is optional, and the composer says so. A user may simply write the settings
into the sentence: GenAIx parses them out of the text itself and then asks for
confirmation before it writes anything, through a `settings_review` interaction. The
chips are the shortcut for a user who already knows the folder id; the free-text path
is the normal one. Either way what governs a CMS write is a `setting` part the user
stands behind.

Under the box, the derived plain rendering is shown live. It is produced by
`lib/compose.js` with the rules from the operation:

| Part | Renders as |
|---|---|
| `text`, `verbatim` | the text itself |
| `reference` | `@[label]` |
| `setting` | `#[key: value]` |
| `file_ref` | `[[file name]]` |
| `filter` | `#[label]` |

Parts are concatenated with nothing between them, so the spacing lives in the `text`
parts, and a literal `[` or `]` anywhere is escaped as `\[` or `\]`. The preview is
not decoration: it is the client asserting that it agrees with the server about what
the message says, and the test pins it against the worked example in `openapi.yaml`.

Input parts are a **closed** registry, unlike the output parts. The client validates
against `UserPartType` before sending and reports what the API would refuse with
`422`, rather than letting the server explain it.

A user message in the history is rendered from the same parts: verbatim passages
highlighted, references and files as chips, settings and filters as small tags above
the sentence. A part type this client does not know still shows its `text`, so a
history written by a newer client stays readable.

## The settings review: nothing inferred is written unconfirmed

GenAIx reads settings out of the user's own words, but an inferred setting never
governs a CMS write until the user has confirmed it. Before the first writing tool
call the run raises one batched `interaction.requested` of kind `settings_review`,
and the answer is made of real `setting` parts. The client renders that card as a
prefilled settings editor.

| In the interaction | In the card |
|---|---|
| `proposed[]` | one prefilled row: label, key, the value in an editable control |
| `proposed[].source` | a small tag, `from your text` / `from context` / `default` |
| `proposed[].evidence` | the quoted fragment of the user's message, under the row |
| `proposed[].options` / `missing[].options` | the control is a select over them, with each option's `ref` shown as a chip |
| `missing[]` | one empty row, marked required, that the run cannot proceed without |

`source` is the whole point of the tag: `from your text` means GenAIx guessed from
what the user wrote and the evidence quote says from which words, `from context`
means it was already in the session context, `default` means it is the installation
default. A proposed value that is not among its own options is kept as its own entry
in the select, so a value can never disappear by being rendered.

**Confirm** posts `{settings: [ ... ]}` to
`POST /sessions/{id}/interactions/{interaction_id}`, one `setting` part per row, with
the optional comment. The client refuses an incomplete answer itself rather than
letting the server explain it: the answer must carry every `proposed` and every
`missing` key, and an extra key the review never asked about is allowed, because the
API carries it as a further constraint. Next to Confirm is the same cancel path every
other pending run has: a review that is cancelled or times out ends the run without a
write, as an unanswered `choice` would.

`interaction.resolved` on a settings review carries `message_id`, and that event is
the only signal for it: no `message.started`, `part.*` or `message.completed` frames
follow, because the message was not produced by the model. The reducer therefore
builds the turn from the answer's `settings`, which are exactly the parts the server
stored, so the confirmed settings appear in the transcript as the user's own turn of
`#[key: value]` chips, tagged `confirmed in review` and carrying
`Message.interaction_id`. The same message comes back under that id from
`GET /sessions/{id}/messages` when the session is reopened, and the reducer keeps the
history copy rather than duplicating it.

Immediately after, `derived_settings.updated` lists every confirmed key in
`corrected_by_user`, and the derived-settings card marks those rows `user-corrected`:
they are the user's decision, not the agent's derivation, and the agent does not
derive them again in this session. A confirmed key with no row of its own is named
below the table rather than dropped.

A pending review also changes the composer. Settings chips filled in while the review
is open are collected into the same `SettingsAnswer` and ride along on the send as
`reply_to_interaction`, which is the plain-client path the API documents. Typed text
alone is *not* an answer, and a blocking review refuses a new message with `409`, so
the client refuses the send itself and names the keys still unanswered rather than
firing a call that cannot succeed.

In replay mode the S1 transcript walks the whole review: the card renders in full with
its controls disabled, and the recorded answer is what resolves it. Nothing is posted,
because there is no server.

## The part registry and the unknown fallback

`lib/render-parts.js` exports `PART_RENDERERS`, a plain object from part `type` to a
function `(part, ctx) => htmlString`. There is one entry per member of `PartType`:

`text`, `status_note`, `tree_view`, `selectable_list`, `properties_list`,
`image_grid`, `table`, `page_structure`, `construct_draft`, `api_call_log`,
`citation`, `file_ref`.

Adding a part type to the API means adding one entry. Until that entry exists,
`renderPartBody` falls back to `renderUnknown`, which is client rule 2 made visible:

- it renders the part's `text` field when the producer supplied one, as markdown
- it names the unknown `type` in a badge, so a contract reviewer can see that
  something new arrived rather than wondering why a message looks short
- it keeps the raw JSON behind a `<details>` disclosure, so nothing is lost and
  nothing leaks into the reading flow
- it never throws, never drops the surrounding parts, and never fails the message

A part with no `type` at all, and a renderer that throws, are both caught and
reported in place, for the same reason.

Renderers return HTML strings rather than DOM nodes. That has two payoffs: the whole
registry is importable under node, which is how `tests/parse-transcripts.mjs`
exercises it, and applying a `part.delta` is a single `innerHTML` on one wrapper
element rather than a transcript rebuild.

Local UI state that is not part of the wire format (table sort order, a pending
selection) lives on a non-enumerable `part._ui`, so it never gets sent back.

## Quality gates

A gate is something the harness ran, not something the model said about itself. The
panel therefore shows a gate's `status` (did it run) and its `verdict` (what it
judged) as two separate badges, because they deliberately differ: a gate can be
`failed` and still only `warn`.

Gate execution records exist **only** in `GET /sessions/{id}/workflow`. No event
carries a `GateResult`. What the stream does carry is `check.updated`, and in
draft.2 onward a check names the gate that produced it (`gate_id`), the gate's `kind` and
machine-readable `evidence`. So the client hydrates `gates` from the snapshot and
re-fetches that snapshot, debounced, whenever a gate-backed check arrives. The
reducer never synthesises a `GateResult` from a check; the test asserts that.

Before anything has run, `Workflow.quality_gates` from the catalogue stands in, the
same way `steps_template` stands in for the steps.

A check's `evidence` object and its gate's `evidence_file_id` are both reachable:
the evidence fields inline behind a disclosure, the artifact as a download link
(`artifact_type: gate_evidence`).

### Acknowledging a non-blocking finding

The transitions bar lists every non-blocking `warn` or `fail` check that is not
already acknowledged, each with a tick box. Ticking one and pressing a transition
sends its id in `acknowledge_checks`, which records that the user saw and accepted
it. A blocking check is never given a box, even if the server lists it in
`acknowledgeable_checks`, because a blocking failure cannot be waived.

When a blocking gate refuses the transition, the `409` `quality_gate_failed` problem
is rendered in full under the transitions bar: the failing gates, the failing checks
that explain them with their evidence, and the ids the server says may be
acknowledged. Fixing the finding re-runs the gate, so there is no retry button.

## Contract coverage

Every operation in `openapi.yaml` has a method in `lib/api.js`. The UI drives:

| Area | Routes |
|---|---|
| Start-up | `GET /ping`, `GET /me`, `GET /workflows` |
| MCP | `GET /mcp/connectors`; `GET`, `POST /mcp/connections`; `GET`, `PATCH`, `DELETE /mcp/connections/{id}`; `GET`, `PUT`, `DELETE /mcp/connections/{id}/authorization` |
| Sessions | `POST /sessions`, `GET /sessions`, `GET /sessions/{id}`, `PATCH /sessions/{id}`, `POST /sessions/{id}/transitions` with `acknowledge_checks` |
| Turns | `POST /sessions/{id}/messages`, `GET /sessions/{id}/messages`, `POST /sessions/{id}/runs/{run_id}/cancel` |
| Stream | `GET /sessions/{id}/events` with `after`, `Last-Event-ID` and reconnect |
| Interactions | `POST /sessions/{id}/interactions/{interaction_id}`, and `reply_to_interaction` on a message |
| Files | `POST /sessions/{id}/files` with `mode`, `GET /sessions/{id}/files`, `GET .../content` |
| Workflow | `GET /sessions/{id}/workflow` |

All 27 event types are handled. `auth.required` raises a banner that names the
connection by label, its `connector` type and its `connection_id`, shows the reason
and offers to jump to that connection's card, except for `unreachable`, where it
says plainly that no credential will help. The banner is keyed by `connection_id`,
so two unusable connections raise two banners even when they share a connector type.
The same condition before a run ever starts is raised from `GET /me`'s
`mcp_connections`.

`interaction.requested` renders a card per kind:
free text for `ask_user`, radio or checkbox options for `choice`, approve and reject
for `confirm`, a generated form for `form` covering strings, numbers, booleans
and enums from the interaction's JSON schema, and a settings editor for
`settings_review` (below). `interaction.resolved` collapses the card and shows the
answer. A pending `ask_user` question also turns the composer into its answer box,
through `reply_to_interaction`.

The session list and the session header show `workflow` with its
`workflow_version`, the new-session form disables a workflow whose `available` is
false and shows its `reason`, and the workflow column raises a badge on
`workflow_version_drift`. The session header also lists the connections the session
works against, from `context.connection_ids`.

The new-session form offers a connection picker per entry in the workflow's
`required_connectors`, but only when the user holds more than one connection of that
type: with a single connection the server's default is the right answer and a
one-item dropdown is noise. The chosen ids go out as `context.connection_ids`. In
the workflow column, a CMS object that carries a `connection_id` is labelled with
that connection, because with two CMS connections "page 9142" alone is ambiguous.

Opening a session follows the reconnect checklist in order: `GET /sessions/{id}`,
then the message history, then the workflow snapshot, then the event stream. It then
connects with `after=0` rather than the highest seq it holds, because the message
history carries no `seq` and because replaying the whole session fills the event log.
That is safe precisely because the reducer is idempotent: the replayed parts converge
on the same content the history already showed. A production client that does not
want the replay would track `seq` itself and resume from it. The
stream reconnects with exponential backoff, and on `410 events_pruned` it
re-hydrates from the message history and restarts at `after=0`, as the contract
requires. The workflow column shows "waiting for GenAIx" after 20 seconds of
silence, which is longer than the 15-second heartbeat interval.

## Known limitations

- **Pagination is ignored.** `next_cursor` is never followed, so the session list,
  the message history and the file list show the first page only (100 items).
- **`Workflow.inputs_schema` is not turned into a form.** `SessionCreate` has no
  `inputs` field, so the session's `inputs` is derived server-side; the new-session
  form collects node, folder and language into `context` as before.
- **The first message cannot carry files.** That is the contract, not a client
  limitation, but it does mean a first turn with an upload takes the two-call flow:
  press Start with the text, then attach and send in the second turn.
- **`SessionCreate.labels` and `model` are not exposed.** `buildSessionCreate`
  accepts a model, but the form offers no picker; the run-mode selector is shared
  with the message composer and applies to the first run too.
- **One session at a time.** Opening another session closes the previous stream.
  The contract supports several streams; this client does not need to.
- **No `types` or `run_id` filter on the stream.** The client always takes the
  unfiltered stream and filters in the event log instead.
- **Markdown is a subset**: headings, bold, italics, inline code, fenced code,
  links, and both list kinds. A production client should use a vetted library. The
  renderer escapes first and only then adds markup, and it refuses link targets that
  are not `http(s)` or relative, but it has not been audited as a sanitiser.
- **A verbatim part marked in the composer always has `source: user`.** Lifting a
  quote out of an uploaded file and recording that file's id as the `source`, which
  the schema allows, needs a document viewer this client does not have. Marking a
  whole upload is supported, through the upload mode selector and the switch on the
  file chip.
- **`filter` parts are rendered but never composed.** The client renders a `filter`
  in the preview and in the history, and `lib/compose.js` builds one from a token,
  but there is no faceted search UI to produce the `criteria`. That belongs to a
  client with a content browser.
- **`setting` parts are limited to three keys.** The row offers `language`, `folder`
  and `template`; `node`, `publish_at` and any other key the agent would carry
  through are not exposed, though the builder accepts them.
- **The composer is `contenteditable`, with the caveats that brings.** Paste is
  forced to plain text and chips are `contenteditable="false"`, but browser editing
  behaviour around an inline non-editable element differs between engines. A
  production client should use an editor framework.
- **Form interactions support only scalar JSON schema types.** Objects and arrays are
  shown as a text field with a note saying so, rather than being dropped.
- **The preview iframe will often be blank.** A CMS preview URL points at a host that
  normally sets `X-Frame-Options` or a frame-ancestors policy. The panel always shows
  the URL and an "Open" link so the contract is still checkable.
- **The installation token lives in `localStorage`.** In production the CMS proxy
  holds it server-side and a browser never sees it. This client talks to GenAIx
  directly, which is why it has the field at all. Use a mock token.
- **`window.prompt` for three inputs**: adding a context reference, correcting the
  session context, and entering a transition comment.
- **No retry of a failed message.** After `auth.required` and a successful
  registration, the contract says to re-send the message that failed. This client
  clears the banner and leaves the re-send to the user.
- **Gates are as fresh as the last snapshot fetch.** They are not in the event
  stream, so the client re-fetches `GET /sessions/{id}/workflow` when a gate-backed
  check arrives, debounced by 600 ms. A gate whose result produces no check is not
  noticed until the next transition or reopen.
- **The OAuth2 flow is only started, never completed here.** `action: start` and the
  returned `authorization_url` are shown; the callback lands on GenAIx, and this
  client polls with Re-check rather than listening for anything. No Phase One server
  uses `oauth2`.
- **A registered bearer token is typed into the page.** It goes straight into
  `PUT /mcp/connections/{id}/authorization`, the field is cleared on success and it
  is never stored in `localStorage`, but it does pass through the browser, which is
  inherent to the client-provisions model.
- **The connection picker is hidden for a single connection.** A user with one `cms`
  connection never sees it, so `context.connection_ids` is then left unset and the
  server's default applies.
- **`max_connections_per_connector` is not read anywhere.** The client discovers the
  cap by getting `409 mcp_connection_limit` and reports it.
- **The client does not create the token it registers.** A real Workspace calls the
  target system itself (for the CMS, `POST /rest/admin/token` with a name, an expiry and
  `pruneOnExpiry`) and never shows the token to anyone. Here it is pasted into the page,
  which is inherent to a reference client that talks to GenAIx and nothing else.
- **The session authorization lifetime is assumed, not read.** 24 hours is the
  installation default from the design brief; the API publishes no field for it, so the
  form defaults to that and lets the user change it.
- **Expiry is not watched.** A credential that expires while a session is open is
  noticed when a run fails or the list is re-read, not by a timer.
- **Replay mode is read-only.** The composer, the transitions and the interaction
  cards are disabled, because there is no server to post to.

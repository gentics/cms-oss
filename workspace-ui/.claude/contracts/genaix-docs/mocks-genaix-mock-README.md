# GenAIx API v1 mock

A standalone Python service that implements every route of
[`../../openapi.yaml`](../../openapi.yaml) (`1.0.0-draft.3`) with in-memory state, and
replays the recorded scenario transcripts in [`../../examples`](../../examples) as live
Server-Sent Events.

It exists so the Workspace UI and the CMS auth/streaming proxy can be built and tested
before the real GenAIx changes land. Nothing here talks to a model, to a CMS or to an MCP
server: every agent turn is a transcript replayed with realistic pacing.

## Run it

```bash
./run.sh                      # http://localhost:8080/api/v1
GENAIX_MOCK_PORT=9090 ./run.sh
./run.sh test                 # the test suite
./run.sh test -k gate         # one test
```

`run.sh` creates `.venv` on first use (with `uv` when available, otherwise
`python3 -m venv`), installs the pinned `requirements.txt` and starts uvicorn with
reload off. Python 3.11 or newer.

- Swagger UI: <http://localhost:8080/api/v1/docs>
- The contract itself, served verbatim: <http://localhost:8080/api/v1/openapi.yaml>
- An index with the loaded scripts and connector types: <http://localhost:8080/api/v1>

CORS is open to any `http://localhost:<port>`, `127.0.0.1`, `[::1]`, and to the `null`
origin a page opened from `file://` sends, so the static reference client in
[`../../client`](../../client) works without a web server.

## Headers on every request

| Header | Required | Mock behaviour |
|---|---|---|
| `Authorization: Bearer sk_gnx_mock` | yes | Any other scheme or an unknown token is `401` `invalid_installation_token`. By default any token starting with `sk_gnx_` is accepted, which is what the spec's mock server description promises; `GENAIX_MOCK_STRICT_TOKEN=1` accepts only `GENAIX_MOCK_TOKEN`. |
| `X-GCMS-Subject` | yes, except `GET /ping` and `DELETE /subjects/{subject}` | The **only** identity header and the owner key of every session, file, connection and authorization. Missing or empty is `400` `subject_missing`. Sessions are partitioned per subject: another subject's session id is `403` `session_forbidden`, an unknown one is `404` `session_not_found`. |

That is the whole header contract: one token, one subject, nothing else. The subject is
an opaque, installation-scoped pseudonym, so the mock never learns which person it stands
for. No user id, login, display name or group list is sent, stored or returned anywhere;
`/me` reports `user: {subject}` and nothing more. `X-GCMS-User-Id`,
`X-GCMS-User-Login`, `X-GCMS-User-Groups`, `X-GCMS-SID`, `X-GCMS-User-Token` and
`X-GCMS-Session-Secret` do not exist in `v1` and are ignored.

The value may be a bare string or a compact JWS carrying `sub`, `iss`, `iat` and `exp`.
Set `GENAIX_MOCK_IDENTITY_KEY` to have the mock verify it: the signature must match
(HS256), `exp` must be in the future, and the `sub` claim becomes the subject. A bare
string, a bad signature, a stale assertion or one without `sub` is then `401`
`identity_invalid`. Without the variable the header is used verbatim, signed-looking or
not, which is the convenient default for a client under development.

`DELETE /subjects/{subject}` erases every session, message, event, file, connection and
authorization of one subject and returns `204`. It carries the installation token and no
subject header of its own, because it is the installation acting on a subject rather than
a user acting on their own data, and it is idempotent: a subject that was never seen is a
`204` too.

GenAIx knows no group membership, so the publish branch comes from two fixture rules. A
`publish` transition resolves to a direct publish when the session holds an authorization
whose token starts with `publisher_`, or when the subject is listed in
`fixtures/publishers.json` (`GENAIX_MOCK_PUBLISHERS`); otherwise it resolves to a review
request. That keeps both branches reachable without inventing an identity the contract no
longer has.

Every response carries `X-GenAIx-Request-Id`. Every error is an RFC 9457
`application/problem+json` document with a `genaix_code`, including unknown routes.

## Connect to a CMS first

GenAIx defines connector **types**; a user creates **connections** of a type, each with a
URL the client supplies, and registers a credential on the connection. `cms` is a
`multiple` connector, so one user can reach several CMS instances.

Every workflow but `free_chat` needs a mandatory `cms` connection, so a fresh user gets
`capabilities.workflows: ["free_chat"]` and a run that needs the CMS answers
`auth.required` followed by `run.failed`. The first `GET /mcp/connections` (and
`GET /me`) auto-creates the installation's default `cms` connection from
`GENAIX_MOCK_CMS_MCP_URL`, labelled `Acme GmbH demo CMS` and `default: true`, so the Workspace
path only has to register a token on it:

```bash
B=http://localhost:8080/api/v1
H=(-H 'Authorization: Bearer sk_gnx_mock' -H 'X-GCMS-Subject: sub_c3f1a07d9e5b4826')

CID=$(curl -s "${H[@]}" "$B/mcp/connections"   | python3 -c 'import json,sys; print(json.load(sys.stdin)["items"][0]["id"])')

curl -sX PUT "${H[@]}" -H 'Content-Type: application/json'   -d '{"auth_type":"bearer","token":"cmstok_anything"}'   "$B/mcp/connections/$CID/authorization"
```

`GET /mcp/connectors` is the read-only type catalogue. `POST /mcp/connections` adds
another connection, `GET`, `PATCH` and `DELETE /mcp/connections/{id}` manage one, and the
credential lives at `/mcp/connections/{id}/authorization`. An unknown or foreign
connection id is `404` `mcp_connection_not_found`.

URL rules are enforced on `POST` and `PATCH`: `https`, with `http` allowed only for
`localhost` and `127.0.0.1`; no embedded credentials, no query string, no fragment.
A violation is `422` `validation_failed` pointing at `/url`. The reachability probe is
informational and stubbed, so a connection is always created even when its server is
down; a URL whose host starts with `unreachable` reports `status.reachable: false` so that
branch is visible. More than `GENAIX_MOCK_MAX_CONNECTIONS` (10) connections of one type is
`409` `mcp_connection_limit`.

A `PATCH` that changes `url` **drops the stored credential**, because a token issued by
the old system proves nothing about the new one. Changing only `label` or `default` keeps
it. The only connection of a type is always its default, so unsetting that flag is `422`;
deleting the default promotes the oldest remaining one.

The mock never contacts a real MCP server. Registration decides the outcome from the token
prefix, so every branch of the contract is reachable:

| Token starts with | Result |
|---|---|
| `invalid_` | `422` `mcp_authorization_rejected` with an `upstream` object, nothing stored |
| `mismatch_` | `422` `mcp_authorization_rejected`, the token resolves to another user, nothing stored |
| `unreachable_` | `424` `mcp_unreachable`, nothing stored |
| `expired_` | stored, and the authorization reports `expired` |
| `revoked_` | stored, and the authorization reports `invalid` |
| anything else | stored, `authorized` |

A refused registration never replaces a working credential. `PUT` with
`{"auth_type":"oauth2","action":"start"}` is accepted only for a connector that declares
`oauth2`; `cms` declares `bearer`, so it answers `422` naming `/auth_type`.

A run resolves one connection per required connector type: `context.connection_ids` wins,
otherwise the user's default of that type. The objects a run touches record where they
live in `cms_objects[].connection_id`, and the replay engine rewrites the transcript's
recorded connection id to the live one.

`GENAIX_MOCK_GATE_WORKFLOWS=0` turns the narrowing off: every workflow is listed and every
run proceeds without an authorization, which is convenient while wiring a client up.

## Session-scoped authorizations

A connection can be authorized twice over: once **standing**, per user, at
`/mcp/connections/{id}/authorization`, and once **per session**, at
`/sessions/{session_id}/authorizations/{connection_id}`. The session one wins wherever
both exist; a connection the session has no entry for falls back to the standing one, and
a run that finds neither emits `auth.required` and fails with `mcp_authorization_required`.

```bash
curl -sX PUT "${H[@]}" "${J[@]}" -d "{
  \"auth_type\": \"bearer\",
  \"token\": \"cmstok_session\",
  \"token_name\": \"genaix-$SID\",
  \"expires_at\": \"2026-10-08T09:14:22Z\"
}" "$B/sessions/$SID/authorizations/$CID"

curl -s "${H[@]}" "$B/sessions/$SID/authorizations"
curl -sX DELETE "${H[@]}" "$B/sessions/$SID/authorizations/$CID"
```

`POST /sessions` takes the same credentials as `authorizations: [{connection_id, auth_type,
token, ...}]`, verified **before** the session is created, so a refused token leaves
nothing behind. With `message` alongside, creating the token, the session and the first
turn is one round trip.

The token prefixes decide the outcome, exactly as for the standing authorization:
`invalid_` and `mismatch_` are `422` `mcp_authorization_rejected`, `unreachable_` is `424`
`mcp_unreachable`, and none of the three replaces what was stored before. `expired_`
stores and reports `expired`, `revoked_` reports `invalid`, anything else `authorized`.
There is no `unauthorized` status here: a session with no credential for a connection
simply has no entry.

Lifetime, which the mock enforces on every request that touches the session:

| The credential is dropped when | What is left |
|---|---|
| `expires_at` passes | the entry, reporting `expired`, so the client sees why the run failed |
| the session reaches `published`, `discarded` or `archived` | nothing; the list is empty |
| `DELETE .../authorizations/{connection_id}` | nothing |

It is kept through `released` and `review_requested`, because publish and request-review
still act on the CMS. To see expiry without waiting, register a token with an `expires_at`
in the past. `GENAIX_MOCK_SESSION_AUTH_HOURS` (default 24) is the lifetime stamped on a
registration that sends none.

`Session.authorizations` and the workflow view carry the same list in short form
(`connection_id`, `status`, `expires_at`), so one snapshot call says whether the session
can still act.

## Creating a session

`POST /sessions` takes an optional `message`, the same body as
`POST /sessions/{id}/messages`. With it, one call creates the session, derives `intent`,
stores the first user turn and starts its run; the `201` is a `SessionCreated`, the
session plus `message_id`, `run_id` and `events_url`, so the client goes straight to the
stream. `parts` is the shape to send.

```bash
curl -s "${H[@]}" "${J[@]}" -d '{
  "workflow": "content_create",
  "title": "Landing page - product launch",
  "context": {"node_id": 3, "language": "de"},
  "message": {"parts": [
    {"type": "text", "text": "Create a landing page in "},
    {"type": "reference", "ref": {"type": "folder", "id": "4021", "node_id": 3, "label": "Campaigns"}},
    {"type": "text", "text": " in "},
    {"type": "setting", "key": "language", "value": "de", "label": "German"},
    {"type": "text", "text": "."}
  ]}
}' "$B/sessions"
```

Without `message` the session is created empty and the first turn is posted separately.
Both flows are supported.

`intent` is derived and read-only: it is the plain rendering of the **first** user
message, whichever flow produced it, and later turns do not replace it. Sending `intent`
on `POST /sessions` is `422` `validation_failed` like any other unknown member. A session
created without a message has no intent until one arrives.

Files are per session, so a first `message` cannot name any: `message.files` and a
`file_ref` part in `message.parts` are `422`, as is a `verbatim` part whose `source` is a
file id. A turn with an attachment is three calls, which is what the lifecycle below
does. A `reply_to_interaction` on the first message is refused for the same reason.

Every message also adds what the user pointed at to `context.references`, which
accumulates over the session.

## User messages: content or parts

`POST /sessions/{id}/messages` takes either shape. `parts` is the canonical, ordered
structure of what the user composed; `content` is its plain rendering. At least one of
the two is required and `parts` wins when both are sent: the mock then derives `content`
from the parts, `references` from the `reference` parts and `files` from the `file_ref`
parts, and ignores whatever the client put in those three fields.

| Part | Renders into `content` as |
|---|---|
| `text` | the text itself |
| `verbatim` | the text itself |
| `reference` | `@[label]`, falling back to `@[type id]` without a label |
| `setting` | `#[key: value]` |
| `file_ref` | `[[file name]]`, the name the mock stored for that session file |
| `filter` | `#[label]` |

Parts are concatenated in order with nothing inserted between them, so the spacing
belongs to the `text` parts. A literal `[` or `]` in a label, a key, a value, a file name
or a text is escaped as `\[` and `\]`, which keeps the rendering readable back.

The input registry is closed. A part whose `type` is not one of the six is `422`
`validation_failed` with a JSON Pointer at the offending part, and so is an unknown
member of a known part.

Both part kinds that can name a session file are checked against it: `file_ref.file_id`,
and `verbatim.source` when it is not the literal `user`. An id that is not in the session
is `422` `validation_failed` pointing at that member with `code: file_not_found`. An
unknown id in the plain `files` array stays `404` `file_not_found`, which is the older
convention of this route.

Verbatim content is declared at two granularities the mock keeps apart: `mode=verbatim`
on `POST /sessions/{id}/files` marks a whole upload, and a `verbatim` part marks one
quote, with `source` naming either `user` or the upload the quote came from. `mode` on a
`file_ref` part overrides the mode the file was uploaded with, for that message. Nothing
is hashed or compared: the mock stores the declaration, and a verbatim part renders as
its own text whatever its source.

History stores the parts as they were sent, so `GET /sessions/{id}/messages` returns the
composed message rather than only its flattened text. A client without a part-aware
composer sends `content` and gets one `text` part back.

`setting` parts are constraints: the mock reflects `node`, `folder`, `template` and
`language` in `GET /sessions/{id}/workflow` -> `derived_settings` and lists them in
`corrected_by_user`. They outrank the scripted run, so a `derived_settings.updated` event
carries the user's value where they set one and the script's value everywhere else. The
other part kinds are stored and rendered but have no further effect in the mock: no
reference is resolved, no verbatim text is hashed and no filter reaches a search.

## Settings review

A setting GenAIx inferred from the user's prose never governs a CMS write unconfirmed.
Before the first writing step, S1 raises one batched `interaction.requested` of kind
`settings_review`:

- `proposed` carries what the run derived, each entry with `key`, `value`, `label` and
  `source` (`text`, `context` or `default`), plus `evidence`, the quoted fragment of the
  user's own text an inference rests on, and `options` where a choice exists.
- `missing` carries what the run needs and could not derive, each with `key`, `label` and
  the `options` to choose from when the set is known.

A client renders `proposed` as prefilled, editable chips and `missing` as empty ones. The
answer is a `SettingsAnswer`, through `POST /sessions/{id}/interactions/{interaction_id}`
or as `reply_to_interaction` on the next message:

```jsonc
{"answer": {"settings": [
  {"type": "setting", "key": "folder",   "value": "42", "label": "Richtlinien"},
  {"type": "setting", "key": "language", "value": "de", "label": "Deutsch"},
  {"type": "setting", "key": "template", "value": "17", "label": "Kampagnen-Landingpage"}
]}}
```

`settings` are real `setting` parts, the same ones a user message is made of, and an
accepted value is indistinguishable from a corrected one: what the run acts on is always
something the user sent. The mock refuses with `422` `validation_failed` when a key the
review raised has no part (`/answer/settings`, one error per key), when a value is outside
the `options` offered for its key (`/answer/settings/{index}/value`), or when an entry is
not a `setting` part. Keys the review did not raise are allowed and are kept as further
constraints. Expiry, cancellation and the double answer behave as for every interaction:
`410` after the lifetime, `409` on a second answer. The contract ends a run whose review
timed out before it writes anything; the mock has no writes to withhold, so the replay
continues from the transcript as it does after any unanswered question.

On resolution the mock stores a **user-role message whose `parts` are exactly the
confirmed settings**, with `interaction_id` pointing at the review. It appears in
`GET /sessions/{id}/messages`, `interaction.resolved` names it in `message_id`, and the
settings become `derived_settings` with every confirmed key in `corrected_by_user` and
`SessionContext` members (`folder_id`, `template_id`, `language`, `node_id`) so the next
run starts from the decision. The history therefore shows the settings as the user's own
turn rather than as something the agent assumed, and a confirmed value is never
overwritten by the transcript afterwards.

**When the mock does not ask.** If the user already decided every setting the review would
raise - a `setting` part on the message (chips), or the `context` the session was created
with - there is nothing to confirm. The review frames are then dropped from the replay
together with the `request_interaction` call around them and the step update that
would wait, so a chip-driven client never sees an interaction at all and the run goes
straight through. The transcript's `corrected_by_user` still lists those keys, which is
what the mock has to say about them: they were the user's input either way.

## Scenario mapping

The session `workflow` picks the script for the **first** user message. A marker is
looked for in the rendered `content`, so it works in a `text` part just as in `content`. Scripts are data:
the mock parses every `.sse` file under `../../examples` and `./scripts` at start-up, so a
new transcript dropped into `scripts/` is picked up on restart. A transcript carrying
several runs is also registered per run as `<name>#<n>`, because the canonical stream is
session-scoped while a script replays one run.

| Selected by | Script | What it produces |
|---|---|---|
| `content_create` | `examples/s1-content-create.sse` | 90 events: plan, steps with substeps, CMS writes, a streamed `page_structure` part, a verbatim proof, a `settings_review` interaction, previews, an artifact |
| `content_research` | `examples/s2-content-research.sse` | 44 events: search calls, a streamed `table` part, citations, no CMS writes |
| `construct_create` | `examples/s3-construct-create.sse` | 56 events: catalogue check, a streamed `construct_draft` part, Handlebars validation, a `confirm` interaction, an `api_call_log` part, preview |
| `admin_user` | `examples/s4-admin-user.sse` | 39 events: group lookup, a `form` interaction, permission impact simulation, user creation |
| `free_chat` | `scripts/free-chat.sse` | A short generic answer |
| second message onward | `scripts/followup.sse` | A status line, one text part, `run.completed` |
| `publish` / `request_review` | `scripts/publish-direct.sse` or `scripts/publish-review.sse` | Permission check, approval chain, publish call, a closing text part |
| `#fail` in the content | `examples/s5-failure-and-recovery.sse#1` | A page locked by another user, a recoverable `error`, an artifact, then `auth.required` and `run.failed` |
| `#recover` in the content | `examples/s5-failure-and-recovery.sse#2` | `artifact.updated`, then `run.cancelled` because the user pressed Stop |
| `#gatefail` in the content | `scripts/gate-failure.sse` | A blocking gate check that failed plus a non-blocking warning, so the `409` refusal can be exercised |
| `#gateack` | `scripts/gate-nonblocking.sse`: one non-blocking gate check fails; transitions answer 409 `quality_gate_failed` with empty `failing_gates` until the client repeats with `acknowledge_checks` |

The three markers exist because the happy-path scenarios do not reach the unhappy events,
and a client still has to render them. Between the markers, a real cancellation and a run
with no MCP authorization, all 27 event types of the contract are reachable.

Replaying rewrites `session_id`, `run_id`, `message_id`, interaction ids and session file
ids to the live ones, re-stamps `seq` and `ts` through the session's event bus, writes
artifacts to disk, materialises the final assistant message into the history and blocks on
`interaction.requested` until the answer arrives.

`seq` is a total order per session starting at 1, gapless, never reused. Heartbeats
consume a `seq` and are persisted, as the contract requires.

## Workflow modules and quality gates

`GET /workflows` reports the modules exactly as the contract's example does: `version`,
`inputs_schema`, `required_connectors` as `{type, optional}` objects, `tool_groups` as an
allowlist keyed by connector type plus the reserved `genaix` key, `quality_gates` (each with
`triggers`, the full trigger list, next to the first trigger in `when` / `step_id`),
`steps_template` with `substeps`, plus the per-user `available` and `reason`. A session
always carries `workflow_version`, `inputs` (the context members the module's
`inputs_schema` names) and `authorizations` (`1.0.0-rc.2`).

`GET /sessions/{id}/workflow` adds `gates`, one `GateResult` per declared gate. The mock
runs no gates: a verdict arrives as a `check.updated` event carrying `gate_id`, and the
replay engine folds it into the matching gate record, mapping severity `pass` or `info` to
verdict `pass`, `warn` to `warn` and `fail` to `fail`. A declared gate that no transcript
reports stays `pending` with `attempt: 0`.

`release`, `publish` and `request_review` are refused with `409` `quality_gate_failed`
while a blocking gate-sourced check has severity `fail`, or a non-blocking one has
severity `warn` or `fail` and has not been acknowledged (`1.0.0-rc.2`: a warning never
silently disappears, so the S1 run's `alt_text` warning has to be acknowledged before the
first release). The problem carries `failing_checks` (every such check, blocking first),
`failing_gates` (the full `GateResult` of each blocking one) and `acknowledgeable_checks`.
Blocking checks cannot be waived; listing a non-blocking check in `acknowledge_checks`
records `acknowledged_by` and `acknowledged_at` on it and takes it off the list. An action
that is illegal from the current status is `409 workflow_transition_invalid` before any
gate is consulted, a repeated `discard` or `archive` is `200` with no event, and `discard`
and `archive` are never gated. `tool.started.tool` on every replayed stream is the bare
wire name of `mcp/tools.json`, with `group` alongside.

## Pacing

`GENAIX_MOCK_SPEED` divides every delay: `1` is real time (an S1 run takes roughly a
minute), `40` makes the suite run in seconds. Text deltas are about 30 ms apart and tool
calls take 300 to 800 ms.

## A full lifecycle with curl

```bash
B=http://localhost:8080/api/v1
H=(-H 'Authorization: Bearer sk_gnx_mock' -H 'X-GCMS-Subject: sub_c3f1a07d9e5b4826')
J=(-H 'Content-Type: application/json')

CID=$(curl -s "${H[@]}" "$B/mcp/connections" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["items"][0]["id"])')
curl -sX PUT "${H[@]}" "${J[@]}" -d '{"auth_type":"bearer","token":"cmstok_x"}' \
  "$B/mcp/connections/$CID/authorization"

# This turn carries an upload, so it is the two-call flow: create, upload, then post.
SID=$(curl -s "${H[@]}" "${J[@]}" \
  -d '{"workflow":"content_create","title":"Landing page","context":{"node_id":3,"folder_id":42,"language":"de"}}' \
  "$B/sessions" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')

FID=$(curl -s "${H[@]}" -F file=@../../examples/session-lifecycle.md -F mode=verbatim \
  "$B/sessions/$SID/files" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')

curl -sX POST "${H[@]}" "${J[@]}" -d "{\"parts\":[
  {\"type\":\"text\",\"text\":\"Create the landing page from \"},
  {\"type\":\"file_ref\",\"file_id\":\"$FID\",\"mode\":\"verbatim\"}
]}" "$B/sessions/$SID/messages"

curl -N "${H[@]}" "$B/sessions/$SID/events?after=0"
```

S1 pauses on the `settings_review`: the session above set the folder and the language, so
the template is the one thing left to decide. Answer it from a second shell with the
`interaction.id` the stream reported, confirming every key it raised, then release and
publish:

```bash
curl -sX POST "${H[@]}" "${J[@]}" -d '{"answer":{"settings":[
  {"type":"setting","key":"folder","value":"42","label":"Richtlinien"},
  {"type":"setting","key":"language","value":"de","label":"Deutsch"},
  {"type":"setting","key":"template","value":"17","label":"Kampagnen-Landingpage"}
]}}' "$B/sessions/$SID/interactions/$IID"
curl -sX POST "${H[@]}" "${J[@]}" -d '{"action":"release"}' "$B/sessions/$SID/transitions"
curl -sX POST "${H[@]}" "${J[@]}" -d '{"action":"publish"}' "$B/sessions/$SID/transitions"
```

## Configuration

| Variable | Default | Meaning |
|---|---|---|
| `GENAIX_MOCK_PORT` | `8080` | Listen port |
| `GENAIX_MOCK_HOST` | `0.0.0.0` | Listen address |
| `GENAIX_MOCK_TOKEN` | `sk_gnx_mock` | Installation token |
| `GENAIX_MOCK_STRICT_TOKEN` | `0` | `1` accepts only the exact token |
| `GENAIX_MOCK_SPEED` | `1` | Replay speed divisor |
| `GENAIX_MOCK_HEARTBEAT` | `15` | Seconds of silence before a `heartbeat` |
| `GENAIX_MOCK_INTERACTION_TIMEOUT` | `120` | Seconds before a pending interaction expires |
| `GENAIX_MOCK_UPLOAD_MAX_BYTES` | `26214400` | Per-file upload limit |
| `GENAIX_MOCK_DATA_DIR` | `./data` | `data/sessions/<id>/{uploads,artifacts}` |
| `GENAIX_MOCK_GATE_WORKFLOWS` | `1` | `0` lists every workflow and lets runs proceed without an authorized connection |
| `GENAIX_MOCK_INSTALLATION_NAME` | `mock` | Drives the suggested token name `genaix-<slug>` |
| `GENAIX_MOCK_CMS_BASE_URL` | `https://cms.example.com` | Reported in `GET /me` |
| `GENAIX_MOCK_IDENTITY_KEY` | unset | HS256 key for `X-GCMS-Subject`. Set it to verify signed assertions; unset, any value is accepted as sent. |
| `GENAIX_MOCK_PUBLISHERS` | `fixtures/publishers.json` | Subjects whose `publish` goes direct instead of to review. |
| `GENAIX_MOCK_SESSION_AUTH_HOURS` | `24` | Lifetime stamped on a session authorization registered without an `expires_at`. |
| `GENAIX_MOCK_MAX_CONNECTIONS` | `10` | Connections one user may hold per connector type |
| `GENAIX_MOCK_CONNECTION_LABEL` | `Acme GmbH demo CMS` | Label of the auto-created default connection |
| `GENAIX_MOCK_CMS_MCP_URL` | `https://cms.example.com/mcp` | The `cms` connector's `default_url` and the URL of the auto-created connection. The CMS MCP endpoint is `/mcp`; set this to whatever the installation serves. |
| `GENAIX_MOCK_SPEC` | `../../openapi.yaml` | The file served at `/api/v1/openapi.yaml` |
| `GENAIX_MOCK_EXAMPLES` | `../../examples` | Where scenario transcripts are loaded from |
| `GENAIX_MOCK_SCRIPTS` | `./scripts` | Where the mock's own transcripts live |

## What is mocked and what is real

Real, in the sense that a client cannot tell the difference:

- Every route, status code, header and response shape of `openapi.yaml`.
- The SSE framing (`id:` is the decimal `seq`, `event:` is the type), replay from `after`
  or `Last-Event-ID`, the `types` and `run_id` filters, `follow=false`, heartbeats, and
  persistence of every event with a gapless `seq`.
- Connectors and connections: the auto-created default, the URL rules, the per-user cap,
  re-pointing dropping the credential, default promotion, verify-before-store, a refusal
  that keeps the previous credential, the status narrowing `capabilities.workflows` and
  `Workflow.available`, and a run that fails with `auth.required` plus
  `mcp_authorization_required` naming the connection it could not use.
- The session status machine, including `release` cancelling an active run, `publish`
  resolving server-side to `published` or `review_requested`, and the quality gate
  refusal with its acknowledgement rules.
- One active run per session, cancellation, and the interaction round trip with `409` on
  a double answer and `410` after expiry.
- The settings review: the answer validated against the keys it raised and the options it
  offered, the confirmed settings stored as the user's own message, and the review skipped
  when chips or the session context already answered it.
- The per-session filesystem, with real bytes, real sha256, real downloads.
- Problem documents, with the `genaix_code` the contract names for each case.

Mocked:

- No model. Assistant turns are recorded transcripts, so the answers do not respond to
  what you actually typed. Only the first turn of a session is scenario-specific.
- No MCP server and no CMS. Tool events are replayed, CMS object ids are the ones from the
  transcripts (page 9142, folder 42, node 3), and preview URLs point at `cms_base_url` and
  will not load.
- Authorization is never verified against anything: the status follows the token prefix.
  The reachability probe contacts nothing either, so `status.reachable` is true for every
  URL except a host starting with `unreachable`, and `server_info` is a fixed stub.
- No gate runner. Gate verdicts come from the transcript's check events, so a gate no
  transcript reports stays `pending`, and a `before_release` gate never runs. The contract
  would refuse a transition on a gate that has not passed; the mock refuses only on a
  blocking check that actually failed, otherwise nothing could ever be released.
- Artifacts contain a short placeholder body, not a real page draft.

## Limitations

- State is in memory. A restart loses every session, connection and authorization; only
  the uploaded and generated files remain under `data/`.
- No pagination cursors beyond a decimal offset, no rate limiting, so `429` is never
  returned. `423` is likewise never returned.
- Events are never pruned, so `410` `events_pruned` cannot be provoked and
  `capabilities.events_retention_hours` is absent, which the contract reads as no pruning.
- `options.mode: plan_only` is accepted and recorded but does not change the script.
- `inputs_schema` is reported but not enforced; only `content_create` declares one, so
  only it derives `Session.inputs`.
- `PATCH /sessions/{id}` stores a corrected context but the next run replays the same
  transcript, so a context patch does not reach `derived_settings`; only `setting` parts
  on a message and a confirmed `settings_review` do. A patched context does count as the
  user's decision for the question of whether a review is raised.
- `workflow_version_drift` is always `false`: the mock has one version per module.
- Multi-process deployment is not supported: the event bus is per process.

## Tests

```bash
./run.sh test
```

64 tests. They start a real uvicorn server on an ephemeral port, because in-process ASGI
transports buffer responses and the SSE behaviour is the point. Covered: a full lifecycle
per scenario (create, upload, message, consume the stream to `run.completed`) asserting
event order, `seq` monotonicity, that every event validates against
`../../schemas/events.schema.json` and every stored assistant part against
`../../schemas/parts.schema.json` and every stored user part against
`../../schemas/user-parts.schema.json`, that the history carries the streamed parts and that
the gates match the module's declaration; every REST response validated against its
`components.schemas` entry in `openapi.yaml`; the connection lifecycle (auto-create, the URL
rules, re-pointing dropping the credential, default promotion, the cap) and the
authorization lifecycle through all five statuses; a run that fails for want of an
authorized connection and succeeds after one is registered; a session selecting a
non-default connection through `context.connection_ids`; the blocking quality gate refusal and its acknowledgement rules; the auth
matrix; `403`, `404`, `409`, `410`, `413` and `422`; the interaction round trip and its
expiry; cancellation; heartbeats; replay and filters; the publish branches; the three
marker transcripts; the user message parts (the rendering table and its escaping, the derived
`references` and `files`, the closed input registry answering `422` with a JSON Pointer, the verbatim
declarations on an upload and on a part, and `setting` parts outranking the transcript's
`derived_settings`); the settings review (the shape of `proposed` and `missing` with its
`source` and `evidence`, an answer that misses a key or leaves the offered options, the
confirmed settings stored as a user message carrying `interaction_id` and named by
`interaction.resolved.message_id`, the corrected values reaching `derived_settings`,
`corrected_by_user` and the session context, answering through `reply_to_interaction`, and
the review being skipped when chips or the session context already cover it); creating a
session with its first message in one call, the derived
read-only `intent`, the two-call flow and the members a first message cannot carry;
the session-scoped authorizations (register, list, replace, delete, every token
prefix, precedence over the standing authorization, and the three ways the credential
is dropped); the subject identity model (one opaque header, signed assertions verified
against a configured key with `401` on a bad signature or a stale `exp`, and subject
erasure through `DELETE /subjects/{subject}`); and a check that every path in
`openapi.yaml` is routed and that the reported `api_version` matches the spec.

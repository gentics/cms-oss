# GenAIx API v1 mock

A standalone Python service that implements every route of
[`../../openapi.yaml`](../../openapi.yaml) (`1.0.0-rc.5` draft, living plans) with in-memory state, and
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

## Identity: two caller modes

Since `1.0.0-rc.3` a request identifies its installation and its user in one of two ways,
told apart by one header.

| Mode | Headers | Mock behaviour |
|---|---|---|
| **Direct** (development, mocks, tooling) | `Authorization: Bearer sk_gnx_mock` and `X-GCMS-Subject: <opaque>` | The default. `X-GCMS-Subject` is the subject, verbatim. Missing or empty is `400` `subject_missing`. Only while no CMS key is configured: with `GENAIX_MOCK_CMS_PUBLIC_KEY` set, a direct request on a user route is `401` `identity_invalid`. |
| **Proxied** (production, the CMS custom proxy) | `X-GenAIx-Installation: sk_gnx_mock` and `Authorization: Bearer <CMS identity assertion>` | Needs `GENAIX_MOCK_CMS_PUBLIC_KEY`. The assertion is verified and the subject derived from its `sub`; `X-GCMS-Subject` is ignored. Without a configured key every proxied user-route request is `401` `identity_invalid`. |

The installation token is checked first, from `X-GenAIx-Installation` when that header is
present and from `Authorization` otherwise. An unknown token is `401`
`invalid_installation_token`; by default any token starting with `sk_gnx_` is accepted,
which is what the spec's mock server description promises, and
`GENAIX_MOCK_STRICT_TOKEN=1` accepts only `GENAIX_MOCK_TOKEN`.

**Verifying the assertion**, exactly as the contract states it:

- a compact JWS with `alg` `RS256`; any other algorithm is refused;
- the signature verifies against one of the configured public keys
  (`GENAIX_MOCK_CMS_PUBLIC_KEY`: a JWK as `GET /rest/admin/publicKey` serves it, a JWKS
  `{"keys": [...]}` or a JSON array of JWKs). A `kid` in the header selects the key with
  that `kid`; without one every key is tried, so a rotation is "add the new key, remove
  the old later";
- `iss` equals `GENAIX_MOCK_CMS_ISSUER` (default `Gentics CMS`), `sub` is a non-empty
  string, `iat` is present and within `GENAIX_MOCK_ASSERTION_MAX_SKEW` (default 60)
  seconds of the clock either way, and `exp` / `nbf` are honoured when present;
- `preferred_username`, `given_name`, `family_name`, `email` and `gcms_groups` are ignored.

Any failure is `401` `identity_invalid`; a proxied request without an assertion is `400`
`subject_missing`. The subject is `sub_` plus the first 16 bytes, hex, of
HMAC-SHA-256 over `<installation id> NUL <sub>` under `GENAIX_MOCK_SUBJECT_SECRET`: the
same login is the same subject from any device, another login another subject, and
nothing that names the person is kept. The assertion itself is never stored, logged,
echoed in a problem or put into an event.

To try proxied mode locally, save the JWK the CMS returns from `GET
/rest/admin/publicKey` (or any RSA public JWK whose private half you sign test
assertions with; the test suite generates its own pair) and start the mock with it:

```bash
GENAIX_MOCK_CMS_PUBLIC_KEY=$PWD/cms-key.jwk ./run.sh
curl -s -H 'X-GenAIx-Installation: sk_gnx_mock' -H "Authorization: Bearer $ASSERTION" \
  http://localhost:8080/api/v1/me
```

Sessions are partitioned per subject: another subject's session id is `403`
`session_forbidden`, an unknown one `404` `session_not_found`. No user id, login, display
name or group list is sent, stored or returned anywhere; `/me` reports `user: {subject}`
and nothing more. `X-GCMS-User-Id`, `X-GCMS-User-Login`, `X-GCMS-User-Groups`,
`X-GCMS-SID`, `X-GCMS-User-Token` and `X-GCMS-Session-Secret` do not exist in `v1` and are
ignored. The rc.2 HS256-signed `X-GCMS-Subject` and `GENAIX_MOCK_IDENTITY_KEY` are gone.

**Installation-level routes** (`GET /ping`, `DELETE /subjects/{subject}`,
`POST /subjects/erase`) take the installation token in either header and ignore any
assertion or subject. Both erasure routes erase every session, message, event, file,
connection and authorization of one subject and answer `204`, also for a subject that was
never seen. `POST /subjects/erase` takes exactly one of `{"cms_sub": "<the assertion's
sub>"}`, hashed like a proxied request's `sub`, or `{"subject": "..."}`; both or neither
is `422`, and `cms_sub` on an installation without a CMS key is `422` pointing at
`/cms_sub`.

**`PATCH` through the proxy.** The CMS custom proxy does not forward `PATCH`, so
`PATCH /sessions/{id}` and `PATCH /mcp/connections/{id}` also accept `POST` with
`X-HTTP-Method-Override: PATCH` (any case). Any other override value is `400`
`malformed_request`, and so is a bare `POST` to those two paths.

Every response carries `X-GenAIx-Request-Id`. Every error is an RFC 9457
`application/problem+json` document with a `genaix_code`, including unknown routes.

## Connect to a CMS first

GenAIx defines connector **types**; a user creates **connections** of a type, each with a
URL the client supplies, and registers a credential on the connection. `cms` is a
`multiple` connector, so one user can reach several CMS instances.

Every workflow module needs a mandatory `cms` connection, so a fresh user gets
`capabilities.workflows: []` and a plan's run that needs the CMS answers `auth.required`
followed by `run.failed`. The session agent answers without one. The first `GET /mcp/connections` (and
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
| the session is archived (`archive` or `DELETE /sessions/{id}`) | nothing; the list is empty |
| `DELETE .../authorizations/{connection_id}` | nothing |

Since `1.0.0-rc.4` plans closing never touch it: a published or discarded plan leaves the
credential to the next plan of the conversation. To see expiry without waiting, register a
token with an `expires_at` in the past. `GENAIX_MOCK_SESSION_AUTH_HOURS` (default 24) is the lifetime stamped on a
registration that sends none.

`Session.authorizations` and every `PlanState` carry the same list in short form
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

`workflow` is optional since `1.0.0-rc.4`. Without it the session starts with the session
agent, the normal start of a Workspace conversation. With it (a CMS context action such as
"Create page here") the first plan is drafted at once: `open_plan_id` names it, `drafting`
at version 0, and the first message goes to that plan's first run, which announces it with
`plan.updated`. A session has no `workflow` or `workflow_version` of its own any more.

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

A first message cannot carry a `plan` part either: a new session has no proposed plan to
accept, so it is `409` `plan_closed` (or `422` outside `execute` mode), and nothing is
created.

**Follow-ups and labels** (`1.0.0-rc.3`). `continued_from` names the earlier session a new
one follows up, typically research before an edit. It must be a session of the same
subject: an unknown id, a foreign one and a malformed one are all `422` pointing at
`/continued_from`, so the answer leaks nothing. The session returns it as
`continued_from` (`null` when unset) and it cannot be patched. The real GenAIx hands the
agent the earlier session's user messages, final answers and `cms_objects`; the mock
replays its transcript regardless. `GET /sessions?continued_from=<id>` lists the
follow-ups, and `GET /sessions?label=a&label=b` the sessions carrying every given label,
compared exactly. A `changeset:<uuid>` label still groups sessions if a client wants
that, but "publish all" no longer needs it (see below). Since `1.0.0-rc.4` follow-up work
in the same conversation is a further plan of the same session, not a new session, and
since `1.0.0-rc.5` a follow-up on the same work is a revision of its plan.

`GET /sessions` filters by `status` (the session's: `active`, `waiting_for_input`,
`archived`), by `workflow` (a session with any plan using that module) and by
`plan_status` (the open plan's status, or the latest plan's when none is open).

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
| `plan` | the items as a numbered list, `1. label` per item, separated by `\n` |

Parts are concatenated in order with nothing inserted between them, so the spacing
belongs to the `text` parts. A literal `[` or `]` in a label, a key, a value, a file name
or a text is escaped as `\[` and `\]`, which keeps the rendering readable back.

The input registry is closed. A part whose `type` is not one of the seven is `422`
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
`language` in the open plan's `derived_settings` (`GET /sessions/{id}/plans/{plan_id}`)
and lists them in
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

## Where a message goes (rc.4, living plans rc.5)

A session is one conversation; its work is a sequence of **plans**, each carried out by
one or more workflow modules (`Plan.workflows`, in run order; rc.5) with its own steps,
checks, gates, publications and lifecycle. Every run
belongs to a plan or to none, which `Run.plan_id` and `Run.mode` say:

| `mode` | Who runs | Plan |
|---|---|---|
| `session` | the session agent: answers, drafts a plan, or revises one (rc.5) | none |
| `side` | the session agent with read tools only, while a plan's run works | none |
| `propose` | a plan's proposing run, for a new plan or a revision: reads and plans, writes nothing | the plan |
| `execute` | a plan's run after the user accepted it, one per segment of one module's pending items (`Run.workflow`) | the plan |
| `transition` | the run of a plan's `publish` or `request_review` | the plan |

A user message goes, in this order:

1. `reply_to_interaction` naming a waiting run's question: the answer resumes that run and
   the message joins it; no run starts.
2. A `plan` part (`options.mode: execute`): while any run is active it is `409`
   `run_already_active` before anything else (rc.5). It accepts the plan it names, which
   must be a `proposed` plan of the session at its current `version` (`409`
   `plan_closed`, `plan_not_proposed`, `plan_outdated` otherwise). Without `items` the
   plan is accepted as proposed and keeps its version (rc.5, as S8 records it); with them
   as adjusted, version + 1. The plan becomes `accepted` (`accepted_version`,
   `accepted_by`, `accepted_at`) and its first execute run starts. The same part naming a
   `done` plan without `items` runs it again (`scripts/followup.sse`).
3. `options.mode: revise` (rc.5): a revision of `options.plan_id`, by default
   `Session.open_plan_id` (`422` at `/options/plan_id` with neither, `404`
   `plan_not_found`, `409` `plan_not_revisable` for a `discarded` or `superseded` plan).
   While that plan's `execute` or `propose` run works it is a **redirect** (below);
   otherwise the plan's proposing run starts at once, without a session-agent run.
4. A plan's run is active: a **side run** (`examples/s7-continuous-session.sse` run 5),
   with `Session.side_run_id`; a second one while the first is active is `409`. A message
   that redirects (rc.5: it starts with "stop", "halt" or "wait", or says "instead",
   "stattdessen" or `#redirect`) redirects the executing or proposing plan instead.
5. The session agent's own run or a side run is active: `409` `run_already_active`.
6. The current open plan is `drafting` (created with the session, or its proposing run
   failed): the plan's first run, or the revision's proposing run again. It is `accepted`
   because its execute run failed or was cancelled: the execute run for its pending
   items again. Both are the contract's "the next message retries".
7. Otherwise the **session agent**. A plain message never accepts a plan, even right after
   a proposal: it is answered, it drafts a new plan, or it revises one.

The mock's session agent decides deterministically (`genaix_mock/agent.py`):

- `#draft:<workflow id>` drafts a new plan under that module (`#new` likewise under the
  module the keywords pick); `#revise` revises the current plan;
- a message ending in `?` is a question and is answered;
- keywords, English and German, as S7 uses them, pick a module: "setze", "ändere",
  "update", "change", "edit", ... with at least one page reference (message or session
  context) `content_edit`; "erstelle", "create", "write", "news", "neue Seite"
  `content_create`; "construct" or "Baustein" `construct_create`; "create user" or
  "Benutzer anlegen" `admin_user`;
- with a revisable current plan (the open plan the conversation works on, else a
  `published` or `review_requested` one) the message **revises** it when it is about the
  same work: it names no page outside the plan, and its module is one of the plan's, or
  `content_edit` on a plan that created a page, or there is no module but a revision word
  ("make it", "mach", "add", "link", "upload", "lade", "instead", "lieber", "soll", ...).
  A new page for a plan that never created one is new work (S7: "Erstelle jetzt eine
  FAQ-Seite dazu" after the edit plan); "Mach daraus lieber eine News-Meldung" revises
  the proposed FAQ plan (S7), "Mach sie bunter." the done PNDemo plan (S8);
- otherwise a module drafts a new plan, and anything else is answered:
  `scripts/session-agent.sse` the first time, then `scripts/followup.sse`;
- `#ask` makes it ask the user first (`ask_user`, `plan_id: null`), so its run waits.

Drafting is one `session` run with a `draft_plan` tool call (`input_summary`
`draft_plan workflows=[<module>] ...`), `plan.updated` with `drafting` at version 0 and
`session.updated`; the plan's first run follows on the same message, so the stream shows
S7's runs 2 and 3. Several plans may be open (rc.5, `Session.open_plan_ids`; there is no
`plan_policy.concurrency` any more), so drafting never waits for another plan; the new
plan becomes `open_plan_id`. It supersedes the open unaccepted proposal only when the
message says so (`#supersede`).

Revising is one `session` run with a `revise_plan` tool call, `plan.updated` with the plan
back at `drafting` (same version, its items kept, `closed_at` cleared for a settled plan)
and `session.updated`; the plan's proposing run follows on the same message (S7 runs 9 and
10, S8 runs 4 and 5). See [Living plans](#living-plans-rc5).

**A plan's first run** depends on the module, as `08-workflow-modules.md` §6.5 lays out:

| Module | First run | Acceptance |
|---|---|---|
| `plan_approval` (`content_create`, `content_edit`) | `propose`: `s1…@plan` or `s6…#1`; ends with the plan `proposed` and nothing written | the user's `plan` part |
| a write tool, no `plan_approval` (`construct_create`, `admin_user`) | `execute`: S3 or S4; the plan is `proposed` until the run's `confirm` interaction | `approved: true` on that `confirm` makes it `accepted`, then `running`; anything else accepts nothing and the run is cancelled before its first write, the plan staying `proposed` |
| no write tool (`content_research`) | `execute`: S2; `drafting` to `running` at once | none, reading is free |

`options.mode: plan_only` makes any plan's first run a proposing run: S1 or S6's, or a plan
built from the module's `steps_template` with nothing read or written.

**The execute run** after a `plan` part replays `s1…@execute` or `s6…#2` for the two
`plan_approval` modules and the module's whole transcript otherwise. It starts with the
mock's own `plan.updated` (`running`, the plan as the user sent it: the same version when
accepted as proposed, version + 1 when adjusted) and ends with `done`. Since rc.5 it
carries out one segment of pending items of one module (`Run.workflow`); the next
segment's run follows on its own when it completes, and the plan is `done` after the last.

A marker in the message picks the transcript of the **execute** run the message starts (the
accepting message, a retry, or the first message of a module without a proposal); on a
proposing run or a session-agent turn it has no effect. A marker is looked for in the
rendered `content`, so it works in a `text` or a `filter` part as well.

| Marker | Transcript | What it produces |
|---|---|---|
| `#fail` | `examples/s5-failure-and-recovery.sse#1` | A page locked by another user, a recoverable `error`, an artifact, then `auth.required` and `run.failed`; the plan stays `accepted` |
| `#recover` | `examples/s5-failure-and-recovery.sse#2` | `artifact.updated`, then `run.cancelled` because the user pressed Stop |
| `#gatefail` | `scripts/gate-failure.sse` | A blocking gate check that failed plus a non-blocking warning, so the `409` refusal of a plan transition can be exercised |
| `#gateack` | `scripts/gate-nonblocking.sse` | One non-blocking gate check fails; transitions answer `409` `quality_gate_failed` with empty `failing_gates` until the client repeats with `acknowledge_checks` |

S5 writes page 8903, which S6's proposal does not name: add it to the plan when accepting
(an item with `target` page 8903), or the write is refused as outside the plan.

| Script | What it produces |
|---|---|
| `examples/s1-content-create.sse@plan` | Reading, a `settings_review` interaction, the search, the plan, a closing text part, `run.completed`; no CMS write |
| `examples/s1-content-create.sse@execute` | The mock's `plan.updated` with the accepted plan, then CMS writes, the page's `publications.updated`, a streamed `page_structure` part, a verbatim proof, previews, an artifact, the final plan |
| `examples/s6-content-edit.sse#1` (`content_edit`) | Reading three pages, their versions and the permissions of both folders, one plan with an item per change (`target` = the page), no CMS write |
| `examples/s6-content-edit.sse#2` | The recorded opening `plan.updated` replaced by the live one. A write per page, each followed by `publications.updated` (`pending`), `approval_chain.updated` after the first, one `st-3` substep per page, three passing step_end gate checks, a `preview.updated` per page, the final plan |
| `examples/s2-content-research.sse` (`content_research`) | Search calls, a streamed `table` part, citations, no CMS writes |
| `examples/s3-construct-create.sse` (`construct_create`) | Catalogue check, a streamed `construct_draft` part, Handlebars validation, the `confirm` that accepts the plan, an `api_call_log` part, preview |
| `examples/s4-admin-user.sse` (`admin_user`) | Group lookup, a `form` interaction, permission impact simulation, the `confirm` that accepts the plan, user creation |
| `examples/s7-continuous-session.sse#5` | The side run: one read tool call and a short answer |
| `scripts/session-agent.sse` | The session agent's first answer |
| `scripts/followup.sse` | A status line, one text part, `run.completed`: later answers, and a done plan run again |
| generated `draft_plan` run | One `draft_plan` call and the plan's `plan.updated` |
| generated (`publish` / `request_review`) | One `publish_page` call per pending or failed page, `publications.updated` after each, `approval_chain.updated`, a summary text part; see [Publishing every page](#publishing-every-page) |

The markers exist because the happy-path scenarios do not reach the unhappy events, and a
client still has to render them. Between the markers, a real cancellation and a run with
no MCP authorization, every event type of the contract is reachable.

**Plan-scoped events.** A plan's run stamps the plan's `plan_id` on every `plan.updated`,
`step.updated`, `check.updated`, `derived_settings.updated`, `approval_chain.updated`,
`preview.updated` and `publications.updated`, on `Check`, `GateResult` and `Publication`,
and on `interaction.requested` (`null` for the session agent's question). A run without a
plan carries none of those events: a transcript frame of that kind is dropped from a
session-agent or side run. The plan's status is the mock's bookkeeping, never the
transcript's: the lifecycle frames a recording carries (`drafting`, `done`, `released`,
...) are emitted when the live plan gets there. A failed or cancelled run leaves its plan
where it was before the run.

**Writes outside the plan.** In an execute run whose plan names page targets, a write to a
page that is neither a target nor created by this plan is refused before it reaches the
CMS: `tool.failed` with a `422` `plan_scope_violation`, then `error` with
`recoverable: true`, and no publication for that page (S7's run 4). A plan without page
targets (S3, S4) is not judged by page.

Replaying rewrites `session_id`, `run_id`, `message_id`, interaction ids and session file
ids to the live ones, re-stamps `seq` and `ts` through the session's event bus, stamps
`plan.version` per plan (0 while drafting, 1 for the first proposal, +1 for every content
change, unchanged by a status change), writes artifacts to disk, materialises the final
assistant message into the history and blocks on `interaction.requested` until the answer
arrives. `PlanState.state` is derived as the contract's table says.

`seq` is a total order per session starting at 1, gapless, never reused. Heartbeats
consume a `seq` and are persisted, as the contract requires. One stream carries every run
of the session interleaved; with `follow=false` it ends once no run of the session is
active, so a draft's follow-up run and a side run are part of it.

## Living plans (rc.5)

A follow-up on the work of a plan **revises** that plan instead of opening another. Every
plan but a `discarded` or `superseded` one is revisable: `proposed`, `done`, `released`,
`published` and `review_requested` alike (rc.4's `reopen` is gone).

- **Revision.** The plan goes back to `drafting` at its current version, items kept
  (`closed_at` cleared for a settled plan; it becomes `open_plan_id` again), then its
  proposing run ends it `proposed` at `version + 1` with `reason: Revised: ...` and
  `PlanVersion.kind: revision`. Items finished earlier stay `done` and `unchanged`; a new
  item has `change: added` and the new `version`; an item the revision rewords is
  `changed`; an accepted item it drops is listed with status and change `removed` until the
  version is accepted. `change` is stated against the version accepted before the current
  one (every item is `added` while the plan was never accepted). The user accepts with a
  `plan` part as usual, and only the `pending` items run.
- **Segments.** An accepted plan runs one execute run per contiguous segment of pending
  items of one module, in item order, `Run.workflow` naming the module; the next segment
  follows when one completes, without another acceptance. A module an item names joins
  `Plan.workflows` (with its steps and gates) in the order its first item runs.
- **Redirect.** A message while the plan's `execute` (or a drafting plan's `propose`) run
  works that redirects it (`options.mode: revise`, or the words above) cancels that run:
  it finishes the tool call it is in, ends `run.cancelled` with `cancel_reason:
  redirected` (an unfinished step `skipped`, the item whose write completed `done`, the
  item it was working on `pending` again), then the plan's proposing run starts with the
  message (`drafting`, then `proposed` at `version + 1`); the `202` names that proposing
  run. A recorded run that ends redirected (S8 run 9) holds still at its end until the live
  redirect arrives, or completes after the interaction timeout as if nobody had.
- **Publications** accumulate over the versions: a page written again is `pending` again,
  and `publish` handles the pages of every version.

**Generated or recorded.** Outside a scenario, the revision's session run (`revise_plan`)
and its proposing run are generated by the mock (`scripts_loader.revise_tool_script`,
`revision_script`; the items from `plans.revision_items`), and its segments replay the
modules' transcripts. `#s8` in the message that drafts a plan binds that plan to
`examples/s8-living-plan.sse`: the transcript is registered per run like any multi-run
transcript (`s8-living-plan.sse#1` to `#12`), indexed by mode in
`scripts_loader.SCENARIO_RUNS`, and each of the plan's runs takes the transcript's next
run of its mode (session runs 1, 4, 7; proposing runs 2, 5, 8, 10; execute runs 3, 6, 9,
11), generic behaviour once they run out. A recorded revision brings its items; items the
live plan already finished stay as they are. Played with the messages S8 describes (see
`tests/test_rc5.py::test_s8_living_plan_replays`), the live session has S8's runs,
modules, plan versions, items and changes. Without the transcript `#s8` does nothing.

## Plans, several pages, follow-ups and publications

What the Workspace prototype worked around on rc.2 (David Barbu's `mock-workarounds`) is
contract since `1.0.0-rc.3`, per plan since `1.0.0-rc.4`, and the mock implements the
contract form.

### Plans and their routes

`GET /sessions/{id}/plans` lists every plan of the session, oldest first, as
`PlanSummary` (`item_count`, `publication_counts`, `accepted_at`, `closed_at`).
`GET /sessions/{id}/plans/{plan_id}` is the `PlanState`: the plan, its steps, checks,
gates, approval chain, publications, previews, derived settings, objects, inputs, the
pending interaction and the session's authorizations; an unknown plan id is `404`
`plan_not_found`. `GET /sessions/{id}/workflow` is deprecated and returns the open plan's
`PlanState`, else the latest plan's, else `404` `plan_not_found`.

`Session` carries `open_plan_id` (the newest plan from `drafting` to `released`),
`executing_plan_id` (the plan `accepted` or `running`), `plan_count` and `side_run_id`;
`session.updated` announces them as they change.

### The plan part

A `plan_approval` module's proposing run ends at the plan: the plan is `proposed`, its
state `awaiting_feedback`, nothing is written, and no `confirm` interaction is raised. The
client shows the plan card, lets the user edit, add, delete and reorder steps, and sends
one `execute` message with a typed `plan` part naming the plan:

```jsonc
{"options": {"mode": "execute"}, "parts": [
  {"type": "text", "text": "Bitte so umsetzen:\n"},
  {"type": "plan", "plan": {"plan_id": "<open plan id>", "based_on_version": 1, "items": [
    {"id": "p1", "label": "Versand und Lieferung: Grenze von 35 auf 50 Euro aendern"},
    {"id": "p3", "label": "Garantiebedingungen: Verweis ergaenzen"},
    {"label": "FAQ ergaenzen", "kind": "create",
     "target": {"type": "page", "id": "8871", "node_id": 3, "label": "Garantiebedingungen"}}
  ]}}
]}
```

- At most one per message and only with `options.mode: execute`, else `422` pointing at
  `/parts/<i>`. `plan_id` is required; `items`, when sent, 1 to 100, `label` 1 to 500
  characters, unknown members refused.
- `plan_id` must name the session's open `proposed` plan: a closed plan or one of
  another session is `409` `plan_closed`, a plan `drafting`, `accepted`, `running` or
  `released` `409` `plan_not_proposed`; a `done` plan is accepted only without `items`,
  which runs it again. `based_on_version` must be that plan's `version`, else `409`
  `plan_outdated`. A refused message is not stored.
- An item whose `id` is a current item keeps it, with its `kind`, `target`, `workflow`
  and `depends_on` unless the item sets them; an item without an id, or with an unknown
  one, is a new item with a mock-assigned id `u<n>` (`kind: update` when it targets a
  page, `other` otherwise, unless set; without `workflow` the module of the item before
  it); a current item not listed is removed. rc.5: a `done` item is kept whether listed
  or not, and changing its `label`, `kind`, `target` or `workflow` is `422` at
  `/parts/<i>/plan/items/<j>`; an item listed before one it `depends_on` (or depending on
  one the part removed) is `422` with `code: dependency_order`; listing a `removed` item
  of a revision brings it back.
- The execute run starts with `plan.updated` holding exactly that plan, `running`. Accepted
  as proposed (no `items`, or the same items) the plan keeps its version and its reason,
  and that version's `PlanVersion` gets `accepted_at`; adjusted it is version current+1
  with `reason` `Adjusted by the user: N kept, N added, N removed.` (`kind: adjustment`).
- `content` renders the part as a numbered list, `1. label` per item.

The transcript's later `plan.updated` is reported on the user's plan: a step the
transcript knows takes its status, detail and target; a step it does not know (added by
the user, or kept although the recording dropped it) is reported `done`; a removed step
stays removed. `#planmerge` anywhere in the message makes the mock merge steps 2 and 3 and
say so in `reason`, the case a UI has to show when the agent deviates from the plan it
was sent. The numbered-list convention of rc.2 is gone: a numbered list in a `text` part
is just text, and since rc.4 a plain `execute` message is no acceptance either.
`options.mode: plan_only` is honoured for every workflow.

### content_edit: several pages

`content_edit` 1.0.0 is an official module (`optional: false`, `scenario: null`). Its
pages are the `type: page` entries of `context.references`, 1 to 20; a later message may
add pages with `reference` parts; they become the plan's `inputs.references`. A
`content_edit` session created with more than 20 page references, or with a first message
and no page at all, is `422` at `/context/references`; the first message of a
`content_edit` plan that still has no page, and `#draft:content_edit` without one, are
`422` at `/parts`. `derived_settings` stays `null`: every write targets an existing page whose
node, folder, template and language are fixed. Gates: `constructs_allowed`,
`references_exist`, `alt_text` (step_end `st-3`), `base_version_current` (blocking,
before_release) and `release_self_check`; no `draft_unpublished`.

The mock replays S6 as recorded: whatever pages the session names, the run edits 8644
*Versand und Lieferung*, 8871 *Garantiebedingungen* (folder 42 *Richtlinien*) and 9011
*Serviceinformationen* (folder 64 *Publikationen*), all of which S2's research table lists,
except a page the accepted plan no longer names: that write is refused as outside the
plan. The rc.2 rewriting of a transcript onto the referenced page is gone.

```bash
curl -s "${H[@]}" "${J[@]}" -d '{
  "workflow": "content_edit",
  "title": "Versandkostenfrei-Grenze auf drei Seiten anpassen",
  "continued_from": "'"$RESEARCH_SID"'",
  "context": {"node_id": 3, "language": "de", "references": [
    {"type": "page", "id": "8644", "node_id": 3, "label": "Versand und Lieferung"},
    {"type": "page", "id": "8871", "node_id": 3, "label": "Garantiebedingungen"},
    {"type": "page", "id": "9011", "node_id": 3, "label": "Serviceinformationen"}]},
  "message": {"content": "Ersetze die alte Versandkostenfrei-Grenze durch 50 Euro."}
}' "$B/sessions"
```

### Publishing every page

A plan publishes **every page it created or changed**, and never a page another plan of the
session wrote. `PlanState.publications` holds one `Publication` per such page, in
first-write order: `{cms_ref, status, approval_chain, plan_id, publish_at?, at?, error?}`. The mock adds the entry, `pending`, when a
replayed tool call first writes the page (`created` or `updated`), and reports the whole
array as `publications.updated`; a page written again after it went out is `pending`
again. The plan-level `approval_chain` is the first entry's chain: a chain the run
reported before its first write (S1) is taken over, otherwise the derived one is reported
as `approval_chain.updated`. `PlanState.previews` holds the latest preview per page, in
publications order; `preview` stays the most recent one.

Plan transitions are `POST /sessions/{id}/plans/{plan_id}/transitions`:

| action | from | to | run |
|---|---|---|---|
| `release` | `done`, or `running` (its run is cancelled after the gates passed) | `released` | no |
| `publish` | `released` | `published` or `review_requested` | yes, `202` with `{run_id, plan, session}` |
| `request_review` | `released` | `review_requested` | yes |
| `discard` | any open status (an active run of the plan is cancelled) | `discarded` | no; `on_discard: restore` is not mocked, no module sets it |

`reopen` is gone (rc.5, `422`): a follow-up message revises a settled plan instead. A
transition on a closed plan is `409` `plan_closed`, an action the
status does not allow `409` `workflow_transition_invalid`, a repeated `discard` `200` with
no event. The session's own route, `POST /sessions/{id}/transitions`, takes `archive` and
`unarchive` only; a plan action sent there is `409` `workflow_transition_invalid` naming
the plan route. Archiving cancels every active run, drops the session credentials and
keeps the plans' statuses; an archived session refuses messages (`409`
`session_archived`) and patches until it is unarchived. The session itself stays `active`
through all plan transitions.

`publish` and `request_review` start a generated run (`mode: transition`) that handles
every entry that is `pending` or `failed`, in order, one `publish_page` call each, and
reports the whole array after each page. Each page goes direct or to review by its own
folder:

- direct when the caller may publish (the subject is in `fixtures/publishers.json`, or
  the session holds an authorization whose token starts with `publisher_`) **and** the
  page's folder is not marked `publish: review` in `fixtures/pages.json`;
- otherwise to review, with the folder's `approvers` group as the pending second step of
  the chain; `request_review` sends every page to review.

The plan ends `published` when every entry is `published`, `review_requested` when at
least one is `review_requested` and none `failed`. Any `failed` entry fails the run
(`run.failed`, `cms_request_failed`), the plan stays `released`, the pages that went
out keep their status, and a repeated `publish` retries only `pending` and `failed`.
`at` on the transition becomes every handled entry's `publish_at`. To reach a failure,
put `#publishfail:<page id>` into the transition's `comment` (a bare `#publishfail`
fails the last page): that page fails in this attempt with `cms_object_locked`.

As `sub_publisher`, the S6 plan ends with 8644 and 8871 `published`, 9011
`review_requested` and the plan `review_requested`, which is S6's recorded outcome.
`fixtures/pages.json` holds the pages of the S1, S2, S6 and S8 transcripts with their folder;
ids, names and folders are the transcripts' own, the URLs, the id of the `Publikationen`
folder (64) and of the `Newsmeldung` template (19) and the approver groups of folders 42
and 57 are invented for the mock.

## Workflow modules and quality gates

`GET /workflows` reports the five modules exactly as the contract's example does,
`content_edit` included; the session agent is no module and `free_chat` is gone (rc.4):
`version`, `plan_approval` (`true` for `content_create` and `content_edit`; the others
leave it out, which means `false`), `plan_policy` (`content_edit`: `on_discard: keep`;
absent means `keep`; rc.5 removed `concurrency`), `inputs_schema`,
`required_connectors` as `{type, optional}` objects,
`tool_groups` as an allowlist keyed by connector type plus the reserved `genaix` key,
`quality_gates` (each with `triggers`, the full trigger list, next to the first trigger in
`when` / `step_id`), `steps_template` with `substeps` or a `description`, plus the
per-user `available` and `reason`. A plan carries `workflows` (`{id, version}` in run
order, rc.5), its `PlanState` the `inputs` keyed by module id (the context members the
module's `inputs_schema` names, for `content_edit` the pages); a session carries `inputs`
(those of the first plan, flat, empty without `workflow`), `authorizations` and
`continued_from`.

`GET /sessions/{id}/plans/{plan_id}` adds `gates`, one `GateResult` per declared gate of
each of the plan's modules, module by module in `workflows` order, each with the plan's
`plan_id` and its `workflow`; attempts count per plan. Steps are the modules'
`steps_template`s in plan order, every step and check id is `<workflow>/<id>`
(`content_edit/st-3`, `content_create/chk-alt-text`, whatever a transcript says), a step of
no module `plan/<id>` without `workflow` (the publish run's `plan/st-publish`). The mock
runs no gates: a verdict arrives as a `check.updated` event carrying `gate_id`, and the
replay engine folds it into the matching gate record, mapping severity `pass` or `info` to
verdict `pass`, `warn` to `warn` and `fail` to `fail`. A declared gate that no transcript
reports stays `pending` with `attempt: 0`. That includes every `before_release` gate:
the mock runs nothing at a release, so `base_version_current` stays `pending` (S6 shows
what the real verdict looks like).

`release`, `publish` and `request_review` are refused with `409` `quality_gate_failed`
while a blocking gate-sourced check has severity `fail`, or a non-blocking one has
severity `warn` or `fail` and has not been acknowledged (`1.0.0-rc.2`: a warning never
silently disappears, so the S1 run's `alt_text` warning has to be acknowledged before the
first release). The problem carries `failing_checks` (every such check, blocking first),
`failing_gates` (the full `GateResult` of each blocking one) and `acknowledgeable_checks`.
Blocking checks cannot be waived; listing a non-blocking check in `acknowledge_checks`
records `acknowledged_by` and `acknowledged_at` on it and takes it off the list. An action
that is illegal from the current status is `409 workflow_transition_invalid` before any
gate is consulted, a repeated `discard` is `200` with no event, and `discard` and the
session's `archive` are never gated. `acknowledge_checks` takes the qualified ids
(`content_create/chk-alt-text`). `tool.started.tool` on every replayed stream is the bare
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
`interaction.id` the stream reported, confirming every key it raised. The plan's
proposing run then proposes the plan and ends its turn without writing. Accept the plan
with a `plan` part naming it, then release (acknowledging the `alt_text` warning the draft
leaves) and publish it:

```bash
curl -sX POST "${H[@]}" "${J[@]}" -d '{"answer":{"settings":[
  {"type":"setting","key":"folder","value":"42","label":"Richtlinien"},
  {"type":"setting","key":"language","value":"de","label":"Deutsch"},
  {"type":"setting","key":"template","value":"17","label":"Kampagnen-Landingpage"}
]}}' "$B/sessions/$SID/interactions/$IID"
PID=$(curl -s "${H[@]}" "$B/sessions/$SID" | python3 -c 'import json,sys; print(json.load(sys.stdin)["open_plan_id"])')
curl -sX POST "${H[@]}" "${J[@]}" -d "{\"options\":{\"mode\":\"execute\"},\"parts\":[
  {\"type\":\"text\",\"text\":\"Passt so, bitte umsetzen.\\n\"},
  {\"type\":\"plan\",\"plan\":{\"plan_id\":\"$PID\",\"based_on_version\":1}}
]}" "$B/sessions/$SID/messages"
curl -sX POST "${H[@]}" "${J[@]}" -d '{"action":"release","acknowledge_checks":["content_create/chk-alt-text"]}' \
  "$B/sessions/$SID/plans/$PID/transitions"
curl -sX POST "${H[@]}" "${J[@]}" -d '{"action":"publish"}' "$B/sessions/$SID/plans/$PID/transitions"
```

Without `workflow` on `POST /sessions`, the same conversation starts with the session
agent: "Erstelle eine Landingpage ..." drafts the `content_create` plan, and its proposing
run follows on the same message.

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
| `GENAIX_MOCK_INSTALLATION_ID` | `7c1f0a52-…-2f5e8c1b40d9` | Reported by `/ping` and `/me`; part of the subject derivation |
| `GENAIX_MOCK_CMS_PUBLIC_KEY` | unset | Path to the CMS public key(s): a JWK, a JWKS or a JSON array of JWKs. Set, the installation takes proxied requests and refuses direct ones; unset, direct mode only. |
| `GENAIX_MOCK_CMS_ISSUER` | `Gentics CMS` | The `iss` the assertion must carry |
| `GENAIX_MOCK_ASSERTION_MAX_SKEW` | `60` | Seconds the assertion's `iat` may differ from the clock, either way |
| `GENAIX_MOCK_SUBJECT_SECRET` | `genaix-mock-subject-secret` | The secret the subject is derived from `sub` under (HMAC-SHA-256). Changing it gives every login a new subject. |
| `GENAIX_MOCK_FILE_LINK_SECRET` | `genaix-mock-file-link-secret` | The secret file links (`GET /file-links/{token}`, `1.0.0-rc.4`) are HMAC-signed under. |
| `GENAIX_MOCK_FILE_LINK_TTL` | `300` | Lifetime of a file link in seconds. |
| `GENAIX_MOCK_PUBLISHERS` | `fixtures/publishers.json` | Subjects who may publish where their folder allows it. |
| `GENAIX_MOCK_PAGES` | `fixtures/pages.json` | Pages and their folders; per folder whether anybody may publish there (`publish: direct` or `review`) and the approving group. |
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
- Both caller modes: direct `X-GCMS-Subject`, and the proxied RS256 CMS assertion with
  every verification rule, key selection by `kid` or trial, the keyed subject derivation
  and the error codes; both erasure routes; `X-HTTP-Method-Override: PATCH`.
- The two state machines: the session (`active`, `waiting_for_input`, `archived`,
  `archive` and `unarchive`) and the living plan of rc.5 (`drafting` to `published` or
  `review_requested`, `discarded`, `superseded`; every plan but a final one revisable,
  version by version), with `plan_closed`, `plan_not_proposed`, `plan_not_revisable`,
  `plan_not_found` and `session_archived`; `release` cancelling the
  plan's active run, `publish` and `request_review` handling every page of the plan with
  its own outcome, the plan status rule over `publications`, the retry of failed pages,
  and the quality gate refusal with its acknowledgement rules, all per plan.
- Run modes and side runs: a message while a plan's run works is a read-only side run,
  `409` `run_already_active` while the session agent's own run or a side run works,
  acceptance never beside a running plan, retries of a failed run.
- Acceptance only by the user: a `plan` part (with all its validation), or the one
  `confirm` of a module without `plan_approval` that writes; a module without a write
  tool runs at once. Several open plans, one executing at a time, revisions (session
  agent or `options.mode: revise`), redirects at a tool boundary, segments per module,
  superseding on request, `plan_scope_violation` on a write outside the plan, and
  `options.mode: plan_only` for every workflow.
- `plan.updated` as a whole plan every time, with `version`, `versions` (`PlanVersion`
  with `kind`) and per item `workflow`, `version`, `change` and `depends_on`; run events
  of a plan's run carry its `plan_id` in the envelope.
- `continued_from` (ownership check, immutability, filter) and the exact `label` filter.
- One active run per session, cancellation, and the interaction round trip with `409` on
  a double answer and `410` after expiry.
- The settings review: the answer validated against the keys it raised and the options it
  offered, the confirmed settings stored as the user's own message, and the review skipped
  when chips or the session context already answered it.
- The per-session filesystem, with real bytes, real sha256, real downloads.
- File links (`1.0.0-rc.4`): `GET /file-links/{token}` serves a session file without credentials
  for a valid, unexpired token and answers `404 file_not_found` otherwise. The mock has no agent that
  uploads into a CMS, so nothing mints a link on its own; `genaix_mock.routes.files.mint_file_link`
  mints one for tests.
- Problem documents, with the `genaix_code` the contract names for each case.

Mocked:

- No model. Assistant turns are recorded transcripts, so the answers do not respond to
  what you actually typed. The session agent drafts a plan from a marker or a few
  keywords, not from understanding; a plan's runs replay its module's transcripts. The
  one thing read from a message beyond that is its `plan` part.
- No MCP server and no CMS. Tool events are replayed, CMS object ids are the ones from the
  transcripts (S1: page 9142; S6: pages 8644, 8871, 9011; S8: page 9207, file 9311), and preview URLs point at
  `cms_base_url` and will not load. Who may publish where comes from the two fixtures,
  not from CMS permissions; a page failing to publish only happens on `#publishfail`.
- Authorization is never verified against anything: the status follows the token prefix.
  The reachability probe contacts nothing either, so `status.reachable` is true for every
  URL except a host starting with `unreachable`, and `server_info` is a fixed stub.
- No gate runner. Gate verdicts come from the transcript's check events, so a gate no
  transcript reports stays `pending`, and a `before_release` gate never runs
  (`base_version_current` never refuses a release). The contract
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
- The plan decides the plan card and the pages written, not the work. An execute run
  replays its transcript's steps; a write to a page the user removed is refused
  (`plan_scope_violation`), but a step the user added is reported `done` without a tool
  call behind it.
- `content_edit` always edits S6's three pages, whatever the session references; the
  references are validated (1 to 20 pages) but not followed. A page outside
  `fixtures/pages.json` publishes direct for a publisher, since its folder is unknown.
- `continued_from` is stored, checked and filterable, but the agent gets nothing from the
  earlier session: the transcript is the same either way. The same holds for the plans of
  the same session: there is no ledger, recall or compaction behind the session agent, so
  `status` `context_compacted` never appears.
- A second plan of the same module replays the same transcript as the first, S1's page
  creation included, so its pages are the first plan's pages again. The same goes for a
  revision's segments outside the S8 scenario: a `content_create` segment replays S1's
  execute half, a `content_edit` one S6's run 2.
- A generated revision reads its instruction by keywords only (`plans.revision_items`):
  an upload adds a `content_create` item, a link a `content_edit` `link` item depending
  on the page and the upload (or changes the pending link item), anything else changes the
  pending `update` item or adds a `content_edit` `update` item on the plan's page. A
  revision does not reset the `before_release` gates in the mock, since it runs none.
- The router's redirect detection is a handful of words (see above); a client that wants
  a redirect for sure sends `options.mode: revise`.
- `on_discard: restore` is not mocked (no module sets it): a discard always keeps the pages.
- The session agent answers `plan_only` like any message; only a plan's first run turns
  into a proposing run.
- `inputs_schema` is reported but not enforced beyond the page count of `content_edit`;
  only `content_create` and `content_edit` declare one, so only they derive inputs.
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

101 tests (`tests/test_api.py`, `tests/test_rc5.py`). They start a real uvicorn server on an ephemeral port, because in-process ASGI
transports buffer responses and the SSE behaviour is the point; proxied mode runs against a
second instance configured with two RS256 public keys the fixture generates. Covered: a
full lifecycle per scenario (create, upload, message, consume the stream to
`run.completed`) asserting event order, `seq` monotonicity, that every event validates
against `../../schemas/events.schema.json` and every stored assistant part against
`../../schemas/parts.schema.json` and every stored user part against
`../../schemas/user-parts.schema.json`, that the history carries the streamed parts and that
the gates match the module's declaration; every example transcript, S6 included, validated
frame by frame; every REST response validated against its `components.schemas` entry in
`openapi.yaml`; the connection lifecycle (auto-create, the URL rules, re-pointing dropping
the credential, default promotion, the cap) and the authorization lifecycle through all
five statuses; a run that fails for want of an authorized connection and succeeds after
one is registered; a session selecting a non-default connection through
`context.connection_ids`; the blocking quality gate refusal and its acknowledgement rules;
the auth matrix; `403`, `404`, `409`, `410`, `413` and `422`; the interaction round trip and
its expiry; cancellation; heartbeats; replay and filters; the three marker transcripts; the
user message parts (the rendering table and its escaping, the derived `references` and
`files`, the closed input registry answering `422` with a JSON Pointer, the verbatim
declarations on an upload and on a part, and `setting` parts outranking the transcript's
`derived_settings`); the settings review (the shape of `proposed` and `missing`, an answer
that misses a key or leaves the offered options, the confirmed settings stored as a user
message, answering through `reply_to_interaction`, and the review being skipped when chips
or the session context already cover it); creating a session with its first message in
one call, the derived read-only `intent`, the two-call flow and the members a first message
cannot carry; the session-scoped authorizations (register, list, replace, delete, every
token prefix, precedence over the standing authorization, and the three ways the
credential is dropped); identity (direct mode, the proxied assertion with a derived,
stable, non-identifying subject across keys of a rotation, every refusal rule as `401`
`identity_invalid` or `400` `subject_missing`, a direct request to a keyed installation, a
proxied one to an unkeyed installation, the installation-level routes taking the token in
either header, erasure by `DELETE`, by `subject` and by `cms_sub`, and the method
override); and rc.3 (`plan_approval` in the catalogue, `content_edit` 1.0.0, `plan_only` for
every workflow, the `plan` part keeping, adding, removing, reordering, relabelling and
reclassifying steps, its validation and `plan_outdated`, accepting unchanged and
`#planmerge`, `content_edit` over S6's three pages with the run shaped like S6's run 2,
pending publications in first-write order, `previews` and `derived_settings: null`, the
1-to-20 page rule, `continued_from` and the label filter, "publish all" ending two
published and one in review, a forced failure retried for that page only, `request_review`
and a non-publisher sending every page to review, S1's single publication with
`publish_at`); rc.4 (sessions with and without a first plan, the plan routes and
`PlanState`, run modes and `plan_id` on every plan-scoped event, acceptance only by a
`plan` part or a module's one confirmation, a plain message going to the session agent,
side runs, the session agent's own question blocking messages, a done plan run again,
retries of a failed run, plan and session transitions with their refusals, credentials
outliving plans, and S7 played live with its run modes and its revision); rc.5 in
`tests/test_rc5.py` (done items kept and never changed, `dependency_order`, removed items
coming back, `change` and `version` per item, segments, the session agent telling a
revision from new work, `options.mode: revise` and its `422`/`404`/`409`, a done plan
revised twice across two modules with one execute run per segment, a redirect of an
execute run and of a proposing run, several open plans and superseding on request, and
S8 replayed live against `examples/s8-living-plan.sse`: the same runs, modules, cancel
reasons, plan versions, items and changes, skipped while the transcript is missing); and
a check that every path in `openapi.yaml` is routed and that the reported `api_version`
matches the spec.

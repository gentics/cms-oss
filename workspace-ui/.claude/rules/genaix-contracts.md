# GenAIx and CMS contracts (version 1.0.0-rc.5)

`1.0.0-rc.5` is agreed by all parties (2026-10-15) and frozen for development, together with its addition of the same day ("plans at a glance", below).

## Where things are
- `.claude/contracts/openapi.yaml`: the GenAIx API. The source of truth for every route, request, SSE event and message part the UI handles.
- `.claude/contracts/mcp-tools.json`: the CMS MCP tool specification. Background only; see below.
- `.claude/contracts/genaix-docs/INDEX.md`: map of the prose documents. Start here.
- What changed since rc.4: `03-genaix-api.md` §16.1, `09-integration-guide.md` §10, and the `changelog` in both contract files (authoritative).
- The rc.5 addition (2026-10-15): `03-genaix-api.md` §8 ("A plan at a glance", findings before acceptance), §9 (a `choice` like the settings review), §16.1 ("Addition of 2026-10-15"); `09-integration-guide.md` "The plan card" ("The plan at a glance"); the `1.0.0-rc.5 addition` entry of the `openapi.yaml` changelog.

## Rules
- Where a prose document and a contract file disagree, the contract file wins.
- The UI never calls GenAIx directly: every call goes through the CMS custom proxy `/rest/proxy/genaix` (`01-architecture.md`, "System context"). The UI never calls the CMS MCP; GenAIx does.
- Two caller modes (`02-auth-and-context-flow.md`; `09-integration-guide.md` §3), told apart by `X-GenAIx-Installation`:
  - proxied (production): the CMS proxy adds `X-GenAIx-Installation: <installation token>` and `Authorization: Bearer <RS256 CMS identity assertion>`.
  - direct (development, mocks): `Authorization: Bearer <installation token>` plus `X-GCMS-Subject`, only against an installation without a CMS key. A dev proxy in direct mode clears `X-GenAIx-Installation`, otherwise a request switches to proxied mode.
- The CMS proxy forwards no `PATCH`: send `PATCH` routes as `POST` with `X-HTTP-Method-Override: PATCH` (`PATCH /sessions/{session_id}`, `PATCH /mcp/connections/{connection_id}`).
- Since rc.4 the session is the conversation and the plan is the unit of work (`03-genaix-api.md` §3, §4, §5.3, §8):
  - A session has no workflow of its own; each plan has its own modules (`Plan.workflows`), status (`PlanStatus`), steps, checks, gates, publications and approval chain.
  - Plan-scoped events carry `plan_id`; key plan state on `plan_id` plus the object id.
  - Read plans with `GET /sessions/{session_id}/plans` and `.../plans/{plan_id}` (`PlanState`). `GET .../workflow` and `WorkflowState` are deprecated.
  - Plan transitions use `POST /sessions/{session_id}/plans/{plan_id}/transitions`: `release`, `publish`, `request_review`, `discard`. `POST /sessions/{session_id}/transitions` takes only `archive` and `unarchive`.
  - Only a user message with a `plan` part (`plan_id`, `based_on_version`, optional `items`) and `options.mode: execute` accepts a plan.
  - `free_chat` is gone; without `workflow` a session starts with the session agent.
- Since rc.5 a plan is living and may use several modules (`03-genaix-api.md` §4.1, §4.4 to §4.6; `09-integration-guide.md` "The plan card"). rc.5 breaks rc.4 clients and has no compatibility switch:
  - `Plan.workflows` (`[{id, version}]`, in run order) replaces `Plan.workflow` and `workflow_version`, likewise on `PlanState` (whose `inputs` is keyed by module id); `PlanSummary.workflows` is a list of module ids. `PlanItem.workflow` is required. `on_discard` is the strictest of the plan's modules (`restore` over `keep`).
  - `Step.id` and `Check.id` are always `<workflow>/<id>` (`plan/<id>` for no module, e.g. `plan/st-publish`), also in a one-module plan. `steps_template` and gate ids in `GET /workflows` stay unprefixed.
  - A follow-up on a plan's work revises the same plan (same `id`): `drafting` (still at the current `version`, items kept), then `proposed` at `version + 1`. Every plan but `discarded` and `superseded` is revisable; `reopen` is removed from `TransitionRequest.action`. Statuses are open (`drafting` to `released`), settled (`published`, `review_requested`; revisable) or final (`discarded`, `superseded`).
  - `options: { mode: 'revise', plan_id }` on a message forces a revision of that plan (default `Session.open_plan_id`); on an executing plan (`accepted`, `running`) it is a redirect. `409 plan_not_revisable` for a final plan, `404 plan_not_found` for one the session does not have.
  - A redirect cancels the executing (or proposing) run with `run.cancelled` and `run.cancel_reason: 'redirected'`, then the plan's proposing run follows. It is not a stop and not a failure.
  - `PlanItem`: `version` (the version that added it), `change` (`added`, `changed`, `removed`, `unchanged`, against the last accepted version; every item is `added` on a plan never accepted), `depends_on`, and item status `removed` (listed until the version is accepted). A `done` item is never removed or changed (`422 validation_failed` at `/parts/<i>/plan/items/<j>`). Sending a removed item's `id` brings it back. An order that puts an item before one it depends on, or drops an item a kept item depends on, is `422` with `errors[].code: dependency_order`.
  - Accepting as proposed keeps `version`; an adjusted acceptance is `version + 1`. Nothing may rely on rc.4's version + 1 on every acceptance.
  - Several plans may be open (`Session.open_plan_ids`); at most one executes (`Session.executing_plan_id`). An accepted plan runs one execute run per contiguous segment of one module's pending items (`Run.workflow`); "done" comes from `plan.status`, never from `run.completed`.
  - Settings review: advisory keys `file_folder` and `image_folder` (a CMS folder id; a `missing` entry with `recommended` and `other_allowed: true`).
- The rc.5 addition of 2026-10-15, "plans at a glance" (`openapi.yaml` changelog; `03-genaix-api.md` §8, §9). Optional members only and one constraint relaxed; a client that ignores them keeps working:
  - `PlanItem.creates` (`PlannedObject`: `type` `folder`|`page`|`file`|`image`|`construct`, `label`, `path?`, `language?`, `of?`; no id yet, so not a `ContextReference`) says what a `create` or `translate` item (or a `construct` item defining a new construct) brings into being. Its `target` stays the object it acts in: for a create, the parent folder.
  - `PlanItem.created` (`ContextReference`): the object the item created, once its write completed; kept when `done`. One item creates at most one object. `PlanState.previews` (one per page, in the order of `publications`) previews it.
  - `PlanItem.parent`: the item is a sub-step (tag fills, verifications) of the item whose object it works on. One level deep, after its parent; a parent is `done` only when its sub-steps are. Every top-level item that writes (`create`, `update`, `link`, `translate`, `publish`, `construct`) concerns exactly one CMS object: its `creates`, else its `target`.
  - The glance: count only items without `parent`, grouped by `kind` and object type (`creates.type`, else `target.type`), e.g. "+3 folders · +4 pages · 2 pages edited · +2 translations". `report` and `other` items are never counted.
  - `UserPlanItem.creates` and `parent` (absent keeps the current item's). Removing a parent removes its sub-steps. A sub-step before its parent, or a sub-step of a sub-step, is `422 validation_failed` with `errors[].code: dependency_order`. `UserPlan.items`: 1 to 200 (was 100).
  - `Check.plan_version` (the version it was raised against) and `Check.item_ids` (the items it concerns). The proposing run reports findings before acceptance (`report_check`: page online, changed since read, near-duplicate, irreversible effect): `info` is a heads-up, `warn` needs attention, neither blocks, and accepting needs no acknowledgement. Before acceptance show the findings with `plan_version === plan.version`; older ones are history. A gate check carries the version its run executed.
  - `choice` interactions: `recommended` (an array of option ids, one unless `multi`; a `settings_review` entry's `recommended` is a single value), `options[].reason`, `other_allowed`. Render like the settings review: recommended option first with its reason, preselected; with `other_allowed` a free-text answer sent as `ChoiceAnswer.other` (1 to 4000 characters), `selected` may then be empty. `other` without `other_allowed` is `422` at `/answer/other`; empty `selected` without `other` is `422` at `/answer/selected`.
- From `mcp-tools.json` the UI needs only:
  - the `inputSchema` of `search_content` and `count_content`: the keys of `UserFilterPart.criteria` must use these field names, camelCase as spelled there (`openapi.yaml`, `UserFilterPart`)
  - tool names and groups, if `tool.started` gets custom labels. `tool.started.group` is a CMS group or `genaix`; the `genaix` tools are listed in `genaix_side_tools.tools` (rc.5 adds `revise_plan`)
- Tools in the `design` group are `phase: "future"`; don't build UI for them.
- Don't invent fields. If a shape is not in `openapi.yaml`, say so and ask.

## Local mock
`INDEX.md` says the mock source is not included here. It is in the hand-out repo instead:
- `git@git.gentics.com:psc/genaix/api-contract.git`, checked out at `../../api-contract` from this app (the path `npm run mock:genaix` uses). The GenAIx mock is `mocks/genaix-mock/`, its transcripts are `examples/*.sse` and `mocks/genaix-mock/scripts/*.sse`.
- rc.5, agreed, is on `main` (commit `98bcbf4` "rc.5 addition (2026-10-15): plans at a glance" adds the addition, mock included). The branch `rc5-draft` holds the draft before agreement; `rc4-draft` the previous candidate. rc.5 adds `examples/s8-living-plan.sse`: one plan, two modules, revisions and a redirect (`#s8` in the drafting message binds a plan to it). The transcripts S1, S6, S7 and S8 carry the addition's members (`creates`, `created`, `parent`, `Check.plan_version`, `item_ids`, a `choice` with `recommended` and `other_allowed`).
- Start it with `npm run mock:genaix` (`GENAIX_MOCK_PORT=8123 ./run.sh` in `mocks/genaix-mock/`; Python 3.11+, creates `.venv` on first run). It serves `http://localhost:8123/api/v1`, the same port as the docker compose stack of the hand-out (`deploy-README.md`). `run.sh` alone defaults to `8080`, which the docs under `contracts/` still name.
- Fixture credentials only, in direct mode: `Authorization: Bearer sk_gnx_mock` (any `sk_gnx_` token works) and any `X-GCMS-Subject`. Proxied mode needs `GENAIX_MOCK_CMS_PUBLIC_KEY` (`mocks-genaix-mock-README.md`, "Identity: two caller modes").
- Only the GenAIx mock is needed. The CMS MCP mock (`mocks/cms-mcp-mock/`) is not, because the UI never calls the MCP and the GenAIx mock never contacts an MCP server.
- `mocks-genaix-mock-README.md` describes the rc.5 mock (plan routing, "Living plans (rc.5)", the `#draft:`, `#revise`, `#redirect`, `#supersede`, `#ask`, `#planmerge` and `#publishfail` markers). Before relying on it, check the version of the local checkout: `genaix_mock/config.py`, `API_VERSION`. An older checkout answers with rc.4 shapes.

## Reading large files
Never read `openapi.yaml` (~14,000 lines) or `mcp-tools.json` (~9,700 lines) in full. Extract what you need:
- OpenAPI schema: `grep -n '^    <SchemaName>:$' .claude/contracts/openapi.yaml`, then read from that line with an offset
- MCP tool: `jq '.groups[].tools[] | select(.name=="search_content")' .claude/contracts/mcp-tools.json`
- All tool names: `jq -r '.groups[].tools[] | "\(.group)\t\(.name)"' .claude/contracts/mcp-tools.json`

For UI work, read the docs in the order given in `INDEX.md` under "Reading path for the Workspace UI".

# The hand-out as a docker compose stack

Everything in this package on one host port: the documents, the contracts, the rendered
sites, the reference client, the GenAIx API mock and the CMS MCP mock. Three containers,
one origin, no local Python and no virtualenvs.

## Start it

```bash
cd stage1/deploy
docker compose up -d --build
```

The first build takes a few minutes, mostly installing the mocks' pinned dependencies.
When `docker compose ps` reports both mocks healthy, open <http://localhost:8123/>.

## The URL map on port 8123

| URL | Served by | What it is |
|---|---|---|
| `/` | nginx | Landing page: the documents, the contracts and the links below |
| `/llms.txt` | nginx | This package as an [llmstxt.org](https://llmstxt.org) index, `text/plain` |
| `/llms-full.txt` | nginx | Every document concatenated, with a `# File: <path>` separator before each |
| `/md/<name>.md` | nginx | One document as raw `text/markdown`, flat names (`01-architecture.md`, `mcp-README.md`) |
| `/docs/` | nginx | The deliverables site: overview, integration guide, Swagger UI, Redoc, the CMS MCP tool browser |
| `/html/` | nginx | Every document rendered as a static page, in reading order |
| `/client/` | nginx | The reference client, pointed at the API mock on this same origin |
| `/openapi.yaml` | nginx | The GenAIx API contract, `application/yaml` |
| `/mcp/tools.json` | nginx | The CMS MCP tool specification |
| `/schemas/*.json` | nginx | The standalone JSON Schemas |
| `/examples/` | nginx | `requests.http` and the five recorded `.sse` transcripts, as text |
| `/api/v1/...` | genaix-mock | The GenAIx API, including `/api/v1/docs` and the event stream |
| `/mcp` | cms-mcp-mock | The MCP endpoint, streamable HTTP over `GET`, `POST` and `DELETE` |
| `/rest/admin/token` | cms-mcp-mock | The mocked CMS token provisioning routes |
| `/preview/page/<id>` | cms-mcp-mock | Fixture page previews |
| `/healthz` | cms-mcp-mock | Readiness of the MCP mock |

Both proxied routes stream: buffering and caching are off, the read timeout is an hour,
and `X-Accel-Buffering` is passed through, so Server-Sent Events and streamable HTTP
arrive event by event rather than in one block at the end.

## Tokens and headers

Fixture credentials only. Nothing here authenticates against anything real.

| Where | Value |
|---|---|
| GenAIx API, `Authorization` | `Bearer sk_gnx_mock` |
| GenAIx API, `X-GCMS-Subject` | any opaque subject string, for example `sub_c3f1a07d9e5b4826`; required on every route except `GET /ping` |
| The CMS connection's credential | any string; a `invalid_`, `mismatch_`, `unreachable_`, `expired_` or `revoked_` prefix selects a failure branch |
| MCP endpoint, `Authorization` | `Bearer cc-demo-token`, `chiefcc-demo-token` or `admin-demo-token`, one per showcase role |
| Token provisioning, `Cookie` | `GCN_SESSION_SECRET=jdoe-session-secret`, `mmueller-session-secret` or `asmith-session-secret` |

In the client's connection panel the Base URL already defaults to this origin's
`/api/v1`; fill in the token and a user id and press **Test connection**. A full run needs
a credential registered on the CMS connection first, which the connection card does.

A session from the command line:

```bash
B=http://localhost:8123/api/v1
H=(-H 'Authorization: Bearer sk_gnx_mock' -H 'X-GCMS-Subject: sub_c3f1a07d9e5b4826')
J=(-H 'Content-Type: application/json')

CID=$(curl -s "${H[@]}" "$B/mcp/connections" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["items"][0]["id"])')
curl -sX PUT "${H[@]}" "${J[@]}" -d '{"auth_type":"bearer","token":"cmstok_demo"}' \
  "$B/mcp/connections/$CID/authorization"

SID=$(curl -s "${H[@]}" "${J[@]}" \
  -d '{"workflow":"content_create","title":"Landing page","context":{"node_id":3,"folder_id":42,"language":"de"}}' \
  "$B/sessions" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')

curl -sX POST "${H[@]}" "${J[@]}" -d '{"content":"Create the landing page"}' "$B/sessions/$SID/messages"
curl -N "${H[@]}" "$B/sessions/$SID/events?after=0"
```

And the MCP endpoint:

```bash
claude mcp add --transport http cms-mcp http://localhost:8123/mcp \
  --header "Authorization: Bearer cc-demo-token"
```

## Rebuild after a contract change

Every generated page is built inside the image, so a change to a document, to
`openapi.yaml`, to `mcp/tools.json` or to the schemas needs the web image rebuilt:

```bash
docker compose build web && docker compose up -d web
```

The mock images only need rebuilding when a mock's own source, its fixtures, the
transcripts in `examples/` or the contract files the mocks read change:

```bash
docker compose build genaix-mock cms-mcp-mock && docker compose up -d
```

`docker compose up -d --build` rebuilds whatever is stale and is always safe.

## Stop it

```bash
docker compose down             # stop and remove the containers
docker compose down -v          # also drop the API mock's uploads and artifacts
```

## What this is not

A demo stack, not a deployment.

- **No TLS and no authentication in front.** Anything that can reach port 8123 can use
  every route, including the token provisioning routes.
- **The mocks hold their state in memory.** A restart loses every session, connection and
  credential; only the uploaded and generated files survive, in the `genaix-mock-data`
  volume.
- **Both mocks are single-process.** The event bus is per process, so neither service can
  be scaled to more than one replica.
- **Fixture data and fixture credentials throughout.** The tokens above are in the
  package; no CMS, no model and no MCP server is contacted by anything here.
- **The containers run as root** and the images carry their test dependencies, because
  the same images are meant to be poked at, not hardened.

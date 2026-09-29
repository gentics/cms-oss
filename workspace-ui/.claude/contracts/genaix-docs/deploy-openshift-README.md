# The hand-out on OpenShift

The same stack as `deploy/docker-compose.yml`, on a cluster, as **one pod with three
containers** behind one route. Nothing is built on the cluster: all three containers run
the official `python:3.12-slim` image and take the package, the virtualenvs and the
generated pages from one `ReadWriteOnce` volume that `sync.sh` fills with `oc rsync`.

```
                     Route (edge TLS, cert-manager)
                                 |
                      Service genaix-handout :8123
                                 |
  +------------------------ one pod ---------------------------------+
  |  initContainer prepare   venvs + the built site on the volume    |
  |  web          :8123      serve.py: the package + both proxies    |
  |  genaix-mock  :8080      the GenAIx API v1 mock                  |
  |  cms-mcp-mock :8765      the CMS MCP server mock                 |
  |  volume /data            stage1/ site/ venv/ mockdata/ home/     |
  +------------------------------------------------------------------+
```

| File | What it is |
|---|---|
| `genaix-handout.yaml` | Every object: PersistentVolumeClaim, Service, Route and Deployment |
| `prepare.sh` | Runs **on the pod**: the three virtualenvs and the built site. Idempotent |
| `serve.py` | Runs **on the pod**: the web tier, replacing `deploy/nginx.conf` |
| `sync.sh` | Runs **locally**: copy, rebuild, restart. The whole deployment step |

All four objects are named `genaix-handout`, so the whole deployment is one name to
remember and `oc get all,pvc -l app.kubernetes.io/name=genaix-handout` shows it.

## Apply it

```bash
oc project <the target project>
oc apply -f stage1/deploy/openshift/genaix-handout.yaml
```

The pod starts immediately and stays **not ready**: all three containers wait for
`/data/stage1/.synced`, which only the first sync creates. That is the intended state, and
it is what makes the volume reachable for the copy.

## First sync

```bash
stage1/deploy/openshift/sync.sh
```

It copies the package to the volume, creates the three virtualenvs, builds the rendered
pages and the machine-readable entry points, and restarts the pod so the mocks read the
contracts. The first run takes a few minutes, almost all of it the mocks' pinned
dependencies; later runs are fast, because each virtualenv is rebuilt only when its
`requirements.txt` changes.

When it finishes it prints the URL.

## Re-sync after a change

The same command. Every change, to a document, to `openapi.yaml`, to `mcp/tools.json`, to
the schemas, to the transcripts or to a mock's own source, is deployed by re-running it:

```bash
stage1/deploy/openshift/sync.sh               # copy, rebuild, restart
stage1/deploy/openshift/sync.sh --no-restart  # copy and rebuild only
```

`--no-restart` is enough for a change to the documents, the schemas or the generated
pages, because the web tier reads those from disk per request. A change to `openapi.yaml`,
to `examples/`, to `mcp/tools.json` or to a mock's source needs the restart, because each
mock loads the contracts once at startup.

## Logs and state

```bash
oc get pod -l app.kubernetes.io/name=genaix-handout
oc logs -l app.kubernetes.io/name=genaix-handout -c prepare        # what the volume got
oc logs -f -l app.kubernetes.io/name=genaix-handout -c web
oc logs -f -l app.kubernetes.io/name=genaix-handout -c genaix-mock
oc logs -f -l app.kubernetes.io/name=genaix-handout -c cms-mcp-mock
oc rsh -c web deploy/genaix-handout                                # poke at /data
```

`oc logs -c prepare` is the first place to look when the pod will not become ready: the
initContainer deliberately exits 0 even when the preparation fails, so that a volume in a
bad state can still be repaired with `oc exec` and another `sync.sh` rather than leaving
the pod in a crash loop.

## The URL map

One origin, so a browser, `curl` and an MCP client all see the same host.

| URL | Served by | What it is |
|---|---|---|
| `/` | web | Landing page: the documents, the contracts and the links below |
| `/llms.txt` | web | This package as an [llmstxt.org](https://llmstxt.org) index, `text/plain` |
| `/llms-full.txt` | web | Every document concatenated, with a `# File: <path>` separator before each |
| `/md/<name>.md` | web | One document as raw `text/markdown`, flat names. Listed |
| `/docs/` | web | The deliverables site: overview, integration guide, Swagger UI, Redoc, the MCP tool browser |
| `/html/` | web | Every document rendered as a static page, in reading order |
| `/client/` | web | The reference client, pointed at the API mock on this same origin |
| `/openapi.yaml` | web | The GenAIx API contract, `application/yaml` |
| `/mcp/tools.json` | web | The CMS MCP tool specification. `/mcp/` is listed |
| `/schemas/*.json` | web | The standalone JSON Schemas. Listed |
| `/examples/` | web | `requests.http` and the five recorded `.sse` transcripts, as text. Listed |
| `/healthz-web` | web | Readiness of the web tier itself |
| `/api/v1/...` | genaix-mock | The GenAIx API, including `/api/v1/docs` and the event stream |
| `/mcp` | cms-mcp-mock | The MCP endpoint, streamable HTTP over `GET`, `POST` and `DELETE` |
| `/rest/admin/token` | cms-mcp-mock | The mocked CMS token provisioning routes |
| `/preview/page/<id>` | cms-mcp-mock | Fixture page previews |
| `/healthz` | cms-mcp-mock | Readiness of the MCP mock |

Both proxied routes stream. `serve.py` forwards request and response bodies chunk by
chunk with no buffering and an hour-long read timeout, and passes `mcp-session-id` and
`X-Accel-Buffering` through, so Server-Sent Events and streamable HTTP arrive event by
event rather than in one block at the end.

## Tokens and headers

Fixture credentials only. Nothing here authenticates against anything real.

| Where | Value |
|---|---|
| GenAIx API, `Authorization` | `Bearer sk_gnx_mock` |
| GenAIx API, `X-GCMS-Subject` | any opaque subject string, for example `sub_c3f1a07d9e5b4826`; required on every route except `GET /ping` |
| The CMS connection's credential | any string; a `invalid_`, `mismatch_`, `unreachable_`, `expired_` or `revoked_` prefix selects a failure branch |
| MCP endpoint, `Authorization` | `Bearer cc-demo-token`, `chiefcc-demo-token` or `admin-demo-token`, one per showcase role |
| Token provisioning, `Cookie` | the three `GCN_SESSION_SECRET` values in `mocks/cms-mcp-mock/fixtures/tokens.json` |

A session from the command line, against the deployed origin:

```bash
B=https://<the route host>/api/v1
H=(-H 'Authorization: Bearer sk_gnx_mock' -H 'X-GCMS-Subject: sub_c3f1a07d9e5b4826')
J=(-H 'Content-Type: application/json')

SID=$(curl -s "${H[@]}" "${J[@]}" \
  -d '{"workflow":"content_create","title":"Landing page","context":{"node_id":3,"folder_id":42,"language":"de"}}' \
  "$B/sessions" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')

curl -sX POST "${H[@]}" "${J[@]}" -d '{"content":"Create the landing page"}' "$B/sessions/$SID/messages"
curl -N "${H[@]}" "$B/sessions/$SID/events?after=0"
```

And the MCP endpoint:

```bash
claude mcp add --transport http cms-mcp https://<the route host>/mcp \
  --header "Authorization: Bearer cc-demo-token"
```

## How it is put together, and why

- **One pod.** Both mocks keep their state in one process and their event bus is per
  process, so neither can be scaled past one replica. The web tier shares the pod because
  it reverse proxies both over `127.0.0.1`, which is also what makes one origin possible
  without a second Service or a second route.
- **No image build.** The cluster pulls `python:3.12-slim` and nothing else. What would be
  an image layer in the compose stack is the volume here, and `sync.sh` is the build step,
  run from a checkout.
- **`Recreate`, not `RollingUpdate`.** The volume is `ReadWriteOnce`, so the old pod has to
  release it before the new one claims it.
- **Everything writable lives on the volume or in `/tmp`.** The pod runs as an arbitrary
  uid with a read-only root filesystem, so the virtualenvs, `HOME`, the pip cache, the
  built site and the API mock's uploads are all under `/data`, and `/tmp` is an `emptyDir`.
- **No `workingDir` under `/data`.** The kubelet would create it on the volume as `root`
  before the first sync, and the pod's uid could then not write into it. The container
  commands `cd` themselves instead.
- **The containers wait for `.synced`.** Each one loops until the package and its own
  virtualenv are on the volume. That way the pod is running, and therefore reachable for
  `oc rsync`, before there is anything to run.
- **`oc rsync` falls back to tar**, because the image has no `rsync`, and the tar fallback
  cannot honour `--delete`. `sync.sh` clears `/data/stage1` itself before the copy, which
  has the same effect and keeps the virtualenvs and the built site.

## What this is not

A demo deployment, not a production service.

- **No authentication in front.** Anything that can reach the route can use every route,
  including the token provisioning routes. TLS terminates at the edge.
- **The mocks hold their state in memory.** A restart, including the one at the end of
  every sync, loses every session, connection and credential. Only the uploaded and
  generated files survive, under `/data/mockdata`.
- **Fixture data and fixture credentials throughout.** No CMS, no model and no MCP server
  is contacted by anything here.
- **The volume is the source of truth for what is served.** A re-sync overwrites it from a
  checkout; nothing edits it in place.


## Access restriction

The route carries the same HAProxy source-address allowlist and 900 s timeout as the GenAIx application route (`haproxy.router.openshift.io/ip_whitelist`, `haproxy.router.openshift.io/timeout`). Requests from other addresses are rejected by the router before they reach the pod; adjust the annotation in `genaix-handout.yaml` and re-apply to change it.


## Content changes versus manifest changes

`sync.sh` copies the package into the volume, rebuilds the site and restarts the pod; it does not touch the cluster objects. When `genaix-handout.yaml` itself changes (environment variables, probes, resources, route annotations), run `oc apply -f genaix-handout.yaml` first and then `sync.sh`.

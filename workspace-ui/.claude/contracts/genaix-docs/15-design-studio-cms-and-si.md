# Design Studio: CMS tools and System Integrator input

## 1. CMS MCP tools

GenAIx reaches the CMS only through the CMS MCP server, on the session user's credential
(`04-mcp-scope.md`). The Design Studio needs the tools below, in the `design` group of
`mcp/tools.json` (`phase: two` since `1.0.0-rc.7`; the group's other tools stay `future`). None of
them uses devtools package synchronisation: each object is read or written by its own REST call. The
group is listed only for users with template rights (`04-mcp-scope.md` §2.4). ioflair builds these tools on a
cms-oss branch, as it builds the Phase Two tools; Gentics reviews and merges them.

| Tool | Access | Purpose | CMS REST |
|---|---|---|---|
| `create_template` | write | Create a page template on the target node and link it to folders | `POST /template`, `POST /template/link` |
| `update_template` | write | Change a template's source, template tags or folder links | `POST /template/{id}` |
| `link_template_to_folders` | write | Make a template available in folders | `POST /template/link` |
| `get_template_usage` | read | The pages that use a template, and per template tag how many of them a change would put out of sync | `GET /template/{id}/tagstatus` (counts per tag); the page list endpoint is Gentics TODO G8 |
| `list_package_objects` | read | The constructs and templates a devtools package contains, with global IDs | `GET /devtools/packages/{name}/constructs`, `…/templates` |
| `add_template_to_package` | write | Add a template to a package (`add_construct_to_package` already exists) | `PUT /devtools/packages/{name}/templates/{template}` |
| `list_design_files` | read | List a directory under `files-private/design/` of a package | `GET /devtools/packages/{name}/files-private?path=design/…` |
| `read_design_file` | read | Read one design file | `GET /devtools/packages/{name}/files-private/design/{path}` |
| `write_design_files` | write | Upload files into one directory under `files-private/design/` | `POST /devtools/packages/{name}/files-private/design/{dir}` |
| `list_portal_implementations` | read | The portal implementations assigned to a node, with their file names | `GET /portalimplementations`, `GET /portalimplementations/{id}/files` |
| `read_portal_implementation_file` | read | Read one file of a portal implementation, for the base, a revision's snapshot and the merge | `GET /portalimplementations/{id}/content/{path}` |
| `write_portal_implementation_file` | write | Write one file of a portal implementation | `PUT /portalimplementations/{id}/content/{path}` |
| `assign_portal_implementation` | write | Assign a portal implementation to a node | `PUT /node/{id}/portalimplementations/{id}` |
| `get_node_publish_status` | read | Whether the design node's last publish run has completed, so GenAIx renders only what the preview portal really serves | to be defined by Gentics |

Notes for the CMS team:

- The design file tools work on the package's private storage (`files-private`, Gentics TODO G1 of
  `10-design-studio.md` §5); until it exists, on `files-internal`, for non-confidential sites only.
- The design file tools accept only normalised paths under `design/`: no `.` or `..` segment, so
  they cannot reach guidelines or other internal files of a package; the guideline tools are the
  model. The CMS must check the normalised path, not only the pattern. GenAIx's own gate also refuses
  the model's calls on `evidence/` paths (`12-design-studio-files.md` §6).
- Portal implementations are on the unreleased branch `f-gpu-2770`; their tools follow its REST API.
- A portal implementation reaches Mesh only by a regular publish run, not by instant publish. The
  preview node therefore needs a way to have its publish run start soon after a revision is written,
  and `get_node_publish_status` to wait for it (Gentics TODO G4, `10-design-studio.md` §5).
  (`get_publish_state`, the publish protocol of `content_read`, answers a different question and
  stays `future`.)
- `render_diff` stays `future`: the render check through the preview portal replaces it
  (`13-design-studio-verification.md`).
- `decompose_design` is removed: decomposition is GenAIx's own work and its result is the mapping in
  the design files (`12-design-studio-files.md` §3).
- Later, with native CMS branches: tools to create, list and delete a branch and to merge it, with a
  conflict report (`10-design-studio.md` §3.2).

## 2. What an integration must declare

A region whose data comes from an external system (events, products, a contact card) is an
**integration block**. The System Integrator builds the integration in code, in the project
repository, against the Integration Kit of the CMP version; the Design Studio and GenAIx never
implement one (19 Nov meeting). For GenAIx to map a region to an integration and render it in a
preview, the integration must be registered in the CMS as an object GenAIx can read, and declare:

| Part | Content | Why GenAIx needs it |
|---|---|---|
| Identity and version | Name, major version, status (released or not) | The mapping names `integration` and its version; a new major version is a new mapping |
| Data types | Each object type as JSON Schema; each field with a description and a **semantic type** from the kit's fixed vocabulary (`title`, `text`, `datetime`, `image`, `link`, `money`, `status`, …) | GenAIx matches a region's content to fields by meaning, not by name |
| Operations | `list`, `get`, `search` as needed, with parameters, paging, sorting and filters (an OpenAPI fragment of the kit) | GenAIx knows which operation a region needs and what a template may configure |
| States | Per operation: success, empty, no results, error | Every state needs a design; GenAIx renders each one |
| Example data | At least three objects per type, and one response per operation and state | The preview and the render check need data without the external system |
| Preview provider | Serves the operations from the example data | The preview portal renders integration blocks without production access |
| Runtime flags | Cacheable, personalised, indexable; what a template may configure | GenAIx does not propose a design the integration cannot serve (for example a personalised block on a cached page) |

An integration declares **no** markup, CSS, presentation parameters or assumptions about where it is
placed, and no secrets: presentation is the Design Studio's work. When no registered integration
provides the data a region needs, GenAIx maps the region as an integration block with a placeholder,
writes no code for it, and the UI can hand the System Integrator a task with the wanted fields
(`03-genaix-api.md` §9.2 shows the same task handover for constructs).

Gentics defines where this object lives in the CMS and how it is read (Gentics TODO G10,
`10-design-studio.md` §5); the tools to list and read integrations follow from it.

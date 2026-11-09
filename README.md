<h1 align="center">Gentics CMS</h1>
<h3 align="center">The friendly, enterprise-grade, open-source CMS – and the editing heart of Gentics CMP</h3>

<p align="center">
  <a href="LICENSING.md"><img src="https://img.shields.io/badge/core-Apache--2.0-brightgreen.svg" alt="Core: Apache-2.0" /></a>
  <a href="LICENSING.md"><img src="https://img.shields.io/badge/ee-BUSL--1.1-blue.svg" alt="Enterprise Modules: BUSL-1.1" /></a>
  <a href="https://hub.docker.com/r/gentics/cms-oss"><img src="https://img.shields.io/badge/docker-gentics%2Fcms--oss-blue.svg" alt="Docker" /></a>
  <a href="https://cla-assistant.io/gentics/cms-oss"><img src="https://cla-assistant.io/readme/badge/gentics/cms-oss" alt="CLA assistant" /></a>
</p>

Gentics CMS is where content gets made. Editors write in a real WYSIWYG editor, admins manage users, roles
and workflows, and the CMS publishes everything into [Gentics Mesh](https://github.com/gentics/mesh), from
where portals, apps and AI agents consume it. It has been in production at public administrations,
chambers and enterprises for more than two decades, and since Gentics CMP 9 the complete code is public.

## Where it sits in Gentics CMP

| Repository | Component |
| --- | --- |
| [gentics/cms-oss](https://github.com/gentics/cms-oss) | **This repo.** Gentics CMS – editing, publishing, Workspace, MCP tools |
| [gentics/mesh](https://github.com/gentics/mesh) | Gentics Mesh – headless content repository, search, Content RAG, MCP server |
| [gentics/cmp-portals](https://github.com/gentics/cmp-portals) | Portal \| php and Portal \| java – render content from Mesh |
| [gentics/cmp](https://github.com/gentics/cmp) | Umbrella: platform changelog, guides, Helm chart, licensing overview |

Content flows from Gentics CMS into Gentics Mesh and from there into the portals or your own applications.
The [cmp README](https://github.com/gentics/cmp#readme) has the big picture and the cross-repository
getting-started guide.

## What you get

* **Editing that editors like.** Content trees as websites think of them, a WYSIWYG editor based on
  [Aloha Editor](cms-aloha-bundle), in-place editing, multi-language content, automatic navigation and
  clean URLs.
* **Publishing into Mesh.** Pages, files, images and structured objects are published into Gentics Mesh and
  are available there through REST and GraphQL.
* **Workspace.** The AI co-pilot for editors ([`workspace-ui`](workspace-ui)): a chat-based assistant
  that works on the content of the CMS, with every action bound to the user's CMS permissions. AI calls go
  through GenAIx, the Gentics-hosted AI service, via the CMS proxy, so no key ever reaches the browser.
* **MCP tools.** The CMS exposes its content operations through the Model Context Protocol
  ([`cms-core/.../mcp`](cms-core/src/main/java/com/gentics/contentnode/mcp)), so agents work with the same
  permission model as humans.
* **A complete REST API** ([`cms-restapi`](cms-restapi)) with [OpenAPI documentation](https://www.gentics.com/Content.Node/guides/),
  a Java client and TypeScript models for the UIs.
* **Built to run for years.** Link checker, search, translation, clustering, and a Keycloak integration,
  content staging, forms and multichannelling as Enterprise Modules.

## Repository map

```
cms-oss/
├── base-api, base-lib        shared foundations
├── cms-api, cms-restapi      public API and REST models (OpenAPI)
├── cms-core                  the server: publishing, permissions, rendering, MCP tools
├── cms-oss-server            server distribution (Docker image gentics/cms-oss)
├── cms-ui                    editor and admin UI, NX monorepo (Angular)
├── workspace-ui              Workspace, the AI co-pilot for editors (React/Vite)
├── cms-aloha-*               Aloha Editor bundle and plugins
├── cms-cache, cms-cluster, cms-search, cms-linkchecker, cms-translation
├── cms-oss-doc, cms-oss-changelog
├── cms-integration-tests
└── ee/                       Enterprise Modules (BUSL-1.1)
    ├── cms-contentstaging    content staging between environments
    ├── cms-forms, formgen    forms and the form generator
    ├── cms-keycloak          SSO with Keycloak
    ├── cms-multichannelling  one content base, many channels
    └── cms-server            enterprise server distribution
```

## Getting started

### Run it

The quickest way to a running CMS is the Docker image
[gentics/cms-oss](https://hub.docker.com/r/gentics/cms-oss). The
[installation guide](https://www.gentics.com/Content.Node/guides/) walks you through the setup with
MariaDB, the configuration files and the first login. For Kubernetes, use the Helm chart of the
[Gentics CMP umbrella chart](https://github.com/gentics/cmp/tree/master/ee/cmp-chart).

### Build it

Requirements: Java 17, Maven 3.9+, Node.js (for the UIs), Docker (for the integration tests).

```bash
git clone https://github.com/gentics/cms-oss.git
cd cms-oss
mvn clean install -DskipTests
```

The reactor builds the core and the `ee/` modules. The UIs have their own tooling, see
[`cms-ui/README.md`](cms-ui/README.md) (NX) and [`workspace-ui/README.md`](workspace-ui/README.md) (Vite,
with a local GenAIx mock for development).

### Talk to it

Everything the UIs do goes through the REST API, so you can too:

```bash
# log in and get a session
curl -X POST http://localhost:8080/rest/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"login":"<user>","password":"<password>"}'
```

The full API reference is in the [guides](https://www.gentics.com/Content.Node/guides/).

### Branches

* `dev` – next release
* `hotfix-<version>` – maintenance of released versions
* `f-<ticket>` – feature branches

## Documentation and help

* [Guides](https://www.gentics.com/Content.Node/guides/) – installation, configuration, development, API
* [Changelog](https://www.gentics.com/Content.Node/changelog/)
* [System requirements](https://www.gentics.com/Content.Node/guides/faq_system_requirements.html)
* [Gentics CMP changelog](https://www.gentics.com/cmp/changelog/) and [guides](https://www.gentics.com/guides/) across all components
* Issues: [github.com/gentics/cms-oss/issues](https://github.com/gentics/cms-oss/issues)

## Licensing

Gentics CMS is **open core**:

* **Core – Apache License 2.0.** Everything outside `ee/`. OSI-approved open source: run it in production,
  modify it, redistribute it.
* **Enterprise Modules – Business Source License 1.1.** Everything inside `ee/`: content staging, forms and the form generator, the Keycloak integration, multichannelling and the enterprise server distribution.
  The source is public and free to use for development, testing, evaluation and demonstration. Production
  use requires a commercial license – contact [sales@gentics.com](mailto:sales@gentics.com). Every module
  converts to Apache 2.0 three years after its release (Change Date); the exact dates are in
  [`ee/LICENSE`](ee/LICENSE).

The core runs without `ee/`. [`LICENSING.md`](LICENSING.md) has the details, the
[cmp repository](https://github.com/gentics/cmp/blob/master/LICENSING.md) the overview across the platform.

## Contributing

Pull requests are welcome.

* Contributions to the core are accepted under the Apache License 2.0 with a DCO sign-off (`git commit -s`).
* Contributions to `ee/` additionally need our [Contributor License Agreement](CLA-individual.md)
  ([corporate version](CLA-corporate.md)). The CLA bot asks on your first pull request that touches `ee/`.

See [`CONTRIBUTING.md`](CONTRIBUTING.md). Found a bug? Open an issue in this repository.

## Commercial services

Gentics Software GmbH, the company behind Gentics CMS, offers product maintenance, developer support,
consulting and custom development. Contact [sales@gentics.com](mailto:sales@gentics.com).

---

Copyright (c) 2014-2026 Gentics Software GmbH. Gentics is a registered trademark of Gentics Software GmbH,
see [`TRADEMARKS.md`](TRADEMARKS.md).

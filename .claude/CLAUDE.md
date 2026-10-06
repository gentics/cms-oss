# Gentics CMS OSS – Development Guidelines

Generic workflow for resolving a task (bug fix, enhancement, or feature) in this repository.
Topic rules live in `.claude/rules/`: `build.md`, `testing.md`, `java.md`, `db-schema.md`,
`changelog.md`.

Changelog Folder: cms-oss-changelog/src/changelog/entries/<year>/<month>
Changelog Types: cms-oss-changelog/pom.xml

## Java

Maven builds of Gentics CMS require `JAVA_HOME` to point to a JDK of version 17 or above.
Check whether it is already set; if not, set it for the current session.

## Repository layout

Maven multi-module build (root `pom.xml`, Java release 17), modules in build order:
`cms-oss-bom`, `base-api`, `base-lib`, `cms-restapi`, `cms-api`, `cms-core`, `cms-js-lib`,
`cms-aloha-plugins`, `cms-aloha-bundle`, `cms-ui`, `cms-cache`, `cms-oss-server`,
`cms-oss-changelog`, `cms-oss-doc`, `cms-integration-tests`.

- `cms-core` – backend core (object model, publishing, rendering, REST implementation).
- `cms-restapi` – REST API models; `src/main/resources/openapiconfig.yaml` configures the generated
  OpenAPI specification.
- `cms-ui` – NX monorepo (Angular, Node `^26.4.0`, see `.nvmrc`) with apps (`editor-ui`, `admin-ui`,
  `ct-form-translations`, `ct-link-checker`, …) and libs (`cms-models`, `cms-rest-client`, `ui-core`,
  `e2e-utils`, …).
- `cms-oss-changelog` – public, customer-facing changelog.
- `cms-oss-doc` – guides (`src/main/source`).
- `cms-integration-tests` – docker compose setup for the Playwright UI integration tests.

NB: Some functionality requires a valid Gentics License, so its unit tests are located in the CMS EE
project, which is typically located in the `../cms` folder. Refer to it while looking for tests
absent in the CMS OSS project.

## Documentation

- Keep `cms-restapi/src/main/resources/openapiconfig.yaml` consistent with the actual REST
  authentication and behaviour.
- Guides live in `cms-oss-doc/src/main/source`; update them in the same branch when a change makes
  them wrong.

## Order of work

1. Add or update tests — see `rules/testing.md`.
2. Implement the change — see `rules/java.md`, and `rules/db-schema.md` for schema changes.
3. Verify — see `rules/build.md`.
4. Add a changelog entry for any user-facing change — see `rules/changelog.md`.

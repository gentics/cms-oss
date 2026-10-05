# Gentics CMS OSS – Development Guidelines

This describes the generic workflow for resolving a task (bug fix, enhancement, or feature) in this repository.

## 1. General guidelines

Follow the general CMP guidelines at `.claude` folder of the system user.
Usage of Maven over Gentics Mesh requires `JAVA_HOME` environment variable to be set to the proper JDK of version 17 or above. Check, if it is already set, and if no, set it for the current session.

## 2. Repository layout

Maven multi-module build (root `pom.xml`, Java release 17), modules in build order:
`cms-oss-bom`, `base-api`, `base-lib`, `cms-restapi`, `cms-api`, `cms-core`, `cms-js-lib`, `cms-aloha-plugins`, `cms-aloha-bundle`, `cms-ui`, `cms-cache`, `cms-oss-server`, `cms-oss-changelog`, `cms-oss-doc`, `cms-integration-tests`.

- `cms-core` – backend core (object model, publishing, rendering, REST implementation).
- `cms-restapi` – REST API models; `src/main/resources/openapiconfig.yaml` configures the generated OpenAPI specification.
- `cms-ui` – NX monorepo (Angular, Node `^26.4.0`, see `.nvmrc`) with apps (`editor-ui`, `admin-ui`, `ct-form-translations`, `ct-link-checker`, …) and libs (`cms-models`, `cms-rest-client`, `ui-core`, `e2e-utils`, …). It must use own additional Claude setup at `.claude/CLAUDE.md`.
- `cms-oss-changelog` – public, customer-facing changelog.
- `cms-oss-doc` – guides (`src/main/source`).
- `cms-integration-tests` – docker compose setup for the Playwright UI integration tests.

NB: Some functionality requires a valid Gentics License, so its unit tests are located in the CMS EE project, which is typically located in `../cms` folder. Please refer to it while looking for tests, absent in the CMS OSS project.

## 3. Tests

Prefer following Test Driven Development methodology, e.g. writing a failing test first, then make the fix to make it passing, as per guidelines from `.claude/skills/` folder of a current user.
If following TDD is not possible according to the task investigation results, propose the user the alternative solution and wait for the explicit permission to proceed or improve.

- Most of the tests require Docker Compose test setup, consisting of a latest MariaDB image, and Testmanager from `docker.gentics.com/apps/gcn-testdb-manager:master`. Please use `TESTMANAGER_HOSTNAME` and `TESTMANAGER_PORT` environment variables, pointing to the Testmanager's container host and port, respectively, for every run of JUnit test, that uses, or considers a usege of the database. The example Testmanager config looks as following:
  ```
  testdbmanager:
    image: docker.gentics.com/apps/gcn-testdb-manager:master
    stop_grace_period: 30s
    environment:
      - GCN_TESTDB_MANAGER_PORT=8080
      - GCN_TESTDB_MANAGER_MYSQL_HOST=172.19.0.2
      - GCN_TESTDB_MANAGER_MYSQL_PORT=3306
      - GCN_TESTDB_MANAGER_MYSQL_USER=root
      - GCN_TESTDB_MANAGER_MYSQL_PASSWORD=finger
      - GCN_TESTDB_MANAGER_GCNDB_LIMIT=1
      - GCN_TESTDB_MANAGER_GCNDB_POOL_CAPACITY=1
      - GCN_TESTDB_MANAGER_FILLER_THREADS=1
      - GCN_TESTDB_MANAGER_DEBUG=true
    ports:
      - '7070:8080'
    depends_on:
      - db
 ```
- JUnit 4 (`org.junit.Test`, `@RunWith(Parameterized.class)` is common) with AssertJ (`assertThat`, preferred in new tests) and the classic `org.junit.Assert` methods.
- Tests needing a database use a `DBTestContext`, usually as `@ClassRule` (156×, `@Rule` 41× in `cms-core` tests; `new DBTestContext().config(…)`); the database comes from the testdb docker containers (MariaDB + `gcn-testdb-manager`) started by the build (see `testdb.*`  properties in the root `pom.xml`). Docker must be available.
- Tests needing Gentics Mesh are tagged `@Category(MeshTest.class)`.
- Surefire reruns failing tests once (`surefire.rerunFailingTestsCount=1`) – a test that only passes on rerun is flaky and must be reported, not ignored.
- Run a single test the way CI does:

  ```sh
  mvn -am -pl 'cms-core,cms-oss-server' test -Dtest=<fully.qualified.TestClass>
  ```

## 4. Java code conventions

- Indentation with **tabs** .
- Javadoc on public classes, fields and methods.
- Database access runs in transactions: `try (Trx trx = new Trx()) { … }` or the helpers `Trx.supply(…)`, `Trx.operate(…)`, `Trx.consume(…)`, `Trx.execute(…)`; channel scope via `ChannelTrx`, wastebin handling via `WastebinFilter`. Reuse these instead of manual transaction handling.
- User-facing backend messages are i18n keys in `cms-core/src/main/resources/contentnode_de_DE.properties` and `contentnode_en_EN.properties` – add every new key to both, translated to German and English, respectively.

## 5. Database schema changes

- Schema changes and data migrations are appended to `cms-core/src/main/resources/changelog/{content,node,system}.CHANGELOG`, one line per statement, format: `<YYYY-MM-DD> CH-<number> <author initials> SQL <statement>;` (optional `## comment`), fields separated by whitespace (tabs in `content.CHANGELOG`, spaces in `system.CHANGELOG`). Example: `2026-07-08  CH-1748 NOP SQL CREATE TABLE api_token (…);` They are applied by `com.gentics.contentnode.changelog.ChangeLogHandler`.
- `CH-` numbers are shared between `content.CHANGELOG` (currently up to CH-1750) and `system.CHANGELOG` (up to CH-1748); new lines use the next free number across both. `node.CHANGELOG` contains no `CH-` entries. The next free number is obtained from a 3rd party service and has to be requested from the user.

## 6. Build

- Full build: `mvn clean package` at the root, with  `-Dskip.integration.tests`.
- Please avoid skipping buiding CMS UI parts where possible.
- For the development purposes you may use `Dmaven.test.skip.exec=true -Dui.skip.test=true -Dui.skip.e2e=true` to proceed with running the distinct JUnit tests, as per task.

## 7. Documentation

- Keep `cms-restapi/src/main/resources/openapiconfig.yaml` consistent with the actual REST authentication and behaviour.
- Guides live in `cms-oss-doc/src/main/source`; update them in the same branch when a change makes them wrong.
  
## 8. Changelog

The changelog entries are placed into `cms-oss-changelog/src/changelog/entries/<year>/<month>` folder. Create the `<year>/<month>` segments for the current year and month, respectively, if those are missing.
The allowed entry types are:
 - `enhancement` — for the new features. Those usually come with ticket IDs named `GPU-*`.
 - `security` — for the updates of dependency versions, when an existing dependency is evidenced to have a security vulnerability. The ticket ID might be either `SUP-*` or, in rare cases, which has to be confirmed by a user, `GPU-*`.
 - `documentation` — for the documentation-only fixes, where no code has been changed, ticket ID is mostly `SUP-*`.
 - `bugfix` — for all the other cases, with ticket IDs marked as `SUP-*`.
 - `manualchange` — the fix brings a breaking change to the user data, so the migration manual has to be provided along, independently of the ticket ID.
 - `optional-manualchange` — the fix may bring a breaking change to the user data, so its usecase description and migration manual have to be provided along, independently of the ticket ID.

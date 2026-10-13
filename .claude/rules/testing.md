# Testing

- Prefer Test Driven Development: write a failing test first, then make the fix that turns it green.
- JUnit 4 (`org.junit.Test`, `@RunWith(Parameterized.class)` is common) with AssertJ (`assertThat`,
  preferred in new tests) and the classic `org.junit.Assert` methods.
- Tests needing a database use a `DBTestContext`, usually as `@ClassRule` (`@Rule` in `cms-core`
  tests; `new DBTestContext().config(…)`).
- Tests needing Gentics Mesh are tagged `@Category(MeshTest.class)`.
- Surefire reruns failing tests once (`surefire.rerunFailingTestsCount=1`) – a test that only passes
  on rerun is flaky and must be reported, not ignored.

## Test database

The build starts the test databases itself: the `local-testdb` profile in the root `pom.xml`
(active unless `-Dtestdb.docker.skip` is given) runs MariaDB (`testdb.mariadb.image`) and
`docker.gentics.com/apps/gcn-testdb-manager:master` via `docker-maven-plugin` and passes
`TESTMANAGER_HOSTNAME` / `TESTMANAGER_PORT` and the `testdb_*` variables to surefire. Docker must be
available; CI logs in to `docker.gentics.com` before the tests (`Jenkinsfile`).

Only with `-Dtestdb.docker.skip` do the tests use an externally started testdb-manager; then set
`TESTMANAGER_HOSTNAME` and `TESTMANAGER_PORT` to its host and port. Example Docker Compose service:

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

## Running a single test

Based on CI (`Jenkinsfile`, parameter `singleTest`), restricted to the module that contains the test:

```
mvn -gs <argline-settings.xml> -Dskip.integration.tests -am -pl cms-core \
    -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false \
    -Dtest=<fully.qualified.TestClass> test
```

- **`argLine` must be defined as a project property.** The surefire configuration in the root
  `pom.xml` uses `@{argLine}`, and nothing in the project defines it. Without it, the forked JVM
  fails with `Error: could not open '{argLine}'` / `The forked VM terminated without properly saying
  goodbye`. `-DargLine=…` on the command line does **not** help; a property from an active settings
  profile does. Create this file in the scratchpad and pass it with `-gs`:
  ```
  <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
    <profiles><profile><id>argline</id>
      <properties><argLine>-Dnoop=true</argLine></properties>
    </profile></profiles>
    <activeProfiles><activeProfile>argline</activeProfile></activeProfiles>
  </settings>
  ```
- Do not add `cms-oss-server` to `-pl` for `cms-core` tests (CI uses `-pl 'cms-core,cms-oss-server'`):
  its upstream `cms-aloha-bundle` unpacks the `cms-aloha-plugins` zip, which a `test`-phase reactor
  run does not package and which is not published as a `-SNAPSHOT`, so the build fails there.
- If the build fails before reaching the `testdb-stop` execution (phase `test`), the testdb
  containers keep running and block the ports of the next run. Report them to the user; do not
  remove them without asking.

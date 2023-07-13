- [IDE Setup](#ide-setup)
  - [Checkout](#checkout)
  - [Setup Runner](#setup-runner)
  - [Running the UIs](#running-the-uis)
  - [Running tests](#running-tests)
- [Building the Changelog locally](#building-the-changelog-locally)
- [Building the Guides locally](#building-the-guides-locally)

# IDE Setup
## Checkout

The following projects need to be checked out in the workspace:
```
git clone git@github.com:alohaeditor/Aloha-Editor.git alohaeditor
git clone git@github.com:gentics/cms-oss.git
git clone git@git.gentics.com:psc/cms.git
```

## Building

It is assumed that all Java modules will be built by the IDE. Additionally some non-Java modules must also be built:

### Aloha Editor

Building Aloha Editor requires Java 8.

```
cd [workspace]/alohaeditor/
mvn clean compile
```

### CMS JS Lib

```
cd [workspace]/cms-oss/
mvn -pl cms-js-lib clean package
```

### Link Checker Custom Tool

```
cd [workspace]/cms-oss/cms-ui/
npm install
npm run build ct-link-checker
```

## Setup Runner

The classes, which contain the server are:

* `com.gentics.contentnode.server.OSSRunner` for the OSS CMS (not containing the commercial modules)
* `com.gentics.contentnode.server.Runner` for the Commercial CMS (including the commercial modules)

When the server should be run from withing an IDE, it is best to configure everything via environment variables. These are the required variables to be set:

| env                      | Description                             | Example |
| ------------------------ | --------------------------------------- | ------- |
| ALOHAEDITOR_PATH         | Path to the Aloha Editor Files          | ../../alohaeditor/src                           |
| ALOHAEDITOR_PLUGINS_PATH | Path to the Aloha Editor Plugins        | ../../cms-oss/cms-aloha-plugins/src/main/js     |
| GCNJSAPI_PATH            | Path to the CMS JS Files                | ../../cms-oss/cms-js-lib/target                 |
| STATIC_SERVE_LIST        | List of statically served directories   | ../../cms-oss/cms-ui/dist/apps/ct-link-checker/ |
| CONF_FILES               | Configuration files                     | custom                                          |
| LICENSEKEY               | License key                             | 0000-0000-0000-0000-0000-0000-0000-0000         |
| NODE_DB_HOST             | Hostname of the CMS DB                  | localhost                                       |
| NODE_DB_PORT             | Port of the CMS DB                      | 3306                                            |
| NODE_DB_NAME             | Name of the CMS DB                      | cms                                             |
| NODE_DB_USER             | Username for the CMS DB                 | cmsuser                                         |
| NODE_DB_PASSWORD         | Password of the CMS DB User             | cmspassword                                     |
| NODE_USER_PASSWORD       | Initial password of the CMS user `node` | node                                            |

The backend database must already exist (but can be empty) and must be accessible with the given credentials/connection data.

All given paths are relative to the work directory of the Runner (which is either `[workspace]/cms/cms-server/` or `[workspace]/cms-oss/cms-oss-server/`).

All configuration files in `.yml` format are placed in the directory `[workspace]/cms/cms-server/config/custom/` or `[workspace]/cms-oss/cms-oss-server/config/custom`.

Setup checks (after starting the Runner):
* Check runner log for error messages (errors reading configuration or connecting to the DB)
* http://localhost:8080/alohaeditor/DEV/lib/aloha.js must return the aloha.js File
* http://localhost:8080/alohaeditor/DEV/css/aloha.css must return the aloha.css File (requires Aloha Editor to be built)
* http://localhost:8080/alohaeditor/DEV/plugins/gcn/gcn/lib/gcn-plugin.js must return the CMS Integration Plugin File
* http://localhost:8080/gcnjsapi/DEV/debug/gcnjsapi.js must return the CMS JS Library File (requires the cms-js-lib to be built)
* http://localhost:8080/ct-link-checker/ must return the link checker customer tool (requires the tool to be built)

## Running the UIs

First the proxy for the UIs must be set up (so that the UIs can access the REST API and other resources served from the Runner):

```
cd [workspace]/cms-oss/cms-ui
cp proxy.conf.json.example proxy.conf.json
```

Optionally adapt the file `cms-oss/cms-ui/proxy.conf.json`.

Then the UIs can be run via

```
cd [workspace]/cms-oss/cms-ui/
npm start editor-ui
```

and

```
cd [workspace]/cms-oss/cms-ui/
npm start admin-ui
```

## Running Tests

Most tests require a backend database, which is provided by the testdb manager. It is recommended to have the testdb manager running locally, e.g. with docker compose:

docker-compose.yml:
```
version: '3.4'
services:
  app:
    image: docker.apa-it.at/gentics/apps/gcn-testdb-manager:master
    stop_grace_period: 30s
    environment:
      - GCN_TESTDB_MANAGER_PORT=8080
      - GCN_TESTDB_MANAGER_MYSQL_HOST=[host of your MariaDB installation]
      - GCN_TESTDB_MANAGER_MYSQL_PORT=[post of your MariaDB installation]
      - GCN_TESTDB_MANAGER_MYSQL_USER=[user of your MariaDB installation]
      - GCN_TESTDB_MANAGER_MYSQL_PASSWORD=[password of your MariaDB installation]
      - GCN_TESTDB_MANAGER_GCNDB_LIMIT=1
      - GCN_TESTDB_MANAGER_GCNDB_POOL_CAPACITY=1
      - GCN_TESTDB_MANAGER_FILLER_THREADS=1
      - GCN_TESTDB_MANAGER_DEBUG=true
    ports:
      - '7070:8080'
```

Note: The testdb manager will forward the configured hostname and port of the MariaDB installation to the tests, which will use those data to connect to the database.

To tell the tests, how to connect to the testdb manager, the following environment variables need to be set:

| env                  | Description                    | Example   |
| -------------------- | ------------------------------ | --------- |
| TESTMANAGER_HOSTNAME | Hostname of the testdb manager | localhost |
| TESTMANAGER_PORT     | Post of the testdb manager     | 7070      |

# Building the Changelog locally

The changelog consists of entries in `cms-oss/cms-oss-changelog/src/changelog/entries` (for changes which apply to the CMS OSS) and `cms/cms-changelog/src/changelog/entries` (for changes which apply only to the Commercial CMS).

Therefore, building the changelog locally has to be done in two steps:

## Step 1 - CMS OSS Changelog

The CMS OSS Changelog has to be **installed** into the local maven repository, because the CMS Changelog has a dependency on the deployment artifacts.

```
cd [workspace]/cms-oss
mvn -pl cms-oss-changelog clean install
```

This will build the changelog (in `cms-oss/cms-oss-changelog/target/output/merged_changelog.html`) and install the artifacts into the local repository.

## Step 2 - CMS Changelog

Then the CMS Changelog can be **packaged** (no need to install it too).

```
cd [workspace]/cms
mvn -pl cms-changelog clean package
```

The combined changelog can then be checked from `cms/cms-changelog/target/output/merged_changelog.html`.

It is also possible to generate a "real" (non-snapshot) changelog by setting the property `-Dchangelog.version`.

```
cd [workspace]/cms
mvn -pl cms-changelog clean package -Dchangelog.version=6.0.0
```

Important: Doing this will generate a mapping file for that version (under `cms-changelog/src/changelog/mappings`), which must **not** be checked into the git repository (unless you know exactly, what you are doing)!

If some changelog entries are missing in the generated changelog, check the directory `cms-changelog/src/changelog/mappings` for generated mapping files of snapshot versions (and remove them). The changelog manager plugin will not add entries to the changelog for a specific version, if the entry is found in a mapping file of a different version (and versions `6.0.0` and `6.0.0-SNAPSHOT` **are** different).

# Building the Guides locally

## CMS OSS Guides

The CMS OSS Guides can be built in a container with the following command (path to the workspace needs to be inserted):

```
docker run --rm -v [workspace]/cms-oss/cms-oss-doc/src/main:/guides --entrypoint=/bin/bash -it --user root docker.apa-it.at/gentics/jenkinsbuilds/jenkins-slave -c "cd /guides ; guides build"
```

The generated guides can be found at `cms-oss/cms-oss-doc/src/main/output`.

## CMS Guides

Building the CMS Guides locally requires three steps:

First, the artifacts containing the assets of the guides need to be installed locally:

```
docker run --rm -v [workspace]/cms-oss/cms-oss-doc/src/main:/guides --entrypoint=/bin/bash -it --user root docker.apa-it.at/gentics/jenkinsbuilds/jenkins-slave -c "cd /guides ; guides build"
cd [workspace]/cms-oss
mvn -pl cms-oss-doc -am clean install -Dguides.exec.skip -Dskip.unit.tests
```

Then, the maven module needs to be built (without actually building the guides):

```
cd [workspace]/cms
mvn -pl cms-doc -am clean package -Dguides.exec.skip
```

Now the guides can be built with

```
docker run --rm -v [workspace]/cms/cms-doc/target:/guides --entrypoint=/bin/bash -it --user root docker.apa-it.at/gentics/jenkinsbuilds/jenkins-slave -c "cd /guides ; guides build"
```

After that, build the package again
```
mvn -pl cms-doc -am package -Dguides.exec.skip 
```
The generated guides can be found at `cms/cms-doc/target/cms-doc-*.zip`.

# Gentics CMS User Interface Monorepo

This mono repo contains all the user interfaces and libraries interacting with the CMS.
Management is done via [NX](https://nx.dev) but is also partially integrated with maven.

## Package repository settings

Setup @gentics scope to the Gentics repository:
```bash
npm config set @gentics:registry https://repo.gentics.com/repository/npm-products/
```

If you wish/need to push new releases, you also need to authenticate to the repository.
To do this, do the following:

1. Since our repository doesn't authenticate with the NPM Tokens however, we need to create the auth value ourself:
    ```bash
    echo -n "{username}:{password}" | base64 -
    ```
2. Edit your global `.npmrc` file (should be `~/.npmrc`), and add the following at the end:
    ```
    @gentics:registry=https://repo.gentics.com/repository/npm-products/
    //repo.gentics.com/repository/npm-products/:_auth={base64Auth}
    ```

## Commands

Since this repo is managed via `nx`, all commands will end up be run via `nx` (See `scripts` inside of `package.json`).
Therefore, all commands can also be executed via [`nx run-many`](https://nx.dev/nx-api/nx/documents/run-many), to run them for multiple projects instead of one specific one.

All commands can be extended by passing flags to the underlying scripts. To do so, split the command with `--` then write the desired flags.
See [official documentation](https://docs.npmjs.com/cli/v7/commands/npm-run-script).

```bash
npm {command} -- -f --flag=true
```

---

### Install dependencies

```bash
npm install
```

### Start/Serve an application

**NX Command**: `serve`

Starts a development server for the project and will run on `localhost:4200` by default.

Be sure to configure the `proxy.conf.json` correctly first (See `proxy.conf.json.example`).

```bash
npm start <app-name>
```

### Unit-Tests

**NX Command**: `test`

Executes the Unit-Tests for the specified project

To run the unit tests and then terminate the test process:

```bash
npm test <app-name/lib-name>
```

To run the unit tests in watch mode (rebuild and rerun on every change):

```bash
npm run test <app-name/lib-name> -- --configuration=watch
```

**Available Configurations**:

* `watch`: To re-run the tests when source- or test-files have been edited/saved.
* `ci`: When run in a CI Server and includes test coverage reporting

### E2E/Integration Tests

**NX Command**: `e2e`

Starts the Cypress E2E/Integration tests with the specific e2e application (`{application-name}-e2e`).

```bash
npm run e2e <e2e-app-name>
```

### Components Tests

**NX Command**: `component-test`

Starts the Cypress Components tests for the specific application or library.

*Note*: When running multiple tests, disable `parallel` with `--parallel=false`, as it spawns a server for the tests which binds to a port.
When multiple try to run, it will fail due to the already bound port.


```bash
npm run component-test <app-name/lib-name>
```

**Available Configurations**:

* `watch`: To start the cypress UI and re-running the tests when source- or test-file have been edited/saved.
* `ci`: When run in a CI Server

### Build an application or library

**NX Command**: `build`

Builds the project and outputs ready to use/serve output files.

```bash
npm run build <app-name/lib-name>
```

**Available Configurations**:

* `production`: *Default*, production ready output
* `development`: Builds it with source-maps and non-optimized content to make debugging easier.
* `ci`: When run in a CI Server


## Tags

NX supports [tagging](https://nx.dev/nx-api/devkit/documents/ProjectConfiguration#tags) of all it's projects and being able to [filter the projects](https://nx.dev/nx-api/nx/documents/run-many) when running multiple commands.

Tags that are used in this Repository:

* `lib`: A library
* `models`: Only contains models/type defintions
* `angular`: Uses angular
* `publish`: Project which can be published
* `app`: An application
* `ui`: Main User-Interface/Standalone application
* `ct`: Custom-Tool which only works with/in the `editor-ui`
* `e2e`: End-to-End/Integration Test project
* `docs`: Documentation project to document one or more libraries/projects
* `demo`: Demo application to showcase the functionality of a library/project

### Structure

This repository using the Nx library together with Angular CLI. Therefore applications and libraries are separated into their dedicated folders `apps` and `libs`.

Custom TypeScript types are placed in the `typings` folder and can be included as other types with `tsconfig.json` files on package level or globally.

### Manage dependencies

There is only a single `package.json` file which contains all packages dependencies. This means if a package needs a new dependency it needs to be added to the root `package.json`.

Also updating these dependencies affects all packages, so after an update, they need to be tested and fixed on breaking changes.

Libraries having their own `package.json` only for packaging purposes, but it's not used for development or to build them.

### Add new application

These commands can be used to create new applications in the monorepo. It will generate the application structure and updates the repository configurations.

**Adds a new Angular application with Karma tests and Protractor e2e, using SCSS and Angular routing:**

```bash
npm run nx -- g @nx/angular:app <app-name> --routing --unit-test-runner=karma --e2e-test-runner=protractor --style=scss
```

### Add new library

These commands can be used to create new libraries in the monorepo. It will generate the library structure and updates the repository configurations.

**Adds a new Angular library with Karma tests, using SCSS:**

```bash
npm run nx -- g @nx/angular:lib <lib-name> --unit-test-runner=karma --style=scss
```

## Packages

### Editor UI

**Name:** editor-ui

**Type:** Application

**Readme**: [apps/editor-ui/README.md](apps/editor-ui/README.md)

### Admin UI

**Name:** admin-ui

**Type:** Application

**Readme**: [apps/admin-ui/README.md](apps/admin-ui/README.md)

### Custom Tool: Link Checker

**Name:** ct-link-checker

**Type:** Application

**Readme**: [apps/ct-link-checker/README.md](apps/ct-link-checker/README.md)

### CMS Models

**Name:** cms-models

**Type:** Library

**Readme**: [libs/cms-models/README.md](libs/cms-models/README.md)

This Angular workspace also contains a [library project](./libs/cms-models/) containing all CMS model types used in the Gentics CMS User Interface and the Gentics CMS Admin User Interface.
The library is published on the Gentics interal Artifactory server and can be added to a project using `npm` after logging in.
For details regarding this and the model types, please see the [README](./libs/cms-models/README.md) file.

To maintain compatibility with legacy code, the default `ModelType` for [normalizable interfaces](./libs/cms-models/README.md#normalizable-model-types), has been changed to `AnyModelType` when re-exporting the types from `common/models`.

### CMS Rest Clients

**Name:** cms-rest-clients-angular

**Type:** Library

**Readme**: [libs/cms-rest-clients-angular/README.md](libs/cms-rest-clients-angular/README.md)

Deprecated implementation of the CMS Rest Client. Use the `cms-rest-client`/`cms-rest-client-angular` libraries instead.

### CMS Rest Client

**Name:** cms-rest-client

**Type:** Library

**Readme**: [libs/cms-rest-client/README.md](libs/cms-rest-client/README.md)

General purpose CMS Rest Client for TypeScript/JavaScript.

### CMS Rest Client Angular

**Name:** cms-rest-client-angular

**Type:** Library

**Readme**: [libs/cms-rest-client-angular/README.md](libs/cms-rest-client-angular/README.md)

Angular wrapper of the CMS Rest Client to work with it easier in angular projects.

### UI Core

**Name**: ui-core

**Type**: Library

**Readme**: [libs/ui-core/README.md](libs/ui-core/README.md)

Previously as [standalone project](https://github.com/gentics/gentics-ui-core/) available, now integrated in the Monorepo.
Provides general purpose ui components to create custom applications.

## Coding Style

-   Follow the rules defined by the [ESLint](https://eslint.org/) configuration where applicable.
-   Always use relative paths for TypeScript import statements if it's within the same project (in Visual Studio Code you can use the setting `"typescript.preferences.importModuleSpecifier": "relative"`).
    If it's from another package/library in this monorepo, simply refer to it with the official package name, i.E. `@gentics/cms-models`.

## Git & Release Workflow

For a documentation of the release process and the necessary branching workflow, refer to the [Git Workflow](GIT_WORKFLOW.md).

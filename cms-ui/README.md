# Gentics CMS User Interface Monorepo

This mono repo contains all the user interfaces and libraries interacting with the CMS.

## Quick start

- [Editor UI a.k.a Gentics CMS UI Readme](apps/editor-ui/README.md)
- [Admin UI a.k.a Gentics CMS Admin UI Readme](apps/admin-ui/README.md)

## General information

### Commands

**Install dependencies:**
```bash
npm install
```

**Start or serve an application:**
```bash
npm start <app-name>
```
The application will run on `localhost:4200` by default.

**Test an application or library:**

To run the unit tests and then terminate the test process:
```bash
npm test <app-name/lib-name>
```
The test will run for the selected application or library. It is possible to run tests against all applications and libraries without specifying any name.

To run the unit tests in watch mode (rebuild and rerun on every change):
```bash
npm run test:watch <app-name/lib-name>
```

**Build an application or library:**
```bash
npm run build <app-name/lib-name>
```

This will build the selected application or library in `production` mode. All the built apps/libs destination is the `./dist` folder.

**Adding flags to the commands:**
```bash
npm test <app-name/lib-name> -- --flag1 --flag2
```
All commands can be extended by passing flags to the underlying scripts. To do so, split the command with `--` then write the desired flags.

### Structure

This repository using the Nx library together with Angular CLI. Therefore applications and libraries are separated into their dedicated folders `apps` and `libs`.

Custom TypeScript types are placed in the `typings` folder and can be included as other types with `tsconfig.json` files on package level or globally.

```
contentnode/contentnode-ui
+-- apps
+--- admin-ui
+--- admin-ui-e2e
+--- editor-ui
+--- editor-ui-e2e
+--- ct-link-checker
+--- ct-link-checker-e2e
+--- image-editor-demo
+--- ui-core-docs
+-- ci
+-- libs
+--- cms-components
+--- cms-models
+--- cms-rest-clients-angular
+--- form-generator
+--- image-editor
+--- ui-core
+-- typings
+-- nx.json
+-- angular.json
+-- package.json
+-- tsconfig.json
+-- README.md
```

### Manage dependencies

There is only a single `package.json` file which contains all packages dependencies. This means if a package needs a new dependency it needs to be added to the root `package.json`.

Also updating these dependencies affects all packages, so after an update, they need to be tested and fixed on breaking changes.

Libraries having their own `package.json` only for packaging purposes, but it's not used for development or to build them.

### Add new application

These commands can be used to create new applications in the monorepo. It will generate the application structure and updates the repository configurations.

**Adds a new Angular application with Karma tests and Protractor e2e, using SCSS and Angular routing:**
```bash
nx g @nrwl/angular:app <app-name> --routing --unit-test-runner=karma --e2e-test-runner=protractor --style=scss
```

### Add new library

These commands can be used to create new libraries in the monorepo. It will generate the library structure and updates the repository configurations.

**Adds a new Angular library with Karma tests, using SCSS:**
```bash
nx g @nrwl/angular:lib <lib-name> --unit-test-runner=karma --style=scss
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

### CMS Admin Rest Clients

**Name:** cms-admin-rest-clients-angular

**Type:** Library

**Readme**: [libs/cms-admin-rest-clients-angular/README.md](libs/cms-admin-rest-clients-angular/README.md)

### Custom Tool: Link Checker

**Name:** ct-link-checker

**Type:** Application

**Readme**: [apps/ct-link-checker/README.md](apps/ct-link-checker/README.md)

## Coding Style

* Make sure that your code editor adheres to the [.editorconfig](./.editorconfig) file. For many editors there are extensions for automatically importing that file (for Visual Studio Code use [EditorConfig for VS Code](https://marketplace.visualstudio.com/items?itemName=EditorConfig.EditorConfig)).
* Always use relative paths for TypeScript import statements (in Visual Studio Code you can use the setting `"typescript.preferences.importModuleSpecifier": "relative"`).

## Package repository settings

Authenticate:
```bash
npm adduser --registry=https://repo.apa-it.at/artifactory/api/npm/gtx-npm/ --always-auth
```

Setup @gentics scope to the APA IT repository:
```bash
npm config set @gentics:registry https://repo.apa-it.at/artifactory/api/npm/gtx-npm/
```

## Git & Release Workflow

For a documentation of the release process and the necessary branching workflow, refer to the [Git Workflow](GIT_WORKFLOW.md).

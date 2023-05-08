# @gentics/cms-rest-clients-angular

This library contains Angular services for communicating with the GCMS [REST API](https://www.gentics.com/Content.Node/guides/#rest-api).

It supplies one central service for accessing common features of the REST API: **GcmsApi**.

For developers, who want to write their own Angular services, this package provides the **ApiBase** service, which performs basic error handling and is used internally by the classes that make up the **GcmsApi**.

## Package repository settings

Authenticate:
```bash
npm adduser --registry=https://repo.apa-it.at/artifactory/api/npm/gtx-npm/ --always-auth
```

Setup @gentics scope to the APA IT repository:
```bash
npm config set @gentics:registry https://repo.apa-it.at/artifactory/api/npm/gtx-npm/
```

## Installing

You need to authenticate before installing the package!

Run `npm i --save @gentics/cms-rest-clients-angular` in your project's folder.

Then import the `GcmsRestClientsAngularModule` in your app's main module.
Note that you need to provide values for the `GCMS_API_BASE_URL` and `GCMS_API_SID` injection tokens, and optionally for the `GCMS_API_ERROR_HANDLER` injection token.

```TypeScript
// app.module.ts
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { GcmsRestClientsAngularModule, GCMS_API_BASE_URL, GCMS_API_ERROR_HANDLER, GCMS_API_SID } from '@gentics/cms-rest-clients-angular';
import { API_BASE_URL } from '../common/utils/base-urls';
import { ApplicationStateService } from '../state';

@NgModule({
    // ...
    imports: [
        FormsModule,
        GcmsRestClientsAngularModule,
        ReactiveFormsModule
    ],
    providers: [
        ApplicationStateService,
        MyErrorHandlerService,
        // ...

        // Provide a value for the GCMS REST API's base URL.
        { provide: GCMS_API_BASE_URL, useValue: API_BASE_URL },

        // Provide an observable for the GCMS session ID using the app state.
        {
            provide: GCMS_API_SID,
            useFactory: (appState: ApplicationStateService) => appState.select(state => state.auth.sid),
            deps: [ ApplicationStateService ]
        },

        // (Optional) Provide an error handler service.
        // The default error handler passes errors to the ErrorHandler from @angular/core.
        { provide: GCMS_API_ERROR_HANDLER, useClass: MyErrorHandlerService },
    ]
}
export class AppModule { }
```

## GCMS Model Types

GCMS model types need to be imported from the `@gentics/cms-models` package, e.g., `import {Page} from '@gentics/cms-models';`.
They must not be imported using a relative path, because otherwise the package build would fail.
The path for `@gentics/cms-models` is redefined in the local `tsconfig.json` of this project, such that it points to the compiled files in the `dist` folder of the workspace.
This means that the `@gentics/cms-models` package needs to be built before working on or building the `@gentics/cms-rest-clients-angular` package.

## Build

Run `npm run build:rest-clients` to build the project.
This will build the `@gentics/cms-models` package first and then the `@gentics/cms-rest-clients-angular` package.
The build artifacts will be stored in the `dist/libs/` directory.

## Testing package locally

Do a build and run `npm link` in the `dist/libs/cms-rest-clients-angular/` folder. After that go to your project where you want to use this package and run `npm link @gentics/cms-rest-clients-angular`.

## Publishing

You need to authenticate before publishing the package!

After building your library with `npm run build:rest-clients`, go to the dist folder `cd dist/libs/cms-rest-clients-angular` and run `npm publish`.

## Running unit tests

Run `npm test cms-rest-clients-angular` to execute the unit tests via [Karma](https://karma-runner.github.io).

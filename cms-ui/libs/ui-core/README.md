# UI Core

Package for basic components which are used throughout multiple applications.
CMS-Specific implementations are in the `@gentics/cms-components` package.

## Using ui-core in a project

1. Install via npm:
    ```sh
    npm install @gentics/ui-core
    ```

2. Adopt `polyfills.ts`:
   1. For new Angular Projects, this is not defined anymore. Follow this first:
      1. Create a new file `src/polyfills.ts`
      2. Add it to the build config `angular.json`: `projects.{projectName}.architect.build.options.polyfills`, and add `"src/polyfills.ts"`.
   2. Add the following to the `polyfills.ts` file:
    ```ts
    // This is necessary, because GUIC uses the Intl library, which requires a global object (like in Node.js).
    (window as any).global = window;
    ```
3. Add style pre-processor options:
    ```json
    // angular.json
    {
        "projects": {
            "{projectName}": {
                "architect": {
                    "build": {
                        "options": {
                            /* Add/merge these */
                            "stylePreprocessorOptions": {
                                "includePaths": [
                                    "node_modules"
                                ]
                            }
                        }
                    }
                }
            }
        }
    }
    ```
5. Add the styles to your project:
    ```scss
    // styles.scss
    @import "@gentics/ui-core/src/styles/variables";
    @import "@gentics/ui-core/src/styles/mixins";
    @import "@gentics/ui-core/src/styles/core";
    ```
6. Add `jQuery` as type to your application:
    ```json
    // tsconfig.app.json
    {
    //...
        "compilerOptions": {
            "types": [ "node" /* may include any other types */, "jquery"]
        }
    }
    ```
7. Import the module and add it to your app's root module:

```TypeScript
// app.module.ts
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { GenticsUICoreModule } from '@gentics/ui-core';

@NgModule({
    // ...
    imports: [
        FormsModule,
        GenticsUICoreModule.forRoot(),
        ReactiveFormsModule
    ]
}
export class AppModule { }
```

7. Add the [`<gtx-overlay-host>`](https://gentics.github.io/gentics-ui-core/#/overlay-host) component to your AppComponent's template if you want to use components that have overlay parts:

```HTML
<!-- app.component.html -->
<!-- ... -->
<gtx-overlay-host></gtx-overlay-host>
```

## Lazy Loading of Routes

If you are using [lazy loading of routes](https://angular.io/guide/lazy-loading-ngmodules),  the singleton services need to be provided again in the lazily loaded module, because otherwise they will not be found. For example:

```TypeScript
// my-lazily-loaded.module.ts
// ...
@NgModule({
    // ...
    providers: [
        ModalService,
        OverlayHostService
        // ...
    ]
})
export class MyLazilyLoadedModule {
    // ...
}
```

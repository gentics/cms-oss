# Gentics UI Core

This is the common core library for all CMS Libraries and Applications.

## Using ui-core in a project

1. Install via npm:

```
npm install gentics-ui-core --save
```

> Note that npm will say that the `foundation-sites` package, which is a dependency of UI Core, requires a peer dependency of `jquery` and `what-input`. For the usage of foundation-sites within gentics-ui-core these peer dependencies are not needed.

2. Import the module and add it to your app's root module:

```ts
// app.module.ts
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { GenticsUICoreModule } from 'gentics-ui-core';

@NgModule({
    // ...
    imports: [
        FormsModule,
        ReactiveFormsModule,
        GenticsUICoreModule.forRoot(),
    ]
}
export class AppModule { }
```

3. Add the [`<gtx-overlay-host>`](https://gentics.github.io/gentics-ui-core/#/overlay-host) component to your AppComponent's template if you want to use components that have overlay parts:

```html
<!-- app.component.html -->
<!-- ... -->
<gtx-overlay-host></gtx-overlay-host>
```
## Lazy Loading of Routes

If you are using [lazy loading of routes](https://angular.io/guide/lazy-loading-ngmodules),  the singleton services need to be provided again in the lazily loaded module, because otherwise they will not be found. For example:

```ts
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

## Using ui-core in an [angular-cli](https://cli.angular.io/) project

1. Create a new app using the angular CLI.
The following command will create a new app with the name `my-example` in the folder `./my-example`, use `me` as the prefix for components, set up a routing module, and use SCSS for defining styles. Please note that while a custom prefix and the routing module are optional, SCSS must be used for the styles in order to be compatible with Gentics UI Core.

```sh
ng new my-example --prefix=me --routing=true --style=scss
```

2. Add the following assignment to `polyfills.ts`:

```ts
/***************************************************************************************************
 * APPLICATION IMPORTS
 */
// This is necessary, because GUIC uses the Intl library, which requires a global object (like in Node.js).
(window as any).global = window;
```

3. Follow the steps from [Using ui-core in a project](#using-ui-core-in-a-project).

4. Add the following paths to your angular project configuration to load the styles correctly: `angular.json` -> `projects.<project-name>.architect.[build|test].options`:

```json
{
    // ...
    "stylePreprocessorOptions": {
        "includePaths": [
            "libs",
            "node_modules"
        ]
    }
    // ...
}
```

1. Add the following imports to your global styles SCSS:

```scss
// styles.scss
@import "gentics-ui-core/src/styles/variables";
@import "gentics-ui-core/src/styles/mixins";
@import "gentics-ui-core/src/styles/core";

// ...
```

You can see the [_variables.scss](src/styles/_variables.scss) file for a list of variables, which you can override before importing the variables file.

## Documentation

Full documentation and examples are available at [https://gentics.github.io/gentics-ui-core/](https://gentics.github.io/gentics-ui-core/)

## Developer Guide

The following sections are intended for people, who want to contribute to Gentics UI Core.

This library is part of the Gentics CMS UI Monorepo, and has to be build in the Monorepo root directory `(../..)`.
Management is done via NX and is further documented in the Monorepo root.

### Building the docs app

Although the UI Core is intended to be consumed in a raw (uncompiled) state,
there is a documentation app project setup in the Monorepo, called `ui-core-docs`.

Please refer to the Projects [README](../../apps/ui-core-docs/README.md) for more information.

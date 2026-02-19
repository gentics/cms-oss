# Auth Module

Optional extension of the CMS-Components which handle everything Authentification related.
Provides a module for the `ngxs`-Store under the `auth` namespace, where all relevant information
will be stored and updated.

Components and basic auth-workflow have yet to be moved to this package.
Therefore please see existing implementations in either `admin-ui` or `editor-ui`.

## Usage

For usage, simply import the `AuthModule` into your Module:

```ts
import { AuthModule } from '@gentics/cms-components/auth';

@NgModule({
    // ...
    imports: [
        // For child routes
        AuthModule,
        // For the root/core module
        AuthModule.forRoot(),
    ],
})
export class MyModule() {}
```


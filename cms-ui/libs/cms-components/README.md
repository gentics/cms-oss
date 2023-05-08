# CMS Components

This library contains common components and their dependencies for UIs.

## Usage

## CmsComponentsModule

To use the CmsComponents, you need to import it's module, and add to your @NgModule's `imports`. This is usually done in the application's CoreModule:

```typescript
import { CmsComponentsModule } from '@gentics/cms-components';

@NgModule({
    // Append the CmsComponentsModule to the imports list
    imports: [
        CmsComponentsModule
    ]
})
export class CoreModule {}
```

## Common AppState

CmsComponents uses the AppState of the embedding UI by an interface. This interface must be implemented in the UI, and it can be provided in a Module, which is mostly done in the CoreModule of the UI, eg.: in Admin UI:

```typescript
import { AppStateStrategy } from '@gentics/cms-components';

// Import the interface implementation in the UI
import { AdminUiAppStateStrategy } from '../state/app-state.strategy';

// Append to your providers array
const PROVIDERS: any[] = [
    {
        provide: AppStateStrategy,
        useClass: AdminUiAppStateStrategy,
        deps: [ AppStateService ],
    }
];
```

## Common i18n

CmsComponents uses the language service of the embedding UI, so you need to provide the CMS language. This is mostly done in the CoreModule of the UI, eg.: in Admin UI:

```typescript
import { GCMS_COMMON_LANGUAGE } from '@gentics/cms-components';

// Add this method in you CoreModule, where you provide it
export function createLanguageObservable(appState: AppStateService): Observable<UILanguage> {
    return appState.select(state => state.ui.language);
}

// Append to your providers array
const PROVIDERS: any[] = [
    {
        provide: GCMS_COMMON_LANGUAGE,
        useFactory: createLanguageObservable,
        deps: [ AppStateService ],
    }
];
```

### DemoComponent

There is a DemoComponent in this library, to show case some techniques for using generic features of all UIs.

```html
    <gtx-demo-component></gtx-demo-component>
```

## Running unit tests

Run `npm test cms-components` to execute the unit tests.


ToDo: Extend this documentation

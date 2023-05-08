# CMS Components

This library contains the Form Generator module.

## Usage

## Import in Parent Module

To use the Form Generator, you need to import it's module, and add to your @NgModule's `imports`. This is usually done in the application's CoreModule:

```typescript
import { CmsComponentsModule } from '@gentics/form-generator';

@NgModule({
    // Append the CmsComponentsModule to the imports list
    imports: [
        GtxFormGeneratorModule
    ]
})
export class CoreModule {}
```


## How to use in Markup

```html
    <gtx-form-editor
        [item]="formJSON"
        [activeUiLanguageCode]="'en'"
        [activeContentLanguageCode]="'de'"
        [formEditMode]="'edit'"
        (formModified)="yourContentHasBeenModifiedGn($event)"
    ></gtx-form-editor>
```

## Running unit tests

Run `npm run test:form-generator` to execute the unit tests.

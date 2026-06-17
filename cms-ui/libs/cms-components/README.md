# CMS Components

This library contains common components and services, for developing in the Gentics CMS context.

## Usage

### Modules

To use the CmsComponents, you need to import it's module, and add to your @NgModule's `imports`.
This is usually done in the application's CoreModule:

```typescript
import { NgModule } from '@angular/core';
import { CmsComponentsModule } from '@gentics/cms-components';

@NgModule({
    // Append the CmsComponentsModule to the imports list
    imports: [
        CmsComponentsModule.forRoot(),
    ],
})
export class CoreModule {}
```

***Important***:
You need to use the `forRoot` function in the root of your application module. Child modules may simply import the module *without*.

This package separates certain features into multiple entry points.
The following entrypoints exist as additional Modules, which can be loaded:

#### Authentification

The authentification module handles the single-sign-on functionality in the CMS and integrates into the state with `@ngxs/store`.

```typescript
import { NgModule } from '@angular/core';
import { CmsComponentsModule } from '@gentics/cms-components';
import { AuthModule } from '@gentics/cms-components/auth';

@NgModule({
    imports: [
        CmsComponentsModule.forRoot(),
        AuthModule.forRoot(),
    ],
})
export class CoreModule {}
```

#### Aloha

The Aloha module which contains components and integration services for handling Aloha-Editor in an IFrame.

```typescript
import { NgModule } from '@angular/core';
import { CmsComponentsModule } from '@gentics/cms-components';
import { AlohaModule } from '@gentics/cms-components/aloha';

@NgModule({
    imports: [
        CmsComponentsModule.forRoot(),
        AlohaModule.forRoot(),
    ],
})
export class CoreModule {}
```

## Common i18n

This library provides i18n functionality which is backed by `ngx-translate`. These include pipes, i18n variants of `@gentics/ui-core` services, and other useful utility components and functions.

Translations can be extended/added by adding these to the `TranslateService` from `@ngx-translate/core`. It is recommended to do so in the initialization process of the application or library, by using the `provideAppInitializer`:

```typescript
import { inject, NgModule, provideAppInitializer } from '@angular/core';
import { CmsComponentsModule } from '@gentics/cms-components';
import { TranslateService } from '@ngx-translate/core';

@NgModule({
    imports: [
        CmsComponentsModule.forRoot(),
        provideAppInitializer(() => {
            const translate = inject(TranslateService);
            translate.setTranslations('de', {
                example: {
                    foo: 'bar',
                    hello: 'Hallo {{ name }}!',
                }
            }, true);
        }),
    ],
})
export class CoreModule {}
```

### Pipes

The following pipes are provided to handle the translations:

#### `gtxI18n`

With the example translations from above:

```html
{{ 'example.foo' | gtxI18n }} <!-- bar -->
{{ 'example.hello' | gtxI18n:{ name: 'world' } }} <!-- Hallo world! -->
```

#### `gtxI18nDate`

Wrapper pipe for [`Intl.DateTimeFormat`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat), which allows for `Date` objects, timestamps and ISO strings to be formatted.
Formatting is only available for pre-defined formats (`KnownDateFormatName`), which are defined the following way:

```typescript
export type KnownDateFormatName = 'date' | 'time' | 'dateTime' | 'dateTimeDetailed' | 'longDate' | 'longTime' | 'longDateTime';

/** Known format options for browsers with Intl support */
export const KNOWN_FORMATS: IndexByKey<Intl.DateTimeFormatOptions> = {
    date: {
        day: 'numeric',
        month: 'numeric',
        year: 'numeric',
    },
    time: {
        hour: 'numeric',
        minute: 'numeric',
    },
    dateTime: {
        day: 'numeric',
        month: 'numeric',
        year: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
    },
    dateTimeDetailed: {
        day: 'numeric',
        month: 'numeric',
        year: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric',
    },
    longDate: {
        weekday: 'long',
        day: 'numeric',
        month: 'numeric',
        year: 'numeric',
    },
    longTime: {
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric',
    },
    longDateTime: {
        weekday: 'long',
        day: 'numeric',
        month: 'numeric',
        year: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric',
    },
};
```

These formats can then simply be selected as argument to the pipe

```typescript
const dateObj = new Date('2026-05-02T09:01');
const timestamp = dateObj.getTime();
const isoString = dateObj.toISOString();
```
```html
{{ dateObj | gtxI18nDate }} <!-- 5/2/2026 -->
{{ timestamp | gtxI18nDate:'longDate' }} <!-- Saturday, 5/2/2026 -->
{{ isoString | gtxI18nDate:'time' }} <!-- 9:01 AM -->
```


#### `gtxI18nDuration`

Wrapper pipe for [`Intl.DurationFormat`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DurationFormat), where the formatting options may be provided as argument to the pipe.

The pipe accepts either a duration object, or a time in milliseconds (or seconds) which will be converted into a duration object.

```typescript
interface Duration {
    years?: number;
    months?: number;
    weeks?: number;
    days?: number;
    hours?: number;
    minutes?: number;
    seconds?: number;
    milliseconds?: number;
    microseconds?: number;
    nannoseconds?: number;
}
```

```html
{{ {hours: 1, minutes: 23, seconds: 45} | gtxI18nDuration }} <!-- 1 hr, 23 min, 45 sec -->
{{ {hours: 1, minutes: 23, seconds: 45} | gtxI18nDuration:{ style: 'long'} }} <!-- 1 hour, 23 minutes, 45 seconds -->
{{ 200052000 | gtxI18nDuration }} <!-- 2 days, 2 hr, 34 min, 12 sec -->
{{ 200052 | gtxI18nDuration:true }} <!-- 2 days, 2 hr, 34 min, 12 sec -->
```


#### `gtxI18nNumber`

Wrapper pipe for [`Intl.NumberFormat`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat), where the formatting options can be provided as pipe argument.

```html
{{ 123456 | gtxI18nNumber }} <!-- 123,456 -->
{{ 123456 | gtxI18nNumber:{ style: "currency", currency: "EUR" } }} <!-- €123,456.00 -->
```


#### `gtxI18nObject`

Simple pipe for `I18nString` objects, which are defined like this

```typescript
type I18nString = Record<string, string>;
const obj: I18nString = {
    de: 'Text in deutsch',
    en: 'Text in english',
};
```

The pipe will then simply use the current language as key and returns the string value.

```html
<!-- Language is "en" -->
{{ obj | gtxI18nObject }} <!-- Text in english -->

<!-- Language is "de" -->
{{ obj | gtxI18nObject }} <!-- Text in deutsch -->
```

#### `gtxI18nRelativeDate`

Wrapper pipe for [`Intl.RelativeTimeFormat`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/RelativeTimeFormat), which automatically determines the relative time to now, and accepts `Date` objects, ISO strings, and timestamps.
Currently does not allow for customization of the formatting.

The value for the formatting will be calculated automatically, and will only use the highest available unit which has a value.

```typescript
const dateObj = new Date();
dateObj.setMonth(dateObj.getMonth() - 1);
```

```html
{{ dateObj | gtxI18nRelativeDate }} <!-- 1 month ago -->
```

```typescript
dateObj.setYear(dateObj.getYear() - 1);
```

```html
{{ dateObj | gtxI18nRelativeDate }} <!-- 1 year ago -->
```

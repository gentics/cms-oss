# @gentics/cms-models

This library contains TypeScript interfaces for

* the most commonly used data types supplied by the [GCMS REST API](https://www.gentics.com/Content.Node/guides/restapi/data.html)
* the types used by the repository browser

## Installing

You need to authenticate before installing the package!

Run `npm i --save @gentics/cms-models` in your project's folder.

You can then import the types from this package, e.g., `import { Page } from '@gentics/cms-models';`.

## Normalizable Model Types

> **DEPRECATED**: Please do not use any of the normalization features/utility.
> Normalization/mangling of the regular Rest models are application dependend, and should be done in the application layer.

Many Gentics apps store model objects, which may have nested model objects, in the application state in a normalized form obtained using [normalizr](https://github.com/paularmstrong/normalizr).
For such model types, their corresponding interfaces support both the **normalized** form stored in the app state and the **raw** form, which is returned by the REST API.

For example, a `Page` object has an `editor` property.
In a *raw* `Page` object this is a `User` object, while in a *normalized* `Page` object this is the ID of the user.
```TypeScript
const rawPage: Page<Raw> = {
    // In a raw page the editor is a User object.
    editor: { id: 10, firstName: 'John', lastName: 'Doe', ... }
    ...
};

const normalizedPage: Page<Normalized> = {
    // In a normalized page the editor is a number, i.e., the user's ID.
    editor: 10
    ...
};
```

Each model type that can be normalized, extends the `NormalizableEntity` interface, which provides a symbol property that is `true` if the object has been normalized.
```TypeScript
/**
 * Used to ease the identification of normalized entity types.
 */
export interface NormalizableEntity<T extends ModelType> {
    /**
     * Indicates if this entity has been normalized.
     *
     * This property is always `true` for normalized entities and
     * does not exist on raw entities.
     */
    [IS_NORMALIZED]?: Normalizable<T, false, true>;
}
```

`ModelType` is an enumeration that is used a type parameter to identify an interface as being *raw* or *normalized*.
For each enum member there is also a shorthand type alias.
```TypeScript
export enum ModelType {
    /** Indicates a raw model type, as returned by the REST API. */
    Raw,

    /** Indicates a normalized model type, as stored in the AppState. */
    Normalized
}

/** Shorthand for indicating a raw model type, as returned by the REST API. */
export type Raw = ModelType.Raw;

/** Shorthand for indicating a normalized model type, as stored in the AppState. */
export type Normalized = ModelType.Normalized;

/** Used to declare data model variables, which may be either raw or normalized. */
export type AnyModelType = ModelType.Raw | ModelType.Normalized;

/** The default `ModelType` that is used if none is specified. */
export type DefaultModelType = AnyModelType;
```

The `Normalizable` type alias is used to define the type of a property, based on the provided ModelType.
```TypeScript
export interface Page<T extends ModelType = DefaultModelType> extends InheritableItem<T> {
    /** 
     * For Page<Raw>, editor is of type User.
     * For Page<Normalized>, editor is of type number.
     * For Page<AnyModelType>, editor is of type User | number.
     */
    editor: Normalizable<T, User, number>;
    ...
}
```

The default model type, which is used if you do not specify a ModelType, when declaring a variable of a certain model type is `AnyModelType`, so a variable of type `Page` is the same as a variable of type `Page<AnyModelType>`.
If you prefer a different default model type in your application, you can import the model interfaces in a central file and export them from there with a different default ModelType, e.g.:
```TypeScript
// File /my-custom-project/common/models.ts
import { ModelType, Page, Raw } from '@gentics/cms-models';

// Export Page to use Raw as default ModelType.
// If we now import Page from 'common/models.ts', instead of from '@gentics/cms-models',
// it will use Raw by default.
export type Page<T extends ModelType = AnyModelType> = Page<T>;
```

To normalize or denormalize entities, the [GcmsNormalizer](./src/lib/models/gcms-normalizer/gcms-normalizer.ts) should be used.

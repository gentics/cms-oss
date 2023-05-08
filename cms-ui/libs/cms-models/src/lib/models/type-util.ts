/**
 * Represents a generic index type that maps keys to their values.
 */
export type Index<K extends string | number, V> = {
    [P in K]: V;
};

/**
 * Represents an index type that maps string keys to their values.
 */
export interface IndexByKey<V> {
    [key: string]: V;
}

/**
 * Represents an index type that maps numeric IDs to their values.
 */
export interface IndexById<V> {
    [id: number]: V;
}

/** Represents a completely recursive partial type. */
export type RecursivePartial<T> = {
    [K in keyof T]?: RecursivePartial<T[K]>
};

/**
 * Used to declare if a model type is being used in its raw form (as returned by the REST API)
 * or in its normalized form (as stored in the AppState).
 *
 * Raw model objects may contain nested entities, while normalized ones do not.
 * E.g., in a raw `Page` object the `publisher` is a `User` object,
 * while in a normalized `Page`, the `publisher` is a number, the ID of the publisher.
 */
export enum ModelType {
    /** Indicates a raw model type, as returned by the REST API. */
    Raw,

    /** Indicates a normalized model type, as stored in the AppState. */
    Normalized,
}

/** Shorthand for indicating a raw model type, as returned by the REST API. */
export type Raw = ModelType.Raw;

/** Shorthand for indicating a normalized model type, as stored in the AppState. */
export type Normalized = ModelType.Normalized;

/**
 * Used to declare data model variables, which may be either raw or normalized.
 *
 * This should be used for method parameters and input properties if a method or component can handle both types.
 * Avoid using this for return values.
 * If you want to use this for a return value, first consider making the method generic.
 * Example:
 * ```TypeScript
 * // Don't do this
 * doSomething(page: Page<AnyModelType>): Page<AnyModelType>
 *
 * // Consider doing this instead:
 * doSomething<T extends ModelType>(page: Page<T>): Page<T>
 * ```
 */
export type AnyModelType = ModelType.Raw | ModelType.Normalized;

/** The default `ModelType` that is used if none is specified. */
export type DefaultModelType = AnyModelType;

/**
 * Helper type used to define a property that has a raw and a normalized version.
 *
 * Example:
 *  ```TypeScript
 *  interface Page<T extends ModelType = Raw> {
 *      // ...
 *       publisher?: Normalizable<T, User, number>;
 *  }
 *  ```
 * For `Page<Raw>` the type of `publisher` will be `User`.
 * For `Page<Normalized> the type of `publisher` will be `number`.
 *
 * @param T The `ModelType` of the enclosing model type.
 * @param R The type to be used if T is `ModelType.Raw`.
 * @param N The type to be used if T is `ModelType.Normalized`.
 */
export type Normalizable<T, R, N> = T extends Raw ? R : N;

/** The Symbol used to store the information whether an entity is normalized. */
export const IS_NORMALIZED = Symbol('Entity.isNormalized');

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

/**
 * Exclude property from type.
 * @note In TypeScript 3.5, the Omit type was added to the standard library. Remove when updated.
 *
 * @example
 *    interface Test {
 *        a: string;
 *        b: number;
 *        c: boolean;
 *    }
 *
 *    // Omit a single property:
 *    type OmitA = Omit<Test, 'a'>; // Equivalent to: {b: number, c: boolean}
 *
 *    // Or, to omit multiple properties:
 *    type OmitAB = Omit<Test, 'a'|'b'>; // Equivalent to: {c: boolean}
 */
export type Omit<T, K extends keyof T> = Pick<T, Exclude<keyof T, K>>;

/**
 * Preprocessor function for the options of normalizr schemas.
 */
export const normalizrPreProcessEntity = (entity: any) => {
    return {
        ...entity,
        [IS_NORMALIZED]: true,
    } as NormalizableEntity<Normalized>;
};

/**
 * GCMSUI component `item-list-row` has different states
 * causing different component behavior.
 */
export enum ItemListRowMode {
    DEFAULT = 'DEFAULT',
    SELECT = 'SELECT',
}

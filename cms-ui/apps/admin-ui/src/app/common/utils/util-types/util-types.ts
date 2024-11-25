// This file contains useful utility types used throughout the application.

/**
 * Shorthand for declaring an interface with the same public members as a class.
 * This is useful for mocking in unit tests.
 */
export type InterfaceOf<T> = { [K in keyof T]: T[K]; };

/**
 * Used to declare a property, which has to be set to the class (i.e., the constructor) of a certain type.
 *
 * Example:
   ```
   // This needs to be set to a class, which implements the Resolve<BreadcrumbInfo> interface.
   breadcrumbResolver: ConstructorOf<Resolve<BreadcrumbInfo>>;
   ```
 */
export interface ConstructorOf<T> { new(...args: any[]): T; }

/**
 * Makes the keys specified as the second type parameter optional.
 *
 * @example
 * // Make the properties `nodeId` and `editedBy` optional.
 * WithOptional<Page, 'nodeId' | 'editedBy'>
 *
 * @see https://github.com/Microsoft/TypeScript/issues/25760
 */
export type WithOptional<T, K extends keyof T> = Omit<T, K> & Partial<Pick<T, K>>;

/** Convert string array to type */
export function asLiterals<T extends string>(arr: T[]): T[] { return arr; }

/**
 * Definition for the callback function passed as arugment to `ControlValueAccessor.registerOnTouched()`.
 */
export type FormControlOnTouchedFn = () => void;

/**
 * Definition for the callback function passed as arugment to `ControlValueAccessor.registerOnChange()`.
 */
export type FormControlOnChangeFn<T> = (formValue: T) => void;

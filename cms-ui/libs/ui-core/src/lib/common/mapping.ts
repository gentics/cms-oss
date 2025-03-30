import { SimpleChange, SimpleChanges } from '@angular/core';

// Used for ...?
export type MappingFn = (value: any, ...params: any[]) => any;

/**
 * Shorthand for the type of the parameter supplied to `ngOnChanges()`.
 *
 * @example
 * ```
 * ngOnChanges(changes: ChangesOf<this>): void {
 *     ...
 * }
 * ```
 */
export type ChangesOf<T> = { [K in keyof T]?: SimpleChange } | SimpleChanges;

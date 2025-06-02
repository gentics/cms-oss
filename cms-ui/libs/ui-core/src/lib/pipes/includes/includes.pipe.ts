import { Pipe, PipeTransform } from '@angular/core';

type EqualityFn = (a: any, b: any, strict: boolean) => boolean;

interface IncludesOptions {
    values: any[] | Set<any>;
    strict?: boolean;
    fn?: EqualityFn;
}

interface BasicObject {
    [key: string | number | symbol]: any;
}

type SourceValue = BasicObject | Array<any> | Set<any>;

export const DEFAULT_COMPARE_FN: EqualityFn = (a, b, strict) => {
    // Null-Checks are always not strict
    if (a == null && b == null) {
        return true;
    }

    // eslint-disable-next-line eqeqeq
    return strict ? a === b : a == b;
};

/**
 * Pipe which simply checks if any of the values is contained in the source value.
 * Usage:
 * ```
 * {{ [123, 'cool'] | includes:'foobar' }}
 * ```
 *
 * On default, it'll compare elements strictly with a `===` comparison.
 * You may specify the strict flag with the options, like so:
 * ```
 * {{ [123, 'cool'] | includes:{ strict: false, values:['foobar'] } }}
 * ```
 */
@Pipe({
    name: 'gtxIncludes',
    standalone: false
})
export class IncludesPipe implements PipeTransform {

    transform(sourceValue: SourceValue, optionsOrValues: IncludesOptions | Array<any> | Set<any>): boolean;
    transform(sourceValue: SourceValue, ...args: Array<any>): boolean {
        if (sourceValue == null || typeof sourceValue !== 'object') {
            return false;
        }

        if (args == null || !Array.isArray(args) || args.length === 0) {
            return false;
        }

        let valuesToCheck: any[] | Set<any> = args;
        let strict = true;
        let compareFn = DEFAULT_COMPARE_FN;
        let parsedOptions = false;

        // If there's only one parameter, it's an object, and has the proper
        // signature of the options, then treat it as such and load the options.
        if (valuesToCheck.length === 1
            && valuesToCheck[0] != null
            && typeof valuesToCheck[0] === 'object'
            && (Array.isArray(valuesToCheck[0].values) || (
                typeof valuesToCheck[0].values === 'object' &&
                valuesToCheck[0].value != null
            ))
        ) {
            const options = valuesToCheck[0] as IncludesOptions;
            valuesToCheck = options.values;
            strict = options.strict ?? strict;
            compareFn = options.fn ?? compareFn;
            parsedOptions = true;
        }

        // Validate/Convert the values to check to be a proper array of values
        if (!Array.isArray(valuesToCheck)) {
            if (valuesToCheck == null) {
                valuesToCheck = [null];
            } else {
                valuesToCheck = [valuesToCheck];
            }
        // catch potential args reordering/not spreading correctly: (`transform(['foo'], ['bar'])` -> `args = [['bar']]`)
        } else if (!parsedOptions && args.length === 1 && (Array.isArray(valuesToCheck[0]) || (
            (valuesToCheck[0] != null && valuesToCheck[0].__proto__ === Set.prototype)
        ))) {
            valuesToCheck = valuesToCheck[0];
        }

        if ((sourceValue as any).__proto__ !== Set.prototype && !Array.isArray(sourceValue)) {
            sourceValue = Object.keys(sourceValue);
        }

        // Compare the values
        for (const singleSourceValue of (sourceValue as Array<any>)) {
            for (const singleCheckValue of valuesToCheck) {
                if (compareFn(singleSourceValue, singleCheckValue, strict)) {
                    return true;
                }
            }
        }

        return false;
    }
}

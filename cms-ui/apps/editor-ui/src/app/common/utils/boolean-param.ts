/**
 * Allows using non-strict values for component inputs.
 *
 * @example
 *   <my-component [isPretty]="true"></my-component>
 *   <my-component isPretty="true"></my-component>
 *   <my-component isPretty="false"></my-component>
 *   <my-component isPretty></my-component>
 *   <my-component isPretty="1"></my-component>
 */
export function booleanParam(value: any): boolean {
    return value === '' || (!!value && value !== 'false');
}

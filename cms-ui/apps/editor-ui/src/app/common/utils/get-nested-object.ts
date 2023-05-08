/**
 * Safely access nested object properties
 */
export function getNestedObject(nestedObj: any, pathArr: Array<any>): any {
    return pathArr.reduce((obj, key) =>
        (obj && obj[key] !== 'undefined') ? obj[key] : undefined, nestedObj);
}

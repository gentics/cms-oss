type PathSegment = string | symbol;

// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function getValueByPath(value: any, path: PathSegment | PathSegment[]): any {
    if (value == null || path == null) {
        return value;
    }

    if (typeof path === 'string') {
        path = path.includes('.') ? path.split('.') : [path];
    } else if (typeof path === 'symbol') {
        path = [path];
    }

    if (Array.isArray(value) && path.length > 0) {
        return value.map(arrValue => getValueByPath(arrValue, path));
    }

    for (const segment of path) {
        value = value?.[segment];
    }

    return value;
}

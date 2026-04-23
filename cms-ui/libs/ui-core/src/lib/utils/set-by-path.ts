// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function setByPath(root: any, path: string | string[], value: any): void {
    if (typeof path === 'string' && path) {
        path = [path];
    } else if (!Array.isArray(path)) {
        // If invalid path, skip
        return;
    } else {
        path = path.filter((segment) => typeof segment === 'string' && segment);
    }

    if (path.length === 0) {
        return;
    }

    let current = root;
    for (let i = 0; i < path.length - 1; i++) {
        let tmp = current[path[i]];
        if (tmp == null || typeof tmp !== 'object') {
            tmp = current[path[i]] = {};
        }
        current = tmp;
    }

    current[path[path.length - 1]] = value;
}

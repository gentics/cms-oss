import * as uuidv4 from 'uuidv4';

export function newUUID(): string {
    let fnOrRes = uuidv4;

    // Sanity check
    if (fnOrRes == null) {
        return null;
    }

    // This may happen in the final build, due to wrong tree-shaking/module-resolution.
    if (typeof fnOrRes === 'object') {
        fnOrRes = (fnOrRes as any).default;
    }
    if (typeof fnOrRes === 'function') {
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        fnOrRes = (fnOrRes as any)();
    }

    return typeof fnOrRes === 'string' ? fnOrRes : null;
}

import { InjectionToken } from '@angular/core';
export const environment = new InjectionToken('environment');

/**
 * In IE 11, the `console` object only exists after the devtools have been opened. Prior to that, any use of the
 * `console` object will result in an exception in IE. Although logging statements should not be in production code,
 * it is likely that some will slip through from time to time. This function prevents errors by patching the
 * `console` object API with noops.
 */
export function patchConsoleObject(): void {
    if (!window['console']) {
        const noop = (): void => {};
        let syntheticConsole = {
            assert: noop,
            clear: noop,
            count: noop,
            debug: noop,
            dir: noop,
            dirxml: noop,
            error: noop,
            group: noop,
            groupCollapsed: noop,
            groupEnd: noop,
            info: noop,
            log: noop,
            profile: noop,
            profileEnd: noop,
            time: noop,
            timeEnd: noop,
            trace: noop,
            warn: noop,
        };

        (window as any)['console'] = syntheticConsole as any;
    }
}


export function logErrors<T extends Function>(originalMethod: T, handle: (firstArg: any, error: Error) => string): T {
    return ((firstArg: any): any => {
        try {
            return originalMethod.apply(null, [ originalMethod, handle ]);
        } catch (err) {
            console.error('[debug] ' + handle(firstArg, err));
            throw err;
        }
    }) as any;
}

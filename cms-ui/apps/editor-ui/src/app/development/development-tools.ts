import {InjectionToken} from '@angular/core';

export const ENVIRONMENT_TOKEN = new InjectionToken('environment');

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
            warn: noop
        };

        (window as any)['console'] = syntheticConsole as any;
    }
}


function logErrors<T extends Function>(originalMethod: T, handle: (firstArg: any, error: Error) => string): T {
    return <any> function (firstArg: any): any {
        try {
            return originalMethod.apply(null, arguments);
        } catch (err) {
            console.error('[debug] ' + handle(firstArg, err));
            throw err;
        }
    };
}


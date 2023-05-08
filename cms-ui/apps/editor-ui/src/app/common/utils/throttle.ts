/**
 * Throttles a function call by the given number of milliseconds.
 * That is to say, the function may be invoked, and then cannot be invoked again until the limit
 * has passed, whereupon it may be invoked again, and so on.
 */
export function throttle<T extends Function>(callback: T, limitInMs: number): T {
    let wait = false;
    return function (...args: any[]): any {
        if (!wait) {
            callback.call(this, ...args);
            wait = true;
            setTimeout(() => wait = false, limitInMs);
        }
    } as any;
}

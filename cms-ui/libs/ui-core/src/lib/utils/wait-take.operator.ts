import { MonoTypeOperatorFunction, Observable } from 'rxjs';

/**
 * Operator which will delay the stream by the provided time.
 * However, if a new value is provided before the delay ended,
 * it'll discard the previous value and reset the timer.
 * Basically a `debounceTime`, but it resets it's timer.
 *
 * @param time The time in milliseconds to wait
 * @returns The latest value after the the timer finished.
 */
export function waitTake<T>(time: number): MonoTypeOperatorFunction<T> {
    return (source$: Observable<T>) => {
        return new Observable<T>(sub => {

            let timeoutId = null;
            let isComplete = false;

            function startAgain(value: T): void {
                if (timeoutId != null) {
                    clearTimeout(timeoutId);
                }
                timeoutId = setTimeout(() => {
                    sub.next(value);
                    if (isComplete) {
                        sub.complete();
                    }
                    timeoutId = null;
                }, time);
            }

            source$.subscribe({
                next: (val: T) => {
                    if (sub.closed || isComplete) {
                        return;
                    }
                    startAgain(val);
                },
                complete: () => {
                    if (sub.closed) {
                        return;
                    }
                    // If no timer is running, we can safely complete it.
                    // Otherwise it'll be completed on timer completion.
                    if (timeoutId == null) {
                        sub.complete();
                    }
                    isComplete = true;
                },
                error: (err) => {
                    if (sub.closed) {
                        return;
                    }
                    isComplete = true;
                    // In case something is still running
                    if (timeoutId != null) {
                        clearTimeout(timeoutId);
                        timeoutId = null;
                    }
                    sub.error(err);
                },
            })
        });
    };
}

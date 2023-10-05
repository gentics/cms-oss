import { MonoTypeOperatorFunction, Observable, SchedulerLike, Subscription, asyncScheduler } from 'rxjs';

/**
 * Operator which will delay the stream by the provided time.
 * However, if a new value is provided before the delay ended,
 * it'll discard the previous value and reset the timer.
 * Basically a `debounceTime`, but it resets it's timer.
 *
 * @param time The time in milliseconds to wait
 * @returns The latest value after the the timer finished.
 */
export function waitTake<T>(time: number, scheduler: SchedulerLike = asyncScheduler): MonoTypeOperatorFunction<T> {
    return (source$: Observable<T>) => {
        return new Observable<T>(sub => {

            let taskRunner: Subscription = null;
            let isComplete = false;

            source$.subscribe({
                next: (value: T) => {
                    if (sub.closed || isComplete) {
                        return;
                    }

                    if (taskRunner != null) {
                        taskRunner.unsubscribe();
                    }

                    taskRunner = scheduler.schedule(() => {
                        sub.next(value);
                        if (isComplete) {
                            sub.complete();
                        }
                        taskRunner = null;
                    }, time);
                },
                complete: () => {
                    if (sub.closed) {
                        return;
                    }

                    isComplete = true;

                    // If no timer is running, we can safely complete it.
                    // Otherwise it'll be completed on timer completion.
                    if (taskRunner == null) {
                        sub.complete();
                    }
                },
                error: (err) => {
                    if (sub.closed) {
                        return;
                    }

                    isComplete = true;

                    // In case something is still running
                    if (taskRunner != null) {
                        taskRunner.unsubscribe();
                        taskRunner = null;
                    }

                    sub.error(err);
                },
            })
        });
    };
}

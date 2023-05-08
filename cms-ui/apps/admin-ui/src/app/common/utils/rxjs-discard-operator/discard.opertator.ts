import { Observable, OperatorFunction, Subscriber } from 'rxjs';

type DiscardHandlerFn<T> = (value: T) => any;

/**
 * Creates a simple operator which discards any value provided to it.
 * Takes a handler which will be called if it exists with the value.
 * The result of the handler is also ignored/discarded.
 * Used for operations where the result should be consumed.
 *
 * @param handler Handler which may handle the value.
 * @returns A new discarding operator.
 */
export function discard<T>(handler?: DiscardHandlerFn<T>): OperatorFunction<T, void> {
    return (source$: Observable<T>) => {
        return new Observable<void>(sub => {
            source$.subscribe({
                next: ((val: T) => {
                    if (sub.closed) {
                        return;
                    }

                    if (typeof handler === 'function') {
                        try {
                            handler(val);
                            sub.next();
                        } catch (err) {
                            sub.error(err);
                        }
                    } else {
                        sub.next();
                    }
                }),
                complete: () => {
                    if (sub.closed) {
                        return;
                    }

                    sub.complete();
                },
                error: (err) => {
                    if (sub.closed) {
                        return;
                    }

                    sub.error(err);
                },
            });
        });
    }
}

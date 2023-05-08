import { tick } from '@angular/core/testing';
import { Observable, of as observableOf, Subscription } from 'rxjs';
import { delay, map, takeUntil } from 'rxjs/operators';
import { ObservableStopper } from '../../common/utils';

/**
 * Returns an observable that emits the specified data after a delay.
 * This is useful for mocking asynchronous responses.
 */
export function createDelayedObservable<T>(data: T, delayMsec: number = 0): Observable<T> {
    return observableOf(data).pipe(delay(delayMsec));
}

/**
 * Returns an observable that emits the specified error after a delay.
 * This is useful for mocking asynchronous errors.
 */
export function createDelayedError(error: Error, delayMsec: number = 0): Observable<never> {
    return observableOf(null).pipe(
        delay(delayMsec),
        map(() => { throw error; }),
    );
}

/**
 * Shorthand for subscribing to the source observable while
 * piping it into `takeUntil(stopper.stopper$)` first.
 */
export function subscribeSafely<T>(
    source$: Observable<T>,
    stopper: ObservableStopper,
    next: (value: T) => void,
    error?: (error: any) => void,
    complete?: () => void,
): Subscription {
    return source$.pipe(
        takeUntil(stopper.stopper$),
    ).subscribe(next, error, complete);
}

/**
 * Subscribes to the source observable, executes a `tick()` to make it emit, and
 * returns the emitted value or `undefined` if there was no emission.
 */
export function tickAndGetEmission<T>(source$: Observable<T>): T {
    let ret: T;
    const sub = source$.subscribe(value => ret = value);

    try {
        tick();
    } finally {
        sub.unsubscribe();
    }
    return ret;
}

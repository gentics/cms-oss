import { Observable, OperatorFunction } from 'rxjs';
import { finalize, switchMap } from 'rxjs/operators';
import { AppStateService, DecrementDetailLoading, IncrementDetailLoading } from '../../../state';

/**
 * Pipeable RxJS operator, which increments the detail loading counter in the state upon subscription
 * and decrements the detail loading counter in the state upon observable completion or error.
 *
 */
export function detailLoading<T>(appState: AppStateService, message?: string): OperatorFunction<T, T> {
    return (source$) => {
        return new Observable(subscriber => {
            const sourceObservable$ = source$.pipe(
                finalize(() => {
                    appState.dispatch(new DecrementDetailLoading());
                }),
            );

            const subscription = appState.dispatch(new IncrementDetailLoading(message))
                .pipe(
                    switchMap(() => sourceObservable$),
                ).subscribe({
                    complete: () => {
                        subscriber.complete();
                    },
                    error: (err) => {
                        subscriber.error(err);
                    },
                    next: (value) => {
                        subscriber.next(value);
                    },
                });

            return subscription;
        });
    };
 }

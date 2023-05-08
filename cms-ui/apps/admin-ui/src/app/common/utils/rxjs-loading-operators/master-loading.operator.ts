import { Observable, OperatorFunction } from 'rxjs';
import { finalize, switchMap } from 'rxjs/operators';
import { AppStateService, DecrementMasterLoading, IncrementMasterLoading } from '../../../state';

/**
 * Pipeable RxJS operator, which increments the master loading counter in the state upon subscription
 * and decrements the master loading counter in the state upon observable completion or error.
 *
 */
export function masterLoading<T>(appState: AppStateService, message?: string): OperatorFunction<T, T> {
    return (source$) => {
        return new Observable(subscriber => {
            const sourceObservable$ = source$.pipe(
                finalize(() => {
                    appState.dispatch(new DecrementMasterLoading());
                }),
            );

            const subscription = appState.dispatch(new IncrementMasterLoading(message))
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

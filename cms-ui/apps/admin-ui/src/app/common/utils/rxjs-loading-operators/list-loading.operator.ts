import { Observable, OperatorFunction } from 'rxjs';
import { finalize, mergeMap, switchMap } from 'rxjs/operators';
import { AppStateService, DecrementListLoading, IncrementListLoading } from '../../../state';
import { ListId } from '@admin-ui/common';

/**
 * Pipeable RxJS operator, which increments the list loading counter of the specified list in the state upon subscription
 * and decrements the list loading counter for the specified list in the state upon observable completion or error.
 */
export function listLoading<T>(appState: AppStateService, listId: ListId): OperatorFunction<T, T> {
    return (source$) => {
        return new Observable(subscriber => {
            const sourceObservable$ = source$.pipe(
                finalize(() => {
                    appState.dispatch(new DecrementListLoading(listId));
                }),
            );

            const subscription = appState.dispatch(new IncrementListLoading(listId))
                .pipe(
                    mergeMap(() => sourceObservable$),
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

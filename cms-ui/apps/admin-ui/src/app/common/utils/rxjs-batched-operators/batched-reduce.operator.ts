import { of as observableOf, OperatorFunction, ReplaySubject } from 'rxjs';
import { switchMap } from 'rxjs/operators';

/** The default batch size used by the batched operators. */
export const DEFAULT_BATCH_SIZE = 1000;

/** The delay between two batches in msec. */
export const BATCH_DELAY_MSEC = 15;

/**
 * A reducer function type, anologous to the one used by `Array.reduce()`.
 */
export type ReducerFn<T, R> = (accumulator: R, currentValue: T, index: number, array: T[]) => R;

/**
 * Used to create the initial value for `batchedReduce()`.
 */
export type InitialValueFactory<T, R> = (source: T[]) => R;

/**
 * Pipeable RxJS operator, which executes a reduce operation on the input array, while splitting
 * the `reducer` calls into multiple batches to keep the UI from freezing.
 *
 * @param reducer A reducer function, anologous to the one used by `Array.reduce()`.
 * @param createInitialVaue A function to create the initial value of the accumualtor, based on the input array.
 * @param batchSize (optional) The size of the batches (default is 1000).
 */
export function batchedReduce<T, R>(
    reducer: ReducerFn<T, R>,
    createInitialVaue: InitialValueFactory<T, R>,
    batchSize: number = DEFAULT_BATCH_SIZE,
): OperatorFunction<T[], R> {
    return (input$) => input$.pipe(
        switchMap(source => {
            const initialValue = createInitialVaue(source);
            if (!source || source.length === 0) {
                return observableOf(initialValue);
            }

            // We need a ReplaySubject in case we only have a single batch, which
            // causes the observer to subscribe after we have emitted the result.
            const resultSubj$ = new ReplaySubject<R>(1);
            batchProcess(source, reducer, initialValue, batchSize, resultSubj$);
            return resultSubj$;
        }),
    );
}

function batchProcess<T, R>(
    source: T[],
    action: ReducerFn<T, R>,
    initialValue: R,
    batchSize: number,
    resultSubj$: ReplaySubject<R>,
): void {
    let accumulator = initialValue;

    const next = (startIndex: number) => {
        if (resultSubj$.observers.length === 0 && startIndex !== 0) {
            // If all observers have unsubscribed and this is not the initial iteration,
            // we can stop processing, because the result is no longer needed.
            return;
        }

        const nextBatchStart = Math.min(source.length, startIndex + batchSize);

        try {
            for (let i = startIndex; i < nextBatchStart; ++i) {
                accumulator = action(accumulator, source[i], i, source);
            }
        } catch (error) {
            resultSubj$.error(error);
            return;
        }


        if (nextBatchStart < source.length) {
            setTimeout(() => next(nextBatchStart), BATCH_DELAY_MSEC);
        } else {
            resultSubj$.next(accumulator);
            resultSubj$.complete();
        }
    };

    next(0);
}

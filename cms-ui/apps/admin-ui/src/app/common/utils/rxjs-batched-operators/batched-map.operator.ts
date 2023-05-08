import { OperatorFunction } from 'rxjs';
import { tap } from 'rxjs/operators';
import { batchedReduce, DEFAULT_BATCH_SIZE } from './batched-reduce.operator';

/**
 * A function to map a single entry of an array (`currentValue`) to a new value -
 * anologous to the callback function expected by `Array.map()`.
 */
export type MapperFn<T, R> = (currentValue: T, index: number, array: T[]) => R;

/**
 * Pipeable RxJS operator, which executes a map operation on the input array, while splitting
 * the `mapper` calls into multiple batches to keep the UI from freezing.
 *
 * @param mapper A mapper function, anologous to the one used by `Array.map()`.
 * @param batchSize (optional) The size of the batches (default is 1000).
 */
export function batchedMap<T, R>(mapper: MapperFn<T, R>, batchSize: number = DEFAULT_BATCH_SIZE): OperatorFunction<T[], R[]> {
    return (input$) => input$.pipe(
        tap(source => {
            if (!source) {
                throw new TypeError(`The input to batchMap() cannot be ${source}.`);
            }
        }),
        batchedReduce(
            (dest, currentValue, index, source) => {
                dest[index] = mapper(currentValue, index, source);
                return dest;
            },
            (source) => new Array<R>(source.length),
            batchSize,
        ),
    );
}

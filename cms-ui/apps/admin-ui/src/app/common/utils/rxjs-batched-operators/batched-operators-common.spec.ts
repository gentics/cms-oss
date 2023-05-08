import { tick } from '@angular/core/testing';
import { of as observableOf, OperatorFunction, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ObservableStopper } from '../observable-stopper/observable-stopper';
import { BATCH_DELAY_MSEC } from './batched-reduce.operator';

export type OperatorFactory<T, R> = () => OperatorFunction<T[], R>;

function assertActionCallsCorrect(actionSpy: jasmine.Spy, expectedActionCalls: any[][], startOffset: number): void {
    expect(actionSpy).toHaveBeenCalledTimes(startOffset + expectedActionCalls.length);
    expectedActionCalls.forEach((expectedArgs, i) => {
        const callIndex = startOffset + i;
        expect(actionSpy.calls.argsFor(callIndex)).toEqual(expectedArgs,
            `The arguments to call ${callIndex} of ${actionSpy.name} did not match the expected values.`);
    });
}

export interface SingleBatchTestConfig<T, R> {
    createOperator: OperatorFactory<T, R>;
    input: T[];
    actionSpy: jasmine.Spy;
    expectedOutput: R;
    expectedActionCalls: any[][];
    stopper: ObservableStopper;
}

export function assertSingleBatchWorks<T, R>(config: SingleBatchTestConfig<T, R>): R {
    let result: R;
    let emitted = false;

    observableOf(config.input).pipe(
        config.createOperator(),
        takeUntil(config.stopper.stopper$),
    ).subscribe(
        output => {
            result = output;
            emitted = true;
        },
        error => fail(error),
    );

    expect(emitted).toBe(true, 'The observable should have emitted immediately (after a single batch).');
    expect(result).toEqual(config.expectedOutput);

    assertActionCallsCorrect(config.actionSpy, config.expectedActionCalls, 0);

    return result;
}


export interface MultiBatchTestConfig<T, R> {
    createOperator: OperatorFactory<T, R>;
    input: T[];
    actionSpy: jasmine.Spy;
    expectedOutput: R;
    batchSize: number;
    /** The expected arguments to the actionSpy during each iteration (must specify 3 iterations). */
    expectedActionCalls: [
        any[][],
        any[][],
        any[][],
    ];
    stopper: ObservableStopper;
}

export function assertMultipleBatchesWork<T, R>(config: MultiBatchTestConfig<T, R>): R {
    // The first two iterations should have a length of batchSize, the last one may be smaller.
    expect(config.expectedActionCalls[0].length).toEqual(config.batchSize);
    expect(config.expectedActionCalls[1].length).toEqual(config.batchSize);

    let result: R;
    let emitted = false;

    observableOf(config.input).pipe(
        config.createOperator(),
        takeUntil(config.stopper.stopper$),
    ).subscribe(
        output => {
            result = output;
            emitted = true;
        },
        error => fail(error),
    );

    // Check state after first (immediate) iteration.
    expect(emitted).toBe(false, 'The observable should not have emitted immediately, because there are multiple batches.');
    assertActionCallsCorrect(config.actionSpy, config.expectedActionCalls[0], 0);

    tick(BATCH_DELAY_MSEC);

    // Check state after second iteration.
    expect(emitted).toBe(false, 'The observable should not have emitted yet, because one batch is still missing.');
    assertActionCallsCorrect(config.actionSpy, config.expectedActionCalls[1], config.batchSize);

    tick(BATCH_DELAY_MSEC);

    // Check state after third (final) iteration.
    expect(emitted).toBe(true, 'The observable should have emitted now, because all batches are supposed to be finished.');
    assertActionCallsCorrect(config.actionSpy, config.expectedActionCalls[2], config.batchSize * 2);
    expect(result).toEqual(config.expectedOutput);

    return result;
}

export function assertProcessingIsStoppedOnUnsubscribe<T, R>(config: MultiBatchTestConfig<T, R>): void {
    const sub = observableOf(config.input).pipe(
        config.createOperator(),
        takeUntil(config.stopper.stopper$),
    ).subscribe(
        output => fail('No emission expected, because observer has unsubscribed.'),
        error => fail(error),
    );

    // Check state after first (immediate) iteration.
    assertActionCallsCorrect(config.actionSpy, config.expectedActionCalls[0], 0);

    tick(BATCH_DELAY_MSEC);

    // Check state after second iteration.
    assertActionCallsCorrect(config.actionSpy, config.expectedActionCalls[1], config.batchSize);

    // Unsubscribe and make sure that there are no more action calls.
    config.actionSpy.calls.reset();
    sub.unsubscribe();
    tick(BATCH_DELAY_MSEC);
    expect(config.actionSpy.calls.count()).toBe(0, 'Expected no more action calls, after unsubscribing');
}

export interface InputWithExpectedActionCalls<T> {
    input: T[];
    /** The expected arguments to the actionSpy during each iteration (must specify 3 iterations). */
    expectedActionCalls: [
        any[][],
        any[][],
        any[][],
    ];
}

export interface MultiBatchTwoInputsTestConfig<T, R> {
    createOperator: OperatorFactory<T, R>;
    /**
     * Input data that should be emitted by the source observable.
     * After the first input has started processing, the second input will be emitted
     * to test if the processing of the first one is stopped.
     */
    inputs: [
        InputWithExpectedActionCalls<T>,
        InputWithExpectedActionCalls<T>,
    ];
    actionSpy: jasmine.Spy;
    expectedOutput: R;
    batchSize: number;
    stopper: ObservableStopper;
}

/**
 * Asserts that processing of the first input array is stopped when the second input array is emitted
 * by the source and asserts that the result is equal to the one expected from the second input.
 */
export function assertProcessingIsRestartedOnNewEmission<T, R>(config: MultiBatchTwoInputsTestConfig<T, R>): R {
    let result: R;
    let emitted = false;

    const source$ = new Subject<T[]>();
    source$.pipe(
        config.createOperator(),
        takeUntil(config.stopper.stopper$),
    ).subscribe(
        output => {
            result = output;
            emitted = true;
        },
        error => fail(error),
    );

    // Check state after first iteration of the first input.
    source$.next(config.inputs[0].input);
    assertActionCallsCorrect(config.actionSpy, config.inputs[0].expectedActionCalls[0], 0);

    // Check state after second iteration.
    tick(BATCH_DELAY_MSEC);
    assertActionCallsCorrect(config.actionSpy, config.inputs[0].expectedActionCalls[1], config.batchSize);

    // Emit a new input value.
    config.actionSpy.calls.reset();
    source$.next(config.inputs[1].input);
    assertActionCallsCorrect(config.actionSpy, config.inputs[1].expectedActionCalls[0], 0);

    // Check state after second iteration.
    tick(BATCH_DELAY_MSEC);
    assertActionCallsCorrect(config.actionSpy, config.inputs[1].expectedActionCalls[1], config.batchSize);

    // Check state after third (final) iteration.
    tick(BATCH_DELAY_MSEC);
    expect(emitted).toBe(true, 'The observable should have emitted now, because all batches are supposed to be finished.');
    assertActionCallsCorrect(config.actionSpy, config.inputs[1].expectedActionCalls[2], config.batchSize * 2);
    expect(result).toEqual(config.expectedOutput);

    return result;
}

export function assertErrorsAreRelayedCorrectly<T, R>(createOperator: OperatorFactory<T, R>, input: T[], expectedError: Error): void {
    let error: Error;
    let errorEmitted = false;

    const sub = observableOf(input).pipe(
        createOperator(),
    ).subscribe(
        output => fail('The observable should have emitted an error.'),
        err => {
            error = err;
            errorEmitted = true;
        },
    );

    sub.unsubscribe();
    expect(errorEmitted).toBe(true, 'Expected an error to have been emitted');
    expect(error).toEqual(expectedError);
}

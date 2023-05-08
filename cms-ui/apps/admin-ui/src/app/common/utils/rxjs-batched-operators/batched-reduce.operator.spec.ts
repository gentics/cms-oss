import { fakeAsync } from '@angular/core/testing';
import { IndexByKey } from '@gentics/cms-models';
import { ObservableStopper } from '../observable-stopper/observable-stopper';
import {
    assertErrorsAreRelayedCorrectly,
    assertMultipleBatchesWork,
    assertProcessingIsRestartedOnNewEmission,
    assertProcessingIsStoppedOnUnsubscribe,
    assertSingleBatchWorks,
    MultiBatchTestConfig,
    MultiBatchTwoInputsTestConfig,
    SingleBatchTestConfig,
} from './batched-operators-common.spec';
import { batchedReduce } from './batched-reduce.operator';

const BATCH_SIZE = 20;

describe('batchedReduce()', () => {

    let stopper: ObservableStopper;
    let createInitialSpy: jasmine.Spy;
    let reducerSpy: jasmine.Spy;

    beforeEach(() => {
        stopper = new ObservableStopper();
        createInitialSpy = jasmine.createSpy('createInitial');
        reducerSpy = jasmine.createSpy('reducer');
    });

    afterEach(() => {
        stopper.stop();
    });

    describe('number accumulation', () => {

        function reduceWithAddition(accumulator: number, currValue: number): number {
            return accumulator + currValue;
        }

        function setUpSingleBatchTest(elementsCount: number, startValue: number = 1): SingleBatchTestConfig<number, number> {
            const input: number[] = [];
            const expectedActionCalls = [];
            let accumulator = 0;

            for (let i = 0; i < elementsCount; ++i) {
                const currValue = i + startValue;
                input.push(currValue);
                expectedActionCalls.push([
                    accumulator, // accumulator
                    currValue, // currentValue
                    i, // index
                    input, // array
                ]);
                accumulator += currValue;
            }

            expect(input.length).toBe(elementsCount);
            expect(expectedActionCalls.length).toBe(elementsCount);

            // Expected output is the sum of all numbers from startValue to elementsCount.
            const expectedOutput = accumulator;

            return {
                createOperator: () => batchedReduce(reducerSpy, createInitialSpy, BATCH_SIZE),
                input,
                actionSpy: reducerSpy,
                expectedOutput,
                expectedActionCalls,
                stopper,
            };
        }

        beforeEach(() => {
            createInitialSpy.and.callFake(() => 0);
            reducerSpy.and.callFake(reduceWithAddition);
        });

        it('works for undefined', fakeAsync(() => {
            assertSingleBatchWorks<number, number>({
                createOperator: () => batchedReduce(reducerSpy, createInitialSpy, BATCH_SIZE),
                input: undefined,
                actionSpy: reducerSpy,
                expectedOutput: 0,
                expectedActionCalls: [],
                stopper,
            });

            expect(createInitialSpy).toHaveBeenCalledTimes(1);
            expect(createInitialSpy).toHaveBeenCalledWith(undefined);
        }));

        it('works for null', fakeAsync(() => {
            assertSingleBatchWorks<number, number>({
                createOperator: () => batchedReduce(reducerSpy, createInitialSpy, BATCH_SIZE),
                input: null,
                actionSpy: reducerSpy,
                expectedOutput: 0,
                expectedActionCalls: [],
                stopper,
            });

            expect(createInitialSpy).toHaveBeenCalledTimes(1);
            expect(createInitialSpy).toHaveBeenCalledWith(null);
        }));

        it('works for an empty array', fakeAsync(() => {
            assertSingleBatchWorks<number, number>({
                createOperator: () => batchedReduce(reducerSpy, createInitialSpy, BATCH_SIZE),
                input: [],
                actionSpy: reducerSpy,
                expectedOutput: 0,
                expectedActionCalls: [],
                stopper,
            });

            expect(createInitialSpy).toHaveBeenCalledTimes(1);
            expect(createInitialSpy).toHaveBeenCalledWith([]);
        }));

        it('works for an array with a single element', fakeAsync(() => {
            const input = [ 1 ];
            assertSingleBatchWorks<number, number>({
                createOperator: () => batchedReduce(reducerSpy, createInitialSpy, BATCH_SIZE),
                input,
                actionSpy: reducerSpy,
                expectedOutput: 1,
                expectedActionCalls: [ [0, 1, 0, input] ],
                stopper,
            });

            expect(createInitialSpy).toHaveBeenCalledTimes(1);
            expect(createInitialSpy).toHaveBeenCalledWith(input);
        }));

        it('works for an array that is smaller than batchSize', fakeAsync(() => {
            const size = 10;
            expect(size).toBeLessThan(BATCH_SIZE);
            const testConfig = setUpSingleBatchTest(size);

            assertSingleBatchWorks<number, number>(testConfig);

            expect(createInitialSpy).toHaveBeenCalledTimes(1);
            expect(createInitialSpy).toHaveBeenCalledWith(testConfig.input);
        }));

        it('works for an array that is exactly batchSize', fakeAsync(() => {
            const testConfig = setUpSingleBatchTest(BATCH_SIZE);

            assertSingleBatchWorks<number, number>(testConfig);

            expect(createInitialSpy).toHaveBeenCalledTimes(1);
            expect(createInitialSpy).toHaveBeenCalledWith(testConfig.input);
        }));

        it('works for an array that is an exact multiple of batchSize', fakeAsync(() => {
            const size = BATCH_SIZE * 3;

            // Create a single batch config and then split it up into three batches.
            const base = setUpSingleBatchTest(size);
            const multiBatchConfig: MultiBatchTestConfig<number, number> = {
                createOperator: base.createOperator,
                input: base.input,
                actionSpy: base.actionSpy,
                expectedOutput: base.expectedOutput,
                batchSize: BATCH_SIZE,
                expectedActionCalls: [
                    base.expectedActionCalls.slice(0, BATCH_SIZE),
                    base.expectedActionCalls.slice(BATCH_SIZE, BATCH_SIZE * 2),
                    base.expectedActionCalls.slice(BATCH_SIZE * 2),
                ],
                stopper,
            };
            expect(multiBatchConfig.expectedActionCalls[2].length).toBe(BATCH_SIZE);

            assertMultipleBatchesWork<number, number>(multiBatchConfig);

            expect(createInitialSpy).toHaveBeenCalledTimes(1);
            expect(createInitialSpy).toHaveBeenCalledWith(multiBatchConfig.input);
        }));

        it('works for an array that is not an exact multiple of batchSize', fakeAsync(() => {
            const size = BATCH_SIZE * 2 + 10;
            expect(size).toBeLessThan(BATCH_SIZE * 3);

            // Create a single batch config and then split it up into three batches.
            const base = setUpSingleBatchTest(size);
            const multiBatchConfig: MultiBatchTestConfig<number, number> = {
                createOperator: base.createOperator,
                input: base.input,
                actionSpy: base.actionSpy,
                expectedOutput: base.expectedOutput,
                batchSize: BATCH_SIZE,
                expectedActionCalls: [
                    base.expectedActionCalls.slice(0, BATCH_SIZE),
                    base.expectedActionCalls.slice(BATCH_SIZE, BATCH_SIZE * 2),
                    base.expectedActionCalls.slice(BATCH_SIZE * 2),
                ],
                stopper,
            };
            expect(multiBatchConfig.expectedActionCalls[2].length).toBe(10);

            assertMultipleBatchesWork<number, number>(multiBatchConfig);

            expect(createInitialSpy).toHaveBeenCalledTimes(1);
            expect(createInitialSpy).toHaveBeenCalledWith(multiBatchConfig.input);
        }));

        it('stops processing if the observer unsubscribes', fakeAsync(() => {
            const size = BATCH_SIZE * 3;

            // Create a single batch config and then split it up into three batches.
            const base = setUpSingleBatchTest(size);
            const multiBatchConfig: MultiBatchTestConfig<number, number> = {
                createOperator: base.createOperator,
                input: base.input,
                actionSpy: base.actionSpy,
                expectedOutput: base.expectedOutput,
                batchSize: BATCH_SIZE,
                expectedActionCalls: [
                    base.expectedActionCalls.slice(0, BATCH_SIZE),
                    base.expectedActionCalls.slice(BATCH_SIZE, BATCH_SIZE * 2),
                    base.expectedActionCalls.slice(BATCH_SIZE * 2),
                ],
                stopper,
            };

            assertProcessingIsStoppedOnUnsubscribe(multiBatchConfig);
        }));

        it('stops processing an existing input when a new one is emitted', fakeAsync(() => {
            const size = BATCH_SIZE * 3;

            // Since input2 has more items than input1, the expected result is greater.
            const input1 = setUpSingleBatchTest(size - 10);
            const input2 = setUpSingleBatchTest(size, 100);

            const config: MultiBatchTwoInputsTestConfig<number, number> = {
                createOperator: input1.createOperator,
                inputs: [
                    {
                        input: input1.input,
                        expectedActionCalls: [
                            input1.expectedActionCalls.slice(0, BATCH_SIZE),
                            input1.expectedActionCalls.slice(BATCH_SIZE, BATCH_SIZE * 2),
                            input1.expectedActionCalls.slice(BATCH_SIZE * 2),
                        ],
                    },
                    {
                        input: input2.input,
                        expectedActionCalls: [
                            input2.expectedActionCalls.slice(0, BATCH_SIZE),
                            input2.expectedActionCalls.slice(BATCH_SIZE, BATCH_SIZE * 2),
                            input2.expectedActionCalls.slice(BATCH_SIZE * 2),
                        ],
                    },
                ],
                actionSpy: input1.actionSpy,
                expectedOutput: input2.expectedOutput,
                batchSize: BATCH_SIZE,
                stopper,
            };

            assertProcessingIsRestartedOnNewEmission<number, number>(config);
        }));

    });

    describe('object accumulation', () => {

        type Accumulator = IndexByKey<number>;

        function getKey(index: number): string {
            return `key${index}`;
        }

        function getExpectedAccumulatorBeforeIterationIndex(index: number): Accumulator {
            const ret: Accumulator = {};
            for (let i = 0; i < index; ++i) {
                ret[getKey(i)] = i;
            }
            return ret;
        }

        function reduceByModifyingObject(accumulator: Accumulator, currValue: number, index: number): Accumulator {
            expect(accumulator).toBe(createInitialSpy.calls.all()[0].returnValue,
                'Expected accumulator to be the object that was created by createInitialValue()');
            const expectedAccumulatorValue = getExpectedAccumulatorBeforeIterationIndex(index);
            expect(accumulator).toEqual(expectedAccumulatorValue);

            const key = getKey(index);
            accumulator[key] = currValue;
            return accumulator;
        }

        function setUpSingleBatchTest(elementsCount: number): SingleBatchTestConfig<number, Accumulator> {
            const input: number[] = [];
            const expectedActionCalls = [];
            const accumulator: Accumulator = {};

            for (let i = 0; i < elementsCount; ++i) {
                input.push(i);
                expectedActionCalls.push([
                    // We cannot check the accumulator by value for every action call,
                    // because it is modified and reused by all calls.
                    jasmine.objectContaining({}),
                    i, // currentValue
                    i, // index
                    input, // array
                ]);
                accumulator[getKey(i)] = i;
            }

            return {
                createOperator: () => batchedReduce(reducerSpy, createInitialSpy, BATCH_SIZE),
                input,
                actionSpy: reducerSpy,
                expectedOutput: accumulator,
                expectedActionCalls,
                stopper,
            };
        }

        beforeEach(() => {
            createInitialSpy.and.callFake(() => ({}));
            reducerSpy.and.callFake(reduceByModifyingObject);
        });

        it('works for an array that is exactly batchSize', fakeAsync(() => {
            const testConfig = setUpSingleBatchTest(BATCH_SIZE);

            const result = assertSingleBatchWorks<number, Accumulator>(testConfig);

            expect(createInitialSpy).toHaveBeenCalledTimes(1);
            expect(createInitialSpy).toHaveBeenCalledWith(testConfig.input);

            expect(result).toBe(createInitialSpy.calls.all()[0].returnValue,
                'Expected the final result to be the same object that was returned by createInitialValue()');
        }));

        it('works for an array that is an exact multiple of batchSize', fakeAsync(() => {
            const size = BATCH_SIZE * 3;

            // Create a single batch config and then split it up into three batches.
            const base = setUpSingleBatchTest(size);
            const multiBatchConfig: MultiBatchTestConfig<number, Accumulator> = {
                createOperator: base.createOperator,
                input: base.input,
                actionSpy: base.actionSpy,
                expectedOutput: base.expectedOutput,
                batchSize: BATCH_SIZE,
                expectedActionCalls: [
                    base.expectedActionCalls.slice(0, BATCH_SIZE),
                    base.expectedActionCalls.slice(BATCH_SIZE, BATCH_SIZE * 2),
                    base.expectedActionCalls.slice(BATCH_SIZE * 2),
                ],
                stopper,
            };
            expect(multiBatchConfig.expectedActionCalls[2].length).toBe(BATCH_SIZE);

            const result = assertMultipleBatchesWork<number, Accumulator>(multiBatchConfig);

            expect(createInitialSpy).toHaveBeenCalledTimes(1);
            expect(createInitialSpy).toHaveBeenCalledWith(multiBatchConfig.input);

            expect(result).toBe(createInitialSpy.calls.all()[0].returnValue,
                'Expected the final result to be the same object that was returned by createInitialValue()');

        }));

    });

    it('relays errors correctly', fakeAsync(() => {
        const expectedError = new Error('ExpectedError');
        reducerSpy.and.callFake(() => { throw expectedError; });

        assertErrorsAreRelayedCorrectly<number, number>(
            () => batchedReduce(reducerSpy, createInitialSpy, BATCH_SIZE),
            [ 1, 2, 3, 4 ],
            expectedError,
        );

        expect(reducerSpy).toHaveBeenCalledTimes(1);
    }));

});

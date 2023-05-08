import { fakeAsync } from '@angular/core/testing';
import { ObservableStopper } from '../observable-stopper/observable-stopper';
import { batchedMap } from './batched-map.operator';
import {
    assertErrorsAreRelayedCorrectly,
    assertMultipleBatchesWork,
    assertSingleBatchWorks,
    MultiBatchTestConfig,
    SingleBatchTestConfig,
} from './batched-operators-common.spec';

const BATCH_SIZE = 20;

interface InputValue {
    input: number;
}

interface OutputValue {
    output: number;
}

describe('batchedMap()', () => {

    let stopper: ObservableStopper;
    let mapperSpy: jasmine.Spy;

    function mapper(currValue: InputValue): OutputValue {
        return { output: currValue.input * 2 };
    }

    function setUpSingleBatchTest(elementsCount: number): SingleBatchTestConfig<InputValue, OutputValue[]> {
        const input: InputValue[] = [];
        const expectedActionCalls = [];
        const expectedOutput: OutputValue[] = [];

        for (let i = 0; i < elementsCount; ++i) {
            input.push({ input: i });
            expectedActionCalls.push([
                { input: i }, // currentValue
                i, // index
                input, // array
            ]);
            expectedOutput.push({ output: i * 2 });
        }

        expect(input.length).toBe(elementsCount);
        expect(expectedActionCalls.length).toBe(elementsCount);
        expect(expectedOutput.length).toBe(elementsCount);

        return {
            createOperator: () => batchedMap(mapperSpy, BATCH_SIZE),
            input,
            actionSpy: mapperSpy,
            expectedOutput,
            expectedActionCalls,
            stopper,
        };
    }

    beforeEach(() => {
        stopper = new ObservableStopper();
        mapperSpy = jasmine.createSpy('mapper').and.callFake(mapper);
    });

    afterEach(() => {
        stopper.stop();
    });

    it('works for an empty array', fakeAsync(() => {
        assertSingleBatchWorks<InputValue, OutputValue[]>({
            createOperator: () => batchedMap(mapperSpy, BATCH_SIZE),
            input: [],
            actionSpy: mapperSpy,
            expectedOutput: [],
            expectedActionCalls: [],
            stopper,
        });
    }));

    it('works for an array with a single element', fakeAsync(() => {
        const input = [ { input: 1 } ];
        assertSingleBatchWorks<InputValue, OutputValue[]>({
            createOperator: () => batchedMap(mapperSpy, BATCH_SIZE),
            input,
            actionSpy: mapperSpy,
            expectedOutput: [ { output: 2 } ],
            expectedActionCalls: [ [input[0], 0, input] ],
            stopper,
        });
    }));

    it('works for an array that is smaller than batchSize', fakeAsync(() => {
        const size = 10;
        expect(size).toBeLessThan(BATCH_SIZE);
        const testConfig = setUpSingleBatchTest(size);

        assertSingleBatchWorks<InputValue, OutputValue[]>(testConfig);
    }));

    it('works for an array that is exactly batchSize', fakeAsync(() => {
        const testConfig = setUpSingleBatchTest(BATCH_SIZE);

        assertSingleBatchWorks<InputValue, OutputValue[]>(testConfig);
    }));

    it('works for an array that is an exact multiple of batchSize', fakeAsync(() => {
        const size = BATCH_SIZE * 3;

        // Create a single batch config and then split it up into three batches.
        const base = setUpSingleBatchTest(size);
        const multiBatchConfig: MultiBatchTestConfig<InputValue, OutputValue[]> = {
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

        assertMultipleBatchesWork(multiBatchConfig);
    }));

    it('works for an array that is not an exact multiple of batchSize', fakeAsync(() => {
        const size = BATCH_SIZE * 2 + 10;
        expect(size).toBeLessThan(BATCH_SIZE * 3);

        // Create a single batch config and then split it up into three batches.
        const base = setUpSingleBatchTest(size);
        const multiBatchConfig: MultiBatchTestConfig<InputValue, OutputValue[]> = {
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

        assertMultipleBatchesWork(multiBatchConfig);
    }));

    describe('error handling', () => {

        it('relays errors correctly', fakeAsync(() => {
            const expectedError = new Error('ExpectedError');
            mapperSpy.and.callFake(() => { throw expectedError; });

            assertErrorsAreRelayedCorrectly<InputValue, OutputValue[]>(
                () => batchedMap(mapperSpy, BATCH_SIZE),
                [ { input: 1 }, { input: 2 }, { input: 3 }, { input: 4 } ],
                expectedError,
            );

            expect(mapperSpy).toHaveBeenCalledTimes(1);
        }));

        it('emits an error for undefined', fakeAsync(() => {
            const expectedError = new TypeError('The input to batchMap() cannot be undefined.');

            assertErrorsAreRelayedCorrectly<InputValue, OutputValue[]>(
                () => batchedMap(mapperSpy, BATCH_SIZE),
                undefined,
                expectedError,
            );
        }));

        it('emits an error for null', fakeAsync(() => {
            const expectedError = new TypeError('The input to batchMap() cannot be null.');

            assertErrorsAreRelayedCorrectly<InputValue, OutputValue[]>(
                () => batchedMap(mapperSpy, BATCH_SIZE),
                null,
                expectedError,
            );
        }));

    });

});

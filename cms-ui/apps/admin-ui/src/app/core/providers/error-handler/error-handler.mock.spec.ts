import { fakeAsync } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { MockErrorHandler } from './error-handler.mock';

describe('MockErrorHandler', () => {

    let errorHandler: MockErrorHandler;
    let expectedError: Error;

    beforeEach(() => {
        errorHandler = new MockErrorHandler();
        expectedError = new Error('Test');
    });

    it('notifyAndRethrow() rethrows the error', () => {
        expect(() => errorHandler.notifyAndRethrow(expectedError)).toThrow(expectedError);
    });

    describe('assertNotifyAndRethrowIsCalled()', () => {

        it('succeeds if notifyAndRethrow() is called and the error is forwarded', fakeAsync(() => {
            let subscribed = false;
            const source$ = new BehaviorSubject<any>(null);
            const action$ = source$.pipe(
                tap(() => subscribed = true),
                map(() => { throw expectedError; }),
                catchError(error => errorHandler.notifyAndRethrow(error)),
            );

            // If assertNotifyAndRethrowIsCalled() fails, this test will also fail.
            errorHandler.assertNotifyAndRethrowIsCalled(action$, expectedError);
            expect(subscribed).toBe(true, 'Observable was never subscribed to');
            expect(source$.observers.length).toBe(0, 'Subscription was not unsubscribed');
        }));

        // ToDo: This test does not work. Maybe we should use mustFail(), but I am not sure if this is safe to use.
        // Currently the mustFail() tests execute as one of the last tests in the suite, so they should not affect too much.
        // it('fails if notifyAndRethrow() is not called', fakeAsync(() => {
        //     const source$ = new BehaviorSubject<any>(null);
        //     const action$ = source$.pipe(
        //         map(() => { throw expectedError; }),
        //         catchError(error => { throw error; }),
        //     );
        //     expect(() => errorHandler.assertNotifyAndRethrowIsCalled(action$, expectedError)).toThrow();
        // }));

    });

});

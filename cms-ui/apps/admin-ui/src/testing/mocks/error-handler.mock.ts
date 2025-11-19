import { tick } from '@angular/core/testing';
import { Observable, Subscription } from 'rxjs';
import { ErrorHandler } from '../../app/core/providers/error-handler/error-handler.service';
import { createSpyFn, Mocked } from '@gentics/ui-core/testing';

/**
 * Mocks the most commonly used parts of the ErrorHandler service
 * and a provides utility method for validating the user of `notifyAndRethrow()`.
 */
export class MockErrorHandler implements Mocked<ErrorHandler> {

    catch = jasmine.createSpy('ErrorHandler.catch');

    notifyAndRethrow = createSpyFn('ErrorHandler.notifyAndRethrow', (error) => { throw error; });

    notifyAndReturnErrorMessage = createSpyFn('ErrorHandler.notifyAndReturnErrorMessage', () => 'fakeErrorMessage');

    /**
     * Subscribes to the specified action and asserts that `notifyAndRethrow()` is called
     * and that the error is properly forwarded.
     *
     * This method must be called inside a FakeAsync zone.
     */
    assertNotifyAndRethrowIsCalled(action$: Observable<any>, expectedError: Error): void {
        let sub: Subscription;

        try {
            let actualError: any;
            sub = action$.subscribe(
                () => fail('This observable should emit an error'),
                (error) => actualError = error,
            );

            tick();
            expect(this.notifyAndRethrow).toHaveBeenCalledTimes(1);
            (expect(this.notifyAndRethrow) as any).toHaveBeenCalledWith(expectedError);
            expect(actualError).toEqual(expectedError);
        } finally {
            if (sub) {
                sub.unsubscribe();
            }
        }
    }
}

import { tick } from '@angular/core/testing';
import { Observable, Subscription } from 'rxjs';

/**
 * Mocks the most commonly used parts of the ErrorHandler service
 * and a provides utility method for validating the user of `notifyAndRethrow()`.
 */
export class MockErrorHandler {
    catch = jasmine.createSpy('ErrorHandler.catch');

    notifyAndRethrow = jasmine.createSpy('ErrorHandler.notifyAndRethrow')
        .and.callFake(error => { throw error; });

    notifyAndReturnErrorMessage = jasmine.createSpy('ErrorHandler.notifyAndReturnErrorMessage')
        .and.callFake(() => 'fakeErrorMessage');

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
                error => actualError = error,
            );

            tick();
            expect(this.notifyAndRethrow).toHaveBeenCalledTimes(1);
            expect(this.notifyAndRethrow).toHaveBeenCalledWith(expectedError);
            expect(actualError).toEqual(expectedError);
        } finally {
            if (sub) {
                sub.unsubscribe();
            }
        }
    }
}

import { createDelayedError } from '@admin-ui/testing';
import { Injectable, Injector } from '@angular/core';
import { fakeAsync, TestBed } from '@angular/core/testing';
import { Observable } from 'rxjs';
import { ErrorHandler } from '../error-handler';
import { MockErrorHandler } from '../error-handler/error-handler.mock';
import { OperationsBase } from './operations.base';

@Injectable()
class TestOperations extends OperationsBase {

    constructor(injector: Injector) {
        super(injector);
    }

    assertDependenciesInjected(): void {
        expect(this.errorHandler).toBeTruthy();
        expect(this.errorHandler instanceof MockErrorHandler).toBe(true);
    }

    executeOperationToTestErrorHandling(error: Error): Observable<any> {
        return createDelayedError(error).pipe(
            this.catchAndRethrowError(),
        );
    }

}

describe('OperationsBase', () => {

    let ops: TestOperations;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                TestOperations,
                { provide: ErrorHandler, useClass: MockErrorHandler },
            ],
        });

        ops = TestBed.get(TestOperations);
    });

    it('gets all required dependencies from the injector', () => {
        ops.assertDependenciesInjected();
    });

    it('catchAndRethrowError() works', fakeAsync(() => {
        const errorHandler = TestBed.get(ErrorHandler) as MockErrorHandler;
        const error = new Error('Test');
        const action$ = ops.executeOperationToTestErrorHandling(error);
        errorHandler.assertNotifyAndRethrowIsCalled(action$, error);
    }));

});

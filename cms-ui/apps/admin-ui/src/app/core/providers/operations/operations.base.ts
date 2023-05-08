import { ServiceBase } from '@admin-ui/shared/providers/service-base/service.base';
import { Injector } from '@angular/core';
import { MonoTypeOperatorFunction, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { ErrorHandler } from '../error-handler';

/**
 * Common superclass for all operations classes.
 *
 * Operations services are used to execute actions involving the REST API and then
 * apply the results to the AppState.
 * They are essentially the glue between the REST API client classes
 * and the AppState.
 *
 * An operations class may also show modals and interact with the user.
 *
 * The constructor takes only an Angular `Injector` as argument to avoid having to
 * modify all `super()` calls if we need to inject some new service in this superclass.
 */
export class OperationsBase extends ServiceBase {

    protected readonly errorHandler: ErrorHandler;

    constructor(
        injector: Injector,
    ) {
        super();
        this.errorHandler = injector.get(ErrorHandler);
    }

    /**
     * Returns an RxJS operator to catch an error using `ErrorHandler.notifyAndRethrow()`.
     */
    protected catchAndRethrowError<T>(): MonoTypeOperatorFunction<T> {
        return catchError(error => this.errorHandler.notifyAndRethrow(error));
    }

    protected catchAndReturnErrorMessage<T>(): MonoTypeOperatorFunction<T | string> {
        return catchError((error) => this.errorHandler.notifyAndReturnErrorMessage(error));
    }

}

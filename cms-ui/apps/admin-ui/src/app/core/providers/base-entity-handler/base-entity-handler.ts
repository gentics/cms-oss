import { MonoTypeOperatorFunction } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ErrorHandler } from '../error-handler';

export abstract class BaseEntityHandlerService {

    protected nameMap: Record<string | number, string> = {};

    constructor(
        protected errorHandler: ErrorHandler,
    ) {}

    /**
     * Returns an RxJS operator to catch an error using `ErrorHandler.notifyAndRethrow()`.
     */
    protected catchAndRethrowError<T>(): MonoTypeOperatorFunction<T> {
        return catchError(error => this.errorHandler.notifyAndRethrow(error));
    }
}

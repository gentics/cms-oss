import {ErrorHandler, Injectable} from '@angular/core';

/**
 * Options for handling an error.
 */
export interface ErrorHandlingOptions {

    /** Determines if a notification should be shown to the user. */
    notification: boolean;

}

/**
 * This interface needs to be implemented by a service that is
 * registered as an error handler for the `GcmsApi` using the
 * `GCMS_API_ERROR_HANDLER` injection token.
 *
 * The default is an implementation, which uses `ErrorHandler` from `@angular/core`
 */
export interface GcmsApiErrorHandler {

    /**
     * Catches and logs an error.
     * @param error The error.
     * @param options Options for handling an error.
     */
    catch(error: Error, options?: ErrorHandlingOptions): void;

}

/**
 * Default GcmsApiErrorHandler, which forwards error to the `ErrorHandler` provided by `@angular/core`.
 */
@Injectable()
export class AngularErrorHandler implements GcmsApiErrorHandler {

    constructor(private errorHandler: ErrorHandler) { }

    catch(error: Error, options?: ErrorHandlingOptions): void {
        this.errorHandler.handleError(error);
    }

}


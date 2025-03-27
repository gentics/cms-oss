import { Injectable } from '@angular/core';
import { DirtQueueListOptions, DirtQueueResponse, DirtQueueSummaryResponse, PublishInfo, PublishQueue } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { catchError, MonoTypeOperatorFunction, Observable } from 'rxjs';
import { ErrorHandler } from '../error-handler';

@Injectable()
export class AdminHandlerService {

    constructor(
        private errorHandler: ErrorHandler,
        private client: GCMSRestClientService,
    ) {}

    /**
     * Returns an RxJS operator to catch an error using `ErrorHandler.notifyAndRethrow()`.
     */
    protected catchAndRethrowError<T>(): MonoTypeOperatorFunction<T> {
        return catchError(error => this.errorHandler.notifyAndRethrow(error));
    }

    getDirtQueue(options?: DirtQueueListOptions): Observable<DirtQueueResponse> {
        return this.client.admin.getDirtQueue(options).pipe(
            this.catchAndRethrowError(),
        );
    }

    getDirtQueueSummary(): Observable<DirtQueueSummaryResponse> {
        return this.client.admin.getDirtQueueSummary().pipe(
            this.catchAndRethrowError(),
        );
    }

    getPublishInfo(): Observable<PublishInfo> {
        return this.client.admin.getPublishInfo().pipe(
            this.catchAndRethrowError(),
        );
    }

    getPublishQueue(): Observable<PublishQueue> {
        return this.client.admin.getPublishQueue().pipe(
            this.catchAndRethrowError(),
        );
    }
}

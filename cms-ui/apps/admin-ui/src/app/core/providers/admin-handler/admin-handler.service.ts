import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import { DirtQueueListOptions, DirtQueueResponse, DirtQueueSummaryResponse, PublishInfo, PublishQueue, Response } from '@gentics/cms-models';
import { RequestMethod } from '@gentics/cms-rest-client';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { catchError, MonoTypeOperatorFunction, Observable, tap, throwError } from 'rxjs';
import { ErrorHandler } from '../error-handler';

@Injectable()
export class AdminHandlerService {

    constructor(
        private errorHandler: ErrorHandler,
        private notification: I18nNotificationService,
        private client: GCMSRestClientService,
    ) {}

    /**
     * Returns an RxJS operator to catch an error using `ErrorHandler.notifyAndRethrow()`.
     */
    protected catchAndRethrowError<T>(): MonoTypeOperatorFunction<T> {
        return catchError((error) => this.errorHandler.notifyAndRethrow(error));
    }

    async stopPublishing(): Promise<PublishInfo> {
        try {
            const res = await this.client.getClient().executeMappedJsonRequest(RequestMethod.DELETE, 'publisher', {
                block: 'true',
                wait: 10_000,
            }).send();

            if (res.running) {
                this.notification.show({
                    type: 'warning',
                    message: 'shared.stop_publishing_delayed',
                });
            } else {
                this.notification.show({
                    type: 'success',
                    message: 'shared.stop_publishing_success',
                });
            }

            return res;
        } catch (err) {
            this.notification.show({
                type: 'alert',
                message: 'shared.stop_publishing_failed',
            });
            throw err;
        }
    }

    reloadConfiguration(): Observable<Response> {
        return this.client.admin.reloadConfiguration().pipe(
            tap(() => this.notification.show({
                type: 'success',
                message: 'shared.reload_configuration_success',
            })),
            catchError((error) => {
                this.notification.show({
                    type: 'alert',
                    message: 'shared.reload_configuration_failed',
                });
                return throwError(() => error);
            }),
        );
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

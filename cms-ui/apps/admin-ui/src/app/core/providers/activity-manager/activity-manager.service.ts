import { Injectable } from '@angular/core';
import { Response as GtxResponse } from '@gentics/cms-models';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { ErrorHandler } from '../error-handler';
import { I18nService } from '../i18n/i18n.service';

export interface GtxActivityManagerActivity {
    id: number;
    label: string;
    expanded: boolean;
    inProgress: boolean;
    succeeded: boolean;
    failed: boolean;
}

export interface GtxActivityManagerActivityConfig {
    /** The activity's label will change to this on resolve. */
    labelOnSuccess?: GtxActivityManagerActivityI18nLabel;
    /** The activity's label will change to this on reject. */
    labelOnFailed?: GtxActivityManagerActivityI18nLabel;
    /** Will be called on resolve. */
    callBackOnSuccess?: (response: any) => any;
    /** Will be called on reject. */
    callBackOnFailed?: (errorMessage: string) => any;
}

export type GtxActivityManagerActivityI18nLabel = string | {
    label: string;
    params: { [key: string]: string; };
    translateParams?: boolean;
};

/**
 * # ActivityManagerService
 * This service manages a queue of labelled activities with status information
 * intended to give the user insight about actions that take a noticeable amount of time,
 * e. g. copying and deleting Nodes.
 *
 * Each activity added to the queue will start to live with status `inProgress: true` and must
 * be equipped with an asynchronous parameter on whose resolve/rejection the activity will
 * either get status `succeeded: true` or `failed: true`. As all these states are visally
 * indicated the user will know which activitys are still running and which have completed.
 *
 * @example
 * ```
 * constructor(
 *       private http: HttpClient,
 *       private activityManager: ActivityManagerService,
 *   ) { }
 *
 * this.activityManager.activityAdd(
 *     'Creating user',
 *     this.http.post('/user', { nameName: 'johndoe' }).toPromise(),
 * );
 * ```
 *
 */
@Injectable()
export class ActivityManagerService {

    get activities$(): Observable<GtxActivityManagerActivity[]> {
        return this._activities$.asObservable();
    }

    get activities(): GtxActivityManagerActivity[] {
        return this._activities$.getValue();
    }

    private _activities$ = new BehaviorSubject<GtxActivityManagerActivity[]>([]);

    private activityCounter = 1;

    private animDelay = 20_000;

    constructor(
        private i18n: I18nService,
        private errorHandler: ErrorHandler,
    ) { }

    /**
     * Adds a activity into the activity queue.
     * @param label which the activity will be displaying to the user
     * @param process the progress of the activity will depend on. On resolve/reject the activity will visually indicate the outcome of the promise.
     * @param useResponseStringAsLabelOnSuccess If TRUE the activity's label will change to the resolved promise's return value.
     * @param useResponseStringAsLabelOnFailed If TRUE the activity's label will change to the rejected promise's return value.
     */
    activityAdd(
        label: GtxActivityManagerActivityI18nLabel,
        process: Observable<any>,
        useResponseStringAsLabelOnSuccess: boolean = false,
        useResponseStringAsLabelOnFailed: boolean = false,
        config?: GtxActivityManagerActivityConfig,
    ): void {

        if (typeof label !== 'string') {
            throw new Error('Label is not valid string.');
        }

        const currentId = this.activityCounter;

        const newActivity: GtxActivityManagerActivity = {
            id: currentId,
            label: this.geti18nString(label),
            expanded: false,
            inProgress: true,
            succeeded: false,
            failed: false,
        };
        this.activityCounter++;

        process.pipe(
            tap((response: unknown) => {
                this.activitySetProperty(currentId, 'inProgress', false);
                this.activitySetProperty(currentId, 'succeeded', true);
                newActivity.label = this.getLabel(response, useResponseStringAsLabelOnSuccess, config.labelOnSuccess, newActivity.label);
                setTimeout(() => {
                    this.activityRemove(newActivity.id);
                    if (config.callBackOnSuccess) {
                        config.callBackOnSuccess(response);
                    }
                }, this.animDelay);
            }),
            catchError((error: any) => {
                const errorMessage = this.errorHandler.notifyAndReturnErrorMessage(error);
                this.activitySetProperty(currentId, 'inProgress', false);
                this.activitySetProperty(currentId, 'failed', true);
                newActivity.label = this.getLabel(errorMessage, useResponseStringAsLabelOnFailed, config.labelOnFailed, newActivity.label);
                setTimeout(() => {
                    if (config.callBackOnFailed) {
                        config.callBackOnFailed(errorMessage);
                    }
                }, this.animDelay);
                return of(error);
            }),
        ).toPromise();

        const allTasks = [...this.activities, newActivity];
        this._activities$.next(allTasks);
    }

    activityRemove(id: number): void {
        const allTask = this.activities.filter(t => t.id !== id);
        this._activities$.next(allTask);
    }

    activitySetExpanded(id: number, value: boolean): void {
        this.activitySetProperty(id, 'expanded', value);
    }

    activityGetProperty<P extends keyof GtxActivityManagerActivity>(
        id: number,
        property: P,
    ): GtxActivityManagerActivity[P] {
        const activity = this.activities.find(a => a.id === id);
        if (!activity) {
            throw new Error(`Activity with ${id} does not exist.`);
        }
        return activity[property];
    }

    private activitySetProperty<P extends keyof GtxActivityManagerActivity>(
        id: number,
        property: P,
        value: GtxActivityManagerActivity[P],
    ): void {
        const activitiesUpdated: GtxActivityManagerActivity[] = [...this.activities];
        activitiesUpdated.forEach(activity => {
            if (activity.id === id) {
                activity[property] = value;
                return;
            }
        });
        this._activities$.next(activitiesUpdated);
    }

    private getLabel(
        response: unknown,
        useResponseStringAsLabel: boolean,
        customLabel: GtxActivityManagerActivityI18nLabel,
        fallback: string,
    ): string {
        if (response == null) {
            return fallback;
        }

        if (typeof response === 'string') {
            const i18nString = this.geti18nString(customLabel);
            return (useResponseStringAsLabel ? response : i18nString) || fallback;
        }

        if (typeof response === 'object' && (response as any).messages) {
            return this.getMessageSuccess(response as GtxResponse) || fallback;
        }

        return fallback;
    }

    private geti18nString(i18nData: GtxActivityManagerActivityI18nLabel): string | null {
        if (i18nData == null) {
            return null;
        }

        if (typeof i18nData === 'string') {
            return this.i18n.instant(i18nData);
        }

        if (typeof i18nData !== 'object' || !i18nData.label) {
            return null;
        }

        if (i18nData.params == null || typeof i18nData.params !== 'object' || Object.keys(i18nData.params).length === 0) {
            return this.i18n.instant(i18nData.label);
        }

        const params: { [key: string]: string; } = { ...i18nData.params };

        if (i18nData.translateParams) {
            Object.entries(params).forEach(([key, value]) => {
                params[key] = this.i18n.instant(value);
            });
        }

        return this.i18n.instant(i18nData.label, params);
    }

    private getMessageSuccess(response: GtxResponse): string | null {
        if (Array.isArray(response.messages) && response.messages.length > 0) {
            return response.messages.map(msg => msg.message || '').join('\n');
        } else {
            return null;
        }
    }

}

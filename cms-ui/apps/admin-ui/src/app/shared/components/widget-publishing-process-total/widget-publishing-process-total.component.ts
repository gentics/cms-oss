import { ObservableStopper, PUBLISH_PROCESS_REFRESH_INTERVAL } from '@admin-ui/common';
import { ErrorHandler } from '@admin-ui/core';
import { AdminOperations } from '@admin-ui/core/providers/operations/admin/admin.operations';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { PublishInfo, PublishObjectsCount } from '@gentics/cms-models';
import { BehaviorSubject, timer } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';

const WIDGET_PUBLISHING_PROCESS_ENTITY_KEYS = [
    'file',
    'folder',
    'form',
    'page',
] as const;

const WIDGET_PUBLISHING_PROCESS_STATUS_KEYS = [
    'toPublish',
    'delayed',
    'published',
    'remaining',
] as const;
/**
 * A table dynmically fetching and displaying all nodes and their publish queue status details.
 */
@Component({
    selector: 'gtx-widget-publishing-total',
    templateUrl: './widget-publishing-process-total.component.html',
    styleUrls: ['./widget-publishing-process-total.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WidgetPublishingProcessTotalComponent {

    /** If TRUE component polls and refreshs the data display in an intervall defined in `lifeSyncIntervall` */
    @Input()
    lifeSyncEnabled = true;
    
    /** Determines the amount of seconds between polling information. */
    @Input()
    set lifeSyncIntervall(v: number) {
        this._lifeSyncIntervall$.next(v);
    }
    private _lifeSyncIntervall$ = new BehaviorSubject<number>(PUBLISH_PROCESS_REFRESH_INTERVAL);

    /** If TRUE table is in loading state. */
    tableIsLoading = true;

    /** information of the current publish process per node */
    infoStatsTotal$ = new BehaviorSubject<PublishInfo>(null);

    widgetPublishingProcessEntityKeys = WIDGET_PUBLISHING_PROCESS_ENTITY_KEYS;
    widgetPublishingProcessStatusKeys = WIDGET_PUBLISHING_PROCESS_STATUS_KEYS;

    /** inferred data for visualization */
    publishState = {
        file: {
            publishedPercentage: 0,
            isProgressing: true,
        },
        folder: {
            publishedPercentage: 0,
            isProgressing: true,
        },
        form: {
            publishedPercentage: 0,
            isProgressing: true,
        },
        page: {
            publishedPercentage: 0,
            isProgressing: true,
        },
    };

    /** Generic subscription stopper, which is inherited in all components. */
    private stopper = new ObservableStopper();

    constructor(
        private adminOps: AdminOperations,
        private errorHandler: ErrorHandler,
        private changeDetectorRef: ChangeDetectorRef,
    ) {

    }

    ngOnInit(): void {

        // initialize data stream of node publish status info
        this._lifeSyncIntervall$.pipe(
            switchMap(milliseconds => timer(0, milliseconds)),
            filter(() => this.lifeSyncEnabled),
            // start loading indicator
            tap(() => this.tableIsLoading = true),
            // request data
            switchMap(() => this.adminOps.getPublishInfo()),
            // validate response
            filter(data => data instanceof Object),

            catchError(error => this.errorHandler.catch(error)),
            takeUntil(this.stopper.stopper$),
        )
        .subscribe((info: PublishInfo) => {
            // emit latest data
            this.infoStatsTotal$.next(info);

            // set loading indicator
            this.tableIsLoading = false;

            this.widgetPublishingProcessEntityKeys.forEach(entityKey => {
                const _entityData: PublishObjectsCount = info[`${entityKey}s`];
                const _divSum = _entityData.toPublish + _entityData.published;
                // division by zero not allowed
                const _percentage = _divSum > 0 ? Math.ceil((_entityData.published / _divSum) * 100) : 0;
                // if percentage is growing, isProgressing becomes TRUE, otherwise FALSE (for animation control)
                this.publishState[entityKey].isProgressing = this.publishState[entityKey].publishedPercentage < _percentage;
                // assign new percentage
                this.publishState[entityKey].publishedPercentage = _entityData.toPublish > 0 ? _percentage : 0;
            });

            // notify change detection
            this.changeDetectorRef.markForCheck();
        });

    }

}

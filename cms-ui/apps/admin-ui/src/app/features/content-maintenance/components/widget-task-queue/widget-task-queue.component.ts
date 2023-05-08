import { PUBLISH_PROCESS_REFRESH_INTERVAL } from '@admin-ui/common';
import { AdminOperations } from '@admin-ui/core/providers/operations/admin/admin.operations';
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { DirtQueueSummary } from '@gentics/cms-models';
import { isEqual } from 'lodash';
import { BehaviorSubject, Subscription, timer } from 'rxjs';
import { distinctUntilChanged, filter, switchMap } from 'rxjs/operators';

@Component({
    selector: 'gtx-widget-task-queue',
    templateUrl: './widget-task-queue.component.html',
    styleUrls: ['./widget-task-queue.component.scss'],
})
export class WidgetTaskQueueComponent implements OnChanges {

    /** If TRUE component polls and refreshs the data display in an intervall defined in `lifeSyncIntervall` */
    @Input()
    lifeSyncEnabled = true;

    /** Determines the amount of seconds between polling information. */
    @Input()
    public lifeSyncIntervall: number;

    public summary$ = new BehaviorSubject<DirtQueueSummary>(null);

    public dirtQueueItemsFailedAmount$ = new BehaviorSubject<number>(null);

    private syncIntervall$ = new BehaviorSubject<number>(PUBLISH_PROCESS_REFRESH_INTERVAL);

    private subscriptions: Subscription[] = [];

    constructor(
        protected adminOps: AdminOperations,
    ) {
        const intervall$ = this.syncIntervall$.asObservable().pipe(
            distinctUntilChanged(isEqual),
            switchMap(milliseconds => timer(0, milliseconds)),
            filter(() => this.lifeSyncEnabled),
        );

        this.subscriptions.push(intervall$.pipe(
            switchMap(() => this.adminOps.getPublishQueueSummary()),
        ).subscribe(summaries => {
            this.summary$.next(summaries);
        }));

        this.subscriptions.push(intervall$.pipe(
            switchMap(() => this.adminOps.getDirtQueue()),
        ).subscribe(tasks => {
            const failedTasks = tasks.filter(t => t.failed).length;
            this.dirtQueueItemsFailedAmount$.next(failedTasks);
        }));
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.lifeSyncIntervall) {
            this.syncIntervall$.next(this.lifeSyncIntervall);
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }
}

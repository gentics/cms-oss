import { PUBLISH_PROCESS_REFRESH_INTERVAL } from '@admin-ui/common';
import { AdminOperations } from '@admin-ui/core/providers/operations/admin/admin.operations';
import { Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { DirtQueueSummary } from '@gentics/cms-models';
import { coerceToBoolean } from '@gentics/ui-core';
import { isEqual } from'lodash-es'
import { BehaviorSubject, Subscription, timer } from 'rxjs';
import { distinctUntilChanged, filter, startWith, switchMap } from 'rxjs/operators';

@Component({
    selector: 'gtx-widget-task-queue',
    templateUrl: './widget-task-queue.component.html',
    styleUrls: ['./widget-task-queue.component.scss'],
    standalone: false
})
export class WidgetTaskQueueComponent implements OnInit, OnChanges, OnDestroy {

    /** If TRUE component polls and refreshs the data display in an intervall defined in `lifeSyncIntervall` */
    @Input()
    public lifeSyncEnabled = true;

    /** Determines the amount of seconds between polling information. */
    @Input()
    public lifeSyncIntervall: number;

    public summary$ = new BehaviorSubject<DirtQueueSummary>(null);

    public dirtQueueItemsFailedAmount$ = new BehaviorSubject<number>(null);

    private syncIntervall$ = new BehaviorSubject<number>(PUBLISH_PROCESS_REFRESH_INTERVAL);

    private subscriptions: Subscription[] = [];

    constructor(
        protected adminOps: AdminOperations,
    ) { }

    ngOnInit(): void {
        const intervall$ = this.syncIntervall$.asObservable().pipe(
            distinctUntilChanged(isEqual),
            switchMap(milliseconds => timer(0, milliseconds)),
            filter(() => this.lifeSyncEnabled),
        );

        this.subscriptions.push(intervall$.pipe(
            startWith(null),
            switchMap(() => this.adminOps.getPublishQueueSummary()),
        ).subscribe(summaries => {
            this.summary$.next(summaries);
        }));

        this.subscriptions.push(intervall$.pipe(
            startWith(null),
            switchMap(() => this.adminOps.getDirtQueue()),
        ).subscribe(tasks => {
            const failedTasks = tasks.filter(t => t.failed).length;
            this.dirtQueueItemsFailedAmount$.next(failedTasks);
        }));
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.lifeSyncEnabled) {
            this.lifeSyncEnabled = coerceToBoolean(this.lifeSyncEnabled);
        }

        if (changes.lifeSyncIntervall) {
            this.syncIntervall$.next(this.lifeSyncIntervall);
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }
}

import { AdminUIModuleRoutes, PUBLISH_PROCESS_REFRESH_INTERVAL } from '@admin-ui/common';
import { ScheduleOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { PublishInfo, SchedulerStatus } from '@gentics/cms-models';
import { isEqual } from'lodash-es'
import { BehaviorSubject, Subscription, timer } from 'rxjs';
import { distinctUntilChanged, filter, startWith, switchMap } from 'rxjs/operators';
import { I18nService } from '../../../core/providers/i18n/i18n.service';
import { AdminOperations } from '../../../core/providers/operations/admin/admin.operations';
import { SidebarItemComponent } from '../sidebar-item/sidebar-item.component';

@Component({
    selector: 'gtx-widget-publishing-process',
    templateUrl: './widget-publishing-process.component.html',
    styleUrls: ['./widget-publishing-process.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class WidgetPublishingProcessComponent extends SidebarItemComponent implements OnInit, OnChanges, OnDestroy {

    public readonly AdminUIModuleRoutes = AdminUIModuleRoutes;

    @Input()
    public showTitle = false;

    /** If TRUE component polls and refreshs the data display in an intervall defined in `lifeSyncIntervall` */
    @Input()
    public lifeSyncEnabled = true;

    /** Determines the amount of seconds between polling information. */
    @Input()
    public lifeSyncIntervall: number;

    public info: PublishInfo = null;
    public hasFailedJobs = true;
    public publisherStatus: SchedulerStatus = null;

    private syncIntervall$ = new BehaviorSubject<number>(PUBLISH_PROCESS_REFRESH_INTERVAL);

    private subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected i18n: I18nService,
        protected adminOps: AdminOperations,
        protected schedulerOps: ScheduleOperations,
    ) {
        super(i18n);
    }

    ngOnInit(): void {
        const intervall$ = this.syncIntervall$.asObservable().pipe(
            distinctUntilChanged(isEqual),
            switchMap(milliseconds => timer(0, milliseconds)),
            filter(() => this.lifeSyncEnabled),
        );

        this.subscriptions.push(intervall$.pipe(
            startWith(null),
            switchMap(() => this.adminOps.getPublishInfo()),
        ).subscribe(info => {
            this.info = info;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(intervall$.pipe(
            startWith(0),
            switchMap(() => this.schedulerOps.getAll({pageSize: 1, sort: '-edate', failed: true})),
        ).subscribe(info => {
            this.hasFailedJobs = info.length > 0;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(intervall$.pipe(
            startWith(null),
            switchMap(() => this.schedulerOps.status()),
        ).subscribe(res => {
            this.publisherStatus = res.status;
            this.changeDetector.markForCheck();
        }));
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.lifeSyncIntervall) {
            this.syncIntervall$.next(this.lifeSyncIntervall);
        }
    }

    ngOnDestroy(): void {
        super.ngOnDestroy();
        this.subscriptions.forEach(s => s.unsubscribe());
    }
}

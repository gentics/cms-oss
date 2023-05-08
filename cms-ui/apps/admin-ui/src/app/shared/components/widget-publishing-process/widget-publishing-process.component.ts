import { AdminUIModuleRoutes, PUBLISH_PROCESS_REFRESH_INTERVAL } from '@admin-ui/common';
import { Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { PublishInfo } from '@gentics/cms-models';
import { isEqual } from 'lodash';
import { BehaviorSubject, Subscription, timer } from 'rxjs';
import { distinctUntilChanged, filter, startWith, switchMap } from 'rxjs/operators';
import { I18nService } from '../../../core/providers/i18n/i18n.service';
import { AdminOperations } from '../../../core/providers/operations/admin/admin.operations';
import { SidebarItemComponent } from '../sidebar-item/sidebar-item.component';

@Component({
    selector: 'gtx-widget-publishing-process',
    templateUrl: './widget-publishing-process.component.html',
    styleUrls: ['./widget-publishing-process.component.scss'],
})
export class WidgetPublishingProcessComponent extends SidebarItemComponent implements OnInit, OnChanges, OnDestroy {

    public readonly AdminUIModuleRoutes = AdminUIModuleRoutes;

    @Input()
    showTitle = false;

    /** If TRUE component polls and refreshs the data display in an intervall defined in `lifeSyncIntervall` */
    @Input()
    lifeSyncEnabled = true;

    /** Determines the amount of seconds between polling information. */
    @Input()
    public lifeSyncIntervall: number;

    info$ = new BehaviorSubject<PublishInfo>(null);
    hasFailedJobs$ = new BehaviorSubject<boolean>(true);

    private syncIntervall$ = new BehaviorSubject<number>(PUBLISH_PROCESS_REFRESH_INTERVAL);

    private subscriptions: Subscription[] = [];

    constructor(
        protected i18n: I18nService,
        protected adminOps: AdminOperations,
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
            switchMap(() => this.adminOps.getPublishInfo()),
        ).subscribe(info => {
            this.info$.next(info);
        }));

        this.subscriptions.push(intervall$.pipe(
            startWith(0),
            switchMap(() => this.adminOps.getJobs({failed: true})),
        ).subscribe(info => {
            this.hasFailedJobs$.next(info.items.length > 0);
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

import { PUBLISH_PROCESS_REFRESH_INTERVAL } from '@admin-ui/common';
import { AppStateService, SelectState, SetUserSettingAction } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Observable, Subscription, combineLatest } from 'rxjs';
import { filter, mergeMap } from 'rxjs/operators';

export enum ContentMaintenanceTabs {
    GENERAL = 'general',
    FAILED_TASKS = 'failedTasks',
}

@Component({
    selector: 'gtx-content-maintenance',
    templateUrl: './content-maintenance.component.html',
    styleUrls: ['./content-maintenance.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContentMaintenanceComponent implements OnInit, OnDestroy {

    readonly ContentMaintenanceTabs = ContentMaintenanceTabs;

    @SelectState(state => state.loading.masterLoading)
    isLoading$: Observable<boolean>;

    activeTabId: ContentMaintenanceTabs = ContentMaintenanceTabs.GENERAL;

    /** Node IDs selected for ction request. */
    selectedNodeIds: number[] = [];

    /** If TRUE components in this view poll and refresh the data they display in an intervall defined in `lifeSyncIntervall` */
    lifeSyncEnabled = false;

    /** Determines the amount of seconds of components in this view between polling information. */
    lifeSyncIntervall = PUBLISH_PROCESS_REFRESH_INTERVAL;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private appState: AppStateService,
    ) {}

    ngOnInit(): void {
        this.subscriptions.push(combineLatest([
            this.appState.select(state => state.auth.isLoggedIn),
            this.appState.select(state => state.auth.currentUserId),
        ]).pipe(
            filter(([loggedIn]) => loggedIn),
            mergeMap(([, userId]) => this.appState.select(state => state.ui.settings[userId])),
        ).subscribe(userSettings => {
            this.lifeSyncEnabled = userSettings.pollContentMaintenance ?? false;
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    changeLifeSyncPoll(doPoll: boolean): void {
        this.appState.dispatch(new SetUserSettingAction('pollContentMaintenance', doPoll));
    }

    changeTab(activeTabId: ContentMaintenanceTabs): void {
        this.activeTabId = activeTabId;
        this.changeDetector.markForCheck();
    }
}

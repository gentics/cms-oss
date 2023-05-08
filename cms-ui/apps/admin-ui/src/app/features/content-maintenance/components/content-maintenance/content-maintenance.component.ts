import { PUBLISH_PROCESS_REFRESH_INTERVAL } from '@admin-ui/common';
import { SelectState } from '@admin-ui/state';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Observable } from 'rxjs';

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
export class ContentMaintenanceComponent {

    readonly ContentMaintenanceTabs = ContentMaintenanceTabs;

    @SelectState(state => state.loading.masterLoading)
    isLoading$: Observable<boolean>;

    activeTabId: ContentMaintenanceTabs = ContentMaintenanceTabs.GENERAL;

    /** Node IDs selected for ction request. */
    selectedNodeIds: number[] = [];

    /** If TRUE components in this view poll and refresh the data they display in an intervall defined in `lifeSyncIntervall` */
    lifeSyncEnabled = true;

    /** Determines the amount of seconds of components in this view between polling information. */
    lifeSyncIntervall = PUBLISH_PROCESS_REFRESH_INTERVAL;

    changeTab(activeTabId: ContentMaintenanceTabs): void {
        this.activeTabId = activeTabId;
    }
}

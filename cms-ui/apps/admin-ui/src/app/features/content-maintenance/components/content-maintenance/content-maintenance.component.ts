import { AdminHandlerService, NodeOperations, NodeTableLoaderService, ScheduleHandlerService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { DirtQueueEntry, DirtQueueResponse, DirtQueueSummaryResponse, Node, PublishInfo, PublishQueue, SchedulerStatus } from '@gentics/cms-models';
import { forkJoin, Subscription } from 'rxjs';

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

    public readonly ContentMaintenanceTabs = ContentMaintenanceTabs;

    public activeTabId: ContentMaintenanceTabs = ContentMaintenanceTabs.GENERAL;

    public loading = false;

    public dirtQueue: DirtQueueResponse;
    public dirtQueueSummary: DirtQueueSummaryResponse;
    public failedTasks: DirtQueueEntry[] = [];

    public publishInfo: PublishInfo;
    public publishQueue: PublishQueue;

    public schedulerStatus: SchedulerStatus;
    public hasFailedSchedules = false;

    public nodes: Node[] = [];

    /** Node IDs selected for ction request. */
    public selectedNodeIds: string[] = [];

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private handler: AdminHandlerService,
        private schedule: ScheduleHandlerService,
        private nodeOps: NodeOperations,
        private nodeTable: NodeTableLoaderService,
    ) {}

    ngOnInit(): void {
        this.loadData();
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    public loadData(): void {
        if (this.loading) {
            return;
        }

        this.loading = true;
        this.changeDetector.markForCheck();

        this.nodeTable.reload();

        this.subscriptions.push(forkJoin([
            this.handler.getDirtQueue(),
            this.handler.getDirtQueueSummary(),

            this.handler.getPublishInfo(),
            this.handler.getPublishQueue(),

            this.schedule.status(),
            this.schedule.list(null, { pageSize: 0, sort: '-edate', failed: true }),

            this.nodeOps.getAll(),
        ]).subscribe(([dirtQueue, summary, publishInfo, publishQueue, schedulerStatus, failedSchedules , nodes]) => {
            this.dirtQueue = dirtQueue;
            this.failedTasks = dirtQueue.items.filter(item => item.failed);
            this.dirtQueueSummary = summary;

            this.publishInfo = publishInfo;
            this.publishQueue = publishQueue;

            this.schedulerStatus = schedulerStatus.status;
            this.hasFailedSchedules = failedSchedules.numItems > 0;

            this.nodes = nodes;

            this.loading = false;
            this.changeDetector.markForCheck();
        }));
    }

    changeTab(activeTabId: ContentMaintenanceTabs): void {
        this.activeTabId = activeTabId;
        this.changeDetector.markForCheck();
    }
}

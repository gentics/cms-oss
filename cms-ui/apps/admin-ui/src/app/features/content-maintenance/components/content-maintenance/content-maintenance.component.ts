import { EntityTableActionClickEvent } from '@admin-ui/common';
import { AdminHandlerService, I18nNotificationService, NodeOperations, NodeTableLoaderService, ScheduleHandlerService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { DirtQueueEntry, DirtQueueResponse, DirtQueueSummaryResponse, Node, PublishInfo, PublishQueue, SchedulerStatus } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { forkJoin, Subscription } from 'rxjs';
import { MaintenanceActionModalAction, MaintenanceActionModalComponent } from '../maintenance-action-modal/maintenance-action-modal.component';

export enum ContentMaintenanceTabs {
    GENERAL = 'general',
    FAILED_TASKS = 'failedTasks',
}

@Component({
    selector: 'gtx-content-maintenance',
    templateUrl: './content-maintenance.component.html',
    styleUrls: ['./content-maintenance.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class ContentMaintenanceComponent implements OnInit, OnDestroy {

    public readonly ContentMaintenanceTabs = ContentMaintenanceTabs;

    public activeTabId: ContentMaintenanceTabs = ContentMaintenanceTabs.GENERAL;

    public loading = false;
    public working = false;

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
        private modals: ModalService,
        private notification: I18nNotificationService,
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
        ]).subscribe(([dirtQueue, summary, publishInfo, publishQueue, schedulerStatus, failedSchedules, nodes]) => {
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

    public async reloadConfiguration(): Promise<void> {
        if (this.working) {
            return;
        }

        this.working = true;
        this.changeDetector.markForCheck();

        try {
            await this.handler.reloadConfiguration().toPromise();
        } catch (err) {
            // Nothing to do
        }

        this.working = false;
        this.changeDetector.markForCheck();
    }

    public async stopPublishing(): Promise<void> {
        if (this.working) {
            return;
        }

        this.working = true;
        this.changeDetector.markForCheck();

        try {
            await this.handler.stopPublishing();
        } catch (err) {
            // Nothing to do
        }

        this.working = false;
        this.changeDetector.markForCheck();
    }

    public async handleTableAction(event: EntityTableActionClickEvent<Node>): Promise<void> {
        const ids = Object.values(MaintenanceActionModalAction);
        if (!ids.includes(event.actionId as any)) {
            return;
        }

        const items = event.selection ? event.selectedItems : [event.item];
        if (items.length === 0) {
            this.notification.show({
                message: 'contentmaintenance.error_no_nodes_selected',
                type: 'warning',
            });
            return;
        }

        const modal = await this.modals.fromComponent(MaintenanceActionModalComponent, {
            closeOnOverlayClick: false,
            width: '50%',
        }, {
            modalAction: event.actionId as any,
            selectedNodeIds: items.map(node => node.id),
        });

        await modal.open();
    }
}

import { EntityTableActionClickEvent } from '@admin-ui/common';
import { AdminHandlerService, I18nNotificationService, I18nService, NodeOperations, PermissionsService, ScheduleHandlerService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import {
    AccessControlledType,
    DirtQueueEntry,
    DirtQueueSummaryResponse,
    GcmsPermission,
    Node,
    PublishInfo,
    PublishQueue,
    ResponseCode,
    SchedulerStatus,
} from '@gentics/cms-models';
import { GCMSRestClientRequestError } from '@gentics/cms-rest-client';
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

    public working = false;

    // Queue summary content
    public dirtQueueLoading = false;
    public dirtQueueError: string | null = null;

    public dirtQueueSummary: DirtQueueSummaryResponse;
    public failedTasks: DirtQueueEntry[] = [];

    // Publish process
    public publishProcessLoading = false;
    public publishProcessError: string | null = null;

    public publishInfo: PublishInfo;
    public hasFailedSchedules = false;
    public schedulerStatus: SchedulerStatus;

    // Nodes
    public nodesLoading = false;
    public nodesError: string | null = null;

    public nodes: Node[] = [];

    // Publish queue
    public publishQueueLoading = false;
    public publishQueueError: string | null = null;

    public publishQueue: PublishQueue;

    // Permissions
    public modifyPermissions = false;

    /** Node IDs selected for ction request. */
    public selectedNodeIds: string[] = [];

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private handler: AdminHandlerService,
        private schedule: ScheduleHandlerService,
        private nodeOps: NodeOperations,
        private modals: ModalService,
        private notification: I18nNotificationService,
        private permissions: PermissionsService,
        private i18n: I18nService,
    ) {}

    ngOnInit(): void {
        this.loadData();
        this.loadPermissions();
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    public loadDirtQueueData(): void {
        if (this.dirtQueueLoading) {
            return;
        }

        this.dirtQueueLoading = true;
        this.dirtQueueError = null;
        this.changeDetector.markForCheck();

        this.subscriptions.push(forkJoin([
            this.handler.getDirtQueue(),
            this.handler.getDirtQueueSummary(),
        ]).subscribe({
            next: ([dirtQueue, summary]) => {
                this.dirtQueueSummary = summary;
                this.failedTasks = dirtQueue.items.filter(item => item.failed);

                this.dirtQueueLoading = false;
                this.dirtQueueError = null
                this.changeDetector.markForCheck();
            },
            error: err => {
                console.error(err);
                this.dirtQueueSummary = null;
                this.failedTasks = [];

                this.dirtQueueLoading = false;
                this.dirtQueueError = this.getErrorMessage(err);
                this.changeDetector.markForCheck();
            },
        }))
    }

    public loadPublishProcessData(): void {
        // Don't start when it's already loading
        if (this.publishProcessLoading) {
            return;
        }

        this.publishProcessLoading = true;
        this.publishProcessError = null;
        this.changeDetector.markForCheck();

        this.subscriptions.push(forkJoin([
            this.handler.getPublishInfo(),
            this.schedule.status(),
            this.schedule.list(null, { pageSize: 0, sort: '-edate', failed: true }),
        ]).subscribe({
            next: ([publishInfo, schedulerStatus, failedSchedules]) => {
                this.schedulerStatus = schedulerStatus.status;
                this.hasFailedSchedules = failedSchedules.numItems > 0;
                this.publishInfo = publishInfo;

                this.publishProcessLoading = false;
                this.publishProcessError = null;
                this.changeDetector.markForCheck();
            },
            error: err => {
                console.error(err);
                this.schedulerStatus = null;
                this.hasFailedSchedules = false;
                this.publishInfo = null;

                this.publishProcessLoading = false;
                this.publishProcessError = this.getErrorMessage(err);
                this.changeDetector.markForCheck();
            },
        }));
    }

    public loadNodes(): void {
        if (this.nodesLoading) {
            return;
        }

        this.nodesLoading = true;
        this.nodesError = null;
        this.changeDetector.markForCheck();

        this.subscriptions.push(forkJoin([
            this.nodeOps.getAll(),
        ]).subscribe({
            next: ([nodes]) => {
                this.nodes = nodes;

                this.nodesLoading = false;
                this.nodesError = null;
                this.changeDetector.markForCheck();
            },
            error: err => {
                console.error(err);
                this.nodes = [];

                this.nodesLoading = false;
                this.nodesError = this.getErrorMessage(err);
                this.changeDetector.markForCheck();
            },
        }));
    }

    public loadPublishQueueData(): void {
        if (this.publishQueueLoading) {
            return;
        }

        this.publishQueueLoading = true;
        this.publishQueueError = null;
        this.changeDetector.markForCheck();

        this.subscriptions.push(forkJoin([
            this.handler.getPublishQueue(),
        ]).subscribe({
            next: ([publishQueue]) => {
                this.publishQueue = publishQueue;

                this.publishQueueLoading = false;
                this.publishQueueError = null;
                this.changeDetector.markForCheck();
            },
            error: err => {
                console.error(err);
                this.publishQueue = null;

                this.publishQueueLoading = false;
                this.publishQueueError = this.getErrorMessage(err);
                this.changeDetector.markForCheck();
            },
        }));
    }

    public loadPermissions(): void {
        this.subscriptions.push(this.permissions.checkPermissions([{
            type: AccessControlledType.CONTENT_ADMIN,
            permissions: [GcmsPermission.READ],
        }]).subscribe(hasPerm => {
            this.modifyPermissions = hasPerm;
        }))
    }

    public loadData(): void {
        this.loadDirtQueueData();
        this.loadPublishProcessData();
        this.loadNodes();
        this.loadPublishQueueData();
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

    private getErrorMessage(error: Error): string {
        if (!(error instanceof GCMSRestClientRequestError)) {
            return this.i18n.instant('common.loading_error');
        }

        if (error.data?.responseInfo?.responseCode === ResponseCode.PERMISSION) {
            return this.i18n.instant('common.general_permission_required');
        }

        return this.i18n.instant('common.loading_error');
    }
}

import { DELETE_ACTION, DirtQueueItemBO } from '@admin-ui/common';
import { AdminOperations, I18nService } from '@admin-ui/core';
import { BaseEntityTableComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { AnyModelType, DirtQueueItem, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { ModalService, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { DirtQueueItemTableLoaderService } from '../../providers';

const REDO_TASK_ACTION = 'redoAction';

@Component({
    selector: 'gtx-dirt-queue-item-table',
    templateUrl: './dirt-queue-item-table.component.html',
    styleUrls: ['./dirt-queue-item-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DirtQueueItemTableComponent extends BaseEntityTableComponent<DirtQueueItem, DirtQueueItemBO> {

    protected rawColumns: TableColumn<DirtQueueItemBO>[] = [
        {
            id: 'label',
            label: 'contentmaintenance.label',
            fieldPath: 'label',
        },
        {
            id: 'objectType',
            label: 'contentmaintenance.object_type',
            fieldPath: 'objType',
        },
        {
            id: 'objectId',
            label: 'contentmaintenance.object_id',
            fieldPath: 'objId',
        },
        {
            id: 'timestamp',
            label: 'contentmaintenance.timestamp',
            fieldPath: 'timestamp',
        },
        {
            id: 'failed',
            label: 'contentmaintenance.failed',
            fieldPath: 'failed',
        },
        {
            id: 'failReason',
            label: 'contentmaintenance.failReason',
            fieldPath: 'failReason',
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = null;

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: DirtQueueItemTableLoaderService,
        modalService: ModalService,
        protected operations: AdminOperations,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );

        this.applyActions([
            {
                id: REDO_TASK_ACTION,
                icon: 'redo',
                label: this.i18n.instant('shared.repeat_failed_dirt_queue_of_node'),
                type: 'secondary',
                enabled: true,
                single: true,
                multiple: true,
            },
            {
                id: DELETE_ACTION,
                icon: 'delete',
                label: this.i18n.instant('shared.delete_failed_dirt_queue_of_node'),
                type: 'alert',
                enabled: true,
                single: true,
                multiple: true,
            },
        ]);
    }

    public override handleAction(event: TableActionClickEvent<DirtQueueItemBO>): void {
        switch (event.actionId) {
            case REDO_TASK_ACTION:
                this.repeatFailedDirtQueueOfNode(this.getAffectedEntityIds(event));
                return;
        }

        super.handleAction(event);
    }

    repeatFailedDirtQueueOfNode(actionIds: (number | string)[]): void {
        Promise.all(actionIds.map(actionId => {
            return this.operations.repeatFailedDirtQueueOfNode(actionId).toPromise();
        }));
    }

    async deleteAll(): Promise<void> {
        // TODO: Once GPU-734 is resolved, use new endpoint
        const allTasks = await this.operations.getDirtQueue({ failed: true }).toPromise();

        if (allTasks.length === 0) {
            return;
        }

        for (const task of allTasks) {
            await this.operations.deleteFailedDirtQueueOfNode(task.id).toPromise();
        }
    }
}

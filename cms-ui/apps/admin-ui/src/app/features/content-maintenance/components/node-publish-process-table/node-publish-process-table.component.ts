import { NodeBO } from '@admin-ui/common';
import { I18nService, NodeTableLoaderService } from '@admin-ui/core';
import { BaseEntityTableComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { AnyModelType, Node, NormalizableEntityTypesMap, PublishQueue } from '@gentics/cms-models';
import { ModalService, TableAction, TableColumn } from '@gentics/ui-core';
import { map, Observable } from 'rxjs';
import { PUBLISH_PLURAL_MAPPING } from '../../models';
import { MaintenanceActionModalAction } from '../maintenance-action-modal/maintenance-action-modal.component';

@Component({
    selector: 'gtx-node-publish-process-table',
    templateUrl: './node-publish-process-table.component.html',
    styleUrls: ['./node-publish-process-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class NodePublishProcessTableComponent extends BaseEntityTableComponent<Node, NodeBO> {

    public readonly PUBLISH_PLURAL_MAPPING = PUBLISH_PLURAL_MAPPING;

    @Input()
    public publishQueue: PublishQueue;

    protected rawColumns: TableColumn<NodeBO>[] = [
        {
            id: 'name',
            label: 'node.node_name',
            fieldPath: 'name',
            sortable: true,
        },
    ];

    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'node';

    public expandedNodes = new Set<string>();

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: NodeTableLoaderService,
        modalService: ModalService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        )
    }

    protected createTableActionLoading(): Observable<TableAction<NodeBO>[]> {
        // Override me when needed
        return this.actionRebuildTrigger$.pipe(
            map(() => {
                const actions: TableAction<NodeBO>[] = [
                    {
                        id: MaintenanceActionModalAction.REPUBLISH_OBJECTS,
                        enabled: true,
                        icon: 'refresh',
                        label: this.i18n.instant('contentmaintenance.republish_objects'),
                        type: 'primary',
                        single: true,
                        multiple: true,
                    },
                    {
                        id: MaintenanceActionModalAction.DELAY_OBJECTS,
                        enabled: true,
                        icon: 'schedule',
                        label: this.i18n.instant('contentmaintenance.delay_objects'),
                        single: true,
                        multiple: true,
                    },
                    {
                        id: MaintenanceActionModalAction.REPUBLISH_DELAYED_OBJECTS,
                        enabled: true,
                        icon: 'history',
                        label: this.i18n.instant('contentmaintenance.republish_delayed_objects'),
                        type: 'warning',
                        single: true,
                        multiple: true,
                    },
                    {
                        id: MaintenanceActionModalAction.MARK_OBJECTS_AS_PUBLISHED,
                        enabled: true,
                        icon: 'approval',
                        label: this.i18n.instant('contentmaintenance.mark_objects_as_published'),
                        type: 'warning',
                        single: true,
                        multiple: true,
                    },
                ];

                return actions;
            }),
        );
    }

    public updateExpandedNodes(id: string, open: boolean): void {
        if (open) {
            this.expandedNodes.add(id);
        } else {
            this.expandedNodes.delete(id);
        }
    }
}

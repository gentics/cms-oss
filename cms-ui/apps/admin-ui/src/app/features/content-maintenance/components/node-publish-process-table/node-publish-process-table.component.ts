import { NodeBO, TableLoadOptions, TableLoadResponse } from '@admin-ui/common';
import { NodeTableLoaderService } from '@admin-ui/core';
import { BaseEntityTableComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges } from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import { AnyModelType, Node, NormalizableEntityTypesMap, PublishQueue } from '@gentics/cms-models';
import { ChangesOf, ModalService, TableAction, TableColumn } from '@gentics/ui-core';
import { map, Observable, of } from 'rxjs';
import { PUBLISH_PLURAL_MAPPING } from '../../models';
import { MaintenanceActionModalAction } from '../maintenance-action-modal/maintenance-action-modal.component';

@Component({
    selector: 'gtx-node-publish-process-table',
    templateUrl: './node-publish-process-table.component.html',
    styleUrls: ['./node-publish-process-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class NodePublishProcessTableComponent extends BaseEntityTableComponent<Node, NodeBO> implements OnChanges {

    public readonly PUBLISH_PLURAL_MAPPING = PUBLISH_PLURAL_MAPPING;

    @Input()
    public publishQueue: PublishQueue;

    @Input()
    public nodes: Node[] = [];

    @Input()
    public nodesLoading = false;

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

    // eslint-disable-next-line @typescript-eslint/no-useless-constructor
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
        );
    }

    override ngOnChanges(changes: ChangesOf<this>): void {
        super.ngOnChanges(changes);

        if (changes.nodes) {
            this.loadTrigger.next();
        }
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
                        label: this.i18n.instant('content_maintenance.republish_objects'),
                        type: 'primary',
                        single: true,
                        multiple: true,
                    },
                    {
                        id: MaintenanceActionModalAction.DELAY_OBJECTS,
                        enabled: true,
                        icon: 'schedule',
                        label: this.i18n.instant('content_maintenance.delay_objects'),
                        single: true,
                        multiple: true,
                    },
                    {
                        id: MaintenanceActionModalAction.REPUBLISH_DELAYED_OBJECTS,
                        enabled: true,
                        icon: 'history',
                        label: this.i18n.instant('content_maintenance.republish_delayed_objects'),
                        type: 'warning',
                        single: true,
                        multiple: true,
                    },
                    {
                        id: MaintenanceActionModalAction.MARK_OBJECTS_AS_PUBLISHED,
                        enabled: true,
                        icon: 'approval',
                        label: this.i18n.instant('content_maintenance.mark_objects_as_published'),
                        type: 'warning',
                        single: true,
                        multiple: true,
                    },
                ];

                return actions;
            }),
        );
    }

    protected override loadTablePage(options: TableLoadOptions, _additionalOptions?: never): Observable<TableLoadResponse<NodeBO>> {
        let sourceNodes = this.nodes;

        if ((options.query || '').trim() !== '') {
            sourceNodes = sourceNodes.filter(node => node.name.includes(options.query));
        }

        const rows = sourceNodes.map(node => {
            const bo = (this.loader as NodeTableLoaderService).mapToBusinessObject(node);
            return this.loader.mapToTableRow(bo);
        })

        return of({
            rows,
            totalCount: rows.length,
            hasError: false,
        });
    }

    public updateExpandedNodes(id: string, open: boolean): void {
        if (open) {
            this.expandedNodes.add(id);
        } else {
            this.expandedNodes.delete(id);
        }
    }
}

import { AdminUIEntityDetailRoutes, BO_PERMISSIONS, NodeBO } from '@admin-ui/common';
import {
    I18nNotificationService,
    I18nService,
    NodeOperations,
    PermissionsService,
} from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { AnyModelType, GcmsPermission, Node, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { ModalService, TableAction, TableColumn } from '@gentics/ui-core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { NodeTableLoaderService } from '../../providers/node-table-loader/node-table-loader.service';
import { WizardService } from '../../providers/wizard/wizard.service';
import { BaseEntityTableComponent, DELETE_ACTION } from '../base-entity-table/base-entity-table.component';

@Component({
    selector: 'gtx-node-table',
    templateUrl: './node-table.component.html',
    styleUrls: ['./node-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeTableComponent extends BaseEntityTableComponent<Node, NodeBO> {

    public readonly AdminUIEntityDetailRoutes = AdminUIEntityDetailRoutes;

    protected rawColumns: TableColumn<NodeBO>[] = [
        {
            id: 'name',
            label: 'node.node_name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'derivedFrom',
            label: 'node.derived_from',
            fieldPath: 'inheritedFromId',
            sortable: false,
        },
        {
            id: 'publishToCr',
            label: 'node.pub_content_repository',
            fieldPath: 'publishContentMap',
            align: 'center',
            sortable: false,
        },
        {
            id: 'contentRepository',
            label: 'node.content_repository',
            fieldPath: 'contentRepositoryId',
            sortable: false,
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'node';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: NodeTableLoaderService,
        modalService: ModalService,
        protected permissions: PermissionsService,
        protected wizardService: WizardService,
        protected notification: I18nNotificationService,
        protected operations: NodeOperations,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        )
    }

    protected override createTableActionLoading(): Observable<TableAction<NodeBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
        ]).pipe(
            map(() => {
                return [
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('shared.delete'),
                        type: 'alert',
                        single: true,
                        multiple: true,
                        enabled: (item) => {
                            if (!item) {
                                return true;
                            } else {
                                const perms = (item?.[BO_PERMISSIONS] || []);
                                return perms.includes(GcmsPermission.READ) && perms.includes(GcmsPermission.DELETE);
                            }
                        },
                    },
                ];
            }),
        );
    }
}

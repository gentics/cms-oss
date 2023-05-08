import { BO_PERMISSIONS, NodeBO } from '@admin-ui/common';
import {
    I18nNotificationService,
    I18nService,
    NodeOperations,
    NodeTableLoaderService,
    PermissionsService
} from '@admin-ui/core';
import { WizardService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { AnyModelType, GcmsPermission, Node, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { ModalService, TableColumn } from '@gentics/ui-core';
import { BaseEntityTableComponent } from '../base-entity-table/base-entity-table.component';

const DELETE_ACTION = 'delete';

@Component({
    selector: 'gtx-node-table',
    templateUrl: './node-table.component.html',
    styleUrls: ['./node-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeTableComponent extends BaseEntityTableComponent<Node, NodeBO> implements OnInit {

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

    ngOnInit(): void {
        super.ngOnInit();

        this.applyActions([
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
        ]);
    }
}

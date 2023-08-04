import { BO_PERMISSIONS } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { MeshRoleBO } from '@admin-ui/mesh/common';
import { MeshRoleTableLoaderService } from '@admin-ui/mesh/providers';
import { BaseEntityTableComponent, DELETE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { AnyModelType, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { Permission, Role } from '@gentics/mesh-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MeshRoleModal } from '../mesh-role-modal/mesh-role-modal.component';
import { MeshRolePropertiesMode } from '../mesh-role-properties/mesh-role-properties.component';

const EDIT_ACTION = 'edit';

@Component({
    selector: 'gtx-mesh-role-table',
    templateUrl: './mesh-role-table.component.html',
    styleUrls: ['./mesh-role-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshRoleTableComponent extends BaseEntityTableComponent<Role, MeshRoleBO> {

    protected rawColumns: TableColumn<MeshRoleBO>[] = [
        {
            id: 'name',
            label: 'role.name',
            fieldPath: 'name',
            sortable: true,
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'role';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: MeshRoleTableLoaderService,
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

    protected override createTableActionLoading(): Observable<TableAction<MeshRoleBO>[]> {
        // Override me when needed
        return this.actionRebuildTrigger$.pipe(
            map(() => {
                const actions: TableAction<MeshRoleBO>[] = [
                    {
                        id: EDIT_ACTION,
                        icon: 'edit',
                        label: this.i18n.instant('common.edit'),
                        enabled: (item) => item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'primary',
                        single: true,
                    },
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('shared.delete'),
                        enabled: (item) => item == null || item[BO_PERMISSIONS].includes(Permission.DELETE),
                        type: 'alert',
                        multiple: true,
                        single: true,
                    },
                ];

                return actions;
            }),
        );
    }

    public override handleCreateButton(): void {
        this.openModal(MeshRolePropertiesMode.CREATE);
    }

    public override handleAction(event: TableActionClickEvent<MeshRoleBO>): void {
        switch (event.actionId) {
            case EDIT_ACTION:
                this.openModal(MeshRolePropertiesMode.EDIT, event.item);
                return;
        }

        super.handleAction(event);
    }

    async openModal(mode: MeshRolePropertiesMode, role?: Role): Promise<void> {
        const dialog = await this.modalService.fromComponent(MeshRoleModal, {}, {
            mode,
            role,
        });
        const res = await dialog.open();
        if (res) {
            this.reload();
        }
    }
}

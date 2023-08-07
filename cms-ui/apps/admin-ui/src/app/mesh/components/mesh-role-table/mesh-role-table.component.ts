import { BO_PERMISSIONS } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { MeshGroupBO, MeshRoleBO } from '@admin-ui/mesh/common';
import { MeshGroupHandlerService, MeshRoleTableLoaderService } from '@admin-ui/mesh/providers';
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
import { SelectGroupModal } from '../select-group-modal/select-group-modal.component';

const EDIT_ACTION = 'edit';
const ASSIGN_TO_GROUPS_ACTION = 'assignToGroups';
const UNASSIGN_FROM_GROUPS_ACTION = 'unassignFromGroup';
const MANAGE_GROUPS_ACTION = 'manageGroups';

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
            label: 'common.name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'groups',
            label: 'common.group_plural',
            // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
            mapper: (role: MeshRoleBO) => (role.groups || []).map(group => group.name).filter(name => !!name).join(', '),
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'role';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: MeshRoleTableLoaderService,
        modalService: ModalService,
        protected handler: MeshGroupHandlerService,
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
                        id: MANAGE_GROUPS_ACTION,
                        icon: 'group',
                        label: this.i18n.instant('mesh.manage_group_assignment'),
                        enabled: (item) => item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'secondary',
                        single: true,
                    },
                    {
                        id: ASSIGN_TO_GROUPS_ACTION,
                        icon: 'link',
                        label: this.i18n.instant('mesh.assignRolesToGroups'),
                        enabled: true,
                        type: 'secondary',
                        multiple: true,
                    },
                    {
                        id: UNASSIGN_FROM_GROUPS_ACTION,
                        icon: 'link_off',
                        label: this.i18n.instant('mesh.unassignRolesFromGroups'),
                        enabled: true,
                        type: 'secondary',
                        multiple: true,
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

            case MANAGE_GROUPS_ACTION:
                this.manageGroupAssignment(event.item);
                return;

            case ASSIGN_TO_GROUPS_ACTION:
                this.handleAssignToGroupsAction(this.getAffectedEntityIds(event));
                return;

            case UNASSIGN_FROM_GROUPS_ACTION:
                this.handleUnassignToGroupsAction(this.getAffectedEntityIds(event));
                return;
        }

        super.handleAction(event);
    }

    async manageGroupAssignment(role: MeshRoleBO): Promise<void> {
        const assignedGroupIds = role.groups.map(group => group.uuid);

        const dialog = await this.modalService.fromComponent(SelectGroupModal, {}, {
            title: 'mesh.manage_group_assignment',
            multiple: true,
            selected: (role.groups || []).map(group => group.uuid),
        });

        const groups: MeshGroupBO[] = await dialog.open();
        const newGroupIds = groups.map(group => group.uuid);

        const toAssign = groups.filter(group => !assignedGroupIds.includes(group.uuid));
        const toRemove = role.groups.filter(group => !newGroupIds.includes(group.uuid));

        // Nothing to do
        if (toAssign.length === 0 && toRemove.length === 0) {
            return;
        }

        for (const group of toAssign) {
            this.handler.assignRoleToGroup(role, group);
        }
        for (const group of toRemove) {
            this.handler.unassignRoleFromGroup(role, group);
        }

        this.reload();
    }

    async handleAssignToGroupsAction(roleIds: string[]): Promise<void> {
        const roles = this.loader.getEntitiesByIds(roleIds);
        const dialog = await this.modalService.fromComponent(SelectGroupModal, {}, {
            title: 'mesh.assignRolesToGroups',
            multiple: true,
        });

        const groups: MeshGroupBO[] = await dialog.open();
        if (groups.length === 0) {
            return;
        }

        for (const group of groups) {
            for (const role of roles) {
                this.handler.assignRoleToGroup(role, group);
            }
        }

        this.reload();
    }

    async handleUnassignToGroupsAction(roleIds: string[]): Promise<void> {
        const roles = this.loader.getEntitiesByIds(roleIds);
        const dialog = await this.modalService.fromComponent(SelectGroupModal, {}, {
            title: 'mesh.unassignRolesFromGroups',
            multiple: true,
        });

        const groups: MeshGroupBO[] = await dialog.open();
        if (groups.length === 0) {
            return;
        }

        for (const group of groups) {
            for (const role of roles) {
                this.handler.unassignRoleFromGroup(role, group);
            }
        }

        this.reload();
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

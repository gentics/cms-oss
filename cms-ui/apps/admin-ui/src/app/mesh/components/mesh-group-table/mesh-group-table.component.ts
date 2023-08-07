import { BO_PERMISSIONS, BusinessObject } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { MeshGroupBO, MeshRoleBO } from '@admin-ui/mesh/common';
import { MeshGroupHandlerService, MeshGroupTableLoaderOptions, MeshGroupTableLoaderService } from '@admin-ui/mesh/providers';
import { BaseEntityTableComponent, DELETE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { AnyModelType, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { Group, GroupResponse, Permission, User } from '@gentics/mesh-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MeshGroupModal } from '../mesh-group-modal/mesh-group-modal.component';
import { MeshGroupPropertiesMode } from '../mesh-group-properties/mesh-group-properties.component';
import { SelectRoleModal } from '../select-role-modal/select-role-modal.component';

const EDIT_ACTION = 'edit';
const ASSIGN_TO_USERS_ACTION = 'assignToUsers';
const UNASSIGN_FROM_USERS_ACTION = 'unassignFromUsers';
const MANAGE_USERS_ACTION = 'manageUsers';
const MANAGE_ROLES_ACTION = 'manageRoles';

function getUserName(user: User): string {
    let out = '';

    if (user.firstname) {
        out = user.firstname;
    }

    if (user.lastname) {
        if (out !== '') {
            out += ' ';
        }
        out += user.lastname;
    }

    if (user.username) {
        if (out !== '') {
            out += ` (${user.username})`;
        } else {
            out = user.username;
        }
    }

    if (out === '') {
        out = `${user.uuid}`;
    }

    return out;
}

@Component({
    selector: 'gtx-mesh-group-table',
    templateUrl: './mesh-group-table.component.html',
    styleUrls: ['./mesh-group-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshGroupTableComponent extends BaseEntityTableComponent<Group, MeshGroupBO, MeshGroupTableLoaderOptions> {

    protected rawColumns: TableColumn<GroupResponse & BusinessObject>[] = [
        {
            id: 'name',
            label: 'common.name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'users',
            label: 'common.user_plural',
            // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
            mapper: (group: MeshGroupBO) => (group.users).map(getUserName).filter(name => !!name).join(', '),
        },
        {
            id: 'roles',
            label: 'common.role_plural',
            // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
            mapper: (group: MeshGroupBO) => (group.roles || []).map(role => role.name).filter(name => !!name).join(', '),
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'group';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: MeshGroupTableLoaderService,
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

    protected override createAdditionalLoadOptions(): MeshGroupTableLoaderOptions {
        return {
            users: true,
        };
    }

    public override handleCreateButton(): void {
        this.openModal(MeshGroupPropertiesMode.CREATE);
    }

    public override handleAction(event: TableActionClickEvent<MeshGroupBO>): void {
        switch (event.actionId) {
            case EDIT_ACTION:
                this.openModal(MeshGroupPropertiesMode.EDIT, event.item);
                return;

            case MANAGE_ROLES_ACTION:
                this.manageRoeAssignment(event.item);
                return;
        }

        super.handleAction(event);
    }

    protected override createTableActionLoading(): Observable<TableAction<MeshGroupBO>[]> {
        // Override me when needed
        return this.actionRebuildTrigger$.pipe(
            map(() => {
                const actions: TableAction<MeshGroupBO>[] = [
                    {
                        id: EDIT_ACTION,
                        icon: 'edit',
                        label: this.i18n.instant('common.edit'),
                        enabled: (item) => item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'primary',
                        single: true,
                    },
                    {
                        id: MANAGE_USERS_ACTION,
                        icon: 'person',
                        label: this.i18n.instant('mesh.manage_user_assignment'),
                        enabled: (item) => item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'secondary',
                        single: true,
                    },
                    {
                        id: MANAGE_ROLES_ACTION,
                        icon: 'fact_check',
                        label: this.i18n.instant('mesh.manage_role_assignment'),
                        enabled: (item) => item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'secondary',
                        single: true,
                    },
                    {
                        id: ASSIGN_TO_USERS_ACTION,
                        icon: 'link',
                        label: this.i18n.instant('mesh.assignRolesToGroups'),
                        enabled: true,
                        type: 'secondary',
                        multiple: true,
                    },
                    {
                        id: UNASSIGN_FROM_USERS_ACTION,
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

    protected async manageRoeAssignment(group: MeshGroupBO): Promise<void> {
        const assignedRoleIds = group.roles.map(group => group.uuid);

        const dialog = await this.modalService.fromComponent(SelectRoleModal, {}, {
            title: 'mesh.manage_role_assignment',
            multiple: true,
            selected: (group.roles || []).map(role => role.uuid),
        });

        const roles: MeshRoleBO[] = await dialog.open();
        const newRoleIds = roles.map(role => role.uuid);

        const toAssign = roles.filter(role => !assignedRoleIds.includes(role.uuid));
        const toRemove = group.roles.filter(role => !newRoleIds.includes(role.uuid));

        // Nothing to do
        if (toAssign.length === 0 && toRemove.length === 0) {
            return;
        }

        for (const role of toAssign) {
            this.handler.assignRoleToGroup(role, group);
        }
        for (const role of toRemove) {
            this.handler.unassignRoleFromGroup(role, group);
        }

        this.reload();
    }

    async openModal(mode: MeshGroupPropertiesMode, group?: Group): Promise<void> {
        const dialog = await this.modalService.fromComponent(MeshGroupModal, {}, {
            mode,
            group: group,
        });
        const res = await dialog.open();
        if (res) {
            this.reload();
        }
    }
}

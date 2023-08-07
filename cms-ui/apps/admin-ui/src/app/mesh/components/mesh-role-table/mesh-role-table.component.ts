import { BO_PERMISSIONS } from '@admin-ui/common';
import { I18nNotificationService, I18nService } from '@admin-ui/core';
import { MeshGroupBO, MeshRoleBO } from '@admin-ui/mesh/common';
import { MeshRoleTableLoaderService } from '@admin-ui/mesh/providers';
import { BaseEntityTableComponent, DELETE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { AnyModelType, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { Permission, Role } from '@gentics/mesh-models';
import { RequestFailedError } from '@gentics/mesh-rest-client';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MeshRoleModal } from '../mesh-role-modal/mesh-role-modal.component';
import { MeshRolePropertiesMode } from '../mesh-role-properties/mesh-role-properties.component';
import { SelectGroupModal } from '../select-group-modal/select-group-modal.component';

const EDIT_ACTION = 'edit';
const ASSIGN_TO_GROUPS_ACTION = 'assignToGroups';
const UNASSIGN_FROM_GROUPS_ACTION = 'unassignFromGroup';

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
        protected mesh: MeshRestClientService,
        protected notification: I18nNotificationService,
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
                        id: ASSIGN_TO_GROUPS_ACTION,
                        icon: 'link',
                        label: this.i18n.instant('mesh.assignRolesToGroups'),
                        enabled: (item) => item == null || item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'secondary',
                        single: true,
                        multiple: true,
                    },
                    {
                        id: UNASSIGN_FROM_GROUPS_ACTION,
                        icon: 'link_off',
                        label: this.i18n.instant('mesh.unassignRolesFromGroups'),
                        enabled: (item) => item == null || item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'secondary',
                        single: true,
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
            case ASSIGN_TO_GROUPS_ACTION:
                this.assignToGroups(this.getAffectedEntityIds(event));
                return;
            case UNASSIGN_FROM_GROUPS_ACTION:
                this.unassignToGroups(this.getAffectedEntityIds(event));
                return;
        }

        super.handleAction(event);
    }

    async assignToGroups(roleIds: string[]): Promise<void> {
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
                try {
                    await this.mesh.groups.assignRole(group.uuid, role.uuid);
                    this.notification.show({
                        type: 'success',
                        message: 'mesh.assign_role_to_group_success',
                        translationParams: {
                            roleName: role.name,
                            groupName: group.name,
                        },
                    });
                } catch (err) {
                    let message: string;
                    let params: Record<string, string> = {};

                    if (err instanceof RequestFailedError) {
                        message = err.data.message;
                    } else {
                        message = 'mesh.assign_role_to_group_error';
                        params = {
                            roleName: role.name,
                            groupName: group.name,
                        };
                    }

                    this.notification.show({
                        type: 'alert',
                        delay: 10_000,
                        message,
                        translationParams: params,
                    });
                }
            }
        }

        this.reload();
    }

    async unassignToGroups(roleIds: string[]): Promise<void> {
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
                try {
                    await this.mesh.groups.unassignRole(group.uuid, role.uuid);
                    this.notification.show({
                        type: 'success',
                        message: 'mesh.unassign_role_from_group_success',
                        translationParams: {
                            roleName: role.name,
                            groupName: group.name,
                        },
                    });
                } catch (err) {
                    let message: string;
                    let params: Record<string, string> = {};

                    if (err instanceof RequestFailedError) {
                        message = err.data.message;
                    } else {
                        message = 'mesh.unassign_role_from_group_error';
                        params = {
                            roleName: role.name,
                            groupName: group.name,
                        };
                    }

                    this.notification.show({
                        type: 'alert',
                        delay: 10_000,
                        message,
                        translationParams: params,
                    });
                }
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

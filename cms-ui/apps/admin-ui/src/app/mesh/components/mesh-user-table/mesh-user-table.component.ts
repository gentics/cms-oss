import { BO_PERMISSIONS } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { MeshGroupBO, MeshUserBO } from '@admin-ui/mesh/common';
import { MeshGroupHandlerService, MeshUserHandlerService, MeshUserTableLoaderService } from '@admin-ui/mesh/providers';
import { BaseEntityTableComponent, DELETE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { AnyModelType, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { Permission, User } from '@gentics/mesh-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { getUserName } from '@admin-ui/mesh/utils';
import { MeshUserModal } from '../mesh-user-modal/mesh-user-modal.component';
import { MeshUserPropertiesMode } from '../mesh-user-properties/mesh-user-properties.component';
import { SelectGroupModal } from '../select-group-modal/select-group-modal.component';
import { CopyTokenModal } from '../copy-token-modal/copy-token-modal.component';

const EDIT_ACTION = 'edit';
const ASSIGN_TO_GROUPS_ACTION = 'assignToGroups';
const UNASSIGN_FROM_GROUPS_ACTION = 'unassignFromGroup';
const MANAGE_GROUPS_ACTION = 'manageGroups';
const CREATE_API_TOKEN_ACTION = 'createApiToken';

@Component({
    selector: 'gtx-mesh-user-table',
    templateUrl: './mesh-user-table.component.html',
    styleUrls: ['./mesh-user-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshUserTableComponent extends BaseEntityTableComponent<User, MeshUserBO> {

    protected rawColumns: TableColumn<MeshUserBO>[] = [
        {
            id: 'username',
            label: 'shared.user_name',
            fieldPath: 'username',
            sortable: true,
        },
        {
            id: 'firstname',
            label: 'shared.first_name',
            fieldPath: 'firstname',
            sortable: true,
        },
        {
            id: 'lastname',
            label: 'shared.last_name',
            fieldPath: 'lastname',
            sortable: true,
        },
        {
            id: 'groups',
            label: 'common.group_plural',
            // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
            mapper: (user: MeshUserBO) => (user.groups || []).map(group => group.name).filter(name => !!name).join(', '),
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'user';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: MeshUserTableLoaderService,
        modalService: ModalService,
        protected handler: MeshUserHandlerService,
        protected groupHandler: MeshGroupHandlerService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    protected override createTableActionLoading(): Observable<TableAction<MeshUserBO>[]> {
        // Override me when needed
        return this.actionRebuildTrigger$.pipe(
            map(() => {
                const actions: TableAction<MeshUserBO>[] = [
                    {
                        id: EDIT_ACTION,
                        icon: 'edit',
                        label: this.i18n.instant('common.edit'),
                        enabled: (item) => item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'primary',
                        single: true,
                    },
                    {
                        id: CREATE_API_TOKEN_ACTION,
                        icon: 'vpn_key',
                        label: this.i18n.instant('mesh.create_api_token'),
                        enabled: (item) => item[BO_PERMISSIONS].includes(Permission.UPDATE),
                        type: 'warning',
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
                        label: this.i18n.instant('mesh.assign_roles_to_groups'),
                        enabled: true,
                        type: 'secondary',
                        multiple: true,
                    },
                    {
                        id: UNASSIGN_FROM_GROUPS_ACTION,
                        icon: 'link_off',
                        label: this.i18n.instant('mesh.unassign_roles_from_groups'),
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
        this.openModal(MeshUserPropertiesMode.CREATE);
    }

    public override handleAction(event: TableActionClickEvent<MeshUserBO>): void {
        switch (event.actionId) {
            case EDIT_ACTION:
                this.openModal(MeshUserPropertiesMode.EDIT, event.item);
                return;

            case CREATE_API_TOKEN_ACTION:
                this.createApiToken(event.item);
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

    async createApiToken(user: MeshUserBO): Promise<void> {
        const dialog = await this.modalService.dialog({
            title: this.i18n.instant('mesh.create_api_token'),
            body: this.i18n.instant('mesh.create_api_token_warning', {
                user: getUserName(user),
            }),
            buttons: [
                {
                    label: this.i18n.instant('shared.confirm_button'),
                    type: 'warning',
                    returnValue: true,
                },
                {
                    label: this.i18n.instant('common.cancel_button'),
                    type: 'secondary',
                    returnValue: false,
                },
            ],
        });
        const shouldProceed = await dialog.open();
        if (!shouldProceed) {
            return;
        }

        const res = await this.handler.createAPIToken(user.uuid);
        const copyModal = await this.modalService.fromComponent(CopyTokenModal, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
        }, {
            token: res.token,
            user: user,
        });
        await copyModal.open();
    }

    async manageGroupAssignment(user: MeshUserBO): Promise<void> {
        const assignedGroupIds = user.groups.map(group => group.uuid);

        const dialog = await this.modalService.fromComponent(SelectGroupModal, {}, {
            title: 'mesh.manage_group_assignment',
            multiple: true,
            selected: (user.groups || []).map(group => group.uuid),
        });

        const groups: MeshGroupBO[] = await dialog.open();
        const newGroupIds = groups.map(group => group.uuid);

        const toAssign = groups.filter(group => !assignedGroupIds.includes(group.uuid));
        const toRemove = user.groups.filter(group => !newGroupIds.includes(group.uuid));

        // Nothing to do
        if (toAssign.length === 0 && toRemove.length === 0) {
            return;
        }

        for (const group of toAssign) {
            this.groupHandler.assignUser(group, user);
        }
        for (const group of toRemove) {
            this.groupHandler.unassignUser(group, user);
        }

        this.reload();
    }

    async handleAssignToGroupsAction(userIds: string[]): Promise<void> {
        const users = this.loader.getEntitiesByIds(userIds);
        const dialog = await this.modalService.fromComponent(SelectGroupModal, {}, {
            title: 'mesh.assign_users_to_groups',
            multiple: true,
        });

        const groups: MeshGroupBO[] = await dialog.open();
        if (groups.length === 0) {
            return;
        }

        for (const group of groups) {
            for (const user of users) {
                this.groupHandler.assignUser(group, user);
            }
        }

        this.reload();
    }

    async handleUnassignToGroupsAction(userIds: string[]): Promise<void> {
        const users = this.loader.getEntitiesByIds(userIds);
        const dialog = await this.modalService.fromComponent(SelectGroupModal, {}, {
            title: 'mesh.unassign_users_from_groups',
            multiple: true,
        });

        const groups: MeshGroupBO[] = await dialog.open();
        if (groups.length === 0) {
            return;
        }

        for (const group of groups) {
            for (const user of users) {
                this.groupHandler.unassignUser(group, user);
            }
        }

        this.reload();
    }

    async openModal(mode: MeshUserPropertiesMode, user?: User): Promise<void> {
        const dialog = await this.modalService.fromComponent(MeshUserModal, {}, {
            mode,
            user,
        });
        const res = await dialog.open();
        if (res) {
            this.reload();
        }
    }
}

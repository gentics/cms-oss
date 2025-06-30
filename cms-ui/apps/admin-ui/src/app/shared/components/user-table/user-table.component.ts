import { AdminUIEntityDetailRoutes, AdminUIModuleRoutes, UserBO } from '@admin-ui/common';
import {
    ErrorHandler,
    GroupOperations,
    I18nService,
    PermissionsService,
    UserOperations,
    UserTableLoaderOptions,
    UserTableLoaderService,
} from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges } from '@angular/core';
import { Group, NormalizableEntityType, Raw, User } from '@gentics/cms-models';
import { ChangesOf, ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { ContextMenuService } from '../../providers/context-menu/context-menu.service';
import { AssignGroupToUsersModal } from '../assign-group-to-users-modal/assign-group-to-users-modal.component';
import { BaseEntityTableComponent, DELETE_ACTION } from '../base-entity-table/base-entity-table.component';
import { CreateUserModalComponent } from '../create-user-modal/create-user-modal.component';

const ASSIGN_TO_GROUP_ACTION = 'assignToGroup';
const REMOVE_FROM_GROUP_ACTION = 'removeFromGroup';

@Component({
    selector: 'gtx-user-table',
    templateUrl: './user-table.component.html',
    styleUrls: ['./user-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserTableComponent extends BaseEntityTableComponent<User<Raw>, UserBO, UserTableLoaderOptions> implements OnChanges {

    public readonly AdminUIModuleRoutes = AdminUIModuleRoutes;
    public readonly AdminUIEntityDetailRoutes = AdminUIEntityDetailRoutes;

    @Input()
    public group: Group;

    @Input()
    public linkGroups = true;

    public sortBy = 'firstName';

    protected rawColumns: TableColumn<UserBO>[] = [
        {
            id: 'login',
            label: 'shared.user_name',
            fieldPath: 'login',
            sortable: true,
        },
        {
            id: 'firstName',
            label: 'shared.first_name',
            fieldPath: 'firstName',
            sortable: true,
        },
        {
            id: 'lastName',
            label: 'shared.last_name',
            fieldPath: 'lastName',
            sortable: true,
        },
        {
            id: 'email',
            label: 'shared.email',
            fieldPath: 'email',
            sortable: true,
        },
        {
            id: 'groups',
            label: 'shared.groups',
            fieldPath: 'groups',
        },
    ];

    protected entityIdentifier: NormalizableEntityType = 'user';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        protected loader: UserTableLoaderService,
        modalService: ModalService,
        protected permissions: PermissionsService,
        protected operations: UserOperations,
        protected contextMenu: ContextMenuService,
        protected groupOps: GroupOperations,
        protected errorHandler: ErrorHandler,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    public override ngOnChanges(changes: ChangesOf<this>): void {
        super.ngOnChanges(changes);

        if (changes.group) {
            this.loadTrigger.next();
            this.actionRebuildTrigger.next();
        }
    }

    protected override createTableActionLoading(): Observable<TableAction<UserBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('user.deleteUser').typePermissions),
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('user.assignUserToGroup').typePermissions),
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('user.removeUserFromGroup').typePermissions),
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canDelete, canAssign, canRemove]) => {
                const actions: TableAction<UserBO>[] = [
                    {
                        id: ASSIGN_TO_GROUP_ACTION,
                        icon: 'link',
                        label: this.i18n.instant('shared.assign_user_to_groups'),
                        type: 'secondary',
                        single: true,
                        multiple: true,
                        enabled: canAssign,
                    },
                ];

                if (this.group) {
                    actions.push({
                        id: REMOVE_FROM_GROUP_ACTION,
                        icon: 'clear',
                        label: this.i18n.instant('shared.remove_user_from_group'),
                        type: 'alert',
                        single: true,
                        multiple: true,
                        enabled: canRemove,
                    });
                } else {
                    actions.push({
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('shared.delete'),
                        type: 'alert',
                        single: true,
                        multiple: true,
                        enabled: canDelete,
                    });
                }

                return actions;
            }),
        )
    }

    protected override createAdditionalLoadOptions(): UserTableLoaderOptions {
        return {
            groupId: this.group?.id,
        };
    }

    public override handleAction(event: TableActionClickEvent<UserBO>): void {
        const items = this.getEntitiesByIds(this.getAffectedEntityIds(event));

        switch (event.actionId) {
            case ASSIGN_TO_GROUP_ACTION:
                this.changeUserGroups(this.getAffectedEntityIds(event).map(id => Number(id)))
                    .then(didChange => {
                        if (!didChange) {
                            return;
                        }

                        if (event.selection) {
                            this.updateSelection([]);
                        }
                        this.loader.reload();
                    });
                return;

            case REMOVE_FROM_GROUP_ACTION:
                if (!this.group) {
                    return;
                }

                this.removeUsersFromGroup(this.group.id, items)
                    .then(didChange => {
                        if (!didChange) {
                            return;
                        }

                        if (event.selection) {
                            this.selected = [];
                            this.selectedChange.emit([]);
                        }
                        this.loader.reload();
                    });
                return;
        }

        super.handleAction(event);
    }

    public override handleCreateButton(): void {
        if (this.group?.id) {
            this.createUser([this.group.id]);
        } else {
            this.createUser();
        }
    }

    public async handleAssignUsersButton(): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            AssignGroupToUsersModal,
            { closeOnOverlayClick: false, width: '50%' },
            { group: this.group },
        );
        await dialog.open();

        this.loader.reload();
    }

    protected async createUser(groupIds: number[] = []): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            CreateUserModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
            { userGroupIds: groupIds },
        );
        const created = await dialog.open();

        if (!created) {
            return;
        }

        this.loader.reload();
    }

    protected changeUserGroups(userIds: number[]): Promise<any> {
        return this.contextMenu.changeGroupsOfUsersModalOpen(userIds);
    }

    protected async removeUsersFromGroup(groupId: number, users: UserBO[]): Promise<boolean> {
        const groupName = this.appState.now.entity.group[groupId].name;
        const userNames = users.map(user => user.login);

        const dialog = await this.modalService.dialog({
            title: this.i18n.instant('modal.confirm_remove_user_from_group_title'),
            body: `${this.i18n.instant('modal.confirm_remove_user_from_group_message', { groupName: groupName })}:
            <br>
            <ul class="browser-default">
                <li><b>${userNames.join('</b></li><li><b>')}</b></li>
            </ul>
            `,
            buttons: [
                {
                    label: this.i18n.instant('modal.confirm_remove_users_button', { userAmount: userNames?.length }),
                    returnValue: true,
                    type: 'alert',
                },
                {
                    label: this.i18n.instant('common.cancel_button'),
                    returnValue: false,
                    flat: true,
                    type: 'secondary',
                },
            ],
        }, {
            closeOnOverlayClick: false,
        });

        const confirmedRemoval = await dialog.open();

        if (!confirmedRemoval) {
            return false;
        }

        let didChange = false;
        for (const user of users) {
            try {
                await this.groupOps.removeUserFromGroup(groupId, user.id).toPromise();
                didChange = true;
            } catch (err) {
                this.errorHandler.catch(err, { notification: true });
            }
        }

        return didChange;
    }
}

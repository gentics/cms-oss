import { AdminUIEntityDetailRoutes, AdminUIModuleRoutes, UserBO } from '@admin-ui/common';
import { GroupOperations, I18nService, PermissionsService, UserOperations, UserTableLoaderOptions, UserTableLoaderService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { NormalizableEntityType, Raw, User } from '@gentics/cms-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { ContextMenuService } from '../../providers/context-menu/context-menu.service';
import { BaseEntityTableComponent, DELETE_ACTION } from '../base-entity-table/base-entity-table.component';
import { ConfirmRemoveUserFromGroupModalComponent } from '../confirm-remove-user-from-group-modal/confirm-remove-user-from-group-modal.component';
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
    public groupId: number;

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
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.groupId) {
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

                if (this.groupId) {
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
            groupId: this.groupId,
        };
    }

    public override handleAction(event: TableActionClickEvent<UserBO>): void {
        switch (event.actionId) {
            case ASSIGN_TO_GROUP_ACTION:
                this.changeUserGroups(this.getAffectedEntityIds(event).map(id => Number(id)))
                    .then(() => {
                        if (event.selection) {
                            this.selectedChange.emit([]);
                        }
                        this.loader.reload();
                    });
                return;

            case REMOVE_FROM_GROUP_ACTION:
                this.removeUsersFromGroup(this.groupId, this.getAffectedEntityIds(event).map(id => Number(id)))
                    .then(() => {
                        if (event.selection) {
                            this.selectedChange.emit([]);
                        }
                        this.loader.reload();
                    });
                return;
        }

        super.handleAction(event);
    }

    public override handleCreateButton(): void {
        if (this.groupId) {
            this.createUser([this.groupId]);
        } else {
            this.createUser();
        }
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

    protected async removeUsersFromGroup(groupId: number, userIds: number[]): Promise<void> {
        const groupName = this.appState.now.entity.group[groupId].name;
        const userNames = this.loader.getEntitiesByIds(userIds).map(user => user.login);

        // open modal
        const dialog = await this.modalService.fromComponent(
            ConfirmRemoveUserFromGroupModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
            {
                groupName,
                userNames,
            },
        );

        const confirmedRemoval = await dialog.open();

        if (!confirmedRemoval) {
            return;
        }

        for (const id of userIds) {
            await this.groupOps.removeUserFromGroup(groupId, id).toPromise();
        }
    }
}

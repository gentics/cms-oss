import { GroupBO } from '@admin-ui/common';
import { ErrorHandler, GroupOperations, GroupTableLoaderOptions, GroupTableLoaderService, I18nService, PermissionsService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { AnyModelType, Group, NormalizableEntityTypesMap, Raw } from '@gentics/cms-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseEntityTableComponent } from '../base-entity-table/base-entity-table.component';
import { CreateGroupModalComponent } from '../create-group-modal/create-group-modal.component';
import { MoveGroupsModalComponent } from '../move-groups-modal/move-groups-modal.component';

const CREATE_SUB_GROUP_ACTION = 'createSubGroup';
const MOVE_MULTIPLE_GROUPS_ACTION = 'moveGroups';
const MOVE_SINGLE_GROUP_ACTION = 'move';
const DELETE_ACTION = 'delete';

@Component({
    selector: 'gtx-group-table',
    templateUrl: './group-table.component.html',
    styleUrls: ['./group-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GroupTableComponent extends BaseEntityTableComponent<Group<Raw>, GroupBO, GroupTableLoaderOptions> implements OnChanges {

    @Input()
    public userId: number;

    @Input()
    public groupId: number;

    protected rawColumns: TableColumn<GroupBO>[] = [
        {
            id: 'name',
            label: 'shared.group_name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'description',
            label: 'shared.description',
            fieldPath: 'description',
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'group';

    public sortBy = 'name';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: GroupTableLoaderService,
        modalService: ModalService,
        protected permissions: PermissionsService,
        protected errorHandler: ErrorHandler,
        protected operations: GroupOperations,
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

        // If the user- or group-id changed, we need to reload the table content
        if (changes.userId || changes.groupId) {
            this.loadTrigger.next();
        }
    }

    protected override applyActions(actions: TableAction<GroupBO>[]): void {
        this.actions = this.hideActions ? [...this.extraActions] : [...this.extraActions, ...actions];
    }

    protected override createTableActionLoading(): Observable<TableAction<GroupBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('group.moveGroup').typePermissions),
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('group.createGroup').typePermissions),
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('group.deleteGroup').typePermissions),
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canMove, canCreate, canDelete]) => {
                const actions: TableAction<GroupBO>[] = [
                    {
                        id: CREATE_SUB_GROUP_ACTION,
                        icon: 'add',
                        label: this.i18n.instant('shared.create_new_sub_group_button'),
                        enabled: canCreate,
                        type: 'success',
                        single: true,
                    },
                    {
                        id: MOVE_MULTIPLE_GROUPS_ACTION,
                        icon: 'subdirectory_arrow_right',
                        label: this.i18n.instant('shared.move'),
                        enabled: canMove,
                        multiple: true,
                    },
                    {
                        id: MOVE_SINGLE_GROUP_ACTION,
                        icon: 'subdirectory_arrow_right',
                        label: this.i18n.instant('shared.move'),
                        enabled: canMove,
                        single: true,
                    },
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('shared.delete'),
                        enabled: canDelete,
                        type: 'alert',
                        multiple: true,
                        single: true,
                    },
                ];

                return actions;
            }),
        );
    }

    protected override createAdditionalLoadOptions(): GroupTableLoaderOptions {
        return {
            userId: this.userId,
            groupId: this.groupId,
        };
    }

    public override handleAction(event: TableActionClickEvent<GroupBO>): void {
        switch (event.actionId) {
            case CREATE_SUB_GROUP_ACTION:
                this.createSubgroup(event.item.id);
                return;

            case MOVE_MULTIPLE_GROUPS_ACTION:
                this.moveGroups(this.getAffectedEntityIds(event));
                return;

            case MOVE_SINGLE_GROUP_ACTION:
                this.moveGroups([event.item.id]);
                return;
        }

        super.handleAction(event);
    }

    public handleCreateButton(): void {
        this.createSubgroup(this.groupId);
    }

    /**
     * If group clicks to create a new group
     */
    protected async createSubgroup(parentGroupId: number): Promise<void> {
        try {
            const dialog = await this.modalService.fromComponent(
                CreateGroupModalComponent,
                { closeOnOverlayClick: false, width: '50%' },
                { parentGroupId },
            );
            const created = await dialog.open();

            if (!created) {
                return;
            }

            this.loader.reload();
        } catch (err) {
            this.errorHandler.catch(err);
        }
    }

    protected async moveGroups(entityIds: (string | number)[]): Promise<any> {
        // if no row is selected, display modal
        if (!entityIds || entityIds.length < 1) {
            // this.notificationNoneSelected();
            return;
        }

        const dialog = await this.modalService.fromComponent(
            MoveGroupsModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
            { sourceGroupIds: entityIds },
        );

        await dialog.open();

        this.loader.reload();
    }
}

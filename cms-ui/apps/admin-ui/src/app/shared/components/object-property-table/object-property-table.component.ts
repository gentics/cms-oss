import { AdminUIEntityDetailRoutes, EditableEntity, ObjectPropertyBO, typeIdsToName } from '@admin-ui/common';
import {
    DevToolPackageTableLoaderService,
    I18nService,
    ObjectPropertyTableLoaderOptions,
    ObjectPropertyTableLoaderService,
    PermissionsService,
} from '@admin-ui/core';
import { ContextMenuService, DELETE_ACTION, UNASSIGN_FROM_PACKAGE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { AnyModelType, NormalizableEntityTypesMap, ObjectPropertiesObjectType, ObjectProperty } from '@gentics/cms-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import {
    AssignNodeRestrictionsToObjectPropertiesModalComponent,
} from '../assign-node-restriction-to-object-properties-modal/assign-node-restriction-to-object-properties-modal.component';
import { BasePackageEntityTableComponent } from '../base-package-entity-table/base-package-entity-table.component';

const ASSIGN_TO_NODES_ACTION = 'assignToNodes';

@Component({
    selector: 'gtx-object-property-table',
    templateUrl: './object-property-table.component.html',
    styleUrls: ['./object-property-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ObjectPropertyTableComponent
    extends BasePackageEntityTableComponent<ObjectProperty, ObjectPropertyBO, ObjectPropertyTableLoaderOptions>
    implements OnChanges {

    public readonly AdminUIEntityDetailRoutes = AdminUIEntityDetailRoutes;

    @Input()
    public type?: ObjectPropertiesObjectType;

    public sortBy = 'name';

    protected rawColumns: TableColumn<ObjectPropertyBO>[] = [
        {
            id: 'name',
            label: 'objectProperty.name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'keyword',
            label: 'objectProperty.keyword',
            fieldPath: 'keyword',
            sortable: true,
        },
        {
            id: 'category',
            label: 'objectProperty.objectPropertyCategory_singular',
            fieldPath: 'categoryId',
            sortable: true,
            sortValue: 'category.name',
        },
        {
            id: 'construct',
            label: 'objectProperty.construct',
            fieldPath: 'constructId',
            sortable: true,
            sortValue: 'construct.name',
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'objectProperty';
    protected focusEntityType = EditableEntity.OBJECT_PROPERTY;

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: ObjectPropertyTableLoaderService,
        modalService: ModalService,
        contextMenu: ContextMenuService,
        packageTableLoader: DevToolPackageTableLoaderService,
        protected permissions: PermissionsService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader as any,
            modalService,
            contextMenu,
            packageTableLoader,
        );
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.type) {
            // If the value switched from/to null
            if (
                (changes.type.currentValue == null && changes.type.previousValue != null)
                || (changes.type.currentValue != null && changes.type.previousValue == null)
            ) {
                this.rebuildColumns();
            }
            // Also reload the page, as the type changes and now needs to load the proper type of properties
            this.loadTrigger.next();
        }
    }

    protected override createTableActionLoading(): Observable<TableAction<ObjectPropertyBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('objectProperty.deleteObjectpropertyInstance').typePermissions),
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('contentadmin.updateContent').typePermissions),
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canDeleteAndEdit, canManagePackage]) => {
                const actions: TableAction<ObjectPropertyBO>[] = [];

                if (!this.packageName) {
                    actions.push({
                        id: ASSIGN_TO_NODES_ACTION,
                        icon: 'device_hub',
                        label: this.i18n.instant('common.assign_objectProperty_to_nodes'),
                        type: 'primary',
                        enabled: canDeleteAndEdit,
                        single: true,
                    },
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('shared.delete'),
                        type: 'alert',
                        enabled: canDeleteAndEdit,
                        single: true,
                        multiple: true,
                    });
                } else {
                    actions.push({
                        id: UNASSIGN_FROM_PACKAGE_ACTION,
                        icon: 'link_off',
                        type: 'alert',
                        label: this.i18n.instant('package.remove_from_package'),
                        enabled: canManagePackage,
                        single: true,
                        multiple: true,
                    });
                }

                return actions;
            }),
        );
    }

    protected override rebuildColumns(): void {
        const newColumns = [...this.rawColumns];
        if (this.type == null) {
            newColumns.splice(2, 0, {
                id: 'type',
                label: 'objectProperty.type',
                fieldPath: 'type',
                mapper: typeIdsToName,
            });
        }
        this.columns = this.translateColumns(newColumns);
    }

    protected override createAdditionalLoadOptions(): ObjectPropertyTableLoaderOptions {
        return {
            packageName: this.packageName,
            types: this.type ? [this.type] : null,
        };
    }

    public override handleAction(event: TableActionClickEvent<ObjectPropertyBO>): void {
        switch (event.actionId) {
            case ASSIGN_TO_NODES_ACTION:
                this.setObjectpropertyNodeRestrictions(event.item);
                return;
        }

        super.handleAction(event);
    }

    protected async setObjectpropertyNodeRestrictions(objectProperty: ObjectPropertyBO): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            AssignNodeRestrictionsToObjectPropertiesModalComponent,
            { closeOnOverlayClick: false, width: '75%' },
            { objectProperty },
        );
        await dialog.open();
    }
}

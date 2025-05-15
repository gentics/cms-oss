import { EditableEntity, ObjectPropertyCategoryBO } from '@admin-ui/common';
import { I18nService, PermissionsService } from '@admin-ui/core';
import { BaseEntityTableComponent, DELETE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { AnyModelType, NormalizableEntityTypesMap, ObjectPropertyCategory } from '@gentics/cms-models';
import { ModalService, TableAction, TableColumn } from '@gentics/ui-core';
import { combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ObjectPropertyCategoryTableLoaderService } from '../../providers';

@Component({
    selector: 'gtx-object-property-category-table',
    templateUrl: './object-property-category-table.component.html',
    styleUrls: ['./object-property-category-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ObjectPropertyCategortTableComponent extends BaseEntityTableComponent<ObjectPropertyCategory, ObjectPropertyCategoryBO> {

    protected rawColumns: TableColumn<ObjectPropertyCategoryBO>[] = [
        {
            id: 'name',
            label: 'objectProperty.name',
            fieldPath: 'name',
            sortable: true,
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'objectPropertyCategory';
    protected focusEntityType = EditableEntity.OBJECT_PROPERTY_CATEGORY;

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: ObjectPropertyCategoryTableLoaderService,
        modalService: ModalService,
        protected permissions: PermissionsService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    protected override createTableActionLoading(): Observable<TableAction<ObjectPropertyCategoryBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('objectPropertyCategory.deleteObjectpropertyCategory').typePermissions),
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canDelete]) => {
                const actions: TableAction<ObjectPropertyCategoryBO>[] = [
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('shared.delete'),
                        type: 'alert',
                        enabled: canDelete,
                        single: true,
                        multiple: true,
                    },
                ];

                return actions;
            }),
        );
    }
}

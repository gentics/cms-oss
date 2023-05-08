import { ConstructCategoryBO } from '@admin-ui/common';
import { I18nService, PermissionsService } from '@admin-ui/core';
import { BaseEntityTableComponent, DELETE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { AnyModelType, ConstructCategory, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { ModalService, TableAction, TableColumn } from '@gentics/ui-core';
import { combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ConstructCategoryTableLoaderService } from '../../providers';

@Component({
    selector: 'gtx-construct-category-table',
    templateUrl: './construct-category-table.component.html',
    styleUrls: ['./construct-category-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConstructCategoryTableComponent extends BaseEntityTableComponent<ConstructCategory, ConstructCategoryBO> {

    protected rawColumns: TableColumn<ConstructCategoryBO>[] = [
        {
            id: 'name',
            label: 'construct.name',
            fieldPath: 'name',
            sortable: true,
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'constructCategory';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: ConstructCategoryTableLoaderService,
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

    protected override createTableActionLoading(): Observable<TableAction<ConstructCategoryBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('constructCategory.deleteCategoryInstance').typePermissions),
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canDelete]) => {
                const actions: TableAction<ConstructCategoryBO>[] = [
                    {
                        id: DELETE_ACTION,
                        label: this.i18n.instant('construct_category.delete_category_singular'),
                        icon: 'delete',
                        type: 'alert',
                        multiple: true,
                        single: true,
                        enabled: canDelete,
                    },
                ];

                return actions;
            }),
        );
    }
}

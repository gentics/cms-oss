import { BO_NEW_SORT_ORDER, BO_ORIGINAL_SORT_ORDER, ConstructCategoryBO, EditableEntity, createMoveActions } from '@admin-ui/common';
import { PermissionsService } from '@admin-ui/core';
import { BaseSortableEntityTableComponent, DELETE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { wasClosedByUser } from '@gentics/cms-integration-api-models';
import { AnyModelType, ConstructCategory, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { ModalService, TableAction, TableColumn } from '@gentics/ui-core';
import { I18nService } from '@gentics/cms-components';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { ConstructCategoryTableLoaderService } from '../../providers';
import { ConstructCategorySortModal } from '../construct-category-sort-modal/construct-category-sort-modal.component';

@Component({
    selector: 'gtx-construct-category-table',
    templateUrl: './construct-category-table.component.html',
    styleUrls: ['./construct-category-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class ConstructCategoryTableComponent extends BaseSortableEntityTableComponent<ConstructCategory, ConstructCategoryBO> {

    protected rawColumns: TableColumn<ConstructCategoryBO>[] = [
        {
            id: 'name',
            label: 'construct.name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'sortorder',
            label: 'construct.categorySortorder',

            mapper: (category: ConstructCategoryBO) => this.sorting
                ? category[BO_NEW_SORT_ORDER] + 1
                : (category.sortOrder ?? (category[BO_ORIGINAL_SORT_ORDER] + 1)),
            sortable: true,
            align: 'right',
        },
    ];

    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'constructCategory';
    protected focusEntityType = EditableEntity.CONSTRUCT_CATEGORY;

    public sortBy = 'sortorder';

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
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('constructCategory.updateCategoryInstance').typePermissions),
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canDelete, canUpdate]) => {
                const actions: TableAction<ConstructCategoryBO>[] = [];

                if (this.sorting) {
                    actions.push(...createMoveActions(this.i18n, canUpdate));
                } else {
                    actions.push({
                        id: DELETE_ACTION,
                        label: this.i18n.instant('construct_category.delete_category_singular'),
                        icon: 'delete',
                        type: 'alert',
                        multiple: true,
                        single: true,
                        enabled: canDelete,
                    });
                }

                return actions;
            }),
        );
    }

    public async openSortModal(): Promise<void> {
        try {
            const dialog = await this.modalService.fromComponent(ConstructCategorySortModal, {
                closeOnEscape: false,
                closeOnOverlayClick: false,
            });
            const didSort = await dialog.open();

            if (didSort) {
                this.reload();
            }
        } catch (err) {
            if (!wasClosedByUser(err)) {
                throw err;
            }
        }
    }
}

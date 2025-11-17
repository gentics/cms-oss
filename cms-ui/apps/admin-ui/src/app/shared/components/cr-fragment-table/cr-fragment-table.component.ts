import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import { AnyModelType, ContentRepositoryFragment, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { ModalService, TableAction, TableColumn } from '@gentics/ui-core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { ContentRepositoryFragmentBO } from '../../../common';
import {
    CRFragmentTableLoaderOptions,
    CRFragmentTableLoaderService,
    DevToolPackageTableLoaderService,
    PermissionsService,
} from '../../../core';
import { AppStateService } from '../../../state';
import { ContextMenuService } from '../../providers/context-menu/context-menu.service';
import { DELETE_ACTION } from '../base-entity-table/base-entity-table.component';
import { BasePackageEntityTableComponent, UNASSIGN_FROM_PACKAGE_ACTION } from '../base-package-entity-table/base-package-entity-table.component';
import { CreateContentRepositoryFragmentModalComponent } from '../create-cr-fragment-modal/create-cr-fragment-modal.component';

@Component({
    selector: 'gtx-cr-fragment-table',
    templateUrl: './cr-fragment-table.component.html',
    styleUrls: ['./cr-fragment-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class CRFragmentTableComponent
    extends BasePackageEntityTableComponent<ContentRepositoryFragment, ContentRepositoryFragmentBO, CRFragmentTableLoaderOptions> {

    protected rawColumns: TableColumn<ContentRepositoryFragmentBO>[] = [
        {
            id: 'name',
            label: 'content_repository_fragment.name',
            fieldPath: 'name',
            sortable: true,
        },
    ];

    public canCreate = false;
    public canDelete = false;
    public canManagePackage = false;

    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'contentRepositoryFragment';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: CRFragmentTableLoaderService,
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

    protected override createTableActionLoading(): Observable<TableAction<ContentRepositoryFragmentBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('contentRepository.createContentRepository').typePermissions),
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('contentRepository.deleteContentRepository').typePermissions),
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('contentadmin.updateContent').typePermissions),
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canCreate, canDelete, canManagePackage]) => {
                this.canCreate = canCreate;
                this.canDelete = canDelete;
                this.canManagePackage = canManagePackage;

                const actions: TableAction<ContentRepositoryFragmentBO>[] = [];

                if (!this.packageName) {
                    actions.push({
                        id: DELETE_ACTION,
                        icon: 'delete',
                        type: 'alert',
                        label: this.i18n.instant('shared.delete'),
                        enabled: canDelete,
                        multiple: true,
                        single: true,
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

    protected override createAdditionalLoadOptions(): CRFragmentTableLoaderOptions {
        return {
            packageName: this.packageName,
        };
    }

    override handleCreateButton(): void {
        this.modalService.fromComponent(
            CreateContentRepositoryFragmentModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
        )
            .then((dialog) => dialog.open())
            .then((created) => {
                if (!created) {
                    return;
                }

                this.loader.reload();
            });
    }
}

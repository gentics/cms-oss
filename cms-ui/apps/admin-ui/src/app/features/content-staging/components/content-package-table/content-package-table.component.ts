import { BO_ID, ContentPackageBO, EntityTableActionClickEvent } from '@admin-ui/common';
import { ContentPackageOperations, I18nService, PermissionsService } from '@admin-ui/core';
import { BaseEntityTableComponent, DELETE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { AnyModelType, ContentPackage, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ContentPackageTableLoaderService } from '../../providers';
import { CreateContentPackageModalComponent } from '../create-content-package-modal/create-content-package-modal.component';
import { UploadContentPackageModalComponent } from '../upload-content-package-modal/upload-content-package-modal.component';

const DOWNLOAD_PACKAGE_ACTION = 'downloadPackage';
const UPLOAD_PACKAGE_ACTION = 'uploadPackage';
const EXPORT_PACKAGE_ACTION = 'exportPackage';
const IMPORT_PACKAGE_ACTION = 'importPackage';

@Component({
    selector: 'gtx-content-package-table',
    templateUrl: './content-package-table.component.html',
    styleUrls: ['./content-package-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ContentPackageTableComponent extends BaseEntityTableComponent<ContentPackage, ContentPackageBO> {

    public canCreate = false;

    protected rawColumns: TableColumn<ContentPackageBO>[] = [
        {
            id: 'name',
            label: 'common.name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'description',
            label: 'common.description',
            fieldPath: 'description',
        },
        {
            id: 'date',
            label: 'common.edate',
            fieldPath: 'timestamp',
        },
        {
            id: 'import_progress',
            label: 'content_staging.imported',
        },
        {
            id: 'import_start',
            label: 'content_staging.import_started',
            fieldPath: 'import.progress.started',
        },
        {
            id: 'import_end',
            label: 'content_staging.import_finished',
            fieldPath: 'import.progress.finished',
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'contentPackage';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        protected loader: ContentPackageTableLoaderService,
        modalService: ModalService,
        protected permissions: PermissionsService,
        protected operations: ContentPackageOperations,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    protected override createTableActionLoading(): Observable<TableAction<ContentPackageBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            ...[
                'createContentPackage',
                'readContentPackage',
                'updateContentPackage',
                'deleteContentPackage',
                'modifyContentPackageContent',
            ].map(actionId => this.permissions.checkPermissions(
                this.permissions.getUserActionPermsForId(`content_staging.${actionId}`).typePermissions),
            ),
        ]).pipe(
            map(([_, ...perms]) => perms as boolean[]),
            map(([canCreate, canRead, canUpdate, canDelete, canModifyContent]) => {
                this.canCreate = canCreate;

                const actions: TableAction<ContentPackageBO>[] = [
                    {
                        id: DOWNLOAD_PACKAGE_ACTION,
                        icon: 'file_download',
                        type: 'secondary',
                        label: this.i18n.instant('content_staging.download_content_package'),
                        single: true,
                        enabled: canRead,
                    },
                    {
                        id: UPLOAD_PACKAGE_ACTION,
                        icon: 'file_upload',
                        type: 'secondary',
                        label: this.i18n.instant('content_staging.upload_content_package'),
                        single: true,
                        enabled: canUpdate,
                    },
                    {
                        id: EXPORT_PACKAGE_ACTION,
                        icon: 'cloud_download',
                        type: 'secondary',
                        label: this.i18n.instant('content_staging.export_content_package'),
                        single: true,
                        multiple: true,
                        enabled: canModifyContent,
                    },
                    {
                        id: IMPORT_PACKAGE_ACTION,
                        icon: 'backup',
                        type: 'secondary',
                        label: this.i18n.instant('content_staging.import_content_package'),
                        single: true,
                        multiple: true,
                        enabled: canRead,
                    },
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('content_staging.delete_content_package'),
                        type: 'alert',
                        single: true,
                        multiple: true,
                        enabled: canDelete,
                    },
                ];

                return actions;
            }),
        );
    }

    public override async handleCreateButton(): Promise<void> {
        const dialog = await this.modalService.fromComponent(CreateContentPackageModalComponent, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
            width: '80%',
        }, {});
        const created = await dialog.open();

        if (!created) {
            return;
        }

        this.loader.reload();
    }

    public override handleAction(event: TableActionClickEvent<ContentPackageBO>): void {
        const items = this.getEntitiesByIds(this.getAffectedEntityIds(event));

        switch (event.actionId) {
            case DOWNLOAD_PACKAGE_ACTION:
                this.operations.download(event.item[BO_ID]).subscribe();
                return;

            case UPLOAD_PACKAGE_ACTION:
                this.uploadContentPackage(event.item);
                return;

            case EXPORT_PACKAGE_ACTION:
                this.exportPackages(items);
                return;

            case IMPORT_PACKAGE_ACTION:
                this.importPackages(items);
                return;
        }

        super.handleAction(event);
    }

    protected async uploadContentPackage(pkg: ContentPackageBO): Promise<void> {
        const dialog = await this.modalService.fromComponent(UploadContentPackageModalComponent, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
        }, {
            contentPackage: pkg,
        });
        await dialog.open();
    }

    async importPackages(packages: ContentPackageBO[]): Promise<void> {
        if (!Array.isArray(packages)) {
            return;
        }

        // TODO: Permission checks?
        packages = packages.filter(pkg => pkg);

        if (packages.length === 0) {
            return;
        }

        const dialog = await this.modalService.dialog({
            title: this.i18n.instant('modal.import_content_package_title'),
            body: this.i18n.instant(`modal.import_content_package_${packages.length === 1 ? 'singular' : 'plural'}`, {
                packageName: packages[0].name,
                packageNames: this.i18n.join(packages.map(pkg => pkg.name), {
                    quoted: true,
                    withLast: false,
                    separator: '</li><li>',
                }),
            }),
            buttons: [
                { label: this.i18n.instant('common.ok_button'), returnValue: true },
                { label: this.i18n.instant('common.cancel_button'), returnValue: false },
            ],
        });
        const doImport = await dialog.open();

        if (!doImport) {
            return;
        }

        this.subscriptions.push(combineLatest(packages
            .map(pkg => this.operations.importFromFileSystem(pkg[BO_ID], { wait: 5_000 })),
        ).subscribe());
        this.reload();
    }

    async exportPackages(packages: ContentPackageBO[]): Promise<void> {
        if (!Array.isArray(packages)) {
            return;
        }

        // TODO: Permission checks?
        packages = packages.filter(pkg => pkg);

        if (packages.length === 0) {
            return;
        }

        const dialog = await this.modalService.dialog({
            title: this.i18n.instant(`modal.export_content_package_title_${packages.length === 1 ? 'singular' : 'plural'}`),
            body: this.i18n.instant(`modal.export_content_package_body_${packages.length === 1 ? 'singular' : 'plural'}`, {
                packageName: packages[0].name,
                packageNames: this.i18n.join(packages.map(pkg => pkg.name), {
                    quoted: true,
                    withLast: false,
                    separator: '</li><li>',
                }),
            }),
            buttons: [
                { label: this.i18n.instant('common.ok_button'), returnValue: true },
                { label: this.i18n.instant('common.cancel_button'), returnValue: false },
            ],
        });
        const doExport = await dialog.open();

        if (!doExport) {
            return;
        }

        this.subscriptions.push(combineLatest(packages
            .map(pkg => this.operations.exportToFileSystem(pkg[BO_ID], { wait: 5_000 })),
        ).subscribe());
    }
}

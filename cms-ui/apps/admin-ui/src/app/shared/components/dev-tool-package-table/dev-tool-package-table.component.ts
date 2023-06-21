import { DevToolPackageBO } from '@admin-ui/common';
import { DevToolPackageTableLoaderOptions, DevToolPackageTableLoaderService, I18nService, PackageOperations, PermissionsService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { AnyModelType, NormalizableEntityTypesMap, Package, PackageSyncResponse } from '@gentics/cms-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { BehaviorSubject, Observable, combineLatest, interval } from 'rxjs';
import { debounceTime, filter, map, startWith, switchMap, tap } from 'rxjs/operators';
import { AssignPackagesToNodeModalComponent } from '../assign-packages-to-node-modal/assign-packages-to-node-modal.component';
import { BaseEntityTableComponent, DELETE_ACTION } from '../base-entity-table/base-entity-table.component';

const SYNC_FROM_FILE_SYSTEM_ACTION = 'syncFromFs';
const SYNC_TO_FILE_SYSTEM_ACTION = 'syncToFs';
const UNASSIGN_FROM_NODE_ACTION = 'unassignFromNode';

@Component({
    selector: 'gtx-dev-tool-package-table',
    templateUrl: './dev-tool-package-table.component.html',
    styleUrls: ['./dev-tool-package-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DevToolPackageTableComponent
    extends BaseEntityTableComponent<Package, DevToolPackageBO, DevToolPackageTableLoaderOptions>
    implements OnInit, OnChanges {

    @Input()
    public nodeId: number;

    /**
     * If it should hide the sync options
     */
    @Input()
    public hideSync = false;

    private syncCheck$ = new BehaviorSubject<boolean>(null);
    private canEditSub = new BehaviorSubject<boolean>(false);

    public syncEnabled = false;
    public syncLoading = true;
    public canEdit$ = this.canEditSub.asObservable();

    protected rawColumns: TableColumn<DevToolPackageBO>[] = [
        {
            id: 'name',
            label: 'package.package_name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'constructs',
            label: 'construct.construct_plural',
            fieldPath: 'constructs',
            align: 'right',
            sortable: true,
        },
        {
            id: 'templates',
            label: 'template.template_plural',
            fieldPath: 'templates',
            align: 'right',
            sortable: true,
        },
        {
            id: 'data-sources',
            label: 'dataSource.dataSource_plural',
            fieldPath: 'datasources',
            align: 'right',
            sortable: true,
            sortValue: 'datasources',
        },
        {
            id: 'object-properties',
            label: 'common.objectProperty_plural',
            fieldPath: 'objectProperties',
            align: 'right',
            sortable: true,
            sortValue: 'objectProperties',
        },
        {
            id: 'content-repositories',
            label: 'contentRepository.contentRepository_plural',
            fieldPath: 'contentRepositories',
            align: 'right',
            sortable: true,
            sortValue: 'cotnentRepositories',
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'package';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: DevToolPackageTableLoaderService,
        modalService: ModalService,
        protected permissions: PermissionsService,
        protected operations: PackageOperations,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );

        this.booleanInputs.push('hideSync');
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(combineLatest([
            this.syncCheck$.asObservable(),
            interval(60_000).pipe(
                startWith(null),
            ),
            this.loadTrigger.asObservable().pipe(
                startWith(null),
            ),
        ]).pipe(
            filter(([allow]) => allow),
            tap(() => {
                this.syncLoading = true;
                this.changeDetector.markForCheck();
            }),
            debounceTime(1_000),
            switchMap(() => this.operations.getSyncState()),
        ).subscribe(res => {
            this.syncEnabled = res.enabled;
            this.syncLoading = false;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(
            this.permissions.checkPermissions(
                this.permissions.getUserActionPermsForId('contentadmin.updateContent').typePermissions,
            ).subscribe(this.canEditSub),
        );

        this.syncCheck$.next(!this.hideSync);
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.nodeId) {
            this.loadTrigger.next();
            this.actionRebuildTrigger.next();
        }

        if (changes.hideSync) {
            this.syncCheck$.next(!this.hideSync);
        }
    }

    protected override createTableActionLoading(): Observable<TableAction<DevToolPackageBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.canEdit$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('contentadmin.deleteContent').typePermissions),
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canEdit, canDelete]) => {
                const actions: TableAction<DevToolPackageBO>[] = [];

                if (this.nodeId) {
                    actions.push({
                        id: UNASSIGN_FROM_NODE_ACTION,
                        icon: 'link_off',
                        label: this.i18n.instant('shared.remove_packages_from_node'),
                        type: 'alert',
                        enabled: true,
                        multiple: true,
                        single: true,
                    });
                } else {
                    actions.push({
                        id: SYNC_TO_FILE_SYSTEM_ACTION,
                        icon: 'cloud_download',
                        label: this.i18n.instant('package.package_sync_cms_to_filesystem'),
                        type: 'primary',
                        enabled: canEdit,
                        single: true,
                        multiple: true,
                    },
                    {
                        id: SYNC_FROM_FILE_SYSTEM_ACTION,
                        icon: 'backup',
                        label: this.i18n.instant('package.package_sync_filesystem_to_cms'),
                        type: 'primary',
                        enabled: canEdit,
                        single: true,
                        multiple: true,
                    },
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('package.package_deletes_singular'),
                        type: 'alert',
                        enabled: canDelete,
                        single: true,
                        multiple: true,
                    });
                }

                return actions;
            }),
        );
    }

    protected override createAdditionalLoadOptions(): DevToolPackageTableLoaderOptions {
        return {
            nodeId: this.nodeId,
        };
    }

    public toggleSync(): void {
        if (this.syncLoading) {
            return;
        }

        let op: Observable<PackageSyncResponse>;
        if (this.syncEnabled) {
            op = this.operations.stopSync();
        } else {
            op = this.operations.startSync();
        }

        this.syncLoading = true;
        this.changeDetector.markForCheck();

        this.subscriptions.push(op.subscribe(res => {
            this.syncEnabled = res.enabled;
            this.syncLoading = false;
            this.changeDetector.markForCheck();
        }));
    }

    public override handleAction(event: TableActionClickEvent<DevToolPackageBO>): void {
        switch (event.actionId) {
            case SYNC_FROM_FILE_SYSTEM_ACTION:
                this.syncPackageFromFilesystem(this.getAffectedEntityIds(event));
                return;

            case SYNC_TO_FILE_SYSTEM_ACTION:
                this.syncPackageToFilesystem(this.getAffectedEntityIds(event));
                return;

            case UNASSIGN_FROM_NODE_ACTION:
                this.unassignPackageFromNode(this.getAffectedEntityIds(event));
                return;
        }

        super.handleAction(event);
    }

    public async handleAssignToNode(): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            AssignPackagesToNodeModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
            { nodeId: this.nodeId },
        );
        const didChange = await dialog.open();

        if (didChange) {
            this.loadTrigger.next();
        }
    }

    protected syncPackageToFilesystem(packageName: string | string[]): Promise<void> {
        return this.operations.syncPackageToFilesystem(packageName, { wait: 5_000 });
    }

    protected syncPackageFromFilesystem(packageName: string | string[]): Promise<void> {
        return this.operations.syncPackageFromFilesystem(packageName, { wait: 5_000 });
    }

    protected unassignPackageFromNode(packageName: string | string[]): Promise<void> {
        return this.operations.removePackageFromNode(this.nodeId, packageName).toPromise()
            .then(() => {
                this.removeFromSelection(packageName);
                this.loadTrigger.next();
            });
    }
}

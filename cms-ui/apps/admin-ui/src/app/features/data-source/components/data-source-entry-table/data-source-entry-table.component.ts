import { BO_NEW_SORT_ORDER, createMoveActions, DataSourceEntryBO } from '@admin-ui/common';
import { I18nService, PermissionsService } from '@admin-ui/core';
import { BaseSortableEntityTableComponent, DELETE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { AnyModelType, DataSourceEntry, NormalizableEntityTypesMap, Raw } from '@gentics/cms-models';
import { ModalService, TableAction, TableColumn } from '@gentics/ui-core';
import { combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { DataSourceEntryTableLoaderOptions, DataSourceEntryTableLoaderService } from '../../providers';
import { CreateDataSourceEntryModalComponent } from '../create-data-source-entry-modal/create-data-source-entry-modal.component';

@Component({
    selector: 'gtx-data-source-entry-table',
    templateUrl: './data-source-entry-table.component.html',
    styleUrls: ['./data-source-entry-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class DataSourceEntryTableComponent
    extends BaseSortableEntityTableComponent<DataSourceEntry<Raw>, DataSourceEntryBO, DataSourceEntryTableLoaderOptions>
    implements OnInit, OnChanges {

    @Input()
    public dataSourceId: string | number;

    protected rawColumns: TableColumn<DataSourceEntryBO>[] = [
        {
            id: 'key',
            label: 'dataSourceEntry.key',
            fieldPath: 'key',
        },
        {
            id: 'value',
            label: 'dataSourceEntry.value',
            fieldPath: 'value',
        },
        {
            id: 'order',
            label: 'shared.order',
            fieldPath: BO_NEW_SORT_ORDER,
            // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
            mapper: (index: number) => index + 1,
            align: 'right',
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'dataSourceEntry';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: DataSourceEntryTableLoaderService,
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

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.dataSourceId) {
            this.loadTrigger.next();
        }
    }

    protected override createTableActionLoading(): Observable<TableAction<DataSourceEntryBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('dataSource.deleteDataSourceEntry').typePermissions),
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('dataSource.updateDataSourceEntries').typePermissions),
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canDelete, canUpdate]) => {
                const actions: TableAction<DataSourceEntryBO>[] = [
                    ...createMoveActions(this.i18n, canUpdate),
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('shared.delete'),
                        enabled: canDelete,
                        type: 'alert',
                        single: true,
                        multiple: true,
                    },
                ];

                return actions;
            }),
        );
    }

    protected override createAdditionalLoadOptions(): DataSourceEntryTableLoaderOptions {
        return {
            dataSourceId: this.dataSourceId,
        };
    }

    protected override callToDeleteEntity(id: string): Promise<void> {
        return (this.loader as DataSourceEntryTableLoaderService).deleteEntry(this.dataSourceId, id);
    }

    async handleCreateButton(): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            CreateDataSourceEntryModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
            { datasourceId: this.dataSourceId },
        );
        const created = await dialog.open();

        if (!created) {
            return;
        }

        this.loader.reload();
    }
}

import {
    DataSourceEntryBO,
    EditableEntity,
    NULL_FORM_TAB_HANDLE,
    TableLoadEndEvent,
    TableSortEvent,
    resetEntitySorting,
    sortEntityRow,
} from '@admin-ui/common';
import { BaseEntityEditorComponent } from '@admin-ui/core/components';
import {
    DataSourceEntryHandlerService,
    DataSourceHandlerService,
    DataSourceTableLoaderService,
    PermissionsService,
} from '@admin-ui/core/providers';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AccessControlledType, DataSource, DataSourceEntryListUpdateRequest, GcmsPermission } from '@gentics/cms-models';
import { TableRow } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { DataSourceEntryTableLoaderService } from '../../providers';

@Component({
    selector: 'gtx-data-source-editor',
    templateUrl: './data-source-editor.component.html',
    styleUrls: ['./data-source-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class DataSourceEditorComponent extends BaseEntityEditorComponent<EditableEntity.DATA_SOURCE> implements OnInit {

    public fgProperties: FormControl<DataSource>;

    public entryRows: TableRow<DataSourceEntryBO>[] = [];
    public entriesChanged = false;

    public permissionDataSourceEntryRead$: Observable<boolean>;

    constructor(
        changeDetector: ChangeDetectorRef,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        handler: DataSourceHandlerService,
        protected tableLoader: DataSourceTableLoaderService,
        protected permissionsService: PermissionsService,
        protected entryHandler: DataSourceEntryHandlerService,
        protected dataSourceEntryLoader: DataSourceEntryTableLoaderService,
    ) {
        super(
            EditableEntity.DATA_SOURCE,
            changeDetector,
            route,
            router,
            appState,
            handler,
        );
    }

    public ngOnInit(): void {
        super.ngOnInit();
    }

    protected initializeTabHandles(): void {
        this.fgProperties = new FormControl(this.entity);
        this.tabHandles[this.Tabs.PROPERTIES] = this.createTabHandle(this.fgProperties);

        this.tabHandles[this.Tabs.ENTRIES] = {
            isDirty: () => this.entriesChanged,
            isValid: () => true,
            save: () => this.updateDataSourceEntries(),
            reset: () => this.resetDataSourceEntries(),
        };
        this.tabHandles[this.Tabs.USAGE_OVERVIEW] = NULL_FORM_TAB_HANDLE;
    }

    protected onEntityChange(): void {
        if (this.fgProperties) {
            this.fgProperties.setValue(this.entity);
            this.fgProperties.markAsPristine();
        }
        this.permissionDataSourceEntryRead$ = this.permissionsService.getPermissions(AccessControlledType.DATA_SOURCE, this.entity.id).pipe(
            map(typePermissions => typePermissions.hasPermission(GcmsPermission.READ)),
        );
    }

    override onEntityUpdate(): void {
        this.tableLoader.reload();
    }

    entriesLoaded(event: TableLoadEndEvent<DataSourceEntryBO>): void {
        this.entryRows = event.rows;
        this.entriesChanged = false;
    }

    sortDataSourceEntry(event: TableSortEvent<DataSourceEntryBO>): void {
        this.entryRows = sortEntityRow(this.entryRows, event.from, event.to);
        this.entriesChanged = true;
    }

    updateDataSourceEntries(): Promise<void> {
        const req: DataSourceEntryListUpdateRequest = this.entryRows.map(row => row.item);
        return this.entryHandler.updateAll(this.entity.id, req)
            .toPromise()
            .then(() => {
                this.entriesChanged = false;
                this.dataSourceEntryLoader.reload();
            });
    }

    resetDataSourceEntries(): Promise<void> {
        this.entryRows = resetEntitySorting(this.entryRows);

        return Promise.resolve();
    }
}

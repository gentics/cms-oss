import {
    createFormSaveDisabledTracker,
    DataSourceDetailTabs,
    DataSourceEntryBO,
    detailLoading,
    FormGroupTabHandle,
    FormTabHandle,
    NULL_FORM_TAB_HANDLE,
    resetEntitySorting,
    sortEntityRow,
    TableLoadEndEvent,
    TableSortEvent,
} from '@admin-ui/common';
import {
    BREADCRUMB_RESOLVER,
    DataSourceEntryTableLoaderService,
    DataSourceEntryOperations,
    DataSourceOperations,
    EditorTabTrackerService,
    PermissionsService,
    ResolveBreadcrumbFn,
    DataSourceTableLoaderService,
} from '@admin-ui/core';
import { BaseDetailComponent, DataSourceDataService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnInit,
    Type,
} from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
    AccessControlledType,
    DataSourceBO,
    DataSourceEntryListUpdateRequest,
    DataSourceUpdateRequest,
    GcmsPermission,
    Index,
    NormalizableEntityType,
    Normalized,
    Raw,
    TypePermissions,
} from '@gentics/cms-models';
import { TableRow } from '@gentics/ui-core';
import { NGXLogger } from 'ngx-logger';
import { Observable, of } from 'rxjs';
import { map, mergeMap, takeUntil, tap } from 'rxjs/operators';

// *************************************************************************************************
/**
 * # DataSourceDetailComponent
 * Display and edit entity dataSource detail information
 */
// *************************************************************************************************
@Component({
    selector: 'gtx-data-source-detail',
    templateUrl: './data-source-detail.component.html',
    styleUrls: [ './data-source-detail.component.scss' ],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DataSourceDetailComponent extends BaseDetailComponent<'dataSource', DataSourceOperations> implements OnInit {

    public readonly DataSourceDetailTabs = DataSourceDetailTabs;

    entityIdentifier: NormalizableEntityType = 'dataSource';

    /** current entity value */
    currentEntity: DataSourceBO<Raw>;
    dsId: number;

    /** form of tab 'Properties' */
    fgProperties: UntypedFormGroup;

    fgPropertiesSaveDisabled$: Observable<boolean>;

    get isLoading(): boolean {
        return this.currentEntity == null || !this.currentEntity.name || this.currentEntity.name === '';
    }

    get activeFormTab(): FormTabHandle {
        return this.tabHandles[this.appState.now.ui.editorTab];
    }

    /** TRUE if logged-in user is allowed to read entity `dataSourceEntry` */
    permissionDataSourceEntryRead$: Observable<boolean>;

    activeTabId$: Observable<string>;

    public entryRows: TableRow<DataSourceEntryBO>[] = [];
    public entriesChanged = false;

    private tabHandles: Index<DataSourceDetailTabs, FormTabHandle>;

    constructor(
        logger: NGXLogger,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        private dataSourceData: DataSourceDataService,
        changeDetectorRef: ChangeDetectorRef,
        private dataSourceOperations: DataSourceOperations,
        private dataSourceEntryOperations: DataSourceEntryOperations,
        private permissionsService: PermissionsService,
        private editorTabTracker: EditorTabTrackerService,
        private dataSourceEntryLoader: DataSourceEntryTableLoaderService,
        private tableLoader: DataSourceTableLoaderService,
    ) {
        super(
            logger,
            route,
            router,
            appState,
            dataSourceData,
            changeDetectorRef,
        );
    }

    static [BREADCRUMB_RESOLVER]: ResolveBreadcrumbFn = (route, injector) => {
        const appState = injector.get<AppStateService>(AppStateService as Type<AppStateService>);
        const user = appState.now.entity.user[Number(route.params.id)];
        return of(user ? { title: user.login, doNotTranslate: true } : null);
    }

    ngOnInit(): void {
        super.ngOnInit();

        // init forms
        this.initForms();

        // assign values and validation of current entity
        this.currentEntity$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((currentEntity: DataSourceBO<Raw>) => {
            this.currentEntity = currentEntity;
            this.dsId = Number(this.currentEntity.id);

            // fill form with entity property values
            this.fgPropertiesUpdate(currentEntity);
            this.changeDetectorRef.markForCheck();
        });

        this.permissionDataSourceEntryRead$ = this.currentEntity$.pipe(
            takeUntil(this.stopper.stopper$),
            mergeMap((currentEntity: DataSourceBO<Raw>) => {
                return this.permissionsService.getPermissions(AccessControlledType.DATA_SOURCE, currentEntity.id).pipe(
                    map((typePermissions: TypePermissions) => typePermissions.hasPermission(GcmsPermission.READ)),
                )
            }),
        );

        this.activeTabId$ = this.editorTabTracker.trackEditorTab(this.route);
    }

    btnSavePropertiesOnClick(): void {
        this.updateDataSource();
    }

    btnSaveDataSourceEntriesSorting(): void {
        this.updateDataSourceEntries();
    }

    /**
     * Update dataSource with new name
     */
    updateDataSource(): Promise<void> {
        // assemble payload with conditional properties
        const dataSource: DataSourceUpdateRequest = {
            ...(this.fgProperties.value.name && { name: this.fgProperties.value.name }),
        };
        return this.dataSourceOperations.update(this.currentEntity.id, dataSource).pipe(
            detailLoading(this.appState),
            tap((updatedDataSource: DataSourceBO<Raw>) => {
                this.currentEntity = updatedDataSource;
                this.dataSourceData.reloadEntities();
                this.tableLoader.reload();
                /**
                 * Usually this change detection would be triggered by updating the entity state.
                 * Since we do not make use of the state in the dataSource data service to keep
                 * compatibility with previous implementation, we need to trigger it manually.
                 */
                this.changeDetectorRef.markForCheck();
            }),
            map(() => { this.fgProperties.markAsPristine(); }),
        ).toPromise();
    }

    sortDataSourceEntry(event: TableSortEvent<DataSourceEntryBO>): void {
        this.entryRows = sortEntityRow(this.entryRows, event.from, event.to);
        this.entriesChanged = true;
    }

    updateDataSourceEntries(): Promise<void> {
        const req: DataSourceEntryListUpdateRequest = this.entryRows.map(row => row.item);
        return this.dataSourceEntryOperations.updateAll(req, this.currentEntity.id)
            .toPromise()
            .then(() => {
                this.entriesChanged = false;
                this.dataSourceEntryLoader.reload();
            });
    }

    entriesLoaded(event: TableLoadEndEvent<DataSourceEntryBO>): void {
        this.entryRows = event.rows;
        this.entriesChanged = false;
    }

    resetDataSourceEntries(): Promise<void> {
        this.entryRows = resetEntitySorting(this.entryRows);

        return Promise.resolve();
    }

    /**
     * Initialize form 'Properties'
     */
    protected fgPropertiesInit(): void {
        this.fgProperties = new UntypedFormGroup({
            name: new UntypedFormControl(''),
        });

        this.fgPropertiesSaveDisabled$ = createFormSaveDisabledTracker(this.fgProperties);
    }

    /**
     * Set new value of form 'Properties'
     */
    protected fgPropertiesUpdate(dataSource: DataSourceBO<Normalized | Raw>): void {
        this.fgProperties.setValue({
            name: dataSource.name,
        });
        this.fgProperties.markAsPristine();
    }

    private initForms(): void {
        this.fgPropertiesInit();

        this.tabHandles = {
            [DataSourceDetailTabs.PROPERTIES]: new FormGroupTabHandle(this.fgProperties, {
                save: () => this.updateDataSource(),
            }),
            [DataSourceDetailTabs.ENTRIES]: {
                isDirty: () => this.entriesChanged,
                isValid: () => true,
                save: () => this.updateDataSourceEntries(),
                reset: () => this.resetDataSourceEntries(),
            },
            [DataSourceDetailTabs.USAGE_OVERVIEW]: NULL_FORM_TAB_HANDLE,
        };
    }

}

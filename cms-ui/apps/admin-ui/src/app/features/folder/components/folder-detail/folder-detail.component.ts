import { BO_ID, detailLoading, FormGroupTabHandle, FormTabHandle, NULL_FORM_TAB_HANDLE } from '@admin-ui/common';
import { EditorTabTrackerService, FolderOperations, FolderTrableLoaderService, PermissionsService } from '@admin-ui/core';
import { BaseDetailComponent, FolderDataService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state/providers/app-state/app-state.service';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
    AccessControlledType,
    Folder,
    GcmsPermission,
    Index,
    NormalizableEntityType,
    Normalized,
    Raw,
    TypePermissions,
} from '@gentics/cms-models';
import { NGXLogger } from 'ngx-logger';
import { Observable } from 'rxjs';
import { map, takeUntil, tap } from 'rxjs/operators';

export enum FolderDetailTabs {
    PROPERTIES = 'properties',
    GROUP_PERMISSIONS = 'groupPermissions',
}

// *************************************************************************************************
/**
 * # FolderDetailComponent
 * Display and edit entity folder detail information
 */
// *************************************************************************************************
@Component({
    selector: 'gtx-folder-detail',
    templateUrl: './folder-detail.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FolderDetailComponent extends BaseDetailComponent<'folder', FolderOperations> implements OnInit {

    public readonly FolderDetailTabs = FolderDetailTabs;

    entityIdentifier: NormalizableEntityType = 'folder';

    /** current entity value */
    currentEntity: Folder<Raw>;

    /** form of tab 'Properties' */
    fgProperties: UntypedFormGroup;

    get isLoading(): boolean {
        return this.currentEntity == null || !this.currentEntity.name || this.currentEntity.name === '';
    }

    /** TRUE if logged-in user is allowed to read entity `content` */
    permissionContentRead$: Observable<boolean>;

    activeTabId$: Observable<string>;

    get activeFormTab(): FormTabHandle {
        return this.tabHandles[this.appState.now.ui.editorTab];
    }

    private tabHandles: Index<FolderDetailTabs, FormTabHandle>;

    constructor(
        logger: NGXLogger,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        folderData: FolderDataService,
        changeDetectorRef: ChangeDetectorRef,
        private folderOperations: FolderOperations,
        private permissionsService: PermissionsService,
        private editorTabTracker: EditorTabTrackerService,
        private trableLoader: FolderTrableLoaderService,
    ) {
        super(
            logger,
            route,
            router,
            appState,
            folderData,
            changeDetectorRef,
        );
    }

    ngOnInit(): void {
        super.ngOnInit();

        // init form
        this.fgPropertiesInit();

        // assign values and validation of current entity
        this.currentEntity$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((currentEntity: Folder<Raw>) => {
            this.currentEntity = currentEntity;
            // fill form with entity property values
            this.fgPropertiesUpdate(currentEntity);
            this.changeDetectorRef.markForCheck();
        });

        this.permissionContentRead$ = this.permissionsService.getPermissions(AccessControlledType.CONTENT_ADMIN).pipe(
            map((typePermissions: TypePermissions) => typePermissions.hasPermission(GcmsPermission.READ)),
        );

        this.activeTabId$ = this.editorTabTracker.trackEditorTab(this.route);
    }

    /**
     * Requests changes of folder by id to CMS
     */
    private updateFolder(): Promise<Folder<Raw>> {
        // assemble payload with conditional properties
        const folder: Partial<Folder<Raw>> = {
            id: this.currentEntity.id,
            ...(this.fgProperties.value.name && { name: this.fgProperties.value.name }),
            ...(this.fgProperties.value.path && { path: this.fgProperties.value.path }),
            ...(this.fgProperties.value.description && { description: this.fgProperties.value.description }),
        };
        return this.folderOperations.update(folder.id, { folder: folder }).pipe(
            detailLoading(this.appState),
            map((updatedFolder: Folder<Raw>) => this.currentEntity = updatedFolder),
            tap(() => {
                this.fgProperties.markAsPristine();
                const bo = this.trableLoader.getEntityById(this.currentEntity.id);
                let didReload = false;
                if (bo) {
                    const row = this.trableLoader.flatStore[bo[BO_ID]];
                    if (row) {
                        this.trableLoader.reloadRow(row, null, true).subscribe();
                        didReload = true;
                    }
                }
                if (!didReload) {
                    this.trableLoader.reload();
                }
            }),
        ).toPromise();
    }

    btnSavePropertiesOnClick(): void {
        this.updateFolder();
    }

    /**
     * Initialize form 'Properties'
     */
    protected fgPropertiesInit(): void {
        this.fgProperties = new UntypedFormGroup({
            name: new UntypedFormControl(''),
            path: new UntypedFormControl(''),
            description: new UntypedFormControl(''),
        });

        this.tabHandles = {
            [FolderDetailTabs.PROPERTIES]: new FormGroupTabHandle(this.fgProperties, {
                save: () => this.updateFolder().then(() => {}),
            }),
            [FolderDetailTabs.GROUP_PERMISSIONS]: NULL_FORM_TAB_HANDLE,

        };
    }

    /**
     * Set new value of form 'Properties'
     */
    protected fgPropertiesUpdate(folder: Folder<Normalized | Raw>): void {
        this.fgProperties.setValue({
            name: folder.name,
            path: folder.path,
            description: folder.description,
        });
        this.fgProperties.markAsPristine();
    }
}

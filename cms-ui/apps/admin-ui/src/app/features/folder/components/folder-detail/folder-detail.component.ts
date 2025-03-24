import { detailLoading, FolderDetailTabs, FormGroupTabHandle, FormTabHandle, NULL_FORM_TAB_HANDLE } from '@admin-ui/common';
import { EditorTabTrackerService, FolderOperations, PermissionsService } from '@admin-ui/core';
import { BaseDetailComponent, FolderDataService, FolderTrableLoaderService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state/providers/app-state/app-state.service';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
    AccessControlledType,
    Folder,
    GcmsPermission,
    NormalizableEntityType,
    Normalized,
    Raw,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { map, takeUntil, tap } from 'rxjs/operators';

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

    private tabHandles: Record<FolderDetailTabs, FormTabHandle>;

    constructor(
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
            map(typePermissions => typePermissions.hasPermission(GcmsPermission.READ)),
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
                this.trableLoader.reload();
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

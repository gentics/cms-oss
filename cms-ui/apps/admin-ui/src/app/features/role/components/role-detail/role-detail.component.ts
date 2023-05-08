import { createFormSaveDisabledTracker, FormGroupTabHandle, FormTabHandle } from '@admin-ui/common';
import { detailLoading } from '@admin-ui/common/utils/rxjs-loading-operators/detail-loading.operator';
import { EditorTabTrackerService, PermissionsService, RoleOperations } from '@admin-ui/core/providers';
import { RoleDataService } from '@admin-ui/shared';
import { BaseDetailComponent } from '@admin-ui/shared/components';
import { LanguageDataService } from '@admin-ui/shared/providers/language-data';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
    AccessControlledType,
    AnyModelType,
    GcmsPermission,
    Index,
    Language,
    NormalizableEntityTypesMap,
    Normalized,
    PagePrivileges,
    Raw,
    RoleBO,
    RolePermissions,
    TypePermissions,
} from '@gentics/cms-models';
import { isEqual } from 'lodash';
import { NGXLogger } from 'ngx-logger';
import { Observable } from 'rxjs';
import { distinctUntilChanged, map, publishReplay, refCount, switchMap, takeUntil, tap } from 'rxjs/operators';
import { RoleTableLoaderService } from '../../providers';

export enum RoleDetailTabs {
    properties = 'properties',
    pagePrivileges = 'pagePrivileges',
    filePrivileges = 'filePrivileges',
}

// *************************************************************************************************
/**
 * # RoleDetailComponent
 * Display and edit entity role detail information
 */
// *************************************************************************************************
@Component({
    selector: 'gtx-role-detail',
    templateUrl: './role-detail.component.html',
    styleUrls: [ 'role-detail.component.scss' ],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoleDetailComponent extends BaseDetailComponent<'role', RoleOperations> implements OnInit {

    public readonly RoleDetailTabs = RoleDetailTabs;

    entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'role';

    /** current entity value */
    currentEntity: RoleBO;

    /** current role permissions */
    currentRolePermissions: RolePermissions;

    /** current languages */
    private currentLanguagesSorted: Language[] = [];

    /** form of tab 'Properties' */
    fgProperties: UntypedFormGroup;
    fgPropertiesSaveDisabled$: Observable<boolean>;

    /** form of tab 'Pages' */
    fgPagePrivileges: UntypedFormGroup;
    fgPagePrivilegesSaveDisabled$: Observable<boolean>;
    private pageLanguagesPrivilegesChildControlNames: string[] = [];
    pageLanguagesSortedAndRemainingChildControlNames: { id: string, name: string }[] = [];

    /** form of tab 'Images and Files' */
    fgFilePrivileges: UntypedFormGroup;
    fgFilePrivilegesSaveDisabled$: Observable<boolean>

    get isLoading(): boolean {
        return this.currentEntity == null || !this.currentEntity.name || this.currentEntity.name === '';
    }

    get activeFormTab(): FormTabHandle {
        return this.tabHandles[this.appState.now.ui.editorTab];
    }

    /** TRUE if logged-in user is allowed to read entity `role` */
    permissionRolesRead$: Observable<boolean>;

    activeTabId$: Observable<string>;

    private tabHandles: Index<RoleDetailTabs, FormTabHandle>;

    constructor(
        logger: NGXLogger,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        roleData: RoleDataService,
        changeDetectorRef: ChangeDetectorRef,
        private roleOperations: RoleOperations,
        private languageData: LanguageDataService,
        private permissionsService: PermissionsService,
        private editorTabTracker: EditorTabTrackerService,
        private tableLoader: RoleTableLoaderService,
    ) {
        super(
            logger,
            route,
            router,
            appState,
            roleData,
            changeDetectorRef,
        );
    }

    ngOnInit(): void {
        super.ngOnInit();

        // init language data
        this.initLanguageData();

        // init forms
        this.initForms();

        // assign values and validation of current entity
        this.currentEntity$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((currentEntity: RoleBO<Raw>) => {
            this.currentEntity = currentEntity;
            // fill form with entity property values
            this.fgPropertiesUpdate(currentEntity);
            this.changeDetectorRef.markForCheck();
        });

        this.currentEntity$.pipe(
            takeUntil(this.stopper.stopper$),
            tap(_ => {
                /**
                 * On tab change, the form must be reset to the current state.
                 * This is how discarded changes are typically removed when reentering the previously unselected tab.
                 *
                 * However, role permissions are not part of the entity state and thus refetched every time the entity
                 * state is updated. In order to avoid excessive network requests, we only refetch when the role id
                 * has changed. Nevertheless, this prevents the default procedure that ensures that discarded changes
                 * are actually gone from the UI, when a user revisits the corresponding tab.
                 *
                 * Thus, we always update the form with the role permissions, in case they exist. And then refetch
                 * and update again in case the role id changed. Without going into too much detail, this won't lead
                 * to UI flickering due to the content shown before the update being either identical or invisible
                 * when changed.
                 */

                // fill form with entity permission values, if they exist
                if (this.currentRolePermissions) {
                    this.fgPagePrivilegesUpdate(this.currentRolePermissions);
                    this.fgFilePrivilegesUpdate(this.currentRolePermissions);
                }
            }),
            map((currentEntity: RoleBO<Raw>) => currentEntity.id),
            distinctUntilChanged(isEqual),
            switchMap((id: string): Observable<RolePermissions> => {
                return this.roleOperations.getPermissions(id);
            }),
        ).subscribe((currentRolePermissions: RolePermissions) => {
            this.currentRolePermissions = currentRolePermissions;
            // fill form with entity permission values
            this.fgPagePrivilegesUpdate(currentRolePermissions);
            this.fgFilePrivilegesUpdate(currentRolePermissions);
            this.changeDetectorRef.markForCheck();
        });

        this.permissionRolesRead$ = this.permissionsService.getPermissions(AccessControlledType.ROLE).pipe(
            map((typePermissions: TypePermissions) => typePermissions.hasPermission(GcmsPermission.READ)),
        );

        this.activeTabId$ = this.editorTabTracker.trackEditorTab(this.route);
    }

    private initLanguageData(): void {
        this.languageData.watchAllEntities().pipe(
            takeUntil(this.stopper.stopper$),
            publishReplay(1),
            refCount(),
        ).subscribe((currentLanguages: Language[]) => {
            this.currentLanguagesSorted = currentLanguages.sort((languageA: Language, languageB: Language) => {
                return languageA.name.localeCompare(languageB.name);
            });
            this.updatePageLanguagesSortedAndRemainingChildControlNames();
        });
    }

    /**
     * Requests changes of role by id to CMS
     */
    private updateRole(): Promise<void> {
        // assemble payload with conditional properties
        const role: Partial<RoleBO<Raw>> = {
            id: this.currentEntity.id,
            ...(this.fgProperties.value.name && { name: this.fgProperties.value.name }),
            ...(this.fgProperties.value.description && { description: this.fgProperties.value.description }),
        };
        return this.roleOperations.update(role.id, role).pipe(
            detailLoading(this.appState),
            tap((updatedRole: RoleBO<Raw>) => {
                this.currentEntity = updatedRole;
                // update the UI
                this.changeDetectorRef.markForCheck();
                this.tableLoader.reload();
            }),
            map(() => this.fgProperties.markAsPristine()),
        ).toPromise();
    }

    /**
     * Requests changes of role by id to CMS
     */
    private updateRolePermissions(type: 'page' | 'file'): Promise<void> {
        // assemble payload with conditional properties
        let rolePermissions: RolePermissions = null;
        if (type === 'page') {
            rolePermissions = Object.assign({}, this.currentRolePermissions, this.fgPagePrivileges.value)
        }
        if (type === 'file') {
            rolePermissions = Object.assign({}, this.currentRolePermissions, this.fgFilePrivileges.value)
        }
        if (rolePermissions !== null) {
            return this.roleOperations.updatePermissions(this.currentEntity.id, rolePermissions).pipe(
                detailLoading(this.appState),
                tap((updatedRolePermissions: RolePermissions) => this.currentRolePermissions = updatedRolePermissions),
                map(() => {
                    if (type === 'page') {
                        this.fgPagePrivileges.markAsPristine();
                    }
                    if (type === 'file') {
                        this.fgFilePrivileges.markAsPristine();
                    }
                }),
            ).toPromise();
        }
    }

    btnSavePropertiesOnClick(): void {
        this.updateRole();
    }

    btnSavePrivilegesOnClick(type: 'page' | 'file'): void {
        this.updateRolePermissions(type);
    }

    /**
     * Initialize form 'Properties'
     */
    protected fgPropertiesInit(): void {
        this.fgProperties = new UntypedFormGroup({
            name: new UntypedFormControl(''),
            description: new UntypedFormControl(''),
        });

        this.fgPropertiesSaveDisabled$ = createFormSaveDisabledTracker(this.fgProperties);
    }

    /**
     * Initialize form 'Page Privileges'
     */
    protected fgPagePrivilegesInit(): void {
        this.fgPagePrivileges = new UntypedFormGroup({
            page: this.buildPagePrivilegesFormGroup(),
            pageLanguages: new UntypedFormGroup({}),
        });

        this.fgPagePrivilegesSaveDisabled$ = createFormSaveDisabledTracker(this.fgPagePrivileges);
    }

    /**
     * Initialize form 'File Privileges'
     */
    protected fgFilePrivilegesInit(): void {
        this.fgFilePrivileges = new UntypedFormGroup({
            file: this.buildFilePrivilegesFormGroup(),
        });

        this.fgFilePrivilegesSaveDisabled$ = createFormSaveDisabledTracker(this.fgFilePrivileges);
    }

    /**
     * Set new value of form 'Properties'
     */
    protected fgPropertiesUpdate(role: RoleBO<Normalized | Raw>): void {
        this.fgProperties.setValue({
            name: role.name,
            description: role.description,
        });
        this.fgProperties.markAsPristine();
    }

    /**
     * Set new value of form 'Properties'
     */
    protected fgPagePrivilegesUpdate(rolePermissions: RolePermissions): void {
        this.fgPagePrivileges.get('page').setValue({
            viewpage: rolePermissions.page.viewpage,
            createpage: rolePermissions.page.createpage,
            updatepage: rolePermissions.page.updatepage,
            deletepage: rolePermissions.page.deletepage,
            publishpage: rolePermissions.page.publishpage,
            translatepage: rolePermissions.page.translatepage,
        }, { onlySelf: false, emitEvent: false });

        const pageLanguagesPrivileges: Index<string, PagePrivileges> = rolePermissions.pageLanguages;
        const updatedPageLanguagesPrivileges: string[] = [];

        const pageLanguagesFormGroup: UntypedFormGroup = (this.fgPagePrivileges.get('pageLanguages') as UntypedFormGroup);

        for (const [id, formGroup] of Object.entries(pageLanguagesFormGroup.controls)) {
            if (pageLanguagesPrivileges[id]) {
                formGroup.setValue({
                    viewpage: pageLanguagesPrivileges[id].viewpage,
                    createpage: pageLanguagesPrivileges[id].createpage,
                    updatepage: pageLanguagesPrivileges[id].updatepage,
                    deletepage: pageLanguagesPrivileges[id].deletepage,
                    publishpage: pageLanguagesPrivileges[id].publishpage,
                    translatepage: pageLanguagesPrivileges[id].translatepage,
                }, { onlySelf: false, emitEvent: false });
                updatedPageLanguagesPrivileges.push(id);
            } else {
                pageLanguagesFormGroup.removeControl(id);
            }
        }

        for (const [id, pageLanguagePrivileges] of Object.entries(pageLanguagesPrivileges)) {
            if (!updatedPageLanguagesPrivileges.includes(id)) {
                pageLanguagesFormGroup.addControl(id, new UntypedFormGroup({
                    viewpage: new UntypedFormControl(pageLanguagePrivileges.viewpage),
                    createpage: new UntypedFormControl(pageLanguagePrivileges.createpage),
                    updatepage: new UntypedFormControl(pageLanguagePrivileges.updatepage),
                    deletepage: new UntypedFormControl(pageLanguagePrivileges.deletepage),
                    publishpage: new UntypedFormControl(pageLanguagePrivileges.publishpage),
                    translatepage: new UntypedFormControl(pageLanguagePrivileges.translatepage),
                }));
            }
        }

        this.fgPagePrivileges.updateValueAndValidity();
        this.pageLanguagesPrivilegesChildControlNames = Object.keys(pageLanguagesFormGroup.controls);
        this.updatePageLanguagesSortedAndRemainingChildControlNames();
        this.fgPagePrivileges.markAsPristine();
    }

    /**
     * Set new value of form 'Properties'
     */
    protected fgFilePrivilegesUpdate(rolePermissions: RolePermissions): void {
        this.fgFilePrivileges.get('file').setValue({
            viewfile: rolePermissions.file.viewfile,
            createfile: rolePermissions.file.createfile,
            updatefile: rolePermissions.file.updatefile,
            deletefile: rolePermissions.file.deletefile,
        });
        this.fgFilePrivileges.markAsPristine();
    }

    private initForms(): void {
        this.fgPropertiesInit();
        this.fgPagePrivilegesInit();
        this.fgFilePrivilegesInit();

        this.tabHandles = {
            [RoleDetailTabs.properties]: new FormGroupTabHandle(this.fgProperties, {
                save: () => this.updateRole(),
            }),
            [RoleDetailTabs.pagePrivileges]: new FormGroupTabHandle(this.fgPagePrivileges, {
                save: () => this.updateRolePermissions('page'),
            }),
            [RoleDetailTabs.filePrivileges]: new FormGroupTabHandle(this.fgFilePrivileges, {
                save: () => this.updateRolePermissions('file'),
            }),
        };
    }

    private buildPagePrivilegesFormGroup(): UntypedFormGroup {
        return new UntypedFormGroup({
            viewpage: new UntypedFormControl(false),
            createpage: new UntypedFormControl(false),
            updatepage: new UntypedFormControl(false),
            deletepage: new UntypedFormControl(false),
            publishpage: new UntypedFormControl(false),
            translatepage: new UntypedFormControl(false),
        })
    }

    private buildFilePrivilegesFormGroup(): UntypedFormGroup {
        return new UntypedFormGroup({
            viewfile: new UntypedFormControl(false),
            createfile: new UntypedFormControl(false),
            updatefile: new UntypedFormControl(false),
            deletefile: new UntypedFormControl(false),
        })
    }

    private updatePageLanguagesSortedAndRemainingChildControlNames(): void {
        this.pageLanguagesSortedAndRemainingChildControlNames = [];
        const addedChildControlNames = [];
        for (const currentLanguage of this.currentLanguagesSorted) {
            const stringifiedId = `${currentLanguage.id}`;
            if (this.pageLanguagesPrivilegesChildControlNames.includes(stringifiedId)) {
                this.pageLanguagesSortedAndRemainingChildControlNames.push({
                    id: stringifiedId,
                    name: currentLanguage.name,
                });
                addedChildControlNames.push(stringifiedId);
            }
        }
        for (const childControlName of this.pageLanguagesPrivilegesChildControlNames) {
            if (!addedChildControlNames.includes(childControlName)) {
                this.pageLanguagesSortedAndRemainingChildControlNames.push({
                    id: childControlName,
                    name: childControlName,
                });
            }
        }
        this.changeDetectorRef.markForCheck();
    }

    childControlInformationTrackBy(_: number, childControlInformation: { id: string, name: string }): string {
        return childControlInformation.id;
    }

}

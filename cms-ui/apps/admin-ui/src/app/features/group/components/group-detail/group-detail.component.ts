import { detailLoading, FormGroupTabHandle, FormTabHandle, GroupDetailTabs, NULL_FORM_TAB_HANDLE, PermissionsTreeType } from '@admin-ui/common';
import {
    BREADCRUMB_RESOLVER,
    EditorTabTrackerService,
    GroupOperations,
    GroupTableLoaderService,
    PermissionsService,
    ResolveBreadcrumbFn,
} from '@admin-ui/core';
import { BaseDetailComponent, GroupDataService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state/providers/app-state/app-state.service';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, Type } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
    AccessControlledType,
    GcmsPermission,
    Group,
    Index,
    NormalizableEntityType,
    Normalized,
    Raw,
    TypePermissions,
} from '@gentics/cms-models';
import { NGXLogger } from 'ngx-logger';
import { Observable, of } from 'rxjs';
import { map, takeUntil, tap } from 'rxjs/operators';

// *************************************************************************************************
/**
 * # GroupDetailComponent
 * Display and edit entity group detail information
 */
// *************************************************************************************************
@Component({
    selector: 'gtx-group-detail',
    templateUrl: './group-detail.component.html',
    styleUrls: [ 'group-detail.component.scss' ],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GroupDetailComponent extends BaseDetailComponent<'group', GroupOperations> implements OnInit {

    public readonly PermissionsTreeType = PermissionsTreeType;
    public readonly GroupDetailTabs = GroupDetailTabs;

    entityIdentifier: NormalizableEntityType = 'group';

    /** current entity value */
    currentEntity: Group<Raw>;

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

    private tabHandles: Index<GroupDetailTabs, FormTabHandle>;

    constructor(
        logger: NGXLogger,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        groupData: GroupDataService,
        changeDetectorRef: ChangeDetectorRef,
        private groupOperations: GroupOperations,
        private permissionsService: PermissionsService,
        private editorTabTracker: EditorTabTrackerService,
        private tableLoader: GroupTableLoaderService,
    ) {
        super(
            logger,
            route,
            router,
            appState,
            groupData,
            changeDetectorRef,
        );
    }

    static [BREADCRUMB_RESOLVER]: ResolveBreadcrumbFn = (route, injector) => {
        const appState = injector.get<AppStateService>(AppStateService as Type<AppStateService>);
        const entity = appState.now.entity.group[Number(route.params.id)];
        return of(entity ? { title: entity.name, doNotTranslate: true } : null);
    }

    ngOnInit(): void {
        super.ngOnInit();

        // init form
        this.fgPropertiesInit();

        // assign values and validation of current entity
        this.currentEntity$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((currentEntity: Group<Raw>) => {
            this.currentEntity = currentEntity;
            // fill form with entity property values
            this.fgPropertiesUpdate(currentEntity);
            this.changeDetectorRef.markForCheck();
        });

        this.permissionContentRead$ = this.permissionsService.getPermissions(AccessControlledType.CONTENT).pipe(
            map((typePermissions: TypePermissions) => typePermissions.hasPermission(GcmsPermission.READ)),
        );

        this.activeTabId$ = this.editorTabTracker.trackEditorTab(this.route);
    }

    /**
     * Requests changes of group by id to CMS
     */
    private updateGroup(): Promise<Group<Raw>> {
        // assemble payload with conditional properties
        const group: Group<Raw> = {
            id: this.currentEntity.id,
            ...(this.fgProperties.value.name && { name: this.fgProperties.value.name }),
            ...(this.fgProperties.value.description && { description: this.fgProperties.value.description }),
        };
        return this.groupOperations.update(group.id, group).pipe(
            detailLoading(this.appState),
            map((updatedGroup: Group<Raw>) => this.currentEntity = updatedGroup),
            tap(() => {
                this.fgProperties.markAsPristine();
                this.tableLoader.reload();
            }),
        ).toPromise();
    }

    btnSavePropertiesOnClick(): void {
        this.updateGroup();
    }

    /**
     * Initialize form 'Properties'
     */
    protected fgPropertiesInit(): void {
        this.fgProperties = new UntypedFormGroup({
            name: new UntypedFormControl(''),
            description: new UntypedFormControl(''),
        });

        this.tabHandles = {
            [GroupDetailTabs.PROPERTIES]: new FormGroupTabHandle(this.fgProperties, {
                save: () => this.updateGroup().then(() => {}),
            }),
            [GroupDetailTabs.SUB_GROUPS]: NULL_FORM_TAB_HANDLE,
            [GroupDetailTabs.CONTENT_PERMISSIONS]: NULL_FORM_TAB_HANDLE,
            [GroupDetailTabs.ADMIN_PERMISSIONS]: NULL_FORM_TAB_HANDLE,
            [GroupDetailTabs.GROUP_USERS]: NULL_FORM_TAB_HANDLE,
        };
    }

    /**
     * Set new value of form 'Properties'
     */
    protected fgPropertiesUpdate(group: Group<Normalized | Raw>): void {
        this.fgProperties.setValue({
            name: group.name,
            description: group.description,
        });
        this.fgProperties.markAsPristine();
    }

}

import { AdminUIModuleRoutes } from '@admin-ui/common';
import { AuthOperations, PermissionsService } from '@admin-ui/core';
import { AppStateService, CloseEditor, SetUIFocusEntity } from '@admin-ui/state';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AccessControlledType, Feature, GcmsPermission } from '@gentics/cms-models';
import { Observable, combineLatest, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

@Component({
    selector: 'gtx-dashboard',
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit {

    readonly AdminUIModuleRoutes = AdminUIModuleRoutes;

    // Observables for checking which modules are enabled based on the user's permissions.
    public usersModuleEnabled$: Observable<boolean>;
    public groupsModuleEnabled$: Observable<boolean>;
    public rolesModuleEnabled$: Observable<boolean>;
    public foldersModuleEnabled$: Observable<boolean>;
    public languagesModuleEnabled$: Observable<boolean>;
    public logsModuleEnabled$: Observable<boolean>;
    public nodesModuleEnabled$: Observable<boolean>;
    public testingModuleEnabled$: Observable<boolean>;
    public dataSourcesModuleEnabled$: Observable<boolean>;
    public packagesModuleEnabled$: Observable<boolean>;
    public contentRepositoriesModuleEnabled$: Observable<boolean>;
    public crFragmentsModuleEnabled$: Observable<boolean>;
    public templatesModuleEnabled$: Observable<boolean>;
    public schedulerModuleEnabled$: Observable<boolean>;
    public contentStagingModuleEnabled$: Observable<boolean>;
    public maintenancemodeModuleEnabled$: Observable<boolean>;
    public searchMaintenanceModuleEnabled$: Observable<boolean>;
    public contentMaintenanceModuleEnabled$: Observable<boolean>;
    public objectPropertiesModuleEnabled$: Observable<boolean>;
    public constructsModuleEnabled$: Observable<boolean>;
    public meshBrowserModuleEnabled$: Observable<boolean>;

    constructor(
        private appState: AppStateService,
        private authOps: AuthOperations,
        private permissions: PermissionsService,
        private router: Router,
    ) {}

    ngOnInit(): void {
        // In case that any item is still focused, we need to clear it
        this.appState.dispatch(new SetUIFocusEntity(null, null, null));

        this.contentRepositoriesModuleEnabled$ = this.permissions.checkPermissions({
            type: AccessControlledType.CONTENT_ADMIN,
            permissions: GcmsPermission.READ,
        });
        this.usersModuleEnabled$ = this.permissions.checkPermissions({ type: AccessControlledType.USER_ADMIN, permissions: GcmsPermission.READ });
        this.groupsModuleEnabled$ = this.permissions.checkPermissions({ type: AccessControlledType.GROUP_ADMIN, permissions: GcmsPermission.READ });
        this.rolesModuleEnabled$ = this.permissions.checkPermissions({ type: AccessControlledType.ROLE, permissions: GcmsPermission.READ });
        this.foldersModuleEnabled$ = this.permissions.checkPermissions({ type: AccessControlledType.CONTENT_ADMIN, permissions: GcmsPermission.READ });
        this.languagesModuleEnabled$ = this.permissions.checkPermissions({ type: AccessControlledType.LANGUAGE_ADMIN, permissions: GcmsPermission.READ });
        this.logsModuleEnabled$ = this.permissions.checkPermissions({ type: AccessControlledType.ACTION_LOG, permissions: GcmsPermission.READ });
        this.nodesModuleEnabled$ = this.permissions.checkPermissions({ type: AccessControlledType.CONTENT, permissions: GcmsPermission.READ });
        this.dataSourcesModuleEnabled$ = this.permissions.checkPermissions({ type: AccessControlledType.DATA_SOURCE_ADMIN, permissions: GcmsPermission.READ });
        this.packagesModuleEnabled$ = combineLatest([
            this.appState.select(state => state.features.global[Feature.DEVTOOLS]),
            this.permissions.checkPermissions({
                type: AccessControlledType.DEVTOOL_ADMIN,
                permissions: GcmsPermission.READ,
            }),
        ]).pipe(
            map(([featureEnabled, hasPermission]) => featureEnabled && hasPermission),
        );
        this.searchMaintenanceModuleEnabled$ = combineLatest([
            this.appState.select(state => state.features.global[Feature.ELASTICSEARCH]),
            this.permissions.checkPermissions({
                type: AccessControlledType.SEARCH_INDEX_MAINTENANCE,
                permissions: GcmsPermission.READ,
            }),
        ]).pipe(
            map(([featureEnabled, hasPermission]) => featureEnabled && hasPermission),
        );
        this.crFragmentsModuleEnabled$ = this.permissions.checkPermissions({ type: AccessControlledType.CR_FRAGMENT_ADMIN, permissions: GcmsPermission.READ });
        this.templatesModuleEnabled$ = this.permissions.checkPermissions({ type: AccessControlledType.ADMIN, permissions: GcmsPermission.READ });
        // Can open the schedule module and can either read schedules or tasks
        this.schedulerModuleEnabled$ = this.permissions.checkPermissions({ type: AccessControlledType.SCHEDULER, permissions: GcmsPermission.READ }).pipe(
            switchMap(canOpen => {
                if (!canOpen) {
                    return of(canOpen);
                }

                return combineLatest([
                    this.permissions.checkPermissions({ type: AccessControlledType.SCHEDULER, permissions: GcmsPermission.READ_SCHEDULES }),
                    this.permissions.checkPermissions({ type: AccessControlledType.SCHEDULER, permissions: GcmsPermission.READ_TASKS }),
                ]).pipe(
                    map(([schedules, tasks]) => schedules || tasks),
                );
            }),
        );
        this.objectPropertiesModuleEnabled$ = this.permissions.checkPermissions({
            type: AccessControlledType.OBJECT_PROPERTY_ADMIN,
            permissions: GcmsPermission.READ,
        });
        this.constructsModuleEnabled$ = this.permissions.checkPermissions({
            type: AccessControlledType.CONSTRUCT_ADMIN,
            permissions: GcmsPermission.READ,
        });
        this.contentStagingModuleEnabled$ = combineLatest([
            this.appState.select(state => state.features.global[Feature.CONTENT_STAGING]),
            this.permissions.checkPermissions({ type: AccessControlledType.CONTENT_STAGING_ADMIN, permissions: GcmsPermission.READ }),
        ]).pipe(
            map(([featureEnabled, hasPermission]) => featureEnabled && hasPermission),
        );
        this.contentMaintenanceModuleEnabled$ = this.permissions.checkPermissions({
            type: AccessControlledType.MAINTENANCE,
            permissions: GcmsPermission.READ,
        });
        this.maintenancemodeModuleEnabled$ = this.permissions.checkPermissions({
            type: AccessControlledType.SYSTEM_MAINTANANCE,
            permissions: GcmsPermission.READ,
        });
        this.meshBrowserModuleEnabled$ = combineLatest([
            this.appState.select(state => state.features.global[Feature.MESH_CR]),
            this.permissions.checkPermissions({ type: AccessControlledType.CONTENT_REPOSITORY_ADMIN, permissions: GcmsPermission.READ }),
        ]).pipe(
            map(([featureEnabled, hasPermission]) => featureEnabled && hasPermission),
        );

        // Just for testing.
        this.testingModuleEnabled$ = this.permissions.checkPermissions([
            { type: AccessControlledType.MAINTENANCE, permissions: GcmsPermission.READ },
            { type: AccessControlledType.SCHEDULER, permissions: [GcmsPermission.READ, GcmsPermission.SET_PERMISSION] },
        ]);

        /* Needed for a UI bug where the detail view stays open if we open it
           in another module - this bug is still relevant with direct navigation
           by URL to modules with no detail view */
        this.appState.dispatch(new CloseEditor());
    }

    onLogoutClick(): void {
        this.authOps.logout(this.appState.now.auth.sid)
            .then(() => {
                this.router.navigate([`/${AdminUIModuleRoutes.LOGIN}`]);
            });
    }

}

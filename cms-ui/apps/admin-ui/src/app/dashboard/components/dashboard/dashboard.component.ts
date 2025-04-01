import { AdminUIModuleRoutes } from '@admin-ui/common';
import { AuthOperations, PermissionsService } from '@admin-ui/core';
import { AppStateService, CloseEditor, SetUIFocusEntity } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AccessControlledType, Feature, GcmsPermission, Variant } from '@gentics/cms-models';
import { Subscription, combineLatest, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

@Component({
    selector: 'gtx-dashboard',
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnInit, OnDestroy {

    readonly AdminUIModuleRoutes = AdminUIModuleRoutes;

    // Observables for checking which modules are enabled based on the user's permissions.
    public usersModuleEnabled = false;
    public groupsModuleEnabled = false;
    public rolesModuleEnabled = false;
    public foldersModuleEnabled = false;
    public languagesModuleEnabled = false;
    public logsModuleEnabled = false;
    public nodesModuleEnabled = false;
    public dataSourcesModuleEnabled = false;
    public packagesModuleEnabled = false;
    public contentRepositoriesModuleEnabled = false;
    public crFragmentsModuleEnabled = false;
    public templatesModuleEnabled = false;
    public schedulerModuleEnabled = false;
    public contentStagingModuleEnabled = false;
    public maintenancemodeModuleEnabled = false;
    public searchMaintenanceModuleEnabled = false;
    public contentMaintenanceModuleEnabled = false;
    public objectPropertiesModuleEnabled = false;
    public constructsModuleEnabled = false;
    public meshBrowserModuleEnabled = false;
    public licenseModuleEnabled = false;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private appState: AppStateService,
        private authOps: AuthOperations,
        private permissions: PermissionsService,
        private router: Router,
    ) {}

    ngOnInit(): void {
        // In case that any item is still focused, we need to clear it
        this.appState.dispatch(new SetUIFocusEntity(null, null, null));

        this.subscriptions.push(this.permissions.checkPermissions({
            type: AccessControlledType.CONTENT_ADMIN,
            permissions: GcmsPermission.READ,
        }).subscribe(enabled => {
            this.contentRepositoriesModuleEnabled = enabled;
            this.foldersModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.permissions.checkPermissions({
            type: AccessControlledType.USER_ADMIN,
            permissions: GcmsPermission.READ,
        }).subscribe(enabled => {
            this.usersModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.permissions.checkPermissions({
            type: AccessControlledType.GROUP_ADMIN,
            permissions: GcmsPermission.READ,
        }).subscribe(enabled => {
            this.groupsModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.permissions.checkPermissions({
            type: AccessControlledType.ROLE,
            permissions: GcmsPermission.READ,
        }).subscribe(enabled => {
            this.rolesModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.permissions.checkPermissions({
            type: AccessControlledType.LANGUAGE_ADMIN,
            permissions: GcmsPermission.READ,
        }).subscribe(enabled => {
            this.languagesModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.permissions.checkPermissions({
            type: AccessControlledType.ACTION_LOG,
            permissions: GcmsPermission.READ,
        }).subscribe(enabled => {
            this.logsModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.permissions.checkPermissions({
            type: AccessControlledType.CONTENT,
            permissions: GcmsPermission.READ,
        }).subscribe(enabled => {
            this.nodesModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.permissions.checkPermissions({
            type: AccessControlledType.DATA_SOURCE_ADMIN,
            permissions: GcmsPermission.READ,
        }).subscribe(enabled => {
            this.dataSourcesModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(combineLatest([
            this.appState.select(state => state.features.global[Feature.DEVTOOLS]),
            this.permissions.checkPermissions({
                type: AccessControlledType.DEVTOOL_ADMIN,
                permissions: GcmsPermission.READ,
            }),
        ]).pipe(
            map(([featureEnabled, hasPermission]) => featureEnabled && hasPermission),
        ).subscribe(enabled => {
            this.packagesModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(combineLatest([
            this.appState.select(state => state.features.global[Feature.ELASTICSEARCH]),
            this.permissions.checkPermissions({
                type: AccessControlledType.SEARCH_INDEX_MAINTENANCE,
                permissions: GcmsPermission.READ,
            }),
        ]).pipe(
            map(([featureEnabled, hasPermission]) => featureEnabled && hasPermission),
        ).subscribe(enabled => {
            this.searchMaintenanceModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }))

        this.subscriptions.push(this.permissions.checkPermissions({
            type: AccessControlledType.CR_FRAGMENT_ADMIN,
            permissions: GcmsPermission.READ,
        }).subscribe(enabled => {
            this.crFragmentsModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.permissions.checkPermissions({
            type: AccessControlledType.ADMIN,
            permissions: GcmsPermission.READ,
        }).subscribe(enabled => {
            this.templatesModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        // Can open the schedule module and can either read schedules or tasks
        this.subscriptions.push(this.permissions.checkPermissions({
            type: AccessControlledType.SCHEDULER,
            permissions: GcmsPermission.READ,
        }).pipe(
            switchMap(canOpen => {
                if (!canOpen) {
                    return of(canOpen);
                }

                return combineLatest([
                    this.permissions.checkPermissions({
                        type: AccessControlledType.SCHEDULER,
                        permissions: GcmsPermission.READ_SCHEDULES,
                    }),
                    this.permissions.checkPermissions({
                        type: AccessControlledType.SCHEDULER,
                        permissions: GcmsPermission.READ_TASKS,
                    }),
                ]).pipe(
                    map(([schedules, tasks]) => schedules || tasks),
                );
            }),
        ).subscribe(enabled => {
            this.schedulerModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.permissions.checkPermissions({
            type: AccessControlledType.OBJECT_PROPERTY_ADMIN,
            permissions: GcmsPermission.READ,
        }).subscribe(enabled => {
            this.objectPropertiesModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.permissions.checkPermissions({
            type: AccessControlledType.CONSTRUCT_ADMIN,
            permissions: GcmsPermission.READ,
        }).subscribe(enabled => {
            this.constructsModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(combineLatest([
            this.appState.select(state => state.features.global[Feature.CONTENT_STAGING]),
            this.permissions.checkPermissions({
                type: AccessControlledType.CONTENT_STAGING_ADMIN,
                permissions: GcmsPermission.READ,
            }),
        ]).pipe(
            map(([featureEnabled, hasPermission]) => featureEnabled && hasPermission),
        ).subscribe(enabled => {
            this.contentStagingModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.permissions.checkPermissions({
            type: AccessControlledType.MAINTENANCE,
            permissions: GcmsPermission.READ,
        }).subscribe(enabled => {
            this.contentMaintenanceModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.permissions.checkPermissions({
            type: AccessControlledType.SYSTEM_MAINTANANCE,
            permissions: GcmsPermission.READ,
        }).subscribe(enabled => {
            this.maintenancemodeModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(combineLatest([
            this.appState.select(state => state.features.global[Feature.MESH_CR]),
            this.permissions.checkPermissions({
                type: AccessControlledType.CONTENT_REPOSITORY_ADMIN,
                permissions: GcmsPermission.READ,
            }),
        ]).pipe(
            map(([featureEnabled, hasPermission]) => featureEnabled && hasPermission),
        ).subscribe(enabled => {
            this.meshBrowserModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(combineLatest([
            this.appState.select(state => state.ui.cmpVersion),
            this.permissions.checkPermissions({
                type: AccessControlledType.LICENSING,
                permissions: GcmsPermission.READ,
            }),
        ]).pipe(
            map(([version, hasPermission]) => version?.variant === Variant.ENTERPRISE && hasPermission),
        ).subscribe(enabled => {
            this.licenseModuleEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        /* Needed for a UI bug where the detail view stays open if we open it
           in another module - this bug is still relevant with direct navigation
           by URL to modules with no detail view */
        this.appState.dispatch(new CloseEditor());
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    onLogoutClick(): void {
        this.authOps.logout(this.appState.now.auth.sid)
            .then(() => {
                this.router.navigate([`/${AdminUIModuleRoutes.LOGIN}`]);
            });
    }

}

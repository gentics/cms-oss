import { RouteData } from '@admin-ui/common/routing/gcms-admin-ui-route';
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { first, take, tap } from 'rxjs/operators';

import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { I18nNotificationService } from '../../i18n-notification';
import { PermissionsService, RequiredTypePermissions } from '../../permissions';

// tslint:disable: jsdoc-format
/**
 * A guard to prevent users from navigating to routes, for which they do not have permissions.
 *
 * @note Currently `PermissionsGuard` supports type permissions only, but if necessary, we can extend it to
 * support resolving instance permissions as well. This would require a resolver that is configured with
 * the permissions that need to be checked and that can extract the `instanceId` and `nodeId` from the route.
 *
 * @example
   ```
   // Route Config:
   {
        path: 'testing',
        canActivate: [PermissionsGuard],
        loadChildren: './features/testing-do-not-release/testing-do-not-release.module#TestingDoNotReleaseModule',
        data: {
            ...,
            typePermissions: [
                { type: AccessControlledType.maintenance, permissions: GcmsPermission.read },
                { type: AccessControlledType.scheduler, permissions: [GcmsPermission.read, GcmsPermission.setperm] }
            ]
        }
    }
   ```
 */
// tslint:enable: jsdoc-format
@Injectable()
export class PermissionsGuard  {

    constructor(
        private permissionsService: PermissionsService,
        private router: Router,
        private i18nNotification: I18nNotificationService,
    ) {}

    canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
        return this._canActivate(route);
    }

    canActivateChild(route: ActivatedRouteSnapshot): Observable<boolean> {
        return this._canActivate(route);
    }

    /**
     * General canActivate method fitting both guard interface implementations.
     */
    private _canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
        const reqPermissions = this.getRequiredPermissions(route);
        return this.permissionsService.checkPermissions(reqPermissions).pipe(
            tap(permissionsGranted => {
                if (!permissionsGranted) {
                    // if user has not required permissions, cancel routing and display notification
                    this.i18nNotification.show({message: 'common.no_permissions_for_module', type: 'alert'});
                    // if user has no permissions for this application's modules, redirect to unauthorized page
                    this.userCanAccessDashboard().pipe(first()).subscribe(userCanAccessDashboard => {
                        if (!userCanAccessDashboard) {
                            this.router.navigateByUrl( 'unauthorized' );
                        }
                    });
                }
            }),
            take(1),
        );
    }

    /**
     * @returns permission data from route for permission service to proceed.
     */
    private getRequiredPermissions(route: ActivatedRouteSnapshot): RequiredTypePermissions | RequiredTypePermissions[] {
        const requiredPermissions = (route.data as RouteData).typePermissions;
        if (requiredPermissions) {
            return requiredPermissions;
        } else {
            throw new Error('When using the PermissionsGuard, the route\'s `data.typePermissions` property must be defined');
        }
    }

    /**
     * @returns FALSE if user has no permissions to view any administrational sub-modules.
     */
    private userCanAccessDashboard(): Observable<boolean> {
        return this.permissionsService.checkPermissions({
            type: AccessControlledType.ADMIN,
            permissions: [
                GcmsPermission.READ,
            ],
        });
    }

}

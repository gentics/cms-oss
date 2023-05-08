import { InterfaceOf } from '@admin-ui/common';
import { RouteData } from '@admin-ui/common/routing/gcms-admin-ui-route';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { take } from 'rxjs/operators';

import { PermissionsService, RequiredPermissions } from '../..';
import { createDelayedObservable } from '../../../../../testing';
import { I18nNotificationService } from '../../i18n-notification';
import { MockI18nNotificationService } from '../../i18n-notification/i18n-notification.service.mock';
import { PermissionsGuard } from './permissions.guard';

const TYPE1 = AccessControlledType.MAINTENANCE;
const TYPE2 = AccessControlledType.USER_ADMIN;

class MockPermissionsService implements Partial<InterfaceOf<PermissionsService>> {
    checkPermissions = jasmine.createSpy('checkPermissions');
}

class MockRouter {
    navigateByUrl = jasmine.createSpy('navigateByUrl').and.stub();
}

describe('PermissionsGuard', () => {

    let permissionsService: MockPermissionsService;
    let permissionsGuard: PermissionsGuard;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                PermissionsGuard,
                { provide: I18nNotificationService, useClass: MockI18nNotificationService },
                { provide: PermissionsService, useClass: MockPermissionsService },
                { provide: Router, useClass: MockRouter },
            ],
        });

        permissionsGuard = TestBed.get(PermissionsGuard);
        permissionsService = TestBed.get(PermissionsService);
    });

    function setUpRouteSnapshot(requiredPermissions: RequiredPermissions | RequiredPermissions[]): ActivatedRouteSnapshot {
        const data: RouteData = { typePermissions: requiredPermissions };
        return { data } as any;
    }

    function setUpPermissionsService(permissionsGranted: boolean): void {
        permissionsService.checkPermissions.and.returnValue(createDelayedObservable(permissionsGranted));
    }

    it('passes the route data to the PermissionsService and returns a positive result', fakeAsync(() => {
        const route = setUpRouteSnapshot({ type: TYPE1, permissions: GcmsPermission.READ });
        setUpPermissionsService(true);

        let canActivate: boolean;
        permissionsGuard.canActivate(route)
            .pipe(take(1))
            .subscribe(result => canActivate = result);

        tick();
        expect(canActivate).toBe(true);
        expect(permissionsService.checkPermissions).toHaveBeenCalledTimes(1);
        expect(permissionsService.checkPermissions).toHaveBeenCalledWith((route.data as RouteData).typePermissions);
    }));

    it('passes the route data to the PermissionsService and returns a negative result', fakeAsync(() => {
        const route = setUpRouteSnapshot([
            { type: TYPE1, permissions: GcmsPermission.READ },
            { type: TYPE2, permissions: [GcmsPermission.READ, GcmsPermission.SET_PERMISSION] },
        ]);
        setUpPermissionsService(false);

        let canActivate: boolean;
        permissionsGuard.canActivate(route)
            .pipe(take(1))
            .subscribe(result => canActivate = result);

        tick();
        expect(canActivate).toBe(false);
        expect(permissionsService.checkPermissions).toHaveBeenCalledTimes(2);
        expect(permissionsService.checkPermissions).toHaveBeenCalledWith((route.data as RouteData).typePermissions);
    }));

    it('shows a notification if the user does not have sufficient permissions', fakeAsync(() => {
        const i18nNotification = TestBed.get(I18nNotificationService) as MockI18nNotificationService;
        const route = setUpRouteSnapshot({ type: TYPE1, permissions: GcmsPermission.READ });
        setUpPermissionsService(false);

        let canActivate: boolean;
        permissionsGuard.canActivate(route)
            .pipe(take(1))
            .subscribe(result => canActivate = result);

        tick();
        expect(canActivate).toBe(false);
        expect(i18nNotification.show).toHaveBeenCalledWith({ message: 'common.no_permissions_for_module', type: 'alert' });
    }));

});

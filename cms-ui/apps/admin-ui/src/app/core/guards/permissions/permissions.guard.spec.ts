import { InterfaceOf } from '@admin-ui/common';
import { RouteData } from '@admin-ui/common/models/routing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute, ActivatedRouteSnapshot, Router } from '@angular/router';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { take } from 'rxjs/operators';
import { createDelayedObservable } from '../../../../testing';
import { PermissionsService, RequiredPermissions } from '../../providers';
import { I18nNotificationService } from '../../providers/i18n-notification/i18n-notification.service';
import { MockI18nNotificationService } from '../../providers/i18n-notification/i18n-notification.service.mock';
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
    let i18nNotification: MockI18nNotificationService;

    function setUpRouteSnapshot(requiredPermissions: RequiredPermissions | RequiredPermissions[]): ActivatedRouteSnapshot {
        const data: RouteData = { typePermissions: requiredPermissions };
        const snapshot: ActivatedRouteSnapshot = { data } as any;

        TestBed.configureTestingModule({
            providers: [
                PermissionsGuard,
                { provide: I18nNotificationService, useClass: MockI18nNotificationService },
                { provide: PermissionsService, useClass: MockPermissionsService },
                { provide: Router, useClass: MockRouter },
                { provide: ActivatedRoute, useValue: { snapshot } },
            ],
        });

        permissionsGuard = TestBed.inject(PermissionsGuard);
        permissionsService = TestBed.inject(PermissionsService) as any;
        i18nNotification = TestBed.inject(I18nNotificationService) as any;

        return snapshot;
    }

    function setUpPermissionsService(permissionsGranted: boolean): void {
        permissionsService.checkPermissions.and.returnValue(createDelayedObservable(permissionsGranted));
    }

    it('passes the route data to the PermissionsService and returns a positive result', fakeAsync(() => {
        const route = setUpRouteSnapshot({ type: TYPE1, permissions: GcmsPermission.READ });
        setUpPermissionsService(true);

        let canActivate: boolean;
        permissionsGuard.canActivate()
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
        permissionsGuard.canActivate()
            .pipe(take(1))
            .subscribe(result => canActivate = result);

        tick();
        expect(canActivate).toBe(false);
        expect(permissionsService.checkPermissions).toHaveBeenCalledTimes(2);
        expect(permissionsService.checkPermissions).toHaveBeenCalledWith((route.data as RouteData).typePermissions);
    }));

    it('shows a notification if the user does not have sufficient permissions', fakeAsync(() => {
        setUpRouteSnapshot({ type: TYPE1, permissions: GcmsPermission.READ });
        setUpPermissionsService(false);

        let canActivate: boolean;
        permissionsGuard.canActivate()
            .pipe(take(1))
            .subscribe(result => canActivate = result);

        tick();
        expect(canActivate).toBe(false);
        expect(i18nNotification.show).toHaveBeenCalledWith({ message: 'common.no_permissions_for_module', type: 'alert' });
    }));

});

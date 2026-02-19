import { ConstructorOf, ObservableStopper, USER_ACTION_PERMISSIONS, USER_ACTION_PERMISSIONS_DEF } from '@admin-ui/common';
import { AddTypePermissionsMap, AppStateService } from '@admin-ui/state';
import { TestAppState, assembleTestAppStateImports } from '@admin-ui/state/utils/test-app-state';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import {
    InstancePermissions,
    InstancePermissionsImpl,
    TypePermissions,
    TypePermissionsImpl,
    UniformInstancePermissions,
    UniformTypePermissions,
} from '@gentics/cms-components';
import {
    AccessControlledType,
    GcmsPermission,
    GcmsPermissionsMap,
    PermissionResponse,
    PermissionsMapCollection,
    ResponseCode,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { deepFreeze } from '@gentics/ui-core/utils/deep-freeze/deep-freeze';
import { ActionType, ofActionDispatched } from '@ngxs/store';
import { Observable, throwError, timer } from 'rxjs';
import { switchMap, takeUntil } from 'rxjs/operators';
import { createDelayedObservable } from '../../../../testing';
import { ErrorHandler } from '../error-handler';
import { MockErrorHandler } from '../error-handler/error-handler.mock';
import { PermissionsService, RequiredInstancePermissions, RequiredPermissions, RequiredTypePermissions } from './permissions.service';

const MOCK_PERM_MAP1: PermissionsMapCollection = {
    permissions: {
        read: true,
        setperm: false,
        update: true,
    },
};
deepFreeze(MOCK_PERM_MAP1);

const MOCK_PERM_MAP2: PermissionsMapCollection = {
    permissions: {
        read: true,
        setperm: false,
        update: false,
        setbundleperm: true,
    },
};
deepFreeze(MOCK_PERM_MAP2);

function mockPermissionsResponse(permissionsCollection: PermissionsMapCollection): Observable<PermissionResponse> {
    const response: PermissionResponse = {
        responseInfo: { responseCode: ResponseCode.OK },
        perm: '1001',
        permissionsMap: permissionsCollection,
    };
    return createDelayedObservable(response);
}

class MockGcmsApi {
    permissions = {
        getPermissionsForType: jasmine.createSpy('getPermissionsForType').and
            .returnValue(mockPermissionsResponse(MOCK_PERM_MAP2)),
        getPermissionsForInstance: jasmine.createSpy('getPermissionsForInstance').and
            .returnValue(mockPermissionsResponse(MOCK_PERM_MAP2)),
    };
}

const INSTANCE_ID = 4711;
const NODE_ID = 2;

describe('PermissionsService', () => {

    let appState: TestAppState;
    let api: MockGcmsApi;
    let permissionsService: PermissionsService;
    let stopper: ObservableStopper;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                assembleTestAppStateImports(),
            ],
            providers: [
                PermissionsService,
                { provide: AppStateService, useClass: TestAppState },
                { provide: GcmsApi, useClass: MockGcmsApi },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: USER_ACTION_PERMISSIONS, useValue: USER_ACTION_PERMISSIONS_DEF },
            ],
        });

        appState = TestBed.get(AppStateService);
        api = TestBed.get(GcmsApi);
        stopper = new ObservableStopper();
        permissionsService = TestBed.get(PermissionsService);

        appState.mockState({
            auth: {
                isLoggedIn: true,
            },
            permissions: {
                types: {
                    [AccessControlledType.INBOX]: MOCK_PERM_MAP1,
                },
            },
        });
    });

    afterEach(() => {
        stopper.stop();
    });

    function assertTypePermissionsCorrect(
        actualPermissions: TypePermissions,
        expectedType: AccessControlledType,
        expectedPermissions: PermissionsMapCollection,
        expectedClass: ConstructorOf<TypePermissions> = TypePermissionsImpl,
    ): void {
        expect(actualPermissions).toBeTruthy();
        expect(actualPermissions instanceof expectedClass).toBe(true);
        expect(actualPermissions.type).toEqual(expectedType);

        Object.keys(expectedPermissions.permissions).forEach((perm: GcmsPermission) => {
            const expectedValue = expectedPermissions.permissions[perm];
            // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
            expect(actualPermissions.hasPermission(perm)).toBe(expectedValue, `Expected permission '${perm}' to be ${expectedValue}`);
        });
    }

    function assertNoPermissions(actualPermissions: TypePermissions, expectedClass: ConstructorOf<UniformTypePermissions | UniformInstancePermissions>): void {
        expect(actualPermissions instanceof expectedClass).toBe(true);
        expect(actualPermissions.hasPermission(GcmsPermission.READ)).toBe(false);
    }

    describe('getTypePermissions()', () => {

        let dispatchedAddActions: AddTypePermissionsMap[];

        beforeEach(() => {
            dispatchedAddActions = [];
            appState.trackActions().pipe(
                ofActionDispatched(AddTypePermissionsMap as ActionType),
                takeUntil(stopper.stopper$),
            ).subscribe(action => dispatchedAddActions.push(action));
        });

        function assertAddActionDispatched(expectedType: AccessControlledType, expectedPermissions: PermissionsMapCollection): void {
            expect(dispatchedAddActions.length).toBe(1, 'No AddTypePermissionsMap action was dispatched.');
            expect(dispatchedAddActions[0].type).toEqual(expectedType);
            expect(dispatchedAddActions[0].permissionsMapCollection).toEqual(expectedPermissions);
        }

        it('fetches uncached permissions and stores them in the AppState', fakeAsync(() => {
            const type = AccessControlledType.MAINTENANCE;
            let permissions: TypePermissions;
            permissionsService.getTypePermissions(type).pipe(
                takeUntil(stopper.stopper$),
            ).subscribe(perms => permissions = perms);

            expect(permissions).toBeUndefined();
            tick();

            expect(api.permissions.getPermissionsForType).toHaveBeenCalledTimes(1);
            expect(api.permissions.getPermissionsForType).toHaveBeenCalledWith(type);

            assertAddActionDispatched(type, MOCK_PERM_MAP2);
            assertTypePermissionsCorrect(permissions, type, MOCK_PERM_MAP2);
        }));

        it('gets cached permissions from the AppState and does not refetch them', () => {
            const type = AccessControlledType.INBOX;
            let permissions: TypePermissions;
            permissionsService.getTypePermissions(type).pipe(
                takeUntil(stopper.stopper$),
            ).subscribe(perms => permissions = perms);

            assertTypePermissionsCorrect(permissions, type, MOCK_PERM_MAP1);
            expect(api.permissions.getPermissionsForType).not.toHaveBeenCalled();
            expect(dispatchedAddActions.length).toBe(0, 'No Add action should be dispatched if permissions are not refetched.');
        });

        it('refetches cached permissions if forceRefresh is true', fakeAsync(() => {
            const type = AccessControlledType.INBOX;
            let permissions: TypePermissions;
            permissionsService.getTypePermissions(type, true).pipe(
                takeUntil(stopper.stopper$),
            ).subscribe(perms => permissions = perms);

            expect(permissions).toBeUndefined('Permissions should have been refetched.');
            tick();

            expect(api.permissions.getPermissionsForType).toHaveBeenCalledTimes(1);
            expect(api.permissions.getPermissionsForType).toHaveBeenCalledWith(type);

            assertAddActionDispatched(type, MOCK_PERM_MAP2);
            assertTypePermissionsCorrect(permissions, type, MOCK_PERM_MAP2);
        }));

        it('emits updates to already delivered permissions', () => {
            const type = AccessControlledType.INBOX;
            let permissions: TypePermissions;
            let emissionCount = 0;

            permissionsService.getTypePermissions(type).pipe(
                takeUntil(stopper.stopper$),
            ).subscribe(perms => {
                permissions = perms;
                ++emissionCount;
            });

            expect(emissionCount).toBe(1);
            assertTypePermissionsCorrect(permissions, type, MOCK_PERM_MAP1);

            appState.dispatch(new AddTypePermissionsMap(type, MOCK_PERM_MAP2));
            expect(emissionCount).toBe(2, 'Existing permissions observable did not emit on AppState update');
            assertTypePermissionsCorrect(permissions, type, MOCK_PERM_MAP2);
        });

        it('emits "no permissions granted" if there is an error during fetching', fakeAsync(() => {
            const errorHandler: MockErrorHandler = TestBed.get(ErrorHandler);
            const type = AccessControlledType.MAINTENANCE;
            let permissions1: TypePermissions;
            let permissions2: TypePermissions;

            // The first attempt to fetch the permissions will fail.
            api.permissions.getPermissionsForType.and.returnValue(
                timer(0).pipe(switchMap(() => throwError('FetchError'))),
            );
            permissionsService.getTypePermissions(type).pipe(
                takeUntil(stopper.stopper$),
            ).subscribe(perms => permissions1 = perms);

            expect(permissions1).toBeUndefined();
            expect(api.permissions.getPermissionsForType).toHaveBeenCalledTimes(1);
            tick();

            assertNoPermissions(permissions1, UniformTypePermissions);
            expect(dispatchedAddActions.length).toBe(0, 'No Add action should be dispatched if there was an error while fetching.');
            expect(errorHandler.catch).toHaveBeenCalledWith('FetchError', { notification: true });

            // Try fetching the permissions again.
            api.permissions.getPermissionsForType.and.returnValue(mockPermissionsResponse(MOCK_PERM_MAP2));
            permissionsService.getTypePermissions(type).pipe(
                takeUntil(stopper.stopper$),
            ).subscribe(perms => permissions2 = perms);

            expect(permissions2).toBeUndefined();
            expect(api.permissions.getPermissionsForType).toHaveBeenCalledTimes(2);
            tick();

            assertAddActionDispatched(type, MOCK_PERM_MAP2);
            assertTypePermissionsCorrect(permissions1, type, MOCK_PERM_MAP2);
            assertTypePermissionsCorrect(permissions2, type, MOCK_PERM_MAP2);
        }));

    });

    describe('getInstancePermissions()', () => {

        function assertInstancePermissionsCorrect(
            actualPermissions: InstancePermissions,
            expectedType: AccessControlledType,
            expectedPermissions: PermissionsMapCollection,
            expectedInstanceId: number,
            expectedNodeId?: number,
        ): void {
            assertTypePermissionsCorrect(actualPermissions, expectedType, expectedPermissions, InstancePermissionsImpl);
            expect(actualPermissions.instanceId).toBe(expectedInstanceId);
            expect(actualPermissions.nodeId).toBe(expectedNodeId);
        }

        function testFetchInstancePermissions(type: AccessControlledType, instanceId: number, nodeId?: number): void {
            let permissions: InstancePermissions;
            permissionsService.getInstancePermissions(type, instanceId, nodeId).pipe(
                takeUntil(stopper.stopper$),
            ).subscribe(perms => permissions = perms);

            tick();

            expect(api.permissions.getPermissionsForInstance).toHaveBeenCalledTimes(1);
            expect(api.permissions.getPermissionsForInstance).toHaveBeenCalledWith(type, instanceId, nodeId);
            assertInstancePermissionsCorrect(permissions, type, MOCK_PERM_MAP2, instanceId, nodeId);
        }

        it('fetches instances permissions without a nodeId', fakeAsync(() => {
            testFetchInstancePermissions(AccessControlledType.MAINTENANCE, INSTANCE_ID);
        }));

        it('fetches instances permissions with a nodeId', fakeAsync(() => {
            testFetchInstancePermissions(AccessControlledType.MAINTENANCE, INSTANCE_ID, NODE_ID);
        }));

        it('emits "no permissions granted" if there is an error during fetching', fakeAsync(() => {
            const errorHandler: MockErrorHandler = TestBed.get(ErrorHandler);
            const type = AccessControlledType.MAINTENANCE;
            let permissions: InstancePermissions;

            // The first attempt to fetch the permissions will fail.
            api.permissions.getPermissionsForInstance.and.returnValue(
                timer(0).pipe(switchMap(() => throwError('FetchError'))),
            );
            permissionsService.getInstancePermissions(type, INSTANCE_ID, NODE_ID).pipe(
                takeUntil(stopper.stopper$),
            ).subscribe(perms => permissions = perms);

            expect(api.permissions.getPermissionsForInstance).toHaveBeenCalledTimes(1);
            tick();
            assertNoPermissions(permissions, UniformInstancePermissions);
            expect(errorHandler.catch).toHaveBeenCalledWith('FetchError', { notification: true });
        }));

    });

    describe('getPermissions()', () => {

        const TYPE1 = AccessControlledType.ADMIN;
        const TYPE2 = AccessControlledType.NODE;

        let getTypePermissionsSpy: jasmine.Spy;
        let getInstancePermissionsSpy: jasmine.Spy;
        let typePermissionsResult: any;
        let instancePermissionsResult: any;

        beforeEach(() => {
            typePermissionsResult = { type: 'Result of getTypePermissions()' };
            instancePermissionsResult = { type: 'Result of getInstancePermissions()' };
            getTypePermissionsSpy = spyOn(permissionsService, 'getTypePermissions').and.returnValue(typePermissionsResult);
            getInstancePermissionsSpy = spyOn(permissionsService, 'getInstancePermissions').and.returnValue(instancePermissionsResult);
        });

        it('delegates to getTypePermissions() if no instanceId is provided', () => {
            const result = permissionsService.getPermissions(TYPE1);
            expect(getTypePermissionsSpy).toHaveBeenCalledTimes(1);
            expect(getTypePermissionsSpy).toHaveBeenCalledWith(TYPE1, false);
            expect(result).toBe(typePermissionsResult);
        });

        it('delegates to getTypePermissions() if instanceId is undefined', () => {
            const result = permissionsService.getPermissions(TYPE1, undefined);
            expect(getTypePermissionsSpy).toHaveBeenCalledTimes(1);
            expect(getTypePermissionsSpy).toHaveBeenCalledWith(TYPE1, false);
            expect(result).toBe(typePermissionsResult);
        });

        it('delegates to getTypePermissions() if instanceId is null', () => {
            const result = permissionsService.getPermissions(TYPE1, null);
            expect(getTypePermissionsSpy).toHaveBeenCalledTimes(1);
            expect(getTypePermissionsSpy).toHaveBeenCalledWith(TYPE1, false);
            expect(result).toBe(typePermissionsResult);
        });

        it('delegates to getInstancePermissions() if an instanceId, but no nodeId is provided', () => {
            const result = permissionsService.getPermissions(TYPE2, INSTANCE_ID);
            expect(getTypePermissionsSpy).not.toHaveBeenCalled();
            expect(getInstancePermissionsSpy).toHaveBeenCalledTimes(1);
            expect(getInstancePermissionsSpy).toHaveBeenCalledWith(TYPE2, INSTANCE_ID, undefined, false);
            expect(result).toBe(instancePermissionsResult);
        });

        it('delegates to getInstancePermissions() if an instanceId and a nodeId are provided', () => {
            const result = permissionsService.getPermissions(TYPE2, INSTANCE_ID, NODE_ID);
            expect(getTypePermissionsSpy).not.toHaveBeenCalled();
            expect(getInstancePermissionsSpy).toHaveBeenCalledTimes(1);
            expect(getInstancePermissionsSpy).toHaveBeenCalledWith(TYPE2, INSTANCE_ID, NODE_ID, false);
            expect(result).toBe(instancePermissionsResult);
        });

        it('delegates to getInstancePermissions() if an instanceId is 0', () => {
            const result = permissionsService.getPermissions(TYPE2, 0);
            expect(getTypePermissionsSpy).not.toHaveBeenCalled();
            expect(getInstancePermissionsSpy).toHaveBeenCalledTimes(1);
            expect(getInstancePermissionsSpy).toHaveBeenCalledWith(TYPE2, 0, undefined, false);
            expect(result).toBe(instancePermissionsResult);
        });

    });

    describe('checkPermissions()', () => {

        const TYPE1 = AccessControlledType.MAINTENANCE;
        const TYPE2 = AccessControlledType.USER_ADMIN;

        let getTypePermissionsSpy: jasmine.Spy;
        let getInstancePermissionsSpy: jasmine.Spy;

        beforeEach(() => {
            getTypePermissionsSpy = spyOn(permissionsService, 'getTypePermissions');
            getInstancePermissionsSpy = spyOn(permissionsService, 'getInstancePermissions');
        });

        function setUpTypePermissions(mockedPermissions: Partial<Record<AccessControlledType, Partial<GcmsPermissionsMap>>>): void {
            getTypePermissionsSpy.and.callFake(
                type => createDelayedObservable(new TypePermissionsImpl(type, { permissions: mockedPermissions[type] })),
            );
        }

        function setUpInstancePermissions(
            mockedPermissions: { type: AccessControlledType, instanceId: number, nodeId?: number, permissions: Partial<GcmsPermissionsMap> }[],
        ): void {
            getInstancePermissionsSpy.and.callFake((type, instanceId, nodeId) => {
                const permissions = mockedPermissions.find(
                    mockedPerms => mockedPerms.type === type && mockedPerms.instanceId === instanceId && mockedPerms.nodeId === nodeId,
                );
                return createDelayedObservable(new InstancePermissionsImpl(type, { permissions: permissions.permissions }, instanceId, nodeId));
            });
        }

        describe('single type', () => {

            it('works for a single required permission, which has been granted', fakeAsync(() => {
                const requiredPerms: RequiredTypePermissions = { type: TYPE1, permissions: GcmsPermission.READ };
                setUpTypePermissions({
                    [TYPE1]: { read: true },
                });

                let permsGranted: boolean;
                permissionsService.checkPermissions(requiredPerms).pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe(result => permsGranted = result);

                tick();
                expect(permsGranted).toBe(true);
                expect(getTypePermissionsSpy).toHaveBeenCalledTimes(1);
                expect(getTypePermissionsSpy).toHaveBeenCalledWith(TYPE1);
            }));

            it('works for a single required permission, which has been denied', fakeAsync(() => {
                const requiredPerms: RequiredTypePermissions = { type: TYPE1, permissions: GcmsPermission.READ };
                setUpTypePermissions({
                    [TYPE1]: { read: false },
                });

                let permsGranted: boolean;
                permissionsService.checkPermissions(requiredPerms).pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe(result => permsGranted = result);

                tick();
                expect(permsGranted).toBe(false);
            }));

            it('works for multiple required permissions, which have all been granted', fakeAsync(() => {
                const requiredPerms: RequiredTypePermissions = {
                    type: TYPE1,
                    permissions: [GcmsPermission.READ, GcmsPermission.BUILD_EXPORT, GcmsPermission.CREATE_USER],
                };
                setUpTypePermissions({
                    [TYPE1]: { read: true, buildexport: true, createuser: true },
                });

                let permsGranted: boolean;
                permissionsService.checkPermissions(requiredPerms).pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe(result => permsGranted = result);

                tick();
                expect(permsGranted).toBe(true);
                expect(getTypePermissionsSpy).toHaveBeenCalledTimes(1);
                expect(getTypePermissionsSpy).toHaveBeenCalledWith(TYPE1);
            }));

            it('works for multiple required permissions, where one has been denied', fakeAsync(() => {
                const requiredPerms: RequiredTypePermissions = {
                    type: TYPE1,
                    permissions: [GcmsPermission.READ, GcmsPermission.BUILD_EXPORT, GcmsPermission.CREATE_USER],
                };
                setUpTypePermissions({
                    [TYPE1]: { read: true, buildexport: false, createuser: true },
                });

                let permsGranted: boolean;
                permissionsService.checkPermissions(requiredPerms).pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe(result => permsGranted = result);

                tick();
                expect(permsGranted).toBe(false);
            }));

        });

        describe('multiple types', () => {

            it('works for one required permission per type, which have all been granted', fakeAsync(() => {
                const requiredPerms: RequiredTypePermissions[] = [
                    { type: TYPE1, permissions: GcmsPermission.READ },
                    { type: TYPE2, permissions: GcmsPermission.EDIT_IMPORT },
                ];
                setUpTypePermissions({
                    [TYPE1]: { read: true },
                    [TYPE2]: { editimport: true },
                });

                let permsGranted: boolean;
                permissionsService.checkPermissions(requiredPerms).pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe(result => permsGranted = result);

                tick();
                expect(permsGranted).toBe(true);
                expect(getTypePermissionsSpy).toHaveBeenCalledTimes(2);
                expect(getTypePermissionsSpy.calls.argsFor(0)).toEqual([TYPE1]);
                expect(getTypePermissionsSpy.calls.argsFor(1)).toEqual([TYPE2]);
            }));

            it('works for one required permission per type, where one has been denied', fakeAsync(() => {
                const requiredPerms: RequiredTypePermissions[] = [
                    { type: TYPE1, permissions: GcmsPermission.READ },
                    { type: TYPE2, permissions: GcmsPermission.EDIT_IMPORT },
                ];
                setUpTypePermissions({
                    [TYPE1]: { read: true },
                    [TYPE2]: { editimport: false },
                });

                let permsGranted: boolean;
                permissionsService.checkPermissions(requiredPerms).pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe(result => permsGranted = result);

                tick();
                expect(permsGranted).toBe(false);
            }));

            it('works for multiple required permissions per type, which have all been granted', fakeAsync(() => {
                const requiredPerms: RequiredTypePermissions[] = [
                    { type: TYPE1, permissions: [GcmsPermission.READ, GcmsPermission.BUILD_EXPORT, GcmsPermission.CREATE_USER] },
                    { type: TYPE2, permissions: [GcmsPermission.READ, GcmsPermission.EDIT_IMPORT, GcmsPermission.IMPORT_ITEMS] },
                ];
                setUpTypePermissions({
                    [TYPE1]: { read: true, buildexport: true, createuser: true },
                    [TYPE2]: { read: true, editimport: true, importitems: true },
                });

                let permsGranted: boolean;
                permissionsService.checkPermissions(requiredPerms).pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe(result => permsGranted = result);

                tick();
                expect(permsGranted).toBe(true);
                expect(getTypePermissionsSpy).toHaveBeenCalledTimes(2);
                expect(getTypePermissionsSpy.calls.argsFor(0)).toEqual([TYPE1]);
                expect(getTypePermissionsSpy.calls.argsFor(1)).toEqual([TYPE2]);
            }));

            it('works for multiple required permissions per type, where one has been denied', fakeAsync(() => {
                const requiredPerms: RequiredTypePermissions[] = [
                    { type: TYPE1, permissions: [GcmsPermission.READ, GcmsPermission.BUILD_EXPORT, GcmsPermission.CREATE_USER] },
                    { type: TYPE2, permissions: [GcmsPermission.READ, GcmsPermission.EDIT_IMPORT, GcmsPermission.IMPORT_ITEMS] },
                ];
                setUpTypePermissions({
                    [TYPE1]: { read: true, buildexport: true, createuser: true },
                    [TYPE2]: { read: true, editimport: false, importitems: true },
                });

                let permsGranted: boolean;
                permissionsService.checkPermissions(requiredPerms).pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe(result => permsGranted = result);

                tick();
                expect(permsGranted).toBe(false);
            }));

        });

        describe('instance permissions', () => {

            it('checking an instance permission works', fakeAsync(() => {
                const requiredPerms: RequiredInstancePermissions = { type: TYPE1, instanceId: INSTANCE_ID, nodeId: NODE_ID, permissions: GcmsPermission.READ };
                setUpInstancePermissions([
                    { type: TYPE1, instanceId: INSTANCE_ID, nodeId: NODE_ID, permissions: { read: true } },
                ]);

                let permsGranted: boolean;
                permissionsService.checkPermissions(requiredPerms).pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe(result => permsGranted = result);

                tick();
                expect(permsGranted).toBe(true);
                expect(getInstancePermissionsSpy).toHaveBeenCalledTimes(1);
                expect(getInstancePermissionsSpy).toHaveBeenCalledWith(TYPE1, INSTANCE_ID, NODE_ID);
            }));

            it('checking a combination of type and instance permissions works', fakeAsync(() => {
                const requiredPerms: RequiredPermissions[] = [
                    { type: TYPE1, instanceId: INSTANCE_ID, nodeId: NODE_ID, permissions: [GcmsPermission.READ, GcmsPermission.SET_PERMISSION] },
                    { type: TYPE2, permissions: GcmsPermission.READ },
                ];
                setUpInstancePermissions([
                    { type: TYPE1, instanceId: INSTANCE_ID, nodeId: NODE_ID, permissions: { read: true, setperm: true } },
                ]);
                setUpTypePermissions({
                    [TYPE2]: { read: true },
                });

                let permsGranted: boolean;
                permissionsService.checkPermissions(requiredPerms).pipe(
                    takeUntil(stopper.stopper$),
                ).subscribe(result => permsGranted = result);

                tick();
                expect(permsGranted).toBe(true);
                expect(getInstancePermissionsSpy).toHaveBeenCalledTimes(1);
                expect(getInstancePermissionsSpy).toHaveBeenCalledWith(TYPE1, INSTANCE_ID, NODE_ID);
                expect(getTypePermissionsSpy).toHaveBeenCalledTimes(1);
                expect(getTypePermissionsSpy).toHaveBeenCalledWith(TYPE2);
            }));

        });

        it('the resulting observable emits again if permissions change in the AppState', fakeAsync(() => {
            const requiredPerms: RequiredPermissions[] = [
                { type: TYPE1, instanceId: INSTANCE_ID, nodeId: NODE_ID, permissions: [GcmsPermission.READ, GcmsPermission.SET_PERMISSION] },
                { type: TYPE2, permissions: GcmsPermission.READ },
            ];
            setUpInstancePermissions([
                { type: TYPE1, instanceId: INSTANCE_ID, nodeId: NODE_ID, permissions: { read: true, setperm: true } },
            ]);
            getTypePermissionsSpy.and.callThrough();
            appState.mockState({
                permissions: {
                    types: {
                        [TYPE2]: {
                            permissions: { read: false },
                        },
                    },
                },
            });

            let permsGranted: boolean;
            let emissionsCount = 0;
            permissionsService.checkPermissions(requiredPerms).pipe(
                takeUntil(stopper.stopper$),
            ).subscribe(result => {
                permsGranted = result;
                ++emissionsCount;
            });

            tick();
            expect(permsGranted).toBe(false);
            expect(emissionsCount).toBe(1);

            appState.mockState({
                permissions: {
                    types: {
                        [TYPE2]: {
                            permissions: { read: true },
                        },
                    },
                },
            });
            expect(permsGranted).toBe(true);
            expect(emissionsCount).toBe(2);
        }));

    });

});

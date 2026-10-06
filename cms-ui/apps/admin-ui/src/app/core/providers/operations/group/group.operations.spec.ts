import { createDelayedError, createDelayedObservable, MockErrorHandler, tickAndGetEmission } from '@admin-ui/testing';
import { fakeAsync, TestBed } from '@angular/core/testing';
import { I18nNotificationService, I18nService, TranslatedNotificationOptions } from '@gentics/cms-components';
import { MockI18nNotificationService, MockI18nService } from '@gentics/cms-components/testing';
import {
    AccessControlledType,
    GcmsNormalizer,
    GcmsPermission,
    Group,
    GroupCreateRequest,
    GroupPermissionsListOptions,
    GroupSetPermissionsRequest,
    GroupUpdateRequest,
    GroupUserCreateRequest,
    GroupUserCreateResponse,
    IndexById,
    IS_NORMALIZED,
    Normalized,
    NormalizedEntityStore,
    PagingSortOrder,
    PermissionInfo,
    PermissionsSet,
    Raw,
    RecursivePartial,
    ResponseCode,
    User,
    UserListOptions,
} from '@gentics/cms-models';
import { getExampleEntityStore, getExampleFolderData } from '@gentics/cms-models/testing';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { cloneDeep as _cloneDeep } from 'lodash-es';
import { Observable, of as observableOf } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ActivityManagerService, EntityManagerService, ErrorHandler, PermissionsService } from '../..';
import { ObservableStopper } from '../../../../common';
import { InterfaceOf } from '../../../../common/utils/util-types/util-types';
import { AppStateService } from '../../../../state';
import { assembleTestAppStateImports, TestAppState } from '../../../../state/utils/test-app-state';
import { MockEntityManagerService } from '../../entity-manager/entity-manager.service.mock';
import { GroupOperations } from './group.operations';

function convertNormalizedToRawArray(
    type: 'user' | 'group',
    normalizedEntities: IndexById<User<Normalized> | Group<Normalized>>,
    state: Partial<NormalizedEntityStore>,
): (User<Raw> | Group<Raw>)[] {
    const normalizer = new GcmsNormalizer();
    return Object.keys(normalizedEntities).map((key) => normalizer.denormalize(type, normalizedEntities[key], state));
}

function createGroups(count: number, startId: number = 1): Group<Raw>[] {
    const groups: Group<Raw>[] = [];
    for (let i = 0; i < count; ++i) {
        const id = startId + i;
        groups.push({
            id,
            name: `Group ${id}`,
            description: `Group ${id} Description`,
        });
    }
    return groups;
}

function createUsers(count: number, startId: number = 1): User<Raw>[] {
    const users: User<Raw>[] = [];
    for (let i = 0; i < count; ++i) {
        const id = startId + i;
        users.push({
            id,
            login: `user${id}`,
            description: '',
            email: '',
            firstName: 'User ',
            lastName: `Test ${id}`,
        });
    }
    return users;
}

function createPermissionInfo(type: GcmsPermission): PermissionInfo {
    return {
        type,
        label: type.toString(),
        value: true,
        editable: true,
        category: '',
    };
}

function createSpy(name: string): jasmine.Spy {
    return jasmine.createSpy(name).and.stub();
}

class MockClient implements RecursivePartial<InterfaceOf<GCMSRestClientService>> {
    group = {
        tree: createSpy('tree'),
        create: createSpy('create'),
        move: createSpy('move'),
        delete: createSpy('delete'),
        update: createSpy('update'),
        listUsers: createSpy('listUsers'),
        createUser: createSpy('createUser'),
        assignUser: createSpy('assignUser'),
        unassignUser: createSpy('unassignUser'),
        listPermissions: createSpy('listPermissions'),
        getPermission: createSpy('getPermission'),
        getInstancePermission: createSpy('getInstancePermission'),
        setPermission: createSpy('setPermission'),
        setInstancePermission: createSpy('setInstancePermission'),
    };
}

class MockPermissionsService implements Partial<PermissionsService> {

}

const MOCKED_GROUPS_COUNT = 10;
const TEST_GROUP_ID = 2;
const TEST_GROUP_NAME = 'Test Group';

describe('GroupOperations', () => {

    let client: MockClient;
    let appState: TestAppState;
    let entityManager: MockEntityManagerService;
    let errorHandler: MockErrorHandler;
    let groupOperations: GroupOperations;
    let normalizer: GcmsNormalizer;
    let notification: MockI18nNotificationService;
    let stopper: ObservableStopper;

    let addEntitySpy: jasmine.Spy;
    let addEntitiesSpy: jasmine.Spy;

    let mockUserRaw: User<Raw>;
    let mockGroups: Group<Raw>[];

    beforeEach(() => {
        stopper = new ObservableStopper();

        TestBed.configureTestingModule({
            imports: [
                assembleTestAppStateImports(),
            ],
            providers: [
                ActivityManagerService,
                EntityManagerService,
                GroupOperations,
                { provide: PermissionsService, useClass: MockPermissionsService },
                { provide: AppStateService, useClass: TestAppState },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: GCMSRestClientService, useClass: MockClient },
                { provide: I18nNotificationService, useClass: MockI18nNotificationService },
                { provide: I18nService, useClass: MockI18nService },
            ],
        });

        client = TestBed.inject(GCMSRestClientService) as any;
        appState = TestBed.inject(AppStateService) as any;
        entityManager = TestBed.inject(EntityManagerService) as any;
        errorHandler = TestBed.inject(ErrorHandler) as any;
        groupOperations = TestBed.inject(GroupOperations);
        normalizer = new GcmsNormalizer();
        notification = TestBed.inject(I18nNotificationService) as any;

        spyOn(notification, 'show').and.callThrough();

        addEntitySpy = spyOn(entityManager, 'addEntity').and.callThrough();
        addEntitiesSpy = spyOn(entityManager, 'addEntities').and.callThrough();

        mockUserRaw = getExampleFolderData({ id: 1, userId: 1 }).editor;
        mockGroups = createGroups(MOCKED_GROUPS_COUNT);

        appState.mockState({
            entity: {
                group: getExampleEntityStore().group,
            },
        });
    });

    afterEach(() => {
        stopper.stop();
    });

    function mockStateWithChildGroups(parentGroupId: number, ...childGroupIds: number[]): void {
        const parentGroup: Group<Normalized> = {
            id: parentGroupId,
            name: 'Parent Group',
            description: 'Parent Group',
            children: [],
            [IS_NORMALIZED]: true,
        };
        const groupEntities: IndexById<Group<Normalized>> = {
            [parentGroupId]: parentGroup,
        };

        childGroupIds.forEach((id) => {
            groupEntities[id] = {
                id,
                name: `Child ${id}`,
                description: 'Child',
                [IS_NORMALIZED]: true,
            };
            parentGroup.children.push(id);
        });

        appState.mockState({
            entity: {
                group: {
                    ...groupEntities,
                },
            },
        });
    }

    function mockStateWithUsersAndGroups(parentGroups: Group<Raw>[], usersCount: number): User<Raw>[] {
        const mockUsersRaw = createUsers(usersCount);
        mockUsersRaw.forEach((user) => user.groups = parentGroups);
        const normalized = normalizer.normalize('user', mockUsersRaw);

        appState.mockState({
            entity: normalized.entities,
        });
        return mockUsersRaw;
    }

    function assertGroupsAreInState(ids: number[]): void {
        ids.forEach((id) => expect(appState.now.entity.group[id]).toBeTruthy);
    }

    function assertErrorHandlingWorks(operation: () => Observable<any>, apiSpy: jasmine.Spy): void {
        const error = new Error('Expected Error');
        apiSpy.and.returnValue(createDelayedError(error));
        errorHandler.assertNotifyAndRethrowIsCalled(operation(), error);
    }

    function assertNotificationWasShown(notificationOptions: Partial<TranslatedNotificationOptions>): void {
        expect(notification.show).toHaveBeenCalledWith(jasmine.objectContaining(notificationOptions));
    }

    describe('getGroups()', () => {

        it('calls the correct API endpoint and adds the group to the entity state', fakeAsync(() => {
            client.group.tree.and.returnValue(createDelayedObservable({ groups: mockGroups }));
            const result$ = groupOperations.getAll();

            const result = tickAndGetEmission(result$);
            expect(client.group.tree).toHaveBeenCalledTimes(1);
            expect(result).toBe(mockGroups);
            expect(entityManager.addEntities).toHaveBeenCalledTimes(1);
            expect(entityManager.addEntities).toHaveBeenCalledWith('group', mockGroups);
        }));

        it('handles errors properly', fakeAsync(() => {
            assertErrorHandlingWorks(() => groupOperations.getAll(), client.group.tree);
        }));

    });

    describe('createSubgroup()', () => {

        const NEW_GROUP_ID = 10;
        const PARENT_GROUP_ID = 2;
        let createReq: GroupCreateRequest;
        let newGroup: Group<Raw>;

        beforeEach(() => {
            createReq = {
                name: 'New Group',
                description: 'Description of New Group',
            };
            newGroup = {
                ...createReq,
                id: NEW_GROUP_ID,
            };
        });

        function runCreateSubgroupTest(expectedGroupEntityState: IndexById<Group<Normalized>>): void {
            client.group.create.and.returnValue(createDelayedObservable({ group: newGroup }));

            const result$ = groupOperations.createSubgroup(PARENT_GROUP_ID, createReq);
            const result = tickAndGetEmission(result$);

            expect(result).toBe(newGroup);
            expect(entityManager.addEntity).toHaveBeenCalledTimes(1);
            expect(entityManager.addEntity).toHaveBeenCalledWith('group', newGroup);

            // Check the group entity state to make sure that the parent group has been adequately modified.
            expect(appState.now.entity.group).toEqual(expectedGroupEntityState);

            assertNotificationWasShown({ message: 'shared.item_created' });
        }

        it('calls the correct API endpoint and updates the entity state if the parent did not have children yet', fakeAsync(() => {
            // The group entity state should contain the new group and its parent group should reference it.
            const expectedGroupEntityState = _cloneDeep(appState.now.entity.group);
            expectedGroupEntityState[NEW_GROUP_ID] = normalizer.normalize('group', newGroup).result;
            expectedGroupEntityState[PARENT_GROUP_ID].children = [NEW_GROUP_ID];

            runCreateSubgroupTest(expectedGroupEntityState);
        }));

        it('calls the correct API endpoint and updates the entity state if the parent already had children', fakeAsync(() => {
            mockStateWithChildGroups(PARENT_GROUP_ID, NEW_GROUP_ID - 1);

            // The group entity state should contain the new group and its parent group should reference it.
            const expectedGroupEntityState = _cloneDeep(appState.now.entity.group);
            expectedGroupEntityState[NEW_GROUP_ID] = normalizer.normalize('group', newGroup).result;
            expectedGroupEntityState[PARENT_GROUP_ID].children.push(NEW_GROUP_ID);

            runCreateSubgroupTest(expectedGroupEntityState);
        }));

        it('handles errors properly', fakeAsync(() => {
            assertErrorHandlingWorks(() => groupOperations.createSubgroup(PARENT_GROUP_ID, createReq), client.group.create);
        }));

    });

    describe('moveSubgroup()', () => {

        it('calls the correct endpoint and refreshes the groups', fakeAsync(() => {
            const movedGroupId = 4;
            const parentGroupId = 2;
            const expectedGroup = mockGroups[movedGroupId];
            client.group.move.and.returnValue(createDelayedObservable({ group: expectedGroup }));
            const getGroupsSpy = spyOn(groupOperations, 'getAll').and.returnValue(createDelayedObservable(null));

            const result$ = groupOperations.moveSubgroup(movedGroupId, parentGroupId);
            const result = tickAndGetEmission(result$);

            expect(result).toBe(expectedGroup);
            expect(client.group.move).toHaveBeenCalledTimes(1);
            expect(client.group.move).toHaveBeenCalledWith(movedGroupId, parentGroupId);
            expect(getGroupsSpy.calls.count()).toBe(1, 'Groups were not refreshed');
            assertNotificationWasShown({ message: 'shared.item_moved' });
        }));

        it('handles errors properly', fakeAsync(() => {
            assertErrorHandlingWorks(() => groupOperations.moveSubgroup(2, 1), client.group.move);
        }));

    });

    describe('deleteGroup()', () => {

        const PARENT_GROUP_A = 2;
        const PARENT_GROUP_B = 3;
        const CHILD_A1 = 4;
        const CHILD_A2 = 5;
        const CHILD_B1 = 6;
        const CHILD_B2 = 7;

        it('calls the correct API and removes the group from the AppState', fakeAsync(() => {
            mockStateWithChildGroups(PARENT_GROUP_A, CHILD_A1, CHILD_A2);
            mockStateWithChildGroups(PARENT_GROUP_B, CHILD_B1, CHILD_B2);
            assertGroupsAreInState([PARENT_GROUP_A, PARENT_GROUP_B, CHILD_A1, CHILD_A2, CHILD_B1, CHILD_B2]);

            const expectedGroupEntities = _cloneDeep(appState.now.entity.group);
            expectedGroupEntities[PARENT_GROUP_A].children = [CHILD_A2];
            delete expectedGroupEntities[CHILD_A1];

            client.group.delete.and.returnValue(createDelayedObservable(null));

            const result$ = groupOperations.delete(CHILD_A1);
            tickAndGetEmission(result$);

            expect(client.group.delete).toHaveBeenCalledTimes(1);
            expect(client.group.delete).toHaveBeenCalledWith(CHILD_A1);
            expect(appState.now.entity.group).toEqual(expectedGroupEntities);
            assertNotificationWasShown({
                type: 'success',
                message: 'shared.item_singular_deleted',
                translationParams: { name: 'Child 4' },
            });
        }));

        it('handles errors properly', fakeAsync(() => {
            assertErrorHandlingWorks(() => groupOperations.delete(CHILD_A1), client.group.delete);
        }));

    });

    describe('updateGroup()', () => {

        let update: GroupUpdateRequest;
        let updatedGroup: Group<Raw>;

        beforeEach(() => {
            update = {
                name: 'new name',
                description: 'new description',
            };
            updatedGroup = _cloneDeep(appState.now.entity.group[TEST_GROUP_ID]) as any;
            delete updatedGroup[IS_NORMALIZED];
        });

        it('calls the correct API and updates the entity state', fakeAsync(() => {
            expect(updatedGroup).toBeTruthy();
            client.group.update.and.returnValue(createDelayedObservable({ group: updatedGroup }));

            const result$ = groupOperations.update(TEST_GROUP_ID, update);
            const result = tickAndGetEmission(result$);

            expect(result).toBe(updatedGroup);
            expect(client.group.update).toHaveBeenCalledTimes(1);
            expect(client.group.update).toHaveBeenCalledWith(TEST_GROUP_ID, update);
            expect(entityManager.addEntity).toHaveBeenCalledWith('group', updatedGroup);
            assertNotificationWasShown({ message: 'shared.item_updated' });
        }));

        it('handles errors properly', fakeAsync(() => {
            assertErrorHandlingWorks(() => groupOperations.update(TEST_GROUP_ID, update), client.group.update);
        }));

    });

    describe('getGroupUsers()', () => {

        it('calls the correct API and adds users to the entity state', fakeAsync(() => {
            const options: UserListOptions = { sort: { attribute: 'lastName', sortOrder: PagingSortOrder.Asc } };
            const expectedUsers = createUsers(10);
            client.group.listUsers.and.returnValue(createDelayedObservable({ items: expectedUsers }));

            const result$ = groupOperations.getGroupUsers(TEST_GROUP_ID, options);
            const result = tickAndGetEmission(result$);

            expect(result).toBe(expectedUsers);
            expect(client.group.listUsers).toHaveBeenCalledTimes(1);
            expect(client.group.listUsers.calls.argsFor(0)[0]).toBe(TEST_GROUP_ID);
            // Make sure that the options object has been passed on directly.
            expect(client.group.listUsers.calls.argsFor(0)[1]).toBe(options);
            expect(entityManager.addEntities).toHaveBeenCalledWith('user', result);
        }));

        it('handles errors properly', fakeAsync(() => {
            assertErrorHandlingWorks(() => groupOperations.getGroupUsers(TEST_GROUP_ID), client.group.listUsers);
        }));

    });

    describe('createUser()', () => {

        it('calls the correct API and updates the entity state', () => {
            // prepare response payload
            const groupsFromState = appState.now.entity.group;
            // select a group where the user will be created
            const groupsFromStateRaw = convertNormalizedToRawArray('group', groupsFromState, appState.now.entity) as Group<Raw>[];
            // remove group children
            const creatingUserGroup: Group<Raw> = Object.keys(groupsFromStateRaw[1])
                .filter((key) => key !== 'children')
                .reduce((result, current) => ({ ...result, [current]: groupsFromStateRaw[1][current] }), {}) as Group<Raw>;
            mockUserRaw.groups = [creatingUserGroup];
            const mockResponse: GroupUserCreateResponse = {
                responseInfo: { responseCode: ResponseCode.OK },
                user: mockUserRaw,
                messages: [],
            };
            // prepare request payload
            const payload: any = Object.keys(mockUserRaw)
                .filter((key) => key !== 'id')
                .filter((key) => key !== 'groups')
                .reduce((result, current) => ({ ...result, [current]: mockUserRaw[current] }), {});
            payload.password = 'testPassword';
            const USER_CREATE_PAYLOAD: GroupUserCreateRequest = payload;
            client.group.createUser.and.returnValue(observableOf(mockResponse));

            const response$ = groupOperations.createUser(creatingUserGroup.id, USER_CREATE_PAYLOAD);
            expect(client.group.createUser).toHaveBeenCalledWith(creatingUserGroup.id, USER_CREATE_PAYLOAD);

            // check if correct response
            let loadedUser: User<Raw>;
            response$.pipe(
                takeUntil(stopper.stopper$),
            ).subscribe((user) => loadedUser = user);
            expect(loadedUser).toEqual(mockUserRaw);

            // check if mock user has been put into store
            expect(entityManager.addEntity).toHaveBeenCalledTimes(1);
            expect(entityManager.addEntity).toHaveBeenCalledWith('user', mockUserRaw);

            assertNotificationWasShown({ message: 'shared.item_created' });
        });

        it('handles errors properly', fakeAsync(() => {
            assertErrorHandlingWorks(() => groupOperations.createUser(2, mockUserRaw[0]), client.group.createUser);
        }));

    });

    describe('addUserToGroup()', () => {

        it('calls the correct API and adds the user to the user to the entity state', fakeAsync(() => {
            const expectedUser = createUsers(1)[0];
            client.group.assignUser.and.returnValue(createDelayedObservable({ user: expectedUser }));

            const result$ = groupOperations.addUserToGroup(TEST_GROUP_ID, expectedUser.id);
            const result = tickAndGetEmission(result$);

            expect(result).toBe(expectedUser);
            expect(client.group.assignUser).toHaveBeenCalledTimes(1);
            expect(client.group.assignUser).toHaveBeenCalledWith(TEST_GROUP_ID, expectedUser.id);
            expect(entityManager.addEntity).toHaveBeenCalledWith('user', result);

            assertNotificationWasShown({ message: 'group.user_added_to_group' });
        }));

        it('handles errors properly', fakeAsync(() => {
            assertErrorHandlingWorks(() => groupOperations.addUserToGroup(TEST_GROUP_ID, 4711), client.group.assignUser);
        }));

    });

    describe('removeUserFromGroup()', () => {

        it('calls the correct API and updates the entity state', fakeAsync(() => {
            const users = mockStateWithUsersAndGroups(mockGroups, 10);
            client.group.unassignUser.and.returnValue(createDelayedObservable({}));

            // Set up the expected state after removing the last user from the first group.
            const removeFromGroupId = mockGroups[0].id;
            const expectedEntityState = _cloneDeep(appState.now.entity);
            const targetUser = expectedEntityState.user[users[users.length - 1].id];
            targetUser.groups = targetUser.groups.filter((id) => id !== removeFromGroupId);
            expect(expectedEntityState).not.toEqual(appState.now.entity);

            const result$ = groupOperations.removeUserFromGroup(removeFromGroupId, targetUser.id);
            tickAndGetEmission(result$);

            expect(client.group.unassignUser).toHaveBeenCalledTimes(1);
            expect(client.group.unassignUser).toHaveBeenCalledWith(removeFromGroupId, targetUser.id);
            expect(expectedEntityState).toEqual(appState.now.entity);

            assertNotificationWasShown({ message: 'group.user_removed_from_group' });
        }));

        it('handles errors properly', fakeAsync(() => {
            assertErrorHandlingWorks(() => groupOperations.removeUserFromGroup(TEST_GROUP_ID, 4711), client.group.unassignUser);
        }));

    });

    describe('getPermissionsSets()', () => {

        it('makes the correct API call and returns the result', fakeAsync(() => {
            const expectedResult: RecursivePartial<PermissionsSet>[] = [
                {
                    type: AccessControlledType.ADMIN,
                    perms: [createPermissionInfo(GcmsPermission.READ)],
                },
                {
                    type: AccessControlledType.AUTO_UPDATE,
                    perms: [createPermissionInfo(GcmsPermission.READ)],
                },
            ];
            client.group.listPermissions.and.returnValue(createDelayedObservable({ items: expectedResult }));

            const options: GroupPermissionsListOptions = { parentId: 4711 };
            const result$ = groupOperations.getPermissionsSets(TEST_GROUP_ID, options);
            const result = tickAndGetEmission(result$);

            expect(client.group.listPermissions).toHaveBeenCalledTimes(1);
            expect(client.group.listPermissions).toHaveBeenCalledWith(TEST_GROUP_ID, options);
            expect(result).toBe(expectedResult as any);
        }));

        it('handles errors properly', fakeAsync(() => {
            assertErrorHandlingWorks(() => groupOperations.getPermissionsSets(TEST_GROUP_ID), client.group.listPermissions);
        }));

    });

    describe('getGroupTypePermissions()', () => {

        const TYPE = AccessControlledType.AUTO_UPDATE;

        it('makes the correct API call and returns the result', fakeAsync(() => {
            const expectedResult: PermissionInfo[] = [
                createPermissionInfo(GcmsPermission.READ),
                createPermissionInfo(GcmsPermission.UPDATE),
            ];
            client.group.getPermission.and.returnValue(createDelayedObservable({ perms: expectedResult }));

            const result$ = groupOperations.getGroupTypePermissions(TEST_GROUP_ID, TYPE);
            const result = tickAndGetEmission(result$);

            expect(client.group.getPermission).toHaveBeenCalledTimes(1);
            expect(client.group.getPermission).toHaveBeenCalledWith(TEST_GROUP_ID, TYPE);
            expect(result).toBe(expectedResult as any);
        }));

        it('handles errors properly', fakeAsync(() => {
            assertErrorHandlingWorks(() => groupOperations.getGroupTypePermissions(TEST_GROUP_ID, TYPE), client.group.getPermission);
        }));

    });

    describe('getGroupInstancePermissions()', () => {

        const TYPE = AccessControlledType.FOLDER;
        const INSTANCE_ID = 4711;

        it('makes the correct API call and returns the result', fakeAsync(() => {
            const expectedResult: PermissionInfo[] = [
                createPermissionInfo(GcmsPermission.READ),
                createPermissionInfo(GcmsPermission.UPDATE),
            ];
            client.group.getInstancePermission.and.returnValue(createDelayedObservable({ perms: expectedResult }));

            const result$ = groupOperations.getGroupInstancePermissions(TEST_GROUP_ID, TYPE, INSTANCE_ID);
            const result = tickAndGetEmission(result$);

            expect(client.group.getInstancePermission).toHaveBeenCalledTimes(1);
            expect(client.group.getInstancePermission).toHaveBeenCalledWith(TEST_GROUP_ID, TYPE, INSTANCE_ID);
            expect(result).toBe(expectedResult as any);
        }));

        it('handles errors properly', fakeAsync(() => {
            assertErrorHandlingWorks(
                () => groupOperations.getGroupInstancePermissions(TEST_GROUP_ID, TYPE, INSTANCE_ID),
                client.group.getInstancePermission,
            );
        }));

    });

    describe('setGroupTypePermissions()', () => {

        const TYPE = AccessControlledType.AUTO_UPDATE;

        it('makes the correct API call', fakeAsync(() => {
            const request: GroupSetPermissionsRequest = {
                perms: [
                    {
                        type: GcmsPermission.READ,
                        value: true,
                    },
                ],
                subGroups: true,
                subObjects: false,
            };
            client.group.setPermission.and.returnValue(createDelayedObservable({}));

            const result$ = groupOperations.setGroupTypePermissions(TEST_GROUP_ID, TEST_GROUP_NAME, TYPE, request);
            tickAndGetEmission(result$);

            expect(client.group.setPermission).toHaveBeenCalledTimes(1);
            expect(client.group.setPermission).toHaveBeenCalledWith(TEST_GROUP_ID, TYPE, request);
            assertNotificationWasShown({
                type: 'success',
                message: 'shared.item_updated',
                translationParams: { name: TEST_GROUP_NAME },
                id: `group.type-permission-change.${TEST_GROUP_ID}`,
            });
        }));

        it('handles errors properly', fakeAsync(() => {
            assertErrorHandlingWorks(() => groupOperations.setGroupTypePermissions(TEST_GROUP_ID, TEST_GROUP_NAME, TYPE, {} as any), client.group.setPermission);
        }));

    });

    describe('setGroupInstancePermissions()', () => {

        const TYPE = AccessControlledType.AUTO_UPDATE;
        const INSTANCE_ID = 4711;

        it('makes the correct API call', fakeAsync(() => {
            const request: GroupSetPermissionsRequest = {
                perms: [
                    {
                        type: GcmsPermission.READ,
                        value: true,
                    },
                ],
                subGroups: true,
                subObjects: false,
            };
            client.group.setInstancePermission.and.returnValue(createDelayedObservable({}));

            const result$ = groupOperations.setGroupInstancePermissions(TEST_GROUP_ID, TEST_GROUP_NAME, TYPE, INSTANCE_ID, request);
            tickAndGetEmission(result$);

            expect(client.group.setInstancePermission).toHaveBeenCalledTimes(1);
            expect(client.group.setInstancePermission).toHaveBeenCalledWith(TEST_GROUP_ID, TYPE, INSTANCE_ID, request);
            assertNotificationWasShown({
                type: 'success',
                message: 'shared.item_updated',
                translationParams: { name: TEST_GROUP_NAME },
                id: `group.instance-permission-change.${TEST_GROUP_ID}`,
            });
        }));

        it('handles errors properly', fakeAsync(() => {
            assertErrorHandlingWorks(
                () => groupOperations.setGroupInstancePermissions(TEST_GROUP_ID, TEST_GROUP_NAME, TYPE, INSTANCE_ID, {} as any),
                client.group.setInstancePermission,
            );
        }));

    });

});

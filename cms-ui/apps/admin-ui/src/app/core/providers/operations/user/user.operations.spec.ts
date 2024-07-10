import { ObservableStopper } from '@admin-ui/common';
import { InterfaceOf } from '@admin-ui/common/utils/util-types/util-types';
import { AppStateService } from '@admin-ui/state';
import { OPTIONS_CONFIG } from '@admin-ui/state/state-store.config';
import { STATE_MODULES } from '@admin-ui/state/state.module';
import { TestBed } from '@angular/core/testing';
import {
    GcmsNormalizer,
    Normalized,
    Raw,
    RecursivePartial,
    ResponseCode,
    User,
    UserListOptions,
    UserListResponse,
    UserResponse,
    UserUpdateResponse,
} from '@gentics/cms-models';
import { getExampleFolderData } from '@gentics/cms-models/testing';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { NgxsModule } from '@ngxs/store';
import { LoggerTestingModule } from 'ngx-logger/testing';
import { of as observableOf } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { EntityManagerService, ErrorHandler, I18nNotificationService } from '../..';
import { MockErrorHandler } from '../../error-handler/error-handler.mock';
import { UserOperations } from './user.operations';

// PREPARE TEST DATA
function convertRawToNormalizedArray(rawEntities: User<Raw>[]): User<Normalized>[] {
    const normalizer = new GcmsNormalizer();
    const indexedEntities = normalizer.normalize('user', rawEntities).entities.user;
    const normalizedEntities = Object.keys(indexedEntities).map(key => indexedEntities[key]);
    return normalizedEntities;
}
const MOCK_USERS_RAW: User<Raw>[] = [
    getExampleFolderData({ id: 1, userId: 1 }).editor,
    getExampleFolderData({ id: 1, userId: 2 }).editor,
];
const MOCK_USERS_NORMALIZED: User<Normalized>[] = convertRawToNormalizedArray(MOCK_USERS_RAW);

class MockApi implements RecursivePartial<InterfaceOf<GcmsApi>> {
    user = {
        getUsers: jasmine.createSpy('getUsers').and.stub(),
        getUser: jasmine.createSpy('getUser').and.stub(),
        updateUser: jasmine.createSpy('updateUser').and.stub(),
        deleteUser: jasmine.createSpy('deleteUser').and.stub(),
    };
}

class MockNotificationService {
    show = jasmine.createSpy('notification.show');
}

describe('UserOperations', () => {

    let api: MockApi;
    let userOperations: UserOperations;
    let notification: MockNotificationService;
    let appState: AppStateService;
    let stopper: ObservableStopper;

    beforeEach(() => {

        notification = new MockNotificationService();
        stopper = new ObservableStopper();

        TestBed.configureTestingModule({
            imports: [
                NgxsModule.forRoot(STATE_MODULES, OPTIONS_CONFIG),
                LoggerTestingModule,
            ],
            providers: [
                AppStateService,
                EntityManagerService,
                UserOperations,
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: GcmsApi, useClass: MockApi },
                { provide: I18nNotificationService, useValue: notification },
            ],
        });

        api = TestBed.get(GcmsApi);
        userOperations = TestBed.get(UserOperations);
        appState = TestBed.get(AppStateService);

    });

    afterEach(() => {
        stopper.stop();
    });

    it('getUsers() works', () => {
        // prepare test data
        const requestOptions: UserListOptions = { pageSize: 10 };
        const mockResponse: UserListResponse = {
            responseInfo: { responseCode: ResponseCode.OK },
            hasMoreItems: false,
            numItems: MOCK_USERS_RAW.length,
            items: MOCK_USERS_RAW,
            messages: [],
        };
        api.user.getUsers.and.returnValue(observableOf(mockResponse));

        // check if state does not yet has any user stored
        const storedUsers$ = appState.select(state => state.entity.user);
        let storedUsers: User<Normalized>[];
        storedUsers$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(users => storedUsers = Object.keys(users).map(key => users[key]));
        expect(storedUsers).toEqual([]);

        const response$ = userOperations.getAll(requestOptions);
        expect(api.user.getUsers).toHaveBeenCalledWith(requestOptions);

        // check if correct response
        let loadedUsers: User<Raw>[];
        response$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(users => loadedUsers = users);
        expect(loadedUsers).toEqual(MOCK_USERS_RAW);

        // check if mock users have been put into store
        storedUsers$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(users => storedUsers = Object.keys(users).map(key => users[key]));
        expect(storedUsers).toEqual(MOCK_USERS_NORMALIZED);
    });

    it('getUser() works', () => {
        // prepare test data
        const MOCKUSER_RAW = MOCK_USERS_RAW[0];
        const MOCKUSER_NORMALIZED = MOCK_USERS_NORMALIZED[0];
        const mockResponse: UserResponse = {
            responseInfo: { responseCode: ResponseCode.OK },
            user: MOCKUSER_RAW,
        };
        api.user.getUser.and.returnValue(observableOf(mockResponse));

        // check if state does not yet has any user stored
        const storedUser$ = appState.select(state => state.entity.user[MOCKUSER_RAW.id]);
        let storedUser: User<Normalized>;
        storedUser$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(user => storedUser = user);
        expect(storedUser).toEqual(undefined);

        const response$ = userOperations.get(MOCKUSER_RAW.id);
        expect(api.user.getUser).toHaveBeenCalledWith(MOCKUSER_RAW.id, undefined);

        // check if correct response
        let loadedUser: User<Raw>;
        response$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(user => loadedUser = user);
        expect(loadedUser).toEqual(MOCKUSER_RAW);

        // check if mock users have been put into store
        storedUser$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(user => storedUser = user);
        expect(storedUser).toEqual(MOCKUSER_NORMALIZED);
    });

    it('updateUser() works', () => {
        // prepare test data
        const MOCKUSER_ORIGINAL_RAW = MOCK_USERS_RAW[0];
        const MOCKUSER_ORIGINAL_NORMALIZED = MOCK_USERS_NORMALIZED[0];
        const mockGetResponse: UserResponse = {
            responseInfo: { responseCode: ResponseCode.OK },
            user: MOCKUSER_ORIGINAL_RAW,
        };
        api.user.getUser.and.returnValue(observableOf(mockGetResponse));

        const MOCKUSER_UPDATED_RAW = { ...MOCKUSER_ORIGINAL_RAW, firstName: 'updatedUser' };
        const MOCKUSER_UPDATED_NORMALIZED = convertRawToNormalizedArray([MOCKUSER_UPDATED_RAW])[0];
        const mockUpdateResponse: UserUpdateResponse = {
            messages: [],
            responseInfo: { responseCode: ResponseCode.OK },
            user: MOCKUSER_UPDATED_RAW,
        };
        api.user.updateUser.and.returnValue(observableOf(mockUpdateResponse));

        // load original user and store in state
        const getResponse$ = userOperations.get(MOCKUSER_ORIGINAL_RAW.id);
        let loadedUser: User<Raw>;
        getResponse$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(user => loadedUser = user);

        // check if state does hold original user
        const storedUser$ = appState.select(state => state.entity.user[MOCKUSER_ORIGINAL_RAW.id]);
        let storedUser: User<Normalized>;
        storedUser$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(user => storedUser = user);
        expect(storedUser).toEqual(MOCKUSER_ORIGINAL_NORMALIZED);

        // update user
        const updateResponse$ = userOperations.update(MOCKUSER_ORIGINAL_RAW.id, MOCKUSER_UPDATED_RAW);
        expect(api.user.updateUser).toHaveBeenCalledWith(MOCKUSER_ORIGINAL_RAW.id, MOCKUSER_UPDATED_RAW);
        let updatedUser: User<Raw>;
        updateResponse$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(user => updatedUser = user);
        expect(updatedUser).toEqual(MOCKUSER_UPDATED_RAW);
        // check if toast notification has been called
        expect(notification.show).toHaveBeenCalledWith({
            type: 'success',
            message: 'shared.item_updated',
            translationParams: { name: MOCKUSER_ORIGINAL_RAW.login },
        });

        // check if updated user has been put into store
        storedUser$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(user => storedUser = user);
        expect(storedUser).toEqual(MOCKUSER_UPDATED_NORMALIZED);
    });

    it('deleteUser() works', () => {
        // prepare test data
        const mockGetResponse: UserListResponse = {
            responseInfo: { responseCode: ResponseCode.OK },
            hasMoreItems: false,
            numItems: MOCK_USERS_RAW.length,
            items: MOCK_USERS_RAW,
            messages: [],
        };
        api.user.getUsers.and.returnValue(observableOf(mockGetResponse));
        api.user.deleteUser.and.returnValue(observableOf({}));

        // load users and store in state
        const getListResponse$ = userOperations.getAll();
        let loadedUsers: User<Raw>[];
        getListResponse$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(users => loadedUsers = users);

        // check if state does hold the users
        const storedUsers$ = appState.select(state => state.entity.user);
        let storedUsers: User<Normalized>[];
        storedUsers$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(users => storedUsers = Object.keys(users).map(key => users[key]));
        expect(storedUsers).toEqual(MOCK_USERS_NORMALIZED);

        // delete user
        const deleteResponse$ = userOperations.delete(MOCK_USERS_RAW[0].id);
        expect(api.user.deleteUser).toHaveBeenCalledWith(MOCK_USERS_RAW[0].id);
        deleteResponse$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(() => {});
        // check if toast notification has been called
        expect(notification.show).toHaveBeenCalledWith({
            type: 'success',
            message: 'shared.item_singular_deleted',
            translationParams: { name: MOCK_USERS_RAW[0].login },
        });

        // check if deleted user has been removed from store
        storedUsers$.pipe(
            takeUntil(stopper.stopper$),
        ).subscribe(users => storedUsers = Object.keys(users).map(key => users[key]));
        expect(storedUsers).toEqual([ MOCK_USERS_NORMALIZED[1] ]);
    });

});

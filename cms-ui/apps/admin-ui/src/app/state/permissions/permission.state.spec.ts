import { TestBed, waitForAsync } from '@angular/core/testing';
import { AccessControlledType, PermissionsMapCollection } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { AppStateService } from '..';
import { TEST_APP_STATE, TestAppState } from '../utils/test-app-state';
import { AddTypePermissionsMap, ClearAllPermissions } from './permissions.actions';
import { INITIAL_PERMISSIONS_STATE, PermissionsStateModule } from './permissions.state';

const MOCK_PERM_MAP1: PermissionsMapCollection = {
    permissions: {
        read: true,
        setperm: false,
        update: true,
    },
};
Object.freeze(MOCK_PERM_MAP1);

const MOCK_PERM_MAP2: PermissionsMapCollection = {
    permissions: {
        read: true,
        setperm: false,
        update: false,
        assignroles: true,
    },
};
Object.freeze(MOCK_PERM_MAP2);

describe('MaintenanceModeStateModule', () => {

    let appState: TestAppState;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot([PermissionsStateModule])],
            providers: [TEST_APP_STATE],
        }).compileComponents();
        appState = TestBed.inject(AppStateService) as any;
    }));

    it('sets the correct initial state', () => {
        expect(appState.now.permissions).toEqual(INITIAL_PERMISSIONS_STATE);
    });

    describe('AddTypePermissionsMap', () => {

        it('adds a new map', () => {
            appState.dispatch(new AddTypePermissionsMap(AccessControlledType.INBOX, MOCK_PERM_MAP1));
            expect(appState.now.permissions.types).toEqual({
                [AccessControlledType.INBOX]: MOCK_PERM_MAP1,
            });
        });

        it('keeps existing maps when adding a new one', () => {
            appState.mockState({
                permissions: {
                    types: {
                        [AccessControlledType.INBOX]: MOCK_PERM_MAP1,
                    },
                },
            });

            appState.dispatch(new AddTypePermissionsMap(AccessControlledType.MAINTENANCE, MOCK_PERM_MAP2));
            expect(appState.now.permissions.types).toEqual({
                [AccessControlledType.INBOX]: MOCK_PERM_MAP1,
                [AccessControlledType.MAINTENANCE]: MOCK_PERM_MAP2,
            });
        });

        it('replaces an existing map', () => {
            appState.mockState({
                permissions: {
                    types: {
                        [AccessControlledType.INBOX]: MOCK_PERM_MAP1,
                    },
                },
            });

            appState.dispatch(new AddTypePermissionsMap(AccessControlledType.INBOX, MOCK_PERM_MAP2));
            expect(appState.now.permissions.types).toEqual({
                [AccessControlledType.INBOX]: MOCK_PERM_MAP2,
            });
        });

    });

    it('ClearAllPermissions works', () => {
        appState.mockState({
            permissions: {
                types: {
                    [AccessControlledType.INBOX]: MOCK_PERM_MAP1,
                    [AccessControlledType.MAINTENANCE]: MOCK_PERM_MAP2,
                },
            },
        });

        appState.dispatch(new ClearAllPermissions());
        expect(appState.now.permissions).toEqual(INITIAL_PERMISSIONS_STATE);
    });

});

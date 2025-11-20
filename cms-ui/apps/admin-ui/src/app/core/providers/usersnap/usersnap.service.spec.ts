import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { UsersnapSettings } from '@gentics/cms-models';
import { tap } from 'rxjs/operators';
import { createDelayedObservable } from '../../../../testing';
import { InterfaceOf } from '../../../common';
import { assembleTestAppStateImports, TEST_APP_STATE, TestAppState } from '../../../state/utils/test-app-state';
import { AdminOperations } from '../operations/admin/admin.operations';
import { UsersnapService } from './usersnap.service';

@Injectable()
class TestUsersnapService extends UsersnapService {

    activateUsersnapSpy = jasmine.createSpy('activateUsersnap').and.stub();

    // Override actual activateUsersnap(), because we do not want to add a new <script>
    // tag to the test.
    protected activateUsersnap(settings: UsersnapSettings): void {
        this.activateUsersnapSpy(settings);
    }

}

class MockAdminOperations implements Partial<InterfaceOf<AdminOperations>> {
    getUsersnapSettings = jasmine.createSpy('getUsersnapSettings').and.stub();
}

describe('UsersnapService', () => {

    let appState: TestAppState;
    let adminOps: MockAdminOperations;
    let usersnapService: TestUsersnapService;
    let usersnapSettings: UsersnapSettings;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                assembleTestAppStateImports(),
            ],
            providers: [
                TEST_APP_STATE,
                { provide: AdminOperations, useClass: MockAdminOperations },
                { provide: UsersnapService, useClass: TestUsersnapService },
            ],
        });

        appState = TestBed.inject(AppStateService) as any;
        adminOps = TestBed.inject(AdminOperations) as any;
        usersnapService = TestBed.inject(UsersnapService) as any;
        usersnapSettings = { key: 'test' };

        adminOps.getUsersnapSettings.and.callFake(() => {
            return createDelayedObservable(usersnapSettings).pipe(
                tap(() => {
                    appState.mockState({
                        ui: {
                            usersnap: usersnapSettings,
                        },
                    });
                }),
            );
        });
    });

    afterEach(() => {
        usersnapService.ngOnDestroy();
    });

    it('loads the Usersnap settings and activates usersnap if the feature is enabled', fakeAsync(() => {
        usersnapService.init();
        expect(adminOps.getUsersnapSettings.calls.count()).toBe(0, 'Usersnap should only be initialized if the corresponding feature is enabled.');

        appState.mockState({
            features: {
                global: {
                    usersnap: true,
                },
            },
        });
        expect(adminOps.getUsersnapSettings).toHaveBeenCalledTimes(1);
        expect(usersnapService.activateUsersnapSpy.calls.count()).toBe(0, 'Usersnap should only be initialized after the its settings have been loaded.');

        tick();
        expect(usersnapService.activateUsersnapSpy).toHaveBeenCalledTimes(1);
        expect(usersnapService.activateUsersnapSpy).toHaveBeenCalledWith(appState.now.ui.usersnap);
    }));

    it('does not activate Usersnap twice', fakeAsync(() => {
        appState.mockState({
            features: {
                global: {
                    usersnap: true,
                },
            },
        });
        usersnapService.init();

        tick();
        expect(usersnapService.activateUsersnapSpy).toHaveBeenCalledTimes(1);

        appState.mockState({
            features: {
                global: {
                    usersnap: false,
                },
            },
        });
        appState.mockState({
            ui: {
                usersnap: { key: 'test2' },
            },
        });
        expect(usersnapService.activateUsersnapSpy.calls.count()).toBe(1, 'Usersnap should not be activated twice.');
    }));

});

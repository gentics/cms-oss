import { Injectable } from '@angular/core';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { User, UsersnapSettings, Variant } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { UIState } from '../../../common/models';
import { ApplicationStateService, STATE_MODULES, UIActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { UsersnapService } from './usersnap.service';

@Injectable()
class TestUsersnapService extends UsersnapService {

    activateUsersnapSpy = jasmine.createSpy('activateUsersnap').and.stub();

    // Override actual activateUsersnap(), because we do not want to add a new <script>
    // tag to the test.
    protected override activateUsersnap(
        settings: UsersnapSettings,
        ui: UIState,
        user: User,
    ): void {
        this.activateUsersnapSpy(settings, ui, user);
    }

}

class MockUIActions {
    getUsersnapSettings = jasmine.createSpy('getUsersnapSettings').and.stub();
}

describe('UsersnapService', () => {

    let appState: TestApplicationState;
    let uiActions: MockUIActions;
    let usersnapService: TestUsersnapService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: UIActionsService, useClass: MockUIActions },
                { provide: UsersnapService, useClass: TestUsersnapService },
            ],
        });

        appState = TestBed.inject(ApplicationStateService) as any;
        uiActions = TestBed.inject(UIActionsService) as any;
        usersnapService = TestBed.inject(UsersnapService) as any;
    });

    afterEach(() => {
        usersnapService.ngOnDestroy();
    });

    it('loads the Usersnap settings and activates usersnap if the feature is enabled', fakeAsync(() => {
        appState.mockState({
            ui: {
                language: 'en',
                uiVersion: '6.4.0',
                cmpVersion: {
                    cmpVersion: '8.4.0',
                    variant: Variant.OPEN_SOURCE,
                    version: '6.4.0',
                    nodeInfo: {},
                },
            },
            auth: {
                user: {
                    id: 1,
                    email: 'example@example.com',
                    firstName: 'abc',
                    lastName: 'xyz',
                },
            },
        });

        usersnapService.init();

        tick(5_000);

        expect(uiActions.getUsersnapSettings.calls.count()).toBe(0, 'Usersnap should only be initialized if the corresponding feature is enabled.');

        appState.mockState({
            features: {
                usersnap: true,
            },
        });

        tick(5_000);

        expect(uiActions.getUsersnapSettings).toHaveBeenCalledTimes(1);
        expect(usersnapService.activateUsersnapSpy.calls.count()).toBe(0, 'Usersnap should only be initialized after the its settings have been loaded.');

        appState.mockState({
            ui: {
                usersnap: { key: 'test' },
            },
        });

        tick(5_000);

        expect(usersnapService.activateUsersnapSpy).toHaveBeenCalledTimes(1);
    }));

    it('does not activate Usersnap twice', fakeAsync(() => {
        appState.mockState({
            features: {
                usersnap: true,
            },
            ui: {
                language: 'en',
                uiVersion: '6.4.0',
                cmpVersion: {
                    cmpVersion: '8.4.0',
                    variant: Variant.OPEN_SOURCE,
                    version: '6.4.0',
                    nodeInfo: {},
                },
            },
            auth: {
                user: {
                    id: 1,
                    email: 'example@example.com',
                    firstName: 'abc',
                    lastName: 'xyz',
                },
            },
        });

        usersnapService.init();

        tick(5_000);

        appState.mockState({
            ui: {
                usersnap: { key: 'test' },
            },
        });

        tick(5_000);

        expect(usersnapService.activateUsersnapSpy).toHaveBeenCalledTimes(1);

        appState.mockState({
            features: {
                usersnap: false,
            },
            ui: {
                usersnap: { key: 'test2' },
            },
        });

        tick(5_000);

        expect(usersnapService.activateUsersnapSpy.calls.count()).toBe(1, 'Usersnap should not be activated twice.');
    }));

});

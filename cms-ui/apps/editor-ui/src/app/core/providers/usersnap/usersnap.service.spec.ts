import { Injectable } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { UsersnapSettings } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { ApplicationStateService, STATE_MODULES, UIActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { UsersnapService } from './usersnap.service';

@Injectable()
class TestUsersnapService extends UsersnapService {

    constructor(
        appState: ApplicationStateService,
        uiActions: UIActionsService,
    ) {
        super(appState, uiActions);
    }

    activateUsersnapSpy = jasmine.createSpy('activateUsersnap').and.stub();

    // Override actual activateUsersnap(), because we do not want to add a new <script>
    // tag to the test.
    protected activateUsersnap(settings: UsersnapSettings): void {
        this.activateUsersnapSpy(settings);
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

        appState = TestBed.get(ApplicationStateService);
        uiActions = TestBed.get(UIActionsService);
        usersnapService = TestBed.get(UsersnapService);
    });

    afterEach(() => {
        usersnapService.ngOnDestroy();
    });

    it('loads the Usersnap settings and activates usersnap if the feature is enabled', () => {
        usersnapService.init();
        expect(uiActions.getUsersnapSettings.calls.count()).toBe(0, 'Usersnap should only be initialized if the corresponding feature is enabled.');

        appState.mockState({
            features: {
                usersnap: true,
            },
        });
        expect(uiActions.getUsersnapSettings).toHaveBeenCalledTimes(1);
        expect(usersnapService.activateUsersnapSpy.calls.count()).toBe(0, 'Usersnap should only be initialized after the its settings have been loaded.');

        appState.mockState({
            ui: {
                usersnap: { key: 'test' },
            },
        });
        expect(usersnapService.activateUsersnapSpy).toHaveBeenCalledTimes(1);
        expect(usersnapService.activateUsersnapSpy).toHaveBeenCalledWith(appState.now.ui.usersnap);
    });

    it('does not activate Usersnap twice', () => {
        appState.mockState({
            features: {
                usersnap: true,
            },
        });
        usersnapService.init();

        appState.mockState({
            ui: {
                usersnap: { key: 'test' },
            },
        });
        expect(usersnapService.activateUsersnapSpy).toHaveBeenCalledTimes(1);

        appState.mockState({
            features: {
                usersnap: false,
            },
        });
        appState.mockState({
            ui: {
                usersnap: { key: 'test2' },
            },
        });
        expect(usersnapService.activateUsersnapSpy.calls.count()).toBe(1, 'Usrsnap should not be activated twice.');
    });

});

import { TestBed } from '@angular/core/testing';
import { MaintenanceModeResponse, ResponseCode } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { MaintenanceModeState } from '../../../common/models';
import { ApplicationStateService } from '../../providers';
import { TestApplicationState } from '../../test-application-state.mock';
import { STATE_MODULES } from '../state-modules';
import { MaintenanceModeFetchErrorAction, MaintenanceModeFetchSuccessAction, StartMaintenanceModeFetchingAction } from './maintenance-mode.actions';

describe('MaintenanceModeStateModule', () => {

    let appState: TestApplicationState;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        appState = TestBed.inject(ApplicationStateService) as any;
    });

    it('sets the correct initial state', () => {
        expect(appState.now.maintenanceMode).toEqual({
            active: false,
            fetching: false,
            message: '',
            reportedByServer: undefined,
            showBanner: false,
        } as MaintenanceModeState);
    });

    it('fetchStatusStart works', () => {
        appState.dispatch(new StartMaintenanceModeFetchingAction());
        expect(appState.now.maintenanceMode.fetching).toBe(true);
    });

    it('fetchStatusSuccess works', () => {
        appState.mockState({
            maintenanceMode: {
                fetching: true,
            },
        });

        const response: MaintenanceModeResponse = {
            messages: [],
            responseInfo: {
                responseCode: ResponseCode.OK,
            },
            maintenance: true,
            banner: false,
            message: 'The building is burning, stop working!',
        };

        appState.dispatch(new MaintenanceModeFetchSuccessAction(response));

        expect(appState.now.maintenanceMode).toEqual({
            active: true,
            fetching: false,
            message: 'The building is burning, stop working!',
            reportedByServer: true,
            showBanner: false,
        });
    });

    it('fetchStatusError works', () => {
        appState.mockState({
            maintenanceMode: {
                fetching: true,
            },
        });

        appState.dispatch(new MaintenanceModeFetchErrorAction());

        expect(appState.now.maintenanceMode).toEqual({
            active: false,
            fetching: false,
            message: '',
            reportedByServer: false,
            showBanner: false,
        });
    });

});

import { TestBed, waitForAsync } from '@angular/core/testing';
import { MaintenanceModeResponse } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';

import { AppStateService } from '../providers/app-state/app-state.service';
import { TestAppState, TEST_APP_STATE } from '../utils/test-app-state';
import { FetchMaintenanceStatusError, FetchMaintenanceStatusStart, FetchMaintenanceStatusSuccess } from './maintenance-mode.actions';
import { INITIAL_MAINTENANCE_MODE_STATE, MaintenanceModeStateModel, MaintenanceModeStateModule } from './maintenance-mode.state';

describe('MaintenanceModeStateModule', () => {

    let appState: TestAppState;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot([MaintenanceModeStateModule])],
            providers: [TEST_APP_STATE],
        }).compileComponents();
        appState = TestBed.get(AppStateService);
    }));

    it('sets the correct initial state', () => {
        appState.selectOnce(state => state.maintenanceMode).subscribe(maintenanceMode => {
            expect(maintenanceMode).toEqual(INITIAL_MAINTENANCE_MODE_STATE);
        });
    });

    it('FetchMaintenanceStatusStart works', () => {
        appState.dispatch(new FetchMaintenanceStatusStart());
        expect(appState.snapshot().maintenanceMode).toEqual(jasmine.objectContaining<MaintenanceModeStateModel>({
            fetching: true,
        }));
    });

    it('FetchMaintenanceStatusSuccess works', () => {
        appState.mockState({
            maintenanceMode: {
                fetching: true,
            },
        });

        const response: MaintenanceModeResponse = {
            messages: [],
            responseInfo: {
                responseCode: 'OK',
            },
            maintenance: true,
            banner: false,
            message: 'The building is burning, stop working!',
        };
        appState.dispatch(new FetchMaintenanceStatusSuccess(response));

        expect(appState.snapshot().maintenanceMode).toEqual({
            active: true,
            fetching: false,
            message: 'The building is burning, stop working!',
            reportedByServer: true,
            showBanner: false,
        });
    });

    it('FetchStatusError works', () => {
        appState.mockState({
            maintenanceMode: {
                fetching: true,
            },
        });

        appState.dispatch(new FetchMaintenanceStatusError());

        expect(appState.snapshot().maintenanceMode).toEqual({
            active: false,
            fetching: false,
            message: '',
            reportedByServer: false,
            showBanner: false,
        });
    });

});

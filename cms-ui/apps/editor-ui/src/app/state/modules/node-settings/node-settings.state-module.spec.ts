import { TestBed } from '@angular/core/testing';
import { NgxsModule } from '@ngxs/store';
import { NodeSettingsState } from '../../../common/models';
import { ApplicationStateService } from '../../providers';
import { TestApplicationState } from '../../test-application-state.mock';
import { STATE_MODULES } from '../state-modules';
import { NodeSettingsFetchingSuccessAction } from './node-settings.actions';

describe('NodeSettingsStateModule', () => {

    let appState: TestApplicationState;
    const NODEID = 1;
    const GLOBALNODESETTINGS = {
        testglobalkey: 'testglobalvalue',
    };
    const NODESETTINGS = {
        testkey: 'testvalue',
    };

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
        expect(appState.now.nodeSettings).toEqual({
            node: {},
            global: {},
        } as NodeSettingsState);
    });

    describe('loaded works', () => {
        it('with node settings', () => {
            appState.dispatch(new NodeSettingsFetchingSuccessAction(NODEID, NODESETTINGS));
            expect(appState.now.nodeSettings.node[NODEID]).toEqual(NODESETTINGS);
            expect(appState.now.nodeSettings.global).not.toEqual(NODESETTINGS);
        });

        it('with global settings', () => {
            appState.dispatch(new NodeSettingsFetchingSuccessAction(NODEID, NODESETTINGS, true));
            expect(appState.now.nodeSettings.node[NODEID]).toEqual(NODESETTINGS);
            expect(appState.now.nodeSettings.global).toEqual(NODESETTINGS);
        });
    });
});

import { TestBed, waitForAsync } from '@angular/core/testing';
import { Feature, NodeFeature } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { cloneDeep as _cloneDeep } from'lodash-es'
import { AppStateService } from '../providers/app-state/app-state.service';
import { TestAppState, TEST_APP_STATE } from '../utils/test-app-state';
import { SetGlobalFeature, SetNodeFeatures } from './features.actions';
import { FeaturesStateModel, FeaturesStateModule, GlobalFeaturesMap, INITIAL_FEATURES_STATE } from './features.state';

const NODE_A = 1;
const NODE_B = 2;
const NODE_C = 3;

describe('FeaturesStateModule', () => {

    let appState: TestAppState;
    let expectedState: FeaturesStateModel;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot([FeaturesStateModule])],
            providers: [TEST_APP_STATE],
        }).compileComponents();

        appState = TestBed.get(AppStateService);
        expectedState = _cloneDeep(INITIAL_FEATURES_STATE);
    }));

    it('sets the correct initial state', () => {
        expect(appState.now.features).toEqual(INITIAL_FEATURES_STATE);
    });

    describe('setNodeFeatures()', () => {

        function mockFeaturesMap(allEnabled: boolean): GlobalFeaturesMap {
            return {
                [Feature.ALWAYS_LOCALIZE]: allEnabled,
                [Feature.ELASTICSEARCH]: allEnabled,
                [Feature.NICE_URLS]: allEnabled,
                [Feature.RECENT_ITEMS]: allEnabled,
            };
        }

        function mockState(globalFeatures: GlobalFeaturesMap): void {
            appState.mockState({
                features: {
                    global: _cloneDeep(globalFeatures),
                },
            });
            expectedState.global = globalFeatures;
        }

        it('enables a feature that has not been set yet', () => {
            appState.dispatch(new SetGlobalFeature(Feature.NICE_URLS, true));
            expectedState.global[Feature.NICE_URLS] = true;
            expect(appState.now.features).toEqual(expectedState);
        });

        it('disables a feature that has not been set yet', () => {
            appState.dispatch(new SetGlobalFeature(Feature.NICE_URLS, false));
            expectedState.global[Feature.NICE_URLS] = false;
            expect(appState.now.features).toEqual(expectedState);
        });

        it('enables a feature that been set and does not affect other features', () => {
            const allDisabled = mockFeaturesMap(false);
            mockState(allDisabled);

            appState.dispatch(new SetGlobalFeature(Feature.ALWAYS_LOCALIZE, true));
            expectedState.global[Feature.ALWAYS_LOCALIZE] = true;
            expect(appState.now.features).toEqual(expectedState);
        });

        it('disables a feature that has been set and does not affect other features', () => {
            const allEnabled = mockFeaturesMap(true);
            mockState(allEnabled);

            appState.dispatch(new SetGlobalFeature(Feature.ALWAYS_LOCALIZE, false));
            expectedState.global[Feature.ALWAYS_LOCALIZE] = false;
            expect(appState.now.features).toEqual(expectedState);
        });

    });

    describe('setNodeFeatures()', () => {

        function mockNodeFeaturesState(): void {
            appState.mockState({
                features: {
                    node: {
                        [NODE_A]: {
                            [NodeFeature.CONTENT_AUTO_OFFLINE]: true,
                            [NodeFeature.ASSET_MANAGEMENT]: true,
                        },
                        [NODE_B]: {
                            [NodeFeature.CONTENT_AUTO_OFFLINE]: true,
                        },
                    },
                },
            });
            expectedState = _cloneDeep(appState.now.features);
        }

        it('sets features for a new node', () => {
            appState.dispatch(new SetNodeFeatures(NODE_A, [NodeFeature.CONTENT_AUTO_OFFLINE, NodeFeature.ASSET_MANAGEMENT]));

            expectedState.node[NODE_A] = {
                [NodeFeature.CONTENT_AUTO_OFFLINE]: true,
                [NodeFeature.ASSET_MANAGEMENT]: true,
            };
            expect(appState.now.features).toEqual(expectedState);
        });

        it('sets features for a new node without affecting other nodes', () => {
            mockNodeFeaturesState();
            appState.dispatch(new SetNodeFeatures(NODE_C, [NodeFeature.CONTENT_AUTO_OFFLINE, NodeFeature.ASSET_MANAGEMENT]));

            expectedState.node[NODE_C] = {
                [NodeFeature.CONTENT_AUTO_OFFLINE]: true,
                [NodeFeature.ASSET_MANAGEMENT]: true,
            };
            expect(appState.now.features).toEqual(expectedState);
        });

        it('replaces features for an existing node', () => {
            mockNodeFeaturesState();
            appState.dispatch(new SetNodeFeatures(NODE_A, [NodeFeature.CONTENT_AUTO_OFFLINE]));

            expectedState.node[NODE_A] = {
                [NodeFeature.CONTENT_AUTO_OFFLINE]: true,
            };
            expect(appState.now.features).toEqual(expectedState);
        });

        it('works for no enabled features', () => {
            mockNodeFeaturesState();
            appState.dispatch(new SetNodeFeatures(NODE_A, []));

            expectedState.node[NODE_A] = {};
            expect(appState.now.features).toEqual(expectedState);
        });

    });

});

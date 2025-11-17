import { InterfaceOf, ObservableStopper } from '@admin-ui/common';
import { AppStateService, GlobalFeaturesMap, NodeFeaturesMap, SetGlobalFeature, SetNodeFeatures } from '@admin-ui/state';
import { assembleTestAppStateImports, TEST_APP_STATE, TestAppState, TrackedActions } from '@admin-ui/state/utils/test-app-state';
import { createDelayedObservable } from '@admin-ui/testing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { Feature, FeatureResponse, NodeFeature, RecursivePartial } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { ofActionDispatched } from '@ngxs/store';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ErrorHandler } from '../../error-handler';
import { MockErrorHandler } from '../../error-handler/error-handler.mock';
import { ALL_GLOBAL_FEATURES, FeatureOperations } from './feature.operations';

class MockApi implements RecursivePartial<InterfaceOf<GcmsApi>> {
    admin = {
        getFeature: jasmine.createSpy('getFeature'),
    };
    folders = {
        getNodeFeatures: jasmine.createSpy('getNodeFeatures'),
    };
}

const GLOBAL_FEATURE_A = Feature.NICE_URLS;
const NODE_FEATURE_A = NodeFeature.ASSET_MANAGEMENT;
const NODE_ID = 2;

describe('FeatureOperations', () => {

    let api: MockApi;
    let appState: TestAppState;
    let featureOps: FeatureOperations;
    let stopper: ObservableStopper;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                assembleTestAppStateImports(),
            ],
            providers: [
                FeatureOperations,
                TEST_APP_STATE,
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: GcmsApi, useClass: MockApi },
            ],
        });

        api = TestBed.inject(GcmsApi) as any;
        appState = TestBed.inject(AppStateService) as any;
        featureOps = TestBed.inject(FeatureOperations);
        stopper = new ObservableStopper();
    });

    afterEach(() => {
        stopper.stop();
    });

    it('ALL_GLOBAL_FEATURES has a non-zero length', () => {
        // If we ever make a change to the compiler config that prevents us from getting enum keys at runtime,
        // this test should fail.
        expect(ALL_GLOBAL_FEATURES.length).toBeGreaterThan(0);
    });

    describe('checkGlobalFeature()', () => {

        let dispatchedActions: TrackedActions<SetGlobalFeature>;

        beforeEach(() => {
            const filterSpy = jasmine.createSpy('ofActionDispatched').and.callFake((...allowedTypes: any[]) => ofActionDispatched(...allowedTypes));
            dispatchedActions = appState.trackActionsAuto(filterSpy, SetGlobalFeature);
        });

        it('checks a feature and adds the result to the AppState', fakeAsync(() => {
            api.admin.getFeature.and.returnValue(createDelayedObservable({ activated: true }));

            let result: boolean;
            featureOps.checkGlobalFeature(GLOBAL_FEATURE_A).pipe(
                takeUntil(stopper.stopper$),
            ).subscribe(active => result = active);

            tick();
            expect(result).toBe(true);
            expect(dispatchedActions.count).toBe(1);
            expect(dispatchedActions.get(0).feature).toEqual(GLOBAL_FEATURE_A);
            expect(dispatchedActions.get(0).enabled).toBe(true);
        }));
    });

    describe('checkAllGlobalFeatures()', () => {

        let dispatchedActions: TrackedActions<SetGlobalFeature>;

        beforeEach(() => {
            const filterSpy = jasmine.createSpy('ofActionDispatched').and.callFake((...allowedTypes: any[]) => ofActionDispatched(...allowedTypes));
            dispatchedActions = appState.trackActionsAuto(filterSpy, SetGlobalFeature);
        });

        it('checks each global feature and emits the global features state after all checks', () => {
            const checks$: Subject<FeatureResponse>[] = [];
            api.admin.getFeature.and.callFake(() => {
                const subj$ = new Subject<FeatureResponse>();
                checks$.push(subj$);
                return subj$;
            });

            let result: GlobalFeaturesMap;
            featureOps.checkAllGlobalFeatures().pipe(
                takeUntil(stopper.stopper$),
            ).subscribe(features => result = features);

            expect(result).toBeUndefined('Feature check observables have not emitted yet.');
            expect(api.admin.getFeature).toHaveBeenCalledTimes(ALL_GLOBAL_FEATURES.length);
            const checkedFeatureKeys = api.admin.getFeature.calls.all().map(call => call.args[0]);
            expect(checkedFeatureKeys).toEqual(ALL_GLOBAL_FEATURES);

            // Emit from all observables, except for the last one.
            for (let i = 0; i < checks$.length - 1; ++i) {
                const subj$ = checks$[i];
                subj$.next({ activated: true } as any);
                subj$.complete();
            }
            expect(result).toBeUndefined('One feature check observable has still not emitted.');

            // Emit from the last observable.
            const lastSubj$ = checks$[checks$.length - 1];
            lastSubj$.next({ activated: true } as any);
            lastSubj$.complete();

            expect(dispatchedActions.count).toBe(ALL_GLOBAL_FEATURES.length);
            for (let i = 0; i < dispatchedActions.count; ++i) {
                const action = dispatchedActions.get(i);
                expect(action.enabled).toBe(true);
                expect(action.feature).toEqual(checkedFeatureKeys[i]);
            }
            expect(result).toEqual(appState.now.features.global);
        });
    });

    describe('getNodeFeatures()', () => {

        let dispatchedActions: TrackedActions<SetNodeFeatures>;

        beforeEach(() => {
            const filterSpy = jasmine.createSpy('ofActionDispatched').and.callFake((...allowedTypes: any[]) => ofActionDispatched(...allowedTypes));
            dispatchedActions = appState.trackActionsAuto(filterSpy, SetNodeFeatures);
        });

        it('fetches the features of a node and adds them to the AppState', fakeAsync(() => {
            const enabledFeatures = [NODE_FEATURE_A];
            api.folders.getNodeFeatures.and.returnValue(createDelayedObservable({ features: enabledFeatures }));

            let result: NodeFeaturesMap;
            featureOps.getNodeFeatures(NODE_ID).pipe(
                takeUntil(stopper.stopper$),
            ).subscribe(features => result = features);

            tick();
            expect(result).toEqual({
                [NODE_FEATURE_A]: true,
            });
            expect(dispatchedActions.count).toBe(1);
            expect(dispatchedActions.get(0).nodeId).toBe(NODE_ID);
            expect(dispatchedActions.get(0).enabledFeatures).toEqual(enabledFeatures);
        }));
    });

});

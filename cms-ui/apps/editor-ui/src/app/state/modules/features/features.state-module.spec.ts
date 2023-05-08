import { TestBed } from '@angular/core/testing';
import { Feature, NodeFeature } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { FeaturesState } from '../../../common/models';
import { ApplicationStateService } from '../../providers';
import { TestApplicationState } from '../../test-application-state.mock';
import { STATE_MODULES } from '../state-modules';
import { SetFeatureAction, SetNodeFeaturesAction } from './features.actions';

const getOrderedObjectKeys = (o: object): string[] => Object.keys(o)
    .filter(k => k !== 'nodeFeatures')
    .sort();

describe('FeaturesStateModule', () => {

    let appState: TestApplicationState;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        appState = TestBed.get(ApplicationStateService);
    });

    it('sets the correct initial state', () => {
        expect(appState.now.features).toEqual({
            nice_urls: false,
            elasticsearch: false,
            always_localize: false,
            recent_items: false,
            focal_point_editing: false,
            hide_manual: false,
            imagemanipulation2: false,
            enable_image_upload_in_tagfill: false,
            tagfill_light: true,
            wastebin: false,
            pub_dir_segment: false,
            usersnap: false,
            autocomplete_folder_path: false,
            devtools: false,
            keycloak_signout: false,
            folder_based_template_selection: false,
            nodeFeatures: {},
            content_staging: false,
        } as FeaturesState);
    });

    it('setFeature() works', () => {
        appState.dispatch(new SetFeatureAction(Feature.nice_urls, true));
        expect(appState.now.features.nice_urls).toBe(true);
    });

    it('setNodeFeatures() works', () => {
        appState.dispatch(new SetNodeFeaturesAction(4711, [ NodeFeature.contentAutoOffline ]));
        expect(appState.now.features.nodeFeatures).toEqual({ 4711: [ NodeFeature.contentAutoOffline ] });
    });

    it('feature definitions are congruent with feature state', () => {
        const featureEnumKeys: string[] = getOrderedObjectKeys(Feature);
        const appStateFeaturesKeys: string[] = getOrderedObjectKeys(appState.now.features);
        expect(featureEnumKeys).toEqual(appStateFeaturesKeys);
    });

});

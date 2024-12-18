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
        const expected: FeaturesState = {
            [Feature.NICE_URLS]: false,
            [Feature.ELASTICSEARCH]: false,
            [Feature.ALWAYS_LOCALIZE]: false,
            [Feature.RECENT_ITEMS]: false,
            [Feature.FOCAL_POINT_EDITING]: false,
            [Feature.HIDE_MANUAL]: false,
            [Feature.IMAGE_MANIPULATION2]: false,
            [Feature.ENABLE_UPLOAD_IN_TAGFILL]: false,
            [Feature.TAGFILL_LIGHT]: true,
            [Feature.WASTEBIN]: false,
            [Feature.PUB_DIR_SEGMENT]: false,
            [Feature.USERSNAP]: false,
            [Feature.AUTOCOMPLETE_FOLDER_PATH]: false,
            [Feature.DEVTOOLS]: false,
            [Feature.KEYCLOAK]: false,
            [Feature.KEYCLOAK_SIGNOUT]: false,
            [Feature.FOLDER_BASED_TEMPLATE_SELECTION]: false,
            [Feature.CONTENT_STAGING]: false,
            [Feature.MULTICHANNELLING]: false,
            [Feature.MESH_CR]: false,
            [Feature.OBJECT_TAG_SYNC]: false,
            [Feature.LINK_CHECKER]: false,

            nodeFeatures: {},
        };
        expect(appState.now.features).toEqual(expected);
    });

    it('setFeature() works', () => {
        appState.dispatch(new SetFeatureAction(Feature.NICE_URLS, true));
        expect(appState.now.features.nice_urls).toBe(true);
    });

    it('setNodeFeatures() works', () => {
        appState.dispatch(new SetNodeFeaturesAction(4711, [ NodeFeature.CONTENT_AUTO_OFFLINE ]));
        expect(appState.now.features.nodeFeatures).toEqual({ 4711: [ NodeFeature.CONTENT_AUTO_OFFLINE ] });
    });

    it('feature definitions are congruent with feature state', () => {
        const featureEnumKeys: string[] = Object.keys(Feature)
            .filter(key => key !== 'nodeSettings')
            .map(key => Feature[key])
            .sort();
        const appStateFeaturesKeys: string[] = getOrderedObjectKeys(appState.now.features);
        expect(featureEnumKeys).toEqual(appStateFeaturesKeys);
    });

});

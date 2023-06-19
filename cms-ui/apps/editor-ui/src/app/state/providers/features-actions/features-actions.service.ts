import { Injectable } from '@angular/core';
import { Feature, NodeFeature } from '@gentics/cms-models';
import { FeaturesState } from '../../../common/models/features-state';
import { Api } from '../../../core/providers/api/api.service';
import { ApplicationStateService } from '../application-state/application-state.service';
import { SetFeatureAction, SetNodeFeaturesAction } from '../../modules';

@Injectable()
export class FeaturesActionsService {

    constructor(
        private appState: ApplicationStateService,
        private api: Api,
    ) {}

    /**
     * Check the activation status of all features. Note that only a subset of total CMS features are
     * checked in the UI.
     */
    checkAll(): void {
        const allFeatures: FeaturesState = {
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
            [Feature.KEYCLOAK_SIGNOUT]: false,
            [Feature.FOLDER_BASED_TEMPLATE_SELECTION]: false,
            [Feature.CONTENT_STAGING]: false,
            [Feature.MULTICHANNELLING]: false,
            [Feature.MESH_CR]: false,
            [Feature.OBJECT_TAG_SYNC]: false,

            nodeFeatures: {},
        };
        Object.keys(allFeatures).forEach((key: keyof FeaturesState) => {
            if (key !== 'nodeFeatures') {
                this.checkFeature(key);
            }
        });
    }

    /**
     * Checks the CMS for whether the given feature is activated, and updates the app state with the result.
     */
    checkFeature(key: Feature): Promise<boolean> {
        return this.api.admin.getFeature(key)
            .toPromise()
            .then(response => {
                if (response) {
                    this.appState.dispatch(new SetFeatureAction(key, response.activated));
                }
                return !!response && response.activated;
            });
    }

    /**
     * Loads the activated node features for the specified node and updates the app state with the result.
     */
    loadNodeFeatures(nodeId: number): Promise<NodeFeature[]> {
        return this.api.folders.getNodeFeatures(nodeId).toPromise().then(response => {
            if (response && response.features) {
                this.appState.dispatch(new SetNodeFeaturesAction(nodeId, response.features));
            }
            return response.features;
        });
    }
}

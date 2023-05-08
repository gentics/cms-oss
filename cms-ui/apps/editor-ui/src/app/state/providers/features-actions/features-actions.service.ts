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
        };
        Object.keys(allFeatures).forEach((key: keyof FeaturesState) => {
            if (key !== 'nodeFeatures') {
                this.checkFeature(Feature[key]);
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

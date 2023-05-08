import { Feature, NodeFeature } from '@gentics/cms-models';


/** Used to indicate which features are enabled. */
export type FeaturesInfo = { [key in Feature]: boolean };

/**
 * Exposes the state of a subset of the CMS features listed at
 * https://www.gentics.com/Content.Node/guides/feature_overview.html
 *
 * Note that the naming convention of the state keys differs from the rest of the state,
 * since they reflect the keys used by the CMS.
 */
export interface FeaturesState extends FeaturesInfo {
    nice_urls: boolean;
    elasticsearch: boolean;
    always_localize: boolean;
    recent_items: boolean;
    focal_point_editing: boolean;
    imagemanipulation2: boolean;
    enable_image_upload_in_tagfill: boolean;
    autocomplete_folder_path: boolean;
    devtools: boolean;

    /**
     * If this feature is activated, it is allowed to define how the publish directories of objects in folders are constructed.
     * I. e. in modal `CreateFolderModal` input `directory`'s string will be interpreted as an URL path segment to the publishing portal.
     * @see https://www.gentics.com/Content.Node/guides/feature_pub_dir_segment.html
     */
    pub_dir_segment: boolean;

    /**
     * Contains an array of activated features per node.
     */
    nodeFeatures: { [id: number]: NodeFeature[] };
}

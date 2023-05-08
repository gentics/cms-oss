export type Features = Record<Feature, string>;
export type NodeFeatures = Record<NodeFeature, string>;

/**
 * Contains a subset of the available CMS features listed at
 * https://www.gentics.com/Content.Node/guides/feature_overview.html
 *
 * For compatibility with the current FeaturesState implementation in the GCMS UI, the names use kebap_case.
 */
export enum Feature {
    autocomplete_folder_path = 'autocomplete_folder_path',
    nice_urls = 'nice_urls',
    elasticsearch = 'elasticsearch',
    always_localize = 'always_localize',
    hide_manual = 'hide_manual',
    recent_items = 'recent_items',
    focal_point_editing = 'focal_point_editing',
    imagemanipulation2 = 'imagemanipulation2',
    enable_image_upload_in_tagfill = 'enable_image_upload_in_tagfill',
    tagfill_light = 'tagfill_light',
    wastebin = 'wastebin',
    devtools = 'devtools',
    /**
     * If this feature is activated, it is allowed to define how the publish directories of objects in folders are constructed.
     * I. e. in modal `CreateFolderModal` input `directory`'s string will be interpreted as an URL path segment to the publishing portal.
     * @see https://www.gentics.com/Content.Node/guides/feature_pub_dir_segment.html
     */
    pub_dir_segment = 'pub_dir_segment',
    usersnap = 'usersnap',
    keycloak_signout = 'keycloak_signout',
    folder_based_template_selection = 'folder_based_template_selection',
    content_staging = 'content_staging',
}

/**
 * Contains a subset of the available node features listed at
 * https://www.gentics.com/Content.Node/guides/restapi/json_NodeFeature.html
 */
export enum NodeFeature {
    /**
     * If this feature is activated, buttons for uploading images/files to CMS will
     * display multiple asset sources configured in `$NODE_SETTINGS -> "asset_management"`.
     * @see https://gentics.com/Content.Node/guides/asset_management.html
     */
    assetManagement = 'asset_management',
    /**
     * If this feature is activated, images and files that are not used by other objects (pages or folders), will not be published
     */
    contentAutoOffline = 'contentfile_auto_offline',

    /**
     * If this feature is activated, the new TagEditor will be used for editing tags in this node (except if disabled on a specific TagType).
     */
    newTagEditor = 'new_tageditor',

    /**
     * If this feature is activated, the link checker will be activated on this node.
     */
    linkChecker = 'link_checker',

    /**
     * If this feature is activated, the backend will not ask if it should localize or edit the inherited object, but it will just localize it.
     */
    alwaysLocalize = 'always_localize',

    /**
     * If this feature is activated, during instant publishing, pages will not be removed from the content repository.
     */
    disableInstantDelete = 'disable_instant_delete',

    /**
     * If this feature is activated, the startpage of parentfolders of instant published pages
     * will also be published during instant publish runs.
     */
    publishFolderStartPage = 'publish_folder_startpage',

    /**
     * If this feature is activated live urls will be shown for objects in the node
     */
    liveUrlsPerNode = 'live_urls_per_node',

    /**
     * If this feature is activated entity `form` is available.
     */
    forms = 'forms',

}

/**
 * Model of a node specific feature.
 */
export interface NodeFeatureModel {

    /** Feature ID */
    id: NodeFeature;

    /** Feature name (translated) */
    name: string;

    /** Feature description (translated) */
    description: string;

}

export type Features = Record<Feature, string>;
export type NodeFeatures = Record<NodeFeature, string>;

/**
 * Contains a subset of the available CMS features listed at
 * https://www.gentics.com/Content.Node/guides/feature_overview.html
 *
 * For compatibility with the current FeaturesState implementation in the GCMS UI, the names use kebap_case.
 */
export enum Feature {
    AUTOCOMPLETE_FOLDER_PATH = 'autocomplete_folder_path',
    NICE_URLS = 'nice_urls',
    ELASTICSEARCH = 'elasticsearch',
    ALWAYS_LOCALIZE = 'always_localize',
    HIDE_MANUAL = 'hide_manual',
    RECENT_ITEMS = 'recent_items',
    FOCAL_POINT_EDITING = 'focal_point_editing',
    IMAGE_MANIPULATION2 = 'imagemanipulation2',
    ENABLE_UPLOAD_IN_TAGFILL = 'enable_image_upload_in_tagfill',
    TAGFILL_LIGHT = 'tagfill_light',
    WASTEBIN = 'wastebin',
    DEVTOOLS = 'devtools',
    /**
     * If this feature is activated, it is allowed to define how the publish directories of objects in folders are constructed.
     * I. e. in modal `CreateFolderModal` input `directory`'s string will be interpreted as an URL path segment to the publishing portal.
     *
     * @see https://www.gentics.com/Content.Node/guides/feature_pub_dir_segment.html
     */
    PUB_DIR_SEGMENT = 'pub_dir_segment',
    USERSNAP = 'usersnap',
    KEYCLOAK_SIGNOUT = 'keycloak_signout',
    FOLDER_BASED_TEMPLATE_SELECTION = 'folder_based_template_selection',
    CONTENT_STAGING = 'content_staging',
    MULTICHANNELLING = 'multichannelling',
    MESH_CR = 'mesh_contentrepository',
    OBJECT_TAG_SYNC = 'objtag_sync',
}

/**
 * Contains a subset of the available node features listed at
 * https://www.gentics.com/Content.Node/guides/restapi/json_NodeFeature.html
 */
export enum NodeFeature {
    /**
     * If this feature is activated, buttons for uploading images/files to CMS will
     * display multiple asset sources configured in `$NODE_SETTINGS -> "asset_management"`.
     *
     * @see https://gentics.com/Content.Node/guides/asset_management.html
     */
    ASSET_MANAGEMENT = 'asset_management',
    /**
     * If this feature is activated, images and files that are not used by other objects (pages or folders), will not be published
     */
    CONTENT_AUTO_OFFLINE = 'contentfile_auto_offline',

    /**
     * If this feature is activated, the link checker will be activated on this node.
     */
    LINK_CHECKER = 'link_checker',

    /**
     * If this feature is activated, the backend will not ask if it should localize or edit the inherited object, but it will just localize it.
     */
    ALWAYS_LOCALIZE = 'always_localize',

    /**
     * If this feature is activated, during instant publishing, pages will not be removed from the content repository.
     */
    DISABLE_INSTANT_DELETE = 'disable_instant_delete',

    /**
     * If this feature is activated, the startpage of parentfolders of instant published pages
     * will also be published during instant publish runs.
     */
    PUBLISH_FOLDER_STARTPAGE = 'publish_folder_startpage',

    /**
     * If this feature is activated live urls will be shown for objects in the node
     */
    LIVE_URLS_PER_NODE = 'live_urls_per_node',

    /**
     * If this feature is activated entity `form` is available.
     */
    FORMS = 'forms',

    /**
     * If this feature is activated, uploaded images are automatically converted to WebP.
     */
    WEBP_CONVERSION = 'webp_conversion',

    /*
     * If this feature is activated a modal dialog with the files properties is opened immediately after the upload.
     */
    UPLOAD_FILE_PROPERTIES = 'upload_file_properties',

    /**
     * If this feature is activated a modal dialog with the images properties is opened immediately after the upload.
     */
    UPLOAD_IMAGE_PROPERTIES = 'upload_image_properties',
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

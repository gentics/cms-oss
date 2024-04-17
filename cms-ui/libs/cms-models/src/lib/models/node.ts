import { Item } from './item';
import { DefaultModelType, ModelType } from './type-util';

/**
 * A Node object as returned from the node/load/{id} endpoints:
 * http://www.gentics.com/Content.Node/guides/restapi/resource_NodeResource.html#path__node_load_-id-.html
 * https://www.gentics.com/Content.Node/guides/restapi/json_Node.html
 */
export interface Node<T extends ModelType = DefaultModelType> extends Item<T> {

    type: 'node' | 'channel';

    /** Publish directory for binaries */
    binaryPublishDir: string;

    /** ID of the assigned content-repository */
    contentRepositoryId: number;

    /** Name of the content-repository */
    contentRepositoryName?: string;

    /** Default File Upload Folder ID */
    defaultFileFolderId?: number;

    /** Default Image Upload Folder ID */
    defaultImageFolderId?: number;

    /** True if publishing content modifications is disabled */
    disablePublish: boolean;

    /**
     * Editor to be used in the node.
     * Possible values are 0 for LiveEditor and 1 for Aloha editor.
     *
     * @deprecated LiveEditor is no longer supported.
     */
    editorVersion: number;

    /** ID of the root folder */
    folderId: number;

    /** Hostname for publishing into the Filesystem */
    host: string;

    /** Property of the hostname for publishing into the Filesystem */
    hostProperty: string;

    /** True if secure https is enabled for this node */
    https: boolean;

    /**
     * ID of the node or channel from which this node is inherited.
     *
     * The ID will point to the node itself if there is no parent node.
     *
     * To understand the difference between `masterNodeId` and `inheritedFromId`,
     * consider the following node hierarchy example:
     * - `A` (root master node => `A.masterNodeId = A.id` and `A.inheritedFromId = A.id`)
     * - `B` (channel derived from node A => `B.masterNodeId = A.id` and `B.inheritedFromId = A.id`)
     * - `C` (channel derived from channel B => `C.masterNodeId = A.id` and `C.inheritedFromId = B.id`)
     */
    inheritedFromId: number;

    /** IDs of the languages enabled on this node */
    languagesId: number[];

    /**
     * The ID of the root master node of this node.
     *
     * The ID will point to the node itself if there is no specific master.
     *
     * To understand the difference between `masterNodeId` and `inheritedFromId`,
     * consider the following node hierarchy example:
     * - `A` (root master node => `A.masterNodeId = A.id` and `A.inheritedFromId = A.id`)
     * - `B` (channel derived from node A => `B.masterNodeId = A.id` and `B.inheritedFromId = A.id`)
     * - `C` (channel derived from channel B => `C.masterNodeId = A.id` and `C.inheritedFromId = B.id`)
     */
    masterNodeId: number;

    /** The name of the master node. */
    masterName?: string;

    /**
     * True if the node shall publish into a contentmap
     * (if a contentRepository is assigned)
     */
    publishContentMap: boolean;

    /** True if to publish files to the content repository */
    publishContentMapFiles: boolean;

    /** True if to publish folders to the content repository */
    publishContentMapFolders: boolean;

    /** True if to publish pages to the content repository */
    publishContentMapPages: boolean;

    /** Publish directory */
    publishDir: string;

    /** True if the node shall publish into the filesystem */
    publishFs: boolean;

    /** True if the node shall publish files into the file system */
    publishFsFiles: boolean;

    /** True if the node shall publish pages into the file system */
    publishFsPages: boolean;

    /** How URLs are rendered for files in this node */
    urlRenderWayFiles: number;

    /** How URLs are rendered for pages in this node */
    urlRenderWayPages: number;

    /**
     * True if the node content should be encoded in UTF8
     *
     * @deprecated No longer used since Aloha editor requires UTF-8.
     */
    utf8: boolean;

    /**
     * If global feature "pub_dir_segment" is activated, node will have this property.
     * Is enabled per node.
     *
     * @see https://www.gentics.com/Content.Node/guides/feature_pub_dir_segment.html
     */
    pubDirSegment?: boolean;

    /**
     * True, if GIS image references should be looked through the node content, and their variants should be created in Mesh.
     */
    publishImageVariants: boolean;

    /** Preview URL of Mesh Portal */
    meshPreviewUrl: string;

    /** Preview URL property of Mesh Portal */
    meshPreviewUrlProperty: string;

    /** Mesh Project, this node publishes into */
    meshProject: string;

    /** Whether insecure connections to the preview URL are allowed */
    insecurePreviewUrl: boolean;

    /** Omit page-file HTML extension in page URL path */
    omitPageExtension: boolean;

    /** Language code modes */
    pageLanguageCode: NodePageLanguageCode;
}

export enum NodePageLanguageCode {
    /** The language code will be in the filename. */
    FILENAME = 'FILENAME',
    /** The language code will be at the beginning of the path. */
    PATH = 'PATH',
    /** There will be no language code. */
    NONE = 'NONE',
}

/**
 * Possible Nostname Type values
 */
export enum NodeHostnameType {
    /** The hostname is set as value */
    VALUE = 'value',
    /** The hostname is set as property */
    PROPERTY = 'property',
}

export const NODE_HOSTNAME_PROPERTY_PREFIX = 'NODE_HOST';

/**
 * Possible PreviewUrl Type values
 */
export enum NodePreviewurlType {
    /** The preview URL is set as value */
    VALUE = 'value',
    /** The preview URL is set as property */
    PROPERTY = 'property',
}

export const NODE_PREVIEW_URL_PROPERTY_PREFIX = 'NODE_PREVIEWURL';

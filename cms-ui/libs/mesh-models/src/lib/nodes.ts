/* eslint-disable @typescript-eslint/naming-convention */
import { BasicListOptions, PagingMetaInfo, PermissionInfo } from './common';
import { FieldMap } from './fields';
import { ProjectReference } from './projects';
import { SchemaReference } from './schemas';
import { TagReference } from './tags';
import { UserReference } from './users';

export interface NodeChildrenInfo {
    /** Count of children which utilize the schema. */
    count: number;
    /** Reference to the schema of the node child */
    schemaUuid: string;
}

export interface NodeCreateRequest {
    /** Dynamic map with fields of the node language specific content. */
    fields: FieldMap;
    /** ISO 639-1 language tag of the node content. */
    language: string;
    /** The project root node. All futher nodes are children of this node. */
    parentNode: NodeReference;
    /**
     * Reference to the schema of the root node. Creating a project will also
     * automatically create the base node of the project and link the schema to the
     * initial branch  of the project.
     */
    schema: SchemaReference;
    /** List of tags that should be used to tag the node. */
    tags?: TagReference[];
}

export interface NodeListOptions extends BasicListOptions {
    /**
     * The role query parameter take a UUID of a role and may be used to add permission information to the response,
     * via the rolePerm property which lists the permissions for the specified role on the element.
     * This may be useful when you are logged in as admin but you want to retrieve the editor role permissions on a given node.
     */
    role?: string;
    /**
     * Stored mesh links will automatically be resolved and replaced by the resolved webroot link.
     * With the parameter set the path property as well as the languagesPath property (for available language variants),
     * will be included in the response.
     * Gentics Mesh links in any HTML-typed field will automatically be resolved and replaced by the resolved WebRoot path.
     * No resolving occurs if no link has been specified.
     */
    resolveLinks?: 'short' | 'medium' | 'full';
    /**
     * ISO 639-1 language tag of the language which should be loaded.
     * Fallback handling can be applied by specifying multiple languages in a comma-separated list.
     * The first matching language will be returned.
     * If omitted or the requested language is not available then the defaultLanguage as configured in `mesh.yml` will be returned.
     */
    lang?: string;
    /**
     * Specifies the version to be loaded. Can either be published/draft or version number. e.g.: `0.1`, `1.0`, `draft`, `published`.
     */
    version?: 'draft' | 'published' | string;
}

export interface NodeListResponse {
    /** Paging information of the list result. */
    _metainfo: PagingMetaInfo;
    /** Array which contains the found elements. */
    data: NodeResponse[];
}

/** The project root node. All futher nodes are children of this node. */
export interface NodeReference {
    /**
     * Optional display name of the node. A display field must be set in the schema in
     * order to populate this property.
     */
    displayName?: string;
    /**
     * Webroot path of the node. The path property will only be provided if the
     * resolveLinks query parameter has been set.
     */
    path?: string;
    /** Name of the project to which the node belongs */
    projectName: string;
    /**
     * Reference to the schema of the root node. Creating a project will also
     * automatically create the base node of the project and link the schema to the
     * initial branch  of the project.
     */
    schema: SchemaReference;
    /** Uuid of the node */
    uuid: string;
}

export interface NodeResponse {
    /** Map of languages for which content is available and their publish status. */
    availableLanguages: {
        [languageCode: string]: PublishStatusModel;
    };
    /**
     * List of nodes which construct the breadcrumb. Note that the start node will not
     * be included in the list.
     */
    breadcrumb: NodeReference[];
    /** Object which contains information about child elements. */
    childrenInfo?: {
        [schemaName: string]: NodeChildrenInfo;
    };
    /**
     * Flag which indicates whether the node is a container and can contain nested
     * elements.
     */
    container: boolean;
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: UserReference;
    /**
     * Display field name of the node. May not be retured if the node schema has no
     * display field.
     */
    displayField?: string;
    /**
     * Display field value of the node. May not be retured if the node schema has no
     * display field.
     */
    displayName?: string;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: UserReference;
    /** Dynamic map with fields of the node language specific content. */
    fields: FieldMap;
    /** ISO 639-1 language tag of the node content. */
    language?: string;
    /**
     * Map of webroot paths per language. This property will only be populated if the
     * resolveLinks query parameter has been set accordingly.
     */
    languagePaths?: {
        [languageCode: string]: string;
    };
    /** The project root node. All futher nodes are children of this node. */
    parentNode: NodeReference | null;
    /**
     * Webroot path to the node content. Will only be provided if the resolveLinks query
     * parameter has been set accordingly.
     */
    path?: string;
    permissions: PermissionInfo;
    /** Reference to the project of the node. */
    project: ProjectReference;
    rolePerms: PermissionInfo;
    /**
     * Reference to the schema of the root node. Creating a project will also
     * automatically create the base node of the project and link the schema to the
     * initial branch  of the project.
     */
    schema: SchemaReference;
    /** List of tags that were used to tag the node. */
    tags: TagReference[];
    /** Uuid of the element */
    uuid: string;
    /** Version of the node content. */
    version: string;
}

export interface NodeUpdateRequest {
    /** Dynamic map with fields of the node language specific content. */
    fields: FieldMap;
    /** ISO 639-1 language tag of the node content. */
    language: string;
    /** List of tags that should be used to tag the node. */
    tags?: TagReference[];
    /**
     * Version number which can be provided in order to handle and detect concurrent
     * changes to the node content.
     */
    version?: string;
}

export interface NodeDeleteOptions {
    /** Specifiy whether deletion should also be applied recursively. */
    recursive?: boolean;
}

export interface NodeVersionsResponse {
    versions?: {
        [languageCode: string]: VersionInfo[];
    };
}

export interface VersionInfo {
    /** Is the version used as a root version in another branch? */
    branchRoot: boolean;
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: UserReference;
    /** Is the content a draft version? */
    draft: boolean;
    /** Is the content published version? */
    published: boolean;
    /** Version of the content. */
    version: string;
}

export interface PublishOptions {
    /** Specifiy whether the invoked action should be applied recursively. */
    recursive: boolean;
}

export interface PublishStatusModel {
    /** ISO8601 formatted publish date string. */
    publishDate?: string;
    /** Flag which indicates whether the content is published. */
    published: boolean;
    /** User reference of the creator of the element. */
    publisher: UserReference;
    /** Version number. */
    version: string;
}

export interface PublishStatusResponse {
    /** Map of publish status entries per language */
    availableLanguages?: {
        [languageCode: string]: PublishStatusModel;
    };
}

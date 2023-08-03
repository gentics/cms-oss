/* eslint-disable @typescript-eslint/naming-convention */
import { BasicListOptions, ElasticSearchSettings, GenericMessageResponse, PagingMetaInfo, PermissionInfo } from './common';
import { FieldSchema } from './fields';
import { UserReference } from './users';

export interface SchemaChange {
    /** Type of operation for this change */
    operation?: string;
    properties?: { [key: string]: any };
    /** Uuid of the change entry */
    uuid?: string;
}

export interface SchemaChanges {
    changes?: SchemaChange[];
}

export interface SchemaCreateRequest {
    /**
     * Auto purge flag of the schema. Controls whether contents of this schema should
     * create new versions.
     */
    autoPurge?: boolean;
    /**
     * Flag which indicates whether nodes which use this schema store additional child
     * nodes.
     */
    container?: boolean;
    /** Description of the schema */
    description?: string;
    /** Name of the display field. */
    displayField?: string;
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: ElasticSearchSettings;
    /** List of schema fields */
    fields?: FieldSchema[];
    /** Name of the schema */
    name: string;
    /**
     * Name of the segment field. This field is used to construct the webroot path to
     * the node.
     */
    segmentField?: string;
    /**
     * Names of the fields which provide a compete url to the node. This property can be
     * used to define custom urls for certain nodes. The webroot API will try to locate
     * the node via it's segment field and via the specified url fields.
     */
    urlFields?: string[];
}

export interface SchemaListOptions extends BasicListOptions { }

export interface SchemaListResponse {
    /** Paging information of the list result. */
    _metainfo: PagingMetaInfo;
    /** Array which contains the found elements. */
    data: SchemaResponse[];
}

/**
 * Reference to the schema of the root node. Creating a project will also
 * automatically create the base node of the project and link the schema to the
 * initial branch  of the project.
 */
export interface SchemaReference {
    name?: string;
    set?: boolean;
    uuid?: string;
    version?: string;
    versionUuid?: string;
}

export interface SchemaResponse {
    /**
     * Auto purge flag of the schema. Controls whether contents of this schema should be
     * automatically purged on update.
     */
    autoPurge?: boolean;
    /**
     * Flag which indicates whether nodes which use this schema store additional child
     * nodes.
     */
    container: boolean;
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: UserReference;
    /** Description of the schema. */
    description?: string;
    /** Name of the display field. */
    displayField: string;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: UserReference;
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: ElasticSearchSettings;
    /** List of schema fields */
    fields: FieldSchema[];
    /** Name of the schema. */
    name: string;
    permissions: PermissionInfo;
    rolePerms: PermissionInfo;
    /**
     * Name of the segment field. This field is used to construct the webroot path to
     * the node.
     */
    segmentField?: string;
    /**
     * Names of the fields which provide a compete url to the node. This property can be
     * used to define custom urls for certain nodes. The webroot API will try to locate
     * the node via it's segment field and via the specified url fields.
     */
    urlFields?: string[];
    /** Uuid of the element */
    uuid: string;
    /** Version of the schema. */
    version: string;
}

export interface SchemaUpdateRequest {
    /**
     * Auto purge flag of the schema. Controls whether contents of this schema should
     * create new versions.
     */
    autoPurge?: boolean;
    /**
     * Flag which indicates whether nodes which use this schema store additional child
     * nodes.
     */
    container?: boolean;
    /** New description of the schema. */
    description?: string;
    /** Name of the display field. */
    displayField?: string;
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: ElasticSearchSettings;
    /** List of schema fields */
    fields: FieldSchema[];
    /** Name of the schema. */
    name: string;
    /**
     * Name of the segment field. This field is used to construct the webroot path to
     * the node.
     */
    segmentField?: string;
    /**
     * Names of the fields which provide a compete url to the node. This property can be
     * used to define custom urls for certain nodes. The webroot API will try to locate
     * the node via it's segment field and via the specified url fields.
     */
    urlFields?: string[];
    /** Version of the schema. */
    version?: string;
}

export interface SchemaValidationResponse {
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: ElasticSearchSettings;
    message?: GenericMessageResponse;
    /** Status of the validation. */
    status: string;
}

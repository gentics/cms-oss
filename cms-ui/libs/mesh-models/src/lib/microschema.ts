/* eslint-disable @typescript-eslint/naming-convention */
import { BasicListOptions, ElasticSearchSettings, PagingMetaInfo, PermissionInfo } from './common';
import { FieldSchema } from './fields';
import { UserReference } from './users';

export interface MicroschemaCreateRequest {
    /** Description of the microschema */
    description?: string;
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: ElasticSearchSettings;
    /** List of microschema fields */
    fields?: FieldSchema[];
    /** Name of the microschema */
    name: string;
}

export interface MicroschemaListOptions extends BasicListOptions { }

export interface MicroschemaListResponse {
    /** Paging information of the list result. */
    _metainfo: PagingMetaInfo;
    /** Array which contains the found elements. */
    data: MicroschemaResponse[];
}

export interface MicroschemaResponse {
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: UserReference;
    /** Description of the microschema */
    description?: string;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: UserReference;
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: ElasticSearchSettings;
    /** List of microschema fields */
    fields: FieldSchema[];
    /** Name of the microschema */
    name: string;
    permissions: PermissionInfo;
    rolePerms: PermissionInfo;
    /** Uuid of the element */
    uuid: string;
    /** Version of the microschema */
    version: string;
}

export interface MicroschemaUpdateRequest {
    /** Description of the microschema */
    description?: string;
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: ElasticSearchSettings;
    /** List of microschema fields */
    fields?: FieldSchema[];
    /** Name of the microschema */
    name?: string;
    /** Version of the microschema */
    version?: string;
}

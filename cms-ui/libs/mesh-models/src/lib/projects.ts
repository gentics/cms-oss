/* eslint-disable @typescript-eslint/naming-convention */
import { BasicListOptions, PagingMetaInfo, PermissionInfo } from './common';
import { NodeReference } from './nodes';
import { SchemaReference } from './schemas';
import { UserReference } from './users';

export interface ProjectCreateRequest {
    /**
     * The hostname of the project can be used to generate links across multiple
     * projects. The hostname will be stored along the initial branch of the project.
     */
    hostname?: string;
    /** Name of the project */
    name: string;
    /** Optional path prefix for webroot path and rendered links. */
    pathPrefix?: string;
    /**
     * Reference to the schema of the root node. Creating a project will also
     * automatically create the base node of the project and link the schema to the
     * initial branch  of the project.
     */
    schema: SchemaReference;
    /**
     * SSL flag of the project which will be used to generate links across multiple
     * projects. The flag will be stored along the intial branch of the project.
     */
    ssl?: boolean;
}

export interface ProjectListOptions extends BasicListOptions { }

export interface ProjectListResponse {
    /** Paging information of the list result. */
    _metainfo: PagingMetaInfo;
    /** Array which contains the found elements. */
    data: ProjectResponse[];
}

/** Reference to the project of the node. */
export interface ProjectReference {
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
}

export interface ProjectResponse {
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: UserReference;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: UserReference;
    /** The name of the project. */
    name: string;
    permissions: PermissionInfo;
    rolePerms: PermissionInfo;
    /** The project root node. All futher nodes are children of this node. */
    rootNode: NodeReference;
    /** Uuid of the element */
    uuid: string;
}

export interface ProjectUpdateRequest {
    /** New name of the project */
    name: string;
}

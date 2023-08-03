/* eslint-disable @typescript-eslint/naming-convention */
import { PagingMetaInfo, PermissionInfo } from './common';
import { UserReference } from './users';

export interface TagCreateRequest {
    /** Name of the tag which will be created. */
    name: string;
}

export interface TagFamilyCreateRequest {
    /** Name of the tag family which will be created. */
    name: string;
}

export interface TagFamilyListResponse {
    /** Paging information of the list result. */
    _metainfo: PagingMetaInfo;
    /** Array which contains the found elements. */
    data: TagFamilyResponse[];
}

/** Reference to the tag family to which the tag belongs. */
export interface TagFamilyReference {
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
}

export interface TagFamilyResponse {
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: UserReference;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: UserReference;
    /** Name of the tag family. */
    name?: string;
    permissions: PermissionInfo;
    rolePerms: PermissionInfo;
    /** Uuid of the element */
    uuid: string;
}

export interface TagFamilyUpdateRequest {
    /** New name of the tag family */
    name: string;
}

export interface TagListResponse {
    /** Paging information of the list result. */
    _metainfo: PagingMetaInfo;
    /** Array which contains the found elements. */
    data: TagResponse[];
}

export interface TagListUpdateRequest {
    /**
     * List of tags which should be assigned to the node. Tags which are not included
     * will be removed from the node.
     */
    tags: TagReference[];
}

export interface TagReference {
    /** Name of the referenced element */
    name?: string;
    tagFamily?: string;
    /** Uuid of the referenced element */
    uuid: string;
}

export interface TagResponse {
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: UserReference;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: UserReference;
    /** Name of the tag. */
    name: string;
    permissions: PermissionInfo;
    rolePerms: PermissionInfo;
    /** Reference to the tag family to which the tag belongs. */
    tagFamily: TagFamilyReference;
    /** Uuid of the element */
    uuid: string;
}

export interface TagUpdateRequest {
    /** New name of the tag. */
    name: string;
}

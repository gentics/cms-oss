/* eslint-disable @typescript-eslint/naming-convention */
import { BasicListOptions, ExpandableNode, PagingMetaInfo, PermissionInfo } from './common';
import { GroupReference } from './groups';

export interface UserAPITokenResponse {
    /** Date of the last time the API token was issued. */
    previousIssueDate: string;
    /** Issued client API token. */
    token: string;
}

export interface UserCreateRequest {
    /** Email address of the user. */
    emailAddress?: string;
    /** Firstname of the user. */
    firstname?: string;
    /** When true, the user needs to change their password on the next login. */
    forcedPasswordChange?: boolean;
    /**
     * Optional group id for the user. If provided the user will automatically be
     * assigned to the identified group.
     */
    groupUuid?: string;
    /** Lastname of the user. */
    lastname?: string;
    /**
     * New node reference of the user. This can also explicitly set to null in order to
     * remove the assigned node from the user
     */
    nodeReference?: ExpandableNode;
    /** Password of the new user. */
    password: string;
    /** Username of the user. */
    username: string;
}

export interface UserListOptions extends BasicListOptions { }

export interface UserListResponse {
    /** Paging information of the list result. */
    _metainfo: PagingMetaInfo;
    /** Array which contains the found elements. */
    data: UserResponse[];
}

export interface UserPermissionResponse {
    /** Flag which indicates whether the create permission is granted. */
    create: boolean;
    /** Flag which indicates whether the delete permission is granted. */
    delete: boolean;
    /** Flag which indicates whether the publish permission is granted. */
    publish?: boolean;
    /** Flag which indicates whether the read permission is granted. */
    read: boolean;
    /** Flag which indicates whether the read published permission is granted. */
    readPublished?: boolean;
    /** Flag which indicates whether the update permission is granted. */
    update: boolean;
}

/** User reference of the creator of the element. */
export interface UserReference {
    /** Firstname of the user */
    firstName?: string;
    /** Lastname of the user */
    lastName?: string;
    /** Uuid of the user */
    uuid: string;
}

export interface UserResetTokenResponse {
    /** ISO8601 date of the creation date for the provided token */
    created: string;
    /** JSON Web Token which was issued by the API. */
    token: string;
}

export interface UserResponse {
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: UserReference;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: UserReference;
    /** Email address of the user */
    emailAddress?: string;
    /**
     * Flag which indicates whether the user is enabled or disabled. Disabled users can
     * no longer log into Gentics Mesh. Deleting a user user will not remove it. Instead
     * the user will just be disabled.
     */
    enabled: boolean;
    /** Firstname of the user. */
    firstname?: string;
    /** When true, the user needs to change their password on the next login. */
    forcedPasswordChange: boolean;
    /** List of group references to which the user belongs. */
    groups: GroupReference[];
    /** Lastname of the user. */
    lastname?: string;
    /**
     * New node reference of the user. This can also explicitly set to null in order to
     * remove the assigned node from the user
     */
    nodeReference?: ExpandableNode;
    permissions: PermissionInfo;
    rolePerms: PermissionInfo;
    /** Hashsum of user roles which can be used for user permission caching. */
    rolesHash: string;
    /** Username of the user. */
    username: string;
    /** Uuid of the element */
    uuid: string;
}

export interface UserUpdateRequest {
    /** New email address of the user */
    emailAddress?: string;
    /** New firstname of the user */
    firstname?: string;
    /** When true, the user needs to change their password on the next login. */
    forcedPasswordChange?: boolean;
    /** New lastname of the user */
    lastname?: string;
    /**
     * New node reference of the user. This can also explicitly set to null in order to
     * remove the assigned node from the user
     */
    nodeReference?: ExpandableNode;
    oldPassword?: string;
    /** New password of the user */
    password?: string;
    /** New username of the user */
    username?: string;
}
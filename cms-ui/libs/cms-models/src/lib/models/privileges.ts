import { IndexByKey } from './type-util';
import { GcmsPermission } from './permissions';

/**
 * This file contains Models for privileges as they are returned from the API.
 * For permission interfaces as they are managed in our app, see ../convenience/editor-permissions.ts
 */

/**
 * Privileges assignable to roles.
 */
export enum GcmsRolePrivilege {
    readitems = 'readitems',
    createitems = 'createitems',
    updateitems = 'updateitems',
    deleteitems = 'deleteitems',
    publishpages = 'publishpages',
    translatepages = 'translatepages',
}

/**
 * Maps each `GcmsRolePrivilege` to its value for a user/role.
 */
export type GcmsRolePrivilegeMap = Record<GcmsRolePrivilege, boolean>;

/**
 * Collects all role privilege maps for a user.
 */
export interface GcmsRolePrivilegeMapCollection {

    /** The role privileges granted on pages in a folder. */
    page: GcmsRolePrivilegeMap;

    /**
     * The role privileges granted on certain languages
     * (indexed by language code, e.g., 'en').
     */
    pageLanguages: IndexByKey<GcmsRolePrivilegeMap>;

    /** The role privileges granted on files in a folder. */
    file: Omit<GcmsRolePrivilegeMap, GcmsRolePrivilege.publishpages | GcmsRolePrivilege.translatepages>;

}

/** Maps each privilege to its corresponding GcmsPermission. */
export const GCMS_ROLE_PRIVILEGES_TO_GCMS_PERMISSIONS_MAP: Record<GcmsRolePrivilege, GcmsPermission> = Object.freeze({
    createitems: GcmsPermission.CREATE_ITEMS,
    readitems: GcmsPermission.READ_ITEMS,
    updateitems: GcmsPermission.UPDATE_ITEMS,
    deleteitems: GcmsPermission.DELETE_ITEMS,
    publishpages: GcmsPermission.PUBLISH_PAGES,
    translatepages: null,
});

/**
 * Privilege map as received from the API in Content.Node >= 5.25.0
 * via `/perm/{type}/{id}?map=true` and `/folder/getFolders/{id}?privilegeMap=true`
 * http://www.gentics.com/Content.Node/cmp8/guides/restapi/resource_PermResource.html
 *
 * @deprecated
 */
export interface PrivilegeMapFromServer {
    privileges: FolderPrivileges;
    languages: {
        language: {
            code: string;
            id: number;
            name: string;
        };
        privileges: RolePrivileges;
    }[];
}

/**
 * Privilege map as returned from the API, with languages normalized
 */
export interface PrivilegeMap {
    privileges: FolderPrivileges;
    languages: {
        [languageId: number]: RolePrivileges;
    };
}

/**
 * Privileges a user has on a folder, as returned from the API.
 * Do not use this interface to query user permissions, use {@see EditorPermissions} instead.
 *
 * @deprecated
 */
export interface FolderPrivileges {
    viewfolder: boolean;
    createfolder: boolean;
    updatefolder: boolean;
    deletefolder: boolean;
    assignpermissions: boolean;
    viewpage: boolean;
    createpage: boolean;
    updatepage: boolean;
    deletepage: boolean;
    publishpage: boolean;
    viewfile: boolean;
    createfile: boolean;
    updatefile: boolean;
    deletefile: boolean;
    viewtemplate: boolean;
    createtemplate: boolean;
    linktemplate: boolean;
    updatetemplate: boolean;
    deletetemplate: boolean;
    updatetagtypes: boolean;
    inheritance: boolean;
    importpage: boolean;
    linkworkflow: boolean;
    synchronizechannel: boolean;
    wastebin: boolean;
    translatepage: boolean;
    viewform: boolean;
    createform: boolean;
    updateform: boolean;
    deleteform: boolean;
    publishform: boolean;
    formreport: boolean;
}

/**
 * Name of privileges as returned by the `folder/getFolders/{id}` endpoint when requested with `privileges: true`.
 * http://www.gentics.com/Content.Node/cmp8/guides/restapi/resource_FolderResource.html#path__folder_getFolders_-id-.html
 */
export type PrivilegeFlagName = keyof FolderPrivileges;

/**
 * Privileges a user receives on pages/files/images of a specific language via role privileges, as returned from the API.
 * Do not use this interface to query user permissions, use {@see EditorPermissions} instead.
 *
 * @deprecated
 */
export interface RolePrivileges {
    viewpage: boolean;
    createpage: boolean;
    updatepage: boolean;
    deletepage: boolean;
    publishpage: boolean;
    translatepage: boolean;
    viewfile: boolean;
    createfile: boolean;
    updatefile: boolean;
    deletefile: boolean;
}

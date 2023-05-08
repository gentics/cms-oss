import { Index } from '../../type-util';
import { GcmsRolePrivilegeMapCollection } from './privileges';

/**
 * CMS types, on which permissions can be read/set.
 *
 * For types that can be instantiated multiple times (e.g., folder) the permissions
 * can be set on each instance.
 */
export enum AccessControlledType {
    SETTING = 'setting',
    INBOX = 'inbox',
    TODO_TOOL = 'todotool',
    PUBLISH_QUEUE = 'pubqueue',
    ADMIN = 'admin',
    USER_ADMIN = 'useradmin',
    GROUP_ADMIN = 'groupadmin',
    PERMISSION = 'permission',
    ROLE = 'role',
    AUTO_UPDATE = 'autoupdate',
    CONTENT_ADMIN = 'contentadmin',
    OBJECT_PROPERTY_ADMIN = 'objpropadmin',
    OBJECT_PROPERTY_TYPE = 'objproptype',
    OBJECT_PROPERTY = 'objprop',
    OBJECT_PROPERTY_CATEGORY = 'objpropcategory',
    OBJECT_PROPERTY_MAINTENANCE = 'objpropmaintenance',
    CONSTRUCT = 'construct',
    CONSTRUCT_ADMIN = 'constructadmin',
    CONSTRUCT_CATEGORY_ADMIN = 'constructcategoryadmin',
    CONSTRUCT_CATEGORY = 'constructCategory',
    DATA_SOURCE_ADMIN = 'datasourceadmin',
    DATA_SOURCE = 'datasource',
    LANGUAGE_ADMIN = 'languageadmin',
    MAINTENANCE = 'maintenance',
    CONTENT_MAP_BROWSER = 'contentmapbrowser',
    CONTENT_REPOSITORY_ADMIN = 'contentrepositoryadmin',
    CONTENT_REPOSITORY = 'contentRepository',
    DEVTOOL_ADMIN = 'devtooladmin',
    CR_FRAGMENT_ADMIN = 'crfragmentadmin',
    CR_FRAGMENT = 'crfragment',
    SEARCH_INDEX_MAINTENANCE = 'searchindexmaintenance',
    SYSTEM_MAINTANANCE = 'systemmaintenance',
    BUNDLE_ADMIN = 'bundleadmin',
    BUNDLE_IMPORT = 'bundleimport',
    BUNDLE_EXPORT = 'bundleexport',
    ERROR_LOG = 'errorlog',
    ACTION_LOG = 'actionlog',
    SCHEDULER = 'scheduler',
    TASK_TEMPLATE_ADMIN = 'tasktemplateadmin',
    TASK_TEMPLATE = 'tasktemplate',
    TASK_ADMIN = 'taskadmin',
    TASK = 'task',
    JOB_ADMIN = 'jobadmin',
    JOB = 'job',
    WORKFLOW_ADMIN = 'workflowadmin',
    CUSTOM_TOOL_ADMIN = 'customtooladmin',
    CUSTOM_TOOL = 'customtool',
    CONTENT = 'content',
    NODE = 'node',
    FOLDER = 'folder',
    FORM = 'form',
    TEMPLATE = 'template',
    CONTENT_STAGING_ADMIN = 'contentstagingadmin',
}

/**
 * All permissions available over the GCMS REST API.
 *
 * The privileges in `./privileges.ts` are derived from these permissions by the CMS
 * (a privilege is basically just an alias for a permission on a certain type/instance).
 *
 * @see https://www.gentics.com/Content.Node/guides/restapi/json_PermType.html
 * @see https://git.gentics.com/psc/contentnode/blob/dev/contentnode-restapi/src/main/java/com/gentics/contentnode/rest/model/perm/PermType.java
 */
export enum GcmsPermission {
    VIEW = 'view',
    EDIT = 'edit',
    READ = 'read',
    SET_PERMISSION = 'setperm',
    UPDATE = 'update',
    INSTANT_MESSAGES = 'instantmessages',
    CREATE_USER = 'createuser',
    UPDATE_USER = 'updateuser',
    DELETE_USER = 'deleteuser',
    CREATE_GROUP = 'creategroup',
    UPDATE_GROUP = 'updategroup',
    DELETE_GROUP = 'deletegroup',
    USER_ASSIGNMENT = 'userassignment',
    UPDATE_GROUP_USER = 'updategroupuser',
    SET_USER_PERMISSIONS = 'setuserperm',
    ASSIGN_ROLES = 'assignroles',
    SYSTEM_INFORMATION = 'sysinfo',
    CREATE = 'create',
    DELETE = 'delete',
    CREATE_IMPORT = 'createimport',
    UPDATE_BUNDLE = 'updatebundle',
    DELETE_BUNDLE = 'deletebundle',
    BUILD = 'build',
    UPDATE_IMPORT = 'updateimport',
    CREATE_EXPORT = 'createexport',
    UPDATE_EXPORT = 'updateexport',
    BUILD_EXPORT = 'buildexport',
    SET_BUNDLE_PERMISSIONS = 'setbundleperm',
    EDIT_IMPORT = 'editimport',
    DELETE_ERROR_LOG = 'deleteerrorlog',
    SUSPEND_SCHEDULER = 'suspendscheduler',
    READ_TASK_TEMPLATE = 'readtasktemplates',
    UPDATE_TASK_TEMPLATES = 'updatetasktemplates',
    READ_TASKS = 'readtasks',
    UPDATE_TASKS = 'updatetasks',
    READ_SCHEDULES = 'readschedules',
    UPDATE_SCHEDULES = 'updateschedules',
    READ_JOBS = 'readjobs',
    UPDATE_JOBS = 'updatejobs',
    CREATE_FOLDER = 'createfolder',
    UPDATE_FOLDER = 'updatefolder',
    DELETE_FOLDER = 'deletefolder',
    LINK_OVERVIEW = 'linkoverview',
    CREATE_OVERVIEW = 'createoverview',
    READ_ITEMS = 'readitems',
    CREATE_ITEMS = 'createitems',
    UPDATE_ITEMS = 'updateitems',
    DELETE_ITEMS = 'deleteitems',
    IMPORT_ITEMS = 'importitems',
    TRANSLATE_PAGES = 'translatepages',
    PUBLISH_PAGES = 'publishpages',
    READ_TEMPLATES = 'readtemplates',
    CREATE_TEMPLATES = 'createtemplates',
    UPDATE_TEMPLATES = 'updatetemplates',
    DELETE_TEMPLATES = 'deletetemplates',
    LINK_TEMPLATES = 'linktemplates',
    UPDATE_CONSTRUCTS = 'updateconstructs',
    CHANNEL_SYNC = 'channelsync',
    UPDATE_INHERITANCE = 'updateinheritance',
    WASTE_BIN = 'wastebin',
    CREATE_FORM = 'createform',
    UPDATE_FORM = 'updateform',
    DELETE_FORM = 'deleteform',
    PUBLISH_FORM = 'publishform',
    VIEW_FORM = 'viewform',
    FORM_REPORTS = 'formreport',
    MODIFY_CONTENT = 'modifycontent',
}

export const INVERSE_GCMS_PERMISSIONS = Object.entries(GcmsPermission)
    .reduce((acc, [key, value]) => {
        acc[value] = key;
        return acc;
    }, {});

/**
 * Describes the permissions on a certain `AccessControlledType` or instance of such a type.
 *
 * If a `GcmsPermission` is not contained in the map, it is not applicable to the `AccessControlledType`,
 * for which it represents the permissions.
 */
export type GcmsPermissionsMap = Partial<Index<GcmsPermission, boolean>>;

/**
 * Collects non-language specific and language specific permissions for a user.
 */
export interface PermissionsMapCollection {

    /** Permissions not specific to languages. */
    permissions: GcmsPermissionsMap;

    /**
     * Role specific permissions.
     *
     * This property is only set for `Folder` instances.
     */
    rolePermissions?: GcmsRolePrivilegeMapCollection;

}

export enum SingleInstancePermissionType {
    EDIT = 'edit',
    VIEW = 'view',
    DELETE = 'delete',
}

export type InstancePermissionMap = Record<SingleInstancePermissionType, boolean>;

export interface InstancePermissionItem {
    /** ID of the item */
    id: string | number;

    /** Instance permissions of the item */
    permissions?: InstancePermissionMap;
}

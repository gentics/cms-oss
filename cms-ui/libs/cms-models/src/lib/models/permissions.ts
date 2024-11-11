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
    LICENCE_ADMIN = 'licenseadmin'
}

/**
 * All permissions available over the GCMS REST API.
 *
 * The privileges in `./privileges.ts` are derived from these permissions by the CMS
 * (a privilege is basically just an alias for a permission on a certain type/instance).
 *
 * @see https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_PermType.html
 * @see https://github.com/gentics/cms-oss/blob/master/cms-restapi/src/main/java/com/gentics/contentnode/rest/model/perm/PermType.java
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
export type GcmsPermissionsMap = Partial<Record<GcmsPermission, boolean>>;

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


/**
 * Permissions for items (of any type)
 */
export interface ItemPermissions {
    /** Permission to create an item of the item type */
    create: boolean;

    /** Permission to delete an item of the item type */
    delete: boolean;

    /** Permission to edit an item of the item type */
    edit: boolean;

    /** Permission to inherit folders/items */
    inherit: boolean;

    /** Permission to localize an inherited item of the item type */
    localize: boolean;

    /** Permission to delete the local version of an inherited item of the item type */
    unlocalize: boolean;

    /** Permission to view an item of the item type */
    view: boolean;
}



/**
 * Permissions for folder items, abstracted from the server logic
 */
export interface FolderPermissions extends ItemPermissions {
    /** Permission to create a folder */
    create: boolean;

    /** Permission to delete a folder */
    delete: boolean;

    /** Permission to edit a folder or its properties */
    edit: boolean;

    /** Permission to localize an inherited folder */
    localize: boolean;

    /** Permission to delete the local version of an inherited folder */
    unlocalize: boolean;

    /** Permission to view a folder and its contents */
    view: boolean;
}

/**
 * Permissions for page items, abstracted from the server logic
 */
export interface PagePermissions extends ItemPermissions {
    /** Permission to create a page */
    create: boolean;

    /** Permission to delete a page */
    delete: boolean;

    /** Permission to edit a page */
    edit: boolean;

    /** Permission to import pages */
    import: boolean;

    /** Permission to link templates */
    linkTemplate: boolean;

    /** Permission to localize an inherited page */
    localize: boolean;

    /** Permission to import pages */
    publish: boolean;

    /** Permission to delete the local version of an inherited page */
    unlocalize: boolean;

    /** Permission to translate a page into the currently active language */
    translate: boolean;

    /** Permission to view a page */
    view: boolean;
}

/**
 * Permissions for files, abstracted from the server logic
 */
export interface FilePermissions extends ItemPermissions {
    /** Permission to upload a file */
    create: boolean;

    /** Permission to delete a file */
    delete: boolean;

    /** Permission to edit a file */
    edit: boolean;

    /** Permission to localize an inherited file */
    localize: boolean;

    /** Permission to import files */
    import: boolean;

    /** Permission to delete the local version of an inherited file */
    unlocalize: boolean;

    /** Permission to upload a file */
    upload: boolean;

    /** Permission to view a file */
    view: boolean;
}

/**
 * Permissions for images, abstracted from the server logic
 */
export interface ImagePermissions extends ItemPermissions {
    /** Permission to upload an image */
    create: boolean;

    /** Permission to delete an image */
    delete: boolean;

    /** Permission to edit an image */
    edit: boolean;

    /** Permission to localize an inherited image */
    localize: boolean;

    /** Permission to import images */
    import: boolean;

    /** Permission to delete the local version of an inherited image */
    unlocalize: boolean;

    /** Permission to upload an image */
    upload: boolean;

    /** Permission to view an image */
    view: boolean;
}

/**
 * Permissions for form items, abstracted from the server logic
 */
export interface FormPermissions extends ItemPermissions {
    /** Permission to import forms */
    publish: boolean;
}

/**
 * Permissions for templates, abstracted from the server logic
 */
export interface TemplatePermissions extends ItemPermissions {
    /** Permission to create a template */
    create: boolean;

    /** Permission to delete a template */
    delete: boolean;

    /** Permission to edit a template */
    edit: boolean;

    /** Permission to link a template to a page */
    link: boolean;

    /** Permission to localize an inherited template */
    localize: boolean;

    /** Permission to delete the local version of an inherited template */
    unlocalize: boolean;

    /** Permission to view a template */
    view: boolean;
}

/**
 * Permissions for tag types, abstracted from the server logic
 */
export interface TagTypePermissions {
    /** Permission to create tag types */
    create: boolean;

    /** Permission to edit tag types */
    edit: boolean;

    /** Permission to delete tag types */
    delete: boolean;

    /** Permission to view tag types */
    view: boolean;
}

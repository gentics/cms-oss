import { I18nKey, RequiredInstancePermissions, RequiredTypePermissions } from '@admin-ui/core';
import { InjectionToken } from '@angular/core';
import { IndexByKey } from '@gentics/cms-models';
import { WithOptional } from '../utils';
import { ADMIN_MODULE_ACTION_PERMISSIONS } from './action-permissions/admin-module.action-permissions';
import { CONSTRUCT_MODULE_ACTION_PERMISSIONS } from './action-permissions/construct-module.action-permissions';
import { CONSTRUCTCATEGORY_MODULE_ACTION_PERMISSIONS } from './action-permissions/construct-category-module.action-permissions';
import { CONTENT_MODULE_ACTION_PERMISSIONS } from './action-permissions/content-module.action-permissions';
import { CONTENT_STAGING_MODULE_ACTION_PERMISSIONS } from './action-permissions/content-staging-module.action-permissions';
import { CONTENTREPOSITORY_MODULE_ACTION_PERMISSIONS } from './action-permissions/content-repository-module.action-permissions';
import { CR_FRAGMENTS_MODULE_ACTION_PERMISSIONS } from './action-permissions/cr-fragment-module.action-permissions';
import { DATASOURCE_MODULE_ACTION_PERMISSIONS } from './action-permissions/datasource-module.action-permissions';
import { GLOBAL_MODULE_ACTION_PERMISSIONS } from './action-permissions/global.action-permissions';
import { GROUP_MODULE_ACTION_PERMISSIONS } from './action-permissions/group-module.action-permissions';
import { LANGUAGE_MODULE_ACTION_PERMISSIONS } from './action-permissions/language-module.action-permissions';
import { NODE_MODULE_ACTION_PERMISSIONS } from './action-permissions/node-module.action-permissions';
import { OBJECTPROPERTY_MODULE_ACTION_PERMISSIONS } from './action-permissions/object-property-module.action-permissions';
import { OBJECTPROPERTYCATEGORY_MODULE_ACTION_PERMISSIONS } from './action-permissions/object-property-category-module.action-permissions';
import { ROLE_MODULE_ACTION_PERMISSIONS } from './action-permissions/role-module.action-permissions';
import { SCHEDULER_MODULE_ACTION_PERMISSIONS } from './action-permissions/scheduler-module.action-permissions';
import { TEMPLATE_MODULE_ACTION_PERMISSIONS } from './action-permissions/template-module.action-permission';
import { USER_MODULE_ACTION_PERMISSIONS } from './action-permissions/user-module.action-permissions';

/**
 * Describes permissions that a user needs to have to execute a specific action.
 *
 * To determine the permissions required for a specific action, there unfortunately
 * seems to be no other way than following this procedure:
 *
 * 1. Check the implementation of the respective REST endpoint and find out which `ObjectPermission` is required:
 * https://git.gentics.com/psc/contentnode/tree/dev/contentnode-lib/src/main/java/com/gentics/contentnode/rest/resource/impl
 *
 * 2. The `ObjectPermission` has `checkObject() and `checkClass()` methods, which use the current transation to
 * check permissions using the `PermHandler`. Thus, check which `PermHandler` method is called by the transaction (see class TransactionImpl
 * in https://git.gentics.com/psc/contentnode/blob/dev/contentnode-lib%2Fsrc%2Fmain%2Fjava%2Fcom%2Fgentics%2Fcontentnode%2Ffactory%2FTransactionManager.java).
 *
 * 3. Go to the respective `PermHandler` method
 * (https://git.gentics.com/psc/contentnode/blob/dev/contentnode-lib%2Fsrc%2Fmain%2Fjava%2Fcom%2Fgentics%2Fcontentnode%2Fperm%2FPermHandler.java)
 * and check which integer constants are used for the permissions check (e.g., `checkPermissionBits(Folder.TYPE_FOLDER, object.getId(), PERM_FOLDER_UPDATE);`).
 * Sometimes it is also possible to find out the integer constant using the `PermHandler.resolvePermission()` method.
 *
 * 4. Use this integer value to find out to which `PermType` enum member it maps:
 * https://git.gentics.com/psc/contentnode/blob/dev/contentnode-restapi%2Fsrc%2Fmain%2Fjava%2Fcom%2Fgentics%2Fcontentnode%2Frest%2Fmodel%2Fperm%2FPermType.java
 * The `PermType` member has a corresponding member in the `GcmsPermission` enum in the `@gentics/cms-models` TypeScript package.
 */
export interface CompleteUserActionPermissions {

    /**
     * Type permissions required to execute the action.
     */
    typePermissions: RequiredTypePermissions[];

    /**
     * Instance permissions required to execute the action.
     *
     * The permissions specified here will be combined with the `instanceId` and the `nodeId`
     * supplied at runtime to the `ActionAllowedDirective`.
     * Note that on the contrary to `typePermissions` only a single `RequiredInstancePermissions`
     * object may be specified.
     */
    instancePermissions: Omit<RequiredInstancePermissions, 'instanceId' | 'nodeId'>;

    /**
     * Tooltip to be displayed on the disabled element if the action is not allowed.
     *
     * If this is not set, a default tooltip will be used.
     */
    disabledTooltip?: I18nKey;

}

/**
 * Describes permissions that a user needs to have to execute a specific action.
 *
 * Each `UserActionPermissions` object must have either the `typePermissions` or the `instancePermissions` or both set.
 */
export type UserActionPermissions =
    WithOptional<CompleteUserActionPermissions, 'typePermissions'> | WithOptional<CompleteUserActionPermissions, 'instancePermissions'>;

/** Describes the `UserActionPermissions` required by a single feature module. */
export type SingleModuleUserActionPermissions = IndexByKey<UserActionPermissions>;

/** Describes the `UserActionPermissions` required by multiple modules. */
export type MultiModuleUserActionPermissions = IndexByKey<SingleModuleUserActionPermissions>;

/** InjectionToken for getting the user action permissions defined for the app. */
export const USER_ACTION_PERMISSIONS = new InjectionToken<MultiModuleUserActionPermissions>('USER_ACTION_PERMISSIONS');

/**
 * Contains all UserActionPermissions structured into modules.
 */
export const USER_ACTION_PERMISSIONS_DEF: MultiModuleUserActionPermissions = {
    admin: ADMIN_MODULE_ACTION_PERMISSIONS,
    construct: CONSTRUCT_MODULE_ACTION_PERMISSIONS,
    constructCategory: CONSTRUCTCATEGORY_MODULE_ACTION_PERMISSIONS,
    content_staging: CONTENT_STAGING_MODULE_ACTION_PERMISSIONS,
    contentadmin: CONTENT_MODULE_ACTION_PERMISSIONS,
    contentRepository: CONTENTREPOSITORY_MODULE_ACTION_PERMISSIONS,
    crFragment: CR_FRAGMENTS_MODULE_ACTION_PERMISSIONS,
    dataSource: DATASOURCE_MODULE_ACTION_PERMISSIONS,
    global: GLOBAL_MODULE_ACTION_PERMISSIONS,
    group: GROUP_MODULE_ACTION_PERMISSIONS,
    language: LANGUAGE_MODULE_ACTION_PERMISSIONS,
    node: NODE_MODULE_ACTION_PERMISSIONS,
    objectProperty: OBJECTPROPERTY_MODULE_ACTION_PERMISSIONS,
    objectPropertyCategory: OBJECTPROPERTYCATEGORY_MODULE_ACTION_PERMISSIONS,
    role: ROLE_MODULE_ACTION_PERMISSIONS,
    scheduler: SCHEDULER_MODULE_ACTION_PERMISSIONS,
    template: TEMPLATE_MODULE_ACTION_PERMISSIONS,
    user: USER_MODULE_ACTION_PERMISSIONS,
};

import { RequiredTypePermissions } from '@admin-ui/core';
import { BreadcrumbInfo } from '@admin-ui/core/providers/breadcrumbs/breadcrumb-info';
import { Type } from '@angular/core';
import { ActivatedRouteSnapshot, CanDeactivateFn, Data, DeprecatedGuard, ResolveData, ResolveFn, Route } from '@angular/router';
import { ConstructorOf } from '../utils/util-types/util-types';
import {
    EditableEntity,
    EditableEntityBusinessObjects,
    ROUTE_ENTITY_LOADED,
    ROUTE_ENTITY_RESOLVER_KEY,
    ROUTE_ENTITY_TYPE_KEY,
    ROUTE_IS_EDITOR_ROUTE,
} from './editors';

export const ROUTE_PERMISSIONS_KEY = 'typePermissions';
export const ROUTE_BREADCRUMB_KEY = 'breadcrumb';
export const ROUTE_CHILD_BREADCRUMB_OUTLET_KEY = 'childOutletsForBreadcrumbs';
export const ROUTE_MANAGEMENT_OUTLET = 'management';
export const ROUTE_PARAM_MESH_TAB = 'activeTab';
export const ROUTE_PATH_MESH = 'mesh';
export const ROUTE_SKIP_BREADCRUMB = 'skipBreadcrumb';

// MESH_BROWSER
export const ROUTE_MESH_REPOSITORY_ID = 'repository';
export const ROUTE_MESH_PROJECT_ID = 'project';
export const ROUTE_MESH_BRANCH_ID = 'branch';
export const ROUTE_MESH_PARENT_NODE_ID = 'parent';
export const ROUTE_MESH_CURRENT_NODE_ID = 'node';
export const ROUTE_MESH_LANGUAGE = 'language';
export const ROUTE_DATA_MESH_REPO_ID = 'repoId';
export const ROUTE_DATA_MESH_REPO_ITEM = 'repoItem';

export enum AdminUIModuleRoutes {
    CONSTRUCTS = 'constructs',
    CONTENT_MAINTENANCE = 'content-maintenance',
    CONTENT_STAGING = 'content-staging',
    CONTENT_REPOSITORIES = 'content-repositories',
    CR_FRAGMENTS = 'cr-fragments',
    DATA_SOURCES = 'data-sources',
    FOLDERS = 'folders',
    GROUPS = 'groups',
    LANGUAGES = 'languages',
    LOGIN = 'login',
    LOGS = 'logs',
    MAINTENANCE_MODE = 'maintenance-mode',
    NODES = 'nodes',
    OBJECT_PROPERTIES = 'object-properties',
    DEV_TOOL_PACKAGES = 'devtool-packages',
    ROLES = 'roles',
    SCHEDULER = 'scheduler',
    SEARCH_INDEX_MAINTENANCE = 'search-index-maintenance',
    TEMPLATES = 'templates',
    USERS = 'users',
    MESH_BROWSER =  'mesh-browser',
    LICENSE = 'license',
}

export enum AdminUIEntityDetailRoutes {
    CONSTRUCT = 'construct',
    CONSTRUCT_CATEGORY = 'construct-category',
    CONTENT_PACKAGE = 'content-package',
    CONTENT_REPOSITORY = 'content-repository',
    CR_FRAGMENT = 'cr-fragment',
    DATA_SOURCE = 'data-source',
    DEVTOOL_PACKAGE = 'devtool-package',
    FOLDER = 'folder',
    GROUP = 'group',
    LANGUAGE = 'language',
    NODE = 'node',
    OBJECT_PROPERTY = 'object-property',
    OBJECT_PROPERTY_CATEGORY = 'object-property-category',
    ROLE = 'role',
    SCHEDULE = 'schedule',
    SCHEDULE_TASK = 'task',
    TEMPLATE = 'template',
    USER = 'user',
    MESH_BROWSER_NODE = 'mesh-node',
    MESH_BROWSER_LIST = 'list',
}

/**
 * Defines the data properties, which are available for routes in the Gentics CMS Admininistration User Interface.
 */
export interface RouteData extends Data {

    /**
     * Info for displaying a breadcrumb for this route segment.
     * If this is not set, no breadcrumb is displayed for this segment.
     */
    [ROUTE_BREADCRUMB_KEY]?: BreadcrumbInfo;

    /**
     * Contains the child outlets, which should be included in the breadcrumbs.
     * If this is not set, PRIMARY_OUTLET is used.
     * This property cannot be set through a Resolve<>.
     */
    [ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]?: string | string[];

    /**
     * Describes the type permissions that a user needs to have to be able to access this route.
     * This is required if a `PermissionsGuard` is used.
     */
    [ROUTE_PERMISSIONS_KEY]?: RequiredTypePermissions | RequiredTypePermissions[];

    /** If the route component is a Entity Editor. */
    [ROUTE_IS_EDITOR_ROUTE]?: boolean;

    /** The entity type of the editor, so the entity can be resolved on load. */
    [ROUTE_ENTITY_TYPE_KEY]?: EditableEntity;

    /** If the entity has already been loaded/resolved. */
    [ROUTE_ENTITY_LOADED]?: boolean;
}

type RouteDataResolvers = { [K in keyof RouteData]?: ConstructorOf<{
    resolve: ResolveFn<RouteData[K]>;
}>; };

export interface RouteDataResolve extends ResolveData, RouteDataResolvers {
    // Some properties must be specified statically and cannot be resolved:
    [ROUTE_IS_EDITOR_ROUTE]?: never;
    [ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]?: never;
    [ROUTE_PERMISSIONS_KEY]?: never;
    [ROUTE_ENTITY_RESOLVER_KEY]?: (route: ActivatedRouteSnapshot) => Promise<EditableEntityBusinessObjects[EditableEntity]>;
}

/**
 * The interface used for defining routes in the Gentics CMS Administration User Interface.
 */
export interface GcmsAdminUiRoute<T = any> extends Route {
    component?: Type<T>;
    children?: GcmsAdminUiRoute<any>[];
    data?: RouteData;
    resolve?: RouteDataResolve;
    canDeactivate?: [CanDeactivateFn<T> | DeprecatedGuard];
}

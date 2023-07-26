import { RequiredTypePermissions } from '@admin-ui/core';
import { BreadcrumbInfo } from '@admin-ui/core/providers/breadcrumbs/breadcrumb-info';
import { Data, ResolveData, ResolveFn, Route } from '@angular/router';
import { ConstructorOf } from '../utils/util-types/util-types';

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
    PACKAGES = 'packages',
    ROLES = 'roles',
    SCHEDULER = 'scheduler',
    SEARCH_INDEX_MAINTENANCE = 'search-index-maintenance',
    TEMPLATES = 'templates',
    USERS = 'users',
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
}

/**
 * Defines the data properties, which are available for routes in the Gentics CMS Admininistration User Interface.
 */
export interface RouteData extends Data {

    /**
     * Info for displaying a breadcrumb for this route segment.
     * If this is not set, no breadcrumb is displayed for this segment.
     */
    breadcrumb?: BreadcrumbInfo;

    /**
     * Contains the child outlets, which should be included in the breadcrumbs.
     * If this is not set, PRIMARY_OUTLET is used.
     * This property cannot be set through a Resolve<>.
     */
    childOutletsForBreadcrumbs?: string | string[];

    /**
     * Describes the type permissions that a user needs to have to be able to access this route.
     * This is required if a `PermissionsGuard` is used.
     */
    typePermissions?: RequiredTypePermissions | RequiredTypePermissions[];

}

type RouteDataResolvers = { [K in keyof RouteData]?: ConstructorOf<{
    resolve: ResolveFn<RouteData[K]>;
}>; };

export interface RouteDataResolve extends ResolveData, RouteDataResolvers {
    // Some properties must be specified statically and cannot be resolved:
    childOutletsForBreadcrumbs?: never;
    typePermissions?: never;
}

/**
 * The interface used for defining routes in the Gentics CMS Administration User Interface.
 */
export interface GcmsAdminUiRoute extends Route {
    children?: GcmsAdminUiRoute[];
    data?: RouteData;
    resolve?: RouteDataResolve;
}

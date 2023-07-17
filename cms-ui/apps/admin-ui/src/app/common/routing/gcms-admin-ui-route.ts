import { RequiredTypePermissions } from '@admin-ui/core';
import { BreadcrumbInfo } from '@admin-ui/core/providers/breadcrumbs/breadcrumb-info';
import { Data, ResolveData, Route, ResolveFn } from '@angular/router';

import { ConstructorOf } from '../utils/util-types/util-types';

export enum AdminUIModuleRoutes {
    LOGIN = 'login',
    USERS = 'users',
    GROUPS = 'groups',
    ROLES = 'roles',
    FOLDERS = 'folders',
    NODES = 'nodes',
    LANGUAGES = 'languages',
    CONTENT_REPOSITORIES = 'content-repositories',
    SEARCH_INDEX_MAINTENANCE = 'search-index-maintenance',
    LOGS = 'logs',
    SCHEDULER = 'scheduler',
    CONTENT_MAINTENANCE = 'content-maintenance',
    MAINTENANCE_MODE = 'maintenance-mode',
    CONTENT_STAGING = 'content-staging',
    OBJECT_PROPERTIES = 'object-properties',
    CONSTRUCTS = 'constructs',
    DATA_SOURCES = 'data-sources',
    CR_FRAGMENTS = 'cr-fragments',
    PACKAGES = 'packages',
    TEMPLATES = 'templates',
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

import { PermissionsGuard } from '@admin-ui/core/guards/permissions/permissions.guard';
import { NgModule, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { ROUTE_DETAIL_OUTLET } from './common';
import { AdminUIModuleRoutes, GcmsAdminUiRoute, ROUTE_BREADCRUMB_KEY, ROUTE_CHILD_BREADCRUMB_OUTLET_KEY, ROUTE_PERMISSIONS_KEY } from './common/models/routing';
import { ViewUnauthorizedComponent } from './core/components/view-unauthorized/view-unauthorized.component';
import { AuthGuard } from './core/guards/auth/auth.guard';
import { GenericRouterOutletComponent } from './shared/components/generic-router-outlet/generic-router-outlet.component';
import { SplitViewRouterOutletComponent } from './shared/components/split-view-router-outlet/split-view-router-outlet.component';

const ADMIN_UI_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        canActivate: [(route, router) => inject(AuthGuard).canActivate(route, router)],
        canActivateChild: [(childRouteState) => inject(PermissionsGuard).canActivateChild(childRouteState)],
        component: GenericRouterOutletComponent,
        data: {
            [ROUTE_BREADCRUMB_KEY]: { title: 'dashboard.dashboard' },
        },
        children: [
            {
                path: 'unauthorized',
                pathMatch: 'full',
                component: ViewUnauthorizedComponent,
                data: {
                    // empty array will allow route without any permissions
                    [ROUTE_PERMISSIONS_KEY]: [],
                },
            },
            {
                path: '',
                pathMatch: 'full',
                loadChildren: () => import('./dashboard/dashboard.module').then(m => m.DashboardModule),
                data: {
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // The root paths and breadcrumbs of all feature modules have to be defined here.
            // Helpful link for module permission relations:
            // https://git.gentics.com/psc/contentnode/blob/f-permrest-gtxpe-570/contentnode-lib/src/main/java/com/gentics/contentnode/perm/TypePerms.java

            // Users Management Module
            {
                path: AdminUIModuleRoutes.USERS,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/user/user.module').then(m => m.UserModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.item_users',
                    },
                    [ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]: [ROUTE_DETAIL_OUTLET],
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.USER_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // Groups Management Module
            {
                path: AdminUIModuleRoutes.GROUPS,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/group/group.module').then(m => m.GroupModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.item_groups',
                    },
                    [ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]: [ROUTE_DETAIL_OUTLET],
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.GROUP_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // Roles Management Module
            {
                path: AdminUIModuleRoutes.ROLES,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/role/role.module').then(m => m.RoleModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.item_roles',
                    },
                    [ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]: [ROUTE_DETAIL_OUTLET],
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.ROLE,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // Folders Management Module
            {
                path: AdminUIModuleRoutes.FOLDERS,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/folder/folder.module').then(m => m.FolderModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.item_folders',
                    },
                    [ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]: [ROUTE_DETAIL_OUTLET],
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.CONTENT_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // Language Management Module
            {
                path: AdminUIModuleRoutes.LANGUAGES,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/language/language.module').then(m => m.LanguageModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.item_languages',
                    },
                    [ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]: [ROUTE_DETAIL_OUTLET],
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.LANGUAGE_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // Logs Management Module
            {
                path: AdminUIModuleRoutes.LOGS,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/logs/logs.module').then(m => m.LogsModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.item_logs',
                    },
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.ACTION_LOG,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // Node Management Module
            {
                path: AdminUIModuleRoutes.NODES,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/node/node.module').then(m => m.NodeModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.item_node_management',
                    },
                    [ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]: [ROUTE_DETAIL_OUTLET],
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.CONTENT,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // Elastic Search Index Maintenance Module
            {
                path: AdminUIModuleRoutes.SEARCH_INDEX_MAINTENANCE,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/elastic-search-index/elastic-search-status.module').then(m => m.ElasticSearchStatusModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.search_index_maintenance',
                    },
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.SEARCH_INDEX_MAINTENANCE,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // DevTools Package Management Module
            {
                path: AdminUIModuleRoutes.DEV_TOOL_PACKAGES,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/dev-tool-package/dev-tool-package.module').then(m => m.DevToolPackageModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.packages',
                    },
                    [ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]: [ROUTE_DETAIL_OUTLET],
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.DEVTOOL_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // DataSource Management Module
            {
                path: AdminUIModuleRoutes.DATA_SOURCES,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/data-source/data-source.module').then(m => m.DataSourceModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.data_sources',
                    },
                    [ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]: [ROUTE_DETAIL_OUTLET],
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.DATA_SOURCE_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // MaintenanceMode Management Module
            {
                path: AdminUIModuleRoutes.MAINTENANCE_MODE,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/maintenance-mode/maintenance-mode.module').then(m => m.MaintenanceModeModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.maintenance_mode',
                    },
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.SYSTEM_MAINTANANCE,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // Contentmaintenance Management Module
            {
                path: AdminUIModuleRoutes.CONTENT_MAINTENANCE,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/content-maintenance/content-maintenance.module').then(m => m.ContentmaintenanceModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.contentmaintenance',
                    },
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.MAINTENANCE,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // ContentRepository Management Module
            {
                path: AdminUIModuleRoutes.CONTENT_REPOSITORIES,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/content-repository/content-repository.module').then(m => m.ContentRepositoryModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.contentrepositories',
                    },
                    [ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]: [ROUTE_DETAIL_OUTLET],
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.CONTENT_REPOSITORY_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // CR-Fragment Management Module
            {
                path: AdminUIModuleRoutes.CR_FRAGMENTS,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/cr-fragment/cr-fragment.module')
                    .then(m => m.ContentRepositoryFragmentModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.item_cr_fragments',
                    },
                    [ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]: [ROUTE_DETAIL_OUTLET],
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.CONTENT_REPOSITORY_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // Templates Module
            {
                path: AdminUIModuleRoutes.TEMPLATES,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/template/template.module')
                    .then(m => m.TemplateModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.item_templates',
                    },
                    [ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]: [ROUTE_DETAIL_OUTLET],
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // Objectproperties Management Module
            {
                path: AdminUIModuleRoutes.OBJECT_PROPERTIES,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/object-property/object-property.module').then(m => m.ObjectPropertyModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.item_object_properties',
                    },
                    [ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]: [ROUTE_DETAIL_OUTLET],
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.OBJECT_PROPERTY_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // TagType Management Module
            {
                path: AdminUIModuleRoutes.CONSTRUCTS,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/construct/construct.module').then(m => m.ConstructModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.item_tagtypes_constructs',
                    },
                    [ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]: [ROUTE_DETAIL_OUTLET],
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.CONSTRUCT_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // Content-Packages
            {
                path: AdminUIModuleRoutes.CONTENT_STAGING,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/content-staging/content-staging.module').then(m => m.ContentStagingModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.item_content_staging',
                    },
                    [ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]: [ROUTE_DETAIL_OUTLET],
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.CONSTRUCT_ADMIN,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // Scheduler Module
            {
                path: AdminUIModuleRoutes.SCHEDULER,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/scheduler/scheduler.module').then(m => m.SchedulerModule),
                data: {
                    [ROUTE_BREADCRUMB_KEY]: {
                        title: 'dashboard.scheduler',
                    },
                    [ROUTE_CHILD_BREADCRUMB_OUTLET_KEY]: [ROUTE_DETAIL_OUTLET],
                    [ROUTE_PERMISSIONS_KEY]: [
                        {
                            type: AccessControlledType.SCHEDULER,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },
        ],
    },
    {
        path: AdminUIModuleRoutes.LOGIN,
        canActivate: [AuthGuard],
        pathMatch: 'full',
        loadChildren: () => import('./login/login.module').then(m => m.LoginModule),
    },
];

@NgModule({
    imports: [
        RouterModule.forRoot(ADMIN_UI_ROUTES, { }),
    ],
    exports: [RouterModule],
})
export class AppRoutingModule {}

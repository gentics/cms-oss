import { PermissionsGuard } from '@admin-ui/core';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';

import { AdminUIModuleRoutes, GcmsAdminUiRoute } from './common/routing/gcms-admin-ui-route';
import { ViewUnauthorizedComponent } from './core/components/view-unauthorized/view-unauthorized.component';
import { AuthGuard } from './core/providers/guards/auth/auth.guard';
import { GenericRouterOutletComponent } from './shared/components/generic-router-outlet/generic-router-outlet.component';
import { SplitViewRouterOutletComponent } from './shared/components/split-view-router-outlet/split-view-router-outlet.component';

const ADMIN_UI_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        canActivate: [AuthGuard],
        canActivateChild: [PermissionsGuard],
        component: GenericRouterOutletComponent,
        data: {
            breadcrumb: { title: 'dashboard.dashboard' },
        },
        children: [
            {
                path: 'unauthorized',
                pathMatch: 'full',
                component: ViewUnauthorizedComponent,
                data: {
                    // empty array will allow route without any permissions
                    typePermissions: [],
                },
            },
            {
                path: '',
                pathMatch: 'full',
                loadChildren: () => import('./dashboard/dashboard.module').then(m => m.DashboardModule),
                data: {
                    typePermissions: [
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
                    breadcrumb: {
                        title: 'dashboard.item_users',
                    },
                    childOutletsForBreadcrumbs: ['detail'],
                    typePermissions: [
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
                    breadcrumb: {
                        title: 'dashboard.item_groups',
                    },
                    childOutletsForBreadcrumbs: ['detail'],
                    typePermissions: [
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
                    breadcrumb: {
                        title: 'dashboard.item_roles',
                    },
                    childOutletsForBreadcrumbs: ['detail'],
                    typePermissions: [
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
                    breadcrumb: {
                        title: 'dashboard.item_folders',
                    },
                    childOutletsForBreadcrumbs: ['detail'],
                    typePermissions: [
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
                    breadcrumb: {
                        title: 'dashboard.item_languages',
                    },
                    childOutletsForBreadcrumbs: ['detail'],
                    typePermissions: [
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
                    breadcrumb: {
                        title: 'dashboard.item_logs',
                    },
                    typePermissions: [
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
                    breadcrumb: {
                        title: 'dashboard.item_node_management',
                    },
                    childOutletsForBreadcrumbs: ['detail'],
                    typePermissions: [
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
                    breadcrumb: {
                        title: 'dashboard.search_index_maintenance',
                    },
                    typePermissions: [
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
                path: AdminUIModuleRoutes.PACKAGES,
                component: SplitViewRouterOutletComponent,
                loadChildren: () => import('./features/package/package.module').then(m => m.PackageModule),
                data: {
                    breadcrumb: {
                        title: 'dashboard.packages',
                    },
                    typePermissions: [
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
                    breadcrumb: {
                        title: 'dashboard.data_sources',
                    },
                    typePermissions: [
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
                    breadcrumb: {
                        title: 'dashboard.maintenance_mode',
                    },
                    typePermissions: [
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
                    breadcrumb: {
                        title: 'dashboard.contentmaintenance',
                    },
                    typePermissions: [
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
                    breadcrumb: {
                        title: 'dashboard.contentrepositories',
                    },
                    childOutletsForBreadcrumbs: ['detail'],
                    typePermissions: [
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
                    breadcrumb: {
                        title: 'dashboard.item_cr_fragments',
                    },
                    childOutletsForBreadcrumbs: ['detail'],
                    typePermissions: [
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
                    breadcrumb: {
                        title: 'dashboard.item_templates',
                    },
                    childOutletsForBreadcrumbs: ['detail'],
                    typePermissions: [
                        {
                            type: AccessControlledType.CONTENT_ADMIN,
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
                    breadcrumb: {
                        title: 'dashboard.item_object_properties',
                    },
                    // childOutletsForBreadcrumbs: ['detail'],
                    typePermissions: [
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
                    breadcrumb: {
                        title: 'dashboard.item_tagtypes_constructs',
                    },
                    // childOutletsForBreadcrumbs: ['detail'],
                    typePermissions: [
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
                    breadcrumb: {
                        title: 'dashboard.item_content_staging',
                    },
                    childOutletsForBreadcrumbs: ['detail'],
                    typePermissions: [
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
                    breadcrumb: {
                        title: 'dashboard.scheduler',
                    },
                    childOutletsForBreadcrumbs: ['detail'],
                    typePermissions: [
                        {
                            type: AccessControlledType.SCHEDULER,
                            permissions: [
                                GcmsPermission.READ,
                            ],
                        },
                    ],
                },
            },

            // {
            //     path: 'testing',
            //     canActivate: [PermissionsGuard],
            //     loadChildren: () => import('./features/testing-do-not-release/testing-do-not-release.module').then(m => m.TestingDoNotReleaseModule),
            //     data: {
            //         breadcrumb: {
            //             title: 'Testing',
            //             doNotTranslate: true,
            //         },
            //         typePermissions: [
            //             { type: AccessControlledType.maintenance, permissions: GcmsPermission.read },
            //             { type: AccessControlledType.scheduler, permissions: [GcmsPermission.read, GcmsPermission.setperm] },
            //         ],
            //     },
            // },

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
        RouterModule.forRoot(ADMIN_UI_ROUTES, {}),
    ],
    exports: [RouterModule],
})
export class AppRoutingModule {}

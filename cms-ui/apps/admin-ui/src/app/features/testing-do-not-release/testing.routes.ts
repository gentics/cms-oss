import { GcmsAdminUiRoute } from '../../common/routing/gcms-admin-ui-route';
import { GenericRouterOutletComponent } from '../../shared/components/generic-router-outlet/generic-router-outlet.component';

export const testingRoutes: GcmsAdminUiRoute[]  = [
    {
        path: '',
        component: GenericRouterOutletComponent,
        children: [
            // put your testing routes here

            // {
            //     path: '',
            //     pathMatch: 'full',
            //     component: TestingOverviewComponentComponent,
            // },
            // {
            //     path: 'permissions-trable',
            //     component: PermissionsTrableTestingComponent,
            //     data: {
            //         breadcrumb: {
            //             title: 'Permissions Trable',
            //             doNotTranslate: true,
            //         },
            //     },
            // },
        ],
    },
];

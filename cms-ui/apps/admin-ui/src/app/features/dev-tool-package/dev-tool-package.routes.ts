import {
    AdminUIEntityDetailRoutes,
    DevToolPackageDetailTabs,
    EditableEntity,
    GcmsAdminUiRoute,
    ROUTE_DETAIL_OUTLET,
    ROUTE_PERMISSIONS_KEY,
    createEntityEditorRoutes,
} from '@admin-ui/common';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { DevToolPackageEditorComponent, DevToolPackageMasterComponent } from './components';

export const DEV_TOOL_PACKAGE_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: DevToolPackageMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.DEVTOOL_PACKAGE,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            [ROUTE_PERMISSIONS_KEY]: [],
        },
        children: [
            ...createEntityEditorRoutes(EditableEntity.DEV_TOOL_PACKAGE, DevToolPackageEditorComponent, DevToolPackageDetailTabs.CONSTRUCTS, {
                [ROUTE_PERMISSIONS_KEY]: [
                    {
                        type: AccessControlledType.CONTENT_ADMIN,
                        permissions: [
                            GcmsPermission.READ,
                        ],
                    },
                ],
            }),
        ],
    },
];

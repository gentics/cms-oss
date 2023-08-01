import {
    AdminUIEntityDetailRoutes,
    DevtoolPackageDetailTabs,
    EditableEntity,
    GcmsAdminUiRoute,
    ROUTE_DETAIL_OUTLET,
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
            typePermissions: [],
        },
        children: [
            ...createEntityEditorRoutes(EditableEntity.DEV_TOOL_PACKAGE, DevToolPackageEditorComponent, DevtoolPackageDetailTabs.CONSTRUCTS, {
                typePermissions: [
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

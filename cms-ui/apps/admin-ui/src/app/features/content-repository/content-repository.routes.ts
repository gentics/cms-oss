import {
    AdminUIEntityDetailRoutes,
    ContentRepositoryDetailTabs,
    EditableEntity,
    GcmsAdminUiRoute,
    ROUTE_DETAIL_OUTLET,
    createEntityEditorRoutes,
} from '@admin-ui/common';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { ContentRepositoryEditorComponent, ContentRepositoryMasterComponent } from './components';

export const CONTENT_REPOSIROTY_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: ContentRepositoryMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.CONTENT_REPOSITORY,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            typePermissions: [],
        },
        children: [
            ...createEntityEditorRoutes(EditableEntity.CONTENT_REPOSITORY, ContentRepositoryEditorComponent, ContentRepositoryDetailTabs.PROPERTIES, {
                typePermissions: [
                    {
                        type: AccessControlledType.CONTENT_REPOSITORY_ADMIN,
                        permissions: [
                            GcmsPermission.READ,
                        ],
                    },
                ],
            }),
        ],
    },
];

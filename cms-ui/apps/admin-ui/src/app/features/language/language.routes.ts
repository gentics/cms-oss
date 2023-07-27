import {
    AdminUIEntityDetailRoutes,
    EditableEntity,
    GcmsAdminUiRoute,
    LanguageDetailTabs,
    ROUTE_DETAIL_OUTLET,
    ROUTE_PERMISSIONS_KEY,
    createEntityEditorRoutes,
} from '@admin-ui/common';
import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { LanguageEditorComponent, LanguageMasterComponent } from './components';

export const LANGUAGE_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: LanguageMasterComponent,
    },
    {
        path: AdminUIEntityDetailRoutes.LANGUAGE,
        outlet: ROUTE_DETAIL_OUTLET,
        data: {
            [ROUTE_PERMISSIONS_KEY]: [],
        },
        children: [
            ...createEntityEditorRoutes(EditableEntity.LANGUAGE, LanguageEditorComponent, LanguageDetailTabs.PROPERTIES, {
                [ROUTE_PERMISSIONS_KEY]: [
                    {
                        type: AccessControlledType.LANGUAGE_ADMIN,
                        permissions: [
                            GcmsPermission.READ,
                        ],
                    },
                ],
            }),
        ],
    },
];


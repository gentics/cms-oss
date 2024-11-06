import { GcmsAdminUiRoute } from '@admin-ui/common/models/routing';
import { MaintenanceModeViewComponent } from './components/maintenance-mode';

export const MaintenanceModeRoutes: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: MaintenanceModeViewComponent,
    },
];

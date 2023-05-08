import { GcmsAdminUiRoute } from '@admin-ui/common/routing/gcms-admin-ui-route';
import { MaintenanceModeViewComponent } from './components/maintenance-mode';

export const MaintenanceModeRoutes: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: MaintenanceModeViewComponent,
    },
];

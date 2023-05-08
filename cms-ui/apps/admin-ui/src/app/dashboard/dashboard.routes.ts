import { GcmsAdminUiRoute } from '../common/routing/gcms-admin-ui-route';
import { DashboardComponent } from './components/dashboard/dashboard.component';

export const DASHBOARD_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        pathMatch: 'full',
        component: DashboardComponent,
    },
];

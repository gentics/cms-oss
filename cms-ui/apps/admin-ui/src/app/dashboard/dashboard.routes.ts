import { GcmsAdminUiRoute } from '../common/models/routing';
import { DashboardComponent } from './components/dashboard/dashboard.component';

export const DASHBOARD_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        pathMatch: 'full',
        component: DashboardComponent,
    },
];

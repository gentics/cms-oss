import { GcmsAdminUiRoute } from '@admin-ui/common/routing/gcms-admin-ui-route';
import { ActionLogEntryMasterComponent } from './components';

export const LOGS_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component:  ActionLogEntryMasterComponent,
    },
];

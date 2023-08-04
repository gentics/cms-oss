import { GcmsAdminUiRoute } from '@admin-ui/common/models/routing';
import { ActionLogEntryMasterComponent } from './components';

export const LOGS_ROUTES: GcmsAdminUiRoute[] = [
    {
        path: '',
        component:  ActionLogEntryMasterComponent,
    },
];

import { GcmsAdminUiRoute } from '../../common/routing/gcms-admin-ui-route';
import { ElasticSearchIndexMasterComponent } from './components/elastic-search-index-master/elastic-search-index-master.component';

export const elasticSearchStatusRoutes: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: ElasticSearchIndexMasterComponent,
    },
];

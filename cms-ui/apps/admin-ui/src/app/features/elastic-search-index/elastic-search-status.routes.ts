import { GcmsAdminUiRoute } from '../../common/models/routing';
import { ElasticSearchIndexMasterComponent } from './components/elastic-search-index-master/elastic-search-index-master.component';

export const elasticSearchStatusRoutes: GcmsAdminUiRoute[] = [
    {
        path: '',
        component: ElasticSearchIndexMasterComponent,
    },
];

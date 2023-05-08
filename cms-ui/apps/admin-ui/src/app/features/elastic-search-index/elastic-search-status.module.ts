import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { ElasticSearchIndexTableComponent, ElasticSearchIndexMasterComponent } from './components';
import { elasticSearchStatusRoutes } from './elastic-search-status.routes';
import { ElasticSearchIndexTableLoaderService } from './providers';

@NgModule({
    declarations: [
        ElasticSearchIndexMasterComponent,
        ElasticSearchIndexTableComponent,
    ],
    providers: [
        ElasticSearchIndexTableLoaderService,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(elasticSearchStatusRoutes),
    ],
})
export class ElasticSearchStatusModule {}

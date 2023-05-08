import { ElasticSearchIndexBO } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ElasticSearchIndex, NormalizableEntityType } from '@gentics/cms-models';

@Component({
    selector: 'gtx-elastic-search-index-master',
    templateUrl: './elastic-search-index-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ElasticSearchIndexMasterComponent extends BaseTableMasterComponent<ElasticSearchIndex, ElasticSearchIndexBO> {

    entityIdentifier: NormalizableEntityType = 'elasticSearchIndex';
}

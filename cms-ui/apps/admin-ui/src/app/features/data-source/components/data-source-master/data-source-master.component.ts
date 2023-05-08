import { DataSourceBO } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AnyModelType, DataSource, NormalizableEntityTypesMap } from '@gentics/cms-models';

@Component({
    selector: 'gtx-data-source-master',
    templateUrl: './data-source-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DataSourceMasterComponent extends BaseTableMasterComponent<DataSource, DataSourceBO> {
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'dataSource';
    protected detailPath = 'data-source';
}

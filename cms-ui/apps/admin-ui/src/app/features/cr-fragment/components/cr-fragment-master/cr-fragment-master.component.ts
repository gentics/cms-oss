import { AdminUIEntityDetailRoutes, ContentRepositoryFragmentBO } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ContentRepositoryFragment, NormalizableEntityType } from '@gentics/cms-models';

@Component({
    selector: 'gtx-content-repository-fragment-master',
    templateUrl: './cr-fragment-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContentRepositoryFragmentMasterComponent extends BaseTableMasterComponent<ContentRepositoryFragment, ContentRepositoryFragmentBO> {

    protected entityIdentifier: NormalizableEntityType = 'contentRepositoryFragment';
    protected detailPath = AdminUIEntityDetailRoutes.CR_FRAGMENT;
}

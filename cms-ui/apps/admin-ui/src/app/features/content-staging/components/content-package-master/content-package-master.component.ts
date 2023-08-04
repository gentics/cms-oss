import { AdminUIEntityDetailRoutes, ContentPackageBO } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ContentPackage, NormalizableEntityType } from '@gentics/cms-models';

@Component({
    selector: 'gtx-content-package-master',
    templateUrl: './content-package-master.component.html',
    styleUrls: ['./content-package-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContentPackageMasterComponent extends BaseTableMasterComponent<ContentPackage, ContentPackageBO> {

    protected entityIdentifier: NormalizableEntityType = 'contentPackage';
    protected detailPath = AdminUIEntityDetailRoutes.CONTENT_PACKAGE;
}

import { FolderBO } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Folder, NormalizableEntityType } from '@gentics/cms-models';
import { TrableRow } from '@gentics/ui-core';

@Component({
    selector: 'gtx-folder-master',
    templateUrl: './folder-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FolderMasterComponent extends BaseTableMasterComponent<Folder, FolderBO> {
    protected entityIdentifier: NormalizableEntityType = 'folder';

    public override handleRowClick(row: TrableRow<FolderBO>): void {
        this.navigateToEntityDetails(row.item.id);
    }
}

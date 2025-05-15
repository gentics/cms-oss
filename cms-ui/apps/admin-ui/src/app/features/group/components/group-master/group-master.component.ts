import { GroupBO } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Group, NormalizableEntityType } from '@gentics/cms-models';

@Component({
    selector: 'gtx-group-master',
    templateUrl: './group-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class GroupMasterComponent extends BaseTableMasterComponent<Group, GroupBO> {
    protected entityIdentifier: NormalizableEntityType = 'group';
}

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Group, NormalizableEntityType } from '@gentics/cms-models';
import { GroupBO } from '../../../../common';
import { BaseTableMasterComponent } from '../../../../shared/components';

@Component({
    selector: 'gtx-group-master',
    templateUrl: './group-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class GroupMasterComponent extends BaseTableMasterComponent<Group, GroupBO> {
    protected entityIdentifier: NormalizableEntityType = 'group';
}

import { UserBO } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { NormalizableEntityType, User } from '@gentics/cms-models';

@Component({
    selector: 'gtx-user-master',
    templateUrl: './user-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserMasterComponent extends BaseTableMasterComponent<User, UserBO> {
    protected entityIdentifier: NormalizableEntityType = 'user';
}

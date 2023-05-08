import { ActionLogEntryBO } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ActionLogEntry, NormalizableEntityType } from '@gentics/cms-models';

@Component({
    selector: 'gtx-action-log-entry-master',
    templateUrl: './action-log-entry-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActionLogEntryMasterComponent extends BaseTableMasterComponent<ActionLogEntry, ActionLogEntryBO> {

    entityIdentifier: NormalizableEntityType = 'logs';
}

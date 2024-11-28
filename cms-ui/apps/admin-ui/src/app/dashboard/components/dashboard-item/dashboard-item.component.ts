import { GtxIcon } from '@admin-ui/shared';
import { Component, Input } from '@angular/core';
import { coerceToBoolean } from '@gentics/ui-core';

@Component({
    selector: 'gtx-dashboard-item',
    templateUrl: './dashboard-item.component.html',
    styleUrls: ['./dashboard-item.component.scss'],
})
export class DashboardItemComponent {

    @Input() title: string;
    @Input() icon: GtxIcon;
    @Input() ribbon: string;
    @Input() set disabled(val: any) {
        this.disabledItem = coerceToBoolean(val);
    }

    disabledItem = false;

    constructor() { }

}

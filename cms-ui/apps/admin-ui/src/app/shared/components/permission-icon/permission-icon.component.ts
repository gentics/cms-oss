import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { PermissionInfo } from '@gentics/cms-models';

@Component({
    selector: 'gtx-permission-icon',
    templateUrl: './permission-icon.component.html',
    styleUrls: ['./permission-icon.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PermissionIconComponent {

    @Input()
    permission: PermissionInfo;

}

import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { License, LicenseStatus } from '@gentics/cms-models';

@Component({
    selector: 'gtx-license-info',
    templateUrl: './license-info.component.html',
    styleUrls: ['./license-info.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LicenseInfoComponent {

    @Input()
    public status: LicenseStatus;

    @Input()
    public license: License;

}

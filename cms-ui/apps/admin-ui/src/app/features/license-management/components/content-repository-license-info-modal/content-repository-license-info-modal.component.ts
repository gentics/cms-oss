import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { ContentRepositoryLicense } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-content-repository-license-info-modal',
    templateUrl: './content-repository-license-info-modal.component.html',
    styleUrls: ['./content-repository-license-info-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContentRepositoryLicenseInfoModal extends BaseModal<void> {

    @Input()
    public info: ContentRepositoryLicense;
}

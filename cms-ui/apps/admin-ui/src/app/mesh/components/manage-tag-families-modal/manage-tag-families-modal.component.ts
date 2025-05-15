import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { ProjectReference } from '@gentics/mesh-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-manage-tag-families-modal',
    templateUrl: './manage-tag-families-modal.component.html',
    styleUrls: ['./manage-tag-families-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ManageTagFamiliesModal extends BaseModal<void> {

    @Input()
    public project: ProjectReference;

}

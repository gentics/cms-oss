import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { User } from '@gentics/mesh-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-mesh-user-token-modal',
    templateUrl: './mesh-user-token-modal.component.html',
    styleUrl: './mesh-user-token-modal.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class MeshUserTokenModal extends BaseModal<void> {

    @Input()
    public user: User;

}

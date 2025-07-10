import { I18nNotificationService } from '@admin-ui/core';
import { MeshUserBO } from '@admin-ui/mesh/common';
import { getUserDisplayName } from '@admin-ui/mesh/utils';
import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { User, UserReference } from '@gentics/mesh-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-mesh-copy-token-modal',
    templateUrl: './copy-token-modal.component.html',
    styleUrls: ['./copy-token-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class CopyTokenModal extends BaseModal<void> implements OnInit {

    @Input()
    public token: string;

    @Input()
    public user: UserReference | User | MeshUserBO;

    public displayName: string;

    constructor(
        protected notification: I18nNotificationService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.displayName = getUserDisplayName(this.user);
    }

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    handleCopy(error?: any): void {
        if (error) {
            this.notification.show({
                type: 'alert',
                message: 'mesh.copy_token_error',
            });
            console.error(error);
            return;
        }

        this.notification.show({
            type: 'success',
            message: 'mesh.copy_token_success',
        });
    }
}

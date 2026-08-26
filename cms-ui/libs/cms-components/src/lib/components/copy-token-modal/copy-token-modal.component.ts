import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';
import { I18nNotificationService } from '../../providers';

@Component({
    selector: 'gtx-api-copy-token-modal',
    templateUrl: './copy-token-modal.component.html',
    styleUrls: ['./copy-token-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class CopyTokenModal extends BaseModal<void> {

    @Input()
    public token: string;

    constructor(
        protected notification: I18nNotificationService,
    ) {
        super();
    }

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    handleCopy(error?: any): void {
        if (error) {
            this.notification.show({
                type: 'alert',
                message: 'mesh.copy_token_error',
            });
            return;
        }

        this.notification.show({
            type: 'success',
            message: 'mesh.copy_token_success',
        });
    }
}

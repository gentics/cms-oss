import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { FormControl } from '@angular/forms';
import { EditableUserTokenData, User, UserTokenResponse } from '@gentics/mesh-models';
import { BaseModal } from '@gentics/ui-core';
import { MeshUserHandlerService } from '../../providers/mesh-user-handler/mesh-user-handler.service';
import { MeshUserTokenFormData } from '../mesh-user-token-properties/mesh-user-token-properties.component';

@Component({
    selector: 'gtx-create-mesh-user-token-modal',
    templateUrl: './create-mesh-user-token-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class CreateMeshUserTokenModal extends BaseModal<UserTokenResponse> {

    @Input()
    public user: User;

    public control = new FormControl<MeshUserTokenFormData>(null);
    public loading = false;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private handler: MeshUserHandlerService,
    ) {
        super();
    }

    public createToken(): void {
        if (this.loading) {
            return;
        }

        this.loading = true;
        this.changeDetector.markForCheck();

        const { expires, ...formData } = this.control.value;

        const req: EditableUserTokenData = {
            ...formData,
        };

        if (expires != null && expires > 0) {
            req.expires = this.parseToISO8601(expires);
        }

        this.handler.createToken(this.user.uuid, req).then((res) => {
            this.closeFn(res);
        }).catch(() => {
            this.loading = false;
            this.changeDetector.markForCheck();
        });
    }

    private parseToISO8601(dateTime: number): string {
        return new Date(dateTime * 1000).toISOString();
    }
}

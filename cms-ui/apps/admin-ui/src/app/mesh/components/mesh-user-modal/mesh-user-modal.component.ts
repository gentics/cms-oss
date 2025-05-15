import { MeshUserHandlerService } from '@admin-ui/mesh/providers';
import { getUserDisplayName } from '@admin-ui/mesh/utils';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { User } from '@gentics/mesh-models';
import { BaseModal } from '@gentics/ui-core';
import { MeshUserPropertiesMode } from '../mesh-user-properties/mesh-user-properties.component';

@Component({
    selector: 'gtx-mesh-user-modal',
    templateUrl: './mesh-user-modal.component.html',
    styleUrls: ['./mesh-user-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MeshUserModal extends BaseModal<User> implements OnInit {

    public readonly MeshUserPropertiesMode = MeshUserPropertiesMode;

    @Input()
    public mode: MeshUserPropertiesMode;

    @Input()
    public user: User;

    public name: string;
    public form: FormControl;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected handler: MeshUserHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.name = this.user == null ? '' : getUserDisplayName(this.user);
        this.form = new FormControl(this.user);
    }

    buttonCreateEntityClicked(): void {
        const val = this.form.value;
        this.form.disable();
        this.changeDetector.markForCheck();

        const op = this.mode === MeshUserPropertiesMode.CREATE
            ? this.handler.create(val)
            : this.handler.update(this.user.uuid, val);

        op.then(res => {
            this.closeFn(res);
        }).catch(() => {
            this.form.enable();
            this.changeDetector.markForCheck();
        });
    }
}

import { I18nNotificationService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { createNestedControlValidator } from '@gentics/cms-components';
import { Role } from '@gentics/mesh-models';
import { RequestFailedError } from '@gentics/mesh-rest-client';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { BaseModal } from '@gentics/ui-core';
import { MeshRolePropertiesMode } from '../mesh-role-properties/mesh-role-properties.component';

@Component({
    selector: 'gtx-mesh-role-modal',
    templateUrl: './mesh-role-modal.component.html',
    styleUrls: ['./mesh-role-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshRoleModal extends BaseModal<Role> implements OnInit {

    @Input()
    public mode: MeshRolePropertiesMode;

    @Input()
    public role: Role;

    public form: FormControl;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected notification: I18nNotificationService,
        protected api: MeshRestClientService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.form = new FormControl(this.role, createNestedControlValidator());
    }

    buttonCreateEntityClicked(): void {
        const val = this.form.value;
        this.form.disable();
        this.changeDetector.markForCheck();

        const op = this.mode === MeshRolePropertiesMode.CREATE
            ? this.api.roles.create(val)
            : this.api.roles.update(this.role.uuid, val);

        op.then(res => {
            this.closeFn(res);
        }).catch((err: RequestFailedError) => {
            this.form.enable();
            this.changeDetector.markForCheck();
            this.notification.show({
                message: err.data.message,
                type: 'alert',
            });
        });
    }
}

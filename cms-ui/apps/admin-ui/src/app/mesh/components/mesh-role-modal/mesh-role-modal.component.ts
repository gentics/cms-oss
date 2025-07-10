import { MeshRoleHandlerService } from '@admin-ui/mesh/providers';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Role } from '@gentics/mesh-models';
import { BaseModal } from '@gentics/ui-core';
import { MeshRolePropertiesMode } from '../mesh-role-properties/mesh-role-properties.component';

@Component({
    selector: 'gtx-mesh-role-modal',
    templateUrl: './mesh-role-modal.component.html',
    styleUrls: ['./mesh-role-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MeshRoleModal extends BaseModal<Role> implements OnInit {

    public readonly MeshRolePropertiesMode = MeshRolePropertiesMode;

    @Input()
    public mode: MeshRolePropertiesMode;

    @Input()
    public role: Role;

    public form: FormControl;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected handler: MeshRoleHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.form = new FormControl(this.role);
    }

    buttonCreateEntityClicked(): void {
        const val = this.form.value;
        this.form.disable();
        this.changeDetector.markForCheck();

        const op = this.mode === MeshRolePropertiesMode.CREATE
            ? this.handler.create(val)
            : this.handler.update(this.role.uuid, val);

        op.then(res => {
            this.closeFn(res);
        }).catch(() => {
            this.form.enable();
            this.changeDetector.markForCheck();
        });
    }
}

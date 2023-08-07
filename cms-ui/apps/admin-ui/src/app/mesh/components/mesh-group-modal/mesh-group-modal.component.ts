import { MeshGroupHandlerService } from '@admin-ui/mesh/providers';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { createNestedControlValidator } from '@gentics/cms-components';
import { Group } from '@gentics/mesh-models';
import { BaseModal } from '@gentics/ui-core';
import { MeshGroupPropertiesMode } from '../mesh-group-properties/mesh-group-properties.component';

@Component({
    selector: 'gtx-mesh-group-modal',
    templateUrl: './mesh-group-modal.component.html',
    styleUrls: ['./mesh-group-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshGroupModal extends BaseModal<Group> implements OnInit {

    public readonly MeshGroupPropertiesMode = MeshGroupPropertiesMode;

    @Input()
    public mode: MeshGroupPropertiesMode;

    @Input()
    public group: Group;

    public form: FormControl;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected handler: MeshGroupHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.form = new FormControl(this.group, createNestedControlValidator());
    }

    buttonCreateEntityClicked(): void {
        const val = this.form.value;
        this.form.disable();
        this.changeDetector.markForCheck();

        const op = this.mode === MeshGroupPropertiesMode.CREATE
            ? this.handler.create(val)
            : this.handler.update(this.group.uuid, val);

        op.then(res => {
            this.closeFn(res);
        }).catch(() => {
            this.form.enable();
            this.changeDetector.markForCheck();
        });
    }
}

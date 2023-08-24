import { MicroschemaHandlerService } from '@admin-ui/mesh/providers';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Microschema, Schema } from '@gentics/mesh-models';
import { BaseModal } from '@gentics/ui-core';
import { MicroschemaPropertiesMode } from '../microschema-properties/microschema-properties.component';

@Component({
    selector: 'gtx-mesh-microschema-modal',
    templateUrl: './microschema-modal.component.html',
    styleUrls: ['./microschema-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroschemaModal extends BaseModal<Schema> implements OnInit {

    public readonly MicroschemaPropertiesMode = MicroschemaPropertiesMode;

    @Input()
    public mode: MicroschemaPropertiesMode;

    @Input()
    public microschema: Microschema;

    public form: FormControl;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected handler: MicroschemaHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.form = new FormControl(this.microschema || {});
    }

    buttonCreateEntityClicked(): void {
        const val = this.form.value;
        this.form.disable();
        this.changeDetector.markForCheck();

        const op = this.mode === MicroschemaPropertiesMode.CREATE
            ? this.handler.create(val)
            : this.handler.update(this.microschema.uuid, val);

        op.then(res => {
            this.closeFn(res);
        }).catch(() => {
            this.form.enable();
            this.changeDetector.markForCheck();
        });
    }
}

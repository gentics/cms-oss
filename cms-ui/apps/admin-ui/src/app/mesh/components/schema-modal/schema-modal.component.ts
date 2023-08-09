import { SchemaHandlerService } from '@admin-ui/mesh/providers';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { createNestedControlValidator } from '@gentics/cms-components';
import { Schema } from '@gentics/mesh-models';
import { BaseModal } from '@gentics/ui-core';
import { SchemaPropertiesMode } from '../schema-properties/schema-properties.component';

@Component({
    selector: 'gtx-mesh-schema-modal',
    templateUrl: './schema-modal.component.html',
    styleUrls: ['./schema-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SchemaModal extends BaseModal<Schema> implements OnInit {

    public readonly SchemaPropertiesMode = SchemaPropertiesMode;

    @Input()
    public mode: SchemaPropertiesMode;

    @Input()
    public schema: Schema;

    public form: FormControl;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected handler: SchemaHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.form = new FormControl(this.schema || {}, createNestedControlValidator());
    }

    buttonCreateEntityClicked(): void {
        const val = this.form.value;
        this.form.disable();
        this.changeDetector.markForCheck();

        const op = this.mode === SchemaPropertiesMode.CREATE
            ? this.handler.create(val)
            : this.handler.update(this.schema.uuid, val);

        op.then(res => {
            this.closeFn(res);
        }).catch(() => {
            this.form.enable();
            this.changeDetector.markForCheck();
        });
    }
}

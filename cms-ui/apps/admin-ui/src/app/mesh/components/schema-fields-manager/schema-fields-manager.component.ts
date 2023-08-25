import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormControl } from '@angular/forms';
import { BasePropertiesListComponent } from '@gentics/cms-components';
import { SchemaField } from '@gentics/mesh-models';
import { ISortableEvent, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';
import { SchemaFieldPropertiesType } from '../schema-field-properties/schema-field-properties.component';

@Component({
    selector: 'gtx-mesh-schema-fields-manager',
    templateUrl: './schema-fields-manager.component.html',
    styleUrls: ['./schema-fields-manager.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(SchemaFieldsManagerComponent),
        generateValidatorProvider(SchemaFieldsManagerComponent),
    ],
})
export class SchemaFieldsManagerComponent extends BasePropertiesListComponent<SchemaField> {

    @Input({ required: true })
    public type: SchemaFieldPropertiesType;

    @Input()
    public label: string;

    @Input()
    public ownName: string;

    @Input()
    public schemaNames: string[]

    @Input()
    public microschemaNames: string[];

    protected createControl(value?: SchemaField): FormControl<SchemaField> {
        return new FormControl(value || {} as any);
    }

    protected configureForm(value: SchemaField[], loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: SchemaField[]): SchemaField[] {
        return value;
    }

    sortFields(event: ISortableEvent): void {
        const ctl = this.form.at(event.oldIndex);
        this.form.removeAt(event.oldIndex, { emitEvent: false });
        this.form.insert(event.newIndex, ctl, { emitEvent: false });
        this.form.updateValueAndValidity();
        this.value = this.form.value;
    }

    moveControl(ctl: FormControl<SchemaField>, from: number, to: number): void {
        this.form.removeAt(from, { emitEvent: false });
        this.form.insert(to, ctl, { emitEvent: false });
        this.form.updateValueAndValidity();
        this.value = this.form.value;
    }
}

import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormArray, FormControl } from '@angular/forms';
import { BasePropertiesListComponent, CONTROL_INVALID_VALUE, createNestedControlValidator } from '@gentics/cms-components';
import { SchemaField } from '@gentics/mesh-models';
import { ISortableEvent, generateFormProvider } from '@gentics/ui-core';
import { SchemaFieldPropertiesType } from '../schema-field-properties/schema-field-properties.component';

@Component({
    selector: 'gtx-mesh-schema-fields-manager',
    templateUrl: './schema-fields-manager.component.html',
    styleUrls: ['./schema-fields-manager.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(SchemaFieldsManagerComponent)],
})
export class SchemaFieldsManagerComponent extends BasePropertiesListComponent<SchemaField> {

    public readonly SchemaFieldPropertiesType = SchemaFieldPropertiesType;

    @Input()
    public ownName: string;

    @Input()
    public schemaNames: string[]

    @Input()
    public microschemaNames: string[];

    protected override createForm(): FormArray<FormControl<SchemaField>> {
        const form = super.createForm();

        form.setValidators((ctl) => {
            const tmp = ctl.value;
            if (tmp === CONTROL_INVALID_VALUE) {
                return { invalidValue: true };
            }
            if (Array.isArray(tmp)) {
                if (tmp.some(innerVal => innerVal === CONTROL_INVALID_VALUE)) {
                    return { invalidValue: true };
                }
            }

            return null;
        });

        return form;
    }

    protected createControl(value?: SchemaField): FormControl<SchemaField> {
        return new FormControl(value || {} as any, createNestedControlValidator());
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

    moveToTop(ctl: FormControl<SchemaField>, index: number): void {
        this.form.removeAt(index, { emitEvent: false });
        this.form.insert(0, ctl, { emitEvent: false });
        this.form.updateValueAndValidity();
        this.value = this.form.value;
    }

    moveToBottom(ctl: FormControl<SchemaField>, index: number): void {
        this.form.removeAt(index, { emitEvent: false });
        this.form.push(ctl, { emitEvent: false });
        this.form.updateValueAndValidity();
        this.value = this.form.value;
    }
}

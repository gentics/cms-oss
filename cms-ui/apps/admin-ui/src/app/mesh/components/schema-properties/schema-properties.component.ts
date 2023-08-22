import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormArray, FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent, createNestedControlValidator } from '@gentics/cms-components';
import { EditableSchemaProperties } from '@gentics/mesh-models';
import { FormProperties, generateFormProvider } from '@gentics/ui-core';
import { SchemaFieldPropertiesType } from '../schema-field-properties/schema-field-properties.component';
import { blacklistValidator } from '@admin-ui/common';

export enum SchemaPropertiesMode {
    CREATE,
    EDIT,
}

@Component({
    selector: 'gtx-mesh-schema-properties',
    templateUrl: './schema-properties.component.html',
    styleUrls: ['./schema-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(SchemaPropertiesComponent)],
})
export class SchemaPropertiesComponent extends BasePropertiesComponent<EditableSchemaProperties> {

    public readonly SchemaPropertiesMode = SchemaPropertiesMode;
    public readonly SchemaFieldPropertiesType = SchemaFieldPropertiesType;

    @Input()
    public mode: SchemaPropertiesMode;

    @Input()
    public ownName: string;

    @Input()
    public schemaNames: string[];

    @Input()
    public microschemaNames: string[];

    protected createForm(): FormGroup<FormProperties<EditableSchemaProperties>> {
        return new FormGroup<FormProperties<EditableSchemaProperties>>({
            name: new FormControl(this.value?.name, [
                Validators.required,
                Validators.pattern(/^[a-zA-Z0-9_]+$/),
                blacklistValidator(() => this.schemaNames.filter(name => name !== this.ownName)),
            ]),
            fields: new FormArray([
                new FormControl(this.value?.fields?.[0], createNestedControlValidator()),
            ]),
        });
    }

    protected configureForm(_value: EditableSchemaProperties, _loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: EditableSchemaProperties): EditableSchemaProperties {
        return value;
    }
}

import { createBlacklistValidator } from '@admin-ui/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { EditableMicroschemaProperties } from '@gentics/mesh-models';
import { FormProperties, createJsonValidator, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';
import { SchemaFieldPropertiesType } from '../schema-field-properties/schema-field-properties.component';

export enum MicroschemaPropertiesMode {
    CREATE,
    EDIT,
}

@Component({
    selector: 'gtx-mesh-microschema-properties',
    templateUrl: './microschema-properties.component.html',
    styleUrls: ['./microschema-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(MicroschemaPropertiesComponent),
        generateValidatorProvider(MicroschemaPropertiesComponent),
    ],
})
export class MicroschemaPropertiesComponent extends BasePropertiesComponent<EditableMicroschemaProperties> {

    public readonly MicroschemaPropertiesMode = MicroschemaPropertiesMode;
    public readonly SchemaFieldPropertiesType = SchemaFieldPropertiesType;

    @Input()
    public mode: MicroschemaPropertiesMode;

    @Input()
    public tabs = false;

    @Input()
    public schemaNames: string[];

    @Input()
    public microschemaNames: string[];

    protected createForm(): FormGroup<FormProperties<EditableMicroschemaProperties>> {
        return new FormGroup<FormProperties<EditableMicroschemaProperties>>({
            name: new FormControl(this.value?.name || '', [
                Validators.required,
                Validators.pattern(/^[a-zA-Z0-9_]+$/),
                createBlacklistValidator(() => this.microschemaNames.filter(name => name !== this.form?.value?.name)),
            ]),
            description: new FormControl(this.value?.description || ''),
            elasticsearch: new FormControl(this.value?.elasticsearch, createJsonValidator()),
            fields: new FormControl(this.value?.fields || []),
        });
    }

    protected configureForm(_value: EditableMicroschemaProperties, _loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: EditableMicroschemaProperties): EditableMicroschemaProperties {
        return value;
    }
}

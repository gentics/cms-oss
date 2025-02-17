import { createBlacklistValidator, createWhitelistValidator } from '@admin-ui/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { EditableSchemaProperties } from '@gentics/mesh-models';
import { FormProperties, createJsonValidator, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';
import { SchemaFieldPropertiesType } from '../schema-field-properties/schema-field-properties.component';

export enum SchemaPropertiesMode {
    CREATE,
    EDIT,
}

@Component({
    selector: 'gtx-mesh-schema-properties',
    templateUrl: './schema-properties.component.html',
    styleUrls: ['./schema-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(SchemaPropertiesComponent),
        generateValidatorProvider(SchemaPropertiesComponent),
    ],
})
export class SchemaPropertiesComponent extends BasePropertiesComponent<EditableSchemaProperties> {

    public readonly SchemaPropertiesMode = SchemaPropertiesMode;
    public readonly SchemaFieldPropertiesType = SchemaFieldPropertiesType;

    @Input()
    public mode: SchemaPropertiesMode;

    @Input()
    public tabs = false;

    @Input()
    public schemaNames: string[];

    @Input()
    public microschemaNames: string[];

    @Input()
    public fieldNames: string[];

    protected override delayedSetup = true;

    protected createForm(): FormGroup<FormProperties<EditableSchemaProperties>> {
        return new FormGroup<FormProperties<EditableSchemaProperties>>({
            name: new FormControl(this.value?.name || '', [
                Validators.required,
                Validators.pattern(/^[a-zA-Z0-9_]+$/),
                createBlacklistValidator(() => this.schemaNames.filter(name => name !== this.form?.value?.name)),
            ]),
            description: new FormControl(this.value?.description || ''),
            autoPurge: new FormControl(this.value?.autoPurge ?? false),
            noIndex: new FormControl(this.value?.noIndex ?? false),
            container: new FormControl(this.value?.container ?? false),
            displayField: new FormControl(this.value?.displayField, createWhitelistValidator(() => this.form?.value?.fields?.map?.(field => field.name))),
            segmentField: new FormControl(this.value?.segmentField, createWhitelistValidator(() => this.form?.value?.fields?.map?.(field => field.name))),
            urlFields: new FormControl(this.value?.urlFields || [], createWhitelistValidator(() => this.form?.value?.fields?.map?.(field => field.name))),
            elasticsearch: new FormControl(this.value?.elasticsearch, createJsonValidator()),
            fields: new FormControl(this.value?.fields || []),
        });
    }

    protected configureForm(value: EditableSchemaProperties, _loud?: boolean): void {
        this.fieldNames = value?.fields?.map?.(field => field.name);
    }

    protected assembleValue(value: EditableSchemaProperties): EditableSchemaProperties {
        return value;
    }
}

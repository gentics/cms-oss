import { createBlacklistValidator, createWhitelistValidator } from '@admin-ui/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { EditableSchemaProperties, FieldType } from '@gentics/mesh-models';
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

    public fieldNames: string[];
    public urlFieldNames: string[];

    protected override delayedSetup = true;

    protected createForm(): FormGroup<FormProperties<EditableSchemaProperties>> {
        return new FormGroup<FormProperties<EditableSchemaProperties>>({
            name: new FormControl(this.safeValue('name') || '', [
                Validators.required,
                Validators.pattern(/^[a-zA-Z0-9_]+$/),
                createBlacklistValidator(() => this.schemaNames.filter(name => name !== this.form?.value?.name)),
            ]),
            description: new FormControl(this.safeValue('description') || ''),
            autoPurge: new FormControl(this.safeValue('autoPurge') ?? false),
            noIndex: new FormControl(this.safeValue('noIndex') ?? false),
            container: new FormControl(this.safeValue('container') ?? false),
            displayField: new FormControl(this.safeValue('displayField'), createWhitelistValidator(() => this.fieldNames)),
            segmentField: new FormControl(this.safeValue('segmentField'), createWhitelistValidator(() => this.fieldNames)),
            urlFields: new FormControl(this.safeValue('urlFields') || [], createWhitelistValidator(() => this.urlFieldNames)),
            elasticsearch: new FormControl(this.safeValue('elasticsearch'), createJsonValidator()),
            fields: new FormControl(this.safeValue('fields') || []),
        });
    }

    protected configureForm(value: EditableSchemaProperties, _loud?: boolean): void {
        this.fieldNames = (value?.fields || [])
            .filter(field => field.type === FieldType.STRING || field.type === FieldType.BINARY)
            .map(field => field.name);
        this.urlFieldNames = (value?.fields || [])
            .filter(field => field.type === FieldType.STRING)
            .map(field => field.name);
    }

    protected assembleValue(value: EditableSchemaProperties): EditableSchemaProperties {
        return value;
    }
}

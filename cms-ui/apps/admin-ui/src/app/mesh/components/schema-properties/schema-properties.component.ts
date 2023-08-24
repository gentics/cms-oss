import { createBlacklistValidator, createWhitelistValidator } from '@admin-ui/common';
import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { EditableSchemaProperties } from '@gentics/mesh-models';
import { FormProperties, createJsonValidator, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

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
export class SchemaPropertiesComponent extends BasePropertiesComponent<EditableSchemaProperties> implements OnChanges {

    public readonly SchemaPropertiesMode = SchemaPropertiesMode;

    @Input()
    public mode: SchemaPropertiesMode;

    @Input()
    public onlyProperties = false;

    @Input()
    public ownName: string;

    @Input()
    public schemaNames: string[];

    @Input()
    public microschemaNames: string[];

    @Input()
    public fieldNames: string[];

    protected override delayedSetup = true;

    public ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.ownName || changes.schemaNames) {
            if (this.form) {
                this.form.controls.name.updateValueAndValidity();
            }
        }

        if (changes.fieldNames) {
            this.updateFieldNames(this.fieldNames);
        }
    }

    protected createForm(): FormGroup<FormProperties<EditableSchemaProperties>> {
        return new FormGroup<FormProperties<EditableSchemaProperties>>({
            name: new FormControl(this.value?.name, [
                Validators.required,
                Validators.pattern(/^[a-zA-Z0-9_]+$/),
                createBlacklistValidator(() => this.schemaNames.filter(name => name !== this.ownName)),
            ]),
            description: new FormControl(this.value?.description),
            autoPurge: new FormControl(this.value?.autoPurge),
            container: new FormControl(this.value?.container),
            displayField: new FormControl(this.value?.displayField, createWhitelistValidator(() => this.fieldNames)),
            segmentField: new FormControl(this.value?.segmentField, createWhitelistValidator(() => this.fieldNames)),
            urlFields: new FormControl(this.value?.urlFields || [], createWhitelistValidator(() => this.fieldNames)),
            elasticsearch: new FormControl(this.value?.elasticsearch, createJsonValidator()),
            fields: new FormControl(this.value?.fields || []),
        });
    }

    protected configureForm(_value: EditableSchemaProperties, _loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: EditableSchemaProperties): EditableSchemaProperties {
        return value;
    }

    protected updateFieldNames(names: string[]): void {
        this.fieldNames = names;

        if (this.form) {
            this.form.controls.displayField.updateValueAndValidity();
            this.form.controls.segmentField.updateValueAndValidity();
            this.form.controls.urlFields.updateValueAndValidity();
        }
    }
}

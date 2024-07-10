import { createWhitelistValidator } from '@admin-ui/common';
import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { FieldExtractOptions, FieldType, SchemaField } from '@gentics/mesh-models';
import { FormProperties, createJsonValidator, generateFormProvider, generateValidatorProvider, setControlsEnabled } from '@gentics/ui-core';
import { pick } from 'lodash-es';

export enum SchemaFieldPropertiesType {
    SCHEMA = 'schema',
    MICROSCHEMA = 'microschema',
}

const TYPES_WITH_ALLOW = [
    FieldType.STRING,
    FieldType.MICRONODE,
    FieldType.NODE,
];

/* eslint-disable @typescript-eslint/naming-convention */
const MicroschemaFieldType = pick(FieldType, ['BINARY', 'BOOLEAN', 'DATE', 'HTML', 'LIST', 'NODE', 'NUMBER', 'STRING']);
const ListFieldType = pick(FieldType, ['BOOLEAN', 'DATE', 'HTML', 'MICRONODE', 'NODE', 'NUMBER', 'STRING']);
const MicroschemaListFieldType = pick(FieldType, ['BOOLEAN', 'DATE', 'HTML', 'NODE', 'NUMBER', 'STRING']);
/* eslint-enable @typescript-eslint/naming-convention */

@Component({
    selector: 'gtx-mesh-schema-field-properties',
    templateUrl: './schema-field-properties.component.html',
    styleUrls: ['./schema-field-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(SchemaFieldPropertiesComponent),
        generateValidatorProvider(SchemaFieldPropertiesComponent),
    ],
})
export class SchemaFieldPropertiesComponent extends BasePropertiesComponent<SchemaField> implements OnChanges {

    public readonly SchemaFieldPropertiesType = SchemaFieldPropertiesType;

    public readonly FieldType = FieldType;

    @Input({ required: true })
    public type: SchemaFieldPropertiesType;

    @Input()
    public ownName: string;

    @Input()
    public schemaNames: string[];

    @Input()
    public microschemaNames: string[];

    public validTypes: FieldType[];
    public validListTypes: FieldType[];
    public effectiveType: FieldType;

    protected override delayedSetup = true;

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.type) {
            this.validTypes = Object.values(this.type === SchemaFieldPropertiesType.SCHEMA
                ? FieldType
                : MicroschemaFieldType);
            this.validListTypes = Object.values(this.type === SchemaFieldPropertiesType.SCHEMA
                ? ListFieldType
                : MicroschemaListFieldType);
            if (this.form) {
                this.form.updateValueAndValidity();
            }
        }

        if (changes.ownName && !changes.ownName.firstChange && this.form) {
            this.form.controls.allow.updateValueAndValidity();
        }
    }

    protected createForm(): FormGroup<FormProperties<SchemaField>> {
        return new FormGroup<FormProperties<SchemaField>>({
            name: new FormControl(this.value?.name || '', [
                Validators.required,
                Validators.pattern(/^[a-zA-Z0-9_]+$/),
            ]),
            type: new FormControl(this.value?.type, [
                Validators.required,
                createWhitelistValidator(() => this.validTypes),
            ]),
            listType: new FormControl(this.value?.listType, [
                Validators.required,
                createWhitelistValidator(() => this.validListTypes),
            ]),
            label: new FormControl(this.value?.label || ''),
            required: new FormControl(this.value?.required ?? false),
            noIndex: new FormControl(this.value?.noIndex ?? false),
            allow: new FormControl(this.value?.allow || [], createWhitelistValidator(() => {
                switch (this.effectiveType) {
                    case FieldType.NODE:
                        return [...(this.schemaNames || []), this.ownName];
                    case FieldType.MICRONODE:
                        return [...(this.microschemaNames || []), this.ownName];

                    // Disable this validator otherwise
                    default:
                        return null;
                }
            })),
            elasticsearch: new FormControl(this.value?.elasticsearch || {}, createJsonValidator()),
            checkServiceUrl: new FormControl(this.value?.checkServiceUrl || ''),
            extract: new FormGroup<FormProperties<FieldExtractOptions>>({
                content: new FormControl(this.value?.extract?.content ?? true),
                metadata: new FormControl(this.value?.extract?.metadata ?? true),
            }),
        });
    }

    protected configureForm(value: SchemaField, loud?: boolean): void {
        const options = { emitEvent: !!loud, onlySelf: true };
        this.effectiveType = value?.type == null ? null : (value.type === FieldType.LIST ? value.listType : value.type);
        setControlsEnabled(this.form, ['listType'], value?.type === FieldType.LIST, options);
        setControlsEnabled(this.form, ['checkServiceUrl', 'extract'], value?.type === FieldType.BINARY, options);
        setControlsEnabled(this.form, ['allow'], TYPES_WITH_ALLOW.includes(this.effectiveType), options);
    }

    protected assembleValue(value: SchemaField): SchemaField {
        return value;
    }
}

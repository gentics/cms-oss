import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { EditableSchemaProperties } from '@gentics/mesh-models';
import { FormProperties, generateFormProvider } from '@gentics/ui-core';

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

    @Input()
    public mode: SchemaPropertiesMode;

    protected createForm(): FormGroup<FormProperties<EditableSchemaProperties>> {
        return new FormGroup<FormProperties<EditableSchemaProperties>>({
            name: new FormControl(this.value?.name, Validators.required),
            fields: new FormControl(this.value?.fields || []),
        });
    }

    protected configureForm(_value: EditableSchemaProperties, _loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: EditableSchemaProperties): EditableSchemaProperties {
        return value;
    }
}

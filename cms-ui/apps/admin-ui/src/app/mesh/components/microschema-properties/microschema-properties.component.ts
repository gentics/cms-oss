import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { EditableMicroschemaProperties } from '@gentics/mesh-models';
import { FormProperties, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

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

    @Input()
    public mode: MicroschemaPropertiesMode;

    protected createForm(): FormGroup<FormProperties<EditableMicroschemaProperties>> {
        return new FormGroup<FormProperties<EditableMicroschemaProperties>>({
            name: new FormControl(this.value?.name, Validators.required),
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

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { EditableUserTokenData } from '@gentics/mesh-models';
import { BaseFormPropertiesComponent, FormProperties, futureDateValidator, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

export interface MeshUserTokenFormData extends Omit<EditableUserTokenData, 'expires'> {
    expires?: number;
}

@Component({
    selector: 'gtx-mesh-user-token-properties',
    templateUrl: './mesh-user-token-properties.component.html',
    styleUrl: './mesh-user-token-properties.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(MeshUserTokenPropertiesComponent),
        generateValidatorProvider(MeshUserTokenPropertiesComponent),
    ],
    standalone: false,
})
export class MeshUserTokenPropertiesComponent extends BaseFormPropertiesComponent<MeshUserTokenFormData> {

    public readonly NOW = new Date();

    protected createForm(): FormGroup<FormProperties<MeshUserTokenFormData>> {
        return new FormGroup<FormProperties<MeshUserTokenFormData>>({
            name: new FormControl(this.safeValue('name'), Validators.required),
            expires: new FormControl(this.safeValue('expires'), {
                validators: [futureDateValidator],
            }),
        });
    }

    protected configureForm(_value: MeshUserTokenFormData, _loud?: boolean): void { }

    protected assembleValue(value: MeshUserTokenFormData): MeshUserTokenFormData {
        return value;
    }
}

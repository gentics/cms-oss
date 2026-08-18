import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { EditableUserTokenData } from '@gentics/mesh-models';
import { BaseFormPropertiesComponent, FormProperties, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

export interface UserTokenFormData extends Omit<EditableUserTokenData, 'expires'> {
    expires?: number;
}

@Component({
    selector: 'gtx-mesh-user-token-properties',
    templateUrl: './mesh-user-token-properties.component.html',
    styleUrl: './mesh-user-token-properties.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(MeshUserTokenPropertiesComponent),
        generateValidatorProvider(MeshUserTokenPropertiesComponent),
    ],
    standalone: false,
})
export class MeshUserTokenPropertiesComponent extends BaseFormPropertiesComponent<UserTokenFormData> {

    public readonly NOW = new Date();

    protected createForm(): FormGroup<FormProperties<UserTokenFormData>> {
        return new FormGroup<FormProperties<UserTokenFormData>>({
            name: new FormControl(this.safeValue('name'), Validators.required),
            expires: new FormControl(this.safeValue('expires')),
        });
    }

    protected configureForm(value: UserTokenFormData, loud?: boolean): void {

    }

    protected assembleValue(value: UserTokenFormData): UserTokenFormData {
        return value;
    }

}

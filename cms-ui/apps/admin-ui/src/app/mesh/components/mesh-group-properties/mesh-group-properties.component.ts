import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { EditableGroupProperties } from '@gentics/mesh-models';
import { FormProperties, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

export enum MeshGroupPropertiesMode {
    CREATE,
    EDIT,
}

@Component({
    selector: 'gtx-mesh-group-properties',
    templateUrl: './mesh-group-properties.component.html',
    styleUrls: ['./mesh-group-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(MeshGroupPropertiesComponent),
        generateValidatorProvider(MeshGroupPropertiesComponent),
    ],
})
export class MeshGroupPropertiesComponent extends BasePropertiesComponent<EditableGroupProperties> {

    public readonly MeshGroupPropertiesMode = MeshGroupPropertiesMode;

    @Input()
    public mode: MeshGroupPropertiesMode;

    protected createForm(): FormGroup<FormProperties<EditableGroupProperties>> {
        return new FormGroup<FormProperties<EditableGroupProperties>>({
            name: new FormControl(this.value?.name, Validators.required),
        });
    }

    protected configureForm(_value: EditableGroupProperties, _loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: EditableGroupProperties): EditableGroupProperties {
        return value;
    }
}

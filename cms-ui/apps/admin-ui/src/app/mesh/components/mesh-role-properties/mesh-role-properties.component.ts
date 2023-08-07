import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { EditableRoleProperties } from '@gentics/mesh-models';
import { FormProperties, generateFormProvider } from '@gentics/ui-core';

export enum MeshRolePropertiesMode {
    CREATE,
    EDIT,
}

@Component({
    selector: 'gtx-mesh-role-properties',
    templateUrl: './mesh-role-properties.component.html',
    styleUrls: ['./mesh-role-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(MeshRolePropertiesComponent)],
})
export class MeshRolePropertiesComponent extends BasePropertiesComponent<EditableRoleProperties> {

    public readonly MeshRolePropertiesMode = MeshRolePropertiesMode;

    @Input()
    public mode: MeshRolePropertiesMode;

    protected createForm(): FormGroup<FormProperties<EditableRoleProperties>> {
        return new FormGroup<FormProperties<EditableRoleProperties>>({
            name: new FormControl(this.value?.name, Validators.required),
        });
    }

    protected configureForm(_value: EditableRoleProperties, _loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: EditableRoleProperties): EditableRoleProperties {
        return value;
    }
}

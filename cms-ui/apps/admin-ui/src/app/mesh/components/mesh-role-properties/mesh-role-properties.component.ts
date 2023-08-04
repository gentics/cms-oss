import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { Role } from '@gentics/mesh-models';
import { generateFormProvider } from '@gentics/ui-core';

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
export class MeshRolePropertiesComponent extends BasePropertiesComponent<Role> {

    public readonly MeshRolePropertiesMode = MeshRolePropertiesMode;

    @Input()
    public mode: MeshRolePropertiesMode;

    protected createForm(): FormGroup<any> {
        return new FormGroup({
            name: new FormControl(this.value?.name, Validators.required),
        });
    }

    protected configureForm(value: Role, loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: Role): Role {
        return value;
    }
}

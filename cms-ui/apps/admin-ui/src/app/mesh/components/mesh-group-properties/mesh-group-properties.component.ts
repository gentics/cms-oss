import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { Group, Role } from '@gentics/mesh-models';
import { generateFormProvider } from '@gentics/ui-core';

export enum MeshGroupPropertiesMode {
    CREATE,
    EDIT,
}

@Component({
    selector: 'gtx-mesh-group-properties',
    templateUrl: './mesh-group-properties.component.html',
    styleUrls: ['./mesh-group-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(MeshGroupPropertiesComponent)],
})
export class MeshGroupPropertiesComponent extends BasePropertiesComponent<Group> {

    public readonly MeshGroupPropertiesMode = MeshGroupPropertiesMode;

    @Input()
    public mode: MeshGroupPropertiesMode;

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

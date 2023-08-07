import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent, createNestedControlValidator } from '@gentics/cms-components';
import { UserUpdateRequest } from '@gentics/mesh-models';
import { FormProperties, generateFormProvider, setControlsEnabled } from '@gentics/ui-core';

export enum MeshUserPropertiesMode {
    CREATE,
    EDIT,
}

@Component({
    selector: 'gtx-mesh-user-properties',
    templateUrl: './mesh-user-properties.component.html',
    styleUrls: ['./mesh-user-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(MeshUserPropertiesComponent)],
})
export class MeshUserPropertiesComponent extends BasePropertiesComponent<UserUpdateRequest> implements OnChanges {

    public readonly MeshUserPropertiesMode = MeshUserPropertiesMode;

    @Input()
    public mode: MeshUserPropertiesMode;

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.mode && this.form) {
            setControlsEnabled(this.form, ['password'], this.mode === MeshUserPropertiesMode.CREATE, { emitEvent: false });
        }
    }

    protected createForm(): FormGroup<FormProperties<UserUpdateRequest>> {
        return new FormGroup<FormProperties<UserUpdateRequest>>({
            username: new FormControl(this.value?.username, Validators.required),
            firstname: new FormControl(this.value?.firstname),
            lastname: new FormControl(this.value?.lastname),
            emailAddress: new FormControl(this.value?.emailAddress, Validators.email),
            forcedPasswordChange: new FormControl(this.value?.forcedPasswordChange ?? false),
            password: new FormControl(this.value?.password || '', [Validators.required, createNestedControlValidator()]),
        });
    }

    protected configureForm(_value: UserUpdateRequest, _loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: UserUpdateRequest): UserUpdateRequest {
        return value;
    }
}

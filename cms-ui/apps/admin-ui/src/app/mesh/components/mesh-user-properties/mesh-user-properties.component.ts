import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent, createNestedControlValidator } from '@gentics/cms-components';
import { UserUpdateRequest } from '@gentics/mesh-models';
import { FormProperties, generateFormProvider, setControlsEnabled } from '@gentics/ui-core';

export enum MeshUserPropertiesMode {
    CREATE = 'create',
    EDIT = 'edit',
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

    public updatePassword = false;

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        let didUpdatePassword = false;
        if (changes.initialValue && this.initialValue) {
            this.updatePassword = false;
            this.updatePasswordVisibility(false);
            didUpdatePassword = true;
        }

        if (changes.mode && this.form) {
            if (!didUpdatePassword) {
                this.updatePasswordVisibility(true);
                didUpdatePassword = true;
            }
        }
    }

    protected createForm(): FormGroup<FormProperties<UserUpdateRequest>> {
        const out = new FormGroup<FormProperties<UserUpdateRequest>>({
            username: new FormControl(this.value?.username, Validators.required),
            firstname: new FormControl(this.value?.firstname),
            lastname: new FormControl(this.value?.lastname),
            emailAddress: new FormControl(this.value?.emailAddress, Validators.email),
            admin: new FormControl(this.value?.admin ?? false),
            forcedPasswordChange: new FormControl(this.value?.forcedPasswordChange ?? false),
            password: new FormControl({
                value: this.value?.password || '',
                disabled: this.mode !== MeshUserPropertiesMode.CREATE,
            }, [
                Validators.required,
                createNestedControlValidator(),
            ]),
        });

        return out;
    }

    protected configureForm(_value: UserUpdateRequest, loud?: boolean): void {
        this.updatePasswordVisibility(loud ?? false);
    }

    protected assembleValue(value: UserUpdateRequest): UserUpdateRequest {
        return value;
    }

    public togglePasswordSet(): void {
        this.updatePassword = !this.updatePassword;
        this.updatePasswordVisibility();
        this.changeDetector.markForCheck();
    }

    protected updatePasswordVisibility(loud: boolean = false): void {
        setControlsEnabled(this.form, ['password'], this.mode === MeshUserPropertiesMode.CREATE || this.updatePassword, { emitEvent: loud });

        this.form.updateValueAndValidity();
        this.changeDetector.markForCheck();
    }
}

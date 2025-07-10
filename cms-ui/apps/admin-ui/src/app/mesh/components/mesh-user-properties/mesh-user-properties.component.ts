import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { UserUpdateRequest } from '@gentics/mesh-models';
import { FormProperties, generateFormProvider, generateValidatorProvider, setControlsEnabled } from '@gentics/ui-core';

export enum MeshUserPropertiesMode {
    CREATE = 'create',
    EDIT = 'edit',
}

@Component({
    selector: 'gtx-mesh-user-properties',
    templateUrl: './mesh-user-properties.component.html',
    styleUrls: ['./mesh-user-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(MeshUserPropertiesComponent),
        generateValidatorProvider(MeshUserPropertiesComponent),
    ],
    standalone: false
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
            username: new FormControl(this.safeValue('username'), Validators.required),
            firstname: new FormControl(this.safeValue('firstname')),
            lastname: new FormControl(this.safeValue('lastname')),
            emailAddress: new FormControl(this.safeValue('emailAddress'), Validators.email),
            admin: new FormControl(this.safeValue('admin') ?? false),
            forcedPasswordChange: new FormControl(this.safeValue('forcedPasswordChange') ?? false),
            password: new FormControl({
                value: this.safeValue('password') || '',
                disabled: this.mode !== MeshUserPropertiesMode.CREATE,
            }, Validators.required),
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

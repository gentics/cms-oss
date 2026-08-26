import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { EditableApiToken } from '@gentics/cms-models';
import { BaseFormPropertiesComponent, FormProperties, futureDateValidator, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

@Component({
    selector: 'gtx-api-tokens-create-form',
    templateUrl: './api-tokens-create-form.component.html',
    styleUrls: ['./api-tokens-create-form.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(ApiTokensCreateFormComponent),
        generateValidatorProvider(ApiTokensCreateFormComponent),
    ],
    standalone: false,
})
export class ApiTokensCreateFormComponent extends BaseFormPropertiesComponent<EditableApiToken> {

    public readonly NOW = new Date();

    @Input()
    readonly apiTokenNames: Array<string>;

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
    }

    protected override createForm(): FormGroup<FormProperties<EditableApiToken>> {
        return new FormGroup<FormProperties<EditableApiToken>>({
            name: new FormControl(this.safeValue('name'), {
                validators: [Validators.required, this.uniqueNameValidator],
            }),
            expires: new FormControl(this.safeValue('expires'), {
                validators: [futureDateValidator],
            }),
        });
    }

    protected override configureForm(_value: EditableApiToken, _loud?: boolean): void {}

    protected override assembleValue(value: EditableApiToken): EditableApiToken {
        return value;
    }

    private readonly uniqueNameValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
        if (!control.value) {
            return null;
        }

        return this.apiTokenNames.includes(control.value) ? { uniqueName: true } : null;
    };
}

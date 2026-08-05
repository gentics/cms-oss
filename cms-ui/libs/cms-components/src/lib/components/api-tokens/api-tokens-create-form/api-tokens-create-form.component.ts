import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output, signal, SimpleChanges } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { ApiTokenHandlerService } from '@gentics/cms-components';
import { EditableApiTokenPackage } from '@gentics/cms-models';
import { BaseFormPropertiesComponent, FormProperties, generateFormProvider, generateValidatorProvider, IModalDialog } from '@gentics/ui-core';


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
export class ApiTokensCreateFormComponent extends BaseFormPropertiesComponent<EditableApiTokenPackage> {
    @Input()
    private readonly apiTokenNames: Array<string>;

    @Output()
    onValidChange = new EventEmitter<boolean>();
    
    @Output()
    onCreated = new EventEmitter<string | null>();

    constructor(
        changeDetector: ChangeDetectorRef,
        protected handler: ApiTokenHandlerService
    ) {
        super(changeDetector);
    }

    override ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(
            this.form.statusChanges.subscribe(() => {
                this.onValidChange.emit(this.form.valid);
            })
        );
    }

    protected override createForm(): FormGroup<FormProperties<EditableApiTokenPackage>> {
        return new FormGroup<FormProperties<EditableApiTokenPackage>>({
            name: new FormControl(this.safeValue('name'), {
                validators: [Validators.required, this.uniqueNameValidator]
            }),
            expires: new FormControl<string | null>(null, {
                validators: [this.futureDateValidator]
            }),
        });
    }

    protected override configureForm(value: EditableApiTokenPackage, loud?: boolean): void {}

    protected override assembleValue(value: EditableApiTokenPackage): EditableApiTokenPackage {
        return value;
    }

    public addApiToken(): void {
        const { name, expires } = this.form.getRawValue();

        const submitData = {
            name,
            ...(expires !== null ? { expires: this.dateToDateNumber(expires) } : {}),
        };

        this.handler.create(submitData)
            .subscribe({
                next: (newToken) => {
                    this.onCreated.emit(newToken.token);
                },
                complete: () => this.onCreated.emit(null)
            });
    }

    private readonly futureDateValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
        if (!control.value) {
            return null;
        }

        const selected = new Date(control.value);
        selected.setHours(0, 0, 0, 0);

        const today = new Date();
        today.setHours(0, 0, 0, 0);

        return selected > today ? null : { futureDate: true };
    };

    private readonly uniqueNameValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
        if (!control.value) {
            return null;
        }

        return this.apiTokenNames.includes(control.value) ? { uniqueName: true } : null;
    };

    private dateToDateNumber(dateString: string): number {
        return Math.floor(new Date(dateString).getTime() / 1000);
    }
}

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { AbstractControl, ValidationErrors, Validator } from '@angular/forms';
import { BaseFormElementComponent, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

@Component({
    selector: 'gtx-password-confirm-input',
    templateUrl: './password-confirm-input.component.html',
    styleUrls: ['./password-confirm-input.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(PasswordConfirmInputComponent),
        generateValidatorProvider(PasswordConfirmInputComponent),
    ],
})
export class PasswordConfirmInputComponent
    extends BaseFormElementComponent<string>
    implements OnChanges, Validator {

    @Input()
    public allowVisibilty = true;

    @Input()
    public visible = false;

    @Input()
    public confirmLabel: string = null;

    @Input()
    public missmatchText: string = null;

    @Input()
    public initialValue = false;

    @Output()
    public visibleChange = new EventEmitter<boolean>();

    public actualPassword = '';
    public confirmationValue = '';
    public valid = true;

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
        this.booleanInputs.push('initialValue');
    }

    public ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.intiialValue && this.initialValue) {
            this.updateConfirmation('');
        }
    }

    validate(control: AbstractControl<any, any>): ValidationErrors {
        if (this.valid) {
            return null;
        }
        return {
            confirmationNoMatch: true,
        };
    }

    updatePassword(value: string): void {
        this.actualPassword = value;
        this.triggerTouch();
        this.checkAndTrigger();
    }

    updateConfirmation(value: string): void {
        this.confirmationValue = value;
        this.triggerTouch();
        this.checkAndTrigger();
    }

    toggleVisibility(): void {
        if (!this.pure) {
            this.visible = !this.visible;
        }
        this.visibleChange.emit(!this.visible);
    }

    checkAndTrigger(): void {
        this.valid = this.actualPassword === '' || this.actualPassword === this.confirmationValue;
        this.triggerChange(this.actualPassword);
    }

    protected onValueChange(): void {
        this.actualPassword = this.value || '';
    }
}

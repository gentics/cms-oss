import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CONTROL_INVALID_VALUE } from '@gentics/cms-components';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';

@Component({
    selector: 'gtx-password-confirm-input',
    templateUrl: './password-confirm-input.component.html',
    styleUrls: ['./password-confirm-input.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(PasswordConfirmInputComponent)],
})
export class PasswordConfirmInputComponent
    extends BaseFormElementComponent<string | typeof CONTROL_INVALID_VALUE>
    implements OnChanges {

    @Input()
    public allowVisibilty = true;

    @Input()
    public visible = false;

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

    updatePassword(value: string): void {
        this.actualPassword = value;
        this.checkAndTrigger();
    }

    updateConfirmation(value: string): void {
        this.confirmationValue = value;
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
        this.triggerChange(this.valid
            ? this.actualPassword
            : CONTROL_INVALID_VALUE,
        );
    }

    protected onValueChange(): void {
        if (this.value === CONTROL_INVALID_VALUE) {
            return;
        }
        this.actualPassword = this.value || '';
    }
}

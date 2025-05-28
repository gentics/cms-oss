import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CHECKBOX_STATE_INDETERMINATE, CheckboxState } from '../../common';
import { cancelEvent, coerceToBoolean, generateFormProvider, randomId } from '../../utils';
import { BaseFormElementComponent } from '../base-form-element/base-form-element.component';

function normalizeValue(value: any): CheckboxState {
    return (typeof value === 'boolean' || value === CHECKBOX_STATE_INDETERMINATE)
        ? value
        : coerceToBoolean(value);
}

/**
 * Checkbox wraps the native `<input type="checkbox">` form element.
 *
 * ```ts
 * const CHECKBOX_STATE_INDETERMINATE = 'indeterminate';
 * type CheckState = boolean | typeof CHECKBOX_STATE_INDETERMINATE;
 * ```
 *
 * ```html
 * <gtx-checkbox
 *     label="Model binding"
 *     [(ngModel)]="isOkay"
 * ></gtx-checkbox>
 *
 * <gtx-checkbox
 *     label="Direct binding"
 *     [value]="checkStates.B"
 *     (valueChange)="updateCheckStates('B')"
 * ></gtx-checkbox>
 *
 * <gtx-checkbox
 *     label="Form binding"
 *     formControlName="myCheckbox"
 * ></gtx-checkbox>
 * ```
 */
@Component({
    selector: 'gtx-checkbox',
    templateUrl: './checkbox.component.html',
    styleUrls: ['./checkbox.component.scss'],
    providers: [generateFormProvider(CheckboxComponent)],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CheckboxComponent extends BaseFormElementComponent<CheckboxState> {

    public readonly CHECKBOX_STATE_INDETERMINATE = CHECKBOX_STATE_INDETERMINATE;

    /**
     * Sets the checkbox to be auto-focused. Handled by `AutofocusDirective`.
     */
    @Input()
    public autofocus = false;

    /**
     * Checkbox ID
     */
    @Input()
    public id = `checkbox-${randomId()}`;

    /**
     * Form name for the checkbox
     */
    @Input()
    public name: string;

    /**
     * The state/value of this checkbox
     */
    @Input({ transform: normalizeValue })
    public value: CheckboxState = false;

    /**
     * The value to be bound to the `value` property of the native checkbox.
     *
     * @see https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/input#value
     * @see https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/input#checked
     */
    @Input()
    public formValue: string | null = null;

    protected onValueChange(): void {
        // no-op
    }

    public override writeValue(value: CheckboxState): void {
        super.writeValue(normalizeValue(value));
    }

    public toggleState(event: Event): void {
        // Always cancel the event, so the input doesn't change itself
        cancelEvent(event);

        // Ignore if disabled, then ignore
        if (this.disabled) {
            return;
        }

        this.triggerTouch();
        const newState: CheckboxState = this.value === CHECKBOX_STATE_INDETERMINATE ? true : !this.value;
        this.triggerChange(newState);
    }

    public handleBlur(): void {
        this.triggerTouch();
    }
}

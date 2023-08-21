import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { JSON_VALUE_INVALID } from '../../common';
import { generateFormProvider } from '../../utils';
import { BaseFormElementComponent } from '../base-form-element/base-form-element.component';

/**
 * Simple wrapper component around the `TextareaComponent`, which handles JSON inputs.
 * Only triggers a change on blur (to prevent changes triggering whie editing the content),
 * and emits a special `JSON_VALUE_INVALID` flag on invalid values.
 * Therefore allows proper `null` values to be set as well.
 * Validation should be done via the `createJsonValidator` utility function/validator.
 *
 * The JSON parsing can be done in this component (default) or on the parent component, via
 * the `raw` flag.
 */
@Component({
    selector: 'gtx-json-input',
    templateUrl: './json-input.component.html',
    styleUrls: ['./json-input.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(JsonInputComponent)],
})
export class JsonInputComponent extends BaseFormElementComponent<typeof JSON_VALUE_INVALID | string | Record<string, any> | any[]> {

    /** If this input should convert the value to a JSON Object/Array when emitting values. */
    @Input()
    public raw = false;

    /**
     * Sets the textarea to be auto-focused. Handled by `AutofocusDirective`.
     */
    @Input()
    public autofocus = false;

    /**
     * Sets the readonly state.
     */
    @Input()
    public readonly = false;

    /**
     * A placeholder text to display when the control is empty.
     */
    @Input()
    public placeholder: string;

    /**
     * The name of the control.
     */
    @Input()
    public name: string;

    /**
     * Sets an id for the control.
     */
    @Input()
    public id: string;

    public rawValue: string | null = null;

    protected onValueChange(): void {
        // Nothing to update
        if (this.value === JSON_VALUE_INVALID) {
            return;
        }

        if ((this.value == null) || (typeof this.value === 'string')) {
            this.rawValue = this.value as string ?? null;
            return;
        }

        if (this.raw) {
            this.rawValue = this.value as unknown as any;
        } else {
            this.rawValue = JSON.stringify(this.value, null, 4);
        }
    }

    public updateRawValue(value: string): void {
        this.rawValue = value;
        this.triggerTouch();
    }

    public handleBlur(): void {
        this.triggerTouch();

        if (this.raw) {
            this.triggerChange(this.rawValue);
            return;
        }

        // Trigger null only once
        if ((this.rawValue == null || this.rawValue.trim().length === 0) && this.value != null) {
            this.triggerChange(null);
            return;
        }

        try {
            const parsed = JSON.parse(this.rawValue);
            this.triggerChange(parsed);
        } catch (err) {
            this.triggerChange(JSON_VALUE_INVALID);
        }
    }
}

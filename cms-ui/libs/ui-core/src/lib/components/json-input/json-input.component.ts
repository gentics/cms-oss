import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { AbstractControl, ValidationErrors, Validator } from '@angular/forms';
import { JsonValidationErrorModel, VALIDATOR_JSON_ERROR_PROPERTY } from '../../common';
import { generateFormProvider, generateValidatorProvider } from '../../utils';
import { BaseFormElementComponent } from '../base-form-element/base-form-element.component';

type JsonValue = null | string | Record<string, any> | number | boolean;

/**
 * Simple wrapper component around the `TextareaComponent`, which handles JSON inputs.
 * Only triggers a change on blur (to prevent changes triggering whie editing the content),
 * and emits a special `JSON_VALUE_INVALID` flag on invalid values.
 * Therefore allows proper `null` values to be set as well.
 * This component will register itself as a validator for itself.
 *
 * The JSON parsing can be done in this component (default) or on the parent component, via
 * the `raw` flag.
 */
@Component({
    selector: 'gtx-json-input',
    templateUrl: './json-input.component.html',
    styleUrls: ['./json-input.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(JsonInputComponent),
        generateValidatorProvider(JsonInputComponent),
    ],
    standalone: false,
})
export class JsonInputComponent
    extends BaseFormElementComponent<JsonValue | JsonValue[]>
    implements Validator {

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

    /** The control to which this component is bound to. */
    protected boundControl: AbstractControl<any, any>;

    protected onValueChange(): void {
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
            this.triggerChange(this.rawValue);
        }
    }

    /** Validation implementation to validate itself */
    public validate(control: AbstractControl<any, any>): ValidationErrors {
        this.boundControl = control;
        const value = control.value;
        const validationError: JsonValidationErrorModel = {
            [VALIDATOR_JSON_ERROR_PROPERTY]: {
                actualValue: value,
            },
        };

        if (typeof value === 'string') {
            // if input is empty, there is no error
            if (!value) {
                return null;
            }
            let parsed: object;
            try {
                parsed = JSON.parse(value);
                if (parsed != null && typeof parsed === 'object') {
                    return null;
                }
            } catch (error) {
                return validationError;
            }
        } else if (value == null) {
            // null values are valid
            return null;
        } else if (typeof value === 'object') {
            // object value set, therefore valid
            return null;
        }

        // if in doubt, return error
        return validationError;
    }
}

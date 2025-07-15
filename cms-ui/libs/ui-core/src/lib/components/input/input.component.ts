import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
} from '@angular/core';
import { cancelEvent, generateFormProvider } from '../../utils';
import { BaseFormElementComponent } from '../base-form-element/base-form-element.component';

/**
 * E-mail validator regex from Angular 8
 *
 * @todo Implement with validators
 * @see https://github.com/angular/angular/blob/8.2.9/packages/forms/src/validators.ts#L60
 */
const EMAIL_REGEXP =
    '^(?=.{1,254}$)(?=.{1,64}@)[-!#$%&\'*+/0-9=?A-Z^_`a-z{|}~]'+
    '+(\\.[-!#$%&\'*+/0-9=?A-Z^_`a-z{|}~]+)*@[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$';

/**
 * Telephone number validator regex
 *
 * @todo Implement with validators
 * @see https://stackoverflow.com/a/26516985
 */
const TEL_REGEXP = '^([()\\- x+]*d[()\\- x+]*){4,16}$';

/**
 * URL validator regex
 *
 * @todo Implement with validators
 * @see https://stackoverflow.com/a/52017332
 */
const URL_REGEXP = '(^|\\s)((https?:\\/\\/)?[\\w-]+(\\.[\\w-]+)+\\.?(:\\d+)?(\\/\\S*)?)';

/**
 * The InputField wraps the native `<input>` form element but should only be used for
 * text, number, password, tel, email or url types. Other types (date, range, file) should have dedicated components.
 *
 *
 * Note that the class is named `InputField` since `Input` is used by the Angular framework to denote
 * component inputs.
 *
 * ```html
 * <gtx-input label="Text Input Label"></gtx-input>
 * <gtx-input placeholder="Number Input Placeholder"
 *            type="number" min="0" max="100" step="5"></gtx-input>
 * ```
 */
@Component({
    selector: 'gtx-input',
    templateUrl: './input.component.html',
    styleUrls: ['./input.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(InputComponent)],
})
export class InputComponent extends BaseFormElementComponent<string | number> implements OnInit {

    /**
     * Sets autocomplete attribute on the input field
     */
    @Input()
    public autocomplete: string;

    /**
     * Sets the input field to be auto-focused. Handled by `AutofocusDirective`.
     */
    @Input()
    public autofocus = false;

    /**
     * Input field id
     */
    @Input()
    public id?: string;

    /**
     * Max allowed value (applies when type = "number")
     */
    @Input()
    public max?: number;

    /**
     * Min allowed value (applies when type = "number")
     */
    @Input()
    public min?: number;

    /**
     * Max allowed length in characters
     */
    @Input()
    public maxlength?: number;

    /**
     * Input field name
     */
    @Input()
    public name?: string;

    /**
     * Regex pattern for complex validation
     * @deprecated For proper validation, the Angular Forms API and a dedicated Validator
     * should be used instead.
     */
    @Input()
    public pattern?: string;

    /**
     * Placeholder text to display when the field is empty
     */
    @Input()
    public placeholder: string;

    /**
     * Wether the element should be clearable or not
     */
    @Input()
    public clearable = false;

    /**
     * Sets the readonly state of the input
     */
    @Input()
    public readonly = false;

    /**
     * Increment step (applies when type is `'number'`)
     */
    @Input()
    public step?: number;

    /**
     * Can be "text", "number", "password", "tel", "email" or "url".
     */
    @Input()
    public type: 'text' | 'number' | 'password' | 'tel' | 'email' | 'url' = 'text';

    /**
     * Fires when the input loses focus.
     * @deprecated Will be removed in next major version, and therefore a bubbled `blur` event from the
     * browser may occur instead, which has a different value.
     */
    @Output()
    public blur = new EventEmitter<string | number>();

    /**
     * Fires when the input gains focus.
     * @deprecated Will be removed in next major version, and therefore a bubbled `focus` event from the
     * browser may occur instead, which has a different value.
     */
    @Output()
    public focus = new EventEmitter<string | number>();

    /**
     * Fires whenever a char is entered into the field.
     * @deprecated Use `valueChange` instead.
     */
    @Output()
    public change = new EventEmitter<string | number>();

    /**
     * Fires when the value has been cleared via the `clearable` button.
     */
    @Output()
    public valueCleared = new EventEmitter<void>();

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
        this.booleanInputs.push('clearable', 'readonly');
    }

    public ngOnInit(): void {
        /**
         * Set default regex patterns for specific field types if not set
         */
        if (!this.pattern) {
            switch (this.type) {
                case 'email':
                    this.pattern = EMAIL_REGEXP;
                    break;
                case 'tel':
                    this.pattern = TEL_REGEXP;
                    break;
                case 'url':
                    this.pattern = URL_REGEXP;
                    break;
                default:
            }
        }
    }

    protected onValueChange(): void {
        // No op
    }

    public override triggerChange(value: string | number): void {
        super.triggerChange(value);
        this.change.emit(this.getFinalValue());
    }

    public handleKeyDown(event: KeyboardEvent): void {
        // Why?
        if (this.type === 'number') {
            if ((event.key === '-' && this.value) || event.key === '+') {
                event.preventDefault();
            } else if (event.key === '-' && !this.value) {
                this.value = '-';
            }
        }
    }

    public handleInputChange(event: InputEvent): void {
        cancelEvent(event);

        this.triggerTouch();
        const value = (event.target as HTMLInputElement).value || '';

        if (this.type !== 'number') {
            this.triggerChange(value);
            return;
        }
        if (value === '') {
            this.triggerChange(null);
            return;
        }

        let parsed = parseFloat(value);
        if (!isFinite(parsed) || isNaN(parsed)) {
            // Invalid value, but ignore for now.
            // If we update the value, we get wonky user behaviour, and triggering a non-sensical
            // change is also not worth it.
            return;
        }

        // Clamp the value if neccessary
        if (this.max != null && parsed > this.max) {
            parsed = this.max;
        }
        if (this.min != null && parsed < this.min) {
            parsed = this.min;
        }

        this.triggerChange(parsed);
    }

    public onBlur(e: Event): void {
        cancelEvent(e);
        this.triggerTouch();
        this.blur.emit(this.value);
    }

    public onFocus(e: Event): void {
        this.focus.emit(this.value);
    }

    public clear(): void {
        this.triggerChange(this.type === 'number' ? null : '');
        this.valueCleared.emit();
    }
}

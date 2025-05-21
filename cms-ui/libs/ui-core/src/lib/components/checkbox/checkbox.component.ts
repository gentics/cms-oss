import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    HostListener,
    Input,
    Output,
    Renderer2,
    ViewChild,
} from '@angular/core';
import { CHECKBOX_STATE_INDETERMINATE, CheckboxState, KeyCode } from '../../common';
import { generateFormProvider, randomId } from '../../utils';
import { BaseFormElementComponent } from '../base-form-element/base-form-element.component';

/**
 * Checkbox wraps the native `<input type="checkbox">` form element.
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
     * Checked state of the checkbox. When set, the Checkbox will be
     * in stateless mode.
     *
     * @deprecated Use the `value` Input instead and use the `pure` Input to control stateless mode.
     */
    @Input()
    public get checked(): boolean {
        return this.value === true;
    }
    public set checked(value: boolean) {
        this.statelessMode = true;
        this.compatibilityMode = true;

        const val: CheckboxState | 'true' | '' = <any> value;
        const nowChecked = val === true || <any> val === 'true' || <any> val === '';
        if (nowChecked !== this.value) {
            this.triggerChange(nowChecked);
            this.change.emit(this.value);
        }
    }

    /**
     * Set to "indeterminate" for an indeterminate state (-).
     *
     * @deprecated Use the `value` Input instead and provide the indeterminate value.
     */
    @Input()
    public get indeterminate(): boolean {
        return this.value === CHECKBOX_STATE_INDETERMINATE;
    }
    public set indeterminate(val: boolean) {
        this.compatibilityMode = true;

        if (val !== (this.value === CHECKBOX_STATE_INDETERMINATE)) {
            const newVal = val ? CHECKBOX_STATE_INDETERMINATE : false;
            this.triggerChange(newVal);
            this.change.emit(this.value);
        }
    }

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
     * Blur event
     *
     * @deprecated Use the `valueChange` Output to detect changes.
     */
    @Output()
    public blur = new EventEmitter<CheckboxState>();

    /**
     * Focus event
     *
     * @deprecated Use the `valueChange` Output to detect changes.
     */
    @Output()
    public focus = new EventEmitter<CheckboxState>();

    /**
     * Change event
     *
     * @deprecated Use the `valueChange` Output to detect changes.
     */
    @Output()
    public change = new EventEmitter<CheckboxState>();

    /**
     * @deprecated Focus is focus, a differentiation between mouse and keyboard focus
     * is not what we want to support/encourage.
     */
    public tabbedFocus = false;

    /**
     * See note above on stateless mode.
     */
    private statelessMode = false;

    /**
     * @deprecated Flag for compatibility of the old (deprecated) functionality.
     */
    protected compatibilityMode = false;

    @ViewChild('input', { static: true })
    public inputElement: ElementRef<HTMLInputElement>;

    constructor(
        changeDetector: ChangeDetectorRef,
        private renderer: Renderer2,
    ) {
        super(changeDetector);
    }

    onBlur(): void {
        this.blur.emit(this.value);
        this.triggerTouch();
        this.tabbedFocus = false;
    }

    onFocus(): void {
        this.focus.emit(this.value);
    }

    protected onValueChange(): void {
        // The binding via `value` doesn't properly work, therefore we have to do it manually
        // with the renderer.
        this.renderer.setProperty(this.inputElement.nativeElement, 'checked', this.value === CHECKBOX_STATE_INDETERMINATE ? null : this.value);
        this.renderer.setProperty(this.inputElement.nativeElement, 'indeterminate', this.value === CHECKBOX_STATE_INDETERMINATE);
    }

    @HostListener('keyup', ['$event'])
    focusHandler(e: KeyboardEvent): void {
        if (e.keyCode === KeyCode.Tab) {
            if (!this.tabbedFocus) {
                this.tabbedFocus = true;
            }
        }
    }

    toggleState(input: HTMLInputElement): void {
        const newState: CheckboxState = this.value === CHECKBOX_STATE_INDETERMINATE ? true : !this.value;

        if (!this.compatibilityMode) {
            this.triggerChange(newState);
            this.change.emit(this.value);
            return;
        }

        if (this.statelessMode) {
            if (input.checked !== this.value) {
                input.checked = !!this.value;
            }
            return;
        }
        if (newState !== this.value) {
            this.triggerChange(newState);
            this.change.emit(this.value);
        }
    }
}

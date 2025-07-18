import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    HostListener,
    Input,
    OnInit,
    Output,
    ViewChild,
} from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';
import { CHECKBOX_STATE_INDETERMINATE, CheckboxState, KeyCode } from '../../common';
import { generateFormProvider, randomId } from '../../utils';

/**
 * Checkbox wraps the native `<input type="checkbox">` form element.
 *
 * ```html
 * <gtx-checkbox [(ngModel)]="isOkay" label="Is it okay?"></gtx-checkbox>
 * <gtx-checkbox [(ngModel)]="checkStates.B" value="B" label="B"></gtx-checkbox>
 * ```
 *
 * ## Stateless Mode
 * By default, the Checkbox keeps track of its own internal checked state. This makes sense
 * for most use cases, such as when used in a form bound to NgControl.
 *
 * However, in some cases we want to explicitly set the state from outside. This is done by binding
 * to the <code>checked</code> attribute. When this attribute is bound, the checked state of the
 * Checkbox will *only* change when the value of the binding changes. Clicking on the Checkbox
 * will have no effect other than to emit an event which the parent can use to update the binding.
 *
 * Here is a basic example of a stateless checkbox where the parent component manages the state:
 *
 * ```html
 * <gtx-checkbox [checked]="isChecked"
 *               (change)="isChecked = $event"></gtx-checkbox>
 * ```
 */
@Component({
    selector: 'gtx-checkbox',
    templateUrl: './checkbox.component.html',
    styleUrls: ['./checkbox.component.scss'],
    providers: [generateFormProvider(CheckboxComponent)],
    standalone: false
})
export class CheckboxComponent implements ControlValueAccessor, OnInit, AfterViewInit {
    /**
     * Sets the checkbox to be auto-focused. Handled by `AutofocusDirective`.
     */
    @Input() autofocus = false;

    /**
     * Checked state of the checkbox. When set, the Checkbox will be
     * in stateless mode.
     */
    @Input() get checked(): boolean {
        return this.checkState === true;
    }
    set checked(value: boolean) {
        this.statelessMode = true;
        let val: boolean | 'true' | '' | 'indeterminate' = <any> value;
        let nowChecked = val === true || <any> val === 'true' || <any> val === '';
        if (nowChecked != this.checkState) {
            this.onChange(this.checkState = nowChecked);
            this.changeDetector.markForCheck();
        }
    }

    /**
     * Set to "indeterminate" for an indeterminate state (-)
     */
    @Input() get indeterminate(): boolean {
        return this.checkState === CHECKBOX_STATE_INDETERMINATE;
    }
    set indeterminate(val: boolean) {
        if (val != (this.checkState === CHECKBOX_STATE_INDETERMINATE)) {
            this.checkState = val ? CHECKBOX_STATE_INDETERMINATE : false;
            this.change.emit(this.checkState);
            this.onChange(this.checkState);
        }
    }

    /**
     * Set the checkbox to its disabled state.
     */
    @Input() disabled = false;
    /**
     * Checkbox ID
     */
    @Input() id = `checkbox-${randomId()}`;
    /**
     * Label for the checkbox
     */
    @Input() label = '';
    /**
     * Form name for the checkbox
     */
    @Input() name: string;
    /**
     * Sets the required property
     */
    @Input() required = false;
    /**
     * The value of the checkbox
     */
    @Input() value: any = '';

    /**
     * Blur event
     */
    @Output() blur = new EventEmitter<CheckboxState>();
    /**
     * Focus event
     */
    @Output() focus = new EventEmitter<CheckboxState>();
    /**
     * Change event
     */
    @Output() change = new EventEmitter<CheckboxState>();

    checkState: CheckboxState = false;
    tabbedFocus = false;

    @ViewChild('labelElement', { static: true })
    labelElement: ElementRef<HTMLLabelElement>;

    /**
     * See note above on stateless mode.
     */
    private statelessMode = false;

    constructor(private changeDetector: ChangeDetectorRef) { }

    onBlur(): void {
        this.blur.emit(this.checkState);
        this.onTouched();
        this.tabbedFocus = false;
    }

    onFocus(): void {
        this.focus.emit(this.checkState);
    }

    @HostListener('keyup', ['$event'])
    focusHandler(e: KeyboardEvent): void {
        if (e.keyCode === KeyCode.Tab) {
            if (!this.tabbedFocus) {
                this.tabbedFocus = true;
            }
        }
    }

    writeValue(value: any): void {
        if (value !== this.checkState) {
            this.checkState = value;
            this.changeDetector.markForCheck();
        }
    }

    ngOnInit(): void {
        this.onChange(this.checkState);
    }

    ngAfterViewInit(): void {
        this.fixInitialAnimation();
    }

    onInputChanged(e: Event, input: HTMLInputElement): boolean {
        if (e) {
            e.stopPropagation();
        }
        let newState: CheckboxState = input.indeterminate ? 'indeterminate' : input.checked;
        if (this.statelessMode) {
            if (input.checked !== this.checkState) {
                input.checked = !!this.checkState;
            }
            this.change.emit(newState);
            return false;
        }
        if (newState != this.checkState) {
            this.checkState = newState;
            this.onChange(newState);
            this.change.emit(newState);
            return true;
        }
    }

    registerOnChange(fn: Function): void { this.onChange = fn; }
    registerOnTouched(fn: Function): void { this.onTouched = fn; }
    setDisabledState(disabled: boolean): void {
        this.disabled = disabled;
        this.changeDetector.markForCheck();
    }

    private onChange: Function = () => {};
    private onTouched: Function = () => {};

    /**
     * This is a hacky fix to prevent Materialize from animating ticked checkboxes which
     * kicks in when a checkbox is added to the dom with checked=false and immediately
     * set to checked=true.
     */
    private fixInitialAnimation(): void {
        if (this.labelElement && this.labelElement.nativeElement) {
            let label = this.labelElement.nativeElement;
            label.style.display = 'none';
            let ignored = label.offsetWidth;
            label.style.display = '';
        }
    }
}

import {
    Attribute,
    ChangeDetectorRef,
    Component,
    EventEmitter, HostListener,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Optional,
    Output,
    SimpleChanges,
} from '@angular/core';
import { isEqual } from 'lodash-es';
import { KeyCode } from '../../common';
import { RadioGroupDirective } from '../../directives/radio-group/radio-group.directive';
import { generateFormProvider, randomId } from '../../utils';
import { BaseFormElementComponent } from '../base-form-element/base-form-element.component';

const NO_SET = Symbol();

/**
 * RadioButton wraps the native `<input type="radio">` form element.
 * To connect multiple radio buttons with a form via ngModel,
 * wrap them in a {@link RadioGroupDirective} (`<gtx-radio-group>`).
 *
 * ```html
 * <gtx-radio-button [(ngModel)]="val" value="A" label="A"></gtx-radio-button>
 * <gtx-radio-button [(ngModel)]="val" value="B" label="B"></gtx-radio-button>
 * <gtx-radio-button [(ngModel)]="val" value="C" label="C"></gtx-radio-button>
 * ```
 *
 * ## Stateless Mode
 * By default, the RadioButton keeps track of its own internal checked state. This makes sense
 * for most use cases, such as when used in a form bound to ngModel.
 *
 * However, in some cases we want to explicitly set the state from outside. This is done by binding
 * to the <code>checked</code> attribute. When this attribute is bound, the checked state of the
 * RadioButton will *only* change when the value of the binding changes. Clicking on the RadioButton
 * will have no effect other than to emit an event which the parent can use to update the binding.
 *
 * Here is a basic example of a stateless RadioButton where the parent component manages the state:
 *
 * ```html
 * <gtx-radio-button [checked]="isChecked"></gtx-checkbox>
 * ```
 */
@Component({
    selector: 'gtx-radio-button',
    templateUrl: './radio-button.component.html',
    styleUrls: ['./radio-button.component.scss'],
    providers: [generateFormProvider(RadioButtonComponent)],
})
export class RadioButtonComponent
    extends BaseFormElementComponent<any>
    implements OnInit, OnChanges, OnDestroy {

    public readonly UNIQUE_ID = `gtx-radio-button-${randomId()}`;

    /**
     * Sets the radio button to be auto-focused. Handled by `AutofocusDirective`.
     */
    @Input()
    public autofocus = false;

    /**
     * ID of the control
     */
    @Input()
    public id: string;

    /**
     * Name of the input group
     */
    @Input()
    public name: string;

    /** If this radio-button is currently checked or not */
    @Input()
    public checked = false;

    /**
     * Blur event
     */
    @Output()
    public blur = new EventEmitter<void>(true);

    /**
     * Focus event
     */
    @Output()
    public focus = new EventEmitter<void>(true);

    /** If the element is focused via tab/keyboard shortcuts. */
    public tabbedFocus = false;

    /** The value that is being written via ngModel/CVA/... and has to be compared to `value`. */
    protected writtenValue: any = NO_SET;

    /**
     * If the checked state is written via the `checked` input, then this will be turned to true.
     */
    public stateless = false;

    constructor(
        changeDetector: ChangeDetectorRef,
        @Optional()
        private group: RadioGroupDirective,
        @Attribute('ngModel')
        modelAttrib: string,
    ) {
        super(changeDetector);
        this.booleanInputs.push('checked', 'autofocus');

        // Pre-set a common input name for grouped input elements
        if (group) {
            this.name = group.UNIQUE_ID;
        } else if (modelAttrib) {
            this.name = modelAttrib;
        }
    }

    ngOnInit(): void {
        this.checkIfSelected();

        if (this.checked) {
            this.triggerChange(this.value);
        }

        if (this.group) {
            this.group.add(this);

            if (this.checked) {
                this.group.radioSelected(this);
            }
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.checked) {
            this.stateless = true;
            this.pure = true;
        }

        // Prevent changes to `pure` via input
        if (changes.pure) {
            this.pure = changes.pure.previousValue;
        }

        super.ngOnChanges(changes);
    }

    ngOnDestroy(): void {
        super.ngOnDestroy();

        if (this.group) {
            this.group.remove(this);
        }
    }

    protected onValueChange(): void {
        this.checkIfSelected();
    }

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    override writeValue(value: any): void {
        this.stateless = false;
        this.writtenValue = value;
        this.checkIfSelected();
        this.changeDetector.markForCheck();
    }

    protected checkIfSelected(): void {
        if (this.stateless || this.pure) {
            return;
        }

        this.checked = this.writtenValue !== NO_SET && isEqual(this.value, this.writtenValue);
    }

    public onBlur(): void {
        this.blur.emit();
        this.triggerTouch();
        this.tabbedFocus = false;
    }

    public onFocus(): void {
        this.focus.emit();
    }

    @HostListener('keyup', ['$event'])
    public focusHandler(e: KeyboardEvent): void {
        if (e.keyCode === KeyCode.Tab) {
            if (!this.tabbedFocus) {
                this.tabbedFocus = true;
            }
        }
    }

    onInputChecked(e: Event, input: HTMLInputElement): boolean {
        if (e) {
            e.stopPropagation();
        }

        if (this.stateless) {
            // If it has been clicked while stateless, then we need to revert the change
            // and trigger a change instead.
            if (input.checked !== this.checked) {
                input.checked = !!this.checked;
            }

            this.triggerChange(this.value);
            return false;
        }

        this.checked = true;
        this.writtenValue = this.value;
        this.triggerChange(this.value);
        if (this.group) {
            this.group.radioSelected(this);
        }
        return true;
    }
}

import { Directive, Input, OnChanges, Optional, SimpleChanges } from '@angular/core';
import { FormControlName } from '@angular/forms';
import {
    CheckboxComponent,
    DateTimePickerComponent,
    InputComponent,
    RadioButtonComponent,
    RangeComponent,
    SelectComponent,
    TextareaComponent,
} from '@gentics/ui-core';

/**
 * In the current version of Angular (2.x), binding to the `disabled` property of a form control bound to a reactive
 * form control via `formControlName` results in a console warning:
 *
 * "It looks like you're using the disabled attribute with a reactive form directive...."
 *
 * The idea is that the disabled state should be controlled in code, not in the template when using reactive forms.
 *
 * However, this approach has two downsides:
 * 1. The imperative setting of `control.disable()` and `control.enable()` adds a lot of extra boilerplate code to any
 * component with reactive forms.
 * 2. Currently the GUIC form control components will not respond correctly to simply setting their associated FormControl
 * to `disabled` - they only trigger the disabled state if the actual "disabled" input of the component is set to `true`.
 * Therefore the suggested handling given here https://github.com/angular/angular/issues/11271 is not currently suitable.
 *
 * This directive can be used to keep the current "declarative" binding for `disabled`, but with the caveat that we
 * cannot use the attribute "disabled", we need to provide our own attribute name - "gtxDisabled".
 *
 * ```
 * <gtx-input label="First Name" [gtxDisabled]="formIsDisabled"></gtx-input>
 * ```
 */
@Directive({
    selector: '[formControlName][gtxDisabled]',
})
export class DynamicDisableDirective implements OnChanges {

    controlInstance: CheckboxComponent
    | DateTimePickerComponent
    | InputComponent
    | RadioButtonComponent
    | RangeComponent
    | SelectComponent
    | TextareaComponent;

    constructor(
        private formControlName: FormControlName,
        @Optional() checkbox: CheckboxComponent,
        @Optional() dateTimePicker: DateTimePickerComponent,
        @Optional() input: InputComponent,
        @Optional() radio: RadioButtonComponent,
        @Optional() range: RangeComponent,
        @Optional() select: SelectComponent,
        @Optional() textarea: TextareaComponent,
    ) {
        this.controlInstance = checkbox || dateTimePicker || input || radio || range || select || textarea;
    }

    @Input() gtxDisabled;

    // Logic moved into ngOnChanges due to Ivy renderer. this.formControlName.control is undefined when @Input set gtxDisabled() is triggered.
    // However, it is set when ngOnChanges is called.
    ngOnChanges(changes: SimpleChanges): void {
        if (changes.gtxDisabled !== undefined) {
            if (!this.formControlName || !this.formControlName.control) {
                return;
            } else if (changes.gtxDisabled.currentValue) {
                this.formControlName.control.disable({ onlySelf: true, emitEvent: false });
                if (this.controlInstance) {
                    this.controlInstance.disabled = true;
                }
            } else {
                this.formControlName.control.enable({ onlySelf: true, emitEvent: false });
                if (this.controlInstance) {
                    this.controlInstance.disabled = false;
                }
            }
        }
    }
}

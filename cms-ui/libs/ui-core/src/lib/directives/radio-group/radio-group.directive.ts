import { Directive } from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';
import { RadioButtonComponent } from '../../components/radio-button/radio-button.component';
import { generateFormProvider } from '../../utils';

/**
 * RadioGroup groups multiple {@link RadioButtonComponent} elements together.
 * Use ngModel to connect it to a form model.
 */
@Directive({
    selector: 'gtx-radio-group, [gtx-radio-group]',
    providers: [generateFormProvider(RadioGroupDirective)],
})
export class RadioGroupDirective implements ControlValueAccessor {

    private static instanceCounter = 0;

    private radioButtons: RadioButtonComponent[] = [];
    private groupID: number;

    public writtenValue: any;

    get uniqueName(): string {
        return `group-${this.groupID}`;
    }

    constructor() {
        this.groupID = RadioGroupDirective.instanceCounter++;
    }

    add(radio: RadioButtonComponent): void {
        if (this.radioButtons.indexOf(radio) < 0) {
            this.radioButtons.push(radio);
            radio.writeValue(this.writtenValue);
        }
    }

    remove(radio: RadioButtonComponent): void {
        const pos: number = this.radioButtons.indexOf(radio);
        if (pos >= 0) {
            this.radioButtons.splice(pos, 1);
        }
    }

    radioSelected(selected?: RadioButtonComponent): void {
        for (const radio of this.radioButtons) {
            if (radio !== selected) {
                radio.writeValue(selected ? selected.value : null);
            }
        }
        // setTimeout because this method is invoked from a child component (RadioButton), which is the wrong direction
        // for change propagation (which should normally always be parent -> child). If we synchronously now update the
        // ngModel value, we will cause "changed after checked" errors in dev mode.
        setTimeout(() => {
            this.onChange(selected ? selected.value : null);
            this.onTouched();
        });
    }

    writeValue(value: any): void {
        this.writtenValue = value;

        for (const radio of this.radioButtons) {
            radio.writeValue(value);
        }
    }

    registerOnChange(fn: () => any): void {
        this.onChange = fn;
    }

    registerOnTouched(fn: (value?: any) => any): void {
        this.onTouched = fn;
    }

    private onTouched: () => any = () => { };
    private onChange: (value?: any) => any = () => { };
}

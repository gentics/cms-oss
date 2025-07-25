import { Directive, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';
import { ChangesOf } from '../../common';
import { RadioButtonComponent } from '../../components/radio-button/radio-button.component';
import { generateFormProvider, randomId } from '../../utils';

/**
 * RadioGroup groups multiple {@link RadioButtonComponent} elements together.
 * Use ngModel to connect it to a form model.
 */
@Directive({
    selector: 'gtx-radio-group, [gtx-radio-group]',
    providers: [generateFormProvider(RadioGroupDirective)],
})
export class RadioGroupDirective implements ControlValueAccessor, OnChanges {

    public disabled = false;

    private radioButtons: RadioButtonComponent[] = [];

    @Input()
    public value: any;

    @Output()
    public valueChange = new EventEmitter<any>();

    public readonly UNIQUE_ID = `group-${randomId()}`;

    public ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.value) {
            this.forwardValueToRadios();
        }
    }

    add(radio: RadioButtonComponent): void {
        if (this.radioButtons.indexOf(radio) < 0) {
            this.radioButtons.push(radio);
            radio.writeValue(this.value);
            radio.setDisabledState(this.disabled);
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
            this.valueChange.emit(selected ? selected.value : null);
            this.onTouched();
        });
    }

    private forwardValueToRadios() {
        for (const radio of this.radioButtons) {
            radio.writeValue(this.value);
        }
    }

    writeValue(value: any): void {
        this.value = value;
        this.forwardValueToRadios();
    }

    registerOnChange(fn: () => any): void {
        this.onChange = fn;
    }

    registerOnTouched(fn: (value?: any) => any): void {
        this.onTouched = fn;
    }

    private onTouched: () => any = () => { };
    private onChange: (value?: any) => any = () => { };

    setDisabledState(isDisabled: boolean): void {
        this.disabled = isDisabled;

        for (const radio of this.radioButtons) {
            radio.setDisabledState(isDisabled);
        }
    }
}

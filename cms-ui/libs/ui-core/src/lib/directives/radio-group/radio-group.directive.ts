import { Directive, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
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
export class RadioGroupDirective implements ControlValueAccessor, OnChanges {

    private static instanceCounter = 0;

    private radioButtons: RadioButtonComponent[] = [];
    private groupID: number;

    @Input()
    public value: any;

    @Output()
    public valueChange = new EventEmitter<any>();

    get uniqueName(): string {
        return `group-${this.groupID}`;
    }

    constructor() {
        this.groupID = RadioGroupDirective.instanceCounter++;
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.value) {
            this.forwardValueToRadios();
        }
    }

    add(radio: RadioButtonComponent): void {
        if (this.radioButtons.indexOf(radio) < 0) {
            this.radioButtons.push(radio);
            radio.writeValue(this.value);
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
}

import { Directive, ElementRef, Input } from '@angular/core';
import { coerceToBoolean } from '../../utils';

@Directive({
    selector: 'gtx-option',
})
export class SelectOptionDirective {

    @Input()
    icon: string;

    @Input()
    set value(value: any) {
        this._value = value;
    }
    get value(): any {
        return this._value;
    }

    @Input()
    set disabled(value: boolean) {
        this._disabled = coerceToBoolean(value);
    }
    get disabled(): boolean {
        return this._disabled;
    }

    private _value: any;
    private _disabled: any;

    constructor(public elementRef: ElementRef) {}

    /**
     * Returns the value of the option as displayed in the view, i.e. a string representation.
     */
    get viewValue(): string {
        const textContent = this.elementRef.nativeElement.textContent.trim();
        if (textContent) {
            return textContent;
        }
        if (!this.isPrimitive(this.value)) {
            return '[Object]';
        }
        return this.value.toString();
    }

    private isPrimitive(value: any): boolean {
        return value !== null && (typeof value !== 'function' && typeof value !== 'object');
    }
}

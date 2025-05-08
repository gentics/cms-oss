import { Directive, ElementRef, Input, OnChanges, SimpleChanges } from '@angular/core';
import { coerceToBoolean } from '../../utils';

@Directive({
    selector: 'gtx-option',
    standalone: false
})
export class SelectOptionDirective implements OnChanges {

    @Input()
    public icon: string;

    @Input()
    public value: any;

    @Input()
    public disabled: boolean;

    constructor(
        public elementRef: ElementRef<HTMLElement>,
    ) {}

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.disabled) {
            this.disabled = coerceToBoolean(this.disabled);
        }
    }

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
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        return this.value.toString();
    }

    private isPrimitive(value: any): boolean {
        return value != null && (typeof value !== 'function' && typeof value !== 'object');
    }
}

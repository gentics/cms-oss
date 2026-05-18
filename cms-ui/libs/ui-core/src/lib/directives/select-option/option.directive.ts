import { booleanAttribute, Directive, ElementRef, Input } from '@angular/core';

@Directive({
    selector: 'gtx-option',
    standalone: false,
})
export class SelectOptionDirective {

    @Input()
    public icon: string;

    @Input()
    public value: any;

    @Input({ transform: booleanAttribute })
    public disabled: boolean;

    constructor(
        public elementRef: ElementRef<HTMLElement>,
    ) {}

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

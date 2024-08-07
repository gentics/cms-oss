import { AfterViewInit, Directive, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { coerceToBoolean } from '../../utils';

/**
 * Handles autofocus for all ui-core form elements.
 */
@Directive({
    selector: `
        gtx-button[autofocus],
        gtx-checkbox[autofocus],
        gtx-date-time-picker[autofocus],
        gtx-file-picker[autofocus],
        gtx-input[autofocus],
        gtx-radio-button[autofocus],
        gtx-search-bar[autofocus],
        gtx-select[autofocus],
        gtx-textarea[autofocus]`,
})
export class AutofocusDirective implements AfterViewInit, OnChanges, OnDestroy {

    @Input()
    public autofocus = false;

    private inputElement: HTMLButtonElement | HTMLInputElement | HTMLDivElement | HTMLTextAreaElement;
    private timeout: any;

    constructor(private element: ElementRef<HTMLElement>) { }

    ngAfterViewInit(): void {
        if (this.element && this.element.nativeElement) {
            this.inputElement = this.element.nativeElement.querySelector('input, .select-input, textarea, button');

            if (this.autofocus) {
                if (!(this.inputElement instanceof HTMLDivElement)) {
                    this.inputElement.autofocus = true;
                }
                this.focusNativeInput();
            }
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.autofocus) {
            this.autofocus = coerceToBoolean(this.autofocus);
            if (this.inputElement != null && !(this.inputElement instanceof HTMLDivElement)) {
                this.inputElement.autofocus = this.autofocus;
            }
        }
    }

    ngOnDestroy(): void {
        this.cleanupTimer();
        this.inputElement = undefined;
    }

    // HTML autofocus does not work with ngIf or modals.
    // Therefore, the input element is focused programatically.
    private focusNativeInput(): void {
        this.cleanupTimer();
        this.timeout = setTimeout(() => {
            // There's already an element which has focus - do not steal the focus!
            if (document.activeElement != null) {
                return;
            }

            this.inputElement.focus();

            if (typeof (this.inputElement as any).scrollIntoViewIfNeeded === 'function') {
                // Chrome only (1/2017)
                // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                (this.inputElement as any).scrollIntoViewIfNeeded();
            } else {
                // Browser support varies
                try {
                    this.inputElement.scrollIntoView({
                        behavior: 'smooth',
                        block: 'nearest',
                        inline: 'nearest',
                    });
                } catch (err) {
                    try {
                        this.inputElement.scrollIntoView({
                            behavior: 'smooth',
                            block: 'start',
                            inline: 'start',
                        });
                    } catch (err) {
                        this.inputElement.scrollIntoView({
                            block: 'start',
                        });
                    }
                }
            }
        }, 50);
    }

    private cleanupTimer(): void {
        if (this.timeout) {
            clearTimeout(this.timeout);
            this.timeout = undefined;
        }
    }
}

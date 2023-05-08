import { Directive, ElementRef, EventEmitter, HostListener, Output } from '@angular/core';

/**
 * This is a directive for detecting clicks outside of a DOM element where the directive is placed on.
 *
 * ## Sample Usage
 * Using this directive is as easy as:
 *
 * ```[html]
 * <div (gtxClickOutside)="close()"></div>
 * ```
 *
 * You can pass the original `MouseEvent` to the registered handler as follows:
 *
 * ```[html]
 * <div (gtxClickOutside)="close($event)"></div>
 * ```
 * Credits to https://github.com/christianliebel/angular2-click-outside
 *
 */
@Directive({
    selector: '[gtxClickOutside]',
})
export class ClickOutsideDirective {
    constructor(private _elementRef: ElementRef) {
    }

    @Output()
    public gtxClickOutside = new EventEmitter<MouseEvent>();

    @HostListener('document:click', ['$event', '$event.target'])
    public onClick(event: MouseEvent, targetElement: HTMLElement): void {
        if (!targetElement) {
            return;
        }

        const clickedInside = this._elementRef.nativeElement.contains(targetElement);
        if (!clickedInside) {
            this.gtxClickOutside.emit(event);
        }
    }
}

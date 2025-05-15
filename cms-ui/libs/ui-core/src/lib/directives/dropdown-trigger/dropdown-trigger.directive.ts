import { Directive, ElementRef } from '@angular/core';
import { DROPDOWN_FOCUSABLE_ITEMS_SELECTOR } from '../../common';

@Directive({
    selector: 'gtx-dropdown-trigger',
    standalone: false
})
export class DropdownTriggerDirective {
    constructor(public elementRef: ElementRef) { }

    /**
     * Focus the first focusable descendant of this element.
     */
    focus(): void {
        const focusable = this.elementRef.nativeElement.querySelector(DROPDOWN_FOCUSABLE_ITEMS_SELECTOR);
        if (focusable && focusable.focus) {
            focusable.focus();
        }
    }
}

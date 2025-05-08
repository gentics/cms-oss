import { AfterContentInit, Component, ContentChildren, ElementRef, EventEmitter, forwardRef, HostListener, QueryList } from '@angular/core';
import { DROPDOWN_FOCUSABLE_ITEMS_SELECTOR, KeyCode } from '../../common';
import { DropdownItemComponent } from '../dropdown-item/dropdown-item.component';

/**
 * Wraps the content and handles keyboard control (tabbing and focus) of the contents.
 */
@Component({
    selector: 'gtx-dropdown-content',
    templateUrl: './dropdown-content.component.html',
    styleUrls: ['./dropdown-content.component.scss'],
    standalone: false
})
export class DropdownContentComponent implements AfterContentInit {

    @ContentChildren(forwardRef(() => DropdownItemComponent), { read: ElementRef })
    items: QueryList<ElementRef>;

    focusLost = new EventEmitter<boolean>();
    focusableItems: HTMLElement[] = [];

    constructor(public elementRef: ElementRef<HTMLElement>) {}

    @HostListener('keydown', ['$event'])
    keyHandler(e: KeyboardEvent): void {
        if (e.keyCode === KeyCode.Tab) {
            if (e.shiftKey) {
                this.focusPrevious(e.target as HTMLElement, e);
            } else {
                this.focusNext(e.target as HTMLElement, e);
            }
        }
    }

    ngAfterContentInit(): void {
        this.focusableItems = Array.from<HTMLElement>(this.elementRef.nativeElement.querySelectorAll(DROPDOWN_FOCUSABLE_ITEMS_SELECTOR));
    }

    focusFirstItem(): void {
        const firstItem = this.focusableItems[0];
        if (firstItem && firstItem.focus) {
            firstItem.focus();
        }
    }

    focusNext(currentElement: HTMLElement, e: KeyboardEvent): void {
        const items = this.focusableItems;
        const index = this.getIndexOfElement(currentElement);
        if (index === items.length - 1) {
            e.preventDefault();
            this.focusLost.emit(true);
        }
    }

    focusPrevious(currentElement: HTMLElement, e: KeyboardEvent): void {
        const index = this.getIndexOfElement(currentElement);
        if (index === 0) {
            e.preventDefault();
            this.focusLost.emit(true);
        }
    }

    private getIndexOfElement(element: HTMLElement): number {
        return this.focusableItems.indexOf(element);
    }
}

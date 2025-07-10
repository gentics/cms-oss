import { Component, ElementRef, Input, OnDestroy, Optional, ViewChild, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { DropdownListComponent } from '@gentics/ui-core';
import { Observable, Subscription } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

/**
 * A ListFilter wraps a list of items and provides a filter input to filter that list.
 * Any items matching the selector will be filtered when text is input.
 *
 * This is especially designed to live within a DropdownList, and hooks into the DropdownList events
 * to provide seamless focus, clear and resize functionality. It can also work without a DropdownList.
 */
@Component({
    selector: 'list-filter',
    templateUrl: './list-filter.tpl.html',
    styleUrls: ['./list-filter.scss'],
    standalone: false
})
export class ListFilterComponent implements OnInit, OnChanges, OnDestroy {

    /** The css selector matching the items in the list to be filtered */
    @Input()
    public selector: string;

    /** A label for the filter input */
    @Input()
    public label = '';
    /**
     * If the number of list items is lower than the threshhold, the filter
     * input will not be displayed.
     */
    @Input()
    public filterThreshhold = 10;

    @ViewChild('filterInput', { read: ElementRef })
    public filterInput: ElementRef;

    public filterString = '';
    public items: HTMLElement[];
    public displayFilter = false;

    private subscription: Subscription;

    constructor(
        private elementRef: ElementRef,
        @Optional() private dropdownList?: DropdownListComponent,
    ) {}

    ngOnInit(): void {
        this.subscription = this.getMutationObservable().pipe(
            debounceTime(100),
        ).subscribe(() => this.populateItems());

        if (this.dropdownList) {
            this.subscription.add(this.dropdownList.open.subscribe(() => this.focus()));
            this.subscription.add(this.dropdownList.close.subscribe(() => this.clear()));
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.filterThreshhold) {
            if (!Number.isInteger(this.filterThreshhold)) {
                // Is NaN, Infinite or another invalid number
                if (typeof this.filterThreshhold === 'number') {
                    this.filterThreshhold = 10;
                } else {
                    this.filterThreshhold = parseInt(this.filterThreshhold as any, 10);
                }
            }
        }
    }

    ngOnDestroy(): void {
        if (this.subscription) {
            this.subscription.unsubscribe();
        }
    }

    populateItems(): void {
        this.items = Array.from(this.elementRef.nativeElement.querySelectorAll(this.selector));
        this.displayFilter = this.filterThreshhold < this.items.length;
    }

    /** Stop clicks from bubbling to prevent DropdownList ancestors from closing */
    onClick(e: MouseEvent): void {
        e.stopPropagation();
    }

    /** Focus the filter input */
    focus(): void {
        if (this.filterInput) {
            setTimeout(() => {
                this.filterInput.nativeElement.querySelector('input').focus();
            }, 50);
        }
    }

    /** Clear the filter input and restore all items */
    clear(): void {
        this.filterString = '';
        this.restoreAllItems();
    }

    onInput(value: string): void {
        if (value !== '') {
            this.items.forEach(item => {
                const match = -1 < item.innerText.toLocaleLowerCase().indexOf(value.toLocaleLowerCase());
                if (match) {
                    item.classList.remove('filter-hidden');
                } else {
                    item.classList.add('filter-hidden');
                }
            });
        } else {
            this.restoreAllItems();
        }
        if (this.dropdownList) {
            this.dropdownList.resize();
        }
    }

    private restoreAllItems(): void {
        if (this.items) {
            this.items.forEach(item => item.classList.remove('filter-hidden'));
        }
    }

    /**
     * Returns an Observable based on a MutationObserver, which emits the matching element each time
     * a node matching the filter's selector is added.
     */
    private getMutationObservable(): Observable<Element> {
        // Polyfill the Element.matches() method for IE11.
        // see https://developer.mozilla.org/en-US/docs/Web/API/Element/matches
        if (!Element.prototype.matches) {
            Element.prototype.matches =
                (<any> Element.prototype).msMatchesSelector ||
                Element.prototype.webkitMatchesSelector;
        }


        return new Observable(subscriber => {
            const observer = new MutationObserver((mutations: MutationRecord[]) => {
                mutations.forEach(mutation => {
                    const addedNode = mutation.addedNodes && mutation.addedNodes[0];
                    if (addedNode instanceof Element && addedNode.matches(this.selector)) {
                        subscriber.next(addedNode);
                    }
                });
            });

            const observerConfig = { attributes: false, childList: true, characterData: false, subtree: true };
            observer.observe(this.elementRef.nativeElement, observerConfig);

            return () => observer.disconnect();
        });
    }
}

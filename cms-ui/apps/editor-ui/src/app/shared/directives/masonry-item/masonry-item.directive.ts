import {AfterViewChecked, AfterViewInit, Directive, ElementRef, EventEmitter, Output} from '@angular/core';

/** See MasonryGrid for usage. */
@Directive({
    // tslint:disable directive-selector
    selector: 'masonry-item, [masonryItem]'
})
export class MasonryItemDirective implements AfterViewChecked, AfterViewInit {

    private previousHeight: number;

    @Output() heightChange = new EventEmitter<true>();

    constructor(private elementRef: ElementRef) {}

    ngAfterViewInit(): void {
        // Store initial size of masonry-item tag or tag with masonryItem attribute
        if (this.elementRef.nativeElement !== null) { // null in case native elements are not supported (e.g. in web workers)
            this.previousHeight = this.elementRef.nativeElement.offsetHeight;
        }
    }

    ngAfterViewChecked(): void {
        // Check size of masonry-item tag or tag with masonryItem attribute after change detection happened on any kind of content (projected or view)
        if (this.elementRef.nativeElement !== null) { // null in case native elements are not supported (e.g. in web workers)
            const height = this.elementRef.nativeElement.offsetHeight;
            if (this.previousHeight !== height) {
                this.heightChange.emit(true);
            }
            this.previousHeight = height;
        }
    }
}

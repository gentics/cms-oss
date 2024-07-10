import { AfterViewInit, Directive, ElementRef, EventEmitter, OnDestroy, Output } from '@angular/core';

/** See MasonryGrid for usage. */
@Directive({
    selector: 'masonry-item, [masonryItem]',
})
export class MasonryItemDirective implements AfterViewInit, OnDestroy {

    @Output()
    public sizeChange = new EventEmitter<true>();

    private obs: ResizeObserver;

    constructor(
        private elementRef: ElementRef<HTMLElement>,
    ) { }

    ngAfterViewInit(): void {
        this.obs = new ResizeObserver(() => {
            this.sizeChange.emit();
        });
        this.obs.observe(this.elementRef.nativeElement);
    }

    ngOnDestroy(): void {
        if (this.obs) {
            this.obs.disconnect();
            this.obs = null;
        }
    }
}

import { Component, EventEmitter, HostListener } from '@angular/core';

/**
 * The scroll mask is a transparent div covering the entire viewport which is intended to prevent scrolling.
 */
@Component({
    selector: 'gtx-scroll-mask',
    template: '',
    styleUrls: ['./scroll-mask.component.scss'],
})
export class ScrollMaskComponent {
    clicked = new EventEmitter<any>();

    @HostListener('click')
    clickHandler(): void {
        this.clicked.emit(true);
    }
}

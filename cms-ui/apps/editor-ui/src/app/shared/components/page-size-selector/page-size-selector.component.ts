import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * A component for selecting the page size.
 */
@Component({
    selector: 'gtx-page-size-selector',
    templateUrl: './page-size-selector.tpl.html',
    styleUrls: ['./page-size-selector.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class PageSizeSelectorComponent {
    /** Page size */
    @Input() size: number;

    /** Page size options */
    @Input() options: number[] = [ 10, 25, 100 ];

    /** Fired when page size has been selected. */
    @Output() sizeChange = new EventEmitter<number>();

    constructor() { }
}

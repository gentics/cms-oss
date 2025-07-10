import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * A custom template wrapper around the ng2-pagination PaginationControlsCmp.
 */
@Component({
    selector: 'gtxct-paging-controls',
    templateUrl: './paging-controls.tpl.html',
    styleUrls: ['./paging-controls.scss'],
    standalone: false
})
export class PagingControlsComponent {
    @Input() id: string;
    @Input() directionLinks = true;
    @Output() pageChange = new EventEmitter<number>();
}

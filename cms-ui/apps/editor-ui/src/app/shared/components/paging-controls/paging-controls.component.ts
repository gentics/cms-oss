import {Component, EventEmitter, Input, Output} from '@angular/core';

/**
 * A custom template wrapper around the ng2-pagination PaginationControlsCmp.
 */
@Component({
    selector: 'paging-controls',
    templateUrl: './paging-controls.tpl.html',
    styleUrls: ['./paging-controls.scss'],
    standalone: false
})
export class PagingControls {
    @Input() id: string;
    @Input() directionLinks: boolean = true;
    @Output() pageChange = new EventEmitter<number>();
}

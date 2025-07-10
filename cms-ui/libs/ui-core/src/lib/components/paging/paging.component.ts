import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Simple pagingation component, to allow users to control the page they are currently on.
 * Wraps the `paginate` pipe of the [ngx-pagination](https://github.com/michaelbromley/ngx-pagination) and therefore
 * also requires the same `id` mechanism to synchronize the pagination data with the controls.
 *
 * @example
 * ```ts
 * class MyComponent {
 *      page = 1;
 *      id = 'something123';
 *      items = [ /* ... /* ];
 * }
 * ```
 *
 * ```html
 * <div *ngFor="let singleItem of items | paginate:{
 *      id: id,
 *      currentPage: page,
 *      itemsPerPage: 10,
 *      totalItems: items.length
 * }">
 *      {{ singleItem | json }}
 * </div>
 *
 * <gtx-paging [id]="id" (pageChange)="page = $event">/<gtx-paging>
 * ```
 */
@Component({
    selector: 'gtx-paging',
    templateUrl: './paging.component.html',
    styleUrls: ['./paging.component.scss'],
    standalone: false
})
export class PaginationComponent {

    /**
     * The same ID which was used in the `paginate` pipe, to synchronize the data.
     */
    @Input()
    id: string;

    /**
     * Event to change the page to the emitted value.
     */
    @Output()
    pageChange = new EventEmitter<number>();

}

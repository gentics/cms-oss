import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';


@Component({
    selector: 'gtx-paging',
    templateUrl: './paging.component.html',
    styleUrls: ['./paging.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginationComponent {

    @Input()
    id: string;

    @Output()
    pageChange = new EventEmitter<number>();

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
    }

}

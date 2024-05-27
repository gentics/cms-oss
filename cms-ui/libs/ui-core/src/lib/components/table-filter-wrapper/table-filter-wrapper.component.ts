import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
    selector: 'gtx-table-filter-wrapper',
    templateUrl: './table-filter-wrapper.component.html',
    styleUrls: ['./table-filter-wrapper.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TableFilterWrapperComponent {

    @Input()
    public icon = 'search';

    @Input()
    public disabled = false;

    @Input()
    public hasValue = false;

    @Output()
    public clear = new EventEmitter<void>();

    public triggerClear(): void {
        this.clear.emit();
    }
}

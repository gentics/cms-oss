import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
    selector: 'gtxct-filter-editable',
    templateUrl: './filter-editable.component.html',
    styleUrls: ['./filter-editable.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class FilterEditableComponent {
    @Input() editable: boolean;

    /** Fired when changed */
    @Output() changed = new EventEmitter<string>();
}

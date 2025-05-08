import { Component, HostBinding, Input, OnChanges, SimpleChanges } from '@angular/core';
import { coerceToBoolean } from '../../utils';

@Component({
    selector: 'gtx-dropdown-item',
    template: '<ng-content></ng-content>',
    styleUrls: ['./dropdown-item.component.scss'],
    standalone: false
})
export class DropdownItemComponent implements OnChanges {

    /**
     * If true, the DropdownItem cannot be clicked or selected. *Default: false*
     */
    @Input()
    @HostBinding('class.disabled')
    public disabled = false;

    @HostBinding('tabindex')
    public tabIndex = 0;

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.disabled) {
            this.disabled = coerceToBoolean(this.disabled);
        }
    }
}

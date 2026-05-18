import { booleanAttribute, Component, HostBinding, Input } from '@angular/core';

@Component({
    selector: 'gtx-dropdown-item',
    template: '<ng-content></ng-content>',
    styleUrls: ['./dropdown-item.component.scss'],
    standalone: false,
})
export class DropdownItemComponent {

    /**
     * If true, the DropdownItem cannot be clicked or selected. *Default: false*
     */
    @Input({ transform: booleanAttribute })
    @HostBinding('class.disabled')
    public disabled = false;

    @HostBinding('tabindex')
    public tabIndex = 0;
}

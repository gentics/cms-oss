import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
    selector: 'gtx-user-menu-toggle',
    templateUrl: './user-menu-toggle.tpl.html',
    styleUrls: ['./user-menu-toggle.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class GtxUserMenuToggleComponent {
    @Input() active = false;
}

import { Component, Input, OnDestroy } from '@angular/core';
import { ObservableStopper } from '../../../common/utils/observable-stopper/observable-stopper';

@Component({
    selector: 'gtx-sidebar-item',
    templateUrl: './sidebar-item.component.html',
    styleUrls: ['./sidebar-item.component.scss'],
    standalone: false,
})
export class SidebarItemComponent implements OnDestroy {

    @Input() title: string;
    @Input() open = true;

    protected stopper = new ObservableStopper();

    toggleOpen(): void {
        this.open = !this.open;
    }

    ngOnDestroy(): void {
        this.stopper.stop();
    }
}

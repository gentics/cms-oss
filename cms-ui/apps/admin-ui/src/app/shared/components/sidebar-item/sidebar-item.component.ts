import { Component, Input, OnDestroy } from '@angular/core';
import { Observable } from 'rxjs';
import { ObservableStopper } from '../../../common/utils/observable-stopper/observable-stopper';
import { I18nService } from '../../../core/providers/i18n/i18n.service';

@Component({
    selector: 'gtx-sidebar-item',
    templateUrl: './sidebar-item.component.html',
    styleUrls: ['./sidebar-item.component.scss'],
})
export class SidebarItemComponent implements OnDestroy {

    @Input() title: Observable<string> | string;
    @Input() open = true;

    protected stopper = new ObservableStopper();

    constructor(protected i18n: I18nService) {

    }

    toggleOpen(): void {
        this.open = !this.open;
    }

    ngOnDestroy(): void {
        this.stopper.stop();
    }
}

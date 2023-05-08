import {ChangeDetectionStrategy, Component, Input} from '@angular/core';

@Component({
    selector: 'gtx-logging-in-overlay',
    templateUrl: './logging-in-overlay.component.html',
    styleUrls: ['./logging-in-overlay.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoggingInOverlayComponent {
    @Input() set loggingIn(val: boolean) {
        this._loggingIn = val;
        this.hideSpinner = false;
    }

    _loggingIn = false;
    hideSpinner = false;
}

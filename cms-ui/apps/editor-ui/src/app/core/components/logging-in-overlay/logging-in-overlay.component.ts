import {ChangeDetectionStrategy, Component, Input} from '@angular/core';

@Component({
    selector: 'logging-in-overlay',
    templateUrl: './logging-in-overlay.tpl.html',
    styleUrls: ['./logging-in-overlay.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoggingInOverlay {
    @Input() set loggingIn(val: boolean) {
        this._loggingIn = val;
        this.hideSpinner = false;
    }

    _loggingIn = false;
    hideSpinner = false;
}

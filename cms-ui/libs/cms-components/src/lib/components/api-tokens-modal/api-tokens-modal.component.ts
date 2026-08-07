import { Component } from '@angular/core';
import { IModalDialog } from '@gentics/ui-core';

@Component({
    selector: 'gtx-api-tokens-modal',
    templateUrl: './api-tokens-modal.component.html',
    standalone: false,
})
export class ApiTokensModalComponent implements IModalDialog {
    closeFn = (): void => {};
    cancelFn = (): void => {};

    registerCloseFn(close: (val?: any) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }
}

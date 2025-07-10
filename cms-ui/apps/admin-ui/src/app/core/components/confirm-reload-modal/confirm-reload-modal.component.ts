import { Component } from '@angular/core';
import { IModalDialog } from '@gentics/ui-core';

@Component({
    selector: 'gtx-confirm-reload-modal',
    templateUrl: './confirm-reload-modal.component.html',
    standalone: false
})
export class ConfirmReloadModalComponent implements IModalDialog {
    closeFn: (result: boolean) => void;
    cancelFn: (val?: any) => void;

    reload(): void {
        this.closeFn(true);
    }

    cancelAndClose(): void {
        this.closeFn(false);
    }

    registerCloseFn(close: (val: boolean) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }
}

import { Component } from '@angular/core';
import { IModalDialog } from '@gentics/ui-core';
import { EditorOverlayModal } from '../editor-overlay-modal/editor-overlay-modal.component';

/**
 * A modal for the user to prevent close EditorOverlayModal if something was modified.
 */
@Component({
    selector: 'confirm-close-modal',
    templateUrl: './confirm-close-modal.tpl.html',
    styleUrls: ['./confirm-close-modal.scss'],
})
export class ConfirmCloseModal implements IModalDialog {
    closeFn: (result: ConfirmCloseResult) => void;
    cancelFn: (val?: any) => void;

    currentModal: EditorOverlayModal;

    saveAndClose(): void {
        this.closeFn('save');
    }

    registerCloseFn(close: (val: ConfirmCloseResult) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }
}

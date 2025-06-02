import { Component } from '@angular/core';
import { IModalDialog } from '@gentics/ui-core';

@Component({
    selector: 'gtx-discard-changes-modal',
    templateUrl: './discard-changes-modal.component.html',
    styleUrls: ['./discard-changes-modal.component.scss'],
    standalone: false
})
export class DiscardChangesModalComponent implements IModalDialog {

    /** Determines if the save button is available. */
    changesValid: boolean;

    /** Update entity data of invoking component */
    updateEntity: () => Promise<any>;

    /** Reset entity data of invoking component */
    resetEntity: () => Promise<void>;

    cancelFn = () => {};

    closeFn = (canCloseDetails: boolean) => {};

    registerCloseFn(close: (val?: any) => void): void {
        this.closeFn = (discardChanges: boolean) => {
            close(discardChanges);
        };
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    onSaveClick(): void {
        this.updateEntity()
            .then(() => this.closeFn(true));
    }

    onDiscardChangesClick(): void {
        this.resetEntity()
            .then(() => this.closeFn(true));
    }

    onCancelClick(): void {
        this.closeFn(false);
    }

}

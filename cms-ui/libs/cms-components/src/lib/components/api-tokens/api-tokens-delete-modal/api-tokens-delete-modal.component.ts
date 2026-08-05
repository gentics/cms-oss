import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ApiTokenHandlerService } from '@gentics/cms-components';
import { IModalDialog } from '@gentics/ui-core';
import { forkJoin } from 'rxjs';


@Component({
    selector: 'gtx-api-tokens-delete-modal',
    templateUrl: './api-tokens-delete-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class ApiTokensDeleteModalComponent implements IModalDialog {
    public apiTokenIds: Array<string>;

    constructor(
        protected handler: ApiTokenHandlerService
    ) {}

    public deleteApiTokens(): void {
        forkJoin(
            this.apiTokenIds.map(id => this.handler.delete(id))
        ).subscribe({
            next: () => {
                this.closeFn();
            },
            error: err => {
                console.error(err);
            }
        });
    }

    closeFn = () => {};
    cancelFn = () => {};

    registerCloseFn(close: (val?: any) => void): void {
        this.closeFn = close;
    }
    
    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    public closeModal() {
        this.closeFn();
    }
}

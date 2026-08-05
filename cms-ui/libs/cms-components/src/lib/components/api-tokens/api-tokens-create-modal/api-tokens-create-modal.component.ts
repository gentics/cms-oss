import { ChangeDetectionStrategy, Component, signal, ViewChild } from '@angular/core';
import { ApiTokenHandlerService, ApiTokensCreateFormComponent } from '@gentics/cms-components';
import { IModalDialog } from '@gentics/ui-core';


@Component({
    selector: 'gtx-api-tokens-create-modal',
    templateUrl: './api-tokens-create-modal.component.html',
    styleUrls: ['./api-tokens-create-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class ApiTokensCreateModalComponent implements IModalDialog {
    @ViewChild(ApiTokensCreateFormComponent)
    readonly createForm!: ApiTokensCreateFormComponent;

    public readonly createdToken = signal<string | null>(null);
    public formvalid: boolean = false;
    public isLoading: boolean = false;
    public apiTokenNames: Array<string>;

    constructor(
        protected handler: ApiTokenHandlerService
    ) {}

    public saveForm(): void {
        this.createForm.addApiToken();
    }

    public submitCallback(token: string | null) {
        if(token !== null) {
            this.createdToken.set(token);
        }

        this.isLoading = false
    }

    public isFormVaid(valid: boolean) {
        this.formvalid = valid;
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
        this.createdToken.set(null);
        this.closeFn();
    }
}

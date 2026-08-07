import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { FormControl } from '@angular/forms';
import { EditableApiToken } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { IModalDialog } from '@gentics/ui-core';
import { I18nNotificationService } from '../../providers';

@Component({
    selector: 'gtx-api-tokens-create-modal',
    templateUrl: './api-tokens-create-modal.component.html',
    styleUrls: ['./api-tokens-create-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class ApiTokensCreateModalComponent implements IModalDialog {
    public readonly createdToken = signal<string | null>(null);
    public readonly loading = signal<boolean>(false);
    public formvalid = false;
    public apiTokenNames: Array<string>;
    public control: FormControl<EditableApiToken> = new FormControl({
        name: '',
        expires: null,
    });

    constructor(
        protected api: GCMSRestClientService,
        protected notification: I18nNotificationService,
    ) {}

    public saveForm(): void {
        this.loading.set(true);
        this.addApiToken();
    }

    public isFormVaid(valid: boolean): void {
        this.formvalid = valid;
    }

    closeFn = (): void => {};
    cancelFn = (): void => {};

    registerCloseFn(close: (val?: any) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    public closeModal(): void {
        this.closeFn();
    }

    private addApiToken(): void {
        const { name, expires } = this.control.getRawValue();

        const submitData = {
            name,
            ...(expires !== null ? { expires: this.dateToDateNumberString(expires) } : {}),
        };

        this.loading.set(true);

        this.api.admin.addApiTokens(submitData)
            .subscribe({
                next: (newToken) => {
                    const name = newToken.data.name;
                    this.createdToken.set(newToken.token);
                    console.log(newToken);

                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_created',
                        id: 'api-token-create-success',
                        translationParams: {
                            name,
                        },
                    });
                },
                complete: () => this.loading.set(false),
            });
    }

    private dateToDateNumberString(dateString: string): string {
        return Math.floor(new Date(dateString).getTime() / 1000).toString();
    }
}

import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ApiTokenCreateResponse, EditableApiToken } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { BaseModal } from '@gentics/ui-core';
import { I18nNotificationService } from '../../providers';

@Component({
    selector: 'gtx-api-tokens-create-modal',
    templateUrl: './api-tokens-create-modal.component.html',
    styleUrls: ['./api-tokens-create-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class ApiTokensCreateModalComponent extends BaseModal<ApiTokenCreateResponse> {

    public readonly loading = signal<boolean>(false);
    public apiTokenNames: Array<string>;

    public control: FormControl<EditableApiToken> = new FormControl({
        name: '',
        expires: null,
    });

    constructor(
        protected api: GCMSRestClientService,
        protected notification: I18nNotificationService,
    ) {
        super();
    }

    public saveForm(): void {
        this.loading.set(true);
        this.addApiToken();
    }

    private addApiToken(): void {
        const { name, expires } = this.control.value;

        const submitData = {
            name,
            ...(expires != null ? { expires: expires } : {}),
        };

        this.loading.set(true);

        this.api.admin.addApiTokens(submitData)
            .subscribe({
                next: (newToken) => {
                    const name = newToken.data.name;

                    if (!newToken) {
                        return;
                    }

                    this.closeFn(newToken);

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
}

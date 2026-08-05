import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { ApiTokenCreateRequest, ApiTokenResponse, ListResponse } from '@gentics/cms-models';


@Injectable()
export class ApiTokenHandlerService {
    constructor(
        protected api: GCMSRestClientService,
        protected notification: I18nNotificationService,
    ) { }

    displayName(entity: ApiTokenResponse): string {
        return entity.name;
    }

    create(data: ApiTokenCreateRequest): Observable<ApiTokenResponse> {
        return this.api.admin.addApiTokens(data).pipe(
            tap((res) => {
                const name = res.name;

                this.notification.show({
                    type: 'success',
                    message: 'shared.item_created',
                    translationParams: {
                        name,
                    },
                });
            })
        );
    }

    delete(id: string): Observable<void> {
        return this.api.admin.deleteApiTokens(id).pipe(
            tap(() => {
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_singular_deleted',
                    translationParams: {
                        name,
                    },
                });
            })
        );
    }

    get(): Observable<ListResponse<ApiTokenResponse>> {
        return this.api.admin.getApiTokens().pipe(
            tap((res) => res)
        );
    }
}
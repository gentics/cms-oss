import { Injectable } from '@angular/core';
import {
    GenericItemResponse,
    PageResponse,
    TranslationRequestOptions as TranslationRequestParams,
    TranslationResponse,
    TranslationTextRequest,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { Observable } from 'rxjs';

@Injectable()
export class TranslationActionsService {
    constructor(private client: GCMSRestClientService) {}

    public translatePage(
        pageId: number,
        params?: TranslationRequestParams,
    ): Promise<GenericItemResponse<PageResponse>> {
        return this.client.translation.translatePage(pageId, params).toPromise();
    }

    public translateText = (
        request: TranslationTextRequest,
    ): Observable<TranslationResponse> => {
        return this.client.translation.translateText(request);
    };
}

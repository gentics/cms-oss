import { MarkupLanguageListResponse } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

/**
 * API methods related to the available backend languages.
 */
export class MarkupLanguageApi {

    constructor(private apiBase: ApiBase) {}

    /** Load all available backend languages */
    getMarkupLanguages(): Observable<MarkupLanguageListResponse> {
        return this.apiBase.get('markupLanguage');
    }

}

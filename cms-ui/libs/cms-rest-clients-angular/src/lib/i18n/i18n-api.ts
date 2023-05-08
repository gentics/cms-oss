import { I18nLanguageListResponse, I18nLanguageResponse, I18nLanguageSetRequest, I18nLanguageSetResponse } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

/**
 * API methods related to the available backend languages.
 */
export class I18nApi {

    constructor(private apiBase: ApiBase) {}

    /** Load all available backend languages */
    getAvailableUiLanguages(): Observable<I18nLanguageListResponse> {
        return this.apiBase.get('i18n/list');
    }

    /** Get the backend language of the current user session. */
    getActiveUiLanguage(): Observable<I18nLanguageResponse> {
        return this.apiBase.get('i18n/get');
    }

    /** Set the backend language of the current user session. */
    setActiveUiLanguage(payload: I18nLanguageSetRequest): Observable<I18nLanguageSetResponse> {
        return this.apiBase.post('i18n/set', payload);
    }

}

import { LanguageCreateRequest, LanguageListOptions, LanguageListResponse, LanguageResponse, LanguageUpdateRequest } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';
import { stringifyPagingSortOptions } from '../util/sort-options/sort-options';

/**
 * API methods related to backend language settings and Internationalization.
 */
export class LanguageApi {

    constructor(private apiBase: ApiBase) {}

    /** Create new language */
    createLanguage(language: LanguageCreateRequest): Observable<LanguageResponse> {
        return this.apiBase.post('language', { name: language.name, code: language.code });
    }

    /** Load all available languages */
    getLanguages(options?: LanguageListOptions): Observable<LanguageListResponse> {
        if (options?.sort) {
            const copy: any = { ...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get('language', options);
    }

    /** Load the language with given id */
    getLanguage(id: number | string): Observable<LanguageResponse> {
        return this.apiBase.get(`language/${id}`);
    }

    /** Delete the language with given id */
    deleteLanguage(id: number | string): Observable<void> {
        return this.apiBase.delete(`language/${id}`);
    }

    /** Update the language with given id */
    updateLanguage(id: number | string, language: LanguageUpdateRequest): Observable<LanguageResponse> {
        return this.apiBase.put(`language/${id}`, language);
    }
}

import { Translator } from '@gentics/cms-integration-api-models';
import { I18nService } from '@gentics/cms-components';
import { Observable } from 'rxjs';

/**
 * Translator that uses the ngx-translate TranslateService to resolve the translations.
 */
export class TranslatorImpl implements Translator {

    constructor(private translateService: I18nService) { }

    get(key: string | string[], interpolateParams?: Object): Observable<string | Object> {
        return this.translateService.get(key, interpolateParams);
    }

    instant(key: string | string[], interpolateParams?: Object): string | Object {
        return this.translateService.instant(key, interpolateParams);
    }

}

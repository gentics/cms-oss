import { Translator } from '@gentics/cms-integration-api-models';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

/**
 * Translator that uses the ngx-translate TranslateService to resolve the translations.
 */
export class TranslatorImpl implements Translator {

    constructor(private translateService: TranslateService) { }

    get(key: string | string[], interpolateParams?: Object): Observable<string | Object> {
        return this.translateService.get(key, interpolateParams);
    }

    instant(key: string | string[], interpolateParams?: Object): string | Object {
        return this.translateService.instant(key, interpolateParams);
    }

}

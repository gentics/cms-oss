import { TranslateService } from '@ngx-translate/core';

/**
 * In many places we need to localize the page type and status. This method allows us to use the string `folder`
 * rather than `common.type_folder`. In the case of types, it also allows the use of a `count` param to create
 * the correct pluralized translation key.
 */
export function applyShortcuts(value: string, params?: any): string {
    switch (value) {
        case 'contenttag':
        case 'file':
        case 'folder':
        case 'form':
        case 'image':
        case 'node':
        case 'object':
        case 'page':
        case 'tag':
        case 'template':
        case 'templatetag':
        case 'linkedPage':
        case 'linkedFile':
        case 'linkedImage':
        case 'variant':
            let key = `common.type_${value}`;
            if (params && params.hasOwnProperty('count') && 1 !== params.count) {
                key += 's';
           }
            return key;
        case 'published':
        case 'edited':
        case 'offline':
        case 'queue':
        case 'timeframe':
        case 'publishat':
            return `common.status_${value}`;
        default:
            return value;
   }
}

export function translateParamsInstant(params: {[key: string]: any}, translate: TranslateService): {[key: string]: any} {
    const translated: {[key: string]: any} = {};
    for (const key in params) {
        if (key === '_lang' || key === '_language') {
            translated[key.substr(1)] = translate.instant('lang.' + params[key]);
       } else if (key[0] === '_') {
            translated[key.substr(1)] = translateParamValue(params[key], params, translate);
       } else {
            translated[key] = params[key];
       }
   }
    return translated;
}

/**
 * If a param value is one of the common pages, we translate it implicitly.
 */
function translateParamValue(value: any, params: {[key: string]: any}, translate: TranslateService): any {
    return translate.instant(applyShortcuts(value, params));
}

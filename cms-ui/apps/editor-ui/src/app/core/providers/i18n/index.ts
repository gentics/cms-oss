import {TranslateLoader, TranslateService} from '@ngx-translate/core';
import {I18nService} from './i18n.service';
import {CustomLoader} from './custom-loader';

export const I18N_PROVIDERS = [
    { provide: TranslateLoader, useClass: CustomLoader },
    TranslateService,
    I18nService
];

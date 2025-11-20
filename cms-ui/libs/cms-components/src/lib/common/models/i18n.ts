import { INotificationOptions } from '@gentics/ui-core';
import { InterpolationParameters } from '@ngx-translate/core';

export const FALLBACK_LANGUAGE = 'en';

export type TranslateParameters = InterpolationParameters;

export interface ParametizedI18nKey {
    /** The i18n key that will be passed to the I18nService. */
    key: string;

    /** Parameters that should be passed to the I18nService. */
    params: TranslateParameters;
}

export interface TranslatedNotificationOptions extends INotificationOptions {
    translationParams?: { [key: string]: any };
}

export interface JoinOptions {
    withLast?: boolean;
    quoted?: boolean;
    separator?: string;
}

/** Type alias for specifying that a parameter or property needs to be set to an i18n key. */
export type I18nKey = string | ParametizedI18nKey;

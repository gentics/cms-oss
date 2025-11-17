import { IndexByKey } from '@gentics/cms-models';

export interface ParametizedI18nKey {
    /** The i18n key that will be passed to the I18nService. */
    key: string;

    /** Parameters that should be passed to the I18nService. */
    params: IndexByKey<any>;
}

export interface JoinOptions {
    withLast?: boolean;
    quoted?: boolean;
    separator?: string;
}

/** Type alias for specifying that a parameter or property needs to be set to an i18n key. */
export type I18nKey = string | ParametizedI18nKey;

import { IndexByKey } from '@gentics/cms-models';

export type BooleanFn = () => boolean;

/** Interface necessary to implement for the guarded component */
export interface OnDiscardChanges {
    /** Flag if should not trigger a close when navigating away. */
    skipClose?: boolean;
    /** Returns TRUE if user has changed something */
    userHasEdited: boolean | BooleanFn;
    /** Returns TRUE if the changes are valid */
    changesValid: boolean | BooleanFn;
    /** Update entity data of invoking component */
    updateEntity: () => Promise<void>;
    /** Reset entity data of invoking component */
    resetEntity: () => Promise<void>;
}

export interface ParametizedI18nKey {
    /** The i18n key that will be passed to the I18nService. */
    key: string;

    /** Parameters that should be passed to the I18nService. */
    params: IndexByKey<any>;
}

/** Type alias for specifying that a parameter or property needs to be set to an i18n key. */
export type I18nKey = string | ParametizedI18nKey;

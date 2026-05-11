import { I18nString } from '@gentics/cms-models';

export function createDefaultFormPageName(): I18nString {
    return {
        de: 'Allgemein',
        en: 'General',
    };
}

/**
 * URL to load when we want to unload a GCMS document, either when closing the frame or switching
 * to a difference url. Required to trigger the beforeunload & unload events which allow
 * us to run some logic to check whether it is safe to navigate away or not.
 */
export const BLANK_PAGE = 'about:blank';

/**
 * This html document is loaded into the iframe when the folder/page/etc properties form is being displayed.
 * It is never directly viewed by the user, but is needed in order for the existing system of "beforeunload"
 * dialogs to work reliably cross browser.
 */
export const BLANK_PROPERTIES_PAGE = 'assets/properties-blank.html';

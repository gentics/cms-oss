import type { TFunction } from 'i18next';

import type { RefusalReason } from '@/helper/cmsEditable/cmsEditable';

/** Why an edit could not be saved directly in the CMS, and that the chat can have it changed (`preview.save.*`). */
export function refusalText(t: TFunction, reason: RefusalReason): string {
    return `${t(`preview.save.refused.${reason}`)} ${t('preview.save.hint')}`;
}

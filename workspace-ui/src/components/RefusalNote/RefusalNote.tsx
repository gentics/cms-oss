import { useTranslation } from 'react-i18next';

import type { RefusalReason } from '@/helper/cmsEditable/cmsEditable';
import { refusalText } from '@/helper/refusalText/refusalText';

import styles from './RefusalNote.module.css';

/** Why an edit could not be saved directly in the CMS, and that the chat can have it changed (`preview.save.*`). */
export function RefusalNote({ reason }: { reason: RefusalReason }) {
    const { t } = useTranslation();

    return <p className={styles.refusal} role="alert">{refusalText(t, reason)}</p>;
}

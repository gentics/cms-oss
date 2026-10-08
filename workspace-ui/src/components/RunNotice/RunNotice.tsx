import { CircleAlertIcon, CircleXIcon, KeyRoundIcon, TriangleAlertIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { problemMessageKey } from '@/helper/errorMapper/errorMapper';
import type { RunNotice as Notice } from '@/store/useWorkspaceEventStore';

import styles from './RunNotice.module.css';

/**
 * A run's ending or error in the stream: Stopped (`run.cancelled`), failed with its problem
 * (`run.failed`), a recoverable `error`, or the MCP connection it could not use (`auth.required`,
 * whose `how_to_fix` the contract marks as safe to show).
 */
export function RunNotice({ notice }: { notice: Notice }) {
    const { t } = useTranslation();

    switch (notice.kind) {
        case 'cancelled':
            return (
                <div className={styles.notice} role="note">
                    <CircleXIcon size={16} aria-hidden />
                    <span className={styles.title}>{t('chat.notice.cancelled')}</span>
                </div>
            );
        case 'failed':
        case 'error':
            return (
                <div className={`${styles.notice} ${notice.kind === 'failed' ? styles.error : styles.warning}`} role="note">
                    {notice.kind === 'failed' ? <CircleAlertIcon size={16} aria-hidden /> : <TriangleAlertIcon size={16} aria-hidden />}
                    <div>
                        <span className={styles.title}>{t(notice.kind === 'failed' ? 'chat.notice.failed' : 'chat.notice.error')}</span>
                        <p className={styles.detail}>{t(problemMessageKey(notice.error))}</p>
                        {notice.error.detail && <p className={styles.detail}>{notice.error.detail}</p>}
                    </div>
                </div>
            );
        case 'auth':
            return (
                <div className={`${styles.notice} ${styles.warning}`} role="note">
                    <KeyRoundIcon size={16} aria-hidden />
                    <div>
                        <span className={styles.title}>{t('chat.notice.auth', { connector: notice.connector })}</span>
                        <p className={styles.detail}>{notice.howToFix}</p>
                    </div>
                </div>
            );
        default:
            return null;
    }
}

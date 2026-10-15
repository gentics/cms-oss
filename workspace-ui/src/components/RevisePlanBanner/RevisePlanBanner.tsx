import { PencilIcon, XIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { IconButton } from '@/components/IconButton/IconButton';
import type { Revision } from '@/helper/planActions/planActions';
import type { Plan } from '@/services/apiService/genaix/types';

import styles from './RevisePlanBanner.module.css';

interface RevisePlanBannerProps {
    plan: Plan;
    /** `stop` while the plan executes: sending stops its run (a redirect, rc.5). */
    revision: Revision;
    onCancel: () => void;
}

/**
 * At the top of the chat composer while its next message revises a plan (`useRevisePlanStore`): which
 * plan, what sending does to it, and × to leave that mode (Esc in the field does the same,
 * `SessionComposer`).
 */
export function RevisePlanBanner({ plan, revision, onCancel }: RevisePlanBannerProps) {
    const { t } = useTranslation();

    return (
        <div className={styles.banner} role="status">
            <PencilIcon size={14} className={styles.icon} aria-hidden />
            <span className={styles.text}>
                <span className={styles.title}>{t('composer.revise.title')}</span>
                {plan.summary && <span className={styles.summary}>{plan.summary}</span>}
                <span className={styles.hint}>{t(`composer.revise.hint.${revision}`)}</span>
            </span>
            <IconButton variant="ghost" size="icon" label={t('composer.revise.cancel')} onClick={onCancel}>
                <XIcon size={14} />
            </IconButton>
        </div>
    );
}

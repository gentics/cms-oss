import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';

import { ThinkingDots } from '@/components/ThinkingIndicator/ThinkingIndicator';

import styles from './PublishingOverlay.module.css';

/**
 * Over the whole page while a plan is published: the quality gates check it (`before_publish`) and its
 * run puts the pages online. Nothing behind it can be reached meanwhile: the app's root is `inert`
 * until the overlay goes, so neither a click nor the keyboard starts anything else.
 */
export function PublishingOverlay({ pageCount }: { pageCount: number }) {
    const { t } = useTranslation();

    useEffect(() => {
        const root = document.getElementById('root');

        if (!root) {
            return;
        }

        root.inert = true;

        return () => {
            root.inert = false;
        };
    }, []);

    return createPortal(
        <div className={styles.overlay}>
            <div className={styles.panel} role="status">
                <ThinkingDots isLarge />
                <span className={styles.title}>{t('planBar.publishing.title')}</span>
                {pageCount > 0 && <span className={styles.text}>{t('planBar.publishing.pages', { count: pageCount })}</span>}
            </div>
        </div>,
        document.body,
    );
}

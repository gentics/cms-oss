import { useTranslation } from 'react-i18next';

import styles from './ThinkingIndicator.module.css';

/**
 * Shows that the agent is working (design.md §8 "Denkt"): the three dots and the latest `status`
 * text, or "Thinking" without one. A live region, so the status is announced as it changes.
 */
export function ThinkingIndicator({ text }: { text?: string }) {
    const { t } = useTranslation();

    return (
        <div className={styles.thinking} role="status">
            <span className={styles.dots} aria-hidden>
                <i />
                <i />
                <i />
            </span>
            <span>{text || t('chat.thinking')}</span>
        </div>
    );
}

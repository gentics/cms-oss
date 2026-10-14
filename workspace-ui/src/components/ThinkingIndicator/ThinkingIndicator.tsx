import { useTranslation } from 'react-i18next';

import styles from './ThinkingIndicator.module.css';

/**
 * The three dots of "Denkt" (design.md §8), decorative. `inheritsColor` paints them in the text colour,
 * for a surface in `--interactive-fill` such as a primary button.
 */
export function ThinkingDots({ inheritsColor = false }: { inheritsColor?: boolean }) {
    return (
        <span className={inheritsColor ? `${styles.dots} ${styles.inherit}` : styles.dots} aria-hidden>
            <i />
            <i />
            <i />
        </span>
    );
}

/**
 * Shows that the agent is working (design.md §8 "Denkt"): the three dots and the latest `status`
 * text, or "Thinking" without one. A live region, so the status is announced as it changes.
 */
export function ThinkingIndicator({ text }: { text?: string }) {
    const { t } = useTranslation();

    return (
        <div className={styles.thinking} role="status">
            <ThinkingDots />
            <span>{text || t('chat.thinking')}</span>
        </div>
    );
}

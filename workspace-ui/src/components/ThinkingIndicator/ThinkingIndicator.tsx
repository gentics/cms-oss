import { useTranslation } from 'react-i18next';

import styles from './ThinkingIndicator.module.css';

/**
 * The three dots of "Denkt" (design.md §8), decorative. `inheritsColor` paints them in the text colour,
 * for a surface in `--interactive-fill` such as a primary button; `isLarge` makes them the one thing to
 * see on a surface that waits, such as the review panel's checklist while the quality gates run.
 */
export function ThinkingDots({ inheritsColor = false, isLarge = false }: { inheritsColor?: boolean; isLarge?: boolean }) {
    return (
        <span className={[styles.dots, inheritsColor && styles.inherit, isLarge && styles.large].filter(Boolean).join(' ')} aria-hidden>
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

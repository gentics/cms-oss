import { useTranslation } from 'react-i18next';

import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

import styles from './ErrorNotifications.module.css';

// Icons: Lucide `circle-alert` and `x` (lucide-static 1.49.0, ISC), stroke width per design.md §9.
const iconProps = {
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.7,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    'aria-hidden': true,
} as const;

/** Shows every error in `useErrorNotificationStore` as a dismissible notification. */
export function ErrorNotifications() {
    const { t } = useTranslation();
    const errors = useErrorNotificationStore((state) => state.errors);
    const dismissError = useErrorNotificationStore((state) => state.dismissError);

    if (errors.length === 0) {
        return null;
    }

    return (
        <section className={styles.region} aria-label={t('errorNotifications.label')}>
            <ul className={styles.list}>
                {errors.map((error) => (
                    <li key={error.id} className={styles.item}>
                        <div role="alert" className={styles.content}>
                            <svg {...iconProps} className={styles.icon} width="18" height="18">
                                <circle cx="12" cy="12" r="10" />
                                <line x1="12" x2="12" y1="8" y2="12" />
                                <line x1="12" x2="12.01" y1="16" y2="16" />
                            </svg>
                            <div className={styles.message}>
                                <p className={styles.text}>{t(error.messageKey)}</p>
                                {error.detail && <p className={styles.detail}>{error.detail}</p>}
                            </div>
                        </div>
                        <button
                            type="button"
                            className={styles.dismiss}
                            aria-label={t('errorNotifications.dismiss')}
                            onClick={() => dismissError(error.id)}
                        >
                            <svg {...iconProps} width="16" height="16">
                                <path d="M18 6 6 18" />
                                <path d="m6 6 12 12" />
                            </svg>
                        </button>
                    </li>
                ))}
            </ul>
        </section>
    );
}

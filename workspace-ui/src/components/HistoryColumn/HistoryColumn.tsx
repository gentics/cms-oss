import { useId } from 'react';
import { useTranslation } from 'react-i18next';

import { SessionSearch } from '@/components/SessionSearch/SessionSearch';

import styles from './HistoryColumn.module.css';

/** The left column: the whole session history, searchable (final_draft.html "COLUMN 1"). */
export function HistoryColumn() {
    const { t } = useTranslation();
    const titleId = useId();

    return (
        <section className={styles.column} aria-labelledby={titleId}>
            <header className={styles.head}>
                <h2 id={titleId} className={styles.title}>{t('sessions.title')}</h2>
            </header>
            <SessionSearch />
        </section>
    );
}

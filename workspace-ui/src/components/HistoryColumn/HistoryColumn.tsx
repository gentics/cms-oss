import { Link } from '@tanstack/react-router';
import { PlusIcon } from 'lucide-react';
import { useId } from 'react';
import { useTranslation } from 'react-i18next';

import { SessionSearch } from '@/components/SessionSearch/SessionSearch';
import { Button } from '@/components/ui/button';

import styles from './HistoryColumn.module.css';

/**
 * The left column: the whole session history, searchable, and "New", which goes back to the
 * dashboard's prompt to start a session (final_draft.html "COLUMN 1", `#newSess`).
 */
export function HistoryColumn() {
    const { t } = useTranslation();
    const titleId = useId();

    return (
        <section className={styles.column} aria-labelledby={titleId}>
            <header className={styles.head}>
                <h2 id={titleId} className={styles.title}>{t('sessions.title')}</h2>
                {/* A link in the look of a button, as the brand in the Topbar. */}
                <Button size="sm" nativeButton={false} role={undefined} render={<Link to="/" />} title={t('sessions.newTitle')}>
                    <PlusIcon size={14} aria-hidden="true" />
                    {t('sessions.new')}
                </Button>
            </header>
            <SessionSearch />
        </section>
    );
}

import { Link } from '@tanstack/react-router';
import { TrashIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { formatSessionDate, sessionTitle } from '@/helper/sessionDisplay/sessionDisplay';
import { useDeleteSession } from '@/hooks/useDeleteSession';
import type { useSessions } from '@/hooks/useGenaixQueries';
import { useSessionArchiveStore } from '@/store/useSessionArchiveStore';

import styles from './SessionList.module.css';

interface SessionListProps {
    /** The result of `useSessions`, with the filters of the list that shows it. */
    sessions: ReturnType<typeof useSessions>;
    /** The search text, for the "no match" message. */
    searchQuery?: string;
    /** Shows at most this many rows and no "Show more" (recent sessions). */
    maxItems?: number;
    /** Called when a row is followed, e.g. to close the drawer around the list. */
    onNavigate?: () => void;
}

/**
 * Session rows, newest activity first: title and the day it was started, a link to the session,
 * and a delete button on hover or focus (design.md §12 "Listenzeile"). Deleted sessions are hidden
 * at once (`useDeleteSession`). "Show more" loads the next page.
 */
export function SessionList({ sessions, searchQuery = '', maxItems, onNavigate }: SessionListProps) {
    const { t, i18n } = useTranslation();
    const hiddenIds = useSessionArchiveStore((state) => state.hiddenIds);
    const deleteSession = useDeleteSession();

    if (sessions.isPending) {
        return <p className={styles.state}>{t('sessions.loading')}</p>;
    }

    if (sessions.isError && !sessions.data) {
        return (
            <div className={styles.state} role="alert">
                <p>{t(errorMessageKey(sessions.error))}</p>
                <Button size="sm" onClick={() => void sessions.refetch()}>{t('sessions.retry')}</Button>
            </div>
        );
    }

    const visible = sessions.data.pages.flatMap((page) => page.items).filter((session) => !hiddenIds.includes(session.id));
    const shown = maxItems === undefined ? visible : visible.slice(0, maxItems);
    const canLoadMore = maxItems === undefined && sessions.hasNextPage;

    if (shown.length === 0 && !canLoadMore) {
        return (
            <p className={styles.state}>
                {searchQuery ? t('sessions.noMatch', { query: searchQuery }) : t('sessions.empty')}
            </p>
        );
    }

    return (
        <>
            <ul className={styles.list}>
                {shown.map((session) => {
                    const title = sessionTitle(session, t);

                    return (
                        <li key={session.id} className={styles.item}>
                            <Link to="/sessions/$id" params={{ id: session.id }} className={styles.row} onClick={onNavigate}>
                                <span className={styles.name}>{title}</span>
                                <span className={styles.meta}>{formatSessionDate(session.created_at, i18n.language)}</span>
                            </Link>
                            <span className={styles.delete}>
                                <Button
                                    variant="ghost-danger"
                                    size="icon"
                                    aria-label={t('sessions.delete', { title })}
                                    title={t('sessions.delete', { title })}
                                    onClick={() => deleteSession(session)}
                                >
                                    <TrashIcon size={16} />
                                </Button>
                            </span>
                        </li>
                    );
                })}
            </ul>
            {canLoadMore && (
                <div className={styles.more}>
                    <Button
                        variant="ghost"
                        size="sm"
                        disabled={sessions.isFetchingNextPage}
                        onClick={() => void sessions.fetchNextPage()}
                    >
                        {t('sessions.showMore')}
                    </Button>
                </div>
            )}
        </>
    );
}

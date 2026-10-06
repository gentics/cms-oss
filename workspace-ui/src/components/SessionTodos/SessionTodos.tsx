import { Link } from '@tanstack/react-router';
import { useId } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { formatSessionDate, sessionTitle } from '@/helper/sessionDisplay/sessionDisplay';
import { useSessions } from '@/hooks/useGenaixQueries';
import type { SessionStatus } from '@/services/apiService/genaix/types';
import { useSessionArchiveStore } from '@/store/useSessionArchiveStore';

import styles from './SessionTodos.module.css';

/**
 * The statuses that make a session a to-do, in the order of their groups: the user's own sessions
 * that wait, for a release, for the reviewer, or for the user's answer (roadmap G7).
 */
const TODO_STATUSES = ['released', 'review_requested', 'waiting_for_input'] as const satisfies readonly SessionStatus[];

/**
 * "Open to-dos" on the dashboard: one group per to-do status, newest activity first. One query
 * for all three statuses; "Show more" loads its next page, whose sessions join their groups.
 * Renders nothing while there are none.
 */
export function SessionTodos() {
    const { t, i18n } = useTranslation();
    const titleId = useId();
    const sessions = useSessions({ status: [...TODO_STATUSES] });
    const hiddenIds = useSessionArchiveStore((state) => state.hiddenIds);

    if (sessions.isPending) {
        return null;
    }

    const visible = sessions.data?.pages.flatMap((page) => page.items).filter((session) => !hiddenIds.includes(session.id)) ?? [];

    if (!sessions.isError && visible.length === 0 && !sessions.hasNextPage) {
        return null;
    }

    return (
        <section className={styles.todos} aria-labelledby={titleId}>
            <h2 id={titleId} className={styles.title}>{t('todos.title')}</h2>

            {sessions.isError && !sessions.data && (
                <div className={styles.state} role="alert">
                    <p>{t(errorMessageKey(sessions.error))}</p>
                    <Button size="sm" onClick={() => void sessions.refetch()}>{t('sessions.retry')}</Button>
                </div>
            )}

            {TODO_STATUSES.map((status) => {
                const group = visible.filter((session) => session.status === status);
                const groupId = `${titleId}-${status}`;

                return group.length > 0 && (
                    <section key={status} className={styles.group} aria-labelledby={groupId}>
                        <h3 id={groupId} className={styles.groupTitle}>{t(`todos.groups.${status}`)}</h3>
                        <ul className={styles.list}>
                            {group.map((session) => (
                                <li key={session.id}>
                                    <Link to="/sessions/$id" params={{ id: session.id }} className={styles.todo}>
                                        <span className={styles.name}>{sessionTitle(session, t)}</span>
                                        <span className={styles.meta}>{formatSessionDate(session.last_activity_at, i18n.language)}</span>
                                    </Link>
                                </li>
                            ))}
                        </ul>
                    </section>
                );
            })}

            {sessions.hasNextPage && (
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
        </section>
    );
}

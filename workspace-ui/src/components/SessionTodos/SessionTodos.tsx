import { Link } from '@tanstack/react-router';
import { useId } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { formatSessionDate, sessionTitle } from '@/helper/sessionDisplay/sessionDisplay';
import { useSessions } from '@/hooks/useGenaixQueries';
import type { SessionFilters } from '@/services/apiService/apiService';
import { useSessionArchiveStore } from '@/store/useSessionArchiveStore';

import styles from './SessionTodos.module.css';

/**
 * What makes a session a to-do, in the order of their groups: the user's own sessions that wait, for
 * a release, for the reviewer, or for the user's answer (roadmap G7). Since rc.4 releasing and review
 * belong to a plan, so the first two filter on the open plan's status, else the latest plan's
 * (`plan_status`); the session itself is only `active`, `waiting_for_input` or `archived`.
 */
const RELEASED: SessionFilters = { plan_status: ['released'] };
const REVIEW_REQUESTED: SessionFilters = { plan_status: ['review_requested'] };
const WAITING_FOR_INPUT: SessionFilters = { status: ['waiting_for_input'] };

/**
 * "Open to-dos" on the dashboard: one group per to-do, newest activity first. One query per group,
 * since a session does not carry its plan's status; "Show more" loads the next page of every group
 * that has one. Renders nothing while there are none.
 */
export function SessionTodos() {
    const { t, i18n } = useTranslation();
    const titleId = useId();
    const groups = [
        { key: 'released', sessions: useSessions(RELEASED) },
        { key: 'review_requested', sessions: useSessions(REVIEW_REQUESTED) },
        { key: 'waiting_for_input', sessions: useSessions(WAITING_FOR_INPUT) },
    ] as const;
    const hiddenIds = useSessionArchiveStore((state) => state.hiddenIds);

    if (groups.some(({ sessions }) => sessions.isPending)) {
        return null;
    }

    const visible = groups.map(({ key, sessions }) => ({
        key,
        items: sessions.data?.pages.flatMap((page) => page.items).filter((session) => !hiddenIds.includes(session.id)) ?? [],
    }));
    const failed = groups.filter(({ sessions }) => sessions.isError && !sessions.data);
    const withMore = groups.filter(({ sessions }) => sessions.hasNextPage);

    if (failed.length === 0 && visible.every(({ items }) => items.length === 0) && withMore.length === 0) {
        return null;
    }

    return (
        <section className={styles.todos} aria-labelledby={titleId}>
            <h2 id={titleId} className={styles.title}>{t('todos.title')}</h2>

            {failed[0] && (
                <div className={styles.state} role="alert">
                    <p>{t(errorMessageKey(failed[0].sessions.error))}</p>
                    <Button size="sm" onClick={() => failed.forEach(({ sessions }) => void sessions.refetch())}>{t('sessions.retry')}</Button>
                </div>
            )}

            {visible.map(({ key, items }) => {
                const groupId = `${titleId}-${key}`;

                return items.length > 0 && (
                    <section key={key} className={styles.group} aria-labelledby={groupId}>
                        <h3 id={groupId} className={styles.groupTitle}>{t(`todos.groups.${key}`)}</h3>
                        <ul className={styles.list}>
                            {items.map((session) => (
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

            {withMore.length > 0 && (
                <div className={styles.more}>
                    <Button
                        variant="ghost"
                        size="sm"
                        disabled={withMore.some(({ sessions }) => sessions.isFetchingNextPage)}
                        onClick={() => withMore.forEach(({ sessions }) => void sessions.fetchNextPage())}
                    >
                        {t('sessions.showMore')}
                    </Button>
                </div>
            )}
        </section>
    );
}

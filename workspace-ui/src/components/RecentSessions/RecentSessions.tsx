import { ChevronDownIcon, RotateCcwClockIcon } from 'lucide-react';
import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { AllSessionsDrawer } from '@/components/AllSessionsDrawer/AllSessionsDrawer';
import { SessionList } from '@/components/SessionList/SessionList';
import { Button } from '@/components/ui/button';
import { useSessions } from '@/hooks/useGenaixQueries';
import { useSessionArchiveStore } from '@/store/useSessionArchiveStore';

import styles from './RecentSessions.module.css';

/** How many sessions the folded-out line shows before "Show all sessions". */
const RECENT_SESSION_COUNT = 3;

/**
 * "Recent sessions" on the dashboard: one line that folds out the newest sessions; "Show all
 * sessions" opens the "All sessions" drawer (final_draft.html `#dashHist`).
 */
export function RecentSessions() {
    const { t } = useTranslation();
    const listId = useId();
    const [isOpen, setIsOpen] = useState(false);
    const [isDrawerOpen, setIsDrawerOpen] = useState(false);
    const sessions = useSessions();
    const hiddenIds = useSessionArchiveStore((state) => state.hiddenIds);
    const visibleCount = sessions.data?.pages.flatMap((page) => page.items).filter((session) => !hiddenIds.includes(session.id)).length ?? 0;
    const hasMore = visibleCount > RECENT_SESSION_COUNT || sessions.hasNextPage;

    return (
        <div className={styles.recent}>
            <Button variant="ghost" aria-expanded={isOpen} aria-controls={listId} onClick={() => setIsOpen((open) => !open)}>
                <RotateCcwClockIcon size={16} aria-hidden="true" />
                {t('sessions.recent')}
                <span className={styles.chevron} data-open={isOpen || undefined}>
                    <ChevronDownIcon size={16} aria-hidden="true" />
                </span>
            </Button>
            <div id={listId} className={styles.list} hidden={!isOpen}>
                {isOpen && (
                    <>
                        <SessionList sessions={sessions} maxItems={RECENT_SESSION_COUNT} />
                        {hasMore && (
                            <Button variant="ghost" size="sm" onClick={() => setIsDrawerOpen(true)}>
                                {t('sessions.showAll')}
                            </Button>
                        )}
                    </>
                )}
            </div>
            <AllSessionsDrawer open={isDrawerOpen} onOpenChange={setIsDrawerOpen} />
        </div>
    );
}

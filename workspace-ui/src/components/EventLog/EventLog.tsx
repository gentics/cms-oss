import { CopyIcon } from 'lucide-react';
import { useId } from 'react';
import { useTranslation } from 'react-i18next';

import { IconButton } from '@/components/IconButton/IconButton';
import { useToast } from '@/components/ui/use-toast';
import type { GenaixEvent } from '@/services/apiService/genaix/types';
import { useEventLogStore } from '@/store/useEventLogStore';

import styles from './EventLog.module.css';

// One empty list for every session without events, so the selector returns a stable value.
const NO_EVENTS: GenaixEvent[] = [];

/**
 * Debug only (`npm run dev:debug`): the events of one session's stream as `useSessionEvents` passed
 * them on (`useEventLogStore`), one row each with its `seq` and type. The copy button puts the event,
 * the `data:` of its SSE frame, into the clipboard as JSON.
 */
export function EventLog({ sessionId }: { sessionId: string }) {
    const { t } = useTranslation();
    const toast = useToast();
    const titleId = useId();
    const events = useEventLogStore((state) => state.events[sessionId] ?? NO_EVENTS);

    async function copy(event: GenaixEvent) {
        try {
            await navigator.clipboard.writeText(JSON.stringify(event, null, 2));
            toast.add({ title: t('eventLog.copied', { seq: event.seq }) });
        } catch {
            toast.add({ title: t('eventLog.copyFailed'), type: 'error' });
        }
    }

    return (
        <section className={styles.log} aria-labelledby={titleId}>
            <header className={styles.head}>
                <h2 id={titleId} className={styles.title}>{t('eventLog.title')}</h2>
                <span className={styles.count}>{events.length}</span>
            </header>
            {events.length === 0
                ? <p className={styles.empty}>{t('eventLog.empty')}</p>
                : (
                    <ol className={styles.list}>
                        {/* By position: the list only grows, and after `410 events_pruned` the replay repeats a `seq`. */}
                        {events.map((event, index) => (
                            <li key={index} className={styles.row}>
                                <span className={styles.seq}>{event.seq}</span>
                                <span className={styles.type}>{event.type}</span>
                                <IconButton
                                    variant="ghost"
                                    size="icon-xs"
                                    label={t('eventLog.copy', { seq: event.seq, type: event.type })}
                                    onClick={() => void copy(event)}
                                >
                                    <CopyIcon size={12} />
                                </IconButton>
                            </li>
                        ))}
                    </ol>
                )}
        </section>
    );
}

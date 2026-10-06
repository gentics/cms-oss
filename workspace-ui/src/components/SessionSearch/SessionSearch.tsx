import { SearchIcon } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { SessionList } from '@/components/SessionList/SessionList';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { useSessions } from '@/hooks/useGenaixQueries';

import styles from './SessionSearch.module.css';

/** Waiting time after the last key before the list is searched. */
const SEARCH_DELAY_MS = 250;

/** `q` of `GET /sessions` takes at most 200 characters (`openapi.yaml`, `listSessions`). */
const MAX_QUERY_LENGTH = 200;

interface SessionSearchProps {
    /** Focuses the search field when it mounts, e.g. when the drawer opens. */
    autoFocus?: boolean;
    /** Called when a session is followed (`SessionList`). */
    onNavigate?: () => void;
}

/**
 * "Find a session" above the user's sessions. The search runs on the server (`q` over title and
 * intent) once typing pauses; Escape clears it.
 */
export function SessionSearch({ autoFocus = false, onNavigate }: SessionSearchProps) {
    const { t } = useTranslation();
    const [input, setInput] = useState('');
    const query = useDebouncedValue(input.trim(), SEARCH_DELAY_MS);
    const sessions = useSessions(query ? { q: query } : {});

    return (
        <div className={styles.search}>
            <label className={styles.field}>
                <SearchIcon size={14} aria-hidden="true" />
                <input
                    type="search"
                    value={input}
                    maxLength={MAX_QUERY_LENGTH}
                    placeholder={t('sessions.find')}
                    aria-label={t('sessions.find')}
                    autoFocus={autoFocus}
                    onChange={(event) => setInput(event.target.value)}
                    onKeyDown={(event) => {
                        if (event.key === 'Escape' && input) {
                            event.stopPropagation();
                            setInput('');
                        }
                    }}
                />
            </label>
            <div className={styles.results}>
                <SessionList sessions={sessions} searchQuery={query} onNavigate={onNavigate} />
            </div>
        </div>
    );
}

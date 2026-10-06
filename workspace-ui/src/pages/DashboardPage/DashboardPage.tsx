import { useNavigate } from '@tanstack/react-router';
import { PencilIcon, PlusIcon, SearchIcon } from 'lucide-react';
import { useRef } from 'react';
import { useTranslation } from 'react-i18next';

import { Composer, type ComposerHandle } from '@/components/Composer/Composer';
import { Topbar } from '@/components/Topbar/Topbar';
import { Button } from '@/components/ui/button';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { type StartSessionInput, useStartSession } from '@/hooks/useGenaixQueries';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

import styles from './DashboardPage.module.css';

// TEMPORARY: GET /me knows no name, only an opaque subject. Replace once the user's name is available.
const GREETING_NAME = 'Dominik';

// Icons per design.md §9: Bearbeiten, Hinzufügen, Suchen.
const STARTERS = [
    { key: 'shortenIntro', Icon: PencilIcon },
    { key: 'landingPage', Icon: PlusIcon },
    { key: 'untouchedPages', Icon: SearchIcon },
] as const;

// The greeting by the hour, as in the draft (`greet`).
function greetingKey(hour: number): string {
    if (hour < 5) {
        return 'dashboard.greeting.night';
    }

    if (hour < 12) {
        return 'dashboard.greeting.morning';
    }

    return hour < 18 ? 'dashboard.greeting.afternoon' : 'dashboard.greeting.evening';
}

/**
 * The page at `/`: the prompt that starts a session. Once the session exists, and its files are
 * uploaded, the session's page opens.
 */
export function DashboardPage() {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const composerRef = useRef<ComposerHandle>(null);
    const startSession = useStartSession();

    function handleSubmit(input: StartSessionInput) {
        startSession.mutate(input, {
            onSuccess: (session) => {
                void navigate({ to: '/sessions/$id', params: { id: session.id } });
            },
            onError: (error) => {
                useErrorNotificationStore.getState().addError({ messageKey: 'dashboard.startFailed', detailKey: errorMessageKey(error) });
            },
        });
    }

    return (
        <div className={styles.page}>
            <Topbar />
            <main className={styles.scroll}>
                <div className={styles.content}>
                    <h1 className={styles.greeting}>{t(greetingKey(new Date().getHours()), { name: GREETING_NAME })}</h1>

                    <Composer ref={composerRef} variant="start" onSubmit={handleSubmit} isSubmitting={startSession.isPending} />

                    <ul className={styles.starters}>
                        {STARTERS.map(({ key, Icon }) => (
                            <li key={key} className={styles.starter}>
                                <Button variant="ghost" onClick={() => composerRef.current?.setText(t(`dashboard.starters.${key}`))}>
                                    <Icon size={16} />
                                    <span className={styles.starterLabel}>{t(`dashboard.starters.${key}`)}</span>
                                </Button>
                            </li>
                        ))}
                    </ul>
                </div>
            </main>
        </div>
    );
}

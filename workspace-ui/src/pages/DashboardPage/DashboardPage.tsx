import { useNavigate } from '@tanstack/react-router';
import { useTranslation } from 'react-i18next';

import { Composer } from '@/components/Composer/Composer';
import { Topbar } from '@/components/Topbar/Topbar';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { type StartSessionInput, useStartSession } from '@/hooks/useGenaixQueries';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

import styles from './DashboardPage.module.css';

// TEMPORARY: GET /me knows no name, only an opaque subject. Replace once the user's name is available.
const GREETING_NAME = 'Dominik';

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

                    <Composer variant="start" onSubmit={handleSubmit} isSubmitting={startSession.isPending} />
                </div>
            </main>
        </div>
    );
}

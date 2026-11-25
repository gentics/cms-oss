import { useRef } from 'react';
import { useTranslation } from 'react-i18next';

import { Composer, type ComposerHandle } from '@/components/Composer/Composer';
import { RecentSessions } from '@/components/RecentSessions/RecentSessions';
import { TaskDrawer } from '@/components/TaskDrawer/TaskDrawer';
import { Topbar } from '@/components/Topbar/Topbar';
import { useCmsUser } from '@/hooks/useCmsAuth';
import { useSessionStart } from '@/hooks/useSessionStart';

import styles from './DashboardPage.module.css';

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
 * The page at `/`: the prompt that starts a session, the open tasks (`TaskDrawer`) and the recent sessions. Once
 * the session exists, and its files are uploaded, the session's page opens.
 */
export function DashboardPage() {
    const { t } = useTranslation();
    const { start, isStarting, uploadProgress } = useSessionStart();
    const composerRef = useRef<ComposerHandle>(null);
    // The CMS user of the session (`LoginGate` shows the page only with one): first name, the login without one.
    const cmsUser = useCmsUser({ refetchOnMount: false }).data;
    const greetingName = cmsUser ? (cmsUser.firstName || cmsUser.login) : null;

    return (
        <div className={styles.page}>
            <Topbar />
            <div className={styles.body}>
                <main className={styles.scroll}>
                    <div className={styles.content}>
                        {greetingName && <h1 className={styles.greeting}>{t(greetingKey(new Date().getHours()), { name: greetingName })}</h1>}

                        <Composer ref={composerRef} variant="start" onSubmit={start} isSubmitting={isStarting} uploadProgress={uploadProgress} />

                        <div className={styles.todos}>
                            <TaskDrawer onEditWithGenaix={(references) => composerRef.current?.addReferences(references)} />
                        </div>

                        <RecentSessions />
                    </div>
                </main>
            </div>
        </div>
    );
}

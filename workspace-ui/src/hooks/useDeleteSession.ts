import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';

import { useToast } from '@/components/ui/use-toast';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { sessionTitle } from '@/helper/sessionDisplay/sessionDisplay';
import { useArchiveSession, useUnarchiveSession } from '@/hooks/useGenaixQueries';
import type { Session } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { useSessionArchiveStore } from '@/store/useSessionArchiveStore';

/**
 * Deleting a session: the row disappears and the session is archived (`DELETE /sessions/{session_id}`)
 * at once, so it stays deleted whatever happens to the page next; a toast offers "Undo", which makes it
 * active again (`unarchive`, rc.4) and shows the row again. A failed archive shows the row again, a
 * failed undo leaves it away; either with an error notification. Needs `UiProvider` around it.
 */
export function useDeleteSession() {
    const { t } = useTranslation();
    const toast = useToast();
    const { mutateAsync: archive } = useArchiveSession();
    const { mutateAsync: unarchive } = useUnarchiveSession();

    return useCallback((session: Pick<Session, 'id' | 'title' | 'intent'>) => {
        const { hide, restore } = useSessionArchiveStore.getState();
        const { addError } = useErrorNotificationStore.getState();

        hide(session.id);

        const toastId = toast.add({
            title: t('sessions.deleted', { title: sessionTitle(session, t) }),
            actionProps: {
                children: t('sessions.undo'),
                onClick: () => {
                    toast.close(toastId);
                    void undo();
                },
            },
        });

        // The promises, not `mutate` callbacks: those are dropped once the list that showed the row
        // has unmounted, for example a closed drawer.
        const isArchived = archive(session.id).then(
            () => true,
            (error: unknown) => {
                restore(session.id);
                toast.close(toastId);
                addError({ messageKey: 'sessions.archiveFailed', detailKey: errorMessageKey(error) });

                return false;
            },
        );

        // Only once the archive is through: an `unarchive` overtaking it would leave the session archived.
        async function undo() {
            if (!await isArchived) {
                return;
            }

            try {
                await unarchive(session.id);
                restore(session.id);
            } catch (error) {
                addError({ messageKey: 'sessions.restoreFailed', detailKey: errorMessageKey(error) });
            }
        }
    }, [archive, t, toast, unarchive]);
}

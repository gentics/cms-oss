import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';

import { useToast } from '@/components/ui/use-toast';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { sessionTitle } from '@/helper/sessionDisplay/sessionDisplay';
import { useArchiveSession } from '@/hooks/useGenaixQueries';
import type { Session } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { useSessionArchiveStore } from '@/store/useSessionArchiveStore';

/**
 * Deleting a session: the row disappears at once and a toast offers "Undo". The session is
 * archived (`DELETE /sessions/{session_id}`) only when the toast closes without an undo, because
 * there is no way back out of `archived`. A failed archive shows the row again with an error
 * notification. Needs `UiProvider` around it.
 */
export function useDeleteSession() {
    const { t } = useTranslation();
    const toast = useToast();
    const { mutateAsync: archive } = useArchiveSession();

    return useCallback((session: Pick<Session, 'id' | 'title' | 'intent'>) => {
        const { hide, restore } = useSessionArchiveStore.getState();
        let isUndone = false;

        hide(session.id);

        const toastId = toast.add({
            title: t('sessions.deleted', { title: sessionTitle(session, t) }),
            actionProps: {
                children: t('sessions.undo'),
                onClick: () => {
                    isUndone = true;
                    restore(session.id);
                    toast.close(toastId);
                },
            },
            // The promise, not `mutate` callbacks: those are dropped once the list that showed the
            // row has unmounted, for example a closed drawer.
            onClose: () => {
                if (!isUndone) {
                    archive(session.id).catch((error: unknown) => {
                        restore(session.id);
                        useErrorNotificationStore.getState().addError({
                            messageKey: 'sessions.archiveFailed',
                            detailKey: errorMessageKey(error),
                        });
                    });
                }
            },
        });
    }, [archive, t, toast]);
}

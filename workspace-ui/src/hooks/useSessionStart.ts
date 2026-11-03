import { useNavigate } from '@tanstack/react-router';
import { useState } from 'react';

import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { type StartSessionInput, useStartSession } from '@/hooks/useGenaixQueries';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

/**
 * Starts a session from a start composer (the dashboard's, the one after publishing): sends the input
 * with its files, reports each file's upload progress for the composer's attachment list, and once the
 * session exists opens its page; a failure is an error notification.
 */
export function useSessionStart() {
    const navigate = useNavigate();
    const startSession = useStartSession();
    // Upload progress of the files sent, by their position, for the composer's attachment list.
    const [uploadProgress, setUploadProgress] = useState<number[]>([]);

    function reportUploadProgress(index: number, fraction: number) {
        setUploadProgress((current) => {
            const next = [...current];

            next[index] = fraction;

            return next;
        });
    }

    function start(input: StartSessionInput) {
        setUploadProgress([]);
        startSession.mutate({ ...input, onFileProgress: reportUploadProgress }, {
            onSuccess: (session) => {
                void navigate({ to: '/sessions/$id', params: { id: session.id } });
            },
            onError: (error) => {
                useErrorNotificationStore.getState().addError({ messageKey: 'dashboard.startFailed', detailKey: errorMessageKey(error) });
            },
        });
    }

    return { start, isStarting: startSession.isPending, uploadProgress };
}

import { useParams } from '@tanstack/react-router';
import { useRef, useState } from 'react';

import { AppShell } from '@/components/AppShell/AppShell';
import { Composer, type ComposerHandle } from '@/components/Composer/Composer';
import { HistoryColumn } from '@/components/HistoryColumn/HistoryColumn';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { type StartSessionInput, useSendTurn } from '@/hooks/useGenaixQueries';
import { useSessionEvents } from '@/hooks/useSessionEvents';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

import styles from './SessionPage.module.css';

/**
 * The chat composer of one session. Enter or send posts the turn (`useSendTurn`); on success the
 * field is emptied, on failure an error is shown and the input stays. `SessionPage` keys it by the
 * session, so going on to another session from the left column starts with an empty composer.
 */
function SessionComposer({ sessionId }: { sessionId: string }) {
    const composerRef = useRef<ComposerHandle>(null);
    const sendTurn = useSendTurn(sessionId);
    // Upload progress of the files sent, by their position, for the composer's attachment list.
    const [uploadProgress, setUploadProgress] = useState<number[]>([]);

    function reportUploadProgress(index: number, fraction: number) {
        setUploadProgress((current) => {
            const next = [...current];

            next[index] = fraction;

            return next;
        });
    }

    function handleSubmit(input: StartSessionInput) {
        setUploadProgress([]);
        // The promise, not `mutate` callbacks: those are dropped once the user has gone on to another
        // session, and a failed send would go unreported.
        sendTurn.mutateAsync({ ...input, onFileProgress: reportUploadProgress }).then(
            () => composerRef.current?.reset(),
            (error: unknown) => {
                useErrorNotificationStore.getState().addError({ messageKey: 'chat.sendFailed', detailKey: errorMessageKey(error) });
            },
        );
    }

    return (
        <Composer
            ref={composerRef}
            variant="chat"
            onSubmit={handleSubmit}
            isSubmitting={sendTurn.isPending}
            uploadProgress={uploadProgress}
            sessionId={sessionId}
        />
    );
}

/**
 * The page of the session routes (`/sessions/$id`, `/sessions/$id/review`): the workspace with the
 * session's chat composer at the bottom of the middle column. It follows the session's event stream,
 * so sent turns and replies land in `useWorkspaceEventStore`. The left column is the searchable
 * session history; showing the conversation and the preview (right) come later.
 */
export function SessionPage() {
    const { id } = useParams({ strict: false });

    // The chat view is the one consumer of the stream (other components read the store).
    useSessionEvents(id);

    // The composer floats over the bottom of the middle column, the column scrolls underneath (draft
    // `.composerwrap`). Both routes have an `id`; the composer that sends renders only with one.
    const chat = (
        <div className={styles.chat}>
            <div className={styles.stream} />
            {id && (
                <div className={styles.dock}>
                    <SessionComposer key={id} sessionId={id} />
                </div>
            )}
        </div>
    );

    // A session starts without a preview; the right column appears once GenAIx sends one
    // (`preview.updated`, not wired up yet).
    return <AppShell left={<HistoryColumn />} center={chat} right={null} isRightColumnVisible={false} />;
}

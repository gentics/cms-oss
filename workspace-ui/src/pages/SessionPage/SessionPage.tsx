import { useParams } from '@tanstack/react-router';
import { useRef } from 'react';

import { AppShell } from '@/components/AppShell/AppShell';
import { Composer, type ComposerHandle } from '@/components/Composer/Composer';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { type StartSessionInput, useSendTurn } from '@/hooks/useGenaixQueries';
import { useSessionEvents } from '@/hooks/useSessionEvents';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

import styles from './SessionPage.module.css';

/**
 * The page of the session routes (`/sessions/$id`, `/sessions/$id/review`): the workspace with the
 * chat composer at the bottom of the middle column. Enter or send posts the turn (`useSendTurn`); on
 * success the field is emptied, on failure an error is shown and the input stays. It follows the
 * session's event stream, so sent turns and replies land in `useWorkspaceEventStore`; showing the
 * conversation, the sessions list (left) and the preview (right) come later.
 */
export function SessionPage() {
    const { id } = useParams({ strict: false });
    const composerRef = useRef<ComposerHandle>(null);
    // Both routes have an `id`; the composer that sends renders only with one (below).
    const sendTurn = useSendTurn(id ?? '');

    // The chat view is the one consumer of the stream (other components read the store).
    useSessionEvents(id);

    function handleSubmit(input: StartSessionInput) {
        sendTurn.mutate(input, {
            onSuccess: () => composerRef.current?.reset(),
            onError: (error) => {
                useErrorNotificationStore.getState().addError({ messageKey: 'chat.sendFailed', detailKey: errorMessageKey(error) });
            },
        });
    }

    // The composer floats over the bottom of the middle column, the column scrolls underneath (draft
    // `.composerwrap`).
    const chat = (
        <div className={styles.chat}>
            <div className={styles.stream} />
            {id && (
                <div className={styles.dock}>
                    <Composer ref={composerRef} variant="chat" onSubmit={handleSubmit} isSubmitting={sendTurn.isPending} />
                </div>
            )}
        </div>
    );

    // A session starts without a preview; the right column appears once GenAIx sends one
    // (`preview.updated`, not wired up yet).
    return <AppShell left={null} center={chat} right={null} isRightColumnVisible={false} />;
}

import { useEffect, useState } from 'react';

import { GenaixApiError } from '@/services/apiService/apiService';
import type { Interaction } from '@/services/apiService/genaix/types';
import { type InteractionLock, useInteractionDraftStore } from '@/store/useInteractionDraftStore';

// The longest delay `setTimeout` keeps (about 24.8 days); a longer one would fire at once.
const MAX_TIMEOUT_MS = 2 ** 31 - 1;

// Whether `time` has passed, turning true at that moment. False for a missing or unreadable time.
function useHasPassed(time: string | undefined): boolean {
    const at = time ? Date.parse(time) : Number.NaN;
    const [now, setNow] = useState(() => Date.now());

    useEffect(() => {
        const remaining = at - now;

        if (Number.isNaN(remaining) || remaining <= 0 || remaining > MAX_TIMEOUT_MS) {
            return;
        }

        // From the real clock, not `now`: `at` may have changed since `now` was taken.
        const timer = setTimeout(() => setNow(Date.now()), Math.max(at - Date.now(), 0));

        return () => clearTimeout(timer);
    }, [at, now]);

    return now >= at;
}

/**
 * Why `interaction` can no longer be answered from here, if it cannot (`InteractionLock`): what the
 * card or the composer recorded, else `expired` once its `expires_at` has passed. GenAIx refuses an
 * answer after that with `410 interaction_expired`.
 */
export function useInteractionLock(interaction: Interaction | undefined): InteractionLock | undefined {
    const lock = useInteractionDraftStore((state) => (interaction ? state.locks[interaction.id] : undefined));
    const hasExpired = useHasPassed(interaction?.expires_at);

    return lock ?? (hasExpired ? 'expired' : undefined);
}

/**
 * Records why an answer to `interactionId` failed: `410 interaction_expired` locks it as `expired`,
 * `409 interaction_already_answered` as `answered`. Any other failure unlocks it, so it can be answered
 * again.
 */
export function lockAfterFailure(interactionId: string, error: unknown) {
    const store = useInteractionDraftStore.getState();
    const code = error instanceof GenaixApiError ? error.genaixCode : undefined;

    if (code === 'interaction_expired') {
        store.lock(interactionId, 'expired');
    } else if (code === 'interaction_already_answered') {
        store.lock(interactionId, 'answered');
    } else {
        store.unlock(interactionId);
    }
}

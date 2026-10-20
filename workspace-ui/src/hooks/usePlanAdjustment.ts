import { useState } from 'react';

import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { planMessage } from '@/helper/planMessage/planMessage';
import { isStalePlanCode, reloadPlans } from '@/helper/planReload/planReload';
import { invalidRowsOf, isChanged, normalized, type PlanRow, rowsOf, stepsOf } from '@/helper/planRows/planRows';
import { useSendMessage } from '@/hooks/useGenaixQueries';
import { GenaixApiError } from '@/services/apiService/apiService';
import type { Plan } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

export interface PlanAdjustment {
    /** The steps as the user adjusted them, while the plan is proposed; none for the plan as proposed. */
    adjusted?: PlanRow[];
    /** The steps while the user adjusts them; none while the plan is only shown. */
    draft?: PlanRow[];
    /** The adjusted rows the server refused, by key: the i18n key of what is wrong. */
    invalid?: Record<string, string>;
    /** Adjust and Accept plan are offered. */
    canAct: boolean;
    /** Done is possible: the draft keeps at least one step. */
    canFinish: boolean;
    /** The plan's run waits on a question of its own, which is answered in the chat. */
    hasQuestion: boolean;
    /** What to say after the plan's status: the adjustment, or that the server refused steps of it. */
    noteKey?: 'parts.plan.adjustedByYou' | 'parts.plan.invalid.caption';
    adjust: () => void;
    updateDraft: (update: (rows: PlanRow[]) => PlanRow[]) => void;
    finish: () => void;
    cancel: () => void;
    accept: () => void;
}

/**
 * Adjusting and accepting a proposed plan (draft `editPlan`), for the plan bar: its card shows the steps
 * and the editor (`PlanCard`), its head the buttons (`PlanAdjustActions`). While the plan is `proposed`,
 * no run works (`canAnswer`) and its run waits on no question, Adjust edits the steps and Accept plan
 * sends the plan to the agent (`planMessage`): as proposed, or as adjusted. Refused because the plan
 * moved on meanwhile (`409 plan_outdated`, `plan_closed`, `plan_not_proposed`), the plans are read again
 * (`reloadPlans`); refused for an item (`422` pointing at `/parts/<i>/plan/items/<j>`), that row is
 * marked `invalid` instead of an error notification.
 *
 * A revision proposes the same plan (same `id`) at a new version (rc.5), which drops what the user did
 * to the previous one (an adjustment, an acceptance), so it can be adjusted and accepted again.
 */
export function usePlanAdjustment(sessionId: string, plan: Plan, canAnswer: boolean): PlanAdjustment {
    const sendMessage = useSendMessage(sessionId);
    const items = plan.items ?? [];
    const [adjustedRows, setAdjusted] = useState<PlanRow[]>();
    const [draftRows, setDraft] = useState<PlanRow[]>();
    const [isAccepted, setIsAccepted] = useState(false);
    // The rows the server refused, by key, until the user adjusts the plan again.
    const [invalidRows, setInvalid] = useState<Record<string, string>>();
    // The version the user's adjustment and acceptance are for.
    const [ownVersion, setOwnVersion] = useState(plan.version);
    const hasQuestion = useWorkspaceEventStore((state) => {
        const session = selectSession(sessionId)(state);

        return (session.interactions ?? []).some(({ id }) => session.interactionPlans?.[id] === plan.id);
    });

    if (ownVersion !== plan.version) {
        setOwnVersion(plan.version);
        setAdjusted(undefined);
        setDraft(undefined);
        setIsAccepted(false);
        setInvalid(undefined);
    }

    // Only a proposed plan can be accepted (contract: `409 plan_not_proposed` otherwise), and not while its
    // run waits on a question (`run_already_active`).
    const isProposed = plan.status === 'proposed';
    const canAct = isProposed && canAnswer && !isAccepted && !hasQuestion;
    // The user's adjustment counts only while the plan waits for them: once it is accepted, the card
    // shows the plan as GenAIx executes it.
    const adjusted = isProposed ? adjustedRows : undefined;
    const invalid = adjusted ? invalidRows : undefined;
    const draft = canAct ? draftRows : undefined;
    const steps = stepsOf(draft ?? []);
    let noteKey: PlanAdjustment['noteKey'];

    if (canAct && !draft && invalid) {
        noteKey = 'parts.plan.invalid.caption';
    } else if (canAct && !draft && adjusted) {
        noteKey = 'parts.plan.adjustedByYou';
    }

    function finish() {
        if (steps.length > 0 && draft) {
            setAdjusted(isChanged(steps, items) ? normalized(draft) : undefined);
            setDraft(undefined);
            setInvalid(undefined);
        }
    }

    function accept() {
        const sent = adjusted && stepsOf(adjusted);

        setIsAccepted(true);
        sendMessage.mutateAsync(planMessage(plan, sent)).catch((error: unknown) => {
            setIsAccepted(false);

            if (error instanceof GenaixApiError && isStalePlanCode(error.genaixCode)) {
                reloadPlans(sessionId).catch((reloadError: unknown) => console.warn('Reading the plans again failed', reloadError));
            }

            const refused = error instanceof GenaixApiError && error.status === 422 && sent ? invalidRowsOf(error.problem, sent) : {};

            if (Object.keys(refused).length > 0) {
                setInvalid(refused);

                return;
            }

            useErrorNotificationStore.getState().addError({ messageKey: 'chat.sendFailed', detailKey: errorMessageKey(error) });
        });
    }

    return {
        adjusted,
        draft,
        invalid,
        canAct,
        canFinish: steps.length > 0,
        hasQuestion,
        noteKey,
        adjust: () => setDraft(adjusted ?? rowsOf(items)),
        updateDraft: (update) => setDraft((current) => current && update(current)),
        finish,
        cancel: () => setDraft(undefined),
        accept,
    };
}

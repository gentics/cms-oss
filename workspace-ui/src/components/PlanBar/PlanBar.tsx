import { ChevronDownIcon, ChevronUpIcon } from 'lucide-react';
import { useId, useLayoutEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { PlanCard } from '@/components/PlanCard/PlanCard';
import { PlanGuidelines } from '@/components/PlanGuidelines/PlanGuidelines';
import { PlanSwitcher } from '@/components/PlanSwitcher/PlanSwitcher';
import { Button } from '@/components/ui/button';
import { isAcceptedOnCard } from '@/helper/planActions/planActions';
import { useWorkflows } from '@/hooks/useGenaixQueries';
import { usePlanAdjustment } from '@/hooks/usePlanAdjustment';
import { usePlanPresence } from '@/hooks/usePlanPresence';
import type { Plan } from '@/services/apiService/genaix/types';
import { isSessionWorking, selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import { PlanAdjustActions } from './PlanAdjustActions';
import { PlanHead } from './PlanHead';
import { ReviewButton } from './ReviewButton';

import styles from './PlanBar.module.css';

interface PlanBarProps {
    sessionId: string;
    /** The session's open plan (`useOpenPlan`). */
    plan: Plan;
    /** Every open plan of the session (`useOpenPlans`); with more than one, the bar offers to show another (`PlanSwitcher`). */
    openPlans?: Plan[];
    /** Shows the plan picked from `openPlans` in the bar. */
    onPickPlan?: (planId: string) => void;
    /** The bar's height whenever it changes, for the chat to keep the room free above the composer. */
    onHeightChange?: (height: number) => void;
}

/**
 * The open plan of the session, right above the chat composer (design.md §11; draft `.relbar`), the only
 * place a plan is shown: the other plans are not in the conversation. Its head (`PlanHead`) always shows where the
 * plan is, the publish outcome of its pages and the transitions its status allows.
 *
 * Expanded, the plan itself shows above the head (`PlanCard`): its steps, below the session's guidelines
 * (`PlanGuidelines`), to which the user adds while the plan is proposed. While it is proposed, Adjust
 * and Accept plan are in the head with its other actions (`PlanAdjustActions`, `usePlanAdjustment`),
 * also while the bar is collapsed; Adjust expands it, so that the steps are adjusted in view.
 *
 * It expands by itself while the plan is proposed and waits for the user's acceptance on it (a module
 * with `plan_approval`, `isAcceptedOnCard`), and collapses once that is done. A plan its run
 * confirms with a question in the chat (`plan_approval: false`) does not expand it. A question of its
 * own (`interaction.requested` with its `plan_id`, e.g. the settings review of a revision) does not
 * expand it: it is answered in the chat. A click on the toggle holds until the plan's waiting changes.
 *
 * A plan not accepted on its card (`planPresence` `head`) is its head alone, with no steps and nothing
 * to expand: Stop while it works, and "Review & release" once it is done. One with nothing left to do
 * is not shown at all (`none`, `SessionPage`).
 *
 * While more than one plan of the session is open (rc.5), "2 open plans" next to the head offers the
 * others (`PlanSwitcher`); picking one shows it here. With one open plan the bar is as it always was.
 *
 * Once the plan is done and has written a page, "Review & release" next to the toggle opens the
 * review page (`ReviewButton` decides), where the plan's preview, checks and pages are shown together, and releases a done
 * plan on the way (`ReviewButton`); publish and request review are made there, not here (`PlanHead`
 * without `withGatedActions`).
 */
export function PlanBar({ sessionId, plan, openPlans = [], onPickPlan, onHeightChange }: PlanBarProps) {
    const { t } = useTranslation();
    const panelId = useId();
    const wrapRef = useRef<HTMLDivElement>(null);
    const isWorking = useWorkspaceEventStore((state) => isSessionWorking(selectSession(sessionId)(state)));
    const adjustment = usePlanAdjustment(sessionId, plan, !isWorking);
    // The user's own toggle, and whether the plan waited for them when they clicked it.
    const [toggled, setToggled] = useState<{ waits: boolean; isExpanded: boolean }>();
    const workflows = useWorkflows().data;
    // A question of the plan's run is answered in the chat, where its card is, and so is the one
    // confirmation of a plan not accepted on its card: the bar stays as it is for both.
    const waits = plan.status === 'proposed' && !adjustment.hasQuestion && isAcceptedOnCard(plan, workflows);
    const isExpanded = toggled?.waits === waits ? toggled.isExpanded : waits;
    // Its head alone, for a plan confirmed in the chat or needing no acceptance.
    const isHeadOnly = usePlanPresence(sessionId, plan) === 'head';

    useLayoutEffect(() => {
        const wrap = wrapRef.current;

        if (!wrap || !onHeightChange) {
            return;
        }

        onHeightChange(wrap.offsetHeight);

        const observer = new ResizeObserver(() => onHeightChange(wrap.offsetHeight));

        observer.observe(wrap);

        return () => observer.disconnect();
    }, [onHeightChange]);

    const ToggleIcon = isExpanded ? ChevronDownIcon : ChevronUpIcon;

    return (
        <div ref={wrapRef} className={styles.wrap}>
            <section className={styles.bar} aria-label={t('planBar.label')}>
                {!isHeadOnly && (
                    <div id={panelId} className={styles.panel} hidden={!isExpanded}>
                        <PlanGuidelines plan={plan} sessionId={sessionId} />
                        <PlanCard plan={plan} sessionId={sessionId} adjustment={adjustment} />
                    </div>
                )}
                <PlanHead sessionId={sessionId} plan={plan} note={!isHeadOnly && adjustment.noteKey ? t(adjustment.noteKey) : undefined}>
                    {!isHeadOnly && <PlanAdjustActions adjustment={adjustment} onAdjust={() => setToggled({ waits, isExpanded: true })} />}
                    <ReviewButton sessionId={sessionId} plan={plan} />
                    {onPickPlan && <PlanSwitcher plans={openPlans} currentId={plan.id} onPick={onPickPlan} />}
                    {!isHeadOnly && (
                        <Button
                            variant="ghost"
                            size="icon"
                            aria-expanded={isExpanded}
                            aria-controls={panelId}
                            aria-label={t(isExpanded ? 'planBar.collapse' : 'planBar.expand')}
                            onClick={() => setToggled({ waits, isExpanded: !isExpanded })}
                        >
                            <ToggleIcon size={16} />
                        </Button>
                    )}
                </PlanHead>
            </section>
        </div>
    );
}

import { Link } from '@tanstack/react-router';
import { ChevronDownIcon, ChevronUpIcon, ClipboardCheckIcon } from 'lucide-react';
import { useId, useLayoutEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { PlanCard } from '@/components/PlanCard/PlanCard';
import { Button } from '@/components/ui/button';
import type { Plan, PlanStatus } from '@/services/apiService/genaix/types';
import { isSessionWorking, selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import { PlanHead } from './PlanHead';

import styles from './PlanBar.module.css';

// The statuses with something to review on the review page: a done plan to release, a released one to
// publish, the outcome of one sent out.
const REVIEWABLE: ReadonlySet<PlanStatus> = new Set(['done', 'released', 'published', 'review_requested']);

interface PlanBarProps {
    sessionId: string;
    /** The session's open plan (`useOpenPlan`). */
    plan: Plan;
    /** The bar's height whenever it changes, for the chat to keep the room free above the composer. */
    onHeightChange?: (height: number) => void;
}

/**
 * The open plan of the session, right above the chat composer (design.md §11; draft `.relbar`); the
 * other plans stay in the conversation (`ChatPlanCard`). Its head (`PlanHead`) always shows where the
 * plan is, the publish outcome of its pages and the transitions its status allows.
 *
 * Expanded, the plan itself shows above the head (`PlanCard`): its steps, and Adjust and Accept while it
 * is proposed. It expands by itself while the plan waits for the user, proposed or with a question of
 * its own open (`interaction.requested` with its `plan_id`), and collapses once that is done. A click
 * on the toggle holds until the plan's waiting changes.
 *
 * While the plan has something to review (`REVIEWABLE`), "Review & release" next to the toggle opens
 * the review page, where the plan's preview, checks and pages are shown together.
 */
export function PlanBar({ sessionId, plan, onHeightChange }: PlanBarProps) {
    const { t } = useTranslation();
    const panelId = useId();
    const wrapRef = useRef<HTMLDivElement>(null);
    const isWorking = useWorkspaceEventStore((state) => isSessionWorking(selectSession(sessionId)(state)));
    const hasOwnQuestion = useWorkspaceEventStore((state) => {
        const session = selectSession(sessionId)(state);

        return (session.interactions ?? []).some((interaction) => session.interactionPlans?.[interaction.id] === plan.id);
    });
    // The user's own toggle, and whether the plan waited for them when they clicked it.
    const [toggled, setToggled] = useState<{ waits: boolean; isExpanded: boolean }>();
    const waits = plan.status === 'proposed' || hasOwnQuestion;
    const isExpanded = toggled?.waits === waits ? toggled.isExpanded : waits;

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
                <div id={panelId} className={styles.panel} hidden={!isExpanded}>
                    <PlanCard plan={plan} sessionId={sessionId} canAnswer={!isWorking} />
                </div>
                <PlanHead sessionId={sessionId} plan={plan}>
                    {REVIEWABLE.has(plan.status) && (
                        <Button variant="secondary" size="sm" nativeButton={false} role={undefined} render={<Link to="/sessions/$id/review" params={{ id: sessionId }} />}>
                            <ClipboardCheckIcon size={14} aria-hidden />
                            {t('planBar.review')}
                        </Button>
                    )}
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
                </PlanHead>
            </section>
        </div>
    );
}

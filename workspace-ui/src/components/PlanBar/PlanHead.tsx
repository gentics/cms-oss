import { CircleAlertIcon, CircleCheckIcon, EyeIcon, GlobeIcon, type LucideIcon, RocketIcon, RouteIcon, Undo2Icon, XIcon } from 'lucide-react';
import { type ReactNode, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { useTransitionPlan } from '@/hooks/useGenaixQueries';
import { GenaixApiError } from '@/services/apiService/apiService';
import type { Plan, PlanStatus, Problem, Publication, TransitionRequest } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import { GateRefusalDialog } from './GateRefusalDialog';

import styles from './PlanBar.module.css';

type Action = TransitionRequest['action'];

// What the user can do with the plan in each status (09-integration-guide.md, "The plan card"), the
// main action last. A proposed plan is accepted from the plan itself, with a message (`planMessage`).
const ACTIONS: Partial<Record<PlanStatus, Action[]>> = {
    proposed: ['discard'],
    accepted: ['release'],
    running: ['release'],
    done: ['discard', 'release'],
    released: ['discard', 'reopen', 'request_review', 'publish'],
    published: ['reopen'],
    review_requested: ['reopen'],
};

const ACTION_ICONS: Record<Action, LucideIcon> = {
    release: RocketIcon,
    publish: GlobeIcon,
    request_review: EyeIcon,
    reopen: Undo2Icon,
    discard: XIcon,
};

const PRIMARY_ACTIONS = new Set<Action>(['release', 'publish']);

type PublicationStatus = Publication['status'];

// The order the page outcome is told in: what went out first, what is left last.
const PUBLICATION_STATUSES: PublicationStatus[] = ['published', 'review_requested', 'failed', 'pending'];

// How many of the plan's pages are in each publish status. A status outside the contract's enum counts
// as pending (client rule: tolerate unknown enum values).
function countByStatus(publications: Publication[]): Partial<Record<PublicationStatus, number>> {
    const counts: Partial<Record<PublicationStatus, number>> = {};

    for (const { status } of publications) {
        const known = PUBLICATION_STATUSES.includes(status) ? status : 'pending';

        counts[known] = (counts[known] ?? 0) + 1;
    }

    return counts;
}

interface PlanHeadProps {
    sessionId: string;
    plan: Plan;
    /** In a chat card's foot (`ChatPlanCard`) rather than the plan bar: no padding of its own. */
    isInCard?: boolean;
    /** Controls after the actions, such as the plan bar's toggle. */
    children?: ReactNode;
}

/**
 * Where one plan is: its status, the publish outcome of the pages it wrote (`publications.updated`,
 * "2 published, 1 in review") and the transitions its status allows (`ACTIONS`, POST
 * …/plans/{plan_id}/transitions): release also while the plan's run works, which stops it once the
 * gates passed, the others only while no run is active. The new status arrives as `plan.updated`. A
 * transition the quality gates refuse opens `GateRefusalDialog`. Stopping a plan's run is the
 * composer's (`SessionComposer`).
 */
export function PlanHead({ sessionId, plan, isInCard = false, children }: PlanHeadProps) {
    const { t } = useTranslation();
    const transitionPlan = useTransitionPlan(sessionId);
    const publications = useWorkspaceEventStore((state) => selectSession(sessionId)(state).publications?.[plan.id]);
    const hasRun = useWorkspaceEventStore((state) => Boolean(selectSession(sessionId)(state).run));
    // The refused transition, while its dialog is open.
    const [refusal, setRefusal] = useState<{ action: Action; problem: Problem }>();

    function transition(action: Action, acknowledgeChecks?: string[]) {
        const request: TransitionRequest = { action, ...(acknowledgeChecks && { acknowledge_checks: acknowledgeChecks }) };

        transitionPlan.mutateAsync({ planId: plan.id, request }).then(
            () => setRefusal(undefined),
            (error: unknown) => {
                if (error instanceof GenaixApiError && error.genaixCode === 'quality_gate_failed' && error.problem) {
                    setRefusal({ action, problem: error.problem });

                    return;
                }

                setRefusal(undefined);
                useErrorNotificationStore.getState().addError({ messageKey: 'planBar.transitionFailed', detailKey: errorMessageKey(error) });
            },
        );
    }

    const counts = countByStatus(publications ?? []);
    const summary = PUBLICATION_STATUSES
        .filter((status) => counts[status])
        .map((status) => t(`planBar.count.${status}`, { count: counts[status] }))
        .join(', ');
    const isUnderWay = plan.status === 'proposed' || plan.status === 'accepted' || plan.status === 'running';
    const isClosed = plan.status === 'discarded' || plan.status === 'superseded';
    const tone = counts.failed ? styles.bad : isUnderWay || isClosed ? undefined : styles.ok;
    const Icon = counts.failed ? CircleAlertIcon : isUnderWay || isClosed ? RouteIcon : CircleCheckIcon;

    return (
        <div className={[styles.head, isInCard && styles.inCard, tone].filter(Boolean).join(' ')}>
            <Icon size={16} className={styles.icon} aria-hidden />
            <span className={styles.text}>
                <span className={styles.status}>{t(`parts.plan.status.${plan.status}`)}</span>
                {summary && ` · ${summary}`}
            </span>
            <span className={styles.actions}>
                {(ACTIONS[plan.status] ?? []).map((action) => {
                    const ActionIcon = ACTION_ICONS[action];

                    return (
                        <Button
                            key={action}
                            variant={PRIMARY_ACTIONS.has(action) ? 'primary' : 'secondary'}
                            size="sm"
                            disabled={transitionPlan.isPending || (hasRun && action !== 'release')}
                            onClick={() => transition(action)}
                        >
                            <ActionIcon size={14} />
                            {t(`planBar.actions.${action}`)}
                        </Button>
                    );
                })}
                {children}
            </span>
            {refusal && (
                <GateRefusalDialog
                    action={refusal.action}
                    problem={refusal.problem}
                    isPending={transitionPlan.isPending}
                    onAcknowledge={(checkIds) => transition(refusal.action, checkIds)}
                    onClose={() => setRefusal(undefined)}
                />
            )}
        </div>
    );
}

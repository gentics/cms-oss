import { CircleAlertIcon, CircleCheckIcon, EyeIcon, GlobeIcon, type LucideIcon, RefreshCwIcon, RocketIcon, RouteIcon, TriangleAlertIcon, Undo2Icon, XIcon } from 'lucide-react';
import { type ReactNode, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { Dialog, DialogBody, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { planActions } from '@/helper/planActions/planActions';
import { gateRefusalOf, usePlanTransition, useTransitionPlan, useWorkflows } from '@/hooks/useGenaixQueries';
import type { Plan, Publication, TransitionRequest } from '@/services/apiService/genaix/types';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import styles from './PlanBar.module.css';

type Action = TransitionRequest['action'];

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
    /**
     * On the review page's panel: also offers the gated transitions (`GATED_ACTIONS` in `planActions`), and what
     * to do about a transition the gates refused. Elsewhere only discard and reopen.
     */
    withGatedActions?: boolean;
    /** Controls after the actions, such as the plan bar's toggle. */
    children?: ReactNode;
}

/**
 * Where one plan is: its status, the publish outcome of the pages it wrote (`publications.updated`,
 * "2 published, 1 in review") and the transitions its status allows (`ACTIONS`, POST
 * …/plans/{plan_id}/transitions): release also while the plan's run works, which stops it once the
 * gates passed, the others only while no run is active, and none while a transition of the plan is on
 * its way, wherever it was sent (`usePlanTransition`). The new status arrives as `plan.updated`.
 *
 * With `withGatedActions`, when the quality gates refused the plan's latest transition
 * (`409 quality_gate_failed`), its button gives way to what can be done about it: "Check again", which
 * repeats it so the gates run again, and with warnings only (`failing_gates` empty) "Release anyway"
 * (or publish, request review), which repeats it with `acknowledgeable_checks` as `acknowledge_checks`
 * (contract, `TransitionRequest.acknowledge_checks`). A blocking check cannot be acknowledged, so then
 * only "Check again". A line above the head says which of the two it is; the failing checks themselves
 * are in the checklist above (`ReviewPanel`), so they are not listed again. This holds until another
 * transition is sent or the plan no longer offers that one.
 *
 * The gated transitions
 * (release, publish, request review) only with `withGatedActions`, on the review page, so that a user
 * releases or publishes there, with the checks in view. Discard asks first, saying what
 * happens to the pages by the module's `plan_policy.on_discard`. Stopping a plan's run is the
 * composer's (`SessionComposer`).
 */
export function PlanHead({ sessionId, plan, isInCard = false, withGatedActions = false, children }: PlanHeadProps) {
    const { t } = useTranslation();
    const transitionPlan = useTransitionPlan(sessionId);
    const { pending, latest } = usePlanTransition(sessionId, plan.id);
    const publications = useWorkspaceEventStore((state) => selectSession(sessionId)(state).publications?.[plan.id]);
    const hasRun = useWorkspaceEventStore((state) => Boolean(selectSession(sessionId)(state).run));
    const [isDiscardOpen, setIsDiscardOpen] = useState(false);
    const onDiscard = useWorkflows().data?.find(({ id }) => id === plan.workflow)?.plan_policy?.on_discard ?? 'keep';

    // `isCheck` for "Check again", which has the gates check the plan rather than release it at the user's word.
    function transition(action: Action, { acknowledgeChecks, isCheck }: { acknowledgeChecks?: string[]; isCheck?: boolean } = {}) {
        if (pending) {
            return;
        }

        const request: TransitionRequest = { action, ...(acknowledgeChecks && { acknowledge_checks: acknowledgeChecks }) };

        // A refusal or any other failure is handled where every transition is (`useTransitionPlan`).
        transitionPlan.mutate({ planId: plan.id, request, isCheck });
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
    const actions = planActions(plan.status, withGatedActions);
    const refusedAction = latest?.input.request.action;
    const refusal = withGatedActions && latest?.status === 'error' && refusedAction && actions.includes(refusedAction)
        ? gateRefusalOf(latest.error)
        : undefined;
    const acknowledgeable = refusal?.acknowledgeable_checks ?? [];
    const canAcknowledge = Boolean(refusal) && (refusal?.failing_gates ?? []).length === 0 && acknowledgeable.length > 0;

    // The buttons of one action: as offered, or for the refused one what can be done about it.
    function actionButtons(action: Action) {
        const ActionIcon = ACTION_ICONS[action];

        if (refusal && action === refusedAction) {
            return [
                <Button key={`${action}-retry`} variant={canAcknowledge ? 'secondary' : 'primary'} size="sm" disabled={Boolean(pending)} onClick={() => transition(action, { isCheck: true })}>
                    <RefreshCwIcon size={14} />
                    {t('planBar.gate.retry')}
                </Button>,
                canAcknowledge && (
                    <Button key={`${action}-anyway`} variant="primary" size="sm" disabled={Boolean(pending)} onClick={() => transition(action, { acknowledgeChecks: acknowledgeable })}>
                        <ActionIcon size={14} />
                        {t(`planBar.gate.anyway.${action}`)}
                    </Button>
                ),
            ];
        }

        return [
            <Button
                key={action}
                variant={PRIMARY_ACTIONS.has(action) ? 'primary' : 'secondary'}
                size="sm"
                disabled={Boolean(pending) || (hasRun && action !== 'release')}
                onClick={() => (action === 'discard' ? setIsDiscardOpen(true) : transition(action))}
            >
                <ActionIcon size={14} />
                {t(`planBar.actions.${action}`)}
            </Button>,
        ];
    }

    const RefusalIcon = canAcknowledge ? TriangleAlertIcon : CircleAlertIcon;

    return (
        <>
            {refusal && (
                <p className={styles.refusal} data-kind={canAcknowledge ? 'warning' : 'blocking'} role="status">
                    <RefusalIcon size={14} aria-hidden className={styles.refusalIcon} />
                    {t(canAcknowledge ? 'planBar.gate.acknowledgeHint' : 'planBar.gate.blockingHint')}
                </p>
            )}
            <div className={[styles.head, isInCard && styles.inCard, tone].filter(Boolean).join(' ')}>
                <Icon size={16} className={styles.icon} aria-hidden />
                <span className={styles.text}>
                    <span className={styles.status}>{t(`parts.plan.status.${plan.status}`)}</span>
                    {summary && ` · ${summary}`}
                </span>
                <span className={styles.actions}>
                    {actions.flatMap(actionButtons)}
                    {children}
                </span>
                <Dialog open={isDiscardOpen} onOpenChange={setIsDiscardOpen}>
                    <DialogContent>
                        <DialogHeader>
                            <DialogTitle>{t('planBar.discard.title')}</DialogTitle>
                        </DialogHeader>
                        <DialogBody>
                            <DialogDescription>{t(`planBar.discard.${onDiscard}`)}</DialogDescription>
                        </DialogBody>
                        <DialogFooter>
                            <Button variant="secondary" size="sm" onClick={() => setIsDiscardOpen(false)}>{t('planBar.discard.cancel')}</Button>
                            <Button
                                variant="danger"
                                size="sm"
                                disabled={Boolean(pending)}
                                onClick={() => {
                                    setIsDiscardOpen(false);
                                    transition('discard');
                                }}
                            >
                                <XIcon size={14} />
                                {t('planBar.actions.discard')}
                            </Button>
                        </DialogFooter>
                    </DialogContent>
                </Dialog>
            </div>
        </>
    );
}

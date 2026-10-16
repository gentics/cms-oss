import { CircleAlertIcon, CircleCheckIcon, EyeIcon, GlobeIcon, type LucideIcon, RefreshCwIcon, RocketIcon, RouteIcon, SquareIcon, TriangleAlertIcon, XIcon } from 'lucide-react';
import { type ReactNode, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { Dialog, DialogBody, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { errorMessageKey, problemMessageKey } from '@/helper/errorMapper/errorMapper';
import { isFirstDraft, planActions } from '@/helper/planActions/planActions';
import { glanceText, isWorthAGlance, planGlance, showsProgress } from '@/helper/planGlance/planGlance';
import { gateRefusalOf, useCancelRun, usePlanTransition, useTransitionPlan, useWorkflows } from '@/hooks/useGenaixQueries';
import type { Plan, Publication, TransitionRequest, Workflow } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import { PublishDialog, type PublishPhase } from './PublishDialog';

import styles from './PlanBar.module.css';

type Action = TransitionRequest['action'];

const ACTION_ICONS: Record<Action, LucideIcon> = {
    release: RocketIcon,
    publish: GlobeIcon,
    request_review: EyeIcon,
    discard: XIcon,
};

const PRIMARY_ACTIONS = new Set<Action>(['release', 'publish']);

type PublicationStatus = Publication['status'];

// The order the page outcome is told in: what went out first, what is left last.
const PUBLICATION_STATUSES: PublicationStatus[] = ['published', 'review_requested', 'failed', 'pending'];

// What a transition is sent with besides its action: acknowledged warnings ("… anyway"), or `isCheck` ("Check again").
interface TransitionOptions {
    acknowledgeChecks?: string[];
    isCheck?: boolean;
}

// Publishing as `PlanHead` drives it: asked (with what confirming sends), sent (with the run that carries
// it out, once the `202` came), or refused. Whether it ended is read from the plan's status and the
// run's end (`publishState`).
type Publishing = { phase: 'confirm'; options: TransitionOptions } | { phase: 'publishing'; runId?: string } | { phase: 'failed'; failureKey: string };

// How many of the plan's pages are in each publish status. A status outside the contract's enum counts
// as pending (client rule: tolerate unknown enum values).
// What discarding does to the pages the plan wrote: the strictest `plan_policy.on_discard` of the plan's
// modules, `restore` over `keep` (rc.5); `keep` while the modules are not known.
function onDiscardOf(plan: Plan, workflows: Workflow[] | undefined): 'keep' | 'restore' {
    const policies = (plan.workflows ?? []).map(({ id }) => workflows?.find((workflow) => workflow.id === id)?.plan_policy?.on_discard);

    return policies.includes('restore') ? 'restore' : 'keep';
}

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
    /** In a panel (`ReviewPanel`) rather than the plan bar: no padding of its own. */
    isInCard?: boolean;
    /**
     * On the review page's panel: also offers the gated transitions (`GATED_ACTIONS` in `planActions`), and what
     * to do about a transition the gates refused. Elsewhere only discard and Stop.
     */
    withGatedActions?: boolean;
    /** Whether the user may publish the plan's pages in the CMS; without it, publish is not offered (request review is). */
    canPublish?: boolean;
    /** The plan at a glance after its status, in one line ("+3 folders · 2 pages edited"), while the plan itself is not shown. */
    withGlance?: boolean;
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
 * releases or publishes there, with the checks in view; publish only with `canPublish`. Every button
 * that can put pages online asks first in a dialog (`PublishDialog`), which then follows publishing to
 * its end: Publish, and after a refused publish "Publish anyway" and "Check again" (which repeats the
 * publish, as the gates only run with the transition). Discard asks first, saying what
 * happens to the pages by the modules' `plan_policy.on_discard` (`onDiscardOf`).
 *
 * Outside the review page, while a run of the plan works (proposing, executing, publishing), Stop cancels
 * it (`POST …/runs/{run_id}/cancel`); the composer stops only the session agent's runs and side runs
 * (`SessionComposer`), so there is one Stop per run.
 *
 * A plan is changed with a message, not a transition (rc.5 removed `reopen`): GenAIx's session agent
 * decides whether a message revises a plan, and while one executes its router whether it redirects it
 * (contract §4.4, §4.5). A revision being drafted shows as such: `drafting`, but with the items of its
 * current version.
 *
 * With `withGlance` the plan at a glance follows the status (`planGlance`), as the plan bar shows it
 * while collapsed, but only while the plan is under way (drafting to running): from `done` on, the head
 * says only the status and the publish outcome of its pages.
 */
export function PlanHead({ sessionId, plan, isInCard = false, withGatedActions = false, canPublish = true, withGlance = false, children }: PlanHeadProps) {
    const { t } = useTranslation();
    const transitionPlan = useTransitionPlan(sessionId);
    const cancelRun = useCancelRun(sessionId);
    const { pending, latest } = usePlanTransition(sessionId, plan.id);
    const publications = useWorkspaceEventStore((state) => selectSession(sessionId)(state).publications?.[plan.id]);
    const hasRun = useWorkspaceEventStore((state) => Boolean(selectSession(sessionId)(state).run));
    // The plan's own run in progress, which Stop cancels.
    const planRun = useWorkspaceEventStore((state) => selectSession(sessionId)(state).runs?.findLast((live) => live.planId === plan.id));
    const [isDiscardOpen, setIsDiscardOpen] = useState(false);
    // The publish dialog, while it is open.
    const [publishing, setPublishing] = useState<Publishing>();
    const notices = useWorkspaceEventStore((state) => selectSession(sessionId)(state).notices);
    const onDiscard = onDiscardOf(plan, useWorkflows().data);

    // `isCheck` for "Check again", which has the gates check the plan rather than release it at the user's word.
    function transition(action: Action, { acknowledgeChecks, isCheck }: TransitionOptions = {}) {
        if (pending) {
            return;
        }

        const request: TransitionRequest = { action, ...(acknowledgeChecks && { acknowledge_checks: acknowledgeChecks }) };

        if (action !== 'publish') {
            // A refusal or any other failure is handled where every transition is (`useTransitionPlan`).
            transitionPlan.mutate({ planId: plan.id, request, isCheck });

            return;
        }

        // Publish, confirmed in its dialog (`ask`): the dialog follows it. A refusal by the gates closes it
        // and shows here like any other.
        setPublishing({ phase: 'publishing' });
        transitionPlan.mutate({ planId: plan.id, request, isCheck }, {
            onSuccess: (result) => setPublishing({ phase: 'publishing', ...('run_id' in result && { runId: result.run_id }) }),
            onError: (error) => setPublishing(gateRefusalOf(error) ? undefined : { phase: 'failed', failureKey: errorMessageKey(error) }),
        });
    }

    // `action` as its button sends it: a publish asks first in the dialog, which sends it once confirmed.
    function ask(action: Action, options: TransitionOptions = {}) {
        if (action === 'publish') {
            setPublishing({ phase: 'confirm', options });
        } else {
            transition(action, options);
        }
    }

    // Where the publishing the dialog follows stands: the plan's status says it went out (`published`,
    // or `review_requested` once a page went to review); the run that carries it out ending as failed or
    // cancelled says it did not (the plan stays `released`, contract).
    function publishState(current: Publishing): { phase: PublishPhase; failureKey?: string } {
        if (current.phase !== 'publishing') {
            return current;
        }

        if (plan.status === 'published' || plan.status === 'review_requested') {
            return { phase: plan.status === 'published' ? 'published' : 'done' };
        }

        const ended = current.runId === undefined ? undefined : notices?.find(({ runId, kind }) => runId === current.runId && (kind === 'failed' || kind === 'cancelled'));

        if (ended?.kind === 'failed') {
            return { phase: 'failed', failureKey: problemMessageKey(ended.error) };
        }

        return ended ? { phase: 'failed', failureKey: 'planBar.publishDialog.cancelled' } : current;
    }

    const counts = countByStatus(publications ?? []);
    const summary = PUBLICATION_STATUSES
        .filter((status) => counts[status])
        .map((status) => t(`planBar.count.${status}`, { count: counts[status] }))
        .join(', ');
    const isUnderWay = plan.status === 'drafting' || plan.status === 'proposed' || plan.status === 'accepted' || plan.status === 'running';
    const isClosed = plan.status === 'discarded' || plan.status === 'superseded';
    const tone = counts.failed ? styles.bad : isUnderWay || isClosed ? undefined : styles.ok;
    const Icon = counts.failed ? CircleAlertIcon : isUnderWay || isClosed ? RouteIcon : CircleCheckIcon;
    const actions = planActions(plan.status, withGatedActions).filter((action) => canPublish || action !== 'publish');
    // A revision is drafted with the items of the version it revises (rc.5); a first draft has none.
    const statusKey = plan.status === 'drafting' && !isFirstDraft(plan) ? 'revising' : plan.status;
    const publish = publishing && publishState(publishing);
    const refusedAction = latest?.input.request.action;
    const refusal = withGatedActions && latest?.status === 'error' && refusedAction && actions.includes(refusedAction)
        ? gateRefusalOf(latest.error)
        : undefined;
    const acknowledgeable = refusal?.acknowledgeable_checks ?? [];
    const canAcknowledge = Boolean(refusal) && (refusal?.failing_gates ?? []).length === 0 && acknowledgeable.length > 0;
    // Only while the plan is under way, and for more than one item; once done, the status and the pages
    // to publish say enough.
    const glanceEntries = withGlance && isUnderWay ? planGlance(plan.items ?? []) : [];
    const glance = isWorthAGlance(glanceEntries)
        ? glanceEntries.map((entry) => {
            const { value, label } = glanceText(t, entry, showsProgress(plan.status));

            return `${value} ${label}`;
        }).join(' · ')
        : '';

    function stop() {
        if (!planRun) {
            return;
        }

        cancelRun.mutate(planRun.id, {
            onError: (error) => {
                useErrorNotificationStore.getState().addError({ messageKey: 'chat.stopFailed', detailKey: errorMessageKey(error) });
            },
        });
    }

    // The buttons of one action: as offered, or for the refused one what can be done about it.
    function actionButtons(action: Action) {
        const ActionIcon = ACTION_ICONS[action];

        if (refusal && action === refusedAction) {
            return [
                <Button key={`${action}-retry`} variant={canAcknowledge ? 'secondary' : 'primary'} size="sm" disabled={Boolean(pending)} onClick={() => ask(action, { isCheck: true })}>
                    <RefreshCwIcon size={14} />
                    {t('planBar.gate.retry')}
                </Button>,
                canAcknowledge && (
                    <Button key={`${action}-anyway`} variant="primary" size="sm" disabled={Boolean(pending)} onClick={() => ask(action, { acknowledgeChecks: acknowledgeable })}>
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
                onClick={() => {
                    if (action === 'discard') {
                        setIsDiscardOpen(true);
                    } else {
                        ask(action);
                    }
                }}
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
                    <span className={styles.status}>{t(`parts.plan.status.${statusKey}`)}</span>
                    {summary && ` · ${summary}`}
                    {glance && <span className={styles.glance}>{` · ${glance}`}</span>}
                </span>
                <span className={styles.actions}>
                    {planRun && !withGatedActions && (
                        <Button variant="secondary" size="sm" disabled={planRun.status === 'cancelling' || cancelRun.isPending} onClick={stop}>
                            <SquareIcon size={14} />
                            {t('chat.stop')}
                        </Button>
                    )}
                    {actions.flatMap(actionButtons)}
                    {children}
                </span>
                {publish && (
                    <PublishDialog
                        phase={publish.phase}
                        pageCount={(counts.pending ?? 0) + (counts.failed ?? 0)}
                        summary={summary}
                        failureKey={publish.failureKey}
                        failedPages={(publications ?? []).filter(({ status }) => status === 'failed')}
                        onConfirm={() => transition('publish', publishing?.phase === 'confirm' ? publishing.options : {})}
                        onClose={() => setPublishing(undefined)}
                    />
                )}
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

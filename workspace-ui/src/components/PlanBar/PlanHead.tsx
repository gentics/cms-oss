import { useNavigate } from '@tanstack/react-router';
import { CircleAlertIcon, CircleCheckIcon, EyeIcon, GlobeIcon, type LucideIcon, RefreshCwIcon, RocketIcon, RouteIcon, SquareIcon, TrashIcon } from 'lucide-react';
import { type ReactNode, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { IconButton } from '@/components/IconButton/IconButton';
import { PublishingOverlay } from '@/components/PublishingOverlay/PublishingOverlay';
import { Button } from '@/components/ui/button';
import { Dialog, DialogBody, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { errorMessageKey, problemMessageKey } from '@/helper/errorMapper/errorMapper';
import { isFirstDraft, planActions } from '@/helper/planActions/planActions';
import { canAcknowledgeRefusal, latestGateRefusal, useCancelRun, usePlanTransition, useTransitionPlan, useWorkflows } from '@/hooks/useGenaixQueries';
import type { Plan, Publication, TransitionRequest, Workflow } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { isSessionWorking, selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import styles from './PlanBar.module.css';

type Action = TransitionRequest['action'];
// The actions with a button among the others; discard has its own at the start of the head.
type ButtonAction = Exclude<Action, 'discard'>;

const ACTION_ICONS: Record<ButtonAction, LucideIcon> = {
    release: RocketIcon,
    publish: GlobeIcon,
    request_review: EyeIcon,
};

const PRIMARY_ACTIONS = new Set<Action>(['release', 'publish']);

// The statuses a failed or stopped run leaves its plan in (contract §4: `drafting` after a proposing
// run, `accepted` after an execute run); the user's next message retries.
const STALLED_STATUSES: ReadonlySet<Plan['status']> = new Set<Plan['status']>(['drafting', 'accepted']);

type PublicationStatus = Publication['status'];

// The order the page outcome is told in: what went out first, what is left last.
const PUBLICATION_STATUSES: PublicationStatus[] = ['published', 'review_requested', 'failed', 'pending'];

// What a transition is sent with besides its action: acknowledged warnings ("… anyway").
interface TransitionOptions {
    acknowledgeChecks?: string[];
}

// Publishing under way, as `PlanHead` follows it: sent, with the run that carries it out once the `202`
// came. Whether it ended is read from the plan's status and the run's end.
interface Publishing {
    runId?: string;
}

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
    /** Controls after the actions, such as the plan bar's toggle. */
    children?: ReactNode;
    /** Said after the status, such as "Adjusted by you" (`usePlanAdjustment`). */
    note?: string;
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
 * (contract, `TransitionRequest.acknowledge_checks`). For a release and a user who may publish
 * (`canPublish`) that is "Publish anyway": the release with the warnings acknowledged, then at once the
 * publish (`releaseAndPublish`). A blocking check cannot be acknowledged, so then
 * only "Check again". Why it was refused, and the failing checks, are in the checklist above
 * (`ReviewPanel`). This holds until another transition is sent or the plan no longer offers that one.
 *
 * The gated transitions
 * (release, publish, request review) only with `withGatedActions`, on the review page, so that a user
 * releases or publishes there, with the checks in view; publish only with `canPublish`. Publish (and
 * after a refused publish "Publish anyway" and "Check again", which repeats it, as the gates only run
 * with the transition) covers the whole page while it is under way (`PublishingOverlay`); once the
 * plan is `published` or `review_requested` the page that says so opens
 * (`/sessions/$id/published?plan=`). A refusal by the gates shows in the checklist like any other; a
 * publish run that fails or is stopped leaves the plan `released` (contract) and is an error
 * notification. Discard asks first, saying what happens to the pages by the modules'
 * `plan_policy.on_discard` (`onDiscardOf`). Its button is a quiet icon (`trash`, design.md §9; red on hover) with its
 * name as tooltip, at the very start of the head, a line between it and the status, far from the way forward.
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
 * A plan its run left `drafting` or `accepted` (failed or stopped, contract §4) has no way forward on
 * the head: while no run works and nothing but discard is offered, the head says in place of the
 * buttons that the next message tells GenAIx how to go on, which retries the run.
 */
export function PlanHead({ sessionId, plan, isInCard = false, withGatedActions = false, canPublish = true, note, children }: PlanHeadProps) {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const transitionPlan = useTransitionPlan(sessionId);
    const cancelRun = useCancelRun(sessionId);
    const planTransition = usePlanTransition(sessionId, plan.id);
    const { pending } = planTransition;
    const publications = useWorkspaceEventStore((state) => selectSession(sessionId)(state).publications?.[plan.id]);
    const hasRun = useWorkspaceEventStore((state) => Boolean(selectSession(sessionId)(state).run));
    // Also a message on its way, before its run started.
    const isWorking = useWorkspaceEventStore((state) => isSessionWorking(selectSession(sessionId)(state)));
    // The plan's own run in progress, which Stop cancels.
    const planRun = useWorkspaceEventStore((state) => selectSession(sessionId)(state).runs?.findLast((live) => live.planId === plan.id));
    const [isDiscardOpen, setIsDiscardOpen] = useState(false);
    // The publishing under way, while the overlay shows it.
    const [publishing, setPublishing] = useState<Publishing>();
    const notices = useWorkspaceEventStore((state) => selectSession(sessionId)(state).notices);
    const onDiscard = onDiscardOf(plan, useWorkflows().data);

    function transition(action: Action, { acknowledgeChecks }: TransitionOptions = {}) {
        if (pending) {
            return;
        }

        const request: TransitionRequest = { action, ...(acknowledgeChecks && { acknowledge_checks: acknowledgeChecks }) };

        if (action !== 'publish') {
            // A refusal or any other failure is handled where every transition is (`useTransitionPlan`).
            transitionPlan.mutate({ planId: plan.id, request });

            return;
        }

        publish(request);
    }

    // Publish: the overlay follows it until it ended (the effect below). Any failure of the request, a
    // refusal by the gates included, is handled where every transition is (`useTransitionPlan`).
    function publish(request: TransitionRequest) {
        setPublishing({});
        transitionPlan.mutate({ planId: plan.id, request }, {
            onSuccess: (result) => setPublishing({ ...('run_id' in result && { runId: result.run_id }) }),
            onError: () => setPublishing(undefined),
        });
    }

    // "Publish anyway" after a release the gates refused with warnings only: the release with them
    // acknowledged, then, once it went through, the publish (its own `before_publish` gates run then; a
    // refusal of the publish shows as any other).
    function releaseAndPublish(acknowledgeChecks: string[]) {
        if (pending) {
            return;
        }

        transitionPlan.mutate({ planId: plan.id, request: { action: 'release', acknowledge_checks: acknowledgeChecks } }, {
            onSuccess: () => publish({ action: 'publish' }),
        });
    }

    // Where the publishing stands: the plan's status says it went out (`published`, or `review_requested`
    // once a page went to review), and the page that says so opens; the run that carries it out ending as
    // failed or cancelled says it did not (the plan stays `released`, contract).
    const publishRunId = publishing?.runId;
    const publishEnd = publishRunId === undefined ? undefined : notices?.find(({ runId, kind }) => runId === publishRunId && (kind === 'failed' || kind === 'cancelled'));
    const hasPublished = plan.status === 'published' || plan.status === 'review_requested';
    const isPublishing = Boolean(publishing) && !hasPublished && !publishEnd;
    // The publishing whose end was acted on, so that it is acted on once.
    const endedPublishing = useRef<Publishing>(undefined);

    useEffect(() => {
        if (!publishing || endedPublishing.current === publishing || !(hasPublished || publishEnd)) {
            return;
        }

        endedPublishing.current = publishing;

        if (hasPublished) {
            void navigate({ to: '/sessions/$id/published', params: { id: sessionId }, search: { plan: plan.id } });
        } else if (publishEnd) {
            useErrorNotificationStore.getState().addError(publishEnd.kind === 'failed'
                ? { messageKey: 'planBar.publishing.failed', detailKey: problemMessageKey(publishEnd.error) }
                : { messageKey: 'planBar.publishing.cancelled' });
        }
    }, [publishing, hasPublished, publishEnd, navigate, sessionId, plan.id]);

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
    // Discard leads the head, before the status; the other actions follow it.
    const canDiscard = actions.includes('discard');
    const buttonActions = actions.filter((action): action is ButtonAction => action !== 'discard');
    // Left by a failed or stopped run, with nothing on the head to go on with.
    const isStalled = STALLED_STATUSES.has(plan.status) && !hasRun && !isWorking && buttonActions.length === 0;
    // A revision is drafted with the items of the version it revises (rc.5); a first draft has none.
    const statusKey = plan.status === 'drafting' && !isFirstDraft(plan) ? 'revising' : plan.status;
    const refusal = withGatedActions ? latestGateRefusal(planTransition, actions) : undefined;
    const acknowledgeable = refusal?.problem.acknowledgeable_checks ?? [];
    const canAcknowledge = refusal ? canAcknowledgeRefusal(refusal.problem) : false;

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
    function actionButtons(action: ButtonAction) {
        const ActionIcon = ACTION_ICONS[action];

        if (action === refusal?.action) {
            // A release refused for warnings goes on to publish at once, for a user who may publish.
            const isPublishAnyway = action === 'release' && canPublish;
            const AnywayIcon = isPublishAnyway ? GlobeIcon : ActionIcon;

            return [
                <Button key={`${action}-retry`} variant={canAcknowledge ? 'secondary' : 'primary'} size="sm" disabled={Boolean(pending)} onClick={() => transition(action)}>
                    <RefreshCwIcon size={14} />
                    {t('planBar.gate.retry')}
                </Button>,
                canAcknowledge && (
                    <Button
                        key={`${action}-anyway`}
                        variant="primary"
                        size="sm"
                        disabled={Boolean(pending)}
                        onClick={() => (isPublishAnyway ? releaseAndPublish(acknowledgeable) : transition(action, { acknowledgeChecks: acknowledgeable }))}
                    >
                        <AnywayIcon size={14} />
                        {t(`planBar.gate.anyway.${isPublishAnyway ? 'publish' : action}`)}
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
                onClick={() => transition(action)}
            >
                <ActionIcon size={14} />
                {t(`planBar.actions.${action}`)}
            </Button>,
        ];
    }

    return (
        <div className={[styles.head, isInCard && styles.inCard, canDiscard && styles.withDiscard, tone].filter(Boolean).join(' ')}>
            {canDiscard && (
                <IconButton
                    label={t('planBar.actions.discard')}
                    variant="ghost-danger"
                    size="icon"
                    disabled={Boolean(pending) || hasRun}
                    onClick={() => setIsDiscardOpen(true)}
                >
                    <TrashIcon size={16} />
                </IconButton>
            )}
            {canDiscard && <span className={styles.divider} aria-hidden />}
            <Icon size={16} className={styles.icon} aria-hidden />
            <span className={styles.text}>
                <span className={styles.status}>{t(`parts.plan.status.${statusKey}`)}</span>
                {summary && ` · ${summary}`}
                {note && ` · ${note}`}
            </span>
            <span className={styles.actions}>
                {planRun && !withGatedActions && (
                    <Button variant="secondary" size="sm" disabled={planRun.status === 'cancelling' || cancelRun.isPending} onClick={stop}>
                        <SquareIcon size={14} />
                        {t('chat.stop')}
                    </Button>
                )}
                {buttonActions.flatMap(actionButtons)}
                {isStalled && <span className={styles.hint}>{t('planBar.stalled')}</span>}
                {children}
            </span>
            {isPublishing && <PublishingOverlay pageCount={(counts.pending ?? 0) + (counts.failed ?? 0)} />}
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
                            <TrashIcon size={14} />
                            {t('planBar.actions.discard')}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}

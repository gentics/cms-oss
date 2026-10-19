import { Link, useNavigate } from '@tanstack/react-router';
import { BanIcon, CheckCheckIcon, ChevronDownIcon, CircleAlertIcon, CircleCheckIcon, CircleDashedIcon, InfoIcon, type LucideIcon, SendIcon, TriangleAlertIcon } from 'lucide-react';
import { useEffect, useId, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { PlanHead } from '@/components/PlanBar/PlanHead';
import { ThinkingDots } from '@/components/ThinkingIndicator/ThinkingIndicator';
import { Button } from '@/components/ui/button';
import { errorMessageKey, problemMessageKey } from '@/helper/errorMapper/errorMapper';
import { checkSummary, isCheckForPage, isFailingCheck, isGateCheck } from '@/helper/pageChecks/pageChecks';
import { GATED_ACTIONS, planActions, writtenPagesOf } from '@/helper/planActions/planActions';
import { planMessage } from '@/helper/planMessage/planMessage';
import { useCanPublishCmsPage, useCanPublishCmsPages } from '@/hooks/useCmsQueries';
import { canAcknowledgeRefusal, type GateRefusal, latestGateRefusal, usePlanTransition, useSendMessage, useTransitionPlan } from '@/hooks/useGenaixQueries';
import type { Check, Plan, PlanState, PlanStatus, Publication } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { isSessionWorking, selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import styles from './ReviewPanel.module.css';

const CLOSED: PlanStatus[] = ['published', 'review_requested', 'discarded', 'superseded'];

// "Accept & review": the version accepted here and, once the message was taken, the run that carries it out.
interface Accepting {
    version: number;
    runId?: string;
}

const SEVERITY_ICONS: Record<Check['severity'], LucideIcon> = { pass: CircleCheckIcon, info: InfoIcon, warn: CircleAlertIcon, fail: CircleAlertIcon };

const PUBLICATION_ICONS: Record<Publication['status'], LucideIcon> = { pending: CircleDashedIcon, published: CircleCheckIcon, review_requested: SendIcon, failed: CircleAlertIcon };

// The checks grouped by where they come from (a guideline, a requirement, a gate), in their order.
function groupChecks(checks: Check[]): [string, Check[]][] {
    const groups = new Map<string, Check[]>();

    for (const check of checks) {
        const name = check.source.label ?? check.source.id ?? check.source.type;

        groups.set(name, [...(groups.get(name) ?? []), check]);
    }

    return [...groups];
}

// The checks with those a refusal reported (`failing_checks`, full `Check` objects) in place of the plan's
// own: the problem says it at once, the plan's state only once it is read again after `check.updated`.
function withRefusedChecks(checks: Check[], refusal: GateRefusal | undefined): Check[] {
    const refused = refusal?.problem.failing_checks ?? [];
    const byId = new Map(refused.map((check) => [check.id, check]));
    const known = new Set(checks.map(({ id }) => id));

    return [...checks.map((check) => byId.get(check.id) ?? check), ...refused.filter(({ id }) => !known.has(id))];
}

/**
 * What the quality gates decided, at the top of the checklist: the transition they refused and why
 * (`409 quality_gate_failed`: blocking checks to fix, or warnings to acknowledge), or once the plan is
 * released, that every check passed, or that it was released with warnings the user acknowledged.
 * Nothing before the gates have decided.
 */
function CheckOutcome({ plan, refusal, checks }: { plan: Plan; refusal?: GateRefusal; checks: Check[] }) {
    const { t } = useTranslation();
    let kind: 'blocking' | 'warning' | 'passed';
    let text: string;

    if (refusal) {
        const { action, problem } = refusal;

        if (canAcknowledgeRefusal(problem)) {
            kind = 'warning';
            text = t(`review.outcome.warning.${action}`, { count: (problem.acknowledgeable_checks ?? []).length });
        } else {
            const count = (problem.failing_checks ?? []).filter(({ blocking }) => blocking).length || (problem.failing_gates ?? []).length;

            kind = 'blocking';
            text = t(`review.outcome.blocking.${action}`, { count });
        }
    } else if (plan.status === 'released') {
        // A released plan's warnings were acknowledged on its release (each check says so).
        const warnings = checks.filter(isFailingCheck).length;

        kind = warnings ? 'warning' : 'passed';
        text = warnings ? t('review.outcome.withWarnings', { count: warnings }) : t('review.outcome.passed');
    } else {
        return null;
    }

    const Icon = { blocking: BanIcon, warning: TriangleAlertIcon, passed: CircleCheckIcon }[kind];

    return (
        <p className={styles.outcome} data-kind={kind} role="status">
            <Icon size={16} aria-hidden className={styles.outcomeIcon} />
            {text}
        </p>
    );
}

interface ReviewPanelProps {
    sessionId: string;
    state: PlanState;
    /** The page shown, when the plan wrote several (`ReviewPages`): the checklist is that page's. */
    page?: Publication;
}

/**
 * The page shown, of a plan that wrote several: its publish status, and while it is not published yet
 * whether the user may publish it (`useCanPublishCmsPage`) or it needs a review; why it failed, if it did.
 */
function PageOutcome({ page }: { page: Publication }) {
    const { t } = useTranslation();
    // A status outside the contract's enum shows as pending (client rule: tolerate unknown enum values).
    const status = Object.hasOwn(PUBLICATION_ICONS, page.status) ? page.status : 'pending';
    const Icon = PUBLICATION_ICONS[status];
    const canPublish = useCanPublishCmsPage(status === 'pending' ? page.cms_ref : undefined);
    const name = page.cms_ref.name || t('preview.pages.unnamed', { id: page.cms_ref.id });

    return (
        <section className={styles.publication} data-status={status} aria-label={name}>
            <div className={styles.publicationHead}>
                <span className={styles.pageName}>{name}</span>
                <span className={styles.publicationStatus}>
                    <Icon size={12} aria-hidden />
                    {t(`review.publication.${status}`)}
                </span>
            </div>
            {canPublish !== undefined && <span className={styles.meta}>{t(canPublish ? 'review.page.canPublish' : 'review.page.needsReview')}</span>}
            {status === 'failed' && page.error && <span className={styles.error}>{page.error.detail ?? page.error.title}</span>}
        </section>
    );
}

/**
 * The right panel of the review page (design.md §11 "Review"): the plan's checklist, and at the bottom the plan's head (`PlanHead`, the same as
 * in the chat's plan bar): its status and the transitions it allows, release first, then publish or
 * request review, a refusal by the quality gates shown in place above them, and the confirmation before
 * discarding. The gated transitions are only offered here, not in the chat; publish only to a user the CMS
 * lets publish every page the plan wrote (`useCanPublishCmsPages`), asked while the plan is released.
 *
 * The quality gates run with every gated transition (contract, transitions route): the release "Review &
 * release" sends on the way here (`ReviewButton`), "Check again", "… anyway", publish and request review.
 * While one is on its way the panel's body is blurred and out of reach, the gates' work in front of it;
 * their checks arrive as `check.updated` and the plan is read again. Then the checklist opens with what
 * they decided (`CheckOutcome`): all passed and released, so the plan can be published or sent to review
 * below; or refused, with warnings the user may acknowledge ("Release anyway") or blocking checks to fix.
 * A refusal's checks show as the problem reported them (`withRefusedChecks`). The checks that passed are
 * folded into one line under it (design.md §12 "erfüllte Punkte zusammenklappbar"), the others listed
 * by where they come from. A failing check's suggested
 * fix is sent to the agent as a revision of this plan, and the chat opens to follow it; a closed plan
 * offers no fixes.
 *
 * A page a publish failed for shows above the checklist with why (`PageOutcome`); publish again
 * retries it.
 *
 * A proposed plan, a revision of a plan accepted before above all (rc.5: a message about its work
 * revises it), offers "Accept & review": the user's acceptance as proposed (`planMessage`, a `plan` part
 * in `execute` mode, contract §5.3), then, while the run carries out its pending items, the panel is out
 * of reach with that in front of it, or with the plan's question and the way to the chat, where it is
 * answered. Once the plan is `done` it is released, so the gates check it as on "Review & release". A
 * run that fails or is stopped leaves the plan `accepted` (contract) and is an error notification.
 *
 * With `page`, of a plan that wrote several, the panel is about that page: its outcome (`PageOutcome`)
 * above the checklist, and of the checks only those naming it as `target` and those naming no page,
 * which are about the whole plan. The transitions stay the plan's: they act on all of its pages.
 */
export function ReviewPanel({ sessionId, state, page }: ReviewPanelProps) {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const titleId = useId();
    const { plan } = state;
    const planTransition = usePlanTransition(sessionId, plan.id);
    const pendingAction = planTransition.pending?.request.action;
    const isChecking = pendingAction !== undefined && GATED_ACTIONS.has(pendingAction);
    const refusal = latestGateRefusal(planTransition, planActions(plan.status, true));
    // Only the gates' checks: the agent's own findings never hold up a transition.
    const gateChecks = withRefusedChecks(state.checks, refusal).filter(isGateCheck);
    const checks = page ? gateChecks.filter((check) => isCheckForPage(check, page.cms_ref)) : gateChecks;
    const sendMessage = useSendMessage(sessionId);
    const isClosed = CLOSED.includes(plan.status);
    // Without a page picked (a plan that wrote one), the pages a publish failed for, each with why.
    const failedPages = page ? [] : (state.publications ?? []).filter(({ status }) => status === 'failed');
    const passedChecks = checks.filter(({ severity }) => severity === 'pass');
    const otherChecks = checks.filter(({ severity }) => severity !== 'pass');
    const passedId = useId();
    const [isPassedOpen, setIsPassedOpen] = useState(false);
    // Asked while the plan can go on to publish: released, or done ("Publish anyway" after a refused release).
    const writtenPages = plan.status === 'released' || plan.status === 'done' ? writtenPagesOf(state.cms_objects) : [];
    const canPublish = useCanPublishCmsPages(writtenPages);
    const transitionPlan = useTransitionPlan(sessionId);
    const [accepting, setAccepting] = useState<Accepting>();
    const isWorking = useWorkspaceEventStore((store) => isSessionWorking(selectSession(sessionId)(store)));
    const hasPlanQuestion = useWorkspaceEventStore((store) => {
        const session = selectSession(sessionId)(store);

        return (session.interactions ?? []).some((interaction) => session.interactionPlans?.[interaction.id] === plan.id);
    });
    const acceptRunId = accepting?.runId;
    const acceptEnd = useWorkspaceEventStore((store) => (acceptRunId === undefined
        ? undefined
        : selectSession(sessionId)(store).notices?.find(({ runId, kind }) => runId === acceptRunId && (kind === 'failed' || kind === 'cancelled'))));
    // From the acceptance until the plan is done: the version accepted here, then its execute run.
    const isCarryingOut = Boolean(accepting) && !acceptEnd && (plan.status === 'accepted' || plan.status === 'running' || (plan.status === 'proposed' && plan.version === accepting?.version));
    // The acceptance whose end was acted on (released, or reported as failed), so that it is acted on once.
    const endedAccepting = useRef<Accepting>(undefined);

    useEffect(() => {
        if (!accepting || endedAccepting.current === accepting) {
            return;
        }

        if (plan.status === 'done') {
            endedAccepting.current = accepting;
            transitionPlan.mutate({ planId: plan.id, request: { action: 'release' } });
        } else if (acceptEnd) {
            endedAccepting.current = accepting;
            useErrorNotificationStore.getState().addError(acceptEnd.kind === 'failed'
                ? { messageKey: 'review.acceptFailed', detailKey: problemMessageKey(acceptEnd.error) }
                : { messageKey: 'review.acceptFailed' });
        }
    }, [accepting, acceptEnd, plan.status, plan.id, transitionPlan]);

    function acceptAndReview() {
        const version = plan.version;

        setAccepting({ version });
        sendMessage.mutate(planMessage(plan), {
            onSuccess: (accepted) => setAccepting({ version, runId: accepted.run_id }),
            onError: (error) => {
                setAccepting(undefined);
                useErrorNotificationStore.getState().addError({ messageKey: 'review.acceptFailed', detailKey: errorMessageKey(error) });
            },
        });
    }

    // A suggested fix is a message to the agent; the chat shows how it goes. It revises this plan
    // (`options.mode: revise`, rc.5) rather than leaving it to the session agent, which may draft a
    // new plan: the fix is one more item of the same work, and the next release checks all of it.
    function fix(prompt: string) {
        sendMessage.mutate({ parts: [{ type: 'text', text: prompt }], options: { mode: 'revise', plan_id: plan.id } });
        void navigate({ to: '/sessions/$id', params: { id: sessionId } });
    }

    // The checks of `list` grouped by where they come from, each with its state in words and, while it
    // fails, what it found in one sentence (`checkSummary`) and its suggested fixes.
    function checkGroups(list: Check[]) {
        return groupChecks(list).map(([name, group]) => (
            <div key={name} className={styles.checkGroup} role="group" aria-label={name}>
                <div className={styles.checkGroupHead}>{name}</div>
                <ul className={styles.list}>
                    {group.map((check) => {
                        const Icon = check.blocking && isFailingCheck(check) ? BanIcon : SEVERITY_ICONS[check.severity] ?? InfoIcon;

                        return (
                            <li key={check.id} className={styles.check} data-severity={check.severity}>
                                <Icon size={14} aria-hidden className={styles.checkIcon} />
                                <div className={styles.checkText}>
                                    <span className={styles.checkLabel}>{check.label}</span>
                                    <span className={styles.meta}>
                                        {t(`review.severity.${check.severity}`)}
                                        {check.blocking && isFailingCheck(check) && ` · ${t('review.blocking')}`}
                                        {check.acknowledged_by && ` · ${t('review.acknowledged')}`}
                                    </span>
                                    {isFailingCheck(check) && <span className={styles.message}>{checkSummary(check)}</span>}
                                    {isFailingCheck(check) && !isClosed && check.suggested_actions?.map((suggestion) => (
                                        <Button key={suggestion.label} variant="secondary" size="sm" className={styles.fix} onClick={() => fix(suggestion.prompt)}>
                                            {suggestion.label}
                                        </Button>
                                    ))}
                                </div>
                            </li>
                        );
                    })}
                </ul>
            </div>
        ));
    }

    return (
        <section className={styles.panel} aria-labelledby={titleId}>
            <header className={styles.head}>
                <h2 id={titleId} className={styles.title}>{t('review.title')}</h2>
            </header>

            <div className={styles.main}>
                <div className={styles.body} data-checking={isChecking || isCarryingOut || undefined} aria-busy={isChecking || isCarryingOut} inert={isChecking || isCarryingOut}>
                    <p className={styles.summary}>{plan.summary}</p>

                    {page && <PageOutcome key={page.cms_ref.id} page={page} />}
                    {failedPages.map((failed) => <PageOutcome key={failed.cms_ref.id} page={failed} />)}

                    <section className={styles.section} aria-label={t('review.checklist')}>
                        <h3 className={styles.overline}>{t('review.checklist')}</h3>
                        {!isChecking && <CheckOutcome plan={plan} refusal={refusal} checks={gateChecks} />}
                        {checks.length === 0 && <p className={styles.empty}>{t('review.noChecks')}</p>}
                        {passedChecks.length > 0 && (
                            <div className={styles.passed}>
                                <Button variant="ghost" size="sm" aria-expanded={isPassedOpen} aria-controls={passedId} onClick={() => setIsPassedOpen((open) => !open)}>
                                    <CircleCheckIcon size={14} aria-hidden className={styles.passedIcon} />
                                    {t('review.passedChecks', { count: passedChecks.length })}
                                    <span className={styles.chevron} data-open={isPassedOpen || undefined}>
                                        <ChevronDownIcon size={14} aria-hidden />
                                    </span>
                                </Button>
                                <div id={passedId} className={styles.passedList} hidden={!isPassedOpen}>
                                    {isPassedOpen && checkGroups(passedChecks)}
                                </div>
                            </div>
                        )}
                        {checkGroups(otherChecks)}
                    </section>
                </div>
                {(isChecking || isCarryingOut) && (
                    <div className={styles.checking} role="status">
                        {isCarryingOut && hasPlanQuestion
                            ? (
                                <>
                                    {t('review.question')}
                                    <Link to="/sessions/$id" params={{ id: sessionId }}>{t('review.answerInChat')}</Link>
                                </>
                            )
                            : (
                                <>
                                    <ThinkingDots isLarge />
                                    {t(isCarryingOut ? 'review.carryingOut' : 'review.checking')}
                                </>
                            )}
                    </div>
                )}
            </div>

            <footer className={styles.foot}>
                <PlanHead sessionId={sessionId} plan={plan} isInCard withGatedActions canPublish={canPublish}>
                    {plan.status === 'proposed' && (
                        <Button variant="primary" size="sm" disabled={isWorking || isCarryingOut} onClick={acceptAndReview}>
                            <CheckCheckIcon size={14} />
                            {t('review.acceptAndReview')}
                        </Button>
                    )}
                </PlanHead>
            </footer>
        </section>
    );
}

import { useNavigate } from '@tanstack/react-router';
import { BanIcon, CircleAlertIcon, CircleCheckIcon, CircleDashedIcon, InfoIcon, type LucideIcon, SendIcon } from 'lucide-react';
import { useId } from 'react';
import { useTranslation } from 'react-i18next';

import { PlanHead } from '@/components/PlanBar/PlanHead';
import { ThinkingDots } from '@/components/ThinkingIndicator/ThinkingIndicator';
import { Button } from '@/components/ui/button';
import { isCheckForPage, isFailingCheck } from '@/helper/pageChecks/pageChecks';
import { writtenPagesOf } from '@/helper/planActions/planActions';
import { useCanPublishCmsPage, useCanPublishCmsPages } from '@/hooks/useCmsQueries';
import { usePlanTransition, useSendMessage } from '@/hooks/useGenaixQueries';
import type { Check, PlanState, PlanStatus, Publication } from '@/services/apiService/genaix/types';

import styles from './ReviewPanel.module.css';

const CLOSED: PlanStatus[] = ['published', 'review_requested', 'discarded', 'superseded'];

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
 * lets publish every page the plan wrote (`useCanPublishCmsPages`), asked while the plan is released. While the quality gates
 * check the plan, on the release "Review & release" sent on the way here (`ReviewButton`) or on "Check
 * again" (`isCheck`), the checklist says so; their checks arrive as `check.updated` and the plan is
 * read again. A release the user makes with its own button shows no check in progress. A failing check's suggested fix is sent to the agent, and the chat opens to follow it; a
 * closed plan offers no fixes.
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
    const checks = page ? state.checks.filter((check) => isCheckForPage(check, page.cms_ref)) : state.checks;
    const sendMessage = useSendMessage(sessionId);
    const isClosed = CLOSED.includes(plan.status);
    const isChecking = Boolean(usePlanTransition(sessionId, plan.id).pending?.isCheck);
    const writtenPages = plan.status === 'released' ? writtenPagesOf(state.cms_objects) : [];
    const canPublish = useCanPublishCmsPages(writtenPages);

    // A suggested fix is a message to the agent; the chat shows how it goes.
    function fix(prompt: string) {
        sendMessage.mutate({ parts: [{ type: 'text', text: prompt }] });
        void navigate({ to: '/sessions/$id', params: { id: sessionId } });
    }

    return (
        <section className={styles.panel} aria-labelledby={titleId}>
            <header className={styles.head}>
                <h2 id={titleId} className={styles.title}>{t('review.title')}</h2>
            </header>

            <div className={styles.body}>
                <p className={styles.summary}>{plan.summary}</p>

                {page && <PageOutcome key={page.cms_ref.id} page={page} />}

                <section className={styles.section} aria-label={t('review.checklist')}>
                    <h3 className={styles.overline}>{t('review.checklist')}</h3>
                    {isChecking && (
                        <p className={styles.checking} role="status">
                            <ThinkingDots />
                            {t('review.checking')}
                        </p>
                    )}
                    {checks.length === 0 && !isChecking && <p className={styles.empty}>{t('review.noChecks')}</p>}
                    {groupChecks(checks).map(([name, group]) => (
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
                                                {isFailingCheck(check) && <span className={styles.message}>{check.message}</span>}
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
                    ))}
                </section>
            </div>

            <footer className={styles.foot}>
                <PlanHead sessionId={sessionId} plan={plan} isInCard withGatedActions canPublish={canPublish} />
            </footer>
        </section>
    );
}

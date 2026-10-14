import { useNavigate } from '@tanstack/react-router';
import { BanIcon, CircleAlertIcon, CircleCheckIcon, CircleDashedIcon, CircleXIcon, InfoIcon, type LucideIcon, MinusIcon, SendIcon } from 'lucide-react';
import { useId } from 'react';
import { useTranslation } from 'react-i18next';

import { PlanHead } from '@/components/PlanBar/PlanHead';
import { Button } from '@/components/ui/button';
import { useSendMessage } from '@/hooks/useGenaixQueries';
import type { ApprovalStep, Check, PlanState, PlanStatus, Publication } from '@/services/apiService/genaix/types';

import styles from './ReviewPanel.module.css';

const CLOSED: PlanStatus[] = ['published', 'review_requested', 'discarded', 'superseded'];

const SEVERITY_ICONS: Record<Check['severity'], LucideIcon> = { pass: CircleCheckIcon, info: InfoIcon, warn: CircleAlertIcon, fail: CircleAlertIcon };

const PUBLICATION_ICONS: Record<Publication['status'], LucideIcon> = { pending: CircleDashedIcon, published: CircleCheckIcon, review_requested: SendIcon, failed: CircleAlertIcon };

const APPROVAL_ICONS: Record<ApprovalStep['status'], LucideIcon> = { pending: CircleDashedIcon, done: CircleCheckIcon, not_required: MinusIcon, rejected: CircleXIcon };

// The checks grouped by where they come from (a guideline, a requirement, a gate), in their order.
function groupChecks(checks: Check[]): [string, Check[]][] {
    const groups = new Map<string, Check[]>();

    for (const check of checks) {
        const name = check.source.label ?? check.source.id ?? check.source.type;

        groups.set(name, [...(groups.get(name) ?? []), check]);
    }

    return [...groups];
}

function isFailing(check: Check): boolean {
    return check.severity === 'warn' || check.severity === 'fail';
}

function ApprovalSteps({ steps }: { steps: ApprovalStep[] }) {
    const { t } = useTranslation();

    return (
        <ol className={styles.approval}>
            {steps.map((step) => {
                const Icon = APPROVAL_ICONS[step.status] ?? CircleDashedIcon;

                return (
                    <li key={step.id} className={styles.approvalStep} data-status={step.status}>
                        <Icon size={13} aria-hidden />
                        <span className={styles.approvalLabel}>{step.label}</span>
                        <span className={styles.meta}>{t(`review.approval.${step.status}`)}</span>
                    </li>
                );
            })}
        </ol>
    );
}

interface ReviewPanelProps {
    sessionId: string;
    state: PlanState;
}

/**
 * The right panel of the review page (design.md §11 "Review"): the plan's checklist, its approval chain
 * and the outcome per page (`publications`), and at the bottom the plan's head (`PlanHead`, the same as
 * in the chat's plan bar): its status and the transitions it allows, release first, then publish or
 * request review, a refusal by the quality gates shown in place above them, and the confirmation before
 * discarding. The gated transitions are only offered here, not in the chat. A failing check's
 * suggested fix is sent to the agent, and the chat opens to follow it; a closed plan offers no fixes.
 */
export function ReviewPanel({ sessionId, state }: ReviewPanelProps) {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const titleId = useId();
    const { plan, checks, approval_chain: approvalChain, publications = [] } = state;
    const sendMessage = useSendMessage(sessionId);
    const isClosed = CLOSED.includes(plan.status);

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

                <section className={styles.section} aria-label={t('review.checklist')}>
                    <h3 className={styles.overline}>{t('review.checklist')}</h3>
                    {checks.length === 0 && <p className={styles.empty}>{t('review.noChecks')}</p>}
                    {groupChecks(checks).map(([name, group]) => (
                        <div key={name} className={styles.checkGroup} role="group" aria-label={name}>
                            <div className={styles.checkGroupHead}>{name}</div>
                            <ul className={styles.list}>
                                {group.map((check) => {
                                    const Icon = check.blocking && isFailing(check) ? BanIcon : SEVERITY_ICONS[check.severity] ?? InfoIcon;

                                    return (
                                        <li key={check.id} className={styles.check} data-severity={check.severity}>
                                            <Icon size={14} aria-hidden className={styles.checkIcon} />
                                            <div className={styles.checkText}>
                                                <span className={styles.checkLabel}>{check.label}</span>
                                                <span className={styles.meta}>
                                                    {t(`review.severity.${check.severity}`)}
                                                    {check.blocking && isFailing(check) && ` · ${t('review.blocking')}`}
                                                    {check.acknowledged_by && ` · ${t('review.acknowledged')}`}
                                                </span>
                                                {isFailing(check) && <span className={styles.message}>{check.message}</span>}
                                                {isFailing(check) && !isClosed && check.suggested_actions?.map((suggestion) => (
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
                <PlanHead sessionId={sessionId} plan={plan} isInCard withGatedActions />
            </footer>
        </section>
    );
}

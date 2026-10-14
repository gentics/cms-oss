import { CircleCheckIcon, CircleMinusIcon, CircleXIcon, ClockIcon, type LucideIcon } from 'lucide-react';
import { useId } from 'react';
import { useTranslation } from 'react-i18next';

import type { ApprovalStep } from '@/services/apiService/genaix/types';
import { selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import styles from './PlanDetails.module.css';

const APPROVAL_ICONS: Record<ApprovalStep['status'], LucideIcon> = { pending: ClockIcon, done: CircleCheckIcon, not_required: CircleMinusIcon, rejected: CircleXIcon };

/**
 * Who has to approve a plan's pages (`approval_chain.updated`, rc.4 keyed by `plan_id`), under its steps
 * in the plan bar; nothing while there is no chain. Status always as icon and word (design.md §3.3).
 */
export function PlanDetails({ sessionId, planId }: { sessionId: string; planId: string }) {
    const { t } = useTranslation();
    const headingId = useId();
    const chain = useWorkspaceEventStore((state) => selectSession(sessionId)(state).approvalChains?.[planId]);

    if (!chain?.length) {
        return null;
    }

    return (
        <section className={styles.details} aria-labelledby={headingId}>
            <h3 id={headingId} className={styles.heading}>{t('planDetails.approval')}</h3>
            <ol className={styles.list}>
                {chain.map((step) => {
                    const Icon = APPROVAL_ICONS[step.status];

                    return (
                        <li key={step.id} className={styles.step} data-status={step.status}>
                            <Icon size={16} className={styles.icon} aria-hidden />
                            <span className={styles.text}>
                                <span className={styles.label}>{step.label}</span>
                                <span className={styles.detail}>
                                    {step.actor.label ?? t(`planDetails.actor.${step.actor.type}`)}
                                    {step.reason && ` · ${step.reason}`}
                                </span>
                            </span>
                            <span className={styles.status}>{t(`planDetails.approvalStatus.${step.status}`)}</span>
                        </li>
                    );
                })}
            </ol>
        </section>
    );
}
